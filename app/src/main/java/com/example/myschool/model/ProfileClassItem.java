package com.example.myschool.model;

/** One row in the teacher profile class list (std 1–12, optional Firebase class). */
public class ProfileClassItem {
    public final int standard;
    public final ClassModel classModel;
    public int studentCount;

    public ProfileClassItem(int standard, ClassModel classModel) {
        this.standard = standard;
        this.classModel = classModel;
    }

    public boolean hasClass() {
        return classModel != null && classModel.id != null;
    }

    public String getDivision() {
        if (classModel == null || classModel.division == null || classModel.division.isEmpty()) {
            return "-";
        }
        return classModel.division;
    }
}
