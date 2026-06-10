package com.kartik.myschool;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kartik.myschool.databinding.ActivityEnterDescriptiveBinding;
import com.kartik.myschool.databinding.ItemSubjectRemarkRowBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnterDescriptiveActivity extends AppCompatActivity {

    private ActivityEnterDescriptiveBinding b;
    private final List<ItemSubjectRemarkRowBinding> remarkRows = new ArrayList<>();
    private Student student;
    private ClassModel classModel;
    private MarksRecord existingMarks;

    private final String[] STANDARD_REMARKS = {
            "अभ्यासात हुशार आहे.",
            "खेळात आवड आहे.",
            "नियमित शाळेत येतो.",
            "अक्षर सुंदर आहे.",
            "संभाषण कौशल्य उत्तम आहे.",
            "मित्रांशी मिळूनमिसळून वागतो.",
            "वाचनाची आवड आहे.",
            "गणितात प्रगती करावी.",
            "अक्षर सुधारण्याची गरज आहे.",
            "शांत व संयमी स्वभाव."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ClassModel callerClass = AppCache.selectedClass;
        SessionContext.load(this);
        super.onCreate(savedInstanceState);
        b = ActivityEnterDescriptiveBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        student = AppCache.selectedStudent;
        classModel = AppCache.selectedClass;
        if (classModel == null)
            classModel = callerClass;

        if (callerClass != null && callerClass.subjects != null && !callerClass.subjects.isEmpty()
                && classModel != null) {
            classModel.subjects = callerClass.subjects;
        }

        AppCache.selectedClass = classModel;
        if (SessionContext.selectedClass != null && classModel != null && classModel.subjects != null) {
            SessionContext.selectedClass.subjects = classModel.subjects;
        }

        if (student == null || classModel == null) {
            finish();
            return;
        }

        if (SessionContext.selectedSemester == null && classModel.semesterId != null
                && !classModel.semesterId.isEmpty()) {
            com.kartik.myschool.model.Semester fallbackSem = new com.kartik.myschool.model.Semester();
            fallbackSem.id = classModel.semesterId;
            fallbackSem.yearId = classModel.yearId;
            fallbackSem.number = classModel.semesterId.contains("2") ? 2 : 1;
            fallbackSem.name = classModel.semesterId.contains("2") ? "Second Semester" : "First Semester";
            SessionContext.selectedSemester = fallbackSem;
        }

        b.tvMarksStudentName.setText(student.name);
        b.tvMarksRollClass.setText("Roll: " + student.rollNo + " | " + classModel.getDisplayName());

        if (classModel.subjects == null)
            classModel.subjects = new ArrayList<>();
        for (Subject sub : classModel.subjects) {
            addRemarkRow(sub);
        }

        loadExistingData();

        b.btnSaveRemarks.setOnClickListener(v -> saveRemarks());
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_help_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(this, "enter_descriptive");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadExistingData() {
        // Layer 1: AppCache — only use if student + class match AND record has an id
        // (id = it was loaded from Firestore, not a locally-constructed object)
        if (AppCache.selectedMarks != null
                && AppCache.selectedMarks.id != null
                && student.id != null
                && student.id.equals(AppCache.selectedMarks.studentId)
                && classModel.id != null
                && classModel.id.equals(AppCache.selectedMarks.classId)) {
            existingMarks = AppCache.selectedMarks;
            Log.d("DESC_LOAD", "Layer1 HIT: using AppCache.selectedMarks id=" + existingMarks.id);
            fillExistingRemarks(existingMarks);
            // Still run Layer 2/3 in background to catch any newer version
        } else {
            Log.d("DESC_LOAD", "Layer1 MISS: AppCache.selectedMarks=" +
                    (AppCache.selectedMarks == null ? "null" : "wrong student/class or no id"));
        }

        // Layer 2: SharedPreferences doc ID — fastest Firestore path, survives app
        // restart
        String prefKey = getMarksDocPrefKey();
        String legacyPrefKey = "marks_doc_" + student.id + "_" + classModel.id;
        android.content.SharedPreferences docPrefs = getSharedPreferences("marks_doc_ids", MODE_PRIVATE);
        String storedDocId = docPrefs.getString(prefKey, null);
        if (storedDocId == null) {
            storedDocId = docPrefs.getString(legacyPrefKey, null);
        }
        final String finalStoredDocId = storedDocId;
        if (storedDocId != null) {
            Log.d("DESC_LOAD", "Layer2: fetching by stored docId=" + storedDocId);
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("marks")
                    .document(finalStoredDocId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            MarksRecord m = doc.toObject(MarksRecord.class);
                            if (m != null) {
                                if (m.id == null || m.id.isEmpty()) {
                                    m.id = doc.getId();
                                }
                                if (!isRecordForCurrentSelection(m)) {
                                    Log.d("DESC_LOAD",
                                            "Layer2: stored doc belongs to a different student/class/semester");
                                    runLayer3();
                                    return;
                                }
                                // Only replace if this is a newer version than what we have
                                if (existingMarks == null || m.updatedAt >= existingMarks.updatedAt) {
                                    Log.d("DESC_LOAD", "Layer2 SUCCESS: loaded docId=" + finalStoredDocId
                                            + " updatedAt=" + m.updatedAt);
                                    existingMarks = m;
                                    AppCache.selectedMarks = m;
                                    docPrefs.edit().putString(prefKey, m.id).apply();
                                    fillExistingRemarks(m);
                                }
                            }
                        } else {
                            Log.d("DESC_LOAD", "Layer2: doc not found for id=" + finalStoredDocId
                                    + " — falling through to Layer3 query");
                            runLayer3();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DESC_LOAD", "Layer2 FAILED: " + e.getMessage());
                        runLayer3();
                    });
        } else {
            // No stored doc ID — must query
            Log.d("DESC_LOAD", "Layer2: no stored docId — going to Layer3 query");
            runLayer3();
        }
    }

    private void runLayer3() {
        String semId = getActiveSemesterId();
        Log.d("DESC_LOAD", "Layer3: Firestore query student=" + student.id
                + " class=" + classModel.id + " sem=" + semId);
        FirebaseRepository.get().getMarksForStudentAndSemester(
                student.id, classModel.id, semId,
                new FirebaseRepository.OnResult<MarksRecord>() {
                    @Override
                    public void onSuccess(MarksRecord m) {
                        if (m != null) {
                            if (existingMarks == null || m.updatedAt >= existingMarks.updatedAt) {
                                Log.d("DESC_LOAD", "Layer3 SUCCESS: docId=" + m.id
                                        + " updatedAt=" + m.updatedAt);
                                existingMarks = m;
                                AppCache.selectedMarks = m;
                                // Persist the doc ID so Layer2 works on next open
                                if (m.id != null) {
                                    getSharedPreferences("marks_doc_ids", MODE_PRIVATE)
                                            .edit()
                                            .putString(getMarksDocPrefKey(), m.id)
                                            .putString("marks_doc_" + m.studentId + "_" + m.classId, m.id)
                                            .apply();
                                }
                                fillExistingRemarks(m);
                            }
                        } else {
                            Log.d("DESC_LOAD", "Layer3: no marks found for student=" + student.id);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("DESC_LOAD", "Layer3 FAILED: " + e.getMessage());
                    }
                });
    }

    private String getActiveSemesterId() {
        return SessionContext.selectedSemester != null && SessionContext.selectedSemester.id != null
                ? SessionContext.selectedSemester.id
                : "sem_1";
    }

    private String getMarksDocPrefKey() {
        return "marks_doc_" + student.id + "_" + classModel.id + "_" + getActiveSemesterId();
    }

    private boolean isRecordForCurrentSelection(MarksRecord record) {
        if (record == null || student == null || classModel == null)
            return false;
        if (student.id == null || !student.id.equals(record.studentId))
            return false;
        if (classModel.id == null || !classModel.id.equals(record.classId))
            return false;
        String semId = getActiveSemesterId();
        return record.semesterId == null || record.semesterId.isEmpty() || semId.equals(record.semesterId);
    }

    private void addRemarkRow(Subject sub) {
        ItemSubjectRemarkRowBinding row = ItemSubjectRemarkRowBinding.inflate(LayoutInflater.from(this), b.llRemarkRows,
                false);
        row.tvSubjectName.setText(sub.name);

        // Hide "Select" button as we are showing the choices directly for fast action!
        row.btnAddRemark.setVisibility(View.GONE);
        row.tvNoRemarks.setVisibility(View.GONE);
        row.cgSelectedRemarks.setVisibility(View.VISIBLE);

        // Populate with all standard remarks as choice chips
        for (String remark : STANDARD_REMARKS) {
            Chip chip = new Chip(this);
            chip.setText(remark);
            chip.setCheckable(true);
            chip.setChecked(false);

            styleInteractiveChip(chip, false);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                styleInteractiveChip(chip, isChecked);
                updateSummaryText(row);
            });

            row.cgSelectedRemarks.addView(chip);
        }

        b.llRemarkRows.addView(row.getRoot());
        remarkRows.add(row);
    }

    private void updateSummaryText(com.kartik.myschool.databinding.ItemSubjectRemarkRowBinding row) {
        List<String> selected = getSelectedRemarksFromChips(row);
        if (selected.isEmpty()) {
            row.tvSummary.setVisibility(android.view.View.GONE);
        } else {
            row.tvSummary.setVisibility(android.view.View.VISIBLE);
            row.tvSummary.setText(selected.size() + " selected");
        }
    }

    private void styleInteractiveChip(Chip chip, boolean checked) {
        float density = getResources().getDisplayMetrics().density;
        chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
        chip.setChipMinHeight((int) (32 * density));

        if (checked) {
            // Checked State: Filled purple/primary with white text
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(0xFF6C4CCF)); // Primary purple
            chip.setTextColor(android.graphics.Color.WHITE);
            chip.setChipStrokeWidth(0f);
            chip.setCheckedIconVisible(true);
            chip.setCheckedIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        } else {
            // Unchecked State: Outlined with primary purple and transparent background
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            chip.setTextColor(0xFF6C4CCF);
            chip.setChipStrokeWidth(1.2f * density);
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0xFFD1C4E9)); // Light purple outline
            chip.setCheckedIconVisible(false);
        }
    }

    private List<String> getSelectedRemarksFromChips(ItemSubjectRemarkRowBinding row) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < row.cgSelectedRemarks.getChildCount(); i++) {
            View child = row.cgSelectedRemarks.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    list.add(chip.getText().toString());
                }
            }
        }
        return list;
    }

    private void fillExistingRemarks(MarksRecord m) {
        if (classModel.subjects == null)
            return;
        for (int i = 0; i < classModel.subjects.size() && i < remarkRows.size(); i++) {
            String subName = MarksRecord.sanitizeKey(classModel.subjects.get(i).name);
            ItemSubjectRemarkRowBinding row = remarkRows.get(i);
            resetRemarkRow(row);

            if (m.detailedMarks != null && m.detailedMarks.containsKey(subName)) {
                MarksRecord.SubjectMarksDetail d = m.detailedMarks.get(subName);
                if (d != null && d.remark != null && !d.remark.isEmpty()) {
                    List<String> parsedRemarks = Arrays.asList(d.remark.split("\\|\\|"));

                    // Match standard remarks first
                    List<String> remainingRemarks = new ArrayList<>();
                    for (String r : parsedRemarks) {
                        String trimmed = r.trim();
                        if (!trimmed.isEmpty()) {
                            remainingRemarks.add(trimmed);
                        }
                    }

                    for (int c = 0; c < row.cgSelectedRemarks.getChildCount(); c++) {
                        View child = row.cgSelectedRemarks.getChildAt(c);
                        if (child instanceof Chip) {
                            Chip chip = (Chip) child;
                            String text = chip.getText().toString();
                            if (remainingRemarks.contains(text)) {
                                chip.setChecked(true);
                                remainingRemarks.remove(text);
                            }
                        }
                    }

                    // Any remaining remarks are custom! Add them as checked chips.
                    for (String customRemark : remainingRemarks) {
                        Chip chip = new Chip(this);
                        chip.setText(customRemark);
                        chip.setCheckable(true);
                        chip.setChecked(true);
                        chip.setTag("custom_remark");
                        styleInteractiveChip(chip, true);
                        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            styleInteractiveChip(chip, isChecked);
                            updateSummaryText(row);
                        });
                        row.cgSelectedRemarks.addView(chip);
                    }
                }
            }
            updateSummaryText(row);
        }
    }

    private void resetRemarkRow(ItemSubjectRemarkRowBinding row) {
        for (int c = row.cgSelectedRemarks.getChildCount() - 1; c >= 0; c--) {
            View child = row.cgSelectedRemarks.getChildAt(c);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if ("custom_remark".equals(chip.getTag())) {
                    row.cgSelectedRemarks.removeViewAt(c);
                } else {
                    chip.setChecked(false);
                    styleInteractiveChip(chip, false);
                }
            }
        }
    }

    private void saveRemarks() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.msg_empty, Toast.LENGTH_LONG).show();
            return;
        }

        MarksRecord m = existingMarks != null ? existingMarks : new MarksRecord();
        m.studentId = student.id;
        m.classId = classModel.id;
        m.examName = classModel.examName;

        if (m.subjectMarks == null)
            m.subjectMarks = new java.util.HashMap<>();
        if (m.subjectMax == null)
            m.subjectMax = new java.util.HashMap<>();
        if (m.detailedMarks == null)
            m.detailedMarks = new java.util.HashMap<>();

        if (SessionContext.selectedSemester != null && SessionContext.selectedSemester.id != null) {
            m.semesterId = SessionContext.selectedSemester.id;
            m.semesterNumber = String.valueOf(SessionContext.selectedSemester.number);
        } else {
            m.semesterId = "sem_1";
            m.semesterNumber = "1";
        }

        for (int i = 0; i < classModel.subjects.size() && i < remarkRows.size(); i++) {
            Subject sub = classModel.subjects.get(i);
            ItemSubjectRemarkRowBinding row = remarkRows.get(i);

            String safeKey = MarksRecord.sanitizeKey(sub.name);
            MarksRecord.SubjectMarksDetail d = m.detailedMarks.get(safeKey);
            if (d == null) {
                d = new MarksRecord.SubjectMarksDetail();
                d.maxMarks = sub.maxMarks;
            }

            List<String> selected = getSelectedRemarksFromChips(row);
            // Join with a special delimiter to avoid comma issues
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < selected.size(); j++) {
                sb.append(selected.get(j));
                if (j < selected.size() - 1)
                    sb.append("||");
            }
            d.remark = sb.toString();

            m.detailedMarks.put(safeKey, d);
        }

        showLoading(true);
        FirebaseRepository.get().saveMarks(m, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String id) {
                m.id = id;
                AppCache.selectedMarks = m;

                // Persist doc ID to SharedPreferences — both keys so both activities find it
                String prefKey = getMarksDocPrefKey();
                String legacyPrefKey = "marks_doc_" + m.studentId + "_" + m.classId;
                getSharedPreferences("marks_doc_ids", MODE_PRIVATE)
                        .edit()
                        .putString(prefKey, id)
                        .putString(legacyPrefKey, id)
                        .apply();
                Log.d("SAVE_REMARKS", "Saved docId=" + id + " key=" + prefKey);

                // ── Signal DescriptiveEntriesFragment to instant-patch this student's card ──
                AppCache.descriptiveJustSaved = true;
                AppCache.descriptiveJustSavedStudentId = student.id;
                AppCache.descriptiveJustSavedRecord = m;

                // Also keep descriptive + formative caches in sync
                if (AppCache.cachedDescriptiveMarksMap == null
                        || !java.util.Objects.equals(classModel.id, AppCache.cachedDescriptiveClassId)
                        || !java.util.Objects.equals(m.semesterId, AppCache.cachedDescriptiveSemesterId)) {
                    AppCache.cachedDescriptiveMarksMap = new java.util.HashMap<>();
                    AppCache.cachedDescriptiveClassId = classModel.id;
                    AppCache.cachedDescriptiveSemesterId = m.semesterId;
                    AppCache.cachedDescriptiveMarksComplete = true;
                }
                AppCache.cachedDescriptiveMarksMap.put(student.id, m);

                if (AppCache.cachedMarksMap == null)
                    AppCache.cachedMarksMap = new java.util.HashMap<>();
                AppCache.cachedMarksMap.put(student.id, m);
                AppCache.cachedClassIdForStudents = classModel.id;
                AppCache.cachedSemesterIdForMarks = m.semesterId;

                // Patch the repo's internal marks cache too
                FirebaseRepository.get().updateMarksInCache(classModel.id, m.semesterId, student.id, m);

                showLoading(false);
                Toast.makeText(EnterDescriptiveActivity.this,
                        R.string.msg_remarks_saved_successfully, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(Exception e) {
                Log.e("SAVE_REMARKS", "FAILED: " + e.getMessage(), e);
                showLoading(false);
                Toast.makeText(EnterDescriptiveActivity.this,
                        "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        b.marksProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSaveRemarks.setEnabled(!show);
    }
}
