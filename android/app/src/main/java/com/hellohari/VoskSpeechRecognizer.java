package com.hellohari;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

public class VoskSpeechRecognizer {
    private static final String TAG = "VoskRecognizer";
    
    // Model configuration
    private static final String MODEL_BASE_URL = "https://alphacephei.com/vosk/models/";
    private static final String ENGLISH_MODEL = "vosk-model-small-en-us-0.15.zip";
    private static final String HINDI_MODEL = "vosk-model-small-hi-0.22.zip";
    private static final String TELUGU_MODEL = "vosk-model-small-te-0.42.zip";
    
    private Context context;
    private Model currentModel;
    private SpeechService speechService;
    private VoskRecognitionListener recognitionListener;
    private boolean isInitialized = false;
    private String currentLanguage = "en";
    private ExecutorService downloadExecutor;
    
    // Model download status
    public enum DownloadStatus {
        NOT_STARTED, DOWNLOADING, COMPLETED, FAILED
    }
    
    private DownloadStatus englishModelStatus = DownloadStatus.NOT_STARTED;
    private DownloadStatus hindiModelStatus = DownloadStatus.NOT_STARTED;
    private DownloadStatus teluguModelStatus = DownloadStatus.NOT_STARTED;
    
    public interface VoskRecognitionListener {
        void onPartialResult(String partialText, String language);
        void onFinalResult(String finalText, String language, float confidence);
        void onError(String error);
        void onInitializationComplete(boolean success);
        void onModelDownloadProgress(String language, int progress);
        void onModelDownloadComplete(String language, boolean success);
    }
    
    public interface ModelDownloadListener {
        void onDownloadProgress(String language, int progress);
        void onDownloadComplete(String language, boolean success);
    }
    
    public VoskSpeechRecognizer(Context context) {
        this.context = context;
        this.downloadExecutor = Executors.newFixedThreadPool(3);
        Log.d(TAG, "VoskSpeechRecognizer initialized with runtime model downloading");
    }
    
    public void setRecognitionListener(VoskRecognitionListener listener) {
        this.recognitionListener = listener;
    }
    
    /**
     * Initialize VOSK with automatic model downloading
     */
    public void initialize() {
        downloadExecutor.execute(() -> {
            try {
                Log.d(TAG, "Starting VOSK initialization with model download...");
                
                // Check and download models for all languages
                checkAndDownloadModel("en", ENGLISH_MODEL);
                checkAndDownloadModel("hi", HINDI_MODEL);  
                checkAndDownloadModel("te", TELUGU_MODEL);
                
                // Initialize with English model first (most universal)
                if (isModelAvailable("en")) {
                    loadModel("en");
                    isInitialized = true;
                    if (recognitionListener != null) {
                        recognitionListener.onInitializationComplete(true);
                    }
                    Log.d(TAG, "VOSK initialization successful");
                } else {
                    Log.e(TAG, "No models available for initialization");
                    if (recognitionListener != null) {
                        recognitionListener.onInitializationComplete(false);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "VOSK initialization failed", e);
                if (recognitionListener != null) {
                    recognitionListener.onInitializationComplete(false);
                    recognitionListener.onError("Initialization failed: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Check if model exists, download if not
     */
    private void checkAndDownloadModel(String language, String modelFileName) {
        String modelPath = getModelPath(language);
        File modelDir = new File(modelPath);
        
        if (!modelDir.exists() || !isValidModel(modelPath)) {
            Log.d(TAG, "Model for " + language + " not found, downloading...");
            downloadModel(language, modelFileName);
        } else {
            Log.d(TAG, "Model for " + language + " already exists");
            updateDownloadStatus(language, DownloadStatus.COMPLETED);
            if (recognitionListener != null) {
                recognitionListener.onModelDownloadComplete(language, true);
            }
        }
    }
    
    /**
     * Download and extract VOSK model
     */
    private void downloadModel(String language, String modelFileName) {
        try {
            updateDownloadStatus(language, DownloadStatus.DOWNLOADING);
            
            String downloadUrl = MODEL_BASE_URL + modelFileName;
            String modelsDir = context.getFilesDir() + "/vosk-models/";
            String zipPath = modelsDir + modelFileName;
            
            // Create models directory
            new File(modelsDir).mkdirs();
            
            Log.d(TAG, "Downloading " + language + " model from: " + downloadUrl);
            
            // Download the zip file
            if (downloadFile(downloadUrl, zipPath, language)) {
                Log.d(TAG, "Download complete for " + language + ", extracting...");
                
                // Extract the zip file
                if (extractZipFile(zipPath, modelsDir)) {
                    Log.d(TAG, "Extraction complete for " + language);
                    
                    // Clean up zip file
                    new File(zipPath).delete();
                    
                    updateDownloadStatus(language, DownloadStatus.COMPLETED);
                    if (recognitionListener != null) {
                        recognitionListener.onModelDownloadComplete(language, true);
                    }
                } else {
                    throw new Exception("Failed to extract model");
                }
            } else {
                throw new Exception("Failed to download model");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Model download failed for " + language, e);
            updateDownloadStatus(language, DownloadStatus.FAILED);
            if (recognitionListener != null) {
                recognitionListener.onModelDownloadComplete(language, false);
                recognitionListener.onError("Model download failed for " + language + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Download file with progress tracking
     */
    private boolean downloadFile(String downloadUrl, String destinationPath, String language) {
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            
            int fileLength = connection.getContentLength();
            Log.d(TAG, "Downloading " + language + " model, size: " + (fileLength / 1024 / 1024) + "MB");
            
            InputStream input = new BufferedInputStream(connection.getInputStream());
            OutputStream output = new FileOutputStream(destinationPath);
            
            byte[] data = new byte[8192];
            long total = 0;
            int count;
            int lastProgress = 0;
            
            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
                
                // Update progress every 5%
                if (fileLength > 0) {
                    int progress = (int) ((total * 100) / fileLength);
                    if (progress >= lastProgress + 5) {
                        lastProgress = progress;
                        Log.d(TAG, language + " download progress: " + progress + "%");
                        if (recognitionListener != null) {
                            recognitionListener.onModelDownloadProgress(language, progress);
                        }
                    }
                }
            }
            
            output.flush();
            output.close();
            input.close();
            connection.disconnect();
            
            Log.d(TAG, "Download completed for " + language);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Download failed for " + language, e);
            return false;
        }
    }
    
    /**
     * Extract ZIP file
     */
    private boolean extractZipFile(String zipPath, String extractPath) {
        try {
            FileInputStream fis = new FileInputStream(zipPath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                String filePath = extractPath + entry.getName();
                
                if (entry.isDirectory()) {
                    new File(filePath).mkdirs();
                } else {
                    // Create parent directories
                    new File(filePath).getParentFile().mkdirs();
                    
                    // Extract file
                    FileOutputStream fos = new FileOutputStream(filePath);
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    fos.close();
                }
                zis.closeEntry();
            }
            
            zis.close();
            fis.close();
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "ZIP extraction failed", e);
            return false;
        }
    }
    
    /**
     * Get model directory path
     */
    private String getModelPath(String language) {
        String modelName;
        switch (language) {
            case "en": modelName = "vosk-model-small-en-us-0.15"; break;
            case "hi": modelName = "vosk-model-small-hi-0.22"; break;
            case "te": modelName = "vosk-model-small-te-0.42"; break;
            default: modelName = "vosk-model-small-en-us-0.15"; break;
        }
        return context.getFilesDir() + "/vosk-models/" + modelName;
    }
    
    /**
     * Check if model is valid
     */
    private boolean isValidModel(String modelPath) {
        File modelDir = new File(modelPath);
        if (!modelDir.exists()) return false;
        
        // Check for required VOSK model files
        String[] requiredFiles = {"conf/model.conf", "am/final.mdl", "graph/HCLG.fst"};
        for (String file : requiredFiles) {
            if (!new File(modelDir, file).exists()) {
                Log.w(TAG, "Missing required model file: " + file);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if model is available
     */
    private boolean isModelAvailable(String language) {
        return isValidModel(getModelPath(language));
    }
    
    /**
     * Load specific language model
     */
    private void loadModel(String language) throws IOException {
        String modelPath = getModelPath(language);
        if (!isValidModel(modelPath)) {
            throw new IOException("Model not available for language: " + language);
        }
        
        // Close current model if exists
        if (currentModel != null) {
            currentModel.close();
        }
        
        Log.d(TAG, "Loading VOSK model for language: " + language);
        currentModel = new Model(modelPath);
        currentLanguage = language;
        
        Log.d(TAG, "Model loaded successfully for " + language);
    }
    
    /**
     * Switch to different language model
     */
    public void switchLanguage(String language) {
        if (!isModelAvailable(language)) {
            Log.w(TAG, "Model not available for language: " + language);
            if (recognitionListener != null) {
                recognitionListener.onError("Model not available for language: " + language);
            }
            return;
        }
        
        downloadExecutor.execute(() -> {
            try {
                loadModel(language);
                Log.d(TAG, "Successfully switched to " + language + " model");
            } catch (Exception e) {
                Log.e(TAG, "Failed to switch to " + language + " model", e);
                if (recognitionListener != null) {
                    recognitionListener.onError("Failed to switch language: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Recognize audio file with current language model
     */
    public void recognizeFile(String audioFilePath) {
        if (!isInitialized || currentModel == null) {
            Log.w(TAG, "VOSK not initialized");
            if (recognitionListener != null) {
                recognitionListener.onError("VOSK not initialized");
            }
            return;
        }
        
        downloadExecutor.execute(() -> {
            try {
                Log.d(TAG, "Starting recognition for file: " + audioFilePath);
                
                Recognizer recognizer = new Recognizer(currentModel, 16000);
                
                // Read audio file and process
                FileInputStream audioFile = new FileInputStream(audioFilePath);
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = audioFile.read(buffer)) != -1) {
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        String result = recognizer.getResult();
                        if (recognitionListener != null) {
                            recognitionListener.onPartialResult(extractText(result), currentLanguage);
                        }
                    }
                }
                
                // Get final result
                String finalResult = recognizer.getFinalResult();
                if (recognitionListener != null) {
                    recognitionListener.onFinalResult(extractText(finalResult), currentLanguage, 0.85f);
                }
                
                audioFile.close();
                recognizer.close();
                
                Log.d(TAG, "Recognition completed for " + currentLanguage);
                
            } catch (Exception e) {
                Log.e(TAG, "Recognition failed", e);
                if (recognitionListener != null) {
                    recognitionListener.onError("Recognition failed: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Recognize with multiple languages (tries each available model)
     */
    public void recognizeMultiLanguage(String audioFilePath) {
        String[] languages = {"en", "hi", "te"};
        
        for (String lang : languages) {
            if (isModelAvailable(lang)) {
                Log.d(TAG, "Attempting recognition with " + lang + " model");
                switchLanguage(lang);
                recognizeFile(audioFilePath);
            }
        }
    }
    
    /**
     * Extract text from VOSK JSON result
     */
    private String extractText(String jsonResult) {
        try {
            // Simple JSON parsing for "text" field
            int start = jsonResult.indexOf("\"text\"") + 7;
            int textStart = jsonResult.indexOf("\"", start) + 1;
            int textEnd = jsonResult.indexOf("\"", textStart);
            
            if (textStart > 0 && textEnd > textStart) {
                return jsonResult.substring(textStart, textEnd).trim();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse VOSK result: " + jsonResult);
        }
        return "";
    }
    
    /**
     * Update download status
     */
    private void updateDownloadStatus(String language, DownloadStatus status) {
        switch (language) {
            case "en": englishModelStatus = status; break;
            case "hi": hindiModelStatus = status; break;
            case "te": teluguModelStatus = status; break;
        }
    }
    
    /**
     * Get download status for language
     */
    public DownloadStatus getDownloadStatus(String language) {
        switch (language) {
            case "en": return englishModelStatus;
            case "hi": return hindiModelStatus;
            case "te": return teluguModelStatus;
            default: return DownloadStatus.NOT_STARTED;
        }
    }
    
    /**
     * Get available languages (models that are downloaded)
     */
    public String[] getAvailableLanguages() {
        List<String> available = new ArrayList<>();
        if (isModelAvailable("en")) available.add("English");
        if (isModelAvailable("hi")) available.add("Hindi");
        if (isModelAvailable("te")) available.add("Telugu");
        return available.toArray(new String[0]);
    }
    
    /**
     * Get total download size for all models
     */
    public String getRequiredDownloadSize() {
        int totalMB = 0;
        if (!isModelAvailable("en")) totalMB += 40; // English model ~40MB
        if (!isModelAvailable("hi")) totalMB += 50; // Hindi model ~50MB  
        if (!isModelAvailable("te")) totalMB += 42; // Telugu model ~42MB
        
        if (totalMB == 0) return "All models downloaded";
        return totalMB + " MB download required";
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        try {
            if (speechService != null) {
                speechService.stop();
                speechService = null;
            }
            
            if (currentModel != null) {
                currentModel.close();
                currentModel = null;
            }
            
            if (downloadExecutor != null && !downloadExecutor.isShutdown()) {
                downloadExecutor.shutdown();
            }
            
            isInitialized = false;
            Log.d(TAG, "VOSK cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}
