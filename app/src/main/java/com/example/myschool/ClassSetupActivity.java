package com.example.myschool;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myschool.databinding.ActivityClassSetupBinding;
import com.example.myschool.databinding.ItemSubjectInputRowBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.Subject;
import com.example.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class ClassSetupActivity extends AppCompatActivity {

    private ActivityClassSetupBinding b;
    private final List<ItemSubjectInputRowBinding> subjectRows = new ArrayList<>();

    private static final String[] CLASSES   = {"1","2","3","4","5","6","7","8","9","10","11","12"};
    private static final String[] DIVISIONS = {"A","B","C","D","E"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityClassSetupBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // Dropdowns
        b.actvClass.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, CLASSES));
        b.actvDivision.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, DIVISIONS));

        b.btnAddSubject.setOnClickListener(v -> addSubjectRow(null, 0));
        b.btnSaveClass.setOnClickListener(v -> saveClass());

        // Default subjects
        String[] defaults = {"Mathematics", "Science", "English", "Social Studies", "Hindi"};
        for (String s : defaults) addSubjectRow(s, 100);

        // Pre-fill if editing
        if (AppCache.selectedClass != null && getIntent().getBooleanExtra("edit", false)) {
            ClassModel c = AppCache.selectedClass;
            b.actvClass.setText(c.className, false);
            b.actvDivision.setText(c.division, false);
            b.etExamName.setText(c.examName);
            b.etYear.setText(String.valueOf(c.year));
            b.llSubjectsContainer.removeAllViews();
            subjectRows.clear();
            for (Subject s : c.subjects) addSubjectRow(s.name, s.maxMarks);
        }
    }

    private void addSubjectRow(String name, int max) {
        ItemSubjectInputRowBinding row = ItemSubjectInputRowBinding.inflate(
                LayoutInflater.from(this), b.llSubjectsContainer, false);
        if (name != null) row.etSubjectName.setText(name);
        if (max > 0) row.etMaxMarks.setText(String.valueOf(max));
        row.btnRemoveSubject.setOnClickListener(v -> {
            b.llSubjectsContainer.removeView(row.getRoot());
            subjectRows.remove(row);
        });
        b.llSubjectsContainer.addView(row.getRoot());
        subjectRows.add(row);
    }

    private void saveClass() {
        String className = b.actvClass.getText().toString().trim();
        String division  = b.actvDivision.getText().toString().trim();
        String examName  = b.etExamName.getText() != null ? b.etExamName.getText().toString().trim() : "";
        String yearStr   = b.etYear.getText() != null ? b.etYear.getText().toString().trim() : "";

        if (TextUtils.isEmpty(className)) { b.tilClass.setError("Select class"); return; }
        if (TextUtils.isEmpty(examName))  { b.tilExamName.setError("Required"); return; }
        b.tilClass.setError(null); b.tilExamName.setError(null);

        // Collect subjects
        List<Subject> subjects = new ArrayList<>();
        for (ItemSubjectInputRowBinding row : subjectRows) {
            String sName = row.etSubjectName.getText() != null
                    ? row.etSubjectName.getText().toString().trim() : "";
            String sMax  = row.etMaxMarks.getText() != null
                    ? row.etMaxMarks.getText().toString().trim() : "100";
            if (!TextUtils.isEmpty(sName)) {
                int max = 100;
                try { max = Integer.parseInt(sMax); } catch (NumberFormatException ignored) {}
                subjects.add(new Subject(sName, max));
            }
        }
        if (subjects.isEmpty()) { Toast.makeText(this, "Add at least one subject", Toast.LENGTH_SHORT).show(); return; }

        ClassModel c = (AppCache.selectedClass != null && getIntent().getBooleanExtra("edit", false))
                ? AppCache.selectedClass : new ClassModel();
        c.schoolId  = (AppCache.selectedSchool != null && AppCache.selectedSchool.id != null) ? AppCache.selectedSchool.id : "";
        c.className = className;
        c.division  = division;
        c.examName  = examName;
        c.subjects  = subjects;
        try { c.year = Integer.parseInt(yearStr); } catch (NumberFormatException ignored) { c.year = 2025; }

        showLoading(true);
        FirebaseRepository.get().saveClass(c, new FirebaseRepository.OnResult<String>() {
            @Override public void onSuccess(String id) {
                showLoading(false);
                Toast.makeText(ClassSetupActivity.this, "Class saved!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(ClassSetupActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        b.classProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSaveClass.setEnabled(!show);
    }
}
