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
                if (s.name != null && Subject.isSameSubject(s.name, item.name)) {
                    match = s;
                    break;
                }
            }

            if (isActive) {
                if (match == null) {
                    Subject newSub = new Subject(item.name, item.maxMarks);
                    newSub.subjectCode = item.code;
                    selectedClass.subjects.add(newSub);
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
            // Deduplicate active subjects to remove English vs Marathi duplicates (e.g. Science vs विज्ञान)
            List<Subject> cleanList = new ArrayList<>();
            boolean modified = false;
            for (Subject active : SessionContext.selectedClass.subjects) {
                // Auto-fix non-academic subjects that have SE marks (e.g., 50-50 instead of 100-0)
                if (Subject.isNonAcademic(active.name)) {
                    int se = active.maxTondi + active.maxPratyakshikB + active.maxLekhi;
                    if (se > 0) {
                        Subject fixed = new Subject(active.name, active.maxMarks);
                        fixed.subjectCode = active.subjectCode;
                        active = fixed;
                        modified = true;
                    }
                }
                
                boolean duplicate = false;
                for (Subject clean : cleanList) {
                    if (Subject.isSameSubject(clean.name, active.name)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    cleanList.add(active);
                }
            }
            if (modified || cleanList.size() != SessionContext.selectedClass.subjects.size()) {
                SessionContext.selectedClass.subjects = cleanList;
                SessionContext.save(getContext());
                com.kartik.myschool.AppCache.selectedClass = SessionContext.selectedClass;
                FirebaseRepository.get().saveClass(SessionContext.selectedClass, null);
            }
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
                if (Subject.isSameSubject(item.name, active.name)) {
                    item.maxMarks = active.maxMarks;
                    int fe = active.maxNirikhshan + active.maxTondiKam + active.maxPratyakshik + active.maxUpkram + active.maxPrakalp + active.maxChachani + active.maxSwadhyay + active.maxItar;
                    int se = active.maxTondi + active.maxPratyakshikB + active.maxLekhi;
                    item.detailsLeft1 = "FE: " + fe;
                    item.detailsLeft2 = se > 0 ? "SE: " + se : "";
                    if (active.subjectCode != null && !active.subjectCode.isEmpty()) {
                        item.code = active.subjectCode;
                    }
                    found = true; break;
                }
            }
            if (!found) {
                int nextOrder = predefined.size() + 1;
                String orderStr = String.format(java.util.Locale.US, "%02d", nextOrder);
                int fe = active.maxNirikhshan + active.maxTondiKam + active.maxPratyakshik + active.maxUpkram + active.maxPrakalp + active.maxChachani + active.maxSwadhyay + active.maxItar;
                int se = active.maxTondi + active.maxPratyakshikB + active.maxLekhi;
                String seStr = se > 0 ? "SE: " + se : "";
                String code = (active.subjectCode != null && !active.subjectCode.isEmpty()) ? active.subjectCode : "";
                predefined.add(new SubjectAdapter.SubjectItem(active.name, code, orderStr, "Custom", active.maxMarks, "FE: " + fe, seStr, "", "#9C27B0"));
            }
        }
        
        adapter.setData(deduplicateItems(predefined), activeSubjects);

        // Fetch global subjects defined by Admin and merge
        FirebaseRepository.get().getGlobalSubjects(new FirebaseRepository.OnResult<List<Subject>>() {
            @Override
            public void onSuccess(List<Subject> globalSubjects) {
                if (globalSubjects != null && !globalSubjects.isEmpty() && isAdded()) {
                    List<SubjectAdapter.SubjectItem> currentList = new ArrayList<>(predefined);
                    for (Subject gSub : globalSubjects) {
                        boolean exists = false;
                        for (SubjectAdapter.SubjectItem existing : currentList) {
                            if (Subject.isSameSubject(existing.name, gSub.name)) {
                                if ("Custom".equals(existing.category)) {
                                    existing.category = "Global";
                                    existing.colorHex = "#FF5722";
                                }
                                exists = true; break;
                            }
                        }
                        if (!exists) {
                            int nextOrder = currentList.size() + 1;
                            String orderStr = String.format(java.util.Locale.US, "%02d", nextOrder);
                            Subject dummy = new Subject(gSub.name, gSub.maxMarks);
                            int fe = dummy.maxNirikhshan + dummy.maxTondiKam + dummy.maxPratyakshik + dummy.maxUpkram + dummy.maxPrakalp + dummy.maxChachani + dummy.maxSwadhyay + dummy.maxItar;
                            int se = dummy.maxTondi + dummy.maxPratyakshikB + dummy.maxLekhi;
                            String seStr = se > 0 ? "SE: " + se : "";
                            currentList.add(new SubjectAdapter.SubjectItem(gSub.name, "", orderStr, "Global", gSub.maxMarks, "FE: " + fe, seStr, "", "#FF5722"));
                        }
                    }
                    adapter.setData(deduplicateItems(currentList), activeSubjects);
                }
            }
            @Override
            public void onError(Exception e) {
                // Ignore, just use predefined
            }
        });
    }

    private List<SubjectAdapter.SubjectItem> deduplicateItems(List<SubjectAdapter.SubjectItem> list) {
        List<SubjectAdapter.SubjectItem> clean = new ArrayList<>();
        for (SubjectAdapter.SubjectItem item : list) {
            boolean dup = false;
            for (SubjectAdapter.SubjectItem c : clean) {
                if (Subject.isSameSubject(c.name, item.name)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                clean.add(item);
            }
        }
        return clean;
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
                        if (Subject.isSameSubject(s.name, name)) {
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
        int std = 1;
        if (SessionContext.selectedClass != null && SessionContext.selectedClass.className != null) {
            try {
                String clean = SessionContext.selectedClass.className.replaceAll("[^0-9]", "");
                if (!clean.isEmpty()) {
                    std = Integer.parseInt(clean);
                    if (std < 1 || std > 10) std = 1;
                }
            } catch (Exception ignored) {}
        }
        
        String p1 = String.format(java.util.Locale.US, "1%02d1", std); // Academic e.g. "1011", "1021"
        String p2 = String.format(java.util.Locale.US, "1%02d2", std); // Activity e.g. "1012"
        String p3 = String.format(java.util.Locale.US, "1%02d3", std); // Personality e.g. "1013"
        String p4 = String.format(java.util.Locale.US, "1%02d4", std); // State Board e.g. "1014"

        List<SubjectAdapter.SubjectItem> list = new ArrayList<>();
        // ── Academic subjects ──────────────────────────────────────────────────
        list.add(createItem("Marathi",            p1 + "01", "01", "Academic",    100, "#2196F3"));
        list.add(createItem("Hindi",              p1 + "02", "02", "Academic",    100, "#2196F3"));
        list.add(createItem("English",            p1 + "03", "03", "Academic",    100, "#2196F3"));
        list.add(createItem("Sanskrit",           p1 + "04", "04", "Academic",    100, "#2196F3"));
        list.add(createItem("Mathematics",        p1 + "05", "05", "Academic",    100, "#2196F3"));
        list.add(createItem("Science",            p1 + "06", "06", "Academic",    100, "#2196F3"));
        list.add(createItem("History",            p1 + "07", "07", "Academic",    100, "#2196F3"));
        list.add(createItem("Social Science",     p1 + "08", "08", "Academic",    100, "#2196F3"));
        // ── Activity subjects ──────────────────────────────────────────────────
        list.add(createItem("Drawing",            p2 + "01", "09", "Activities",  100, "#4CAF50"));
        list.add(createItem("Work Experience",    p2 + "02", "10", "Activities",  100, "#4CAF50"));
        list.add(createItem("Physical Education", p2 + "03", "11", "Activities",  100, "#4CAF50"));
        // ── Personality development ────────────────────────────────────────────
        list.add(createItem("Special Development",p3 + "01", "12", "Personality", 100, "#009688"));
        list.add(createItem("Personality Development",p3 + "02", "13", "Personality", 100, "#009688"));
        // ── State Board Additions ──────────────────────────────────────────────
        list.add(createItem("Information & Comm. Technology (ICT)", p4 + "01", "14", "State Board", 100, "#FF9800"));
        list.add(createItem("Water Security & Environment Studies", p4 + "02", "15", "State Board", 100, "#FF9800"));
        return list;
    }
}
