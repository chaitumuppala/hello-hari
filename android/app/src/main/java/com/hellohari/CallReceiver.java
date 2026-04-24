package com.hellohari;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                Log.d(TAG, "Incoming call from: " + number);
                // We'll handle the call here once permissions are working
            }
        }
    }
}
