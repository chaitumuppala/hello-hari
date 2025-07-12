package com.hellohari;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * SimpleCallDetector - A class to detect incoming and outgoing calls
 * with improved error handling and memory management.
 */
public class SimpleCallDetector {
    private static final String TAG = "SimpleCallDetector";
    private final WeakReference<Context> contextRef;
    private TelephonyManager telephonyManager;
    private CallStateListener callStateListener;
    private CallDetectionListener detectionListener;
    
    // Track call state between callbacks
    private int lastState = TelephonyManager.CALL_STATE_IDLE;
    private String lastNumber = "";
    private long callStartTime = 0;
    private boolean isIncoming = false;

    /**
     * Interface for call detection callback events
     */
    public interface CallDetectionListener {
        void onIncomingCallStarted(String number, String time);
        void onIncomingCallEnded(String number, String time);
        void onOutgoingCallStarted(String number, String time);
        void onOutgoingCallEnded(String number, String time);
        void onMissedCall(String number, String time);
        void onCallStateChanged(String number, String state);
        
        // Deprecated methods - included for backward compatibility
        void onCallStarted(String phoneNumber);
        void onCallEnded(String phoneNumber);
    }

    /**
     * Constructor for SimpleCallDetector
     * @param context Application context
     */
    public SimpleCallDetector(Context context) {
        this.contextRef = new WeakReference<>(context);
        Context ctx = contextRef.get();
        if (ctx != null) {
            try {
                telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
                Log.d(TAG, "SimpleCallDetector initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing telephony manager", e);
            }
        } else {
            Log.e(TAG, "Context is null, cannot initialize telephony manager");
        }
    }

    /**
     * Set the call detection listener for callbacks
     * @param listener CallDetectionListener implementation
     */
    public void setCallDetectionListener(CallDetectionListener listener) {
        this.detectionListener = listener;
    }

    /**
     * Start monitoring phone state
     */
    public void startMonitoring() {
        try {
            if (callStateListener == null && telephonyManager != null) {
                callStateListener = new CallStateListener();
                telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                Log.d(TAG, "Started call monitoring");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting call monitoring", e);
        }
    }
    
    /**
     * Stop monitoring phone state
     */
    public void stopMonitoring() {
        try {
            if (callStateListener != null && telephonyManager != null) {
                telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
                callStateListener = null;
                Log.d(TAG, "Stopped call monitoring");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping call monitoring", e);
        }
    }
    
    // Aliases for startMonitoring and stopMonitoring for API consistency
    public void startCallDetection() {
        startMonitoring();
    }
    
    public void stopCallDetection() {
        stopMonitoring();
    }

    /**
     * Get formatted time string for current time
     * @return Formatted time string
     */
    private String getCurrentTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * Convert milliseconds to formatted duration
     * @param millis Duration in milliseconds
     * @return Formatted duration string
     */
    private String formatCallDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    /**
     * Phone state listener to detect call events
     */
    private class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            try {
                if (detectionListener == null) {
                    return;
                }
                
                // Clean phone number
                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    phoneNumber = "unknown";
                }
                
                // Report raw state change
                String stateStr = getStateString(state);
                detectionListener.onCallStateChanged(phoneNumber, stateStr);
                
                // Handle call state transitions
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        isIncoming = true;
                        callStartTime = System.currentTimeMillis();
                        lastNumber = phoneNumber;
                        detectionListener.onIncomingCallStarted(phoneNumber, getCurrentTimeString());
                        break;
                        
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        // Transition from IDLE to OFFHOOK = outgoing call
                        if (lastState == TelephonyManager.CALL_STATE_IDLE) {
                            isIncoming = false;
                            callStartTime = System.currentTimeMillis();
                            detectionListener.onOutgoingCallStarted(phoneNumber, getCurrentTimeString());
                        }
                        break;
                        
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Call ended
                        if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                            // Missed call
                            detectionListener.onMissedCall(lastNumber, getCurrentTimeString());
                        } else if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                            // Call ended
                            long callDuration = System.currentTimeMillis() - callStartTime;
                            String durationStr = formatCallDuration(callDuration);
                            
                            if (isIncoming) {
                                detectionListener.onIncomingCallEnded(lastNumber, durationStr);
                            } else {
                                detectionListener.onOutgoingCallEnded(lastNumber, durationStr);
                            }
                        }
                        break;
                }
                
                lastState = state;
                
            } catch (Exception e) {
                Log.e(TAG, "Error in call state changed", e);
            }
        }
        
        /**
         * Convert phone state integer to readable string
         * @param state Phone state
         * @return Readable string representation
         */
        private String getStateString(int state) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE: return "IDLE";
                case TelephonyManager.CALL_STATE_RINGING: return "RINGING";
                case TelephonyManager.CALL_STATE_OFFHOOK: return "OFFHOOK";
                default: return "UNKNOWN";
            }
        }
    }
}
