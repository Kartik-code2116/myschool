package com.example.myschool;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap photo = (Bitmap) extras.get("data");
                        processOcr(photo);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        processOcr(bmp);
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityEnterMarksBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        ocrHelper = new OcrHelper();
        student   = AppCache.selectedStudent;
        classModel = AppCache.selectedClass;

        if (student == null || classModel == null) { finish(); return; }

        // Fill header
        b.tvMarksStudentName.setText(student.name);
        b.tvMarksRollClass.setText("Roll: " + student.rollNo + " | " + classModel.getDisplayName());

        // Build marks rows from class subjects
        if (classModel.subjects != null) {
            for (Subject sub : classModel.subjects) addMarksRow(sub.name, sub.maxMarks);
        }

        // Load existing marks if any
        FirebaseRepository.get().getMarksForStudent(student.id, classModel.id,
                new FirebaseRepository.OnResult<MarksRecord>() {
                    @Override public void onSuccess(MarksRecord m) {
                        if (m != null) {
                            existingMarks = m;
                            fillExistingMarks(m);
                        }
                    }
                    @Override public void onError(Exception e) {}
                });

        b.btnScanMarksheet.setOnClickListener(v -> showScanOptions());
        b.btnSaveMarks.setOnClickListener(v -> saveMarks());
    }

    private void addMarksRow(String subjectName, int maxMarks) {
        ItemSubjectMarksRowBinding row = ItemSubjectMarksRowBinding.inflate(
                LayoutInflater.from(this), b.llMarksRows, false);
        row.tvSubjectName.setText(subjectName);
        
        // Header clicks toggle expand/collapse details
        row.layoutHeader.setOnClickListener(v -> {
            boolean isVisible = row.layoutDetails.getVisibility() == View.VISIBLE;
            row.layoutDetails.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            row.ivExpandArrow.setRotation(isVisible ? 0f : 180f);
        });

        // Initialize header
        updateSubjectHeader(row, maxMarks);

        // Bind TextWatchers to all inputs for real-time totals & grade updates
        addListeners(row, maxMarks);

        b.llMarksRows.addView(row.getRoot());
        marksRows.add(row);
    }

    private int getInt(android.widget.EditText et) {
        if (et == null || et.getText() == null) return 0;
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private void updateSubjectHeader(ItemSubjectMarksRowBinding row, int maxMarks) {
        int nirikhshan = getInt(row.etNirikhshan);
        int tondiKam = getInt(row.etTondiKam);
        int pratyakshik = getInt(row.etPratyakshik);
        int upkram = getInt(row.etUpkram);
        int prakalp = getInt(row.etPrakalp);
        int chachani = getInt(row.etChachani);
        int swadhyay = getInt(row.etSwadhyay);
        int itar = getInt(row.etItar);
        
        int formativeTotal = nirikhshan + tondiKam + pratyakshik + upkram + prakalp + chachani + swadhyay + itar;
        
        int tondiB = getInt(row.etTondiB);
        int pratyakshikB = getInt(row.etPratyakshikB);
        int lekhi = getInt(row.etLekhi);
        
        int summativeTotal = tondiB + pratyakshikB + lekhi;
        int grandTotal = formativeTotal + summativeTotal;
        
        row.tvSubjectTotal.setText("Total: " + grandTotal + "/" + maxMarks);
        String gr = GradeCalculator.getMyschoolGrade(grandTotal, maxMarks);
        row.tvSubjectGrade.setText("Grade: " + gr);
    }

    private void addListeners(ItemSubjectMarksRowBinding row, int maxMarks) {
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSubjectHeader(row, maxMarks);
                recalcTotals();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
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
        
        row.etSubjectRemark.addTextChangedListener(watcher);
    }

    private void fillExistingMarks(MarksRecord m) {
        if (classModel.subjects == null) return;
        
        // Fill Attendance details
        b.etPresentDays.setText(String.valueOf(m.presentDays));
        b.etTotalDays.setText(String.valueOf(m.totalDays));
        
        for (int i = 0; i < classModel.subjects.size() && i < marksRows.size(); i++) {
            String subName = classModel.subjects.get(i).name;
            ItemSubjectMarksRowBinding row = marksRows.get(i);
            
            if (m.detailedMarks != null && m.detailedMarks.containsKey(subName)) {
                MarksRecord.SubjectMarksDetail detail = m.detailedMarks.get(subName);
                if (detail != null) {
                    row.etNirikhshan.setText(String.valueOf(detail.nirikhshan));
                    row.etTondiKam.setText(String.valueOf(detail.tondiKam));
                    row.etPratyakshik.setText(String.valueOf(detail.pratyakshik));
                    row.etUpkram.setText(String.valueOf(detail.upkram));
                    row.etPrakalp.setText(String.valueOf(detail.prakalp));
                    row.etChachani.setText(String.valueOf(detail.chachani));
                    row.etSwadhyay.setText(String.valueOf(detail.swadhyay));
                    row.etItar.setText(String.valueOf(detail.itar));
                    
                    row.etTondiB.setText(String.valueOf(detail.tondi));
                    row.etPratyakshikB.setText(String.valueOf(detail.pratyakshikB));
                    row.etLekhi.setText(String.valueOf(detail.lekhi));
                    
                    row.etSubjectRemark.setText(detail.remark != null ? detail.remark : "");
                }
            } else if (m.subjectMarks.containsKey(subName)) {
                // Fallback for flat map compatibility
                double val = m.subjectMarks.get(subName);
                row.etLekhi.setText(formatMark(val));
            }
            updateSubjectHeader(row, classModel.subjects.get(i).maxMarks);
        }
        recalcTotals();
    }

    private void recalcTotals() {
        double total = 0;
        int maxTotal = 0;
        if (classModel.subjects == null) return;
        for (int i = 0; i < classModel.subjects.size() && i < marksRows.size(); i++) {
            int maxMarks = classModel.subjects.get(i).maxMarks;
            maxTotal += maxMarks;
            
            ItemSubjectMarksRowBinding row = marksRows.get(i);
            int nirikhshan = getInt(row.etNirikhshan);
            int tondiKam = getInt(row.etTondiKam);
            int pratyakshik = getInt(row.etPratyakshik);
            int upkram = getInt(row.etUpkram);
            int prakalp = getInt(row.etPrakalp);
            int chachani = getInt(row.etChachani);
            int swadhyay = getInt(row.etSwadhyay);
            int itar = getInt(row.etItar);
            int akarikTotal = nirikhshan + tondiKam + pratyakshik + upkram + prakalp + chachani + swadhyay + itar;
            
            int tondiB = getInt(row.etTondiB);
            int pratyakshikB = getInt(row.etPratyakshikB);
            int lekhi = getInt(row.etLekhi);
            int sanklit = tondiB + pratyakshikB + lekhi;
            
            int grandTotal = akarikTotal + sanklit;
            total += grandTotal;
        }
        double pct = GradeCalculator.getPercentage(total, maxTotal);
        b.tvTotalMax.setText(String.valueOf(maxTotal));
        b.tvTotalObtained.setText(formatMark(total));
        b.tvPercentage.setText(String.format("%.2f%%", pct));
        b.tvGrade.setText("Grade: " + GradeCalculator.getMyschoolGrade(total, maxTotal));
        String result = GradeCalculator.getResult(pct);
        b.tvResult.setText(result);
        b.tvResult.setBackgroundResource("PASS".equals(result)
                ? R.drawable.bg_result_pass : R.drawable.bg_result_fail);
        b.tvResult.setTextColor(getResources().getColor(
                "PASS".equals(result) ? R.color.success : R.color.error, null));
    }

    private void showScanOptions() {
        String[] opts = {"Camera", "Gallery"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Scan Marksheet")
                .setItems(opts, (d, w) -> {
                    if (w == 0) {
                        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(cam);
                    } else {
                        Intent gallery = new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(gallery);
                    }
                }).show();
    }

    private void processOcr(Bitmap bitmap) {
        if (bitmap == null) return;
        b.cardOcrPreview.setVisibility(View.VISIBLE);
        b.ivOcrPreview.setImageBitmap(bitmap);
        b.tvOcrRaw.setText("Processing...");

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
        if (classModel.subjects == null) return;
        int idx = 0;
        for (int i = 0; i < marksRows.size() && idx < numbers.size() && i < classModel.subjects.size(); i++) {
            String val = numbers.get(idx++);
            try {
                int d = Integer.parseInt(val);
                int max = classModel.subjects.get(i).maxMarks;
                if (d >= 0 && d <= max) {
                    marksRows.get(i).etLekhi.setText(val);
                }
            } catch (NumberFormatException ignored) {}
        }
        recalcTotals();
        Toast.makeText(this, "Marks filled into Written (लेखी) — please verify!", Toast.LENGTH_LONG).show();
    }

    private void saveMarks() {
        MarksRecord m = existingMarks != null ? existingMarks : new MarksRecord();
        m.studentId = student.id;
        m.classId   = classModel.id;
        m.examName  = classModel.examName;
        m.subjectMarks.clear();
        m.subjectMax.clear();
        m.detailedMarks.clear();
        
        // Attendance
        try {
            m.presentDays = Integer.parseInt(b.etPresentDays.getText().toString());
        } catch (NumberFormatException ignored) {}
        try {
            m.totalDays = Integer.parseInt(b.etTotalDays.getText().toString());
        } catch (NumberFormatException ignored) {}
        
        // Semester Details
        if (SessionContext.selectedSemester != null) {
            m.semesterId = SessionContext.selectedSemester.id;
            m.semesterNumber = String.valueOf(SessionContext.selectedSemester.number);
        } else {
            m.semesterId = "sem_1";
            m.semesterNumber = "1";
        }

        double total = 0; int maxTotal = 0;
        if (classModel.subjects == null) {
            Toast.makeText(this, "No subjects configured", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < classModel.subjects.size() && i < marksRows.size(); i++) {
            Subject sub = classModel.subjects.get(i);
            ItemSubjectMarksRowBinding row = marksRows.get(i);
            
            MarksRecord.SubjectMarksDetail detail = new MarksRecord.SubjectMarksDetail();
            detail.nirikhshan = getInt(row.etNirikhshan);
            detail.tondiKam = getInt(row.etTondiKam);
            detail.pratyakshik = getInt(row.etPratyakshik);
            detail.upkram = getInt(row.etUpkram);
            detail.prakalp = getInt(row.etPrakalp);
            detail.chachani = getInt(row.etChachani);
            detail.swadhyay = getInt(row.etSwadhyay);
            detail.itar = getInt(row.etItar);
            detail.akarikTotal = detail.nirikhshan + detail.tondiKam + detail.pratyakshik + detail.upkram + detail.prakalp + detail.chachani + detail.swadhyay + detail.itar;
            
            detail.tondi = getInt(row.etTondiB);
            detail.pratyakshikB = getInt(row.etPratyakshikB);
            detail.lekhi = getInt(row.etLekhi);
            detail.sanklit = detail.tondi + detail.pratyakshikB + detail.lekhi;
            
            detail.grandTotal = detail.akarikTotal + detail.sanklit;
            detail.maxMarks = sub.maxMarks;
            detail.grade = GradeCalculator.getMyschoolGrade(detail.grandTotal, detail.maxMarks);
            detail.remark = row.etSubjectRemark.getText() != null ? row.etSubjectRemark.getText().toString() : "";
            
            m.detailedMarks.put(sub.name, detail);
            
            // Backward compatibility
            m.subjectMarks.put(sub.name, (double) detail.grandTotal);
            m.subjectMax.put(sub.name, sub.maxMarks);
            
            total    += detail.grandTotal;
            maxTotal += sub.maxMarks;
        }
        m.totalObtained = total;
        m.totalMax      = maxTotal;
        m.percentage    = GradeCalculator.getPercentage(total, maxTotal);
        m.grade         = GradeCalculator.getMyschoolGrade(total, maxTotal);
        m.result        = GradeCalculator.getResult(m.percentage);

        showLoading(true);
        FirebaseRepository.get().saveMarks(m, new FirebaseRepository.OnResult<String>() {
            @Override public void onSuccess(String id) {
                showLoading(false);
                // Mark student as done
                student.marksEntered = true;
                FirebaseRepository.get().saveStudent(student, new FirebaseRepository.OnResult<String>() {
                    @Override public void onSuccess(String i) {}
                    @Override public void onError(Exception e) {}
                });
                Toast.makeText(EnterMarksActivity.this, "Marks saved!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(EnterMarksActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        b.marksProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSaveMarks.setEnabled(!show);
    }

    private String formatMark(double v) {
        return (v == Math.floor(v)) ? String.valueOf((int) v) : String.valueOf(v);
    }

    @Override protected void onDestroy() { super.onDestroy(); ocrHelper.close(); }
}
