package com.kartik.myschool.model;

import java.util.ArrayList;
import java.util.List;

public class ClassModel {
    public String id;
    public String schoolId;
    public String teacherId; // Security rule compliance field
    public String yearId;
    public String academicYearLabel;
    public String semesterId;
    public String className;   // "7"
    public String division;    // "A"
    public String examName;
    public int year;
    public String teacherName;
    public String assistantTeacherName;
    public String teacherEmail;
    public String teacherPhone;
    public int studentCount;
    public List<Subject> subjects = new ArrayList<>();
    public java.util.Map<String, Integer> monthlyWorkingDays = new java.util.HashMap<>();

    public ClassModel() {}

    public String getDisplayName() {
        return "Class " + className + " - " + division;
    }
}
