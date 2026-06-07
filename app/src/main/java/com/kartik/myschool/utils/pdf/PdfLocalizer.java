package com.kartik.myschool.utils.pdf;

import android.content.Context;

public class PdfLocalizer {
    public static String get(Context ctx, String mrText, String enText) {
        if (ctx == null) return mrText;
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("myschool_settings_prefs", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "mr");
        if ("en".equals(lang)) {
            return enText;
        }
        return mrText;
    }

    public static boolean isEnglish(Context ctx) {
        if (ctx == null) return false;
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("myschool_settings_prefs", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "mr");
        return "en".equals(lang);
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
        
        // General contains checks
        if (s.contains("english") || s.contains("इंग्रजी")) return isEn ? "English" : "इंग्रजी";
        if (s.contains("marathi") || s.contains("मराठी")) return isEn ? "Marathi" : "मराठी";
        if (s.contains("math") || s.contains("गणित")) return isEn ? "Mathematics" : "गणित";
        if (s.contains("hindi") || s.contains("हिंदी")) return isEn ? "Hindi" : "हिंदी";
        if (s.contains("science") || s.contains("विज्ञान")) return isEn ? "Science" : "विज्ञान";
        if (s.contains("social") || s.contains("सामाजिक")) return isEn ? "Social Science" : "सामाजिक शास्त्र";
        if (s.contains("art") || s.contains("drawing") || s.contains("कला")) return isEn ? "Art" : "कला";
        if (s.contains("work exp") || s.contains("workexp") || s.contains("कार्यानुभव")) return isEn ? "Work Experience" : "कार्यानुभव";
        if (s.contains("phys") || s.contains("p.e.") || s.contains("pe") || s.contains("शारीरिक") || s.contains("शा.शि.")) return isEn ? "Physical Education" : "शारीरिक शिक्षण";
        if (s.contains("इतिहास")) return isEn ? "History" : "इतिहास";
        if (s.contains("भूगोल")) return isEn ? "Geography" : "भूगोल";

        return subName;
    }
}
