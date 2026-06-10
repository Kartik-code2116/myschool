package com.kartik.myschool.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

import com.kartik.myschool.AppCache;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsDashboardFragment extends Fragment {

    private TextView tvHeaderStripInfo;
    private TextView tvTotalStudents;
    private RecyclerView rvDashboard;
    private RecyclerView rvStudentProgress;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;
    private ImageButton btnStudentDashboard;
    
    private DashboardSubjectAdapter adapter;
    private StudentProgressDashboardAdapter studentAdapter;
    
    private ClassModel activeClass;
    private String activeSemesterId;
    private boolean isShowingStudentProgress = false;
    private int currentTotalStudents = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stats_dashboard, container, false);

        tvHeaderStripInfo = v.findViewById(R.id.tvHeaderStripInfo);
        tvTotalStudents = v.findViewById(R.id.tvTotalStudents);
        rvDashboard = v.findViewById(R.id.rvDashboard);
        rvStudentProgress = v.findViewById(R.id.rvStudentProgress);
        layoutEmptyState = v.findViewById(R.id.layoutEmptyState);
        progressBar = v.findViewById(R.id.progressBar);

        rvDashboard.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DashboardSubjectAdapter();
        rvDashboard.setAdapter(adapter);

        rvStudentProgress.setLayoutManager(new LinearLayoutManager(getContext()));
        studentAdapter = new StudentProgressDashboardAdapter();
        rvStudentProgress.setAdapter(studentAdapter);

        btnStudentDashboard = v.findViewById(R.id.btnStudentDashboard);
        if (btnStudentDashboard != null) {
            btnStudentDashboard.setOnClickListener(view -> {
                isShowingStudentProgress = !isShowingStudentProgress;
                updateViewVisibility();
            });
        }

        return v;
    }

    private void updateViewVisibility() {
        if (isShowingStudentProgress) {
            rvDashboard.setVisibility(View.GONE);
            
            rvStudentProgress.setAlpha(0f);
            rvStudentProgress.setVisibility(View.VISIBLE);
            rvStudentProgress.animate().alpha(1f).setDuration(300).start();
            
            btnStudentDashboard.setImageResource(R.drawable.ic_chart);
            
            int subCount = (activeClass != null && activeClass.subjects != null) ? activeClass.subjects.size() : 0;
            tvTotalStudents.setText(subCount + " Subjects");
            tvTotalStudents.setVisibility(View.VISIBLE);
        } else {
            rvStudentProgress.setVisibility(View.GONE);
            
            rvDashboard.setAlpha(0f);
            rvDashboard.setVisibility(View.VISIBLE);
            rvDashboard.animate().alpha(1f).setDuration(300).start();
            
            btnStudentDashboard.setImageResource(R.drawable.ic_students);
            
            tvTotalStudents.setText(currentTotalStudents + " Students");
            tvTotalStudents.setVisibility(View.VISIBLE);
        }
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
            tvHeaderStripInfo.setText("Semester " + semNum + " Progress");
        } else {
            tvHeaderStripInfo.setText(R.string.msg_dashboard_no_class_selected);
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
        rvStudentProgress.setVisibility(View.GONE);

        FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                final List<Student> finalStudents = (students == null) ? new ArrayList<>() : students;
                final int totalStudents = finalStudents.size();
                currentTotalStudents = totalStudents;
                
                if (isShowingStudentProgress) {
                    int subCount = (activeClass != null && activeClass.subjects != null) ? activeClass.subjects.size() : 0;
                    tvTotalStudents.setText(subCount + " Subjects");
                } else {
                    tvTotalStudents.setText(totalStudents + " Students");
                }

                if (totalStudents == 0) {
                    progressBar.setVisibility(View.GONE);
                    showEmptyState();
                    return;
                }

                FirebaseRepository.get().getMarksForClassAndSemester(activeClass.id, activeSemesterId, new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                    @Override
                    public void onSuccess(Map<String, MarksRecord> marksMap) {
                        progressBar.setVisibility(View.GONE);
                        
                        // Merge with local cache ONLY if cache belongs to the active class and semester
                        if (AppCache.cachedDescriptiveMarksMap != null 
                                && java.util.Objects.equals(activeClass.id, AppCache.cachedDescriptiveClassId)
                                && java.util.Objects.equals(activeSemesterId, AppCache.cachedDescriptiveSemesterId)) {
                            for (Map.Entry<String, MarksRecord> entry : AppCache.cachedDescriptiveMarksMap.entrySet()) {
                                if (entry.getValue().updatedAt >= (marksMap.containsKey(entry.getKey()) ? marksMap.get(entry.getKey()).updatedAt : 0)) {
                                    marksMap.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }

                        if (AppCache.cachedMarksMap != null
                                && java.util.Objects.equals(activeClass.id, AppCache.cachedClassIdForStudents)
                                && java.util.Objects.equals(activeSemesterId, AppCache.cachedSemesterIdForMarks)) {
                            for (Map.Entry<String, MarksRecord> entry : AppCache.cachedMarksMap.entrySet()) {
                                if (entry.getValue().updatedAt >= (marksMap.containsKey(entry.getKey()) ? marksMap.get(entry.getKey()).updatedAt : 0)) {
                                    marksMap.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }

                        calculateAndDisplayStats(totalStudents, finalStudents, marksMap);
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

    private void calculateAndDisplayStats(int totalStudents, List<Student> students, Map<String, MarksRecord> marksMap) {
        if (activeClass == null || activeClass.subjects == null || activeClass.subjects.isEmpty()) {
            showEmptyState();
            return;
        }

        List<DashboardSubjectAdapter.SubjectStats> statsList = new ArrayList<>();
        List<StudentProgressDashboardAdapter.StudentProgressStats> studentStatsList = new ArrayList<>();
        
        int totalSubjects = activeClass.subjects.size();

        // Initialize student stats
        for (Student student : students) {
            studentStatsList.add(new StudentProgressDashboardAdapter.StudentProgressStats(student, totalSubjects));
        }

        for (int i = 0; i < activeClass.subjects.size(); i++) {
            Subject subject = activeClass.subjects.get(i);
            DashboardSubjectAdapter.SubjectStats stat = new DashboardSubjectAdapter.SubjectStats(subject, i + 1, totalStudents);

            String safeKey = MarksRecord.sanitizeKey(subject.name);

            // Calculate progress for students belonging to this class only
            for (int sIdx = 0; sIdx < students.size(); sIdx++) {
                Student student = students.get(sIdx);
                StudentProgressDashboardAdapter.StudentProgressStats stuStat = studentStatsList.get(sIdx);
                
                MarksRecord record = marksMap.get(student.id);
                if (record != null && record.detailedMarks != null) {
                    MarksRecord.SubjectMarksDetail detail = record.detailedMarks.get(safeKey);
                    if (detail != null) {
                        if (detail.akarikTotal > 0) {
                            stat.formativeFilled++;
                            stuStat.formativeFilled++;
                        }
                        if (detail.sanklit > 0) {
                            stat.summativeFilled++;
                            stuStat.summativeFilled++;
                        }
                        if (detail.remark != null) {
                            String cleanRemark = detail.remark.replace("||", "").replace("null", "").trim();
                            if (!cleanRemark.isEmpty()) {
                                stat.descriptiveFilled++;
                                stuStat.descriptiveFilled++;
                            }
                        }
                    }
                }
            }

            statsList.add(stat);
        }

        adapter.setData(statsList);
        studentAdapter.setData(studentStatsList);
        
        updateViewVisibility();
    }

    private void showEmptyState() {
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvDashboard.setVisibility(View.GONE);
        rvStudentProgress.setVisibility(View.GONE);
    }
}
