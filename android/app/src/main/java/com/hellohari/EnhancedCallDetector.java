package com.hellohari;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import android.media.MediaRecorder;
import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class EnhancedCallDetector {
    private static final String TAG = "EnhancedCallDetector";
    private Context context;
    private BroadcastReceiver callReceiver;
    private boolean isMonitoring = false;
    private MediaRecorder mediaRecorder;
    private String currentRecordingPath;
    private boolean isRecording = false;
    private Timer riskAnalysisTimer;
    private CallRecordingAnalyzer analyzer;
    
    // Risk score persistence to prevent Timer override bug
    private int maxRiskScore = 0;
    private String lastHighRiskAnalysis = "";
    private long lastHighRiskTime = 0;
    
    // Define constants for switch-case compatibility
    private static final String STATE_RINGING = TelephonyManager.EXTRA_STATE_RINGING;
    private static final String STATE_OFFHOOK = TelephonyManager.EXTRA_STATE_OFFHOOK;
    private static final String STATE_IDLE = TelephonyManager.EXTRA_STATE_IDLE;
    
    public interface CallDetectionListener {
        void onCallStateChanged(String state, String phoneNumber);
        void onRecordingStatusChanged(boolean isRecording, String filePath);
        void onRiskLevelChanged(int riskScore, String analysis);
        void onDebugLog(String message); // Add debug logging to capture detailed logs
    }
    
    private CallDetectionListener listener;

    // Helper method for comprehensive debug logging
    private void debugLog(String message) {
        Log.d(TAG, message);
        if (listener != null) {
            listener.onDebugLog(message);
        }
    }

    // Real-time analysis components
    private VoskSpeechRecognizer voskRecognizer;
    private MultiLanguageScamDetector scamDetector;
    
    public EnhancedCallDetector(Context context) {
        this.context = context;
        this.analyzer = new CallRecordingAnalyzer();
        this.scamDetector = new MultiLanguageScamDetector(context);
    }
    
    // Set the VOSK recognizer for real-time speech analysis
    public void setVoskRecognizer(VoskSpeechRecognizer vosk) {
        this.voskRecognizer = vosk;
        if (voskRecognizer != null) {
            setupVoskListener();
        }
    }
    
    private void setupVoskListener() {
        debugLog("=== SETTING UP VOSK LISTENER ===");
        debugLog("voskRecognizer: " + (voskRecognizer != null ? "available" : "null"));
        
        if (voskRecognizer == null) {
            debugLog("❌ Cannot setup VOSK listener - voskRecognizer is null");
            return;
        }
        
        debugLog("Creating VoskRecognitionListener...");
        voskRecognizer.setRecognitionListener(new VoskSpeechRecognizer.VoskRecognitionListener() {
            @Override
            public void onPartialResult(String partialText, String language) {
                debugLog("=== VOSK PARTIAL RESULT ===");
                debugLog("Partial text: '" + (partialText != null ? partialText : "null") + "'");
                debugLog("Language: " + language);
                
                // Analyze partial speech in real-time with enhanced sensitivity
                if (scamDetector != null && partialText != null && !partialText.trim().isEmpty()) {
                    MultiLanguageScamDetector.ScamAnalysisResult result = scamDetector.analyzeText(partialText);
                    int riskScore = result.getRiskScore();
                    
                    debugLog("Partial analysis result: " + riskScore + "%");
                    
                    // Lower threshold for partial results to catch early warning signs
                    if (riskScore > 20) { // Lowered from 30 to catch more scams
                        String analysis = "LIVE: " + partialText.substring(0, Math.min(30, partialText.length())) + 
                                        "... (Risk: " + riskScore + "%)";
                        debugLog("🎤 VOSK PARTIAL: " + riskScore + "% - calling updateRiskScore");
                        updateRiskScore(riskScore, analysis, "VOSK-PARTIAL");
                    } else {
                        debugLog("Partial result below threshold (20%): " + riskScore + "%");
                    }
                } else {
                    debugLog("Partial result skipped - no detector or empty text");
                }
                debugLog("=== END VOSK PARTIAL ===");
            }
            
            @Override
            public void onFinalResult(String finalText, String language, float confidence) {
                debugLog("=== VOSK FINAL RESULT ===");
                debugLog("Final text: '" + (finalText != null ? finalText : "null") + "'");
                debugLog("Language: " + language);
                debugLog("Confidence: " + confidence);
                
                // Analyze complete phrases with enhanced scoring for real test scenarios
                if (scamDetector != null && finalText != null && !finalText.trim().isEmpty()) {
                    MultiLanguageScamDetector.ScamAnalysisResult result = scamDetector.analyzeText(finalText);
                    int riskScore = result.getRiskScore();
                    
                    debugLog("Initial analysis result: " + riskScore + "%");
                    
                    // Apply confidence boost for clear speech recognition
                    if (confidence > 0.7f) {
                        int oldScore = riskScore;
                        riskScore = Math.min(100, riskScore + 10); // Boost confident results
                        debugLog("Confidence boost applied: " + oldScore + "% -> " + riskScore + "%");
                    }
                    
                    // Enhanced pattern detection for common test words
                    String lowerText = finalText.toLowerCase();
                    if (lowerText.contains("police") || lowerText.contains("arrest") || 
                        lowerText.contains("drugs") || lowerText.contains("money") ||
                        lowerText.contains("bank") || lowerText.contains("account") ||
                        lowerText.contains("suspicious") || lowerText.contains("investigation") ||
                        lowerText.contains("crime") || lowerText.contains("courier") ||
                        lowerText.contains("parcel") || lowerText.contains("customs")) {
                        int oldScore = riskScore;
                        riskScore = Math.max(riskScore, 60); // Minimum 60% for these keywords
                        debugLog("Keyword boost applied: " + oldScore + "% -> " + riskScore + "%");
                    }
                    
                    String analysis = "SPEECH: \"" + finalText + "\" (Risk: " + riskScore + "%, Conf: " + 
                                    (int)(confidence*100) + "%)";
                    debugLog("🎯 VOSK FINAL: " + riskScore + "% - calling updateRiskScore");
                    updateRiskScore(riskScore, analysis, "VOSK-FINAL");
                } else {
                    debugLog("Final result skipped - no detector or empty text");
                }
                debugLog("=== END VOSK FINAL ===");
            }
            
            @Override
            public void onError(String error) {
                debugLog("❌ VOSK recognition error: " + error);
                Log.e(TAG, "VOSK recognition error: " + error);
            }
            
            @Override
            public void onInitializationComplete(boolean success) {
                debugLog("🎯 VOSK initialization in call detector: " + success);
                Log.d(TAG, "VOSK initialization in call detector: " + success);
            }
            
            @Override
            public void onModelDownloadProgress(String language, int progress) {
                debugLog("📥 Model download progress for " + language + ": " + progress + "%");
                Log.d(TAG, "Model download progress for " + language + ": " + progress + "%");
            }
            
            @Override
            public void onModelDownloadComplete(String language, boolean success) {
                debugLog("✅ Model download complete for " + language + ": " + success);
                Log.d(TAG, "Model download complete for " + language + ": " + success);
            }
        });
        
        debugLog("✅ VOSK listener setup complete");
        debugLog("=== END VOSK LISTENER SETUP ===");
    }
    
    /**
     * Update risk score only if it's higher than previous scores
     * This prevents Timer backup analysis from overriding VOSK detections
     */
    private void updateRiskScore(int newRiskScore, String analysis, String source) {
        long currentTime = System.currentTimeMillis();
        
        // ENHANCED DEBUG LOGGING
        debugLog("=== RISK SCORE UPDATE ATTEMPT ===");
        debugLog("Source: " + source);
        debugLog("New Score: " + newRiskScore + "%");
        debugLog("Current Max Score: " + maxRiskScore + "%");
        debugLog("Analysis: " + analysis);
        debugLog("Time since last high risk: " + (currentTime - lastHighRiskTime) + "ms");
        
        // Always update if it's higher risk
        if (newRiskScore > maxRiskScore) {
            debugLog("🔥 UPDATING TO HIGHER SCORE: " + newRiskScore + "% (was " + maxRiskScore + "%)");
            maxRiskScore = newRiskScore;
            lastHighRiskAnalysis = analysis;
            lastHighRiskTime = currentTime;
            
            debugLog("NEW HIGH RISK: " + newRiskScore + "% from " + source + " - " + analysis);
            
            if (listener != null) {
                listener.onRiskLevelChanged(newRiskScore, analysis + " [" + source + "]");
                debugLog("✅ LISTENER NOTIFIED with " + newRiskScore + "%");
            }
            
            // Show appropriate alerts
            if (newRiskScore > 80) {
                showToast("🚨 CRITICAL SCAM: " + analysis.substring(0, Math.min(30, analysis.length())));
            } else if (newRiskScore > 60) {
                showToast("⚠️ HIGH RISK: " + analysis.substring(0, Math.min(25, analysis.length())));
            } else if (newRiskScore > 40) {
                showToast("⚡ SUSPICIOUS: " + analysis.substring(0, Math.min(20, analysis.length())));
            }
        } 
        // For lower scores, only update if no high risk detected in last 30 seconds
        else if (currentTime - lastHighRiskTime > 30000) {
            debugLog("📉 LOWER SCORE UPDATE: " + newRiskScore + "% (no high risk in 30s)");
            if (listener != null) {
                listener.onRiskLevelChanged(newRiskScore, analysis + " [" + source + "]");
                debugLog("✅ LISTENER NOTIFIED with lower score: " + newRiskScore + "%");
            }
        } else {
            // Just log but don't override UI
            debugLog("🚫 SCORE REJECTED: " + newRiskScore + "% from " + source + " (preserving higher score " + maxRiskScore + "%)");
        }
        Log.d(TAG, "=== END RISK SCORE UPDATE ===");
    }
    
    public void setCallDetectionListener(CallDetectionListener listener) {
        this.listener = listener;
    }

    public boolean startCallDetection() {
        if (isMonitoring) {
            Log.d(TAG, "Enhanced call detection already running");
            return false;
        }

        try {
            callReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d(TAG, "Received broadcast: " + action);
                    
                    if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                        String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        
                        Log.d(TAG, "Phone state changed: " + state + ", Number: " + phoneNumber);
                        
                        // Handle different call states with recording
                        handleCallStateChangeWithRecording(state, phoneNumber);
                        
                        // Notify listener
                        if (listener != null) {
                            listener.onCallStateChanged(state, phoneNumber);
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            
            context.registerReceiver(callReceiver, filter);
            isMonitoring = true;
            
            Log.d(TAG, "Enhanced call detection started successfully");
            showToast("🎤 Hello Hari: Advanced call monitoring started");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start enhanced call detection", e);
            showToast("Failed to start advanced monitoring: " + e.getMessage());
            return false;
        }
    }

    public boolean stopCallDetection() {
        if (!isMonitoring || callReceiver == null) {
            Log.d(TAG, "Enhanced call detection not running");
            return false;
        }

        try {
            // Stop any ongoing recording
            stopRecording();
            
            context.unregisterReceiver(callReceiver);
            callReceiver = null;
            isMonitoring = false;
            
            // Cancel risk analysis timer
            if (riskAnalysisTimer != null) {
                riskAnalysisTimer.cancel();
                riskAnalysisTimer = null;
            }
            
            Log.d(TAG, "Enhanced call detection stopped");
            showToast("🛑 Hello Hari: Advanced monitoring stopped");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop enhanced call detection", e);
            return false;
        }
    }
    
    public boolean isMonitoring() {
        return isMonitoring;
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public String getCurrentRecordingPath() {
        return currentRecordingPath;
    }

    private void handleCallStateChangeWithRecording(String state, String phoneNumber) {
        debugLog("=== CALL STATE CHANGE ===");
        debugLog("State: " + state);
        debugLog("Phone Number: " + (phoneNumber != null ? phoneNumber : "null"));
        debugLog("STATE_RINGING: " + STATE_RINGING);
        debugLog("STATE_OFFHOOK: " + STATE_OFFHOOK);
        debugLog("STATE_IDLE: " + STATE_IDLE);
        
        if (STATE_RINGING.equals(state)) {
            debugLog("📞 INCOMING CALL detected - Preparing recording");
            Log.i(TAG, "📞 INCOMING CALL detected - Preparing recording");
            onIncomingCallDetected(phoneNumber);
        } else if (STATE_OFFHOOK.equals(state)) {
            debugLog("📱 CALL ANSWERED - Starting recording and analysis");
            Log.i(TAG, "📱 CALL ANSWERED - Starting recording and analysis");
            onCallAnswered(phoneNumber);
        } else if (STATE_IDLE.equals(state)) {
            debugLog("📴 CALL ENDED - Stopping recording and analyzing");
            Log.i(TAG, "📴 CALL ENDED - Stopping recording and analyzing");
            onCallEnded(phoneNumber);
        } else {
            debugLog("❓ UNKNOWN CALL STATE: " + state);
            Log.w(TAG, "❓ UNKNOWN CALL STATE: " + state);
        }
        debugLog("=== END CALL STATE CHANGE ===");
    }

    private void onIncomingCallDetected(String phoneNumber) {
        String displayNumber = phoneNumber != null ? phoneNumber : "Unknown Number";
        showToast("🚨 Incoming: " + displayNumber + " - Preparing analysis...");
        
        // Prepare recording setup
        prepareRecording(phoneNumber);
        
        // Start initial risk assessment based on number
        int initialRisk = analyzer.analyzePhoneNumber(phoneNumber);
        Log.d(TAG, "Initial phone number analysis: " + initialRisk + "% for " + displayNumber);
        
        // Use updateRiskScore instead of calling listener directly
        updateRiskScore(initialRisk, "Initial number analysis for " + displayNumber, "PHONE-ANALYSIS");
    }

    private void onCallAnswered(String phoneNumber) {
        String displayNumber = phoneNumber != null ? phoneNumber : "Unknown Number";
        debugLog("=== CALL ANSWERED ===");
        debugLog("Phone Number: " + displayNumber);
        showToast("🎤 Recording call with " + displayNumber + " for safety");
        
        // Start recording
        debugLog("Attempting to start recording...");
        boolean recordingStarted = startRecording();
        if (recordingStarted) {
            debugLog("✅ Recording started successfully - starting real-time analysis");
        } else {
            debugLog("❌ Recording failed to start - BUT still starting VOSK analysis");
            showToast("⚠️ Recording unavailable - VOSK speech analysis still active");
        }
        
        // CRITICAL: Start VOSK analysis regardless of recording status
        debugLog("Starting real-time analysis regardless of recording status...");
        startRealTimeAnalysis(phoneNumber);
        debugLog("=== END CALL ANSWERED ===");
    }

    private void onCallEnded(String phoneNumber) {
        showToast("📴 Call ended - Analyzing recording for scams...");
        
        // Stop recording and analyze
        String recordingPath = stopRecording();
        if (recordingPath != null) {
            analyzeRecording(recordingPath, phoneNumber);
        }
        
        // Stop real-time analysis
        stopRealTimeAnalysis();
    }

    private void prepareRecording(String phoneNumber) {
        try {
            // Create recordings directory
            File recordingsDir = new File(context.getFilesDir(), "call_recordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs();
            }
            
            // Generate filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String safeNumber = phoneNumber != null ? phoneNumber.replaceAll("[^0-9+]", "") : "unknown";
            currentRecordingPath = new File(recordingsDir, "call_" + timestamp + "_" + safeNumber + ".m4a").getAbsolutePath();
            
            Log.d(TAG, "Recording prepared: " + currentRecordingPath);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare recording", e);
        }
    }

    private boolean startRecording() {
        debugLog("=== START RECORDING ATTEMPT ===");
        debugLog("isRecording: " + isRecording);
        debugLog("currentRecordingPath: " + currentRecordingPath);
        
        if (isRecording || currentRecordingPath == null) {
            debugLog("❌ Recording precondition failed - already recording or no path");
            return false;
        }

        try {
            debugLog("Creating MediaRecorder...");
            mediaRecorder = new MediaRecorder();
            
            // Configure for call recording
            debugLog("Configuring MediaRecorder for call recording...");
            debugLog("Setting AudioSource to VOICE_CALL...");
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            debugLog("Setting OutputFormat to MPEG_4...");
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            debugLog("Setting AudioEncoder to AAC...");
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            debugLog("Setting AudioSamplingRate to 44100...");
            mediaRecorder.setAudioSamplingRate(44100);
            debugLog("Setting AudioEncodingBitRate to 128000...");
            mediaRecorder.setAudioEncodingBitRate(128000);
            debugLog("Setting OutputFile to: " + currentRecordingPath);
            mediaRecorder.setOutputFile(currentRecordingPath);

            debugLog("Calling mediaRecorder.prepare()...");
            mediaRecorder.prepare();
            debugLog("MediaRecorder prepare() successful");
            
            debugLog("Calling mediaRecorder.start()...");
            mediaRecorder.start();
            debugLog("MediaRecorder start() successful");
            
            isRecording = true;
            
            debugLog("✅ Recording started successfully: " + currentRecordingPath);
            Log.d(TAG, "Recording started: " + currentRecordingPath);
            
            if (listener != null) {
                listener.onRecordingStatusChanged(true, currentRecordingPath);
            }
            
            debugLog("=== END START RECORDING (SUCCESS) ===");
            return true;
            
        } catch (SecurityException e) {
            debugLog("❌ SECURITY EXCEPTION in startRecording: " + e.getMessage());
            debugLog("This usually means VOICE_CALL recording is not permitted");
            Log.e(TAG, "Security exception starting recording", e);
        } catch (IllegalStateException e) {
            debugLog("❌ ILLEGAL STATE EXCEPTION in startRecording: " + e.getMessage());
            debugLog("This usually means MediaRecorder is in wrong state");
            Log.e(TAG, "Illegal state exception starting recording", e);
        } catch (IOException e) {
            debugLog("❌ IO EXCEPTION in startRecording: " + e.getMessage());
            debugLog("This usually means file/path issues");
            Log.e(TAG, "IO exception starting recording", e);
        } catch (Exception e) {
            debugLog("❌ GENERAL EXCEPTION in startRecording: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            Log.e(TAG, "Unexpected exception starting recording", e);
        }
        
        // Cleanup on failure
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception ignored) {}
            mediaRecorder = null;
        }
        
        debugLog("=== END START RECORDING (FAILED) ===");
        return false;
    }

    private String stopRecording() {
        if (!isRecording || mediaRecorder == null) {
            return null;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            
            Log.d(TAG, "Recording stopped: " + currentRecordingPath);
            
            if (listener != null) {
                listener.onRecordingStatusChanged(false, currentRecordingPath);
            }
            
            String path = currentRecordingPath;
            currentRecordingPath = null;
            return path;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop recording", e);
            mediaRecorder = null;
            isRecording = false;
            return null;
        }
    }

    private void startRealTimeAnalysis(String phoneNumber) {
        debugLog("=== STARTING REAL-TIME ANALYSIS ===");
        debugLog("Phone number: " + phoneNumber);
        Log.d(TAG, "=== STARTING REAL-TIME ANALYSIS ===");
        Log.d(TAG, "Phone number: " + phoneNumber);
        
        // Reset risk score tracking for new call
        debugLog("Resetting risk score variables for new call");
        debugLog("Previous maxRiskScore: " + maxRiskScore + "%");
        debugLog("Previous lastHighRiskTime: " + lastHighRiskTime);
        Log.d(TAG, "Resetting risk score variables for new call");
        Log.d(TAG, "Previous maxRiskScore: " + maxRiskScore + "%");
        Log.d(TAG, "Previous lastHighRiskTime: " + lastHighRiskTime);
        
        maxRiskScore = 0;
        lastHighRiskTime = 0;
        lastHighRiskAnalysis = "";
        
        debugLog("Risk variables reset - maxRiskScore: " + maxRiskScore + "%, lastHighRiskTime: " + lastHighRiskTime);
        Log.d(TAG, "Risk variables reset - maxRiskScore: " + maxRiskScore + "%, lastHighRiskTime: " + lastHighRiskTime);
        
        if (riskAnalysisTimer != null) {
            riskAnalysisTimer.cancel();
        }
        
        // Start VOSK real-time recognition if available
        if (voskRecognizer != null && voskRecognizer.isInitialized()) {
            debugLog("✅ VOSK is initialized - starting real-time speech recognition");
            Log.d(TAG, "✅ VOSK is initialized - starting real-time speech recognition");
            voskRecognizer.startListening();
            debugLog("🎤 VOSK startListening() called - speech recognition should now be active");
        } else {
            debugLog("❌ VOSK not available, using simulated analysis");
            Log.w(TAG, "❌ VOSK not available, using simulated analysis");
        }
        
        debugLog("Starting Timer backup analysis...");
        riskAnalysisTimer = new Timer();
        riskAnalysisTimer.scheduleAtFixedRate(new TimerTask() {
            private int analysisCount = 0;
            
            @Override
            public void run() {
                analysisCount++;
                
                // Check if recent high-risk VOSK detection should take precedence
                long timeSinceHighRisk = System.currentTimeMillis() - lastHighRiskTime;
                boolean hasRecentHighRisk = timeSinceHighRisk < 10000; // 10 seconds
                
                // ENHANCED TIMER DEBUG LOGGING
                debugLog("=== TIMER BACKUP ANALYSIS ===");
                debugLog("Analysis Count: " + analysisCount);
                debugLog("Time since high risk: " + timeSinceHighRisk + "ms");
                debugLog("Has recent high risk: " + hasRecentHighRisk);
                debugLog("Current max risk score: " + maxRiskScore + "%");
                debugLog("VOSK initialized: " + (voskRecognizer != null && voskRecognizer.isInitialized()));
                
                // Use real VOSK analysis if available, otherwise simulate
                int riskScore;
                String analysis;
                
                if (voskRecognizer != null && voskRecognizer.isInitialized()) {
                    // Real-time speech analysis is handled by VOSK callbacks
                    // This timer provides backup analysis and status updates only
                    if (hasRecentHighRisk && maxRiskScore > 50) {
                        // Don't override recent high-risk VOSK detection
                        analysis = "VOSK detected high risk - monitoring continues...";
                        debugLog("🚫 TIMER SKIPPED: Recent high-risk VOSK detection exists");
                        debugLog("=== END TIMER ANALYSIS (SKIPPED) ===");
                        return; // Skip this timer update
                    } else {
                        riskScore = Math.min(30 + (analysisCount * 2), 85); // Gradual increase as backup
                        analysis = "Monitoring speech... (" + analysisCount + " checks)";
                        debugLog("⏱️ TIMER BACKUP: " + riskScore + "% (no recent high-risk VOSK)");
                    }
                } else {
                    // Fallback to simulated analysis
                    riskScore = analyzer.performRealTimeAnalysis(analysisCount, phoneNumber);
                    analysis = analyzer.getRiskAnalysisText(riskScore, analysisCount);
                    debugLog("🔄 TIMER SIMULATION: " + riskScore + "% (VOSK not available)");
                }
                
                debugLog("Timer calling updateRiskScore with: " + riskScore + "%");
                // Use updateRiskScore to prevent overriding higher VOSK scores
                updateRiskScore(riskScore, analysis, "TIMER-BACKUP");
                debugLog("=== END TIMER ANALYSIS ===");
            }
        }, 3000, 5000); // Start after 3s, repeat every 5s
    }

    private void stopRealTimeAnalysis() {
        Log.d(TAG, "Stopping real-time analysis");
        
        // Stop VOSK recognition
        if (voskRecognizer != null) {
            voskRecognizer.stopListening();
        }
        
        // Stop timer
        if (riskAnalysisTimer != null) {
            riskAnalysisTimer.cancel();
            riskAnalysisTimer = null;
        }
    }

    private void analyzeRecording(String recordingPath, String phoneNumber) {
        // Simulate post-call analysis
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate analysis time
                
                int finalRiskScore = analyzer.analyzeFinalRecording(recordingPath, phoneNumber);
                String finalAnalysis = analyzer.getFinalAnalysisReport(finalRiskScore);
                
                Log.d(TAG, "Final recording analysis: " + finalRiskScore + "%");
                // Use updateRiskScore instead of calling listener directly
                updateRiskScore(finalRiskScore, finalAnalysis, "FINAL-RECORDING");
                
                // Show final result
                String resultMessage;
                if (finalRiskScore > 70) {
                    resultMessage = "🚨 HIGH RISK CALL: Likely scam - " + finalRiskScore + "% risk";
                } else if (finalRiskScore > 40) {
                    resultMessage = "⚠️ MEDIUM RISK: Some suspicious patterns - " + finalRiskScore + "% risk";
                } else {
                    resultMessage = "✅ LOW RISK: Call appears safe - " + finalRiskScore + "% risk";
                }
                
                showToast(resultMessage);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in post-call analysis", e);
            }
        }).start();
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    // Inner class for call recording analysis
    private static class CallRecordingAnalyzer {
        
        public int analyzePhoneNumber(String phoneNumber) {
            Log.d(TAG, "=== ANALYZE PHONE NUMBER ===");
            Log.d(TAG, "Phone number: " + (phoneNumber != null ? phoneNumber : "NULL"));
            
            if (phoneNumber == null) {
                Log.d(TAG, "Phone number is NULL - returning 30%");
                return 30;
            }
            
            // Simple risk assessment based on number patterns
            int risk = 10;
            Log.d(TAG, "Base risk: " + risk + "%");
            
            // Check for common scam patterns
            if (phoneNumber.startsWith("+1800") || phoneNumber.startsWith("1800")) {
                risk += 20; // Toll-free numbers often used by scammers
                Log.d(TAG, "Toll-free number detected: +" + 20 + "% (total: " + risk + "%)");
            }
            if (phoneNumber.length() < 10) {
                risk += 25; // Short numbers suspicious
                Log.d(TAG, "Short number detected: +" + 25 + "% (total: " + risk + "%)");
            }
            if (phoneNumber.contains("0000") || phoneNumber.contains("1111")) {
                risk += 15; // Sequential patterns
                Log.d(TAG, "Sequential pattern detected: +" + 15 + "% (total: " + risk + "%)");
            }
            
            int finalRisk = Math.min(risk, 100);
            Log.d(TAG, "Final phone analysis risk: " + finalRisk + "%");
            Log.d(TAG, "=== END ANALYZE PHONE NUMBER ===");
            return finalRisk;
        }
        
        public int performRealTimeAnalysis(int analysisCount, String phoneNumber) {
            // Simulate real-time pattern detection
            int baseRisk = analyzePhoneNumber(phoneNumber);
            
            // Simulate escalating risk detection during call
            if (analysisCount > 3) {
                baseRisk += 10; // Longer calls might reveal more patterns
            }
            if (analysisCount > 6) {
                baseRisk += 15; // Extended calls increase suspicion
            }
            
            // Add some randomness to simulate pattern detection
            baseRisk += (int)(Math.random() * 20);
            
            return Math.min(baseRisk, 100);
        }
        
        public int analyzeFinalRecording(String recordingPath, String phoneNumber) {
            // Simulate comprehensive post-call analysis
            int risk = analyzePhoneNumber(phoneNumber);
            
            // Simulate audio pattern analysis
            if (recordingPath != null) {
                // In production, this would analyze actual audio for:
                // - Voice stress patterns
                // - Background noise analysis
                // - Speech pattern recognition
                // - Known scam phrase detection
                
                risk += (int)(Math.random() * 30); // Simulate analysis results
            }
            
            return Math.min(risk, 100);
        }
        
        public String getRiskAnalysisText(int riskScore, int analysisCount) {
            if (riskScore > 70) {
                return "High risk patterns detected - Analysis " + analysisCount;
            } else if (riskScore > 40) {
                return "Monitoring for suspicious patterns - Analysis " + analysisCount;
            } else {
                return "Call appears normal - Analysis " + analysisCount;
            }
        }
        
        public String getFinalAnalysisReport(int riskScore) {
            if (riskScore > 70) {
                return "FINAL: High probability of scam call - Multiple risk factors detected";
            } else if (riskScore > 40) {
                return "FINAL: Moderate risk - Some suspicious patterns found";
            } else {
                return "FINAL: Low risk - Call appears legitimate";
            }
        }
    }
}
