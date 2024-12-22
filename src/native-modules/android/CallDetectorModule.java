package com.hellohari;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class CallDetectorModule extends ReactContextBaseJavaModule {
    private final ReactContext reactContext;
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
    public void startCallDetection() {
        if (callReceiver != null) {
            return;
        }

        callReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

                WritableMap params = Arguments.createMap();
                params.putString("state", state);
                params.putString("number", number);

                sendEvent("CallStateChanged", params);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        reactContext.registerReceiver(callReceiver, filter);
    }

    @ReactMethod
    public void stopCallDetection() {
        if (callReceiver != null) {
            reactContext.unregisterReceiver(callReceiver);
            callReceiver = null;
        }
    }

    private void sendEvent(String eventName, WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        stopCallDetection();
    }
}
