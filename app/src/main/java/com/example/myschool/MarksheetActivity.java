package com.example.myschool;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.myschool.databinding.ActivityMarksheetBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.model.Subject;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.PdfGenerator;

import java.io.File;
import java.util.List;

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

        b.fabSavePdf.setOnClickListener(v -> generateAndPrint());
    }

    private void renderMarksheet() {
        if (school != null) {
            String sch = school.name;
            b.incPdfPaper.tvPdfMockSchool1.setText(sch);
            b.incPdfPaper.tvPdfMockSchool2.setText(sch);
        }

        String acYear = "2025-2026";
        String subText = student.name + " | " + acYear + " | UID: " + student.rollNo;
        b.incPdfPaper.tvPdfMockSub1.setText(subText);
        b.incPdfPaper.tvPdfMockSub2.setText(subText);

        b.incPdfPaper.llPdfMockRows1.removeAllViews();
        b.incPdfPaper.llPdfMockRows2.removeAllViews();

        if (classModel.subjects == null) classModel.subjects = new java.util.ArrayList<>();
        
        for (Subject sub : classModel.subjects) {
            String safeKey = MarksRecord.sanitizeKey(sub.name);
            MarksRecord.SubjectMarksDetail d = null;
            if (marks.detailedMarks != null && marks.detailedMarks.containsKey(safeKey)) {
                d = marks.detailedMarks.get(safeKey);
            }

            // Row for Sem 1
            b.incPdfPaper.llPdfMockRows1.addView(createMockRow(
                sub.name, 
                d != null && d.grade != null ? d.grade : "—", 
                d != null && d.remark != null ? d.remark : ""
            ));

            // Row for Sem 2
            b.incPdfPaper.llPdfMockRows2.addView(createMockRow(
                sub.name, 
                d != null && d.grade != null ? d.grade : "—", 
                d != null && d.remark != null ? d.remark : ""
            ));
        }
    }

    private View createMockRow(String sub, String grade, String remark) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvSub = new TextView(this);
        tvSub.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 4);
        tvSub.setTextColor(0xFF000000);
        tvSub.setPadding(2, 2, 2, 2);
        tvSub.setText(sub);

        View div1 = new View(this);
        div1.setLayoutParams(new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT));
        div1.setBackgroundColor(0xFF2962FF);

        TextView tvGrade = new TextView(this);
        tvGrade.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvGrade.setTextSize(TypedValue.COMPLEX_UNIT_SP, 4);
        tvGrade.setTextColor(0xFF000000);
        tvGrade.setPadding(2, 2, 2, 2);
        tvGrade.setGravity(Gravity.CENTER);
        tvGrade.setText(grade);

        View div2 = new View(this);
        div2.setLayoutParams(new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT));
        div2.setBackgroundColor(0xFF2962FF);

        TextView tvRemark = new TextView(this);
        tvRemark.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        tvRemark.setTextSize(TypedValue.COMPLEX_UNIT_SP, 4);
        tvRemark.setTextColor(0xFF000000);
        tvRemark.setPadding(2, 2, 2, 2);
        tvRemark.setText(remark);

        row.addView(tvSub);
        row.addView(div1);
        row.addView(tvGrade);
        row.addView(div2);
        row.addView(tvRemark);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT));
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);
        
        View bottomDiv = new View(this);
        bottomDiv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        bottomDiv.setBackgroundColor(0xFF2962FF);
        wrapper.addView(bottomDiv);

        return wrapper;
    }

    private void generateAndPrint() {
        if (school == null) { Toast.makeText(this, R.string.msg_school_data_not_loaded, Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this, R.string.msg_generating_pdf, Toast.LENGTH_SHORT).show();
        PdfGenerator.generate(this, school, classModel, student, marks, new PdfGenerator.PdfCallback() {
            @Override public void onSuccess(File f) {
                runOnUiThread(() -> {
                    Uri uri = FileProvider.getUriForFile(MarksheetActivity.this,
                            getPackageName() + ".fileprovider", f);
                    Intent view = new Intent(Intent.ACTION_VIEW);
                    view.setDataAndType(uri, "application/pdf");
                    view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(view, "Open PDF"));
                });
            }
            @Override public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(MarksheetActivity.this,
                        "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}
