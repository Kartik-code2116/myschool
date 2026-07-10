package com.kartik.myschool;

import com.kartik.myschool.model.AcademicYear;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Semester;
import com.kartik.myschool.model.Student;

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

    // Global sort options for students list
    public static final int SORT_BY_ROLL_ASC = 0;
    public static final int SORT_BY_ROLL_DESC = 1;
    public static final int SORT_BY_NAME_ASC = 2;
    public static final int SORT_BY_NAME_DESC = 3;
    public static int currentSortType = SORT_BY_ROLL_ASC;



    /** Teacher name — cached here for zero-latency display on home & profile screens. */
    public static String cachedTeacherName;

    /** classId → student count — cached to avoid re-fetching all students on every profile open. */
    public static Map<String, Integer> cachedStudentCountByClassId;

    // Student & Marks Caching for zero-latency screen loading
    public static List<Student> cachedStudents;
    public static Map<String, MarksRecord> cachedMarksMap;
    public static String cachedClassIdForStudents;
    public static String cachedSemesterIdForMarks;

    // Descriptive Entries Caching
    public static List<Student> cachedDescriptiveStudents;
    public static Map<String, MarksRecord> cachedDescriptiveMarksMap;
    public static String cachedDescriptiveClassId;
    public static String cachedDescriptiveSemesterId;
    public static boolean cachedDescriptiveMarksComplete = false;

    // ── Cross-screen save signals ─────────────────────────────────────────────
    // Set by EnterMarksActivity on success; consumed by FormativeSummativeFragment.onResume()
    public static boolean marksJustSaved           = false;
    public static String  marksJustSavedStudentId  = null;
    public static MarksRecord marksJustSavedRecord = null;

    // Set by EnterDescriptiveActivity on success; consumed by DescriptiveEntriesFragment.onResume()
    public static boolean descriptiveJustSaved           = false;
    public static String  descriptiveJustSavedStudentId  = null;
    public static MarksRecord descriptiveJustSavedRecord = null;

    // Per-subject remark entry context (set before launching SubjectRemarkEntryActivity)
    public static String selectedSubjectName  = null;
    public static int    selectedSubjectIndex = 0;

    // In-memory remark bank cache: subjectName -> list of remark options
    public static java.util.Map<String, java.util.List<String>> cachedRemarkBank = new java.util.HashMap<>();

    // ── TTL Cache Expiration (5 minutes) ───────────────────────────────────────
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private static final java.util.Map<String, Long> cacheTimestamps = new java.util.HashMap<>();

    /**
     * Checks if a cache entry is valid and hasn't expired.
     */
    public static boolean isCacheValid(String key) {
        Long timestamp = cacheTimestamps.get(key);
        if (timestamp == null) return false;
        return (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS;
    }

    /**
     * Marks a cache entry as fresh with the current timestamp.
     */
    public static void markCacheFresh(String key) {
        cacheTimestamps.put(key, System.currentTimeMillis());
    }

    /**
     * Invalidates a specific cache entry.
     */
    public static void invalidateCache(String key) {
        cacheTimestamps.remove(key);
    }
}
