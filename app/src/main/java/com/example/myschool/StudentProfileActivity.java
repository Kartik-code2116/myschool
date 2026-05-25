package com.example.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myschool.databinding.ActivityStudentProfileBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.Student;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.DetailRowHelper;

public class StudentProfileActivity extends AppCompatActivity {

    private ActivityStudentProfileBinding b;
    private Student student;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityStudentProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        student = AppCache.selectedStudent;
        if (student == null || student.id == null) {
            Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        b.btnBack.setOnClickListener(v -> finish());
        b.btnEditStudent.setOnClickListener(v ->
                startActivity(new Intent(this, StudentEditActivity.class)));
        b.btnEnterMarks.setOnClickListener(v -> openMarks());
        b.btnViewReport.setOnClickListener(v -> openReport());

        loadStudent();
    }

    private void loadStudent() {
        FirebaseRepository.get().getStudent(student.id, new FirebaseRepository.OnResult<Student>() {
            @Override public void onSuccess(Student s) {
                if (s == null) return;
                student = s;
                AppCache.selectedStudent = s;
                bindUi(s);
            }
            @Override public void onError(Exception e) {
                bindUi(student);
            }
        });
    }

    private void bindUi(Student s) {
        String std = s.standard != null ? s.standard : extractStandard(s);
        String div = s.division != null ? s.division : "-";
        String subtitle = getString(R.string.standard_division_format, std, div);

        b.tvToolbarName.setText(s.name != null ? s.name : "");
        b.tvToolbarStandard.setText(subtitle);
        b.tvStudentNameLarge.setText(s.name != null ? s.name : "");
        b.tvRegBadge.setText(getString(R.string.reg_format,
                s.registrationNo != null ? s.registrationNo : (s.rollNo != null ? s.rollNo : "—")));
        b.tvDobBadge.setText(s.dob != null ? s.dob : "—");

        b.tvRoll1.setText(val(s.rollNo));
        b.tvRoll2.setText(val(s.rollNo2));
        b.tvGenderVal.setText(val(s.gender));
        b.tvCastVal.setText(val(s.cast));

        b.llFamilyDetails.removeAllViews();
        DetailRowHelper.addRow(this, b.llFamilyDetails, getString(R.string.label_mother_name), s.motherName);
        DetailRowHelper.addRow(this, b.llFamilyDetails, getString(R.string.label_mother_occupation), s.motherOccupation);
        DetailRowHelper.addRow(this, b.llFamilyDetails, getString(R.string.label_mother_phone), s.motherPhone);
        DetailRowHelper.addRow(this, b.llFamilyDetails, getString(R.string.label_father_name), s.fatherName);
        DetailRowHelper.addRow(this, b.llFamilyDetails, getString(R.string.label_father_occupation), s.fatherOccupation);
        DetailRowHelper.addRow(this, b.llFamilyDetails, getString(R.string.label_father_phone), s.fatherPhone);
        DetailRowHelper.addRow(this, b.llFamilyDetails, getString(R.string.label_home_address), s.address);

        b.llBankDetails.removeAllViews();
        DetailRowHelper.addRow(this, b.llBankDetails, getString(R.string.label_account_no), s.bankAccount);
        DetailRowHelper.addRow(this, b.llBankDetails, getString(R.string.label_branch), s.bankBranch);
        DetailRowHelper.addRow(this, b.llBankDetails, getString(R.string.label_ifsc), s.bankIfsc);
        DetailRowHelper.addRow(this, b.llBankDetails, getString(R.string.label_uid), s.bankUid);

        b.tvMedium.setText(val(s.medium));
        b.tvMotherTongue.setText(val(s.motherTongue));
        b.tvDateAdmission.setText(val(s.dateOfAdmission));

        b.llIdDetails.removeAllViews();
        DetailRowHelper.addRow(this, b.llIdDetails, getString(R.string.label_student_id), s.studentIdNumber);
        DetailRowHelper.addRow(this, b.llIdDetails, getString(R.string.label_uid), s.uid);
    }

    private String extractStandard(Student s) {
        if (s.className != null && s.className.contains(" ")) {
            String[] p = s.className.split(" ");
            if (p.length > 1) return p[1];
        }
        return "1";
    }

    private String val(String v) {
        return v != null && !v.isEmpty() ? v : "—";
    }

    private void openMarks() {
        loadClassThen(() -> {
            if (AppCache.selectedClass == null) {
                Toast.makeText(this, R.string.select_class_first, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, EnterMarksActivity.class));
        });
    }

    private void openReport() {
        loadClassThen(() -> {
            if (AppCache.selectedClass == null) return;
            FirebaseRepository.get().getMarksForStudent(student.id, student.classId,
                    new FirebaseRepository.OnResult<MarksRecord>() {
                        @Override public void onSuccess(MarksRecord m) {
                            if (m != null) {
                                AppCache.selectedMarks = m;
                                startActivity(new Intent(StudentProfileActivity.this, MarksheetActivity.class));
                            } else {
                                Toast.makeText(StudentProfileActivity.this,
                                        "No marks entered yet", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onError(Exception e) {}
                    });
        });
    }

    private void loadClassThen(Runnable next) {
        if (SessionContext.selectedClass != null && student.classId != null
                && student.classId.equals(SessionContext.selectedClass.id)) {
            AppCache.selectedClass = SessionContext.selectedClass;
            next.run();
            return;
        }
        FirebaseRepository.get().getClassesForSchool(student.schoolId,
                new FirebaseRepository.OnResult<java.util.List<ClassModel>>() {
                    @Override public void onSuccess(java.util.List<ClassModel> list) {
                        AppCache.selectedClass = null;
                        if (list != null) {
                            for (ClassModel c : list) {
                                if (c.id != null && c.id.equals(student.classId)) {
                                    AppCache.selectedClass = c;
                                    break;
                                }
                            }
                        }
                        runOnUiThread(next);
                    }
                    @Override public void onError(Exception e) { runOnUiThread(next); }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppCache.selectedStudent != null) {
            student = AppCache.selectedStudent;
            loadStudent();
        }
    }
}
