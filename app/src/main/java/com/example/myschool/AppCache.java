package com.example.myschool;

import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;

/**
 * Simple static cache to pass complex objects between Activities
 * without Parcelable/Serializable overhead.
 */
public class AppCache {
    public static School      selectedSchool;
    public static ClassModel  selectedClass;
    public static Student     selectedStudent;
    public static MarksRecord selectedMarks;
}
