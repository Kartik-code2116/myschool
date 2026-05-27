package com.example.myschool.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myschool.AppCache;
import com.example.myschool.HomeActivity;
import com.example.myschool.SessionContext;
import com.example.myschool.EnterMarksActivity;
import com.example.myschool.MarksheetActivity;
import com.example.myschool.ClassSetupActivity;
import com.example.myschool.R;
import com.example.myschool.StudentEditActivity;
import com.example.myschool.StudentProfileActivity;
import com.example.myschool.adapter.StudentAdapter;
import com.example.myschool.databinding.FragmentStudentListBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.UiAnimations;

import java.util.ArrayList;
import java.util.List;

public class StudentListFragment extends Fragment {

    private FragmentStudentListBinding b;
    private StudentAdapter studentAdapter;
    private List<Student> allStudents = new ArrayList<>();
    private List<Student> filteredStudents = new ArrayList<>();
    private List<School> schools = new ArrayList<>();
    private List<ClassModel> classes = new ArrayList<>();
    private School selectedSchool = null;
    private ClassModel selectedClass = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentStudentListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecycler();
        setupSearch();
        setupFab();
        setupCustomToolbar();
        UiAnimations.staggerFadeIn(b.etSearch, b.rvStudents, b.fabAddStudent);

        applySessionFilterIfAny();
    }

    private void setupCustomToolbar() {
        if (b.btnCustomMenu != null) {
            b.btnCustomMenu.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).openDrawer();
                }
            });
        }

        if (b.btnHelp != null) {
            b.btnHelp.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("विद्यार्थी यादी मदत (Student List Help)")
                        .setMessage("१. विद्यार्थ्याचे गुण भरण्यासाठी किंवा उपस्थिती बदलण्यासाठी प्रत्येक विद्यार्थ्याच्या कार्डवरील ३-बिंदू मेनूवर क्लिक करा.\n\n"
                                + "२. नवीन विद्यार्थी जोडण्यासाठी खाली उजव्या कोपऱ्यातील '+' बटणावर क्लिक करा.\n\n"
                                + "३. शोध घेण्यासाठी वरील सर्च बारचा वापर करा.")
                        .setPositiveButton("ठीक आहे", null)
                        .show();
            });
        }

        if (b.btnCalc != null) {
            b.btnCalc.setOnClickListener(v -> {
                android.widget.Toast.makeText(requireContext(), "गुण/निकाल मोजणी विभाग लवकरच येत आहे", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        if (b.btnIdCard != null) {
            b.btnIdCard.setOnClickListener(v -> {
                android.widget.Toast.makeText(requireContext(), "आयडी कार्ड प्रिंटिंग लवकरच सुरू होईल", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        if (b.btnMoreOptions != null) {
            b.btnMoreOptions.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).showHomeMoreMenu(v);
                }
            });
        }
    }

    private void applySessionFilterIfAny() {
        SessionContext.syncFromAppCache();
        if (SessionContext.selectedClass != null && SessionContext.selectedClass.id != null) {
            FirebaseRepository.get().getStudentsForClass(SessionContext.selectedClass.id,
                    new FirebaseRepository.OnResult<List<Student>>() {
                        @Override public void onSuccess(List<Student> list) {
                            allStudents.clear();
                            allStudents.addAll(list);
                            filteredStudents.clear();
                            filteredStudents.addAll(list);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    studentAdapter.setData(filteredStudents);
                                    b.emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
                                });
                            }
                        }
                        @Override public void onError(Exception e) { loadAllStudents(); }
                    });
        }
    }

    private void setupRecycler() {
        studentAdapter = new StudentAdapter();
        studentAdapter.setListener(new StudentAdapter.OnStudentClick() {
            @Override
            public void onClick(Student student, int position) {
                AppCache.selectedStudent = student;
                startActivity(new Intent(requireContext(), StudentProfileActivity.class));
            }

            @Override
            public void onEnterMarksClick(Student student, int position) {
                AppCache.selectedStudent = student;
                loadClassForStudent(student, () -> {
                    if (AppCache.selectedClass == null) return;
                    Intent intent = new Intent(requireContext(), EnterMarksActivity.class);
                    startActivity(intent);
                });
            }

            @Override
            public void onAttendanceClick(Student student, int position) {
                AppCache.selectedStudent = student;
                loadClassForStudent(student, () -> {
                    if (AppCache.selectedClass == null) return;
                    SessionContext.selectedClass = AppCache.selectedClass;
                    SessionContext.save(requireContext());
                    if (getActivity() instanceof HomeActivity) {
                        ((HomeActivity) getActivity()).navigateTo(R.id.nav_attendance);
                    }
                });
            }

            @Override
            public void onEditInfoClick(Student student, int position) {
                AppCache.selectedStudent = student;
                startActivity(new Intent(requireContext(), StudentEditActivity.class)
                        .putExtra("new_student", false));
            }

            @Override
            public void onDeleteClick(Student student, int position) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Delete Student")
                        .setMessage("Are you sure you want to delete " + student.name + "?")
                        .setPositiveButton("Delete", (d, w) -> {
                            FirebaseRepository.get().deleteStudent(student.id, new FirebaseRepository.OnResult<Void>() {
                                @Override
                                public void onSuccess(Void v) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            allStudents.remove(student);
                                            filteredStudents.remove(student);
                                            studentAdapter.setData(filteredStudents);
                                            b.emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
                                            android.widget.Toast.makeText(requireContext(), "Student deleted successfully", android.widget.Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }
                                @Override
                                public void onError(Exception e) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            android.widget.Toast.makeText(requireContext(), "Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        b.rvStudents.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvStudents.setAdapter(studentAdapter);
        UiAnimations.setupRecyclerAnimations(b.rvStudents);
    }

    private void setupSearch() {
        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
                b.btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        b.btnClearSearch.setOnClickListener(v -> {
            b.etSearch.setText("");
            filterStudents("");
        });
    }

    private void setupFab() {
        b.fabAddStudent.setOnClickListener(v -> {
            AppCache.selectedStudent = new Student();
            startActivity(new Intent(requireContext(), StudentEditActivity.class)
                    .putExtra("new_student", true));
        });
    }

    private void loadAllStudents() {
        b.emptyState.setVisibility(View.GONE);
        FirebaseRepository.get().getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                allStudents.clear();
                allStudents.addAll(list);
                filteredStudents.clear();
                filteredStudents.addAll(list);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        studentAdapter.setData(filteredStudents);
                        b.emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                }
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> b.emptyState.setVisibility(View.VISIBLE));
                }
            }
        });
    }

    private void filterStudents(String query) {
        filteredStudents.clear();
        if (query.isEmpty()) {
            filteredStudents.addAll(allStudents);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Student s : allStudents) {
                if (s.name != null && s.name.toLowerCase().contains(lowerQuery)) {
                    filteredStudents.add(s);
                } else if (s.rollNo != null && s.rollNo.toLowerCase().contains(lowerQuery)) {
                    filteredStudents.add(s);
                }
            }
        }
        studentAdapter.setData(filteredStudents);
        b.emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Bug #7 fix: always clear AppCache.selectedClass before searching to prevent
    // stale data from a previous student leaking into the next navigation action.
    private void loadClassForStudent(Student student, Runnable onComplete) {
        AppCache.selectedClass = null;  // clear before search
        FirebaseRepository.get().getClassesForSchool(student.schoolId, new FirebaseRepository.OnResult<List<ClassModel>>() {
            @Override
            public void onSuccess(List<ClassModel> list) {
                for (ClassModel c : list) {
                    if (c.id.equals(student.classId)) {
                        AppCache.selectedClass = c;
                        break;
                    }
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(onComplete);
                }
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(onComplete);
                }
            }
        });
    }

    private void loadMarksForStudent(Student student) {
        loadClassForStudent(student, () -> {
            if (AppCache.selectedClass == null) return;
            String semId = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.id : "sem_1";
            FirebaseRepository.get().getMarksForStudentAndSemester(student.id, student.classId, semId,
                    new FirebaseRepository.OnResult<MarksRecord>() {
                        @Override
                        public void onSuccess(MarksRecord m) {
                            if (m != null) {
                                AppCache.selectedMarks = m;
                                startActivity(new Intent(requireContext(), MarksheetActivity.class));
                            } else {
                                // No marks yet, go to enter marks
                                startActivity(new Intent(requireContext(), EnterMarksActivity.class));
                            }
                        }
                        @Override
                        public void onError(Exception e) {}
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        SessionContext.syncFromAppCache();

        // Hide parent top app bar & fix CoordinatorLayout scrolling behavior offset bug:
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.GONE);
            }
            
            View navHost = ha.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                        (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost.getLayoutParams();
                params.setBehavior(null);
                float density = getResources().getDisplayMetrics().density;
                params.bottomMargin = (int) (64 * density);
                navHost.setLayoutParams(params);
            }
        }

        // Dynamically set sub-text and header section details
        String yearLabel = SessionContext.getYearLabel();
        String classVal = "1";
        String divVal = "1";
        if (SessionContext.selectedClass != null) {
            classVal = SessionContext.selectedClass.className != null ? SessionContext.selectedClass.className : "1";
            divVal = SessionContext.selectedClass.division != null && !SessionContext.selectedClass.division.isEmpty() 
                    ? SessionContext.selectedClass.division : "1";
        }
        
        if (b.tvCustomSubtitle != null) {
            b.tvCustomSubtitle.setText("• Class: " + classVal + " • Div: " + divVal + " •");
        }
        
        if (b.tvHeaderSessionInfo != null) {
            b.tvHeaderSessionInfo.setText("Year: " + yearLabel + " Class: " + classVal + ", Div: " + divVal);
        }

        if (SessionContext.selectedClass != null) {
            applySessionFilterIfAny();
        } else {
            loadAllStudents();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Restore parent top app bar and scrolling behavior:
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.VISIBLE);
            }
            
            View navHost = ha.findViewById(R.id.navHostFragment);
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
}
