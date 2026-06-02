package com.example.myschool.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myschool.AppCache;
import com.example.myschool.R;
import com.example.myschool.SessionContext;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.Student;
import com.example.myschool.model.Subject;
import com.example.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsDashboardFragment extends Fragment {

    private TextView tvHeaderStripInfo;
    private TextView tvTotalStudents;
    private RecyclerView rvDashboard;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;
    
    private DashboardSubjectAdapter adapter;
    private ClassModel activeClass;
    private String activeSemesterId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stats_dashboard, container, false);

        tvHeaderStripInfo = v.findViewById(R.id.tvHeaderStripInfo);
        tvTotalStudents = v.findViewById(R.id.tvTotalStudents);
        rvDashboard = v.findViewById(R.id.rvDashboard);
        layoutEmptyState = v.findViewById(R.id.layoutEmptyState);
        progressBar = v.findViewById(R.id.progressBar);

        rvDashboard.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DashboardSubjectAdapter();
        rvDashboard.setAdapter(adapter);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        activeClass = SessionContext.selectedClass;
        if (SessionContext.selectedSemester != null && SessionContext.selectedSemester.id != null) {
            activeSemesterId = SessionContext.selectedSemester.id;
        } else {
            activeSemesterId = "sem_1";
        }

        updateHeader();
        loadDashboardData();
    }

    private void updateHeader() {
        if (activeClass != null) {
            String semNum = SessionContext.selectedSemester != null ? String.valueOf(SessionContext.selectedSemester.number) : "1";
            tvHeaderStripInfo.setText("Class " + activeClass.className + " Div " + activeClass.division + " • Semester " + semNum);
        } else {
            tvHeaderStripInfo.setText("Dashboard - No Class Selected");
        }
    }

    private void loadDashboardData() {
        if (activeClass == null || activeClass.id == null) {
            showEmptyState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        rvDashboard.setVisibility(View.GONE);

        FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                if (students == null) students = new ArrayList<>();
                final int totalStudents = students.size();
                tvTotalStudents.setText(totalStudents + " Students");

                if (totalStudents == 0) {
                    progressBar.setVisibility(View.GONE);
                    showEmptyState();
                    return;
                }

                FirebaseRepository.get().getMarksForClassAndSemester(activeClass.id, activeSemesterId, new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                    @Override
                    public void onSuccess(Map<String, MarksRecord> marksMap) {
                        progressBar.setVisibility(View.GONE);
                        
                        // Merge with local cache
                        if (AppCache.cachedDescriptiveMarksMap != null) {
                            for (Map.Entry<String, MarksRecord> entry : AppCache.cachedDescriptiveMarksMap.entrySet()) {
                                if (entry.getValue().updatedAt >= (marksMap.containsKey(entry.getKey()) ? marksMap.get(entry.getKey()).updatedAt : 0)) {
                                    marksMap.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }

                        if (AppCache.cachedMarksMap != null) {
                            for (Map.Entry<String, MarksRecord> entry : AppCache.cachedMarksMap.entrySet()) {
                                if (entry.getValue().updatedAt >= (marksMap.containsKey(entry.getKey()) ? marksMap.get(entry.getKey()).updatedAt : 0)) {
                                    marksMap.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }

                        calculateAndDisplayStats(totalStudents, marksMap);
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to load marks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void calculateAndDisplayStats(int totalStudents, Map<String, MarksRecord> marksMap) {
        if (activeClass == null || activeClass.subjects == null || activeClass.subjects.isEmpty()) {
            showEmptyState();
            return;
        }

        rvDashboard.setVisibility(View.VISIBLE);
        List<DashboardSubjectAdapter.SubjectStats> statsList = new ArrayList<>();

        for (int i = 0; i < activeClass.subjects.size(); i++) {
            Subject subject = activeClass.subjects.get(i);
            DashboardSubjectAdapter.SubjectStats stat = new DashboardSubjectAdapter.SubjectStats(subject, i + 1, totalStudents);

            String safeKey = MarksRecord.sanitizeKey(subject.name);

            // Calculate progress
            for (MarksRecord record : marksMap.values()) {
                if (record != null && record.detailedMarks != null) {
                    MarksRecord.SubjectMarksDetail detail = record.detailedMarks.get(safeKey);
                    if (detail != null) {
                        if (detail.akarikTotal > 0) {
                            stat.formativeFilled++;
                        }
                        if (detail.sanklit > 0) {
                            stat.summativeFilled++;
                        }
                        if (detail.remark != null && !detail.remark.trim().isEmpty()) {
                            stat.descriptiveFilled++;
                        }
                    }
                }
            }

            statsList.add(stat);
        }

        adapter.setData(statsList);
    }

    private void showEmptyState() {
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvDashboard.setVisibility(View.GONE);
    }
}
