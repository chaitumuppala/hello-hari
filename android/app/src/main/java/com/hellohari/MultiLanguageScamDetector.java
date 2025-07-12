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
        put("commodity trading signals", 70);
        put("gold investment scheme", 68);
        put("real estate fixed returns", 70);
        put("startup equity investment", 75);
        put("peer to peer lending", 72);
        put("cryptocurrency mining pool", 75);
        put("defi staking rewards", 78);
        put("nft investment opportunity", 65);
        put("metaverse land purchase", 62);
        put("blockchain technology investment", 70);
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
        put("operation ki zarurat hai", 85);
        put("blood ki emergency hai", 85);
        put("police case mein fansa hai", 90);
        put("college ragging mein problem", 80);
        put("dost ke saath mushkil mein", 75);
        put("paise ki bahut zarurat hai", 80);
        put("mama chacha emergency", 85);
        put("bua ki tabiyat kharab", 82);
        put("nana nani hospital", 88);
        put("cousin brother accident", 85);
        put("family member arrested", 92);
        put("relative needs urgent surgery", 88);
        put("grandmother heart attack", 90);
        put("uncle needs immediate help", 85);
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
        put("military deployment restricted", 78);
        put("peacekeeping mission abroad", 76);
        put("oil rig worker", 72);
        put("doctor without borders", 74);
        put("overseas construction project", 70);
        put("diplomatic mission confidential", 82);
        put("international business meeting", 68);
        put("medical conference emergency", 70);
        put("family illness need money", 85);
        put("wallet stolen in foreign country", 80);
        put("bank account frozen abroad", 82);
        put("emergency medical treatment", 78);
        put("legal issues need lawyer fees", 85);
        put("hotel bill payment problem", 75);
        put("flight cancellation stranded", 72);
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
        
        // Authority terms
        put("collector sahab se baat karo", 85);
        put("sp sahab ka order hai", 90);
        put("judge sahab ne kaha hai", 95);
        put("commissioner ka call hai", 90);
        put("magistrate ka summon", 90);
        put("thana incharge se milna hoga", 85);
        put("sarkari kaam hai urgent", 80);
        put("government ka faisla", 85);
        put("mantri ji ka order", 88);
        put("secretary sahab ka message", 85);
        put("dm sahab se baat", 87);
        put("ias officer calling", 85);
        put("ips officer urgent", 88);
        put("tehsildar ka notice", 82);
        put("patwari se verification", 75);
        put("bjp office se call", 70);
        put("congress office urgent", 70);
        put("aap party worker", 68);
        put("election commission notice", 85);
        put("returning officer message", 80);
        
        // Banking/Financial Hindi
        put("bank manager urgent call", 75);
        put("loan default case", 85);
        put("emi bounce notice", 80);
        put("credit card block", 78);
        put("account overdraft", 76);
        put("cheque bounce case", 85);
        put("loan recovery agent", 82);
        put("bank fraud detection", 85);
        put("suspicious transaction", 80);
        put("kyc verification pending", 75);
        put("aadhar link mandatory", 72);
        put("pan card verification", 70);
        put("income tax notice", 85);
        put("gst registration issue", 78);
        put("service tax pending", 75);
        put("property tax notice", 72);
        put("electricity bill default", 68);
        put("gas connection problem", 65);
        put("water bill pending", 62);
        put("telephone bill issue", 65);
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
        put("it returns filing problem", 78);
        put("form 16 discrepancy", 75);
        put("tds certificate issue", 72);
        put("pf account problem", 70);
        put("esi registration issue", 68);
        put("visa interview call", 82);
        put("embassy verification", 85);
        put("consulate urgent message", 80);
        put("immigration department", 88);
        put("homeland security", 90);
        put("customs declaration", 75);
        put("airport security alert", 85);
        
        // Regional Telugu patterns
        put("collector garu message", 85);
        put("sp garu urgent call", 88);
        put("mla garu office", 75);
        put("mp garu secretary", 78);
        put("cm office nundi", 90);
        put("governor office call", 85);
        put("ias officer message", 82);
        put("ips officer urgent", 85);
        put("mandal officer call", 75);
        put("village secretary", 68);
        put("vro office urgent", 70);
        put("vra verification", 65);
        put("asha worker message", 60);
        put("anganwadi urgent", 58);
        put("school headmaster", 65);
        put("principal urgent call", 68);
        put("college fees issue", 70);
        put("hostel fee pending", 68);
        put("scholarship problem", 72);
        put("fee reimbursement", 70);
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
        put("credit card details verify", 88);
        put("debit card block ho gaya", 82);
        put("internet banking suspended", 85);
        put("mobile banking issue", 78);
        put("paytm account problem", 75);
        put("phonepe verification", 72);
        put("google pay security", 75);
        put("bhim app update", 68);
        put("upi transaction failed", 70);
        put("digital wallet freeze", 80);
        put("crypto wallet hack", 85);
        put("trading account issue", 82);
        put("demat account problem", 78);
        put("mutual fund redemption", 72);
        put("insurance claim pending", 75);
    }};
    
    // === URGENCY INDICATORS (CROSS-LANGUAGE) ===
    private static final Set<String> URGENCY_WORDS = new HashSet<String>() {{
        // English
        add("immediately"); add("urgent"); add("now"); add("quickly"); add("emergency");
        add("instant"); add("right now"); add("within minutes"); add("before midnight");
        add("today only"); add("limited time"); add("last chance"); add("expires soon");
        add("deadline"); add("time sensitive"); add("critical"); add("asap");
        add("without delay"); add("right away"); add("this instant"); add("at once");
        
        // Hindi
        add("turant"); add("jaldi"); add("abhi"); add("foran"); add("tatkal");
        add("emergency"); add("zaruri"); add("aaj hi"); add("do ghante mein");
        add("der mat karo"); add("time nahi hai"); add("jaldi karo");
        add("abhi ke abhi"); add("is waqt"); add("isi samay"); add("turant se");
        
        // Telugu
        add("వెంటనే"); add("త్వరగా"); add("ఇప్పుడే"); add("అత్యవసరం");
        add("immediatelyga"); add("jaldiga"); add("emergency lo");
        add("time ledu"); add("twaraga cheyyandi"); add("ventane cheyandi");
        
        // Mixed
        add("urgent hai"); add("jaldi karo"); add("immediate action");
        add("emergency mein"); add("abhi ke abhi"); add("right away");
        add("turant karo"); add("emergency call"); add("urgent matter");
    }};
    
    // === AUTHORITY INDICATORS ===
    private static final Set<String> AUTHORITY_WORDS = new HashSet<String>() {{
        // Law enforcement
        add("police"); add("cbi"); add("ncb"); add("ed"); add("income tax");
        add("customs"); add("rbi"); add("sebi"); add("trai"); add("court");
        add("judge"); add("magistrate"); add("collector"); add("commissioner");
        add("inspector"); add("superintendent"); add("deputy"); add("assistant");
        add("constable"); add("head constable"); add("sub inspector"); add("circle officer");
        
        // Hindi authorities
        add("पुलिस"); add("न्यायाधीश"); add("कलेक्टर"); add("आयुक्त");
        add("थाना"); add("कोर्ट"); add("सरकार"); add("अफसर");
        add("मजिस्ट्रेट"); add("न्यायालय"); add("पुलिस अधीक्षक");
        
        // Telugu authorities
        add("పోలీసు"); add("న్యాయమూర్తి"); add("కలెక్టర్"); add("కమిషనర్");
        add("ప్రభుత్వం"); add("అధికారి"); add("కోర్టు"); add("న్యాయస్థానం");
        
        // Mixed/Romanized
        add("police waala"); add("officer sahab"); add("sarkar"); add("government");
        add("adhikari"); add("inspector"); add("asi"); add("si"); add("dy sp");
        add("circle inspector"); add("crime branch"); add("special branch");
        add("vigilance"); add("anti corruption"); add("enforcement");
    }};
    
    // === FINANCIAL TERMS (HIGH RISK) ===
    private static final Set<String> FINANCIAL_RISK_TERMS = new HashSet<String>() {{
        // Direct money requests
        add("money transfer"); add("bank details"); add("account number");
        add("ifsc code"); add("upi pin"); add("otp"); add("cvv"); add("atm pin");
        add("net banking password"); add("debit card number"); add("credit card details");
        add("expiry date"); add("security code"); add("mpin"); add("transaction password");
        
        // Hindi financial terms
        add("paisa bhejo"); add("account details do"); add("pin batao");
        add("otp share karo"); add("bank se paise"); add("transfer karo");
        add("paise ki zarurat"); add("amount send"); add("rupaye bhejo");
        
        // Banking apps
        add("phonepe"); add("paytm"); add("google pay"); add("bhim upi");
        add("amazon pay"); add("mobikwik"); add("freecharge"); add("airtel money");
        
        // Cryptocurrency
        add("bitcoin"); add("crypto"); add("wallet address"); add("private key");
        add("metamask"); add("binance"); add("coinbase"); add("usdt");
        add("ethereum"); add("dogecoin"); add("blockchain"); add("mining");
        
        // Investment terms
        add("guaranteed returns"); add("double money"); add("risk free");
        add("insider information"); add("sure shot profit"); add("limited offer");
        add("high returns"); add("quick money"); add("easy profit");
    }};
    
    // === TECH SUPPORT INDICATORS ===
    private static final Set<String> TECH_SUPPORT_TERMS = new HashSet<String>() {{
        add("microsoft"); add("windows"); add("virus"); add("malware");
        add("firewall"); add("security"); add("hacker"); add("ip address");
        add("remote access"); add("teamviewer"); add("anydesk"); add("chrome");
        add("computer slow"); add("pop up"); add("browser"); add("update");
        add("license expired"); add("technical support"); add("customer care");
        add("antivirus"); add("trojan"); add("spyware"); add("ransomware");
        add("phishing"); add("suspicious activity"); add("unauthorized access");
        add("system compromise"); add("data breach"); add("identity theft");
    }};

    public MultiLanguageScamDetector(Context context) {
        this.context = context;
    }

    public ScamAnalysisResult analyzeRecording(String audioFilePath) {
        try {
            Log.d(TAG, "Starting comprehensive multi-language scam analysis for: " + audioFilePath);
            
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
            // Enhanced simulation with realistic scam patterns for testing
            String[] scamSamples = {
                "Your account will be suspended please verify immediately or face legal action",
                "आपका खाता बंद हो जाएगा तुरंत verify करें police station jaana hoga arrest warrant",
                "మీ ఖాతా మూసివేయబడుతుంది వెంటనే verify చేయండి arrest warrant వచ్చింది court lo",
                "This is Mumbai police cyber cell you are under digital arrest stay on video call",
                "Main TRAI se bol raha hun aapka SIM band hone wala hai 22 complaints",
                "FedEx se call kar rahe drugs found in your parcel 140 grams narcotics thailand",
                "Bank manager calling account hack ho gaya virus detect security breach",
                "Guaranteed crypto returns 10x profit join VIP group limited time offer expires",
                "Hello beta serious trouble accident hospital money needed urgent don't tell parents",
                "I love you different from others god brought us together visa money needed"
            };
            
            String[] legitimateSamples = {
                "Hello this is John calling about your insurance renewal policy update",
                "हैलो मैं आपके बारे में बात करना चाहता हूं insurance के बारे में renewal",
                "Hi this is a follow up call regarding your recent purchase thank you",
                "Namaste bank se call kar raha hun loan approval ke liye documents",
                "Good morning calling from hospital appointment reminder tomorrow",
                "मैं आपकी कार सर्विस के बारे में बात करना चाहता हूं next week",
                "Hello delivery update your order will arrive tomorrow evening",
                "School se call kar rahe admission process ke liye meeting"
            };
            
            Random random = new Random();
            boolean isScam = random.nextDouble() < 0.7; // 70% chance scam for comprehensive testing
            
            if (isScam) {
                return scamSamples[random.nextInt(scamSamples.length)];
            } else {
                return legitimateSamples[random.nextInt(legitimateSamples.length)];
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Transcription failed for " + languageCode, e);
            return null;
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
        if (lower.contains("the") || lower.contains("and") || lower.contains("है") || lower.contains("का") || lower.contains("మీ") || lower.contains("నీ")) {
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
        Map<String, Integer> categoryRisks = new HashMap<>();
        
        // Analyze each transcription result
        for (TranscriptionResult result : transcription.getResults()) {
            if (result.getQuality() > maxConfidence) {
                maxConfidence = result.getQuality();
                primaryLanguage = result.getLanguageName();
            }
            
            detectedLanguages.add(result.getLanguageName());
            String transcript = result.getTranscript().toLowerCase();
            
            // Check all pattern categories
            int categoryScore = 0;
            
            // Digital Arrest patterns (highest priority)
            categoryScore = checkPatterns(transcript, DIGITAL_ARREST_PATTERNS, detectedPatterns, "DIGITAL_ARREST");
            categoryRisks.put("DIGITAL_ARREST", categoryScore);
            totalRiskScore += categoryScore;
            
            // TRAI/Telecom patterns
            categoryScore = checkPatterns(transcript, TRAI_PATTERNS, detectedPatterns, "TRAI_SCAM");
            categoryRisks.put("TRAI_SCAM", categoryScore);
            totalRiskScore += categoryScore;
            
            // Courier/Package patterns
            categoryScore = checkPatterns(transcript, COURIER_PATTERNS, detectedPatterns, "COURIER_SCAM");
            categoryRisks.put("COURIER_SCAM", categoryScore);
            totalRiskScore += categoryScore;
            
            // Investment/Crypto patterns
            categoryScore = checkPatterns(transcript, INVESTMENT_PATTERNS, detectedPatterns, "INVESTMENT_FRAUD");
            categoryRisks.put("INVESTMENT_FRAUD", categoryScore);
            totalRiskScore += categoryScore;
            
            // Family Emergency patterns
            categoryScore = checkPatterns(transcript, FAMILY_EMERGENCY_PATTERNS, detectedPatterns, "FAMILY_EMERGENCY");
            categoryRisks.put("FAMILY_EMERGENCY", categoryScore);
            totalRiskScore += categoryScore;
            
            // Romance Scam patterns
            categoryScore = checkPatterns(transcript, ROMANCE_PATTERNS, detectedPatterns, "ROMANCE_SCAM");
            categoryRisks.put("ROMANCE_SCAM", categoryScore);
            totalRiskScore += categoryScore;
            
            // Language-specific patterns
            if ("Hindi".equals(result.getLanguageName())) {
                categoryScore = checkPatterns(transcript, HINDI_ADVANCED_PATTERNS, detectedPatterns, "HINDI_SCAM");
                categoryRisks.put("HINDI_SCAM", categoryScore);
                totalRiskScore += categoryScore;
            } else if ("Telugu".equals(result.getLanguageName())) {
                categoryScore = checkPatterns(transcript, TELUGU_ADVANCED_PATTERNS, detectedPatterns, "TELUGU_SCAM");
                categoryRisks.put("TELUGU_SCAM", categoryScore);
                totalRiskScore += categoryScore;
            }
            
            // Mixed language patterns (always check)
            categoryScore = checkPatterns(transcript, HINGLISH_PATTERNS, detectedPatterns, "HINGLISH_SCAM");
            categoryRisks.put("HINGLISH_SCAM", categoryScore);
            totalRiskScore += categoryScore;
            
            // Check urgency and authority indicators
            totalRiskScore += checkUrgencyIndicators(transcript, detectedPatterns);
            totalRiskScore += checkAuthorityIndicators(transcript, detectedPatterns);
            totalRiskScore += checkFinancialRiskTerms(transcript, detectedPatterns);
            totalRiskScore += checkTechSupportTerms(transcript, detectedPatterns);
            
            // Weight by transcription quality
            totalRiskScore = (int)(totalRiskScore * result.getQuality());
        }
        
        // Boost score if multiple languages detected (often indicates scam)
        if (detectedLanguages.size() > 1) {
            totalRiskScore += 15;
            detectedPatterns.add("MULTI-LANG: Multiple languages detected (+15)");
        }
        
        // Determine primary threat category
        String primaryThreat = determinePrimaryThreat(categoryRisks);
        
        // Cap the score at 100
        totalRiskScore = Math.min(100, totalRiskScore);
        
        return new ScamAnalysisResult(
            totalRiskScore,
            detectedPatterns,
            generateAnalysisMessage(totalRiskScore, detectedPatterns, primaryLanguage, primaryThreat, categoryRisks),
            primaryLanguage,
            detectedLanguages
        );
    }
    
    private int checkPatterns(String transcript, Map<String, Integer> patterns, List<String> detected, String category) {
        int score = 0;
        int patternCount = 0;
        
        for (Map.Entry<String, Integer> pattern : patterns.entrySet()) {
            if (transcript.contains(pattern.getKey().toLowerCase())) {
                score += pattern.getValue();
                patternCount++;
                detected.add("[" + category + "] " + pattern.getKey() + " (+" + pattern.getValue() + ")");
            }
        }
        
        // Apply multiplier for multiple patterns in same category
        if (patternCount > 1) {
            score = (int)(score * (1.0 + (patternCount - 1) * 0.1)); // 10% boost per additional pattern
        }
        
        return score;
    }
    
    private int checkUrgencyIndicators(String transcript, List<String> detected) {
        for (String urgencyWord : URGENCY_WORDS) {
            if (transcript.toLowerCase().contains(urgencyWord.toLowerCase())) {
                detected.add("URGENCY: " + urgencyWord + " (+15)");
                return 15; // Only count once per category
            }
        }
        return 0;
    }
    
    private int checkAuthorityIndicators(String transcript, List<String> detected) {
        for (String authorityWord : AUTHORITY_WORDS) {
            if (transcript.toLowerCase().contains(authorityWord.toLowerCase())) {
                detected.add("AUTHORITY: " + authorityWord + " (+20)");
                return 20; // Only count once per category
            }
        }
        return 0;
    }
    
    private int checkFinancialRiskTerms(String transcript, List<String> detected) {
        for (String financialTerm : FINANCIAL_RISK_TERMS) {
            if (transcript.toLowerCase().contains(financialTerm.toLowerCase())) {
                detected.add("FINANCIAL_RISK: " + financialTerm + " (+25)");
                return 25; // High risk for financial terms
            }
        }
        return 0;
    }
    
    private int checkTechSupportTerms(String transcript, List<String> detected) {
        for (String techTerm : TECH_SUPPORT_TERMS) {
            if (transcript.toLowerCase().contains(techTerm.toLowerCase())) {
                detected.add("TECH_SUPPORT: " + techTerm + " (+12)");
                return 12; // Moderate risk for tech support
            }
        }
        return 0;
    }
    
    private String determinePrimaryThreat(Map<String, Integer> categoryRisks) {
        String primaryThreat = "UNKNOWN";
        int maxRisk = 0;
        
        for (Map.Entry<String, Integer> entry : categoryRisks.entrySet()) {
            if (entry.getValue() > maxRisk) {
                maxRisk = entry.getValue();
                primaryThreat = entry.getKey();
            }
        }
        
        return primaryThreat;
    }
    
    private String generateAnalysisMessage(int riskScore, List<String> patterns, String primaryLanguage, 
                                         String primaryThreat, Map<String, Integer> categoryRisks) {
        StringBuilder message = new StringBuilder();
        
        if (riskScore > 90) {
            message.append("🚨 CRITICAL THREAT DETECTED\n");
        } else if (riskScore > 70) {
            message.append("⚠️ HIGH RISK SCAM DETECTED\n");
        } else if (riskScore > 40) {
            message.append("⚡ MODERATE RISK - SUSPICIOUS PATTERNS\n");
        } else if (riskScore > 20) {
            message.append("⚠️ LOW-MODERATE RISK\n");
        } else {
            message.append("✅ LOW RISK - APPEARS LEGITIMATE\n");
        }
        
        message.append("Primary Language: ").append(primaryLanguage).append("\n");
        message.append("Risk Score: ").append(riskScore).append("%\n");
        message.append("Primary Threat: ").append(primaryThreat).append("\n");
        
        // Add category breakdown if there are significant risks
        if (riskScore > 30) {
            message.append("\nTHREAT BREAKDOWN:\n");
            for (Map.Entry<String, Integer> entry : categoryRisks.entrySet()) {
                if (entry.getValue() > 0) {
                    message.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" points\n");
                }
            }
        }
        
        if (!patterns.isEmpty()) {
            message.append("\nDetected Patterns (").append(patterns.size()).append("):\n");
            // Show first 10 patterns
            for (int i = 0; i < Math.min(patterns.size(), 10); i++) {
                message.append("• ").append(patterns.get(i)).append("\n");
            }
            if (patterns.size() > 10) {
                message.append("... and ").append(patterns.size() - 10).append(" more patterns\n");
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
    
    // Public method to get pattern count for statistics
    public int getPatternCount() {
        return DIGITAL_ARREST_PATTERNS.size() + 
               TRAI_PATTERNS.size() + 
               COURIER_PATTERNS.size() + 
               INVESTMENT_PATTERNS.size() + 
               FAMILY_EMERGENCY_PATTERNS.size() + 
               ROMANCE_PATTERNS.size() + 
               HINDI_ADVANCED_PATTERNS.size() + 
               TELUGU_ADVANCED_PATTERNS.size() + 
               HINGLISH_PATTERNS.size() +
               URGENCY_WORDS.size() +
               AUTHORITY_WORDS.size() +
               FINANCIAL_RISK_TERMS.size() +
               TECH_SUPPORT_TERMS.size();
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

    /**
     * REAL VOSK INTEGRATION - This will replace the simulation above
     */
    public void analyzeWithVosk(String audioFilePath, VoskSpeechRecognizer voskRecognizer) {
        if (voskRecognizer == null || !voskRecognizer.isInitialized()) {
            Log.w(TAG, "VOSK not available, using simulation");
            return;
        }
        
        // Set up VOSK recognition listener
        voskRecognizer.setRecognitionListener(new VoskSpeechRecognizer.VoskRecognitionListener() {
            @Override
            public void onPartialResult(String partialText, String language) {
                Log.d(TAG, "VOSK Partial (" + language + "): " + partialText);
            }
            
            @Override
            public void onFinalResult(String finalText, String language, float confidence) {
                Log.d(TAG, "VOSK Final (" + language + "): " + finalText + " (confidence: " + confidence + ")");
                
                // Analyze the real transcription with our scam patterns
                ScamAnalysisResult result = analyzeTranscriptionForScams(
                    new MultiLanguageTranscription(java.util.Arrays.asList(
                        new TranscriptionResult(finalText, getLanguageName(language), language, confidence)
                    ))
                );
                
                Log.d(TAG, "VOSK Analysis Complete - Risk Score: " + result.getRiskScore() + "%");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "VOSK Recognition Error: " + error);
            }
            
            @Override
            public void onInitializationComplete(boolean success) {
                Log.d(TAG, "VOSK Initialization: " + (success ? "SUCCESS" : "FAILED"));
            }
            
            @Override
            public void onModelDownloadProgress(String language, int progress) {
                Log.d(TAG, "VOSK Model Download Progress - " + language + ": " + progress + "%");
            }
            
            @Override
            public void onModelDownloadComplete(String language, boolean success) {
                Log.d(TAG, "VOSK Model Download Complete - " + language + ": " + (success ? "SUCCESS" : "FAILED"));
            }
        });
        
        // Start multi-language recognition
        voskRecognizer.recognizeMultiLanguage(audioFilePath);
    }
}
