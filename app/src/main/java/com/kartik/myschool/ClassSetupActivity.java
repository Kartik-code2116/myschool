package com.kartik.myschool;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kartik.myschool.SessionContext;
import com.kartik.myschool.databinding.ActivityClassSetupBinding;
import com.kartik.myschool.databinding.ItemSubjectInputRowBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class ClassSetupActivity extends BaseActivity {

    private ActivityClassSetupBinding b;
    private boolean shouldNavigateToSubjects = false;

    private static final String[] CLASSES   = {"1","2","3","4","5","6","7","8","9","10","11","12"};
    private static final String[] DIVISIONS = {"No Division", "A", "B", "C", "D", "E"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Screen opened: " + this.getClass().getSimpleName());
        b = ActivityClassSetupBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // Dropdowns
        b.actvClass.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, CLASSES));
        b.actvDivision.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, DIVISIONS));

        b.btnAddSubject.setOnClickListener(v -> {
            shouldNavigateToSubjects = true;
            saveClass();
        });
        b.btnSaveClass.setOnClickListener(v -> {
            shouldNavigateToSubjects = false;
            saveClass();
        });

        // Pre-fill if editing
        if (AppCache.selectedClass != null && getIntent().getBooleanExtra("edit", false)) {
            ClassModel c = AppCache.selectedClass;
            b.actvClass.setText(c.className, false);
            String divVal = c.division;
            if (TextUtils.isEmpty(divVal) || "-".equals(divVal)) {
                divVal = "No Division";
            }
            b.actvDivision.setText(divVal, false);
            b.etExamName.setText(c.examName);
            b.etYear.setText(String.valueOf(c.year));
        }
    }

    private void saveClass() {
        String className = b.actvClass.getText().toString().trim();
        String division  = b.actvDivision.getText().toString().trim();
        if (TextUtils.isEmpty(division) || "No Division".equalsIgnoreCase(division)) {
            division = "-";
        }
        
        // Auto-populated fields (hidden fields)
        String examName = SessionContext.selectedSemester != null && SessionContext.selectedSemester.name != null 
                ? SessionContext.selectedSemester.name : "Semester Exam";
        String yearStr = SessionContext.selectedYear != null 
                ? String.valueOf(SessionContext.selectedYear.startYear) : "2026";

        if (TextUtils.isEmpty(className)) { b.tilClass.setError("Select class"); return; }
        b.tilClass.setError(null);

        boolean isEdit = getIntent().getBooleanExtra("edit", false);
        ClassModel c = (AppCache.selectedClass != null && isEdit)
                ? AppCache.selectedClass : new ClassModel();
        
        // Preserve already assigned subjects or seed defaults for new classes
        if (c.subjects == null) {
            c.subjects = new ArrayList<>();
        }
        
        c.schoolId  = (AppCache.selectedSchool != null && AppCache.selectedSchool.id != null) ? AppCache.selectedSchool.id : "";
        c.className = className;
        c.division  = division;
        c.examName  = examName;
        try { c.year = Integer.parseInt(yearStr); } catch (NumberFormatException ignored) { c.year = 2025; }

        SessionContext.syncFromAppCache();
        if (SessionContext.selectedYear != null) {
            c.yearId = SessionContext.selectedYear.id;
            c.academicYearLabel = SessionContext.selectedYear.label;
        }
        if (SessionContext.selectedSemester != null) {
            c.semesterId = SessionContext.selectedSemester.id;
        }

        showLoading(true);
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
            @Override public void onSuccess(com.kartik.myschool.model.Teacher t) {
                if (t != null) {
                    c.teacherName = t.name;
                }
                FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<com.kartik.myschool.model.School>() {
                    @Override public void onSuccess(com.kartik.myschool.model.School school) {
                        c.schoolId = school.id;
                        SessionContext.selectedSchool = school;
                        AppCache.selectedSchool = school;
                        if (!isEdit && c.subjects.isEmpty()) {
                            FirebaseRepository.get().getClassDefaultSubjects(className, new FirebaseRepository.OnResult<List<Subject>>() {
                                @Override public void onSuccess(List<Subject> subjects) {
                                    if (subjects != null && !subjects.isEmpty()) {
                                        c.subjects.addAll(subjects);
                                    } else {
                                        c.subjects.addAll(Subject.getDefaultSubjectsForClass(className));
                                    }
                                    persistClass(c);
                                }
                                @Override public void onError(Exception e) {
                                    c.subjects.addAll(Subject.getDefaultSubjectsForClass(className));
                                    persistClass(c);
                                }
                            });
                        } else {
                            persistClass(c);
                        }
                    }
                    @Override public void onError(Exception e) {
                        showLoading(false);
                        Toast.makeText(ClassSetupActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(ClassSetupActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void persistClass(ClassModel c) {
        FirebaseRepository.get().saveClass(c, new FirebaseRepository.OnResult<String>() {
            @Override public void onSuccess(String id) {
                showLoading(false);
                Toast.makeText(ClassSetupActivity.this, R.string.msg_class_saved, Toast.LENGTH_SHORT).show();
                c.id = id;
                SessionContext.selectedClass = c;
                SessionContext.save(ClassSetupActivity.this);
                setResult(RESULT_OK);
                
                if (shouldNavigateToSubjects) {
                    android.content.Intent intent = new android.content.Intent(ClassSetupActivity.this, HomeActivity.class);
                    intent.putExtra("navigate_to", R.id.nav_subjects);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
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
