package com.hellohari;

import android.media.MediaRecorder;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import java.io.IOException;
import java.io.File;

public class AudioRecorderModule extends ReactContextBaseJavaModule {
    private MediaRecorder mediaRecorder;
    private String currentFilePath;
    private final ReactApplicationContext reactContext;
    private boolean isRecording = false;

    public AudioRecorderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "AudioRecorder";
    }

    @ReactMethod
    public void start(Promise promise) {
        if (isRecording) {
            promise.reject("RECORDING_IN_PROGRESS", "Already recording");
            return;
        }

        try {
            currentFilePath = getReactApplicationContext().getFilesDir() + "/audio_record.wav";
            File audioFile = new File(currentFilePath);
            if (audioFile.exists()) {
                audioFile.delete();
            }

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncodingBitRate(192000);
            mediaRecorder.setOutputFile(currentFilePath);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                
                WritableMap params = Arguments.createMap();
                params.putBoolean("recording", true);
                params.putString("path", currentFilePath);
                sendEvent("recordingStatus", params);
                
                promise.resolve("Recording started");
            } catch (IOException e) {
                promise.reject("RECORDING_FAILED", e.getMessage());
            }
        } catch (Exception e) {
            promise.reject("RECORDING_FAILED", e.getMessage());
        }
    }

    @ReactMethod
    public void stop(Promise promise) {
        if (!isRecording) {
            promise.reject("NOT_RECORDING", "Not currently recording");
            return;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            WritableMap params = Arguments.createMap();
            params.putBoolean("recording", false);
            params.putString("path", currentFilePath);
            sendEvent("recordingStatus", params);

            promise.resolve(currentFilePath);
        } catch (Exception e) {
            promise.reject("STOP_RECORDING_FAILED", e.getMessage());
        }
    }

    @ReactMethod
    public void isRecording(Promise promise) {
        promise.resolve(isRecording);
    }

    private void sendEvent(String eventName, WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
        }
    }
}
