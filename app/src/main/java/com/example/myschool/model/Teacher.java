package com.example.myschool.model;

import java.util.ArrayList;
import java.util.List;

public class Teacher {
    public String id;
    public String name;
    public String email;
    public String phone;
    public String photoUrl;
    public String udiseCode;
    public String schoolName;
    public String academicYearLabel;
    public String pasteLinkResult;
    public List<String> schoolIds = new ArrayList<>();
    public String subscriptionStatus = "inactive"; // inactive, active, pending
    public long subscriptionExpiry = 0; // timestamp
    public int studentsCount = 0;

    public Teacher() {}
}
