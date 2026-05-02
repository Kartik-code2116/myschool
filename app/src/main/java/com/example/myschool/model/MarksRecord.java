package com.example.myschool.model;

import java.util.HashMap;
import java.util.Map;

public class MarksRecord {
    public String id;
    public String studentId;
    public String classId;
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

    public MarksRecord() {}
}
