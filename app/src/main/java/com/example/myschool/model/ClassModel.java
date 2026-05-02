package com.example.myschool.model;

import java.util.ArrayList;
import java.util.List;

public class ClassModel {
    public String id;
    public String schoolId;
    public String className;   // "7"
    public String division;    // "A"
    public String examName;
    public int year;
    public List<Subject> subjects = new ArrayList<>();

    public ClassModel() {}

    public String getDisplayName() {
        return "Class " + className + " - " + division;
    }
}
