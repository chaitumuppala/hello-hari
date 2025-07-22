package com.hellohari;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RobustCallRecorder {
    private static final String TAG = "RobustCallRecorder";
    
    private Context context;
    private MediaRecorder mediaRecorder;
    private String currentRecordingPath;
    private boolean isRecording = false;
    private AudioManager audioManager;
    private RecordingStrategy currentStrategy = RecordingStrategy.NONE;
    private RecordingListener listener;
    
    // Recording strategies in order of preference
    private enum RecordingStrategy {
        NONE,
        VOICE_CALL,           // Best quality but limited device support
        VOICE_COMMUNICATION,  // Good for VoIP calls
        VOICE_RECOGNITION,    // Most compatible according to research
        MIC_WITH_SPEAKER,     // Fallback with speaker phone
        MIC_ONLY              // Last resort
    }
    
    public interface RecordingListener {
        void onRecordingStarted(String filePath, String strategy);
        void onRecordingFailed(String error);
        void onRecordingStopped(String filePath);
        void onStrategyChanged(String newStrategy);
    }
    
    public RobustCallRecorder(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
    
    public void setRecordingListener(RecordingListener listener) {
        this.listener = listener;
    }
    
    public boolean startRecording(String phoneNumber) {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress");
            return false;
        }
        
        // Prepare recording file
        if (!prepareRecordingFile(phoneNumber)) {
            notifyError("Failed to prepare recording file");
            return false;
        }
        
        // Try different recording strategies
        return tryRecordingStrategies();
    }
    
    private boolean tryRecordingStrategies() {
        RecordingStrategy[] strategies = {
            RecordingStrategy.VOICE_RECOGNITION,    // Start with most compatible
            RecordingStrategy.VOICE_COMMUNICATION,  // Try VoIP optimized
            RecordingStrategy.VOICE_CALL,          // Try traditional call recording
            RecordingStrategy.MIC_WITH_SPEAKER,    // Fallback with speaker
            RecordingStrategy.MIC_ONLY             // Last resort
        };
        
        for (RecordingStrategy strategy : strategies) {
            Log.d(TAG, "Trying recording strategy: " + strategy);
            
            if (tryRecordingStrategy(strategy)) {
                currentStrategy = strategy;
                isRecording = true;
                
                String strategyName = getStrategyDisplayName(strategy);
                Log.i(TAG, "Recording started successfully with strategy: " + strategyName);
                
                if (listener != null) {
                    listener.onRecordingStarted(currentRecordingPath, strategyName);
                }
                return true;
            }
        }
        
        // All strategies failed
        Log.e(TAG, "All recording strategies failed");
        notifyError("Recording not supported on this device");
        return false;
    }
    
    private boolean tryRecordingStrategy(RecordingStrategy strategy) {
        try {
            mediaRecorder = new MediaRecorder();
            
            // Configure based on strategy
            switch (strategy) {
                case VOICE_CALL:
                    return configureVoiceCallRecording();
                    
                case VOICE_COMMUNICATION:
                    return configureVoiceCommunicationRecording();
                    
                case VOICE_RECOGNITION:
                    return configureVoiceRecognitionRecording();
                    
                case MIC_WITH_SPEAKER:
                    return configureMicWithSpeakerRecording();
                    
                case MIC_ONLY:
                    return configureMicOnlyRecording();
                    
                default:
                    return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Strategy " + strategy + " failed: " + e.getMessage());
            cleanupRecorder();
            return false;
        }
    }
    
    private boolean configureVoiceCallRecording() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setOutputFile(currentRecordingPath);
            
            mediaRecorder.prepare();
            
            // Add delay for some devices that need it
            Thread.sleep(1000);
            
            mediaRecorder.start();
            return true;
            
        } catch (Exception e) {
            Log.d(TAG, "VOICE_CALL not supported: " + e.getMessage());
            return false;
        }
    }
    
    private boolean configureVoiceCommunicationRecording() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
            mediaRecorder.setAudioSamplingRate(48000);
            mediaRecorder.setAudioEncodingBitRate(192000);
            mediaRecorder.setOutputFile(currentRecordingPath);
            
            mediaRecorder.prepare();
            Thread.sleep(500);
            mediaRecorder.start();
            return true;
            
        } catch (Exception e) {
            Log.d(TAG, "VOICE_COMMUNICATION not supported: " + e.getMessage());
            return false;
        }
    }
    
    private boolean configureVoiceRecognitionRecording() {
        try {
            // Most compatible strategy according to research
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setOutputFile(currentRecordingPath);
            
            mediaRecorder.prepare();
            Thread.sleep(500);
            mediaRecorder.start();
            return true;
            
        } catch (Exception e) {
            Log.d(TAG, "VOICE_RECOGNITION not supported: " + e.getMessage());
            return false;
        }
    }
    
    private boolean configureMicWithSpeakerRecording() {
        try {
            // Enable speaker phone for better audio capture
            enableSpeakerPhone();
            
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setOutputFile(currentRecordingPath);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            if (listener != null) {
                listener.onStrategyChanged("Recording via microphone with speaker phone");
            }
            
            return true;
            
        } catch (Exception e) {
            Log.d(TAG, "MIC_WITH_SPEAKER failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean configureMicOnlyRecording() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentRecordingPath);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            if (listener != null) {
                listener.onStrategyChanged("Recording via microphone only - limited quality");
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Even MIC_ONLY failed: " + e.getMessage());
            return false;
        }
    }
    
    private void enableSpeakerPhone() {
        if (audioManager != null) {
            // Save current audio state
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
            
            // Enable speaker phone
            audioManager.setSpeakerphoneOn(true);
            
            // Increase volume for better recording
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0);
            
            Log.d(TAG, "Speaker phone enabled, volume set to maximum");
        }
    }
    
    private void disableSpeakerPhone() {
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(false);
            Log.d(TAG, "Speaker phone disabled");
        }
    }
    
    public boolean stopRecording() {
        if (!isRecording || mediaRecorder == null) {
            Log.w(TAG, "No recording in progress");
            return false;
        }
        
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            
            // Disable speaker phone if it was enabled
            if (currentStrategy == RecordingStrategy.MIC_WITH_SPEAKER) {
                disableSpeakerPhone();
            }
            
            Log.i(TAG, "Recording stopped successfully");
            
            if (listener != null) {
                listener.onRecordingStopped(currentRecordingPath);
            }
            
            String path = currentRecordingPath;
            currentRecordingPath = null;
            currentStrategy = RecordingStrategy.NONE;
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage());
            cleanupRecorder();
            return false;
        }
    }
    
    private boolean prepareRecordingFile(String phoneNumber) {
        try {
            // Create recordings directory in app's private storage
            File recordingsDir = new File(context.getFilesDir(), "call_recordings");
            if (!recordingsDir.exists() && !recordingsDir.mkdirs()) {
                Log.e(TAG, "Failed to create recordings directory");
                return false;
            }
            
            // Generate filename with timestamp and phone number
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String safeNumber = phoneNumber != null ? phoneNumber.replaceAll("[^0-9+]", "") : "unknown";
            String fileName = "call_" + timestamp + "_" + safeNumber + ".m4a";
            
            currentRecordingPath = new File(recordingsDir, fileName).getAbsolutePath();
            
            Log.d(TAG, "Recording file prepared: " + currentRecordingPath);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare recording file: " + e.getMessage());
            return false;
        }
    }
    
    private void cleanupRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaRecorder: " + e.getMessage());
            }
            mediaRecorder = null;
        }
        isRecording = false;
        currentStrategy = RecordingStrategy.NONE;
        
        // Disable speaker phone if enabled
        disableSpeakerPhone();
    }
    
    private String getStrategyDisplayName(RecordingStrategy strategy) {
        switch (strategy) {
            case VOICE_CALL:
                return "Voice Call (High Quality)";
            case VOICE_COMMUNICATION:
                return "Voice Communication (VoIP Optimized)";
            case VOICE_RECOGNITION:
                return "Voice Recognition (Most Compatible)";
            case MIC_WITH_SPEAKER:
                return "Microphone with Speaker";
            case MIC_ONLY:
                return "Microphone Only (Limited)";
            default:
                return "Unknown";
        }
    }
    
    private void notifyError(String error) {
        if (listener != null) {
            listener.onRecordingFailed(error);
        }
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public String getCurrentRecordingPath() {
        return currentRecordingPath;
    }
    
    public String getCurrentStrategy() {
        return currentStrategy != RecordingStrategy.NONE ? 
               getStrategyDisplayName(currentStrategy) : "Not Recording";
    }
    
    // Diagnostic method to test what audio sources are available
    public String testAudioSources() {
        StringBuilder report = new StringBuilder();
        report.append("=== AUDIO SOURCE COMPATIBILITY TEST ===\n");
        report.append("Device: ").append(android.os.Build.MANUFACTURER)
              .append(" ").append(android.os.Build.MODEL).append("\n");
        report.append("Android: ").append(android.os.Build.VERSION.SDK_INT).append("\n\n");
        
        int[] audioSources = {
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.CAMCORDER
        };
        
        String[] sourceNames = {
            "MIC", "VOICE_RECOGNITION", "VOICE_COMMUNICATION", "VOICE_CALL", "CAMCORDER"
        };
        
        for (int i = 0; i < audioSources.length; i++) {
            boolean supported = testAudioSource(audioSources[i]);
            report.append(sourceNames[i]).append(": ")
                  .append(supported ? "✅ SUPPORTED" : "❌ NOT SUPPORTED").append("\n");
        }
        
        return report.toString();
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
}
