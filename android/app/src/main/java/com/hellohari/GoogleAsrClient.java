package com.hellohari;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * Fallback ASR using Android's built-in SpeechRecognizer (Google Cloud).
 *
 * Supports all major Indian languages. Used when the hello-hari-recorder
 * backend is unreachable. Requires internet (Google servers).
 *
 * Must be created and used on the main thread.
 */
public final class GoogleAsrClient {

    private static final String TAG = "GoogleASR";

    public interface Listener {
        void onPartialResult(String text);
        void onFinalResult(String text, float confidence);
        void onError(String message);
        void onReady();
        void onEnd();
    }

    private final Context context;
    private SpeechRecognizer recognizer;
    private Listener listener;
    private boolean listening = false;

    /** Map our ISO 639-1 codes to Android BCP-47 locale tags. */
    private static String toAndroidLocale(String lang) {
        switch (lang) {
            case "hi": return "hi-IN";
            case "te": return "te-IN";
            case "ta": return "ta-IN";
            case "bn": return "bn-IN";
            case "mr": return "mr-IN";
            case "gu": return "gu-IN";
            case "kn": return "kn-IN";
            case "ml": return "ml-IN";
            case "pa": return "pa-IN";
            case "or": return "or-IN";
            case "en": default: return "en-IN";
        }
    }

    public GoogleAsrClient(Context context) {
        this.context = context;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean isAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    /**
     * Start continuous listening. Google SpeechRecognizer will auto-stop
     * after a silence, so we restart it in onEndOfSpeech for continuous
     * monitoring.
     */
    public void start(String language, boolean continuous) {
        if (listening) return;

        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new InternalListener(language, continuous));

        Intent intent = createRecognizerIntent(language);
        recognizer.startListening(intent);
        listening = true;
        Log.i(TAG, "Started listening in " + language);
    }

    public void stop() {
        listening = false;
        if (recognizer != null) {
            try {
                recognizer.stopListening();
                recognizer.destroy();
            } catch (Exception e) {
                Log.w(TAG, "Error stopping recognizer", e);
            }
            recognizer = null;
        }
        Log.i(TAG, "Stopped");
    }

    public boolean isListening() {
        return listening;
    }

    private Intent createRecognizerIntent(String language) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, toAndroidLocale(language));
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        // Allow longer pauses before end-of-speech
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L);
        return intent;
    }

    private class InternalListener implements RecognitionListener {
        private final String language;
        private final boolean continuous;

        InternalListener(String language, boolean continuous) {
            this.language = language;
            this.continuous = continuous;
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            if (listener != null) listener.onReady();
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> texts = partialResults
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (texts != null && !texts.isEmpty() && listener != null) {
                listener.onPartialResult(texts.get(0));
            }
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> texts = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

            if (texts != null && !texts.isEmpty() && listener != null) {
                float confidence = (scores != null && scores.length > 0) ? scores[0] : 0.8f;
                listener.onFinalResult(texts.get(0), confidence);
            }

            // Restart for continuous monitoring
            if (continuous && listening && recognizer != null) {
                recognizer.startListening(createRecognizerIntent(language));
            }
        }

        @Override
        public void onError(int error) {
            String msg;
            switch (error) {
                case SpeechRecognizer.ERROR_NETWORK:
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    msg = "Network error"; break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    msg = "No speech detected"; break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    msg = "Speech timeout"; break;
                case SpeechRecognizer.ERROR_AUDIO:
                    msg = "Audio recording error"; break;
                default:
                    msg = "Recognition error (" + error + ")"; break;
            }

            // No-match and timeout are not fatal — restart
            boolean canRestart = (error == SpeechRecognizer.ERROR_NO_MATCH
                    || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT);

            if (canRestart && continuous && listening && recognizer != null) {
                Log.d(TAG, msg + " — restarting");
                recognizer.startListening(createRecognizerIntent(language));
            } else {
                Log.w(TAG, msg);
                if (listener != null) listener.onError(msg);
            }
        }

        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() {
            if (listener != null) listener.onEnd();
        }
        @Override public void onEvent(int eventType, Bundle params) { }
    }
}
