package com.kartik.myschool.model;

public class Student {
    public String id;
    public String classId;
    public String schoolId;
    public String teacherId;
    public String className;
    public String schoolName;
    public String standard;
    public String division;

    // Basic
    public String name;
    public String rollNo;
    public String rollNo2;
    public String registrationNo;
    public String dob;
    public String gender;
    public String cast;
    public String parentName;
    public String photoUrl;
    public boolean marksEntered;
    public String heightSem1;
    public String weightSem1;
    public String heightSem2;
    public String weightSem2;

    // Monthly Attendance: e.g., "जून" -> "20/22"
    public java.util.Map<String, String> monthlyAttendance = new java.util.HashMap<>();

    // Family
    public String motherName;
    public String motherOccupation;
    public String motherPhone;
    public String fatherName;
    public String fatherOccupation;
    public String fatherPhone;
    public String address;

    // Bank
    public String bankName;
    public String bankAccount;
    public String bankBranch;
    public String bankIfsc;
    public String bankUid;

    // Academic
    public String medium;
    public String motherTongue;
    public String dateOfAdmission;
    public String studentIdNumber;
    public String uid;

    public Student() {}
}
