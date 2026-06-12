package com.kartik.myschool;

import com.kartik.myschool.model.AcademicYear;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Semester;

/**
 * Global session for Year → Semester → Class selection (Myschool-style workflow).
 */
public final class SessionContext {

    public static AcademicYear selectedYear;
    public static Semester selectedSemester;
    public static School selectedSchool;
    public static ClassModel selectedClass;
    public static com.kartik.myschool.model.Student currentStudentForMarks;
    public static com.kartik.myschool.model.Student currentStudentForAttendance;
    public static com.kartik.myschool.model.AttendanceRecord currentRecordForAttendance;

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

    public static void save(android.content.Context ctx) {
        if (ctx == null) return;
        android.content.SharedPreferences.Editor editor = ctx.getSharedPreferences("myschool_session_prefs", android.content.Context.MODE_PRIVATE).edit();
        
        // Teacher name
        if (AppCache.cachedTeacherName != null) {
            editor.putString("teacher_name", AppCache.cachedTeacherName);
        } else {
            editor.remove("teacher_name");
        }

        // Year
        if (selectedYear != null) {
            editor.putString("year_id", selectedYear.id);
            editor.putString("year_label", selectedYear.label);
            editor.putInt("year_start", selectedYear.startYear);
            editor.putInt("year_end", selectedYear.endYear);
            editor.putBoolean("year_active", selectedYear.active);
        } else {
            editor.remove("year_id");
        }

        // Semester
        if (selectedSemester != null) {
            editor.putString("semester_id", selectedSemester.id);
            editor.putString("semester_year_id", selectedSemester.yearId);
            editor.putInt("semester_number", selectedSemester.number);
            editor.putString("semester_name", selectedSemester.name);
            editor.putString("semester_subtitle", selectedSemester.subtitle);
        } else {
            editor.remove("semester_id");
        }

        // School
        if (selectedSchool != null) {
            editor.putString("school_id", selectedSchool.id);
            editor.putString("school_name", selectedSchool.name);
            editor.putString("school_udise", selectedSchool.udiseCode);
            editor.putString("school_board", selectedSchool.board);
            editor.putString("school_address", selectedSchool.address);
            editor.putString("school_principal", selectedSchool.principalName);
        } else {
            editor.remove("school_id");
        }

        // Class
        if (selectedClass != null) {
            editor.putString("class_id", selectedClass.id);
            editor.putString("class_school_id", selectedClass.schoolId);
            editor.putString("class_year_id", selectedClass.yearId);
            editor.putString("class_year_label", selectedClass.academicYearLabel);
            editor.putString("class_semester_id", selectedClass.semesterId);
            editor.putString("class_name", selectedClass.className);
            editor.putString("class_division", selectedClass.division);
            editor.putString("class_exam_name", selectedClass.examName);
            editor.putInt("class_year", selectedClass.year);
            editor.putString("class_teacher", selectedClass.teacherName);
            editor.putString("class_asst_teacher", selectedClass.assistantTeacherName);
            editor.putString("class_teacher_email", selectedClass.teacherEmail);
            editor.putString("class_teacher_phone", selectedClass.teacherPhone);
            editor.putInt("class_student_count", selectedClass.studentCount);
            
            // Serialize subjects
            if (selectedClass.subjects != null) {
                try {
                    org.json.JSONArray arr = new org.json.JSONArray();
                    for (com.kartik.myschool.model.Subject s : selectedClass.subjects) {
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
            if (selectedClass.monthlyWorkingDays != null) {
                try {
                    org.json.JSONObject mwd = new org.json.JSONObject();
                    for (java.util.Map.Entry<String, Integer> entry : selectedClass.monthlyWorkingDays.entrySet()) {
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
        syncToAppCache();
    }

    public static void load(android.content.Context ctx) {
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

    public static void clear(android.content.Context ctx) {
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
