package com.example.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myschool.HomeActivity;
import com.example.myschool.R;
import com.example.myschool.SessionContext;
import com.example.myschool.adapter.AttendanceAdapter;
import com.example.myschool.databinding.DialogEditAttendanceBinding;
import com.example.myschool.databinding.FragmentAttendanceBinding;
import com.example.myschool.model.AttendanceRecord;
import com.example.myschool.model.Student;
import com.example.myschool.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceFragment extends Fragment implements AttendanceAdapter.OnOptionClickListener {

    private FragmentAttendanceBinding b;
    private AttendanceAdapter adapter;
    private List<Student> studentsList = new ArrayList<>();
    private final Map<String, AttendanceRecord> attendanceMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentAttendanceBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        displayHeaderInfo();
        setupToolbarActions();
        loadData();
    }

    private void setupRecyclerView() {
        b.rvAttendanceStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AttendanceAdapter(this);
        b.rvAttendanceStudents.setAdapter(adapter);
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        String classVal = "5";
        String divVal = "1";
        if (SessionContext.selectedClass != null) {
            classVal = SessionContext.selectedClass.className != null ? SessionContext.selectedClass.className : "5";
            divVal = SessionContext.selectedClass.division != null && !SessionContext.selectedClass.division.isEmpty() 
                    ? SessionContext.selectedClass.division : "1";
        }
        b.tvAttendanceContext.setText("सन : " + yearLabel + "  इयत्ता: " + classVal + ", तुकडी: " + divVal);
    }

    private void setupToolbarActions() {
        // Menu Hamburger click opens drawer
        b.ivMenu.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).openDrawer();
            }
        });

        // Chat/Help Icon click
        b.ivActionHelp.setOnClickListener(v -> showHelpDialog());

        // Add Icon click (initialize record for student)
        b.ivActionAdd.setOnClickListener(v -> showAddStudentAttendanceDialog());

        // Calculator/Report Icon click
        b.ivActionReport.setOnClickListener(v -> showClassReportDialog());

        // More Vertical click
        b.ivActionMore.setOnClickListener(v -> showSettingsPopup());
    }

    private void loadData() {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(getContext(), "Please select class first", Toast.LENGTH_SHORT).show();
            return;
        }
        String classId = SessionContext.selectedClass.id;
        
        // Fetch Students
        FirebaseRepository.get().getStudentsForClass(classId, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                if (list != null) {
                    studentsList = list;
                    
                    // Fetch Attendance Records
                    FirebaseRepository.get().getAttendanceRecordsForClass(classId, new FirebaseRepository.OnResult<List<AttendanceRecord>>() {
                        @Override
                        public void onSuccess(List<AttendanceRecord> records) {
                            attendanceMap.clear();
                            if (records != null) {
                                for (AttendanceRecord r : records) {
                                    if (r.studentId != null) attendanceMap.put(r.studentId, r);
                                }
                            }
                            adapter.updateData(studentsList, attendanceMap);
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getContext(), "Failed to load attendance records", Toast.LENGTH_SHORT).show();
                            adapter.updateData(studentsList, attendanceMap);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to load students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------- Option menu callbacks from Adapter ----------

    @Override
    public void onEdit(Student student, AttendanceRecord record) {
        showEditDialog(student, record);
    }

    @Override
    public void onDuplicate(Student student, AttendanceRecord record) {
        showDuplicateDialog(student, record);
    }

    @Override
    public void onDelete(Student student, AttendanceRecord record) {
        showDeleteConfirmation(student, record);
    }

    // ---------- Dialog Flows ----------

    private void showHelpDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("उपस्थिती मदत (Help Guidelines)")
                .setMessage("१. मासिक उपस्थिती बदल करण्यासाठी प्रत्येक कार्डच्या उजव्या बाजूला असलेल्या तीन-बिंदू मेनूवर क्लिक करा आणि 'बदल करा' निवडा.\n\n"
                        + "२. एका विद्यार्थ्याची मासिक हजेरी दुसऱ्या विद्यार्थ्यावर जशीच्या तशी कॉपी करण्यासाठी 'डुप्लिकेट' निवडा.\n\n"
                        + "३. रेकॉर्ड डिलीट करण्यासाठी 'डिलीट करा' वर क्लिक करा.\n\n"
                        + "४. हजेरी जोडण्यासाठी वर दिलेल्या '+' आयकॉनवर क्लिक करा.")
                .setPositiveButton("ठीक आहे", null)
                .show();
    }

    private void showAddStudentAttendanceDialog() {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), "No students found to add attendance", Toast.LENGTH_SHORT).show();
            return;
        }

        // Filter students who do NOT have attendance records yet
        List<Student> eligibleStudents = new ArrayList<>();
        for (Student s : studentsList) {
            if (!attendanceMap.containsKey(s.id)) {
                eligibleStudents.add(s);
            }
        }

        if (eligibleStudents.isEmpty()) {
            Toast.makeText(getContext(), "सर्व विद्यार्थ्यांची उपस्थिती आधीच तयार केली आहे!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] eligibleNames = new String[eligibleStudents.size()];
        for (int i = 0; i < eligibleStudents.size(); i++) {
            eligibleNames[i] = eligibleStudents.get(i).rollNo + ". " + eligibleStudents.get(i).name;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("उपस्थिती तयार करा (Initialize Attendance)")
                .setItems(eligibleNames, (dialog, which) -> {
                    Student selected = eligibleStudents.get(which);
                    AttendanceRecord newRecord = new AttendanceRecord();
                    newRecord.studentId = selected.id;
                    newRecord.classId = SessionContext.selectedClass.id;
                    newRecord.academicYear = SessionContext.getYearLabel();
                    
                    FirebaseRepository.get().saveAttendanceRecord(newRecord, new FirebaseRepository.OnResult<String>() {
                        @Override
                        public void onSuccess(String id) {
                            Toast.makeText(getContext(), selected.name + " ची उपस्थिती तयार झाली!", Toast.LENGTH_SHORT).show();
                            loadData();
                        }
                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void showClassReportDialog() {
        int studentsCount = studentsList.size();
        int totalWorking = 0;
        int totalPresent = 0;
        double overallPercentage = 0.0;
        String bestStudent = "—";
        int bestCount = -1;

        for (Student s : studentsList) {
            AttendanceRecord r = attendanceMap.get(s.id);
            if (r != null) {
                r.recalculateTotals();
                totalPresent += r.totalPresent;
                totalWorking += r.totalWorking;
                if (r.totalPresent > bestCount) {
                    bestCount = r.totalPresent;
                    bestStudent = s.name;
                }
            }
        }

        if (totalWorking > 0) {
            overallPercentage = ((double) totalPresent / totalWorking) * 100.0;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("वर्गवार उपस्थिती अहवाल (Attendance Analytics)")
                .setMessage("• एकूण विद्यार्थी (Total Students): " + studentsCount + " विद्यार्थी\n\n"
                        + "• एकूण हजेरी बेरीज (Total Present): " + totalPresent + " दिवस\n\n"
                        + "• एकूण कामकाजाचे दिवस (Total Working): " + totalWorking + " दिवस\n\n"
                        + "• सरासरी उपस्थिती टक्केवारी (Average Attendance): " + String.format(java.util.Locale.getDefault(), "%.2f%%", overallPercentage) + "\n\n"
                        + "• सर्वाधिक उपस्थित विद्यार्थी (Best Attender): " + bestStudent + " (" + bestCount + " दिवस)")
                .setPositiveButton("बंद करा", null)
                .show();
    }

    private void showSettingsPopup() {
        Toast.makeText(getContext(), "सेटिंग्ज लवकरच येत आहेत", Toast.LENGTH_SHORT).show();
    }

    private void showEditDialog(Student student, AttendanceRecord record) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_attendance, null, false);
        DialogEditAttendanceBinding binding = DialogEditAttendanceBinding.bind(view);

        // Prepopulate inputs
        prepopPair(binding.etJunPresent, binding.etJunTotal, record.monthlyData.get("जून"));
        prepopPair(binding.etJulPresent, binding.etJulTotal, record.monthlyData.get("जुलै"));
        prepopPair(binding.etAugPresent, binding.etAugTotal, record.monthlyData.get("ऑगस्ट"));
        prepopPair(binding.etSepPresent, binding.etSepTotal, record.monthlyData.get("सप्टें"));
        prepopPair(binding.etOctPresent, binding.etOctTotal, record.monthlyData.get("ऑक्टो"));
        prepopPair(binding.etNovPresent, binding.etNovTotal, record.monthlyData.get("नोव्हे"));
        prepopPair(binding.etDecPresent, binding.etDecTotal, record.monthlyData.get("डिसें"));
        prepopPair(binding.etJanPresent, binding.etJanTotal, record.monthlyData.get("जाने"));
        prepopPair(binding.etFebPresent, binding.etFebTotal, record.monthlyData.get("फेब्रु"));
        prepopPair(binding.etMarPresent, binding.etMarTotal, record.monthlyData.get("मार्च"));
        prepopPair(binding.etAprPresent, binding.etAprTotal, record.monthlyData.get("एप्रिल"));
        prepopPair(binding.etMayPresent, binding.etMayTotal, record.monthlyData.get("मे"));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        binding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        binding.btnSave.setOnClickListener(v -> {
            savePair(binding.etJunPresent, binding.etJunTotal, "जून", record);
            savePair(binding.etJulPresent, binding.etJulTotal, "जुलै", record);
            savePair(binding.etAugPresent, binding.etAugTotal, "ऑगस्ट", record);
            savePair(binding.etSepPresent, binding.etSepTotal, "सप्टें", record);
            savePair(binding.etOctPresent, binding.etOctTotal, "ऑक्टो", record);
            savePair(binding.etNovPresent, binding.etNovTotal, "नोव्हे", record);
            savePair(binding.etDecPresent, binding.etDecTotal, "डिसें", record);
            savePair(binding.etJanPresent, binding.etJanTotal, "जाने", record);
            savePair(binding.etFebPresent, binding.etFebTotal, "फेब्रु", record);
            savePair(binding.etMarPresent, binding.etMarTotal, "मार्च", record);
            savePair(binding.etAprPresent, binding.etAprTotal, "एप्रिल", record);
            savePair(binding.etMayPresent, binding.etMayTotal, "मे", record);

            record.recalculateTotals();

            FirebaseRepository.get().saveAttendanceRecord(record, new FirebaseRepository.OnResult<String>() {
                @Override
                public void onSuccess(String id) {
                    Toast.makeText(getContext(), student.name + " ची उपस्थिती जतन झाली!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadData();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void prepopPair(EditText etPresent, EditText etTotal, String val) {
        if (val != null && val.contains("/")) {
            String[] parts = val.split("/");
            if (parts.length == 2) {
                etPresent.setText(parts[0].trim());
                etTotal.setText(parts[1].trim());
            }
        }
    }

    private void savePair(EditText etPresent, EditText etTotal, String month, AttendanceRecord r) {
        String pStr = etPresent.getText() != null ? etPresent.getText().toString().trim() : "0";
        String tStr = etTotal.getText() != null ? etTotal.getText().toString().trim() : "0";
        if (pStr.isEmpty()) pStr = "0";
        if (tStr.isEmpty()) tStr = "0";
        r.monthlyData.put(month, pStr + "/" + tStr);
    }

    private void showDuplicateDialog(Student srcStudent, AttendanceRecord srcRecord) {
        if (studentsList == null || studentsList.isEmpty()) return;

        // Choose a target student to copy attendance to
        List<Student> targets = new ArrayList<>();
        for (Student s : studentsList) {
            if (!s.id.equals(srcStudent.id)) targets.add(s);
        }

        if (targets.isEmpty()) {
            Toast.makeText(getContext(), "हजेरी डुप्लिकेट करण्यासाठी वर्गात इतर विद्यार्थी नाहीत", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] targetNames = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            targetNames[i] = targets.get(i).rollNo + ". " + targets.get(i).name;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("उपस्थिती डुप्लिकेट करा (Copy To)")
                .setItems(targetNames, (dialog, which) -> {
                    Student targetStudent = targets.get(which);
                    
                    AttendanceRecord targetRecord = attendanceMap.get(targetStudent.id);
                    if (targetRecord == null) {
                        targetRecord = new AttendanceRecord();
                        targetRecord.studentId = targetStudent.id;
                        targetRecord.classId = SessionContext.selectedClass.id;
                        targetRecord.academicYear = SessionContext.getYearLabel();
                    }

                    // Copy monthly data map
                    targetRecord.monthlyData.clear();
                    targetRecord.monthlyData.putAll(srcRecord.monthlyData);
                    targetRecord.recalculateTotals();

                    FirebaseRepository.get().saveAttendanceRecord(targetRecord, new FirebaseRepository.OnResult<String>() {
                        @Override
                        public void onSuccess(String id) {
                            Toast.makeText(getContext(), srcStudent.name + " ची उपस्थिती " + targetStudent.name + " वर कॉपी झाली!", Toast.LENGTH_SHORT).show();
                            loadData();
                        }
                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void showDeleteConfirmation(Student student, AttendanceRecord record) {
        if (record.id == null) {
            Toast.makeText(getContext(), "या विद्यार्थ्याची उपस्थिती आधीच रिकामी आहे!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("डिलीट करा?")
                .setMessage("तुम्हाला खात्री आहे की तुम्ही " + student.name + " ची उपस्थिती नष्ट (डिलीट) करू इच्छिता?")
                .setNegativeButton("रद्द करा", null)
                .setPositiveButton("डिलीट", (dialog, which) -> {
                    FirebaseRepository.get().deleteAttendanceRecord(record.id, new FirebaseRepository.OnResult<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(getContext(), "उपस्थिती डिलीट झाली!", Toast.LENGTH_SHORT).show();
                            loadData();
                        }
                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    // ---------- Enforcing screen fullscreen custom Top Bar visibility ----------

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            ha.findViewById(R.id.appBarLayout).setVisibility(View.GONE);
            ha.findViewById(R.id.bottomNav).setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            ha.findViewById(R.id.appBarLayout).setVisibility(View.VISIBLE);
            ha.findViewById(R.id.bottomNav).setVisibility(View.VISIBLE);
        }
    }
}
