package com.hellohari;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * WebSocket client that streams PCM audio to the hello-hari-recorder backend
 * and receives real-time transcription + scam analysis results.
 *
 * Protocol (mirrors frontend/src/hooks/useAudioRecorder.ts):
 * 1. Connect to ws://{host}/api/ws/stream
 * 2. Send JSON config: {"language": "hi"}
 * 3. Send binary PCM int16 audio frames
 * 4. Receive JSON: {type: "transcription", text, scam_analysis, ...}
 * 5. Send JSON {"type": "stop"} to end; server drains queue and closes.
 */
public final class StreamingAsrClient {

    private static final String TAG = "StreamingASR";

    public interface Listener {
        /** Called when the WebSocket connection is established. */
        void onConnected();

        /** Called for each transcription result from the backend. */
        void onTranscription(String text, String language, boolean isScam,
                             double riskScore, String explanation);

        /** Called when the backend reports silence (no speech detected). */
        void onSilence();

        /** Called when the session ends (server closed after drain). */
        void onSessionEnd(int totalChunks);

        /** Called on any error (network, parse, etc.). */
        void onError(String message);
    }

    private final OkHttpClient httpClient;
    private WebSocket webSocket;
    private Listener listener;
    private volatile boolean connected = false;

    public StreamingAsrClient() {
        this.httpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)   // no read timeout for streaming
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Connect to the backend WebSocket and send the language config.
     *
     * @param serverUrl base URL, e.g. "ws://192.168.1.100:8000"
     * @param language  ISO 639-1 language code, e.g. "hi", "te", "en"
     */
    public void connect(String serverUrl, String language) {
        if (connected) {
            Log.w(TAG, "Already connected");
            return;
        }

        String wsUrl = serverUrl.replaceFirst("^http", "ws") + "/api/ws/stream";
        Log.i(TAG, "Connecting to " + wsUrl);

        Request request = new Request.Builder().url(wsUrl).build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                connected = true;
                Log.i(TAG, "WebSocket connected");

                // Send language config (same as frontend)
                JSONObject config = new JSONObject();
                try {
                    config.put("language", language);
                } catch (JSONException e) {
                    // impossible
                }
                ws.send(config.toString());

                if (listener != null) listener.onConnected();
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                try {
                    JSONObject msg = new JSONObject(text);
                    String type = msg.optString("type", "");

                    switch (type) {
                        case "transcription": {
                            String transcript = msg.optString("text", "");
                            String lang = msg.optString("language", "");
                            JSONObject analysis = msg.optJSONObject("scam_analysis");
                            boolean isScam = analysis != null && analysis.optBoolean("is_scam", false);
                            double score = analysis != null ? analysis.optDouble("risk_score", 0) : 0;
                            String explanation = analysis != null ? analysis.optString("explanation", "") : "";

                            if (listener != null) {
                                listener.onTranscription(transcript, lang, isScam, score, explanation);
                            }
                            break;
                        }
                        case "silence":
                            if (listener != null) listener.onSilence();
                            break;

                        case "done": {
                            int total = msg.optInt("total_chunks", 0);
                            if (listener != null) listener.onSessionEnd(total);
                            break;
                        }
                        case "error": {
                            // Legacy: server sends {"error": "..."} on 503
                            // before newer {"type": "error", ...}
                            break;
                        }
                        default:
                            // Check for top-level "error" key (engine not ready)
                            if (msg.has("error")) {
                                String err = msg.optString("error", "Unknown error");
                                if (listener != null) listener.onError(err);
                            }
                            break;
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "Failed to parse server message: " + text, e);
                }
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                connected = false;
                Log.e(TAG, "WebSocket failure", t);
                if (listener != null) listener.onError("Connection failed: " + t.getMessage());
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                connected = false;
                Log.i(TAG, "WebSocket closed: " + code + " " + reason);
            }
        });
    }

    /**
     * Send a chunk of PCM int16 audio data to the backend.
     * Call this from AudioRecord's read loop.
     *
     * @param pcmInt16 raw PCM int16 samples (16 kHz mono)
     * @param length   number of samples to send
     */
    public void sendAudio(short[] pcmInt16, int length) {
        if (!connected || webSocket == null) return;

        // Convert short[] to little-endian byte[] (matches frontend's Int16Array.buffer)
        ByteBuffer buf = ByteBuffer.allocate(length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < length; i++) {
            buf.putShort(pcmInt16[i]);
        }
        webSocket.send(ByteString.of(buf.array()));
    }

    /**
     * Signal the backend to stop, drain remaining queued chunks, and close.
     * After calling this, do NOT send more audio.
     */
    public void stop() {
        if (webSocket != null) {
            try {
                JSONObject stop = new JSONObject();
                stop.put("type", "stop");
                webSocket.send(stop.toString());
            } catch (JSONException ignored) {
            }
            // Don't close — server will close after draining
        }
    }

    /** Hard disconnect (e.g. on Activity destroy). */
    public void disconnect() {
        connected = false;
        if (webSocket != null) {
            webSocket.cancel();
            webSocket = null;
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
