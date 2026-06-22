package com.kartik.myschool;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.kartik.myschool.databinding.ActivityParentPortalBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.ParentLink;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.PdfGenerator;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentPortalActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "parent_portal_prefs";
    private static final String KEY_STUDENT_ID = "linked_student_id";
    private static final String KEY_STUDENT_NAME = "linked_student_name";
    private static final String KEY_CLASS_NAME = "linked_class_name";
    private static final String KEY_SCHOOL_NAME = "linked_school_name";
    private static final String KEY_TEACHER_ID = "linked_teacher_id";
    private static final String KEY_CLASS_ID = "linked_class_id";
    private static final String KEY_SCHOOL_ID = "linked_school_id";

    private ActivityParentPortalBinding b;
    private SharedPreferences prefs;

    private Student linkedStudent;
    private List<MarksRecord> linkedMarks = new ArrayList<>();
    private School linkedSchool;
    private ClassModel linkedClass;

    private int selectedSem = 0; // 0 = Sem 1, 1 = Sem 2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityParentPortalBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupClickListeners();
        checkLinkStatus();
    }

    private void setupClickListeners() {
        b.btnVerifyCode.setOnClickListener(v -> {
            String code = b.etParentCodeInput.getText() != null ? b.etParentCodeInput.getText().toString().trim() : "";
            if (code.length() != 6) {
                Toast.makeText(this, "कृपया ६ अंकी अचूक कोड प्रविष्ट करा / Please enter a valid 6-digit code", Toast.LENGTH_LONG).show();
                return;
            }
            verifyAndLinkParent(code);
        });

        b.btnUnlinkStudent.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("विद्यार्थी बदला / Change Student")
                    .setMessage("तुम्हाला पाल्याची जोडणी काढायची आहे का? / Are you sure you want to unlink this student?")
                    .setPositiveButton("होय / Yes", (dialog, which) -> unlinkStudent())
                    .setNegativeButton("नाही / No", null)
                    .show();
        });

        b.btnRefreshParent.setOnClickListener(v -> refreshData());

        b.btnParentGeneratePdf.setOnClickListener(v -> generateReportCardPdf());

        b.tabLayoutSem.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedSem = tab.getPosition();
                renderMarks();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void checkLinkStatus() {
        String studentId = prefs.getString(KEY_STUDENT_ID, null);
        if (studentId != null) {
            showDashboard();
            loadLinkedData(studentId);
        } else {
            showCodeEntry();
        }
    }

    private void showCodeEntry() {
        b.panelCodeEntry.setVisibility(View.VISIBLE);
        b.panelDashboard.setVisibility(View.GONE);
    }

    private void showDashboard() {
        b.panelCodeEntry.setVisibility(View.GONE);
        b.panelDashboard.setVisibility(View.VISIBLE);
    }

    private void verifyAndLinkParent(String code) {
        com.kartik.myschool.utils.LoadingDialog loading = new com.kartik.myschool.utils.LoadingDialog(this, null, "कोड तपासत आहे / Verifying code...");
        loading.show();

        FirebaseRepository.get().claimParentLink(code, new FirebaseRepository.OnResult<ParentLink>() {
            @Override
            public void onSuccess(ParentLink link) {
                loading.dismiss();
                if (link != null) {
                    Toast.makeText(ParentPortalActivity.this, "जोडणी यशस्वी! / Link Successful!", Toast.LENGTH_SHORT).show();
                    
                    // Save linking data
                    prefs.edit()
                            .putString(KEY_STUDENT_ID, link.studentId)
                            .putString(KEY_STUDENT_NAME, link.studentName)
                            .putString(KEY_CLASS_NAME, link.className)
                            .putString(KEY_SCHOOL_NAME, link.schoolName)
                            .putString(KEY_TEACHER_ID, link.teacherId)
                            .apply();

                    showDashboard();
                    loadLinkedData(link.studentId);
                }
            }

            @Override
            public void onError(Exception e) {
                loading.dismiss();
                Toast.makeText(ParentPortalActivity.this, "जोडणी अयशस्वी: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void unlinkStudent() {
        prefs.edit().clear().apply();
        linkedStudent = null;
        linkedMarks.clear();
        linkedSchool = null;
        linkedClass = null;
        b.etParentCodeInput.setText("");
        showCodeEntry();
    }

    private void loadLinkedData(String studentId) {
        // Pre-fill cache details if offline or loading
        b.tvParentStudentName.setText(prefs.getString(KEY_STUDENT_NAME, "विद्यार्थी"));
        b.tvParentStudentClass.setText("वर्ग: " + prefs.getString(KEY_CLASS_NAME, "-"));
        b.tvParentSchoolName.setText(prefs.getString(KEY_SCHOOL_NAME, "शाळा"));

        FirebaseRepository.get().getStudentForParent(studentId, new FirebaseRepository.OnResult<Student>() {
            @Override
            public void onSuccess(Student s) {
                if (s == null) return;
                linkedStudent = s;
                b.tvParentStudentName.setText(s.name);
                b.tvParentStudentClass.setText("Standard: " + (s.standard != null ? s.standard : "-") + " | Div: " + (s.division != null ? s.division : "-"));
                
                // Save classId & schoolId dynamically to load school details
                prefs.edit()
                        .putString(KEY_CLASS_ID, s.classId)
                        .putString(KEY_SCHOOL_ID, s.schoolId)
                        .apply();

                renderAttendance(s);

                // Fetch school details
                if (s.schoolId != null) {
                    FirebaseRepository.get().getSchoolForParent(s.schoolId, new FirebaseRepository.OnResult<School>() {
                        @Override public void onSuccess(School school) { linkedSchool = school; if (school != null) b.tvParentSchoolName.setText(school.name); }
                        @Override public void onError(Exception e) {}
                    });
                }

                // Fetch classModel details
                if (s.classId != null) {
                    FirebaseRepository.get().getClassForParent(s.classId, new FirebaseRepository.OnResult<ClassModel>() {
                        @Override public void onSuccess(ClassModel cls) { linkedClass = cls; renderMarks(); }
                        @Override public void onError(Exception e) {}
                    });
                }

                // Fetch academic marks
                loadMarksData(s.id, s.classId);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ParentPortalActivity.this, "माहिती लोड करण्यात अडचण आली: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMarksData(String studentId, String classId) {
        if (studentId == null || classId == null) return;

        FirebaseRepository.get().getMarksForParent(studentId, classId, new FirebaseRepository.OnResult<List<MarksRecord>>() {
            @Override
            public void onSuccess(List<MarksRecord> list) {
                linkedMarks = list != null ? list : new ArrayList<>();
                renderMarks();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ParentPortalActivity.this, "गुण मिळवण्यात अडचण: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshData() {
        String studentId = prefs.getString(KEY_STUDENT_ID, null);
        if (studentId != null) {
            Toast.makeText(this, "माहिती अद्ययावत करत आहे... / Refreshing...", Toast.LENGTH_SHORT).show();
            loadLinkedData(studentId);
        }
    }

    private void renderAttendance(Student s) {
        if (s == null || s.monthlyAttendance == null) return;

        b.layoutMonthlyDetails.removeAllViews();

        int totalPresent = 0;
        int totalWorking = 0;

        String[] months = {"जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे", "डिसें", "जाने", "फेब्रु", "मार्च", "एप्रिल", "मे"};
        LayoutInflater inflater = LayoutInflater.from(this);

        LinearLayout rowLayout = null;

        for (int i = 0; i < months.length; i++) {
            String m = months[i];
            String val = s.monthlyAttendance.get(m);
            int p = 0;
            int t = 0;

            if (val != null && val.contains("/")) {
                String[] parts = val.split("/");
                if (parts.length == 2) {
                    try {
                        p = Integer.parseInt(parts[0].trim());
                        t = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
            }

            totalPresent += p;
            totalWorking += t;

            if (t > 0) {
                // Add mini view for active months in a 3-column format
                if (i % 3 == 0 || rowLayout == null) {
                    rowLayout = new LinearLayout(this);
                    rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                    rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    b.layoutMonthlyDetails.addView(rowLayout);
                }

                View cell = inflater.inflate(R.layout.item_detail_row, rowLayout, false);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) cell.getLayoutParams();
                lp.width = 0;
                lp.weight = 1;
                cell.setLayoutParams(lp);

                TextView tvLabel = cell.findViewById(R.id.tvDetailLabel);
                TextView tvVal = cell.findViewById(R.id.tvDetailValue);

                if (tvLabel != null) tvLabel.setText(m);
                if (tvVal != null) tvVal.setText(p + "/" + t);

                rowLayout.addView(cell);
            }
        }

        b.tvAttendanceFraction.setText("एकूण उपस्थिती: " + totalPresent + " / " + totalWorking + " दिवस");

        if (totalWorking > 0) {
            int pct = (totalPresent * 100) / totalWorking;
            b.tvAttendancePct.setText(pct + "%");
            b.pbAttendance.setProgress(pct);
        } else {
            b.tvAttendancePct.setText("0%");
            b.pbAttendance.setProgress(0);
        }
    }

    private void renderMarks() {
        b.layoutMarksTable.removeAllViews();
        b.tvNoMarksParentAlert.setVisibility(View.GONE);

        if (linkedClass == null || linkedClass.subjects == null) return;

        // Filter marks by selected semester
        MarksRecord targetRecord = null;
        String semString = (selectedSem == 0) ? "sem_1" : "sem_2";
        int semNum = selectedSem + 1;

        for (MarksRecord mr : linkedMarks) {
            if (mr.semesterId != null && mr.semesterId.contains(String.valueOf(semNum))) {
                targetRecord = mr;
                break;
            }
            if (mr.semesterNumber != null && mr.semesterNumber.equals(String.valueOf(semNum))) {
                targetRecord = mr;
                break;
            }
        }

        if (targetRecord == null && !linkedMarks.isEmpty()) {
            // Fallback to match in-memory semester logic if fields are empty
            for (MarksRecord mr : linkedMarks) {
                if (selectedSem == 0 && (mr.semesterId == null || mr.semesterId.isEmpty())) {
                    targetRecord = mr; // Legacy maps to Sem 1
                    break;
                }
            }
        }

        if (targetRecord == null) {
            b.tvNoMarksParentAlert.setVisibility(View.VISIBLE);
            b.cardParentRemarks.setVisibility(View.GONE);
            return;
        }

        b.tvNoMarksParentAlert.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (com.kartik.myschool.model.Subject sub : linkedClass.subjects) {
            String safeKey = MarksRecord.sanitizeKey(sub.name);
            MarksRecord.SubjectMarksDetail detail = null;

            if (targetRecord.detailedMarks != null && targetRecord.detailedMarks.containsKey(safeKey)) {
                detail = targetRecord.detailedMarks.get(safeKey);
            }

            View row = inflater.inflate(R.layout.item_detail_row, b.layoutMarksTable, false);

            TextView tvSubName = row.findViewById(R.id.tvDetailLabel);
            TextView tvSubScore = row.findViewById(R.id.tvDetailValue);

            if (tvSubName != null) tvSubName.setText(sub.name);

            if (detail != null) {
                int got = detail.grandTotal;
                int max = detail.maxMarks > 0 ? detail.maxMarks : sub.maxMarks;
                String grade = detail.grade != null ? detail.grade : "—";
                if (tvSubScore != null) tvSubScore.setText(got + " / " + max + " [" + grade + "]");
            } else {
                if (tvSubScore != null) tvSubScore.setText("— / " + sub.maxMarks);
            }

            b.layoutMarksTable.addView(row);
        }

        // Render general remarks if available
        b.cardParentRemarks.setVisibility(View.GONE);
        if (targetRecord.detailedMarks != null) {
            StringBuilder remarksBuilder = new StringBuilder();
            for (Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : targetRecord.detailedMarks.entrySet()) {
                String subName = entry.getKey();
                MarksRecord.SubjectMarksDetail det = entry.getValue();
                if (det != null && det.remark != null && !det.remark.trim().isEmpty()) {
                    remarksBuilder.append("• ").append(subName).append(": ").append(det.remark.trim()).append("\n");
                }
            }
            if (remarksBuilder.length() > 0) {
                b.tvParentGeneralRemarks.setText(remarksBuilder.toString().trim());
                b.cardParentRemarks.setVisibility(View.VISIBLE);
            }
        }
    }

    private void generateReportCardPdf() {
        if (linkedStudent == null || linkedSchool == null || linkedClass == null || linkedMarks.isEmpty()) {
            Toast.makeText(this, "माहिती अपूर्ण आहे, कृपया रिफ्रेश करा. / Information incomplete, please refresh.", Toast.LENGTH_LONG).show();
            return;
        }

        // Get Semester 1 marks
        MarksRecord m = null;
        for (MarksRecord mr : linkedMarks) {
            if (mr.semesterNumber == null || mr.semesterNumber.equals("1") || mr.semesterId == null || mr.semesterId.contains("1")) {
                m = mr;
                break;
            }
        }
        if (m == null && !linkedMarks.isEmpty()) {
            m = linkedMarks.get(0);
        }

        if (m == null) {
            Toast.makeText(this, "गुण उपलब्ध नाहीत / Marks not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "प्रगतीपुस्तक तयार होत आहे... / Generating Report Card...", Toast.LENGTH_SHORT).show();

        PdfGenerator.generate(this, linkedSchool, linkedClass, linkedStudent, m, new PdfGenerator.PdfCallback() {
            @Override
            public void onSuccess(File pdfFile) {
                runOnUiThread(() -> showPdfInAppDialog(pdfFile));
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(ParentPortalActivity.this, "PDF Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showPdfInAppDialog(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) return;

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "application/pdf");
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(view, "प्रगतीपुस्तक उघडा / Open Report Card"));
        } catch (Exception e) {
            Toast.makeText(this, "PDF व्ह्यूअर सापडला नाही. कृपया PDF वाचक ॲप डाउनलोड करा. / No PDF viewer found.", Toast.LENGTH_LONG).show();
        }
    }
}
