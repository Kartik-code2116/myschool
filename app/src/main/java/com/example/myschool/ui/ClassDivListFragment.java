package com.example.myschool.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myschool.AppCache;
import com.example.myschool.ClassSetupActivity;
import com.example.myschool.HomeActivity;
import com.example.myschool.R;
import com.example.myschool.SessionContext;
import com.example.myschool.StudentEditActivity;
import com.example.myschool.adapter.ClassDivAdapter;
import com.example.myschool.databinding.FragmentClassDivListBinding;
import com.example.myschool.model.AcademicYear;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.School;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.UiAnimations;

import java.util.List;

public class ClassDivListFragment extends Fragment {

    private FragmentClassDivListBinding b;
    private ClassDivAdapter adapter;
    private int selectedIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentClassDivListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).updateToolbar(
                    getString(R.string.title_class_div),
                    getString(R.string.subtitle_class_div));
        }

        b.tvClassDivYear.setText(getString(R.string.year_label, SessionContext.getYearLabel()));

        adapter = new ClassDivAdapter();
        adapter.setListener(new ClassDivAdapter.Listener() {
            @Override
            public void onClassClick(ClassModel c, int position) {
                selectedIndex = position;
                adapter.setSelectedIndex(position);
                SessionContext.selectedClass = c;
                SessionContext.syncToAppCache();
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
                }
            }

            @Override
            public void onClassLongClick(ClassModel c) {
                showClassOptions(c);
            }
        });
        b.rvClassDiv.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvClassDiv.setAdapter(adapter);
        UiAnimations.setupRecyclerAnimations(b.rvClassDiv);

        b.fabAddClass.setOnClickListener(v -> {
            UiAnimations.pulse(b.fabAddClass);
            openAddClass();
        });

        UiAnimations.staggerFadeIn(b.tvClassDivYear, b.rvClassDiv, b.fabAddClass);
        ensureSchoolAndLoad();
    }

    private void ensureSchoolAndLoad() {
        if (SessionContext.selectedSchool != null) {
            loadClasses();
            return;
        }
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.example.myschool.model.Teacher>() {
            @Override public void onSuccess(com.example.myschool.model.Teacher t) {
                FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {
                    @Override public void onSuccess(School s) {
                        SessionContext.selectedSchool = s;
                        AppCache.selectedSchool = s;
                        loadClasses();
                    }
                    @Override public void onError(Exception e) {
                        Toast.makeText(requireContext(), R.string.select_school_first, Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override public void onError(Exception e) {
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadClasses() {
        AcademicYear y = SessionContext.selectedYear;
        if (y != null && y.id != null) {
            FirebaseRepository.get().getClassesForYear(y.id, new FirebaseRepository.OnResult<List<ClassModel>>() {
                @Override public void onSuccess(List<ClassModel> list) {
                    if (isAdded() && b != null) bindClasses(list);
                }
                @Override public void onError(Exception e) { loadBySchool(); }
            });
        } else {
            loadBySchool();
        }
    }

    private void loadBySchool() {
        if (SessionContext.selectedSchool == null) return;
        FirebaseRepository.get().getClassesForSchool(SessionContext.selectedSchool.id,
                new FirebaseRepository.OnResult<List<ClassModel>>() {
                    @Override public void onSuccess(List<ClassModel> list) {
                        if (isAdded() && b != null) bindClasses(list);
                    }
                    @Override public void onError(Exception e) {}
                });
    }

    private void bindClasses(List<ClassModel> list) {
        if (b == null || adapter == null) return;
        if (list == null || list.isEmpty()) {
            adapter.setData(list, 0);
            return;
        }
        if (SessionContext.selectedClass != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).id != null && list.get(i).id.equals(SessionContext.selectedClass.id)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        adapter.setData(list, selectedIndex);
        UiAnimations.fadeIn(b.rvClassDiv);
    }

    private void openAddClass() {
        if (SessionContext.selectedSchool == null) {
            Toast.makeText(requireContext(), R.string.select_school_first, Toast.LENGTH_LONG).show();
            return;
        }
        AppCache.selectedSchool = SessionContext.selectedSchool;
        AppCache.selectedClass = null;
        startActivity(new Intent(requireContext(), ClassSetupActivity.class));
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
    }

    private void showClassOptions(ClassModel c) {
        String[] opts = {getString(R.string.menu_student_list), getString(R.string.add_student_short), getString(R.string.menu_subjects)};
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(c.getDisplayName())
                .setItems(opts, (d, which) -> {
                    SessionContext.selectedClass = c;
                    SessionContext.syncToAppCache();
                    switch (which) {
                        case 0:
                            if (getActivity() instanceof HomeActivity) {
                                ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
                            }
                            break;
                        case 1:
                            AppCache.selectedStudent = new com.example.myschool.model.Student();
                            startActivity(new Intent(requireContext(), StudentEditActivity.class)
                                    .putExtra("new_student", true));
                            break;
                        case 2:
                            startActivity(new Intent(requireContext(), ClassSetupActivity.class)
                                    .putExtra("edit", true));
                            break;
                    }
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b == null) return;
        b.tvClassDivYear.setText(getString(R.string.year_label, SessionContext.getYearLabel()));
        loadClasses();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
