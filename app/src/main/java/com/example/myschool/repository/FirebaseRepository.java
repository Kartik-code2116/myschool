package com.example.myschool.repository;

import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.model.Teacher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirebaseRepository {

    private static FirebaseRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public static final String COL_TEACHERS = "teachers";
    public static final String COL_SCHOOLS = "schools";
    public static final String COL_CLASSES = "classes";
    public static final String COL_STUDENTS = "students";
    public static final String COL_MARKS = "marks";

    private FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static FirebaseRepository get() {
        if (instance == null) instance = new FirebaseRepository();
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
                    // Sort by name in memory to avoid composite index
                    Collections.sort(schools, (a, b) -> {
                        if (a.name == null) return -1;
                        if (b.name == null) return 1;
                        return a.name.compareTo(b.name);
                    });
                    cb.onSuccess(schools);
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
                    // Sort by className in memory to avoid composite index
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

    public void getStudentsForClass(String classId, OnResult<List<Student>> cb) {
        if (classId == null) {
            cb.onError(new IllegalArgumentException("Class ID cannot be null"));
            return;
        }
        db.collection(COL_STUDENTS)
                .whereEqualTo("classId", classId)
                .orderBy("rollNo", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> cb.onSuccess(snap != null ? snap.toObjects(Student.class) : null))
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
                .addOnSuccessListener(snap -> cb.onSuccess(snap != null ? snap.toObjects(Student.class) : null))
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
                .addOnSuccessListener(snap -> cb.onSuccess(snap != null ? snap.toObjects(Student.class) : null))
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
                    if (snap != null && !snap.isEmpty() && snap.getDocuments().size() > 0) {
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
                .addOnSuccessListener(snap -> cb.onSuccess(snap != null ? snap.toObjects(MarksRecord.class) : null))
                .addOnFailureListener(cb::onError);
    }

    // ---------- Callback interface ----------
    public interface OnResult<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}
