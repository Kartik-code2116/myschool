package com.kartik.myschool.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.AppCache;
import com.kartik.myschool.EnterDescriptiveActivity;
import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.SubjectRemarkEntryDialog;
import com.kartik.myschool.databinding.FragmentDescriptiveEntriesBinding;
import com.kartik.myschool.databinding.ItemDescriptiveStudentBlockBinding;
import com.kartik.myschool.databinding.ItemDescriptiveSubjectCardBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DescriptiveEntriesFragment extends Fragment {

    private FragmentDescriptiveEntriesBinding b;
    private DescriptiveAdapter adapter;
    private ClassModel activeClass;
    private String activeSemesterId = "sem_1";
    private int activeSemesterNumber = 1;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private boolean isGridViewMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        b = FragmentDescriptiveEntriesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activeClass = SessionContext.selectedClass;
        if (SessionContext.selectedSemester != null && SessionContext.selectedSemester.id != null) {
            activeSemesterId = SessionContext.selectedSemester.id;
            activeSemesterNumber = SessionContext.selectedSemester.number;
        }

        setupCustomAppBar();
        setupHeaderStrip();

        swipeRefresh = b.swipeRefreshLayout;
        swipeRefresh.setColorSchemeColors(0xFF6C4CCF, 0xFF9C27B0, 0xFF00A5CF);
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFFFFFFFF);
        swipeRefresh.setOnRefreshListener(() -> {
            AppCache.cachedDescriptiveStudents = null;
            AppCache.cachedDescriptiveMarksMap = null;
            AppCache.cachedDescriptiveClassId = null;
            AppCache.cachedDescriptiveSemesterId = null;
            AppCache.cachedDescriptiveMarksComplete = false;
            FirebaseRepository.get().clearMarksCache();
            loadDescriptiveData();
        });

        b.rvDescriptiveStudents.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DescriptiveAdapter();
        b.rvDescriptiveStudents.setAdapter(adapter);
    }

    private void setupCustomAppBar() {
        // Drawer Menu Button Click
        b.btnDrawerMenu.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).openDrawer();
            }
        });

        // 3-Dot Toolbar Menu
        b.btnThreeDotMenu.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).showHomeMoreMenu(v);
            }
        });

        // Subtitle dynamic binding
        String clsLabel = activeClass != null ? activeClass.className : "5";
        String divLabel = activeClass != null ? activeClass.division : "1";
        b.tvAppSubtitle
                .setText("Cls: " + clsLabel + "-" + divLabel + " • Sem: " + activeSemesterNumber);

        // Outlined button click actions
        b.btnHelpSquare.setOnClickListener(
                v -> Toast.makeText(requireContext(), R.string.msg_descriptive_entries_manual_ope, Toast.LENGTH_SHORT)
                        .show());
        b.btnAddSquare.setOnClickListener(
                v -> Toast.makeText(requireContext(), R.string.msg_add_student_clicked, Toast.LENGTH_SHORT).show());
    }

    private void setupHeaderStrip() {
        String yr = SessionContext.selectedYear != null ? SessionContext.selectedYear.label : "2025-26";
        String cls = activeClass != null ? activeClass.className : "5";
        String div = activeClass != null ? activeClass.division : "1";
        b.tvHeaderStripInfo
                .setText("Year: " + yr + " | Cls: " + cls + "-" + div + " | Sem: " + activeSemesterNumber);

        // Set initial icon (show grid icon when in slide mode, show list/bullet icon
        // when in grid mode)
        b.btnGridListToggle.setImageResource(isGridViewMode ? R.drawable.ic_list_bullet : R.drawable.ic_table_grid);

        b.btnGridListToggle.setOnClickListener(v -> {
            isGridViewMode = !isGridViewMode;
            b.btnGridListToggle.setImageResource(isGridViewMode ? R.drawable.ic_list_bullet : R.drawable.ic_table_grid);
            Toast.makeText(requireContext(), isGridViewMode ? "Grid View Enabled" : "Slide View Enabled",
                    Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
        });
    }

    private void loadDescriptiveData() {
        if (activeClass == null) {
            Toast.makeText(requireContext(), R.string.select_class_first, Toast.LENGTH_LONG).show();
            return;
        }

        // Initialize subjects list if null
        if (activeClass.subjects == null) {
            activeClass.subjects = new ArrayList<>();
        }

        // 1. Instant Cache rendering (zero-latency display):
        if (AppCache.cachedDescriptiveStudents != null
                && AppCache.cachedDescriptiveMarksComplete
                && java.util.Objects.equals(activeClass.id, AppCache.cachedDescriptiveClassId)
                && java.util.Objects.equals(activeSemesterId, AppCache.cachedDescriptiveSemesterId)) {
            List<Student> cachedList = AppCache.cachedDescriptiveStudents;
            Map<String, MarksRecord> cachedMarks = AppCache.cachedDescriptiveMarksMap != null
                    ? AppCache.cachedDescriptiveMarksMap
                    : new HashMap<>();

            // Render instantly!
            adapter.setData(cachedList, cachedMarks);
            if (swipeRefresh != null)
                swipeRefresh.setRefreshing(false);
        }

        // ★ FIX: Do NOT clear repo marks cache here.
        // Clearing on every resume wipes data that was just saved or loaded,
        // causing the fragment to always show stale/empty data until Firestore returns.
        // The cache is cleared selectively (by EnterDescriptiveActivity on save,
        // or by the swipe-to-refresh gesture).

        // 2. Fetch from network in background (stale-while-revalidate):
        FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                if (students == null)
                    students = new ArrayList<>();
                List<Student> finalList = students;

                FirebaseRepository.get().getMarksForClassAndSemester(activeClass.id, activeSemesterId,
                        new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                            @Override
                            public void onSuccess(Map<String, MarksRecord> marksMap) {
                                Map<String, MarksRecord> finalMarks = marksMap != null ? marksMap : new HashMap<>();

                                // Merge network results with the fresh local states based on updatedAt
                                if (adapter != null && adapter.marksMap != null) {
                                    for (Map.Entry<String, MarksRecord> entry : adapter.marksMap.entrySet()) {
                                        String sId = entry.getKey();
                                        MarksRecord adapterRecord = entry.getValue();
                                        MarksRecord fetchedRecord = finalMarks.get(sId);
                                        if (adapterRecord != null && (fetchedRecord == null
                                                || adapterRecord.updatedAt >= fetchedRecord.updatedAt)) {
                                            finalMarks.put(sId, adapterRecord);
                                        }
                                    }
                                }

                                if (AppCache.cachedMarksMap != null
                                        && java.util.Objects.equals(activeClass.id, AppCache.cachedClassIdForStudents)
                                        && java.util.Objects.equals(activeSemesterId,
                                                AppCache.cachedSemesterIdForMarks)) {
                                    for (Map.Entry<String, MarksRecord> entry : AppCache.cachedMarksMap.entrySet()) {
                                        String sId = entry.getKey();
                                        MarksRecord cachedRecord = entry.getValue();
                                        MarksRecord fetchedRecord = finalMarks.get(sId);
                                        if (cachedRecord != null && (fetchedRecord == null
                                                || cachedRecord.updatedAt >= fetchedRecord.updatedAt)) {
                                            finalMarks.put(sId, cachedRecord);
                                        }
                                    }
                                }

                                if (AppCache.cachedDescriptiveMarksMap != null
                                        && AppCache.cachedDescriptiveMarksComplete
                                        && java.util.Objects.equals(activeClass.id, AppCache.cachedDescriptiveClassId)
                                        && java.util.Objects.equals(activeSemesterId,
                                                AppCache.cachedDescriptiveSemesterId)) {
                                    for (Map.Entry<String, MarksRecord> entry : AppCache.cachedDescriptiveMarksMap
                                            .entrySet()) {
                                        String sId = entry.getKey();
                                        MarksRecord cachedRecord = entry.getValue();
                                        MarksRecord fetchedRecord = finalMarks.get(sId);
                                        if (cachedRecord != null && (fetchedRecord == null
                                                || cachedRecord.updatedAt >= fetchedRecord.updatedAt)) {
                                            finalMarks.put(sId, cachedRecord);
                                        }
                                    }
                                }

                                android.util.Log.d("DESCRIPTIVE_DEBUG", "Loaded finalMarks size=" + finalMarks.size()
                                        + " for classId=" + activeClass.id + " sem=" + activeSemesterId);
                                for (Map.Entry<String, MarksRecord> entry : finalMarks.entrySet()) {
                                    MarksRecord rec = entry.getValue();
                                    if (rec != null && rec.detailedMarks != null) {
                                        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> d : rec.detailedMarks
                                                .entrySet()) {
                                            if (d.getValue() != null && d.getValue().remark != null
                                                    && !d.getValue().remark.isEmpty()) {
                                                android.util.Log.d("DESCRIPTIVE_DEBUG", "  student=" + entry.getKey()
                                                        + " subject=" + d.getKey() + " remark=" + d.getValue().remark);
                                            }
                                        }
                                    }
                                }

                                // Cache the loaded results
                                AppCache.cachedDescriptiveStudents = finalList;
                                AppCache.cachedDescriptiveMarksMap = finalMarks;
                                AppCache.cachedDescriptiveClassId = activeClass.id;
                                AppCache.cachedDescriptiveSemesterId = activeSemesterId;
                                AppCache.cachedDescriptiveMarksComplete = true;

                                if (isAdded() && b != null) {
                                    adapter.setData(finalList, finalMarks);
                                    if (swipeRefresh != null)
                                        swipeRefresh.setRefreshing(false);
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                if (isAdded() && b != null) {
                                    if (swipeRefresh != null)
                                        swipeRefresh.setRefreshing(false);
                                    if (AppCache.cachedDescriptiveStudents == null || !java.util.Objects
                                            .equals(activeClass.id, AppCache.cachedDescriptiveClassId)) {
                                        adapter.setData(finalList, new HashMap<>());
                                    }
                                }
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    if (swipeRefresh != null)
                        swipeRefresh.setRefreshing(false);
                    if (AppCache.cachedDescriptiveStudents == null
                            || !java.util.Objects.equals(activeClass.id, AppCache.cachedDescriptiveClassId)) {
                        Toast.makeText(requireContext(), "Failed to load students: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (SessionContext.selectedClass != null) {
            activeClass = SessionContext.selectedClass;
        }

        if (SessionContext.selectedSemester == null && activeClass != null
                && activeClass.semesterId != null && !activeClass.semesterId.isEmpty()) {
            com.kartik.myschool.model.Semester fallbackSem = new com.kartik.myschool.model.Semester();
            fallbackSem.id = activeClass.semesterId;
            fallbackSem.yearId = activeClass.yearId;
            fallbackSem.number = activeClass.semesterId.contains("2") ? 2 : 1;
            fallbackSem.name = activeClass.semesterId.contains("2") ? "Second Semester" : "First Semester";
            SessionContext.selectedSemester = fallbackSem;
        }

        if (SessionContext.selectedSemester != null && SessionContext.selectedSemester.id != null) {
            activeSemesterId = SessionContext.selectedSemester.id;
            activeSemesterNumber = SessionContext.selectedSemester.number;
        }

        setupCustomAppBar();
        setupHeaderStrip();

        if (getActivity() instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) getActivity();
            View activityAppBar = activity.findViewById(R.id.appBarLayout);
            if (activityAppBar != null)
                activityAppBar.setVisibility(View.GONE);

            View navHost = activity.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost
                    .getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost
                        .getLayoutParams();
                params.setBehavior(null);
                float density = getResources().getDisplayMetrics().density;
                params.bottomMargin = (int) (64 * density);
                navHost.setLayoutParams(params);
            }
        }

        // FIX: If remarks were just saved in EnterDescriptiveActivity:
        // 1. Instantly patch only that student's card — 0ms, no network
        // 2. Then do a full Firestore reload in background to confirm
        if (AppCache.descriptiveJustSaved
                && AppCache.descriptiveJustSavedStudentId != null
                && AppCache.descriptiveJustSavedRecord != null) {

            android.util.Log.d("DESCRIPTIVE", "descriptiveJustSaved=true for student="
                    + AppCache.descriptiveJustSavedStudentId + " — patching adapter instantly");

            // Instant patch: update only the one card that changed
            adapter.patchStudentMarks(
                    AppCache.descriptiveJustSavedStudentId,
                    AppCache.descriptiveJustSavedRecord);

            // Consume the flags so next unrelated resume doesn’t re-patch
            AppCache.descriptiveJustSaved = false;
            AppCache.descriptiveJustSavedStudentId = null;
            AppCache.descriptiveJustSavedRecord = null;

            // CRITICAL FIX: Clear selectedMarks so it doesn't bleed into
            // getDisplayMarksForStudent() for OTHER students in the list.
            AppCache.selectedMarks = null;
        }

        loadDescriptiveData();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) getActivity();
            View activityAppBar = activity.findViewById(R.id.appBarLayout);
            if (activityAppBar != null) {
                activityAppBar.setVisibility(View.VISIBLE);
            }

            // Restore CoordinatorLayout scrolling behavior:
            View navHost = activity.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost
                    .getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost
                        .getLayoutParams();
                params.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
                float density = getResources().getDisplayMetrics().density;
                params.bottomMargin = (int) (64 * density);
                navHost.setLayoutParams(params);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // RECYCLERVIEW ADAPTER
    // ════════════════════════════════════════════════════════════════════════════
    private class DescriptiveAdapter extends RecyclerView.Adapter<DescriptiveAdapter.ViewHolder> {

        private final List<Student> students = new ArrayList<>();
        private final Map<String, MarksRecord> marksMap = new HashMap<>();

        public void setData(List<Student> list, Map<String, MarksRecord> map) {
            students.clear();
            students.addAll(list);
            marksMap.clear();
            marksMap.putAll(map);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> notifyDataSetChanged());
        }

        /**
         * Instantly updates one student's card without a full list reload.
         * Called by onResume() immediately after returning from
         * EnterDescriptiveActivity.
         */
        public void patchStudentMarks(String studentId, MarksRecord record) {
            // Update the adapter's marks map
            marksMap.put(studentId, record);
            // Also update the descriptive AppCache so future renders are correct
            if (AppCache.cachedDescriptiveMarksMap == null
                    || !java.util.Objects.equals(activeClass.id, AppCache.cachedDescriptiveClassId)
                    || !java.util.Objects.equals(activeSemesterId, AppCache.cachedDescriptiveSemesterId)) {
                AppCache.cachedDescriptiveMarksMap = new HashMap<>();
                AppCache.cachedDescriptiveClassId = activeClass.id;
                AppCache.cachedDescriptiveSemesterId = activeSemesterId;
                AppCache.cachedDescriptiveMarksComplete = true;
            }
            AppCache.cachedDescriptiveMarksMap.put(studentId, record);
            // Notify only the changed item for a smooth, flicker-free update
            for (int i = 0; i < students.size(); i++) {
                if (java.util.Objects.equals(studentId, students.get(i).id)) {
                    final int pos = i;
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .post(() -> notifyItemChanged(pos));
                    android.util.Log.d("DESCRIPTIVE",
                            "patchStudentMarks: notified pos=" + pos + " student=" + studentId);
                    return;
                }
            }
            // Student not in list yet — do a full refresh
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> notifyDataSetChanged());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDescriptiveStudentBlockBinding binding = ItemDescriptiveStudentBlockBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Student st = students.get(position);
            holder.bind(st, position + 1);
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemDescriptiveStudentBlockBinding binding;

            public ViewHolder(ItemDescriptiveStudentBlockBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Student s, int index) {
                binding.tvStudentName.setText(index + ". " + s.name);
                binding.btnStudentMore.setOnClickListener(v -> showStudentRemarkMenu(v, s));

                MarksRecord marks = getDisplayMarksForStudent(s);
                // User requested to not show remarks twice, only in the subject boxes
                // renderSavedRemarkSummary(marks);

                if (isGridViewMode) {
                    // Show 2-Column Grid, Hide Horizontal Scroll
                    binding.layoutSubjectsScroll.setVisibility(View.GONE);
                    binding.layoutSubjectsGridContainer.setVisibility(View.VISIBLE);
                    binding.layoutSubjectsGridContainer.removeAllViews();

                    if (activeClass.subjects != null) {
                        LinearLayout currentRow = null;
                        for (int i = 0; i < activeClass.subjects.size(); i++) {
                            if (i % 2 == 0) {
                                currentRow = new LinearLayout(itemView.getContext());
                                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                                currentRow.setWeightSum(2f);
                                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                                currentRow.setLayoutParams(rowParams);
                                binding.layoutSubjectsGridContainer.addView(currentRow);
                            }

                            Subject sub = activeClass.subjects.get(i);
                            View cardView = createSubjectCard(s, sub, i + 1, marks);

                            // Set layout params for 2-column grid item
                            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f);

                            float density = itemView.getResources().getDisplayMetrics().density;
                            int margin = (int) (3 * density);
                            param.setMargins(margin, margin, margin, margin);
                            cardView.setLayoutParams(param);

                            if (currentRow != null) {
                                currentRow.addView(cardView);
                            }
                        }

                        // If odd number of subjects, add an empty placeholder view to balance the last
                        // row
                        if (activeClass.subjects.size() % 2 != 0 && currentRow != null) {
                            View placeholder = new View(itemView.getContext());
                            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f);
                            placeholder.setLayoutParams(param);
                            currentRow.addView(placeholder);
                        }
                    }
                } else {
                    // Show Horizontal Scroll (Slide Layout), Hide Grid
                    binding.layoutSubjectsGridContainer.setVisibility(View.GONE);
                    binding.layoutSubjectsScroll.setVisibility(View.VISIBLE);
                    binding.layoutSubjectsHorizontal.removeAllViews();

                    if (activeClass.subjects != null) {
                        for (int i = 0; i < activeClass.subjects.size(); i++) {
                            Subject sub = activeClass.subjects.get(i);
                            View cardView = createSubjectCard(s, sub, i + 1, marks);
                            binding.layoutSubjectsHorizontal.addView(cardView);
                        }
                    }
                }
            }

            private void showStudentRemarkMenu(View anchor, Student student) {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(
                        itemView.getContext(), anchor);
                popup.getMenu().add(0, 1, 0, "Edit remark");
                popup.getMenu().add(0, 2, 1, "Delete remark");
                popup.getMenu().add(0, 3, 2, "Gender change sentence");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        openMarksEntry(student);
                        return true;
                    }
                    if (item.getItemId() == 2) {
                        confirmDeleteRemarks(student);
                        return true;
                    }
                    applyGenderRemarkChange(student);
                    return true;
                });
                popup.show();
            }

            private void confirmDeleteRemarks(Student student) {
                new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext())
                        .setTitle(R.string.msg_delete_remarks)
                        .setMessage("This will remove saved descriptive remarks for this student only.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> deleteRemarks(student))
                        .show();
            }

            private void deleteRemarks(Student student) {
                MarksRecord record = getDisplayMarksForStudent(student);
                if (record == null || record.detailedMarks == null || record.detailedMarks.isEmpty()) {
                    Toast.makeText(itemView.getContext(), R.string.msg_no_saved_remarks_found, Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                boolean changed = false;
                for (MarksRecord.SubjectMarksDetail detail : record.detailedMarks.values()) {
                    if (detail != null && detail.remark != null && !detail.remark.trim().isEmpty()) {
                        detail.remark = "";
                        changed = true;
                    }
                }

                if (!changed) {
                    Toast.makeText(itemView.getContext(), R.string.msg_no_saved_remarks_found, Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                saveStudentRemarkRecord(student, record, "Remarks deleted.");
            }

            private void applyGenderRemarkChange(Student student) {
                MarksRecord record = getDisplayMarksForStudent(student);
                if (record == null || record.detailedMarks == null || record.detailedMarks.isEmpty()) {
                    Toast.makeText(itemView.getContext(), R.string.msg_no_saved_remarks_found, Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                boolean female = isFemaleStudent(student);
                boolean changed = false;
                for (MarksRecord.SubjectMarksDetail detail : record.detailedMarks.values()) {
                    if (detail == null || detail.remark == null || detail.remark.trim().isEmpty()) {
                        continue;
                    }
                    String updated = adjustRemarkGender(detail.remark, female);
                    if (!updated.equals(detail.remark)) {
                        detail.remark = updated;
                        changed = true;
                    }
                }

                if (!changed) {
                    Toast.makeText(itemView.getContext(), R.string.msg_no_gender_words_found_to_chang,
                            Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                saveStudentRemarkRecord(student, record, "Gender sentence updated.");
            }

            private boolean isFemaleStudent(Student student) {
                String gender = student != null && student.gender != null
                        ? student.gender.trim().toLowerCase(java.util.Locale.ROOT)
                        : "";
                return gender.contains("female")
                        || gender.contains("girl")
                        || gender.contains("\u092e\u0941\u0932\u0917\u0940")
                        || gender.contains("\u0938\u094d\u0924\u094d\u0930\u0940");
            }

            private String adjustRemarkGender(String remark, boolean female) {
                String result = remark;
                String[][] femalePairs = {
                        { "\u092f\u0947\u0924\u094b", "\u092f\u0947\u0924\u0947" },
                        { "\u0935\u093e\u0917\u0924\u094b", "\u0935\u093e\u0917\u0924\u0947" },
                        { "\u0915\u0930\u0924\u094b", "\u0915\u0930\u0924\u0947" },
                        { "\u0918\u0947\u0924\u094b", "\u0918\u0947\u0924\u0947" },
                        { "\u0926\u0947\u0924\u094b", "\u0926\u0947\u0924\u0947" },
                        { "\u092c\u094b\u0932\u0924\u094b", "\u092c\u094b\u0932\u0924\u0947" },
                        { "\u0905\u0938\u0924\u094b", "\u0905\u0938\u0924\u0947" }
                };
                for (String[] pair : femalePairs) {
                    result = female ? result.replace(pair[0], pair[1]) : result.replace(pair[1], pair[0]);
                }
                return result;
            }

            private void saveStudentRemarkRecord(Student student, MarksRecord record, String successMessage) {
                record.studentId = student.id;
                if (activeClass != null) {
                    record.classId = activeClass.id;
                    record.examName = activeClass.examName;
                }
                record.semesterId = activeSemesterId;
                record.semesterNumber = String.valueOf(activeSemesterNumber);

                FirebaseRepository.get().saveMarks(record, new FirebaseRepository.OnResult<String>() {
                    @Override
                    public void onSuccess(String id) {
                        record.id = id;
                        AppCache.selectedMarks = null;
                        patchStudentMarks(student.id, record);
                        FirebaseRepository.get().updateMarksInCache(record.classId, record.semesterId, student.id,
                                record);
                        Toast.makeText(itemView.getContext(), successMessage, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(itemView.getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }

            private View createSubjectCard(Student student, Subject sub, int number, MarksRecord record) {
                ItemDescriptiveSubjectCardBinding cardB = ItemDescriptiveSubjectCardBinding.inflate(
                        LayoutInflater.from(itemView.getContext()),
                        isGridViewMode ? binding.layoutSubjectsGridContainer : binding.layoutSubjectsHorizontal,
                        false);

                cardB.tvSubjectName.setText(number + ". " + sub.name);

                List<String> remarks = new ArrayList<>();
                MarksRecord.SubjectMarksDetail detail = getSubjectDetail(record, sub, number - 1);
                if (detail != null && detail.remark != null && !detail.remark.trim().isEmpty()) {
                    String[] parts = detail.remark.trim().split("\\|\\|");
                    for (String p : parts) {
                        String trimmed = p.trim();
                        if (!trimmed.isEmpty()) {
                            remarks.add(trimmed);
                        }
                    }
                }

                com.google.android.material.card.MaterialCardView cardRoot = (com.google.android.material.card.MaterialCardView) cardB
                        .getRoot();

                if (!remarks.isEmpty()) {
                    // ── Has remarks → show ALL chips, hide empty state ────────────────
                    cardB.cgRemarkChips.setVisibility(View.VISIBLE);
                    cardB.layoutEmptyRemark.setVisibility(View.GONE);
                    cardB.cgRemarkChips.removeAllViews();

                    StringBuilder sb = new StringBuilder();
                    for (int r = 0; r < remarks.size(); r++) {
                        if (r > 0)
                            sb.append("\n");
                        sb.append("• ").append(remarks.get(r));
                    }

                    TextView tv = new TextView(itemView.getContext());
                    tv.setText(sb.toString());
                    tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f);
                    tv.setTextColor(0xFF455A64); // Dark greyish blue
                    tv.setLineSpacing(0f, 1.15f);

                    cardB.cgRemarkChips.addView(tv);

                    // Green border = filled
                    cardRoot.setStrokeColor(0xFF81C784);
                } else {
                    // ── No remarks → show empty state ──────────────────────
                    cardB.cgRemarkChips.setVisibility(View.GONE);
                    cardB.layoutEmptyRemark.setVisibility(View.VISIBLE);
                    // Orange border signals needs-entry
                    cardRoot.setStrokeColor(0xFFFFB74D);
                }

                // Tap anywhere on card -> open this subject's remark screen
                cardB.getRoot().setOnClickListener(v -> openSubjectRemarkEntry(student, sub, number - 1));

                // Layout params configured depending on active mode (Grid mode has weight,
                // Slide mode has fixed 240dp width)
                float density = itemView.getResources().getDisplayMetrics().density;
                android.widget.LinearLayout.LayoutParams param;
                if (isGridViewMode) {
                    param = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    int margin = (int) (3 * density);
                    param.setMargins(margin, margin, margin, margin);
                } else {
                    param = new android.widget.LinearLayout.LayoutParams(
                            (int) (240 * density),
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    int margin = (int) (4 * density);
                    param.setMargins(margin, margin, margin, margin);
                }
                cardB.getRoot().setLayoutParams(param);

                return cardB.getRoot();
            }

            private void renderSavedRemarkSummary(MarksRecord record) {
                binding.cgSavedRemarkSummary.removeAllViews();
                java.util.List<String> remarks = getAllSavedRemarksForBlock(record);
                if (remarks.isEmpty()) {
                    binding.cgSavedRemarkSummary.setVisibility(View.GONE);
                    return;
                }

                binding.cgSavedRemarkSummary.setVisibility(View.VISIBLE);
                float density = itemView.getResources().getDisplayMetrics().density;
                for (String remark : remarks) {
                    com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(
                            itemView.getContext());
                    chip.setText(remark);
                    chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f);
                    chip.setTextColor(0xFF2E7D32);
                    chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
                    chip.setChipStrokeWidth(1 * density);
                    chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0xFF81C784));
                    chip.setEnsureMinTouchTargetSize(false);
                    chip.setChipMinHeight((int) (24 * density));

                    binding.cgSavedRemarkSummary.addView(chip);
                }
            }

            private java.util.List<String> getAllSavedRemarksForBlock(MarksRecord record) {
                java.util.LinkedHashSet<String> remarks = new java.util.LinkedHashSet<>();
                if (record == null || record.detailedMarks == null || record.detailedMarks.isEmpty()) {
                    return new ArrayList<>();
                }

                if (activeClass != null && activeClass.subjects != null) {
                    for (int i = 0; i < activeClass.subjects.size(); i++) {
                        Subject subject = activeClass.subjects.get(i);
                        MarksRecord.SubjectMarksDetail detail = getSubjectDetail(record, subject, i);
                        addRemarkParts(remarks, detail);
                    }
                }

                for (MarksRecord.SubjectMarksDetail detail : record.detailedMarks.values()) {
                    addRemarkParts(remarks, detail);
                }

                return new ArrayList<>(remarks);
            }

            private void addRemarkParts(java.util.LinkedHashSet<String> remarks,
                    MarksRecord.SubjectMarksDetail detail) {
                if (detail == null || detail.remark == null || detail.remark.trim().isEmpty()) {
                    return;
                }
                String[] parts = detail.remark.trim().split("\\|\\|");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        remarks.add(trimmed);
                    }
                }
            }

            private MarksRecord.SubjectMarksDetail getSubjectDetail(MarksRecord record, Subject sub, int subjectIndex) {
                if (record == null || record.detailedMarks == null || sub == null || sub.name == null) {
                    return null;
                }

                String safeKey = MarksRecord.sanitizeKey(sub.name);
                MarksRecord.SubjectMarksDetail detail = record.detailedMarks.get(safeKey);
                if (detail != null) {
                    return detail;
                }

                detail = record.detailedMarks.get(sub.name);
                if (detail != null) {
                    return detail;
                }

                for (Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : record.detailedMarks.entrySet()) {
                    String key = entry.getKey();
                    if (key != null && MarksRecord.sanitizeKey(key).equals(safeKey)) {
                        return entry.getValue();
                    }
                }

                if (subjectIndex >= 0 && subjectIndex < record.detailedMarks.size()) {
                    int i = 0;
                    for (MarksRecord.SubjectMarksDetail value : record.detailedMarks.values()) {
                        if (i == subjectIndex) {
                            return value;
                        }
                        i++;
                    }
                }
                return null;
            }

            private MarksRecord getDisplayMarksForStudent(Student student) {
                // Primary source: adapter's marksMap (always keyed by studentId)
                MarksRecord record = marksMap.get(student.id);
                MarksRecord cachedRecord = null;
                if (AppCache.cachedMarksMap != null
                        && java.util.Objects.equals(activeClass != null ? activeClass.id : null,
                                AppCache.cachedClassIdForStudents)
                        && java.util.Objects.equals(activeSemesterId, AppCache.cachedSemesterIdForMarks)) {
                    cachedRecord = AppCache.cachedMarksMap.get(student.id);
                }
                if (cachedRecord != null && (record == null || cachedRecord.updatedAt >= record.updatedAt)) {
                    record = cachedRecord;
                }

                MarksRecord descriptiveCachedRecord = null;
                if (AppCache.cachedDescriptiveMarksMap != null
                        && java.util.Objects.equals(activeClass != null ? activeClass.id : null,
                                AppCache.cachedDescriptiveClassId)
                        && java.util.Objects.equals(activeSemesterId, AppCache.cachedDescriptiveSemesterId)) {
                    descriptiveCachedRecord = AppCache.cachedDescriptiveMarksMap.get(student.id);
                }
                if (descriptiveCachedRecord != null
                        && (record == null || descriptiveCachedRecord.updatedAt >= record.updatedAt)) {
                    record = descriptiveCachedRecord;
                }

                // Secondary: AppCache.selectedMarks — ONLY use if it belongs to THIS student
                MarksRecord selectedRecord = AppCache.selectedMarks;
                if (selectedRecord == null
                        || student.id == null
                        || !java.util.Objects.equals(student.id, selectedRecord.studentId)) {
                    // selectedRecord is null or belongs to a DIFFERENT student — ignore it
                    return record;
                }
                if (activeClass == null
                        || activeClass.id == null
                        || !java.util.Objects.equals(activeClass.id, selectedRecord.classId)) {
                    return record;
                }
                if (selectedRecord.semesterId != null
                        && activeSemesterId != null
                        && !selectedRecord.semesterId.isEmpty()
                        && !java.util.Objects.equals(activeSemesterId, selectedRecord.semesterId)) {
                    return record;
                }
                // selectedRecord belongs to this student; use whichever is newer
                if (record == null || selectedRecord.updatedAt >= record.updatedAt) {
                    return selectedRecord;
                }
                return record;
            }

            private boolean hasEnteredMarks(MarksRecord.SubjectMarksDetail detail) {
                return detail.nirikhshan > 0
                        || detail.tondiKam > 0
                        || detail.pratyakshik > 0
                        || detail.upkram > 0
                        || detail.prakalp > 0
                        || detail.chachani > 0
                        || detail.swadhyay > 0
                        || detail.itar > 0
                        || detail.akarikTotal > 0
                        || detail.tondi > 0
                        || detail.pratyakshikB > 0
                        || detail.lekhi > 0
                        || detail.sanklit > 0
                        || detail.grandTotal > 0;
            }

            private void openMarksEntry(Student student) {
                AppCache.selectedStudent = student;
                // FIX: Always use SessionContext.selectedClass for the freshest subjects.
                ClassModel freshClass = SessionContext.selectedClass != null
                        ? SessionContext.selectedClass
                        : activeClass;
                AppCache.selectedClass = freshClass;
                activeClass = freshClass;
                // FIX: Pass the existing marks record from the adapter's marksMap.
                // This ensures EnterDescriptiveActivity loads the correct existing doc
                // (with proper id) so it UPDATE rather than INSERT a new document.
                MarksRecord existingRecord = marksMap.get(student.id);
                AppCache.selectedMarks = existingRecord;
                android.util.Log.d("DESCRIPTIVE", "openMarksEntry: student=" + student.id
                        + " existingRecord=" + (existingRecord != null ? existingRecord.id : "null"));
                Intent intent = new Intent(itemView.getContext(), EnterDescriptiveActivity.class);
                itemView.getContext().startActivity(intent);
            }

            private void openSubjectRemarkEntry(Student student, Subject subject, int subjectIndex) {
                ClassModel freshClass = SessionContext.selectedClass != null
                        ? SessionContext.selectedClass
                        : activeClass;
                activeClass = freshClass;
                MarksRecord record = getDisplayMarksForStudent(student);

                SubjectRemarkEntryDialog dialog = SubjectRemarkEntryDialog.newInstance(
                        student, subject, subjectIndex, freshClass, record);
                dialog.setOnRemarkSavedListener((studentId, newRecord) -> {
                    adapter.patchStudentMarks(studentId, newRecord);
                });
                dialog.show(DescriptiveEntriesFragment.this.getChildFragmentManager(), "SubjectRemarkEntryDialog");
            }
        }
    }
}
