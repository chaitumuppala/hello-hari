package com.hellohari;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoskSpeechRecognizer {
    private static final String TAG = "VoskSpeechRecognizer";
    private static final int SAMPLE_RATE = 16000;

    private final Context context;
    private final Map<String, Model> models = new HashMap<>();
    private Recognizer recognizer;
    private String currentLanguage = "en";
    private boolean initialized = false;
    private VoskRecognitionListener listener;
    private Object speechService = null;
    private Object speechStreamService = null;
    private Model model = null;
    private ExecutorService modelExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Interface for VOSK speech recognition callbacks
     */
    public interface VoskRecognitionListener {
        void onPartialResult(String partialText, String language);
        void onFinalResult(String finalText, String language, float confidence);
        void onError(String error);
        void onInitializationComplete(boolean success);
        void onModelDownloadProgress(String language, int progress);
        void onModelDownloadComplete(String language, boolean success);
    }

    /**
     * Constructor for VoskSpeechRecognizer
     * @param context Application context
     */
    public VoskSpeechRecognizer(Context context) {
        this.context = context;
        initVosk();
    }

    /**
     * Initialize the VOSK library
     */
    private void initVosk() {
        try {
            LibVosk.setLogLevel(LogLevel.INFO);
            initialized = loadModel("en"); // Load English model by default
            if (initialized) {
                setLanguage("en");
                Log.d(TAG, "VOSK initialized successfully with English model");
            } else {
                Log.e(TAG, "Failed to initialize VOSK");
                initialize(); // Try asynchronous initialization
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VOSK: " + e.getMessage(), e);
            initialized = false;
        }
    }

    /**
     * Set the recognition listener
     * @param listener The listener to set
     */
    public void setListener(VoskRecognitionListener listener) {
        this.listener = listener;
    }

    /**
     * Check if VOSK is initialized
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Load a language model
     * @param language Language code to load
     * @return true if loaded successfully
     */
    private boolean loadModel(String language) {
        try {
            if (models.containsKey(language)) {
                return true;
            }

            String modelPath = context.getExternalFilesDir(null).getPath() + "/vosk-model-" + language;
            File modelDir = new File(modelPath);
            
            if (!modelDir.exists()) {
                Log.w(TAG, "Model for " + language + " not found at " + modelPath);
                return false;
            }

            Model model = new Model(modelPath);
            models.put(language, model);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error loading model for " + language + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if model for language exists
     * @param language Language code to check
     * @return true if model exists
     */
    private boolean hasModelForLanguage(String language) {
        String modelPath = context.getExternalFilesDir(null).getPath() + "/vosk-model-" + language;
        File modelDir = new File(modelPath);
        return modelDir.exists() && modelDir.isDirectory();
    }

    /**
     * Set the current language
     * @param language Language code
     */
    public void setLanguage(String language) {
        if (!models.containsKey(language)) {
            if (!loadModel(language)) {
                if (listener != null) {
                    listener.onError("Model for " + language + " not available");
                }
                return;
            }
        }

        currentLanguage = language;
        try {
            if (recognizer != null) {
                recognizer.close();
            }
            recognizer = new Recognizer(models.get(language), SAMPLE_RATE);
            Log.d(TAG, "Set language to: " + language);
        } catch (Exception e) {
            Log.e(TAG, "Error setting language to " + language + ": " + e.getMessage(), e);
        }
    }

    /**
     * Recognize speech from a file
     * @param inputStream Input stream of audio file
     */
    public void recognizeFile(InputStream inputStream) {
        if (!initialized || recognizer == null) {
            if (listener != null) {
                listener.onError("Recognizer not initialized");
            }
            return;
        }

        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int nbytes;
                
                recognizer.startUtterance();
                
                while ((nbytes = inputStream.read(buffer)) >= 0) {
                    if (recognizer.acceptWaveForm(buffer, nbytes)) {
                        String result = recognizer.getResult();
                        processResult(result, true);
                    } else {
                        String partialResult = recognizer.getPartialResult();
                        processResult(partialResult, false);
                    }
                }
                
                recognizer.endUtterance();
                String finalResult = recognizer.getFinalResult();
                processResult(finalResult, true);
                
                inputStream.close();
                
            } catch (Exception e) {
                Log.e(TAG, "Error during recognition: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError("Recognition error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Process VOSK result JSON
     * @param resultJson JSON string from VOSK
     * @param isFinal Whether this is a final result
     */
    private void processResult(String resultJson, boolean isFinal) {
        try {
            JSONObject json = new JSONObject(resultJson);
            
            if (isFinal && json.has("text")) {
                String text = json.getString("text").trim();
                float conf = json.has("confidence") ? (float)json.getDouble("confidence") : 0.0f;
                
                if (listener != null && !text.isEmpty()) {
                    listener.onFinalResult(text, currentLanguage, conf);
                }
            } else if (!isFinal && json.has("partial")) {
                String partial = json.getString("partial").trim();
                
                if (listener != null && !partial.isEmpty()) {
                    listener.onPartialResult(partial, currentLanguage);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing result JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }
        
        for (Model model : models.values()) {
            model.close();
        }
        models.clear();
        
        initialized = false;
    }
    
    /**
     * Cancel any active recognition task
     */
    public void cancel() {
        if (listener != null) {
            listener.onError("Recognition cancelled by user");
        }
    }
    
    /**
     * Stop active listening
     */
    public void stopListening() {
        if (speechService != null) {
            speechService = null;
            Log.i(TAG, "Stopped listening");
        }
        
        if (speechStreamService != null) {
            speechStreamService = null;
            Log.i(TAG, "Stopped file recognition");
        }
    }
    
    /**
     * Release all resources
     */
    public void release() {
        stopListening();
        closeModel();
    }
    
    /**
     * Close model resources
     */
    private void closeModel() {
        if (model != null) {
            model = null;
        }
    }
    
    /**
     * Initialize VOSK asynchronously
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        modelExecutor.execute(() -> {
            try {
                // Try to download the model if necessary
                if (!hasModelForLanguage("en")) {
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onModelDownloadProgress("en", 0);
                        }
                    });
                    
                    // In a real implementation, this would actually download the model
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onModelDownloadComplete("en", false);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing models", e);
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onInitializationComplete(false);
                    }
                });
            }
        });
    }
    
    /**
     * Get available languages with models
     * @return Array of language codes
     */
    public String[] getAvailableLanguages() {
        List<String> availableLanguages = new ArrayList<>();
        
        if (hasModelForLanguage("en")) availableLanguages.add("en");
        if (hasModelForLanguage("hi")) availableLanguages.add("hi");
        if (hasModelForLanguage("te")) availableLanguages.add("te");
        
        return availableLanguages.toArray(new String[0]);
    }
    
    /**
     * Get current language
     * @return Current language code
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Get the required download size for missing models
     * @return Size in MB as string
     */
    public String getRequiredDownloadSize() {
        int totalSize = 0;
        
        if (!hasModelForLanguage("en")) totalSize += 40;
        if (!hasModelForLanguage("hi")) totalSize += 50;
        if (!hasModelForLanguage("te")) totalSize += 42;
        
        if (totalSize == 0) return "0 MB";
        return totalSize + " MB";
    }
    
    /**
     * Recognize multiple languages in sequence
     * @param filePath Path to audio file
     */
    public void recognizeMultiLanguage(String filePath) {
        try {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: " + filePath);
                if (listener != null) {
                    listener.onError("Audio file not found: " + filePath);
                }
                return;
            }
            
            // First try with default language
            FileInputStream inputStream = new FileInputStream(audioFile);
            recognizeFile(inputStream);
            
            // Check if other languages are available
            String[] languages = getAvailableLanguages();
            if (languages.length > 1) {
                // In a full implementation, we would try other languages
                // after analyzing the first language results
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error opening audio file: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("Error opening audio file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle partial result callbacks (for compatibility with Vosk SDK)
     */
    public void onPartialResult(String hypothesis) {
        try {
            Log.d(TAG, "Partial result: " + hypothesis);
            JSONObject json = new JSONObject(hypothesis);
            if (json.has("partial")) {
                String text = json.getString("partial").trim();
                if (!text.isEmpty() && listener != null) {
                    listener.onPartialResult(text, currentLanguage);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing partial result", e);
        }
    }
    
    /**
     * Handle final result callbacks (for compatibility with Vosk SDK)
     */
    public void onResult(String hypothesis) {
        try {
            Log.d(TAG, "Final result: " + hypothesis);
            JSONObject json = new JSONObject(hypothesis);
            if (json.has("text")) {
                String text = json.getString("text").trim();
                float confidence = json.has("confidence") ? 
                    (float) json.getDouble("confidence") : 0.0f;
                if (!text.isEmpty() && listener != null) {
                    listener.onFinalResult(text, currentLanguage, confidence);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing final result", e);
        }
    }
    
    /**
     * Handle error callbacks (for compatibility with Vosk SDK)
     */
    public void onError(Exception exception) {
        Log.e(TAG, "Recognition error", exception);
        if (listener != null) {
            listener.onError(exception.getMessage());
        }
    }
    
    /**
     * Handle timeout callbacks (for compatibility with Vosk SDK)
     */
    public void onTimeout() {
        // Not used in our implementation
    }
}
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing partial result", e);
        }
    }
    
    @Override
    public void onResult(String hypothesis) {
        try {
            if (hypothesis == null || hypothesis.isEmpty()) return;
            
            JSONObject json = new JSONObject(hypothesis);
            if (json.has("text")) {
                String text = json.getString("text").trim();
                float confidence = json.has("confidence") ? 
                    (float)json.getDouble("confidence") : 0.8f;
                    
                if (!text.isEmpty() && listener != null) {
                    listener.onFinalResult(text, currentLanguage, confidence);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing result", e);
        }
    }
    
    @Override
    public void onError(Exception exception) {
        Log.e(TAG, "Recognition error", exception);
        if (listener != null) {
            listener.onError(exception.getMessage());
        }
    }
    
    @Override
    public void onTimeout() {
        Log.i(TAG, "Recognition timeout");
    }
}
