package com.hellohari;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Streams live call audio to the hello-hari-recorder backend
 * (https://github.com/chaitumuppala/hello-hari-recorder) via WebSocket
 * for real-time multilingual scam detection.
 *
 * Backend protocol (/api/ws/stream):
 * 1. Client connects
 * 2. Client sends JSON config: {"language": "hi"}
 * 3. Client streams raw PCM16 audio (16kHz mono 16-bit)
 * 4. Server returns JSON: {type: "transcription", text, scam_analysis: {...}}
 *
 * The backend does ASR (IndicConformer for Indian langs, faster-whisper for English)
 * plus 574 pattern + 13 archetype scam detection on each 5-second window.
 */
public class RealTimeScamAnalyzer {

    private static final String TAG = "RealTimeScamAnalyzer";

    // Default backend URL — the user's HuggingFace Space
    public static final String DEFAULT_BACKEND_URL =
            "wss://chaitumuppala-indian-scam-detector.hf.space/api/ws/stream";

    // Audio capture settings — must match backend sample_rate in config.py
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    public interface AnalysisListener {
        /** Called on stream connect/disconnect/error. */
        void onStatusChanged(String status, String error);

        /** Called for each chunk of server-side analysis results. */
        void onAnalysisResult(AnalysisResult result);
    }

    public static class AnalysisResult {
        public final String transcript;
        public final String language;
        public final double riskScore;          // 0.0 – 1.0
        public final boolean isScam;
        public final String riskLevel;          // "safe" | "low" | "medium" | "high"
        public final String[] matchedPatterns;

        public AnalysisResult(String transcript, String language, double riskScore,
                              boolean isScam, String riskLevel, String[] matchedPatterns) {
            this.transcript = transcript;
            this.language = language;
            this.riskScore = riskScore;
            this.isScam = isScam;
            this.riskLevel = riskLevel;
            this.matchedPatterns = matchedPatterns;
        }

        public int getRiskPercent() {
            return (int) Math.round(riskScore * 100);
        }
    }

    private final Context context;
    private final OkHttpClient httpClient;
    private final String backendUrl;
    private AnalysisListener listener;

    private WebSocket webSocket;
    private AudioRecord audioRecord;
    private Thread captureThread;
    private final AtomicBoolean isStreaming = new AtomicBoolean(false);

    public RealTimeScamAnalyzer(Context context) {
        this(context, DEFAULT_BACKEND_URL);
    }

    public RealTimeScamAnalyzer(Context context, String backendUrl) {
        this.context = context.getApplicationContext();
        this.backendUrl = normalizeUrl(backendUrl);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)  // no read timeout for streaming
                .pingInterval(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void setListener(AnalysisListener listener) {
        this.listener = listener;
    }

    public boolean isStreaming() {
        return isStreaming.get();
    }

    /**
     * Connect to the backend and start streaming microphone audio.
     * @param language ISO 639-1 code ("hi", "te", "ta", "en", etc.)
     */
    public void start(String language) {
        if (isStreaming.get()) {
            Log.w(TAG, "Already streaming");
            return;
        }
        Log.i(TAG, "Starting scam analysis | lang=" + language + " | url=" + backendUrl);

        Request request = new Request.Builder().url(backendUrl).build();
        webSocket = httpClient.newWebSocket(request, new WsListener(language));
    }

    /**
     * Stop streaming and close the WebSocket.
     */
    public void stop() {
        Log.i(TAG, "Stopping scam analysis");
        isStreaming.set(false);
        stopCapture();
        if (webSocket != null) {
            try {
                webSocket.send("{\"type\":\"stop\"}");
            } catch (Exception ignored) {
            }
            webSocket.close(1000, "Client stop");
            webSocket = null;
        }
    }

    // ------------------------------------------------------------------ WebSocket

    private class WsListener extends WebSocketListener {
        private final String language;

        WsListener(String language) {
            this.language = language;
        }

        @Override
        public void onOpen(WebSocket ws, Response response) {
            Log.i(TAG, "WebSocket connected");
            // Send config
            JSONObject config = new JSONObject();
            try {
                config.put("language", language);
            } catch (JSONException e) {
                Log.w(TAG, "Config JSON error", e);
            }
            ws.send(config.toString());

            // Start audio capture
            startCapture();
            isStreaming.set(true);
            notifyStatus("connected", null);
        }

        @Override
        public void onMessage(WebSocket ws, String text) {
            try {
                JSONObject msg = new JSONObject(text);
                String type = msg.optString("type", "");

                if ("transcription".equals(type)) {
                    JSONObject analysis = msg.optJSONObject("scam_analysis");
                    if (analysis != null) {
                        String[] patterns = parseStringArray(analysis.optJSONArray("matched_patterns"));
                        AnalysisResult result = new AnalysisResult(
                                msg.optString("text", ""),
                                msg.optString("language", language),
                                analysis.optDouble("risk_score", 0),
                                analysis.optBoolean("is_scam", false),
                                analysis.optString("risk_level", "safe"),
                                patterns
                        );
                        if (listener != null) {
                            listener.onAnalysisResult(result);
                        }
                    }
                } else if (msg.has("error")) {
                    notifyStatus("error", msg.optString("error"));
                }
                // "silence" type — ignored
            } catch (JSONException e) {
                Log.w(TAG, "Failed to parse server message", e);
            }
        }

        @Override
        public void onFailure(WebSocket ws, Throwable t, Response response) {
            Log.e(TAG, "WebSocket failure: " + t.getMessage(), t);
            isStreaming.set(false);
            stopCapture();
            notifyStatus("error", t.getMessage());
        }

        @Override
        public void onClosing(WebSocket ws, int code, String reason) {
            ws.close(1000, null);
        }

        @Override
        public void onClosed(WebSocket ws, int code, String reason) {
            Log.i(TAG, "WebSocket closed: " + code + " " + reason);
            isStreaming.set(false);
            stopCapture();
            notifyStatus("disconnected", null);
        }
    }

    // ------------------------------------------------------------------ Audio capture

    private void startCapture() {
        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING);
        int bufferSize = Math.max(minBuffer, SAMPLE_RATE * 2); // ≥1s buffer

        // Prefer VOICE_COMMUNICATION for call audio; fall back to VOICE_RECOGNITION.
        audioRecord = tryCreateRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, bufferSize);
        if (audioRecord == null) {
            audioRecord = tryCreateRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, bufferSize);
        }
        if (audioRecord == null) {
            Log.e(TAG, "Could not initialize AudioRecord");
            notifyStatus("error", "Microphone unavailable");
            return;
        }

        audioRecord.startRecording();

        captureThread = new Thread(() -> {
            // Stream ~0.5s chunks; backend buffers these into 5s ASR windows.
            final int chunkBytes = SAMPLE_RATE;  // 0.5s of 16-bit samples
            byte[] buffer = new byte[chunkBytes];

            while (isStreaming.get()) {
                int read = audioRecord.read(buffer, 0, chunkBytes);
                if (read > 0 && webSocket != null) {
                    try {
                        webSocket.send(ByteString.of(buffer, 0, read));
                    } catch (Exception e) {
                        Log.w(TAG, "Audio send failed, stopping stream", e);
                        break;
                    }
                } else if (read < 0) {
                    Log.w(TAG, "AudioRecord.read error: " + read);
                    break;
                }
            }
        }, "ScamAnalyzer-Audio");
        captureThread.start();
    }

    private AudioRecord tryCreateRecord(int source, int bufferSize) {
        try {
            AudioRecord r = new AudioRecord(source, SAMPLE_RATE, CHANNEL, ENCODING, bufferSize);
            if (r.getState() == AudioRecord.STATE_INITIALIZED) {
                Log.i(TAG, "AudioRecord initialized with source=" + source);
                return r;
            }
            r.release();
        } catch (Exception e) {
            Log.w(TAG, "Failed to create AudioRecord with source=" + source, e);
        }
        return null;
    }

    private void stopCapture() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.w(TAG, "Error stopping AudioRecord", e);
            }
            audioRecord = null;
        }
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
    }

    // ------------------------------------------------------------------ Helpers

    private void notifyStatus(String status, String error) {
        if (listener != null) {
            listener.onStatusChanged(status, error);
        }
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) return DEFAULT_BACKEND_URL;
        if (url.startsWith("http://")) url = "ws://" + url.substring(7);
        else if (url.startsWith("https://")) url = "wss://" + url.substring(8);
        if (!url.endsWith("/api/ws/stream")) {
            if (!url.endsWith("/")) url += "/";
            url += "api/ws/stream";
        }
        return url;
    }

    private static String[] parseStringArray(JSONArray arr) {
        if (arr == null) return new String[0];
        String[] out = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            out[i] = arr.optString(i, "");
        }
        return out;
    }
}
