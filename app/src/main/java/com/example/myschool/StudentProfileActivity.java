package com.example.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myschool.databinding.ActivityStudentProfileBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.Student;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.DetailRowHelper;
import com.example.myschool.utils.UiAnimations;
import com.google.android.material.chip.Chip;

public class StudentProfileActivity extends AppCompatActivity {

    private ActivityStudentProfileBinding b;
    private Student student;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SessionContext.load(this);
        super.onCreate(savedInstanceState);
        b = ActivityStudentProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
        });

        student = AppCache.selectedStudent;
        if (student == null || student.id == null) {
            Toast.makeText(this, R.string.student_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        b.btnEditStudent.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnEditStudent);
            startActivity(new Intent(this, StudentEditActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
        b.btnEnterMarks.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnEnterMarks);
            openMarks();
        });
        b.btnViewReport.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnViewReport);
            openReport();
        });

        // Set quick scroll section navigation click listeners with scale pulse feedback
        b.btnNavBasic.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnNavBasic);
            b.scrollStudentProfile.smoothScrollTo(0, b.cardBasic.getTop());
        });
        b.btnNavFamily.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnNavFamily);
            b.scrollStudentProfile.smoothScrollTo(0, b.cardFamily.getTop());
        });
        b.btnNavBank.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnNavBank);
            b.scrollStudentProfile.smoothScrollTo(0, b.cardBank.getTop());
        });
        b.btnNavAcademic.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnNavAcademic);
            b.scrollStudentProfile.smoothScrollTo(0, b.cardAcademic.getTop());
        });

        // Animate elements entering staggered for highly premium micro-interactions
        UiAnimations.staggerFadeIn(
                b.layoutHeaderBanner, b.cardHeaderPhoto, b.cardStats,
                b.btnNavBasic, b.btnNavFamily, b.btnNavBank, b.btnNavAcademic,
                b.cardBasic, b.cardFamily, b.cardBank, b.cardAcademic
        );
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
        String name = val(s.name);
        b.toolbar.setTitle(name);
        b.tvStudentName.setText(name);

        String std = val(s.standard);
        if ("—".equals(std) && s.className != null) {
            std = extractStandardFromClassName(s.className);
        }
        String div = val(s.division);
        b.toolbar.setSubtitle("Standard: " + std + " and Division: " + div);

        // Bind the top card critical highlight badges (Pills)
        b.tvTopRegNo.setText("Reg: " + val(s.registrationNo));
        b.tvTopDob.setText(val(s.dob));

        // Bind the 4-column green statistic card values
        b.tvTopRollNo.setText(val(s.rollNo));
        b.tvTopRollNo2.setText(val(s.rollNo2));
        b.tvTopGender.setText(val(s.gender));
        b.tvTopCast.setText(val(s.cast));

        // Dynamically bind custom monoline outline avatars based on gender
        if (s.gender != null) {
            String g = s.gender.toLowerCase().trim();
            if (g.contains("female") || g.contains("स्त्री") || g.contains("मुलगी")) {
                b.ivStudentPhoto.setImageResource(R.drawable.ic_girl_avatar);
            } else {
                b.ivStudentPhoto.setImageResource(R.drawable.ic_boy_avatar);
            }
        } else {
            b.ivStudentPhoto.setImageResource(R.drawable.ic_boy_avatar);
        }

        bindMarksChip(s);

        DetailRowHelper.fillRows(this, b.llBasicDetails, new String[][]{
                {getString(R.string.label_roll_no_1), s.rollNo},
                {getString(R.string.label_roll_no_2), s.rollNo2},
                {getString(R.string.label_reg_no), s.registrationNo},
                {getString(R.string.label_dob), s.dob},
                {getString(R.string.label_gender), s.gender},
                {getString(R.string.label_cast), s.cast},
                {getString(R.string.label_standard), s.standard},
                {getString(R.string.label_division), s.division},
                {getString(R.string.label_school_name), s.schoolName},
                {getString(R.string.label_class_short), s.className},
        });

        DetailRowHelper.fillRows(this, b.llFamilyDetails, new String[][]{
                {getString(R.string.label_parent_name), s.parentName},
                {getString(R.string.label_mother_name), s.motherName},
                {getString(R.string.label_mother_occupation), s.motherOccupation},
                {getString(R.string.label_mother_phone), s.motherPhone},
                {getString(R.string.label_father_name), s.fatherName},
                {getString(R.string.label_father_occupation), s.fatherOccupation},
                {getString(R.string.label_father_phone), s.fatherPhone},
                {getString(R.string.label_home_address), s.address},
        });

        DetailRowHelper.fillRows(this, b.llBankDetails, new String[][]{
                {getString(R.string.label_account_no), s.bankAccount},
                {getString(R.string.label_branch), s.bankBranch},
                {getString(R.string.label_ifsc), s.bankIfsc},
                {getString(R.string.label_bank_uid), s.bankUid},
        });

        DetailRowHelper.fillRows(this, b.llAcademicDetails, new String[][]{
                {getString(R.string.label_medium), s.medium},
                {getString(R.string.label_mother_tongue), s.motherTongue},
                {getString(R.string.label_date_admission), s.dateOfAdmission},
                {getString(R.string.label_student_id), s.studentIdNumber},
                {getString(R.string.label_uid), s.uid},
        });
    }

    private void bindMarksChip(Student s) {
        Chip chip = b.chipMarksStatus;
        if (s.marksEntered) {
            chip.setText(R.string.marks_entered_yes);
            chip.setChipBackgroundColorResource(R.color.success_container);
            chip.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            chip.setText(R.string.marks_entered_no);
            chip.setChipBackgroundColorResource(R.color.warning_container);
            chip.setTextColor(ContextCompat.getColor(this, R.color.warning));
        }
    }

    private String extractStandardFromClassName(String className) {
        if (className.contains(" ")) {
            String[] p = className.split(" ");
            if (p.length > 1) return p[1];
        }
        return className;
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
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
    }

    private void openReport() {
        loadClassThen(() -> {
            if (AppCache.selectedClass == null) {
                Toast.makeText(this, R.string.select_class_first, Toast.LENGTH_SHORT).show();
                return;
            }
            String semId = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.id : "sem_1";
            FirebaseRepository.get().getMarksForStudentAndSemester(student.id, student.classId, semId,
                    new FirebaseRepository.OnResult<MarksRecord>() {
                        @Override public void onSuccess(MarksRecord m) {
                            if (m != null) {
                                AppCache.selectedMarks = m;
                                startActivity(new Intent(StudentProfileActivity.this, MarksheetActivity.class));
                                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
                            } else {
                                Toast.makeText(StudentProfileActivity.this,
                                        R.string.no_marks_yet, Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onError(Exception e) {
                            Toast.makeText(StudentProfileActivity.this,
                                    e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
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
        if (student.schoolId == null) {
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
        if (AppCache.selectedStudent != null && AppCache.selectedStudent.id != null) {
            student = AppCache.selectedStudent;
            loadStudent();
        }
    }
}
