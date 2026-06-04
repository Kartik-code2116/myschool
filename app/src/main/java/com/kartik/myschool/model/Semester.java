package com.kartik.myschool.model;

public class Semester {
    public String id;
    public String yearId;
    public String teacherId;
    public int number;        // 1 or 2
    public String name;       // "First Semester"
    public String subtitle;   // "Easy Reports"

    public Semester() {}

    public Semester(int number, String name, String subtitle) {
        this.number = number;
        this.name = name;
        this.subtitle = subtitle;
    }
}
