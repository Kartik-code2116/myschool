package com.kartik.myschool.model;

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

    // Detailed Edu Report grading structure
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

    public static String sanitizeKey(String key) {
        if (key == null) return "unknown";
        return key.replace(".", "_")
                  .replace("#", "_")
                  .replace("$", "_")
                  .replace("[", "_")
                  .replace("]", "_")
                  .replace("/", "_")
                  .replace("\\", "_")
                  .replace("~", "_")
                  .replace("*", "_");
    }

    public MarksRecord clone() {
        MarksRecord m = new MarksRecord();
        m.id = this.id;
        m.studentId = this.studentId;
        m.classId = this.classId;
        m.teacherId = this.teacherId;
        m.examName = this.examName;
        m.subjectMarks.putAll(this.subjectMarks);
        m.subjectMax.putAll(this.subjectMax);
        m.totalObtained = this.totalObtained;
        m.totalMax = this.totalMax;
        m.percentage = this.percentage;
        m.grade = this.grade;
        m.result = this.result;
        m.editedBy = this.editedBy;
        m.updatedAt = this.updatedAt;
        m.presentDays = this.presentDays;
        m.totalDays = this.totalDays;
        m.semesterId = this.semesterId;
        m.semesterNumber = this.semesterNumber;
        for (Map.Entry<String, SubjectMarksDetail> e : this.detailedMarks.entrySet()) {
            SubjectMarksDetail d = new SubjectMarksDetail();
            SubjectMarksDetail old = e.getValue();
            if (old != null) {
                d.nirikhshan = old.nirikhshan;
                d.tondiKam = old.tondiKam;
                d.pratyakshik = old.pratyakshik;
                d.upkram = old.upkram;
                d.prakalp = old.prakalp;
                d.chachani = old.chachani;
                d.swadhyay = old.swadhyay;
                d.itar = old.itar;
                d.akarikTotal = old.akarikTotal;
                d.tondi = old.tondi;
                d.pratyakshikB = old.pratyakshikB;
                d.lekhi = old.lekhi;
                d.sanklit = old.sanklit;
                d.grandTotal = old.grandTotal;
                d.maxMarks = old.maxMarks;
                d.grade = old.grade;
                d.remark = old.remark;
            }
            m.detailedMarks.put(e.getKey(), d);
        }
        return m;
    }
}
