package com.example.myschool.model;

public class Student {
    public String id;
    public String classId;
    public String schoolId;
    public String teacherId;    // Bug #1 fix: required for getAllStudentsForTeacher query
    public String className;    // Bug #5 fix: denormalized for display in adapter
    public String schoolName;   // Bug #5 fix: denormalized for display in adapter
    public String name;
    public String rollNo;
    public String dob;
    public String gender;
    public String parentName;
    public String photoUrl;
    public boolean marksEntered;

    public Student() {}
}
