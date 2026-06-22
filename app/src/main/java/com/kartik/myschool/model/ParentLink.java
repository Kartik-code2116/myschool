package com.kartik.myschool.model;

public class ParentLink {
    public String id; // matches studentId
    public String studentId;
    public String teacherId;
    public String code;
    public String parentUid;
    public String studentName;
    public String className;
    public String schoolName;
    public long createdAt;
    public long claimedAt;

    public ParentLink() {}
}
