package com.hellohari;

import android.Manifest;
import android.content.Intent;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SimpleCallDetector.CallDetectionListener {
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
    
    // Language Selection Components
    private android.widget.CheckBox englishCheckbox;
    private android.widget.CheckBox hindiCheckbox;
    private android.widget.CheckBox teluguCheckbox;
    private boolean englishSelected = false;
    private boolean hindiSelected = false;
    private boolean teluguSelected = false;
    
    // State Management
    private boolean isProtectionActive = false;
    private boolean isVoskInitialized = false;
    private List<String> callLogs = new ArrayList<>();
    private String currentRecordingPath = null;
    private boolean serviceRunning = false;
    private String currentAudioPath = null;
    
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
                            addToCallLog("VOSK initialization complete - AI models ready!");
                            updateVoskDownloadUI();
                        } else {
                            addToCallLog("VOSK initialization failed - select and download models above");
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
                            addToCallLog(language + " model download completed successfully!");
                        } else {
                            addToCallLog(language + " model download failed");
                        }
                        updateVoskDownloadUI();
                    });
                }
            });
            
            // Check for existing models but don't auto-download
            voskRecognizer.checkExistingModels();
            addToCallLog("VOSK initialized - Please select languages to download above");
            
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
        
        // Language selection section
        TextView languageLabel = new TextView(this);
        languageLabel.setText("Select Languages to Download:");
        languageLabel.setTextSize(14);
        languageLabel.setTextColor(Color.parseColor("#374151"));
        languageLabel.setTypeface(null, Typeface.BOLD);
        languageLabel.setPadding(0, 0, 0, 12);
        modelDownloadCard.addView(languageLabel);
        
        // Create language checkboxes
        createLanguageSelection();
        
        addSpacing(modelDownloadCard, 16);
        
        downloadStatusText = new TextView(this);
        downloadStatusText.setText("Select languages above to start download");
        downloadStatusText.setTextSize(14);
        downloadStatusText.setTextColor(Color.parseColor("#6B7280"));
        modelDownloadCard.addView(downloadStatusText);
        
        addSpacing(modelDownloadCard, 12);
        
        downloadSizeText = new TextView(this);
        downloadSizeText.setText("Total download size: 0 MB");
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
        
        downloadModelsButton = createActionButton("Download Selected Models", "#2563EB");
        downloadModelsButton.setOnClickListener(v -> startSelectedModelsDownload());
        downloadModelsButton.setEnabled(false); // Enable when languages are selected
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
        // First row - AI Testing
        LinearLayout aiTestRow = new LinearLayout(this);
        aiTestRow.setOrientation(LinearLayout.HORIZONTAL);
        aiTestRow.setWeightSum(2.0f);
        
        Button testAIButton = createActionButton("Test VOSK AI", "#2563EB");
        testAIButton.setOnClickListener(v -> testVoskIntegration());
        aiTestRow.addView(testAIButton);
        
        addHorizontalSpacing(aiTestRow, 12);
        
        Button testAudioButton = createActionButton("Test Audio", "#6B7280");
        testAudioButton.setOnClickListener(v -> testAudioCompatibility());
        aiTestRow.addView(testAudioButton);
        
        parent.addView(aiTestRow);
        addSpacing(parent, 12);
        
        // Second row - Mock Testing
        LinearLayout mockTestRow = new LinearLayout(this);
        mockTestRow.setOrientation(LinearLayout.HORIZONTAL);
        mockTestRow.setWeightSum(2.0f);
        
        Button mockCallButton = createActionButton("Mock Scam Call", "#EF4444");
        mockCallButton.setOnClickListener(v -> runMockScamCall());
        mockTestRow.addView(mockCallButton);
        
        addHorizontalSpacing(mockTestRow, 12);
        
        Button mockSafeButton = createActionButton("Mock Safe Call", "#10B981");
        mockSafeButton.setOnClickListener(v -> runMockSafeCall());
        mockTestRow.addView(mockSafeButton);
        
        parent.addView(mockTestRow);
        addSpacing(parent, 12);
        
        // Third row - Advanced Testing
        LinearLayout advancedTestRow = new LinearLayout(this);
        advancedTestRow.setOrientation(LinearLayout.HORIZONTAL);
        advancedTestRow.setWeightSum(2.0f);
        
        Button patternTestButton = createActionButton("Test Patterns", "#F59E0B");
        patternTestButton.setOnClickListener(v -> testScamPatterns());
        advancedTestRow.addView(patternTestButton);
        
        addHorizontalSpacing(advancedTestRow, 12);
        
        Button clearLogsButton = createActionButton("Clear Logs", "#6B7280");
        clearLogsButton.setOnClickListener(v -> clearCallLogs());
        advancedTestRow.addView(clearLogsButton);
        
        parent.addView(advancedTestRow);
        addSpacing(parent, 16);
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
    
    private void createLanguageSelection() {
        // Create checkboxes for language selection
        englishCheckbox = new android.widget.CheckBox(this);
        englishCheckbox.setText("English (20 MB)");
        englishCheckbox.setTextColor(Color.parseColor("#374151"));
        englishCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            englishSelected = isChecked;
            updateDownloadButton();
        });
        modelDownloadCard.addView(englishCheckbox);
        
        hindiCheckbox = new android.widget.CheckBox(this);
        hindiCheckbox.setText("Hindi/हिंदी (25 MB)");
        hindiCheckbox.setTextColor(Color.parseColor("#374151"));
        hindiCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hindiSelected = isChecked;
            updateDownloadButton();
        });
        modelDownloadCard.addView(hindiCheckbox);
        
        teluguCheckbox = new android.widget.CheckBox(this);
        teluguCheckbox.setText("Telugu/తెలుగు (30 MB)");
        teluguCheckbox.setTextColor(Color.parseColor("#374151"));
        teluguCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            teluguSelected = isChecked;
            updateDownloadButton();
        });
        modelDownloadCard.addView(teluguCheckbox);
    }
    
    private void updateDownloadButton() {
        int totalSize = 0;
        int selectedCount = 0;
        
        if (englishSelected) {
            totalSize += 20;
            selectedCount++;
        }
        if (hindiSelected) {
            totalSize += 25;
            selectedCount++;
        }
        if (teluguSelected) {
            totalSize += 30;
            selectedCount++;
        }
        
        downloadSizeText.setText("Total download size: " + totalSize + " MB");
        
        if (selectedCount > 0) {
            downloadModelsButton.setEnabled(true);
            downloadModelsButton.setText("Download " + selectedCount + " Model(s) (" + totalSize + " MB)");
        } else {
            downloadModelsButton.setEnabled(false);
            downloadModelsButton.setText("Select Languages to Download");
        }
    }
    
    private void startSelectedModelsDownload() {
        if (!englishSelected && !hindiSelected && !teluguSelected) {
            addToCallLog("Please select at least one language to download");
            return;
        }
        
        downloadModelsButton.setEnabled(false);
        downloadModelsButton.setText("Downloading...");
        addToCallLog("Starting download for selected language models...");
        
        // Initialize VOSK with selected languages only
        if (voskRecognizer != null) {
            voskRecognizer.downloadSelectedModels(englishSelected, hindiSelected, teluguSelected);
        }
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
                // Fix: Change method call to match SimpleCallDetector implementation
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
                // Fix: Change method call to match SimpleCallDetector implementation
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
    
    // Implementation of CallDetectionListener interface method
    
    @Override
    public void onCallStateChanged(String state, String phoneNumber) {
        Log.d(TAG, "Call state changed: " + state + " for number: " + phoneNumber);
        
        // Handle different call states
        switch (state) {
            case "INCOMING_CALL_STARTED":
                onIncomingCallStarted(phoneNumber, System.currentTimeMillis() + "");
                break;
            case "INCOMING_CALL_ENDED":
                onIncomingCallEnded(phoneNumber, System.currentTimeMillis() + "");
                break;
            case "OUTGOING_CALL_STARTED":
                onOutgoingCallStarted(phoneNumber, System.currentTimeMillis() + "");
                break;
            case "OUTGOING_CALL_ENDED":
                onOutgoingCallEnded(phoneNumber, System.currentTimeMillis() + "");
                break;
            case "MISSED_CALL":
                onMissedCall(phoneNumber, System.currentTimeMillis() + "");
                break;
            case "CALL_STARTED":
                onCallStarted(phoneNumber);
                break;
            case "CALL_ENDED":
                onCallEnded(phoneNumber);
                break;
            default:
                Log.w(TAG, "Unknown call state: " + state);
        }
    }
    
    // Helper methods for different call states (no longer @Override)
    
    public void onCallStarted(String phoneNumber) {
        addToCallLog("Call started: " + phoneNumber);
        startCallRecording(phoneNumber);
    }
    
    public void onCallEnded(String phoneNumber) {
        addToCallLog("Call ended: " + phoneNumber);
        if (currentRecordingPath != null) {
            analyzeRecordingForScamsAI(currentRecordingPath, phoneNumber);
        }
    }
    
    public void onIncomingCallStarted(String number, String time) {
        Log.d(TAG, "Incoming call started: " + number + " at " + time);
        addToCallLog("Incoming call: " + number);
        startCallRecording(number);
    }
    
    public void onIncomingCallEnded(String number, String time) {
        Log.d(TAG, "Incoming call ended: " + number + " at " + time);
        addToCallLog("Call ended: " + number);
        if (currentRecordingPath != null) {
            analyzeRecordingForScamsAI(currentRecordingPath, number);
        }
    }
    
    public void onOutgoingCallStarted(String number, String time) {
        Log.d(TAG, "Outgoing call started: " + number + " at " + time);
        addToCallLog("Outgoing call: " + number);
        startCallRecording(number);
    }
    
    public void onOutgoingCallEnded(String number, String time) {
        Log.d(TAG, "Outgoing call ended: " + number + " at " + time);
        addToCallLog("Call ended: " + number);
        if (currentRecordingPath != null) {
            analyzeRecordingForScamsAI(currentRecordingPath, number);
        }
    }
    
    public void onMissedCall(String number, String time) {
        Log.d(TAG, "Missed call: " + number + " at " + time);
        addToCallLog("Missed call: " + number);
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
    protected void onResume() {
        super.onResume();
        // No need to automatically start monitoring here
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // No need to automatically stop monitoring here
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
    
    // Mock Testing Methods
    
    private void runMockScamCall() {
        addToCallLog("=== MOCK SCAM CALL TEST ===");
        
        // Simulate incoming scam call
        String mockScamNumber = "+1-800-SCAMMER";
        addToCallLog("Simulating incoming call from: " + mockScamNumber);
        
        // Simulate call start
        onCallStateChanged("INCOMING_CALL_STARTED", mockScamNumber);
        
        // Simulate scam conversation patterns
        String[] scamPhrases = {
            "Your social security number has been suspended",
            "This is IRS calling about tax fraud",
            "Your bank account will be frozen",
            "You have won a lottery prize of $50,000",
            "Pay immediately or face legal action"
        };
        
        // Test each phrase
        for (String phrase : scamPhrases) {
            addToCallLog("Mock Transcription: \"" + phrase + "\"");
            
            if (aiDetector != null) {
                // Test scam detection
                MultiLanguageScamDetector.ScamAnalysisResult result = aiDetector.analyzeText(phrase);
                int riskScore = result.getRiskScore();
                addToCallLog("Risk Analysis: " + riskScore + "% risk detected");
                updateRiskLevel(riskScore, "SCAM DETECTED: " + phrase.substring(0, Math.min(30, phrase.length())) + "...");
            }
            
            try {
                Thread.sleep(1500); // Pause between phrases
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Simulate call end
        addToCallLog("Mock call ended after 45 seconds");
        onCallStateChanged("INCOMING_CALL_ENDED", mockScamNumber);
        addToCallLog("=== MOCK TEST COMPLETED ===");
    }
    
    private void runMockSafeCall() {
        addToCallLog("=== MOCK SAFE CALL TEST ===");
        
        // Simulate safe call
        String mockSafeNumber = "+1-555-FRIEND";
        addToCallLog("Simulating safe call from: " + mockSafeNumber);
        
        // Simulate call start
        onCallStateChanged("INCOMING_CALL_STARTED", mockSafeNumber);
        
        // Simulate normal conversation
        String[] safePhrases = {
            "Hi, how are you doing?",
            "Do you want to meet for lunch today?",
            "I saw your message about the project",
            "The weather is really nice today",
            "Thanks for helping me yesterday"
        };
        
        // Test each phrase
        for (String phrase : safePhrases) {
            addToCallLog("Mock Transcription: \"" + phrase + "\"");
            
            if (aiDetector != null) {
                MultiLanguageScamDetector.ScamAnalysisResult result = aiDetector.analyzeText(phrase);
                int riskScore = result.getRiskScore();
                addToCallLog("Risk Analysis: " + riskScore + "% risk detected");
                updateRiskLevel(riskScore, "Normal conversation: " + phrase.substring(0, Math.min(30, phrase.length())) + "...");
            }
            
            try {
                Thread.sleep(1000); // Pause between phrases
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Simulate call end
        addToCallLog("Mock safe call ended after 30 seconds");
        onCallStateChanged("INCOMING_CALL_ENDED", mockSafeNumber);
        addToCallLog("=== SAFE CALL TEST COMPLETED ===");
    }
    
    private void testScamPatterns() {
        addToCallLog("=== TESTING SCAM PATTERNS ===");
        
        if (aiDetector == null) {
            addToCallLog("AI Detector not initialized!");
            return;
        }
        
        addToCallLog("Total patterns loaded: " + aiDetector.getPatternCount());
        
        // Test different language patterns
        String[] testPhrases = {
            // English scam patterns
            "You owe money to IRS pay now",
            "Your computer has virus call support",
            "Bank account compromised verify details",
            
            // Hindi patterns (romanized)
            "Aapka account band ho jayega",
            "Paisa bhejiye emergency hai",
            
            // Telugu patterns (romanized)  
            "Meeru money send cheyali",
            "Police case vestaru"
        };
        
        String[] languages = {"en", "en", "en", "hi", "hi", "te", "te"};
        
        for (int i = 0; i < testPhrases.length; i++) {
            String phrase = testPhrases[i];
            String lang = languages[i];
            
            addToCallLog("Testing (" + lang + "): \"" + phrase + "\"");
            MultiLanguageScamDetector.ScamAnalysisResult result = aiDetector.analyzeText(phrase);
            int riskScore = result.getRiskScore();
            
            String risk = "LOW";
            if (riskScore > 70) risk = "HIGH";
            else if (riskScore > 40) risk = "MEDIUM";
            
            addToCallLog("Result: " + riskScore + "% (" + risk + " RISK)");
            addToCallLog("---");
        }
        
        addToCallLog("=== PATTERN TESTING COMPLETED ===");
    }
    
    private void clearCallLogs() {
        callLogs.clear();
        addToCallLog("Call logs cleared at " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
        addToCallLog("Hello Hari ready for new session");
        
        // Reset risk level
        updateRiskLevel(0, "No active calls - Risk: 0%");
    }
}
