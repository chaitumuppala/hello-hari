package com.hellohari;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for Hello Hari — clean dark UI matching the web recorder.
 *
 * Architecture:
 *   - XML layout (activity_main.xml) with shield indicator, language picker, record button
 *   - AsrManager handles: backend WebSocket (primary) → Google SpeechRecognizer (fallback)
 *   - ScamPatternEngine runs analysis (backend does it for WS; local for Google fallback)
 */
public class MainActivityNew extends AppCompatActivity {

    private static final String TAG = "HelloHari";
    private static final int PERMISSION_REQUEST = 100;
    private static final String PREFS_NAME = "hello_hari_prefs";
    private static final String PREF_SERVER_URL = "server_url";

    // Language data (matches frontend/src/types/index.ts LANGUAGES)
    private static final String[][] LANGUAGES = {
            {"te", "Telugu", "తెలుగు"},
            {"hi", "Hindi", "हिन्दी"},
            {"en", "English", "English"},
            {"ta", "Tamil", "தமிழ்"},
            {"bn", "Bengali", "বাংলা"},
            {"mr", "Marathi", "मराठी"},
            {"gu", "Gujarati", "ગુજરાતી"},
            {"kn", "Kannada", "ಕನ್ನಡ"},
            {"ml", "Malayalam", "മലയാളം"},
            {"pa", "Punjabi", "ਪੰਜਾਬੀ"},
            {"or", "Odia", "ଓଡ଼ିଆ"},
    };

    // UI views
    private View shieldRing;
    private TextView shieldIcon, shieldLabel, shieldScore;
    private TextView statusText, engineBadge;
    private Button recordButton;
    private Spinner languageSpinner;
    private EditText serverUrlInput;
    private View alertCard;
    private TextView alertTitle, alertExplanation;
    private FlexboxLayout pillContainer;
    private TextView transcriptHeader;
    private RecyclerView transcriptList;

    // State
    private AsrManager asrManager;
    private boolean recording = false;
    private final List<TranscriptItem> transcripts = new ArrayList<>();
    private TranscriptAdapter transcriptAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupLanguageSpinner();
        setupTranscriptList();
        loadSavedServerUrl();

        asrManager = new AsrManager(this);
        asrManager.setListener(asrListener);

        recordButton.setOnClickListener(v -> toggleRecording());

        requestPermissions();

        Log.i(TAG, "Hello Hari initialized");
    }

    private void bindViews() {
        shieldRing = findViewById(R.id.shieldRing);
        shieldIcon = findViewById(R.id.shieldIcon);
        shieldLabel = findViewById(R.id.shieldLabel);
        shieldScore = findViewById(R.id.shieldScore);
        statusText = findViewById(R.id.statusText);
        engineBadge = findViewById(R.id.engineBadge);
        recordButton = findViewById(R.id.recordButton);
        languageSpinner = findViewById(R.id.languageSpinner);
        serverUrlInput = findViewById(R.id.serverUrlInput);
        alertCard = findViewById(R.id.alertCard);
        alertTitle = findViewById(R.id.alertTitle);
        alertExplanation = findViewById(R.id.alertExplanation);
        pillContainer = findViewById(R.id.pillContainer);
        transcriptHeader = findViewById(R.id.transcriptHeader);
        transcriptList = findViewById(R.id.transcriptList);
    }

    private void setupLanguageSpinner() {
        String[] labels = new String[LANGUAGES.length];
        for (int i = 0; i < LANGUAGES.length; i++) {
            labels[i] = LANGUAGES[i][2] + " (" + LANGUAGES[i][1] + ")";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(ContextCompat.getColor(MainActivityNew.this, R.color.text_primary));
                tv.setTextSize(14);
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(ContextCompat.getColor(MainActivityNew.this, R.color.text_primary));
                tv.setBackgroundColor(ContextCompat.getColor(MainActivityNew.this, R.color.bg_surface));
                tv.setPadding(32, 24, 32, 24);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        languageSpinner.setSelection(0); // Telugu default
    }

    private void setupTranscriptList() {
        transcriptAdapter = new TranscriptAdapter(transcripts);
        transcriptList.setLayoutManager(new LinearLayoutManager(this));
        transcriptList.setAdapter(transcriptAdapter);
    }

    private void loadSavedServerUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String saved = prefs.getString(PREF_SERVER_URL, "");
        if (!saved.isEmpty()) {
            serverUrlInput.setText(saved);
        }
    }

    private void saveServerUrl(String url) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putString(PREF_SERVER_URL, url).apply();
    }

    // --- Recording toggle ---

    private void toggleRecording() {
        if (recording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        String serverUrl = serverUrlInput.getText().toString().trim();
        saveServerUrl(serverUrl);

        int langIndex = languageSpinner.getSelectedItemPosition();
        String langCode = LANGUAGES[langIndex][0];

        asrManager.setServerUrl(serverUrl);
        asrManager.setLanguage(langCode);
        asrManager.start();

        recording = true;
        recordButton.setText("Stop");
        recordButton.setBackgroundResource(R.drawable.button_stop);
        languageSpinner.setEnabled(false);
        serverUrlInput.setEnabled(false);

        setShieldState("🎙️", "LISTENING", -1);
        statusText.setText("Connecting...");
        shieldRing.setBackgroundResource(R.drawable.shield_ring_safe);

        transcripts.clear();
        transcriptAdapter.notifyDataSetChanged();
        alertCard.setVisibility(View.GONE);
        pillContainer.setVisibility(View.GONE);
        pillContainer.removeAllViews();
    }

    private void stopRecording() {
        asrManager.stop();
        recording = false;
        recordButton.setText("Start Recording");
        recordButton.setBackgroundResource(R.drawable.button_start);
        languageSpinner.setEnabled(true);
        serverUrlInput.setEnabled(true);
    }

    // --- AsrManager listener ---

    private final AsrManager.Listener asrListener = new AsrManager.Listener() {
        @Override
        public void onEngineChanged(AsrManager.Engine engine) {
            String label = engine == AsrManager.Engine.BACKEND
                    ? "⚡ Backend (IndicConformer + Whisper)"
                    : "🔄 Google Speech (Fallback)";
            engineBadge.setText(label);
            engineBadge.setVisibility(View.VISIBLE);
        }

        @Override
        public void onListening() {
            setShieldState("🎙️", "LISTENING", -1);
            statusText.setText("Listening — put the call on speaker");
        }

        @Override
        public void onTranscription(String text, String language) {
            if (text == null || text.trim().isEmpty()) return;

            transcripts.add(0, new TranscriptItem(text, language));
            transcriptAdapter.notifyItemInserted(0);
            transcriptList.scrollToPosition(0);
            transcriptHeader.setVisibility(View.VISIBLE);
            transcriptList.setVisibility(View.VISIBLE);

            int count = transcripts.size();
            statusText.setText("Listening — " + count + " chunk" + (count != 1 ? "s" : "") + " analyzed");
        }

        @Override
        public void onScamResult(boolean isScam, int riskScore, String explanation) {
            if (riskScore <= 0) {
                if (recording) {
                    setShieldState("🛡️", "SAFE", -1);
                    shieldRing.setBackgroundResource(R.drawable.shield_ring_safe);
                }
                alertCard.setVisibility(View.GONE);
                return;
            }

            if (isScam) {
                setShieldState("🚨", "SCAM DETECTED", riskScore);
                shieldRing.setBackgroundResource(R.drawable.shield_ring_danger);
                alertCard.setBackgroundResource(R.drawable.alert_card_bg);
                alertTitle.setText("🚨 SCAM DETECTED");
            } else {
                setShieldState("⚠️", "SUSPICIOUS", riskScore);
                shieldRing.setBackgroundResource(R.drawable.shield_ring_warning);
                alertCard.setBackgroundResource(R.drawable.alert_card_warning_bg);
                alertTitle.setText("⚠️ Suspicious Activity");
            }

            alertExplanation.setText(explanation);
            alertCard.setVisibility(View.VISIBLE);
        }

        @Override
        public void onError(String message) {
            statusText.setText("Error: " + message);
        }

        @Override
        public void onSessionEnd() {
            if (!recording) return;
            recording = false;
            recordButton.setText("Start Recording");
            recordButton.setBackgroundResource(R.drawable.button_start);
            languageSpinner.setEnabled(true);
            serverUrlInput.setEnabled(true);

            int count = transcripts.size();
            statusText.setText("Analysis complete — " + count + " chunk" + (count != 1 ? "s" : ""));
            engineBadge.setVisibility(View.GONE);
        }
    };

    // --- Shield UI ---

    private void setShieldState(String icon, String label, int score) {
        shieldIcon.setText(icon);
        shieldLabel.setText(label);
        if (score >= 0) {
            shieldScore.setText(score + "%");
            shieldScore.setVisibility(View.VISIBLE);
        } else {
            shieldScore.setVisibility(View.GONE);
        }
    }

    // --- Permissions ---

    private void requestPermissions() {
        String[] perms = {
                Manifest.permission.RECORD_AUDIO,
        };

        List<String> needed = new ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    statusText.setText("Microphone permission required");
                    recordButton.setEnabled(false);
                    return;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (asrManager != null) {
            asrManager.destroy();
        }
    }

    // --- Transcript adapter ---

    private static class TranscriptItem {
        final String text;
        final String language;

        TranscriptItem(String text, String language) {
            this.text = text;
            this.language = language;
        }
    }

    private static class TranscriptAdapter extends RecyclerView.Adapter<TranscriptAdapter.VH> {
        private final List<TranscriptItem> items;

        TranscriptAdapter(List<TranscriptItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate a simple transcript row programmatically (avoids extra XML)
            android.widget.LinearLayout row = new android.widget.LinearLayout(parent.getContext());
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setBackgroundResource(R.drawable.transcript_item_bg);
            row.setPadding(dp(parent, 12), dp(parent, 10), dp(parent, 12), dp(parent, 10));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(parent, 6);
            row.setLayoutParams(lp);

            // Language badge
            TextView badge = new TextView(parent.getContext());
            badge.setId(android.R.id.text1);
            badge.setBackgroundResource(R.drawable.lang_badge_bg);
            badge.setPadding(dp(parent, 8), dp(parent, 2), dp(parent, 8), dp(parent, 2));
            badge.setTextSize(10);
            badge.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.text_secondary));
            row.addView(badge);

            // Spacer
            View spacer = new View(parent.getContext());
            spacer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(parent, 10), 0));
            row.addView(spacer);

            // Text
            TextView text = new TextView(parent.getContext());
            text.setId(android.R.id.text2);
            text.setTextSize(13);
            text.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.text_primary));
            text.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            row.addView(text);

            return new VH(row);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TranscriptItem item = items.get(position);
            holder.badge.setText(item.language != null ? item.language.toUpperCase() : "");
            holder.text.setText(item.text);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static int dp(View parent, int dp) {
            return (int) (dp * parent.getResources().getDisplayMetrics().density);
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView badge;
            final TextView text;

            VH(View itemView) {
                super(itemView);
                badge = itemView.findViewById(android.R.id.text1);
                text = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
