package com.hellohari;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MultiLanguageScamDetector {
    private static final String TAG = "MultiLangScamDetector";
    private Context context;
    
    // Enhanced scam patterns for 3 languages
    private static final Map<String, Integer> ENGLISH_PATTERNS = new HashMap<String, Integer>() {{
        put("account suspended", 45);
        put("verify immediately", 40);
        put("legal action", 50);
        put("arrest warrant", 55);
        put("tax refund", 35);
        put("you have won", 40);
        put("urgent action required", 30);
        put("confirm your details", 25);
        put("bank account blocked", 45);
        put("police station", 50);
        put("fraud detected", 40);
        put("suspend your account", 45);
        put("verify your identity", 35);
        put("immediate verification", 40);
        put("security breach", 35);
        put("unauthorized transaction", 30);
        put("click this link", 25);
        put("call back immediately", 30);
        put("limited time offer", 20);
        put("congratulations you won", 40);
    }};
    
    private static final Map<String, Integer> HINDI_PATTERNS = new HashMap<String, Integer>() {{
        // Devanagari script patterns
        put("खाता बंद", 45);              // account closed
        put("तुरंत verify", 40);          // immediately verify
        put("पुलिस आएगी", 50);           // police will come
        put("गिरफ्तार", 55);              // arrest
        put("कानूनी कार्रवाई", 50);        // legal action
        put("बैंक से फोन", 30);           // call from bank
        put("खाता suspend", 45);          // account suspend
        put("जल्दी करें", 35);            // do quickly
        put("पैसा वापस", 30);             // money back
        put("लॉटरी जीती", 40);            // won lottery
        put("फ्रॉड detect", 35);          // fraud detected
        put("सिक्योरिटी ब्रीच", 35);       // security breach
        
        // Romanized Hindi patterns (how people actually speak)
        put("aapka account", 35);
        put("police station jaana", 50);
        put("bank se call", 30);
        put("verify karna hoga", 40);
        put("turant action", 35);
        put("paisa return", 30);
        put("fraud mila", 40);
        put("account band", 45);
        put("legal notice", 45);
        put("arrest hoga", 55);
        put("police complaint", 50);
        put("cyber crime", 40);
        put("rbi se call", 35);
        put("income tax", 30);
        put("refund milega", 30);
    }};
    
    private static final Map<String, Integer> TELUGU_PATTERNS = new HashMap<String, Integer>() {{
        // Telugu script patterns
        put("ఖాతా మూసివేయబడింది", 45);      // account closed
        put("వెంటనే verify", 40);          // immediately verify
        put("పోలీసులు వస్తారు", 50);         // police will come
        put("అరెస్ట్ చేస్తారు", 55);         // will arrest
        put("చట్టపరమైన చర్య", 50);          // legal action
        put("బ్యాంక్ నుండి కాల్", 30);       // call from bank
        put("ఖాతా suspend", 45);           // account suspend
        put("త్వరగా చేయండి", 35);          // do quickly
        put("డబ్బు తిరిగి", 30);            // money back
        put("లాటరీ గెలిచారు", 40);          // won lottery
        
        // Romanized Telugu (mixed usage)
        put("mee account", 35);
        put("police station vellaali", 50);
        put("bank nundi call", 30);
        put("verify cheyyaali", 40);
        put("arrest avuthaaru", 55);
        put("legal case", 45);
        put("fraud detect", 40);
        put("money return", 30);
        put("cyber crime case", 45);
        put("rbi notice", 35);
        put("income tax raid", 40);
        put("refund vasthundi", 30);
    }};
    
    // Mixed language patterns (very common in Indian scam calls)
    private static final Map<String, Integer> MIXED_PATTERNS = new HashMap<String, Integer>() {{
        put("aapka bank account", 40);
        put("police station mein", 50);
        put("legal action lenge", 45);
        put("arrest kar denge", 55);
        put("verify karo jaldi", 40);
        put("fraud ho gaya", 40);
        put("account band kar", 45);
        put("money transfer kar", 30);
        put("otp share karo", 45);
        put("link pe click", 35);
        put("refund mil jayega", 30);
        put("lottery jeet gaye", 40);
        put("cyber crime mein", 45);
        put("rbi ka notice", 35);
        put("income tax ka", 30);
    }};
    
    // Urgency indicators across languages
    private static final Set<String> URGENCY_WORDS = new HashSet<String>() {{
        add("immediately"); add("urgent"); add("jaldi"); add("turant"); 
        add("త్వరగా"); add("వెంటనే"); add("now"); add("abhi");
        add("तुरंत"); add("जल्दी"); add("अभी");
    }};
    
    // Authority/threat words
    private static final Set<String> AUTHORITY_WORDS = new HashSet<String>() {{
        add("police"); add("arrest"); add("legal"); add("court"); add("रिपोर्ट");
        add("पुलिस"); add("गिरफ्तार"); add("कानूनी"); add("कोर्ट");
        add("పోలీసు"); add("అరెస్ట్"); add("కోర్ట్"); add("చట్టం");
        add("rbi"); add("income tax"); add("cyber crime");
    }};

    public MultiLanguageScamDetector(Context context) {
        this.context = context;
    }

    public ScamAnalysisResult analyzeRecording(String audioFilePath) {
        try {
            Log.d(TAG, "Starting multi-language scam analysis for: " + audioFilePath);
            
            // Step 1: Multi-language transcription
            MultiLanguageTranscription transcription = performMultiLanguageTranscription(audioFilePath);
            
            // Step 2: Cross-language pattern analysis
            ScamAnalysisResult result = analyzeTranscriptionForScams(transcription);
            
            Log.d(TAG, "Analysis complete. Risk score: " + result.getRiskScore());
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Multi-language analysis failed: " + e.getMessage(), e);
            return createFallbackResult("Analysis failed: " + e.getMessage());
        }
    }
    
    private MultiLanguageTranscription performMultiLanguageTranscription(String audioFilePath) {
        List<TranscriptionResult> results = new ArrayList<>();
        
        // Try transcription in all 3 languages
        String[] languages = {"en-IN", "hi-IN", "te-IN"};
        String[] languageNames = {"English", "Hindi", "Telugu"};
        
        for (int i = 0; i < languages.length; i++) {
            try {
                Log.d(TAG, "Attempting transcription in: " + languageNames[i]);
                String transcript = transcribeAudioWithLanguage(audioFilePath, languages[i]);
                
                if (transcript != null && transcript.trim().length() > 5) {
                    TranscriptionResult result = new TranscriptionResult(
                        transcript.trim(),
                        languageNames[i],
                        languages[i],
                        calculateTranscriptionQuality(transcript)
                    );
                    results.add(result);
                    Log.d(TAG, languageNames[i] + " transcription: " + transcript.substring(0, Math.min(50, transcript.length())) + "...");
                }
            } catch (Exception e) {
                Log.w(TAG, "Transcription failed for " + languageNames[i] + ": " + e.getMessage());
            }
        }
        
        return new MultiLanguageTranscription(results);
    }
    
    private String transcribeAudioWithLanguage(String audioFilePath, String languageCode) {
        try {
            // Create speech recognizer for specific language
            SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            
            final StringBuilder transcriptionResult = new StringBuilder();
            final CountDownLatch latch = new CountDownLatch(1);
            final Exception[] transcriptionError = {null};
            
            // Set up recognition listener
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Speech recognizer ready for " + languageCode);
                }
                
                @Override
                public void onBeginningOfSpeech() {}
                
                @Override
                public void onRmsChanged(float rmsdB) {}
                
                @Override
                public void onBufferReceived(byte[] buffer) {}
                
                @Override
                public void onEndOfSpeech() {}
                
                @Override
                public void onError(int error) {
                    transcriptionError[0] = new Exception("Speech recognition error: " + error);
                    latch.countDown();
                }
                
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        transcriptionResult.append(matches.get(0));
                    }
                    latch.countDown();
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {}
                
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
            
            // Prepare recognition intent
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            
            // For audio file transcription, we'll use a simplified approach
            // In production, you might want to use Google Cloud Speech API or similar
            // For now, we'll simulate transcription based on language patterns
            String simulatedTranscription = simulateTranscriptionForTesting(audioFilePath, languageCode);
            
            speechRecognizer.destroy();
            return simulatedTranscription;
            
        } catch (Exception e) {
            Log.e(TAG, "Transcription failed for " + languageCode, e);
            return null;
        }
    }
    
    // Temporary simulation for testing - replace with actual STT
    private String simulateTranscriptionForTesting(String audioFilePath, String languageCode) {
        // This is a placeholder that simulates different language transcriptions
        // In actual implementation, this would be replaced with real STT
        
        String[] sampleScamTexts = {
            "Your account will be suspended please verify immediately",
            "आपका खाता बंद हो जाएगा तुरंत verify करें",
            "మీ ఖాతా మూసివేయబడుతుంది వెంటనే verify చేయండి"
        };
        
        String[] sampleLegitTexts = {
            "Hello this is John calling about your insurance renewal",
            "हैलो मैं आपके बारे में बात करना चाहता हूं",
            "హలో నేను మీ భీమా గురించి మాట్లాడాలనుకుంటున్నాను"
        };
        
        // Simulate based on language code
        Random random = new Random();
        boolean isScam = random.nextBoolean(); // 50% chance of scam for testing
        
        switch (languageCode) {
            case "en-IN":
                return isScam ? sampleScamTexts[0] : sampleLegitTexts[0];
            case "hi-IN":
                return isScam ? sampleScamTexts[1] : sampleLegitTexts[1];
            case "te-IN":
                return isScam ? sampleScamTexts[2] : sampleLegitTexts[2];
            default:
                return sampleLegitTexts[0];
        }
    }
    
    private float calculateTranscriptionQuality(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) return 0.0f;
        
        // Quality heuristics
        float quality = 0.5f; // Base quality
        
        // More words = better quality (usually)
        int wordCount = transcript.split("\\s+").length;
        quality += Math.min(0.3f, wordCount * 0.02f);
        
        // Presence of common words increases confidence
        String lower = transcript.toLowerCase();
        if (lower.contains("the") || lower.contains("and") || lower.contains("है") || lower.contains("का")) {
            quality += 0.1f;
        }
        
        // Reasonable length indicates good transcription
        if (transcript.length() > 20 && transcript.length() < 500) {
            quality += 0.1f;
        }
        
        return Math.min(1.0f, quality);
    }
    
    private ScamAnalysisResult analyzeTranscriptionForScams(MultiLanguageTranscription transcription) {
        int totalRiskScore = 0;
        List<String> detectedPatterns = new ArrayList<>();
        List<String> detectedLanguages = new ArrayList<>();
        String primaryLanguage = "Unknown";
        float maxConfidence = 0.0f;
        
        // Analyze each transcription result
        for (TranscriptionResult result : transcription.getResults()) {
            if (result.getQuality() > maxConfidence) {
                maxConfidence = result.getQuality();
                primaryLanguage = result.getLanguageName();
            }
            
            detectedLanguages.add(result.getLanguageName());
            String transcript = result.getTranscript().toLowerCase();
            
            // Check language-specific patterns
            int languageScore = 0;
            
            if ("English".equals(result.getLanguageName())) {
                languageScore += checkPatterns(transcript, ENGLISH_PATTERNS, detectedPatterns, "EN");
            } else if ("Hindi".equals(result.getLanguageName())) {
                languageScore += checkPatterns(transcript, HINDI_PATTERNS, detectedPatterns, "HI");
            } else if ("Telugu".equals(result.getLanguageName())) {
                languageScore += checkPatterns(transcript, TELUGU_PATTERNS, detectedPatterns, "TE");
            }
            
            // Check mixed language patterns for all transcriptions
            languageScore += checkPatterns(transcript, MIXED_PATTERNS, detectedPatterns, "MX");
            
            // Check urgency and authority indicators
            languageScore += checkUrgencyIndicators(transcript, detectedPatterns);
            languageScore += checkAuthorityIndicators(transcript, detectedPatterns);
            
            // Weight by transcription quality
            totalRiskScore += (int)(languageScore * result.getQuality());
        }
        
        // Boost score if multiple languages detected (often indicates scam)
        if (detectedLanguages.size() > 1) {
            totalRiskScore += 15;
            detectedPatterns.add("MULTI-LANG: Multiple languages detected");
        }
        
        // Cap the score at 100
        totalRiskScore = Math.min(100, totalRiskScore);
        
        return new ScamAnalysisResult(
            totalRiskScore,
            detectedPatterns,
            generateAnalysisMessage(totalRiskScore, detectedPatterns, primaryLanguage),
            primaryLanguage,
            detectedLanguages
        );
    }
    
    private int checkPatterns(String transcript, Map<String, Integer> patterns, List<String> detected, String langCode) {
        int score = 0;
        for (Map.Entry<String, Integer> pattern : patterns.entrySet()) {
            if (transcript.contains(pattern.getKey().toLowerCase())) {
                score += pattern.getValue();
                detected.add("[" + langCode + "] " + pattern.getKey() + " (+" + pattern.getValue() + ")");
            }
        }
        return score;
    }
    
    private int checkUrgencyIndicators(String transcript, List<String> detected) {
        int score = 0;
        for (String urgencyWord : URGENCY_WORDS) {
            if (transcript.toLowerCase().contains(urgencyWord.toLowerCase())) {
                score += 10;
                detected.add("URGENCY: " + urgencyWord + " (+10)");
                break; // Only count once per category
            }
        }
        return score;
    }
    
    private int checkAuthorityIndicators(String transcript, List<String> detected) {
        int score = 0;
        for (String authorityWord : AUTHORITY_WORDS) {
            if (transcript.toLowerCase().contains(authorityWord.toLowerCase())) {
                score += 15;
                detected.add("AUTHORITY: " + authorityWord + " (+15)");
                break; // Only count once per category
            }
        }
        return score;
    }
    
    private String generateAnalysisMessage(int riskScore, List<String> patterns, String primaryLanguage) {
        StringBuilder message = new StringBuilder();
        
        if (riskScore > 70) {
            message.append("🚨 HIGH RISK SCAM DETECTED\n");
        } else if (riskScore > 40) {
            message.append("⚠️ MODERATE RISK - SUSPICIOUS PATTERNS\n");
        } else if (riskScore > 20) {
            message.append("⚡ LOW-MODERATE RISK\n");
        } else {
            message.append("✅ LOW RISK - APPEARS LEGITIMATE\n");
        }
        
        message.append("Primary Language: ").append(primaryLanguage).append("\n");
        message.append("Risk Score: ").append(riskScore).append("%\n");
        
        if (!patterns.isEmpty()) {
            message.append("Detected Patterns:\n");
            for (String pattern : patterns) {
                message.append("• ").append(pattern).append("\n");
            }
        }
        
        return message.toString();
    }
    
    private ScamAnalysisResult createFallbackResult(String errorMessage) {
        return new ScamAnalysisResult(
            25, // Default moderate risk when analysis fails
            Arrays.asList("ANALYSIS_FAILED: " + errorMessage),
            "⚠️ Analysis failed - using fallback risk assessment",
            "Unknown",
            Arrays.asList("Unknown")
        );
    }
    
    // Data classes for results
    public static class ScamAnalysisResult {
        private final int riskScore;
        private final List<String> detectedPatterns;
        private final String analysisMessage;
        private final String primaryLanguage;
        private final List<String> detectedLanguages;
        
        public ScamAnalysisResult(int riskScore, List<String> detectedPatterns, String analysisMessage, 
                                String primaryLanguage, List<String> detectedLanguages) {
            this.riskScore = riskScore;
            this.detectedPatterns = detectedPatterns;
            this.analysisMessage = analysisMessage;
            this.primaryLanguage = primaryLanguage;
            this.detectedLanguages = detectedLanguages;
        }
        
        public int getRiskScore() { return riskScore; }
        public List<String> getDetectedPatterns() { return detectedPatterns; }
        public String getAnalysisMessage() { return analysisMessage; }
        public String getPrimaryLanguage() { return primaryLanguage; }
        public List<String> getDetectedLanguages() { return detectedLanguages; }
    }
    
    private static class TranscriptionResult {
        private final String transcript;
        private final String languageName;
        private final String languageCode;
        private final float quality;
        
        public TranscriptionResult(String transcript, String languageName, String languageCode, float quality) {
            this.transcript = transcript;
            this.languageName = languageName;
            this.languageCode = languageCode;
            this.quality = quality;
        }
        
        public String getTranscript() { return transcript; }
        public String getLanguageName() { return languageName; }
        public String getLanguageCode() { return languageCode; }
        public float getQuality() { return quality; }
    }
    
    private static class MultiLanguageTranscription {
        private final List<TranscriptionResult> results;
        
        public MultiLanguageTranscription(List<TranscriptionResult> results) {
            this.results = results;
        }
        
        public List<TranscriptionResult> getResults() { return results; }
    }
}
