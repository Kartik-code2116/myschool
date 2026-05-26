package com.example.myschool;

import com.example.myschool.model.AcademicYear;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Semester;
import com.example.myschool.model.Student;

import java.util.List;
import java.util.Map;

/**
 * Simple static cache to pass complex objects between Activities
 * without Parcelable/Serializable overhead.
 */
public class AppCache {
    public static School      selectedSchool;
    public static ClassModel  selectedClass;
    public static Student     selectedStudent;
    public static MarksRecord selectedMarks;

    public static List<AcademicYear> cachedYears;
    public static List<Semester>     cachedSemesters;
    public static List<ClassModel>   cachedClasses;

    /** Teacher name — cached here for zero-latency display on home & profile screens. */
    public static String cachedTeacherName;

    /** classId → student count — cached to avoid re-fetching all students on every profile open. */
    public static Map<String, Integer> cachedStudentCountByClassId;
}
