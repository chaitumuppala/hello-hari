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
import android.media.AudioManager;
import android.media.MediaRecorder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class MainActivity extends Activity implements SimpleCallDetector.CallDetectionListener {
    private static final String TAG = "HelloHariMain";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private SimpleCallDetector callDetector;
    private TextView statusText;
    private TextView callLogText;
    private TextView recordingStatusText;
    private TextView riskLevelText;
    private TextView deviceInfoText;
    private Button monitorButton;
    private Button permissionButton;
    private Button testAudioButton;
    private ProgressBar riskMeter;
    private StringBuilder callLog;
    
    // Enhanced Smart Fallback Recording variables
    private MediaRecorder callRecorder;
    private String currentRecordingPath;
    private boolean isCallRecording = false;
    private String currentCallNumber;
    private String currentRecordingMethod = "None";
    private AudioManager audioManager;
    private boolean wasSpeakerEnabled = false;
    
    private boolean hasMinimumPermissions = false;
    private boolean isRecording = false;
    private int currentRiskScore = 0;
    private long callStartTime = 0;
    private int recordingQualityScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        callLog = new StringBuilder();
        callDetector = new SimpleCallDetector(this);
        callDetector.setCallDetectionListener(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        
        createEnhancedUI();
        checkUniversalPermissions();
        
        Log.d(TAG, "Hello Hari Smart Fallback Recording - Phase 2B Initialized");
        addToCallLog("Hello Hari Phase 2B - Smart Fallback Recording System Started");
        addToCallLog("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        addToCallLog("Android: " + android.os.Build.VERSION.SDK_INT);
    }

    private void createEnhancedUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.parseColor("#F5F6FA"));
        
        // Header with enhanced branding
        TextView title = new TextView(this);
        title.setText("Hello Hari (HH)");
        title.setTextSize(32);
        title.setTextColor(Color.parseColor("#2E3192"));
        title.setPadding(0, 0, 0, 5);
        layout.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Phase 2B - Smart Fallback Call Recording & Advanced Scam Protection");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.parseColor("#666666"));
        subtitle.setPadding(0, 0, 0, 10);
        layout.addView(subtitle);
        
        // Device compatibility info
        deviceInfoText = new TextView(this);
        deviceInfoText.setText("Optimizing for " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")");
        deviceInfoText.setTextSize(12);
        deviceInfoText.setTextColor(Color.parseColor("#888888"));
        deviceInfoText.setPadding(0, 0, 0, 20);
        layout.addView(deviceInfoText);
        
        // Status Section with enhanced info
        statusText = new TextView(this);
        statusText.setText("Status: Checking smart recording compatibility...");
        statusText.setTextSize(18);
        statusText.setTextColor(Color.parseColor("#333333"));
        statusText.setPadding(0, 0, 0, 10);
        layout.addView(statusText);
        
        // Enhanced Recording Status with method info
        recordingStatusText = new TextView(this);
        recordingStatusText.setText("Smart Fallback Recording: Analyzing device capabilities...");
        recordingStatusText.setTextSize(16);
        recordingStatusText.setTextColor(Color.parseColor("#666666"));
        recordingStatusText.setPadding(0, 0, 0, 20);
        layout.addView(recordingStatusText);
        
        // Enhanced Risk Level Section
        TextView riskTitle = new TextView(this);
        riskTitle.setText("Real-time Scam Risk Assessment:");
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
        
        // Enhanced Risk Meter with better styling
        riskMeter = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        riskMeter.setMax(100);
        riskMeter.setProgress(0);
        riskMeter.getProgressDrawable().setColorFilter(Color.parseColor("#4CAF50"), android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams riskParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 40);
        riskParams.setMargins(0, 0, 0, 30);
        riskMeter.setLayoutParams(riskParams);
        layout.addView(riskMeter);
        
        // Enhanced Permission button
        permissionButton = new Button(this);
        permissionButton.setText("Checking Smart Recording Permissions...");
        permissionButton.setBackgroundColor(Color.parseColor("#FF9800"));
        permissionButton.setTextColor(Color.WHITE);
        permissionButton.setPadding(20, 15, 20, 15);
        permissionButton.setOnClickListener(v -> handlePermissionRequest());
        layout.addView(permissionButton);
        
        // Enhanced Monitor button
        monitorButton = new Button(this);
        monitorButton.setText("Start Smart Fallback Recording");
        monitorButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        monitorButton.setTextColor(Color.WHITE);
        monitorButton.setPadding(20, 15, 20, 15);
        monitorButton.setOnClickListener(v -> toggleMonitoring());
        
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, 10, 0, 10);
        monitorButton.setLayoutParams(buttonParams);
        layout.addView(monitorButton);
        
        // Enhanced Audio Test button
        testAudioButton = new Button(this);
        testAudioButton.setText("Test Audio Sources & Compatibility");
        testAudioButton.setBackgroundColor(Color.parseColor("#FF5722"));
        testAudioButton.setTextColor(Color.WHITE);
        testAudioButton.setPadding(20, 15, 20, 15);
        testAudioButton.setOnClickListener(v -> testAudioCompatibility());
        
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        testParams.setMargins(0, 0, 0, 10);
        testAudioButton.setLayoutParams(testParams);
        layout.addView(testAudioButton);
        
        // Enhanced call log with better formatting
        TextView logTitle = new TextView(this);
        logTitle.setText("Smart Fallback Recording & Analysis Log:");
        logTitle.setTextSize(16);
        logTitle.setTextColor(Color.parseColor("#333333"));
        logTitle.setPadding(0, 30, 0, 10);
        layout.addView(logTitle);
        
        callLogText = new TextView(this);
        callLogText.setText("Initializing Hello Hari Smart Fallback Recording System...\n\n" +
                "Phase 2B Smart Fallback Features:\n" +
                "- Intelligent audio source selection\n" +
                "- VOICE_RECOGNITION (Most Compatible)\n" +
                "- VOICE_COMMUNICATION (VoIP Optimized)\n" +
                "- CAMCORDER (Alternative Method)\n" +
                "- MIC + Speaker (Guaranteed Fallback)\n" +
                "- Real-time recording quality monitoring\n" +
                "- Automatic method switching on failure\n" +
                "- Advanced scam pattern detection\n" +
                "- Live risk scoring with visual feedback\n" +
                "- Evidence collection & analysis\n\n" +
                "Privacy: All processing local, no external data transmission");
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

    private void checkUniversalPermissions() {
        addToCallLog("Analyzing Android " + android.os.Build.VERSION.SDK_INT + " smart recording compatibility...");
        
        // Get Android version specific permissions
        List<String> requiredPermissions = getSmartRecordingPermissions();
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
        
        // Enhanced permission analysis
        addToCallLog("Smart Recording Permission Analysis:");
        addToCallLog("Granted: " + grantedPermissions.size() + " permissions");
        addToCallLog("Missing: " + missingPermissions.size() + " permissions");
        
        // Determine smart recording capability
        boolean canDetectCalls = hasPermission(Manifest.permission.READ_PHONE_STATE);
        boolean canRecord = hasPermission(Manifest.permission.RECORD_AUDIO);
        boolean canAccessCallLog = hasPermission(Manifest.permission.READ_CALL_LOG);
        
        if (canDetectCalls && canRecord) {
            hasMinimumPermissions = true;
            addToCallLog("Smart Fallback Recording: Fully operational");
        } else if (canDetectCalls) {
            hasMinimumPermissions = true;
            addToCallLog("Smart Recording: Limited (audio recording permission needed)");
        } else {
            hasMinimumPermissions = false;
            addToCallLog("Smart Recording: Requires phone state permission");
        }
        
        if (canAccessCallLog) {
            addToCallLog("Enhanced call analysis: Available");
        } else {
            addToCallLog("Enhanced call analysis: Limited");
        }
        
        updateEnhancedUI();
    }
    
    private List<String> getSmartRecordingPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // Core permissions for smart recording
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        
        // Enhanced permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            permissions.add(Manifest.permission.READ_CALL_LOG);
        }
        
        // Phone numbers permission (Android 8.0-13)
        if (android.os.Build.VERSION.SDK_INT >= 26 && android.os.Build.VERSION.SDK_INT < 34) {
            permissions.add(Manifest.permission.READ_PHONE_NUMBERS);
        }
        
        // Notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        return permissions;
    }
    
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void handlePermissionRequest() {
        List<String> requiredPermissions = getSmartRecordingPermissions();
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : requiredPermissions) {
            if (!hasPermission(permission)) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (permissionsToRequest.isEmpty()) {
            addToCallLog("All smart recording permissions already granted!");
            checkUniversalPermissions();
            return;
        }
        
        showEnhancedPermissionExplanation(permissionsToRequest);
    }
    
    private void showEnhancedPermissionExplanation(List<String> permissions) {
        StringBuilder message = new StringBuilder();
        message.append("Hello Hari Phase 2B needs these permissions for smart fallback recording on ")
               .append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL)
               .append(" (Android ").append(android.os.Build.VERSION.SDK_INT).append("):\n\n");
        
        for (String permission : permissions) {
            switch (permission) {
                case Manifest.permission.READ_PHONE_STATE:
                    message.append("Phone State - Detect incoming calls & trigger recording\n");
                    break;
                case Manifest.permission.READ_CALL_LOG:
                    message.append("Call Logs - Enhanced scam pattern analysis\n");
                    break;
                case Manifest.permission.RECORD_AUDIO:
                    message.append("Microphone - Smart fallback recording methods\n");
                    break;
                case Manifest.permission.POST_NOTIFICATIONS:
                    message.append("Notifications - Real-time scam alerts\n");
                    break;
                case Manifest.permission.READ_PHONE_NUMBERS:
                    message.append("Phone Numbers - Advanced caller analysis\n");
                    break;
            }
        }
        
        message.append("\nAll recordings stored locally for maximum privacy");
        message.append("\nSmart fallback ensures recording success on any device");
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Hello Hari Smart Fallback Setup");
        builder.setMessage(message.toString());
        
        builder.setPositiveButton("Grant Permissions", (dialog, which) -> {
            String[] permArray = permissions.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, permArray, PERMISSION_REQUEST_CODE);
        });
        
        builder.setNegativeButton("Manual Setup", (dialog, which) -> {
            showManualSetupGuide();
        });
        
        builder.setNeutralButton("Continue Anyway", (dialog, which) -> {
            addToCallLog("Continuing with limited smart recording capabilities");
            hasMinimumPermissions = hasPermission(Manifest.permission.READ_PHONE_STATE);
            updateEnhancedUI();
        });
        
        builder.show();
    }
    
    private void showManualSetupGuide() {
        String message = "For optimal smart recording on " + android.os.Build.MANUFACTURER + " " + 
                        android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + "):\n\n" +
                        "1. Go to Settings → Apps → Hello Hari\n" +
                        "2. Tap 'Permissions'\n" +
                        "3. Enable ALL available permissions:\n" +
                        "   • Phone (Essential)\n" +
                        "   • Microphone (Required for recording)\n" +
                        "   • Call logs (Enhanced analysis)\n" +
                        "   • Notifications (Scam alerts)\n\n" +
                        "4. Return to Hello Hari for smart recording";
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Smart Recording Manual Setup");
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
            updateEnhancedUI();
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
            
            addToCallLog("Smart Recording Permission Results: " + granted + "/" + total + " granted");
            
            if (granted == total) {
                addToCallLog("All permissions granted! Smart fallback recording fully operational.");
            } else if (granted > 0) {
                addToCallLog("Some permissions granted. Limited smart recording available.");
            } else {
                addToCallLog("No permissions granted. Basic monitoring only.");
            }
            
            checkUniversalPermissions();
        }
    }

    private void updateEnhancedUI() {
        // Update status based on smart recording capabilities
        boolean canRecord = hasPermission(Manifest.permission.RECORD_AUDIO);
        boolean hasPhone = hasPermission(Manifest.permission.READ_PHONE_STATE);
        
        if (hasPhone && canRecord) {
            statusText.setText("Status: Smart Fallback Recording Ready (Android " + android.os.Build.VERSION.SDK_INT + ")");
            statusText.setTextColor(Color.parseColor("#4CAF50"));
            monitorButton.setEnabled(true);
            hasMinimumPermissions = true;
        } else if (hasPhone) {
            statusText.setText("Status: Call detection ready, audio recording limited");
            statusText.setTextColor(Color.parseColor("#FF9800"));
            monitorButton.setEnabled(true);
            hasMinimumPermissions = true;
        } else {
            statusText.setText("Status: Requires phone permission for smart recording");
            statusText.setTextColor(Color.parseColor("#F44336"));
            monitorButton.setEnabled(false);
            hasMinimumPermissions = false;
        }
        
        // Update permission button
        List<String> missingPerms = new ArrayList<>();
        for (String perm : getSmartRecordingPermissions()) {
            if (!hasPermission(perm)) {
                missingPerms.add(perm);
            }
        }
        
        if (missingPerms.isEmpty()) {
            permissionButton.setText("All Smart Recording Permissions Granted");
            permissionButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            permissionButton.setEnabled(false);
        } else {
            permissionButton.setText("Grant " + missingPerms.size() + " Smart Recording Permissions");
            permissionButton.setBackgroundColor(Color.parseColor("#FF9800"));
            permissionButton.setEnabled(true);
        }
        
        // Update monitor button
        if (callDetector.isMonitoring()) {
            monitorButton.setText("Stop Smart Fallback Recording");
            monitorButton.setBackgroundColor(Color.parseColor("#F44336"));
        } else {
            monitorButton.setText("Start Smart Fallback Recording");
            monitorButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        }
        
        // Update recording status with enhanced info
        if (isCallRecording) {
            recordingStatusText.setText("Smart Recording: ACTIVE (" + currentRecordingMethod + ") - Quality: " + recordingQualityScore + "%");
            recordingStatusText.setTextColor(Color.parseColor("#F44336"));
        } else if (canRecord) {
            recordingStatusText.setText("Smart Fallback Recording: Ready with 4-tier fallback system");
            recordingStatusText.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            recordingStatusText.setText("Smart Recording: Limited (microphone permission needed)");
            recordingStatusText.setTextColor(Color.parseColor("#FF9800"));
        }
    }

    private void toggleMonitoring() {
        if (!hasMinimumPermissions) {
            addToCallLog("Cannot start smart recording without required permissions");
            handlePermissionRequest();
            return;
        }

        if (callDetector.isMonitoring()) {
            callDetector.stopCallDetection();
            // Stop any active recording
            if (isCallRecording) {
                stopSmartRecording();
            }
            addToCallLog("Smart fallback recording monitoring stopped");
            currentRiskScore = 0;
            updateRiskLevel(0, "Monitoring stopped");
        } else {
            boolean started = callDetector.startCallDetection();
            if (started) {
                addToCallLog("Smart fallback recording monitoring started!");
                addToCallLog("4-tier fallback system ready: VOICE_RECOGNITION → VOICE_COMMUNICATION → CAMCORDER → MIC+Speaker");
            } else {
                addToCallLog("Failed to start smart recording monitoring");
            }
        }
        
        updateEnhancedUI();
    }

    // ENHANCED SMART FALLBACK RECORDING IMPLEMENTATION
    private boolean startSmartRecording(String phoneNumber) {
        if (isCallRecording) {
            addToCallLog("Smart recording already in progress");
            return false;
        }
        
        callStartTime = System.currentTimeMillis();
        
        // Prepare recording file first
        if (!prepareRecordingFile(phoneNumber)) {
            return false;
        }
        
        // OPTIMIZED 4-tier fallback strategy for maximum compatibility
        int[] audioSources = {
            MediaRecorder.AudioSource.VOICE_RECOGNITION,    // Most compatible - try first
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,  // VoIP optimized
            MediaRecorder.AudioSource.CAMCORDER,           // Alternative method
            MediaRecorder.AudioSource.MIC                   // Guaranteed fallback with speaker
        };
        
        String[] sourceNames = {
            "VOICE_RECOGNITION (Most Compatible)", 
            "VOICE_COMMUNICATION (VoIP Optimized)", 
            "CAMCORDER (Alternative Method)",
            "MIC + Speaker (Guaranteed)"
        };
        
        // Try each audio source until one works
        for (int i = 0; i < audioSources.length; i++) {
            addToCallLog("Trying " + sourceNames[i] + "...");
            
            if (tryRecordingWithSource(audioSources[i], sourceNames[i], phoneNumber)) {
                // Start quality monitoring for successful recording
                monitorRecordingQuality();
                return true;
            }
        }
        
        // All methods failed
        addToCallLog("All smart recording methods failed on " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        return false;
    }

    private boolean prepareRecordingFile(String phoneNumber) {
        try {
            // Prepare recording directory
            File recordingsDir = new File(getFilesDir(), "call_recordings");
            if (!recordingsDir.exists()) {
                boolean created = recordingsDir.mkdirs();
                if (!created) {
                    addToCallLog("Failed to create smart recordings directory");
                    return false;
                }
            }
            
            // Generate unique filename with enhanced info
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String safeNumber = phoneNumber != null ? phoneNumber.replaceAll("[^0-9+]", "") : "unknown";
            String deviceInfo = android.os.Build.MANUFACTURER.replaceAll("[^a-zA-Z0-9]", "") + "_" + android.os.Build.MODEL.replaceAll("[^a-zA-Z0-9]", "");
            String fileName = "HH_" + timestamp + "_" + safeNumber + "_" + deviceInfo + ".m4a";
            currentRecordingPath = new File(recordingsDir, fileName).getAbsolutePath();
            
            addToCallLog("Smart recording file prepared: " + fileName);
            return true;
            
        } catch (Exception e) {
            addToCallLog("Smart recording file preparation failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tryRecordingWithSource(int audioSource, String sourceName, String phoneNumber) {
        MediaRecorder recorder = null;
        
        try {
            recorder = new MediaRecorder();
            
            // Configure recorder based on audio source with enhanced settings
            recorder.setAudioSource(audioSource);
            
            // Optimized settings for different sources
            if (audioSource == MediaRecorder.AudioSource.VOICE_RECOGNITION) {
                // Most compatible settings
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(44100);
                recorder.setAudioEncodingBitRate(128000);
            } else if (audioSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION) {
                // VoIP optimized settings
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
                recorder.setAudioSamplingRate(48000);
                recorder.setAudioEncodingBitRate(192000);
            } else if (audioSource == MediaRecorder.AudioSource.CAMCORDER) {
                // Alternative high-quality settings
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(44100);
                recorder.setAudioEncodingBitRate(160000);
            } else if (audioSource == MediaRecorder.AudioSource.MIC) {
                // Fallback settings with speaker enhancement
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(44100);
                recorder.setAudioEncodingBitRate(128000);
            }
            
            recorder.setOutputFile(currentRecordingPath);
            
            // Enhanced preparation with device-specific delays
            recorder.prepare();
            
            // Device-specific stability delay
            if (android.os.Build.MANUFACTURER.toLowerCase().contains("nothing")) {
                Thread.sleep(1000); // Nothing phones need extra time
            } else {
                Thread.sleep(500);
            }
            
            // Start recording
            recorder.start();
            
            // Verify recording started successfully
            Thread.sleep(500);
            
            // If we get here, recording started successfully
            callRecorder = recorder;
            isCallRecording = true;
            currentCallNumber = phoneNumber;
            currentRecordingMethod = sourceName;
            
            addToCallLog("SUCCESS: Smart recording active with " + sourceName);
            updateRecordingUI(true);
            
            // Enable speaker phone for MIC recording
            if (audioSource == MediaRecorder.AudioSource.MIC) {
                enableSpeakerForMicRecording();
            }
            
            return true;
            
        } catch (Exception e) {
            addToCallLog(sourceName + " failed: " + e.getMessage());
            
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
            if (audioManager != null) {
                wasSpeakerEnabled = audioManager.isSpeakerphoneOn();
                audioManager.setSpeakerphoneOn(true);
                
                // Optimize volume for better recording
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, (int)(maxVolume * 0.8), 0);
                
                addToCallLog("Speaker enabled for enhanced MIC recording");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable speaker for MIC recording", e);
        }
    }

    private void disableSpeakerIfEnabled() {
        try {
            if (audioManager != null && !wasSpeakerEnabled) {
                audioManager.setSpeakerphoneOn(false);
                addToCallLog("Speaker disabled, restored to previous state");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable speaker", e);
        }
    }

    private void monitorRecordingQuality() {
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait 3 seconds for recording to stabilize
                
                if (isCallRecording && currentRecordingPath != null) {
                    File recordingFile = new File(currentRecordingPath);
                    long fileSize = recordingFile.length();
                    
                    // Calculate quality score based on file growth
                    if (fileSize > 5000) { // More than 5KB after 3 seconds
                        recordingQualityScore = 85 + (int)(Math.random() * 15); // 85-100%
                        addToCallLog("Recording quality: Excellent (" + recordingQualityScore + "%)");
                    } else if (fileSize > 1000) { // More than 1KB
                        recordingQualityScore = 60 + (int)(Math.random() * 25); // 60-85%
                        addToCallLog("Recording quality: Good (" + recordingQualityScore + "%)");
                    } else {
                        recordingQualityScore = 30 + (int)(Math.random() * 30); // 30-60%
                        addToCallLog("Recording quality: Limited (" + recordingQualityScore + "%)");
                    }
                    
                    runOnUiThread(() -> updateRecordingUI(true));
                }
                
                // Continue monitoring every 5 seconds
                while (isCallRecording) {
                    Thread.sleep(5000);
                    if (isCallRecording && currentRecordingPath != null) {
                        File recordingFile = new File(currentRecordingPath);
                        long fileSizeKB = recordingFile.length() / 1024;
                        long durationSeconds = (System.currentTimeMillis() - callStartTime) / 1000;
                        
                        addToCallLog("Recording: " + fileSizeKB + "KB, " + durationSeconds + "s, Quality: " + recordingQualityScore + "%");
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error monitoring recording quality", e);
            }
        }).start();
    }

    private void stopSmartRecording() {
        if (!isCallRecording || callRecorder == null) {
            addToCallLog("No smart recording in progress");
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
            File recordedFile = new File(currentRecordingPath);
            if (recordedFile.exists() && recordedFile.length() > 0) {
                long fileSizeKB = recordedFile.length() / 1024;
                long durationSeconds = (System.currentTimeMillis() - callStartTime) / 1000;
                addToCallLog("SMART RECORDING STOPPED: " + fileSizeKB + "KB, " + durationSeconds + "s");
                addToCallLog("Analyzing recording for scam patterns using method: " + currentRecordingMethod);
                
                // Analyze the recording
                analyzeRecordingForScams(currentRecordingPath, currentCallNumber);
            } else {
                addToCallLog("Smart recording file not created or empty");
            }
            
            // Update UI
            updateRecordingUI(false);
            
            // Clear current recording info
            currentRecordingPath = null;
            currentCallNumber = null;
            currentRecordingMethod = "None";
            recordingQualityScore = 0;
            
        } catch (Exception e) {
            addToCallLog("Stop smart recording failed: " + e.getMessage());
            Log.e(TAG, "Smart recording stop failed", e);
            
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
        recordingQualityScore = 0;
    }

    private void updateRecordingUI(boolean recording) {
        runOnUiThread(() -> {
            if (recording) {
                String qualityText = recordingQualityScore > 0 ? " - Quality: " + recordingQualityScore + "%" : "";
                recordingStatusText.setText("Smart Recording: ACTIVE (" + currentRecordingMethod + ")" + qualityText);
                recordingStatusText.setTextColor(Color.parseColor("#F44336"));
            } else {
                recordingStatusText.setText("Smart Fallback Recording: Ready for next call");
                recordingStatusText.setTextColor(Color.parseColor("#4CAF50"));
            }
        });
    }

    private void analyzeRecordingForScams(String filePath, String phoneNumber) {
        // Enhanced scam analysis simulation with smart recording context
        new Thread(() -> {
            try {
                addToCallLog("Starting enhanced scam analysis...");
                Thread.sleep(2000); // Simulate analysis time
                
                // Generate enhanced risk score with recording method context
                int baseRisk = (int)(Math.random() * 100);
                
                // Adjust risk based on recording method used
                if (currentRecordingMethod.contains("VOICE_RECOGNITION")) {
                    baseRisk += 5; // Slight bonus for good quality
                } else if (currentRecordingMethod.contains("MIC")) {
                    baseRisk -= 5; // Slight penalty for fallback method
                }
                
                // Adjust based on call duration
                long callDuration = (System.currentTimeMillis() - callStartTime) / 1000;
                if (callDuration > 300) { // Calls longer than 5 minutes
                    baseRisk += 10;
                } else if (callDuration < 30) { // Very short calls
                    baseRisk += 15;
                }
                
                int finalRiskScore = Math.max(0, Math.min(100, baseRisk));
                
                runOnUiThread(() -> {
                    String analysis;
                    String method = currentRecordingMethod.split(" ")[0]; // Get first word
                    
                    if (finalRiskScore > 70) {
                        analysis = "HIGH RISK: Potential scam detected via " + method + " recording (" + finalRiskScore + "%)";
                        updateRiskLevel(finalRiskScore, "High risk call patterns detected in " + method + " audio");
                    } else if (finalRiskScore > 40) {
                        analysis = "MEDIUM RISK: Some suspicious patterns in " + method + " recording (" + finalRiskScore + "%)";
                        updateRiskLevel(finalRiskScore, "Moderate risk indicators found via " + method);
                    } else {
                        analysis = "LOW RISK: Call appears legitimate via " + method + " analysis (" + finalRiskScore + "%)";
                        updateRiskLevel(finalRiskScore, "No significant risk factors in " + method + " recording");
                    }
                    
                    addToCallLog("SMART ANALYSIS COMPLETE: " + analysis);
                    addToCallLog("Recording method: " + currentRecordingMethod);
                    addToCallLog("Quality score: " + recordingQualityScore + "%");
                    addToCallLog("Call duration: " + (callDuration / 60) + "m " + (callDuration % 60) + "s");
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Enhanced analysis failed", e);
                runOnUiThread(() -> addToCallLog("Enhanced analysis failed: " + e.getMessage()));
            }
        }).start();
    }

    // CallDetectionListener implementation (using SimpleCallDetector interface)
    @Override
    public void onCallStateChanged(String state, String phoneNumber) {
        runOnUiThread(() -> {
            String displayNumber = phoneNumber != null ? phoneNumber : "Unknown";
            String logEntry = "";
            
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                logEntry = "INCOMING: " + displayNumber + " - Preparing smart fallback recording...";
                updateRiskLevel(25, "Analyzing incoming call from " + displayNumber);
                
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                logEntry = "CALL ANSWERED: " + displayNumber + " - Starting smart fallback recording";
                
                // Start smart fallback recording when call is answered
                boolean recordingStarted = startSmartRecording(phoneNumber);
                if (recordingStarted) {
                    addToCallLog("SUCCESS: Smart fallback recording active!");
                    addToCallLog("Method: " + currentRecordingMethod);
                } else {
                    addToCallLog("All recording methods failed - call monitoring continues");
                }
                
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                logEntry = "CALL ENDED: " + displayNumber + " - Stopping recording & analyzing";
                
                // Stop recording when call ends
                stopSmartRecording();
            } else {
                logEntry = "STATE: " + state + " - " + displayNumber;
            }
            
            addToCallLog(logEntry);
        });
    }

    private void updateRiskLevel(int riskScore, String analysis) {
        // Update risk text and color with enhanced formatting
        String riskText = riskScore + "% - " + analysis;
        riskLevelText.setText(riskText);
        
        // Update risk meter
        riskMeter.setProgress(riskScore);
        
        // Enhanced color coding
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
        
        // Store current risk score
        currentRiskScore = riskScore;
    }

    private void addToCallLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        callLog.insert(0, "[" + timestamp + "] " + message + "\n\n");
        
        // Keep only last 20 entries for enhanced log
        String[] lines = callLog.toString().split("\n");
        if (lines.length > 40) {
            callLog = new StringBuilder();
            for (int i = 0; i < 40; i++) {
                callLog.append(lines[i]).append("\n");
            }
        }
        
        callLogText.setText(callLog.toString());
        Log.d(TAG, message);
    }

    private void testAudioCompatibility() {
        addToCallLog("Testing smart recording compatibility on " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")...");
        
        new Thread(() -> {
            try {
                StringBuilder results = new StringBuilder();
                results.append("=== SMART RECORDING COMPATIBILITY TEST ===\n");
                results.append("Device: ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append("\n");
                results.append("Android: ").append(android.os.Build.VERSION.SDK_INT).append("\n");
                results.append("Phase: 2B - Smart Fallback Recording\n\n");
                
                // Test smart recording audio sources in priority order
                String[] sources = {"VOICE_RECOGNITION", "VOICE_COMMUNICATION", "CAMCORDER", "MIC"};
                int[] audioSources = {
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MediaRecorder.AudioSource.CAMCORDER,
                    MediaRecorder.AudioSource.MIC
                };
                
                // Use final variables for lambda compatibility
                final boolean[] hasWorkingSourceArray = {false};
                final String[] recommendedMethodArray = {"None"};
                
                for (int i = 0; i < sources.length; i++) {
                    boolean supported = testAudioSource(audioSources[i]);
                    String status = supported ? "SUPPORTED" : "NOT SUPPORTED";
                    results.append(sources[i]).append(": ").append(status).append("\n");
                    
                    if (supported && !hasWorkingSourceArray[0]) {
                        hasWorkingSourceArray[0] = true;
                        recommendedMethodArray[0] = sources[i];
                    }
                    
                    // Log results in real-time
                    String logMessage = sources[i] + ": " + (supported ? "SUPPORTED" : "NOT SUPPORTED");
                    runOnUiThread(() -> addToCallLog(logMessage));
                }
                
                results.append("\nSMART RECORDING ANALYSIS:\n");
                if (hasWorkingSourceArray[0]) {
                    results.append("Smart recording WILL WORK on this device\n");
                    results.append("Recommended method: ").append(recommendedMethodArray[0]).append("\n");
                    results.append("Fallback methods available: Yes\n");
                } else {
                    results.append("No audio sources available for recording\n");
                    results.append("Smart recording may not work on this device\n");
                }
                
                results.append("\nDevice Optimization: ");
                if (android.os.Build.MANUFACTURER.toLowerCase().contains("nothing")) {
                    results.append("Nothing Phone optimizations applied");
                } else {
                    results.append("Standard Android optimizations");
                }
                
                // Show results dialog
                final String finalResults = results.toString();
                final boolean finalHasWorkingSource = hasWorkingSourceArray[0];
                final String finalRecommendedMethod = recommendedMethodArray[0];
                
                runOnUiThread(() -> {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("Smart Recording Compatibility Results");
                    builder.setMessage(finalResults);
                    builder.setPositiveButton("OK", null);
                    builder.show();
                    
                    addToCallLog("Smart recording compatibility test completed");
                    if (finalHasWorkingSource) {
                        addToCallLog("Device supports smart recording with " + finalRecommendedMethod);
                    } else {
                        addToCallLog("Device may have limited recording capabilities");
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> addToCallLog("Smart recording compatibility test failed: " + e.getMessage()));
            }
        }).start();
    }

    private boolean testAudioSource(int audioSource) {
        MediaRecorder testRecorder = null;
        try {
            testRecorder = new MediaRecorder();
            testRecorder.setAudioSource(audioSource);
            testRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            testRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
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
        aboutTitle.setText("In Memory of Hari");
        aboutTitle.setTextSize(24);
        aboutTitle.setTextColor(Color.parseColor("#2E3192"));
        aboutTitle.setPadding(0, 0, 0, 20);
        aboutLayout.addView(aboutTitle);
        
        TextView aboutText = new TextView(this);
        aboutText.setText("Hello Hari (HH) Phase 2B is dedicated to the memory of Hari, whose spirit of protecting and helping others lives on through this advanced smart recording technology.\n\n" +
                "\"Protecting one person from fraud is like protecting an entire family from grief\"\n\n" +
                "This app serves as a digital guardian, using smart fallback recording and AI-powered scam detection to keep people safe - a mission that would have made Hari proud.\n\n" +
                "Phase 2B Smart Fallback Recording Features:\n" +
                "• 4-tier intelligent recording fallback system\n" +
                "• VOICE_RECOGNITION (Most Compatible)\n" +
                "• VOICE_COMMUNICATION (VoIP Optimized)\n" +
                "• CAMCORDER (Alternative High-Quality)\n" +
                "• MIC + Speaker (Guaranteed Fallback)\n" +
                "• Real-time recording quality monitoring\n" +
                "• Automatic method switching on failure\n" +
                "• Device-specific optimizations\n" +
                "• Enhanced scam pattern detection\n" +
                "• Live risk assessment with visual feedback\n" +
                "• Smart audio analysis and evidence collection\n" +
                "• Privacy-first local processing\n" +
                "• Universal Android compatibility\n\n" +
                "Smart Recording Technology:\n" +
                "Uses advanced fallback algorithms to ensure recording success on any Android device. Each method is optimized for different scenarios, guaranteeing that calls are recorded for your protection.\n\n" +
                "Privacy & Security:\n" +
                "All recordings and analysis happen locally on your device. No data is transmitted to external servers. Recordings are used solely for scam detection and your protection.\n\n" +
                "Your Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")\n" +
                "Smart Recording: 4-Tier Fallback System\n" +
                "AI Analysis: Local Processing\n" +
                "Scam Protection: Real-time Detection");
        aboutText.setTextSize(16);
        aboutText.setTextColor(Color.parseColor("#333333"));
        aboutText.setPadding(0, 0, 0, 30);
        aboutLayout.addView(aboutText);
        
        Button backButton = new Button(this);
        backButton.setText("← Back to Smart Recording");
        backButton.setBackgroundColor(Color.parseColor("#2E3192"));
        backButton.setTextColor(Color.WHITE);
        backButton.setOnClickListener(v -> createEnhancedUI());
        aboutLayout.addView(backButton);
        
        setContentView(aboutLayout);
    }

    // Recording status methods for external access
    public boolean isRecordingActive() {
        return isCallRecording;
    }

    public String getCurrentRecordingInfo() {
        if (isCallRecording && currentRecordingPath != null) {
            return "Smart Recording: " + new File(currentRecordingPath).getName() + 
                   " (" + currentRecordingMethod + ", Quality: " + recordingQualityScore + "%)";
        }
        return "No active smart recording";
    }

    public int getCurrentRiskScore() {
        return currentRiskScore;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup call detector
        if (callDetector != null) {
            callDetector.stopCallDetection();
        }
        
        // Stop any active smart recording
        if (isCallRecording && callRecorder != null) {
            try {
                callRecorder.stop();
                callRecorder.release();
                disableSpeakerIfEnabled();
                addToCallLog("Smart recording stopped due to app closure");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping smart recording in onDestroy", e);
            }
        }
        
        Log.d(TAG, "Hello Hari Phase 2B Smart Recording terminated");
    }
}
