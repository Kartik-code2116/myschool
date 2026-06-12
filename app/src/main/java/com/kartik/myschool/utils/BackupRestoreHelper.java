package com.kartik.myschool.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kartik.myschool.R;
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

public class BackupRestoreHelper {

    private static final String TAG = "BackupRestore";
    private static final Gson gson = new Gson();

    public static class FullBackupData {
        public List<School> schools = new ArrayList<>();
        public List<AcademicYear> academicYears = new ArrayList<>();
        public List<Semester> semesters = new ArrayList<>();
        public List<ClassModel> classes = new ArrayList<>();
        public List<Student> students = new ArrayList<>();
        public List<MarksRecord> marks = new ArrayList<>();
        public List<AttendanceRecord> attendance = new ArrayList<>();
    }

    public static void exportFullAccount(Context context) {
        ProgressDialog pd = new ProgressDialog(context);
        pd.setTitle("Generating Backup");
        pd.setMessage("Fetching all account data...");
        pd.setCancelable(false);
        pd.show();

        FullBackupData data = new FullBackupData();
        FirebaseRepository repo = FirebaseRepository.get();

        // 1. Fetch Schools
        repo.getSchools(new FirebaseRepository.OnResult<List<School>>() {
            @Override
            public void onSuccess(List<School> schools) {
                if (schools != null) data.schools.addAll(schools);
                
                // 2. Fetch Academic Years
                repo.getAcademicYears(new FirebaseRepository.OnResult<List<AcademicYear>>() {
                    @Override
                    public void onSuccess(List<AcademicYear> years) {
                        if (years != null) data.academicYears.addAll(years);

                        // 3. Fetch Semesters
                        repo.getAllSemestersForTeacher(new FirebaseRepository.OnResult<List<Semester>>() {
                            @Override
                            public void onSuccess(List<Semester> sems) {
                                if (sems != null) data.semesters.addAll(sems);

                                // 4. Fetch Classes
                                repo.getAllClassesForTeacher(new FirebaseRepository.OnResult<List<ClassModel>>() {
                                    @Override
                                    public void onSuccess(List<ClassModel> classes) {
                                        if (classes != null) data.classes.addAll(classes);

                                        // 5. Fetch Students
                                        repo.getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
                                            @Override
                                            public void onSuccess(List<Student> students) {
                                                if (students != null) data.students.addAll(students);

                                                // 6. Fetch Marks
                                                repo.getAllMarksForTeacher(new FirebaseRepository.OnResult<List<MarksRecord>>() {
                                                    @Override
                                                    public void onSuccess(List<MarksRecord> marks) {
                                                        if (marks != null) data.marks.addAll(marks);

                                                        // 7. Fetch Attendance
                                                        fetchAttendanceForClasses(repo, data.classes, data, pd, context);
                                                    }

                                                    @Override
                                                    public void onError(Exception e) { failExport(pd, context, e); }
                                                });
                                            }

                                            @Override
                                            public void onError(Exception e) { failExport(pd, context, e); }
                                        });
                                    }

                                    @Override
                                    public void onError(Exception e) { failExport(pd, context, e); }
                                });
                            }

                            @Override
                            public void onError(Exception e) { failExport(pd, context, e); }
                        });
                    }

                    @Override
                    public void onError(Exception e) { failExport(pd, context, e); }
                });
            }

            @Override
            public void onError(Exception e) { failExport(pd, context, e); }
        });
    }

    private static void fetchAttendanceForClasses(FirebaseRepository repo, List<ClassModel> classes, FullBackupData data, ProgressDialog pd, Context context) {
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

    private static void failExport(ProgressDialog pd, Context context, Exception e) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (pd.isShowing()) pd.dismiss();
            Toast.makeText(context, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private static void finalizeExport(FullBackupData data, ProgressDialog pd, Context context) {
        new Handler(Looper.getMainLooper()).post(() -> {
            pd.setMessage("Saving backup file...");
        });

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
                    if (pd.isShowing()) pd.dismiss();
                    context.startActivity(Intent.createChooser(intent, "Share Full Account Backup"));
                });

            } catch (Exception e) {
                failExport(pd, context, e);
            }
        }).start();
    }

    public static void importFullAccount(Context context, Uri uri, Runnable onComplete) {
        ProgressDialog pd = new ProgressDialog(context);
        pd.setTitle("Restoring Backup");
        pd.setMessage("Reading backup file...");
        pd.setCancelable(false);
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
                    if (pd.isShowing()) pd.dismiss();
                    Toast.makeText(context, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private static void uploadRestoredData(Context context, FullBackupData data, ProgressDialog pd, Runnable onComplete) {
        FirebaseRepository repo = FirebaseRepository.get();
        String currentUid = repo.currentUid();
        
        if (currentUid == null) {
            if (pd.isShowing()) pd.dismiss();
            Toast.makeText(context, "Error: User not authenticated", Toast.LENGTH_LONG).show();
            return;
        }

        // Map Old ID -> New ID
        Map<String, String> idMap = new HashMap<>();

        // Generate new IDs for everything
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

        int totalTasks = data.schools.size() + data.academicYears.size() + data.semesters.size() +
                data.classes.size() + data.students.size() + data.marks.size() + data.attendance.size();

        if (totalTasks == 0) {
            if (pd.isShowing()) pd.dismiss();
            Toast.makeText(context, "Backup file is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        FirebaseRepository.OnResult<String> genericCb = new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String result) {
                checkDone(totalTasks, completed.incrementAndGet(), failed.get(), pd, context, onComplete);
            }
            @Override
            public void onError(Exception e) {
                checkDone(totalTasks, completed.get(), failed.incrementAndGet(), pd, context, onComplete);
            }
        };
        
        FirebaseRepository.OnResult<Void> voidCb = new FirebaseRepository.OnResult<Void>() {
            @Override
            public void onSuccess(Void result) {
                checkDone(totalTasks, completed.incrementAndGet(), failed.get(), pd, context, onComplete);
            }
            @Override
            public void onError(Exception e) {
                checkDone(totalTasks, completed.get(), failed.incrementAndGet(), pd, context, onComplete);
            }
        };

        for (School s : data.schools) repo.saveSchool(s, genericCb);
        for (AcademicYear y : data.academicYears) repo.saveAcademicYear(y, genericCb);
        for (Semester s : data.semesters) repo.saveSemester(s, genericCb);
        for (ClassModel c : data.classes) repo.saveClass(c, genericCb);
        for (Student s : data.students) repo.saveStudent(s, genericCb);
        for (MarksRecord m : data.marks) repo.saveMarks(m, genericCb);
        for (AttendanceRecord a : data.attendance) repo.saveAttendanceRecord(a, genericCb);
    }

    private static void checkDone(int total, int completed, int failed, ProgressDialog pd, Context context, Runnable onComplete) {
        if (completed + failed >= total) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (pd.isShowing()) pd.dismiss();
                if (failed > 0) {
                    Toast.makeText(context, "Restore completed with " + failed + " errors.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Account Restore Complete!", Toast.LENGTH_LONG).show();
                }
                if (onComplete != null) onComplete.run();
            });
        }
    }
}
