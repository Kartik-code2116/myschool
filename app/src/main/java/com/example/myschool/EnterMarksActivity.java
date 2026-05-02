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
        row.tvMaxMarks.setText(String.valueOf(maxMarks));
        row.etObtainedMarks.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b2, int c) { recalcTotals(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        b.llMarksRows.addView(row.getRoot());
        marksRows.add(row);
    }

    private void fillExistingMarks(MarksRecord m) {
        if (classModel.subjects == null) return;
        for (int i = 0; i < classModel.subjects.size() && i < marksRows.size(); i++) {
            String subName = classModel.subjects.get(i).name;
            if (m.subjectMarks.containsKey(subName)) {
                double val = m.subjectMarks.get(subName);
                String display = (val == Math.floor(val))
                        ? String.valueOf((int) val)
                        : String.valueOf(val);
                marksRows.get(i).etObtainedMarks.setText(display);
            }
        }
        recalcTotals();
    }

    private void recalcTotals() {
        double total = 0;
        int maxTotal = 0;
        if (classModel.subjects == null) return;
        for (int i = 0; i < classModel.subjects.size() && i < marksRows.size(); i++) {
            maxTotal += classModel.subjects.get(i).maxMarks;
            String txt = marksRows.get(i).etObtainedMarks.getText() != null
                    ? marksRows.get(i).etObtainedMarks.getText().toString() : "";
            try { total += Double.parseDouble(txt); } catch (NumberFormatException ignored) {}
        }
        double pct = GradeCalculator.getPercentage(total, maxTotal);
        b.tvTotalMax.setText(String.valueOf(maxTotal));
        b.tvTotalObtained.setText(formatMark(total));
        b.tvPercentage.setText(String.format("%.2f%%", pct));
        b.tvGrade.setText("Grade: " + GradeCalculator.getGrade(pct));
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
        // Fill marks row by row using detected numbers
        if (classModel.subjects == null) return;
        int idx = 0;
        for (int i = 0; i < marksRows.size() && idx < numbers.size() && i < classModel.subjects.size(); i++) {
            String val = numbers.get(idx++);
            try {
                double d = Double.parseDouble(val);
                int max = classModel.subjects.get(i).maxMarks;
                if (d >= 0 && d <= max) {
                    marksRows.get(i).etObtainedMarks.setText(val);
                }
            } catch (NumberFormatException ignored) {}
        }
        recalcTotals();
        Toast.makeText(this, "Marks filled — please verify!", Toast.LENGTH_LONG).show();
    }

    private void saveMarks() {
        MarksRecord m = existingMarks != null ? existingMarks : new MarksRecord();
        m.studentId = student.id;
        m.classId   = classModel.id;
        m.examName  = classModel.examName;
        m.subjectMarks.clear();
        m.subjectMax.clear();

        double total = 0; int maxTotal = 0;
        if (classModel.subjects == null) {
            Toast.makeText(this, "No subjects configured", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < classModel.subjects.size() && i < marksRows.size(); i++) {
            Subject sub = classModel.subjects.get(i);
            String txt  = marksRows.get(i).etObtainedMarks.getText() != null
                    ? marksRows.get(i).etObtainedMarks.getText().toString() : "0";
            double obtained = 0;
            try { obtained = Double.parseDouble(txt); } catch (NumberFormatException ignored) {}
            m.subjectMarks.put(sub.name, obtained);
            m.subjectMax.put(sub.name, sub.maxMarks);
            total    += obtained;
            maxTotal += sub.maxMarks;
        }
        m.totalObtained = total;
        m.totalMax      = maxTotal;
        m.percentage    = GradeCalculator.getPercentage(total, maxTotal);
        m.grade         = GradeCalculator.getGrade(m.percentage);
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
