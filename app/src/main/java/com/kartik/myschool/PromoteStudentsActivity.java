package com.kartik.myschool;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.kartik.myschool.adapter.StudentPromoteAdapter;
import com.kartik.myschool.databinding.ActivityPromoteStudentsBinding;
import com.kartik.myschool.model.AcademicYear;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.Semester;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PromoteStudentsActivity extends AppCompatActivity {

    private ActivityPromoteStudentsBinding b;
    private StudentPromoteAdapter adapter;
    private ClassModel sourceClass;
    private final List<AcademicYear> academicYears = new ArrayList<>();
    private final List<ClassModel> targetYearClasses = new ArrayList<>();

    private static final String[] CLASSES = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
    private static final String[] DIVISIONS = {"No Division", "A", "B", "C", "D", "E"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPromoteStudentsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        sourceClass = SessionContext.selectedClass;
        if (sourceClass == null) {
            Toast.makeText(this, R.string.err_no_active_class, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupSourceClassDetails();
        setupSpinners();
        setupRecyclerView();
        loadAcademicYears();

        b.btnAddNewYear.setOnClickListener(v -> showAddNewYearDialog());
        b.btnProcessPromotion.setOnClickListener(v -> processRosterAdjustment());

        b.cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adapter != null) {
                adapter.selectAll(isChecked);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_help_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(this, "promote_students");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSourceClassDetails() {
        b.tvSourceYear.setText(sourceClass.academicYearLabel != null ? sourceClass.academicYearLabel : "—");
        b.tvSourceClass.setText(sourceClass.className != null ? "Class " + sourceClass.className : "—");
        
        String div = sourceClass.division;
        if (TextUtils.isEmpty(div) || "-".equals(div)) {
            div = "No Division";
        } else {
            div = "Div " + div;
        }
        b.tvSourceDivision.setText(div);
    }

    private void setupSpinners() {
        // Class Spinner
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CLASSES);
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spTargetClass.setAdapter(classAdapter);

        // Pre-select next class standard (Source + 1)
        int srcClassNum = 1;
        try {
            srcClassNum = Integer.parseInt(sourceClass.className);
        } catch (NumberFormatException ignored) {}
        
        int targetIdx = srcClassNum; // index for srcClassNum + 1 (since index 0 = "1", index 1 = "2", etc.)
        if (targetIdx >= CLASSES.length) {
            targetIdx = CLASSES.length - 1;
        }
        b.spTargetClass.setSelection(targetIdx);

        // Division Spinner
        ArrayAdapter<String> divAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, DIVISIONS);
        divAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spTargetDivision.setAdapter(divAdapter);

        // Pre-select matching division
        String srcDiv = sourceClass.division;
        int divIdx = 0; // Default: No Division
        if (!TextUtils.isEmpty(srcDiv) && !"-".equals(srcDiv)) {
            for (int i = 0; i < DIVISIONS.length; i++) {
                if (DIVISIONS[i].equalsIgnoreCase(srcDiv)) {
                    divIdx = i;
                    break;
                }
            }
        }
        b.spTargetDivision.setSelection(divIdx);

        // Listen for selections to check if target class already exists
        AdapterView.OnItemSelectedListener checkListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkIfTargetClassExists();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        b.spTargetClass.setOnItemSelectedListener(checkListener);
        b.spTargetDivision.setOnItemSelectedListener(checkListener);
    }

    private void setupRecyclerView() {
        adapter = new StudentPromoteAdapter();
        adapter.setListener((checkedCount, totalCount) -> {
            b.cbSelectAll.setOnCheckedChangeListener(null);
            b.cbSelectAll.setChecked(checkedCount == totalCount && totalCount > 0);
            b.cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.selectAll(isChecked));
            
            b.btnProcessPromotion.setText(getString(R.string.btn_execute_adjustment_count, checkedCount));
            b.btnProcessPromotion.setEnabled(checkedCount > 0);
        });

        b.rvStudentsChecklist.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        b.rvStudentsChecklist.setAdapter(adapter);

        // Load students
        showLoading(true);
        FirebaseRepository.get().getStudentsForClass(sourceClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (students == null || students.isEmpty()) {
                        Toast.makeText(PromoteStudentsActivity.this, R.string.err_no_students_found, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        adapter.setData(students);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PromoteStudentsActivity.this, getString(R.string.err_failed_load_students, e.getMessage()), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void loadAcademicYears() {
        FirebaseRepository.get().getAcademicYears(new FirebaseRepository.OnResult<List<AcademicYear>>() {
            @Override
            public void onSuccess(List<AcademicYear> list) {
                academicYears.clear();
                if (list != null) {
                    academicYears.addAll(list);
                }
                
                // Sort years descending
                Collections.sort(academicYears, (a, b) -> Integer.compare(b.startYear, a.startYear));

                runOnUiThread(() -> {
                    List<String> yearLabels = new ArrayList<>();
                    for (AcademicYear y : academicYears) {
                        yearLabels.add(y.label);
                    }
                    
                    ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(PromoteStudentsActivity.this,
                            android.R.layout.simple_spinner_item, yearLabels);
                    yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    b.spTargetYear.setAdapter(yearAdapter);

                    // Pre-select current target year if available
                    b.spTargetYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            loadClassesForSelectedYear();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    if (!academicYears.isEmpty()) {
                        loadClassesForSelectedYear();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(PromoteStudentsActivity.this, R.string.err_failed_load_years, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadClassesForSelectedYear() {
        int pos = b.spTargetYear.getSelectedItemPosition();
        if (pos < 0 || pos >= academicYears.size()) return;

        AcademicYear selectedYear = academicYears.get(pos);
        FirebaseRepository.get().getClassesForYear(selectedYear.id, new FirebaseRepository.OnResult<List<ClassModel>>() {
            @Override
            public void onSuccess(List<ClassModel> list) {
                targetYearClasses.clear();
                if (list != null) {
                    targetYearClasses.addAll(list);
                }
                runOnUiThread(() -> checkIfTargetClassExists());
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> checkIfTargetClassExists());
            }
        });
    }

    private void checkIfTargetClassExists() {
        int yearPos = b.spTargetYear.getSelectedItemPosition();
        if (yearPos < 0 || yearPos >= academicYears.size()) {
            b.tvTargetClassStatus.setText(R.string.err_select_valid_year);
            b.tvTargetClassStatus.setTextColor(getResources().getColor(R.color.error));
            return;
        }

        String targetClass = b.spTargetClass.getSelectedItem().toString();
        String targetDiv = b.spTargetDivision.getSelectedItem().toString();
        if (targetDiv.equalsIgnoreCase("No Division")) {
            targetDiv = "-";
        }

        boolean exists = false;
        for (ClassModel c : targetYearClasses) {
            if (TextUtils.equals(c.className, targetClass) && TextUtils.equals(c.division, targetDiv)) {
                exists = true;
                break;
            }
        }

        if (exists) {
            b.tvTargetClassStatus.setText(R.string.status_class_ready);
            b.tvTargetClassStatus.setTextColor(ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7D32")));
        } else {
            b.tvTargetClassStatus.setText(R.string.status_class_not_exist);
            b.tvTargetClassStatus.setTextColor(ColorStateList.valueOf(android.graphics.Color.parseColor("#E65100")));
        }
    }

    private void showAddNewYearDialog() {
        EditText input = new EditText(this);

        // Auto-suggest the next academic year label based on current years in list
        int suggestedStart = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        if (!academicYears.isEmpty()) {
            // Find the highest startYear already in the list and add 1
            int maxStart = 0;
            for (AcademicYear ay : academicYears) {
                if (ay.startYear > maxStart) maxStart = ay.startYear;
            }
            suggestedStart = maxStart + 1;
        }
        String suggestedEnd = String.valueOf((suggestedStart + 1) % 100).length() == 1
                ? "0" + ((suggestedStart + 1) % 100)
                : String.valueOf((suggestedStart + 1) % 100);
        String suggestedLabel = suggestedStart + "-" + suggestedEnd;

        input.setHint("e.g., " + suggestedLabel);
        input.setText(suggestedLabel);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_add_academic_year)
                .setMessage(R.string.msg_enter_year_label)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String raw = input.getText() != null ? input.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(raw)) {
                        Toast.makeText(this, R.string.msg_year_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Auto-format: if user enters just "2027" expand to "2027-28"
                    String label = raw;
                    if (!label.contains("-")) {
                        try {
                            int startY = Integer.parseInt(label);
                            int endY = (startY + 1) % 100;
                            String endStr = endY < 10 ? "0" + endY : String.valueOf(endY);
                            label = startY + "-" + endStr;
                        } catch (NumberFormatException ignored) {
                            // Not a plain number — keep as-is
                        }
                    }
                    saveNewAcademicYear(label);
                })
                .show();
    }

    private void saveNewAcademicYear(String label) {
        // Parse start and end years
        int start = 2026;
        int end = 2027;
        
        try {
            if (label.contains("-")) {
                String[] parts = label.split("-");
                start = Integer.parseInt(parts[0].trim());
                String endPart = parts[1].trim();
                if (endPart.length() == 2) {
                    end = start - (start % 100) + Integer.parseInt(endPart);
                } else {
                    end = Integer.parseInt(endPart);
                }
            } else {
                start = Integer.parseInt(label);
                end = start + 1;
            }
        } catch (Exception ignored) {}

        showLoading(true);
        AcademicYear newYear = new AcademicYear(label, start, end);
        newYear.schoolId = (AppCache.selectedSchool != null) ? AppCache.selectedSchool.id : "";

        FirebaseRepository.get().saveAcademicYear(newYear, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String yearId) {
                newYear.id = yearId;
                
                // Seed semesters for this year
                Semester s1 = new Semester(1, "First Semester", "First half evaluation");
                s1.yearId = yearId;
                Semester s2 = new Semester(2, "Second Semester", "Final evaluation");
                s2.yearId = yearId;

                FirebaseRepository.get().saveSemester(s1, new FirebaseRepository.OnResult<String>() {
                    @Override
                    public void onSuccess(String id1) {
                        FirebaseRepository.get().saveSemester(s2, new FirebaseRepository.OnResult<String>() {
                            @Override
                            public void onSuccess(String id2) {
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(PromoteStudentsActivity.this, R.string.msg_year_sem_created, Toast.LENGTH_SHORT).show();
                                    loadAcademicYears();
                                });
                            }
                            @Override
                            public void onError(Exception e) { handleErr(e); }
                        });
                    }
                    @Override
                    public void onError(Exception e) { handleErr(e); }
                });
            }

            @Override
            public void onError(Exception e) { handleErr(e); }

            private void handleErr(Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PromoteStudentsActivity.this, getString(R.string.err_failed_create_year, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void processRosterAdjustment() {
        int yearPos = b.spTargetYear.getSelectedItemPosition();
        if (yearPos < 0 || yearPos >= academicYears.size()) {
            Toast.makeText(this, R.string.err_select_valid_year, Toast.LENGTH_SHORT).show();
            return;
        }

        AcademicYear targetYear = academicYears.get(yearPos);
        String targetClassNum = b.spTargetClass.getSelectedItem().toString();
        String targetDiv = b.spTargetDivision.getSelectedItem().toString();
        if (targetDiv.equalsIgnoreCase("No Division")) {
            targetDiv = "-";
        }

        List<Student> selectedStudents = adapter.getSelectedStudents();
        if (selectedStudents.isEmpty()) {
            Toast.makeText(this, R.string.err_select_one_student, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isPromoteCopy = b.rbModePromote.isChecked();

        showLoading(true);

        // Find or create target class
        findOrCreateTargetClass(targetYear, targetClassNum, targetDiv, new FirebaseRepository.OnResult<ClassModel>() {
            @Override
            public void onSuccess(ClassModel targetClassObj) {
                // Perform batch updates for student records
                executeStudentAdjustment(selectedStudents, targetClassObj, isPromoteCopy);
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PromoteStudentsActivity.this, getString(R.string.err_failed_init_class, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void findOrCreateTargetClass(AcademicYear targetYear, String className, String division, FirebaseRepository.OnResult<ClassModel> cb) {
        // Check local list first
        ClassModel matched = null;
        for (ClassModel c : targetYearClasses) {
            if (TextUtils.equals(c.className, className) && TextUtils.equals(c.division, division)) {
                matched = c;
                break;
            }
        }

        if (matched != null) {
            cb.onSuccess(matched);
            return;
        }

        // Target class does not exist. Fetch target year's semesters first to set class semester ID.
        FirebaseRepository.get().getSemestersForYear(targetYear.id, new FirebaseRepository.OnResult<List<Semester>>() {
            @Override
            public void onSuccess(List<Semester> semesters) {
                if (semesters == null || semesters.isEmpty()) {
                    // Seed standard semesters
                    Semester s1 = new Semester(1, "First Semester", "First half evaluation");
                    s1.yearId = targetYear.id;
                    FirebaseRepository.get().saveSemester(s1, new FirebaseRepository.OnResult<String>() {
                        @Override
                        public void onSuccess(String semId) {
                            createAndSaveTargetClass(targetYear, semId, className, division, cb);
                        }

                        @Override
                        public void onError(Exception e) { cb.onError(e); }
                    });
                } else {
                    String semId = semesters.get(0).id;
                    createAndSaveTargetClass(targetYear, semId, className, division, cb);
                }
            }

            @Override
            public void onError(Exception e) { cb.onError(e); }
        });
    }

    private void createAndSaveTargetClass(AcademicYear year, String semId, String className, String division, FirebaseRepository.OnResult<ClassModel> cb) {
        ClassModel c = new ClassModel();
        c.schoolId = (AppCache.selectedSchool != null) ? AppCache.selectedSchool.id : "";
        c.yearId = year.id;
        c.academicYearLabel = year.label;
        c.semesterId = semId;
        c.className = className;
        c.division = division;
        c.examName = "First Semester";
        c.year = year.startYear;
        c.subjects = new ArrayList<>(Subject.getDefaultSubjectsForClass(className));
        c.studentCount = 0;

        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
            @Override
            public void onSuccess(com.kartik.myschool.model.Teacher teacher) {
                if (teacher != null) {
                    c.teacherName = teacher.name;
                    c.teacherEmail = teacher.email;
                    c.teacherPhone = teacher.phone;
                }
                
                FirebaseRepository.get().saveClass(c, new FirebaseRepository.OnResult<String>() {
                    @Override
                    public void onSuccess(String classId) {
                        c.id = classId;
                        cb.onSuccess(c);
                    }

                    @Override
                    public void onError(Exception e) { cb.onError(e); }
                });
            }

            @Override
            public void onError(Exception e) { cb.onError(e); }
        });
    }

    private void executeStudentAdjustment(List<Student> students, ClassModel targetClassObj, boolean isCopy) {
        AtomicInteger remaining = new AtomicInteger(students.size());
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (Student s : students) {
            Student adjustStudent;
            if (isCopy) {
                // Create clean copy of the student profile
                adjustStudent = new Student();
                adjustStudent.name = s.name;
                adjustStudent.gender = s.gender;
                adjustStudent.dob = s.dob;
                adjustStudent.cast = s.cast;
                adjustStudent.parentName = s.parentName;
                adjustStudent.motherName = s.motherName;
                adjustStudent.motherOccupation = s.motherOccupation;
                adjustStudent.motherPhone = s.motherPhone;
                adjustStudent.fatherName = s.fatherName;
                adjustStudent.fatherOccupation = s.fatherOccupation;
                adjustStudent.fatherPhone = s.fatherPhone;
                adjustStudent.address = s.address;
                adjustStudent.bankAccount = s.bankAccount;
                adjustStudent.bankBranch = s.bankBranch;
                adjustStudent.bankIfsc = s.bankIfsc;
                adjustStudent.bankUid = s.bankUid;
                adjustStudent.medium = s.medium;
                adjustStudent.motherTongue = s.motherTongue;
                adjustStudent.dateOfAdmission = s.dateOfAdmission;
                adjustStudent.studentIdNumber = s.studentIdNumber;
                adjustStudent.uid = s.uid;
                adjustStudent.registrationNo = s.registrationNo;
                adjustStudent.rollNo = s.rollNo;
                adjustStudent.rollNo2 = s.rollNo2;
                adjustStudent.marksEntered = false;
            } else {
                // Update pointers on existing student document directly
                adjustStudent = s;
            }

            // Bind target class properties
            adjustStudent.classId = targetClassObj.id;
            adjustStudent.className = targetClassObj.getDisplayName();
            adjustStudent.standard = targetClassObj.className;
            adjustStudent.division = targetClassObj.division;
            adjustStudent.schoolId = targetClassObj.schoolId;
            adjustStudent.schoolName = (AppCache.selectedSchool != null) ? AppCache.selectedSchool.name : "My School";
            adjustStudent.teacherId = FirebaseRepository.get().currentUid();

            FirebaseRepository.get().saveStudent(adjustStudent, new FirebaseRepository.OnResult<String>() {
                @Override
                public void onSuccess(String id) {
                    successes.incrementAndGet();
                    checkAdjustmentComplete(remaining.decrementAndGet(), successes.get(), failures.get());
                }

                @Override
                public void onError(Exception e) {
                    failures.incrementAndGet();
                    checkAdjustmentComplete(remaining.decrementAndGet(), successes.get(), failures.get());
                }
            });
        }
    }

    private void checkAdjustmentComplete(int rem, int succ, int fail) {
        if (rem == 0) {
            runOnUiThread(() -> {
                showLoading(false);
                
                // Clear AppCache cached student count & list map, forcing refresh
                AppCache.cachedStudentCountByClassId = null;
                AppCache.cachedClasses = null;

                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_process_complete)
                        .setMessage(getString(R.string.msg_adjustment_complete, succ, fail))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            });
        }
    }

    private void showLoading(boolean show) {
        b.progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnProcessPromotion.setEnabled(!show);
    }
}
