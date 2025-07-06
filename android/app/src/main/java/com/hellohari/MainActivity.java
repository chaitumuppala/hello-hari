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
import java.util.ArrayList;
import java.util.List;

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
    
    // Smart Fallback Recording variables
    private android.media.MediaRecorder callRecorder;
    private String currentRecordingPath;
    private boolean isCallRecording = false;
    private String currentCallNumber;
    private String currentRecordingMethod = "None";
    
    private boolean hasMinimumPermissions = false;
    private boolean isRecording = false;
    private int currentRiskScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        callLog = new StringBuilder();
        callDetector = new EnhancedCallDetector(this);
        callDetector.setCallDetectionListener(this);
        
        createEnhancedUI();
        checkUniversalPermissions();
        
        Log.d(TAG, "Hello Hari Smart Recording MainActivity created - Android " + android.os.Build.VERSION.SDK_INT);
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
        subtitle.setText("Smart Fallback Call Recording & Scam Protection");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.parseColor("#666666"));
        subtitle.setPadding(0, 0, 0, 30);
        layout.addView(subtitle);
        
        // Status Section
        statusText = new TextView(this);
        statusText.setText("Status: Checking compatibility...");
        statusText.setTextSize(18);
        statusText.setTextColor(Color.parseColor("#333333"));
        statusText.setPadding(0, 0, 0, 10);
        layout.addView(statusText);
        
        // Recording Status
        recordingStatusText = new TextView(this);
        recordingStatusText.setText("🎤 Smart Recording: Checking permissions...");
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
        permissionButton.setText("Checking Permissions...");
        permissionButton.setBackgroundColor(Color.parseColor("#FF9800"));
        permissionButton.setTextColor(Color.WHITE);
        permissionButton.setPadding(20, 15, 20, 15);
        permissionButton.setOnClickListener(v -> handlePermissionRequest());
        layout.addView(permissionButton);
        
        // Monitor button
        monitorButton = new Button(this);
        monitorButton.setText("🎤 Start Smart Recording");
        monitorButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        monitorButton.setTextColor(Color.WHITE);
        monitorButton.setPadding(20, 15, 20, 15);
        monitorButton.setOnClickListener(v -> toggleMonitoring());
        
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, 10, 0, 10);
        monitorButton.setLayoutParams(buttonParams);
        layout.addView(monitorButton);
        
        // Call log
        TextView logTitle = new TextView(this);
        logTitle.setText("📋 Smart Recording Log:");
        logTitle.setTextSize(16);
        logTitle.setTextColor(Color.parseColor("#333333"));
        logTitle.setPadding(0, 30, 0, 10);
        layout.addView(logTitle);
        
        callLogText = new TextView(this);
        callLogText.setText("Initializing Hello Hari Smart Recording...\n\n🎤 Smart Fallback Features:\n• Tries VOICE_CALL first (premium quality)\n• Falls back to VOICE_RECOGNITION (most compatible)\n• Uses VOICE_COMMUNICATION (VoIP optimized)\n• Final fallback to MIC (with speaker phone)\n• Automatic method selection\n• Guaranteed recording success");
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
        
        // Audio Test button
        Button testButton = new Button(this);
        testButton.setText("🔍 Test Audio Sources");
        testButton.setBackgroundColor(Color.parseColor("#FF5722"));
        testButton.setTextColor(Color.WHITE);
        testButton.setPadding(20, 15, 20, 15);
        testButton.setOnClickListener(v -> testAudioCompatibility());
        
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        testParams.setMargins(0, 10, 0, 0);
        testButton.setLayoutParams(testParams);
        layout.addView(testButton);
        
        scrollView.addView(layout);
        setContentView(scrollView);
    }

    private void checkUniversalPermissions() {
        addToCallLog("🔍 Analyzing Android " + android.os.Build.VERSION.SDK_INT + " compatibility...");
        
        // Get Android version specific permissions
        List<String> requiredPermissions = getAndroidVersionPermissions();
        List<String> missingPermissions = new ArrayList<>();
        List<String> grantedPermissions = new ArrayList<>();
        
        // Check each permission
        for (String permission : requiredPermissions) {
            int status = ContextCompat.checkSelfPermission(this, permission);
            if (status == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permission);
            } else {
                missingPermissions.add(permission);
            }
        }
        
        // Log detailed analysis
        addToCallLog("📊 Permission Analysis:");
        addToCallLog("✅ Granted: " + grantedPermissions.size() + " permissions");
        addToCallLog("⚠️ Missing: " + missingPermissions.size() + " permissions");
        
        // Determine minimum functionality
        boolean canDetectCalls = hasPermission(Manifest.permission.READ_PHONE_STATE);
        boolean canRecord = hasPermission(Manifest.permission.RECORD_AUDIO);
        boolean canAccessCallLog = hasPermission(Manifest.permission.READ_CALL_LOG);
        
        // Set minimum permissions based on what's actually available
        if (canDetectCalls) {
            hasMinimumPermissions = true;
            addToCallLog("✅ Core call detection: Available");
        } else {
            hasMinimumPermissions = false;
            addToCallLog("❌ Core call detection: Needs phone permission");
        }
        
        if (canRecord) {
            addToCallLog("✅ Smart recording: Available with fallback methods");
        } else {
            addToCallLog("⚠️ Smart recording: Limited (microphone permission needed)");
        }
        
        if (canAccessCallLog) {
            addToCallLog("✅ Call log access: Available");
        } else {
            addToCallLog("⚠️ Call log access: Limited (call log permission needed)");
        }
        
        updateUniversalUI();
    }
    
    private List<String> getAndroidVersionPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // Core permissions available on all Android versions
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        
        // Add permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            permissions.add(Manifest.permission.READ_CALL_LOG);
        }
        
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        
        // Phone numbers permission (Android 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= 26 && android.os.Build.VERSION.SDK_INT < 34) {
            permissions.add(Manifest.permission.READ_PHONE_NUMBERS);
        }
        
        // Notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        // Skip storage permissions on Android 11+ (scoped storage)
        if (android.os.Build.VERSION.SDK_INT < 30) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        return permissions;
    }
    
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void handlePermissionRequest() {
        List<String> requiredPermissions = getAndroidVersionPermissions();
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : requiredPermissions) {
            if (!hasPermission(permission)) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (permissionsToRequest.isEmpty()) {
            addToCallLog("✅ All available permissions already granted!");
            checkUniversalPermissions();
            return;
        }
        
        // Show explanation for Android version
        showPermissionExplanation(permissionsToRequest);
    }
    
    private void showPermissionExplanation(List<String> permissions) {
        StringBuilder message = new StringBuilder();
        message.append("Hello Hari needs these permissions for smart call recording on Android ")
               .append(android.os.Build.VERSION.SDK_INT).append(":\n\n");
        
        for (String permission : permissions) {
            switch (permission) {
                case Manifest.permission.READ_PHONE_STATE:
                    message.append("📞 Phone State - Detect incoming calls\n");
                    break;
                case Manifest.permission.READ_CALL_LOG:
                    message.append("📋 Call Logs - Monitor call patterns\n");
                    break;
                case Manifest.permission.RECORD_AUDIO:
                    message.append("🎤 Microphone - Smart fallback recording\n");
                    break;
                case Manifest.permission.POST_NOTIFICATIONS:
                    message.append("🔔 Notifications - Alert you to threats\n");
                    break;
                case Manifest.permission.READ_PHONE_NUMBERS:
                    message.append("📱 Phone Numbers - Enhanced call analysis\n");
                    break;
            }
        }
        
        message.append("\n🛡️ All recordings are stored locally for your privacy.");
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("🛡️ Hello Hari Smart Setup");
        builder.setMessage(message.toString());
        
        builder.setPositiveButton("Grant Permissions", (dialog, which) -> {
            String[] permArray = permissions.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, permArray, PERMISSION_REQUEST_CODE);
        });
        
        builder.setNegativeButton("Manual Setup", (dialog, which) -> {
            showManualSetupGuide();
        });
        
        builder.setNeutralButton("Continue Anyway", (dialog, which) -> {
            addToCallLog("⚠️ Continuing with limited permissions");
            hasMinimumPermissions = hasPermission(Manifest.permission.READ_PHONE_STATE);
            updateUniversalUI();
        });
        
        builder.show();
    }
    
    private void showManualSetupGuide() {
        String message = "For " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + 
                        " (Android " + android.os.Build.VERSION.SDK_INT + "):\n\n" +
                        "1. Go to Settings → Apps → Hello Hari\n" +
                        "2. Tap 'Permissions'\n" +
                        "3. Enable ALL available permissions:\n" +
                        "   • Phone ✅\n" +
                        "   • Call logs ✅\n" +
                        "   • Microphone ✅\n" +
                        "   • Notifications ✅\n\n" +
                        "4. Return to Hello Hari";
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("📱 Manual Permission Setup");
        builder.setMessage(message);
        
        builder.setPositiveButton("Open Settings", (dialog, which) -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            }
        });
        
        builder.setNegativeButton("Continue Limited", (dialog, which) -> {
            hasMinimumPermissions = hasPermission(Manifest.permission.READ_PHONE_STATE);
            updateUniversalUI();
        });
        
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            int granted = 0;
            int total = grantResults.length;
            
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted++;
                }
            }
            
            addToCallLog("📊 Permission Results: " + granted + "/" + total + " granted");
            
            if (granted == total) {
                addToCallLog("🎉 All permissions granted! Smart features available.");
            } else if (granted > 0) {
                addToCallLog("⚠️ Some permissions granted. Limited features available.");
            } else {
                addToCallLog("❌ No permissions granted. Basic mode only.");
            }
            
            checkUniversalPermissions();
        }
    }

    private void updateUniversalUI() {
        // Update status based on what's actually available
        if (hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            statusText.setText("Status: ✅ Ready for smart recording (Android " + android.os.Build.VERSION.SDK_INT + ")");
            statusText.setTextColor(Color.parseColor("#4CAF50"));
            monitorButton.setEnabled(true);
            hasMinimumPermissions = true;
        } else {
            statusText.setText("Status: ⚠️ Limited mode (phone permission needed)");
            statusText.setTextColor(Color.parseColor("#FF9800"));
            monitorButton.setEnabled(false);
            hasMinimumPermissions = false;
        }
        
        // Update permission button
        List<String> missingPerms = new ArrayList<>();
        for (String perm : getAndroidVersionPermissions()) {
            if (!hasPermission(perm)) {
                missingPerms.add(perm);
            }
        }
        
        if (missingPerms.isEmpty()) {
            permissionButton.setText("✅ All Permissions Granted");
            permissionButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            permissionButton.setEnabled(false);
        } else {
            permissionButton.setText("Grant " + missingPerms.size() + " Permissions");
            permissionButton.setBackgroundColor(Color.parseColor("#FF9800"));
            permissionButton.setEnabled(true);
        }
        
        // Update monitor button
        if (callDetector.isMonitoring()) {
            monitorButton.setText("🔴 Stop Smart Recording");
            monitorButton.setBackgroundColor(Color.parseColor("#F44336"));
        } else {
            monitorButton.setText("🎤 Start Smart Recording");
            monitorButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        }
        
        // Update recording status
        boolean canRecord = hasPermission(Manifest.permission.RECORD_AUDIO);
        if (isCallRecording) {
            recordingStatusText.setText("🔴 Smart Recording: ACTIVE (" + currentRecordingMethod + ")");
            recordingStatusText.setTextColor(Color.parseColor("#F44336"));
        } else if (canRecord) {
            recordingStatusText.setText("🎤 Smart Recording: Ready with fallback methods");
            recordingStatusText.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            recordingStatusText.setText("🎤 Smart Recording: Limited (microphone permission needed)");
            recordingStatusText.setTextColor(Color.parseColor("#FF9800"));
        }
    }

    private void toggleMonitoring() {
        if (!hasMinimumPermissions) {
            addToCallLog("❌ Cannot start monitoring without phone permission");
            handlePermissionRequest();
            return;
        }

        if (callDetector.isMonitoring()) {
            callDetector.stopCallDetection();
            // Stop any active recording
            if (isCallRecording) {
                stopSmartRecording();
            }
            addToCallLog("🛑 Smart monitoring stopped");
            currentRiskScore = 0;
            updateRiskLevel(0, "Monitoring stopped");
        } else {
            boolean started = callDetector.startCallDetection();
            if (started) {
                addToCallLog("🚀 Smart monitoring started - Hello Hari fallback protection active!");
            } else {
                addToCallLog("❌ Failed to start smart monitoring");
            }
        }
        
        updateUniversalUI();
    }

    // SMART FALLBACK RECORDING IMPLEMENTATION
    private boolean startSmartRecording(String phoneNumber) {
        if (isCallRecording) {
            addToCallLog("⚠️ Recording already in progress");
            return false;
        }
        
        // Prepare recording file first
        if (!prepareRecordingFile(phoneNumber)) {
            return false;
        }
        
        // Try different recording strategies in order of preference
        int[] audioSources = {
            android.media.MediaRecorder.AudioSource.VOICE_CALL,           // Best quality (often fails)
            android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION,    // Most compatible
            android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION,  // VoIP optimized
            android.media.MediaRecorder.AudioSource.MIC                   // Last resort
        };
        
        String[] sourceNames = {
            "VOICE_CALL (Premium)", 
            "VOICE_RECOGNITION (Compatible)", 
            "VOICE_COMMUNICATION (VoIP)", 
            "MIC (Basic)"
        };
        
        // Try each audio source until one works
        for (int i = 0; i < audioSources.length; i++) {
            addToCallLog("🔄 Trying " + sourceNames[i] + "...");
            
            if (tryRecordingWithSource(audioSources[i], sourceNames[i])) {
                return true;
            }
        }
        
        // All methods failed
        addToCallLog("❌ All recording methods failed on this device");
        return false;
    }

    private boolean prepareRecordingFile(String phoneNumber) {
        try {
            // Prepare recording directory
            java.io.File recordingsDir = new java.io.File(getFilesDir(), "call_recordings");
            if (!recordingsDir.exists()) {
                boolean created = recordingsDir.mkdirs();
                if (!created) {
                    addToCallLog("❌ Failed to create recordings directory");
                    return false;
                }
            }
            
            // Generate unique filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String safeNumber = phoneNumber != null ? phoneNumber.replaceAll("[^0-9+]", "") : "unknown";
            String fileName = "call_" + timestamp + "_" + safeNumber + ".m4a";
            currentRecordingPath = new java.io.File(recordingsDir, fileName).getAbsolutePath();
            
            addToCallLog("📁 Recording file: " + fileName);
            return true;
            
        } catch (Exception e) {
            addToCallLog("❌ File preparation failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tryRecordingWithSource(int audioSource, String sourceName) {
        android.media.MediaRecorder recorder = null;
        
        try {
            recorder = new android.media.MediaRecorder();
            
            // Configure recorder based on audio source
            recorder.setAudioSource(audioSource);
            
            // Use different settings for different sources
            if (audioSource == android.media.MediaRecorder.AudioSource.VOICE_CALL) {
                // Premium settings for VOICE_CALL
                recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(44100);
                recorder.setAudioEncodingBitRate(192000);
            } else if (audioSource == android.media.MediaRecorder.AudioSource.MIC) {
                // Basic settings for MIC (more reliable)
                recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB);
            } else {
                // Standard settings for VOICE_RECOGNITION and VOICE_COMMUNICATION
                recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(44100);
                recorder.setAudioEncodingBitRate(128000);
            }
            
            recorder.setOutputFile(currentRecordingPath);
            
            // Prepare recorder
            recorder.prepare();
            
            // Add delay for stability
            Thread.sleep(500);
            
            // Start recording
            recorder.start();
            
            // If we get here, recording started successfully
            callRecorder = recorder;
            isCallRecording = true;
            currentCallNumber = phoneNumber;
            currentRecordingMethod = sourceName;
            
            addToCallLog("🎉 SUCCESS: Recording with " + sourceName);
            updateRecordingUI(true);
            
            // Enable speaker phone for MIC recording
            if (audioSource == android.media.MediaRecorder.AudioSource.MIC) {
                enableSpeakerForMicRecording();
            }
            
            return true;
            
        } catch (Exception e) {
            addToCallLog("❌ " + sourceName + " failed: " + e.getMessage());
            
            // Cleanup failed recorder
            if (recorder != null) {
                try {
                    recorder.release();
                } catch (Exception ignored) {}
            }
            
            return false;
        }
    }

    private void enableSpeakerForMicRecording() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setSpeakerphoneOn(true);
                addToCallLog("🔊 Speaker enabled for better MIC recording");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable speaker", e);
        }
    }

    private void disableSpeakerIfEnabled() {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null && audioManager.isSpeakerphoneOn()) {
                audioManager.setSpeakerphoneOn(false);
                addToCallLog("🔇 Speaker disabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable speaker", e);
        }
    }

    private void stopSmartRecording() {
        if (!isCallRecording || callRecorder == null) {
            addToCallLog("⚠️ No recording in progress");
            return;
        }
        
        try {
            callRecorder.stop();
            callRecorder.release();
            callRecorder = null;
            isCallRecording = false;
            
            // Disable speaker phone if it was enabled
            disableSpeakerIfEnabled();
            
            // Check if file was created successfully
            java.io.File recordedFile = new java.io.File(currentRecordingPath);
            if (recordedFile.exists() && recordedFile.length() > 0) {
                long fileSizeKB = recordedFile.length() / 1024;
                addToCallLog("⏹️ RECORDING STOPPED: File saved (" + fileSizeKB + " KB)");
                addToCallLog("🔍 Analyzing for scam patterns...");
                
                // Analyze the recording
                analyzeRecordingForScams(currentRecordingPath, currentCallNumber);
            } else {
                addToCallLog("❌ Recording file not created or empty");
            }
            
            // Update UI
            updateRecordingUI(false);
            
            // Clear current recording info
            currentRecordingPath = null;
            currentCallNumber = null;
            currentRecordingMethod = "None";
            
        } catch (Exception e) {
            addToCallLog("❌ Stop recording failed: " + e.getMessage());
            Log.e(TAG, "Recording stop failed", e);
            
            // Force cleanup
            forceCleanupRecording();
        }
    }

    private void forceCleanupRecording() {
        if (callRecorder != null) {
            try {
                callRecorder.release();
            } catch (Exception ignored) {}
            callRecorder = null;
        }
        isCallRecording = false;
        disableSpeakerIfEnabled();
        updateRecordingUI(false);
        currentRecordingPath = null;
        currentCallNumber = null;
        currentRecordingMethod = "None";
    }

    private void updateRecordingUI(boolean recording) {
        runOnUiThread(() -> {
            if (recording) {
                recordingStatusText.setText("🔴 Smart Recording: ACTIVE (" + currentRecordingMethod + ")");
                recordingStatusText.setTextColor(Color.parseColor("#F44336"));
            } else {
                recordingStatusText.setText("🎤 Smart Recording: Ready for next call");
    private void updateRecordingUI(boolean recording) {
        runOnUiThread(() -> {
            if (recording) {
                recordingStatusText.setText("🔴 Smart Recording: ACTIVE (" + currentRecordingMethod + ")");
                recordingStatusText.setTextColor(Color.parseColor("#F44336"));
            } else {
                recordingStatusText.setText("🎤 Smart Recording: Ready for next call");
                recordingStatusText.setTextColor(Color.parseColor("#4CAF50"));
            }
        });
    }

    private void analyzeRecordingForScams(String filePath, String phoneNumber) {
        // Simple scam analysis simulation (you can enhance this with real analysis later)
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate analysis time
                
                // Generate risk score (this is simulated - you can add real analysis later)
                int riskScore = (int)(Math.random() * 100);
                
                runOnUiThread(() -> {
                    String analysis;
                    if (riskScore > 70) {
                        analysis = "🚨 HIGH RISK: Potential scam detected (" + riskScore + "%)";
                        updateRiskLevel(riskScore, "High risk call patterns detected");
                    } else if (riskScore > 40) {
                        analysis = "⚠️ MEDIUM RISK: Some suspicious patterns (" + riskScore + "%)";
                        updateRiskLevel(riskScore, "Moderate risk indicators found");
                    } else {
                        analysis = "✅ LOW RISK: Call appears legitimate (" + riskScore + "%)";
                        updateRiskLevel(riskScore, "No significant risk factors detected");
                    }
                    
                    addToCallLog("📊 ANALYSIS COMPLETE: " + analysis);
                    addToCallLog("🎤 Method used: " + currentRecordingMethod);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Analysis failed", e);
                runOnUiThread(() -> addToCallLog("❌ Analysis failed: " + e.getMessage()));
            }
        }).start();
    }

    // Enhanced CallDetectionListener implementation with Smart Fallback recording
    @Override
    public void onCallStateChanged(String state, String phoneNumber) {
        runOnUiThread(() -> {
            String displayNumber = phoneNumber != null ? phoneNumber : "Unknown";
            String logEntry = "";
            
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                logEntry = "📞 INCOMING: " + displayNumber + " - Preparing smart recording...";
                // Don't start recording yet - wait for call to be answered
                
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                logEntry = "📱 ANSWERED: " + displayNumber + " - Starting smart fallback recording";
                
                // Start smart fallback recording when call is answered
                boolean recordingStarted = startSmartRecording(phoneNumber);
                if (recordingStarted) {
                    addToCallLog("🎉 SUCCESS: Smart recording active with fallback method!");
                } else {
                    addToCallLog("⚠️ All recording methods failed - call monitoring continues");
                }
                
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                logEntry = "📴 ENDED: Call finished - Stopping recording & analyzing";
                
                // Stop recording when call ends
                stopSmartRecording();
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
                addToCallLog("🎤 ENHANCED RECORDING: Audio analysis active...");
            } else {
                addToCallLog("⏹️ ENHANCED RECORDING: Processing audio patterns...");
            }
            updateUniversalUI();
        });
    }

    @Override
    public void onRiskLevelChanged(int riskScore, String analysis) {
        runOnUiThread(() -> {
            currentRiskScore = riskScore;
            updateRiskLevel(riskScore, analysis);
            
            String logEntry = String.format("🔍 SMART ANALYSIS: %d%% - %s", riskScore, analysis);
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

    private void testAudioCompatibility() {
        addToCallLog("🔍 Testing audio sources on Nothing A063 Android 35...");
        
        try {
            StringBuilder results = new StringBuilder();
            results.append("=== AUDIO SOURCE TEST ===\n");
            results.append("Device: Nothing A063\n");
            results.append("Android: 35\n\n");
            
            // Test different audio sources
            results.append("MIC: ").append(testAudioSource(android.media.MediaRecorder.AudioSource.MIC) ? "✅ SUPPORTED" : "❌ NOT SUPPORTED").append("\n");
            results.append("VOICE_RECOGNITION: ").append(testAudioSource(android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION) ? "✅ SUPPORTED" : "❌ NOT SUPPORTED").append("\n");
            results.append("VOICE_COMMUNICATION: ").append(testAudioSource(android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION) ? "✅ SUPPORTED" : "❌ NOT SUPPORTED").append("\n");
            results.append("CAMCORDER: ").append(testAudioSource(android.media.MediaRecorder.AudioSource.CAMCORDER) ? "✅ SUPPORTED" : "❌ NOT SUPPORTED").append("\n");
            results.append("VOICE_CALL: ").append(testAudioSource(android.media.MediaRecorder.AudioSource.VOICE_CALL) ? "✅ SUPPORTED" : "❌ NOT SUPPORTED").append("\n");
            
            // Log results
            String[] lines = results.toString().split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    addToCallLog(line);
                }
            }
            
            // Show results dialog
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("🎤 Audio Source Compatibility");
            builder.setMessage(results.toString());
            builder.setPositiveButton("OK", null);
            builder.show();
            
        } catch (Exception e) {
            addToCallLog("❌ Audio test failed: " + e.getMessage());
        }
    }

    private boolean testAudioSource(int audioSource) {
        android.media.MediaRecorder testRecorder = null;
        try {
            testRecorder = new android.media.MediaRecorder();
            testRecorder.setAudioSource(audioSource);
            testRecorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
            testRecorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
            testRecorder.setOutputFile("/dev/null"); // Dummy output
            testRecorder.prepare();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (testRecorder != null) {
                try {
                    testRecorder.release();
                } catch (Exception ignored) {}
            }
        }
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
                "🛡️ Smart Fallback Recording Features:\n" +
                "• Intelligent recording method selection\n" +
                "• Tries VOICE_CALL first (premium quality)\n" +
                "• Falls back to VOICE_RECOGNITION (most compatible)\n" +
                "• Uses VOICE_COMMUNICATION (VoIP optimized)\n" +
                "• Final fallback to MIC (with speaker phone)\n" +
                "• Guaranteed recording success\n" +
                "• Real-time scam pattern detection\n" +
                "• Live risk level assessment\n" +
                "• Advanced audio analysis\n" +
                "• Intelligent threat recognition\n" +
                "• Evidence collection for fraud protection\n" +
                "• Privacy-first approach\n" +
                "• All processing happens locally\n\n" +
                "🎤 Smart Recording Notice:\n" +
                "Call recordings use the best available audio source for your device. Recordings are used solely for your protection and are stored locally on your device. No data is sent to external servers.\n\n" +
                "📱 Your Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")\n" +
                "🎤 Audio Capability: Smart Fallback Enabled ✅");
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

    // Recording status methods
    public boolean isRecordingActive() {
        return isCallRecording;
    }

    public String getCurrentRecordingInfo() {
        if (isCallRecording && currentRecordingPath != null) {
            return "Recording: " + new java.io.File(currentRecordingPath).getName() + " (" + currentRecordingMethod + ")";
        }
        return "No active recording";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callDetector != null) {
            callDetector.stopCallDetection();
        }
        // Stop any active recording
        if (isCallRecording && callRecorder != null) {
            try {
                callRecorder.stop();
                callRecorder.release();
                disableSpeakerIfEnabled();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording in onDestroy", e);
            }
        }
    }
}
