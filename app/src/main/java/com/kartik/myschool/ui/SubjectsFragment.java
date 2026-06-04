package com.kartik.myschool.ui;

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

import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.adapter.SubjectAdapter;
import com.kartik.myschool.databinding.FragmentSubjectsBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;

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
                Toast.makeText(getContext(), R.string.msg_no_active_class_selected_pleas, Toast.LENGTH_SHORT).show();
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

            // FIX: Save to SharedPrefs IMMEDIATELY (before Firestore returns) so
            // EnterMarksActivity always reads fresh subjects even if user navigates fast.
            SessionContext.save(getContext());
            // Also sync AppCache so openMarksEntry uses the latest subjects
            com.kartik.myschool.AppCache.selectedClass = selectedClass;

            // Persist to Firestore in background
            FirebaseRepository.get().saveClass(selectedClass, new FirebaseRepository.OnResult<String>() {
                @Override
                public void onSuccess(String result) {
                    // SharedPrefs already saved above; just refresh adapter UI
                    if (isAdded()) {
                        adapter.setData(getPredefinedSubjects(), selectedClass.subjects);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
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
                    v -> Toast.makeText(getContext(), R.string.msg_subjects_help_guidelines, Toast.LENGTH_SHORT).show(),
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
        // ── Academic subjects ──────────────────────────────────────────────────
        list.add(new SubjectAdapter.SubjectItem("Marathi",            "101101", "01", "Academic",    100, "FE: 50", "SE: 50", "", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Hindi",              "101102", "02", "Academic",    100, "FE: 50", "SE: 50", "", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("English",            "101103", "03", "Academic",    100, "FE: 50", "SE: 50", "", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Mathematics",        "101104", "04", "Academic",    100, "FE: 50", "SE: 50", "", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Science",            "101105", "05", "Academic",    100, "FE: 50", "SE: 50", "", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Science / EVS",      "101106", "06", "Academic",    100, "FE: 50", "SE: 50", "", "#2196F3"));
        list.add(new SubjectAdapter.SubjectItem("Soc. Science",       "101107", "07", "Academic",    100, "FE: 50", "SE: 50", "", "#2196F3"));
        // ── Activity subjects ──────────────────────────────────────────────────
        list.add(new SubjectAdapter.SubjectItem("Drawing",            "101201", "08", "Activities",  100, "FE: 100", "",       "", "#4CAF50"));
        list.add(new SubjectAdapter.SubjectItem("Work Experience",    "101202", "09", "Activities",  100, "FE: 100", "",       "", "#4CAF50"));
        list.add(new SubjectAdapter.SubjectItem("Physical Education", "101203", "10", "Activities",  100, "FE: 100", "",       "", "#4CAF50"));
        // ── Personality development ────────────────────────────────────────────
        list.add(new SubjectAdapter.SubjectItem("Special Development","101301", "11", "Personality", 100, "FE: 100", "",       "", "#009688"));
        return list;
    }
}
