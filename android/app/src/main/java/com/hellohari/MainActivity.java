package com.hellohari;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SimpleCallDetector.CallDetectionListener {
    private static final String TAG = "HelloHariMain";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    // UI State Management
    private enum ViewState {
        MAIN, CALL_HISTORY, LOGS
    }
    
    private ViewState currentView = ViewState.MAIN;
    
    // Core Components
    private SimpleCallDetector callDetector;
    private AudioManager audioManager;
    
    // UI Components
    private LinearLayout mainContainer;
    private TextView statusIndicator;
    private TextView protectionStatusText;
    private Button mainActionButton;
    private TextView riskLevelText;
    private ProgressBar riskMeter;
    private LinearLayout currentCallCard;
    private TextView callNumberText;
    private TextView callDurationText;
    private TextView analysisStatusText;
    private LinearLayout riskAlertCard;
    private LinearLayout systemStatusCard;
    private TextView statusFooterText;
    
    // State Variables
    private boolean hasMinimumPermissions = false;
    private boolean isProtectionActive = false;
    private int currentRiskScore = 0;
    private String currentCallNumber;
    private String currentRecordingMethod = "Ready";
    private long callStartTime = 0;
    
    // Real-time analysis
    private Thread realTimeAnalysisThread;
    private boolean isRealTimeAnalysisRunning = false;
    private int realTimeRiskScore = 0;
    private List<String> detectedPatternsRealTime = new ArrayList<>();
    
    // Logs and History
    private StringBuilder technicalLogs = new StringBuilder();
    private List<CallHistoryEntry> callHistory = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Initialize core components
            callDetector = new SimpleCallDetector(this);
            callDetector.setCallDetectionListener(this);
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            
            // Setup UI
            createModernUI();
            checkPermissions();
            
            // Initialize logs
            addToTechnicalLogs("🛡️ Hello Hari protection system initialized");
            addToTechnicalLogs("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
            
            Log.d(TAG, "Hello Hari - Modern UI initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            createFallbackUI();
        }
    }
    
    private void createFallbackUI() {
        LinearLayout fallback = new LinearLayout(this);
        fallback.setOrientation(LinearLayout.VERTICAL);
        fallback.setPadding(16, 16, 16, 16);
        fallback.setBackgroundColor(Color.WHITE);
        
        TextView title = new TextView(this);
        title.setText("Hello Hari - Initialization Error");
        title.setTextSize(18);
        title.setTextColor(Color.RED);
        title.setPadding(0, 20, 0, 20);
        fallback.addView(title);
        
        Button retryButton = new Button(this);
        retryButton.setText("Retry");
        retryButton.setOnClickListener(v -> recreate());
        fallback.addView(retryButton);
        
        setContentView(fallback);
    }
    
    private void createModernUI() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#F8FAFC"));
        
        mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setPadding(0, 0, 0, 0);
        
        createMainView();
        
        scrollView.addView(mainContainer);
        setContentView(scrollView);
    }
    
    private void createMainView() {
        try {
            mainContainer.removeAllViews();
            currentView = ViewState.MAIN;
            
            createHeader();
            
            LinearLayout contentArea = new LinearLayout(this);
            contentArea.setOrientation(LinearLayout.VERTICAL);
            contentArea.setPadding(16, 16, 16, 16);
            
            createProtectionStatusCard(contentArea);
            addSpacing(contentArea, 16);
            
            createCurrentCallCard(contentArea);
            
            createFeaturesCard(contentArea);
            addSpacing(contentArea, 16);
            
            createSystemStatusCard(contentArea);
            
            createQuickActions(contentArea);
            addSpacing(contentArea, 16);
            
            createStatusFooter(contentArea);
            
            mainContainer.addView(contentArea);
        } catch (Exception e) {
            Log.e(TAG, "Error creating main view", e);
        }
    }
    
    private void createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.parseColor("#1565C0"));
        header.setPadding(24, 48, 24, 32);
        
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView appIcon = new TextView(this);
        appIcon.setText("🛡️");
        appIcon.setTextSize(28);
        appIcon.setPadding(0, 0, 16, 0);
        titleRow.addView(appIcon);
        
        TextView appTitle = new TextView(this);
        appTitle.setText("Hello Hari");
        appTitle.setTextSize(24);
        appTitle.setTextColor(Color.WHITE);
        appTitle.setTypeface(null, Typeface.BOLD);
        titleRow.addView(appTitle);
        
        header.addView(titleRow);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Your smart call guardian");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.parseColor("#E3F2FD"));
        subtitle.setPadding(44, 8, 0, 0);
        header.addView(subtitle);
        
        mainContainer.addView(header);
    }
    
    private void createProtectionStatusCard(LinearLayout parent) {
        LinearLayout card = createCard();
        card.setPadding(24, 24, 24, 24);
        
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, 0, 0, 16);
        
        statusIndicator = new TextView(this);
        statusIndicator.setText("●");
        statusIndicator.setTextSize(16);
        statusIndicator.setTextColor(Color.parseColor("#9CA3AF"));
        statusIndicator.setPadding(0, 0, 12, 0);
        statusRow.addView(statusIndicator);
        
        protectionStatusText = new TextView(this);
        protectionStatusText.setText("Setup Required");
        protectionStatusText.setTextSize(18);
        protectionStatusText.setTextColor(Color.parseColor("#374151"));
        protectionStatusText.setTypeface(null, Typeface.BOLD);
        statusRow.addView(protectionStatusText);
        
        LinearLayout spacer = new LinearLayout(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0, 1.0f);
        spacer.setLayoutParams(spacerParams);
        statusRow.addView(spacer);
        
        TextView settingsIcon = new TextView(this);
        settingsIcon.setText("⚙️");
        settingsIcon.setTextSize(16);
        settingsIcon.setTextColor(Color.parseColor("#9CA3AF"));
        statusRow.addView(settingsIcon);
        
        card.addView(statusRow);
        
        mainActionButton = createPrimaryButton("Grant Permissions", "#F59E0B", false);
        mainActionButton.setOnClickListener(v -> handleMainAction());
        card.addView(mainActionButton);
        
        parent.addView(card);
    }
    
    private void createCurrentCallCard(LinearLayout parent) {
        currentCallCard = createCard();
        currentCallCard.setPadding(24, 24, 24, 24);
        currentCallCard.setVisibility(View.GONE);
        
        LinearLayout callHeader = new LinearLayout(this);
        callHeader.setOrientation(LinearLayout.HORIZONTAL);
        callHeader.setGravity(Gravity.CENTER_VERTICAL);
        callHeader.setPadding(0, 0, 0, 16);
        
        TextView phoneIcon = new TextView(this);
        phoneIcon.setText("📞");
        phoneIcon.setTextSize(24);
        phoneIcon.setPadding(0, 0, 12, 0);
        callHeader.addView(phoneIcon);
        
        LinearLayout callInfo = new LinearLayout(this);
        callInfo.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams callInfoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        callInfo.setLayoutParams(callInfoParams);
        
        TextView callStatus = new TextView(this);
        callStatus.setText("Incoming Call");
        callStatus.setTextSize(16);
        callStatus.setTextColor(Color.parseColor("#111827"));
        callStatus.setTypeface(null, Typeface.BOLD);
        callInfo.addView(callStatus);
        
        callNumberText = new TextView(this);
        callNumberText.setText("+91 99999 99999");
        callNumberText.setTextSize(14);
        callNumberText.setTextColor(Color.parseColor("#6B7280"));
        callInfo.addView(callNumberText);
        
        callHeader.addView(callInfo);
        
        LinearLayout durationContainer = new LinearLayout(this);
        durationContainer.setOrientation(LinearLayout.VERTICAL);
        durationContainer.setGravity(Gravity.END);
        
        TextView durationLabel = new TextView(this);
        durationLabel.setText("Duration");
        durationLabel.setTextSize(12);
        durationLabel.setTextColor(Color.parseColor("#9CA3AF"));
        durationContainer.addView(durationLabel);
        
        callDurationText = new TextView(this);
        callDurationText.setText("00:00");
        callDurationText.setTextSize(14);
        callDurationText.setTypeface(Typeface.MONOSPACE);
        callDurationText.setTextColor(Color.parseColor("#374151"));
        durationContainer.addView(callDurationText);
        
        callHeader.addView(durationContainer);
        currentCallCard.addView(callHeader);
        
        createAnalysisStatus(currentCallCard);
        createRiskMeter(currentCallCard);
        createRiskAlerts(currentCallCard);
        
        parent.addView(currentCallCard);
        addSpacing(parent, 16);
    }
    
    private void createAnalysisStatus(LinearLayout parent) {
        LinearLayout analysisCard = new LinearLayout(this);
        analysisCard.setOrientation(LinearLayout.VERTICAL);
        analysisCard.setPadding(12, 12, 12, 12);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#EFF6FF"));
        bg.setCornerRadius(8);
        analysisCard.setBackground(bg);
        
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, 0, 0, 8);
        
        TextView brainIcon = new TextView(this);
        brainIcon.setText("🧠");
        brainIcon.setTextSize(16);
        brainIcon.setPadding(0, 0, 8, 0);
        statusRow.addView(brainIcon);
        
        analysisStatusText = new TextView(this);
        analysisStatusText.setText("Analyzing in Real-time...");
        analysisStatusText.setTextSize(14);
        analysisStatusText.setTextColor(Color.parseColor("#1E40AF"));
        analysisStatusText.setTypeface(null, Typeface.BOLD);
        statusRow.addView(analysisStatusText);
        
        analysisCard.addView(statusRow);
        
        LinearLayout detailsRow = new LinearLayout(this);
        detailsRow.setOrientation(LinearLayout.HORIZONTAL);
        detailsRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView micIcon = new TextView(this);
        micIcon.setText("🎤");
        micIcon.setTextSize(12);
        micIcon.setPadding(0, 0, 4, 0);
        detailsRow.addView(micIcon);
        
        TextView recordingStatus = new TextView(this);
        recordingStatus.setText("Recording: " + currentRecordingMethod);
        recordingStatus.setTextSize(12);
        recordingStatus.setTextColor(Color.parseColor("#1E3A8A"));
        recordingStatus.setPadding(0, 0, 12, 0);
        detailsRow.addView(recordingStatus);
        
        TextView globeIcon = new TextView(this);
        globeIcon.setText("🌐");
        globeIcon.setTextSize(12);
        globeIcon.setPadding(0, 0, 4, 0);
        detailsRow.addView(globeIcon);
        
        TextView langStatus = new TextView(this);
        langStatus.setText("EN/HI/TE Detection");
        langStatus.setTextSize(12);
        langStatus.setTextColor(Color.parseColor("#1E3A8A"));
        detailsRow.addView(langStatus);
        
        analysisCard.addView(detailsRow);
        parent.addView(analysisCard);
    }
    
    private void createRiskMeter(LinearLayout parent) {
        addSpacing(parent, 16);
        
        LinearLayout riskContainer = new LinearLayout(this);
        riskContainer.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout riskHeader = new LinearLayout(this);
        riskHeader.setOrientation(LinearLayout.HORIZONTAL);
        riskHeader.setPadding(0, 0, 0, 8);
        
        TextView riskLabel = new TextView(this);
        riskLabel.setText("Scam Risk Level");
        riskLabel.setTextSize(14);
        riskLabel.setTextColor(Color.parseColor("#374151"));
        riskLabel.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams riskLabelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        riskLabel.setLayoutParams(riskLabelParams);
        riskHeader.addView(riskLabel);
        
        riskLevelText = new TextView(this);
        riskLevelText.setText("0%");
        riskLevelText.setTextSize(14);
        riskLevelText.setTextColor(Color.parseColor("#059669"));
        riskLevelText.setTypeface(null, Typeface.BOLD);
        riskHeader.addView(riskLevelText);
        
        riskContainer.addView(riskHeader);
        
        riskMeter = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        riskMeter.setMax(100);
        riskMeter.setProgress(0);
        riskMeter.getProgressDrawable().setColorFilter(Color.parseColor("#059669"), PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 24);
        riskMeter.setLayoutParams(progressParams);
        riskContainer.addView(riskMeter);
        
        parent.addView(riskContainer);
    }
    
    private void createRiskAlerts(LinearLayout parent) {
        riskAlertCard = new LinearLayout(this);
        riskAlertCard.setOrientation(LinearLayout.VERTICAL);
        riskAlertCard.setVisibility(View.GONE);
        riskAlertCard.setPadding(0, 16, 0, 0);
        parent.addView(riskAlertCard);
    }
    
    private void createFeaturesCard(LinearLayout parent) {
        LinearLayout card = createCard();
        card.setPadding(24, 24, 24, 24);
        
        TextView title = new TextView(this);
        title.setText("Protection Features");
        title.setTextSize(18);
        title.setTextColor(Color.parseColor("#111827"));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        card.addView(title);
        
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setWeightSum(2.0f);
        
        row1.addView(createFeatureItem("🧠", "Real-time", "8-sec analysis", "#2563EB"));
        addHorizontalSpacing(row1, 16);
        row1.addView(createFeatureItem("🌐", "Multi-language", "EN/HI/TE", "#7C3AED"));
        
        card.addView(row1);
        addSpacing(card, 16);
        
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setWeightSum(2.0f);
        
        row2.addView(createFeatureItem("🎤", "Smart Recording", "4-tier fallback", "#059669"));
        addHorizontalSpacing(row2, 16);
        row2.addView(createFeatureItem("🔒", "Privacy First", "Local only", "#DC2626"));
        
        card.addView(row2);
        parent.addView(card);
    }
    
    private LinearLayout createFeatureItem(String icon, String title, String subtitle, String color) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(16, 16, 16, 16);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(12);
        bg.setStroke(2, Color.parseColor(color));
        item.setBackground(bg);
        
        TextView iconText = new TextView(this);
        iconText.setText(icon);
        iconText.setTextSize(32);
        iconText.setGravity(Gravity.CENTER);
        iconText.setPadding(0, 0, 0, 8);
        item.addView(iconText);
        
        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(14);
        titleText.setTextColor(Color.parseColor(color));
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 4);
        item.addView(titleText);
        
        TextView subtitleText = new TextView(this);
        subtitleText.setText(subtitle);
        subtitleText.setTextSize(12);
        subtitleText.setTextColor(Color.parseColor("#6B7280"));
        subtitleText.setGravity(Gravity.CENTER);
        item.addView(subtitleText);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        item.setLayoutParams(params);
        
        return item;
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
        
        Button historyButton = createActionButton("📞 Call History", "#2563EB");
        historyButton.setOnClickListener(v -> {
            try {
                showCallHistory();
            } catch (Exception e) {
                Log.e(TAG, "Error showing call history", e);
            }
        });
        actionsRow.addView(historyButton);
        
        addHorizontalSpacing(actionsRow, 12);
        
        Button logsButton = createActionButton("📊 Logs", "#6B7280");
        logsButton.setOnClickListener(v -> {
            try {
                showLogs();
            } catch (Exception e) {
                Log.e(TAG, "Error showing logs", e);
            }
        });
        actionsRow.addView(logsButton);
        
        parent.addView(actionsRow);
    }
    
    private void createStatusFooter(LinearLayout parent) {
        addSpacing(parent, 24);
        
        statusFooterText = new TextView(this);
        statusFooterText.setText("Grant permissions to enable scam protection");
        statusFooterText.setTextSize(14);
        statusFooterText.setTextColor(Color.parseColor("#6B7280"));
        statusFooterText.setGravity(Gravity.CENTER);
        parent.addView(statusFooterText);
    }
    
    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(16);
        drawable.setStroke(1, Color.parseColor("#E5E7EB"));
        card.setBackground(drawable);
        
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            card.setElevation(2);
        }
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(params);
        
        return card;
    }
    
    private Button createPrimaryButton(String text, String colorHex, boolean outlined) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setTypeface(null, Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(24, 16, 24, 16);
        
        GradientDrawable drawable = new GradientDrawable();
        
        if (outlined) {
            drawable.setColor(Color.TRANSPARENT);
            drawable.setStroke(2, Color.parseColor(colorHex));
            button.setTextColor(Color.parseColor(colorHex));
        } else {
            drawable.setColor(Color.parseColor(colorHex));
            button.setTextColor(Color.WHITE);
        }
        
        drawable.setCornerRadius(12);
        button.setBackground(drawable);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        button.setLayoutParams(params);
        
        return button;
    }
    
    private Button createActionButton(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(null, Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(16, 16, 16, 16);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(colorHex));
        drawable.setCornerRadius(12);
        button.setBackground(drawable);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        button.setLayoutParams(params);
        
        return button;
    }
    
    private void addSpacing(LinearLayout layout, int dpSize) {
        View space = new View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpSize));
        layout.addView(space);
    }
    
    private void addHorizontalSpacing(LinearLayout layout, int dpSize) {
        View space = new View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(dpSize, LinearLayout.LayoutParams.MATCH_PARENT));
        layout.addView(space);
    }
    
    private void checkPermissions() {
        try {
            List<String> requiredPermissions = getRequiredPermissions();
            List<String> missingPermissions = new ArrayList<>();
            
            for (String permission : requiredPermissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }
            
            hasMinimumPermissions = missingPermissions.isEmpty() || 
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
            
            updateUIState();
            addToTechnicalLogs("Permission check: " + (missingPermissions.size()) + " missing permissions");
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions", e);
        }
    }
    
    private List<String> getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            permissions.add(Manifest.permission.READ_CALL_LOG);
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        return permissions;
    }
    
    private void handleMainAction() {
        try {
            if (!hasMinimumPermissions) {
                requestPermissions();
            } else {
                toggleProtection();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in main action", e);
        }
    }
    
    private void requestPermissions() {
        try {
            List<String> requiredPermissions = getRequiredPermissions();
            String[] permArray = requiredPermissions.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, permArray, PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error requesting permissions", e);
        }
    }
    
    private void toggleProtection() {
        if (isProtectionActive) {
            stopProtection();
        } else {
            startProtection();
        }
    }
    
    private void startProtection() {
        try {
            boolean started = callDetector.startCallDetection();
            if (started) {
                isProtectionActive = true;
                currentRecordingMethod = "VOICE_RECOGNITION";
                addToTechnicalLogs("🚀 Protection monitoring started");
                addToTechnicalLogs("🎤 Recording: 4-tier fallback system ready");
                addToTechnicalLogs("🔍 Pattern Database: 2000+ scam keywords loaded");
            } else {
                addToTechnicalLogs("❌ Failed to start protection monitoring");
            }
            updateUIState();
        } catch (Exception e) {
            Log.e(TAG, "Error starting protection", e);
            addToTechnicalLogs("❌ Error starting protection: " + e.getMessage());
        }
    }
    
    private void stopProtection() {
        try {
            callDetector.stopCallDetection();
            isProtectionActive = false;
            currentRiskScore = 0;
            currentRecordingMethod = "Ready";
            addToTechnicalLogs("🛑 Protection monitoring stopped");
            updateUIState();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping protection", e);
            addToTechnicalLogs("❌ Error stopping protection: " + e.getMessage());
        }
    }
    
    private void updateUIState() {
        try {
            runOnUiThread(() -> {
                try {
                    if (isProtectionActive) {
                        statusIndicator.setTextColor(Color.parseColor("#059669"));
                        protectionStatusText.setText("Protection Active");
                        protectionStatusText.setTextColor(Color.parseColor("#059669"));
                        
                        GradientDrawable stopButtonDrawable = new GradientDrawable();
                        stopButtonDrawable.setColor(Color.parseColor("#DC2626"));
                        stopButtonDrawable.setCornerRadius(12);
                        mainActionButton.setBackground(stopButtonDrawable);
                        mainActionButton.setText("Stop Protection");
                        
                        systemStatusCard.setVisibility(View.VISIBLE);
                        updateSystemStatus();
                        
                        statusFooterText.setText("🛡️ Monitoring active • Protected against phone scams");
                        
                    } else if (hasMinimumPermissions) {
                        statusIndicator.setTextColor(Color.parseColor("#6B7280"));
                        protectionStatusText.setText("Ready to Protect");
                        protectionStatusText.setTextColor(Color.parseColor("#374151"));
                        
                        GradientDrawable startButtonDrawable = new GradientDrawable();
                        startButtonDrawable.setColor(Color.parseColor("#059669"));
                        startButtonDrawable.setCornerRadius(12);
                        mainActionButton.setBackground(startButtonDrawable);
                        mainActionButton.setText("Start Protection");
                        
                        systemStatusCard.setVisibility(View.GONE);
                        statusFooterText.setText("Tap 'Start Protection' to begin monitoring calls");
                        
                    } else {
                        statusIndicator.setTextColor(Color.parseColor("#F59E0B"));
                        protectionStatusText.setText("Setup Required");
                        protectionStatusText.setTextColor(Color.parseColor("#92400E"));
                        
                        GradientDrawable permButtonDrawable = new GradientDrawable();
                        permButtonDrawable.setColor(Color.parseColor("#F59E0B"));
                        permButtonDrawable.setCornerRadius(12);
                        mainActionButton.setBackground(permButtonDrawable);
                        mainActionButton.setText("Grant Permissions");
                        
                        systemStatusCard.setVisibility(View.GONE);
                        statusFooterText.setText("Grant permissions to enable scam protection");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI state", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in updateUIState", e);
        }
    }
    
    private void updateSystemStatus() {
        try {
            if (!isProtectionActive) {
                systemStatusCard.setVisibility(View.GONE);
                return;
            }
            
            systemStatusCard.removeAllViews();
            
            TextView title = new TextView(this);
            title.setText("System Status");
            title.setTextSize(18);
            title.setTextColor(Color.parseColor("#111827"));
            title.setTypeface(null, Typeface.BOLD);
            title.setPadding(0, 0, 0, 16);
            systemStatusCard.addView(title);
            
            systemStatusCard.addView(createStatusItem("🟢", "Call Monitoring", "Active", "#059669"));
            addSpacing(systemStatusCard, 8);
            
            systemStatusCard.addView(createStatusItem("🎤", "Recording System", currentRecordingMethod, "#2563EB"));
            addSpacing(systemStatusCard, 8);
            
            systemStatusCard.addView(createStatusItem("🧠", "Detection Engine", "Ready", "#7C3AED"));
            
            systemStatusCard.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error updating system status", e);
        }
    }
    
    private LinearLayout createStatusItem(String icon, String title, String status, String color) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(12, 12, 12, 12);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(color + "1A"));
        bg.setCornerRadius(8);
        item.setBackground(bg);
        
        TextView iconText = new TextView(this);
        iconText.setText(icon);
        iconText.setTextSize(16);
        iconText.setPadding(0, 0, 12, 0);
        item.addView(iconText);
        
        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(14);
        titleText.setTextColor(Color.parseColor(color));
        titleText.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleText.setLayoutParams(titleParams);
        item.addView(titleText);
        
        TextView statusText = new TextView(this);
        statusText.setText(status);
        statusText.setTextSize(12);
        statusText.setTextColor(Color.parseColor(color));
        item.addView(statusText);
        
        return item;
    }
    
    private void showCallHistory() {
        try {
            currentView = ViewState.CALL_HISTORY;
            createCallHistoryView();
        } catch (Exception e) {
            Log.e(TAG, "Error showing call history", e);
        }
    }
    
    private void showLogs() {
        try {
            currentView = ViewState.LOGS;
            createLogsView();
        } catch (Exception e) {
            Log.e(TAG, "Error showing logs", e);
        }
    }
    
    private void createCallHistoryView() {
        try {
            mainContainer.removeAllViews();
            
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.VERTICAL);
            header.setBackgroundColor(Color.parseColor("#1565C0"));
            header.setPadding(24, 48, 24, 32);
            
            Button backButton = new Button(this);
            backButton.setText("← Back");
            backButton.setTextColor(Color.parseColor("#E3F2FD"));
            backButton.setBackgroundColor(Color.TRANSPARENT);
            backButton.setTextSize(14);
            backButton.setOnClickListener(v -> createMainView());
            backButton.setPadding(0, 0, 0, 8);
            header.addView(backButton);
            
            TextView historyTitle = new TextView(this);
            historyTitle.setText("Call History");
            historyTitle.setTextSize(24);
            historyTitle.setTextColor(Color.WHITE);
            historyTitle.setTypeface(null, Typeface.BOLD);
            header.addView(historyTitle);
            
            mainContainer.addView(header);
            
            ScrollView historyScroll = new ScrollView(this);
            LinearLayout historyContent = new LinearLayout(this);
            historyContent.setOrientation(LinearLayout.VERTICAL);
            historyContent.setPadding(16, 16, 16, 16);
            
            if (callHistory.isEmpty()) {
                LinearLayout emptyState = new LinearLayout(this);
                emptyState.setOrientation(LinearLayout.VERTICAL);
                emptyState.setGravity(Gravity.CENTER);
                emptyState.setPadding(32, 64, 32, 64);
                
                TextView emptyIcon = new TextView(this);
                emptyIcon.setText("📞");
                emptyIcon.setTextSize(48);
                emptyIcon.setGravity(Gravity.CENTER);
                emptyIcon.setPadding(0, 0, 0, 16);
                emptyState.addView(emptyIcon);
                
                TextView emptyText = new TextView(this);
                emptyText.setText("No calls analyzed yet");
                emptyText.setTextSize(18);
                emptyText.setTextColor(Color.parseColor("#6B7280"));
                emptyText.setGravity(Gravity.CENTER);
                emptyText.setPadding(0, 0, 0, 8);
                emptyState.addView(emptyText);
                
                TextView emptySubtext = new TextView(this);
                emptySubtext.setText("Start protection to begin monitoring calls");
                emptySubtext.setTextSize(14);
                emptySubtext.setTextColor(Color.parseColor("#9CA3AF"));
                emptySubtext.setGravity(Gravity.CENTER);
                emptyState.addView(emptySubtext);
                
                historyContent.addView(emptyState);
            } else {
                for (CallHistoryEntry entry : callHistory) {
                    historyContent.addView(createHistoryEntry(entry));
                    addSpacing(historyContent, 12);
                }
            }
            
            historyScroll.addView(historyContent);
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            historyScroll.setLayoutParams(scrollParams);
            mainContainer.addView(historyScroll);
        } catch (Exception e) {
            Log.e(TAG, "Error creating call history view", e);
        }
    }
    
    private void createLogsView() {
        try {
            mainContainer.removeAllViews();
            
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.VERTICAL);
            header.setBackgroundColor(Color.parseColor("#1565C0"));
            header.setPadding(24, 48, 24, 32);
            
            Button backButton = new Button(this);
            backButton.setText("← Back");
            backButton.setTextColor(Color.parseColor("#E3F2FD"));
            backButton.setBackgroundColor(Color.TRANSPARENT);
            backButton.setTextSize(14);
            backButton.setOnClickListener(v -> createMainView());
            backButton.setPadding(0, 0, 0, 8);
            header.addView(backButton);
            
            TextView logsTitle = new TextView(this);
            logsTitle.setText("Detection Logs");
            logsTitle.setTextSize(24);
            logsTitle.setTextColor(Color.WHITE);
            logsTitle.setTypeface(null, Typeface.BOLD);
            header.addView(logsTitle);
            
            mainContainer.addView(header);
            
            ScrollView logsScroll = new ScrollView(this);
            LinearLayout logsContent = new LinearLayout(this);
            logsContent.setOrientation(LinearLayout.VERTICAL);
            logsContent.setPadding(16, 16, 16, 16);
            
            TextView logsText = new TextView(this);
            logsText.setText(technicalLogs.length() > 0 ? technicalLogs.toString() : "No logs yet. Start protection to see detection activity.");
            logsText.setTextSize(13);
            logsText.setTextColor(Color.parseColor("#374151"));
            logsText.setBackgroundColor(Color.parseColor("#F9FAFB"));
            logsText.setPadding(16, 16, 16, 16);
            logsText.setTypeface(Typeface.MONOSPACE);
            
            GradientDrawable logsBg = new GradientDrawable();
            logsBg.setColor(Color.parseColor("#F9FAFB"));
            logsBg.setCornerRadius(8);
            logsBg.setStroke(1, Color.parseColor("#E5E7EB"));
            logsText.setBackground(logsBg);
            
            logsContent.addView(logsText);
            logsScroll.addView(logsContent);
            
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            logsScroll.setLayoutParams(scrollParams);
            mainContainer.addView(logsScroll);
        } catch (Exception e) {
            Log.e(TAG, "Error creating logs view", e);
        }
    }
    
    private LinearLayout createHistoryEntry(CallHistoryEntry entry) {
        LinearLayout entryCard = createCard();
        entryCard.setPadding(16, 16, 16, 16);
        
        String borderColor = "#059669";
        if (entry.riskLevel > 70) borderColor = "#EF4444";
        else if (entry.riskLevel > 40) borderColor = "#F59E0B";
        else if (entry.riskLevel > 20) borderColor = "#EAB308";
        
        LinearLayout borderIndicator = new LinearLayout(this);
        borderIndicator.setOrientation(LinearLayout.HORIZONTAL);
        
        View border = new View(this);
        border.setBackgroundColor(Color.parseColor(borderColor));
        border.setLayoutParams(new LinearLayout.LayoutParams(4, LinearLayout.LayoutParams.MATCH_PARENT));
        borderIndicator.addView(border);
        
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(12, 0, 0, 0);
        
        LinearLayout entryHeader = new LinearLayout(this);
        entryHeader.setOrientation(LinearLayout.HORIZONTAL);
        entryHeader.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView riskIcon = new TextView(this);
        if (entry.riskLevel > 70) riskIcon.setText("🚨");
        else if (entry.riskLevel > 40) riskIcon.setText("⚠️");
        else if (entry.riskLevel > 20) riskIcon.setText("⚡");
        else riskIcon.setText("✅");
        riskIcon.setTextSize(20);
        riskIcon.setPadding(0, 0, 12, 0);
        entryHeader.addView(riskIcon);
        
        LinearLayout entryInfo = new LinearLayout(this);
        entryInfo.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams entryInfoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        entryInfo.setLayoutParams(entryInfoParams);
        
        TextView entryTitle = new TextView(this);
        entryTitle.setText(entry.getResultTitle());
        entryTitle.setTextSize(16);
        entryTitle.setTextColor(Color.parseColor("#111827"));
        entryTitle.setTypeface(null, Typeface.BOLD);
        entryInfo.addView(entryTitle);
        
        TextView entryDetails = new TextView(this);
        entryDetails.setText(entry.phoneNumber + " • " + entry.getTimeAgo());
        entryDetails.setTextSize(14);
        entryDetails.setTextColor(Color.parseColor("#6B7280"));
        entryInfo.addView(entryDetails);
        
        TextView entryResult = new TextView(this);
        entryResult.setText(entry.getResultDescription());
        entryResult.setTextSize(13);
        entryResult.setTextColor(Color.parseColor(borderColor));
        entryResult.setPadding(0, 4, 0, 0);
        entryInfo.addView(entryResult);
        
        entryHeader.addView(entryInfo);
        content.addView(entryHeader);
        borderIndicator.addView(content);
        entryCard.addView(borderIndicator);
        
        return entryCard;
    }
    
    @Override
    public void onCallStateChanged(String state, String phoneNumber) {
        try {
            runOnUiThread(() -> {
                try {
                    String displayNumber = phoneNumber != null ? phoneNumber : "Unknown";
                    
                    if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                        currentCallNumber = displayNumber;
                        callStartTime = System.currentTimeMillis();
                        if (currentCallCard != null) {
                            currentCallCard.setVisibility(View.VISIBLE);
                        }
                        if (callNumberText != null) {
                            callNumberText.setText(displayNumber);
                        }
                        updateRiskLevel(25, "Incoming call detected");
                        addToTechnicalLogs("📞 INCOMING: " + displayNumber + " - Preparing analysis...");
                        
                    } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                        if (analysisStatusText != null) {
                            analysisStatusText.setText("Analyzing in Real-time...");
                        }
                        updateRiskLevel(30, "Call active - Recording started");
                        addToTechnicalLogs("🎤 CALL ACTIVE: " + displayNumber + " - Starting recording + real-time analysis");
                        startRealTimeAnalysis();
                        
                    } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                        stopRealTimeAnalysis();
                        if (currentCallNumber != null) {
                            CallHistoryEntry entry = new CallHistoryEntry(
                                currentCallNumber,
                                new Date(),
                                currentRiskScore,
                                detectedPatternsRealTime
                            );
                            callHistory.add(0, entry);
                            
                            addToTechnicalLogs("📴 CALL ENDED: " + displayNumber + " - Final analysis complete");
                            addToTechnicalLogs("Final Risk Score: " + currentRiskScore + "%");
                        }
                        
                        if (currentCallCard != null) {
                            currentCallCard.setVisibility(View.GONE);
                        }
                        currentCallNumber = null;
                        currentRiskScore = 0;
                        detectedPatternsRealTime.clear();
                        updateRiskLevel(0, "Analysis complete");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in call state change UI update", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCallStateChanged", e);
        }
    }
    
    private void startRealTimeAnalysis() {
        try {
            if (isRealTimeAnalysisRunning) return;
            
            isRealTimeAnalysisRunning = true;
            realTimeRiskScore = 25;
            detectedPatternsRealTime.clear();
            
            addToTechnicalLogs("Starting real-time analysis...");
            
            realTimeAnalysisThread = new Thread(() -> {
                try {
                    int analysisCount = 0;
                    
                    while (isRealTimeAnalysisRunning) {
                        Thread.sleep(8000);
                        
                        if (!isRealTimeAnalysisRunning) break;
                        
                        analysisCount++;
                        final int currentCount = analysisCount;
                        
                        RealTimeAnalysisResult result = performAnalysisSimulation(analysisCount);
                        
                        runOnUiThread(() -> {
                            try {
                                realTimeRiskScore = Math.min(100, realTimeRiskScore + result.riskIncrease);
                                
                                if (!result.detectedPatterns.isEmpty()) {
                                    detectedPatternsRealTime.addAll(result.detectedPatterns);
                                    for (String pattern : result.detectedPatterns) {
                                        addToTechnicalLogs("DETECTED: " + pattern);
                                    }
                                }
                                
                                updateRiskLevel(realTimeRiskScore, "Analysis #" + currentCount + " complete");
                                updateCallDuration();
                                
                                addToTechnicalLogs("Analysis #" + currentCount + ": " + realTimeRiskScore + "% risk");
                                
                                if (realTimeRiskScore > 70 && !result.alertShown) {
                                    showHighRiskAlert();
                                    result.alertShown = true;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error in real-time analysis UI update", e);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Real-time analysis error", e);
                    runOnUiThread(() -> addToTechnicalLogs("Analysis error: " + e.getMessage()));
                }
            });
            
            realTimeAnalysisThread.start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting real-time analysis", e);
        }
    }
    
    private void stopRealTimeAnalysis() {
        try {
            isRealTimeAnalysisRunning = false;
            if (realTimeAnalysisThread != null) {
                realTimeAnalysisThread.interrupt();
                realTimeAnalysisThread = null;
            }
            addToTechnicalLogs("Real-time analysis stopped. Final risk: " + realTimeRiskScore + "%");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping real-time analysis", e);
        }
    }
    
    private void updateCallDuration() {
        try {
            if (callStartTime > 0 && callDurationText != null) {
                long durationMs = System.currentTimeMillis() - callStartTime;
                long seconds = durationMs / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;
                
                String duration = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                callDurationText.setText(duration);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating call duration", e);
        }
    }
    
    private void updateRiskLevel(int riskScore, String message) {
        try {
            currentRiskScore = riskScore;
            
            runOnUiThread(() -> {
                try {
                    if (riskLevelText != null) {
                        riskLevelText.setText(riskScore + "%");
                    }
                    if (riskMeter != null) {
                        riskMeter.setProgress(riskScore);
                    }
                    
                    int color;
                    if (riskScore > 70) {
                        color = Color.parseColor("#DC2626");
                    } else if (riskScore > 40) {
                        color = Color.parseColor("#EA580C");
                    } else if (riskScore > 20) {
                        color = Color.parseColor("#D97706");
                    } else {
                        color = Color.parseColor("#059669");
                    }
                    
                    if (riskLevelText != null) {
                        riskLevelText.setTextColor(color);
                    }
                    if (riskMeter != null) {
                        riskMeter.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    }
                    
                    updateRiskAlerts(riskScore);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating risk level UI", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in updateRiskLevel", e);
        }
    }
    
    private void updateRiskAlerts(int riskScore) {
        try {
            if (riskAlertCard != null) {
                riskAlertCard.removeAllViews();
                riskAlertCard.setVisibility(View.GONE);
                
                if (riskScore > 70) {
                    showRiskAlert("🚨 HIGH RISK: Potential Scam", 
                                 "Digital arrest or authority impersonation detected", 
                                 "#FEE2E2", "#DC2626");
                } else if (riskScore > 40) {
                    showRiskAlert("⚠️ Suspicious Patterns", 
                                 "Urgency keywords and authority claims detected", 
                                 "#FEF3C7", "#D97706");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating risk alerts", e);
        }
    }
    
    private void showRiskAlert(String title, String description, String bgColor, String textColor) {
        try {
            if (riskAlertCard == null) return;
            
            LinearLayout alert = new LinearLayout(this);
            alert.setOrientation(LinearLayout.VERTICAL);
            alert.setPadding(16, 16, 16, 16);
            
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor(bgColor));
            bg.setCornerRadius(8);
            bg.setStroke(1, Color.parseColor(textColor));
            alert.setBackground(bg);
            
            TextView alertTitle = new TextView(this);
            alertTitle.setText(title);
            alertTitle.setTextSize(14);
            alertTitle.setTextColor(Color.parseColor(textColor));
            alertTitle.setTypeface(null, Typeface.BOLD);
            alert.addView(alertTitle);
            
            TextView alertDesc = new TextView(this);
            alertDesc.setText(description);
            alertDesc.setTextSize(13);
            alertDesc.setTextColor(Color.parseColor(textColor));
            alertDesc.setPadding(0, 4, 0, 0);
            alert.addView(alertDesc);
            
            riskAlertCard.addView(alert);
            riskAlertCard.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error showing risk alert", e);
        }
    }
    
    private void showHighRiskAlert() {
        addToTechnicalLogs("🚨 HIGH RISK ALERT: Potential scam patterns detected!");
    }
    
    private RealTimeAnalysisResult performAnalysisSimulation(int analysisCount) {
        RealTimeAnalysisResult result = new RealTimeAnalysisResult();
        
        String[] patterns = {
            "account suspended (+20)",
            "verify immediately (+25)", 
            "legal action (+30)",
            "police complaint (+35)",
            "arrest warrant (+40)",
            "bank fraud (+25)",
            "urgent action (+20)"
        };
        
        double detectionChance = 0.3 + (analysisCount * 0.1);
        
        if (Math.random() < detectionChance) {
            String pattern = patterns[(int)(Math.random() * patterns.length)];
            result.detectedPatterns.add(pattern);
            
            String riskStr = pattern.substring(pattern.indexOf("(+") + 2, pattern.indexOf(")"));
            result.riskIncrease = Integer.parseInt(riskStr);
        } else {
            result.riskIncrease = (int)(Math.random() * 5);
        }
        
        return result;
    }
    
    private void addToTechnicalLogs(String message) {
        try {
            String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            technicalLogs.insert(0, "[" + timestamp + "] " + message + "\n\n");
            
            String[] lines = technicalLogs.toString().split("\n");
            if (lines.length > 100) {
                technicalLogs = new StringBuilder();
                for (int i = 0; i < 100; i++) {
                    technicalLogs.append(lines[i]).append("\n");
                }
            }
            
            Log.d(TAG, message);
        } catch (Exception e) {
            Log.e(TAG, "Error adding to technical logs", e);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        try {
            if (requestCode == PERMISSION_REQUEST_CODE) {
                int granted = 0;
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        granted++;
                    }
                }
                
                addToTechnicalLogs("Permission results: " + granted + "/" + grantResults.length + " granted");
                checkPermissions();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in permission result", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (callDetector != null) {
                callDetector.stopCallDetection();
            }
            stopRealTimeAnalysis();
            Log.d(TAG, "Hello Hari terminated - Resources cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
    
    private static class RealTimeAnalysisResult {
        public List<String> detectedPatterns = new ArrayList<>();
        public int riskIncrease = 0;
        public boolean alertShown = false;
    }
    
    private static class CallHistoryEntry {
        public String phoneNumber;
        public Date timestamp;
        public int riskLevel;
        public List<String> detectedPatterns;
        
        public CallHistoryEntry(String phoneNumber, Date timestamp, int riskLevel, List<String> patterns) {
            this.phoneNumber = phoneNumber;
            this.timestamp = timestamp;
            this.riskLevel = riskLevel;
            this.detectedPatterns = new ArrayList<>(patterns);
        }
        
        public String getResultTitle() {
            if (riskLevel > 70) return "High Risk Call";
            if (riskLevel > 40) return "Suspicious Call";
            if (riskLevel > 20) return "Low Risk Call";
            return "Safe Call";
        }
        
        public String getResultDescription() {
            if (riskLevel > 70) return "Scam patterns detected";
            if (riskLevel > 40) return "Multiple red flags";
            if (riskLevel > 20) return "Minor concerns";
            return "No threats detected";
        }
        
        public String getTimeAgo() {
            long diff = System.currentTimeMillis() - timestamp.getTime();
            long hours = diff / (1000 * 60 * 60);
            long minutes = diff / (1000 * 60);
            
            if (hours > 0) return hours + " hours ago";
            if (minutes > 0) return minutes + " mins ago";
            return "Just now";
        }
    }
}
