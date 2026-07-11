package com.kartik.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.adapter.AttendanceReportAdapter;
import com.kartik.myschool.databinding.FragmentAttendanceReportBinding;
import com.kartik.myschool.model.AttendanceRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AttendanceReportFragment extends Fragment {

    private FragmentAttendanceReportBinding b;
    private AttendanceReportAdapter adapter;
    private List<Student> studentsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentAttendanceReportBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SessionContext.ensureCacheLoaded(requireContext());

        setupToolbar();
        setupRecyclerView();
        displayHeaderInfo();
        loadData();
    }

    private void setupToolbar() {
        b.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        b.toolbar.inflateMenu(R.menu.menu_help_only);
        b.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_help) {
                com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(requireContext(), "attendance_report");
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        b.rvReportStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AttendanceReportAdapter();
        b.rvReportStudents.setAdapter(adapter);
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        b.tvReportContext.setText("Year: " + yearLabel + " | " + SessionContext.getClassDivSubtitle());
    }

    private void loadData() {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(getContext(), R.string.msg_please_select_class_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String classId = SessionContext.selectedClass.id;
        
        b.progressBar.setVisibility(View.VISIBLE);
        b.rvReportStudents.setVisibility(View.GONE);
        
        FirebaseRepository.get().getStudentsForClass(classId, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        b.progressBar.setVisibility(View.GONE);
                        b.rvReportStudents.setVisibility(View.VISIBLE);
                        if (list != null) {
                            studentsList = list;
                            processAnalytics();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        b.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), R.string.msg_failed_to_load_students, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void processAnalytics() {
        if (studentsList == null || studentsList.isEmpty()) {
            b.tvEmptyState.setVisibility(View.VISIBLE);
            return;
        } else {
            b.tvEmptyState.setVisibility(View.GONE);
        }

        int studentsCount = studentsList.size();
        int totalWorking = 0;
        int totalPresent = 0;
        double overallPercentage = 0.0;
        String bestStudent = "—";
        int bestCount = -1;
        double bestPercentage = -1;

        List<AttendanceReportAdapter.StudentReportItem> reportItems = new ArrayList<>();

        for (Student s : studentsList) {
            AttendanceRecord r = new AttendanceRecord();
            if (s.monthlyAttendance != null) {
                r.monthlyData.putAll(s.monthlyAttendance);
            }
            r.recalculateTotals();
            
            totalPresent += r.totalPresent;
            totalWorking += r.totalWorking;

            AttendanceReportAdapter.StudentReportItem item = new AttendanceReportAdapter.StudentReportItem(s, r.totalPresent, r.totalWorking);
            reportItems.add(item);

            if (item.percentage > bestPercentage || (item.percentage == bestPercentage && r.totalPresent > bestCount)) {
                bestPercentage = item.percentage;
                bestCount = r.totalPresent;
                bestStudent = s.name;
            }
        }

        if (totalWorking > 0) {
            overallPercentage = ((double) totalPresent / totalWorking) * 100.0;
        }

        // Update Summary Card
        b.tvTotalStudents.setText(String.valueOf(studentsCount));
        b.tvAvgAttendance.setText(String.format(Locale.getDefault(), "%.1f%%", overallPercentage));
        b.tvTotalPresent.setText(String.valueOf(totalPresent));
        b.tvTotalWorking.setText(String.valueOf(totalWorking));
        
        if (bestCount >= 0) {
            b.tvBestAttender.setText(bestStudent + " (" + String.format(Locale.getDefault(), "%.1f%%", bestPercentage) + ")");
        } else {
            b.tvBestAttender.setText("—");
        }

        // Sort students by percentage descending
        Collections.sort(reportItems, (a, bItem) -> Double.compare(bItem.percentage, a.percentage));
        
        adapter.setData(reportItems);
        UiAnimations.setupRecyclerAnimations(b.rvReportStudents);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
