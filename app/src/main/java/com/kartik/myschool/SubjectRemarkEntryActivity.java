package com.kartik.myschool;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.kartik.myschool.databinding.ActivitySubjectRemarkEntryBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public class SubjectRemarkEntryActivity extends AppCompatActivity {

    private ActivitySubjectRemarkEntryBinding b;
    private Student student;
    private ClassModel classModel;
    private MarksRecord marksRecord;
    private String subjectName;
    private int subjectIndex;
    private final List<String> bankOptions = new ArrayList<>();
    private final LinkedHashSet<String> selectedRemarks = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ClassModel callerClass = AppCache.selectedClass;
        SessionContext.load(this);
        super.onCreate(savedInstanceState);

        b = ActivitySubjectRemarkEntryBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        student = AppCache.selectedStudent;
        classModel = AppCache.selectedClass != null ? AppCache.selectedClass : callerClass;
        subjectName = AppCache.selectedSubjectName;
        subjectIndex = AppCache.selectedSubjectIndex;
        marksRecord = AppCache.selectedMarks;

        if (classModel == null) {
            classModel = SessionContext.selectedClass;
        }
        if (subjectName == null || subjectName.trim().isEmpty()) {
            subjectName = getSubjectNameFromIndex();
        }

        if (student == null || classModel == null || subjectName == null || subjectName.trim().isEmpty()) {
            Toast.makeText(this, "Missing student or subject details.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ensureSemester();
        bindHeader();
        loadExistingSelection();
        renderSelection();
        loadRemarkBank();

        b.btnChooseRemarks.setOnClickListener(v -> showRemarkDialog());
        b.btnSaveRemark.setOnClickListener(v -> saveSubjectRemark());
    }

    private String getSubjectNameFromIndex() {
        if (classModel != null && classModel.subjects != null
                && subjectIndex >= 0 && subjectIndex < classModel.subjects.size()) {
            Subject subject = classModel.subjects.get(subjectIndex);
            return subject != null ? subject.name : null;
        }
        return null;
    }

    private void ensureSemester() {
        if (SessionContext.selectedSemester == null && classModel.semesterId != null
                && !classModel.semesterId.isEmpty()) {
            com.kartik.myschool.model.Semester fallbackSem = new com.kartik.myschool.model.Semester();
            fallbackSem.id = classModel.semesterId;
            fallbackSem.yearId = classModel.yearId;
            fallbackSem.number = classModel.semesterId.contains("2") ? 2 : 1;
            fallbackSem.name = fallbackSem.number == 2 ? "Second Semester" : "First Semester";
            SessionContext.selectedSemester = fallbackSem;
        }
    }

    private void bindHeader() {
        String name = student.name != null ? student.name : "Student";
        String roll = student.rollNo != null ? student.rollNo : "-";
        
        // Top line: Class
        String classContext = "<b>Class:</b> " + classModel.getDisplayName();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            b.tvSubjectContext.setText(android.text.Html.fromHtml(classContext, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            b.tvSubjectContext.setText(android.text.Html.fromHtml(classContext));
        }

        // Bottom line: Student Name, Roll No, and Subject Name
        String studentDetails = "<b>" + name + "</b> (Roll: " + roll + ") &nbsp;&nbsp;•&nbsp;&nbsp; <b>" + subjectName + "</b>";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            b.tvStudentName.setText(android.text.Html.fromHtml(studentDetails, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            b.tvStudentName.setText(android.text.Html.fromHtml(studentDetails));
        }
    }

    private void loadExistingSelection() {
        MarksRecord.SubjectMarksDetail detail = getCurrentSubjectDetail(false);
        if (detail == null || detail.remark == null || detail.remark.trim().isEmpty()) {
            return;
        }
        for (String part : detail.remark.split("\\|\\|")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                selectedRemarks.add(trimmed);
            }
        }
    }

    private void loadRemarkBank() {
        String schoolId = classModel.schoolId != null ? classModel.schoolId
                : SessionContext.selectedSchool != null ? SessionContext.selectedSchool.id : null;
        List<String> cached = AppCache.cachedRemarkBank.get(subjectName);
        if (cached != null && !cached.isEmpty()) {
            bankOptions.clear();
            bankOptions.addAll(cached);
            renderSelection();
            return;
        }

        FirebaseRepository.get().getRemarkBank(schoolId, subjectName,
                new FirebaseRepository.OnResult<List<String>>() {
                    @Override
                    public void onSuccess(List<String> options) {
                        bankOptions.clear();
                        if (options != null) {
                            bankOptions.addAll(options);
                            AppCache.cachedRemarkBank.put(subjectName, new ArrayList<>(options));
                        }
                        renderSelection();
                    }

                    @Override
                    public void onError(Exception e) {
                        bankOptions.clear();
                        bankOptions.addAll(com.kartik.myschool.model.RemarkBank.defaultOptionsFor(subjectName));
                        renderSelection();
                    }
                });
    }

    private void showRemarkDialog() {
        if (bankOptions.isEmpty()) {
            bankOptions.addAll(com.kartik.myschool.model.RemarkBank.defaultOptionsFor(subjectName));
        }
        String[] labels = bankOptions.toArray(new String[0]);
        boolean[] checked = new boolean[labels.length];
        for (int i = 0; i < labels.length; i++) {
            checked[i] = selectedRemarks.contains(labels[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose remarks")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedRemarks.add(labels[which]);
                    } else {
                        selectedRemarks.remove(labels[which]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Done", (dialog, which) -> renderSelection())
                .show();
    }

    private void renderSelection() {
        b.cgSelectedRemarks.removeAllViews();
        if (selectedRemarks.isEmpty()) {
            b.tvSelectedSummary.setText("No remarks selected");
        } else {
            b.tvSelectedSummary.setText(selectedRemarks.size() + " remark(s) selected");
        }

        for (String remark : selectedRemarks) {
            Chip chip = new Chip(this);
            chip.setText(remark);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                selectedRemarks.remove(remark);
                renderSelection();
            });
            chip.setEnsureMinTouchTargetSize(false);
            b.cgSelectedRemarks.addView(chip);
        }
    }

    private void saveSubjectRemark() {
        String custom = b.etCustomRemark.getText() != null ? b.etCustomRemark.getText().toString().trim() : "";
        LinkedHashSet<String> finalRemarks = new LinkedHashSet<>(selectedRemarks);
        if (!custom.isEmpty()) {
            finalRemarks.add(custom);
        }

        MarksRecord record = marksRecord != null ? marksRecord : new MarksRecord();
        record.studentId = student.id;
        record.classId = classModel.id;
        record.examName = classModel.examName;
        if (record.subjectMarks == null) record.subjectMarks = new java.util.HashMap<>();
        if (record.subjectMax == null) record.subjectMax = new java.util.HashMap<>();
        if (record.detailedMarks == null) record.detailedMarks = new java.util.HashMap<>();

        if (SessionContext.selectedSemester != null && SessionContext.selectedSemester.id != null) {
            record.semesterId = SessionContext.selectedSemester.id;
            record.semesterNumber = String.valueOf(SessionContext.selectedSemester.number);
        } else {
            record.semesterId = classModel.semesterId != null ? classModel.semesterId : "sem_1";
            record.semesterNumber = record.semesterId.contains("2") ? "2" : "1";
        }

        MarksRecord.SubjectMarksDetail detail = getCurrentSubjectDetail(true, record);
        detail.remark = joinRemarks(finalRemarks);
        String safeKey = MarksRecord.sanitizeKey(subjectName);
        record.detailedMarks.put(safeKey, detail);

        showLoading(true);
        FirebaseRepository.get().saveMarks(record, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String id) {
                record.id = id;
                marksRecord = record;
                AppCache.selectedMarks = record;
                getSharedPreferences("marks_doc_ids", MODE_PRIVATE)
                        .edit()
                        .putString(getMarksDocPrefKey(record), id)
                        .putString("marks_doc_" + record.studentId + "_" + record.classId, id)
                        .apply();

                AppCache.descriptiveJustSaved = true;
                AppCache.descriptiveJustSavedStudentId = student.id;
                AppCache.descriptiveJustSavedRecord = record;
                syncCaches(record);
                FirebaseRepository.get().updateMarksInCache(record.classId, record.semesterId, student.id, record);

                showLoading(false);
                Toast.makeText(SubjectRemarkEntryActivity.this, "Remark saved successfully.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(SubjectRemarkEntryActivity.this,
                        "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private MarksRecord.SubjectMarksDetail getCurrentSubjectDetail(boolean create) {
        return getCurrentSubjectDetail(create, marksRecord);
    }

    private MarksRecord.SubjectMarksDetail getCurrentSubjectDetail(boolean create, MarksRecord record) {
        if (record == null || record.detailedMarks == null) {
            return create ? newDetailForSubject() : null;
        }

        String safeKey = MarksRecord.sanitizeKey(subjectName);
        MarksRecord.SubjectMarksDetail detail = record.detailedMarks.get(safeKey);
        if (detail != null) return detail;

        detail = record.detailedMarks.get(subjectName);
        if (detail != null) return detail;

        for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : record.detailedMarks.entrySet()) {
            if (entry.getKey() != null && Objects.equals(MarksRecord.sanitizeKey(entry.getKey()), safeKey)) {
                return entry.getValue();
            }
        }
        return create ? newDetailForSubject() : null;
    }

    private MarksRecord.SubjectMarksDetail newDetailForSubject() {
        MarksRecord.SubjectMarksDetail detail = new MarksRecord.SubjectMarksDetail();
        if (classModel.subjects != null && subjectIndex >= 0 && subjectIndex < classModel.subjects.size()) {
            Subject subject = classModel.subjects.get(subjectIndex);
            if (subject != null) {
                detail.maxMarks = subject.maxMarks;
            }
        }
        return detail;
    }

    private String joinRemarks(LinkedHashSet<String> remarks) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String remark : remarks) {
            if (i > 0) sb.append("||");
            sb.append(remark);
            i++;
        }
        return sb.toString();
    }

    private String getMarksDocPrefKey(MarksRecord record) {
        return "marks_doc_" + record.studentId + "_" + record.classId + "_" + record.semesterId;
    }

    private void syncCaches(MarksRecord record) {
        if (AppCache.cachedDescriptiveMarksMap == null
                || !Objects.equals(record.classId, AppCache.cachedDescriptiveClassId)
                || !Objects.equals(record.semesterId, AppCache.cachedDescriptiveSemesterId)) {
            AppCache.cachedDescriptiveMarksMap = new java.util.HashMap<>();
            AppCache.cachedDescriptiveClassId = record.classId;
            AppCache.cachedDescriptiveSemesterId = record.semesterId;
            AppCache.cachedDescriptiveMarksComplete = true;
        }
        AppCache.cachedDescriptiveMarksMap.put(student.id, record);

        if (AppCache.cachedMarksMap == null) {
            AppCache.cachedMarksMap = new java.util.HashMap<>();
        }
        AppCache.cachedMarksMap.put(student.id, record);
        AppCache.cachedClassIdForStudents = record.classId;
        AppCache.cachedSemesterIdForMarks = record.semesterId;
    }

    private void showLoading(boolean show) {
        b.progress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSaveRemark.setEnabled(!show);
        b.btnChooseRemarks.setEnabled(!show);
    }
}
