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
        boolean isEn = com.kartik.myschool.utils.pdf.PdfLocalizer.isEnglish(getContext());
        b.tvHeaderLabel.setText((isEn ? "Year: " : "वर्ष: ") + yearLabel + "   " + classDiv);
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
                com.kartik.myschool.model.Subject.sortSubjects(cleanList);
                SessionContext.selectedClass.subjects = cleanList;
                SessionContext.save(getContext());
                com.kartik.myschool.AppCache.selectedClass = SessionContext.selectedClass;
                FirebaseRepository.get().saveClass(SessionContext.selectedClass, new com.kartik.myschool.repository.FirebaseRepository.OnResult<String>() {
                    @Override public void onSuccess(String result) {}
                    @Override public void onError(Exception e) {}
                });
            }
            activeSubjects = SessionContext.selectedClass.subjects;
            
            // Auto-populate default subjects if class is completely empty
            if (activeSubjects.isEmpty() && SessionContext.selectedClass.className != null && !SessionContext.selectedClass.className.isEmpty()) {
                resetSubjectsToDefault(true);
                return; // UI will be refreshed by the callback
            }
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
                    item.name = active.name; // Keep the newly edited custom name
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
        
        List<SubjectAdapter.SubjectItem> finalPredefined = deduplicateItems(predefined);
        sortSubjectItems(finalPredefined);
        adapter.setData(finalPredefined, activeSubjects);

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
                    List<SubjectAdapter.SubjectItem> finalGlobal = deduplicateItems(currentList);
                    sortSubjectItems(finalGlobal);
                    adapter.setData(finalGlobal, activeSubjects);
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

    private void sortSubjectItems(List<SubjectAdapter.SubjectItem> list) {
        if (list == null || list.size() <= 1) return;
        java.util.Collections.sort(list, new java.util.Comparator<SubjectAdapter.SubjectItem>() {
            @Override
            public int compare(SubjectAdapter.SubjectItem s1, SubjectAdapter.SubjectItem s2) {
                String c1 = s1.code != null ? s1.code.trim() : "";
                String c2 = s2.code != null ? s2.code.trim() : "";
                
                if (c1.isEmpty() && c2.isEmpty()) return 0;
                if (c1.isEmpty()) return 1;
                if (c2.isEmpty()) return -1;
                
                try {
                    long n1 = Long.parseLong(c1.replaceAll("[^0-9]", ""));
                    long n2 = Long.parseLong(c2.replaceAll("[^0-9]", ""));
                    return Long.compare(n1, n2);
                } catch (Exception e) {
                    return c1.compareTo(c2);
                }
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
                        popup.getMenu().add("Reset All to Default");
                        popup.setOnMenuItemClickListener(menuItem -> {
                            if (menuItem.getTitle().toString().equals("Reset All to Default")) {
                                new AlertDialog.Builder(getContext())
                                    .setTitle("Reset Subjects")
                                    .setMessage("Are you sure you want to reset subjects to their defaults for this class standard? This will replace your current selection.")
                                    .setPositiveButton("Reset", (d, w) -> resetSubjectsToDefault(false))
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            } else {
                                loadSubjects();
                            }
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

    private void resetSubjectsToDefault(boolean silent) {
        if (SessionContext.selectedClass == null) return;
        ClassModel cls = SessionContext.selectedClass;
        
        int std = 1;
        if (cls.className != null) {
            try {
                String clean = cls.className.replaceAll("[^0-9]", "");
                if (!clean.isEmpty()) std = Integer.parseInt(clean);
            } catch (Exception ignored) {}
        }
        
        List<SubjectAdapter.SubjectItem> predefined = getPredefinedSubjects();
        List<Subject> newSubjects = new ArrayList<>();
        
        List<String> req = new ArrayList<>();
        req.add("Marathi");
        req.add("English");
        req.add("Mathematics");
        
        if (std == 1 || std == 2) {
            req.add("Play, Do, Learn");
        } else if (std == 3 || std == 4) {
            req.add("Environmental Studies");
            req.add("Play, Do, Learn");
        } else if (std == 5) {
            req.add("Hindi");
            req.add("Environmental Studies Part 1");
            req.add("Environmental Studies Part 2");
            req.add("Health & Physical Education");
            req.add("Work Experience");
            req.add("Art");
        } else {
            req.add("Hindi");
            req.add("Science");
            req.add("History and Civics");
            req.add("Geography");
            req.add("Health & Physical Education");
            req.add("Work Experience");
            req.add("Art");
        }
        
        for (String nameToFind : req) {
            for (SubjectAdapter.SubjectItem item : predefined) {
                if (Subject.isSameSubject(item.name, nameToFind)) {
                    Subject s = new Subject(item.name, item.maxMarks);
                    s.subjectCode = item.code;
                    newSubjects.add(s);
                    break;
                }
            }
        }
        
        cls.subjects = newSubjects;
        Subject.sortSubjects(cls.subjects);
        SessionContext.save(getContext());
        com.kartik.myschool.AppCache.selectedClass = cls;
        
        final int finalStd = std;
        FirebaseRepository.get().saveClass(cls, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String result) {
                if (isAdded()) {
                    loadSubjects();
                    if (!silent) Toast.makeText(getContext(), "Subjects reset for Std " + finalStd, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(Exception e) {
                if (isAdded() && !silent) Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
        int academicIdx = 1;
        int activityIdx = 1;
        int totalIdx = 1;

        // 1. First Language (Marathi)
        list.add(createItem("Marathi", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        
        // 2. Second Language (English)
        list.add(createItem("English", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));

        // 3. Third Language (Hindi)
        list.add(createItem("Hindi", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));

        // 4. Mathematics
        list.add(createItem("Mathematics", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));

        // 5. Play, Do, Learn
        list.add(createItem("Play, Do, Learn", String.format(java.util.Locale.US, "%s%02d", p2, activityIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Activities", 100, "#4CAF50"));
        
        // 6. Environmental Studies
        list.add(createItem("Environmental Studies", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        list.add(createItem("Environmental Studies Part 1", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        list.add(createItem("Environmental Studies Part 2", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        
        // 7. Sciences and Social Sciences
        list.add(createItem("Science", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        list.add(createItem("History and Civics", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        list.add(createItem("Geography", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));

        // 8. Activities
        list.add(createItem("Health & Physical Education", String.format(java.util.Locale.US, "%s%02d", p2, activityIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Activities", 100, "#4CAF50"));
        list.add(createItem("Work Experience", String.format(java.util.Locale.US, "%s%02d", p2, activityIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Activities", 100, "#4CAF50"));
        list.add(createItem("Art", String.format(java.util.Locale.US, "%s%02d", p2, activityIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Activities", 100, "#4CAF50"));

        // 9. State Board Specials
        list.add(createItem("Information & Comm. Technology (ICT)", String.format(java.util.Locale.US, "%s%02d", p4, 1), String.format(java.util.Locale.US, "%02d", totalIdx++), "State Board", 100, "#FF9800"));
        list.add(createItem("Water Security & Environment Studies", String.format(java.util.Locale.US, "%s%02d", p4, 2), String.format(java.util.Locale.US, "%02d", totalIdx++), "State Board", 100, "#FF9800"));
        
        return list;
    }
}
