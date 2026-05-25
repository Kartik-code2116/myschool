package com.example.myschool.repository;

import com.example.myschool.model.AcademicYear;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Semester;
import com.example.myschool.model.Student;
import com.example.myschool.model.Teacher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirebaseRepository {

    private static volatile FirebaseRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public static final String COL_TEACHERS = "teachers";
    public static final String COL_SCHOOLS = "schools";
    public static final String COL_ACADEMIC_YEARS = "academic_years";
    public static final String COL_SEMESTERS = "semesters";
    public static final String COL_CLASSES = "classes";
    public static final String COL_STUDENTS = "students";
    public static final String COL_MARKS = "marks";

    private FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
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
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        t.id = uid;
        db.collection(COL_TEACHERS).document(t.id).set(t)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getTeacher(OnResult<Teacher> cb) {
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        db.collection(COL_TEACHERS).document(uid).get()
                .addOnSuccessListener(snap -> cb.onSuccess(snap != null ? snap.toObject(Teacher.class) : null))
                .addOnFailureListener(cb::onError);
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
                .addOnSuccessListener(v -> cb.onSuccess(y.id))
                .addOnFailureListener(cb::onError);
    }

    public void getAcademicYears(OnResult<List<AcademicYear>> cb) {
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
                    cb.onSuccess(years);
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
                .addOnSuccessListener(v -> cb.onSuccess(s.id))
                .addOnFailureListener(cb::onError);
    }

    public void getSemestersForYear(String yearId, OnResult<List<Semester>> cb) {
        if (yearId == null) {
            cb.onError(new IllegalArgumentException("Year ID cannot be null"));
            return;
        }
        db.collection(COL_SEMESTERS)
                .whereEqualTo("yearId", yearId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Semester> list = snap != null ? snap.toObjects(Semester.class) : new ArrayList<>();
                    Collections.sort(list, (a, b) -> Integer.compare(a.number, b.number));
                    cb.onSuccess(list);
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
                saveSemester(s2, new OnResult<String>() {
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
                    cb.onSuccess(classes);
                })
                .addOnFailureListener(cb::onError);
    }

    // ---------- Class ----------
    public void saveClass(ClassModel c, OnResult<String> cb) {
        DocumentReference ref = c.id != null
                ? db.collection(COL_CLASSES).document(c.id)
                : db.collection(COL_CLASSES).document();
        c.id = ref.getId();
        ref.set(c)
                .addOnSuccessListener(v -> cb.onSuccess(c.id))
                .addOnFailureListener(cb::onError);
    }

    public void getClassesForSchool(String schoolId, OnResult<List<ClassModel>> cb) {
        if (schoolId == null) {
            cb.onError(new IllegalArgumentException("School ID cannot be null"));
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
                    cb.onSuccess(classes);
                })
                .addOnFailureListener(cb::onError);
    }

    // ---------- Student ----------
    public void saveStudent(Student s, OnResult<String> cb) {
        DocumentReference ref = s.id != null
                ? db.collection(COL_STUDENTS).document(s.id)
                : db.collection(COL_STUDENTS).document();
        s.id = ref.getId();
        ref.set(s)
                .addOnSuccessListener(v -> cb.onSuccess(s.id))
                .addOnFailureListener(cb::onError);
    }

    // Bug #3 fix: removed .orderBy("rollNo") to avoid requiring a composite Firestore index.
    // Sorting is now done in memory, consistent with other methods.
    public void getStudentsForClass(String classId, OnResult<List<Student>> cb) {
        if (classId == null) {
            cb.onError(new IllegalArgumentException("Class ID cannot be null"));
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
                        return ra.compareTo(rb);
                    });
                    cb.onSuccess(students);
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
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        db.collection(COL_STUDENTS)
                .whereEqualTo("teacherId", uid)
                .get()
                // Bug #6 fix: return empty list instead of null
                .addOnSuccessListener(snap -> cb.onSuccess(snap != null ? snap.toObjects(Student.class) : new ArrayList<>()))
                .addOnFailureListener(cb::onError);
    }

    // ---------- Marks ----------
    public void saveMarks(MarksRecord m, OnResult<String> cb) {
        String uid = currentUid();
        if (uid == null) {
            cb.onError(new IllegalStateException("User not authenticated"));
            return;
        }
        m.editedBy = uid;
        m.updatedAt = System.currentTimeMillis();
        DocumentReference ref = m.id != null
                ? db.collection(COL_MARKS).document(m.id)
                : db.collection(COL_MARKS).document();
        m.id = ref.getId();
        ref.set(m)
                .addOnSuccessListener(v -> cb.onSuccess(m.id))
                .addOnFailureListener(cb::onError);
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

    // ---------- Callback interface ----------
    public interface OnResult<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}
