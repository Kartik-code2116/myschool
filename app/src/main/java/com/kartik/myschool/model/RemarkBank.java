package com.kartik.myschool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore model for a teacher's custom remark option bank per subject.
 * Document ID: schoolId + "_" + sanitizedSubjectName  (or "general")
 * Collection: remarkBanks
 */
public class RemarkBank {
    public String id;            // Firestore document ID
    public String schoolId;
    public String subjectName;   // Subject this applies to; "general" for cross-subject defaults
    public List<String> options = new ArrayList<>();
    public long updatedAt;

    // Required no-arg constructor for Firestore deserialization
    public RemarkBank() {}

    public RemarkBank(String schoolId, String subjectName, List<String> options) {
        this.schoolId    = schoolId;
        this.subjectName = subjectName;
        this.options     = options != null ? options : new ArrayList<>();
        this.updatedAt   = System.currentTimeMillis();
    }

    /** Default remark options shown when no custom bank has been saved. */
    public static List<String> defaultOptionsFor(String subjectName, int semesterNumber) {
        List<String> defaults = new ArrayList<>();
        if (subjectName == null) subjectName = "";
        String s = subjectName.toLowerCase();

        boolean isSem1 = (semesterNumber == 1);

        if (s.contains("मराठी") || s.contains("marathi") || s.contains("hindi") || s.contains("हिंदी") ||
                s.contains("english") || s.contains("इंग्रजी") || s.contains("भाषा")) {
            if (isSem1) {
                defaults.add("प्रथम सत्रातील भाषिक प्रगती चांगली आहे.");
                defaults.add("वाचन व लेखन उत्तम आहे.");
                defaults.add("कविता गायन चांगल्या प्रकारे करतो.");
                defaults.add("अक्षर सुंदर व वळणदार आहे.");
                defaults.add("वाचनाचा सराव आवश्यक आहे.");
                defaults.add("लेखनात सुधारणा करावी.");
            } else {
                defaults.add("द्वितीय सत्रात भाषिक कौशल्य वाढले आहे.");
                defaults.add("वार्षिक प्रगती समाधानकारक आहे.");
                defaults.add("निबंध लेखन उत्तम करतो.");
                defaults.add("भाषण कौशल्य प्रभावी आहे.");
                defaults.add("व्याकरणाचा अधिक सराव हवा.");
            }
        } else if (s.contains("गणित") || s.contains("math")) {
            if (isSem1) {
                defaults.add("प्रथम सत्रातील गणितात प्रगती उत्तम.");
                defaults.add("बेरीज-वजाबाकी अचूक करतो.");
                defaults.add("पाढे पाठांतर चांगले आहे.");
                defaults.add("गणितात अधिक सराव आवश्यक.");
            } else {
                defaults.add("द्वितीय सत्रात गणितात चांगली समज आली आहे.");
                defaults.add("गुणाकार-भागाकार चांगला जमतो.");
                defaults.add("शाब्दिक उदाहरणे सहज सोडवतो.");
                defaults.add("वार्षिक प्रगती उत्तम आहे.");
            }
        } else if (s.contains("विज्ञान") || s.contains("science") || s.contains("परिसर")) {
            if (isSem1) {
                defaults.add("प्रथम सत्रात परिसराची माहिती उत्तम ठेवली आहे.");
                defaults.add("प्रयोगात रस घेतो.");
                defaults.add("निरीक्षण कौशल्य छान आहे.");
            } else {
                defaults.add("द्वितीय सत्रात विज्ञानातील प्रगती चांगली.");
                defaults.add("शास्त्रीय विचारसरणीत वाढ झाली आहे.");
                defaults.add("वार्षिक मूल्यमापनात छान कामगिरी.");
            }
        } else if (s.contains("कला") || s.contains("art") || s.contains("कार्यानुभव") || s.contains("work")) {
            if (isSem1) {
                defaults.add("प्रथम सत्रात कलाकुसरीत चांगली प्रगती.");
                defaults.add("चित्रकलेची आवड आहे.");
            } else {
                defaults.add("द्वितीय सत्रात नवनिर्मिती छान केली.");
                defaults.add("उपक्रमात उत्स्फूर्त सहभाग.");
            }
        } else if (s.contains("शारीरिक") || s.contains("खेळ") || s.contains("physical")) {
            if (isSem1) {
                defaults.add("प्रथम सत्रात खेळांमध्ये उत्साह दिसला.");
                defaults.add("मैदानी खेळात चांगली चमक.");
            } else {
                defaults.add("द्वितीय सत्रात शारीरिक क्षमता वाढली.");
                defaults.add("खेळाडूवृत्ती उत्तम आहे.");
            }
        } else {
            // Generic defaults
            if (isSem1) {
                defaults.add("प्रथम सत्रात अभ्यासात चांगली गती.");
                defaults.add("वर्गकार्यात सक्रिय सहभाग.");
                defaults.add("नियमित शाळेत उपस्थित असतो.");
                defaults.add("प्रथम सत्रात अधिक लक्ष देणे गरजेचे.");
            } else {
                defaults.add("द्वितीय सत्रातील प्रगती समाधानकारक.");
                defaults.add("संपूर्ण वर्षातील काम छान.");
                defaults.add("पुढील वर्षासाठी शुभेच्छा.");
                defaults.add("वार्षिक अभ्यासात अधिक मेहनत हवी.");
            }
        }
        return defaults;
    }
}
