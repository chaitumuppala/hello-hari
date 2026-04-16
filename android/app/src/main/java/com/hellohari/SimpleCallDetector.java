package com.hellohari;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class SimpleCallDetector {
    private static final String TAG = "SimpleCallDetector";
    private Context context;
    private BroadcastReceiver callReceiver;
    private boolean isMonitoring = false;
    
    // Define constants for switch-case compatibility
    private static final String STATE_RINGING = TelephonyManager.EXTRA_STATE_RINGING;
    private static final String STATE_OFFHOOK = TelephonyManager.EXTRA_STATE_OFFHOOK;
    private static final String STATE_IDLE = TelephonyManager.EXTRA_STATE_IDLE;
    
    public interface CallDetectionListener {
        void onCallStateChanged(String state, String phoneNumber);
    }
    
    private CallDetectionListener listener;

    public SimpleCallDetector(Context context) {
        this.context = context;
    }
    
    public void setCallDetectionListener(CallDetectionListener listener) {
        this.listener = listener;
    }

    public boolean startCallDetection() {
        if (isMonitoring) {
            Log.d(TAG, "Call detection already running");
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
                        
                        // Show toast for testing
                        showToast("Call State: " + state + (phoneNumber != null ? " from " + phoneNumber : ""));
                        
                        // Handle different call states
                        handleCallStateChange(state, phoneNumber);
                        
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
            
            Log.d(TAG, "Call detection started successfully");
            showToast("Hello Hari: Call monitoring started");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start call detection", e);
            showToast("Failed to start call monitoring: " + e.getMessage());
            return false;
        }
    }

    public boolean stopCallDetection() {
        if (!isMonitoring || callReceiver == null) {
            Log.d(TAG, "Call detection not running");
            return false;
        }

        try {
            context.unregisterReceiver(callReceiver);
            callReceiver = null;
            isMonitoring = false;
            
            Log.d(TAG, "Call detection stopped");
            showToast("Hello Hari: Call monitoring stopped");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop call detection", e);
            return false;
        }
    }
    
    public boolean isMonitoring() {
        return isMonitoring;
    }

    private void handleCallStateChange(String state, String phoneNumber) {
        // Use if-else instead of switch for string comparison
        if (STATE_RINGING.equals(state)) {
            Log.i(TAG, "📞 INCOMING CALL detected from: " + (phoneNumber != null ? phoneNumber : "Unknown"));
            onIncomingCall(phoneNumber);
        } else if (STATE_OFFHOOK.equals(state)) {
            Log.i(TAG, "📱 CALL ANSWERED or OUTGOING CALL started");
            onCallAnswered(phoneNumber);
        } else if (STATE_IDLE.equals(state)) {
            Log.i(TAG, "📴 CALL ENDED or PHONE IDLE");
            onCallEnded(phoneNumber);
        } else {
            Log.d(TAG, "Unknown call state: " + state);
        }
    }

    private void onIncomingCall(String phoneNumber) {
        // This is where we'll add scam detection logic later
        String displayNumber = phoneNumber != null ? phoneNumber : "Unknown Number";
        showToast("🚨 Incoming call from " + displayNumber + " - Analyzing...");
        
        // TODO: Add scam pattern detection
        // TODO: Start audio recording preparation
    }

    private void onCallAnswered(String phoneNumber) {
        String displayNumber = phoneNumber != null ? phoneNumber : "Unknown Number";
        showToast("📞 Call with " + displayNumber + " - Recording for safety");
        
        // TODO: Start actual call recording
    }

    private void onCallEnded(String phoneNumber) {
        showToast("📴 Call ended - Analysis complete");
        
        // TODO: Stop recording and analyze for scam patterns
        // TODO: Generate risk report
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    // Legacy methods for MainActivity compatibility
    public boolean startMonitoring() {
        return startCallDetection();
    }
    
    public boolean stopMonitoring() {
        return stopCallDetection();
    }
}
