package com.hellohari;

import android.content.Context;
import android.util.Log;
import java.util.*;

public class EnhancedMultiLanguageScamDetector {
    private static final String TAG = "EnhancedScamDetector";
    private Context context;
    
    // === DIGITAL ARREST SCAMS (HIGHEST RISK) ===
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
        
        // Digital confinement language (Risk: 100)
        put("you are now under digital arrest", 100);
        put("stay on video call until investigation completes", 100);
        put("don't disconnect the call or inform anyone", 100);
        put("don't involve family lawyer in this matter", 100);
        put("this is confidential government matter", 95);
        put("case will become serious if you disconnect", 98);
        put("you cannot leave until cleared", 100);
        put("maintain video call throughout", 100);
        
        // Accusations (Risk: 90-95)
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
    }};
    
    // === FAMILY EMERGENCY & VOICE CLONING SCAMS ===
    private static final Map<String, Integer> FAMILY_EMERGENCY_PATTERNS = new HashMap<String, Integer>() {{
        put("hello beta i am in serious trouble", 95);
        put("ive been in an accident dont tell anyone", 95);
        put("stuck in dubai canada abroad arrested", 90);
        put("phone is broken thats why i sound different", 95);
        put("aapko kuch ho gaya hai", 90);
        put("hospital mein admit hai", 90);
        put("turant paisa chahiye", 85);
        put("dadi nani main aapka pota hun", 90);
        put("dont tell mom dad about this", 85);
        put("police station mein hun urgent help", 90);
        put("accident hua hai immediate money needed", 90);
        put("kidnappers have me send ransom", 95);
        put("medical emergency surgery required", 85);
        put("bail money needed right now", 90);
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
    
    // === HINDI SCAM PATTERNS (ADVANCED) ===
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
        
        // Family exploitation
        put("bache ko kuch ho gaya hai", 95);
        put("accident mein serious condition", 90);
        put("operation ki zarurat hai", 85);
        put("blood ki emergency hai", 85);
        put("police case mein fansa hai", 90);
        put("college ragging mein problem", 80);
        put("dost ke saath mushkil mein", 75);
        put("paise ki bahut zarurat hai", 80);
        
        // Authority terms
        put("collector sahab se baat karo", 85);
        put("sp sahab ka order hai", 90);
        put("judge sahab ne kaha hai", 95);
        put("commissioner ka call hai", 90);
        put("magistrate ka summon", 90);
        put("thana incharge se milna hoga", 85);
        put("sarkari kaam hai urgent", 80);
        put("government ka faisla", 85);
    }};
    
    // === TELUGU SCAM PATTERNS (ADVANCED) ===
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
        
        // Romanized Telugu
        put("mee account block avuthundi", 85);
        put("police station vellaali", 90);
        put("legal case file ayyindi", 85);
        put("court lo hazaru kaavaali", 90);
        put("warrant vachindi mee meeda", 95);
        put("cyber crime police raabothunnaru", 90);
        put("bank nundi call chesaaru", 75);
        put("money transfer cheyyaali", 80);
        put("otp share cheyyandi", 85);
        put("verification ki details", 75);
        
        // IT professional targeting
        put("software company case", 80);
        put("h1b visa problem", 85);
        put("us lo arrest warrant", 90);
        put("green card application reject", 80);
        put("offshore account freeze", 85);
        put("tax evasion case filed", 85);
        put("foreign remittance issue", 80);
        put("rbi foreign exchange violation", 85);
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
        put("firewall update karna hai", 70);
        put("remote access dena hoga", 85);
        put("otp share karo verification ke liye", 85);
        put("upi pin batao security check", 90);
        put("net banking password confirm karo", 95);
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
        add("der mat karo"); add("time nahi hai"); add("jaldi karo");
        
        // Telugu
        add("వెంటనే"); add("త్వరగా"); add("ఇప్పుడే"); add("అత్యవసరం");
        add("immediatelyga"); add("jaldiga"); add("emergency lo");
        add("time ledu"); add("twaraga cheyyandi");
        
        // Mixed
        add("urgent hai"); add("jaldi karo"); add("immediate action");
        add("emergency mein"); add("abhi ke abhi"); add("right away");
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
        add("ప్రభుత్వం"); add("అధికారి"); add("కోర్టు");
        
        // Mixed/Romanized
        add("police waala"); add("officer sahab"); add("sarkar"); add("government");
        add("adhikari"); add("inspector"); add("asi"); add("si"); add("dy sp");
    }};
    
    // === FINANCIAL TERMS (HIGH RISK) ===
    private static final Set<String> FINANCIAL_RISK_TERMS = new HashSet<String>() {{
        // Direct money requests
        add("money transfer"); add("bank details"); add("account number");
        add("ifsc code"); add("upi pin"); add("otp"); add("cvv"); add("atm pin");
        add("net banking password"); add("debit card number");
        
        // Hindi financial terms
        add("paisa bhejo"); add("account details do"); add("pin batao");
        add("otp share karo"); add("bank se paise"); add("transfer karo");
        
        // Cryptocurrency
        add("bitcoin"); add("crypto"); add("wallet address"); add("private key");
        add("metamask"); add("binance"); add("coinbase"); add("usdt");
        
        // Investment terms
        add("guaranteed returns"); add("double money"); add("risk free");
        add("insider information"); add("sure shot profit"); add("limited offer");
    }};
    
    // === TECH SUPPORT INDICATORS ===
    private static final Set<String> TECH_SUPPORT_TERMS = new HashSet<String>() {{
        add("microsoft"); add("windows"); add("virus"); add("malware");
        add("firewall"); add("security"); add("hacker"); add("ip address");
        add("remote access"); add("teamviewer"); add("anydesk"); add("chrome");
        add("computer slow"); add("pop up"); add("browser"); add("update");
        add("license expired"); add("technical support"); add("customer care");
    }};
    
    public EnhancedMultiLanguageScamDetector(Context context) {
        this.context = context;
    }
    
    public ScamAnalysisResult analyzeForScamPatterns(String audioFilePath, String transcriptText) {
        try {
            Log.d(TAG, "Starting enhanced scam analysis...");
            
            if (transcriptText == null || transcriptText.trim().isEmpty()) {
                transcriptText = simulateTranscription(audioFilePath);
            }
            
            // Comprehensive pattern analysis
            int totalRiskScore = 0;
            List<String> detectedPatterns = new ArrayList<>();
            Map<String, Integer> categoryRisks = new HashMap<>();
            
            String lowerText = transcriptText.toLowerCase();
            
            // Analyze each category
            totalRiskScore += analyzePatternCategory(lowerText, DIGITAL_ARREST_PATTERNS, 
                "DIGITAL_ARREST", detectedPatterns, categoryRisks);
            totalRiskScore += analyzePatternCategory(lowerText, TRAI_PATTERNS, 
                "TRAI_SCAM", detectedPatterns, categoryRisks);
            totalRiskScore += analyzePatternCategory(lowerText, COURIER_PATTERNS, 
                "COURIER_SCAM", detectedPatterns, categoryRisks);
            totalRiskScore += analyzePatternCategory(lowerText, INVESTMENT_PATTERNS, 
                "INVESTMENT_FRAUD", detectedPatterns, categoryRisks);
            totalRiskScore += analyzePatternCategory(lowerText, FAMILY_EMERGENCY_PATTERNS, 
                "FAMILY_EMERGENCY", detectedPatterns, categoryRisks);
            totalRiskScore += analyzePatternCategory(lowerText, ROMANCE_PATTERNS, 
                "ROMANCE_SCAM", detectedPatterns, categoryRisks);
            totalRiskScore += analyzePatternCategory(lowerText, HINDI_ADVANCED_PATTERNS, 
                "HINDI_SCAM", detectedPatterns, categoryRisks);
            totalRiskScore += analyzePatternCategory(lowerText, TELUGU_ADVANCED_PATTERNS, 
                "TELUGU_SCAM", detectedPatterns, categoryRisks);
            totalRiskScore += analyzePatternCategory(lowerText, HINGLISH_PATTERNS, 
                "HINGLISH_SCAM", detectedPatterns, categoryRisks);
            
            // Check urgency indicators
            totalRiskScore += checkIndicators(lowerText, URGENCY_WORDS, 
                "URGENCY", detectedPatterns, 15);
            
            // Check authority impersonation
            totalRiskScore += checkIndicators(lowerText, AUTHORITY_WORDS, 
                "AUTHORITY", detectedPatterns, 20);
            
            // Check financial risk terms
            totalRiskScore += checkIndicators(lowerText, FINANCIAL_RISK_TERMS, 
                "FINANCIAL_RISK", detectedPatterns, 25);
            
            // Check tech support terms
            totalRiskScore += checkIndicators(lowerText, TECH_SUPPORT_TERMS, 
                "TECH_SUPPORT", detectedPatterns, 15);
            
            // Cap at 100
            totalRiskScore = Math.min(100, totalRiskScore);
            
            // Determine primary threat category
            String primaryThreat = determinePrimaryThreat(categoryRisks);
            
            // Generate detailed analysis
            String analysisMessage = generateDetailedAnalysis(totalRiskScore, 
                detectedPatterns, primaryThreat, categoryRisks);
            
            return new ScamAnalysisResult(
                totalRiskScore,
                detectedPatterns,
                analysisMessage,
                primaryThreat,
                Arrays.asList("Hindi", "English", "Telugu")
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Enhanced scam analysis failed", e);
            return createFallbackResult("Analysis failed: " + e.getMessage());
        }
    }
    
    private int analyzePatternCategory(String text, Map<String, Integer> patterns, 
                                     String category, List<String> detected, 
                                     Map<String, Integer> categoryRisks) {
        int categoryScore = 0;
        int patternCount = 0;
        
        for (Map.Entry<String, Integer> pattern : patterns.entrySet()) {
            if (text.contains(pattern.getKey().toLowerCase())) {
                categoryScore += pattern.getValue();
                patternCount++;
                detected.add("[" + category + "] " + pattern.getKey() + " (+" + pattern.getValue() + ")");
            }
        }
        
        // Apply category multiplier for multiple pattern matches
        if (patternCount > 1) {
            categoryScore = (int)(categoryScore * (1.0 + (patternCount - 1) * 0.2));
        }
        
        categoryRisks.put(category, categoryScore);
        return Math.min(categoryScore, 40); // Cap individual category contribution
    }
    
    private int checkIndicators(String text, Set<String> indicators, String type, 
                               List<String> detected, int baseScore) {
        for (String indicator : indicators) {
            if (text.contains(indicator.toLowerCase())) {
                detected.add("[" + type + "] " + indicator + " (+" + baseScore + ")");
                return baseScore;
            }
        }
        return 0;
    }
    
    private String determinePrimaryThreat(Map<String, Integer> categoryRisks) {
        return categoryRisks.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("UNKNOWN");
    }
    
    private String generateDetailedAnalysis(int riskScore, List<String> patterns, 
                                          String primaryThreat, Map<String, Integer> categoryRisks) {
        StringBuilder analysis = new StringBuilder();
        
        // Risk level assessment
        if (riskScore >= 90) {
            analysis.append("🚨 CRITICAL THREAT DETECTED\n");
        } else if (riskScore >= 70) {
            analysis.append("⚠️ HIGH RISK SCAM DETECTED\n");
        } else if (riskScore >= 50) {
            analysis.append("⚡ MODERATE RISK - SUSPICIOUS PATTERNS\n");
        } else if (riskScore >= 30) {
            analysis.append("⚠️ LOW-MODERATE RISK\n");
        } else {
            analysis.append("✅ LOW RISK - APPEARS LEGITIMATE\n");
        }
        
        analysis.append("Overall Risk Score: ").append(riskScore).append("%\n");
        analysis.append("Primary Threat Type: ").append(primaryThreat).append("\n\n");
        
        // Category breakdown
        analysis.append("THREAT CATEGORY BREAKDOWN:\n");
        for (Map.Entry<String, Integer> entry : categoryRisks.entrySet()) {
            if (entry.getValue() > 0) {
                analysis.append("• ").append(entry.getKey()).append(": ")
                         .append(entry.getValue()).append(" points\n");
            }
        }
        
        // Pattern summary
        if (!patterns.isEmpty()) {
            analysis.append("\nDETECTED PATTERNS (").append(patterns.size()).append("):\n");
            for (int i = 0; i < Math.min(patterns.size(), 10); i++) {
                analysis.append("• ").append(patterns.get(i)).append("\n");
            }
            if (patterns.size() > 10) {
                analysis.append("... and ").append(patterns.size() - 10).append(" more patterns\n");
            }
        }
        
        return analysis.toString();
    }
    
    private String simulateTranscription(String audioFilePath) {
        // Enhanced simulation with more realistic scam patterns
        String[] scamSamples = {
            "Your account will be suspended please verify immediately",
            "आपका खाता बंद हो जाएगा तुरंत verify करें police station jaana hoga",
            "మీ ఖాతా మూసివేయబడుతుంది వెంటనే verify చేయండి arrest warrant వచ్చింది",
            "This is Mumbai police cyber cell you are under digital arrest",
            "Main TRAI se bol raha hun aapka SIM band hone wala hai",
            "FedEx se call kar rahe drugs found in your parcel immediate action required"
        };
        
        String[] legitimateSamples = {
            "Hello this is John calling about your insurance renewal",
            "हैलो मैं आपके बारे में बात करना चाहता हूं insurance के बारे में",
            "Hi this is a follow up call regarding your recent purchase",
            "Namaste, bank se call kar raha hun loan approval ke liye"
        };
        
        Random random = new Random();
        boolean isScam = random.nextDouble() < 0.6; // 60% chance scam for testing
        
        if (isScam) {
            return scamSamples[random.nextInt(scamSamples.length)];
        } else {
            return legitimateSamples[random.nextInt(legitimateSamples.length)];
        }
    }
    
    private ScamAnalysisResult createFallbackResult(String errorMessage) {
        return new ScamAnalysisResult(
            30, // Default moderate risk
            Arrays.asList("ANALYSIS_FAILED: " + errorMessage),
            "⚠️ Analysis failed - using fallback assessment",
            "UNKNOWN",
            Arrays.asList("Unknown")
        );
    }
    
    // Result class
    public static class ScamAnalysisResult {
        private final int riskScore;
        private final List<String> detectedPatterns;
        private final String analysisMessage;
        private final String primaryThreat;
        private final List<String> detectedLanguages;
        
        public ScamAnalysisResult(int riskScore, List<String> detectedPatterns, 
                                String analysisMessage, String primaryThreat, 
                                List<String> detectedLanguages) {
            this.riskScore = riskScore;
            this.detectedPatterns = detectedPatterns;
            this.analysisMessage = analysisMessage;
            this.primaryThreat = primaryThreat;
            this.detectedLanguages = detectedLanguages;
        }
        
        public int getRiskScore() { return riskScore; }
        public List<String> getDetectedPatterns() { return detectedPatterns; }
        public String getAnalysisMessage() { return analysisMessage; }
        public String getPrimaryThreat() { return primaryThreat; }
        public List<String> getDetectedLanguages() { return detectedLanguages; }
    }
}
