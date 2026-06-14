package com.kartik.myschool;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kartik.myschool.databinding.ActivityStudentRegisterBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StudentRegisterActivity extends AppCompatActivity {

    private ActivityStudentRegisterBinding b;
    private final List<School> schools   = new ArrayList<>();
    private final List<ClassModel> classes = new ArrayList<>();
    private School selectedSchool;
    private ClassModel selectedClass;
    private Student editStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityStudentRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // Date picker
        b.tilDob.setEndIconOnClickListener(v -> showDatePicker());
        b.etDob.setOnClickListener(v -> showDatePicker());

        SessionContext.syncFromAppCache();

        // Load schools
        loadSchools();

        // School selection → load classes
        b.actvSelectSchool.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos >= 0 && pos < schools.size()) {
                selectedSchool = schools.get(pos);
                loadClassesForSchool(selectedSchool.id);
            }
        });

        b.actvSelectClass.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos >= 0 && pos < classes.size()) {
                selectedClass = classes.get(pos);
            }
        });

        b.btnSaveStudent.setOnClickListener(v -> saveStudent());

        // Edit mode
        if (getIntent().getBooleanExtra("edit", false) && AppCache.selectedStudent != null) {
            editStudent = AppCache.selectedStudent;
            prefill();
        }
    }

    private void loadSchools() {
        showLoading(true);
        FirebaseRepository.get().getSchools(new FirebaseRepository.OnResult<List<School>>() {
            @Override public void onSuccess(List<School> list) {
                showLoading(false);
                schools.clear();
                schools.addAll(list);

                if (schools.isEmpty()) {
                    b.tilSelectSchool.setHint("No schools available");
                    b.actvSelectSchool.setEnabled(false);
                    b.tilSelectClass.setEnabled(false);
                    Toast.makeText(StudentRegisterActivity.this,
                            R.string.msg_please_add_a_school_first, Toast.LENGTH_LONG).show();
                    return;
                }

                b.actvSelectSchool.setEnabled(true);
                b.tilSelectSchool.setHint(getString(R.string.label_select_school));

                List<String> names = new ArrayList<>();
                for (School s : list) names.add(s.name);
                b.actvSelectSchool.setAdapter(new ArrayAdapter<>(StudentRegisterActivity.this,
                        android.R.layout.simple_dropdown_item_1line, names));

                // Pre-select from session or cache
                School preSchool = SessionContext.selectedSchool != null
                        ? SessionContext.selectedSchool : AppCache.selectedSchool;
                if (preSchool != null) {
                    for (int i = 0; i < schools.size(); i++) {
                        if (java.util.Objects.equals(schools.get(i).id, preSchool.id)) {
                            b.actvSelectSchool.setText(schools.get(i).name, false);
                            selectedSchool = schools.get(i);
                            loadClassesForSchool(selectedSchool.id);
                            break;
                        }
                    }
                }
            }
            @Override public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(StudentRegisterActivity.this,
                        "Failed to load schools: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadClassesForSchool(String schoolId) {
        b.actvSelectClass.setText(""); // Clear previous selection
        selectedClass = null;
        showLoading(true);

        FirebaseRepository.get().getClassesForSchool(schoolId, new FirebaseRepository.OnResult<List<ClassModel>>() {
            @Override public void onSuccess(List<ClassModel> list) {
                showLoading(false);
                classes.clear();
                classes.addAll(list);

                if (classes.isEmpty()) {
                    b.tilSelectClass.setHint("No classes in this school");
                    b.actvSelectClass.setEnabled(false);
                    Toast.makeText(StudentRegisterActivity.this,
                            R.string.msg_please_add_a_class_to_this_sch, Toast.LENGTH_LONG).show();
                    return;
                }

                b.actvSelectClass.setEnabled(true);
                b.tilSelectClass.setHint(getString(R.string.label_select_class));

                List<String> names = new ArrayList<>();
                for (ClassModel c : list) names.add(c.getDisplayName() + " - " + c.examName);
                b.actvSelectClass.setAdapter(new ArrayAdapter<>(StudentRegisterActivity.this,
                        android.R.layout.simple_dropdown_item_1line, names));

                ClassModel preClass = SessionContext.selectedClass != null
                        ? SessionContext.selectedClass : AppCache.selectedClass;
                if (preClass != null) {
                    for (int i = 0; i < classes.size(); i++) {
                        if (classes.get(i).id != null && classes.get(i).id.equals(preClass.id)) {
                            b.actvSelectClass.setText(names.get(i), false);
                            selectedClass = classes.get(i);
                            break;
                        }
                    }
                }
            }
            @Override public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(StudentRegisterActivity.this,
                        "Failed to load classes: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            String date = String.format("%02d/%02d/%04d", d, m + 1, y);
            b.etDob.setText(date);
        }, cal.get(Calendar.YEAR) - 10, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void prefill() {
        b.etStudentName.setText(editStudent.name);
        b.etRollNo.setText(editStudent.rollNo);
        b.etDob.setText(editStudent.dob);
        b.etParentName.setText(editStudent.parentName);
        if ("Female".equals(editStudent.gender)) b.rbFemale.setChecked(true);
        else b.rbMale.setChecked(true);
    }

    private void saveStudent() {
        String name   = str(b.etStudentName);
        String roll   = str(b.etRollNo);
        String dob    = b.etDob.getText() != null ? b.etDob.getText().toString().trim() : "";
        String parent = str(b.etParentName);
        String gender = b.rbFemale.isChecked() ? "Female" : "Male";

        boolean valid = true;
        if (TextUtils.isEmpty(name)) { b.tilStudentName.setError("Required"); valid = false; }
        else if (name.length() > 100) { b.tilStudentName.setError("Name too long (max 100 chars)"); valid = false; }
        
        if (TextUtils.isEmpty(roll)) { b.tilRollNo.setError("Required"); valid = false; }
        else if (roll.length() > 50) { b.tilRollNo.setError("Roll no too long (max 50 chars)"); valid = false; }
        
        if (parent.length() > 100) { b.tilParentName.setError("Parent name too long (max 100 chars)"); valid = false; }
        
        if (selectedSchool == null)  { b.tilSelectSchool.setError("Select a school"); valid = false; }
        if (selectedClass == null)   { b.tilSelectClass.setError("Select a class"); valid = false; }
        
        if (!valid) return;
        
        b.tilStudentName.setError(null); b.tilRollNo.setError(null); b.tilParentName.setError(null);
        b.tilSelectSchool.setError(null); b.tilSelectClass.setError(null);

        Student s = editStudent != null ? editStudent : new Student();
        s.name       = name;
        s.rollNo     = roll;
        s.dob        = dob;
        s.parentName = parent;
        s.gender     = gender;
        s.schoolId   = selectedSchool.id;
        s.classId    = selectedClass.id;

        // Bug #2 fix: set denormalized fields so adapter can display them
        // and teacherId so getAllStudentsForTeacher query works
        s.teacherId  = FirebaseRepository.get().currentUid();
        s.schoolName = selectedSchool.name != null ? selectedSchool.name : "";
        s.className  = selectedClass.getDisplayName();

        showLoading(true);
        FirebaseRepository.get().saveStudent(s, new FirebaseRepository.OnResult<String>() {
            @Override public void onSuccess(String id) {
                showLoading(false);
                Toast.makeText(StudentRegisterActivity.this,
                        editStudent != null ? "Student updated!" : "Student added!",
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(StudentRegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        b.studentProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSaveStudent.setEnabled(!show);
    }

    private String str(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
