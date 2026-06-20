package com.kartik.myschool.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.kartik.myschool.model.AcademicYear;
import com.kartik.myschool.model.AttendanceRecord;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Semester;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BackupRestoreHelper — handles full account export/import.
 *
 * Audit fixes applied:
 *   #3  — Replaced deprecated android.app.ProgressDialog with LoadingDialog (MaterialAlertDialog)
 *   #6  — Replaced N parallel Firestore writes with WriteBatch (max 500 ops per batch)
 *         for atomic, faster, rate-limit-safe restores.
 */
public class BackupRestoreHelper {

    private static final String TAG = "BackupRestore";
    private static final Gson gson = new Gson();

    /** Maximum Firestore WriteBatch size (Firestore hard limit is 500). */
    private static final int BATCH_SIZE = 499;

    public static class FullBackupData {
        public List<School> schools = new ArrayList<>();
        public List<AcademicYear> academicYears = new ArrayList<>();
        public List<Semester> semesters = new ArrayList<>();
        public List<ClassModel> classes = new ArrayList<>();
        public List<Student> students = new ArrayList<>();
        public List<MarksRecord> marks = new ArrayList<>();
        public List<AttendanceRecord> attendance = new ArrayList<>();
    }

    // ─────────────────────────────── EXPORT ────────────────────────────────

    public static void exportFullAccount(Context context) {
        // Fix #3: Use LoadingDialog instead of deprecated ProgressDialog
        LoadingDialog pd = new LoadingDialog(context, "Generating Backup", "Fetching all account data...");
        pd.show();

        FullBackupData data = new FullBackupData();
        FirebaseRepository repo = FirebaseRepository.get();

        // Sequential fetching via named private methods (reduces callback nesting depth)
        fetchSchools(repo, data, pd, context);
    }

    private static void fetchSchools(FirebaseRepository repo, FullBackupData data, LoadingDialog pd, Context context) {
        repo.getSchools(new FirebaseRepository.OnResult<List<School>>() {
            @Override public void onSuccess(List<School> schools) {
                if (schools != null) data.schools.addAll(schools);
                fetchAcademicYears(repo, data, pd, context);
            }
            @Override public void onError(Exception e) { failExport(pd, context, e); }
        });
    }

    private static void fetchAcademicYears(FirebaseRepository repo, FullBackupData data, LoadingDialog pd, Context context) {
        repo.getAcademicYears(new FirebaseRepository.OnResult<List<AcademicYear>>() {
            @Override public void onSuccess(List<AcademicYear> years) {
                if (years != null) data.academicYears.addAll(years);
                fetchSemesters(repo, data, pd, context);
            }
            @Override public void onError(Exception e) { failExport(pd, context, e); }
        });
    }

    private static void fetchSemesters(FirebaseRepository repo, FullBackupData data, LoadingDialog pd, Context context) {
        repo.getAllSemestersForTeacher(new FirebaseRepository.OnResult<List<Semester>>() {
            @Override public void onSuccess(List<Semester> sems) {
                if (sems != null) data.semesters.addAll(sems);
                fetchClasses(repo, data, pd, context);
            }
            @Override public void onError(Exception e) { failExport(pd, context, e); }
        });
    }

    private static void fetchClasses(FirebaseRepository repo, FullBackupData data, LoadingDialog pd, Context context) {
        repo.getAllClassesForTeacher(new FirebaseRepository.OnResult<List<ClassModel>>() {
            @Override public void onSuccess(List<ClassModel> classes) {
                if (classes != null) data.classes.addAll(classes);
                fetchStudents(repo, data, pd, context);
            }
            @Override public void onError(Exception e) { failExport(pd, context, e); }
        });
    }

    private static void fetchStudents(FirebaseRepository repo, FullBackupData data, LoadingDialog pd, Context context) {
        repo.getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
            @Override public void onSuccess(List<Student> students) {
                if (students != null) data.students.addAll(students);
                fetchMarks(repo, data, pd, context);
            }
            @Override public void onError(Exception e) { failExport(pd, context, e); }
        });
    }

    private static void fetchMarks(FirebaseRepository repo, FullBackupData data, LoadingDialog pd, Context context) {
        repo.getAllMarksForTeacher(new FirebaseRepository.OnResult<List<MarksRecord>>() {
            @Override public void onSuccess(List<MarksRecord> marks) {
                if (marks != null) data.marks.addAll(marks);
                fetchAttendanceForClasses(repo, data.classes, data, pd, context);
            }
            @Override public void onError(Exception e) { failExport(pd, context, e); }
        });
    }

    private static void fetchAttendanceForClasses(FirebaseRepository repo, List<ClassModel> classes,
                                                   FullBackupData data, LoadingDialog pd, Context context) {
        if (classes.isEmpty()) {
            finalizeExport(data, pd, context);
            return;
        }

        AtomicInteger pending = new AtomicInteger(classes.size());
        boolean[] hasError = {false};

        for (ClassModel cls : classes) {
            repo.getAttendanceRecordsForClass(cls.id, new FirebaseRepository.OnResult<List<AttendanceRecord>>() {
                @Override
                public void onSuccess(List<AttendanceRecord> records) {
                    if (hasError[0]) return;
                    if (records != null) {
                        synchronized (data.attendance) {
                            data.attendance.addAll(records);
                        }
                    }
                    if (pending.decrementAndGet() == 0) {
                        finalizeExport(data, pd, context);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (!hasError[0]) {
                        hasError[0] = true;
                        failExport(pd, context, e);
                    }
                }
            });
        }
    }

    private static void failExport(LoadingDialog pd, Context context, Exception e) {
        new Handler(Looper.getMainLooper()).post(() -> {
            pd.dismiss();
            Toast.makeText(context, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private static void finalizeExport(FullBackupData data, LoadingDialog pd, Context context) {
        new Handler(Looper.getMainLooper()).post(() -> pd.setMessage("Saving backup file..."));

        new Thread(() -> {
            try {
                String jsonString = gson.toJson(data);
                File cachePath = new File(context.getCacheDir(), "backups");
                if (!cachePath.exists()) cachePath.mkdirs();

                String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File file = new File(cachePath, "account_backup_" + dateStr + ".json");
                FileWriter writer = new FileWriter(file);
                writer.write(jsonString);
                writer.close();

                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                new Handler(Looper.getMainLooper()).post(() -> {
                    pd.dismiss();
                    context.startActivity(Intent.createChooser(intent, "Share Full Account Backup"));
                });

            } catch (Exception e) {
                failExport(pd, context, e);
            }
        }).start();
    }

    // ─────────────────────────────── IMPORT ────────────────────────────────

    public static void importFullAccount(Context context, Uri uri, Runnable onComplete) {
        // Fix #3: Use LoadingDialog instead of deprecated ProgressDialog
        LoadingDialog pd = new LoadingDialog(context, "Restoring Backup", "Reading backup file...");
        pd.show();

        new Thread(() -> {
            try {
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is == null) throw new Exception("Could not open file");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                is.close();

                FullBackupData data = gson.fromJson(sb.toString(), FullBackupData.class);
                if (data == null) throw new Exception("Invalid backup file format");

                new Handler(Looper.getMainLooper()).post(() -> {
                    pd.setMessage("Restoring data to Firestore...");
                    uploadRestoredData(context, data, pd, onComplete);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    pd.dismiss();
                    Toast.makeText(context, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private static void uploadRestoredData(Context context, FullBackupData data, LoadingDialog pd, Runnable onComplete) {
        FirebaseRepository repo = FirebaseRepository.get();
        String currentUid = repo.currentUid();

        if (currentUid == null) {
            pd.dismiss();
            Toast.makeText(context, "Error: User not authenticated", Toast.LENGTH_LONG).show();
            return;
        }

        // Remap all IDs to new UUIDs so we don't collide with existing data
        Map<String, String> idMap = new HashMap<>();

        for (School s : data.schools) {
            String newId = UUID.randomUUID().toString();
            if (s.id != null) idMap.put(s.id, newId);
            s.id = newId;
            s.teacherId = currentUid;
        }
        for (AcademicYear y : data.academicYears) {
            String newId = UUID.randomUUID().toString();
            if (y.id != null) idMap.put(y.id, newId);
            y.id = newId;
            y.teacherId = currentUid;
        }
        for (Semester s : data.semesters) {
            String newId = UUID.randomUUID().toString();
            if (s.id != null) idMap.put(s.id, newId);
            s.id = newId;
            s.teacherId = currentUid;
            if (s.yearId != null && idMap.containsKey(s.yearId)) s.yearId = idMap.get(s.yearId);
        }
        for (ClassModel c : data.classes) {
            String newId = UUID.randomUUID().toString();
            if (c.id != null) idMap.put(c.id, newId);
            c.id = newId;
            c.teacherId = currentUid;
            if (c.schoolId != null && idMap.containsKey(c.schoolId)) c.schoolId = idMap.get(c.schoolId);
            if (c.yearId != null && idMap.containsKey(c.yearId)) c.yearId = idMap.get(c.yearId);
            if (c.semesterId != null && idMap.containsKey(c.semesterId)) c.semesterId = idMap.get(c.semesterId);
        }
        for (Student s : data.students) {
            String newId = UUID.randomUUID().toString();
            if (s.id != null) idMap.put(s.id, newId);
            s.id = newId;
            s.teacherId = currentUid;
            if (s.schoolId != null && idMap.containsKey(s.schoolId)) s.schoolId = idMap.get(s.schoolId);
            if (s.classId != null && idMap.containsKey(s.classId)) s.classId = idMap.get(s.classId);
        }
        for (MarksRecord m : data.marks) {
            String newId = UUID.randomUUID().toString();
            if (m.id != null) idMap.put(m.id, newId);
            m.id = newId;
            m.teacherId = currentUid;
            m.editedBy = currentUid;
            if (m.studentId != null && idMap.containsKey(m.studentId)) m.studentId = idMap.get(m.studentId);
            if (m.classId != null && idMap.containsKey(m.classId)) m.classId = idMap.get(m.classId);
            if (m.semesterId != null && idMap.containsKey(m.semesterId)) m.semesterId = idMap.get(m.semesterId);
        }
        for (AttendanceRecord a : data.attendance) {
            String newId = UUID.randomUUID().toString();
            if (a.id != null) idMap.put(a.id, newId);
            a.id = newId;
            if (a.studentId != null && idMap.containsKey(a.studentId)) a.studentId = idMap.get(a.studentId);
            if (a.classId != null && idMap.containsKey(a.classId)) a.classId = idMap.get(a.classId);
        }

        // Collect all documents to write
        List<Object[]> docsToWrite = new ArrayList<>(); // {collectionPath, id, object}
        for (School s : data.schools) docsToWrite.add(new Object[]{FirebaseRepository.COL_SCHOOLS, s.id, s});
        for (AcademicYear y : data.academicYears) docsToWrite.add(new Object[]{FirebaseRepository.COL_ACADEMIC_YEARS, y.id, y});
        for (Semester s : data.semesters) docsToWrite.add(new Object[]{FirebaseRepository.COL_SEMESTERS, s.id, s});
        for (ClassModel c : data.classes) docsToWrite.add(new Object[]{FirebaseRepository.COL_CLASSES, c.id, c});
        for (Student s : data.students) docsToWrite.add(new Object[]{FirebaseRepository.COL_STUDENTS, s.id, s});
        for (MarksRecord m : data.marks) docsToWrite.add(new Object[]{FirebaseRepository.COL_MARKS, m.id, m});
        for (AttendanceRecord a : data.attendance) docsToWrite.add(new Object[]{FirebaseRepository.COL_ATTENDANCE_RECORDS, a.id, a});

        int totalDocs = docsToWrite.size();
        if (totalDocs == 0) {
            pd.dismiss();
            Toast.makeText(context, "Backup file is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fix #6: Use WriteBatch instead of N parallel individual writes.
        // Firestore WriteBatch is atomic (all-or-nothing) and much faster.
        // Max batch size is 500 operations; split into multiple batches for large restores.
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Split into batches of BATCH_SIZE
        List<List<Object[]>> batches = new ArrayList<>();
        for (int i = 0; i < totalDocs; i += BATCH_SIZE) {
            batches.add(docsToWrite.subList(i, Math.min(i + BATCH_SIZE, totalDocs)));
        }

        commitBatchesSequentially(db, batches, 0, totalDocs, pd, context, onComplete);
    }

    /**
     * Commits Firestore WriteBatches sequentially to avoid overwhelming the server.
     * Each batch is committed only after the previous one succeeds.
     */
    private static void commitBatchesSequentially(FirebaseFirestore db, List<List<Object[]>> batches,
                                                   int batchIndex, int totalDocs,
                                                   LoadingDialog pd, Context context, Runnable onComplete) {
        if (batchIndex >= batches.size()) {
            // All batches committed successfully
            new Handler(Looper.getMainLooper()).post(() -> {
                pd.dismiss();
                Toast.makeText(context, "Account Restore Complete! (" + totalDocs + " records)", Toast.LENGTH_LONG).show();
                if (onComplete != null) onComplete.run();
            });
            return;
        }

        List<Object[]> batchDocs = batches.get(batchIndex);
        WriteBatch batch = db.batch();

        for (Object[] doc : batchDocs) {
            String collection = (String) doc[0];
            String id = (String) doc[1];
            Object data = doc[2];
            batch.set(db.collection(collection).document(id), data);
        }

        int batchNum = batchIndex + 1;
        int total = batches.size();
        new Handler(Looper.getMainLooper()).post(() ->
                pd.setMessage("Restoring batch " + batchNum + " of " + total + "..."));

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // Commit next batch
                    commitBatchesSequentially(db, batches, batchIndex + 1, totalDocs, pd, context, onComplete);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "WriteBatch commit failed for batch " + batchNum, e);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        pd.dismiss();
                        Toast.makeText(context, "Restore failed on batch " + batchNum + ": " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                });
    }

    // Unused checkDone removed — WriteBatch handles atomicity natively
}
