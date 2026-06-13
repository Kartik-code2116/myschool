package com.kartik.myschool.utils.pdf;

import android.content.Context;

public class PdfLocalizer {
    private static Boolean cachedIsEnglish = null;

    public static synchronized void clearCache() {
        cachedIsEnglish = null;
    }

    public static String get(Context ctx, String mrText, String enText) {
        if (isEnglish(ctx)) {
            return enText;
        }
        return mrText;
    }

    public static boolean isEnglish(Context ctx) {
        if (cachedIsEnglish != null) {
            return cachedIsEnglish;
        }
        if (ctx == null) return false;
        android.content.SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences("myschool_settings_prefs", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "mr");
        cachedIsEnglish = "en".equals(lang);
        return cachedIsEnglish;
    }

    public static String translateSubject(Context ctx, String subName) {
        if (subName == null) return "";
        boolean isEn = isEnglish(ctx);
        String s = subName.trim().toLowerCase();

        if (s.equals("english") || s.equals("इंग्रजी") || s.equals("इंग्रजी (english)")) {
            return isEn ? "English" : "इंग्रजी";
        }
        if (s.equals("marathi") || s.equals("मराठी") || s.equals("मराठी (marathi)")) {
            return isEn ? "Marathi" : "मराठी";
        }
        if (s.equals("mathematics") || s.equals("maths") || s.equals("math") || s.equals("गणित") || s.equals("गणित (mathematics)")) {
            return isEn ? "Mathematics" : "गणित";
        }
        if (s.equals("hindi") || s.equals("हिंदी")) {
            return isEn ? "Hindi" : "हिंदी";
        }
        if (s.equals("science") || s.equals("विज्ञान") || s.equals("science & tech") || s.contains("science & technology")) {
            return isEn ? "Science" : "विज्ञान";
        }
        if (s.equals("history") || s.equals("इतिहास")) {
            return isEn ? "History" : "इतिहास";
        }
        if (s.equals("geography") || s.equals("भूगोल")) {
            return isEn ? "Geography" : "भूगोल";
        }
        if (s.equals("civics") || s.equals("नागरिकशास्त्र")) {
            return isEn ? "Civics" : "नागरिकशास्त्र";
        }
        if (s.equals("social science") || s.equals("social sciences") || s.equals("सामाजिक शास्त्र") || s.equals("इतिहास व नागरिकशास्त्र") || s.equals("भूगोल व सामाजिक शास्त्र")) {
            return isEn ? "Social Science" : "सामाजिक शास्त्र";
        }
        if (s.contains("परिसर अभ्यास १") || s.contains("परिसर अभ्यास 1") || s.contains("evs 1") || s.contains("evs i")) {
            return isEn ? "EVS 1" : "परिसर अभ्यास १";
        }
        if (s.contains("परिसर अभ्यास २") || s.contains("परिसर अभ्यास 2") || s.contains("evs 2") || s.contains("evs ii")) {
            return isEn ? "EVS 2" : "परिसर अभ्यास २";
        }
        if (s.contains("परिसर अभ्यास") || s.equals("environmental studies") || s.equals("evs")) {
            return isEn ? "EVS" : "परिसर अभ्यास";
        }
        if (s.equals("art") || s.equals("drawing") || s.equals("कला")) {
            return isEn ? "Art" : "कला";
        }
        if (s.equals("work experience") || s.equals("work exp") || s.equals("workexperience") || s.equals("कार्यानुभव")) {
            return isEn ? "Work Experience" : "कार्यानुभव";
        }
        if (s.equals("physical education") || s.equals("p.e.") || s.equals("p.e") || s.equals("शारीरिक शिक्षण") || s.equals("शा.शि.") || s.equals("शा. शिक्षण")) {
            return isEn ? "Physical Education" : "शारीरिक शिक्षण";
        }
        if (s.equals("personality development") || s.equals("व्यक्तित्व विकास") || s.equals("व्यक्तिमत्त्व विकास")) {
            return isEn ? "Personality Development" : "व्यक्तिमत्त्व विकास";
        }
        if (s.contains("information & comm") || s.contains("ict") || s.contains("माहिती व संप्रेषण")) {
            return isEn ? "Information & Comm. Technology (ICT)" : "माहिती व संप्रेषण तंत्रज्ञान (ICT)";
        }
        if (s.contains("water security") || s.contains("जलसुरक्षा")) {
            return isEn ? "Water Security & Environment Studies" : "जलसुरक्षा व पर्यावरण अभ्यास";
        }
        
        // General contains checks
        if (s.contains("english") || s.contains("इंग्रजी")) return isEn ? "English" : "इंग्रजी";
        if (s.contains("marathi") || s.contains("मराठी")) return isEn ? "Marathi" : "मराठी";
        if (s.contains("math") || s.contains("गणित")) return isEn ? "Mathematics" : "गणित";
        if (s.contains("hindi") || s.contains("हिंदी")) return isEn ? "Hindi" : "हिंदी";
        if (s.contains("science") || s.contains("विज्ञान")) return isEn ? "Science" : "विज्ञान";
        if (s.contains("social") || s.contains("soc.") || s.contains("soc science") || s.contains("सामाजिक")) return isEn ? "Social Science" : "सामाजिक शास्त्र";
        if (s.equals("art") || s.contains("drawing") || s.contains("कला")) return isEn ? "Art" : "कला";
        if (s.contains("work exp") || s.contains("workexp") || s.contains("कार्यानुभव")) return isEn ? "Work Experience" : "कार्यानुभव";
        if (s.contains("physical") || s.contains("p.e.") || s.equals("pe") || s.contains("शारीरिक") || s.contains("शा.शि.")) return isEn ? "Physical Education" : "शारीरिक शिक्षण";
        if (s.contains("personality") || s.contains("व्यक्तित्व") || s.contains("व्यक्तिमत्त्व")) return isEn ? "Personality Development" : "व्यक्तिमत्त्व विकास";
        if (s.contains("ict") || s.contains("संप्रेषण")) return isEn ? "Information & Comm. Technology (ICT)" : "माहिती व संप्रेषण तंत्रज्ञान (ICT)";
        if (s.contains("water security") || s.contains("जलसुरक्षा")) return isEn ? "Water Security & Environment Studies" : "जलसुरक्षा व पर्यावरण अभ्यास";
        if (s.contains("इतिहास")) return isEn ? "History" : "इतिहास";
        if (s.contains("भूगोल")) return isEn ? "Geography" : "भूगोल";

        if (s.contains("urdu") || s.contains("उर्दू")) return isEn ? "Urdu" : "उर्दू";
        if (s.contains("sanskrit") || s.contains("संस्कृत")) return isEn ? "Sanskrit" : "संस्कृत";
        if (s.contains("gujarati") || s.contains("गुजराती")) return isEn ? "Gujarati" : "गुजराती";
        if (s.contains("kannada") || s.contains("कन्नड")) return isEn ? "Kannada" : "कन्नड";
        if (s.contains("telugu") || s.contains("तेलुगू")) return isEn ? "Telugu" : "तेलुगू";
        if (s.contains("bengali") || s.contains("बंगाली")) return isEn ? "Bengali" : "बंगाली";
        if (s.contains("sindhi") || s.contains("सिंधी")) return isEn ? "Sindhi" : "सिंधी";
        if (s.contains("tamil") || s.contains("तमिळ")) return isEn ? "Tamil" : "तमिळ";
        if (s.contains("malayalam") || s.contains("मल्याळम")) return isEn ? "Malayalam" : "मल्याळम";
        if (s.contains("punjabi") || s.contains("पंजाबी")) return isEn ? "Punjabi" : "पंजाबी";
        if (s.contains("odia") || s.contains("ओडिया")) return isEn ? "Odia" : "ओडिया";
        if (s.contains("assamese") || s.contains("आसामी")) return isEn ? "Assamese" : "आसामी";

        if (s.contains("vishesh pragati") || s.contains("special development") || s.contains("vishesh vikas") || s.contains("विशेष प्रगती") || s.contains("विशेष विकास")) return isEn ? "Special Development" : "विशेष विकास";
        if (s.contains("aavad,chanda,etc") || s.contains("aavad, chanda, etc") || s.contains("aavad,chhand")) return isEn ? "Interests, Hobbies, etc" : "आवड, छंद, इत्यादी";
        if (s.contains("sudharna aavashyaka") || s.contains("sudharna aavasghyaka")) return isEn ? "Needs Improvement" : "सुधारणा आवश्यक";
        if (s.contains("vyaktimatva gun vishgesh") || s.contains("vyaktimatva gun vishesh")) return isEn ? "Personality Traits" : "व्यक्तिमत्त्व गुण विशेष";

        return subName;
    }
}
