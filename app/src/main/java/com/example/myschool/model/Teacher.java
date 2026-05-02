package com.example.myschool.model;

import java.util.ArrayList;
import java.util.List;

public class Teacher {
    public String id;
    public String name;
    public String email;
    public String phone;
    public String photoUrl;
    public List<String> schoolIds = new ArrayList<>();

    public Teacher() {}
}
