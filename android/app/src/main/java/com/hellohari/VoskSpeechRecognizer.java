package com.hellohari;

import android.content.Context;
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
import java.util.HashMap;
import java.util.Map;

public class VoskSpeechRecognizer {
    private static final String TAG = "VoskSpeechRecognizer";
    private static final int SAMPLE_RATE = 16000;

    private final Context context;
    private final Map<String, Model> models = new HashMap<>();
    private Recognizer recognizer;
    private String currentLanguage;
    private boolean initialized = false;
    private VoskRecognitionListener listener;

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
                downloadModel(language);
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
     * Download a language model
     * @param language Language code to download
     */
    private void downloadModel(String language) {
        Log.d(TAG, "Initiating download of model for language: " + language);
        // This would connect to a server to download models
        if (listener != null) {
            listener.onModelDownloadProgress(language, 0);
            // In a real implementation, update progress periodically
            listener.onModelDownloadComplete(language, false);
        }
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
     * Recognize multiple languages in sequence
     * @param audioPath Path to audio file
     */
    public void recognizeMultiLanguage(String audioPath) {
        // Try English first, then other languages if confidence is low
        try {
            File audioFile = new File(audioPath);
            if (!audioFile.exists()) {
                if (listener != null) {
                    listener.onError("Audio file not found: " + audioPath);
                }
                return;
            }
            
            setLanguage("en");
            recognizeFile(new FileInputStream(audioFile));
            
            // Additional languages would be handled in a real implementation
        } catch (IOException e) {
            Log.e(TAG, "Error opening audio file: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("Error opening audio file: " + e.getMessage());
            }
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
}
        for (Model model : models.values()) {
            model.close();
        }
        models.clear();
        
        initialized = false;
    }
}
        } catch (IOException e) {
            Log.e(TAG, "Failed to start file recognition", e);
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        }
    }
    
    /**
     * Stop recognition
     */
    public void stopListening() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
            Log.i(TAG, "Stopped listening");
        }
        
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
            Log.i(TAG, "Stopped file recognition");
        }
    }
    
    /**
     * Release resources
     */
    public void release() {
        stopListening();
        closeModel();
    }
    
    /**
     * Close the model and free resources
     */
    private void closeModel() {
        if (model != null) {
            model.close();
            model = null;
        }
    }
    
    /**
     * Initialize VOSK and prepare all language models
     * This will trigger downloads if models are not present
     */
    public void initialize() {
        // Initialize English model first
        initModel("en");
        
        // Check and initialize other languages if needed
        modelExecutor.execute(() -> {
            try {
                // Check Hindi model
                if (!hasModelForLanguage("hi")) {
                    downloadModel("hi");
                }
                
                // Check Telugu model
                if (!hasModelForLanguage("te")) {
                    downloadModel("te");
                }
                
                // Notify listener that initialization is complete
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onInitializationComplete(true);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during initialization", e);
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onInitializationComplete(false);
                    }
                });
            }
        });
    }
    
    /**
     * Get available language models that have been downloaded
     * @return Array of language codes
     */
    public String[] getAvailableLanguages() {
        ArrayList<String> availableLanguages = new ArrayList<>();
        
        if (hasModelForLanguage("en")) availableLanguages.add("en");
        if (hasModelForLanguage("hi")) availableLanguages.add("hi");
        if (hasModelForLanguage("te")) availableLanguages.add("te");
        
        return availableLanguages.toArray(new String[0]);
    }
    
    /**
     * Get the current language being used for recognition
     * @return Current language code
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Calculate required download size for all missing models
     * @return Human-readable size string (e.g., "132 MB")
     */
    public String getRequiredDownloadSize() {
        // Approximate sizes for each model
        int totalSize = 0;
        
        if (!hasModelForLanguage("en")) totalSize += 40;
        if (!hasModelForLanguage("hi")) totalSize += 50;
        if (!hasModelForLanguage("te")) totalSize += 42;
        
        if (totalSize == 0) return "0 MB";
        return totalSize + " MB";
    }
    
    /**
     * Recognize speech in a file using multiple languages
     * @param filePath Path to audio file
     */
    public void recognizeMultiLanguage(String filePath) {
        try {
            // Create input stream from file
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file does not exist: " + filePath);
                if (listener != null) {
                    listener.onError("Audio file not found: " + filePath);
                }
                return;
            }
            
            // Start with default language
            InputStream inputStream = new FileInputStream(audioFile);
            recognizeFile(inputStream);
            
            // Try recognition in other languages if available
            String[] languages = getAvailableLanguages();
            if (languages.length > 1) {
                // Schedule recognition in other languages after the first one completes
                // This would be implemented based on your specific requirements
                Log.d(TAG, "Will attempt recognition in multiple languages: " + Arrays.toString(languages));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in multi-language recognition", e);
            if (listener != null) {
                listener.onError("Error processing audio file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Clean up resources properly
     */
    public void cleanup() {
        stopListening();
        release();
    }
    
    // RecognitionListener implementation
    
    @Override
    public void onPartialResult(String hypothesis) {
        try {
            if (hypothesis == null || hypothesis.isEmpty()) return;
            
            JSONObject json = new JSONObject(hypothesis);
            if (json.has("partial")) {
                String text = json.getString("partial").trim();
                if (!text.isEmpty() && listener != null) {
                    listener.onPartialResult(text, currentLanguage);
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
