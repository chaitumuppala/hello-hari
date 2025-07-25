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
import java.util.List;
import java.util.ArrayList;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

public class VoskSpeechRecognizer {
    private static final String TAG = "VoskRecognizer";
    
    // Static initializer to ensure VOSK library is loaded
    static {
        try {
            System.loadLibrary("vosk");
            Log.d(TAG, "VOSK native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load VOSK native library", e);
        }
    }
    
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
    
    public interface DownloadCallback {
        void onComplete(boolean success);
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
     * Check for existing models without auto-downloading
     */
    public void checkExistingModels() {
        downloadExecutor.execute(() -> {
            try {
                Log.d(TAG, "Checking for existing VOSK models...");
                
                // Check which models are already available
                boolean englishAvailable = isModelAvailable("en");
                boolean hindiAvailable = isModelAvailable("hi");
                boolean teluguAvailable = isModelAvailable("te");
                
                if (englishAvailable || hindiAvailable || teluguAvailable) {
                    // At least one model is available, initialize with it
                    String firstAvailable = englishAvailable ? "en" : (hindiAvailable ? "hi" : "te");
                    loadModel(firstAvailable);
                    isInitialized = true;
                    if (recognitionListener != null) {
                        recognitionListener.onInitializationComplete(true);
                    }
                    Log.d(TAG, "VOSK initialized with existing models");
                } else {
                    Log.d(TAG, "No existing models found");
                    if (recognitionListener != null) {
                        recognitionListener.onInitializationComplete(false);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Model check failed", e);
                if (recognitionListener != null) {
                    recognitionListener.onInitializationComplete(false);
                    recognitionListener.onError("Model check failed: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Download only selected models
     */
    public void downloadSelectedModels(boolean downloadEnglish, boolean downloadHindi, boolean downloadTelugu) {
        downloadExecutor.execute(() -> {
            try {
                Log.d(TAG, "Starting download for selected models...");
                
                // Count total downloads needed
                int totalDownloads = 0;
                if (downloadEnglish) totalDownloads++;
                if (downloadHindi) totalDownloads++;
                if (downloadTelugu) totalDownloads++;
                
                if (totalDownloads == 0) {
                    Log.w(TAG, "No models selected for download");
                    if (recognitionListener != null) {
                        recognitionListener.onInitializationComplete(false);
                    }
                    return;
                }
                
                // Use CountDownLatch to wait for all downloads to complete
                java.util.concurrent.CountDownLatch downloadLatch = new java.util.concurrent.CountDownLatch(totalDownloads);
                
                // Track download results
                java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
                
                // Download completion callback
                Runnable onDownloadComplete = () -> {
                    downloadLatch.countDown();
                    Log.d(TAG, "Download completed. Remaining: " + downloadLatch.getCount());
                };
                
                // Start downloads
                if (downloadEnglish) {
                    downloadModelWithCallback("en", ENGLISH_MODEL, new DownloadCallback() {
                        @Override
                        public void onComplete(boolean success) {
                            if (success) successCount.incrementAndGet();
                            onDownloadComplete.run();
                        }
                    });
                }
                if (downloadHindi) {
                    downloadModelWithCallback("hi", HINDI_MODEL, new DownloadCallback() {
                        @Override
                        public void onComplete(boolean success) {
                            if (success) successCount.incrementAndGet();
                            onDownloadComplete.run();
                        }
                    });
                }
                if (downloadTelugu) {
                    downloadModelWithCallback("te", TELUGU_MODEL, new DownloadCallback() {
                        @Override
                        public void onComplete(boolean success) {
                            if (success) successCount.incrementAndGet();
                            onDownloadComplete.run();
                        }
                    });
                }
                
                // Wait for all downloads to complete
                try {
                    Log.d(TAG, "Waiting for " + totalDownloads + " downloads to complete...");
                    downloadLatch.await();
                    Log.d(TAG, "All downloads completed. Success count: " + successCount.get());
                    
                    // Give a small delay to ensure file system operations are complete
                    Thread.sleep(1000);
                    Log.d(TAG, "Proceeding with model initialization after delay...");
                    
                } catch (InterruptedException e) {
                    Log.e(TAG, "Download wait interrupted", e);
                    Thread.currentThread().interrupt();
                }
                
                // Now try to initialize with any available model (regardless of what was requested)
                boolean initSuccess = false;
                Exception initError = null;
                
                try {
                    // Check all models and initialize with the first available one
                    if (isModelAvailable("en")) {
                        Log.d(TAG, "Attempting to load English model...");
                        loadModel("en");
                        initSuccess = true;
                        Log.d(TAG, "Successfully initialized with English model");
                    } else if (isModelAvailable("hi")) {
                        Log.d(TAG, "Attempting to load Hindi model...");
                        loadModel("hi");
                        initSuccess = true;
                        Log.d(TAG, "Successfully initialized with Hindi model");
                    } else if (isModelAvailable("te")) {
                        Log.d(TAG, "Attempting to load Telugu model...");
                        loadModel("te");
                        initSuccess = true;
                        Log.d(TAG, "Successfully initialized with Telugu model");
                    } else {
                        Log.w(TAG, "No valid models found after download. Checking model status:");
                        Log.w(TAG, "  English model available: " + isModelAvailable("en"));
                        Log.w(TAG, "  Hindi model available: " + isModelAvailable("hi"));
                        Log.w(TAG, "  Telugu model available: " + isModelAvailable("te"));
                        Log.w(TAG, "  Download requests - EN:" + downloadEnglish + " HI:" + downloadHindi + " TE:" + downloadTelugu);
                        
                        // Force detailed validation check for debugging
                        String[] languages = {"en", "hi", "te"};
                        for (String lang : languages) {
                            String modelPath = getModelPath(lang);
                            Log.w(TAG, "  Detailed check for " + lang + ":");
                            Log.w(TAG, "    Model path: " + modelPath);
                            File modelDir = new File(modelPath);
                            Log.w(TAG, "    Directory exists: " + modelDir.exists());
                            Log.w(TAG, "    Is directory: " + modelDir.isDirectory());
                            if (modelDir.exists()) {
                                File[] files = modelDir.listFiles();
                                Log.w(TAG, "    Contains " + (files != null ? files.length : 0) + " items");
                                if (files != null && files.length > 0) {
                                    for (File f : files) {
                                        Log.w(TAG, "      - " + f.getName());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Model loading failed during initialization", e);
                    initError = e;
                    initSuccess = false;
                }
                
                isInitialized = initSuccess;
                
                if (recognitionListener != null) {
                    recognitionListener.onInitializationComplete(initSuccess);
                    if (!initSuccess && initError != null) {
                        recognitionListener.onError("Model loading failed: " + initError.getMessage());
                    }
                }
                
                if (initSuccess) {
                    Log.d(TAG, "VOSK initialization successful after model download");
                } else {
                    Log.e(TAG, "VOSK initialization failed - no valid models found after download" + 
                          (initError != null ? ": " + initError.getMessage() : ""));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Selected model download failed", e);
                if (recognitionListener != null) {
                    recognitionListener.onInitializationComplete(false);
                    recognitionListener.onError("Download failed: " + e.getMessage());
                }
            }
        });
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
     * Download model with callback for completion
     */
    private void downloadModelWithCallback(String language, String modelFileName, DownloadCallback callback) {
        downloadExecutor.execute(() -> {
            String modelPath = getModelPath(language);
            File modelDir = new File(modelPath);
            
            if (modelDir.exists() && isValidModel(modelPath)) {
                Log.d(TAG, "Model for " + language + " already exists");
                updateDownloadStatus(language, DownloadStatus.COMPLETED);
                if (recognitionListener != null) {
                    recognitionListener.onModelDownloadComplete(language, true);
                }
                callback.onComplete(true);
                return;
            }
            
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
                        callback.onComplete(true);
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
                callback.onComplete(false);
            }
        });
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
            Log.d(TAG, "Starting extraction of: " + zipPath + " to: " + extractPath);
            
            File zipFile = new File(zipPath);
            if (!zipFile.exists()) {
                Log.e(TAG, "ZIP file does not exist: " + zipPath);
                return false;
            }
            
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;
            int extractedFiles = 0;
            
            // Ensure extraction directory exists
            File extractDir = new File(extractPath);
            if (!extractDir.exists()) {
                boolean created = extractDir.mkdirs();
                Log.d(TAG, "Created extraction directory: " + extractPath + " - " + created);
            }
            
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                String filePath = extractPath + "/" + entryName; // Fixed: added missing "/"
                
                Log.d(TAG, "Processing ZIP entry: " + entryName + " (size: " + entry.getSize() + ")");
                
                if (entry.isDirectory()) {
                    File dir = new File(filePath);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                } else {
                    // Create parent directories
                    File parentDir = new File(filePath).getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    
                    // Extract file with better error handling and verification
                    try {
                        FileOutputStream fos = new FileOutputStream(filePath);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        byte[] buffer = new byte[16384]; // Larger buffer for better performance
                        int length;
                        long totalBytes = 0;
                        long expectedSize = entry.getSize();
                        
                        while ((length = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, length);
                            totalBytes += length;
                        }
                        
                        bos.flush();
                        bos.close();
                        fos.close();
                        
                        // Verify file size if known
                        if (expectedSize > 0 && totalBytes != expectedSize) {
                            Log.w(TAG, "Size mismatch for " + entryName + ": expected " + expectedSize + ", got " + totalBytes);
                        }
                        
                        extractedFiles++;
                        Log.d(TAG, "Extracted: " + entryName + " (" + totalBytes + " bytes)");
                        
                        // Special verification for critical VOSK files
                        if (entryName.endsWith("graph/HCLG.fst") || entryName.endsWith("graph/HCLr.fst") || 
                            entryName.endsWith("graph/Gr.fst") || entryName.endsWith("am/final.mdl") || 
                            entryName.endsWith("conf/model.conf") || entryName.endsWith("conf/mfcc.conf") ||
                            entryName.endsWith("graph/phones/word_boundary.int")) {
                            File extractedFile = new File(filePath);
                            if (extractedFile.exists() && extractedFile.length() > 0) {
                                Log.i(TAG, "CRITICAL FILE: " + entryName + " extracted successfully - " + extractedFile.length() + " bytes");
                            } else {
                                Log.e(TAG, "CRITICAL FILE: " + entryName + " extraction failed or empty!");
                                // Don't fail immediately, but log the issue
                            }
                        }
                        
                    } catch (Exception fileEx) {
                        Log.e(TAG, "Failed to extract file: " + entryName, fileEx);
                        // Continue with other files even if one fails
                    }
                }
                zis.closeEntry();
            }
            
            zis.close();
            fis.close();
            
            Log.d(TAG, "Extraction completed. Files extracted: " + extractedFiles);
            
            // Verify extraction by checking if required files exist
            if (extractedFiles == 0) {
                Log.e(TAG, "No files were extracted from ZIP");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "ZIP extraction failed: " + e.getMessage(), e);
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
        Log.d(TAG, "Checking model validity at: " + modelPath);
        
        if (!modelDir.exists()) {
            Log.d(TAG, "Model directory does not exist: " + modelPath);
            return false;
        }
        
        if (!modelDir.isDirectory()) {
            Log.w(TAG, "Model path is not a directory: " + modelPath);
            return false;
        }
        
        // List contents for debugging
        File[] files = modelDir.listFiles();
        if (files != null) {
            Log.d(TAG, "Model directory contains " + files.length + " items:");
            for (File file : files) {
                Log.d(TAG, "  - " + file.getName() + (file.isDirectory() ? " (dir)" : " (file)"));
            }
        }
        
        // Check for required VOSK model files based on official documentation
        // See: https://alphacephei.com/vosk/models
        String[] requiredFiles = {
            "conf/model.conf", 
            "conf/mfcc.conf",
            "am/final.mdl", 
            "graph/phones/word_boundary.int"
        };
        
        for (String file : requiredFiles) {
            File requiredFile = new File(modelDir, file);
            if (!requiredFile.exists()) {
                Log.w(TAG, "Missing required model file: " + file + " at " + requiredFile.getAbsolutePath());
                return false;
            } else {
                Log.d(TAG, "Found required file: " + file + " (" + requiredFile.length() + " bytes)");
            }
        }
        
        // Check for graph files - can be either single HCLG.fst or split HCLr.fst + Gr.fst
        File hclgFile = new File(modelDir, "graph/HCLG.fst");
        File hclrFile = new File(modelDir, "graph/HCLr.fst");
        File grFile = new File(modelDir, "graph/Gr.fst");
        
        boolean hasSingleGraph = hclgFile.exists();
        boolean hasSplitGraph = hclrFile.exists() && grFile.exists();
        
        if (hasSingleGraph) {
            Log.d(TAG, "Found single graph: HCLG.fst (" + hclgFile.length() + " bytes)");
        } else if (hasSplitGraph) {
            Log.d(TAG, "Found split graph: HCLr.fst (" + hclrFile.length() + " bytes) + Gr.fst (" + grFile.length() + " bytes)");
        } else {
            Log.w(TAG, "Missing graph files - need either HCLG.fst OR (HCLr.fst + Gr.fst)");
            return false;
        }
        
        Log.d(TAG, "Model validation successful for: " + modelPath);
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
        Log.d(TAG, "Attempting to load model for " + language + " at path: " + modelPath);
        
        if (!isValidModel(modelPath)) {
            String error = "Model not available for language: " + language + " at path: " + modelPath;
            Log.e(TAG, error);
            throw new IOException(error);
        }
        
        // Close current model if exists
        if (currentModel != null) {
            Log.d(TAG, "Closing existing model for " + currentLanguage);
            try {
                currentModel.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing previous model", e);
            }
        }
        
        try {
            Log.d(TAG, "Creating new VOSK model for language: " + language);
            currentModel = new Model(modelPath);
            currentLanguage = language;
            Log.d(TAG, "Model loaded successfully for " + language);
        } catch (Exception e) {
            String error = "Failed to create VOSK model for " + language + ": " + e.getMessage();
            Log.e(TAG, error, e);
            throw new IOException(error, e);
        }
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
