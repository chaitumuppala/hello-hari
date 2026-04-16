package com.hellohari;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallDetectionService extends Service {
    private static final String TAG = "CallDetectionService";
    private static final String CHANNEL_ID = "CallDetectionChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int SCAM_ALERT_ID = 2000;
    
    private TelephonyManager telephonyManager;
    private CallStateListener callStateListener;
    private MediaRecorder mediaRecorder;
    private String currentRecordingPath;
    private MultiLanguageScamDetector scamDetector;
    private RealTimeScamAnalyzer realTimeAnalyzer;
    private boolean isRecording = false;
    private int lastLiveRiskPercent = 0;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Monitoring calls"));
        
        // Initialize components
        scamDetector = new MultiLanguageScamDetector(this);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        callStateListener = new CallStateListener();
        
        // Start listening for call state changes
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        
        // Stop recording if in progress
        stopRecording();
        stopRealTimeAnalysis();
        
        // Stop listening for call state changes
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
        
        super.onDestroy();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Detection Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Used to monitor incoming calls for scam detection");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Hello Hari")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
    }
    
    private void startRecording(String phoneNumber) {
        if (isRecording) {
            Log.d(TAG, "Already recording");
            return;
        }
        
        try {
            // Create recordings directory if it doesn't exist
            File recordingsDir = new File(getExternalFilesDir(null), "recordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs();
            }
            
            // Create file name based on timestamp and phone number
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String caller = phoneNumber.replaceAll("[^0-9]", "");
            if (caller.isEmpty()) {
                caller = "unknown";
            }
            
            String fileName = timestamp + "_" + caller + ".3gp";
            currentRecordingPath = new File(recordingsDir, fileName).getAbsolutePath();
            
            // Initialize media recorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentRecordingPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            Log.d(TAG, "Started recording: " + currentRecordingPath);
            
            // Update notification
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.notify(NOTIFICATION_ID, createNotification("Recording call from " + phoneNumber));
            
        } catch (IOException e) {
            Log.e(TAG, "Error starting recording", e);
            isRecording = false;
        }
    }
    
    private void stopRecording() {
        if (!isRecording || mediaRecorder == null) {
            return;
        }
        
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            
            Log.d(TAG, "Recording stopped");
            
            // Analyze the recording
            analyzeRecording(currentRecordingPath);
            
            // Update notification
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.notify(NOTIFICATION_ID, createNotification("Monitoring calls"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        }
    }
    
    private void analyzeRecording(String recordingPath) {
        if (scamDetector != null && recordingPath != null) {
            Log.d(TAG, "Analyzing recording: " + recordingPath);
            
            // Perform preliminary analysis
            int riskScore = scamDetector.processRecording(recordingPath);
            
            // If risk score is high, notify the user
            if (riskScore > 50) {
                showScamWarningNotification(riskScore);
            }
        }
    }
    
    private void showScamWarningNotification(int riskScore) {
        String risk = scamDetector.getRiskAssessment(riskScore);
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("recording_path", currentRecordingPath);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Potential Scam Call Detected")
                .setContentText(risk + " - Risk score: " + riskScore + "%")
                .setSmallIcon(R.drawable.ic_warning)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
        
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(2000, notification);
    }
    
    private void startRealTimeAnalysis() {
        if (realTimeAnalyzer != null && realTimeAnalyzer.isStreaming()) {
            return;
        }
        realTimeAnalyzer = new RealTimeScamAnalyzer(this);
        realTimeAnalyzer.setListener(new RealTimeScamAnalyzer.AnalysisListener() {
            @Override
            public void onStatusChanged(String status, String error) {
                Log.d(TAG, "RealTime status: " + status + (error != null ? " — " + error : ""));
            }

            @Override
            public void onAnalysisResult(RealTimeScamAnalyzer.AnalysisResult result) {
                lastLiveRiskPercent = result.getRiskPercent();
                Log.d(TAG, String.format(
                        "Live analysis | risk=%d%% | scam=%s | text=%s",
                        lastLiveRiskPercent, result.isScam,
                        result.transcript.length() > 80
                                ? result.transcript.substring(0, 80) + "..."
                                : result.transcript));

                // Update ongoing call notification with live risk
                String notifText = result.isScam
                        ? "⚠ SCAM SUSPECTED — Risk " + lastLiveRiskPercent + "%"
                        : "Analyzing call — Risk " + lastLiveRiskPercent + "%";
                NotificationManager nm = getSystemService(NotificationManager.class);
                nm.notify(NOTIFICATION_ID, createNotification(notifText));

                // High-risk alert
                if (result.isScam && lastLiveRiskPercent >= 60) {
                    showLiveScamAlert(result);
                }
            }
        });
        // Default to Hindi; user can extend to detect active language later.
        realTimeAnalyzer.start("hi");
    }

    private void stopRealTimeAnalysis() {
        if (realTimeAnalyzer != null) {
            realTimeAnalyzer.stop();
            realTimeAnalyzer = null;
        }
    }

    private void showLiveScamAlert(RealTimeScamAnalyzer.AnalysisResult result) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String patterns = result.matchedPatterns.length > 0
                ? String.join(", ",
                        java.util.Arrays.copyOfRange(result.matchedPatterns, 0,
                                Math.min(3, result.matchedPatterns.length)))
                : "Suspicious language detected";

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⚠ Possible Scam Call")
                .setContentText("Risk " + result.getRiskPercent() + "% — " + patterns)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Risk " + result.getRiskPercent() + "%\nIndicators: " + patterns
                                + "\n\nBe cautious. Do not share OTPs, card numbers, or personal info."))
                .setSmallIcon(R.drawable.ic_warning)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(SCAM_ALERT_ID, notification);
    }

    /**
     * Listener for call state changes
     */
    private class CallStateListener extends PhoneStateListener {
        private boolean wasRinging = false;
        private String incomingNumber = null;
        
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.d(TAG, "Incoming call from: " + phoneNumber);
                    wasRinging = true;
                    incomingNumber = phoneNumber;
                    break;
                    
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (wasRinging) {
                        Log.d(TAG, "Call answered, starting recording + real-time analysis");
                        startRecording(incomingNumber);
                        startRealTimeAnalysis();
                    }
                    break;
                    
                case TelephonyManager.CALL_STATE_IDLE:
                    if (isRecording) {
                        Log.d(TAG, "Call ended, stopping recording");
                        stopRecording();
                    }
                    stopRealTimeAnalysis();
                    lastLiveRiskPercent = 0;
                    wasRinging = false;
                    incomingNumber = null;
                    break;
            }
        }
    }
}
