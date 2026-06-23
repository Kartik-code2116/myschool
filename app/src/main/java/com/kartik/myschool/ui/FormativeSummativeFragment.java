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
import com.kartik.myschool.SingleSubjectMarksDialog;
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
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());

        activeClass = SessionContext.selectedClass;
        if (SessionContext.selectedSemester != null) {
            activeSemesterId = SessionContext.selectedSemester.id;
            activeSemesterNumber = SessionContext.selectedSemester.number > 0 ? SessionContext.selectedSemester.number : 1;
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
        RecyclerView.RecycledViewPool pool = null;
        if (getActivity() instanceof HomeActivity) {
            pool = ((HomeActivity) getActivity()).sharedPool;
        }
        adapter = new EvaluationAdapter(pool);
        b.rvEvaluationStudents.setAdapter(adapter);

        // Play smooth layout enter animation
        com.kartik.myschool.utils.UiAnimations.playLayoutEnter(b.getRoot());
        com.kartik.myschool.utils.UiAnimations.setupRecyclerAnimations(b.rvEvaluationStudents);
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
        String divLabel = activeClass != null && activeClass.division != null && !activeClass.division.isEmpty()
                ? activeClass.division
                : "-";
        b.tvAppSubtitle
                .setText("Cls: " + clsLabel + "-" + divLabel + " • Sem: " + activeSemesterNumber);

        // Outlined button click actions
        b.btnHelpSquare.setOnClickListener(
                v -> com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(requireContext(),
                        "formative_summative"));
        b.btnAddSquare.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
            }
        });
        b.btnCalcSquare.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).navigateTo(R.id.nav_print_report);
            }
        });
    }

    private void setupHeaderStrip() {
        String yr = SessionContext.selectedYear != null ? SessionContext.selectedYear.label : "2025-26";
        String cls = activeClass != null ? activeClass.className : "5";
        String div = activeClass != null && activeClass.division != null && !activeClass.division.isEmpty()
                ? activeClass.division
                : "-";
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
                b.shimmerViewContainer.stopShimmer();
                b.shimmerViewContainer.setVisibility(View.GONE);
                b.tvEmptyState.setVisibility(View.VISIBLE);
                b.tvEmptyState
                        .setText(R.string.msg_no_subjects_configured_ngo_to);
            }
            if (swipeRefresh != null)
                swipeRefresh.setRefreshing(false);
            return;
        }

        // Show progress spinner while fetching
        if (b != null) {
            b.swipeRefreshLayout.setVisibility(View.GONE);
            b.shimmerViewContainer.setVisibility(View.VISIBLE);
            b.shimmerViewContainer.startShimmer();
            b.tvEmptyState.setVisibility(View.GONE);
        }

        // 1. Instant Cache rendering (zero-latency display):
        boolean renderedFromCache = false;
        if (AppCache.cachedStudents != null
                && java.util.Objects.equals(activeClass.id, AppCache.cachedClassIdForStudents)
                && java.util.Objects.equals(activeSemesterId, AppCache.cachedSemesterIdForMarks)) {
            List<Student> cachedList = AppCache.cachedStudents;
            Map<String, MarksRecord> cachedMarks = AppCache.cachedMarksMap != null ? AppCache.cachedMarksMap
                    : new HashMap<>();

            // Render instantly from cache after a brief delay to let slide animation start smoothly
            renderedFromCache = true;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && b != null) {
                    if (b != null) {
                        b.shimmerViewContainer.stopShimmer();
                        b.shimmerViewContainer.setVisibility(View.GONE);
                        b.swipeRefreshLayout.setVisibility(View.VISIBLE);
                    }
                    if (swipeRefresh != null)
                        swipeRefresh.setRefreshing(false);
                    if (activeClass != null && activeClass.subjects != null) {
                        com.kartik.myschool.model.Subject.sortSubjects(activeClass.subjects);
                    }
                    adapter.setData(cachedList, cachedMarks, true); // Reset animation for the first instant render
                }
            }, 200);
        }
        
        final boolean finalRenderedFromCache = renderedFromCache;

        // 2. Background fetch (parallel to cut loading time in half):
        final boolean[] fetchesDone = new boolean[2]; // [0] = students, [1] = marks
        final List<Student>[] studentsResult = new List[]{null};
        final Map<String, MarksRecord>[] marksResult = new Map[]{null};

        Runnable onBothFetchesDone = () -> {
            if (!fetchesDone[0] || !fetchesDone[1]) return;
            
            List<Student> finalList = studentsResult[0] != null ? studentsResult[0] : new ArrayList<>();
            Map<String, MarksRecord> finalMarks = marksResult[0] != null ? marksResult[0] : new HashMap<>();

            // Merge network results with the fresh cache based on updatedAt
            if (AppCache.cachedMarksMap != null
                    && java.util.Objects.equals(activeSemesterId, AppCache.cachedSemesterIdForMarks)
                    && java.util.Objects.equals(activeClass.id, AppCache.cachedClassIdForStudents)) {
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
                b.shimmerViewContainer.stopShimmer();
                b.shimmerViewContainer.setVisibility(View.GONE);
                b.swipeRefreshLayout.setVisibility(View.VISIBLE);
                if (swipeRefresh != null)
                    swipeRefresh.setRefreshing(false);
                if (finalList.isEmpty()) {
                    b.tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    b.tvEmptyState.setVisibility(View.GONE);
                }
                
                // If we already rendered from cache, do NOT reset the animation, so the UI doesn't visually jump/stutter.
                adapter.setData(finalList, finalMarks, !finalRenderedFromCache);
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
                
                if (isAdded() && b != null) {
                    if (AppCache.cachedStudents == null || activeClass == null || !java.util.Objects.equals(activeClass.id, AppCache.cachedClassIdForStudents)) {
                        String errMsg = (e != null && e.getMessage() != null) ? e.getMessage() : "Unknown error";
                        Toast.makeText(requireContext(), "Failed to load students: " + errMsg, Toast.LENGTH_SHORT).show();
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
                Log.e("FORMATIVE", "getMarksForClassAndSemester failed: " + e.getMessage(), e);
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
            activeSemesterNumber = SessionContext.selectedSemester.number > 0 ? SessionContext.selectedSemester.number : 1;
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
            AppCache.marksJustSaved = false;
            AppCache.marksJustSavedStudentId = null;
            AppCache.marksJustSavedRecord = null;

            // Clear selectedMarks so it doesn't bleed into other students
            AppCache.selectedMarks = null;
        }

        boolean isFirstLoad = lastLoadedClassId == null || lastLoadedSemesterId == null;
        boolean classChanged = activeClass != null && !java.util.Objects.equals(activeClass.id, lastLoadedClassId);
        boolean semesterChanged = !java.util.Objects.equals(activeSemesterId, lastLoadedSemesterId);

        if (isFirstLoad || classChanged || semesterChanged) {
            b.swipeRefreshLayout.setVisibility(View.GONE);
            b.shimmerViewContainer.setVisibility(View.VISIBLE);
            b.shimmerViewContainer.startShimmer();
            loadEvaluationData();
        } else {
            // Always refresh adapter if coming back from Subjects page with modified subjects
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
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
        private final int[] lastPosition = new int[] { -1 };
        private final RecyclerView.RecycledViewPool viewPool;

        public EvaluationAdapter(RecyclerView.RecycledViewPool sharedPool) {
            this.viewPool = sharedPool != null ? sharedPool : new RecyclerView.RecycledViewPool();
            this.viewPool.setMaxRecycledViews(0, 50);
            this.viewPool.setMaxRecycledViews(1, 50);
        }

        public void setData(List<Student> list, Map<String, MarksRecord> map, boolean resetAnimation) {
            final List<Student> newStudents = new ArrayList<>(list);
            final Map<String, MarksRecord> newMarks = new HashMap<>(map);

            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                this.students.clear();
                this.students.addAll(newStudents);
                this.marksMap.clear();
                this.marksMap.putAll(newMarks);
                if (resetAnimation) {
                    lastPosition[0] = -1;
                }
                notifyDataSetChanged();
            });
        }

        public void patchStudentMarks(String studentId, MarksRecord record) {
            marksMap.put(studentId, record);
            if (AppCache.cachedMarksMap == null
                    || !java.util.Objects.equals(activeClass.id, AppCache.cachedClassIdForStudents)
                    || !java.util.Objects.equals(activeSemesterId, AppCache.cachedSemesterIdForMarks)) {
                AppCache.cachedMarksMap = new HashMap<>();
                AppCache.cachedClassIdForStudents = activeClass.id;
                AppCache.cachedSemesterIdForMarks = activeSemesterId;
            }
            AppCache.cachedMarksMap.put(studentId, record);
            for (int i = 0; i < students.size(); i++) {
                if (java.util.Objects.equals(studentId, students.get(i).id)) {
                    final int pos = i;
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .post(() -> notifyItemChanged(pos));
                    return;
                }
            }
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
            com.kartik.myschool.utils.UiAnimations.animateScrollReveal(holder.itemView, position, lastPosition);
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemEvaluationStudentBlockBinding binding;
            private final SubjectInnerAdapter innerAdapter;

            public ViewHolder(ItemEvaluationStudentBlockBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                binding.rvSubjectsHorizontal.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
                binding.rvSubjectsHorizontal.setRecycledViewPool(viewPool);
                binding.rvSubjectsHorizontal.setHasFixedSize(true);
                binding.rvSubjectsHorizontal.setNestedScrollingEnabled(false);

                innerAdapter = new SubjectInnerAdapter(null, new ArrayList<>(), null, isGridView, this);
                binding.rvSubjectsHorizontal.setAdapter(innerAdapter);
            }

            public void bind(Student s, int index) {
                binding.tvStudentName.setText(index + ". " + s.name);
                binding.btnStudentMore.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(
                            itemView.getContext(), v);
                    popup.getMenu().add(0, 1, 0, "Enter Marks");
                    popup.getMenu().add(0, 3, 1, "Edit Marks of Box");
                    popup.getMenu().add(0, 2, 2, "Quick View Info");
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) {
                            openMarksEntry(s);
                            return true;
                        } else if (item.getItemId() == 3) {
                            Toast.makeText(itemView.getContext(), "Please select a specific subject's menu to edit its box", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        Toast.makeText(itemView.getContext(), R.string.msg_info_opened, Toast.LENGTH_SHORT).show();
                        return true;
                    });
                    popup.show();
                });

                MarksRecord marks = marksMap.get(s.id);

                List<String> gradesToDisplay = new ArrayList<>();
                if (marks != null && marks.detailedMarks != null && activeClass.subjects != null) {
                    for (Subject sub : activeClass.subjects) {
                        String sk = MarksRecord.sanitizeKey(sub.name);
                        MarksRecord.SubjectMarksDetail detail = marks.detailedMarks.get(sk);
                        if (detail == null) detail = marks.detailedMarks.get(sub.name);
                        if (detail == null) {
                            for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> e : marks.detailedMarks.entrySet()) {
                                if (e.getKey() != null && MarksRecord.sanitizeKey(e.getKey()).equals(sk)) { detail = e.getValue(); break; }
                            }
                        }
                        if (detail == null) {
                            for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> e : marks.detailedMarks.entrySet()) {
                                if (e.getKey() != null && com.kartik.myschool.model.Subject.isSameSubject(e.getKey(), sub.name)) { detail = e.getValue(); break; }
                            }
                        }
                        if (detail != null && detail.grade != null && !detail.grade.isEmpty()
                                && hasEnteredMarks(detail)) {
                            gradesToDisplay.add(detail.grade);
                        }
                    }
                }

                while (binding.layoutGradeChips.getChildCount() < gradesToDisplay.size()) {
                    binding.layoutGradeChips.addView(createGradeChipBase());
                }
                while (binding.layoutGradeChips.getChildCount() > gradesToDisplay.size()) {
                    binding.layoutGradeChips.removeViewAt(binding.layoutGradeChips.getChildCount() - 1);
                }

                for (int i = 0; i < gradesToDisplay.size(); i++) {
                    TextView tv = (TextView) binding.layoutGradeChips.getChildAt(i);
                    bindGradeChip(tv, gradesToDisplay.get(i));
                }

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

                binding.layoutGradeChips.setVisibility(isGridView ? View.GONE : View.VISIBLE);
                binding.rvSubjectsHorizontal.setVisibility(View.VISIBLE);
                binding.layoutSummaryGrid.setVisibility(View.GONE);

                if (activeClass != null && activeClass.subjects != null) {
                    innerAdapter.updateData(s, activeClass.subjects, marks, isGridView);
                } else {
                    innerAdapter.updateData(s, new ArrayList<>(), null, isGridView);
                }
            }

            private TextView createGradeChipBase() {
                TextView tv = new TextView(itemView.getContext());
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
                tv.setGravity(android.view.Gravity.CENTER);
                float density = itemView.getResources().getDisplayMetrics().density;
                tv.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
                tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMarginEnd((int) (6 * density));
                tv.setLayoutParams(lp);
                return tv;
            }

            private void bindGradeChip(TextView tv, String grade) {
                tv.setText(grade);
                float density = itemView.getResources().getDisplayMetrics().density;
                int textColor = 0xFF6C4CCF;
                int borderColor = 0xFFE1D5FF;
                int bgColor = 0xFFF3EEFF;

                if (grade.startsWith("A-1") || grade.startsWith("अ-1")) {
                    textColor = 0xFF6C4CCF; borderColor = 0xFFD7C4FF; bgColor = 0xFFF3EEFF;
                } else if (grade.startsWith("A-2") || grade.startsWith("अ-2")) {
                    textColor = 0xFF00A5CF; borderColor = 0xFFB2E7F5; bgColor = 0xFFE0F7FA;
                } else if (grade.startsWith("B-1") || grade.startsWith("ब-1")) {
                    textColor = 0xFF2E7D32; borderColor = 0xFFC8E6C9; bgColor = 0xFFE8F5E9;
                } else if (grade.startsWith("B-2") || grade.startsWith("ब-2")) {
                    textColor = 0xFF9E9D24; borderColor = 0xFFF0F4C3; bgColor = 0xFFF9FBE7;
                } else {
                    textColor = 0xFFE65100; borderColor = 0xFFFFE0B2; bgColor = 0xFFFFF3E0;
                }

                tv.setTextColor(textColor);
                android.graphics.drawable.Drawable background = tv.getBackground();
                GradientDrawable gd;
                if (background instanceof GradientDrawable) {
                    gd = (GradientDrawable) background;
                } else {
                    gd = new GradientDrawable();
                    tv.setBackground(gd);
                }
                gd.setColor(bgColor);
                gd.setCornerRadius(6 * density);
                gd.setStroke((int) (1 * density), borderColor);
            }

            public void openSingleSubjectMarks(Student student, Subject subject) {
                MarksRecord record = marksMap.get(student.id);
                ClassModel freshClass = SessionContext.selectedClass != null
                        ? SessionContext.selectedClass
                        : activeClass;

                SingleSubjectMarksDialog dialog = SingleSubjectMarksDialog.newInstance(
                        student, subject, freshClass, record);
                dialog.setOnMarksSavedListener((studentId, newRecord) -> {
                    patchStudentMarks(studentId, newRecord);
                });
                dialog.show(FormativeSummativeFragment.this.getChildFragmentManager(), "SingleSubjectMarksDialog");
            }

            private void openMarksEntry(Student student) {
                AppCache.selectedStudent = student;
                ClassModel freshClass = SessionContext.selectedClass != null
                        ? SessionContext.selectedClass
                        : activeClass;
                AppCache.selectedClass = freshClass;
                activeClass = freshClass;
                AppCache.selectedMarks = marksMap.get(student.id);
                Intent intent = new Intent(itemView.getContext(), EnterMarksActivity.class);
                itemView.getContext().startActivity(intent);
            }
        }
    }

    public static boolean hasEnteredMarks(MarksRecord.SubjectMarksDetail detail) {
        if (detail == null) return false;
        boolean hasField = detail.nirikhshan > 0 || detail.tondiKam > 0 || detail.pratyakshik > 0
                || detail.upkram > 0 || detail.prakalp > 0 || detail.chachani > 0
                || detail.swadhyay > 0 || detail.itar > 0 || detail.tondi > 0
                || detail.pratyakshikB > 0 || detail.lekhi > 0;
        return hasField && detail.grandTotal > 0;
    }

    private static class SubjectInnerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private Student student;
        private List<Subject> subjects;
        private MarksRecord marks;
        private boolean isGridView;
        private final EvaluationAdapter.ViewHolder parentHolder;

        public SubjectInnerAdapter(Student student, List<Subject> subjects, MarksRecord marks, boolean isGridView, EvaluationAdapter.ViewHolder parentHolder) {
            this.student = student;
            this.subjects = subjects;
            this.marks = marks;
            this.isGridView = isGridView;
            this.parentHolder = parentHolder;
        }

        public boolean isGridView() { return isGridView; }

        public void updateData(Student student, List<Subject> subjects, MarksRecord marks, boolean isGridView) {
            boolean subjectsChanged = this.subjects == null || subjects == null || !this.subjects.equals(subjects);
            boolean studentChanged = this.student == null || student == null || !this.student.id.equals(student.id);
            boolean marksChanged = this.marks != marks && (this.marks == null || marks == null || this.marks.updatedAt != marks.updatedAt);
            boolean gridChanged = this.isGridView != isGridView;

            this.student = student;
            this.subjects = subjects;
            this.marks = marks;
            this.isGridView = isGridView;

            if (subjectsChanged || studentChanged || marksChanged || gridChanged) {
                notifyDataSetChanged();
            }
        }

        @Override
        public int getItemViewType(int position) {
            return isGridView ? 1 : 0;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            float density = parent.getResources().getDisplayMetrics().density;
            if (viewType == 1) {
                ItemEvaluationSubjectCardCompactBinding binding = ItemEvaluationSubjectCardCompactBinding.inflate(inflater, parent, false);
                RecyclerView.LayoutParams param = new RecyclerView.LayoutParams((int) (186 * density), RecyclerView.LayoutParams.WRAP_CONTENT);
                int margin = (int) (6 * density);
                param.setMargins(margin, margin, margin, margin);
                binding.getRoot().setLayoutParams(param);
                return new CompactViewHolder(binding);
            } else {
                ItemEvaluationSubjectCardBinding binding = ItemEvaluationSubjectCardBinding.inflate(inflater, parent, false);
                RecyclerView.LayoutParams param = new RecyclerView.LayoutParams((int) (300 * density), RecyclerView.LayoutParams.WRAP_CONTENT);
                int margin = (int) (6 * density);
                param.setMargins(margin, margin, margin, margin);
                binding.getRoot().setLayoutParams(param);
                return new NormalViewHolder(binding);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Subject sub = subjects.get(position);
            if (holder instanceof CompactViewHolder) {
                ((CompactViewHolder) holder).bind(student, sub, position + 1, marks, parentHolder);
            } else if (holder instanceof NormalViewHolder) {
                ((NormalViewHolder) holder).bind(student, sub, position + 1, marks, parentHolder);
            }
        }

        @Override
        public int getItemCount() {
            return subjects != null ? subjects.size() : 0;
        }

        class NormalViewHolder extends RecyclerView.ViewHolder {
            ItemEvaluationSubjectCardBinding cardB;
            public NormalViewHolder(ItemEvaluationSubjectCardBinding binding) {
                super(binding.getRoot());
                this.cardB = binding;
            }
            public void bind(Student student, Subject sub, int number, MarksRecord record, EvaluationAdapter.ViewHolder parentHolder) {
                cardB.tvSubjectName.setText(number + ". " + com.kartik.myschool.utils.pdf.PdfLocalizer.translateSubject(itemView.getContext(), sub.name));
                String fe1 = "0", fe2 = "0", fe3 = "0", fe4 = "0", fe5 = "0", fe6 = "0", fe7 = "0", fe8 = "0";
                String se1 = "0", se2 = "0", se3 = "0", fet = "0", set = "0", tet = "0", gradeVal = "—";
                boolean hasMarks = false;

                String safeKey = MarksRecord.sanitizeKey(sub.name);
                MarksRecord.SubjectMarksDetail d = null;
                if (record != null && record.detailedMarks != null) {
                    d = record.detailedMarks.get(safeKey);
                    if (d == null) d = record.detailedMarks.get(sub.name);
                    if (d == null) {
                        for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> e : record.detailedMarks.entrySet()) {
                            if (e.getKey() != null && MarksRecord.sanitizeKey(e.getKey()).equals(safeKey)) { d = e.getValue(); break; }
                        }
                    }
                    if (d == null) {
                        for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> e : record.detailedMarks.entrySet()) {
                            if (e.getKey() != null && com.kartik.myschool.model.Subject.isSameSubject(e.getKey(), sub.name)) { d = e.getValue(); break; }
                        }
                    }
                }
                if (d != null && hasEnteredMarks(d)) {
                    hasMarks = true;
                    fe1 = String.valueOf(d.nirikhshan); fe2 = String.valueOf(d.tondiKam);
                    fe3 = String.valueOf(d.pratyakshik); fe4 = String.valueOf(d.upkram);
                    fe5 = String.valueOf(d.prakalp); fe6 = String.valueOf(d.chachani);
                    fe7 = String.valueOf(d.swadhyay); fe8 = String.valueOf(d.itar);
                    se1 = String.valueOf(d.tondi); se2 = String.valueOf(d.pratyakshikB); se3 = String.valueOf(d.lekhi);
                    fet = String.valueOf(d.akarikTotal); set = String.valueOf(d.sanklit); tet = String.valueOf(d.grandTotal);
                    if (d.grade != null && !d.grade.isEmpty()) gradeVal = d.grade;
                }


                cardB.tvFE1.setText(fe1); cardB.tvFE2.setText(fe2); cardB.tvFE3.setText(fe3); cardB.tvFE4.setText(fe4);
                cardB.tvFE5.setText(fe5); cardB.tvFE6.setText(fe6); cardB.tvFE7.setText(fe7); cardB.tvFE8.setText(fe8);
                cardB.tvSE1.setText(se1); cardB.tvSE2.setText(se2); cardB.tvSE3.setText(se3);
                cardB.tvFET.setText(fet); cardB.tvSET.setText(set); cardB.tvTET.setText(tet);

                if (hasMarks) {
                    ((com.google.android.material.card.MaterialCardView) cardB.getRoot()).setStrokeColor(0xFFE0E0E0);
                } else {
                    ((com.google.android.material.card.MaterialCardView) cardB.getRoot()).setStrokeColor(0xFFFFB74D);
                }

                float density = itemView.getResources().getDisplayMetrics().density;
                int gradeColor = 0xFF90A4AE;
                if (gradeVal.startsWith("A-1") || gradeVal.startsWith("अ-1")) gradeColor = 0xFF6C4CCF;
                else if (gradeVal.startsWith("A-2") || gradeVal.startsWith("अ-2")) gradeColor = 0xFF00A5CF;
                else if (gradeVal.startsWith("B-1") || gradeVal.startsWith("ब-1")) gradeColor = 0xFF2E7D32;
                else if (gradeVal.startsWith("B-2") || gradeVal.startsWith("ब-2")) gradeColor = 0xFF9E9D24;
                else if (!gradeVal.equals("—")) gradeColor = 0xFFE65100;

                cardB.tvHeaderGrade.setText(gradeVal);
                if (gradeVal.equals("—")) {
                    cardB.tvHeaderGrade.setVisibility(View.GONE);
                } else {
                    cardB.tvHeaderGrade.setVisibility(View.VISIBLE);
                    android.graphics.drawable.Drawable background = cardB.tvHeaderGrade.getBackground();
                    GradientDrawable gd;
                    if (background instanceof GradientDrawable) {
                        gd = (GradientDrawable) background;
                    } else {
                        gd = new GradientDrawable();
                        cardB.tvHeaderGrade.setBackground(gd);
                    }
                    gd.setColor(gradeColor);
                    gd.setCornerRadius(6 * density);
                }

                cardB.btnSubjectMore.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(itemView.getContext(), v);
                    popup.getMenu().add(0, 1, 0, "Enter Marks");
                    popup.getMenu().add(0, 3, 1, "Edit Marks of Box");
                    popup.getMenu().add(0, 2, 2, "Quick View Info");
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) {
                            // Using reflection-like or direct call if we make openMarksEntry package-private
                            try {
                                java.lang.reflect.Method m = parentHolder.getClass().getDeclaredMethod("openMarksEntry", Student.class);
                                m.setAccessible(true);
                                m.invoke(parentHolder, student);
                            } catch (Exception e) {}
                            return true;
                        } else if (item.getItemId() == 3) {
                            parentHolder.openSingleSubjectMarks(student, sub);
                            return true;
                        }
                        Toast.makeText(itemView.getContext(), R.string.msg_info_opened, Toast.LENGTH_SHORT).show();
                        return true;
                    });
                    popup.show();
                });

                cardB.getRoot().setOnClickListener(v -> parentHolder.openSingleSubjectMarks(student, sub));
            }
        }

        class CompactViewHolder extends RecyclerView.ViewHolder {
            ItemEvaluationSubjectCardCompactBinding cardB;
            public CompactViewHolder(ItemEvaluationSubjectCardCompactBinding binding) {
                super(binding.getRoot());
                this.cardB = binding;
            }
            public void bind(Student student, Subject sub, int number, MarksRecord record, EvaluationAdapter.ViewHolder parentHolder) {
                cardB.tvSubjectName.setText(number + ". " + com.kartik.myschool.utils.pdf.PdfLocalizer.translateSubject(itemView.getContext(), sub.name));

                String fet = "0", set = "0", tet = "0", gradeVal = "—";
                int formativeMax = sub.maxNirikhshan + sub.maxTondiKam + sub.maxPratyakshik + sub.maxUpkram + sub.maxPrakalp + sub.maxChachani + sub.maxSwadhyay + sub.maxItar;
                int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
                boolean hasMarks = false;

                if (formativeMax == 0 && summativeMax == 0) {
                    formativeMax = sub.maxMarks / 2;
                    summativeMax = sub.maxMarks - formativeMax;
                    if (com.kartik.myschool.model.Subject.isNonAcademic(sub.name)) {
                        formativeMax = sub.maxMarks;
                        summativeMax = 0;
                    }
                }
                int totalMax = sub.maxMarks;

                String safeKey = MarksRecord.sanitizeKey(sub.name);
                MarksRecord.SubjectMarksDetail d = null;
                if (record != null && record.detailedMarks != null) {
                    d = record.detailedMarks.get(safeKey);
                    if (d == null) d = record.detailedMarks.get(sub.name);
                    if (d == null) {
                        for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> e : record.detailedMarks.entrySet()) {
                            if (e.getKey() != null && MarksRecord.sanitizeKey(e.getKey()).equals(safeKey)) { d = e.getValue(); break; }
                        }
                    }
                    if (d == null) {
                        for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> e : record.detailedMarks.entrySet()) {
                            if (e.getKey() != null && com.kartik.myschool.model.Subject.isSameSubject(e.getKey(), sub.name)) { d = e.getValue(); break; }
                        }
                    }
                }
                if (d != null && hasEnteredMarks(d)) {
                    hasMarks = true;
                    fet = String.valueOf(d.akarikTotal);
                    set = String.valueOf(d.sanklit);
                    tet = String.valueOf(d.grandTotal);
                    if (d.grade != null && !d.grade.isEmpty()) gradeVal = d.grade;
                }


                cardB.tvFETObtained.setText(fet); cardB.tvSETObtained.setText(set); cardB.tvTETObtained.setText(tet);
                cardB.tvFETMax.setText(String.valueOf(formativeMax)); cardB.tvSETMax.setText(String.valueOf(summativeMax)); cardB.tvTETMax.setText(String.valueOf(totalMax));
                cardB.tvGrade.setText(gradeVal);

                int gradeColor = 0xFF90A4AE;
                if (gradeVal.startsWith("A-1") || gradeVal.startsWith("अ-1")) gradeColor = 0xFF6C4CCF;
                else if (gradeVal.startsWith("A-2") || gradeVal.startsWith("अ-2")) gradeColor = 0xFF00A5CF;
                else if (gradeVal.startsWith("B-1") || gradeVal.startsWith("ब-1")) gradeColor = 0xFF2E7D32;
                else if (gradeVal.startsWith("B-2") || gradeVal.startsWith("ब-2")) gradeColor = 0xFF9E9D24;
                else if (!gradeVal.equals("—")) gradeColor = 0xFFE53935;
                cardB.tvGrade.setBackgroundColor(gradeColor);

                if (hasMarks) {
                    ((com.google.android.material.card.MaterialCardView) cardB.getRoot()).setStrokeColor(0xFFE0E0E0);
                } else {
                    ((com.google.android.material.card.MaterialCardView) cardB.getRoot()).setStrokeColor(0xFFFFB74D);
                }

                cardB.btnSubjectMore.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(itemView.getContext(), v);
                    popup.getMenu().add(0, 1, 0, "Enter Marks");
                    popup.getMenu().add(0, 3, 1, "Edit Marks of Box");
                    popup.getMenu().add(0, 2, 2, "Quick View Info");
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) {
                            try {
                                java.lang.reflect.Method m = parentHolder.getClass().getDeclaredMethod("openMarksEntry", Student.class);
                                m.setAccessible(true);
                                m.invoke(parentHolder, student);
                            } catch (Exception e) {}
                            return true;
                        } else if (item.getItemId() == 3) {
                            parentHolder.openSingleSubjectMarks(student, sub);
                            return true;
                        }
                        Toast.makeText(itemView.getContext(), R.string.msg_info_opened, Toast.LENGTH_SHORT).show();
                        return true;
                    });
                    popup.show();
                });

                cardB.getRoot().setOnClickListener(v -> parentHolder.openSingleSubjectMarks(student, sub));
            }
        }
    }
}
