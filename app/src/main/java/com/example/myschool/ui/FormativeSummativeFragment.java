package com.example.myschool.ui;

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

import com.example.myschool.AppCache;
import com.example.myschool.EnterMarksActivity;
import com.example.myschool.HomeActivity;
import com.example.myschool.R;
import com.example.myschool.SessionContext;
import com.example.myschool.databinding.FragmentFormativeSummativeBinding;
import com.example.myschool.databinding.ItemEvaluationStudentBlockBinding;
import com.example.myschool.databinding.ItemEvaluationSubjectCardBinding;
import com.example.myschool.databinding.ItemSubjectMarksRowBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.Student;
import com.example.myschool.model.Subject;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.GradeCalculator;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        updateLayoutManager();
        adapter = new EvaluationAdapter();
        b.rvEvaluationStudents.setAdapter(adapter);
    }

    private void updateLayoutManager() {
        if (isGridView) {
            b.rvEvaluationStudents.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2));
        } else {
            b.rvEvaluationStudents.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
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
        b.tvAppSubtitle.setText("• Class: " + clsLabel + " • Div: " + divLabel + " • Semester: " + activeSemesterNumber);

        // Outlined button click actions
        b.btnHelpSquare.setOnClickListener(v -> Toast.makeText(requireContext(), "Evaluation Help Manual opened!", Toast.LENGTH_SHORT).show());
        b.btnAddSquare.setOnClickListener(v -> Toast.makeText(requireContext(), "Add student clicked", Toast.LENGTH_SHORT).show());
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
        b.tvHeaderStripInfo.setText("Year: " + yr + "  Class: " + cls + ", Div: " + div + ", Sem: " + activeSemesterNumber);

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

        // Fallback default subjects if empty
        if (activeClass.subjects == null || activeClass.subjects.isEmpty()) {
            activeClass.subjects = new ArrayList<>();
            activeClass.subjects.add(new Subject("English", 100));
            activeClass.subjects.add(new Subject("Mathematics", 100));
            activeClass.subjects.add(new Subject("Science", 100));
            activeClass.subjects.add(new Subject("Marathi", 100));
        }

        // Show progress spinner while fetching
        if (b != null) {
            b.progressLoading.setVisibility(View.VISIBLE);
            b.tvEmptyState.setVisibility(View.GONE);
        }

        // 1. Instant Cache rendering (zero-latency display):
        if (AppCache.cachedStudents != null
                && activeClass.id.equals(AppCache.cachedClassIdForStudents)) {
            List<Student> cachedList = AppCache.cachedStudents;
            Map<String, MarksRecord> cachedMarks = AppCache.cachedMarksMap != null ? AppCache.cachedMarksMap : new HashMap<>();
            
            // Render instantly from cache and hide progress
            if (b != null) b.progressLoading.setVisibility(View.GONE);
            adapter.setData(cachedList, cachedMarks);
        }

        // 2. Background fetch (always runs to get fresh data):
        FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                if (students == null) students = new ArrayList<>();
                List<Student> finalList = students;
                
                // Fetch marks map
                FirebaseRepository.get().getMarksForClassAndSemester(activeClass.id, activeSemesterId,
                        new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                            @Override
                            public void onSuccess(Map<String, MarksRecord> marksMap) {
                                Map<String, MarksRecord> finalMarks = marksMap != null ? marksMap : new HashMap<>();
                                
                                // Update cache
                                AppCache.cachedStudents = finalList;
                                AppCache.cachedMarksMap = finalMarks;
                                AppCache.cachedClassIdForStudents = activeClass.id;
                                AppCache.cachedSemesterIdForMarks = activeSemesterId;

                                if (isAdded() && b != null) {
                                    b.progressLoading.setVisibility(View.GONE);
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
                                Log.e("FORMATIVE", "getMarksForClassAndSemester failed: " + e.getMessage(), e);
                                if (isAdded() && b != null) {
                                    b.progressLoading.setVisibility(View.GONE);
                                    // FIX: only fallback to empty marks when there is truly no
                                    // previously-cached data. Never wipe marks just because of a
                                    // transient network error on resume.
                                    boolean hasCachedMarks = AppCache.cachedMarksMap != null
                                            && activeClass.id.equals(AppCache.cachedClassIdForStudents)
                                            && activeSemesterId.equals(AppCache.cachedSemesterIdForMarks);
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
                    // Only show network error toast if cache is fully empty
                    if (AppCache.cachedStudents == null || !activeClass.id.equals(AppCache.cachedClassIdForStudents)) {
                        Toast.makeText(requireContext(), "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // FIX: Re-read session context on every resume so that class/semester changes
        // (e.g. user pressed back and switched class) are always reflected here.
        if (SessionContext.selectedClass != null) {
            activeClass = SessionContext.selectedClass;
        }
        
        // Smart fallback if semester is null
        if (SessionContext.selectedSemester == null && activeClass != null && activeClass.semesterId != null && !activeClass.semesterId.isEmpty()) {
            com.example.myschool.model.Semester fallbackSem = new com.example.myschool.model.Semester();
            fallbackSem.id = activeClass.semesterId;
            fallbackSem.yearId = activeClass.yearId;
            fallbackSem.number = activeClass.semesterId.contains("2") ? 2 : 1;
            fallbackSem.name = activeClass.semesterId.contains("2") ? "Second Semester" : "First Semester";
            SessionContext.selectedSemester = fallbackSem;
        }

        if (SessionContext.selectedSemester != null) {
            activeSemesterId = SessionContext.selectedSemester.id;
            activeSemesterNumber = SessionContext.selectedSemester.number;
        }

        // Dynamically update toolbar and header strip text views on resume
        setupCustomAppBar();
        setupHeaderStrip();

        if (getActivity() instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) getActivity();
            View activityAppBar = activity.findViewById(R.id.appBarLayout);
            if (activityAppBar != null) {
                activityAppBar.setVisibility(View.GONE);
            }

            // Fix CoordinatorLayout scrolling behavior offset bug:
            View navHost = activity.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                        (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost.getLayoutParams();
                params.setBehavior(null);
                float density = getResources().getDisplayMetrics().density;
                params.bottomMargin = (int) (64 * density);
                navHost.setLayoutParams(params);
            }
        }
        loadEvaluationData();
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
            if (navHost != null && navHost.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                        (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost.getLayoutParams();
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
    //  RECYCLERVIEW ADAPTER
    // ════════════════════════════════════════════════════════════════════════════
    private class EvaluationAdapter extends RecyclerView.Adapter<EvaluationAdapter.ViewHolder> {

        private final List<Student> students = new ArrayList<>();
        private final Map<String, MarksRecord> marksMap = new HashMap<>();

        public void setData(List<Student> list, Map<String, MarksRecord> map) {
            students.clear();
            students.addAll(list);
            marksMap.clear();
            marksMap.putAll(map);
            notifyDataSetChanged();
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
                binding.btnStudentMore.setOnClickListener(v -> Toast.makeText(itemView.getContext(), "Options for " + s.name, Toast.LENGTH_SHORT).show());

                MarksRecord marks = marksMap.get(s.id);

                // Build Grade Chips Row
                binding.layoutGradeChips.removeAllViews();
                if (marks != null && marks.detailedMarks != null && activeClass.subjects != null) {
                    for (Subject sub : activeClass.subjects) {
                        MarksRecord.SubjectMarksDetail detail = marks.detailedMarks.get(sub.name);
                        if (detail != null && detail.grade != null && !detail.grade.isEmpty()) {
                            binding.layoutGradeChips.addView(createGradeChip(detail.grade));
                        }
                    }
                }

                // Adjust card margins depending on layout mode
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.getRoot().getLayoutParams();
                float density = itemView.getResources().getDisplayMetrics().density;
                if (isGridView) {
                    lp.setMarginStart((int) (6 * density));
                    lp.setMarginEnd((int) (6 * density));
                    lp.bottomMargin = (int) (10 * density);
                } else {
                    lp.setMarginStart((int) (14 * density));
                    lp.setMarginEnd((int) (14 * density));
                    lp.bottomMargin = (int) (16 * density);
                }
                binding.getRoot().setLayoutParams(lp);

                // Build Subject Cards depending on layout mode
                if (isGridView) {
                    binding.scrollSubjects.setVisibility(View.GONE);
                } else {
                    binding.scrollSubjects.setVisibility(View.VISIBLE);
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

            private View createGradeChip(String grade) {
                TextView tv = new TextView(itemView.getContext());
                tv.setText(grade);
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
                tv.setGravity(android.view.Gravity.CENTER);
                float density = getResources().getDisplayMetrics().density;
                tv.setPadding((int)(8 * density), (int)(3 * density), (int)(8 * density), (int)(3 * density));
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
                gd.setStroke((int)(1 * density), borderColor);
                tv.setBackground(gd);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMarginEnd((int)(6 * density));
                tv.setLayoutParams(lp);

                return tv;
            }

            private View createSubjectCard(Student student, Subject sub, int number, MarksRecord record) {
                ItemSubjectMarksRowBinding dummy; // layout ref
                ItemEvaluationSubjectCardBinding cardB = ItemEvaluationSubjectCardBinding.inflate(
                        LayoutInflater.from(itemView.getContext()), binding.layoutSubjectsHorizontal, false);

                cardB.tvSubjectName.setText(number + ". " + sub.name);

                // Fetch details
                int formativeObt = 0;
                int summativeObt = 0;
                int totalObt = 0;
                String gradeVal = "—";

                if (record != null && record.detailedMarks != null && record.detailedMarks.containsKey(sub.name)) {
                    MarksRecord.SubjectMarksDetail d = record.detailedMarks.get(sub.name);
                    if (d != null) {
                        formativeObt = d.akarikTotal;
                        summativeObt = d.sanklit;
                        totalObt = d.grandTotal;
                        gradeVal = d.grade != null && !d.grade.isEmpty() ? d.grade : "—";
                    }
                }

                cardB.tvObtainedFET.setText(String.valueOf(formativeObt));
                cardB.tvObtainedSET.setText(String.valueOf(summativeObt));
                cardB.tvObtainedTET.setText(String.valueOf(totalObt));
                cardB.tvSubjectGrade.setText(gradeVal);

                // Style dynamic color block
                int gradeColor = 0xFF90A4AE; // gray
                if (gradeVal.startsWith("A-1") || gradeVal.startsWith("अ-1")) gradeColor = 0xFF6C4CCF;
                else if (gradeVal.startsWith("A-2") || gradeVal.startsWith("अ-2")) gradeColor = 0xFF00A5CF;
                else if (gradeVal.startsWith("B-1") || gradeVal.startsWith("ब-1")) gradeColor = 0xFF2E7D32;
                else if (gradeVal.startsWith("B-2") || gradeVal.startsWith("ब-2")) gradeColor = 0xFF9E9D24;
                else if (!gradeVal.equals("—")) gradeColor = 0xFFE65100;

                cardB.layoutGradeBlock.setBackgroundColor(gradeColor);

                // Setup 3-dot popup menu
                cardB.btnSubjectMore.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(itemView.getContext(), v);
                    popup.getMenu().add(0, 1, 0, "Enter Marks");
                    popup.getMenu().add(0, 2, 1, "Quick View Info");
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) {
                            openMarksEntry(student);
                            return true;
                        }
                        Toast.makeText(itemView.getContext(), "Info opened", Toast.LENGTH_SHORT).show();
                        return true;
                    });
                    popup.show();
                });

                // JOIN TO ENTER MARKS PAGE: Click on the entire subject card opens marks entry!
                cardB.getRoot().setOnClickListener(v -> openMarksEntry(student));

                // Set consistent layout width and margins for horizontal scrolling
                float density = itemView.getResources().getDisplayMetrics().density;
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        (int) (190 * density),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                int margin = (int) (6 * density);
                param.setMargins(margin, margin, margin, margin);
                cardB.getRoot().setLayoutParams(param);

                return cardB.getRoot();
            }

            private void openMarksEntry(Student student) {
                AppCache.selectedStudent = student;
                AppCache.selectedClass = activeClass;
                Intent intent = new Intent(itemView.getContext(), EnterMarksActivity.class);
                itemView.getContext().startActivity(intent);
            }
        }
    }
}
