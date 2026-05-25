package com.example.myschool;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myschool.databinding.ActivityStudentEditBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;
import com.google.android.material.textfield.TextInputEditText;

public class StudentEditActivity extends AppCompatActivity {

    private ActivityStudentEditBinding b;
    private Student student;
    private boolean isNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityStudentEditBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        isNew = getIntent().getBooleanExtra("new_student", false);
        student = AppCache.selectedStudent;
        if (!isNew && student == null) {
            Toast.makeText(this, "No student to edit", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (isNew) student = new Student();

        if (!isNew && student.id != null) {
            FirebaseRepository.get().getStudent(student.id, new FirebaseRepository.OnResult<Student>() {
                @Override public void onSuccess(Student s) {
                    if (s != null) {
                        student = s;
                        bindToForm(s);
                    }
                }
                @Override public void onError(Exception e) { bindToForm(student); }
            });
        } else {
            prefillFromSession();
        }

        b.btnSaveStudent.setOnClickListener(v -> save());
    }

    private void prefillFromSession() {
        if (SessionContext.selectedClass != null) {
            b.etStandard.setText(SessionContext.selectedClass.className);
            b.etDivision.setText(SessionContext.selectedClass.division);
        }
    }

    private void bindToForm(Student s) {
        set(b.etName, s.name);
        set(b.etRoll1, s.rollNo);
        set(b.etRoll2, s.rollNo2);
        set(b.etRegNo, s.registrationNo);
        set(b.etDob, s.dob);
        set(b.etGender, s.gender);
        set(b.etCast, s.cast);
        set(b.etStandard, s.standard);
        set(b.etDivision, s.division);
        set(b.etMotherName, s.motherName);
        set(b.etMotherOccupation, s.motherOccupation);
        set(b.etMotherPhone, s.motherPhone);
        set(b.etFatherName, s.fatherName);
        set(b.etFatherOccupation, s.fatherOccupation);
        set(b.etFatherPhone, s.fatherPhone);
        set(b.etAddress, s.address);
        set(b.etBankAccount, s.bankAccount);
        set(b.etBankBranch, s.bankBranch);
        set(b.etBankIfsc, s.bankIfsc);
        set(b.etBankUid, s.bankUid);
        set(b.etMedium, s.medium);
        set(b.etMotherTongue, s.motherTongue);
        set(b.etDateAdmission, s.dateOfAdmission);
        set(b.etStudentId, s.studentIdNumber);
        set(b.etUid, s.uid);
    }

    private void set(TextInputEditText et, String v) {
        if (et != null) et.setText(v != null ? v : "");
    }

    private void save() {
        String name = str(b.etName);
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Student s = student != null ? student : new Student();
        s.name = name;
        s.rollNo = str(b.etRoll1);
        s.rollNo2 = str(b.etRoll2);
        s.registrationNo = str(b.etRegNo);
        s.dob = str(b.etDob);
        s.gender = str(b.etGender);
        s.cast = str(b.etCast);
        s.standard = str(b.etStandard);
        s.division = str(b.etDivision);
        s.motherName = str(b.etMotherName);
        s.motherOccupation = str(b.etMotherOccupation);
        s.motherPhone = str(b.etMotherPhone);
        s.fatherName = str(b.etFatherName);
        s.fatherOccupation = str(b.etFatherOccupation);
        s.fatherPhone = str(b.etFatherPhone);
        s.address = str(b.etAddress);
        s.bankAccount = str(b.etBankAccount);
        s.bankBranch = str(b.etBankBranch);
        s.bankIfsc = str(b.etBankIfsc);
        s.bankUid = str(b.etBankUid);
        s.medium = str(b.etMedium);
        s.motherTongue = str(b.etMotherTongue);
        s.dateOfAdmission = str(b.etDateAdmission);
        s.studentIdNumber = str(b.etStudentId);
        s.uid = str(b.etUid);
        s.parentName = s.fatherName != null && !s.fatherName.isEmpty() ? s.fatherName : s.motherName;
        s.teacherId = FirebaseRepository.get().currentUid();

        if (SessionContext.selectedClass != null) {
            s.classId = SessionContext.selectedClass.id;
            s.className = SessionContext.selectedClass.getDisplayName();
        }

        b.progress.setVisibility(View.VISIBLE);
        b.btnSaveStudent.setEnabled(false);

        Runnable doSave = () -> FirebaseRepository.get().saveStudent(s, new FirebaseRepository.OnResult<String>() {
            @Override public void onSuccess(String id) {
                s.id = id;
                AppCache.selectedStudent = s;
                Toast.makeText(StudentEditActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void onError(Exception e) {
                b.progress.setVisibility(View.GONE);
                b.btnSaveStudent.setEnabled(true);
                Toast.makeText(StudentEditActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        if (s.schoolId != null && !s.schoolId.isEmpty()) {
            doSave.run();
            return;
        }

        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher t) {
                FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {
                    @Override public void onSuccess(School school) {
                        s.schoolId = school.id;
                        s.schoolName = school.name;
                        SessionContext.selectedSchool = school;
                        AppCache.selectedSchool = school;
                        doSave.run();
                    }
                    @Override public void onError(Exception e) {
                        b.progress.setVisibility(View.GONE);
                        b.btnSaveStudent.setEnabled(true);
                        Toast.makeText(StudentEditActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onError(Exception e) {
                b.progress.setVisibility(View.GONE);
                b.btnSaveStudent.setEnabled(true);
            }
        });
    }

    private String str(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
