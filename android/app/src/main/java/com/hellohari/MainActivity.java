package com.hellohari;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SimpleCallDetector.CallDetectionListener {
    private static final String TAG = "HelloHariMain";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    // Core Components
    private SimpleCallDetector callDetector;
    private AudioManager audioManager;
    private MultiLanguageScamDetector aiDetector;
    private VoskSpeechRecognizer voskRecognizer;
    
    // UI Components
    private LinearLayout rootContainer;
    private TextView statusIndicator;
    private TextView protectionStatusText;
    private Button mainActionButton;
    private TextView riskLevelText;
    private ProgressBar riskMeter;
    private LinearLayout riskAlertCard;
    private TextView alertIcon;
    private LinearLayout systemStatusCard;
    private TextView callLogText;
    private ScrollView callLogScrollView;
    
    // VOSK Download UI Components
    private LinearLayout modelDownloadCard;
    private TextView downloadStatusText;
    private ProgressBar downloadProgressBar;
    private Button downloadModelsButton;
    private TextView downloadSizeText;
    
    // State Management
    private boolean isProtectionActive = false;
    private boolean isVoskInitialized = false;
    private List<String> callLogs = new ArrayList<>();
    private String currentRecordingPath = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "Hello Hari initializing...");
            
            setupPermissions();
            createUI();
            initializeComponents();
            initializeVosk();
            
            // Initialize logs
            addToCallLog("Hello Hari protection system initialized");
            addToCallLog("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
            addToCallLog("AI Engine: " + aiDetector.getPatternCount() + " scam patterns loaded");
            
            Log.d(TAG, "Hello Hari initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            createFallbackUI();
        }
    }
    
    private void initializeVosk() {
        try {
            addToCallLog("Initializing VOSK speech recognition...");
            
            voskRecognizer = new VoskSpeechRecognizer(this);
            voskRecognizer.setRecognitionListener(new VoskSpeechRecognizer.VoskRecognitionListener() {
                @Override
                public void onPartialResult(String partialText, String language) {
                    runOnUiThread(() -> {
                        addToCallLog("VOSK Partial (" + language + "): " + partialText);
                    });
                }
                
                @Override
                public void onFinalResult(String finalText, String language, float confidence) {
                    runOnUiThread(() -> {
                        addToCallLog("VOSK Final (" + language + "): " + finalText + " (" + (int)(confidence*100) + "% confidence)");
                        
                        // Analyze with real VOSK transcription
                        if (aiDetector != null) {
                            aiDetector.analyzeWithVosk(currentRecordingPath, voskRecognizer);
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        addToCallLog("VOSK Error: " + error);
                    });
                }
                
                @Override
                public void onInitializationComplete(boolean success) {
                    isVoskInitialized = success;
                    runOnUiThread(() -> {
                        if (success) {
                            addToCallLog("VOSK initialization complete - Real AI ready!");
                            updateVoskDownloadUI();
                        } else {
                            addToCallLog("VOSK initialization failed - using simulation mode");
                        }
                        updateSystemStatus();
                    });
                }
                
                @Override
                public void onModelDownloadProgress(String language, int progress) {
                    runOnUiThread(() -> {
                        updateDownloadProgress(language, progress);
                    });
                }
                
                @Override
                public void onModelDownloadComplete(String language, boolean success) {
                    runOnUiThread(() -> {
                        if (success) {
                            addToCallLog(language + " model download completed");
                        } else {
                            addToCallLog(language + " model download failed");
                        }
                        updateVoskDownloadUI();
                    });
                }
            });
            
            // Start VOSK initialization (will auto-download models)
            voskRecognizer.initialize();
            
        } catch (Exception e) {
            Log.e(TAG, "VOSK initialization error", e);
            addToCallLog("VOSK initialization error: " + e.getMessage());
        }
    }
    
    private void createUI() {
        // Main container
        rootContainer = new LinearLayout(this);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setPadding(24, 24, 24, 24);
        rootContainer.setBackgroundColor(Color.parseColor("#F8FAFC"));
        
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(rootContainer);
        setContentView(scrollView);
        
        // Create all UI components
        createHeader();
        createMainStatusCard();
        createModelDownloadCard();
        createRiskAlerts(rootContainer);
        createSystemStatusCard(rootContainer);
        createQuickActions(rootContainer);
        createCallLogCard(rootContainer);
    }
    
    private void createHeader() {
        TextView headerTitle = new TextView(this);
        headerTitle.setText("Hello Hari");
        headerTitle.setTextSize(28);
        headerTitle.setTextColor(Color.parseColor("#111827"));
        headerTitle.setTypeface(null, Typeface.BOLD);
        headerTitle.setGravity(Gravity.CENTER);
        headerTitle.setPadding(0, 0, 0, 8);
        rootContainer.addView(headerTitle);
        
        TextView headerSubtitle = new TextView(this);
        headerSubtitle.setText("AI-Powered Scam Protection");
        headerSubtitle.setTextSize(16);
        headerSubtitle.setTextColor(Color.parseColor("#6B7280"));
        headerSubtitle.setGravity(Gravity.CENTER);
        headerSubtitle.setPadding(0, 0, 0, 24);
        rootContainer.addView(headerSubtitle);
    }
    
    private void createMainStatusCard() {
        LinearLayout card = createCard();
        card.setPadding(24, 24, 24, 24);
        
        // Status indicator
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        
        statusIndicator = new TextView(this);
        statusIndicator.setText("●");
        statusIndicator.setTextSize(24);
        statusIndicator.setTextColor(Color.parseColor("#EF4444"));
        statusIndicator.setPadding(0, 0, 16, 0);
        statusRow.addView(statusIndicator);
        
        protectionStatusText = new TextView(this);
        protectionStatusText.setText("Protection Inactive");
        protectionStatusText.setTextSize(18);
        protectionStatusText.setTextColor(Color.parseColor("#111827"));
        protectionStatusText.setTypeface(null, Typeface.BOLD);
        statusRow.addView(protectionStatusText);
        
        card.addView(statusRow);
        addSpacing(card, 16);
        
        // Main action button
        mainActionButton = createActionButton("Start AI Scam Protection", "#10B981");
        mainActionButton.setOnClickListener(v -> toggleProtection());
        card.addView(mainActionButton);
        
        addSpacing(card, 16);
        
        // Risk level display
        TextView riskLabel = new TextView(this);
        riskLabel.setText("Current Risk Level");
        riskLabel.setTextSize(14);
        riskLabel.setTextColor(Color.parseColor("#6B7280"));
        card.addView(riskLabel);
        
        addSpacing(card, 8);
        
        riskMeter = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        riskMeter.setMax(100);
        riskMeter.setProgress(0);
        riskMeter.getProgressDrawable().setColorFilter(Color.parseColor("#10B981"), PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 24);
        riskMeter.setLayoutParams(progressParams);
        card.addView(riskMeter);
        
        addSpacing(card, 8);
        
        riskLevelText = new TextView(this);
        riskLevelText.setText("No active calls - Risk: 0%");
        riskLevelText.setTextSize(14);
        riskLevelText.setTextColor(Color.parseColor("#6B7280"));
        card.addView(riskLevelText);
        
        rootContainer.addView(card);
        addSpacing(rootContainer, 16);
    }
    
    private void createModelDownloadCard() {
        modelDownloadCard = createCard();
        modelDownloadCard.setPadding(24, 24, 24, 24);
        
        TextView title = new TextView(this);
        title.setText("VOSK AI Models");
        title.setTextSize(18);
        title.setTextColor(Color.parseColor("#111827"));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        modelDownloadCard.addView(title);
        
        downloadStatusText = new TextView(this);
        downloadStatusText.setText("Checking model availability...");
        downloadStatusText.setTextSize(14);
        downloadStatusText.setTextColor(Color.parseColor("#6B7280"));
        modelDownloadCard.addView(downloadStatusText);
        
        addSpacing(modelDownloadCard, 12);
        
        downloadSizeText = new TextView(this);
        downloadSizeText.setText("Download size: Calculating...");
        downloadSizeText.setTextSize(12);
        downloadSizeText.setTextColor(Color.parseColor("#9CA3AF"));
        modelDownloadCard.addView(downloadSizeText);
        
        addSpacing(modelDownloadCard, 16);
        
        downloadProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        downloadProgressBar.setMax(100);
        downloadProgressBar.setProgress(0);
        downloadProgressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 24);
        downloadProgressBar.setLayoutParams(progressParams);
        modelDownloadCard.addView(downloadProgressBar);
        
        addSpacing(modelDownloadCard, 16);
        
        downloadModelsButton = createActionButton("Download AI Models", "#2563EB");
        downloadModelsButton.setOnClickListener(v -> startModelDownload());
        modelDownloadCard.addView(downloadModelsButton);
        
        rootContainer.addView(modelDownloadCard);
        addSpacing(rootContainer, 16);
    }
    
    private void updateVoskDownloadUI() {
        if (voskRecognizer == null) return;
        
        String[] availableLanguages = voskRecognizer.getAvailableLanguages();
        String downloadSize = voskRecognizer.getRequiredDownloadSize();
        
        downloadSizeText.setText("Download size: " + downloadSize);
        
        if (availableLanguages.length == 0) {
            downloadStatusText.setText("No models downloaded. Download required for AI functionality.");
            downloadStatusText.setTextColor(Color.parseColor("#EF4444"));
            downloadModelsButton.setVisibility(View.VISIBLE);
            downloadModelsButton.setText("Download AI Models (" + downloadSize + ")");
        } else if (availableLanguages.length < 3) {
            downloadStatusText.setText("Partial models: " + String.join(", ", availableLanguages));
            downloadStatusText.setTextColor(Color.parseColor("#F59E0B"));
            downloadModelsButton.setVisibility(View.VISIBLE);
            downloadModelsButton.setText("Download Remaining Models");
        } else {
            downloadStatusText.setText("All models ready: " + String.join(", ", availableLanguages));
            downloadStatusText.setTextColor(Color.parseColor("#10B981"));
            downloadModelsButton.setVisibility(View.GONE);
        }
    }
    
    private void updateDownloadProgress(String language, int progress) {
        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadProgressBar.setProgress(progress);
        downloadStatusText.setText("Downloading " + language + " model: " + progress + "%");
        
        if (progress == 100) {
            downloadProgressBar.setVisibility(View.GONE);
        }
    }
    
    private void startModelDownload() {
        if (voskRecognizer != null) {
            downloadModelsButton.setEnabled(false);
            downloadModelsButton.setText("Downloading...");
            addToCallLog("Starting AI model download...");
            voskRecognizer.initialize(); // This will trigger downloads if needed
        }
    }
    
    private void createRiskAlerts(LinearLayout parent) {
        riskAlertCard = new LinearLayout(this);
        riskAlertCard.setOrientation(LinearLayout.VERTICAL);
        riskAlertCard.setVisibility(View.GONE);
        riskAlertCard.setPadding(0, 16, 0, 0);
        
        alertIcon = new TextView(this);
        parent.addView(riskAlertCard);
    }
    
    private void createSystemStatusCard(LinearLayout parent) {
        systemStatusCard = createCard();
        systemStatusCard.setPadding(24, 24, 24, 24);
        systemStatusCard.setVisibility(View.GONE);
        
        TextView title = new TextView(this);
        title.setText("System Status");
        title.setTextSize(18);
        title.setTextColor(Color.parseColor("#111827"));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        systemStatusCard.addView(title);
        
        parent.addView(systemStatusCard);
        addSpacing(parent, 16);
    }
    
    private void createQuickActions(LinearLayout parent) {
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setWeightSum(2.0f);
        
        Button testAIButton = createActionButton("Test VOSK AI", "#2563EB");
        testAIButton.setOnClickListener(v -> testVoskIntegration());
        actionsRow.addView(testAIButton);
        
        addHorizontalSpacing(actionsRow, 12);
        
        Button testAudioButton = createActionButton("Test Audio", "#6B7280");
        testAudioButton.setOnClickListener(v -> testAudioCompatibility());
        actionsRow.addView(testAudioButton);
        
        parent.addView(actionsRow);
    }
    
    private void createCallLogCard(LinearLayout parent) {
        LinearLayout card = createCard();
        card.setPadding(24, 24, 24, 24);
        
        TextView title = new TextView(this);
        title.setText("Detection Logs");
        title.setTextSize(18);
        title.setTextColor(Color.parseColor("#111827"));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        card.addView(title);
        
        callLogScrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 400);
        callLogScrollView.setLayoutParams(scrollParams);
        
        callLogText = new TextView(this);
        callLogText.setTextSize(12);
        callLogText.setTextColor(Color.parseColor("#374151"));
        callLogText.setPadding(16, 16, 16, 16);
        callLogText.setBackgroundColor(Color.parseColor("#F9FAFB"));
        callLogScrollView.addView(callLogText);
        
        card.addView(callLogScrollView);
        parent.addView(card);
    }
    
    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(12);
        background.setStroke(1, Color.parseColor("#E5E7EB"));
        card.setBackground(background);
        
        return card;
    }
    
    private Button createActionButton(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setTypeface(null, Typeface.BOLD);
        
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor(colorHex));
        background.setCornerRadius(8);
        button.setBackground(background);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1.0f;
        button.setLayoutParams(params);
        
        return button;
    }
    
    private void addSpacing(LinearLayout parent, int dpHeight) {
        View spacer = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (int)(dpHeight * getResources().getDisplayMetrics().density));
        spacer.setLayoutParams(params);
        parent.addView(spacer);
    }
    
    private void addHorizontalSpacing(LinearLayout parent, int dpWidth) {
        View spacer = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            (int)(dpWidth * getResources().getDisplayMetrics().density), LinearLayout.LayoutParams.MATCH_PARENT);
        spacer.setLayoutParams(params);
        parent.addView(spacer);
    }
    
    private void initializeComponents() {
        try {
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            aiDetector = new MultiLanguageScamDetector(this);
            
            callDetector = new SimpleCallDetector(this);
            callDetector.setCallDetectionListener(this);
            
            addToCallLog("Core components initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Component initialization error", e);
            addToCallLog("Component initialization error: " + e.getMessage());
        }
    }
    
    private void setupPermissions() {
        String[] permissions = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        };
        
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }
    
    private void toggleProtection() {
        isProtectionActive = !isProtectionActive;
        updateSystemStatus();
        
        if (isProtectionActive) {
            startProtection();
        } else {
            stopProtection();
        }
    }
    
    private void startProtection() {
        try {
            if (callDetector != null) {
                callDetector.startMonitoring();
            }
            
            addToCallLog("Hello Hari protection activated");
            addToCallLog("Monitoring for incoming calls...");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting protection", e);
            addToCallLog("Error starting protection: " + e.getMessage());
        }
    }
    
    private void stopProtection() {
        try {
            if (callDetector != null) {
                callDetector.stopMonitoring();
            }
            
            addToCallLog("Hello Hari protection deactivated");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping protection", e);
            addToCallLog("Error stopping protection: " + e.getMessage());
        }
    }
    
    private void updateSystemStatus() {
        if (isProtectionActive) {
            statusIndicator.setTextColor(Color.parseColor("#10B981"));
            protectionStatusText.setText("Protection Active");
            mainActionButton.setText("Stop Protection");
            mainActionButton.setBackgroundColor(Color.parseColor("#EF4444"));
        } else {
            statusIndicator.setTextColor(Color.parseColor("#EF4444"));
            protectionStatusText.setText("Protection Inactive");
            mainActionButton.setText("Start AI Scam Protection");
            
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.parseColor("#10B981"));
            background.setCornerRadius(8);
            mainActionButton.setBackground(background);
        }
    }
    
    private void testVoskIntegration() {
        addToCallLog("Testing VOSK AI integration...");
        
        if (voskRecognizer == null) {
            addToCallLog("VOSK not initialized");
            return;
        }
        
        if (!isVoskInitialized) {
            addToCallLog("VOSK not ready - models may still be downloading");
            return;
        }
        
        String[] availableLanguages = voskRecognizer.getAvailableLanguages();
        addToCallLog("Available languages: " + String.join(", ", availableLanguages));
        addToCallLog("Current language: " + voskRecognizer.getCurrentLanguage());
        addToCallLog("VOSK test completed");
        
        // Test AI pattern detection
        if (aiDetector != null) {
            addToCallLog("Testing scam pattern detection...");
            addToCallLog("Pattern count: " + aiDetector.getPatternCount());
            addToCallLog("AI detection system ready");
        }
    }
    
    private void testAudioCompatibility() {
        addToCallLog("Testing audio compatibility...");
        
        int[] audioSources = {
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.CAMCORDER
        };
        
        String[] sourceNames = {
            "MIC", "VOICE_RECOGNITION", "VOICE_COMMUNICATION", "CAMCORDER"
        };
        
        for (int i = 0; i < audioSources.length; i++) {
            try {
                MediaRecorder recorder = new MediaRecorder();
                recorder.setAudioSource(audioSources[i]);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                recorder.setOutputFile("/dev/null");
                recorder.prepare();
                recorder.release();
                
                addToCallLog(sourceNames[i] + ": SUPPORTED");
            } catch (Exception e) {
                addToCallLog(sourceNames[i] + ": NOT SUPPORTED");
            }
        }
        
        addToCallLog("Audio compatibility test completed");
    }
    
    @Override
    public void onCallStarted(String phoneNumber) {
        addToCallLog("Call started: " + phoneNumber);
        startCallRecording(phoneNumber);
    }
    
    @Override
    public void onCallEnded(String phoneNumber) {
        addToCallLog("Call ended: " + phoneNumber);
        if (currentRecordingPath != null) {
            analyzeRecordingForScamsAI(currentRecordingPath, phoneNumber);
        }
    }
    
    private void startCallRecording(String phoneNumber) {
        try {
            addToCallLog("Starting call recording...");
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            currentRecordingPath = getExternalFilesDir(null) + "/call_" + timestamp + ".3gp";
            
            // Implementation would depend on your SimpleCallDetector
            addToCallLog("Recording to: " + currentRecordingPath);
            
        } catch (Exception e) {
            Log.e(TAG, "Recording error", e);
            addToCallLog("Recording error: " + e.getMessage());
        }
    }
    
    private void analyzeRecordingForScamsAI(String recordingPath, String phoneNumber) {
        if (!isVoskInitialized || voskRecognizer == null) {
            addToCallLog("VOSK not available, using fallback analysis");
            performBasicFallbackAnalysis(recordingPath, phoneNumber);
            return;
        }
        
        addToCallLog("Analyzing call with VOSK AI...");
        
        try {
            // Use VOSK to analyze the recording
            voskRecognizer.recognizeMultiLanguage(recordingPath);
            
        } catch (Exception e) {
            Log.e(TAG, "AI analysis error", e);
            addToCallLog("AI analysis error: " + e.getMessage());
            performBasicFallbackAnalysis(recordingPath, phoneNumber);
        }
    }
    
    private void performBasicFallbackAnalysis(String recordingPath, String phoneNumber) {
        addToCallLog("Performing fallback analysis...");
        
        // Basic analysis without AI
        int riskScore = 15; // Default low risk
        String analysisMessage = "Basic metadata analysis - No AI transcription available";
        
        updateRiskLevel(riskScore, analysisMessage);
        addToCallLog("Fallback analysis complete - Risk: " + riskScore + "%");
    }
    
    private void updateRiskLevel(int riskScore, String message) {
        riskMeter.setProgress(riskScore);
        riskLevelText.setText(message + " - Risk: " + riskScore + "%");
        
        // Update risk meter color
        int color;
        if (riskScore < 30) {
            color = Color.parseColor("#10B981"); // Green
        } else if (riskScore < 70) {
            color = Color.parseColor("#F59E0B"); // Orange
        } else {
            color = Color.parseColor("#EF4444"); // Red
        }
        
        riskMeter.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        riskLevelText.setTextColor(color);
    }
    
    private void addToCallLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = timestamp + " | " + message + "\n";
        
        callLogs.add(logEntry);
        
        runOnUiThread(() -> {
            if (callLogText != null) {
                StringBuilder fullLog = new StringBuilder();
                for (String log : callLogs) {
                    fullLog.append(log);
                }
                callLogText.setText(fullLog.toString());
                
                callLogScrollView.post(() -> callLogScrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
        
        Log.d(TAG, message);
    }
    
    private void createFallbackUI() {
        TextView errorText = new TextView(this);
        errorText.setText("Hello Hari initialization failed. Check logs for details.");
        errorText.setTextColor(Color.parseColor("#EF4444"));
        errorText.setGravity(Gravity.CENTER);
        errorText.setPadding(24, 24, 24, 24);
        setContentView(errorText);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (callDetector != null) {
                callDetector.stopMonitoring();
            }
            if (voskRecognizer != null) {
                voskRecognizer.cleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "Cleanup error", e);
        }
    }
}
