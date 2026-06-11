package com.kartik.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
        
        b.fabAddSubject.setOnClickListener(v -> showAddCustomSubjectDialog());
        
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
            
            // Ensure subjects are always saved and displayed in predefined order
            Subject.sortSubjects(selectedClass.subjects);

            // FIX: Save to SharedPrefs IMMEDIATELY (before Firestore returns) so
            // EnterMarksActivity always reads fresh subjects even if user navigates fast.
            SessionContext.save(getContext());
            // Also sync AppCache so openMarksEntry uses the latest subjects
            com.kartik.myschool.AppCache.selectedClass = selectedClass;

            // Refresh adapter UI immediately to show the toggle change
            adapter.updateActiveSubjects(selectedClass.subjects);

            // Persist to Firestore in background
            FirebaseRepository.get().saveClass(selectedClass, new FirebaseRepository.OnResult<String>() {
                @Override
                public void onSuccess(String result) {
                    // Already refreshed UI above
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
        final List<Subject> activeSubjects;
        if (SessionContext.selectedClass != null && SessionContext.selectedClass.subjects != null) {
            activeSubjects = SessionContext.selectedClass.subjects;
        } else {
            activeSubjects = new ArrayList<>();
        }
        
        List<SubjectAdapter.SubjectItem> predefined = getPredefinedSubjects();
        
        // Ensure any custom subjects already saved by the teacher are shown in the list
        // and update predefined items if the teacher modified their maxMarks or distribution.
        for (Subject active : activeSubjects) {
            boolean found = false;
            for (SubjectAdapter.SubjectItem item : predefined) {
                if (item.name.equalsIgnoreCase(active.name)) {
                    item.maxMarks = active.maxMarks;
                    int fe = active.maxNirikhshan + active.maxTondiKam + active.maxPratyakshik + active.maxUpkram + active.maxPrakalp + active.maxChachani + active.maxSwadhyay + active.maxItar;
                    int se = active.maxTondi + active.maxPratyakshikB + active.maxLekhi;
                    item.detailsLeft1 = "FE: " + fe;
                    item.detailsLeft2 = se > 0 ? "SE: " + se : "";
                    found = true; break;
                }
            }
            if (!found) {
                int nextOrder = predefined.size() + 1;
                String orderStr = String.format("%02d", nextOrder);
                int fe = active.maxNirikhshan + active.maxTondiKam + active.maxPratyakshik + active.maxUpkram + active.maxPrakalp + active.maxChachani + active.maxSwadhyay + active.maxItar;
                int se = active.maxTondi + active.maxPratyakshikB + active.maxLekhi;
                String seStr = se > 0 ? "SE: " + se : "";
                predefined.add(new SubjectAdapter.SubjectItem(active.name, "", orderStr, "Custom", active.maxMarks, "FE: " + fe, seStr, "", "#9C27B0"));
            }
        }
        
        adapter.setData(predefined, activeSubjects);

        // Fetch global subjects defined by Admin and merge
        FirebaseRepository.get().getGlobalSubjects(new FirebaseRepository.OnResult<List<Subject>>() {
            @Override
            public void onSuccess(List<Subject> globalSubjects) {
                if (globalSubjects != null && !globalSubjects.isEmpty() && isAdded()) {
                    List<SubjectAdapter.SubjectItem> currentList = new ArrayList<>(predefined);
                    for (Subject gSub : globalSubjects) {
                        boolean exists = false;
                        for (SubjectAdapter.SubjectItem existing : currentList) {
                            if (existing.name.equalsIgnoreCase(gSub.name)) {
                                if ("Custom".equals(existing.category)) {
                                    existing.category = "Global";
                                    existing.colorHex = "#FF5722";
                                }
                                exists = true; break;
                            }
                        }
                        if (!exists) {
                            int nextOrder = currentList.size() + 1;
                            String orderStr = String.format("%02d", nextOrder);
                            Subject dummy = new Subject(gSub.name, gSub.maxMarks);
                            int fe = dummy.maxNirikhshan + dummy.maxTondiKam + dummy.maxPratyakshik + dummy.maxUpkram + dummy.maxPrakalp + dummy.maxChachani + dummy.maxSwadhyay + dummy.maxItar;
                            int se = dummy.maxTondi + dummy.maxPratyakshikB + dummy.maxLekhi;
                            String seStr = se > 0 ? "SE: " + se : "";
                            currentList.add(new SubjectAdapter.SubjectItem(gSub.name, "", orderStr, "Global", gSub.maxMarks, "FE: " + fe, seStr, "", "#FF5722"));
                        }
                    }
                    adapter.setData(currentList, activeSubjects);
                }
            }
            @Override
            public void onError(Exception e) {
                // Ignore, just use predefined
            }
        });
    }

    private void showAddCustomSubjectDialog() {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(getContext(), R.string.msg_no_active_class_selected_pleas, Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final android.widget.EditText inputName = new android.widget.EditText(getContext());
        inputName.setHint("Subject Name (e.g. Computer)");
        layout.addView(inputName);

        final android.widget.EditText inputMarks = new android.widget.EditText(getContext());
        inputMarks.setHint("Max Marks (e.g. 100)");
        inputMarks.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputMarks);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Custom Subject")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    String marksStr = inputMarks.getText().toString().trim();
                    
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    int marks = 100;
                    if (!marksStr.isEmpty()) {
                        try { marks = Integer.parseInt(marksStr); } catch (Exception ignored) {}
                    }
                    
                    ClassModel cls = SessionContext.selectedClass;
                    if (cls.subjects == null) cls.subjects = new ArrayList<>();
                    
                    // Check if already exists
                    for (Subject s : cls.subjects) {
                        if (s.name.equalsIgnoreCase(name)) {
                            Toast.makeText(getContext(), "Subject already exists in your class", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    
                    cls.subjects.add(new Subject(name, marks));
                    Subject.sortSubjects(cls.subjects);
                    
                    SessionContext.save(getContext());
                    com.kartik.myschool.AppCache.selectedClass = cls;
                    
                    FirebaseRepository.get().saveClass(cls, new FirebaseRepository.OnResult<String>() {
                        @Override public void onSuccess(String result) {
                            if (isAdded()) {
                                loadSubjects(); // Reload to update UI
                                Toast.makeText(getContext(), "Custom subject added", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onError(Exception e) {
                            if (isAdded()) Toast.makeText(getContext(), "Failed to save class", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSubjects();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) getActivity();
            activity.showCustomToolbarActions(
                    true,
                    v -> com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(activity, "subjects"),
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
            android.widget.ImageButton btn = activity.findViewById(R.id.btnToolbarNotifications);
            if (btn != null) {
                btn.setImageResource(R.drawable.ic_help_outline);
                btn.setContentDescription("Help");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).showCustomToolbarActions(false, null, null);
        }
    }

    private SubjectAdapter.SubjectItem createItem(String name, String code, String serial, String category, int maxMarks, String color) {
        Subject s = new Subject(name, maxMarks);
        int fe = s.maxNirikhshan + s.maxTondiKam + s.maxPratyakshik + s.maxUpkram + s.maxPrakalp + s.maxChachani + s.maxSwadhyay + s.maxItar;
        int se = s.maxTondi + s.maxPratyakshikB + s.maxLekhi;
        String seStr = se > 0 ? "SE: " + se : "";
        return new SubjectAdapter.SubjectItem(name, code, serial, category, maxMarks, "FE: " + fe, seStr, "", color);
    }

    private List<SubjectAdapter.SubjectItem> getPredefinedSubjects() {
        List<SubjectAdapter.SubjectItem> list = new ArrayList<>();
        // ── Academic subjects ──────────────────────────────────────────────────
        list.add(createItem("Marathi",            "101101", "01", "Academic",    100, "#2196F3"));
        list.add(createItem("Hindi",              "101102", "02", "Academic",    100, "#2196F3"));
        list.add(createItem("English",            "101103", "03", "Academic",    100, "#2196F3"));
        list.add(createItem("Mathematics",        "101104", "04", "Academic",    100, "#2196F3"));
        list.add(createItem("Science",            "101105", "05", "Academic",    100, "#2196F3"));
        list.add(createItem("Science / EVS",      "101106", "06", "Academic",    100, "#2196F3"));
        list.add(createItem("Soc. Science",       "101107", "07", "Academic",    100, "#2196F3"));
        // ── Activity subjects ──────────────────────────────────────────────────
        list.add(createItem("Drawing",            "101201", "08", "Activities",  100, "#4CAF50"));
        list.add(createItem("Work Experience",    "101202", "09", "Activities",  100, "#4CAF50"));
        list.add(createItem("Physical Education", "101203", "10", "Activities",  100, "#4CAF50"));
        // ── Personality development ────────────────────────────────────────────
        list.add(createItem("Special Development","101301", "11", "Personality", 100, "#009688"));
        list.add(createItem("Personality Development","101302", "12", "Personality", 100, "#009688"));
        // ── State Board Additions ──────────────────────────────────────────────
        list.add(createItem("Information & Comm. Technology (ICT)", "101401", "13", "State Board", 100, "#FF9800"));
        list.add(createItem("Water Security & Environment Studies", "101402", "14", "State Board", 100, "#FF9800"));
        return list;
    }
}
