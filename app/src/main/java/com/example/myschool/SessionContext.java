package com.example.myschool;

import com.example.myschool.model.AcademicYear;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.School;
import com.example.myschool.model.Semester;

/**
 * Global session for Year → Semester → Class selection (Myschool-style workflow).
 */
public final class SessionContext {

    public static AcademicYear selectedYear;
    public static Semester selectedSemester;
    public static School selectedSchool;
    public static ClassModel selectedClass;

    private SessionContext() {}

    public static String getYearLabel() {
        return selectedYear != null && selectedYear.label != null
                ? selectedYear.label : "2026-27";
    }

    public static String getSemesterLabel() {
        return selectedSemester != null && selectedSemester.name != null
                ? selectedSemester.name : "First Semester";
    }

    public static String getClassDivLabel() {
        if (selectedClass == null) return "Class: 1, Div: -";
        String div = selectedClass.division != null && !selectedClass.division.isEmpty()
                ? selectedClass.division : "-";
        return "Class: " + selectedClass.className + ", Div: " + div;
    }

    public static void syncFromAppCache() {
        if (AppCache.selectedSchool != null) selectedSchool = AppCache.selectedSchool;
        if (AppCache.selectedClass != null) selectedClass = AppCache.selectedClass;
    }

    public static void syncToAppCache() {
        AppCache.selectedSchool = selectedSchool;
        AppCache.selectedClass = selectedClass;
    }
}
