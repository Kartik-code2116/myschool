package com.example.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myschool.HomeActivity;
import com.example.myschool.R;
import com.example.myschool.SessionContext;
import com.example.myschool.adapter.SubjectAdapter;
import com.example.myschool.databinding.FragmentSubjectsBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.Subject;
import com.example.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class SubjectsFragment extends Fragment {

    private FragmentSubjectsBinding b;
    private SubjectAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentSubjectsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        displayHeaderInfo();
        loadSubjects();
    }

    private void setupRecyclerView() {
        b.rvSubjectsList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SubjectAdapter();
        b.rvSubjectsList.setAdapter(adapter);

        adapter.setOnToggleListener((item, isActive) -> {
            if (SessionContext.selectedClass == null) {
                Toast.makeText(getContext(), "No active class selected. Please select a class.", Toast.LENGTH_SHORT).show();
                return;
            }

            ClassModel selectedClass = SessionContext.selectedClass;
            if (selectedClass.subjects == null) {
                selectedClass.subjects = new ArrayList<>();
            }

            Subject match = null;
            for (Subject s : selectedClass.subjects) {
                if (s.name != null && s.name.equalsIgnoreCase(item.name)) {
                    match = s;
                    break;
                }
            }

            if (isActive) {
                if (match == null) {
                    selectedClass.subjects.add(new Subject(item.name, item.maxMarks));
                }
            } else {
                if (match != null) {
                    selectedClass.subjects.remove(match);
                }
            }

            // Real-time Firestore Sync
            FirebaseRepository.get().saveClass(selectedClass, new FirebaseRepository.OnResult<String>() {
                @Override
                public void onSuccess(String result) {
                    SessionContext.syncToAppCache();
                    // Sync adapter local data state
                    adapter.setData(getPredefinedSubjects(), selectedClass.subjects);
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        String classDiv = SessionContext.getClassDivLabel();
        b.tvHeaderLabel.setText("Year: " + yearLabel + "   " + classDiv);
    }

    private void loadSubjects() {
        List<Subject> activeSubjects = new ArrayList<>();
        if (SessionContext.selectedClass != null && SessionContext.selectedClass.subjects != null) {
            activeSubjects = SessionContext.selectedClass.subjects;
        }
        adapter.setData(getPredefinedSubjects(), activeSubjects);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).showCustomToolbarActions(
                    true,
                    v -> Toast.makeText(getContext(), "Subjects Help & Guidelines", Toast.LENGTH_SHORT).show(),
                    v -> {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        popup.getMenu().add("Refresh Subjects");
                        popup.getMenu().add("Reset All");
                        popup.setOnMenuItemClickListener(menuItem -> {
                            Toast.makeText(getContext(), menuItem.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
                            return true;
                        });
                        popup.show();
                    }
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).showCustomToolbarActions(false, null, null);
        }
    }

    private List<SubjectAdapter.SubjectItem> getPredefinedSubjects() {
        List<SubjectAdapter.SubjectItem> list = new ArrayList<>();
        list.add(new SubjectAdapter.SubjectItem("Marathi", "101101", "01", "Academic", 100, "FE: 70", "I_1, I_2, etc.", "SE: 30", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Hindi", "101102", "02", "Academic", 100, "FE: 70", "I_1, I_2, etc.", "SE: 30", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("English", "101103", "03", "Academic", 100, "FE: 70", "I_1, I_2, etc.", "SE: 30", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Mathematics", "101104", "04", "Academic", 100, "FE: 70", "I_1, I_2, etc.", "SE: 30", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Science / EVS", "101105", "05", "Academic", 100, "FE: 70", "I_1, I_2, etc.", "SE: 30", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Soc. Science", "101106", "06", "Academic", 100, "FE: 70", "I_1, I_2, etc.", "SE: 30", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Drawing", "101201", "07", "Activities", 100, "FE: 100", "I_1, I_2, etc.", "", "#4CAF50"));
        list.add(new SubjectAdapter.SubjectItem("Work Experience", "101202", "08", "Activities", 100, "FE: 100", "I_1, I_2, etc.", "", "#4CAF50"));
        list.add(new SubjectAdapter.SubjectItem("Physical Education", "101203", "09", "Activities", 100, "FE: 100", "I_1, I_2, etc.", "", "#4CAF50"));
        list.add(new SubjectAdapter.SubjectItem("Special Development", "101301", "10", "Personality", 100, "FE: 100", "I_1, I_2, etc.", "", "#009688"));
        return list;
    }
}
