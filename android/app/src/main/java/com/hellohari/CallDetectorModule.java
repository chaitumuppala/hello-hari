package com.hellohari;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Promise;

public class CallDetectorModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    private BroadcastReceiver callReceiver;

    public CallDetectorModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "CallDetector";
    }

    @ReactMethod
    public void checkPermission(Promise promise) {
        boolean hasPermission = ContextCompat.checkSelfPermission(
            reactContext,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED;
        
        promise.resolve(hasPermission);
    }

    @ReactMethod
    public void startCallDetection(Promise promise) {
        if (callReceiver != null) {
            promise.reject("ALREADY_RUNNING", "Call detection is already running");
            return;
        }

        if (ContextCompat.checkSelfPermission(reactContext, Manifest.permission.READ_PHONE_STATE) 
            != PackageManager.PERMISSION_GRANTED) {
            promise.reject("PERMISSION_DENIED", "Phone state permission not granted");
            return;
        }

        callReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

                WritableMap params = Arguments.createMap();
                params.putString("state", state);
                params.putString("number", number != null ? number : "");
                
                if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                    params.putString("callState", "RINGING");
                } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                    params.putString("callState", "OFFHOOK");
                } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                    params.putString("callState", "IDLE");
                }

                sendEvent("CallStateChanged", params);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        reactContext.registerReceiver(callReceiver, filter);
        
        promise.resolve(true);
    }

    @ReactMethod
    public void stopCallDetection(Promise promise) {
        if (callReceiver != null) {
            reactContext.unregisterReceiver(callReceiver);
            callReceiver = null;
            promise.resolve(true);
        } else {
            promise.reject("NOT_RUNNING", "Call detection is not running");
        }
    }

    private void sendEvent(String eventName, WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }
}
