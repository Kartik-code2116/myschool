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
import com.kartik.myschool.adapter.DescriptiveReportAdapter;
import com.kartik.myschool.databinding.FragmentDescriptiveReportBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DescriptiveReportFragment extends Fragment {

    private FragmentDescriptiveReportBinding b;
    private DescriptiveReportAdapter adapter;
    private List<Student> studentsList = new ArrayList<>();
    private Map<String, MarksRecord> marksMap = new HashMap<>();

    private ClassModel activeClass;
    private String activeSemesterId = "sem_1";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentDescriptiveReportBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SessionContext.ensureCacheLoaded(requireContext());

        activeClass = SessionContext.selectedClass;
        if (SessionContext.selectedSemester != null) {
            activeSemesterId = SessionContext.selectedSemester.id;
        }

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
                com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(requireContext(), "descriptive_report");
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        b.rvReportStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DescriptiveReportAdapter();
        b.rvReportStudents.setAdapter(adapter);
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        String classVal = "5";
        String divVal = "1";
        if (activeClass != null) {
            classVal = activeClass.className != null ? activeClass.className : "5";
            divVal = activeClass.division != null && !activeClass.division.isEmpty() ? activeClass.division : "-";
        }
        b.tvReportContext.setText("Year: " + yearLabel + " | Class: " + classVal + "-" + divVal);
    }

    private void loadData() {
        if (activeClass == null) {
            Toast.makeText(getContext(), R.string.msg_please_select_class_first, Toast.LENGTH_SHORT).show();
            return;
        }
        
        b.progressBar.setVisibility(View.VISIBLE);
        b.rvReportStudents.setVisibility(View.GONE);
        
        final boolean[] fetchesDone = new boolean[2];

        FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                if (list != null) studentsList = list;
                fetchesDone[0] = true;
                checkDataComplete(fetchesDone);
            }
            @Override
            public void onError(Exception e) {
                fetchesDone[0] = true;
                checkDataComplete(fetchesDone);
            }
        });

        FirebaseRepository.get().getMarksForClassAndSemester(activeClass.id, activeSemesterId, new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
            @Override
            public void onSuccess(Map<String, MarksRecord> map) {
                if (map != null) marksMap = map;
                fetchesDone[1] = true;
                checkDataComplete(fetchesDone);
            }
            @Override
            public void onError(Exception e) {
                fetchesDone[1] = true;
                checkDataComplete(fetchesDone);
            }
        });
    }

    private void checkDataComplete(boolean[] fetchesDone) {
        if (fetchesDone[0] && fetchesDone[1]) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    b.progressBar.setVisibility(View.GONE);
                    b.rvReportStudents.setVisibility(View.VISIBLE);
                    processAnalytics();
                });
            }
        }
    }

    private void processAnalytics() {
        if (studentsList == null || studentsList.isEmpty()) {
            b.tvEmptyState.setVisibility(View.VISIBLE);
            return;
        } else {
            b.tvEmptyState.setVisibility(View.GONE);
        }

        int studentsCount = studentsList.size();
        
        List<Subject> allDescriptiveSubjects = new ArrayList<>();
        if (activeClass != null && activeClass.subjects != null) {
            allDescriptiveSubjects.addAll(activeClass.subjects);
        }
        allDescriptiveSubjects.add(new Subject("Vishesh pragati", 0));
        allDescriptiveSubjects.add(new Subject("Aavad, chanda, etc", 0));
        allDescriptiveSubjects.add(new Subject("Sudharna Aavashyaka", 0));
        allDescriptiveSubjects.add(new Subject("Vyaktimatva gun vishgesh", 0));

        int totalAttributes = allDescriptiveSubjects.size();
        if (totalAttributes == 0) totalAttributes = 1; // avoid divide by zero

        int classTotalRemarks = 0;
        int classMaxPossibleRemarks = studentsCount * totalAttributes;
        
        String bestStudent = "—";
        int bestRemarksCount = -1;

        List<DescriptiveReportAdapter.StudentRemarkItem> reportItems = new ArrayList<>();

        for (Student s : studentsList) {
            MarksRecord record = marksMap.get(s.id);
            int studentRemarksCount = 0;

            if (record != null && record.detailedMarks != null) {
                for (Subject sub : allDescriptiveSubjects) {
                    MarksRecord.SubjectMarksDetail detail = DescriptiveEntriesFragment.getSubjectDetail(record, sub, 0);
                    if (detail != null && detail.remark != null && !detail.remark.trim().isEmpty()) {
                        studentRemarksCount++;
                    }
                }
            }

            DescriptiveReportAdapter.StudentRemarkItem item = new DescriptiveReportAdapter.StudentRemarkItem(s, studentRemarksCount, totalAttributes);
            reportItems.add(item);

            if (studentRemarksCount > bestRemarksCount) {
                bestRemarksCount = studentRemarksCount;
                bestStudent = s.name;
            }

            classTotalRemarks += studentRemarksCount;
        }

        double overallPercentage = classMaxPossibleRemarks > 0 ? ((double) classTotalRemarks / classMaxPossibleRemarks) * 100.0 : 0.0;

        // Update Summary Card
        b.tvTotalStudents.setText(String.valueOf(studentsCount));
        b.tvTotalSubjects.setText(String.valueOf(totalAttributes));
        b.tvRemarksFilled.setText(String.valueOf(classTotalRemarks));
        b.tvCompletion.setText(String.format(Locale.getDefault(), "%.1f%%", overallPercentage));
        
        if (bestRemarksCount >= 0) {
            b.tvTopStudent.setText(bestStudent + " (" + bestRemarksCount + " remarks)");
        } else {
            b.tvTopStudent.setText("—");
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
