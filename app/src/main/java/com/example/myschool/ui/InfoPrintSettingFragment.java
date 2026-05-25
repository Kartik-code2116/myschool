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

import androidx.navigation.NavController;

import androidx.navigation.Navigation;



import com.example.myschool.AppCache;

import com.example.myschool.ClassSetupActivity;

import com.example.myschool.HomeActivity;

import com.example.myschool.R;

import com.example.myschool.SessionContext;

import com.example.myschool.databinding.FragmentInfoPrintSettingBinding;

import com.example.myschool.model.AcademicYear;

import com.example.myschool.model.ClassModel;

import com.example.myschool.model.School;

import com.example.myschool.model.Semester;

import com.example.myschool.model.Teacher;

import com.example.myschool.repository.FirebaseRepository;

import com.example.myschool.utils.UiAnimations;



import java.util.ArrayList;

import java.util.List;



public class InfoPrintSettingFragment extends Fragment {



    private FragmentInfoPrintSettingBinding b;

    private final List<AcademicYear> years = new ArrayList<>();

    private final List<Semester> semesters = new ArrayList<>();

    private final List<ClassModel> classes = new ArrayList<>();

    private int yearIndex = 0;

    private int semesterIndex = 0;

    private int classIndex = 0;

    private boolean entrancePlayed;



    @Nullable

    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        b = FragmentInfoPrintSettingBinding.inflate(inflater, container, false);

        return b.getRoot();

    }



    @Override

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof HomeActivity) {

            ((HomeActivity) getActivity()).updateToolbar(

                    getString(R.string.nav_home),

                    getString(R.string.subtitle_info_print));

        }



        b.btnYearPrev.setOnClickListener(v -> cycleYear(-1));

        b.btnYearNext.setOnClickListener(v -> cycleYear(1));

        b.btnSemesterPrev.setOnClickListener(v -> cycleSemester(-1));

        b.btnSemesterNext.setOnClickListener(v -> cycleSemester(1));

        b.btnClassPrev.setOnClickListener(v -> cycleClass(-1));

        b.btnClassNext.setOnClickListener(v -> cycleClass(1));



        b.btnGoToClass.setOnClickListener(v -> {

            UiAnimations.pulse(b.btnGoToClass);

            goToClassStudents();

        });

        b.btnAllClasses.setOnClickListener(v -> {

            UiAnimations.pulse(b.btnAllClasses);

            navigateWithAnim(R.id.nav_class_div);

        });



        b.panelClass.setOnClickListener(v -> {

            if (!classes.isEmpty()) {

                UiAnimations.pulse(b.panelClass);

                goToClassStudents();

            }

        });



        playEntranceIfNeeded();

        loadTeacherName();

        initSessionData();

    }



    private void playEntranceIfNeeded() {

        if (entrancePlayed || b == null) return;

        entrancePlayed = true;

        b.homeRoot.setAlpha(1f);

        b.homeRoot.post(() -> {
            if (!isViewActive()) return;
            UiAnimations.staggerFadeIn(
                    b.headerBand, b.cardMain, b.btnGoToClass, b.btnAllClasses);
        });

    }



    private void navigateWithAnim(int destId) {

        NavController nav = Navigation.findNavController(requireView());

        if (getActivity() instanceof HomeActivity) {

            ((HomeActivity) getActivity()).navigateTo(destId);

        } else {

            nav.navigate(destId, null, UiAnimations.navSlideForward());

        }

    }



    private void loadTeacherName() {

        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {

            @Override public void onSuccess(Teacher t) {

                if (b != null && t != null && t.name != null) {

                    requireActivity().runOnUiThread(() -> {

                        b.tvTeacherNameHeader.setText(t.name);

                        UiAnimations.fadeIn(b.tvTeacherNameHeader);

                    });

                }

            }

            @Override public void onError(Exception e) {}

        });

    }



    private void initSessionData() {

        FirebaseRepository.get().ensureDefaultYearAndSemesters(new FirebaseRepository.OnResult<AcademicYear>() {

            @Override public void onSuccess(AcademicYear year) { loadYears(); }

            @Override public void onError(Exception e) { loadYears(); }

        });

        ensureSchoolSelected();

    }



    private void ensureSchoolSelected() {

        if (SessionContext.selectedSchool != null) return;

        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {

            @Override public void onSuccess(Teacher t) {

                FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {

                    @Override public void onSuccess(School s) {

                        SessionContext.selectedSchool = s;

                        AppCache.selectedSchool = s;

                    }

                    @Override public void onError(Exception e) {}

                });

            }

            @Override public void onError(Exception e) {}

        });

    }



    private void loadYears() {

        FirebaseRepository.get().getAcademicYears(new FirebaseRepository.OnResult<List<AcademicYear>>() {

            @Override public void onSuccess(List<AcademicYear> list) {

                years.clear();

                if (list != null) years.addAll(list);

                if (years.isEmpty()) {

                    years.add(new AcademicYear("2026-27", 2026, 2027));

                }

                if (SessionContext.selectedYear != null) {

                    for (int i = 0; i < years.size(); i++) {

                        if (years.get(i).id != null && years.get(i).id.equals(SessionContext.selectedYear.id)) {

                            yearIndex = i;

                            break;

                        }

                    }

                }

                applyYear(0);

                loadSemesters();

            }

            @Override public void onError(Exception e) {

                years.clear();

                years.add(new AcademicYear("2026-27", 2026, 2027));

                applyYear(0);

            }

        });

    }



    private void loadSemesters() {

        AcademicYear y = getCurrentYear();

        if (y == null || y.id == null) {

            semesters.clear();

            semesters.add(new Semester(1, "First Semester", "Easy Reports"));

            semesters.add(new Semester(2, "Second Semester", "Final Reports"));

            applySemester(0);

            loadClasses();

            return;

        }

        FirebaseRepository.get().getSemestersForYear(y.id, new FirebaseRepository.OnResult<List<Semester>>() {

            @Override public void onSuccess(List<Semester> list) {

                semesters.clear();

                if (list != null && !list.isEmpty()) {

                    semesters.addAll(list);

                } else {

                    semesters.add(new Semester(1, "First Semester", "Easy Reports"));

                    semesters.add(new Semester(2, "Second Semester", "Final Reports"));

                }

                semesterIndex = 0;

                applySemester(0);

                loadClasses();

            }

            @Override public void onError(Exception e) {

                semesters.clear();

                semesters.add(new Semester(1, "First Semester", "Easy Reports"));

                applySemester(0);

                loadClasses();

            }

        });

    }



    private void loadClasses() {

        AcademicYear y = getCurrentYear();

        if (y == null || y.id == null) {

            loadClassesForSchool();

            return;

        }

        FirebaseRepository.get().getClassesForYear(y.id, new FirebaseRepository.OnResult<List<ClassModel>>() {

            @Override public void onSuccess(List<ClassModel> list) {

                classes.clear();

                if (list != null && !list.isEmpty()) {

                    classes.addAll(list);

                } else {

                    loadClassesForSchool();

                    return;

                }

                classIndex = 0;

                applyClass(0);

            }

            @Override public void onError(Exception e) { loadClassesForSchool(); }

        });

    }



    private void loadClassesForSchool() {

        if (SessionContext.selectedSchool == null) {

            classes.clear();

            applyClass(0);

            return;

        }

        FirebaseRepository.get().getClassesForSchool(SessionContext.selectedSchool.id,

                new FirebaseRepository.OnResult<List<ClassModel>>() {

                    @Override public void onSuccess(List<ClassModel> list) {

                        classes.clear();

                        if (list != null) classes.addAll(list);

                        classIndex = 0;

                        applyClass(0);

                    }

                    @Override public void onError(Exception e) {

                        classes.clear();

                        applyClass(0);

                    }

                });

    }



    private AcademicYear getCurrentYear() {

        if (years.isEmpty()) return null;

        yearIndex = Math.max(0, Math.min(yearIndex, years.size() - 1));

        return years.get(yearIndex);

    }



    private void cycleYear(int delta) {

        if (years.isEmpty()) return;

        yearIndex = (yearIndex + delta + years.size()) % years.size();

        applyYear(delta);

        loadSemesters();

    }



    private void cycleSemester(int delta) {

        if (semesters.isEmpty()) return;

        semesterIndex = (semesterIndex + delta + semesters.size()) % semesters.size();

        applySemester(delta);

    }



    private void cycleClass(int delta) {

        if (classes.isEmpty()) return;

        classIndex = (classIndex + delta + classes.size()) % classes.size();

        applyClass(delta);

    }



    private boolean isViewActive() {

        return b != null && isAdded();

    }



    private void applyYear(int animDirection) {

        AcademicYear y = getCurrentYear();

        if (y == null) return;

        SessionContext.selectedYear = y;

        if (!isViewActive()) return;

        b.tvYearLabel.setText(y.label != null ? y.label : getString(R.string.year_label, "—"));

        if (animDirection != 0) {

            UiAnimations.animateSelectorChange(b.tvYearLabel, animDirection);

        }

    }



    private void applySemester(int animDirection) {

        if (semesters.isEmpty()) return;

        semesterIndex = Math.max(0, Math.min(semesterIndex, semesters.size() - 1));

        Semester s = semesters.get(semesterIndex);

        SessionContext.selectedSemester = s;

        if (!isViewActive()) return;

        b.tvSemesterName.setText(s.name != null ? s.name : "");

        if (animDirection != 0) {

            UiAnimations.animateSelectorChange(b.panelSemester, animDirection);

        }

    }



    private void applyClass(int animDirection) {

        if (!isViewActive()) return;

        boolean empty = classes.isEmpty();

        b.tvNoClassHint.setVisibility(empty ? View.VISIBLE : View.GONE);

        b.btnGoToClass.setEnabled(!empty);

        if (empty) {

            SessionContext.selectedClass = null;

            b.tvClassNumberBig.setText("—");

            b.tvClassDivLabel.setText(getString(R.string.home_no_class_hint));

            return;

        }

        classIndex = Math.max(0, Math.min(classIndex, classes.size() - 1));

        ClassModel c = classes.get(classIndex);

        SessionContext.selectedClass = c;

        SessionContext.syncToAppCache();

        String classNum = c.className != null ? c.className : "—";

        String div = c.division != null && !c.division.isEmpty() ? c.division : "-";

        b.tvClassNumberBig.setText(classNum);

        b.tvClassDivLabel.setText(getString(R.string.class_div_format, classNum, div));

        if (animDirection != 0) {

            UiAnimations.animateSelectorChange(b.panelClass, animDirection);

        }

    }



    private void goToClassStudents() {

        if (SessionContext.selectedClass == null) {

            Toast.makeText(requireContext(), R.string.add_class, Toast.LENGTH_SHORT).show();

            if (SessionContext.selectedSchool != null) {

                AppCache.selectedSchool = SessionContext.selectedSchool;

                startActivity(new Intent(requireContext(), ClassSetupActivity.class));

                requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

            }

            return;

        }

        navigateWithAnim(R.id.nav_students);

    }



    @Override

    public void onResume() {

        super.onResume();

        if (isViewActive()) {

            loadClasses();

        }

    }



    @Override

    public void onDestroyView() {

        super.onDestroyView();

        b = null;

    }

}

