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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceFragment extends Fragment implements AttendanceAdapter.OnOptionClickListener {

    private FragmentAttendanceBinding b;
    private AttendanceAdapter adapter;
    private List<Student> studentsList = new ArrayList<>();

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

        // Add Icon click (Friendly instructions dialog)
        b.ivActionAdd.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.msg_new_attendance)
                    .setMessage("नवीन हजेरी जोडण्यासाठी संबंधित विद्यार्थ्याच्या कार्डवरील उजव्या ३-बिंदू चिन्हावर क्लिक करा आणि 'बदल करा' (Edit) पर्याय निवडा.")
                    .setPositiveButton("ठीक आहे", null)
                    .show();
        });

        // Calculator/Report Icon click
        b.ivActionReport.setOnClickListener(v -> showClassReportDialog());

        // More Vertical click
        b.ivActionMore.setOnClickListener(v -> {
            Toast.makeText(getContext(), R.string.msg_empty_4, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadData() {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(getContext(), R.string.msg_please_select_class_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String classId = SessionContext.selectedClass.id;
        
        // Fetch Students
        FirebaseRepository.get().getStudentsForClass(classId, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                if (list != null) {
                    studentsList = list;
                    adapter.updateData(studentsList);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), R.string.msg_failed_to_load_students, Toast.LENGTH_SHORT).show();
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
                .setTitle(R.string.msg_help_guidelines)
                .setMessage("१. मासिक उपस्थिती बदल करण्यासाठी प्रत्येक कार्डच्या उजव्या बाजूला असलेल्या तीन-बिंदू मेनूवर क्लिक करा आणि 'बदल करा' निवडा.\n\n"
                        + "२. एका विद्यार्थ्याची मासिक हजेरी दुसऱ्या विद्यार्थ्यावर जशीच्या तशी कॉपी करण्यासाठी 'डुप्लिकेट' निवडा.\n\n"
                        + "३. रेकॉर्ड डिलीट करण्यासाठी 'डिलीट करा' वर क्लिक करा.")
                .setPositiveButton("ठीक आहे", null)
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
            AttendanceRecord r = new AttendanceRecord();
            if (s.monthlyAttendance != null) {
                r.monthlyData.putAll(s.monthlyAttendance);
            }
            r.recalculateTotals();
            totalPresent += r.totalPresent;
            totalWorking += r.totalWorking;
            if (r.totalPresent > bestCount) {
                bestCount = r.totalPresent;
                bestStudent = s.name;
            }
        }

        if (totalWorking > 0) {
            overallPercentage = ((double) totalPresent / totalWorking) * 100.0;
        }

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.msg_attendance_analytics)
                .setMessage("• एकूण विद्यार्थी (Total Students): " + studentsCount + " विद्यार्थी\n\n"
                        + "• एकूण हजेरी बेरीज (Total Present): " + totalPresent + " दिवस\n\n"
                        + "• एकूण कामकाजाचे दिवस (Total Working): " + totalWorking + " दिवस\n\n"
                        + "• सरासरी उपस्थिती टक्केवारी (Average Attendance): " + String.format(java.util.Locale.getDefault(), "%.2f%%", overallPercentage) + "\n\n"
                        + "• सर्वाधिक उपस्थित विद्यार्थी (Best Attender): " + bestStudent + " (" + bestCount + " दिवस)")
                .setPositiveButton("बंद करा", null)
                .show();
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

            // Copy monthly data back to student object
            if (student.monthlyAttendance == null) student.monthlyAttendance = new HashMap<>();
            student.monthlyAttendance.clear();
            student.monthlyAttendance.putAll(record.monthlyData);

            FirebaseRepository.get().saveStudent(student, new FirebaseRepository.OnResult<String>() {
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
            if (!java.util.Objects.equals(s.id, srcStudent.id)) targets.add(s);
        }

        if (targets.isEmpty()) {
            Toast.makeText(getContext(), R.string.msg_empty_5, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] targetNames = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            targetNames[i] = targets.get(i).rollNo + ". " + targets.get(i).name;
        }

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.msg_copy_to)
                .setItems(targetNames, (dialog, which) -> {
                    Student targetStudent = targets.get(which);
                    
                    if (targetStudent.monthlyAttendance == null) {
                        targetStudent.monthlyAttendance = new HashMap<>();
                    }
                    targetStudent.monthlyAttendance.clear();
                    targetStudent.monthlyAttendance.putAll(srcRecord.monthlyData);

                    FirebaseRepository.get().saveStudent(targetStudent, new FirebaseRepository.OnResult<String>() {
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
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.msg_empty_7)
                .setMessage("तुम्हाला खात्री आहे की तुम्ही " + student.name + " ची उपस्थिती नष्ट (डिलीट) करू इच्छिता?")
                .setNegativeButton("रद्द करा", null)
                .setPositiveButton("डिलीट", (dialog, which) -> {
                    if (student.monthlyAttendance != null) {
                        student.monthlyAttendance.clear();
                    }
                    FirebaseRepository.get().saveStudent(student, new FirebaseRepository.OnResult<String>() {
                        @Override
                        public void onSuccess(String id) {
                            Toast.makeText(getContext(), R.string.msg_empty_6, Toast.LENGTH_SHORT).show();
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
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.GONE);
            }
            View bottomNav = ha.findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            
            // Fix CoordinatorLayout scrolling behavior offset bug:
            View navHost = ha.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                        (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost.getLayoutParams();
                params.setBehavior(null);
                params.bottomMargin = 0;
                navHost.setLayoutParams(params);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Restore bottomNav in case onPause() was skipped (e.g., back stack pop)
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View bottomNav = ha.findViewById(R.id.bottomNav);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) appBar.setVisibility(View.VISIBLE);
        }
        b = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) appBar.setVisibility(View.VISIBLE);
            View bottomNav = ha.findViewById(R.id.bottomNav);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);

            // Restore CoordinatorLayout scrolling behavior and margins:
            View navHost = ha.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params =
                        (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost.getLayoutParams();
                params.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
                float density = getResources().getDisplayMetrics().density;
                params.bottomMargin = (int) (64 * density);
                navHost.setLayoutParams(params);
            }
        }
    }
}
