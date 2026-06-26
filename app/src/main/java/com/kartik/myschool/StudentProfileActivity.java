package com.kartik.myschool;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.kartik.myschool.databinding.ActivityStudentProfileBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.DetailRowHelper;
import com.kartik.myschool.utils.PdfGenerator;
import com.kartik.myschool.utils.UiAnimations;
import com.google.android.material.chip.Chip;

import java.io.File;
import java.util.List;

public class StudentProfileActivity extends BaseActivity {

    private ActivityStudentProfileBinding b;
    private Student student;
    private ImageView dialogPhotoPreview;
    private android.graphics.Bitmap currentPhotoBitmap;
    private Uri tempCameraUri;
    private static final int REQ_CODE_CAMERA = 1001;
    private static final int REQ_CODE_GALLERY = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SessionContext.load(this);
        super.onCreate(savedInstanceState); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Screen opened: " + this.getClass().getSimpleName());
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
        b.btnGenerateReport.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnGenerateReport);
            showReportSelectionDialog();
        });
        b.btnParentCode.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnParentCode);
            showParentCodeDialog();
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
        b.ivStudentPhoto.setOnClickListener(v -> {
            UiAnimations.pulse(b.ivStudentPhoto);
            showPhotoEditDialog();
        });
        loadStudent();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_help_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(this, "student_profile");
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        if (("â€”".equals(std) || "-".equals(std)) && s.className != null) {
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
        b.tvTopGender.setText(getDisplayGender(s.gender));
        b.tvTopCast.setText(getDisplayCast(s.cast));

        // Load student photo or avatar placeholder
        loadStudentPhoto(s, b.ivStudentPhoto);
        // Load student photo into header banner background (blurred/scaled behind name)
        loadHeaderBannerPhoto(s);

        bindMarksChip(s);

        DetailRowHelper.fillRows(this, b.llBasicDetails, new String[][]{
                {getString(R.string.label_roll_no_1), s.rollNo},
                {getString(R.string.label_roll_no_2), s.rollNo2},
                {getString(R.string.label_reg_no), s.registrationNo},
                {getString(R.string.label_dob), s.dob},
                {getString(R.string.label_gender), getDisplayGender(s.gender)},
                {getString(R.string.label_cast), getDisplayCast(s.cast)},
                {getString(R.string.label_birthplace), s.birthPlace},
                {getString(R.string.label_religion), s.religion},
                {getString(R.string.label_blood_group), s.bloodGroup},
                {getString(R.string.label_standard), s.standard},
                {getString(R.string.label_division), s.division},
                {getString(R.string.label_school_name), s.schoolName},
                {getString(R.string.label_class_short), s.className},
                {getString(R.string.label_height_sem1), s.heightSem1},
                {getString(R.string.label_weight_sem1), s.weightSem1},
                {getString(R.string.label_height_sem2), s.heightSem2},
                {getString(R.string.label_weight_sem2), s.weightSem2},
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
                {getString(R.string.label_bank_name), s.bankName},
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
        return v != null && !v.isEmpty() ? v : "-";
    }

    private String getDisplayGender(String gender) {
        if (gender == null || gender.isEmpty()) return "â€”";
        String g = gender.trim().toLowerCase();
        if (g.equals("1") || g.equals("male") || g.equals("boy") || g.equals("à¤ªà¥à¤°à¥à¤·") || g.equals("à¤®à¥à¤²à¤—à¤¾")) {
            return "M";
        }
        if (g.equals("2") || g.equals("female") || g.equals("girl") || g.equals("à¤¸à¥à¤¤à¥à¤°à¥€") || g.equals("à¤®à¥à¤²à¤—à¥€")) {
            return "F";
        }
        if (!gender.trim().isEmpty()) {
            return gender.trim().substring(0, Math.min(gender.trim().length(), 1)).toUpperCase();
        }
        return "â€”";
    }

    private String getDisplayCast(String cast) {
        if (cast == null || cast.isEmpty()) return "â€”";
        String trim = cast.trim();
        if (trim.equals("0") || trim.equals("1") || trim.equalsIgnoreCase("SC")) {
            return "SC";
        }
        if (trim.equals("2") || trim.equalsIgnoreCase("ST")) {
            return "ST";
        }
        if (trim.equals("3") || trim.equalsIgnoreCase("VJ")) {
            return "VJ";
        }
        if (trim.equals("4") || trim.equalsIgnoreCase("NT")) {
            return "NT";
        }
        if (trim.equals("5") || trim.equalsIgnoreCase("OBC")) {
            return "OBC";
        }
        if (trim.equals("6") || trim.equalsIgnoreCase("SBC")) {
            return "SBC";
        }
        if (trim.equals("7") || trim.equalsIgnoreCase("Open")) {
            return "Open";
        }
        return getShortCast(cast);
    }

    private String getShortCast(String cast) {
        if (cast == null || cast.isEmpty()) return "â€”";
        String trim = cast.trim();
        if (trim.equals("0") || trim.equals("1")) return "SC";
        if (trim.equals("2")) return "ST";
        if (trim.equals("3")) return "VJ";
        if (trim.equals("4")) return "NT";
        if (trim.equals("5")) return "OBC";
        if (trim.equals("6")) return "SBC";
        if (trim.equals("7")) return "Open";

        String upper = trim.toUpperCase();
        if (upper.equals("SC") || upper.contains("SCHEDULED CASTES") || upper.contains("SCHEDULED CASTE") || upper.contains("à¤…à¤¨à¥à¤¸à¥‚à¤šà¤¿à¤¤ à¤œà¤¾à¤¤à¥€") || upper.contains("à¤…à¤¨à¥. à¤œà¤¾à¤¤à¥€")) {
            return "SC";
        }
        if (upper.equals("ST") || upper.contains("SCHEDULED TRIBES") || upper.contains("SCHEDULED TRIBE") || upper.contains("à¤…à¤¨à¥à¤¸à¥‚à¤šà¤¿à¤¤ à¤œà¤®à¤¾à¤¤à¥€") || upper.contains("à¤…à¤¨à¥. à¤œà¤®à¤¾à¤¤à¥€")) {
            return "ST";
        }
        if (upper.equals("VJ") || upper.contains("VIMUKT JATI") || upper.contains("VIMUKT") || upper.contains("à¤µà¤¿à¤®à¥à¤•à¥à¤¤")) {
            return "VJ";
        }
        if (upper.equals("NT") || upper.contains("NOMADIC TRIBES") || upper.contains("NOMADIC TRIBE") || upper.contains("BHATKYA") || upper.contains("à¤­à¤Ÿà¤•à¥à¤¯à¤¾") || upper.contains("à¤­.à¤œ.")) {
            return "NT";
        }
        if (upper.equals("OBC") || upper.contains("OTHER BACKWARD") || upper.contains("à¤‡à¤¤à¤° à¤®à¤¾à¤—à¤¾à¤¸") || upper.contains("à¤‡.à¤®à¤¾.à¤µ.")) {
            return "OBC";
        }
        if (upper.equals("SBC") || upper.contains("SPECIAL BACKWARD") || upper.contains("à¤µà¤¿à¤¶à¥‡à¤· à¤®à¤¾à¤—à¤¾à¤¸") || upper.contains("à¤µà¤¿.à¤®à¤¾.à¤ªà¥à¤°.")) {
            return "SBC";
        }
        if (upper.equals("OPEN") || upper.contains("GENERAL") || upper.contains("à¤–à¥à¤²à¤¾")) {
            return "Open";
        }
        if (cast.contains("(")) {
            String before = cast.substring(0, cast.indexOf("(")).trim();
            if (!before.isEmpty()) {
                return before;
            }
        }
        if (cast.length() > 6) {
            return cast.substring(0, 6).trim();
        }
        return cast;
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

    private String[] getSemesterIds() {
        String sem1Id = "sem_1";
        String sem2Id = "sem_2";
        if (com.kartik.myschool.AppCache.cachedSemesters != null) {
            for (com.kartik.myschool.model.Semester sem : com.kartik.myschool.AppCache.cachedSemesters) {
                if (sem.number == 1 && sem.id != null && !sem.id.isEmpty()) {
                    sem1Id = sem.id;
                } else if (sem.number == 2 && sem.id != null && !sem.id.isEmpty()) {
                    sem2Id = sem.id;
                }
            }
        }
        if (sem1Id == null || sem1Id.isEmpty()) sem1Id = "sem_1";
        if (sem2Id == null || sem2Id.isEmpty()) sem2Id = "sem_2";
        return new String[] { sem1Id, sem2Id };
    }

    private void openPdfFile(File file) {
        if (file == null || !file.exists()) return;
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            sharePdfFile(file);
        }
    }

    private void sharePdfFile(File file) {
        if (file == null || !file.exists()) return;
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share Report PDF"));
        } catch (Exception e) {
            Toast.makeText(this, R.string.msg_pdf, Toast.LENGTH_LONG).show();
        }
    }

    private void loadPrerequisitesThen(Runnable next) {
        loadClassThen(() -> {
            if (AppCache.selectedClass == null) {
                runOnUiThread(() -> Toast.makeText(this, R.string.select_class_first, Toast.LENGTH_SHORT).show());
                return;
            }
            SessionContext.selectedClass = AppCache.selectedClass;
            // 1. Load school if needed
            if (SessionContext.selectedSchool == null && student.schoolId != null) {
                FirebaseRepository.get().getSchools(new FirebaseRepository.OnResult<List<com.kartik.myschool.model.School>>() {
                    @Override
                    public void onSuccess(List<com.kartik.myschool.model.School> schools) {
                        if (schools != null) {
                            for (com.kartik.myschool.model.School school : schools) {
                                if (school.id != null && school.id.equals(student.schoolId)) {
                                    SessionContext.selectedSchool = school;
                                    AppCache.selectedSchool = school;
                                    break;
                                }
                            }
                            if (SessionContext.selectedSchool == null && !schools.isEmpty()) {
                                SessionContext.selectedSchool = schools.get(0);
                                AppCache.selectedSchool = schools.get(0);
                            }
                        }
                        loadSemestersThen(next);
                    }
                    @Override
                    public void onError(Exception e) {
                        loadSemestersThen(next);
                    }
                });
            } else {
                if (SessionContext.selectedSchool == null && AppCache.selectedSchool != null) {
                    SessionContext.selectedSchool = AppCache.selectedSchool;
                }
                loadSemestersThen(next);
            }
        });
    }

    private void loadSemestersThen(Runnable next) {
        boolean cacheValid = false;
        if (AppCache.cachedSemesters != null && !AppCache.cachedSemesters.isEmpty()) {
            cacheValid = true;
            for (com.kartik.myschool.model.Semester sem : AppCache.cachedSemesters) {
                if (sem.id == null || sem.id.isEmpty()) {
                    cacheValid = false;
                    break;
                }
            }
        }
        if (cacheValid) {
            next.run();
            return;
        }
        String yearId = SessionContext.selectedYear != null ? SessionContext.selectedYear.id : 
                        (AppCache.selectedClass != null ? AppCache.selectedClass.yearId : null);
        if (yearId == null) {
            next.run();
            return;
        }
        FirebaseRepository.clearCache(); // Force clear repository cache to map IDs freshly
        FirebaseRepository.get().getSemestersForYear(yearId, new FirebaseRepository.OnResult<List<com.kartik.myschool.model.Semester>>() {
            @Override
            public void onSuccess(List<com.kartik.myschool.model.Semester> list) {
                if (list != null) {
                    AppCache.cachedSemesters = list;
                }
                runOnUiThread(next);
            }
            @Override
            public void onError(Exception e) {
                runOnUiThread(next);
            }
        });
    }

    private void openReport() {
        loadPrerequisitesThen(() -> {
            Toast.makeText(this, student.name + " à¤šà¤¾ à¤°à¤¿à¤ªà¥‹à¤°à¥à¤Ÿ à¤¤à¤¯à¤¾à¤° à¤¹à¥‹à¤¤ à¤†à¤¹à¥‡...", Toast.LENGTH_SHORT).show();
            String classId = (AppCache.selectedClass != null && AppCache.selectedClass.id != null) 
                             ? AppCache.selectedClass.id : student.classId;
            String[] sids = getSemesterIds();
            FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, sids[0], new FirebaseRepository.OnResult<MarksRecord>() {
                @Override
                public void onSuccess(MarksRecord s1) {
                    FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, sids[1], new FirebaseRepository.OnResult<MarksRecord>() {
                        @Override
                        public void onSuccess(MarksRecord s2) {
                            if (s1 == null && s2 == null) {
                                runOnUiThread(() -> Toast.makeText(StudentProfileActivity.this, R.string.no_marks_yet, Toast.LENGTH_SHORT).show());
                                return;
                            }
                            com.kartik.myschool.utils.pdf.GunapattrakGenerator.generateGunapattrak(StudentProfileActivity.this, SessionContext.selectedSchool, AppCache.selectedClass, student, s1, s2, new PdfGenerator.PdfCallback() {
                                @Override
                                public void onSuccess(File pdfFile) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(StudentProfileActivity.this, R.string.msg_empty_3, Toast.LENGTH_SHORT).show();
                                        openPdfFile(pdfFile);
                                        com.kartik.myschool.utils.AnalyticsHelper.logPdfGenerated("gunapattrak");
                                        com.kartik.myschool.utils.ReviewHelper.incrementPdfCountAndCheck(StudentProfileActivity.this);
                                    });
                                }
                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(StudentProfileActivity.this, "à¤¤à¥à¤°à¥à¤Ÿà¥€ à¤†à¤¢à¤³li: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                        }
                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> Toast.makeText(StudentProfileActivity.this, "à¤¦à¥à¤µà¤¿à¤¤à¥€à¤¯à¤¾ à¤¸à¤¤à¥à¤°à¤¾à¤šà¥‡ à¤—à¥à¤£ à¤®à¤¿à¤³à¤µà¤£à¥à¤¯à¤¾à¤¤ à¤…à¤ªà¤¯à¤¶ à¤†à¤²à¥‡: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                }
                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(StudentProfileActivity.this, "à¤ªà¥à¤°à¤¥à¤® à¤¸à¤¤à¥à¤°à¤¾à¤šà¥‡ à¤—à¥à¤£ à¤®à¤¿à¤³à¤µà¤£à¥à¤¯à¤¾à¤¤ à¤…à¤ªà¤¯à¤¶ à¤†à¤²à¥‡: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        });
    }

    private void showReportSelectionDialog() {
        String[] reportNames = {
            getString(R.string.report_title_12), // 12. Annual Marksheet
            getString(R.string.report_title_13), // 13. Result Sheet
            getString(R.string.report_title_14), // 14. Gunapattrak (Progress Card Inner)
            getString(R.string.report_title_19), // Holistic Progress Card
            getString(R.string.report_title_4)   // Descriptive Remarks
        };
        final int[] reportPositions = {11, 12, 13, 18, 3}; // Corresponding positions in ReportPrintingFragment

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("अहवाल निवडा")
            .setItems(reportNames, (dialog, which) -> {
                generateReportForStudent(reportPositions[which], reportNames[which]);
            })
            .show();
    }

    private void generateReportForStudent(int reportIndex, String reportName) {
        loadPrerequisitesThen(() -> {
            com.kartik.myschool.utils.LoadingDialog pd = new com.kartik.myschool.utils.LoadingDialog(this, null, "Generating " + reportName + "...");
            pd.show();
            
            String classId = (AppCache.selectedClass != null && AppCache.selectedClass.id != null) 
                             ? AppCache.selectedClass.id : student.classId;
            String[] sids = getSemesterIds();
            FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, sids[0], new FirebaseRepository.OnResult<MarksRecord>() {
                @Override
                public void onSuccess(MarksRecord s1) {
                    FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, sids[1], new FirebaseRepository.OnResult<MarksRecord>() {
                        @Override
                        public void onSuccess(MarksRecord s2) {
                            if (s1 == null && s2 == null) {
                                runOnUiThread(() -> {
                                    pd.dismiss();
                                    Toast.makeText(StudentProfileActivity.this, R.string.no_marks_yet, Toast.LENGTH_SHORT).show();
                                });
                                return;
                            }
                            
                            PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
                                @Override
                                public void onSuccess(File pdfFile) {
                                    runOnUiThread(() -> {
                                        pd.dismiss();
                                        Toast.makeText(StudentProfileActivity.this, R.string.msg_empty_3, Toast.LENGTH_SHORT).show();
                                        openPdfFile(pdfFile);
                                        com.kartik.myschool.utils.AnalyticsHelper.logPdfGenerated("single_report_" + reportIndex);
                                        com.kartik.myschool.utils.ReviewHelper.incrementPdfCountAndCheck(StudentProfileActivity.this);
                                    });
                                }
                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() -> {
                                        pd.dismiss();
                                        Toast.makeText(StudentProfileActivity.this, "Error generating report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                                }
                            };

                            List<Student> list = java.util.Collections.singletonList(student);
                            java.util.Map<String, MarksRecord> map1 = s1 != null ? java.util.Collections.singletonMap(student.id, s1) : new java.util.HashMap<>();
                            java.util.Map<String, MarksRecord> map2 = s2 != null ? java.util.Collections.singletonMap(student.id, s2) : new java.util.HashMap<>();

                            switch (reportIndex) {
                                case 11: // Annual Marksheet
                                    com.kartik.myschool.utils.pdf.AnnualMarksheetGenerator.generateAnnualMarksheet(StudentProfileActivity.this, SessionContext.selectedSchool, SessionContext.selectedClass, list, map2, cb);
                                    break;
                                case 12: // Result Sheet
                                    com.kartik.myschool.utils.pdf.ResultSheetGenerator.generateResultSheet(StudentProfileActivity.this, SessionContext.selectedSchool, SessionContext.selectedClass, list, map2, cb);
                                    break;
                                case 13: // Gunapattrak (Progress Card Inner)
                                    com.kartik.myschool.utils.pdf.ProgressCardPortraitGenerator.generateProgressCardPortrait(StudentProfileActivity.this, SessionContext.selectedSchool, SessionContext.selectedClass, list, cb);
                                    break;
                                case 0: // Cover Page
                                    com.kartik.myschool.utils.pdf.CoverPageGenerator.generateCoverPage(StudentProfileActivity.this, SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                                    break;
                                case 18: // HPC
                                    com.kartik.myschool.utils.pdf.HPCGenerator.generateHPC(StudentProfileActivity.this, SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                                    break;
                                case 3: // Descriptive Remarks
                                    com.kartik.myschool.utils.pdf.DescriptiveRemarksGenerator.generateDescriptive(StudentProfileActivity.this, SessionContext.selectedSchool, SessionContext.selectedClass, list, map1, map2, cb);
                                    break;
                                default:
                                    com.kartik.myschool.utils.PdfGenerator.generatePersonalityRecord(StudentProfileActivity.this, SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                                    break;
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                pd.dismiss();
                                Toast.makeText(StudentProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                }
                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        pd.dismiss();
                        Toast.makeText(StudentProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
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

    private void loadStudentPhoto(Student s, ImageView iv) {
        if (s.photoUrl != null && !s.photoUrl.isEmpty()) {
            iv.setPadding(0, 0, 0, 0);
            iv.setImageTintList(null);
            if (s.photoUrl.startsWith("data:image")) {
                try {
                    String base64Data = s.photoUrl.substring(s.photoUrl.indexOf(",") + 1);
                    byte[] decodedString = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    iv.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    loadPlaceholderPhoto(s, iv);
                }
            } else {
                com.bumptech.glide.Glide.with(this)
                        .load(s.photoUrl)
                        .centerCrop()
                        .placeholder(getPlaceholderRes(s))
                        .into(iv);
            }
        } else {
            loadPlaceholderPhoto(s, iv);
        }
    }

    /**
     * Loads the student photo into the header banner background ImageView (ivHeaderBg).
     * If the student has a photo, it is loaded full-bleed; otherwise the default drawable is used.
     */
    private void loadHeaderBannerPhoto(Student s) {
        if (b.ivHeaderBg == null) return;
        if (s.photoUrl != null && !s.photoUrl.isEmpty()) {
            if (s.photoUrl.startsWith("data:image")) {
                try {
                    String base64Data = s.photoUrl.substring(s.photoUrl.indexOf(",") + 1);
                    byte[] decoded = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    b.ivHeaderBg.setImageBitmap(bmp);
                    b.ivHeaderBg.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                } catch (Exception e) {
                    b.ivHeaderBg.setImageResource(R.drawable.bg_profile_header);
                }
            } else {
                com.bumptech.glide.Glide.with(this)
                        .load(s.photoUrl)
                        .centerCrop()
                        .placeholder(R.drawable.bg_profile_header)
                        .into(b.ivHeaderBg);
            }
        } else {
            // No photo selected â€” show plain color (FrameLayout background shows through)
            b.ivHeaderBg.setImageDrawable(null);
        }
    }

    private int getPlaceholderRes(Student s) {
        if (s.gender != null) {
            String g = s.gender.toLowerCase().trim();
            if (g.contains("female") || g.contains("à¤¸à¥à¤¤à¥à¤°à¥€") || g.contains("à¤®à¥à¤²à¤—à¥€")
                    || g.equals("2") || g.equals("f")) {
                return R.drawable.ic_girl_avatar;
            }
        }
        return R.drawable.ic_boy_avatar;
    }

    private void loadPlaceholderPhoto(Student s, ImageView iv) {
        iv.setImageResource(getPlaceholderRes(s));
        iv.setPadding(0, 0, 0, 0);
        iv.setImageTintList(null);
    }

    private void showPhotoEditDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_photo_edit, null);
        dialogPhotoPreview = view.findViewById(R.id.ivPhotoPreview);
        android.widget.ImageButton btnCamera = view.findViewById(R.id.btnCamera);
        android.widget.ImageButton btnCrop = view.findViewById(R.id.btnCrop);
        android.widget.ImageButton btnDelete = view.findViewById(R.id.btnDelete);
        android.widget.ImageButton btnRotate = view.findViewById(R.id.btnRotate);
        android.widget.ImageButton btnSave = view.findViewById(R.id.btnSave);

        loadStudentPhoto(student, dialogPhotoPreview);

        try {
            android.graphics.drawable.Drawable drawable = b.ivStudentPhoto.getDrawable();
            if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                currentPhotoBitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
            } else {
                currentPhotoBitmap = null;
            }
        } catch (Exception e) {
            currentPhotoBitmap = null;
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(view);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCamera.setOnClickListener(v -> {
            String[] options = {"à¤•à¥…à¤®à¥‡à¤°à¤¾ (Camera)", "à¤—à¥…à¤²à¤°à¥€ (Gallery)"};
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("à¤«à¥‹à¤Ÿà¥‹à¤šà¤¾ à¤¸à¥à¤¤à¥à¤°à¥‹à¤¤ à¤¨à¤¿à¤µà¤¡à¤¾ (Choose Photo Source)")
                    .setItems(options, (dialogInterface, which) -> {
                        if (which == 0) {
                            launchCamera();
                        } else {
                            launchGallery();
                        }
                    })
                    .show();
        });

        btnCrop.setOnClickListener(v -> {
            if (currentPhotoBitmap != null) {
                int width = currentPhotoBitmap.getWidth();
                int height = currentPhotoBitmap.getHeight();
                int newWidth = Math.min(width, height);
                int cropW = (width - newWidth) / 2;
                int cropH = (height - newWidth) / 2;
                currentPhotoBitmap = android.graphics.Bitmap.createBitmap(currentPhotoBitmap, cropW, cropH, newWidth, newWidth);
                dialogPhotoPreview.setImageBitmap(currentPhotoBitmap);
                Toast.makeText(this, "à¤«à¥‹à¤Ÿà¥‹ à¤•à¥à¤°à¥‰à¤ª à¤•à¥‡à¤²à¤¾", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "à¤•à¥à¤°à¥‰à¤ª à¤•à¤°à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤†à¤§à¥€ à¤«à¥‹à¤Ÿà¥‹ à¤¨à¤¿à¤µà¤¡à¤¾", Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(v -> {
            currentPhotoBitmap = null;
            dialogPhotoPreview.setImageResource(getPlaceholderRes(student));
            Toast.makeText(this, "à¤«à¥‹à¤Ÿà¥‹ à¤•à¤¾à¤¢à¤²à¤¾ (à¤¡à¤¿à¤«à¥‰à¤²à¥à¤Ÿ à¤…à¤µà¤¤à¤¾à¤° à¤¦à¤°à¥à¤¶à¤µà¤¿à¤²à¤¾ à¤œà¤¾à¤ˆà¤²)", Toast.LENGTH_SHORT).show();
        });

        btnRotate.setOnClickListener(v -> {
            if (currentPhotoBitmap != null) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(90);
                currentPhotoBitmap = android.graphics.Bitmap.createBitmap(currentPhotoBitmap, 0, 0, currentPhotoBitmap.getWidth(), currentPhotoBitmap.getHeight(), matrix, true);
                dialogPhotoPreview.setImageBitmap(currentPhotoBitmap);
            } else {
                Toast.makeText(this, "à¤«à¤¿à¤°à¤µà¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤†à¤§à¥€ à¤«à¥‹à¤Ÿà¥‹ à¤¨à¤¿à¤µà¤¡à¤¾", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> {
            if (currentPhotoBitmap == null) {
                Toast.makeText(this, "à¤«à¥‹à¤Ÿà¥‹ à¤•à¤¾à¤¢à¥‚à¤¨ à¤Ÿà¤¾à¤•à¤¤ à¤†à¤¹à¥‡...", Toast.LENGTH_SHORT).show();
                FirebaseRepository.get().deleteStudentPhoto(student.id, new FirebaseRepository.OnResult<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        student.photoUrl = "";
                        loadStudentPhoto(student, b.ivStudentPhoto);
                        Toast.makeText(StudentProfileActivity.this, "à¤«à¥‹à¤Ÿà¥‹ à¤¯à¤¶à¤¸à¥à¤µà¥€à¤°à¥€à¤¤à¥à¤¯à¤¾ à¤•à¤¾à¤¢à¤²à¤¾", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(StudentProfileActivity.this, "à¤¤à¥à¤°à¥à¤Ÿà¥€: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(currentPhotoBitmap, 800, 800, true);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, baos);
                byte[] bytes = baos.toByteArray();

                Toast.makeText(this, "à¤«à¥‹à¤Ÿà¥‹ à¤…à¤ªà¤²à¥‹à¤¡ à¤¹à¥‹à¤¤ à¤†à¤¹à¥‡...", Toast.LENGTH_SHORT).show();
                FirebaseRepository.get().uploadStudentPhoto(student.id, bytes, new FirebaseRepository.OnResult<String>() {
                    @Override
                    public void onSuccess(String url) {
                        student.photoUrl = url;
                        loadStudentPhoto(student, b.ivStudentPhoto);
                        Toast.makeText(StudentProfileActivity.this, "à¤«à¥‹à¤Ÿà¥‹ à¤¯à¤¶à¤¸à¥à¤µà¥€à¤°à¥€à¤¤à¥à¤¯à¤¾ à¤œà¤¤à¤¨ à¤•à¥‡à¤²à¤¾", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(StudentProfileActivity.this, "à¤¤à¥à¤°à¥à¤Ÿà¥€: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void launchCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, 100);
            return;
        }
        try {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            // Verify that a camera app can handle the intent
            if (intent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "à¤•à¥…à¤®à¥‡à¤°à¤¾ à¤à¤ª à¤†à¤¢à¤³à¤²à¥‡ à¤¨à¤¾à¤¹à¥€", Toast.LENGTH_SHORT).show();
                return;
            }
            java.io.File tempFile = new java.io.File(getExternalCacheDir(), "temp_profile.jpg");
            tempCameraUri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", tempFile);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, tempCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQ_CODE_CAMERA);
        } catch (Exception e) {
            Toast.makeText(this, "à¤•à¥…à¤®à¥‡à¤°à¤¾ à¤¸à¥à¤°à¥‚ à¤•à¤°à¤¤à¤¾ à¤†à¤²à¤¾ à¤¨à¤¾à¤¹à¥€: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_CODE_CAMERA) {
                try {
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(
                            getContentResolver().openInputStream(tempCameraUri));
                    if (bitmap != null) {
                        currentPhotoBitmap = bitmap;
                        if (dialogPhotoPreview != null) {
                            dialogPhotoPreview.setImageBitmap(currentPhotoBitmap);
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "à¤«à¥‹à¤Ÿà¥‹ à¤²à¥‹à¤¡ à¤•à¤°à¤¤à¤¾ à¤†à¤²à¤¾ à¤¨à¤¾à¤¹à¥€: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQ_CODE_GALLERY) {
                if (data != null && data.getData() != null) {
                    try {
                        android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                                getContentResolver(), data.getData());
                        if (bitmap != null) {
                            currentPhotoBitmap = bitmap;
                            if (dialogPhotoPreview != null) {
                                dialogPhotoPreview.setImageBitmap(currentPhotoBitmap);
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "à¤«à¥‹à¤Ÿà¥‹ à¤²à¥‹à¤¡ à¤•à¤°à¤¤à¤¾ à¤†à¤²à¤¾ à¤¨à¤¾à¤¹à¥€: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void showParentCodeDialog() {
        com.kartik.myschool.utils.LoadingDialog loading = new com.kartik.myschool.utils.LoadingDialog(this, null, "à¤•à¥‹à¤¡ à¤®à¤¿à¤³à¤µà¤¤ à¤†à¤¹à¥‡ / Fetching code...");
        loading.show();

        FirebaseRepository.get().getParentLinkForStudent(student.id, new FirebaseRepository.OnResult<com.kartik.myschool.model.ParentLink>() {
            @Override
            public void onSuccess(com.kartik.myschool.model.ParentLink link) {
                loading.dismiss();
                if (link != null && link.code != null) {
                    displayParentLinkCode(link);
                } else {
                    generateAndSaveParentLinkCode(loading);
                }
            }

            @Override
            public void onError(Exception e) {
                loading.dismiss();
                Toast.makeText(StudentProfileActivity.this, "à¤¤à¥à¤°à¥à¤Ÿà¥€: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateAndSaveParentLinkCode(com.kartik.myschool.utils.LoadingDialog loading) {
        if (loading != null) {
            loading.setMessage("à¤¨à¤µà¥€à¤¨ à¤•à¥‹à¤¡ à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤¤ à¤†à¤¹à¥‡ / Generating new code...");
            if (!loading.isShowing()) loading.show();
        }

        String schName = SessionContext.selectedSchool != null ? SessionContext.selectedSchool.name : "MySchool";
        String clsName = AppCache.selectedClass != null ? AppCache.selectedClass.className : (student.className != null ? student.className : "");
        String tId = FirebaseRepository.get().currentUid();

        FirebaseRepository.get().createParentLink(student.id, student.name, clsName, schName, tId, new FirebaseRepository.OnResult<com.kartik.myschool.model.ParentLink>() {
            @Override
            public void onSuccess(com.kartik.myschool.model.ParentLink link) {
                if (loading != null) loading.dismiss();
                displayParentLinkCode(link);
            }

            @Override
            public void onError(Exception e) {
                if (loading != null) loading.dismiss();
                Toast.makeText(StudentProfileActivity.this, "à¤•à¥‹à¤¡ à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤£à¥à¤¯à¤¾à¤¤ à¤…à¤ªà¤¯à¤¶: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayParentLinkCode(com.kartik.myschool.model.ParentLink link) {
        String msg = "à¤ªà¤¾à¤²à¤• à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¥à¤¸à¥à¤¤à¤• à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¹à¤¾ à¥¬ à¤…à¤‚à¤•à¥€ à¤•à¥‹à¤¡ à¤µà¤¾à¤ªà¤°à¥‚ à¤¶à¤•à¤¤à¤¾à¤¤:\n\n" +
                     "â˜… à¤•à¥‹à¤¡: " + link.code + " â˜…\n\n" +
                     "(à¤¹à¤¾ à¤•à¥‹à¤¡ 'MySchool Parent' à¥²à¤ªà¤®à¤§à¥à¤¯à¥‡ à¤ªà¥à¤°à¤µà¤¿à¤·à¥à¤Ÿ à¤•à¤°à¤¾)";

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("à¤ªà¤¾à¤²à¤• à¤œà¥‹à¤¡à¤£à¥€ à¤•à¥‹à¤¡ / Parent Code")
                .setMessage(msg)
                .setPositiveButton("WhatsApp à¤µà¤° à¤ªà¤¾à¤ à¤µà¤¾ / Share", (dialog, which) -> {
                    String shareText = "à¤¨à¤®à¤¸à¥à¤•à¤¾à¤°, à¤†à¤ªà¤²à¥à¤¯à¤¾ à¤ªà¤¾à¤²à¥à¤¯à¤¾à¤šà¥‡ à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¥à¤¸à¥à¤¤à¤• à¤†à¤£à¤¿ à¤‰à¤ªà¤¸à¥à¤¥à¤¿à¤¤à¥€ à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ 'MySchool Parent' à¥²à¤ª à¤¡à¤¾à¤‰à¤¨à¤²à¥‹à¤¡ à¤•à¤°à¤¾ à¤†à¤£à¤¿ à¤–à¤¾à¤²à¥€à¤² à¤•à¥‹à¤¡ à¤ªà¥à¤°à¤µà¤¿à¤·à¥à¤Ÿ à¤•à¤°à¤¾:\n\n" +
                                       "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€: " + student.name + "\n" +
                                       "à¤œà¥‹à¤¡à¤£à¥€ à¤•à¥‹à¤¡: " + link.code + "\n\n" +
                                       "à¤§à¤¨à¥à¤¯à¤µà¤¾à¤¦!";
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                })
                .setNeutralButton("à¤¨à¤µà¥€à¤¨ à¤•à¥‹à¤¡ à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤¾ / Reset", (dialog, which) -> {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("à¤ªà¥à¤·à¥à¤Ÿà¥€à¤•à¤°à¤£ / Reset Code")
                            .setMessage("à¤¨à¤µà¥€à¤¨ à¤•à¥‹à¤¡ à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤¾à¤¯à¤šà¤¾ à¤†à¤¹à¥‡ à¤•à¤¾? à¤œà¥à¤¨à¤¾ à¤•à¥‹à¤¡ à¤•à¤¾à¤® à¤•à¤°à¤£à¤¾à¤° à¤¨à¤¾à¤¹à¥€.")
                            .setPositiveButton("à¤¹à¥‹à¤¯ / Yes", (d, w) -> {
                                com.kartik.myschool.utils.LoadingDialog loading = new com.kartik.myschool.utils.LoadingDialog(this, null, "à¤¨à¤µà¥€à¤¨ à¤•à¥‹à¤¡ à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤¤ à¤†à¤¹à¥‡...");
                                generateAndSaveParentLinkCode(loading);
                            })
                            .setNegativeButton("à¤¨à¤¾à¤¹à¥€ / No", null)
                            .show();
                })
                .setNegativeButton("à¤¬à¤‚à¤¦ à¤•à¤°à¤¾ / Close", null);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted â€“ re-launch camera
                launchCamera();
            } else {
                Toast.makeText(this, "à¤•à¥…à¤®à¥‡à¤°à¤¾ à¤µà¤¾à¤ªà¤°à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤ªà¤°à¤µà¤¾à¤¨à¤—à¥€ à¤¦à¥à¤¯à¤¾", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
