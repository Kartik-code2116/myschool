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
    public static List<String> defaultOptionsFor(String subjectName) {
        List<String> defaults = new ArrayList<>();
        if (subjectName == null) subjectName = "";
        String s = subjectName.toLowerCase();

        if (s.contains("मराठी") || s.contains("marathi") || s.contains("hindi") || s.contains("हिंदी") ||
                s.contains("english") || s.contains("इंग्रजी") || s.contains("भाषा")) {
            defaults.add("वाचन उत्तम आहे.");
            defaults.add("लेखन सुंदर आहे.");
            defaults.add("उच्चार स्पष्ट आहेत.");
            defaults.add("कवितापठण उत्कृष्ट आहे.");
            defaults.add("लेखनात सुधारणा आवश्यक.");
            defaults.add("वाचन सुधारणे आवश्यक.");
        } else if (s.contains("गणित") || s.contains("math")) {
            defaults.add("गणितात प्रगती उत्तम आहे.");
            defaults.add("बेरीज-वजाबाकी चांगली जमते.");
            defaults.add("गुणाकार-भागाकार सुधारावा.");
            defaults.add("गणितात अधिक सराव आवश्यक.");
            defaults.add("शाब्दिक उदाहरणे चांगली सोडवतो.");
        } else if (s.contains("विज्ञान") || s.contains("science") || s.contains("परिसर")) {
            defaults.add("प्रयोगात रस आहे.");
            defaults.add("निरीक्षण कौशल्य उत्तम आहे.");
            defaults.add("शास्त्रीय विचारसरणी आहे.");
            defaults.add("परिसर अभ्यासात उत्सुकता दिसते.");
        } else if (s.contains("कला") || s.contains("art")) {
            defaults.add("चित्रकलेत उत्तम आहे.");
            defaults.add("सर्जनशीलता उत्तम आहे.");
            defaults.add("रंगकाम सुंदर आहे.");
        } else if (s.contains("शारीरिक") || s.contains("खेळ") || s.contains("physical")) {
            defaults.add("शारीरिक शिक्षणात उत्साही आहे.");
            defaults.add("खेळात आवड आहे.");
            defaults.add("मैदानी खेळात सहभाग उत्तम आहे.");
        } else {
            // Generic defaults
            defaults.add("अभ्यासात हुशार आहे.");
            defaults.add("नियमित शाळेत येतो.");
            defaults.add("वर्गकार्यात सक्रिय सहभाग घेतो.");
            defaults.add("शांत व संयमी स्वभाव.");
            defaults.add("मित्रांशी मिळूनमिसळून वागतो.");
            defaults.add("सुधारणा आवश्यक आहे.");
        }
        return defaults;
    }
}
