package com.kartik.myschool.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.AppCache;
import com.kartik.myschool.EnterMarksActivity;
import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.databinding.FragmentFormativeSummativeBinding;
import com.kartik.myschool.databinding.ItemEvaluationStudentBlockBinding;
import com.kartik.myschool.databinding.ItemEvaluationSubjectCardBinding;
import com.kartik.myschool.databinding.ItemEvaluationSubjectCardCompactBinding;
import com.kartik.myschool.databinding.ItemSubjectMarksRowBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.GradeCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormativeSummativeFragment extends Fragment {

    private FragmentFormativeSummativeBinding b;
    private EvaluationAdapter adapter;
    private ClassModel activeClass;
    private String activeSemesterId = "sem_1";
    private int activeSemesterNumber = 1;
    private boolean isGridView = false;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private String lastLoadedClassId = null;
    private String lastLoadedSemesterId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        b = FragmentFormativeSummativeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activeClass = SessionContext.selectedClass;
        if (SessionContext.selectedSemester != null) {
            activeSemesterId = SessionContext.selectedSemester.id;
            activeSemesterNumber = SessionContext.selectedSemester.number;
        }

        setupCustomAppBar();
        setupHeaderStrip();

        swipeRefresh = b.swipeRefreshLayout;
        swipeRefresh.setColorSchemeColors(0xFF6C4CCF, 0xFF9C27B0, 0xFF00A5CF);
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFFFFFFFF);
        swipeRefresh.setOnRefreshListener(() -> {
            // Clear all caches to force a true Firebase reload
            AppCache.cachedStudents = null;
            AppCache.cachedMarksMap = null;
            AppCache.cachedClassIdForStudents = null;
            AppCache.cachedSemesterIdForMarks = null;
            FirebaseRepository.get().clearMarksCache();
            loadEvaluationData();
        });

        updateLayoutManager();
        adapter = new EvaluationAdapter();
        b.rvEvaluationStudents.setAdapter(adapter);
    }

    private void updateLayoutManager() {
        b.rvEvaluationStudents.setLayoutManager(new LinearLayoutManager(requireContext()));
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
                v -> Toast.makeText(requireContext(), R.string.msg_evaluation_help_manual_opened, Toast.LENGTH_SHORT).show());
        b.btnAddSquare.setOnClickListener(
                v -> Toast.makeText(requireContext(), R.string.msg_add_student_clicked, Toast.LENGTH_SHORT).show());
        b.btnCalcSquare.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).navigateTo(R.id.nav_print_report);
            }
        });
    }

    private void setupHeaderStrip() {
        String yr = SessionContext.selectedYear != null ? SessionContext.selectedYear.label : "2025-26";
        String cls = activeClass != null ? activeClass.className : "5";
        String div = activeClass != null ? activeClass.division : "1";
        b.tvHeaderStripInfo
                .setText("Year: " + yr + " | Cls: " + cls + "-" + div + " | Sem: " + activeSemesterNumber);

        b.btnGridListToggle.setOnClickListener(v -> {
            isGridView = !isGridView;

            // Toggle icon
            if (isGridView) {
                b.btnGridListToggle.setImageResource(R.drawable.ic_list_bullet);
            } else {
                b.btnGridListToggle.setImageResource(R.drawable.ic_table_grid);
            }

            updateLayoutManager();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void loadEvaluationData() {
        if (activeClass == null) {
            Toast.makeText(requireContext(), R.string.select_class_first, Toast.LENGTH_LONG).show();
            return;
        }

        lastLoadedClassId = activeClass.id;
        lastLoadedSemesterId = activeSemesterId;

        // Subjects must be activated via the Subjects page — no hardcoded defaults.
        if (activeClass.subjects == null) {
            activeClass.subjects = new ArrayList<>();
        }
        if (activeClass.subjects.isEmpty()) {
            if (b != null) {
                b.progressLoading.setVisibility(View.GONE);
                b.tvEmptyState.setVisibility(View.VISIBLE);
                b.tvEmptyState
                        .setText(R.string.msg_no_subjects_configured_ngo_to);
            }
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }

        // Show progress spinner while fetching
        if (b != null) {
            b.progressLoading.setVisibility(View.VISIBLE);
            b.tvEmptyState.setVisibility(View.GONE);
        }

        // 1. Instant Cache rendering (zero-latency display):
        if (AppCache.cachedStudents != null
                && java.util.Objects.equals(activeClass.id, AppCache.cachedClassIdForStudents)
                && java.util.Objects.equals(activeSemesterId, AppCache.cachedSemesterIdForMarks)) {
            List<Student> cachedList = AppCache.cachedStudents;
            Map<String, MarksRecord> cachedMarks = AppCache.cachedMarksMap != null ? AppCache.cachedMarksMap
                    : new HashMap<>();

            // Render instantly from cache and hide progress
            if (b != null)
                b.progressLoading.setVisibility(View.GONE);
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            adapter.setData(cachedList, cachedMarks);
        }

        // 2. Background fetch (always runs to get fresh data):
        FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                if (students == null)
                    students = new ArrayList<>();
                List<Student> finalList = students;

                // Fetch marks map
                FirebaseRepository.get().getMarksForClassAndSemester(activeClass.id, activeSemesterId,
                        new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                            @Override
                            public void onSuccess(Map<String, MarksRecord> marksMap) {
                                Map<String, MarksRecord> finalMarks = marksMap != null ? marksMap : new HashMap<>();

                                // Merge network results with the fresh cache based on updatedAt
                                if (AppCache.cachedMarksMap != null) {
                                    for (Map.Entry<String, MarksRecord> entry : AppCache.cachedMarksMap.entrySet()) {
                                        String sId = entry.getKey();
                                        MarksRecord cachedRecord = entry.getValue();
                                        MarksRecord fetchedRecord = finalMarks.get(sId);

                                        // If cache has a newer or same record, keep the cache!
                                        if (cachedRecord != null && (fetchedRecord == null
                                                || cachedRecord.updatedAt >= fetchedRecord.updatedAt)) {
                                            finalMarks.put(sId, cachedRecord);
                                        }
                                    }
                                }

                                // Update cache
                                AppCache.cachedStudents = finalList;
                                AppCache.cachedMarksMap = finalMarks;
                                AppCache.cachedClassIdForStudents = activeClass.id;
                                AppCache.cachedSemesterIdForMarks = activeSemesterId;

                                if (isAdded() && b != null) {
                                    b.progressLoading.setVisibility(View.GONE);
                                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                                    if (finalList.isEmpty()) {
                                        b.tvEmptyState.setVisibility(View.VISIBLE);
                                    } else {
                                        b.tvEmptyState.setVisibility(View.GONE);
                                    }
                                    adapter.setData(finalList, finalMarks);
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                String errMsg = (e != null && e.getMessage() != null) ? e.getMessage() : "Unknown error";
                                Log.e("FORMATIVE", "getMarksForClassAndSemester failed: " + errMsg, e);
                                if (isAdded() && b != null) {
                                    b.progressLoading.setVisibility(View.GONE);
                                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                                    // FIX: only fallback to empty marks when there is truly no
                                    // previously-cached data. Never wipe marks just because of a
                                    // transient network error on resume.
                                    boolean hasCachedMarks = AppCache.cachedMarksMap != null
                                            && activeClass != null
                                            && java.util.Objects.equals(activeClass.id,
                                                    AppCache.cachedClassIdForStudents)
                                            && java.util.Objects.equals(activeSemesterId,
                                                    AppCache.cachedSemesterIdForMarks);
                                    if (!hasCachedMarks) {
                                        adapter.setData(finalList, new HashMap<>());
                                    }
                                }
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                if (isAdded() && b != null) {
                    b.progressLoading.setVisibility(View.GONE);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    // Only show network error toast if cache is fully empty
                    if (AppCache.cachedStudents == null
                            || activeClass == null
                            || !java.util.Objects.equals(activeClass.id, AppCache.cachedClassIdForStudents)) {
                        String errMsg = (e != null && e.getMessage() != null) ? e.getMessage() : "Unknown error";
                        Toast.makeText(requireContext(), "Failed to load students: " + errMsg,
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

        if (SessionContext.selectedSemester == null && activeClass != null && activeClass.semesterId != null
                && !activeClass.semesterId.isEmpty()) {
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
        } else {
            activeSemesterId = "sem_1";
            activeSemesterNumber = 1;
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

        // FIX: If marks were just saved in EnterMarksActivity:
        // 1. Instantly patch only that student's card — 0ms, no network
        // 2. Then do a full Firestore reload in background to confirm
        if (AppCache.marksJustSaved
                && AppCache.marksJustSavedStudentId != null
                && AppCache.marksJustSavedRecord != null) {

            android.util.Log.d("FORMATIVE_SUMMATIVE", "marksJustSaved=true for student="
                    + AppCache.marksJustSavedStudentId + " — patching adapter instantly");

            // Instant patch: update only the one card that changed
            adapter.patchStudentMarks(
                    AppCache.marksJustSavedStudentId,
                    AppCache.marksJustSavedRecord);

            // Consume the flags so next unrelated resume doesn’t re-patch
            AppCache.marksJustSaved          = false;
            AppCache.marksJustSavedStudentId = null;
            AppCache.marksJustSavedRecord    = null;

            // Clear selectedMarks so it doesn't bleed into other students
            AppCache.selectedMarks = null;
        }

        boolean isFirstLoad = lastLoadedClassId == null || lastLoadedSemesterId == null;
        boolean classChanged = activeClass != null && !java.util.Objects.equals(activeClass.id, lastLoadedClassId);
        boolean semesterChanged = !java.util.Objects.equals(activeSemesterId, lastLoadedSemesterId);

        if (isFirstLoad || classChanged || semesterChanged) {
            loadEvaluationData();
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
    private class EvaluationAdapter extends RecyclerView.Adapter<EvaluationAdapter.ViewHolder> {

        private final List<Student> students = new ArrayList<>();
        private final Map<String, MarksRecord> marksMap = new HashMap<>();

        public void setData(List<Student> list, Map<String, MarksRecord> map) {
            students.clear();
            students.addAll(list);
            marksMap.clear();
            marksMap.putAll(map);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> notifyDataSetChanged());
        }

        /** Instantly updates one student's card without a full list reload.
         *  Called by onResume() immediately after returning from EnterMarksActivity. */
        public void patchStudentMarks(String studentId, MarksRecord record) {
            // Update the adapter's marks map
            marksMap.put(studentId, record);
            // Also update the marks AppCache so future renders are correct
            if (AppCache.cachedMarksMap == null
                    || !java.util.Objects.equals(activeClass.id, AppCache.cachedClassIdForStudents)
                    || !java.util.Objects.equals(activeSemesterId, AppCache.cachedSemesterIdForMarks)) {
                AppCache.cachedMarksMap = new HashMap<>();
                AppCache.cachedClassIdForStudents = activeClass.id;
                AppCache.cachedSemesterIdForMarks = activeSemesterId;
            }
            AppCache.cachedMarksMap.put(studentId, record);
            // Notify only the changed item for a smooth, flicker-free update
            for (int i = 0; i < students.size(); i++) {
                if (java.util.Objects.equals(studentId, students.get(i).id)) {
                    final int pos = i;
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .post(() -> notifyItemChanged(pos));
                    android.util.Log.d("FORMATIVE_SUMMATIVE",
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
            ItemEvaluationStudentBlockBinding binding = ItemEvaluationStudentBlockBinding.inflate(
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
            private final ItemEvaluationStudentBlockBinding binding;

            public ViewHolder(ItemEvaluationStudentBlockBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Student s, int index) {
                binding.tvStudentName.setText(index + ". " + s.name);
                binding.btnStudentMore.setOnClickListener(
                        v -> Toast.makeText(itemView.getContext(), "Options for " + s.name, Toast.LENGTH_SHORT).show());

                MarksRecord marks = marksMap.get(s.id);

                // Build Grade Chips Row
                binding.layoutGradeChips.removeAllViews();
                if (marks != null && marks.detailedMarks != null && activeClass.subjects != null) {
                    for (Subject sub : activeClass.subjects) {
                        MarksRecord.SubjectMarksDetail detail = marks.detailedMarks
                                .get(MarksRecord.sanitizeKey(sub.name));
                        if (detail != null) {
                            android.util.Log.d("GRADE_BUG", "Student: " + s.name 
                                + " | Subject: " + sub.name 
                                + " | grade: " + detail.grade 
                                + " | nirikhshan: " + detail.nirikhshan
                                + " | grandTotal: " + detail.grandTotal
                                + " | entered: " + hasEnteredMarks(detail));
                            if (detail.grade != null && !detail.grade.isEmpty() && hasEnteredMarks(detail)) {
                                binding.layoutGradeChips.addView(createGradeChip(detail.grade));
                            }
                        }
                    }
                }

                // Adjust card margins depending on layout mode
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.getRoot().getLayoutParams();
                float density = itemView.getResources().getDisplayMetrics().density;
                if (isGridView) {
                    lp.setMarginStart((int) (10 * density));
                    lp.setMarginEnd((int) (10 * density));
                    lp.bottomMargin = (int) (12 * density);
                } else {
                    lp.setMarginStart((int) (14 * density));
                    lp.setMarginEnd((int) (14 * density));
                    lp.bottomMargin = (int) (16 * density);
                }
                binding.getRoot().setLayoutParams(lp);

                // Show/hide grade chips under student name
                binding.layoutGradeChips.setVisibility(isGridView ? View.GONE : View.VISIBLE);

                // Build Subject Cards depending on layout mode
                binding.scrollSubjects.setVisibility(View.VISIBLE);
                binding.layoutSummaryGrid.setVisibility(View.GONE);
                binding.layoutSubjectsHorizontal.removeAllViews();

                if (activeClass.subjects != null) {
                    for (int i = 0; i < activeClass.subjects.size(); i++) {
                        Subject sub = activeClass.subjects.get(i);
                        View cardView;
                        if (isGridView) {
                            cardView = createSubjectCardCompact(s, sub, i + 1, marks);
                        } else {
                            cardView = createSubjectCard(s, sub, i + 1, marks);
                        }
                        binding.layoutSubjectsHorizontal.addView(cardView);
                    }
                }
            }

            private View createGradeChip(String grade) {
                TextView tv = new TextView(itemView.getContext());
                tv.setText(grade);
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
                tv.setGravity(android.view.Gravity.CENTER);
                float density = getResources().getDisplayMetrics().density;
                tv.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
                tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));

                int textColor = 0xFF6C4CCF;
                int borderColor = 0xFFE1D5FF;
                int bgColor = 0xFFF3EEFF;

                if (grade.startsWith("A-1") || grade.startsWith("अ-1")) {
                    textColor = 0xFF6C4CCF;
                    borderColor = 0xFFD7C4FF;
                    bgColor = 0xFFF3EEFF;
                } else if (grade.startsWith("A-2") || grade.startsWith("अ-2")) {
                    textColor = 0xFF00A5CF;
                    borderColor = 0xFFB2E7F5;
                    bgColor = 0xFFE0F7FA;
                } else if (grade.startsWith("B-1") || grade.startsWith("ब-1")) {
                    textColor = 0xFF2E7D32;
                    borderColor = 0xFFC8E6C9;
                    bgColor = 0xFFE8F5E9;
                } else if (grade.startsWith("B-2") || grade.startsWith("ब-2")) {
                    textColor = 0xFF9E9D24;
                    borderColor = 0xFFF0F4C3;
                    bgColor = 0xFFF9FBE7;
                } else {
                    textColor = 0xFFE65100;
                    borderColor = 0xFFFFE0B2;
                    bgColor = 0xFFFFF3E0;
                }

                tv.setTextColor(textColor);

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(bgColor);
                gd.setCornerRadius(6 * density);
                gd.setStroke((int) (1 * density), borderColor);
                tv.setBackground(gd);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMarginEnd((int) (6 * density));
                tv.setLayoutParams(lp);

                return tv;
            }

            private String formatVal(int val) {
                return String.valueOf(val);
            }

            private View createSubjectCard(Student student, Subject sub, int number, MarksRecord record) {
                ItemEvaluationSubjectCardBinding cardB = ItemEvaluationSubjectCardBinding.inflate(
                        LayoutInflater.from(itemView.getContext()), binding.layoutSubjectsHorizontal, false);

                cardB.tvSubjectName.setText(number + ". " + sub.name);

                // Default table values
                String fe1 = "0", fe2 = "0", fe3 = "0", fe4 = "0", fe5 = "0", fe6 = "0", fe7 = "0", fe8 = "0";
                String se1 = "0", se2 = "0", se3 = "0";
                String fet = "0", set = "0", tet = "0";
                String gradeVal = "—";
                boolean hasMarks = false;

                String safeKey = MarksRecord.sanitizeKey(sub.name);
                if (record != null && record.detailedMarks != null && record.detailedMarks.containsKey(safeKey)) {
                    MarksRecord.SubjectMarksDetail d = record.detailedMarks.get(safeKey);
                    if (d != null && hasEnteredMarks(d)) {
                        hasMarks = true;
                        fe1 = formatVal(d.nirikhshan);
                        fe2 = formatVal(d.tondiKam);
                        fe3 = formatVal(d.pratyakshik);
                        fe4 = formatVal(d.upkram);
                        fe5 = formatVal(d.prakalp);
                        fe6 = formatVal(d.chachani);
                        fe7 = formatVal(d.swadhyay);
                        fe8 = formatVal(d.itar);

                        se1 = formatVal(d.tondi);
                        se2 = formatVal(d.pratyakshikB);
                        se3 = formatVal(d.lekhi);

                        fet = formatVal(d.akarikTotal);
                        set = formatVal(d.sanklit);
                        tet = formatVal(d.grandTotal);
                        if (d.grade != null && !d.grade.isEmpty()) {
                            gradeVal = d.grade;
                        }
                    }
                }

                // Set table text views
                cardB.tvFE1.setText(fe1);
                cardB.tvFE2.setText(fe2);
                cardB.tvFE3.setText(fe3);
                cardB.tvFE4.setText(fe4);
                cardB.tvFE5.setText(fe5);
                cardB.tvFE6.setText(fe6);
                cardB.tvFE7.setText(fe7);
                cardB.tvFE8.setText(fe8);

                cardB.tvSE1.setText(se1);
                cardB.tvSE2.setText(se2);
                cardB.tvSE3.setText(se3);

                cardB.tvFET.setText(fet);
                cardB.tvSET.setText(set);
                cardB.tvTET.setText(tet);

                // Tint card border orange to signal "needs entry" if no marks are entered yet
                if (hasMarks) {
                    ((com.google.android.material.card.MaterialCardView) cardB.getRoot())
                            .setStrokeColor(0xFFE0E0E0);
                } else {
                    ((com.google.android.material.card.MaterialCardView) cardB.getRoot())
                            .setStrokeColor(0xFFFFB74D);
                }

                // Style dynamic Grade Chip in header
                float density = itemView.getResources().getDisplayMetrics().density;
                int gradeColor = 0xFF90A4AE; // gray
                if (gradeVal.startsWith("A-1") || gradeVal.startsWith("अ-1"))
                    gradeColor = 0xFF6C4CCF;
                else if (gradeVal.startsWith("A-2") || gradeVal.startsWith("अ-2"))
                    gradeColor = 0xFF00A5CF;
                else if (gradeVal.startsWith("B-1") || gradeVal.startsWith("ब-1"))
                    gradeColor = 0xFF2E7D32;
                else if (gradeVal.startsWith("B-2") || gradeVal.startsWith("ब-2"))
                    gradeColor = 0xFF9E9D24;
                else if (!gradeVal.equals("—"))
                    gradeColor = 0xFFE65100;

                cardB.tvHeaderGrade.setText(gradeVal);
                if (gradeVal.equals("—")) {
                    cardB.tvHeaderGrade.setVisibility(View.GONE);
                } else {
                    cardB.tvHeaderGrade.setVisibility(View.VISIBLE);
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setColor(gradeColor);
                    gd.setCornerRadius(6 * density);
                    cardB.tvHeaderGrade.setBackground(gd);
                }

                // Setup 3-dot popup menu
                cardB.btnSubjectMore.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(
                            itemView.getContext(), v);
                    popup.getMenu().add(0, 1, 0, "Enter Marks");
                    popup.getMenu().add(0, 2, 1, "Quick View Info");
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) {
                            openMarksEntry(student);
                            return true;
                        }
                        Toast.makeText(itemView.getContext(), R.string.msg_info_opened, Toast.LENGTH_SHORT).show();
                        return true;
                    });
                    popup.show();
                });

                // JOIN TO ENTER MARKS PAGE: Click on the entire subject card opens marks entry!
                cardB.getRoot().setOnClickListener(v -> openMarksEntry(student));

                // Set consistent layout width and margins for horizontal scrolling (300dp to
                // fit table)
                android.widget.LinearLayout.LayoutParams param = new android.widget.LinearLayout.LayoutParams(
                        (int) (300 * density),
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                int margin = (int) (6 * density);
                param.setMargins(margin, margin, margin, margin);
                cardB.getRoot().setLayoutParams(param);

                return cardB.getRoot();
            }

            private View createSubjectCardCompact(Student student, Subject sub, int number, MarksRecord record) {
                ItemEvaluationSubjectCardCompactBinding cardB = ItemEvaluationSubjectCardCompactBinding.inflate(
                        LayoutInflater.from(itemView.getContext()), binding.layoutSubjectsHorizontal, false);

                cardB.tvSubjectName.setText(number + ". " + sub.name);

                // Default table values
                String fet = "0", set = "0", tet = "0";
                int formativeMax = 0, summativeMax = 0, totalMax = 0;
                String gradeVal = "—";
                boolean hasMarks = false;

                // Max values calculations
                formativeMax = sub.maxNirikhshan + sub.maxTondiKam + sub.maxPratyakshik + sub.maxUpkram
                        + sub.maxPrakalp + sub.maxChachani + sub.maxSwadhyay + sub.maxItar;
                if (formativeMax == 0) {
                    formativeMax = sub.maxMarks / 2;
                }
                summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
                if (summativeMax == 0) {
                    summativeMax = sub.maxMarks - (sub.maxMarks / 2);
                }
                totalMax = sub.maxMarks;

                String safeKey = MarksRecord.sanitizeKey(sub.name);
                if (record != null && record.detailedMarks != null && record.detailedMarks.containsKey(safeKey)) {
                    MarksRecord.SubjectMarksDetail d = record.detailedMarks.get(safeKey);
                    if (d != null && hasEnteredMarks(d)) {
                        hasMarks = true;
                        fet = formatVal(d.akarikTotal);
                        set = formatVal(d.sanklit);
                        tet = formatVal(d.grandTotal);
                        if (d.grade != null && !d.grade.isEmpty()) {
                            gradeVal = d.grade;
                        }
                    }
                }

                // Set table values (Obtained)
                cardB.tvFETObtained.setText(fet);
                cardB.tvSETObtained.setText(set);
                cardB.tvTETObtained.setText(tet);

                // Set table values (Max)
                cardB.tvFETMax.setText(String.valueOf(formativeMax));
                cardB.tvSETMax.setText(String.valueOf(summativeMax));
                cardB.tvTETMax.setText(String.valueOf(totalMax));

                // Style grade cell
                cardB.tvGrade.setText(gradeVal);

                int gradeColor = 0xFF90A4AE; // gray
                if (gradeVal.startsWith("A-1") || gradeVal.startsWith("अ-1")) {
                    gradeColor = 0xFF6C4CCF;
                } else if (gradeVal.startsWith("A-2") || gradeVal.startsWith("अ-2")) {
                    gradeColor = 0xFF00A5CF;
                } else if (gradeVal.startsWith("B-1") || gradeVal.startsWith("ब-1")) {
                    gradeColor = 0xFF2E7D32;
                } else if (gradeVal.startsWith("B-2") || gradeVal.startsWith("ब-2")) {
                    gradeColor = 0xFF9E9D24;
                } else if (!gradeVal.equals("—")) {
                    gradeColor = 0xFFE53935; // Crimson/Red for E-2 or lower
                }
                cardB.tvGrade.setBackgroundColor(gradeColor);

                // Tint card border orange to signal "needs entry" if no marks are entered yet
                if (hasMarks) {
                    ((com.google.android.material.card.MaterialCardView) cardB.getRoot())
                            .setStrokeColor(0xFFE0E0E0);
                } else {
                    ((com.google.android.material.card.MaterialCardView) cardB.getRoot())
                            .setStrokeColor(0xFFFFB74D);
                }

                // Setup 3-dot popup menu
                cardB.btnSubjectMore.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(
                            itemView.getContext(), v);
                    popup.getMenu().add(0, 1, 0, "Enter Marks");
                    popup.getMenu().add(0, 2, 1, "Quick View Info");
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) {
                            openMarksEntry(student);
                            return true;
                        }
                        Toast.makeText(itemView.getContext(), R.string.msg_info_opened, Toast.LENGTH_SHORT).show();
                        return true;
                    });
                    popup.show();
                });

                // Click on entire subject card opens marks entry!
                cardB.getRoot().setOnClickListener(v -> openMarksEntry(student));

                // Set compact card layout width
                float density = itemView.getResources().getDisplayMetrics().density;
                android.widget.LinearLayout.LayoutParams param = new android.widget.LinearLayout.LayoutParams(
                        (int) (186 * density),
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                int margin = (int) (6 * density);
                param.setMargins(margin, margin, margin, margin);
                cardB.getRoot().setLayoutParams(param);

                return cardB.getRoot();
            }

            private void openMarksEntry(Student student) {
                AppCache.selectedStudent = student;
                // FIX: Always use SessionContext.selectedClass (reflects latest subject
                // toggles).
                // activeClass may be a stale local copy from when the fragment was created.
                // SessionContext.selectedClass is always kept up-to-date by SubjectsFragment.
                ClassModel freshClass = SessionContext.selectedClass != null
                        ? SessionContext.selectedClass
                        : activeClass;
                AppCache.selectedClass = freshClass;
                // Also keep activeClass in sync so the fragment header stays correct
                activeClass = freshClass;
                AppCache.selectedMarks = marksMap.get(student.id);
                Intent intent = new Intent(itemView.getContext(), EnterMarksActivity.class);
                itemView.getContext().startActivity(intent);
            }

            private boolean hasEnteredMarks(MarksRecord.SubjectMarksDetail detail) {
                if (detail == null)
                    return false;
                boolean hasField = detail.nirikhshan > 0 || detail.tondiKam > 0 || detail.pratyakshik > 0
                        || detail.upkram > 0 || detail.prakalp > 0 || detail.chachani > 0
                        || detail.swadhyay > 0 || detail.itar > 0 || detail.tondi > 0
                        || detail.pratyakshikB > 0 || detail.lekhi > 0;
                return hasField && detail.grandTotal > 0;
            }
        }
    }
}
