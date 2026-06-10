package com.kartik.myschool.repository;

import com.kartik.myschool.model.AcademicYear;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Semester;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Teacher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirebaseRepository {

    private static volatile FirebaseRepository instance;

    // Smart In-Memory Caching to make UI transitions completely instant (0ms latency)
    private static Teacher cachedTeacher;
    private static List<AcademicYear> cachedYears;
    private static final java.util.Map<String, List<Semester>> cachedSemestersMap = new java.util.HashMap<>();
    private static final java.util.Map<String, List<ClassModel>> cachedClassesForYearMap = new java.util.HashMap<>();
    private static final java.util.Map<String, List<ClassModel>> cachedClassesForSchoolMap = new java.util.HashMap<>();
    private static List<Student> cachedStudentsForTeacher;
    private static final java.util.Map<String, List<Student>> cachedStudentsForClassMap = new java.util.HashMap<>();
    private static final java.util.Map<String, java.util.Map<String, MarksRecord>> cachedClassSemesterMarksMap = new java.util.HashMap<>();

    public static void clearCache() {
        synchronized (FirebaseRepository.class) {
            cachedTeacher = null;
            cachedYears = null;
            cachedSemestersMap.clear();
            cachedClassesForYearMap.clear();
            cachedClassesForSchoolMap.clear();
            cachedStudentsForTeacher = null;
            cachedStudentsForClassMap.clear();
            cachedClassSemesterMarksMap.clear();
        }
    }

    /** Clears only the marks cache, leaving student/class/year caches intact. */
    public void clearMarksCache() {
        synchronized (FirebaseRepository.class) {
            cachedClassSemesterMarksMap.clear();
        }
    }

    /** Directly updates/patches the marks cache for the specified class, semester, and student. */
    public void updateMarksInCache(String classId, String semesterId, String studentId, MarksRecord record) {
        if (classId == null || semesterId == null || studentId == null || record == null) return;
        synchronized (FirebaseRepository.class) {
            String cacheKey = classId + "_" + semesterId;
            java.util.Map<String, MarksRecord> marksMap = cachedClassSemesterMarksMap.get(cacheKey);
            if (marksMap == null) {
                Log.d("FIRESTORE_MARKS", "Skipped creating partial marks cache for class=" + classId + " sem=" + semesterId + " student=" + studentId);
                return;
            }
            marksMap.put(studentId, record);
            Log.d("FIRESTORE_MARKS", "Directly updated internal memory cache for class=" + classId + " sem=" + semesterId + " student=" + studentId);
        }
    }

    /** Call this when switching Firebase projects to force re-initialization. */
    public static void resetInstance() {
        synchronized (FirebaseRepository.class) {
            clearCache();
            instance = null; // forces constructor to run again on next get()
        }
    }
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public static final String COL_TEACHERS = "teachers";
    public static final String COL_SCHOOLS = "schools";
    public static final String COL_ACADEMIC_YEARS = "academic_years";
    public static final String COL_SEMESTERS = "semesters";
    public static final String COL_CLASSES = "classes";
    public static final String COL_STUDENTS = "students";
    public static final String COL_MARKS = "marks";
    public static final String COL_ATTENDANCE_RECORDS = "attendance_records";
    public static final String COL_SUBSCRIPTIONS = "subscriptions";

    private FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Diagnostic: confirm which Firebase project this instance is connected to
        try {
            com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance();
            Log.d("FIREBASE_INIT", "=====================================");
            Log.d("FIREBASE_INIT", "FirebaseApp name     : " + app.getName());
            Log.d("FIREBASE_INIT", "Project ID           : " + app.getOptions().getProjectId());
            Log.d("FIREBASE_INIT", "Application ID       : " + app.getOptions().getApplicationId());
            Log.d("FIREBASE_INIT", "Storage bucket       : " + app.getOptions().getStorageBucket());
            Log.d("FIREBASE_INIT", "Auth domain          : " + app.getOptions().getDatabaseUrl());
            Log.d("FIREBASE_INIT", "Current Auth user    : " + (auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "none"));
            Log.d("FIREBASE_INIT", "=====================================");
        } catch (Exception e) {
            Log.e("FIREBASE_INIT", "Could not read FirebaseApp options", e);
        }
    }

    // Bug #6 fix: thread-safe singleton with double-checked locking
    public static FirebaseRepository get() {
        if (instance == null) {
            synchronized (FirebaseRepository.class) {
                if (instance == null) instance = new FirebaseRepository();
            }
        }
        return instance;
    }

    public String currentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // ---------- Teacher ----------
    public void saveTeacher(Teacher t, OnResult<Void> cb) {
        String uid = currentUid();
        if (uid == null) {
            Log.e("FIRESTORE", "saveTeacher: uid is null — user not authenticated!");
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        t.id = uid;
        Log.d("FIRESTORE", "saveTeacher: saving uid=" + uid + " email=" + t.email);
        db.collection(COL_TEACHERS).document(t.id).set(t)
                .addOnSuccessListener(v -> {
                    Log.d("FIRESTORE", "saveTeacher: SUCCESS for uid=" + uid);
                    cachedTeacher = t;
                    cb.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "saveTeacher: FAILED for uid=" + uid, e);
                    cb.onError(e);
                });
    }

    public void getTeacher(OnResult<Teacher> cb) {
        if (cachedTeacher != null) {
            cb.onSuccess(cachedTeacher);
            return;
        }
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        db.collection(COL_TEACHERS).document(uid).get()
                .addOnSuccessListener(snap -> {
                    Teacher t = snap != null ? snap.toObject(Teacher.class) : null;
                    cachedTeacher = t;
                    cb.onSuccess(t);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getTeacherFresh(OnResult<Teacher> cb) {
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        db.collection(COL_TEACHERS).document(uid).get()
                .addOnSuccessListener(snap -> {
                    Teacher t = snap != null ? snap.toObject(Teacher.class) : null;
                    cachedTeacher = t;
                    cb.onSuccess(t);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getAppConfig(OnResult<com.kartik.myschool.model.AppConfig> cb) {
        db.collection("admin_settings").document("app_config").get()
                .addOnSuccessListener(snap -> {
                    com.kartik.myschool.model.AppConfig config = snap != null ? snap.toObject(com.kartik.myschool.model.AppConfig.class) : null;
                    if (config == null) {
                        config = new com.kartik.myschool.model.AppConfig();
                    }
                    cb.onSuccess(config);
                })
                .addOnFailureListener(e -> {
                    cb.onSuccess(new com.kartik.myschool.model.AppConfig());
                });
    }

    // ---------- School ----------
    public void saveSchool(School s, OnResult<String> cb) {
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        s.teacherId = uid;
        DocumentReference ref = s.id != null
                ? db.collection(COL_SCHOOLS).document(s.id)
                : db.collection(COL_SCHOOLS).document();
        s.id = ref.getId();
        ref.set(s)
                .addOnSuccessListener(v -> cb.onSuccess(s.id))
                .addOnFailureListener(cb::onError);
    }

    public void getSchools(OnResult<List<School>> cb) {
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        db.collection(COL_SCHOOLS)
                .whereEqualTo("teacherId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<School> schools = snap != null ? snap.toObjects(School.class) : new ArrayList<>();
                    Collections.sort(schools, (a, b) -> {
                        if (a.name == null) return -1;
                        if (b.name == null) return 1;
                        return a.name.compareTo(b.name);
                    });
                    cb.onSuccess(schools);
                })
                .addOnFailureListener(cb::onError);
    }

    /** Single school per teacher — created from profile UDISE (no manual school registration). */
    public void ensureTeacherSchool(Teacher teacher, OnResult<School> cb) {
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        getSchools(new OnResult<List<School>>() {
            @Override public void onSuccess(List<School> schools) {
                if (schools != null && !schools.isEmpty()) {
                    cb.onSuccess(schools.get(0));
                    return;
                }
                School s = new School();
                s.teacherId = uid;
                s.name = teacher != null && teacher.schoolName != null && !teacher.schoolName.isEmpty()
                        ? teacher.schoolName : "My School";
                s.udiseCode = teacher != null ? teacher.udiseCode : "";
                s.board = "State";
                saveSchool(s, new OnResult<String>() {
                    @Override public void onSuccess(String id) {
                        s.id = id;
                        cb.onSuccess(s);
                    }
                    @Override public void onError(Exception e) { cb.onError(e); }
                });
            }
            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }

    public void getStudent(String studentId, OnResult<Student> cb) {
        if (studentId == null) {
            cb.onError(new IllegalArgumentException("Student ID cannot be null"));
            return;
        }
        db.collection(COL_STUDENTS).document(studentId).get()
                .addOnSuccessListener(snap -> cb.onSuccess(snap != null ? snap.toObject(Student.class) : null))
                .addOnFailureListener(cb::onError);
    }

    // ---------- Academic Year ----------
    public void saveAcademicYear(AcademicYear y, OnResult<String> cb) {
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        y.teacherId = uid;
        DocumentReference ref = y.id != null
                ? db.collection(COL_ACADEMIC_YEARS).document(y.id)
                : db.collection(COL_ACADEMIC_YEARS).document();
        y.id = ref.getId();
        ref.set(y)
                .addOnSuccessListener(v -> {
                    cachedYears = null; // Invalidate
                    cb.onSuccess(y.id);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getAcademicYears(OnResult<List<AcademicYear>> cb) {
        if (cachedYears != null) {
            cb.onSuccess(new ArrayList<>(cachedYears));
            return;
        }
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        db.collection(COL_ACADEMIC_YEARS)
                .whereEqualTo("teacherId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<AcademicYear> years = snap != null ? snap.toObjects(AcademicYear.class) : new ArrayList<>();
                    Collections.sort(years, (a, b) -> Integer.compare(b.startYear, a.startYear));
                    cachedYears = years;
                    cb.onSuccess(new ArrayList<>(years));
                })
                .addOnFailureListener(cb::onError);
    }

    // ---------- Semester ----------
    public void saveSemester(Semester s, OnResult<String> cb) {
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        s.teacherId = uid;
        DocumentReference ref = s.id != null
                ? db.collection(COL_SEMESTERS).document(s.id)
                : db.collection(COL_SEMESTERS).document();
        s.id = ref.getId();
        ref.set(s)
                .addOnSuccessListener(v -> {
                    cachedSemestersMap.clear(); // Invalidate
                    cb.onSuccess(s.id);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getSemestersForYear(String yearId, OnResult<List<Semester>> cb) {
        if (yearId == null) {
            cb.onError(new IllegalArgumentException("Year ID cannot be null"));
            return;
        }
        if (cachedSemestersMap.containsKey(yearId)) {
            cb.onSuccess(new ArrayList<>(cachedSemestersMap.get(yearId)));
            return;
        }
        db.collection(COL_SEMESTERS)
                .whereEqualTo("yearId", yearId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Semester> list = new ArrayList<>();
                    if (snap != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            Semester sem = doc.toObject(Semester.class);
                            if (sem != null) {
                                if (sem.id == null || sem.id.isEmpty()) {
                                    sem.id = doc.getId();
                                }
                                list.add(sem);
                            }
                        }
                    }
                    Collections.sort(list, (a, b) -> Integer.compare(a.number, b.number));
                    cachedSemestersMap.put(yearId, list);
                    cb.onSuccess(new ArrayList<>(list));
                })
                .addOnFailureListener(cb::onError);
    }

    /** Seed default year + semesters when teacher has none. */
    public void ensureDefaultYearAndSemesters(OnResult<AcademicYear> cb) {
        getAcademicYears(new OnResult<List<AcademicYear>>() {
            @Override
            public void onSuccess(List<AcademicYear> years) {
                if (years != null && !years.isEmpty()) {
                    cb.onSuccess(years.get(0));
                    return;
                }
                AcademicYear y = new AcademicYear("2026-27", 2026, 2027);
                saveAcademicYear(y, new OnResult<String>() {
                    @Override
                    public void onSuccess(String yearId) {
                        seedSemesters(yearId, () -> cb.onSuccess(y));
                    }
                    @Override
                    public void onError(Exception e) { cb.onError(e); }
                });
            }
            @Override
            public void onError(Exception e) { cb.onError(e); }
        });
    }

    private void seedSemesters(String yearId, Runnable done) {
        Semester s1 = new Semester(1, "First Semester", "Easy Reports");
        s1.yearId = yearId;
        Semester s2 = new Semester(2, "Second Semester", "Final Reports");
        s2.yearId = yearId;
        saveSemester(s1, new OnResult<String>() {
            @Override public void onSuccess(String id) {
                s1.id = id;
                saveSemester(s2, new OnResult<String>() {
                    @Override public void onSuccess(String id2) {
                        s2.id = id2;
                        // Also seed default classes, subjects and students under the first semester!
                        seedDefaultClassesAndSubjects(yearId, id, done);
                    }
                    @Override public void onError(Exception e) { done.run(); }
                });
            }
            @Override public void onError(Exception e) { done.run(); }
        });
    }

    private void seedDefaultClassesAndSubjects(String yearId, String semesterId, Runnable done) {
        String uid = currentUid();
        if (uid == null) {
            done.run();
            return;
        }
        String schoolId = com.kartik.myschool.SessionContext.selectedSchool != null ? com.kartik.myschool.SessionContext.selectedSchool.id : null;
        if (schoolId == null) {
            getSchools(new OnResult<List<School>>() {
                @Override public void onSuccess(List<School> schools) {
                    if (schools != null && !schools.isEmpty()) {
                        createClasses(schools.get(0).id, yearId, semesterId, done);
                    } else {
                        done.run();
                    }
                }
                @Override public void onError(Exception e) { done.run(); }
            });
        } else {
            createClasses(schoolId, yearId, semesterId, done);
        }
    }

    private void createClasses(String schoolId, String yearId, String semesterId, Runnable done) {
        ClassModel c1 = new ClassModel();
        c1.schoolId = schoolId;
        c1.yearId = yearId;
        c1.semesterId = semesterId;
        c1.className = "1";
        c1.division = "A";
        c1.academicYearLabel = "2026-27";
        c1.studentCount = 2;
        // No default subjects — teacher selects from the Subjects page

        ClassModel c2 = new ClassModel();
        c2.schoolId = schoolId;
        c2.yearId = yearId;
        c2.semesterId = semesterId;
        c2.className = "2";
        c2.division = "A";
        c2.academicYearLabel = "2026-27";
        c2.studentCount = 2;
        // No default subjects — teacher selects from the Subjects page

        saveClass(c1, new OnResult<String>() {
            @Override public void onSuccess(String classId1) {
                seedDefaultStudents(classId1, schoolId, "1", "A", () -> {
                    saveClass(c2, new OnResult<String>() {
                        @Override public void onSuccess(String classId2) {
                            seedDefaultStudents(classId2, schoolId, "2", "A", done);
                        }
                        @Override public void onError(Exception e) { done.run(); }
                    });
                });
            }
            @Override public void onError(Exception e) { done.run(); }
        });
    }

    private void seedDefaultStudents(String classId, String schoolId, String className, String division, Runnable done) {
        String uid = currentUid();
        Student s1 = new Student();
        s1.classId = classId;
        s1.schoolId = schoolId;
        s1.teacherId = uid;
        s1.name = "Kartik Thorat";
        s1.rollNo = "1";
        s1.registrationNo = "101" + className + "01";
        s1.dob = "25/05/2012";
        s1.gender = "Male";
        s1.className = className;
        s1.standard = className;
        s1.division = division;
        s1.schoolName = "My School";

        s1.monthlyAttendance.put("जून", "18/20");
        s1.monthlyAttendance.put("जुलै", "20/22");
        s1.monthlyAttendance.put("ऑगस्ट", "19/21");
        s1.monthlyAttendance.put("सप्टें", "21/23");
        s1.monthlyAttendance.put("ऑक्टो", "15/17");
        s1.monthlyAttendance.put("नोव्हे", "16/18");
        s1.monthlyAttendance.put("डिसें", "17/19");
        s1.monthlyAttendance.put("जाने", "18/20");
        s1.monthlyAttendance.put("फेब्रु", "19/21");
        s1.monthlyAttendance.put("मार्च", "20/22");
        s1.monthlyAttendance.put("एप्रिल", "21/23");
        s1.monthlyAttendance.put("मे", "0/0");

        Student s2 = new Student();
        s2.classId = classId;
        s2.schoolId = schoolId;
        s2.teacherId = uid;
        s2.name = "Priya Patil";
        s2.rollNo = "2";
        s2.registrationNo = "101" + className + "02";
        s2.dob = "12/08/2012";
        s2.gender = "Female";
        s2.className = className;
        s2.standard = className;
        s2.division = division;
        s2.schoolName = "My School";

        s2.monthlyAttendance.put("जून", "17/20");
        s2.monthlyAttendance.put("जुलै", "19/22");
        s2.monthlyAttendance.put("ऑगस्ट", "20/21");
        s2.monthlyAttendance.put("सप्टें", "20/23");
        s2.monthlyAttendance.put("ऑक्टो", "14/17");
        s2.monthlyAttendance.put("नोव्हे", "15/18");
        s2.monthlyAttendance.put("डिसें", "18/19");
        s2.monthlyAttendance.put("जाने", "19/20");
        s2.monthlyAttendance.put("फेब्रु", "18/21");
        s2.monthlyAttendance.put("मार्च", "21/22");
        s2.monthlyAttendance.put("एप्रिल", "22/23");
        s2.monthlyAttendance.put("मे", "0/0");

        saveStudent(s1, new OnResult<String>() {
            @Override public void onSuccess(String id) {
                saveStudent(s2, new OnResult<String>() {
                    @Override public void onSuccess(String id2) { done.run(); }
                    @Override public void onError(Exception e) { done.run(); }
                });
            }
            @Override public void onError(Exception e) { done.run(); }
        });
    }

    public void getClassesForYear(String yearId, OnResult<List<ClassModel>> cb) {
        if (yearId == null) {
            cb.onError(new IllegalArgumentException("Year ID cannot be null"));
            return;
        }
        if (cachedClassesForYearMap.containsKey(yearId)) {
            cb.onSuccess(new ArrayList<>(cachedClassesForYearMap.get(yearId)));
            return;
        }
        db.collection(COL_CLASSES)
                .whereEqualTo("yearId", yearId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ClassModel> classes = snap != null ? snap.toObjects(ClassModel.class) : new ArrayList<>();
                    Collections.sort(classes, (a, b) -> {
                        try {
                            int ca = Integer.parseInt(a.className != null ? a.className : "0");
                            int cb2 = Integer.parseInt(b.className != null ? b.className : "0");
                            return Integer.compare(ca, cb2);
                        } catch (NumberFormatException e) {
                            String na = a.className != null ? a.className : "";
                            String nb = b.className != null ? b.className : "";
                            return na.compareTo(nb);
                        }
                    });
                    cachedClassesForYearMap.put(yearId, classes);
                    cb.onSuccess(new ArrayList<>(classes));
                })
                .addOnFailureListener(cb::onError);
    }

    // ---------- Class ----------
    public void saveClass(ClassModel c, OnResult<String> cb) {
        String uid = currentUid();
        if (uid != null) {
            c.teacherId = uid;
        }
        DocumentReference ref = c.id != null
                ? db.collection(COL_CLASSES).document(c.id)
                : db.collection(COL_CLASSES).document();
        c.id = ref.getId();
        ref.set(c)
                .addOnSuccessListener(v -> {
                    cachedClassesForYearMap.clear(); // Invalidate
                    cachedClassesForSchoolMap.clear(); // Invalidate
                    com.kartik.myschool.AppCache.cachedClasses = null; // Clear static AppCache
                    cb.onSuccess(c.id);
                })
                .addOnFailureListener(cb::onError);
    }

    public void deleteClass(String classId, OnResult<Void> cb) {
        if (classId == null) {
            cb.onError(new IllegalArgumentException("Class ID cannot be null"));
            return;
        }
        db.collection(COL_CLASSES).document(classId).delete()
                .addOnSuccessListener(v -> {
                    cachedClassesForYearMap.clear(); // Invalidate
                    cachedClassesForSchoolMap.clear(); // Invalidate
                    com.kartik.myschool.AppCache.cachedClasses = null; // Clear static AppCache
                    cb.onSuccess(null);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getClassesForSchool(String schoolId, OnResult<List<ClassModel>> cb) {
        if (schoolId == null) {
            cb.onError(new IllegalArgumentException("School ID cannot be null"));
            return;
        }
        if (cachedClassesForSchoolMap.containsKey(schoolId)) {
            cb.onSuccess(new ArrayList<>(cachedClassesForSchoolMap.get(schoolId)));
            return;
        }
        db.collection(COL_CLASSES)
                .whereEqualTo("schoolId", schoolId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ClassModel> classes = snap != null ? snap.toObjects(ClassModel.class) : new ArrayList<>();
                    Collections.sort(classes, (a, b) -> {
                        String nameA = a.className != null ? a.className : "";
                        String nameB = b.className != null ? b.className : "";
                        return nameA.compareTo(nameB);
                    });
                    cachedClassesForSchoolMap.put(schoolId, classes);
                    cb.onSuccess(new ArrayList<>(classes));
                })
                .addOnFailureListener(cb::onError);
    }

    // ---------- Student ----------
    public void saveStudent(Student s, OnResult<String> cb) {
        String uid = currentUid();
        if (uid != null) {
            s.teacherId = uid;
        }
        DocumentReference ref = s.id != null
                ? db.collection(COL_STUDENTS).document(s.id)
                : db.collection(COL_STUDENTS).document();
        s.id = ref.getId();
        ref.set(s)
                .addOnSuccessListener(v -> {
                    cachedStudentsForTeacher = null; // Invalidate
                    if (s.classId != null) {
                        cachedStudentsForClassMap.remove(s.classId); // Invalidate
                    }
                    cb.onSuccess(s.id);
                })
                .addOnFailureListener(cb::onError);
    }

    public void deleteStudent(String studentId, OnResult<Void> cb) {
        if (studentId == null) {
            cb.onError(new IllegalArgumentException("Student ID cannot be null"));
            return;
        }
        db.collection(COL_STUDENTS).document(studentId).delete()
                .addOnSuccessListener(v -> {
                    cachedStudentsForTeacher = null; // Invalidate
                    cachedStudentsForClassMap.clear(); // Invalidate
                    cb.onSuccess(null);
                })
                .addOnFailureListener(cb::onError);
    }

    // Bug #3 fix: removed .orderBy("rollNo") to avoid requiring a composite Firestore index.
    // Sorting is now done in memory, consistent with other methods.
    public void getStudentsForClass(String classId, OnResult<List<Student>> cb) {
        if (classId == null) {
            cb.onError(new IllegalArgumentException("Class ID cannot be null"));
            return;
        }
        if (cachedStudentsForClassMap.containsKey(classId)) {
            cb.onSuccess(new ArrayList<>(cachedStudentsForClassMap.get(classId)));
            return;
        }
        db.collection(COL_STUDENTS)
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(snap -> {
                    // Bug #6 fix: return empty list instead of null
                    List<Student> students = snap != null ? snap.toObjects(Student.class) : new ArrayList<>();
                    // In-memory sort by rollNo (avoids composite index requirement)
                    Collections.sort(students, (a, b) -> {
                        String ra = a.rollNo != null ? a.rollNo : "";
                        String rb = b.rollNo != null ? b.rollNo : "";
                        // Numeric sort so roll 2 < 10 (not string "10" < "2")
                        try {
                            return Integer.compare(Integer.parseInt(ra), Integer.parseInt(rb));
                        } catch (NumberFormatException e) {
                            return ra.compareTo(rb);
                        }
                    });
                    cachedStudentsForClassMap.put(classId, students);
                    cb.onSuccess(new ArrayList<>(students));
                })
                .addOnFailureListener(cb::onError);
    }

    public void getStudentsForSchool(String schoolId, OnResult<List<Student>> cb) {
        if (schoolId == null) {
            cb.onError(new IllegalArgumentException("School ID cannot be null"));
            return;
        }
        db.collection(COL_STUDENTS)
                .whereEqualTo("schoolId", schoolId)
                .get()
                // Bug #6 fix: return empty list instead of null
                .addOnSuccessListener(snap -> cb.onSuccess(snap != null ? snap.toObjects(Student.class) : new ArrayList<>()))
                .addOnFailureListener(cb::onError);
    }

    public void getAllStudentsForTeacher(OnResult<List<Student>> cb) {
        if (cachedStudentsForTeacher != null) {
            cb.onSuccess(new ArrayList<>(cachedStudentsForTeacher));
            return;
        }
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        db.collection(COL_STUDENTS)
                .whereEqualTo("teacherId", uid)
                .get()
                // Bug #6 fix: return empty list instead of null
                .addOnSuccessListener(snap -> {
                    List<Student> students = snap != null ? snap.toObjects(Student.class) : new ArrayList<>();
                    cachedStudentsForTeacher = students;
                    cb.onSuccess(new ArrayList<>(students));
                })
                .addOnFailureListener(cb::onError);
    }

    // ---------- Marks ----------
    public void saveMarks(MarksRecord m, OnResult<String> cb) {
        String uid = currentUid();
        if (uid == null) {
            Log.e("FIRESTORE_MARKS", "saveMarks ABORTED: currentUid() is null — user not signed in.");
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        m.editedBy = uid;
        m.teacherId = uid; // Complies with Firestore security rules
        m.updatedAt = System.currentTimeMillis();
        DocumentReference ref = m.id != null
                ? db.collection(COL_MARKS).document(m.id)
                : db.collection(COL_MARKS).document();
        m.id = ref.getId();

        Log.d("FIRESTORE_MARKS", "Saving marks doc: " + ref.getPath());
        Log.d("FIRESTORE_MARKS", "  teacherId  = " + m.teacherId);
        Log.d("FIRESTORE_MARKS", "  studentId  = " + m.studentId);
        Log.d("FIRESTORE_MARKS", "  classId    = " + m.classId);
        Log.d("FIRESTORE_MARKS", "  semesterId = " + m.semesterId);
        Log.d("FIRESTORE_MARKS", "  subjects   = " + m.detailedMarks.size());

        ref.set(m)
                .addOnSuccessListener(v -> {
                    Log.d("FIRESTORE_MARKS", "SUCCESS: doc written at " + ref.getPath());
                    cachedClassSemesterMarksMap.clear();
                    cb.onSuccess(m.id);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_MARKS", "FAILURE writing marks: " + e.getMessage(), e);
                    cb.onError(e);
                });
    }

    public void getMarksForStudent(String studentId, String classId, OnResult<MarksRecord> cb) {
        if (studentId == null || classId == null) {
            cb.onError(new IllegalArgumentException("Student ID and Class ID cannot be null"));
            return;
        }
        db.collection(COL_MARKS)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("classId", classId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        cb.onSuccess(snap.getDocuments().get(0).toObject(MarksRecord.class));
                    } else {
                        cb.onSuccess(null);
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    public void getMarksForStudentAndSemester(String studentId,
    String classId, String semesterId, OnResult<MarksRecord> cb) {
        if (studentId == null || classId == null || semesterId == null) {
            cb.onError(new IllegalArgumentException("Arguments cannot be null"));
            return;
        }
        // Query by studentId + classId (no composite index needed via merge), then filter semesterId in memory.
        db.collection(COL_MARKS)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        int targetSemNum = 1;
                        boolean foundSem = false;
                        if (com.kartik.myschool.AppCache.cachedSemesters != null) {
                            for (com.kartik.myschool.model.Semester sem : com.kartik.myschool.AppCache.cachedSemesters) {
                                if (java.util.Objects.equals(semesterId, sem.id)) {
                                    targetSemNum = sem.number;
                                    foundSem = true;
                                    break;
                                }
                            }
                        }
                        if (!foundSem) {
                            if (com.kartik.myschool.SessionContext.selectedSemester != null 
                                    && java.util.Objects.equals(semesterId, com.kartik.myschool.SessionContext.selectedSemester.id)) {
                                targetSemNum = com.kartik.myschool.SessionContext.selectedSemester.number;
                            } else if (semesterId.equals("sem_2") || semesterId.toLowerCase().contains("second") || (semesterId.length() < 10 && semesterId.contains("2"))) {
                                targetSemNum = 2;
                            } else if (semesterId.equals("sem_1") || semesterId.toLowerCase().contains("first") || (semesterId.length() < 10 && semesterId.contains("1"))) {
                                targetSemNum = 1;
                            } else if (com.kartik.myschool.SessionContext.selectedSemester != null) {
                                if (java.util.Objects.equals(semesterId, com.kartik.myschool.SessionContext.selectedSemester.id)) {
                                    targetSemNum = com.kartik.myschool.SessionContext.selectedSemester.number;
                                }
                            }
                        }
                        final int finalTargetSemNum = targetSemNum;

                        List<MarksRecord> matches = new ArrayList<>();
                        List<MarksRecord> fallbacks = new ArrayList<>();

                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            MarksRecord m = doc.toObject(MarksRecord.class);
                            if (m == null) continue;
                            if (m.id == null || m.id.isEmpty()) {
                                m.id = doc.getId();
                            }
                            
                            int recordSemNum = 1;
                            boolean foundRecordSem = false;
                            if (com.kartik.myschool.AppCache.cachedSemesters != null && m.semesterId != null) {
                                for (com.kartik.myschool.model.Semester sem : com.kartik.myschool.AppCache.cachedSemesters) {
                                    if (sem.id != null && sem.id.equals(m.semesterId)) {
                                        recordSemNum = sem.number;
                                        foundRecordSem = true;
                                        break;
                                    }
                                }
                            }
                            if (!foundRecordSem) {
                                if (m.semesterNumber != null && !m.semesterNumber.isEmpty()) {
                                    try {
                                        recordSemNum = Integer.parseInt(m.semesterNumber);
                                    } catch (NumberFormatException ignored) {}
                                } else if (m.semesterId != null && !m.semesterId.isEmpty() && m.semesterId.length() < 10) {
                                    if (m.semesterId.contains("2") || m.semesterId.toLowerCase().contains("second")) {
                                        recordSemNum = 2;
                                    }
                                }
                            }
                            
                            if (semesterId.equals(m.semesterId) || finalTargetSemNum == recordSemNum) {
                                matches.add(m);
                            } else if (m.semesterId == null || m.semesterId.isEmpty()) {
                                // Only fallback to empty/legacy records if we are looking for Semester 1
                                if (finalTargetSemNum == 1) {
                                    fallbacks.add(m);
                                }
                            }
                        }

                        MarksRecord result = null;
                        if (!matches.isEmpty()) {
                            Collections.sort(matches, (a, b) -> Long.compare(a.updatedAt, b.updatedAt));
                            result = matches.get(matches.size() - 1);
                            for (int i = 0; i < matches.size() - 1; i++) {
                                mergeRecords(result, matches.get(i));
                            }
                            Log.d("FIRESTORE_MARKS", "getMarksForStudentAndSemester: found " + matches.size() + " matches, merged into result id=" + result.id);
                        } else if (!fallbacks.isEmpty()) {
                            Collections.sort(fallbacks, (a, b) -> Long.compare(a.updatedAt, b.updatedAt));
                            result = fallbacks.get(fallbacks.size() - 1);
                            for (int i = 0; i < fallbacks.size() - 1; i++) {
                                mergeRecords(result, fallbacks.get(i));
                            }
                            Log.d("FIRESTORE_MARKS", "getMarksForStudentAndSemester: no matches, found " + fallbacks.size() + " fallbacks, merged into result id=" + result.id);
                        }

                        cb.onSuccess(result);
                    } else {
                        Log.d("FIRESTORE_MARKS", "getMarksForStudentAndSemester: no docs found for student=" + studentId + " class=" + classId);
                        cb.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_MARKS", "getMarksForStudentAndSemester FAILED: " + e.getMessage(), e);
                    cb.onError(e);
                });
    }

    public void getMarksForClass(String classId, OnResult<List<MarksRecord>> cb) {
        if (classId == null) {
            cb.onError(new IllegalArgumentException("Class ID cannot be null"));
            return;
        }
        db.collection(COL_MARKS)
                .whereEqualTo("classId", classId)
                .get()
                // Bug #6 fix: return empty list instead of null
                .addOnSuccessListener(snap -> cb.onSuccess(snap != null ? snap.toObjects(MarksRecord.class) : new ArrayList<>()))
                .addOnFailureListener(cb::onError);
    }

    public void getMarksForClassAndSemester(String classId, String semesterId, OnResult<java.util.Map<String, MarksRecord>> cb) {
        if (classId == null || semesterId == null) {
            cb.onError(new IllegalArgumentException("Class ID and Semester ID cannot be null"));
            return;
        }
        String cacheKey = classId + "_" + semesterId;
        if (cachedClassSemesterMarksMap.containsKey(cacheKey)) {
            Log.d("FIRESTORE_MARKS", "getMarksForClassAndSemester: cache hit key=" + cacheKey
                    + " size=" + cachedClassSemesterMarksMap.get(cacheKey).size());
            cb.onSuccess(new java.util.HashMap<>(cachedClassSemesterMarksMap.get(cacheKey)));
            return;
        }
        Log.d("FIRESTORE_MARKS", "getMarksForClassAndSemester: querying Firestore classId=" + classId + " semesterId=" + semesterId);
        db.collection(COL_MARKS)
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(snap -> {
                    java.util.Map<String, MarksRecord> marksMap = new java.util.HashMap<>();
                    java.util.Map<String, MarksRecord> fallbackMap = new java.util.HashMap<>();
                    int totalDocs = snap != null ? snap.size() : 0;
                    Log.d("FIRESTORE_MARKS", "getMarksForClassAndSemester: got " + totalDocs + " docs for classId=" + classId);
                    
                    int targetSemNum = 1;
                    boolean foundSem = false;
                    if (com.kartik.myschool.AppCache.cachedSemesters != null) {
                        for (com.kartik.myschool.model.Semester sem : com.kartik.myschool.AppCache.cachedSemesters) {
                            if (java.util.Objects.equals(semesterId, sem.id)) {
                                targetSemNum = sem.number;
                                foundSem = true;
                                break;
                            }
                        }
                    }
                    if (!foundSem) {
                        if (com.kartik.myschool.SessionContext.selectedSemester != null 
                                && java.util.Objects.equals(semesterId, com.kartik.myschool.SessionContext.selectedSemester.id)) {
                            targetSemNum = com.kartik.myschool.SessionContext.selectedSemester.number;
                        } else if (semesterId.equals("sem_2") || semesterId.toLowerCase().contains("second") || (semesterId.length() < 10 && semesterId.contains("2"))) {
                            targetSemNum = 2;
                        } else if (semesterId.equals("sem_1") || semesterId.toLowerCase().contains("first") || (semesterId.length() < 10 && semesterId.contains("1"))) {
                            targetSemNum = 1;
                        } else if (com.kartik.myschool.SessionContext.selectedSemester != null) {
                            if (java.util.Objects.equals(semesterId, com.kartik.myschool.SessionContext.selectedSemester.id)) {
                                targetSemNum = com.kartik.myschool.SessionContext.selectedSemester.number;
                            }
                        }
                    }
                    final int finalTargetSemNum = targetSemNum;
                    Log.d("FIRESTORE_MARKS", "getMarksForClassAndSemester: active target semester number is " + finalTargetSemNum);

                    if (snap != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            MarksRecord m = doc.toObject(MarksRecord.class);
                            if (m != null && m.studentId != null) {
                                if (m.id == null || m.id.isEmpty()) {
                                    m.id = doc.getId();
                                }
                                Log.d("FIRESTORE_MARKS", "  doc studentId=" + m.studentId
                                        + " semesterId=" + m.semesterId
                                        + " semesterNumber=" + m.semesterNumber
                                        + " total=" + m.totalObtained);
                                        
                                int recordSemNum = 1;
                                boolean foundRecordSem = false;
                                if (com.kartik.myschool.AppCache.cachedSemesters != null && m.semesterId != null) {
                                    for (com.kartik.myschool.model.Semester sem : com.kartik.myschool.AppCache.cachedSemesters) {
                                        if (sem.id != null && sem.id.equals(m.semesterId)) {
                                            recordSemNum = sem.number;
                                            foundRecordSem = true;
                                            break;
                                        }
                                    }
                                }
                                if (!foundRecordSem) {
                                    if (m.semesterNumber != null && !m.semesterNumber.isEmpty()) {
                                        try {
                                            recordSemNum = Integer.parseInt(m.semesterNumber);
                                        } catch (NumberFormatException ignored) {}
                                    } else if (m.semesterId != null && !m.semesterId.isEmpty() && m.semesterId.length() < 10) {
                                        if (m.semesterId.contains("2") || m.semesterId.toLowerCase().contains("second")) {
                                            recordSemNum = 2;
                                        }
                                    }
                                }
                                
                                if (semesterId.equals(m.semesterId) || finalTargetSemNum == recordSemNum) {
                                    // Match — include & merge duplicate docs per student
                                    MarksRecord existing = marksMap.get(m.studentId);
                                    if (existing == null) {
                                        marksMap.put(m.studentId, m);
                                    } else {
                                        if (m.updatedAt >= existing.updatedAt) {
                                            mergeRecords(m, existing);
                                            marksMap.put(m.studentId, m);
                                        } else {
                                            mergeRecords(existing, m);
                                        }
                                    }
                                } else if (m.semesterId == null || m.semesterId.isEmpty()) {
                                    // No semesterId saved — include as fallback only if target is Sem 1
                                    if (finalTargetSemNum == 1) {
                                        MarksRecord fallbackExisting = fallbackMap.get(m.studentId);
                                        if (fallbackExisting == null) {
                                            fallbackMap.put(m.studentId, m);
                                        } else {
                                            if (m.updatedAt >= fallbackExisting.updatedAt) {
                                                mergeRecords(m, fallbackExisting);
                                                fallbackMap.put(m.studentId, m);
                                            } else {
                                                mergeRecords(fallbackExisting, m);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Merge fallback records into marksMap
                    if (!fallbackMap.isEmpty()) {
                        Log.d("FIRESTORE_MARKS", "Merging fallback records: " + fallbackMap.size());
                        for (java.util.Map.Entry<String, MarksRecord> entry : fallbackMap.entrySet()) {
                            String studentId = entry.getKey();
                            MarksRecord fallbackRec = entry.getValue();
                            if (!marksMap.containsKey(studentId)) {
                                marksMap.put(studentId, fallbackRec);
                            } else {
                                MarksRecord existing = marksMap.get(studentId);
                                if (fallbackRec.updatedAt >= existing.updatedAt) {
                                    mergeRecords(fallbackRec, existing);
                                    marksMap.put(studentId, fallbackRec);
                                } else {
                                    mergeRecords(existing, fallbackRec);
                                }
                            }
                        }
                    }
                    Log.d("FIRESTORE_MARKS", "getMarksForClassAndSemester: returning " + marksMap.size() + " marks records");
                    cachedClassSemesterMarksMap.put(cacheKey, marksMap);
                    cb.onSuccess(new java.util.HashMap<>(marksMap));
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_MARKS", "getMarksForClassAndSemester FAILED: " + e.getMessage(), e);
                    cb.onError(e);
                });
    }

    // ---------- Attendance ----------
    public void saveAttendanceRecord(com.kartik.myschool.model.AttendanceRecord r, OnResult<String> cb) {
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        DocumentReference ref = r.id != null
                ? db.collection(COL_ATTENDANCE_RECORDS).document(r.id)
                : db.collection(COL_ATTENDANCE_RECORDS).document();
        r.id = ref.getId();
        ref.set(r)
                .addOnSuccessListener(v -> cb.onSuccess(r.id))
                .addOnFailureListener(cb::onError);
    }

    public void deleteAttendanceRecord(String id, OnResult<Void> cb) {
        db.collection(COL_ATTENDANCE_RECORDS).document(id).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getAttendanceRecordsForClass(String classId, OnResult<List<com.kartik.myschool.model.AttendanceRecord>> cb) {
        if (classId == null) {
            cb.onError(new IllegalArgumentException("Class ID cannot be null"));
            return;
        }
        db.collection(COL_ATTENDANCE_RECORDS)
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<com.kartik.myschool.model.AttendanceRecord> list =
                            snap != null ? snap.toObjects(com.kartik.myschool.model.AttendanceRecord.class) : new ArrayList<>();
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    private void mergeRecords(MarksRecord target, MarksRecord source) {
        if (target == null || source == null) return;
        
        // Merge detailedMarks maps
        if (source.detailedMarks != null) {
            if (target.detailedMarks == null) {
                target.detailedMarks = new java.util.HashMap<>();
            }
            for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : source.detailedMarks.entrySet()) {
                String subName = entry.getKey();
                MarksRecord.SubjectMarksDetail sourceDetail = entry.getValue();
                if (sourceDetail == null) continue;
                
                MarksRecord.SubjectMarksDetail targetDetail = target.detailedMarks.get(subName);
                if (targetDetail == null) {
                    target.detailedMarks.put(subName, sourceDetail);
                } else {
                    // Merge subject sub-fields
                    if (sourceDetail.nirikhshan > 0) targetDetail.nirikhshan = sourceDetail.nirikhshan;
                    if (sourceDetail.tondiKam > 0) targetDetail.tondiKam = sourceDetail.tondiKam;
                    if (sourceDetail.pratyakshik > 0) targetDetail.pratyakshik = sourceDetail.pratyakshik;
                    if (sourceDetail.upkram > 0) targetDetail.upkram = sourceDetail.upkram;
                    if (sourceDetail.prakalp > 0) targetDetail.prakalp = sourceDetail.prakalp;
                    if (sourceDetail.chachani > 0) targetDetail.chachani = sourceDetail.chachani;
                    if (sourceDetail.swadhyay > 0) targetDetail.swadhyay = sourceDetail.swadhyay;
                    if (sourceDetail.itar > 0) targetDetail.itar = sourceDetail.itar;
                    if (sourceDetail.akarikTotal > 0) targetDetail.akarikTotal = sourceDetail.akarikTotal;
                    
                    if (sourceDetail.tondi > 0) targetDetail.tondi = sourceDetail.tondi;
                    if (sourceDetail.pratyakshikB > 0) targetDetail.pratyakshikB = sourceDetail.pratyakshikB;
                    if (sourceDetail.lekhi > 0) targetDetail.lekhi = sourceDetail.lekhi;
                    if (sourceDetail.sanklit > 0) targetDetail.sanklit = sourceDetail.sanklit;
                    
                    if (sourceDetail.grandTotal > 0) targetDetail.grandTotal = sourceDetail.grandTotal;
                    if (sourceDetail.maxMarks > 0) targetDetail.maxMarks = sourceDetail.maxMarks;
                    if (sourceDetail.grade != null && !sourceDetail.grade.isEmpty()) targetDetail.grade = sourceDetail.grade;
                    
                    // Merge remarks
                    if (sourceDetail.remark != null && !sourceDetail.remark.trim().isEmpty()) {
                        if (targetDetail.remark == null || targetDetail.remark.trim().isEmpty()) {
                            targetDetail.remark = sourceDetail.remark;
                        } else if (!targetDetail.remark.contains(sourceDetail.remark)) {
                            // Merge distinct parts
                            java.util.LinkedHashSet<String> parts = new java.util.LinkedHashSet<>();
                            for (String p : targetDetail.remark.split("\\|\\|")) {
                                if (!p.trim().isEmpty()) parts.add(p.trim());
                            }
                            for (String p : sourceDetail.remark.split("\\|\\|")) {
                                if (!p.trim().isEmpty()) parts.add(p.trim());
                            }
                            StringBuilder sb = new StringBuilder();
                            java.util.List<String> list = new java.util.ArrayList<>(parts);
                            for (int i = 0; i < list.size(); i++) {
                                sb.append(list.get(i));
                                if (i < list.size() - 1) sb.append("||");
                            }
                            targetDetail.remark = sb.toString();
                        }
                    }
                }
            }
        }
        
        // Merge attendance
        if (source.presentDays > 0) target.presentDays = source.presentDays;
        if (source.totalDays > 0) target.totalDays = source.totalDays;
        
        // Merge top-level subjectMarks & subjectMax for legacy/compat compatibility
        if (source.subjectMarks != null) {
            if (target.subjectMarks == null) target.subjectMarks = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Double> entry : source.subjectMarks.entrySet()) {
                if (entry.getValue() > 0) {
                    target.subjectMarks.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (source.subjectMax != null) {
            if (target.subjectMax == null) target.subjectMax = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Integer> entry : source.subjectMax.entrySet()) {
                if (entry.getValue() > 0) {
                    target.subjectMax.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        // Merge total marks
        if (source.totalObtained > 0) target.totalObtained = source.totalObtained;
        if (source.totalMax > 0) target.totalMax = source.totalMax;
        if (source.percentage > 0) target.percentage = source.percentage;
        if (source.grade != null && !source.grade.isEmpty()) target.grade = source.grade;
        if (source.result != null && !source.result.isEmpty()) target.result = source.result;
    }

    // ---------- Subscriptions ----------
    public void getSubscriptionHistory(String teacherId, OnResult<List<com.kartik.myschool.model.SubscriptionRequest>> cb) {
        if (teacherId == null) {
            cb.onError(new IllegalArgumentException("Teacher ID cannot be null"));
            return;
        }
        db.collection(COL_SUBSCRIPTIONS)
                .where(com.google.firebase.firestore.Filter.or(
                        com.google.firebase.firestore.Filter.equalTo("teacherId", teacherId),
                        com.google.firebase.firestore.Filter.equalTo("userId", teacherId)
                ))
                .get()
                .addOnSuccessListener(snap -> {
                    List<com.kartik.myschool.model.SubscriptionRequest> list = new ArrayList<>();
                    if (snap != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            com.kartik.myschool.model.SubscriptionRequest req = doc.toObject(com.kartik.myschool.model.SubscriptionRequest.class);
                            if (req != null) {
                                req.id = doc.getId();
                                list.add(req);
                            }
                        }
                    }
                    // Sort descending by timestamp
                    Collections.sort(list, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    // ═══════════════════════════════════════════════════════════════════
    // REMARK BANK — per-subject dropdown options (customisable by teacher)
    // ═══════════════════════════════════════════════════════════════════

    private static final String COL_REMARK_BANKS = "remarkBanks";

    /**
     * Fetches remark options for a given subject from Firestore.
     * Falls back to RemarkBank.defaultOptionsFor(subjectName) when no doc exists.
     */
    public void getRemarkBank(String schoolId, String subjectName,
                              OnResult<java.util.List<String>> cb) {
        String docId = makeRemarkBankId(schoolId, subjectName);
        db.collection(COL_REMARK_BANKS).document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        com.kartik.myschool.model.RemarkBank bank =
                                doc.toObject(com.kartik.myschool.model.RemarkBank.class);
                        if (bank != null && bank.options != null && !bank.options.isEmpty()) {
                            cb.onSuccess(bank.options);
                            return;
                        }
                    }
                    // No custom bank → return defaults
                    cb.onSuccess(com.kartik.myschool.model.RemarkBank.defaultOptionsFor(subjectName));
                })
                .addOnFailureListener(e -> {
                    // On network error also fall back to defaults
                    cb.onSuccess(com.kartik.myschool.model.RemarkBank.defaultOptionsFor(subjectName));
                });
    }

    /**
     * Saves (creates or overwrites) the remark bank for a subject.
     */
    public void saveRemarkBank(String schoolId, String subjectName,
                               java.util.List<String> options,
                               OnResult<Void> cb) {
        com.kartik.myschool.model.RemarkBank bank =
                new com.kartik.myschool.model.RemarkBank(schoolId, subjectName, options);
        String docId = makeRemarkBankId(schoolId, subjectName);
        db.collection(COL_REMARK_BANKS).document(docId)
                .set(bank)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    private String makeRemarkBankId(String schoolId, String subjectName) {
        String safe = subjectName != null
                ? subjectName.replaceAll("[^a-zA-Z0-9\\u0900-\\u097F]", "_")
                : "general";
        return (schoolId != null ? schoolId : "default") + "_" + safe;
    }

    public void getClassesForUdiseCode(String udiseCode, OnResult<List<ClassModel>> cb) {
        if (udiseCode == null || udiseCode.isEmpty()) {
            cb.onError(new IllegalArgumentException("UDISE code cannot be null or empty"));
            return;
        }
        db.collection(COL_SCHOOLS)
                .whereEqualTo("udiseCode", udiseCode)
                .get()
                .addOnSuccessListener(schoolSnap -> {
                    List<String> schoolIds = new ArrayList<>();
                    if (schoolSnap != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : schoolSnap.getDocuments()) {
                            schoolIds.add(doc.getId());
                        }
                    }
                    if (schoolIds.isEmpty()) {
                        cb.onSuccess(new ArrayList<>());
                        return;
                    }
                    
                    fetchClassesForSchoolIds(schoolIds, new OnResult<List<ClassModel>>() {
                        @Override
                        public void onSuccess(List<ClassModel> classes) {
                            if (classes.isEmpty()) {
                                cb.onSuccess(classes);
                                return;
                            }
                            fetchStudentCountsForSchoolIds(schoolIds, new OnResult<java.util.Map<String, Integer>>() {
                                @Override
                                public void onSuccess(java.util.Map<String, Integer> counts) {
                                    for (ClassModel c : classes) {
                                        c.studentCount = counts.containsKey(c.id) ? counts.get(c.id) : 0;
                                    }
                                    Collections.sort(classes, (a, b) -> {
                                        String nameA = a.className != null ? a.className : "";
                                        String nameB = b.className != null ? b.className : "";
                                        int comp = nameA.compareTo(nameB);
                                        if (comp != 0) return comp;
                                        String divA = a.division != null ? a.division : "";
                                        String divB = b.division != null ? b.division : "";
                                        return divA.compareTo(divB);
                                    });
                                    cb.onSuccess(classes);
                                }
                                @Override
                                public void onError(Exception e) {
                                    cb.onError(e);
                                }
                            });
                        }
                        @Override
                        public void onError(Exception e) {
                            cb.onError(e);
                        }
                    });
                })
                .addOnFailureListener(cb::onError);
    }

    private void fetchClassesForSchoolIds(List<String> schoolIds, OnResult<List<ClassModel>> cb) {
        List<ClassModel> allClasses = new ArrayList<>();
        int limit = 30;
        int size = schoolIds.size();
        java.util.concurrent.atomic.AtomicInteger pendingQueries = new java.util.concurrent.atomic.AtomicInteger((size + limit - 1) / limit);
        java.util.concurrent.atomic.AtomicReference<Exception> errorRef = new java.util.concurrent.atomic.AtomicReference<>();
        
        for (int i = 0; i < size; i += limit) {
            List<String> chunk = schoolIds.subList(i, Math.min(i + limit, size));
            db.collection(COL_CLASSES)
                    .whereIn("schoolId", chunk)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null) {
                            synchronized (allClasses) {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                    ClassModel c = doc.toObject(ClassModel.class);
                                    if (c != null) {
                                        if (c.id == null || c.id.isEmpty()) {
                                            c.id = doc.getId();
                                        }
                                        allClasses.add(c);
                                    }
                                }
                            }
                        }
                        if (pendingQueries.decrementAndGet() == 0) {
                            if (errorRef.get() != null) {
                                cb.onError(errorRef.get());
                            } else {
                                cb.onSuccess(allClasses);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        errorRef.set(e);
                        if (pendingQueries.decrementAndGet() == 0) {
                            cb.onError(e);
                        }
                    });
        }
    }

    private void fetchStudentCountsForSchoolIds(List<String> schoolIds, OnResult<java.util.Map<String, Integer>> cb) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        int limit = 30;
        int size = schoolIds.size();
        java.util.concurrent.atomic.AtomicInteger pendingQueries = new java.util.concurrent.atomic.AtomicInteger((size + limit - 1) / limit);
        java.util.concurrent.atomic.AtomicReference<Exception> errorRef = new java.util.concurrent.atomic.AtomicReference<>();
        
        for (int i = 0; i < size; i += limit) {
            List<String> chunk = schoolIds.subList(i, Math.min(i + limit, size));
            db.collection(COL_STUDENTS)
                    .whereIn("schoolId", chunk)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null) {
                            synchronized (counts) {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                    Student s = doc.toObject(Student.class);
                                    if (s != null && s.classId != null) {
                                        counts.put(s.classId, counts.getOrDefault(s.classId, 0) + 1);
                                    }
                                }
                            }
                        }
                        if (pendingQueries.decrementAndGet() == 0) {
                            if (errorRef.get() != null) {
                                cb.onError(errorRef.get());
                            } else {
                                cb.onSuccess(counts);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        errorRef.set(e);
                        if (pendingQueries.decrementAndGet() == 0) {
                            cb.onError(e);
                        }
                    });
        }
    }

    public void getStudentsForUdiseCode(String udiseCode, OnResult<List<Student>> cb) {
        if (udiseCode == null || udiseCode.isEmpty()) {
            cb.onError(new IllegalArgumentException("UDISE code cannot be null or empty"));
            return;
        }
        db.collection(COL_SCHOOLS)
                .whereEqualTo("udiseCode", udiseCode)
                .get()
                .addOnSuccessListener(schoolSnap -> {
                    List<String> schoolIds = new ArrayList<>();
                    if (schoolSnap != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : schoolSnap.getDocuments()) {
                            schoolIds.add(doc.getId());
                        }
                    }
                    if (schoolIds.isEmpty()) {
                        cb.onSuccess(new ArrayList<>());
                        return;
                    }
                    
                    fetchStudentsForSchoolIds(schoolIds, cb);
                })
                .addOnFailureListener(cb::onError);
    }

    private void fetchStudentsForSchoolIds(List<String> schoolIds, OnResult<List<Student>> cb) {
        List<Student> allStudents = new ArrayList<>();
        int limit = 30;
        int size = schoolIds.size();
        java.util.concurrent.atomic.AtomicInteger pendingQueries = new java.util.concurrent.atomic.AtomicInteger((size + limit - 1) / limit);
        java.util.concurrent.atomic.AtomicReference<Exception> errorRef = new java.util.concurrent.atomic.AtomicReference<>();
        
        for (int i = 0; i < size; i += limit) {
            List<String> chunk = schoolIds.subList(i, Math.min(i + limit, size));
            db.collection(COL_STUDENTS)
                    .whereIn("schoolId", chunk)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null) {
                            synchronized (allStudents) {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                    Student s = doc.toObject(Student.class);
                                    if (s != null) {
                                        if (s.id == null || s.id.isEmpty()) {
                                            s.id = doc.getId();
                                        }
                                        allStudents.add(s);
                                    }
                                }
                            }
                        }
                        if (pendingQueries.decrementAndGet() == 0) {
                            if (errorRef.get() != null) {
                                cb.onError(errorRef.get());
                            } else {
                                cb.onSuccess(allStudents);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        errorRef.set(e);
                        if (pendingQueries.decrementAndGet() == 0) {
                            cb.onError(e);
                        }
                    });
        }
    }

    // ---------- Callback interface ----------
    public interface OnResult<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}
