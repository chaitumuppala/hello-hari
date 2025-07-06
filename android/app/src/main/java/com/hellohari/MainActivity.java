package com.hellohari;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ProgressBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements EnhancedCallDetector.CallDetectionListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private EnhancedCallDetector callDetector;
    private TextView statusText;
    private TextView callLogText;
    private TextView recordingStatusText;
    private TextView riskLevelText;
    private Button monitorButton;
    private Button permissionButton;
    private ProgressBar riskMeter;
    private StringBuilder callLog;
    
    private boolean hasAllPermissions = false;
    private boolean isRecording = false;
    private int currentRiskScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        callLog = new StringBuilder();
        callDetector = new EnhancedCallDetector(this);
        callDetector.setCallDetectionListener(this);
        
        createEnhancedUI();
        checkPermissions();
        
        Log.d(TAG, "Hello Hari Enhanced MainActivity created");
    }

    private void createEnhancedUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.parseColor("#F5F6FA"));
        
        // Header
        TextView title = new TextView(this);
        title.setText("🛡️ Hello Hari (HH)");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#2E3192"));
        title.setPadding(0, 0, 0, 10);
        layout.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Advanced Call Safety with Recording");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.parseColor("#666666"));
        subtitle.setPadding(0, 0, 0, 30);
        layout.addView(subtitle);
        
        // Status Section
        statusText = new TextView(this);
        statusText.setText("Status: Ready");
        statusText.setTextSize(18);
        statusText.setTextColor(Color.parseColor("#333333"));
        statusText.setPadding(0, 0, 0, 10);
        layout.addView(statusText);
        
        // Recording Status
        recordingStatusText = new TextView(this);
        recordingStatusText.setText("🎤 Recording: Inactive");
        recordingStatusText.setTextSize(16);
        recordingStatusText.setTextColor(Color.parseColor("#666666"));
        recordingStatusText.setPadding(0, 0, 0, 20);
        layout.addView(recordingStatusText);
        
        // Risk Level Section
        TextView riskTitle = new TextView(this);
        riskTitle.setText("🚨 Current Risk Level:");
        riskTitle.setTextSize(16);
        riskTitle.setTextColor(Color.parseColor("#333333"));
        riskTitle.setPadding(0, 0, 0, 10);
        layout.addView(riskTitle);
        
        riskLevelText = new TextView(this);
        riskLevelText.setText("0% - No active call");
        riskLevelText.setTextSize(20);
        riskLevelText.setTextColor(Color.parseColor("#4CAF50"));
        riskLevelText.setPadding(0, 0, 0, 10);
        layout.addView(riskLevelText);
        
        // Risk Meter
        riskMeter = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        riskMeter.setMax(100);
        riskMeter.setProgress(0);
        riskMeter.getProgressDrawable().setColorFilter(Color.parseColor("#4CAF50"), android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams riskParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 30);
        riskParams.setMargins(0, 0, 0, 30);
        riskMeter.setLayoutParams(riskParams);
        layout.addView(riskMeter);
        
        // Permission button
        permissionButton = new Button(this);
        permissionButton.setText("Grant Permissions");
        permissionButton.setBackgroundColor(Color.parseColor("#FF9800"));
        permissionButton.setTextColor(Color.WHITE);
        permissionButton.setPadding(20, 15, 20, 15);
        permissionButton.setOnClickListener(v -> requestPermissions());
        layout.addView(permissionButton);
        
        // Monitor button
        monitorButton = new Button(this);
        monitorButton.setText("🎤 Start Advanced Monitoring");
        monitorButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        monitorButton.setTextColor(Color.WHITE);
        monitorButton.setPadding(20, 15, 20, 15);
        monitorButton.setOnClickListener(v -> toggleMonitoring());
        monitorButton.setEnabled(false);
        
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, 10, 0, 10);
        monitorButton.setLayoutParams(buttonParams);
        layout.addView(monitorButton);
        
        // Call log
        TextView logTitle = new TextView(this);
        logTitle.setText("📋 Advanced Call Detection Log:");
        logTitle.setTextSize(16);
        logTitle.setTextColor(Color.parseColor("#333333"));
        logTitle.setPadding(0, 30, 0, 10);
        layout.addView(logTitle);
        
        callLogText = new TextView(this);
        callLogText.setText("No calls detected yet...\n\n🎤 Advanced features:\n• Real-time call recording\n• Scam pattern analysis\n• Risk level assessment\n• Audio safety monitoring");
        callLogText.setTextSize(14);
        callLogText.setTextColor(Color.parseColor("#666666"));
        callLogText.setBackgroundColor(Color.parseColor("#FFFFFF"));
        callLogText.setPadding(15, 15, 15, 15);
        layout.addView(callLogText);
        
        // About button
        Button aboutButton = new Button(this);
        aboutButton.setText("About Hello Hari");
        aboutButton.setBackgroundColor(Color.parseColor("#2E3192"));
        aboutButton.setTextColor(Color.WHITE);
        aboutButton.setPadding(20, 15, 20, 15);
        aboutButton.setOnClickListener(v -> showAbout());
        
        LinearLayout.LayoutParams aboutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        aboutParams.setMargins(0, 20, 0, 0);
        aboutButton.setLayoutParams(aboutParams);
        layout.addView(aboutButton);
        
        scrollView.addView(layout);
        setContentView(scrollView);
    }

    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        hasAllPermissions = allGranted;
        updateUI();
    }

    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            hasAllPermissions = allGranted;
            
            if (allGranted) {
                addToCallLog("✅ All permissions granted - Advanced features enabled!");
            } else {
                addToCallLog("⚠️ Some permissions denied. Recording features may not work.");
            }
            
            updateUI();
        }
    }

    private void updateUI() {
        if (hasAllPermissions) {
            statusText.setText("Status: ✅ Ready for advanced monitoring");
            statusText.setTextColor(Color.parseColor("#4CAF50"));
            permissionButton.setText("Permissions Granted ✓");
            permissionButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            permissionButton.setEnabled(false);
            monitorButton.setEnabled(true);
        } else {
            statusText.setText("Status: ⚠️ Permissions needed for recording");
            statusText.setTextColor(Color.parseColor("#FF9800"));
            permissionButton.setText("Grant Permissions");
            permissionButton.setBackgroundColor(Color.parseColor("#FF9800"));
            permissionButton.setEnabled(true);
            monitorButton.setEnabled(false);
        }
        
        // Update monitor button
        if (callDetector.isMonitoring()) {
            monitorButton.setText("🔴 Stop Advanced Monitoring");
            monitorButton.setBackgroundColor(Color.parseColor("#F44336"));
        } else {
            monitorButton.setText("🎤 Start Advanced Monitoring");
            monitorButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        }
        
        // Update recording status
        if (isRecording) {
            recordingStatusText.setText("🔴 Recording: ACTIVE - Analyzing audio...");
            recordingStatusText.setTextColor(Color.parseColor("#F44336"));
        } else {
            recordingStatusText.setText("🎤 Recording: Inactive");
            recordingStatusText.setTextColor(Color.parseColor("#666666"));
        }
    }

    private void toggleMonitoring() {
        if (!hasAllPermissions) {
            addToCallLog("❌ Cannot start advanced monitoring without permissions");
            return;
        }

        if (callDetector.isMonitoring()) {
            callDetector.stopCallDetection();
            addToCallLog("🛑 Advanced call monitoring stopped");
            currentRiskScore = 0;
            updateRiskLevel(0, "Monitoring stopped");
        } else {
            boolean started = callDetector.startCallDetection();
            if (started) {
                addToCallLog("🚀 Advanced monitoring started - Hello Hari recording protection active!");
            } else {
                addToCallLog("❌ Failed to start advanced monitoring");
            }
        }
        
        updateUI();
    }

    // Enhanced CallDetectionListener implementation
    @Override
    public void onCallStateChanged(String state, String phoneNumber) {
        runOnUiThread(() -> {
            String displayNumber = phoneNumber != null ? phoneNumber : "Unknown";
            String logEntry = "";
            
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                logEntry = "📞 INCOMING: " + displayNumber + " - Preparing recording & analysis...";
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                logEntry = "📱 ANSWERED: " + displayNumber + " - Recording & analyzing for scams";
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                logEntry = "📴 ENDED: Call finished - Final analysis complete";
            } else {
                logEntry = "📋 STATE: " + state + " - " + displayNumber;
            }
            
            addToCallLog(logEntry);
        });
    }

    @Override
    public void onRecordingStatusChanged(boolean recording, String filePath) {
        runOnUiThread(() -> {
            isRecording = recording;
            if (recording) {
                addToCallLog("🎤 RECORDING STARTED: Audio analysis in progress...");
            } else {
                addToCallLog("⏹️ RECORDING STOPPED: Analyzing audio for scam patterns...");
            }
            updateUI();
        });
    }

    @Override
    public void onRiskLevelChanged(int riskScore, String analysis) {
        runOnUiThread(() -> {
            currentRiskScore = riskScore;
            updateRiskLevel(riskScore, analysis);
            
            String logEntry = String.format("🔍 RISK ANALYSIS: %d%% - %s", riskScore, analysis);
            addToCallLog(logEntry);
        });
    }

    private void updateRiskLevel(int riskScore, String analysis) {
        // Update risk text and color
        String riskText = riskScore + "% - " + analysis;
        riskLevelText.setText(riskText);
        
        // Update risk meter
        riskMeter.setProgress(riskScore);
        
        // Change colors based on risk level
        int color;
        if (riskScore > 70) {
            color = Color.parseColor("#F44336"); // Red - High risk
        } else if (riskScore > 40) {
            color = Color.parseColor("#FF9800"); // Orange - Medium risk
        } else {
            color = Color.parseColor("#4CAF50"); // Green - Low risk
        }
        
        riskLevelText.setTextColor(color);
        riskMeter.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void addToCallLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        callLog.insert(0, "[" + timestamp + "] " + message + "\n\n");
        
        // Keep only last 15 entries for enhanced log
        String[] lines = callLog.toString().split("\n");
        if (lines.length > 30) {
            callLog = new StringBuilder();
            for (int i = 0; i < 30; i++) {
                callLog.append(lines[i]).append("\n");
            }
        }
        
        callLogText.setText(callLog.toString());
        Log.d(TAG, message);
    }

    private void showAbout() {
        LinearLayout aboutLayout = new LinearLayout(this);
        aboutLayout.setOrientation(LinearLayout.VERTICAL);
        aboutLayout.setPadding(40, 40, 40, 40);
        aboutLayout.setBackgroundColor(Color.parseColor("#F5F6FA"));
        
        TextView aboutTitle = new TextView(this);
        aboutTitle.setText("💙 In Memory of Hari");
        aboutTitle.setTextSize(24);
        aboutTitle.setTextColor(Color.parseColor("#2E3192"));
        aboutTitle.setPadding(0, 0, 0, 20);
        aboutLayout.addView(aboutTitle);
        
        TextView aboutText = new TextView(this);
        aboutText.setText("Hello Hari (HH) is dedicated to the memory of Hari, whose spirit of protecting and helping others lives on through this app.\n\n" +
                "\"Protecting one person from fraud is like protecting an entire family from grief\"\n\n" +
                "This app serves as a guardian, helping people stay safe from scams and frauds - a mission that would have made Hari proud.\n\n" +
                "🛡️ Phase 2 Features:\n" +
                "• Real-time call recording\n" +
                "• Advanced scam pattern detection\n" +
                "• Live risk level assessment\n" +
                "• Audio analysis for fraud detection\n" +
                "• Intelligent threat recognition\n" +
                "• Privacy-first approach\n" +
                "• All processing happens locally\n\n" +
                "🎤 Recording Notice:\n" +
                "Call recordings are used solely for your protection and are stored locally on your device. No data is sent to external servers.");
        aboutText.setTextSize(16);
        aboutText.setTextColor(Color.parseColor("#333333"));
        aboutText.setPadding(0, 0, 0, 30);
        aboutLayout.addView(aboutText);
        
        Button backButton = new Button(this);
        backButton.setText("← Back to Main");
        backButton.setBackgroundColor(Color.parseColor("#2E3192"));
        backButton.setTextColor(Color.WHITE);
        backButton.setOnClickListener(v -> createEnhancedUI());
        aboutLayout.addView(backButton);
        
        setContentView(aboutLayout);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callDetector != null) {
            callDetector.stopCallDetection();
        }
    }
}
