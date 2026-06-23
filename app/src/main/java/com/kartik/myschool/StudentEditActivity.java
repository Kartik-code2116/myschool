package com.kartik.myschool;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kartik.myschool.databinding.ActivityStudentEditBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Teacher;
import com.kartik.myschool.repository.FirebaseRepository;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ArrayAdapter;
import android.widget.EditText;

public class StudentEditActivity extends BaseActivity {

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
            Toast.makeText(this, R.string.msg_no_student_to_edit, Toast.LENGTH_SHORT).show();
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

        // Setup dropdown for Gender
        String[] genderOptions = {
                getString(R.string.gender_male),
                getString(R.string.gender_female)
        };
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                genderOptions
        );
        b.etGender.setAdapter(genderAdapter);

        // Setup dropdown for Cast Categories
        String[] castOptions = {
                "SC (Scheduled Castes)",
                "ST (Scheduled Tribes)",
                "VJ (Vimukt Jati)",
                "NT (Nomadic Tribes)",
                "OBC (Other Backward Classes)",
                "Open"
        };
        ArrayAdapter<String> castAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                castOptions
        );
        b.etCast.setAdapter(castAdapter);

        // Setup dropdown for Medium
        String[] mediumOptions = {
                getString(R.string.lang_marathi),
                getString(R.string.lang_english),
                getString(R.string.lang_semi_english),
                getString(R.string.lang_hindi),
                getString(R.string.lang_urdu),
                getString(R.string.lang_gujarati),
                getString(R.string.lang_kannada)
        };
        ArrayAdapter<String> mediumAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                mediumOptions
        );
        b.etMedium.setAdapter(mediumAdapter);

        // Setup dropdown for Mother Tongue
        String[] motherTongueOptions = {
                getString(R.string.lang_marathi),
                getString(R.string.lang_hindi),
                getString(R.string.lang_english),
                getString(R.string.lang_urdu),
                getString(R.string.lang_gujarati),
                getString(R.string.lang_kannada),
                getString(R.string.lang_telugu),
                getString(R.string.lang_tamil),
                getString(R.string.lang_sindhi),
                getString(R.string.lang_punjabi),
                getString(R.string.lang_bengali)
        };
        ArrayAdapter<String> motherTongueAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                motherTongueOptions
        );
        b.etMotherTongue.setAdapter(motherTongueAdapter);

        b.tilDob.setEndIconOnClickListener(v -> showDatePicker(b.etDob));
        b.tilDateAdmission.setEndIconOnClickListener(v -> showDatePicker(b.etDateAdmission));

        b.btnSaveStudent.setOnClickListener(v -> save());
    }

    private void showDatePicker(android.widget.EditText et) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String currentText = et.getText().toString().trim();
        if (!android.text.TextUtils.isEmpty(currentText)) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
                java.util.Date parsedDate = sdf.parse(currentText);
                if (parsedDate != null) {
                    cal.setTime(parsedDate);
                }
            } catch (Exception ignored) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    java.util.Date parsedDate = sdf.parse(currentText);
                    if (parsedDate != null) {
                        cal.setTime(parsedDate);
                    }
                } catch (Exception ignored2) {}
            }
        }
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH);
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
        android.app.DatePickerDialog picker = new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            String dateStr = String.format(java.util.Locale.US, "%02d-%02d-%04d", d, m + 1, y);
            et.setText(dateStr);
        }, year, month, day);
        picker.show();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_help_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(this, "student_edit");
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        if (s.gender != null) {
            if (s.gender.equalsIgnoreCase("Female") || s.gender.equals("2")) {
                b.etGender.setText(getString(R.string.gender_female), false);
            } else if (s.gender.equalsIgnoreCase("Male") || s.gender.equals("1")) {
                b.etGender.setText(getString(R.string.gender_male), false);
            } else {
                b.etGender.setText(s.gender, false);
            }
        } else {
            b.etGender.setText("", false);
        }

        if (s.cast != null) {
            String normalizedCast = s.cast;
            String upper = s.cast.toUpperCase().trim();
            if (upper.equals("SC") || upper.contains("SCHEDULED CASTES") || upper.equals("1") || upper.equals("0")) {
                normalizedCast = "SC (Scheduled Castes)";
            } else if (upper.equals("ST") || upper.contains("SCHEDULED TRIBES") || upper.equals("2")) {
                normalizedCast = "ST (Scheduled Tribes)";
            } else if (upper.equals("VJ") || upper.contains("VIMUKT JATI") || upper.contains("VIMUKT") || upper.equals("3")) {
                normalizedCast = "VJ (Vimukt Jati)";
            } else if (upper.equals("NT") || upper.contains("NOMADIC TRIBES") || upper.contains("BHATKYA") || upper.equals("4")) {
                normalizedCast = "NT (Nomadic Tribes)";
            } else if (upper.equals("OBC") || upper.contains("OTHER BACKWARD CLASSES") || upper.contains("SBC") || upper.equals("5") || upper.equals("6")) {
                normalizedCast = "OBC (Other Backward Classes)";
            } else if (upper.equalsIgnoreCase("Open") || upper.equalsIgnoreCase("General") || upper.equals("7")) {
                normalizedCast = "Open";
            }
            b.etCast.setText(normalizedCast, false);
        } else {
            b.etCast.setText("", false);
        }
        set(b.etBirthPlace, s.birthPlace);
        set(b.etReligion, s.religion);
        set(b.etBloodGroup, s.bloodGroup);
        set(b.etStandard, s.standard);
        set(b.etDivision, s.division);
        set(b.etHeightSem1, s.heightSem1);
        set(b.etWeightSem1, s.weightSem1);
        set(b.etHeightSem2, s.heightSem2);
        set(b.etWeightSem2, s.weightSem2);
        set(b.etMotherName, s.motherName);
        set(b.etMotherOccupation, s.motherOccupation);
        set(b.etMotherPhone, s.motherPhone);
        set(b.etFatherName, s.fatherName);
        set(b.etFatherOccupation, s.fatherOccupation);
        set(b.etFatherPhone, s.fatherPhone);
        set(b.etAddress, s.address);
        set(b.etBankName, s.bankName);
        set(b.etBankAccount, s.bankAccount);
        set(b.etBankBranch, s.bankBranch);
        set(b.etBankIfsc, s.bankIfsc);
        set(b.etBankUid, s.bankUid);
        if (s.medium != null) {
            b.etMedium.setText(s.medium, false);
        } else {
            b.etMedium.setText("", false);
        }
        if (s.motherTongue != null) {
            b.etMotherTongue.setText(s.motherTongue, false);
        } else {
            b.etMotherTongue.setText("", false);
        }
        set(b.etDateAdmission, s.dateOfAdmission);
        set(b.etStudentId, s.studentIdNumber);
        set(b.etUid, s.uid);
    }

    private void set(EditText et, String v) {
        if (et != null) et.setText(v != null ? v : "");
    }

    private void save() {
        String name = str(b.etName);
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.msg_name_is_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Student s = student != null ? student : new Student();
        s.name = name;
        s.rollNo = str(b.etRoll1);
        s.rollNo2 = str(b.etRoll2);
        s.registrationNo = str(b.etRegNo);
        s.dob = str(b.etDob);
        String selectedGender = str(b.etGender);
        if (selectedGender.equals(getString(R.string.gender_female))) {
            s.gender = "Female";
        } else if (selectedGender.equals(getString(R.string.gender_male))) {
            s.gender = "Male";
        } else {
            s.gender = selectedGender;
        }
        s.cast = str(b.etCast);
        s.birthPlace = str(b.etBirthPlace);
        s.religion = str(b.etReligion);
        s.bloodGroup = str(b.etBloodGroup);
        s.standard = str(b.etStandard);
        s.division = str(b.etDivision);
        s.heightSem1 = str(b.etHeightSem1);
        s.weightSem1 = str(b.etWeightSem1);
        s.heightSem2 = str(b.etHeightSem2);
        s.weightSem2 = str(b.etWeightSem2);
        s.motherName = str(b.etMotherName);
        s.motherOccupation = str(b.etMotherOccupation);
        s.motherPhone = str(b.etMotherPhone);
        s.fatherName = str(b.etFatherName);
        s.fatherOccupation = str(b.etFatherOccupation);
        s.fatherPhone = str(b.etFatherPhone);
        s.address = str(b.etAddress);
        s.bankName = str(b.etBankName);
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
                Toast.makeText(StudentEditActivity.this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
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

    private String str(EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
