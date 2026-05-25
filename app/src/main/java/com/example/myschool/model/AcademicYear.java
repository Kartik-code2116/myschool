package com.example.myschool.model;

public class AcademicYear {
    public String id;
    public String teacherId;
    public String schoolId;
    public String label;      // e.g. "2026-27"
    public int startYear;
    public int endYear;
    public boolean active;

    public AcademicYear() {}

    public AcademicYear(String label, int start, int end) {
        this.label = label;
        this.startYear = start;
        this.endYear = end;
        this.active = true;
    }
}
