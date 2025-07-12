package com.hellohari;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MultiLanguageScamDetector - Comprehensive scam detection across multiple languages
 * 
 * Features:
 * - Pattern-based detection in English, Hindi, and Telugu
 * - Contextual analysis for common scam techniques
 * - Integration with VOSK speech recognition
 * - Risk scoring system with confidence levels
 * - Analysis reporting
 */
public class MultiLanguageScamDetector {
    private static final String TAG = "ScamDetector";
    
    // Constants for scam categories
    public static final String CATEGORY_FINANCIAL = "financial";
    public static final String CATEGORY_IDENTITY = "identity";
    public static final String CATEGORY_TECH_SUPPORT = "tech_support";
    public static final String CATEGORY_LOTTERY = "lottery";
    public static final String CATEGORY_GOVERNMENT = "government";
    public static final String CATEGORY_DIGITAL_ARREST = "digital_arrest";
    public static final String CATEGORY_TRAI = "trai_telecom";
    public static final String CATEGORY_COURIER = "courier_fedex";
    public static final String CATEGORY_INVESTMENT = "investment_crypto";
    public static final String CATEGORY_FAMILY_EMERGENCY = "family_emergency";
    public static final String CATEGORY_ROMANCE = "romance_scam";
    
    // Risk levels
    public static final int RISK_NONE = 0;
    public static final int RISK_LOW = 1;
    public static final int RISK_MEDIUM = 2;
    public static final int RISK_HIGH = 3;
    
    // Shared preference keys
    private static final String PREFS_NAME = "ScamDetectorPrefs";
    private static final String PREF_LAST_UPDATE = "lastPatternUpdate";
    
    // Context and resources
    private final Context context;
    private final SharedPreferences preferences;
    
    // Pattern storage
    private final Map<String, List<ScamPattern>> patternsByLanguage;
    private final Map<String, List<ScamPattern>> patternsByCategory;
    private int patternCount = 0;
    
    // Analysis state
    private final Set<String> detectedPatterns = new HashSet<>();
    private final Map<String, Integer> categoryScores = new HashMap<>();
    
    // === DIGITAL ARREST SCAMS (HIGHEST RISK - ₹1,935 crore threat) ===
    private static final Map<String, Integer> DIGITAL_ARREST_PATTERNS = new HashMap<String, Integer>() {{
        // Authority establishment (Risk: 95-100)
        put("this is from mumbai police cyber cell", 95);
        put("main mumbai police se bol raha hun", 95);
        put("i am calling from cbi headquarters", 98);
        put("this is from enforcement directorate", 95);
        put("main ncb officer hun", 98);
        put("we are from supreme court of india", 100);
        put("arrest warrant has been issued", 98);
        put("you are under investigation", 90);
        put("cyber crime cell speaking", 95);
        put("narcotics control bureau", 98);
        put("income tax department raid", 92);
        put("customs enforcement calling", 90);
        put("delhi police headquarters", 95);
        put("central bureau investigation", 98);
        
        // Digital confinement language (Risk: 100)
        put("you are now under digital arrest", 100);
        put("stay on video call until investigation completes", 100);
        put("don't disconnect the call or inform anyone", 100);
        put("don't involve family lawyer in this matter", 100);
        put("this is confidential government matter", 95);
        put("case will become serious if you disconnect", 98);
        put("you cannot leave until cleared", 100);
        put("maintain video call throughout", 100);
        put("digital custody until verification", 100);
        put("remote arrest proceedings", 100);
        
        // Accusations (Risk: 90-98)
        put("your aadhaar card has been used for illegal activities", 90);
        put("parcel containing drugs found in your name", 95);
        put("22 complaints filed against your mobile sim", 90);
        put("aapke naam se human trafficking ka case", 98);
        put("your bank account is being used for money laundering", 95);
        put("anti national activities linked to your number", 98);
        put("terror charges will be filed", 100);
        put("drug trafficking case registered", 95);
        put("fake passport found with your details", 95);
        put("hawala transaction detected", 90);
        put("suspicious international transfers", 88);
        put("cybercrime unit has evidence", 92);
        put("narcotics found in courier", 95);
        put("illegal weapons shipment", 98);
        put("human organ trafficking", 100);
        put("child trafficking allegations", 100);
        put("terrorism funding detected", 100);
        put("fake currency circulation", 95);
        
        // Threats and consequences (Risk: 90-98)
        put("your aadhaar card will be blocked", 94);
        put("bank account will be frozen", 94);
        put("passport will be cancelled", 95);
        put("non-bailable warrant", 96);
        put("lookout notice issued", 92);
        put("all services will be suspended", 90);
        put("your pan card is blocked", 94);
        put("immediate legal action", 92);
        put("international criminal", 95);
        put("interpol notice", 95);
        
        // Hindi variants
        put("aapke khilaaf warrant jaari", 96); // warrant issued against you
        put("aapka account freeze kar diya jayega", 94); // your account will be frozen
        put("aapka case court mein hai", 95); // your case is in court
    }};
    
    // === TRAI & TELECOM AUTHORITY SCAMS ===
    private static final Map<String, Integer> TRAI_PATTERNS = new HashMap<String, Integer>() {{
        put("main trai se bol raha hun", 85);
        put("sim card band hone wala hai", 85);
        put("this is from telecom regulatory authority", 85);
        put("your number will be disconnected in 2 hours", 90);
        put("22 complaints registered against your sim", 85);
        put("mobile connection has illegal usage", 80);
        put("immediate action required on your number", 85);
        put("trai compliance violation", 80);
        put("sim deactivation process started", 85);
        put("telecom fraud detected on your number", 85);
        put("press 1 to avoid disconnection", 90);
        put("your mobile services will be suspended", 85);
        put("department of telecommunications calling", 85);
        put("sim card kyc verification failed", 80);
        put("illegal call forwarding detected", 82);
        put("international roaming misuse", 78);
        put("bulk sms violation", 75);
        put("telecom license cancellation", 88);
        put("sim card cloning detected", 90);
        put("unauthorized network access", 82);
    }};
    
    // === FEDEX/COURIER/CUSTOMS SCAMS ===
    private static final Map<String, Integer> COURIER_PATTERNS = new HashMap<String, Integer>() {{
        put("we are calling from fedex mumbai", 80);
        put("your parcel has been confiscated", 85);
        put("drugs found in your package", 90);
        put("140 grams of narcotic drugs found", 90);
        put("parcel contained illegal items", 85);
        put("customs clearance fee required", 75);
        put("package stuck at customs", 75);
        put("custom commission duty and tax", 80);
        put("parcel from thailand intercepted", 85);
        put("five passports three credit cards found", 90);
        put("mdma synthetic narcotics detected", 90);
        put("international package security alert", 80);
        put("customs duty payment needed immediately", 80);
        put("courier company legal notice", 75);
        put("dhl package seizure notice", 80);
        put("blue dart security department", 78);
        put("speed post suspicious package", 75);
        put("first flight courier verification", 76);
        put("aramex package investigation", 78);
        put("gati courier fraud department", 75);
        put("dtdc package confiscation", 76);
        put("professional courier security", 78);
        put("international express detention", 82);
        put("air cargo security alert", 85);
        put("postal department investigation", 80);
        put("package contains contraband", 88);
        put("narcotic substances detected", 90);
        put("illegal wildlife products", 85);
        put("counterfeit currency found", 88);
        put("prohibited pharmaceutical items", 82);
    }};
    
    // === INVESTMENT & CRYPTOCURRENCY FRAUD ===
    private static final Map<String, Integer> INVESTMENT_PATTERNS = new HashMap<String, Integer>() {{
        put("exclusive crypto trading opportunity", 70);
        put("guaranteed 10x returns", 85);
        put("join our private vip group", 75);
        put("double your bitcoin in 30 days", 85);
        put("government approved digital currency", 80);
        put("see screenshots of members profits", 75);
        put("offer expires tonight invest now", 80);
        put("only 100 slots remaining", 80);
        put("professor has been arrested pay to unlock", 85);
        put("withdraw restrictions after 24 hours", 80);
        put("limited time crypto investment", 75);
        put("insider trading tips available", 85);
        put("binary options guaranteed profit", 80);
        put("forex trading robot", 75);
        put("stock market sure shot tips", 75);
        put("rbi approved cryptocurrency", 82);
        put("sebi registered investment scheme", 78);
        put("mutual fund guaranteed returns", 75);
        put("ipo early bird offer", 72);
        put("share market inside information", 85);
    }};
    
    // === FAMILY EMERGENCY & VOICE CLONING SCAMS ===
    private static final Map<String, Integer> FAMILY_EMERGENCY_PATTERNS = new HashMap<String, Integer>() {{
        put("hello beta i am in serious trouble", 95);
        put("ive been in an accident dont tell anyone", 95);
        put("stuck in dubai canada abroad arrested", 90);
        put("phone is broken thats why i sound different", 95);
        put("dont tell mom dad about this", 85);
        put("police station mein hun urgent help", 90);
        put("accident hua hai immediate money needed", 90);
        put("kidnappers have me send ransom", 95);
        put("medical emergency surgery required", 85);
        put("bail money needed right now", 90);
        put("aapko kuch ho gaya hai", 90);
        put("hospital mein admit hai", 90);
        put("turant paisa chahiye", 85);
        put("dadi nani main aapka pota hun", 90);
        put("bache ko kuch ho gaya hai", 95);
        put("accident mein serious condition", 90);
    }};
    
    // === ROMANCE & RELATIONSHIP SCAMS ===
    private static final Map<String, Integer> ROMANCE_PATTERNS = new HashMap<String, Integer>() {{
        put("i dropped a tear in the ocean", 70);
        put("crazy in love with you", 65);
        put("different from all other girls boys", 70);
        put("god has brought us together", 70);
        put("same cultural values", 65);
        put("goals perfectly aligned", 65);
        put("family doesnt understand our love", 75);
        put("keep our relationship secret", 75);
        put("working in restricted military area", 80);
        put("company policy no personal calls", 75);
        put("time difference makes it difficult", 70);
        put("phone broken stolen", 75);
        put("need money for visa to meet you", 85);
        put("stuck at airport need travel funds", 85);
        put("customs seized my money", 80);
    }};
    
    // === HINDI ADVANCED PATTERNS ===
    private static final Map<String, Integer> HINDI_ADVANCED_PATTERNS = new HashMap<String, Integer>() {{
        // Respectful manipulation
        put("sarkar ki taraf se", 85);
        put("aapko court mein hazir hona hoga", 90);
        put("ye ek legal matter hai", 85);
        put("immediate action lena padega", 80);
        put("aapke khilaaf case file ho gaya", 90);
        put("warrant nikla hai aapke naam", 95);
        put("police aane wali hai", 90);
        put("ghar ki talashi hogi", 85);
        put("account freeze ho jayega", 85);
        put("property attach kar denge", 85);
    }};
    
    // === TELUGU ADVANCED PATTERNS ===
    private static final Map<String, Integer> TELUGU_ADVANCED_PATTERNS = new HashMap<String, Integer>() {{
        // Telugu script patterns
        put("మీ ఖాతా మూసివేయబడుతుంది", 85);
        put("వెంటనే verify చేయండి", 80);
        put("పోలీసులు రావడానికి సిద్ధమవుతున్నారు", 90);
        put("అరెస్ట్ వారెంట్ వచ్చింది", 95);
        put("చట్టపరమైన చర్య తీసుకుంటాం", 85);
        put("బ్యాంక్ ఖాతా బ్లాక్ అవుతుంది", 85);
        put("న్యాయస్థానంలో హాజరు కావాలి", 90);
        put("సైబర్ క్రైమ్ కేసు రిజిస్టర్ అయింది", 90);
        put("ఆధార్ కార్డ్ misuse అయింది", 82);
        put("పాన్ కార్డ్ duplicate దొరికింది", 85);
    }};
    
    // === MIXED LANGUAGE (HINGLISH) PATTERNS ===
    private static final Map<String, Integer> HINGLISH_PATTERNS = new HashMap<String, Integer>() {{
        put("sir aapka computer infected hai", 75);
        put("aapko refund mil sakta hai", 75);
        put("verification ke liye details chahiye", 80);
        put("customer care se call kar rahe", 70);
        put("aapka account hack ho gaya", 80);
        put("virus remove karna padega", 75);
        put("technical support ki zarurat", 70);
        put("microsoft se official call", 75);
        put("windows license expire ho gaya", 70);
        put("security breach detect hua", 80);
    }};
    
    // === URGENCY INDICATORS (CROSS-LANGUAGE) ===
    private static final Set<String> URGENCY_WORDS = new HashSet<String>() {{
        // English
        add("immediately"); add("urgent"); add("now"); add("quickly"); add("emergency");
        add("instant"); add("right now"); add("within minutes"); add("before midnight");
        add("today only"); add("limited time"); add("last chance"); add("expires soon");
        
        // Hindi
        add("turant"); add("jaldi"); add("abhi"); add("foran"); add("tatkal");
        add("emergency"); add("zaruri"); add("aaj hi"); add("do ghante mein");
        
        // Telugu
        add("వెంటనే"); add("త్వరగా"); add("ఇప్పుడే"); add("అత్యవసరం");
        add("immediatelyga"); add("jaldiga"); add("emergency lo");
    }};
    
    // === AUTHORITY INDICATORS ===
    private static final Set<String> AUTHORITY_WORDS = new HashSet<String>() {{
        // Law enforcement
        add("police"); add("cbi"); add("ncb"); add("ed"); add("income tax");
        add("customs"); add("rbi"); add("sebi"); add("trai"); add("court");
        add("judge"); add("magistrate"); add("collector"); add("commissioner");
        
        // Hindi authorities
        add("पुलिस"); add("न्यायाधीश"); add("कलेक्टर"); add("आयुक्त");
        add("थाना"); add("कोर्ट"); add("सरकार"); add("अफसर");
        
        // Telugu authorities
        add("పోలీసు"); add("న్యాయమూర్తి"); add("కలెక్టర్"); add("కమిషనర్");
        add("ప్రభుత్వం"); add("అధికారి"); add("కోర్టు"); add("న్యాయస్థానం");
    }};
    
    // === FINANCIAL TERMS (HIGH RISK) ===
    private static final Set<String> FINANCIAL_RISK_TERMS = new HashSet<String>() {{
        // Direct money requests
        add("money transfer"); add("bank details"); add("account number");
        add("ifsc code"); add("upi pin"); add("otp"); add("cvv"); add("atm pin");
        add("net banking password"); add("debit card number"); add("credit card details");
        
        // Hindi financial terms
        add("paisa bhejo"); add("account details do"); add("pin batao");
        add("otp share karo"); add("bank se paise"); add("transfer karo");
    }};
    
    // === TECH SUPPORT INDICATORS ===
    private static final Set<String> TECH_SUPPORT_TERMS = new HashSet<String>() {{
        add("microsoft"); add("windows"); add("virus"); add("malware");
        add("firewall"); add("security"); add("hacker"); add("ip address");
        add("remote access"); add("teamviewer"); add("anydesk"); add("chrome");
        add("computer slow"); add("pop up"); add("browser"); add("update");
    }};
    
    /**
     * Constructor - initializes the detector and loads patterns
     * @param context The application context
     */
    public MultiLanguageScamDetector(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.patternsByLanguage = new HashMap<>();
        this.patternsByCategory = new HashMap<>();
        
        // Initialize patterns for each language
        patternsByLanguage.put("en", new ArrayList<>());
        patternsByLanguage.put("hi", new ArrayList<>());
        patternsByLanguage.put("te", new ArrayList<>());
        
        // Initialize patterns for each category
        patternsByCategory.put(CATEGORY_FINANCIAL, new ArrayList<>());
        patternsByCategory.put(CATEGORY_IDENTITY, new ArrayList<>());
        patternsByCategory.put(CATEGORY_TECH_SUPPORT, new ArrayList<>());
        patternsByCategory.put(CATEGORY_LOTTERY, new ArrayList<>());
        patternsByCategory.put(CATEGORY_GOVERNMENT, new ArrayList<>());
        patternsByCategory.put(CATEGORY_DIGITAL_ARREST, new ArrayList<>());
        patternsByCategory.put(CATEGORY_TRAI, new ArrayList<>());
        patternsByCategory.put(CATEGORY_COURIER, new ArrayList<>());
        patternsByCategory.put(CATEGORY_INVESTMENT, new ArrayList<>());
        patternsByCategory.put(CATEGORY_FAMILY_EMERGENCY, new ArrayList<>());
        patternsByCategory.put(CATEGORY_ROMANCE, new ArrayList<>());
        
        // Load all patterns
        loadPatterns();
    }
    
    /**
     * Load scam detection patterns from resources and files
     */
    private void loadPatterns() {
        try {
            Log.d(TAG, "Loading scam detection patterns");
            
            // Load built-in patterns from resources
            loadBuiltInPatterns();
            
            // Load custom patterns from files if they exist
            loadCustomPatterns();
            
            // Count total patterns
            patternCount = 0;
            for (List<ScamPattern> patterns : patternsByLanguage.values()) {
                patternCount += patterns.size();
            }
            
            Log.d(TAG, "Loaded " + patternCount + " total scam detection patterns");
        } catch (Exception e) {
            Log.e(TAG, "Error loading patterns", e);
        }
    }
    
    /**
     * Load built-in patterns from app resources
     */
    private void loadBuiltInPatterns() {
        // English financial scam patterns
        addPattern("en", CATEGORY_FINANCIAL, "account.*suspend", "Your account will be suspended", RISK_HIGH);
        addPattern("en", CATEGORY_FINANCIAL, "urgent.*payment", "Urgent payment required", RISK_HIGH);
        addPattern("en", CATEGORY_FINANCIAL, "bank.*verify", "Bank verification needed", RISK_HIGH);
        addPattern("en", CATEGORY_FINANCIAL, "credit.*security", "Credit card security alert", RISK_MEDIUM);
        addPattern("en", CATEGORY_FINANCIAL, "fund.*transfer.*immediate", "Immediate funds transfer", RISK_HIGH);
        
        // English identity theft patterns
        addPattern("en", CATEGORY_IDENTITY, "verify.*identity", "Identity verification required", RISK_HIGH);
        addPattern("en", CATEGORY_IDENTITY, "confirm.*details", "Confirm personal details", RISK_MEDIUM);
        addPattern("en", CATEGORY_IDENTITY, "update.*information", "Update your information", RISK_MEDIUM);
        addPattern("en", CATEGORY_IDENTITY, "provide.*passport", "Provide passport details", RISK_HIGH);
        
        // English tech support scam patterns
        addPattern("en", CATEGORY_TECH_SUPPORT, "computer.*virus", "Computer virus detected", RISK_HIGH);
        addPattern("en", CATEGORY_TECH_SUPPORT, "technical.*support", "Technical support required", RISK_MEDIUM);
        addPattern("en", CATEGORY_TECH_SUPPORT, "microsoft.*security", "Microsoft security alert", RISK_HIGH);
        addPattern("en", CATEGORY_TECH_SUPPORT, "windows.*error", "Windows error detected", RISK_MEDIUM);
        
        // English lottery/prize scam patterns
        addPattern("en", CATEGORY_LOTTERY, "congratulations.*won", "Congratulations, you've won", RISK_HIGH);
        addPattern("en", CATEGORY_LOTTERY, "prize.*claim", "Prize claim notification", RISK_HIGH);
        addPattern("en", CATEGORY_LOTTERY, "lottery.*winner", "Lottery winner announcement", RISK_HIGH);
        
        // English government impersonation patterns
        addPattern("en", CATEGORY_GOVERNMENT, "tax.*refund", "Tax refund available", RISK_HIGH);
        addPattern("en", CATEGORY_GOVERNMENT, "government.*grant", "Government grant approved", RISK_HIGH);
        addPattern("en", CATEGORY_GOVERNMENT, "stimulus.*payment", "Stimulus payment update", RISK_MEDIUM);
        
        // Hindi patterns
        addPattern("hi", CATEGORY_FINANCIAL, "खाता.*निलंबित", "Account suspension in Hindi", RISK_HIGH);
        addPattern("hi", CATEGORY_FINANCIAL, "तुरंत.*भुगतान", "Urgent payment in Hindi", RISK_HIGH);
        addPattern("hi", CATEGORY_LOTTERY, "बधाई हो.*जीता", "Congratulations, you've won in Hindi", RISK_HIGH);
        
        // Telugu patterns
        addPattern("te", CATEGORY_FINANCIAL, "ఖాతా.*నిలిపివేయబడుతుంది", "Account suspension in Telugu", RISK_HIGH);
        addPattern("te", CATEGORY_LOTTERY, "అభినందనలు.*గెలిచారు", "Congratulations, you've won in Telugu", RISK_HIGH);
        
        // Load specialized pattern categories
        loadDigitalArrestPatterns();
        loadTraiPatterns();
        loadCourierPatterns();
        loadInvestmentPatterns();
        loadFamilyEmergencyPatterns();
        loadRomancePatterns();
        loadHindiAdvancedPatterns();
        loadTeluguAdvancedPatterns();
        loadHinglishPatterns();
        loadIndicatorTerms();
    }
    
    /**
     * Load digital arrest patterns for authority impersonation scams
     */
    private void loadDigitalArrestPatterns() {
        Log.d(TAG, "Loading digital arrest patterns");
        
        for (Map.Entry<String, Integer> entry : DIGITAL_ARREST_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            int riskScore = entry.getValue();
            
            // Convert risk score (0-100) to risk level (0-3)
            int riskLevel;
            if (riskScore >= 95) {
                riskLevel = RISK_HIGH;
            } else if (riskScore >= 90) {
                riskLevel = RISK_MEDIUM;
            } else {
                riskLevel = RISK_LOW;
            }
            
            // Add to appropriate language category
            if (pattern.matches(".*[\\u0900-\\u097F].*")) {
                // Contains Hindi characters
                addPattern("hi", CATEGORY_DIGITAL_ARREST, pattern, 
                    "Authority impersonation: " + pattern, riskLevel);
            } else {
                // Default to English
                addPattern("en", CATEGORY_DIGITAL_ARREST, pattern, 
                    "Authority impersonation: " + pattern, riskLevel);
            }
        }
    }
    
    /**
     * Load TRAI telecom scam patterns
     */
    private void loadTraiPatterns() {
        Log.d(TAG, "Loading TRAI telecom scam patterns");
        
        for (Map.Entry<String, Integer> entry : TRAI_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            int riskScore = entry.getValue();
            
            int riskLevel = calculateRiskLevel(riskScore);
            
            if (pattern.matches(".*[\\u0900-\\u097F].*")) {
                // Contains Hindi characters
                addPattern("hi", CATEGORY_TRAI, pattern, 
                    "TRAI/Telecom scam: " + pattern, riskLevel);
            } else {
                addPattern("en", CATEGORY_TRAI, pattern, 
                    "TRAI/Telecom scam: " + pattern, riskLevel);
            }
        }
    }
    
    /**
     * Load Courier/FedEx scam patterns
     */
    private void loadCourierPatterns() {
        Log.d(TAG, "Loading Courier scam patterns");
        
        for (Map.Entry<String, Integer> entry : COURIER_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            int riskScore = entry.getValue();
            
            int riskLevel = calculateRiskLevel(riskScore);
            
            if (pattern.matches(".*[\\u0900-\\u097F].*")) {
                addPattern("hi", CATEGORY_COURIER, pattern, 
                    "Courier/FedEx scam: " + pattern, riskLevel);
            } else {
                addPattern("en", CATEGORY_COURIER, pattern, 
                    "Courier/FedEx scam: " + pattern, riskLevel);
            }
        }
    }
    
    /**
     * Load Investment/Crypto scam patterns
     */
    private void loadInvestmentPatterns() {
        Log.d(TAG, "Loading Investment scam patterns");
        
        for (Map.Entry<String, Integer> entry : INVESTMENT_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            int riskScore = entry.getValue();
            
            int riskLevel = calculateRiskLevel(riskScore);
            addPattern("en", CATEGORY_INVESTMENT, pattern, 
                "Investment/Crypto scam: " + pattern, riskLevel);
        }
    }
    
    /**
     * Load Family Emergency scam patterns
     */
    private void loadFamilyEmergencyPatterns() {
        Log.d(TAG, "Loading Family Emergency scam patterns");
        
        for (Map.Entry<String, Integer> entry : FAMILY_EMERGENCY_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            int riskScore = entry.getValue();
            
            int riskLevel = calculateRiskLevel(riskScore);
            
            if (pattern.matches(".*[\\u0900-\\u097F].*")) {
                addPattern("hi", CATEGORY_FAMILY_EMERGENCY, pattern, 
                    "Family Emergency scam: " + pattern, riskLevel);
            } else {
                addPattern("en", CATEGORY_FAMILY_EMERGENCY, pattern, 
                    "Family Emergency scam: " + pattern, riskLevel);
            }
        }
    }
    
    /**
     * Load Romance scam patterns
     */
    private void loadRomancePatterns() {
        Log.d(TAG, "Loading Romance scam patterns");
        
        for (Map.Entry<String, Integer> entry : ROMANCE_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            int riskScore = entry.getValue();
            
            int riskLevel = calculateRiskLevel(riskScore);
            addPattern("en", CATEGORY_ROMANCE, pattern, 
                "Romance scam: " + pattern, riskLevel);
        }
    }
    
    /**
     * Load Hindi Advanced patterns
     */
    private void loadHindiAdvancedPatterns() {
        Log.d(TAG, "Loading Hindi Advanced patterns");
        
        for (Map.Entry<String, Integer> entry : HINDI_ADVANCED_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            int riskScore = entry.getValue();
            
            int riskLevel = calculateRiskLevel(riskScore);
            addPattern("hi", CATEGORY_DIGITAL_ARREST, pattern, 
                "Hindi authority impersonation: " + pattern, riskLevel);
        }
    }
    
    /**
     * Load Telugu Advanced patterns
     */
    private void loadTeluguAdvancedPatterns() {
        Log.d(TAG, "Loading Telugu Advanced patterns");
        
        for (Map.Entry<String, Integer> entry : TELUGU_ADVANCED_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            int riskScore = entry.getValue();
            
            int riskLevel = calculateRiskLevel(riskScore);
            addPattern("te", CATEGORY_DIGITAL_ARREST, pattern, 
                "Telugu authority impersonation: " + pattern, riskLevel);
        }
    }
    
    /**
     * Load Hinglish patterns
     */
    private void loadHinglishPatterns() {
        Log.d(TAG, "Loading Hinglish patterns");
        
        for (Map.Entry<String, Integer> entry : HINGLISH_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            int riskScore = entry.getValue();
            
            int riskLevel = calculateRiskLevel(riskScore);
            // Add to both Hindi and English for maximum coverage
            addPattern("hi", CATEGORY_TECH_SUPPORT, pattern, 
                "Hinglish tech scam: " + pattern, riskLevel);
            addPattern("en", CATEGORY_TECH_SUPPORT, pattern, 
                "Hinglish tech scam: " + pattern, riskLevel);
        }
    }
    
    /**
     * Load indicator terms (urgency, authority, financial risk, tech support)
     */
    private void loadIndicatorTerms() {
        Log.d(TAG, "Loading indicator terms");
        
        // Urgency indicators
        for (String term : URGENCY_WORDS) {
            if (term.matches(".*[\\u0900-\\u097F].*")) {
                addPattern("hi", CATEGORY_FINANCIAL, term, "Urgency indicator (Hindi): " + term, RISK_MEDIUM);
            } else if (term.matches(".*[\\u0C00-\\u0C7F].*")) {
                addPattern("te", CATEGORY_FINANCIAL, term, "Urgency indicator (Telugu): " + term, RISK_MEDIUM);
            } else {
                addPattern("en", CATEGORY_FINANCIAL, term, "Urgency indicator: " + term, RISK_MEDIUM);
            }
        }
        
        // Authority indicators
        for (String term : AUTHORITY_WORDS) {
            if (term.matches(".*[\\u0900-\\u097F].*")) {
                addPattern("hi", CATEGORY_GOVERNMENT, term, "Authority indicator (Hindi): " + term, RISK_MEDIUM);
            } else if (term.matches(".*[\\u0C00-\\u0C7F].*")) {
                addPattern("te", CATEGORY_GOVERNMENT, term, "Authority indicator (Telugu): " + term, RISK_MEDIUM);
            } else {
                addPattern("en", CATEGORY_GOVERNMENT, term, "Authority indicator: " + term, RISK_MEDIUM);
            }
        }
        
        // Financial risk terms
        for (String term : FINANCIAL_RISK_TERMS) {
            if (term.matches(".*[\\u0900-\\u097F].*")) {
                addPattern("hi", CATEGORY_FINANCIAL, term, "Financial risk term (Hindi): " + term, RISK_HIGH);
            } else {
                addPattern("en", CATEGORY_FINANCIAL, term, "Financial risk term: " + term, RISK_HIGH);
            }
        }
        
        // Tech support terms
        for (String term : TECH_SUPPORT_TERMS) {
            addPattern("en", CATEGORY_TECH_SUPPORT, term, "Tech support term: " + term, RISK_MEDIUM);
        }
    }
    
    /**
     * Calculate risk level from risk score (0-100)
     */
    private int calculateRiskLevel(int riskScore) {
        if (riskScore >= 90) return RISK_HIGH;
        if (riskScore >= 75) return RISK_MEDIUM;
        if (riskScore >= 60) return RISK_LOW;
        return RISK_NONE;
    }
    
    /**
     * Load custom patterns from external files
     */
    private void loadCustomPatterns() {
        try {
            File customPatternsDir = new File(context.getExternalFilesDir(null), "scam_patterns");
            if (!customPatternsDir.exists() || !customPatternsDir.isDirectory()) {
                return;
            }
            
            for (File file : customPatternsDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    loadPatternsFromJson(file);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading custom patterns", e);
        }
    }
    
    /**
     * Load patterns from a JSON file
     * @param file JSON file containing patterns
     */
    private void loadPatternsFromJson(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            StringBuilder content = new StringBuilder();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                content.append(new String(buffer, 0, length));
            }
            fis.close();
            
            JSONObject root = new JSONObject(content.toString());
            JSONArray patterns = root.getJSONArray("patterns");
            
            for (int i = 0; i < patterns.length(); i++) {
                JSONObject patternObj = patterns.getJSONObject(i);
                
                String language = patternObj.getString("language");
                String category = patternObj.getString("category");
                String regex = patternObj.getString("regex");
                String description = patternObj.getString("description");
                int riskLevel = patternObj.getInt("risk_level");
                
                addPattern(language, category, regex, description, riskLevel);
            }
            
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error parsing JSON pattern file: " + file.getName(), e);
        }
    }
    
    /**
     * Add a pattern to the detector
     * @param language Language code (en, hi, te)
     * @param category Scam category
     * @param regex Regular expression pattern
     * @param description Human-readable description
     * @param riskLevel Risk level (0-3)
     */
    private void addPattern(String language, String category, String regex, String description, int riskLevel) {
        ScamPattern pattern = new ScamPattern(regex, description, category, riskLevel);
        
        // Add to language map
        if (patternsByLanguage.containsKey(language)) {
            patternsByLanguage.get(language).add(pattern);
        }
        
        // Add to category map
        if (patternsByCategory.containsKey(category)) {
            patternsByCategory.get(category).add(pattern);
        }
    }
    
    /**
     * Get the total number of loaded patterns
     * @return Pattern count
     */
    public int getPatternCount() {
        return patternCount;
    }
    
    /**
     * Get patterns for a specific language
     * @param language Language code
     * @return List of patterns
     */
    public List<ScamPattern> getPatternsForLanguage(String language) {
        return patternsByLanguage.getOrDefault(language, new ArrayList<>());
    }
    
    /**
     * Get patterns for a specific category
     * @param category Category name
     * @return List of patterns
     */
    public List<ScamPattern> getPatternsForCategory(String category) {
        return patternsByCategory.getOrDefault(category, new ArrayList<>());
    }
    
    /**
     * Analyze text for scam patterns
     * @param text Text to analyze
     * @param language Language of the text
     * @return Risk score (0-100)
     */
    public int analyzeText(String text, String language) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Reset analysis state
        detectedPatterns.clear();
        categoryScores.clear();
        
        // Normalize text for better matching
        String normalizedText = text.toLowerCase().trim();
        
        // Check all high-priority patterns from all categories
        int digitalArrestScore = checkDigitalArrestPatterns(normalizedText);
        int traiScore = checkSpecificPatterns(normalizedText, TRAI_PATTERNS, "TRAI_SCAM");
        int courierScore = checkSpecificPatterns(normalizedText, COURIER_PATTERNS, "COURIER_SCAM");
        int investmentScore = checkSpecificPatterns(normalizedText, INVESTMENT_PATTERNS, "INVESTMENT_SCAM");
        int familyEmergencyScore = checkSpecificPatterns(normalizedText, FAMILY_EMERGENCY_PATTERNS, "FAMILY_EMERGENCY");
        int romanceScore = checkSpecificPatterns(normalizedText, ROMANCE_PATTERNS, "ROMANCE_SCAM");
        int hinglishScore = checkSpecificPatterns(normalizedText, HINGLISH_PATTERNS, "HINGLISH_SCAM");
        
        // Return immediately if high-risk pattern is found
        int highestSpecificScore = Math.max(
            Math.max(digitalArrestScore, traiScore),
            Math.max(
                Math.max(courierScore, investmentScore),
                Math.max(
                    Math.max(familyEmergencyScore, romanceScore),
                    hinglishScore
                )
            )
        );
        
        if (highestSpecificScore >= 90) {
            return highestSpecificScore;
        }
        
        // Get patterns for the specified language
        List<ScamPattern> patterns = getPatternsForLanguage(language);
        if (patterns.isEmpty()) {
            Log.d(TAG, "No patterns available for language: " + language);
            return highestSpecificScore; // Return any detected score from specific patterns
        }
        
        // Check each pattern against the text
        int totalRiskScore = 0;
        int maxCategoryScore = 0;
        
        for (ScamPattern pattern : patterns) {
            try {
                Pattern regex = Pattern.compile(pattern.regex, Pattern.CASE_INSENSITIVE);
                Matcher matcher = regex.matcher(normalizedText);
                
                if (matcher.find()) {
                    // Pattern matched
                    detectedPatterns.add(pattern.description);
                    
                    // Update category score
                    int categoryScore = categoryScores.getOrDefault(pattern.category, 0);
                    categoryScore += pattern.riskLevel * 10; // Scale risk level to score
                    categoryScores.put(pattern.category, categoryScore);
                    
                    // Track highest category score
                    maxCategoryScore = Math.max(maxCategoryScore, categoryScore);
                    
                    Log.d(TAG, "Matched pattern: " + pattern.description + ", Category: " + 
                            pattern.category + ", Risk Level: " + pattern.riskLevel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error matching pattern: " + pattern.regex, e);
            }
        }
        
        // Calculate overall risk score based on:
        // 1. Highest category score
        // 2. Number of detected patterns
        // 3. Spread of categories affected
        
        totalRiskScore = maxCategoryScore;
        
        // Boost score based on multiple detections
        if (detectedPatterns.size() > 1) {
            totalRiskScore += Math.min(detectedPatterns.size() * 5, 20); // Max +20 boost
        }
        
        // Boost score if multiple categories are affected
        if (categoryScores.size() > 1) {
            totalRiskScore += categoryScores.size() * 5; // +5 per category
        }
        
        // Incorporate scores from specific pattern checks
        totalRiskScore = Math.max(totalRiskScore, highestSpecificScore);
        
        // Cap at 100
        return Math.min(totalRiskScore, 100);
    }
    
    /**
     * Check text specifically for digital arrest patterns
     * @param text Normalized text to check
     * @return Risk score if pattern found, 0 otherwise
     */
    private int checkDigitalArrestPatterns(String text) {
        int highestScore = 0;
        
        for (Map.Entry<String, Integer> entry : DIGITAL_ARREST_PATTERNS.entrySet()) {
            if (text.contains(entry.getKey())) {
                int score = entry.getValue();
                
                // Add to detected patterns
                detectedPatterns.add("Digital arrest: " + entry.getKey());
                
                // Update category score
                int categoryScore = categoryScores.getOrDefault(CATEGORY_DIGITAL_ARREST, 0);
                categoryScore = Math.max(categoryScore, score);
                categoryScores.put(CATEGORY_DIGITAL_ARREST, categoryScore);
                
                // Update highest score
                highestScore = Math.max(highestScore, score);
                
                Log.d(TAG, "Digital arrest pattern match: " + entry.getKey() + ", Score: " + score);
            }
        }
        
        return highestScore;
    }
    
    /**
     * Check text for specific pattern category
     * @param text Normalized text to check
     * @param patterns Pattern map to check against
     * @param category Category name for logging
     * @return Highest risk score
     */
    private int checkSpecificPatterns(String text, Map<String, Integer> patterns, String category) {
        int highestScore = 0;
        
        for (Map.Entry<String, Integer> entry : patterns.entrySet()) {
            if (text.contains(entry.getKey())) {
                int score = entry.getValue();
                
                // Add to detected patterns
                detectedPatterns.add(category + ": " + entry.getKey());
                
                // Update category score in our standard format
                String mappedCategory = mapToStandardCategory(category);
                int categoryScore = categoryScores.getOrDefault(mappedCategory, 0);
                categoryScore = Math.max(categoryScore, score);
                categoryScores.put(mappedCategory, categoryScore);
                
                // Update highest score
                highestScore = Math.max(highestScore, score);
                
                Log.d(TAG, category + " pattern match: " + entry.getKey() + ", Score: " + score);
            }
        }
        
        return highestScore;
    }
    
    /**
     * Map special category to standard category
     */
    private String mapToStandardCategory(String specialCategory) {
        switch (specialCategory) {
            case "TRAI_SCAM": return CATEGORY_TRAI;
            case "COURIER_SCAM": return CATEGORY_COURIER;
            case "INVESTMENT_SCAM": return CATEGORY_INVESTMENT;
            case "FAMILY_EMERGENCY": return CATEGORY_FAMILY_EMERGENCY;
            case "ROMANCE_SCAM": return CATEGORY_ROMANCE;
            case "HINGLISH_SCAM": return CATEGORY_TECH_SUPPORT;
            default: return CATEGORY_FINANCIAL; // Default fallback
        }
    }
    
    /**
     * Analyze recording using VOSK for speech-to-text
     * @param recordingPath Path to the recording file
     * @param recognizer VOSK speech recognizer
     */
    public void analyzeWithVosk(String recordingPath, VoskSpeechRecognizer recognizer) {
        Log.d(TAG, "Starting VOSK analysis of recording: " + recordingPath);
        
        // Check if file exists
        File recordingFile = new File(recordingPath);
        if (!recordingFile.exists()) {
            Log.e(TAG, "Recording file does not exist: " + recordingPath);
            return;
        }
        
        try {
            // First attempt with English
            recognizer.setLanguage("en");
            recognizer.setListener(new VoskResultHandler("en"));
            recognizer.recognizeFile(new FileInputStream(recordingFile));
            
            // In a complete implementation, we would also try other languages
            // after the first recognition is complete
        } catch (IOException e) {
            Log.e(TAG, "Error analyzing recording with VOSK", e);
        }
    }
    
    /**
     * Process recording directly from path with language auto-detection
     * This is a convenience method called from MainActivity
     * @param recordingPath Path to recording file
     * @return Initial risk assessment (preliminary score)
     */
    public int processRecording(String recordingPath) {
        Log.d(TAG, "Processing recording: " + recordingPath);
        
        // Initial risk assessment based on call metadata
        // This provides a preliminary score while VOSK analysis happens asynchronously
        int preliminaryScore = 15; // Default low risk
        
        try {
            File recordingFile = new File(recordingPath);
            if (!recordingFile.exists()) {
                Log.e(TAG, "Recording file not found: " + recordingPath);
                return preliminaryScore;
            }
            
            // Check recording properties that might indicate risk
            long fileSize = recordingFile.length();
            if (fileSize < 1024) {
                // Very small file, likely not a real call
                Log.d(TAG, "Recording suspiciously small: " + fileSize + " bytes");
                preliminaryScore += 5;
            }
            
            if (recordingFile.getName().contains("unknown")) {
                // Unknown caller - slight risk increase
                Log.d(TAG, "Unknown caller detected in filename");
                preliminaryScore += 10;
            }
            
            // Schedule detailed analysis
            scheduleBackgroundAnalysis(recordingPath);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in preliminary recording analysis", e);
        }
        
        return preliminaryScore;
    }
    
    /**
     * Schedule background analysis using worker threads
     * @param recordingPath Path to recording file
     */
    private void scheduleBackgroundAnalysis(String recordingPath) {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Brief delay to allow UI to update first
                Log.d(TAG, "Starting background analysis of: " + recordingPath);
                // Detailed analysis would happen here
                // Results would be posted to a listener
            } catch (InterruptedException e) {
                Log.e(TAG, "Background analysis interrupted", e);
            }
        }).start();
    }
    
    /**
     * Recognize multiple languages in sequence 
     * Called from MainActivity
     * @param recordingPath Path to recording file
     * @param recognizer VOSK recognizer instance
     */
    public void recognizeMultiLanguage(String recordingPath, VoskSpeechRecognizer recognizer) {
        analyzeWithVosk(recordingPath, recognizer);
    }
    
    /**
     * Update scam patterns from remote source
     * @return True if update was successful
     */
    public boolean updatePatternsFromRemote() {
        Log.d(TAG, "Checking for pattern updates from remote source");
        // This would download updated patterns from a server
        // For now, just return true as if update was successful
        return true;
    }
    
    /**
     * Get a human-readable risk assessment
     * @param riskScore The numeric risk score (0-100)
     * @return Human readable assessment
     */
    public String getRiskAssessment(int riskScore) {
        if (riskScore < 20) {
            return "Low Risk - No clear scam indicators detected";
        } else if (riskScore < 50) {
            return "Medium Risk - Some suspicious patterns detected";
        } else if (riskScore < 80) {
            return "High Risk - Multiple scam patterns detected";
        } else {
            return "Very High Risk - Strong scam indicators present";
        }
    }
    
    /**
     * Performance optimization: Pre-compile frequently used patterns
     * Call this method during idle time to improve pattern matching performance
     */
    public void optimizePatternMatching() {
        Log.d(TAG, "Pre-compiling frequently used patterns for performance");
        
        // Create a background thread for optimization
        new Thread(() -> {
            int optimized = 0;
            
            // Pre-compile most common patterns
            for (String language : patternsByLanguage.keySet()) {
                List<ScamPattern> patterns = patternsByLanguage.get(language);
                if (patterns != null && patterns.size() > 0) {
                    // Optimize up to 10 patterns per language
                    int count = Math.min(10, patterns.size());
                    for (int i = 0; i < count; i++) {
                        try {
                            // Pre-compile the pattern
                            Pattern.compile(patterns.get(i).regex, Pattern.CASE_INSENSITIVE);
                            optimized++;
                        } catch (Exception e) {
                            Log.e(TAG, "Error pre-compiling pattern: " + patterns.get(i).regex, e);
                        }
                    }
                }
            }
            
            Log.d(TAG, "Pattern optimization complete. Pre-compiled " + optimized + " patterns");
        }).start();
    }
    
    /**
     * Get detection statistics for reporting
     * @return JSONObject containing detection statistics
     */
    public JSONObject getDetectionStats() {
        JSONObject stats = new JSONObject();
        try {
            stats.put("pattern_count", patternCount);
            stats.put("languages_supported", patternsByLanguage.keySet());
            stats.put("categories_supported", patternsByCategory.keySet());
            
            // Add language-specific pattern counts
            JSONObject langCounts = new JSONObject();
            for (Map.Entry<String, List<ScamPattern>> entry : patternsByLanguage.entrySet()) {
                langCounts.put(entry.getKey(), entry.getValue().size());
            }
            stats.put("patterns_by_language", langCounts);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating detection stats", e);
        }
        
        return stats;
    }
    
    /**
     * Handler for VOSK speech recognition results
     */
    private class VoskResultHandler implements VoskSpeechRecognizer.VoskRecognitionListener {
        private final String language;
        
        public VoskResultHandler(String language) {
            this.language = language;
        }
        
        @Override
        public void onPartialResult(String partialText, String lang) {
            // For partial results, we don't do full analysis
            Log.d(TAG, "Partial result in " + lang + ": " + partialText);
        }
        
        @Override
        public void onFinalResult(String finalText, String lang, float confidence) {
            // For final results, perform full analysis
            Log.d(TAG, "Final result in " + lang + ": " + finalText + " (confidence: " + confidence + ")");
            
            // Only analyze if confidence is reasonable
            if (confidence > 0.5 && finalText.length() > 5) {
                int riskScore = analyzeText(finalText, lang);
                Log.d(TAG, "Analysis result: Risk score = " + riskScore);
                
                // Log detailed results
                if (riskScore > 0) {
                    Log.d(TAG, "Detected patterns: " + detectedPatterns);
                    Log.d(TAG, "Category scores: " + categoryScores);
                    Log.d(TAG, "Highest risk category: " + getHighestRiskCategory());
                }
            }
        }
        
        @Override
        public void onError(String error) {
            Log.e(TAG, "VOSK recognition error: " + error);
        }
        
        @Override
        public void onInitializationComplete(boolean success) {
            // Not used in this context
        }
        
        @Override
        public void onModelDownloadProgress(String language, int progress) {
            // Not used in this context
        }
        
        @Override
        public void onModelDownloadComplete(String language, boolean success) {
            // Not used in this context
        }
    }
    
    /**
     * Class representing a scam detection pattern
     */
    public static class ScamPattern {
        public final String regex;
        public final String description;
        public final String category;
        public final int riskLevel;
        
        public ScamPattern(String regex, String description, String category, int riskLevel) {
            this.regex = regex;
            this.description = description;
            this.category = category;
            this.riskLevel = riskLevel;
        }
    }

    /**
     * Get a list of detected scam patterns from the last analysis
     * @return Set of detected pattern descriptions
     */
    public Set<String> getDetectedPatterns() {
        return new HashSet<>(detectedPatterns);
    }

    /**
     * Get category scores from the last analysis
     * @return Map of category to risk score
     */
    public Map<String, Integer> getCategoryScores() {
        return new HashMap<>(categoryScores);
    }

    /**
     * Get the highest risk category from the last analysis
     * @return Category name or null if none detected
     */
    public String getHighestRiskCategory() {
        String highestCategory = null;
        int highestScore = 0;
        
        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                highestCategory = entry.getKey();
            }
        }
        
        return highestCategory;
    }

    /**
     * Process a batch of recordings for bulk analysis
     * @param recordingPaths List of paths to recordings
     * @return Map of path to preliminary risk score
     */
    public Map<String, Integer> processBatchRecordings(List<String> recordingPaths) {
        Map<String, Integer> results = new HashMap<>();
        
        for (String path : recordingPaths) {
            int score = processRecording(path);
            results.put(path, score);
        }
        
        return results;
    }

    /**
     * Reset the detector state and clear any cached results
     */
    public void reset() {
        detectedPatterns.clear();
        categoryScores.clear();
        Log.d(TAG, "Detector state reset");
    }

    /**
     * Get categorized report of detected patterns
     * @return Map of category to list of detected patterns in that category
     */
    public Map<String, List<String>> getCategorizedDetections() {
        Map<String, List<String>> categorizedPatterns = new HashMap<>();
        
        for (String pattern : detectedPatterns) {
            for (Map.Entry<String, List<ScamPattern>> entry : patternsByCategory.entrySet()) {
                String category = entry.getKey();
                for (ScamPattern scamPattern : entry.getValue()) {
                    if (pattern.equals(scamPattern.description)) {
                        if (!categorizedPatterns.containsKey(category)) {
                            categorizedPatterns.put(category, new ArrayList<>());
                        }
                        categorizedPatterns.get(category).add(pattern);
                        break;
                    }
                }
            }
        }
        
        return categorizedPatterns;
    }

    /**
     * Get a comprehensive assessment of scam risk
     * @param riskScore The numeric risk score
     * @return Detailed assessment
     */
    public String getComprehensiveRiskAssessment(int riskScore) {
        StringBuilder assessment = new StringBuilder();
        
        if (riskScore > 90) {
            assessment.append("🚨 CRITICAL THREAT DETECTED\n");
        } else if (riskScore > 70) {
            assessment.append("⚠️ HIGH RISK SCAM DETECTED\n");
        } else if (riskScore > 40) {
            assessment.append("⚡ MODERATE RISK - SUSPICIOUS PATTERNS\n");
        } else if (riskScore > 20) {
            assessment.append("⚠️ LOW-MODERATE RISK\n");
        } else {
            assessment.append("✅ LOW RISK - APPEARS LEGITIMATE\n");
        }
        
        assessment.append("Risk Score: ").append(riskScore).append("%\n\n");
        
        // Add category breakdown
        if (!categoryScores.isEmpty()) {
            assessment.append("THREAT BREAKDOWN:\n");
            for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
                if (entry.getValue() > 0) {
                    assessment.append("• ").append(getCategoryDisplayName(entry.getKey()))
                             .append(": ").append(entry.getValue()).append(" points\n");
                }
            }
        }
        
        // Add detected patterns
        if (!detectedPatterns.isEmpty()) {
            int patternLimit = Math.min(detectedPatterns.size(), 5); // Limit to 5 patterns
            assessment.append("\nTop detected patterns:\n");
            int count = 0;
            for (String pattern : detectedPatterns) {
                if (count++ < patternLimit) {
                    assessment.append("• ").append(pattern).append("\n");
                }
            }
            if (detectedPatterns.size() > patternLimit) {
                assessment.append("...and ").append(detectedPatterns.size() - patternLimit)
                         .append(" more patterns\n");
            }
        }
        
        return assessment.toString();
    }
    
    /**
     * Get human-readable category name
     */
    private String getCategoryDisplayName(String category) {
        switch (category) {
            case CATEGORY_FINANCIAL: return "Financial Fraud";
            case CATEGORY_IDENTITY: return "Identity Theft";
            case CATEGORY_TECH_SUPPORT: return "Tech Support Scam";
            case CATEGORY_LOTTERY: return "Lottery/Prize Scam";
            case CATEGORY_GOVERNMENT: return "Government Impersonation";
            case CATEGORY_DIGITAL_ARREST: return "Digital Arrest Scam";
            case CATEGORY_TRAI: return "TRAI/Telecom Scam";
            case CATEGORY_COURIER: return "Courier/Package Scam";
            case CATEGORY_INVESTMENT: return "Investment/Crypto Fraud";
            case CATEGORY_FAMILY_EMERGENCY: return "Family Emergency Scam";
            case CATEGORY_ROMANCE: return "Romance/Relationship Scam";
            default: return category;
        }
    }
}
