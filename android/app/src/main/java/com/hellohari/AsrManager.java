package com.hellohari;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Manages the ASR pipeline with automatic fallback:
 *
 *   Primary:  WebSocket → hello-hari-recorder backend (IndicConformer + Whisper)
 *   Fallback: Android SpeechRecognizer (Google Cloud)
 *
 * Captures microphone audio via AudioRecord (16 kHz mono PCM int16) and streams
 * it to whichever engine is active. Scam analysis is handled:
 *   - By the backend (primary) — results arrive in the WebSocket messages
 *   - By ScamPatternEngine locally (fallback) — run on Google's transcript
 */
public final class AsrManager {

    private static final String TAG = "AsrManager";
    private static final int SAMPLE_RATE = 16000;

    public enum Engine { NONE, BACKEND, GOOGLE }

    public interface Listener {
        void onEngineChanged(Engine engine);
        void onListening();
        void onTranscription(String text, String language);
        void onScamResult(boolean isScam, int riskScore, String explanation);
        void onError(String message);
        void onSessionEnd();
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private StreamingAsrClient streamingClient;
    private GoogleAsrClient googleClient;
    private ScamPatternEngine patternEngine;

    private Listener listener;
    private Engine activeEngine = Engine.NONE;
    private String language = "hi";
    private String serverUrl = "";

    // Audio capture
    private AudioRecord audioRecord;
    private Thread captureThread;
    private volatile boolean capturing = false;

    public AsrManager(Context context) {
        this.context = context;
        try {
            this.patternEngine = ScamPatternEngine.getInstance(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load ScamPatternEngine", e);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setServerUrl(String url) {
        this.serverUrl = url;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Engine getActiveEngine() {
        return activeEngine;
    }

    /**
     * Start the ASR pipeline. Tries backend first, falls back to Google.
     */
    public void start() {
        if (activeEngine != Engine.NONE) {
            Log.w(TAG, "Already running");
            return;
        }

        if (!serverUrl.isEmpty()) {
            startWithBackend();
        } else {
            startWithGoogle();
        }
    }

    public void stop() {
        stopCapture();
        if (streamingClient != null) {
            streamingClient.stop();
            // Don't disconnect — server will close after draining
        }
        if (googleClient != null) {
            googleClient.stop();
        }
        activeEngine = Engine.NONE;
        notifyOnMainThread(() -> {
            if (listener != null) listener.onSessionEnd();
        });
    }

    public void destroy() {
        stopCapture();
        if (streamingClient != null) {
            streamingClient.disconnect();
            streamingClient = null;
        }
        if (googleClient != null) {
            googleClient.stop();
            googleClient = null;
        }
        activeEngine = Engine.NONE;
    }

    // --- Backend (primary) ---

    private void startWithBackend() {
        streamingClient = new StreamingAsrClient();
        streamingClient.setListener(new StreamingAsrClient.Listener() {
            @Override
            public void onConnected() {
                activeEngine = Engine.BACKEND;
                notifyOnMainThread(() -> {
                    if (listener != null) {
                        listener.onEngineChanged(Engine.BACKEND);
                        listener.onListening();
                    }
                });
                startCapture();
            }

            @Override
            public void onTranscription(String text, String lang, boolean isScam,
                                        double riskScore, String explanation) {
                notifyOnMainThread(() -> {
                    if (listener != null) {
                        listener.onTranscription(text, lang);
                        listener.onScamResult(isScam, (int) Math.round(riskScore * 100), explanation);
                    }
                });
            }

            @Override
            public void onSilence() { /* no-op */ }

            @Override
            public void onSessionEnd(int totalChunks) {
                activeEngine = Engine.NONE;
                notifyOnMainThread(() -> {
                    if (listener != null) listener.onSessionEnd();
                });
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Backend error: " + message + " — falling back to Google");
                stopCapture();
                if (streamingClient != null) {
                    streamingClient.disconnect();
                }
                notifyOnMainThread(() -> startWithGoogle());
            }
        });

        streamingClient.connect(serverUrl, language);
    }

    // --- Google SpeechRecognizer (fallback) ---

    private void startWithGoogle() {
        googleClient = new GoogleAsrClient(context);

        if (!googleClient.isAvailable()) {
            notifyOnMainThread(() -> {
                if (listener != null) listener.onError("No speech recognition available");
            });
            return;
        }

        googleClient.setListener(new GoogleAsrClient.Listener() {
            @Override
            public void onReady() {
                activeEngine = Engine.GOOGLE;
                notifyOnMainThread(() -> {
                    if (listener != null) {
                        listener.onEngineChanged(Engine.GOOGLE);
                        listener.onListening();
                    }
                });
            }

            @Override
            public void onPartialResult(String text) {
                // Optionally show partial text — skip scam analysis on partials
            }

            @Override
            public void onFinalResult(String text, float confidence) {
                notifyOnMainThread(() -> {
                    if (listener != null) {
                        listener.onTranscription(text, language);
                    }
                    // Run scam analysis locally
                    analyzeLocally(text);
                });
            }

            @Override
            public void onError(String message) {
                notifyOnMainThread(() -> {
                    if (listener != null) listener.onError(message);
                });
            }

            @Override
            public void onEnd() { /* auto-restarts in continuous mode */ }
        });

        // Google SpeechRecognizer must be started on the main thread
        mainHandler.post(() -> googleClient.start(language, true));
    }

    /** Analyze transcript locally using ScamPatternEngine (for Google fallback). */
    private void analyzeLocally(String text) {
        if (patternEngine == null || text == null || text.trim().isEmpty()) return;

        ScamPatternEngine.Result result = patternEngine.analyze(text);
        if (listener != null) {
            listener.onScamResult(
                    result.isScam(),
                    result.getRiskScore(),
                    result.getExplanation()
            );
        }
    }

    // --- Audio capture (for backend streaming only) ---

    private void startCapture() {
        int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        // Use at least 4096 samples per read
        bufferSize = Math.max(bufferSize, 4096 * 2);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize");
            notifyOnMainThread(() -> {
                if (listener != null) listener.onError("Microphone not available");
            });
            return;
        }

        capturing = true;
        audioRecord.startRecording();

        captureThread = new Thread(() -> {
            short[] buffer = new short[4096];
            while (capturing) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0 && streamingClient != null) {
                    streamingClient.sendAudio(buffer, read);
                }
            }
        }, "AudioCapture");
        captureThread.start();
    }

    private void stopCapture() {
        capturing = false;
        if (captureThread != null) {
            try {
                captureThread.join(2000);
            } catch (InterruptedException ignored) {
            }
            captureThread = null;
        }
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing AudioRecord", e);
            }
            audioRecord = null;
        }
    }

    private void notifyOnMainThread(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            mainHandler.post(r);
        }
    }
}
