package com.hellohari;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

public class MainActivity extends Activity implements SimpleCallDetector.CallDetectionListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private SimpleCallDetector callDetector;
    private TextView statusText;
    private TextView callLogText;
    private Button monitorButton;
    private Button permissionButton;
    private StringBuilder callLog;
    
    private boolean hasAllPermissions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        callLog = new StringBuilder();
        callDetector = new SimpleCallDetector(this);
        callDetector.setCallDetectionListener(this);
        
        createUI();
        checkPermissions();
        
        Log.d(TAG, "Hello Hari MainActivity created");
    }

    private void createUI() {
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
        subtitle.setText("Your Call Safety Companion");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.parseColor("#666666"));
        subtitle.setPadding(0, 0, 0, 30);
        layout.addView(subtitle);
        
        // Status
        statusText = new TextView(this);
        statusText.setText("Status: Ready");
        statusText.setTextSize(18);
        statusText.setTextColor(Color.parseColor("#333333"));
        statusText.setPadding(0, 0, 0, 20);
        layout.addView(statusText);
        
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
        monitorButton.setText("Start Monitoring");
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
        logTitle.setText("📋 Call Detection Log:");
        logTitle.setTextSize(16);
        logTitle.setTextColor(Color.parseColor("#333333"));
        logTitle.setPadding(0, 30, 0, 10);
        layout.addView(logTitle);
        
        callLogText = new TextView(this);
        callLogText.setText("No calls detected yet...");
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
            Manifest.permission.POST_NOTIFICATIONS
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
            Manifest.permission.POST_NOTIFICATIONS
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
                addToCallLog("✅ All permissions granted!");
            } else {
                addToCallLog("⚠️ Some permissions denied. App may not work correctly.");
            }
            
            updateUI();
        }
    }

    private void updateUI() {
        if (hasAllPermissions) {
            statusText.setText("Status: ✅ Ready for monitoring");
            statusText.setTextColor(Color.parseColor("#4CAF50"));
            permissionButton.setText("Permissions Granted ✓");
            permissionButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            permissionButton.setEnabled(false);
            monitorButton.setEnabled(true);
        } else {
            statusText.setText("Status: ⚠️ Permissions needed");
            statusText.setTextColor(Color.parseColor("#FF9800"));
            permissionButton.setText("Grant Permissions");
            permissionButton.setBackgroundColor(Color.parseColor("#FF9800"));
            permissionButton.setEnabled(true);
            monitorButton.setEnabled(false);
        }
        
        // Update monitor button
        if (callDetector.isMonitoring()) {
            monitorButton.setText("🔴 Stop Monitoring");
            monitorButton.setBackgroundColor(Color.parseColor("#F44336"));
        } else {
            monitorButton.setText("▶️ Start Monitoring");
            monitorButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        }
    }

    private void toggleMonitoring() {
        if (!hasAllPermissions) {
            addToCallLog("❌ Cannot start monitoring without permissions");
            return;
        }

        if (callDetector.isMonitoring()) {
            callDetector.stopCallDetection();
            addToCallLog("🛑 Call monitoring stopped");
        } else {
            boolean started = callDetector.startCallDetection();
            if (started) {
                addToCallLog("🚀 Call monitoring started - Hello Hari is protecting you!");
            } else {
                addToCallLog("❌ Failed to start call monitoring");
            }
        }
        
        updateUI();
    }

    @Override
    public void onCallStateChanged(String state, String phoneNumber) {
        runOnUiThread(() -> {
            String displayNumber = phoneNumber != null ? phoneNumber : "Unknown";
            String logEntry = "";
            
            switch (state) {
                case TelephonyManager.EXTRA_STATE_RINGING:
                    logEntry = "📞 INCOMING: " + displayNumber + " - Analyzing for scams...";
                    break;
                case TelephonyManager.EXTRA_STATE_OFFHOOK:
                    logEntry = "📱 ANSWERED: " + displayNumber + " - Recording for safety";
                    break;
                case TelephonyManager.EXTRA_STATE_IDLE:
                    logEntry = "📴 ENDED: Call finished - Analysis complete";
                    break;
                default:
                    logEntry = "📋 STATE: " + state + " - " + displayNumber;
                    break;
            }
            
            addToCallLog(logEntry);
        });
    }

    private void addToCallLog(String message) {
        String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        callLog.insert(0, "[" + timestamp + "] " + message + "\n\n");
        
        // Keep only last 10 entries
        String[] lines = callLog.toString().split("\n");
        if (lines.length > 20) {
            callLog = new StringBuilder();
            for (int i = 0; i < 20; i++) {
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
                "🛡️ Features:\n" +
                "• Real-time call monitoring\n" +
                "• Scam pattern detection\n" +
                "• Audio analysis for fraud detection\n" +
                "• Privacy-first approach\n" +
                "• All processing happens locally");
        aboutText.setTextSize(16);
        aboutText.setTextColor(Color.parseColor("#333333"));
        aboutText.setPadding(0, 0, 0, 30);
        aboutLayout.addView(aboutText);
        
        Button backButton = new Button(this);
        backButton.setText("← Back to Main");
        backButton.setBackgroundColor(Color.parseColor("#2E3192"));
        backButton.setTextColor(Color.WHITE);
        backButton.setOnClickListener(v -> createUI());
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
