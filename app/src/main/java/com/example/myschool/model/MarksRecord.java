package com.example.myschool.model;

import java.util.HashMap;
import java.util.Map;

public class MarksRecord {
    public String id;
    public String studentId;
    public String classId;
    public String teacherId; // Security rule compliance field
    public String examName;
    public Map<String, Double> subjectMarks = new HashMap<>(); // subjectName -> obtained
    public Map<String, Integer> subjectMax = new HashMap<>();  // subjectName -> max
    public double totalObtained;
    public int totalMax;
    public double percentage;
    public String grade;
    public String result; // "PASS" or "FAIL"
    public String editedBy;
    public long updatedAt;

    // Detailed Myschool grading structure
    public Map<String, SubjectMarksDetail> detailedMarks = new HashMap<>();
    public int presentDays;    // हजर दिवस
    public int totalDays;      // एकूण दिवस
    public String semesterId;
    public String semesterNumber; // "1" or "2"

    public static class SubjectMarksDetail {
        // आकारिक (Formative - Part A)
        public int nirikhshan;    // निरीक्षण
        public int tondiKam;      // तोंडीकाम  
        public int pratyakshik;   // प्रात्यक्षिक
        public int upkram;        // उपक्रम
        public int prakalp;       // प्रकल्प
        public int chachani;      // चाचणी
        public int swadhyay;      // स्वाध्याय
        public int itar;          // इतर
        public int akarikTotal;   // एकूण A

        // संकलित (Summative - Part B)
        public int tondi;         // तोंडी
        public int pratyakshikB;  // प्रात्य.
        public int lekhi;         // लेखी
        public int sanklit;       // एकूण B
        
        public int grandTotal;    // एकूण (A+B)
        public int maxMarks;      // पैकी
        public String grade;      // श्रेणी

        // Descriptive remark for this subject
        public String remark;     // वर्णनात्मक नोंद

        public SubjectMarksDetail() {}
    }

    public MarksRecord() {}
}
