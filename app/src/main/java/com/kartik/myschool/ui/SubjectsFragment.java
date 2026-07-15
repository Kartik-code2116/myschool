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
    private boolean showOnlyClassSubjects = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentSubjectsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());

        setupRecyclerView();
        displayHeaderInfo();
        
        b.fabAddSubject.setOnClickListener(v -> showAddCustomSubjectDialog());
    }

    private void setupRecyclerView() {
        b.rvSubjectsList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SubjectAdapter();
        b.rvSubjectsList.setAdapter(adapter);

        androidx.recyclerview.widget.ItemTouchHelper helper = new androidx.recyclerview.widget.ItemTouchHelper(new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                adapter.swapItems(from, to);
                return true;
            }
            @Override
            public void onSwiped(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int direction) {}
            
            @Override
            public void clearView(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                saveNewSequence();
            }
        });
        helper.attachToRecyclerView(b.rvSubjectsList);

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

        adapter.setOnDeleteSubjectListener(item -> {
            if (SessionContext.selectedClass == null) return;
            ClassModel cls = SessionContext.selectedClass;
            
            new AlertDialog.Builder(getContext())
                .setTitle("Delete Subject")
                .setMessage("Are you sure you want to permanently delete '" + item.name + "' from this class?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (cls.subjects != null) {
                        for (int i = 0; i < cls.subjects.size(); i++) {
                            if (Subject.isSameSubject(cls.subjects.get(i).name, item.name)) {
                                cls.subjects.remove(i);
                                break;
                            }
                        }
                    }
                    Subject.sortSubjects(cls.subjects);
                    SessionContext.save(getContext());
                    com.kartik.myschool.AppCache.selectedClass = cls;
                    
                    FirebaseRepository.get().saveClass(cls, new FirebaseRepository.OnResult<String>() {
                        @Override public void onSuccess(String result) {
                            if (isAdded()) {
                                loadSubjects();
                                Toast.makeText(getContext(), "Subject deleted", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onError(Exception e) {
                            if (isAdded()) Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void saveNewSequence() {
        if (SessionContext.selectedClass == null) return;
        List<SubjectAdapter.SubjectItem> items = adapter.getItems();
        
        for (int i = 0; i < items.size(); i++) {
            SubjectAdapter.SubjectItem item = items.get(i);
            
            String oldCode = item.code != null ? item.code : "";
            String newCode;
            if (oldCode.length() >= 2 && oldCode.matches(".*\\d\\d$")) {
                newCode = oldCode.substring(0, oldCode.length() - 2) + String.format(java.util.Locale.US, "%02d", i + 1);
            } else {
                newCode = oldCode + String.format(java.util.Locale.US, "%02d", i + 1);
            }
            
            String newSerial = String.format(java.util.Locale.US, "%02d", i + 1);
            item.code = newCode;
            item.serial = newSerial;
            
            if (SessionContext.selectedClass.subjects != null) {
                for (Subject active : SessionContext.selectedClass.subjects) {
                    if (Subject.isSameSubject(active.name, item.name)) {
                        active.subjectCode = newCode;
                        break;
                    }
                }
            }
        }
        
        if (SessionContext.selectedClass.subjects != null) {
            Subject.sortSubjects(SessionContext.selectedClass.subjects);
        }
        
        adapter.notifyDataSetChanged();
        
        SessionContext.save(getContext());
        com.kartik.myschool.AppCache.selectedClass = SessionContext.selectedClass;
        
        FirebaseRepository.get().saveClass(SessionContext.selectedClass, new FirebaseRepository.OnResult<String>() {
            @Override public void onSuccess(String result) {}
            @Override public void onError(Exception e) {}
        });
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        boolean isEn = com.kartik.myschool.utils.pdf.PdfLocalizer.isEnglish(getContext());
        b.tvHeaderLabel.setText((isEn ? "Year: " : "वर्ष: ") + yearLabel + " | " + SessionContext.getClassDivSemSubtitle(requireContext()));
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
            
            // Auto-activate true descriptive subjects if missing
            String[] trueDescriptives = {"विशेष प्रगती", "आवड/छंद", "सुधारणा आवश्यक", "व्यक्तिमत्व गुणविशेष"};
            for (String desc : trueDescriptives) {
                boolean found = false;
                for (Subject clean : cleanList) {
                    if (Subject.isSameSubject(clean.name, desc)) {
                        found = true; break;
                    }
                }
                if (!found) {
                    Subject newDesc = new Subject(desc, 0);
                    // Find subject code from predefined list if possible
                    for (SubjectAdapter.SubjectItem item : getPredefinedSubjects()) {
                        if (Subject.isSameSubject(item.name, desc)) {
                            newDesc.subjectCode = item.code;
                            break;
                        }
                    }
                    cleanList.add(newDesc);
                    modified = true;
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
        if (showOnlyClassSubjects) {
            finalPredefined = filterActiveItems(finalPredefined, activeSubjects);
        }
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
                            int fe = gSub.maxNirikhshan + gSub.maxTondiKam + gSub.maxPratyakshik + gSub.maxUpkram + gSub.maxPrakalp + gSub.maxChachani + gSub.maxSwadhyay + gSub.maxItar;
                            int se = gSub.maxTondi + gSub.maxPratyakshikB + gSub.maxLekhi;
                            String seStr = se > 0 ? "SE: " + se : "";
                            currentList.add(new SubjectAdapter.SubjectItem(gSub.name, "", orderStr, "Global", gSub.maxMarks, "FE: " + fe, seStr, "", "#FF5722"));
                        }
                    }
                    List<SubjectAdapter.SubjectItem> finalGlobal = deduplicateItems(currentList);
                    sortSubjectItems(finalGlobal);
                    if (showOnlyClassSubjects) {
                        finalGlobal = filterActiveItems(finalGlobal, activeSubjects);
                    }
                    adapter.setData(finalGlobal, activeSubjects);
                }
            }
            @Override
            public void onError(Exception e) {
                // Ignore, just use predefined
            }
        });
    }

    private List<SubjectAdapter.SubjectItem> filterActiveItems(List<SubjectAdapter.SubjectItem> list, List<Subject> activeSubjects) {
        List<SubjectAdapter.SubjectItem> filtered = new ArrayList<>();
        for (SubjectAdapter.SubjectItem item : list) {
            for (Subject active : activeSubjects) {
                if (Subject.isSameSubject(item.name, active.name)) {
                    filtered.add(item);
                    break;
                }
            }
        }
        return filtered;
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

        int maxSeq = 0;
        String prefix = "";
        if (SessionContext.selectedClass.subjects != null) {
            for (Subject s : SessionContext.selectedClass.subjects) {
                if (s.subjectCode != null && s.subjectCode.length() >= 2) {
                    try {
                        String lastTwo = s.subjectCode.substring(s.subjectCode.length() - 2);
                        int seq = Integer.parseInt(lastTwo);
                        if (seq > maxSeq) {
                            maxSeq = seq;
                            prefix = s.subjectCode.substring(0, s.subjectCode.length() - 2);
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (maxSeq == 0) {
                maxSeq = SessionContext.selectedClass.subjects.size();
                if (!SessionContext.selectedClass.subjects.isEmpty()) {
                    Subject last = SessionContext.selectedClass.subjects.get(SessionContext.selectedClass.subjects.size() - 1);
                    if (last.subjectCode != null && last.subjectCode.length() >= 2) {
                        prefix = last.subjectCode.substring(0, last.subjectCode.length() - 2);
                    }
                }
            }
        }
        
        if (prefix.isEmpty()) {
            int std = 1;
            try { std = Integer.parseInt(SessionContext.selectedClass.className.replaceAll("[^0-9]", "")); } catch (Exception e) {}
            prefix = String.format(java.util.Locale.US, "1%02d1", std);
        }
        
        String nextCode = prefix + String.format(java.util.Locale.US, "%02d", maxSeq + 1);

        android.content.Intent intent = new android.content.Intent(getContext(), com.kartik.myschool.SubjectUpdateActivity.class);
        intent.putExtra("subject_name", ""); // empty name means create new
        intent.putExtra("subject_code", nextCode);
        intent.putExtra("subject_serial", String.format(java.util.Locale.US, "%02d", maxSeq + 1));
        intent.putExtra("subject_category", "Academic");
        intent.putExtra("subject_max_marks", 100);
        intent.putExtra("details_left_1", "FE: 50");
        intent.putExtra("details_left_2", "SE: 50");
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());
        loadSubjects();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) getActivity();
            activity.showCustomToolbarActions(
                    true,
                    v -> com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(activity, "subjects"),
                    v -> {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        popup.getMenu().add(showOnlyClassSubjects ? "Show All Subjects" : "Show Only This Class Subjects");
                        popup.getMenu().add("Reset All to Default");
                        popup.setOnMenuItemClickListener(menuItem -> {
                            String title = menuItem.getTitle().toString();
                            if (title.equals("Show All Subjects") || title.equals("Show Only This Class Subjects")) {
                                showOnlyClassSubjects = !showOnlyClassSubjects;
                                loadSubjects();
                            } else if (title.equals("Reset All to Default")) {
                                new AlertDialog.Builder(getContext())
                                    .setTitle("Reset Subjects")
                                    .setMessage("Are you sure you want to reset subjects to their defaults for this class standard? This will replace your current selection.")
                                    .setPositiveButton("Reset", (d, w) -> resetSubjectsToDefault(false))
                                    .setNegativeButton("Cancel", null)
                                    .show();
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
        String className = cls.className != null ? cls.className : "1";
        
        FirebaseRepository.get().getClassDefaultSubjects(className, new FirebaseRepository.OnResult<List<Subject>>() {
            @Override
            public void onSuccess(List<Subject> subjects) {
                if (subjects != null && !subjects.isEmpty()) {
                    applyResetAndSave(cls, subjects, silent);
                } else {
                    applyHardcodedResetAndSave(cls, className, silent);
                }
            }
            @Override
            public void onError(Exception e) {
                applyHardcodedResetAndSave(cls, className, silent);
            }
        });
    }

    private void applyResetAndSave(ClassModel cls, List<Subject> subjects, boolean silent) {
        cls.subjects = new ArrayList<>(subjects);
        Subject.sortSubjects(cls.subjects);
        SessionContext.save(getContext());
        com.kartik.myschool.AppCache.selectedClass = cls;
        
        FirebaseRepository.get().saveClass(cls, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String result) {
                if (isAdded()) {
                    loadSubjects();
                    if (!silent) Toast.makeText(getContext(), "Subjects reset for Std " + cls.className, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(Exception e) {
                if (isAdded() && !silent) Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyHardcodedResetAndSave(ClassModel cls, String className, boolean silent) {
        int std = 1;
        try {
            String clean = className.replaceAll("[^0-9]", "");
            if (!clean.isEmpty()) std = Integer.parseInt(clean);
        } catch (Exception ignored) {}
        
        List<SubjectAdapter.SubjectItem> predefined = getPredefinedSubjects();
        List<Subject> newSubjects = new ArrayList<>();
        
        List<String> req = new ArrayList<>();
        req.add("मराठी");
        req.add("इंग्रजी");
        req.add("गणित");
        
        if (std == 1 || std == 2) {
            req.add("खेळू, करू, शिकू");
        } else if (std == 3 || std == 4) {
            req.add("परिसर अभ्यास");
            req.add("खेळू, करू, शिकू");
        } else if (std == 5) {
            req.add("हिंदी");
            req.add("परिसर अभ्यास भाग १");
            req.add("परिसर अभ्यास भाग २");
            req.add("आरोग्य व शारीरिक शिक्षण");
            req.add("कार्यानुभव");
            req.add("कला");
        } else {
            req.add("हिंदी");
            req.add("सामान्य विज्ञान");
            req.add("इतिहास व नागरिकशास्त्र");
            req.add("भूगोल");
            req.add("आरोग्य व शारीरिक शिक्षण");
            req.add("कार्यानुभव");
            req.add("कला");
        }
        
        req.add("विशेष प्रगती");
        req.add("आवड/छंद");
        req.add("सुधारणा आवश्यक");
        req.add("व्यक्तिमत्व गुणविशेष");
        
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
        
        applyResetAndSave(cls, newSubjects, silent);
    }

    private SubjectAdapter.SubjectItem createItem(String name, String code, String serial, String category, int maxMarks, String color) {
        Subject s = new Subject(name, maxMarks);
        if (maxMarks == 0 || Subject.isDescriptiveOnly(name)) {
            return new SubjectAdapter.SubjectItem(name, code, serial, category, 0, "Only Descriptive", "", "", color);
        }
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
        list.add(createItem("मराठी", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        
        // 2. Second Language (English)
        list.add(createItem("इंग्रजी", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));

        // 3. Third Language (Hindi)
        list.add(createItem("हिंदी", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));

        // 4. Mathematics
        list.add(createItem("गणित", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));

        // 5. Play, Do, Learn
        list.add(createItem("खेळू, करू, शिकू", String.format(java.util.Locale.US, "%s%02d", p2, activityIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Activities", 100, "#4CAF50"));
        
        // 6. Environmental Studies
        list.add(createItem("परिसर अभ्यास", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        list.add(createItem("परिसर अभ्यास भाग १", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        list.add(createItem("परिसर अभ्यास भाग २", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        
        // 7. Sciences and Social Sciences
        list.add(createItem("सामान्य विज्ञान", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        list.add(createItem("इतिहास व नागरिकशास्त्र", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));
        list.add(createItem("भूगोल", String.format(java.util.Locale.US, "%s%02d", p1, academicIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Academic", 100, "#2196F3"));

        // 8. Activities
        list.add(createItem("आरोग्य व शारीरिक शिक्षण", String.format(java.util.Locale.US, "%s%02d", p2, activityIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Activities", 100, "#4CAF50"));
        list.add(createItem("कार्यानुभव", String.format(java.util.Locale.US, "%s%02d", p2, activityIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Activities", 100, "#4CAF50"));
        list.add(createItem("कला", String.format(java.util.Locale.US, "%s%02d", p2, activityIdx++), String.format(java.util.Locale.US, "%02d", totalIdx++), "Activities", 100, "#4CAF50"));

        // 9. State Board Specials
        list.add(createItem("माहिती व संप्रेषण तंत्रज्ञान (ICT)", String.format(java.util.Locale.US, "%s%02d", p4, 1), String.format(java.util.Locale.US, "%02d", totalIdx++), "State Board", 100, "#FF9800"));
        list.add(createItem("जलसुरक्षा व पर्यावरण अभ्यास", String.format(java.util.Locale.US, "%s%02d", p4, 2), String.format(java.util.Locale.US, "%02d", totalIdx++), "State Board", 100, "#FF9800"));
        
        // 10. True Descriptive Entries
        list.add(createItem("विशेष प्रगती", String.format(java.util.Locale.US, "%s%02d", p3, 1), String.format(java.util.Locale.US, "%02d", totalIdx++), "Personality", 0, "#ff9800"));
        list.add(createItem("आवड/छंद", String.format(java.util.Locale.US, "%s%02d", p3, 2), String.format(java.util.Locale.US, "%02d", totalIdx++), "Personality", 0, "#ff9800"));
        list.add(createItem("सुधारणा आवश्यक", String.format(java.util.Locale.US, "%s%02d", p3, 3), String.format(java.util.Locale.US, "%02d", totalIdx++), "Personality", 0, "#ff9800"));
        list.add(createItem("व्यक्तिमत्व गुणविशेष", String.format(java.util.Locale.US, "%s%02d", p3, 4), String.format(java.util.Locale.US, "%02d", totalIdx++), "Personality", 0, "#ff9800"));
        
        return list;
    }
}
