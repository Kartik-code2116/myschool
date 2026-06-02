package com.example.myschool;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myschool.databinding.ActivityEnterMarksBinding;
import com.example.myschool.databinding.ItemSubjectMarksRowBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.Student;
import com.example.myschool.model.Subject;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.GradeCalculator;
import com.example.myschool.utils.OcrHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EnterMarksActivity extends AppCompatActivity {

    private ActivityEnterMarksBinding b;
    private final List<ItemSubjectMarksRowBinding> marksRows = new ArrayList<>();
    private OcrHelper ocrHelper;
    private Student student;
    private ClassModel classModel;
    private MarksRecord existingMarks;

    // ── Per-subject max-mark breakdowns (computed once in addMarksRow) ─────────
    // Each array: [akarikMax, sanklitMax, nirikhshan, tondiKam, pratyakshik,
    // upkram, prakalp, chachani, swadhyay, itar, tondiB, pratyakshikB, lekhi]
    private final List<int[]> subjectMaxBreakdown = new ArrayList<>();

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap photo = (Bitmap) extras.get("data");
                        processOcr(photo);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        processOcr(bmp);
                    } catch (IOException e) {
                        Toast.makeText(this, R.string.msg_failed_to_load_image, Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // FIX: Capture AppCache.selectedClass set by caller (openMarksEntry) BEFORE
        // SessionContext.load() overwrites it with potentially stale SharedPrefs data.
        ClassModel callerClass = AppCache.selectedClass;

        SessionContext.load(this);
        super.onCreate(savedInstanceState);
        b = ActivityEnterMarksBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        ocrHelper = new OcrHelper();
        student = AppCache.selectedStudent;

        // FIX: Build classModel by merging caller (in-memory, freshest) with
        // SharedPrefs.
        // Priority: callerClass subjects > SharedPrefs subjects > hardcoded fallback.
        // This ensures subject toggle changes always appear immediately in marks entry.
        classModel = AppCache.selectedClass; // from SessionContext.load()
        if (classModel == null)
            classModel = callerClass;

        // If same class but callerClass has fresher subject list — use it.
        if (callerClass != null
                && callerClass.subjects != null
                && !callerClass.subjects.isEmpty()
                && classModel != null) {
            // callerClass was set by openMarksEntry from SessionContext.selectedClass
            // just before starting this activity — it is always the freshest.
            classModel.subjects = callerClass.subjects;
            Log.d("ENTER_MARKS", "Using callerClass subjects: " + callerClass.subjects.size() + " subjects");
        }

        // Also keep AppCache and SessionContext in sync
        AppCache.selectedClass = classModel;
        if (SessionContext.selectedClass != null && classModel != null
                && classModel.subjects != null) {
            SessionContext.selectedClass.subjects = classModel.subjects;
        }

        if (student == null || classModel == null) {
            finish();
            return;
        }

        // Fallback for null selectedSemester using classModel's semesterId
        if (SessionContext.selectedSemester == null && classModel.semesterId != null
                && !classModel.semesterId.isEmpty()) {
            com.example.myschool.model.Semester fallbackSem = new com.example.myschool.model.Semester();
            fallbackSem.id = classModel.semesterId;
            fallbackSem.yearId = classModel.yearId;
            fallbackSem.number = classModel.semesterId.contains("2") ? 2 : 1;
            fallbackSem.name = classModel.semesterId.contains("2") ? "Second Semester" : "First Semester";
            SessionContext.selectedSemester = fallbackSem;
        }

        b.tvMarksStudentName.setText(student.name);
        b.tvMarksRollClass.setText("Roll: " + student.rollNo + " | " + classModel.getDisplayName());

        // Build one row per subject.
        if (classModel.subjects == null) {
            classModel.subjects = new ArrayList<>();
        }
        for (Subject sub : classModel.subjects) {
            addMarksRow(sub);
        }

        // ── Load existing marks (3-layer strategy) ───────────────────────────
        // Layer 1: AppCache (same session, instant — already in RAM)
        if (AppCache.selectedMarks != null
                && student.id != null
                && student.id.equals(AppCache.selectedMarks.studentId)
                && classModel.id != null
                && classModel.id.equals(AppCache.selectedMarks.classId)) {
            Log.d("SAVE_MARKS", "Layer1: Using AppCache.selectedMarks student=" + student.id);
            existingMarks = AppCache.selectedMarks;
            fillExistingMarks(existingMarks);
        }

        // Layer 2: SharedPreferences (after app restart — fetch by stored doc ID)
        String semId = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.id : "sem_1";
        String prefKey = "marks_doc_" + student.id + "_" + classModel.id + "_" + semId;
        String legacyPrefKey = "marks_doc_" + student.id + "_" + classModel.id;
        android.content.SharedPreferences docPrefs = getSharedPreferences("marks_doc_ids", MODE_PRIVATE);
        String storedDocId = docPrefs.getString(prefKey, null);
        if (storedDocId == null) {
            storedDocId = docPrefs.getString(legacyPrefKey, null);
        }
        final String finalStoredDocId = storedDocId;
        if (finalStoredDocId != null && existingMarks == null) {
            Log.d("SAVE_MARKS", "Layer2: Fetching marks by stored docId=" + finalStoredDocId);
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("marks")
                    .document(finalStoredDocId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            MarksRecord m = doc.toObject(MarksRecord.class);
                            if (m != null) {
                                Log.d("SAVE_MARKS", "Layer2 SUCCESS: loaded marks docId=" + finalStoredDocId);
                                existingMarks = m;
                                AppCache.selectedMarks = m;
                                fillExistingMarks(m);
                            }
                        } else {
                            Log.d("SAVE_MARKS", "Layer2: doc not found for id=" + finalStoredDocId);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("SAVE_MARKS", "Layer2 FAILED: " + e.getMessage(), e));
        }

        // Layer 3: Firestore query by studentId+classId (always runs in background for
        // freshness)
        Log.d("SAVE_MARKS",
                "Layer3: Firestore query student=" + student.id + " class=" + classModel.id + " sem=" + semId);
        FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classModel.id, semId,
                new FirebaseRepository.OnResult<MarksRecord>() {
                    @Override
                    public void onSuccess(MarksRecord m) {
                        if (m != null) {
                            Log.d("SAVE_MARKS", "Layer3 SUCCESS: marks docId=" + m.id + " semId=" + m.semesterId);
                            existingMarks = m;
                            AppCache.selectedMarks = m;
                            fillExistingMarks(m);
                        } else {
                            Log.d("SAVE_MARKS", "Layer3: No marks in Firestore for student=" + student.id);
                            if (existingMarks == null) {
                                // No marks at all — pre-fill attendance
                                int[] att = calculateAttendanceForSemester();
                                if (att[1] > 0) {
                                    if (b.etPresentDays != null)
                                        b.etPresentDays.setText(String.valueOf(att[0]));
                                    if (b.etTotalDays != null)
                                        b.etTotalDays.setText(String.valueOf(att[1]));
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("SAVE_MARKS", "Layer3 FAILED: " + e.getMessage(), e);
                        if (existingMarks == null) {
                            Toast.makeText(EnterMarksActivity.this,
                                    "गुण लोड करताना त्रुटी: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        b.btnScanMarksheet.setOnClickListener(v -> showScanOptions());
        b.btnSaveMarks.setOnClickListener(v -> saveMarks());
    }

    // ── Row builder ────────────────────────────────────────────────────────────
    private void addMarksRow(Subject sub) {
        ItemSubjectMarksRowBinding row = ItemSubjectMarksRowBinding.inflate(
                LayoutInflater.from(this), b.llMarksRows, false);

        row.tvSubjectName.setText(sub.name);

        int maxMarks = sub.maxMarks;
        int nirikhshanMax = sub.maxNirikhshan;
        int tondiKamMax = sub.maxTondiKam;
        int pratyakshikAMax = sub.maxPratyakshik;
        int upkramMax = sub.maxUpkram;
        int prakalpMax = sub.maxPrakalp;
        int chachaniMax = sub.maxChachani;
        int swadhyayMax = sub.maxSwadhyay;
        int itarMax = sub.maxItar;

        int tondiBMax = sub.maxTondi;
        int pratyakshikBMax = sub.maxPratyakshikB;
        int lekhiMax = sub.maxLekhi;

        // Fallback to standard 50-50 scaling breakdown if sub-fields are not
        // initialized
        if (nirikhshanMax == 0 && tondiKamMax == 0 && pratyakshikAMax == 0 &&
                upkramMax == 0 && prakalpMax == 0 && chachaniMax == 0 && swadhyayMax == 0 &&
                tondiBMax == 0 && pratyakshikBMax == 0 && lekhiMax == 0) {

            int akarikMax = maxMarks / 2;
            int sanklitMax = maxMarks - akarikMax;

            nirikhshanMax = akarikMax * 10 / 50;
            tondiKamMax = akarikMax * 10 / 50;
            pratyakshikAMax = akarikMax * 10 / 50;
            upkramMax = akarikMax * 5 / 50;
            prakalpMax = akarikMax * 5 / 50;
            chachaniMax = akarikMax * 5 / 50;
            swadhyayMax = akarikMax * 5 / 50;
            itarMax = akarikMax - nirikhshanMax - tondiKamMax - pratyakshikAMax
                    - upkramMax - prakalpMax - chachaniMax - swadhyayMax;

            tondiBMax = sanklitMax * 10 / 50;
            pratyakshikBMax = sanklitMax * 10 / 50;
            lekhiMax = sanklitMax - tondiBMax - pratyakshikBMax;
        }

        int akarikMax = nirikhshanMax + tondiKamMax + pratyakshikAMax + upkramMax + prakalpMax + chachaniMax
                + swadhyayMax + itarMax;
        int sanklitMax = tondiBMax + pratyakshikBMax + lekhiMax;
        final int finalMaxMarks = akarikMax + sanklitMax;

        // Store so updateSubjectHeader can read them without re-computing
        int[] mx = { akarikMax, sanklitMax,
                nirikhshanMax, tondiKamMax, pratyakshikAMax,
                upkramMax, prakalpMax, chachaniMax, swadhyayMax, itarMax,
                tondiBMax, pratyakshikBMax, lekhiMax };
        subjectMaxBreakdown.add(mx);

        // ── Set badge labels ───────────────────────────────────────────────────
        row.tvAkarikMaxBadge.setText(getString(R.string.label_out_of) + akarikMax);
        row.tvSanklitMaxBadge.setText(getString(R.string.label_out_of) + sanklitMax);

        // ── Set individual sub-field max labels (shown as "/10", "/5" etc.) ───
        row.tvNirikhshanMax.setText("/" + nirikhshanMax);
        row.tvTondiKamMax.setText("/" + tondiKamMax);
        row.tvPratyakshikAMax.setText("/" + pratyakshikAMax);
        row.tvUpkramMax.setText("/" + upkramMax);
        row.tvPrakalpMax.setText("/" + prakalpMax);
        row.tvChachaniMax.setText("/" + chachaniMax);
        row.tvSwadhyayMax.setText("/" + swadhyayMax);
        row.tvItarMax.setText(itarMax > 0 ? "/" + itarMax : "/—");
        row.tvTondiBMax.setText("/" + tondiBMax);
        row.tvPratyakshikBMax.setText("/" + pratyakshikBMax);
        row.tvLekhiMax.setText("/" + lekhiMax);

        // Max marks validation to prevent typing marks > max
        setupMaxMarksValidation(row.etNirikhshan, nirikhshanMax);
        setupMaxMarksValidation(row.etTondiKam, tondiKamMax);
        setupMaxMarksValidation(row.etPratyakshik, pratyakshikAMax);
        setupMaxMarksValidation(row.etUpkram, upkramMax);
        setupMaxMarksValidation(row.etPrakalp, prakalpMax);
        setupMaxMarksValidation(row.etChachani, chachaniMax);
        setupMaxMarksValidation(row.etSwadhyay, swadhyayMax);
        setupMaxMarksValidation(row.etItar, itarMax);
        setupMaxMarksValidation(row.etTondiB, tondiBMax);
        setupMaxMarksValidation(row.etPratyakshikB, pratyakshikBMax);
        setupMaxMarksValidation(row.etLekhi, lekhiMax);

        // ── Initial display of totals ──────────────────────────────────────────
        int rowIdx = marksRows.size();
        updateRow(row, mx, finalMaxMarks);

        // ── Expand / collapse on header tap ───────────────────────────────────
        row.layoutHeader.setOnClickListener(v -> {
            boolean open = row.layoutDetails.getVisibility() == View.VISIBLE;
            row.layoutDetails.setVisibility(open ? View.GONE : View.VISIBLE);
            row.ivExpandArrow.setRotation(open ? 0f : 180f);
        });

        // ── Live recalculation on every keystroke ──────────────────────────────
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRow(row, mx, finalMaxMarks);
                recalcAllSubjectsTotals();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        };

        row.etNirikhshan.addTextChangedListener(watcher);
        row.etTondiKam.addTextChangedListener(watcher);
        row.etPratyakshik.addTextChangedListener(watcher);
        row.etUpkram.addTextChangedListener(watcher);
        row.etPrakalp.addTextChangedListener(watcher);
        row.etChachani.addTextChangedListener(watcher);
        row.etSwadhyay.addTextChangedListener(watcher);
        row.etItar.addTextChangedListener(watcher);
        row.etTondiB.addTextChangedListener(watcher);
        row.etPratyakshikB.addTextChangedListener(watcher);
        row.etLekhi.addTextChangedListener(watcher);

        b.llMarksRows.addView(row.getRoot());
        marksRows.add(row);
    }

    /**
     * Updates ALL live displays inside a single subject row:
     * - आकारिक subtotal bar
     * - संकलित subtotal bar
     * - Grand total bar (inside expanded section)
     * - Header chip (always-visible एकूण + श्रेणी)
     */
    private void updateRow(ItemSubjectMarksRowBinding row, int[] mx, int maxMarks) {
        // mx layout: [akarikMax, sanklitMax, n, tk, pt, uk, pk, ch, sw, it, tb, pb, lk]
        int akarikMax = mx[0];
        int sanklitMax = mx[1];

        // Sum आकारिक fields
        int akarikObtained = getInt(row.etNirikhshan) + getInt(row.etTondiKam)
                + getInt(row.etPratyakshik) + getInt(row.etUpkram)
                + getInt(row.etPrakalp) + getInt(row.etChachani)
                + getInt(row.etSwadhyay) + getInt(row.etItar);

        // Sum संकलित fields
        int sanklitObtained = getInt(row.etTondiB) + getInt(row.etPratyakshikB) + getInt(row.etLekhi);

        int grandTotal = akarikObtained + sanklitObtained;

        // ── Subtotal bars inside expanded section ──────────────────────────────
        row.tvAkarikSubTotal.setText(akarikObtained + " / " + akarikMax);
        row.tvSanklitSubTotal.setText(sanklitObtained + " / " + sanklitMax);
        row.tvGrandTotalInDetail.setText(grandTotal + " / " + maxMarks);

        // ── Header chip (always visible, even when collapsed) ──────────────────
        row.tvSubjectTotal.setText(getString(R.string.label_marks_total) + grandTotal + "/" + maxMarks);
        row.tvSubjectGrade.setText(
                getString(R.string.label_marks_grade) + GradeCalculator.getMyschoolGrade(grandTotal, maxMarks));
    }

    // ── Grand total footer card ────────────────────────────────────────────────
    private void recalcAllSubjectsTotals() {
        double total = 0;
        int maxTotal = 0;
        if (classModel.subjects == null)
            return;
        for (int i = 0; i < classModel.subjects.size() && i < marksRows.size(); i++) {
            int maxMarks = classModel.subjects.get(i).maxMarks;
            maxTotal += maxMarks;

            ItemSubjectMarksRowBinding row = marksRows.get(i);
            int akarik = getInt(row.etNirikhshan) + getInt(row.etTondiKam)
                    + getInt(row.etPratyakshik) + getInt(row.etUpkram)
                    + getInt(row.etPrakalp) + getInt(row.etChachani)
                    + getInt(row.etSwadhyay) + getInt(row.etItar);
            int sanklit = getInt(row.etTondiB) + getInt(row.etPratyakshikB) + getInt(row.etLekhi);
            total += akarik + sanklit;
        }
        double pct = GradeCalculator.getPercentage(total, maxTotal);
        b.tvTotalMax.setText(String.valueOf(maxTotal));
        b.tvTotalObtained.setText(formatMark(total));
        b.tvPercentage.setText(String.format("%.2f%%", pct));
        b.tvGrade.setText(getString(R.string.label_marks_grade) + GradeCalculator.getMyschoolGrade(total, maxTotal));
        String result = GradeCalculator.getResult(pct);
        b.tvResult.setText(result);
        b.tvResult.setBackgroundResource("PASS".equals(result)
                ? R.drawable.bg_result_pass
                : R.drawable.bg_result_fail);
        b.tvResult.setTextColor(getResources().getColor(
                "PASS".equals(result) ? R.color.success : R.color.error, null));
    }

    private int[] calculateAttendanceForSemester() {
        int present = 0;
        int total = 0;
        if (student != null && student.monthlyAttendance != null) {
            int semNum = 1;
            if (SessionContext.selectedSemester != null) {
                semNum = SessionContext.selectedSemester.number;
            }

            String[] months;
            if (semNum == 2) {
                months = new String[] { "डिसें", "जाने", "फेब्रु", "मार्च", "एप्रिल", "मे" };
            } else {
                months = new String[] { "जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे" };
            }

            for (String m : months) {
                String val = student.monthlyAttendance.get(m);
                if (val != null && val.contains("/")) {
                    String[] parts = val.split("/");
                    if (parts.length == 2) {
                        try {
                            present += Integer.parseInt(parts[0].trim());
                            total += Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        return new int[] { present, total };
    }

    // ── Fill existing saved marks back into the form ───────────────────────────
    private void fillExistingMarks(MarksRecord m) {
        if (classModel.subjects == null)
            return;

        if (b.etPresentDays != null) {
            if (m.totalDays > 0) {
                b.etPresentDays.setText(String.valueOf(m.presentDays));
            } else {
                int[] att = calculateAttendanceForSemester();
                if (att[1] > 0) {
                    b.etPresentDays.setText(String.valueOf(att[0]));
                } else {
                    b.etPresentDays.setText("");
                }
            }
        }
        if (b.etTotalDays != null) {
            if (m.totalDays > 0) {
                b.etTotalDays.setText(String.valueOf(m.totalDays));
            } else {
                int[] att = calculateAttendanceForSemester();
                if (att[1] > 0) {
                    b.etTotalDays.setText(String.valueOf(att[1]));
                } else {
                    b.etTotalDays.setText("");
                }
            }
        }

        for (int i = 0; i < classModel.subjects.size() && i < marksRows.size(); i++) {
            String subName = MarksRecord.sanitizeKey(classModel.subjects.get(i).name);
            ItemSubjectMarksRowBinding row = marksRows.get(i);

            if (m.detailedMarks != null && m.detailedMarks.containsKey(subName)) {
                MarksRecord.SubjectMarksDetail d = m.detailedMarks.get(subName);
                if (d != null) {
                    if (d.nirikhshan > 0) row.etNirikhshan.setText(String.valueOf(d.nirikhshan));
                    if (d.tondiKam > 0)   row.etTondiKam.setText(String.valueOf(d.tondiKam));
                    if (d.pratyakshik > 0) row.etPratyakshik.setText(String.valueOf(d.pratyakshik));
                    if (d.upkram > 0)     row.etUpkram.setText(String.valueOf(d.upkram));
                    if (d.prakalp > 0)    row.etPrakalp.setText(String.valueOf(d.prakalp));
                    if (d.chachani > 0)   row.etChachani.setText(String.valueOf(d.chachani));
                    if (d.swadhyay > 0)   row.etSwadhyay.setText(String.valueOf(d.swadhyay));
                    if (d.itar > 0)       row.etItar.setText(String.valueOf(d.itar));
                    if (d.tondi > 0)      row.etTondiB.setText(String.valueOf(d.tondi));
                    if (d.pratyakshikB > 0) row.etPratyakshikB.setText(String.valueOf(d.pratyakshikB));
                    if (d.lekhi > 0)      row.etLekhi.setText(String.valueOf(d.lekhi));
                }
            } else if (m.subjectMarks != null && m.subjectMarks.containsKey(subName)) {
                // Backward-compat: flat map → fill into लेखी
                row.etLekhi.setText(formatMark(m.subjectMarks.get(subName)));
            }

            // Trigger display refresh after setting values
            int[] mx = i < subjectMaxBreakdown.size() ? subjectMaxBreakdown.get(i) : null;
            if (mx != null)
                updateRow(row, mx, classModel.subjects.get(i).maxMarks);
        }
        recalcAllSubjectsTotals();
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    private void saveMarks() {
        // ── Guard: check auth before doing any work ────────────────────────────
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (currentUser == null) {
            Log.e("SAVE_MARKS", "ABORT: User is NOT authenticated. Cannot save to Firestore.");
            Toast.makeText(this,
                    R.string.msg_empty_1, Toast.LENGTH_LONG).show();
            return;
        }
        Log.d("SAVE_MARKS", "Auth OK — UID: " + currentUser.getUid());

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

        java.util.Map<String, String> oldRemarks = new java.util.HashMap<>();
        if (existingMarks != null && existingMarks.detailedMarks != null) {
            for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : existingMarks.detailedMarks.entrySet()) {
                if (entry.getValue() != null && entry.getValue().remark != null) {
                    oldRemarks.put(entry.getKey(), entry.getValue().remark);
                }
            }
        }

        m.subjectMarks.clear();
        m.subjectMax.clear();
        m.detailedMarks.clear();

        // Attendance
        try {
            m.presentDays = Integer.parseInt(b.etPresentDays.getText().toString());
        } catch (Exception ignored) {
        }
        try {
            m.totalDays = Integer.parseInt(b.etTotalDays.getText().toString());
        } catch (Exception ignored) {
        }

        // Semester
        if (SessionContext.selectedSemester != null && SessionContext.selectedSemester.id != null) {
            m.semesterId = SessionContext.selectedSemester.id;
            m.semesterNumber = String.valueOf(SessionContext.selectedSemester.number);
        } else {
            m.semesterId = "sem_1";
            m.semesterNumber = "1";
        }

        // ── Diagnostic log before save ─────────────────────────────────────────
        Log.d("SAVE_MARKS", "========== MARKS SAVE PAYLOAD ==========");
        Log.d("SAVE_MARKS", "studentId  : " + m.studentId);
        Log.d("SAVE_MARKS", "classId    : " + m.classId);
        Log.d("SAVE_MARKS", "semesterId : " + m.semesterId);
        Log.d("SAVE_MARKS", "existingId : " + (m.id != null ? m.id : "NEW"));
        Log.d("SAVE_MARKS", "subjects   : " + (classModel.subjects != null ? classModel.subjects.size() : 0));

        if (classModel.subjects == null) {
            Toast.makeText(this, R.string.msg_no_subjects_configured, Toast.LENGTH_SHORT).show();
            return;
        }

        double total = 0;
        int maxTotal = 0;
        for (int i = 0; i < classModel.subjects.size() && i < marksRows.size(); i++) {
            Subject sub = classModel.subjects.get(i);
            ItemSubjectMarksRowBinding row = marksRows.get(i);

            MarksRecord.SubjectMarksDetail d = new MarksRecord.SubjectMarksDetail();
            d.nirikhshan = getInt(row.etNirikhshan);
            d.tondiKam = getInt(row.etTondiKam);
            d.pratyakshik = getInt(row.etPratyakshik);
            d.upkram = getInt(row.etUpkram);
            d.prakalp = getInt(row.etPrakalp);
            d.chachani = getInt(row.etChachani);
            d.swadhyay = getInt(row.etSwadhyay);
            d.itar = getInt(row.etItar);
            d.akarikTotal = d.nirikhshan + d.tondiKam + d.pratyakshik + d.upkram
                    + d.prakalp + d.chachani + d.swadhyay + d.itar;

            d.tondi = getInt(row.etTondiB);
            d.pratyakshikB = getInt(row.etPratyakshikB);
            d.lekhi = getInt(row.etLekhi);
            d.sanklit = d.tondi + d.pratyakshikB + d.lekhi;

            d.grandTotal = d.akarikTotal + d.sanklit;
            d.maxMarks = sub.maxMarks;
            d.grade = GradeCalculator.getMyschoolGrade(d.grandTotal, d.maxMarks);

            String safeKey = MarksRecord.sanitizeKey(sub.name);

            // Preserve existing remark if any
            d.remark = "";
            if (oldRemarks.containsKey(safeKey)) {
                d.remark = oldRemarks.get(safeKey);
            }

            Log.d("SAVE_MARKS", "  [" + sub.name + "] akarik=" + d.akarikTotal
                    + " sanklit=" + d.sanklit + " total=" + d.grandTotal + " grade=" + d.grade);

            m.detailedMarks.put(safeKey, d);

            // Backward compat (used by older MarksheetActivity / PDF legacy path)
            m.subjectMarks.put(safeKey, (double) d.grandTotal);
            m.subjectMax.put(safeKey, sub.maxMarks);

            total += d.grandTotal;
            maxTotal += sub.maxMarks;
        }

        m.totalObtained = total;
        m.totalMax = maxTotal;
        m.percentage = GradeCalculator.getPercentage(total, maxTotal);
        m.grade = GradeCalculator.getMyschoolGrade(total, maxTotal);
        m.result = GradeCalculator.getResult(m.percentage);

        Log.d("SAVE_MARKS", "totalObtained: " + total + " / " + maxTotal
                + " | %: " + m.percentage + " | grade: " + m.grade);
        Log.d("SAVE_MARKS", "Calling FirebaseRepository.saveMarks() now...");

        showLoading(true);
        FirebaseRepository.get().saveMarks(m, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String id) {
                Log.d("SAVE_MARKS", "SUCCESS — Firestore doc id: " + id);
                m.id = id;
                AppCache.selectedMarks = m;

                // Persist doc ID to SharedPreferences so it survives app restart.
                String semId = m.semesterId != null && !m.semesterId.isEmpty() ? m.semesterId 
                        : (SessionContext.selectedSemester != null ? SessionContext.selectedSemester.id : "sem_1");
                String prefKey = "marks_doc_" + m.studentId + "_" + m.classId + "_" + semId;
                String legacyPrefKey = "marks_doc_" + m.studentId + "_" + m.classId;
                getSharedPreferences("marks_doc_ids", MODE_PRIVATE)
                        .edit()
                        .putString(prefKey, id)
                        .putString(legacyPrefKey, id)
                        .apply();
                Log.d("SAVE_MARKS", "Saved docId to SharedPreferences key=" + prefKey + " and " + legacyPrefKey);

                // Patch the marks map immediately so instant-cache render shows new marks.
                // Initialize the map if it was null (first save of the session).
                if (AppCache.cachedMarksMap == null) {
                    AppCache.cachedMarksMap = new java.util.HashMap<>();
                }
                AppCache.cachedMarksMap.put(student.id, m);
                AppCache.cachedClassIdForStudents = classModel.id;
                AppCache.cachedSemesterIdForMarks = m.semesterId;
                Log.d("SAVE_MARKS", "Patched AppCache.cachedMarksMap for student=" + student.id);

                // Patch descriptive entries cache too.
                if (AppCache.cachedDescriptiveMarksMap == null
                        || !java.util.Objects.equals(classModel.id, AppCache.cachedDescriptiveClassId)
                        || !java.util.Objects.equals(m.semesterId, AppCache.cachedDescriptiveSemesterId)) {
                    AppCache.cachedDescriptiveMarksMap = new java.util.HashMap<>();
                    AppCache.cachedDescriptiveClassId = classModel.id;
                    AppCache.cachedDescriptiveSemesterId = m.semesterId;
                    AppCache.cachedDescriptiveMarksComplete = true;
                }
                AppCache.cachedDescriptiveMarksMap.put(student.id, m);

                // Set cross-screen save signals so FormativeSummativeFragment
                // can instantly patch this student's card without waiting for Firestore.
                AppCache.marksJustSaved = true;
                AppCache.marksJustSavedStudentId = student.id;
                AppCache.marksJustSavedRecord = m;
                Log.d("SAVE_MARKS", "Set marksJustSaved signals for student=" + student.id);

                // Clear repo marks cache so Firestore is queried fresh on next load
                FirebaseRepository.get().clearMarksCache();
                Log.d("SAVE_MARKS", "Cleared repo marks cache — fragment will re-fetch from Firestore.");

                showLoading(false);
                student.marksEntered = true;
                FirebaseRepository.get().saveStudent(student, new FirebaseRepository.OnResult<String>() {
                    @Override
                    public void onSuccess(String i) {
                        Log.d("SAVE_MARKS", "saveStudent SUCCESS — id: " + i);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("SAVE_MARKS", "saveStudent FAILED: " + e.getMessage(), e);
                    }
                });

                Toast.makeText(EnterMarksActivity.this, R.string.msg_empty_2, Toast.LENGTH_SHORT)
                        .show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(Exception e) {
                Log.e("SAVE_MARKS", "FIRESTORE ERROR: " + e.getMessage(), e);
                showLoading(false);
                Toast.makeText(EnterMarksActivity.this,
                        "❌ जतन अयशस्वी: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── OCR ───────────────────────────────────────────────────────────────────
    private void showScanOptions() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.msg_scan_marksheet)
                .setItems(new String[] { "Camera", "Gallery" }, (d, w) -> {
                    if (w == 0) {
                        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                    } else {
                        galleryLauncher.launch(new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                    }
                }).show();
    }

    private void processOcr(Bitmap bitmap) {
        if (bitmap == null)
            return;
        b.cardOcrPreview.setVisibility(View.VISIBLE);
        b.ivOcrPreview.setImageBitmap(bitmap);
        b.tvOcrRaw.setText(R.string.msg_processing);
        ocrHelper.processImage(bitmap, new OcrHelper.OcrCallback() {
            @Override
            public void onResult(List<String> numbers, String rawText) {
                runOnUiThread(() -> {
                    b.tvOcrRaw.setText("Detected: " + numbers);
                    autoFillFromOcr(numbers);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> b.tvOcrRaw.setText("OCR Error: " + e.getMessage()));
            }
        });
    }

    private void autoFillFromOcr(List<String> numbers) {
        if (classModel.subjects == null)
            return;
        int idx = 0;
        for (int i = 0; i < marksRows.size() && idx < numbers.size() && i < classModel.subjects.size(); i++) {
            try {
                int d = Integer.parseInt(numbers.get(idx++));
                int max = classModel.subjects.get(i).maxMarks;
                if (d >= 0 && d <= max)
                    marksRows.get(i).etLekhi.setText(String.valueOf(d));
            } catch (NumberFormatException ignored) {
            }
        }
        recalcAllSubjectsTotals();
        Toast.makeText(this, R.string.msg_marks_filled_into_written_plea, Toast.LENGTH_LONG).show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private int getInt(android.widget.EditText et) {
        if (et == null || et.getText() == null)
            return 0;
        String s = et.getText().toString().trim();
        if (s.isEmpty())
            return 0;

        // Convert Marathi digits to English digits
        s = s.replace('०', '0')
                .replace('१', '1')
                .replace('२', '2')
                .replace('३', '3')
                .replace('४', '4')
                .replace('५', '5')
                .replace('६', '6')
                .replace('७', '7')
                .replace('८', '8')
                .replace('९', '9');

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showLoading(boolean show) {
        b.marksProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSaveMarks.setEnabled(!show);
    }

    private void setupMaxMarksValidation(android.widget.EditText et, int maxVal) {
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String valStr = s.toString().trim();
                if (valStr.isEmpty())
                    return;
                try {
                    int val = Integer.parseInt(valStr);
                    if (val > maxVal) {
                        et.removeTextChangedListener(this);
                        et.setText(String.valueOf(maxVal));
                        et.setSelection(et.getText().length());
                        et.addTextChangedListener(this);
                        Toast.makeText(EnterMarksActivity.this, "Marks cannot exceed " + maxVal, Toast.LENGTH_SHORT)
                                .show();
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        });
    }

    private String formatMark(double v) {
        return (v == Math.floor(v)) ? String.valueOf((int) v) : String.valueOf(v);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ocrHelper.close();
    }
}
