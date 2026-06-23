package com.kartik.myschool.ui;

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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.adapter.AttendanceAdapter;
import com.kartik.myschool.databinding.FragmentAttendanceBinding;
import com.kartik.myschool.model.AttendanceRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.AttendanceEntryDialog;
import com.kartik.myschool.repository.FirebaseRepository;

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
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());

        setupRecyclerView();
        displayHeaderInfo();
        setupActionIcons();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());
    }

    private void setupActionIcons() {
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View btnAdd = ha.findViewById(R.id.btnToolbarAdd);
            if (btnAdd != null) {
                btnAdd.setOnClickListener(v -> ha.navigateTo(R.id.nav_students));
            }
            View btnCalc = ha.findViewById(R.id.btnToolbarCalc);
            if (btnCalc != null) {
                btnCalc.setOnClickListener(v -> showClassReportDialog());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
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
                    ? SessionContext.selectedClass.division : "-";
        }
        b.tvAttendanceContext.setText("सन : " + yearLabel + "  इयत्ता: " + classVal + ", तुकडी: " + divVal);
    }


    private void loadData() {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(getContext(), R.string.msg_please_select_class_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String classId = SessionContext.selectedClass.id;
        
        if (b != null) {
            b.rvAttendanceStudents.setVisibility(View.GONE);
            b.shimmerViewContainer.setVisibility(View.VISIBLE);
            b.shimmerViewContainer.startShimmer();
        }
        
        // Fetch Students
        FirebaseRepository.get().getStudentsForClass(classId, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (b != null) {
                            b.shimmerViewContainer.stopShimmer();
                            b.shimmerViewContainer.setVisibility(View.GONE);
                            b.rvAttendanceStudents.setVisibility(View.VISIBLE);
                        }
                        if (list != null) {
                            studentsList = list;
                            adapter.updateData(studentsList);
                        }
                        if (b != null) {
                            b.emptyState.setVisibility(studentsList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (b != null) {
                            b.shimmerViewContainer.stopShimmer();
                            b.shimmerViewContainer.setVisibility(View.GONE);
                            b.rvAttendanceStudents.setVisibility(View.VISIBLE);
                            b.emptyState.setVisibility(studentsList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                        Toast.makeText(getContext(), R.string.msg_failed_to_load_students, Toast.LENGTH_SHORT).show();
                    });
                }
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
        com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(requireContext(), "attendance");
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
        AttendanceEntryDialog dialog = AttendanceEntryDialog.newInstance(student, record);
        dialog.setOnAttendanceSavedListener((s, r) -> loadData());
        dialog.show(getChildFragmentManager(), "AttendanceEntryDialog");
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


}
