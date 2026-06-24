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
import com.kartik.myschool.adapter.EvaluationReportAdapter;
import com.kartik.myschool.databinding.FragmentEvaluationReportBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.GradeCalculator;
import com.kartik.myschool.utils.UiAnimations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EvaluationReportFragment extends Fragment {

    private FragmentEvaluationReportBinding b;
    private EvaluationReportAdapter adapter;
    private List<Student> studentsList = new ArrayList<>();
    private Map<String, MarksRecord> marksMap = new HashMap<>();

    private ClassModel activeClass;
    private String activeSemesterId = "sem_1";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentEvaluationReportBinding.inflate(inflater, container, false);
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
                com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(requireContext(), "evaluation_report");
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        b.rvReportStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EvaluationReportAdapter();
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
        b.tvReportContext.setText("Year: " + yearLabel + " | Cls: " + classVal + "-" + divVal);
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
        int passedCount = 0;
        int failedCount = 0;
        int totalClassMarks = 0;
        int totalClassOutOf = 0;
        
        String bestStudent = "—";
        double bestPercentage = -1;

        List<EvaluationReportAdapter.StudentEvalItem> reportItems = new ArrayList<>();

        for (Student s : studentsList) {
            MarksRecord record = marksMap.get(s.id);
            int studentTotalMarks = 0;
            int studentOutOfMarks = 0;

            if (record != null && record.detailedMarks != null && activeClass != null && activeClass.subjects != null) {
                for (Subject sub : activeClass.subjects) {
                    MarksRecord.SubjectMarksDetail detail = DescriptiveEntriesFragment.getSubjectDetail(record, sub, 0);
                    if (detail != null) {
                        studentTotalMarks += detail.grandTotal;
                        if (detail.maxMarks > 0) {
                            studentOutOfMarks += detail.maxMarks;
                        } else {
                            studentOutOfMarks += 100; // Default to 100 if maxMarks is 0
                        }
                    } else {
                        studentOutOfMarks += 100; // Assuming 100 marks per subject
                    }
                }
            }

            if (studentOutOfMarks == 0) {
                studentOutOfMarks = 100; // Default to avoid div by zero if no subjects
            }

            double percentage = studentOutOfMarks > 0 ? ((double) studentTotalMarks / studentOutOfMarks) * 100.0 : 0.0;
            String grade = GradeCalculator.getGrade(percentage);

            EvaluationReportAdapter.StudentEvalItem item = new EvaluationReportAdapter.StudentEvalItem(s, studentTotalMarks, studentOutOfMarks, grade);
            reportItems.add(item);

            if (percentage > bestPercentage) {
                bestPercentage = percentage;
                bestStudent = s.name;
            }

            totalClassMarks += studentTotalMarks;
            totalClassOutOf += studentOutOfMarks;

            if ("E1".equals(grade) || "E2".equals(grade)) {
                failedCount++;
            } else if (!"-".equals(grade) && !grade.trim().isEmpty()) {
                passedCount++;
            }
        }

        double overallPercentage = totalClassOutOf > 0 ? ((double) totalClassMarks / totalClassOutOf) * 100.0 : 0.0;
        String avgGrade = GradeCalculator.getGrade(overallPercentage);

        // Update Summary Card
        b.tvTotalStudents.setText(String.valueOf(studentsCount));
        b.tvAvgGrade.setText(avgGrade + " (" + String.format(Locale.getDefault(), "%.1f%%", overallPercentage) + ")");
        b.tvTotalPass.setText(String.valueOf(passedCount));
        b.tvTotalFail.setText(String.valueOf(failedCount));
        
        if (bestPercentage >= 0) {
            b.tvTopScorer.setText(bestStudent + " (" + String.format(Locale.getDefault(), "%.1f%%", bestPercentage) + ")");
        } else {
            b.tvTopScorer.setText("—");
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
