package com.kartik.myschool.ui;

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

import com.kartik.myschool.AppCache;
import com.kartik.myschool.ClassSetupActivity;
import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.StudentEditActivity;
import com.kartik.myschool.adapter.ClassDivAdapter;
import com.kartik.myschool.databinding.FragmentClassDivListBinding;
import com.kartik.myschool.model.AcademicYear;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.School;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;
import androidx.appcompat.widget.PopupMenu;

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
                SessionContext.save(getContext());
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
                }
            }

            @Override
            public void onClassLongClick(ClassModel c) {
                showClassOptions(c);
            }

            @Override
            public void onClassOptionsClick(ClassModel c, View anchorView, int position) {
                showClassPopupMenu(c, anchorView, position);
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
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
            @Override public void onSuccess(com.kartik.myschool.model.Teacher t) {
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
                    SessionContext.save(getContext());
                    switch (which) {
                        case 0:
                            if (getActivity() instanceof HomeActivity) {
                                ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
                            }
                            break;
                        case 1:
                            AppCache.selectedStudent = new com.kartik.myschool.model.Student();
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

    private void showClassPopupMenu(ClassModel c, View anchorView, int position) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenu().add(0, 1, 0, getString(R.string.menu_subjects));
        popup.getMenu().add(0, 2, 1, getString(R.string.menu_student_list));
        popup.getMenu().add(0, 3, 2, getString(R.string.add_student_short));
        popup.getMenu().add(0, 4, 3, getString(R.string.edit_class));
        popup.getMenu().add(0, 5, 4, getString(R.string.delete_class));

        popup.setOnMenuItemClickListener(item -> {
            SessionContext.selectedClass = c;
            SessionContext.save(getContext());
            switch (item.getItemId()) {
                case 1: // Subjects
                    if (getActivity() instanceof HomeActivity) {
                        ((HomeActivity) getActivity()).navigateTo(R.id.nav_subjects);
                    }
                    return true;
                case 2: // Students
                    if (getActivity() instanceof HomeActivity) {
                        ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
                    }
                    return true;
                case 3: // Add Student
                    AppCache.selectedStudent = new com.kartik.myschool.model.Student();
                    startActivity(new Intent(requireContext(), StudentEditActivity.class)
                            .putExtra("new_student", true));
                    return true;
                case 4: // Edit Class
                    AppCache.selectedClass = c;
                    startActivity(new Intent(requireContext(), ClassSetupActivity.class)
                            .putExtra("edit", true));
                    return true;
                case 5: // Delete Class
                    confirmDeleteClass(c);
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDeleteClass(ClassModel c) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_class))
                .setMessage(getString(R.string.delete_class_confirm, c.getDisplayName()))
                .setPositiveButton(getString(R.string.delete_class), (dialog, which) -> {
                    FirebaseRepository.get().deleteClass(c.id, new FirebaseRepository.OnResult<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(requireContext(), R.string.msg_class_deleted_successfully, Toast.LENGTH_SHORT).show();
                            loadClasses();
                        }
                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
