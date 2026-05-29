package com.example.myschool;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.myschool.databinding.ActivityMarksheetBinding;
import com.example.myschool.databinding.ItemPrintMarksRowBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.model.Subject;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.GradeCalculator;
import com.example.myschool.utils.PdfGenerator;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class MarksheetActivity extends AppCompatActivity {

    private ActivityMarksheetBinding b;
    private Student student;
    private ClassModel classModel;
    private MarksRecord marks;
    private School school;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMarksheetBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        student    = AppCache.selectedStudent;
        classModel = AppCache.selectedClass;
        marks      = AppCache.selectedMarks;

        if (student == null || classModel == null || marks == null) { finish(); return; }

        // Load school then render
        FirebaseRepository.get().getSchools(new FirebaseRepository.OnResult<List<School>>() {
            @Override public void onSuccess(List<School> list) {
                for (School s : list) {
                    if (s.id.equals(student.schoolId)) { school = s; break; }
                }
                if (school == null && !list.isEmpty()) school = list.get(0);
                renderMarksheet();
            }
            @Override public void onError(Exception e) { renderMarksheet(); }
        });

        b.btnPrintMarksheet.setOnClickListener(v -> generateAndPrint());
        b.btnShareMarksheet.setOnClickListener(v -> generateAndShare());
    }

    private void renderMarksheet() {
        // School info
        if (school != null) {
            b.tvPrintSchoolName.setText(school.name);
            b.tvPrintSchoolAddress.setText(school.address != null ? school.address : "");
            b.tvPrintBoard.setText("Board: " + (school.board != null ? school.board : ""));
        }
        // Guard against null examName
        String examName = classModel.examName != null ? classModel.examName : "";
        b.tvPrintExamTitle.setText(examName.toUpperCase(Locale.getDefault()) + " — REPORT CARD");

        // Student info
        b.tvPrintStudentName.setText(student.name);
        b.tvPrintRollNo.setText(student.rollNo);
        b.tvPrintClass.setText(classModel.getDisplayName());

        // Marks rows
        b.llPrintMarksRows.removeAllViews();
        boolean alt = false;
        // Build rows per subject.
        if (classModel.subjects == null) {
            classModel.subjects = new java.util.ArrayList<>();
        }
        for (Subject sub : classModel.subjects) {
            ItemPrintMarksRowBinding row = ItemPrintMarksRowBinding.inflate(
                    LayoutInflater.from(this), b.llPrintMarksRows, false);
            String safeKey = MarksRecord.sanitizeKey(sub.name);
            double obt = marks.subjectMarks.containsKey(safeKey)
                    ? marks.subjectMarks.get(safeKey) : 0;
            double pct = GradeCalculator.getPercentage(obt, sub.maxMarks);
            row.tvRowSubject.setText(sub.name);
            row.tvRowMax.setText(String.valueOf(sub.maxMarks));
            row.tvRowObtained.setText(formatMark(obt));
            row.tvRowGrade.setText(GradeCalculator.getGrade(pct));
            if (alt) row.getRoot().setBackgroundColor(0xFFF5F7FA);
            alt = !alt;
            b.llPrintMarksRows.addView(row.getRoot());
        }

        // Totals
        b.tvPrintTotalMax.setText(String.valueOf(marks.totalMax));
        b.tvPrintTotalObtained.setText(formatMark(marks.totalObtained));
        b.tvPrintGradeTotal.setText(marks.grade);
        b.tvPrintPercentage.setText(String.format("%.2f%%", marks.percentage));
        b.tvPrintResult.setText(marks.result);
        boolean pass = "PASS".equals(marks.result);
        b.tvPrintResult.setBackgroundResource(pass ? R.drawable.bg_result_pass : R.drawable.bg_result_fail);
        b.tvPrintResult.setTextColor(getResources().getColor(
                pass ? R.color.success : R.color.error, null));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_print) { generateAndPrint(); return true; }
        if (item.getItemId() == R.id.action_share) { generateAndShare(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void generateAndPrint() {
        if (school == null) { Toast.makeText(this, "School data not loaded", Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this, "Generating PDF…", Toast.LENGTH_SHORT).show();
        PdfGenerator.generate(this, school, classModel, student, marks, new PdfGenerator.PdfCallback() {
            @Override public void onSuccess(File f) {
                runOnUiThread(() -> {
                    // Bug #9 fix: removed unused PrintManager variable
                    Uri uri = FileProvider.getUriForFile(MarksheetActivity.this,
                            getPackageName() + ".fileprovider", f);
                    Intent view = new Intent(Intent.ACTION_VIEW);
                    view.setDataAndType(uri, "application/pdf");
                    view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(view, "Open PDF to Print"));
                });
            }
            @Override public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(MarksheetActivity.this,
                        "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void generateAndShare() {
        if (school == null) { Toast.makeText(this, "School data not loaded", Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this, "Generating PDF…", Toast.LENGTH_SHORT).show();
        PdfGenerator.generate(this, school, classModel, student, marks, new PdfGenerator.PdfCallback() {
            @Override public void onSuccess(File f) {
                runOnUiThread(() -> {
                    Uri uri = FileProvider.getUriForFile(MarksheetActivity.this,
                            getPackageName() + ".fileprovider", f);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("application/pdf");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.putExtra(Intent.EXTRA_SUBJECT, "Report Card - " + student.name);
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share, "Share Marksheet"));
                });
            }
            @Override public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(MarksheetActivity.this,
                        "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String formatMark(double v) {
        return (v == Math.floor(v)) ? String.valueOf((int) v) : String.format("%.1f", v);
    }
}
