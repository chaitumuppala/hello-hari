package com.hellohari;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

public class PermissionModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    private static final int REQUEST_CODE = 1;

    public PermissionModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "PermissionModule";
    }

    @ReactMethod
    public void requestPermissions(Promise promise) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            promise.reject("ACTIVITY_NULL", "Activity is null");
            return;
        }

        String[] permissions = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_NUMBERS
        };

        boolean shouldShowRationale = false;
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(currentActivity, permission)) {
                shouldShowRationale = true;
                break;
            }
        }

        if (shouldShowRationale) {
            // Open app settings if permission was previously denied
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", currentActivity.getPackageName(), null);
            intent.setData(uri);
            currentActivity.startActivity(intent);
            promise.resolve(false);
        } else {
            ActivityCompat.requestPermissions(currentActivity, permissions, REQUEST_CODE);
            promise.resolve(true);
        }
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
            activity.startActivity(intent);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("OPEN_SETTINGS_ERROR", e.getMessage());
        }
    }
}
