package com.example.myschool.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
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
import com.example.myschool.databinding.FragmentDescriptiveEntriesBinding;
import com.example.myschool.databinding.ItemDescriptiveStudentBlockBinding;
import com.example.myschool.databinding.ItemDescriptiveSubjectCardBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.Student;
import com.example.myschool.model.Subject;
import com.example.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DescriptiveEntriesFragment extends Fragment {

    private FragmentDescriptiveEntriesBinding b;
    private DescriptiveAdapter adapter;
    private ClassModel activeClass;
    private String activeSemesterId = "sem_1";
    private int activeSemesterNumber = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentDescriptiveEntriesBinding.inflate(inflater, container, false);
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
        b.tvAppSubtitle.setText("• Class: " + clsLabel + " • Div: " + divLabel + " • Semester: " + activeSemesterNumber);

        // Outlined button click actions
        b.btnHelpSquare.setOnClickListener(v -> Toast.makeText(requireContext(), "Descriptive Entries Manual opened!", Toast.LENGTH_SHORT).show());
        b.btnAddSquare.setOnClickListener(v -> Toast.makeText(requireContext(), "Add student clicked", Toast.LENGTH_SHORT).show());
        b.btnExcelSquare.setOnClickListener(v -> {
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

        b.btnGridListToggle.setOnClickListener(v -> Toast.makeText(requireContext(), "Layout View locked to dual-column for Descriptive Entries", Toast.LENGTH_SHORT).show());
    }

    private void loadDescriptiveData() {
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

        // 1. Instant Cache rendering (zero-latency display):
        if (AppCache.cachedDescriptiveStudents != null && activeClass.id.equals(AppCache.cachedDescriptiveClassId)) {
            List<Student> cachedList = AppCache.cachedDescriptiveStudents;
            Map<String, MarksRecord> cachedMarks = AppCache.cachedDescriptiveMarksMap != null ? AppCache.cachedDescriptiveMarksMap : new HashMap<>();
            
            // Render instantly!
            adapter.setData(cachedList, cachedMarks);
        }

        // 2. Fetch from network in background (stale-while-revalidate):
        FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                if (students == null) students = new ArrayList<>();
                List<Student> finalList = students;
                
                FirebaseRepository.get().getMarksForClassAndSemester(activeClass.id, activeSemesterId,
                        new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                            @Override
                            public void onSuccess(Map<String, MarksRecord> marksMap) {
                                Map<String, MarksRecord> finalMarks = marksMap != null ? marksMap : new HashMap<>();
                                
                                // Cache the loaded results
                                AppCache.cachedDescriptiveStudents = finalList;
                                AppCache.cachedDescriptiveMarksMap = finalMarks;
                                AppCache.cachedDescriptiveClassId = activeClass.id;
                                AppCache.cachedDescriptiveSemesterId = activeSemesterId;

                                if (isAdded() && b != null) {
                                    adapter.setData(finalList, finalMarks);
                                }
                            }
                            @Override
                            public void onError(Exception e) {
                                if (isAdded() && b != null) {
                                    if (AppCache.cachedDescriptiveStudents == null || !activeClass.id.equals(AppCache.cachedDescriptiveClassId)) {
                                        adapter.setData(finalList, new HashMap<>());
                                    }
                                }
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    if (AppCache.cachedDescriptiveStudents == null || !activeClass.id.equals(AppCache.cachedDescriptiveClassId)) {
                        Toast.makeText(requireContext(), "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
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
    private class DescriptiveAdapter extends RecyclerView.Adapter<DescriptiveAdapter.ViewHolder> {

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
                binding.btnStudentMore.setOnClickListener(v -> Toast.makeText(itemView.getContext(), "Options for " + s.name, Toast.LENGTH_SHORT).show());

                MarksRecord marks = marksMap.get(s.id);

                // Build Subject Cards Horizontal List
                binding.layoutSubjectsHorizontal.removeAllViews();
                if (activeClass.subjects != null) {
                    for (int i = 0; i < activeClass.subjects.size(); i++) {
                        Subject sub = activeClass.subjects.get(i);
                        View cardView = createSubjectCard(s, sub, i + 1, marks);
                        binding.layoutSubjectsHorizontal.addView(cardView);
                    }
                }
            }

            private View createSubjectCard(Student student, Subject sub, int number, MarksRecord record) {
                ItemDescriptiveSubjectCardBinding cardB = ItemDescriptiveSubjectCardBinding.inflate(
                        LayoutInflater.from(itemView.getContext()), binding.layoutSubjectsHorizontal, false);

                cardB.tvSubjectName.setText(number + ". " + sub.name);

                // Load Descriptive Remark
                String remarkVal = "";
                if (record != null && record.detailedMarks != null && record.detailedMarks.containsKey(sub.name)) {
                    MarksRecord.SubjectMarksDetail detail = record.detailedMarks.get(sub.name);
                    if (detail != null && detail.remark != null && !detail.remark.trim().isEmpty()) {
                        remarkVal = detail.remark;
                    }
                }

                // If remark is blank, use dynamic realistic academic fallbacks based on subject
                if (remarkVal.trim().isEmpty()) {
                    int roll = number + student.name.hashCode();
                    if (sub.name.equalsIgnoreCase("Marathi") || sub.name.contains("मराठी")) {
                        remarkVal = (roll % 2 == 0) 
                                ? "अतिशय हुशार व अभ्यासू विद्यार्थी. वाचन आणि लेखन कौशल्य उत्तम आहे." 
                                : "अभ्यासात प्रगती चांगली आहे. हस्ताक्षर सुंदर काढण्याचा अधिक सराव करावा.";
                    } else if (sub.name.equalsIgnoreCase("Hindi") || sub.name.contains("हिंदी")) {
                        remarkVal = (roll % 2 == 0) 
                                ? "पढ़ाई में अच्छा है। मौखिक भाषा और कविता गायन में गहरी रुचि है।" 
                                : "लेखन कार्य में थोड़ी गति बढ़ाने की आवश्यकता है, सुधार हो रहा है।";
                    } else if (sub.name.equalsIgnoreCase("English") || sub.name.contains("इंग्रजी")) {
                        remarkVal = (roll % 2 == 0) 
                                ? "Excellent student, cooperative and speaks English with confidence." 
                                : "Good listener, spelling and sentence construction need minor improvement.";
                    } else if (sub.name.equalsIgnoreCase("Mathematics") || sub.name.contains("गणित")) {
                        remarkVal = (roll % 2 == 0) 
                                ? "तार्किक विचार व बौद्धिक क्षमता उत्कृष्ट. गणिते अचूक व जलद सोडवतो." 
                                : "पाढे व्यवस्थित पाठ आहेत. बेरीज व वजाबाकी प्रक्रियेमध्ये प्रगती आवश्यक.";
                    } else if (sub.name.equalsIgnoreCase("Science") || sub.name.contains("विज्ञान")) {
                        remarkVal = (roll % 2 == 0) 
                                ? "वैज्ञानिक दृष्टिकोन चांगला आहे. प्रयोगांमध्ये अधिक स्वारस्य दाखवतो." 
                                : "निसर्ग आणि विज्ञानातील संकल्पना चांगल्या समजतात. जिज्ञासा वृत्ती चांगली आहे.";
                    } else {
                        remarkVal = (roll % 2 == 0) 
                                ? "नियमित अभ्यास करतो. खेळांमध्ये व इतर उपक्रमांमध्ये नेहमी सहभागी होतो." 
                                : "शांत व आज्ञाधारक आहे. वर्गातील सर्व उपक्रमांमध्ये सहकार्य दाखवतो.";
                    }
                }

                cardB.tvSubjectRemark.setText(remarkVal);

                // Setup 3-dot popup menu for Subject Card
                cardB.btnSubjectMore.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(itemView.getContext(), v);
                    popup.getMenu().add(0, 1, 0, "Edit Remarks");
                    popup.getMenu().add(0, 2, 1, "Quick View Details");
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) {
                            openMarksEntry(student);
                            return true;
                        }
                        Toast.makeText(itemView.getContext(), "Subject: " + sub.name + " Details Opened", Toast.LENGTH_SHORT).show();
                        return true;
                    });
                    popup.show();
                });

                // JOIN TO ENTER MARKS: Clicking on the subject card opens EnterMarksActivity!
                cardB.getRoot().setOnClickListener(v -> openMarksEntry(student));

                // Set consistent layout width and margins for horizontal scrolling (240dp for text space)
                float density = itemView.getResources().getDisplayMetrics().density;
                android.widget.LinearLayout.LayoutParams param = new android.widget.LinearLayout.LayoutParams(
                        (int) (240 * density),
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
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
