package com.kartik.myschool;

import com.kartik.myschool.model.AcademicYear;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Semester;

/**
 * Global session for Year → Semester → Class selection (Edu Report-style workflow).
 */
public final class SessionContext {

    public static volatile AcademicYear selectedYear;
    public static volatile Semester selectedSemester;
    public static volatile School selectedSchool;
    public static volatile ClassModel selectedClass;
    public static volatile com.kartik.myschool.model.Student currentStudentForMarks;
    public static volatile com.kartik.myschool.model.Student currentStudentForAttendance;
    public static volatile com.kartik.myschool.model.AttendanceRecord currentRecordForAttendance;

    private SessionContext() {}

    public static synchronized String getYearLabel() {
        return selectedYear != null && selectedYear.label != null
                ? selectedYear.label : "2026-27";
    }

    public static synchronized String getSemesterLabel() {
        return selectedSemester != null && selectedSemester.name != null
                ? selectedSemester.name : "First Semester";
    }

    public static synchronized String getClassDivLabel() {
        if (selectedClass == null) return "वर्ग निवडलेला नाही";
        String cls = (selectedClass.className != null && !selectedClass.className.trim().isEmpty()) ? selectedClass.className : "1";
        String div = (selectedClass.division != null && !selectedClass.division.trim().isEmpty()) ? selectedClass.division : "-";
        return "इयत्ता: " + cls + ", तुकडी: " + div;
    }

    public static synchronized void syncFromAppCache() {
        if (AppCache.selectedSchool != null) selectedSchool = AppCache.selectedSchool;
        if (AppCache.selectedClass != null) selectedClass = AppCache.selectedClass;
    }

    public static synchronized void syncToAppCache() {
        AppCache.selectedSchool = selectedSchool;
        AppCache.selectedClass = selectedClass;
        AppCache.cachedRemarkBank.clear();
    }

    /**
     * Saves session to SharedPreferences.
     *
     * Audit fix #5: JSON serialization of subjects (15+ subjects × 12 fields) was running
     * on the UI thread, causing 10–30ms jank. Now runs on a background thread.
     * SharedPreferences.apply() is always called on a background thread internally anyway,
     * so this is safe.
     */
    public static synchronized void save(android.content.Context ctx) {
        if (ctx == null) return;

        // Capture volatile fields under lock before handing to background thread
        final AcademicYear year = selectedYear;
        final Semester semester = selectedSemester;
        final School school = selectedSchool;
        final ClassModel cls = selectedClass;
        final String teacherName = com.kartik.myschool.AppCache.cachedTeacherName;

        // Audit fix #5: Run JSON serialization on a background thread to avoid UI jank
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            android.content.SharedPreferences.Editor editor = ctx
                    .getSharedPreferences("myschool_session_prefs", android.content.Context.MODE_PRIVATE).edit();

            // Teacher name
            if (teacherName != null) {
                editor.putString("teacher_name", teacherName);
            } else {
                editor.remove("teacher_name");
            }

            // Year
            if (year != null) {
                editor.putString("year_id", year.id);
                editor.putString("year_label", year.label);
                editor.putInt("year_start", year.startYear);
                editor.putInt("year_end", year.endYear);
                editor.putBoolean("year_active", year.active);
            } else {
                editor.remove("year_id");
            }

            // Semester
            if (semester != null) {
                editor.putString("semester_id", semester.id);
                editor.putString("semester_year_id", semester.yearId);
                editor.putInt("semester_number", semester.number);
                editor.putString("semester_name", semester.name);
                editor.putString("semester_subtitle", semester.subtitle);
            } else {
                editor.remove("semester_id");
            }

            // School
            if (school != null) {
                editor.putString("school_id", school.id);
                editor.putString("school_name", school.name);
                editor.putString("school_udise", school.udiseCode);
                editor.putString("school_board", school.board);
                editor.putString("school_address", school.address);
                editor.putString("school_principal", school.principalName);
            } else {
                editor.remove("school_id");
            }

            // Class
            if (cls != null) {
                editor.putString("class_id", cls.id);
                editor.putString("class_school_id", cls.schoolId);
                editor.putString("class_year_id", cls.yearId);
                editor.putString("class_year_label", cls.academicYearLabel);
                editor.putString("class_semester_id", cls.semesterId);
                editor.putString("class_name", cls.className);
                editor.putString("class_division", cls.division);
                editor.putString("class_exam_name", cls.examName);
                editor.putInt("class_year", cls.year);
                editor.putString("class_teacher", cls.teacherName);
                editor.putString("class_asst_teacher", cls.assistantTeacherName);
                editor.putString("class_teacher_email", cls.teacherEmail);
                editor.putString("class_teacher_phone", cls.teacherPhone);
                editor.putInt("class_student_count", cls.studentCount);

                // Serialize subjects (JSON — this is why we need the background thread)
                if (cls.subjects != null) {
                    try {
                        org.json.JSONArray arr = new org.json.JSONArray();
                        for (com.kartik.myschool.model.Subject s : cls.subjects) {
                            org.json.JSONObject obj = new org.json.JSONObject();
                            obj.put("name", s.name);
                            obj.put("maxMarks", s.maxMarks);
                            obj.put("maxNirikhshan", s.maxNirikhshan);
                            obj.put("maxTondiKam", s.maxTondiKam);
                            obj.put("maxPratyakshik", s.maxPratyakshik);
                            obj.put("maxUpkram", s.maxUpkram);
                            obj.put("maxPrakalp", s.maxPrakalp);
                            obj.put("maxChachani", s.maxChachani);
                            obj.put("maxSwadhyay", s.maxSwadhyay);
                            obj.put("maxItar", s.maxItar);
                            obj.put("maxTondi", s.maxTondi);
                            obj.put("maxPratyakshikB", s.maxPratyakshikB);
                            obj.put("maxLekhi", s.maxLekhi);
                            arr.put(obj);
                        }
                        editor.putString("class_subjects_json", arr.toString());
                    } catch (org.json.JSONException e) {
                        android.util.Log.e("SessionContext", "Error serializing subjects", e);
                    }
                } else {
                    editor.remove("class_subjects_json");
                }

                // Serialize monthlyWorkingDays
                if (cls.monthlyWorkingDays != null) {
                    try {
                        org.json.JSONObject mwd = new org.json.JSONObject();
                        for (java.util.Map.Entry<String, Integer> entry : cls.monthlyWorkingDays.entrySet()) {
                            mwd.put(entry.getKey(), entry.getValue());
                        }
                        editor.putString("class_monthly_working_days_json", mwd.toString());
                    } catch (org.json.JSONException e) {
                        android.util.Log.e("SessionContext", "Error serializing monthly working days", e);
                    }
                } else {
                    editor.remove("class_monthly_working_days_json");
                }
            } else {
                editor.remove("class_id");
                editor.remove("class_subjects_json");
                editor.remove("class_monthly_working_days_json");
            }

            editor.apply();
        });

        // syncToAppCache is quick (no I/O) — keep on calling thread
        syncToAppCache();
    }

    public static synchronized void load(android.content.Context ctx) {
        if (ctx == null) return;
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("myschool_session_prefs", android.content.Context.MODE_PRIVATE);
        
        // Teacher name
        AppCache.cachedTeacherName = prefs.getString("teacher_name", null);

        // Year
        String yearId = prefs.getString("year_id", null);
        if (yearId != null) {
            selectedYear = new AcademicYear();
            selectedYear.id = yearId;
            selectedYear.label = prefs.getString("year_label", "");
            selectedYear.startYear = prefs.getInt("year_start", 0);
            selectedYear.endYear = prefs.getInt("year_end", 0);
            selectedYear.active = prefs.getBoolean("year_active", false);
        }

        // Semester
        String semesterId = prefs.getString("semester_id", null);
        if (semesterId != null) {
            selectedSemester = new Semester();
            selectedSemester.id = semesterId;
            selectedSemester.yearId = prefs.getString("semester_year_id", "");
            selectedSemester.number = prefs.getInt("semester_number", 1);
            if (selectedSemester.number <= 0) selectedSemester.number = 1;
            selectedSemester.name = prefs.getString("semester_name", "");
            selectedSemester.subtitle = prefs.getString("semester_subtitle", "");
        }

        // School
        String schoolId = prefs.getString("school_id", null);
        if (schoolId != null) {
            selectedSchool = new School();
            selectedSchool.id = schoolId;
            selectedSchool.name = prefs.getString("school_name", "");
            selectedSchool.udiseCode = prefs.getString("school_udise", "");
            selectedSchool.board = prefs.getString("school_board", "");
            selectedSchool.address = prefs.getString("school_address", "");
            selectedSchool.principalName = prefs.getString("school_principal", "");
        }

        // Class
        String classId = prefs.getString("class_id", null);
        if (classId != null) {
            selectedClass = new ClassModel();
            selectedClass.id = classId;
            selectedClass.schoolId = prefs.getString("class_school_id", "");
            selectedClass.yearId = prefs.getString("class_year_id", "");
            selectedClass.academicYearLabel = prefs.getString("class_year_label", "");
            selectedClass.semesterId = prefs.getString("class_semester_id", "");
            selectedClass.className = prefs.getString("class_name", "");
            selectedClass.division = prefs.getString("class_division", "");
            selectedClass.examName = prefs.getString("class_exam_name", "");
            selectedClass.year = prefs.getInt("class_year", 0);
            selectedClass.teacherName = prefs.getString("class_teacher", "");
            selectedClass.assistantTeacherName = prefs.getString("class_asst_teacher", "");
            selectedClass.teacherEmail = prefs.getString("class_teacher_email", "");
            selectedClass.teacherPhone = prefs.getString("class_teacher_phone", "");
            selectedClass.studentCount = prefs.getInt("class_student_count", 0);

            // Deserialize subjects
            String subjectsJson = prefs.getString("class_subjects_json", null);
            if (subjectsJson != null) {
                try {
                    org.json.JSONArray arr = new org.json.JSONArray(subjectsJson);
                    selectedClass.subjects = new java.util.ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject obj = arr.getJSONObject(i);
                        com.kartik.myschool.model.Subject s = new com.kartik.myschool.model.Subject();
                        s.name = obj.optString("name", "");
                        s.maxMarks = obj.optInt("maxMarks", 100);
                        s.maxNirikhshan = obj.optInt("maxNirikhshan", 0);
                        s.maxTondiKam = obj.optInt("maxTondiKam", 0);
                        s.maxPratyakshik = obj.optInt("maxPratyakshik", 0);
                        s.maxUpkram = obj.optInt("maxUpkram", 0);
                        s.maxPrakalp = obj.optInt("maxPrakalp", 0);
                        s.maxChachani = obj.optInt("maxChachani", 0);
                        s.maxSwadhyay = obj.optInt("maxSwadhyay", 0);
                        s.maxItar = obj.optInt("maxItar", 0);
                        s.maxTondi = obj.optInt("maxTondi", 0);
                        s.maxPratyakshikB = obj.optInt("maxPratyakshikB", 0);
                        s.maxLekhi = obj.optInt("maxLekhi", 0);
                        selectedClass.subjects.add(s);
                    }
                    com.kartik.myschool.model.Subject.sortSubjects(selectedClass.subjects);
                } catch (org.json.JSONException e) {
                    android.util.Log.e("SessionContext", "Error deserializing subjects", e);
                }
            }

            // Deserialize monthlyWorkingDays
            String mwdJson = prefs.getString("class_monthly_working_days_json", null);
            if (mwdJson != null) {
                try {
                    org.json.JSONObject mwd = new org.json.JSONObject(mwdJson);
                    selectedClass.monthlyWorkingDays = new java.util.HashMap<>();
                    java.util.Iterator<String> keys = mwd.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        selectedClass.monthlyWorkingDays.put(key, mwd.getInt(key));
                    }
                } catch (org.json.JSONException e) {
                    android.util.Log.e("SessionContext", "Error deserializing monthly working days", e);
                }
            }
        }
        
        syncToAppCache();
    }

    public static synchronized void clear(android.content.Context ctx) {
        selectedYear = null;
        selectedSemester = null;
        selectedSchool = null;
        selectedClass = null;

        if (ctx != null) {
            ctx.getSharedPreferences("myschool_session_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().clear().apply();
        }

        AppCache.selectedSchool = null;
        AppCache.selectedClass = null;
        AppCache.selectedStudent = null;
        AppCache.selectedMarks = null;
        AppCache.cachedYears = null;
        AppCache.cachedSemesters = null;
        AppCache.cachedClasses = null;
        AppCache.cachedTeacherName = null;
        AppCache.cachedStudentCountByClassId = null;

        com.kartik.myschool.repository.FirebaseRepository.clearCache();
    }
}
