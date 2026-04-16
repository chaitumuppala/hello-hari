package com.hellohari;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Native module that streams live call audio to the hello-hari-recorder backend
 * via WebSocket for real-time scam detection.
 *
 * Protocol (matches hello-hari-recorder /api/ws/stream):
 * 1. Connect to ws://<backend>/api/ws/stream
 * 2. Send JSON config: {"language": "hi"}
 * 3. Stream raw PCM16 audio (16kHz mono 16-bit)
 * 4. Receive JSON results: {type: "transcription", scam_analysis: {...}}
 */
public class ScamAnalysisModule extends ReactContextBaseJavaModule {

    private static final String TAG = "ScamAnalysis";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private final ReactApplicationContext reactContext;
    private final OkHttpClient httpClient;

    private WebSocket webSocket;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private final AtomicBoolean isStreaming = new AtomicBoolean(false);

    // Default to HF Space URL — configurable via setBackendUrl()
    private String backendWsUrl = "wss://chaitumuppala-indian-scam-detector.hf.space/api/ws/stream";

    public ScamAnalysisModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
    }

    @Override
    public String getName() {
        return "ScamAnalysis";
    }

    @ReactMethod
    public void setBackendUrl(String url) {
        // Convert http(s) to ws(s) and append path if needed
        if (url.startsWith("http")) {
            url = url.replaceFirst("^http", "ws");
        }
        if (!url.endsWith("/api/ws/stream")) {
            if (!url.endsWith("/")) url += "/";
            url += "api/ws/stream";
        }
        this.backendWsUrl = url;
        Log.i(TAG, "Backend URL set to: " + url);
    }

    @ReactMethod
    public void startStreaming(String language, Promise promise) {
        if (isStreaming.get()) {
            promise.reject("ALREADY_STREAMING", "Streaming is already active");
            return;
        }

        Log.i(TAG, "Starting scam analysis stream | lang=" + language + " | url=" + backendWsUrl);

        // Connect WebSocket first, then start audio capture
        Request request = new Request.Builder()
                .url(backendWsUrl)
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                Log.i(TAG, "WebSocket connected");

                // Send config message (matches hello-hari-recorder protocol)
                JSONObject config = new JSONObject();
                try {
                    config.put("language", language);
                } catch (JSONException e) {
                    Log.e(TAG, "JSON error", e);
                }
                ws.send(config.toString());

                // Start audio capture
                startAudioCapture();
                isStreaming.set(true);

                sendEvent("scamStreamStatus", createStatusMap("connected", null));
                promise.resolve("Streaming started");
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                try {
                    JSONObject msg = new JSONObject(text);
                    String type = msg.optString("type", "");

                    if ("transcription".equals(type)) {
                        JSONObject analysis = msg.optJSONObject("scam_analysis");
                        if (analysis != null) {
                            WritableMap params = Arguments.createMap();
                            params.putString("type", "transcription");
                            params.putString("text", msg.optString("text", ""));
                            params.putString("language", msg.optString("language", ""));
                            params.putDouble("riskScore", analysis.optDouble("risk_score", 0));
                            params.putBoolean("isScam", analysis.optBoolean("is_scam", false));
                            params.putString("riskLevel", analysis.optString("risk_level", "safe"));

                            // Send matched patterns as JSON string
                            if (analysis.has("matched_patterns")) {
                                params.putString("matchedPatterns",
                                        analysis.getJSONArray("matched_patterns").toString());
                            }
                            // Send category scores as JSON string
                            if (analysis.has("category_scores")) {
                                params.putString("categoryScores",
                                        analysis.getJSONObject("category_scores").toString());
                            }

                            sendEvent("scamAnalysisResult", params);
                        }
                    } else if ("silence".equals(type)) {
                        // No speech detected in this chunk — ignore
                    } else if (msg.has("error")) {
                        sendEvent("scamStreamStatus",
                                createStatusMap("error", msg.optString("error")));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing server response", e);
                }
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure: " + t.getMessage());
                isStreaming.set(false);
                stopAudioCapture();
                sendEvent("scamStreamStatus",
                        createStatusMap("disconnected", t.getMessage()));

                // Only reject if promise hasn't been resolved yet
                try {
                    promise.reject("WS_FAILED", "Connection failed: " + t.getMessage());
                } catch (Exception ignored) {
                    // Promise already resolved
                }
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                Log.i(TAG, "WebSocket closing: " + code + " " + reason);
                ws.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                Log.i(TAG, "WebSocket closed: " + code + " " + reason);
                isStreaming.set(false);
                stopAudioCapture();
                sendEvent("scamStreamStatus", createStatusMap("disconnected", null));
            }
        });
    }

    @ReactMethod
    public void stopStreaming(Promise promise) {
        Log.i(TAG, "Stopping scam analysis stream");
        isStreaming.set(false);
        stopAudioCapture();

        if (webSocket != null) {
            // Send stop signal (matches hello-hari-recorder protocol)
            try {
                webSocket.send("{\"type\":\"stop\"}");
            } catch (Exception e) {
                Log.w(TAG, "Error sending stop signal", e);
            }
            webSocket.close(1000, "User stopped");
            webSocket = null;
        }

        promise.resolve("Streaming stopped");
    }

    @ReactMethod
    public void isStreaming(Promise promise) {
        promise.resolve(isStreaming.get());
    }

    // ---------- Audio capture (records mic at 16kHz PCM16 and streams to WebSocket) ----------

    private void startAudioCapture() {
        int bufferSize = Math.max(
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING),
                SAMPLE_RATE * 2  // At least 1 second buffer
        );

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL,
                ENCODING,
                bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize");
            sendEvent("scamStreamStatus",
                    createStatusMap("error", "Microphone unavailable"));
            return;
        }

        audioRecord.startRecording();

        recordingThread = new Thread(() -> {
            // Send ~0.5s chunks of PCM data continuously
            // Backend buffers these into 5s chunks for ASR
            int chunkSize = SAMPLE_RATE;  // 0.5s of 16-bit samples = 16000 bytes
            byte[] buffer = new byte[chunkSize];

            while (isStreaming.get()) {
                int bytesRead = audioRecord.read(buffer, 0, chunkSize);
                if (bytesRead > 0 && webSocket != null) {
                    ByteString data = ByteString.of(buffer, 0, bytesRead);
                    try {
                        webSocket.send(data);
                    } catch (Exception e) {
                        Log.w(TAG, "Error sending audio data", e);
                        break;
                    }
                }
            }
        }, "ScamAnalysis-AudioThread");
        recordingThread.start();
    }

    private void stopAudioCapture() {
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.w(TAG, "Error stopping AudioRecord", e);
            }
            audioRecord = null;
        }
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }
    }

    // ---------- Helpers ----------

    private WritableMap createStatusMap(String status, String error) {
        WritableMap map = Arguments.createMap();
        map.putString("status", status);
        if (error != null) {
            map.putString("error", error);
        }
        return map;
    }

    private void sendEvent(String eventName, WritableMap params) {
        try {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } catch (Exception e) {
            Log.w(TAG, "Cannot send event " + eventName, e);
        }
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        isStreaming.set(false);
        stopAudioCapture();
        if (webSocket != null) {
            webSocket.close(1000, "App closing");
            webSocket = null;
        }
    }
}
