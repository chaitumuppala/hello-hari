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
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * VoskSpeechRecognizer - Handles speech recognition with VOSK
 * Supports multi-language models and automatic model downloads
 */
public class VoskSpeechRecognizer implements RecognitionListener {
    private static final String TAG = "VoskSpeechRecognizer";
    
    // Language model URLs and file paths
    private static final String MODEL_EN_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip";
    private static final String MODEL_HI_URL = "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip";
    private static final String MODEL_TE_URL = "https://alphacephei.com/vosk/models/vosk-model-small-te-0.3.zip";
    
    private static final String MODEL_EN_PATH = "model-en";
    private static final String MODEL_HI_PATH = "model-hi";
    private static final String MODEL_TE_PATH = "model-te";
    
    // Current language setting
    private String currentLanguage = "en"; // Default to English
    
    // VOSK components
    private final WeakReference<Context> contextRef;
    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private final Executor modelExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Callback interface
    private VoskRecognitionListener listener;
    
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
        this.contextRef = new WeakReference<>(context);
        // Set log level
        LibVosk.setLogLevel(LogLevel.INFO);
    }
    
    /**
     * Set the recognition listener for callbacks
     * @param listener VoskRecognitionListener implementation
     */
    public void setListener(VoskRecognitionListener listener) {
        this.listener = listener;
    }
    
    /**
     * Set the language for recognition
     * @param language Language code ("en", "hi", or "te")
     */
    public void setLanguage(String language) {
        if (!language.equals(currentLanguage)) {
            currentLanguage = language;
            // Close existing model if any
            closeModel();
            // Initialize model for new language
            initModel(language);
        }
    }
    
    /**
     * Check if model for specified language exists
     * @param language Language code
     * @return True if model exists
     */
    public boolean hasModelForLanguage(String language) {
        Context context = contextRef.get();
        if (context == null) return false;
        
        String modelPath = getModelPathForLanguage(language);
        File modelDir = new File(context.getExternalFilesDir(null), modelPath);
        return modelDir.exists() && new File(modelDir, "am/final.mdl").exists();
    }
    
    /**
     * Initialize the model for the specified language
     * @param language Language code
     */
    public void initModel(String language) {
        Context context = contextRef.get();
        if (context == null) {
            Log.e(TAG, "Context is null, cannot initialize model");
            return;
        }
        
        modelExecutor.execute(() -> {
            try {
                String modelPath = getModelPathForLanguage(language);
                
                // Check if model exists
                if (!hasModelForLanguage(language)) {
                    // Download model
                    downloadModel(language);
                    return; // Will be called again after download
                }
                
                // Load model
                File modelDir = new File(context.getExternalFilesDir(null), modelPath);
                model = new Model(modelDir.getAbsolutePath());
                
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onModelDownloadComplete(language, true);
                    }
                });
                
                Log.i(TAG, "Loaded model for " + language);
                
            } catch (final Exception e) {
                Log.e(TAG, "Failed to init model for " + language, e);
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onModelDownloadComplete(language, false);
                    }
                });
            }
        });
    }
    
    /**
     * Download model for the specified language
     * @param language Language code
     */
    private void downloadModel(String language) {
        Context context = contextRef.get();
        if (context == null) {
            Log.e(TAG, "Context is null, cannot download model");
            return;
        }
        
        String modelUrl = getModelUrlForLanguage(language);
        String modelPath = getModelPathForLanguage(language);
        
        try {
            StorageService.unpack(context, modelUrl, modelPath, 
                (nBytes, totalBytes) -> {
                    final int percent = (int) (nBytes * 100 / totalBytes);
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onModelDownloadProgress(language, percent);
                        }
                    });
                }, 
                model -> {
                    this.model = model;
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onModelDownloadComplete(language, true);
                        }
                    });
                });
        } catch (final IOException e) {
            Log.e(TAG, "Failed to download model for " + language, e);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onModelDownloadComplete(language, false);
                }
            });
        }
    }
    
    /**
     * Get the model path for the specified language
     * @param language Language code
     * @return Model path
     */
    private String getModelPathForLanguage(String language) {
        switch (language.toLowerCase()) {
            case "hi": return MODEL_HI_PATH;
            case "te": return MODEL_TE_PATH;
            default: return MODEL_EN_PATH;
        }
    }
    
    /**
     * Get the model URL for the specified language
     * @param language Language code
     * @return Model URL
     */
    private String getModelUrlForLanguage(String language) {
        switch (language.toLowerCase()) {
            case "hi": return MODEL_HI_URL;
            case "te": return MODEL_TE_URL;
            default: return MODEL_EN_URL;
        }
    }
    
    /**
     * Start recognition from microphone
     */
    public void startListening() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
        }
        
        if (model == null) {
            Log.e(TAG, "Model not initialized");
            if (listener != null) {
                listener.onError("Model not initialized");
            }
            return;
        }
        
        try {
            Recognizer recognizer = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(recognizer, 16000.0f);
            speechService.startListening(this);
            Log.i(TAG, "Started listening");
        } catch (IOException e) {
            Log.e(TAG, "Failed to start listening", e);
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        }
    }
    
    /**
     * Start recognition from audio file
     * @param stream Audio input stream
     */
    public void recognizeFile(InputStream stream) {
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        }
        
        if (model == null) {
            Log.e(TAG, "Model not initialized");
            if (listener != null) {
                listener.onError("Model not initialized");
            }
            return;
        }
        
        try {
            Recognizer recognizer = new Recognizer(model, 16000.0f);
            speechStreamService = new SpeechStreamService(recognizer, stream, 16000);
            speechStreamService.start(this);
            Log.i(TAG, "Started file recognition");
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
