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
    private String lastLoadedClassId = null;
    private String lastLoadedSemesterId = null;

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
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());

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
            AppCache.cachedRemarkBank.clear();
            FirebaseRepository.get().clearMarksCache();
            loadDescriptiveData();
        });

        b.rvDescriptiveStudents.setLayoutManager(new LinearLayoutManager(requireContext()));
        RecyclerView.RecycledViewPool pool = null;
        if (getActivity() instanceof HomeActivity) {
            pool = ((HomeActivity) getActivity()).sharedPool;
        }
        adapter = new DescriptiveAdapter(pool);
        b.rvDescriptiveStudents.setAdapter(adapter);
        b.rvDescriptiveStudents.setItemViewCacheSize(10);
        b.rvDescriptiveStudents.setHasFixedSize(true);
        b.rvDescriptiveStudents.getRecycledViewPool().setMaxRecycledViews(0, 20);
        b.rvDescriptiveStudents.setItemAnimator(null);

        // Play smooth layout enter animation
        com.kartik.myschool.utils.UiAnimations.playLayoutEnter(b.getRoot());
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
        b.tvAppSubtitle
                .setText(SessionContext.getClassDivSemSubtitle());

        // Outlined button click actions
        b.btnHelpSquare.setOnClickListener(
                v -> com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(requireContext(), "descriptive"));
        b.btnAddSquare.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
            }
        });
        if (b.btnCalcSquare != null) {
            b.btnCalcSquare.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).navigateTo(R.id.nav_descriptive_report);
                }
            });
        }
    }

    private void setupHeaderStrip() {
        String yr = SessionContext.selectedYear != null ? SessionContext.selectedYear.label : "2025-26";
        b.tvHeaderStripInfo
                .setText("Year: " + yr + " | " + SessionContext.getClassDivSemSubtitle());

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

        if (b.btnSearchStudents != null && b.etSearchStudents != null) {
            b.btnSearchStudents.setOnClickListener(v -> {
                boolean isSearching = b.etSearchStudents.getVisibility() == View.VISIBLE;
                if (isSearching) {
                    b.etSearchStudents.setText("");
                    b.etSearchStudents.setVisibility(View.GONE);
                    b.tvHeaderStripInfo.setVisibility(View.VISIBLE);
                    b.btnSearchStudents.setImageResource(R.drawable.ic_search);
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(b.etSearchStudents.getWindowToken(), 0);
                } else {
                    b.tvHeaderStripInfo.setVisibility(View.GONE);
                    b.etSearchStudents.setVisibility(View.VISIBLE);
                    b.etSearchStudents.requestFocus();
                    b.btnSearchStudents.setImageResource(R.drawable.ic_close);
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(b.etSearchStudents, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            });

            b.etSearchStudents.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterStudents(s.toString());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void filterStudents(String query) {
        if (AppCache.cachedDescriptiveStudents == null || adapter == null) return;
        Map<String, MarksRecord> marks = AppCache.cachedDescriptiveMarksMap != null ? AppCache.cachedDescriptiveMarksMap : new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            List<Student> sortedList = new ArrayList<>(AppCache.cachedDescriptiveStudents);
            com.kartik.myschool.utils.StudentSortUtils.sortStudents(sortedList);
            adapter.setData(sortedList, marks, false);
            return;
        }
        String q = query.toLowerCase().trim();
        List<Student> filtered = new ArrayList<>();
        for (Student s : AppCache.cachedDescriptiveStudents) {
            boolean matchName = s.name != null && s.name.toLowerCase().contains(q);
            boolean matchRoll = s.rollNo != null && s.rollNo.toLowerCase().contains(q);
            if (matchName || matchRoll) {
                filtered.add(s);
            }
        }
        com.kartik.myschool.utils.StudentSortUtils.sortStudents(filtered);
        adapter.setData(filtered, marks, false);
    }

    private void loadDescriptiveData() {
        if (activeClass == null) {
            Toast.makeText(requireContext(), R.string.select_class_first, Toast.LENGTH_LONG).show();
            return;
        }

        lastLoadedClassId = activeClass.id;
        lastLoadedSemesterId = activeSemesterId;

        // Initialize subjects list if null
        if (activeClass.subjects == null) {
            activeClass.subjects = new ArrayList<>();
        }

        boolean renderedFromCache = false;
        // 1. Instant Cache rendering (zero-latency display):
        if (AppCache.cachedDescriptiveStudents != null
                && AppCache.cachedDescriptiveMarksComplete
                && java.util.Objects.equals(activeClass.id, AppCache.cachedDescriptiveClassId)
                && java.util.Objects.equals(activeSemesterId, AppCache.cachedDescriptiveSemesterId)) {
            List<Student> cachedList = AppCache.cachedDescriptiveStudents;
            Map<String, MarksRecord> cachedMarks = AppCache.cachedDescriptiveMarksMap != null
                    ? AppCache.cachedDescriptiveMarksMap
                    : new HashMap<>();

            // Render instantly from cache after a brief delay to let slide animation start smoothly
            renderedFromCache = true;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && b != null) {
                    List<Student> sortedList = new ArrayList<>(cachedList);
                    com.kartik.myschool.utils.StudentSortUtils.sortStudents(sortedList);
                    adapter.setData(sortedList, cachedMarks, true);
                    if (swipeRefresh != null)
                        swipeRefresh.setRefreshing(false);
                }
            }, 200);
        }

        final boolean finalRenderedFromCache = renderedFromCache;

        // ★ FIX: Do NOT clear repo marks cache here.
        // Clearing on every resume wipes data that was just saved or loaded,
        // causing the fragment to always show stale/empty data until Firestore returns.
        // The cache is cleared selectively (by EnterDescriptiveActivity on save,
        // or by the swipe-to-refresh gesture).

        // 2. Fetch from network in parallel to drastically improve load time:
        final boolean[] fetchesDone = new boolean[2]; // [0] = students, [1] = marks
        final List<Student>[] studentsResult = new List[]{null};
        final Map<String, MarksRecord>[] marksResult = new Map[]{null};

        Runnable onBothFetchesDone = () -> {
            if (!fetchesDone[0] || !fetchesDone[1]) return;
            
            List<Student> finalList = studentsResult[0] != null ? studentsResult[0] : new ArrayList<>();
            Map<String, MarksRecord> finalMarks = marksResult[0] != null ? marksResult[0] : new HashMap<>();

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
                List<Student> sortedList = new ArrayList<>(finalList);
                com.kartik.myschool.utils.StudentSortUtils.sortStudents(sortedList);
                adapter.setData(sortedList, finalMarks, !finalRenderedFromCache);
                if (swipeRefresh != null)
                    swipeRefresh.setRefreshing(false);
            }
        };

        FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                studentsResult[0] = students;
                fetchesDone[0] = true;
                onBothFetchesDone.run();
            }

            @Override
            public void onError(Exception e) {
                studentsResult[0] = new ArrayList<>();
                fetchesDone[0] = true;
                onBothFetchesDone.run();

                if (isAdded()) {
                    if (AppCache.cachedDescriptiveStudents == null
                            || !java.util.Objects.equals(activeClass.id, AppCache.cachedDescriptiveClassId)) {
                        Toast.makeText(requireContext(), "Failed to load students: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        FirebaseRepository.get().getMarksForClassAndSemester(activeClass.id, activeSemesterId, new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
            @Override
            public void onSuccess(Map<String, MarksRecord> marksMap) {
                marksResult[0] = marksMap;
                fetchesDone[1] = true;
                onBothFetchesDone.run();
            }

            @Override
            public void onError(Exception e) {
                marksResult[0] = new HashMap<>();
                fetchesDone[1] = true;
                onBothFetchesDone.run();

                if (isAdded() && b != null) {
                    if (AppCache.cachedDescriptiveStudents == null || !java.util.Objects
                            .equals(activeClass.id, AppCache.cachedDescriptiveClassId)) {
                        // In case marks fetch fails but students succeeded, adapter.setData will be called with empty marks
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());

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

        boolean isFirstLoad = lastLoadedClassId == null || lastLoadedSemesterId == null;
        boolean classChanged = activeClass != null && !java.util.Objects.equals(activeClass.id, lastLoadedClassId);
        boolean semesterChanged = !java.util.Objects.equals(activeSemesterId, lastLoadedSemesterId);

        if (isFirstLoad || classChanged || semesterChanged) {
            loadDescriptiveData();
        }
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
        private final int[] lastPosition = new int[] { -1 };
        private final RecyclerView.RecycledViewPool viewPool;

        public DescriptiveAdapter(RecyclerView.RecycledViewPool sharedPool) {
            this.viewPool = sharedPool != null ? sharedPool : new RecyclerView.RecycledViewPool();
            this.viewPool.setMaxRecycledViews(0, 30);
            this.viewPool.setMaxRecycledViews(1, 30);
        }

        public void setData(List<Student> list, Map<String, MarksRecord> map, boolean resetAnimation) {
            final List<Student> newStudents = new ArrayList<>(list);
            final Map<String, MarksRecord> newMarks = new HashMap<>(map);

            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                // Take a snapshot of old data for DiffUtil before mutating
                final List<Student> oldStudents = new ArrayList<>(students);
                final Map<String, MarksRecord> oldMarks = new HashMap<>(marksMap);

                // Update the data first
                this.students.clear();
                this.students.addAll(newStudents);
                this.marksMap.clear();
                this.marksMap.putAll(newMarks);
                if (resetAnimation) {
                    lastPosition[0] = -1;
                }

                try {
                    androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
                        @Override
                        public int getOldListSize() { return oldStudents.size(); }
                        @Override
                        public int getNewListSize() { return newStudents.size(); }
                        @Override
                        public boolean areItemsTheSame(int oldPos, int newPos) {
                            return java.util.Objects.equals(oldStudents.get(oldPos).id, newStudents.get(newPos).id);
                        }
                        @Override
                        public boolean areContentsTheSame(int oldPos, int newPos) {
                            Student oldS = oldStudents.get(oldPos);
                            Student newS = newStudents.get(newPos);
                            MarksRecord oldM = oldMarks.get(oldS.id);
                            MarksRecord newM = newMarks.get(newS.id);
                            boolean studentSame = java.util.Objects.equals(oldS.name, newS.name) && java.util.Objects.equals(oldS.rollNo, newS.rollNo);
                            boolean marksSame = (oldM == null && newM == null) || (oldM != null && newM != null && oldM.updatedAt == newM.updatedAt);
                            return studentSame && marksSame;
                        }
                    });
                    diffResult.dispatchUpdatesTo(this);
                } catch (Exception e) {
                    // Safety net: if DiffUtil crashes during scroll, fall back
                    notifyDataSetChanged();
                }
            });
        }

        public void patchStudentMarks(String studentId, MarksRecord record) {
            marksMap.put(studentId, record);
            if (AppCache.cachedDescriptiveMarksMap == null
                    || !java.util.Objects.equals(activeClass.id, AppCache.cachedDescriptiveClassId)
                    || !java.util.Objects.equals(activeSemesterId, AppCache.cachedDescriptiveSemesterId)) {
                AppCache.cachedDescriptiveMarksMap = new HashMap<>();
                AppCache.cachedDescriptiveClassId = activeClass.id;
                AppCache.cachedDescriptiveSemesterId = activeSemesterId;
                AppCache.cachedDescriptiveMarksComplete = true;
            }
            AppCache.cachedDescriptiveMarksMap.put(studentId, record);
            final int size = students.size();
            for (int i = 0; i < size; i++) {
                if (java.util.Objects.equals(studentId, students.get(i).id)) {
                    final int pos = i;
                    if (b != null && b.rvDescriptiveStudents != null && !b.rvDescriptiveStudents.isComputingLayout()) {
                        notifyItemChanged(pos);
                    } else {
                        new android.os.Handler(android.os.Looper.getMainLooper())
                                .post(() -> { try { notifyItemChanged(pos); } catch (Exception ignored) {} });
                    }
                    return;
                }
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> { try { notifyDataSetChanged(); } catch (Exception ignored) {} });
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
            private final SubjectInnerAdapter innerAdapter;

            public ViewHolder(ItemDescriptiveStudentBlockBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                binding.rvSubjectsHorizontal.setRecycledViewPool(viewPool);
                binding.rvSubjectsHorizontal.setNestedScrollingEnabled(false);
                binding.rvSubjectsHorizontal.setItemViewCacheSize(6);
                binding.rvSubjectsHorizontal.setItemAnimator(null);

                innerAdapter = new SubjectInnerAdapter(null, new ArrayList<>(), null, isGridViewMode, this);
                binding.rvSubjectsHorizontal.setAdapter(innerAdapter);
            }

            public void bind(Student s, int index) {
                String roll = (s.rollNo != null && !s.rollNo.isEmpty()) ? s.rollNo : String.valueOf(index);
                binding.tvStudentName.setText(roll + ". " + s.name);
                binding.btnStudentMore.setOnClickListener(v -> showStudentRemarkMenu(v, s));

                MarksRecord marks = getDisplayMarksForStudent(s);
                List<Subject> allDescriptiveSubjects = new ArrayList<>();
                if (activeClass.subjects != null) {
                    allDescriptiveSubjects.addAll(activeClass.subjects);
                }
                allDescriptiveSubjects.add(new Subject("Vishesh pragati", 0));
                allDescriptiveSubjects.add(new Subject("Aavad, chanda, etc", 0));
                allDescriptiveSubjects.add(new Subject("Sudharna Aavashyaka", 0));
                allDescriptiveSubjects.add(new Subject("Vyaktimatva gun vishgesh", 0));

                binding.layoutSubjectsGridContainer.setVisibility(View.GONE);
                binding.rvSubjectsHorizontal.setVisibility(View.VISIBLE);

                if (binding.rvSubjectsHorizontal.getLayoutManager() == null) {
                    if (isGridViewMode) {
                        binding.rvSubjectsHorizontal.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(itemView.getContext(), 2));
                    } else {
                        LinearLayoutManager lm = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
                        lm.setItemPrefetchEnabled(true);
                        lm.setInitialPrefetchItemCount(4);
                        binding.rvSubjectsHorizontal.setLayoutManager(lm);
                    }
                } else {
                    RecyclerView.LayoutManager currentLm = binding.rvSubjectsHorizontal.getLayoutManager();
                    if (isGridViewMode && !(currentLm instanceof androidx.recyclerview.widget.GridLayoutManager)) {
                        binding.rvSubjectsHorizontal.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(itemView.getContext(), 2));
                    } else if (!isGridViewMode && currentLm instanceof androidx.recyclerview.widget.GridLayoutManager) {
                        LinearLayoutManager lm = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
                        lm.setItemPrefetchEnabled(true);
                        lm.setInitialPrefetchItemCount(4);
                        binding.rvSubjectsHorizontal.setLayoutManager(lm);
                    }
                }

                innerAdapter.updateData(s, allDescriptiveSubjects, marks, isGridViewMode);
            }



            private void showStudentRemarkMenu(View anchor, Student student) {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(itemView.getContext(), anchor);
                popup.getMenu().add(0, 1, 0, "Edit remark");
                popup.getMenu().add(0, 2, 1, "Delete remark");
                popup.getMenu().add(0, 3, 2, "Change to Male (सर्व विषय)");
                popup.getMenu().add(0, 4, 3, "Change to Female (सर्व विषय)");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) { openMarksEntry(student); return true; }
                    if (item.getItemId() == 2) { confirmDeleteRemarks(student); return true; }
                    if (item.getItemId() == 3) { applyGenderRemarkChange(student, false); return true; }
                    if (item.getItemId() == 4) { applyGenderRemarkChange(student, true); return true; }
                    return false;
                });
                popup.show();
            }

            private void confirmDeleteRemarks(Student student) {
                new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext())
                        .setTitle(R.string.msg_delete_remarks)
                        .setMessage("This will remove saved descriptive remarks for this student only.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> deleteRemarks(student)).show();
            }

            private void deleteRemarks(Student student) {
                MarksRecord record = getDisplayMarksForStudent(student);
                if (record == null || record.detailedMarks == null || record.detailedMarks.isEmpty()) {
                    Toast.makeText(itemView.getContext(), R.string.msg_no_saved_remarks_found, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(itemView.getContext(), R.string.msg_no_saved_remarks_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                saveStudentRemarkRecord(student, record, "Remarks deleted.");
            }

            private void applyGenderRemarkChange(Student student, boolean female) {
                MarksRecord record = getDisplayMarksForStudent(student);
                if (record == null || record.detailedMarks == null || record.detailedMarks.isEmpty()) {
                    Toast.makeText(itemView.getContext(), R.string.msg_no_saved_remarks_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean changed = false;
                for (MarksRecord.SubjectMarksDetail detail : record.detailedMarks.values()) {
                    if (detail == null || detail.remark == null || detail.remark.trim().isEmpty()) continue;
                    String updated = adjustRemarkGender(detail.remark, female);
                    if (!updated.equals(detail.remark)) {
                        detail.remark = updated;
                        changed = true;
                    }
                }
                if (!changed) {
                    Toast.makeText(itemView.getContext(), R.string.msg_no_gender_words_found_to_chang, Toast.LENGTH_SHORT).show();
                    return;
                }
                saveStudentRemarkRecord(student, record, "Gender sentence updated.");
            }

            private String adjustRemarkGender(String remark, boolean female) {
                String[] roots = { "कर", "दे", "घे", "शिक", "वाच", "लिहि", "बोल", "सांग", "सोडव", "दाखव", "ओळख", "वापर", "जप", "जोपास", "वाढव", "मांड", "ऐक", "निवड", "रेखाट", "रंगव", "खेळ", "धाव", "हो", "जिंक", "राह", "ठेव", "पाड", "विचार", "आवड", "चाल", "पाह", "ये", "जा", "खा", "पि", "झोप", "उठ", "बस", "म्हण", "वाग", "अस" };
                String rootPattern = String.join("|", roots);
                String updated = remark;
                if (!female) {
                    updated = updated.replaceAll("(?<![\\p{L}])(" + rootPattern + ")ते(?![\\p{L}])", "$1तो");
                    updated = updated.replaceAll("(?<![\\p{L}])ती(?![\\p{L}])", "तो");
                    updated = updated.replaceAll("([^\\s/]+?)\\s*तो\\s*/\\s*([^\\s/]+?)\\s*ते", "$1तो");
                } else {
                    updated = updated.replaceAll("(?<![\\p{L}])(" + rootPattern + ")तो(?![\\p{L}])", "$1ते");
                    updated = updated.replaceAll("(?<![\\p{L}])तो(?![\\p{L}])", "ती");
                    updated = updated.replaceAll("([^\\s/]+?)\\s*तो\\s*/\\s*([^\\s/]+?)\\s*ते", "$2ते");
                }
                return updated;
            }

            private MarksRecord getDisplayMarksForStudent(Student student) {
                MarksRecord record = marksMap.get(student.id);
                MarksRecord cachedRecord = null;
                if (AppCache.cachedMarksMap != null
                        && java.util.Objects.equals(activeClass != null ? activeClass.id : null, AppCache.cachedClassIdForStudents)
                        && java.util.Objects.equals(activeSemesterId, AppCache.cachedSemesterIdForMarks)) {
                    cachedRecord = AppCache.cachedMarksMap.get(student.id);
                }
                if (cachedRecord != null && (record == null || cachedRecord.updatedAt >= record.updatedAt)) {
                    record = cachedRecord;
                }

                MarksRecord descriptiveCachedRecord = null;
                if (AppCache.cachedDescriptiveMarksMap != null
                        && java.util.Objects.equals(activeClass != null ? activeClass.id : null, AppCache.cachedDescriptiveClassId)
                        && java.util.Objects.equals(activeSemesterId, AppCache.cachedDescriptiveSemesterId)) {
                    descriptiveCachedRecord = AppCache.cachedDescriptiveMarksMap.get(student.id);
                }
                if (descriptiveCachedRecord != null && (record == null || descriptiveCachedRecord.updatedAt >= record.updatedAt)) {
                    record = descriptiveCachedRecord;
                }

                MarksRecord selectedRecord = AppCache.selectedMarks;
                if (selectedRecord == null || student.id == null || !java.util.Objects.equals(student.id, selectedRecord.studentId)) return record;
                if (activeClass == null || activeClass.id == null || !java.util.Objects.equals(activeClass.id, selectedRecord.classId)) return record;
                if (selectedRecord.semesterId != null && activeSemesterId != null && !selectedRecord.semesterId.isEmpty() && !java.util.Objects.equals(activeSemesterId, selectedRecord.semesterId)) return record;
                if (record == null || selectedRecord.updatedAt >= record.updatedAt) return selectedRecord;
                return record;
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
                        FirebaseRepository.get().updateMarksInCache(record.classId, record.semesterId, student.id, record);
                        Toast.makeText(itemView.getContext(), successMessage, Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(itemView.getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            public void openSubjectRemarkEntry(Student student, Subject subject, int subjectIndex) {
                ClassModel freshClass = SessionContext.selectedClass != null ? SessionContext.selectedClass : activeClass;
                activeClass = freshClass;
                MarksRecord record = getDisplayMarksForStudent(student);
                SubjectRemarkEntryDialog dialog = SubjectRemarkEntryDialog.newInstance(student, subject, subjectIndex, freshClass, record);
                dialog.setOnRemarkSavedListener((studentId, newRecord) -> {
                    patchStudentMarks(studentId, newRecord);
                });
                dialog.show(DescriptiveEntriesFragment.this.getChildFragmentManager(), "SubjectRemarkEntryDialog");
            }

            public void openMarksEntry(Student student) {
                AppCache.selectedStudent = student;
                ClassModel freshClass = SessionContext.selectedClass != null ? SessionContext.selectedClass : activeClass;
                AppCache.selectedClass = freshClass;
                activeClass = freshClass;
                MarksRecord existingRecord = marksMap.get(student.id);
                AppCache.selectedMarks = existingRecord;
                Intent intent = new Intent(itemView.getContext(), EnterDescriptiveActivity.class);
                itemView.getContext().startActivity(intent);
            }
        }
    }

    public static MarksRecord.SubjectMarksDetail getSubjectDetail(MarksRecord record, Subject sub, int subjectIndex) {
        if (record == null || record.detailedMarks == null || sub == null || sub.name == null) return null;
        String safeKey = MarksRecord.sanitizeKey(sub.name);
        // Strategy 1: sanitized key (most common path)
        MarksRecord.SubjectMarksDetail detail = record.detailedMarks.get(safeKey);
        if (detail != null) return detail;
        // Strategy 2: raw subject name (in case stored un-sanitized)
        detail = record.detailedMarks.get(sub.name);
        if (detail != null) return detail;
        // Strategy 3: compare sanitized forms of all stored keys
        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : record.detailedMarks.entrySet()) {
            String key = entry.getKey();
            if (key != null && MarksRecord.sanitizeKey(key).equals(safeKey)) return entry.getValue();
        }
        // Strategy 4: language-aware equivalence (English ↔ Marathi subject names).
        // This handles the case where the admin changes subject display language / sequence
        // and the stored key no longer matches the current name exactly.
        // NOTE: We intentionally do NOT fall back by index because HashMap has no
        // guaranteed iteration order — an index-based lookup would return wrong data
        // whenever subjects are reordered.
        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : record.detailedMarks.entrySet()) {
            String key = entry.getKey();
            if (key != null && com.kartik.myschool.model.Subject.isSameSubject(key, sub.name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static class SubjectInnerAdapter extends RecyclerView.Adapter<SubjectInnerAdapter.ViewHolder> {
        private Student student;
        private List<Subject> subjects;
        private MarksRecord marks;
        private boolean isGridViewMode;
        private final DescriptiveAdapter.ViewHolder parentHolder;

        public SubjectInnerAdapter(Student student, List<Subject> subjects, MarksRecord marks, boolean isGridViewMode, DescriptiveAdapter.ViewHolder parentHolder) {
            this.student = student;
            this.subjects = subjects;
            this.marks = marks;
            this.isGridViewMode = isGridViewMode;
            this.parentHolder = parentHolder;
        }

        public boolean isGridViewMode() { return isGridViewMode; }

        public void updateData(Student student, List<Subject> subjects, MarksRecord marks, boolean isGridViewMode) {
            boolean changed = this.student == null || student == null
                    || !java.util.Objects.equals(this.student.id, student.id)
                    || this.marks != marks
                    || this.isGridViewMode != isGridViewMode
                    || this.subjects != subjects;

            this.student = student;
            this.subjects = subjects;
            this.marks = marks;
            this.isGridViewMode = isGridViewMode;

            if (changed) {
                notifyDataSetChanged();
            }
        }

        @Override
        public int getItemViewType(int position) {
            return 10;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDescriptiveSubjectCardBinding cardB = ItemDescriptiveSubjectCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(cardB);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Subject sub = subjects.get(position);
            holder.bind(student, sub, position + 1, marks, parentHolder);
        }

        @Override
        public int getItemCount() {
            return subjects != null ? subjects.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemDescriptiveSubjectCardBinding cardB;
            public ViewHolder(ItemDescriptiveSubjectCardBinding binding) {
                super(binding.getRoot());
                this.cardB = binding;
            }

            public void bind(Student student, Subject sub, int number, MarksRecord record, DescriptiveAdapter.ViewHolder parentHolder) {
                cardB.tvSubjectName.setText(number + ". " + com.kartik.myschool.utils.pdf.PdfLocalizer.translateSubject(itemView.getContext(), sub.name));
                List<String> remarks = new ArrayList<>();
                MarksRecord.SubjectMarksDetail detail = getSubjectDetail(record, sub, number - 1);
                if (detail != null && detail.remark != null && !detail.remark.trim().isEmpty()) {
                    String[] parts = detail.remark.trim().split("\\|\\|");
                    for (String p : parts) {
                        String trimmed = p.trim();
                        if (!trimmed.isEmpty()) remarks.add(trimmed);
                    }
                }

                com.google.android.material.card.MaterialCardView cardRoot = (com.google.android.material.card.MaterialCardView) cardB.getRoot();
                if (!remarks.isEmpty()) {
                    cardB.tvRemarksList.setVisibility(View.VISIBLE);
                    cardB.layoutEmptyRemark.setVisibility(View.GONE);
                    StringBuilder sb = new StringBuilder();
                    for (int r = 0; r < remarks.size(); r++) {
                        if (r > 0) sb.append("\n");
                        sb.append("• ").append(remarks.get(r));
                    }
                    cardB.tvRemarksList.setText(sb.toString());
                    cardRoot.setStrokeColor(0xFF81C784);
                } else {
                    cardB.tvRemarksList.setVisibility(View.GONE);
                    cardB.layoutEmptyRemark.setVisibility(View.VISIBLE);
                    cardRoot.setStrokeColor(0xFFFFB74D);
                }

                cardB.getRoot().setOnClickListener(v -> parentHolder.openSubjectRemarkEntry(student, sub, number - 1));

                float density = itemView.getResources().getDisplayMetrics().density;
                ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) cardB.getRoot().getLayoutParams();
                if (isGridViewMode) {
                    param.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    param.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    int margin = (int) (3 * density);
                    param.setMargins(margin, margin, margin, margin);
                } else {
                    param.width = (int) (240 * density);
                    param.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    int margin = (int) (4 * density);
                    param.setMargins(margin, margin, margin, margin);
                }
                cardB.getRoot().setLayoutParams(param);
            }
        }
    }
}
