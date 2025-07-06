package com.hellohari;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple UI programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // Title
        TextView title = new TextView(this);
        title.setText("Hello Hari - Call Protection");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);
        
        // Subtitle
        TextView subtitle = new TextView(this);
        subtitle.setText("Your Call Safety Companion");
        subtitle.setTextSize(16);
        subtitle.setPadding(0, 0, 0, 50);
        layout.addView(subtitle);
        
        // Request permissions button
        Button permButton = new Button(this);
        permButton.setText("Grant Permissions");
        permButton.setOnClickListener(v -> requestPermissions());
        layout.addView(permButton);
        
        // About button
        Button aboutButton = new Button(this);
        aboutButton.setText("About Hello Hari");
        aboutButton.setOnClickListener(v -> showAbout());
        layout.addView(aboutButton);
        
        setContentView(layout);
        
        // Request permissions on startup
        requestPermissions();
    }

    private void requestPermissions() {
        String[] permissions = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        };
        
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.parse("package:" + getPackageName());
        intent.setData(uri);
        startActivity(intent);
    }
    
    private void showAbout() {
        // Create simple about dialog programmatically
        TextView aboutText = new TextView(this);
        aboutText.setText("Hello Hari (HH) is dedicated to protecting people from phone scams and fraudulent calls.\n\nIn memory of Hari, whose spirit of protecting others lives on through this app.");
        aboutText.setPadding(50, 50, 50, 50);
        aboutText.setTextSize(16);
        
        LinearLayout aboutLayout = new LinearLayout(this);
        aboutLayout.addView(aboutText);
        
        setContentView(aboutLayout);
    }
}
