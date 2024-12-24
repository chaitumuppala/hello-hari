package com.hellohari;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

public class PermissionModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    private static final int REQUEST_CODE = 1;
    private static final String TAG = "PermissionModule";

    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.POST_NOTIFICATIONS
    };

    public PermissionModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "PermissionModule";
    }

    @ReactMethod
    public void checkPermissions(Promise promise) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.reject("ACTIVITY_NULL", "Activity is null");
            return;
        }

        WritableMap result = Arguments.createMap();
        boolean allGranted = true;

        for (String permission : REQUIRED_PERMISSIONS) {
            boolean isGranted = ContextCompat.checkSelfPermission(activity, permission) 
                == PackageManager.PERMISSION_GRANTED;
            result.putBoolean(permission, isGranted);
            if (!isGranted) allGranted = false;
        }

        result.putBoolean("allGranted", allGranted);
        promise.resolve(result);
    }

    @ReactMethod
    public void requestPermissions(Promise promise) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.reject("ACTIVITY_NULL", "Activity is null");
            return;
        }

        // For Android 14 and above, ensure we handle the new permission model
        if (Build.VERSION.SDK_INT >= 34) { // Using SDK_INT 34 for Android 14
            boolean shouldShowSettings = false;
            for (String permission : REQUIRED_PERMISSIONS) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    shouldShowSettings = true;
                    break;
                }
            }

            if (shouldShowSettings) {
                openSettings(promise);
                return;
            }
        }

        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE);
        promise.resolve(true);
    }

    @ReactMethod
    public void openSettings(Promise promise) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.reject("ACTIVITY_NULL", "Activity is null");
            return;
        }

        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error opening settings", e);
            promise.reject("SETTINGS_ERROR", e.getMessage());
        }
    }
}
