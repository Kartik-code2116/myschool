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
import androidx.navigation.Navigation;

import com.example.myschool.AppCache;
import com.example.myschool.ClassSetupActivity;
import com.example.myschool.HomeActivity;
import com.example.myschool.R;
import com.example.myschool.SessionContext;
import com.example.myschool.StudentRegisterActivity;
import com.example.myschool.databinding.FragmentInfoPrintSettingBinding;
import com.example.myschool.model.AcademicYear;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.School;
import com.example.myschool.model.Semester;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;

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
                    getString(R.string.title_info_print),
                    getString(R.string.subtitle_info_print));
        }

        b.btnYearPrev.setOnClickListener(v -> cycleYear(-1));
        b.btnYearNext.setOnClickListener(v -> cycleYear(1));
        b.btnSemesterPrev.setOnClickListener(v -> cycleSemester(-1));
        b.btnSemesterNext.setOnClickListener(v -> cycleSemester(1));
        b.btnClassPrev.setOnClickListener(v -> cycleClass(-1));
        b.btnClassNext.setOnClickListener(v -> cycleClass(1));

        b.btnGoToClass.setOnClickListener(v -> goToClassStudents());
        b.btnAllClasses.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.nav_class_div));
        b.btnHowToUse.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.hint_fill_data, Toast.LENGTH_LONG).show());
        b.btnOnlineHelp.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.hint_question_mark, Toast.LENGTH_LONG).show());

        loadTeacherName();
        initSessionData();
    }

    private void loadTeacherName() {
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher t) {
                if (b != null && t != null && t.name != null) {
                    requireActivity().runOnUiThread(() -> b.tvTeacherNameHeader.setText(t.name));
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    private void initSessionData() {
        FirebaseRepository.get().ensureDefaultYearAndSemesters(new FirebaseRepository.OnResult<AcademicYear>() {
            @Override public void onSuccess(AcademicYear year) {
                loadYears();
            }
            @Override public void onError(Exception e) {
                loadYears();
            }
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
                    AcademicYear y = new AcademicYear("2026-27", 2026, 2027);
                    years.add(y);
                }
                if (SessionContext.selectedYear != null) {
                    for (int i = 0; i < years.size(); i++) {
                        if (years.get(i).id != null && years.get(i).id.equals(SessionContext.selectedYear.id)) {
                            yearIndex = i;
                            break;
                        }
                    }
                }
                applyYear();
                loadSemesters();
            }
            @Override public void onError(Exception e) {
                years.clear();
                years.add(new AcademicYear("2026-27", 2026, 2027));
                applyYear();
            }
        });
    }

    private void loadSemesters() {
        AcademicYear y = getCurrentYear();
        if (y == null || y.id == null) {
            semesters.clear();
            semesters.add(new Semester(1, "First Semester", "Easy Reports"));
            semesters.add(new Semester(2, "Second Semester", "Final Reports"));
            applySemester();
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
                applySemester();
                loadClasses();
            }
            @Override public void onError(Exception e) {
                semesters.clear();
                semesters.add(new Semester(1, "First Semester", "Easy Reports"));
                applySemester();
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
                applyClass();
            }
            @Override public void onError(Exception e) { loadClassesForSchool(); }
        });
    }

    private void loadClassesForSchool() {
        if (SessionContext.selectedSchool == null) {
            classes.clear();
            applyClass();
            return;
        }
        FirebaseRepository.get().getClassesForSchool(SessionContext.selectedSchool.id,
                new FirebaseRepository.OnResult<List<ClassModel>>() {
                    @Override public void onSuccess(List<ClassModel> list) {
                        classes.clear();
                        if (list != null) classes.addAll(list);
                        classIndex = 0;
                        applyClass();
                    }
                    @Override public void onError(Exception e) {
                        classes.clear();
                        applyClass();
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
        applyYear();
        loadSemesters();
    }

    private void cycleSemester(int delta) {
        if (semesters.isEmpty()) return;
        semesterIndex = (semesterIndex + delta + semesters.size()) % semesters.size();
        applySemester();
    }

    private void cycleClass(int delta) {
        if (classes.isEmpty()) return;
        classIndex = (classIndex + delta + classes.size()) % classes.size();
        applyClass();
    }

    private boolean isViewActive() {
        return b != null && isAdded();
    }

    private void applyYear() {
        AcademicYear y = getCurrentYear();
        if (y == null) return;
        SessionContext.selectedYear = y;
        if (!isViewActive()) return;
        b.tvYearLabel.setText(getString(R.string.year_label, y.label));
    }

    private void applySemester() {
        if (semesters.isEmpty()) return;
        semesterIndex = Math.max(0, Math.min(semesterIndex, semesters.size() - 1));
        Semester s = semesters.get(semesterIndex);
        SessionContext.selectedSemester = s;
        if (!isViewActive()) return;
        b.tvSemesterNumber.setText(String.valueOf(s.number));
        b.tvSemesterName.setText(s.name);
    }

    private void applyClass() {
        if (classes.isEmpty()) {
            SessionContext.selectedClass = null;
            if (!isViewActive()) return;
            b.tvClassNumberBig.setText("1");
            b.tvClassDivLabel.setText(SessionContext.getClassDivLabel());
            return;
        }
        classIndex = Math.max(0, Math.min(classIndex, classes.size() - 1));
        ClassModel c = classes.get(classIndex);
        SessionContext.selectedClass = c;
        SessionContext.syncToAppCache();
        if (!isViewActive()) return;
        String classNum = c.className != null ? c.className : "1";
        String div = c.division != null && !c.division.isEmpty() ? c.division : "-";
        b.tvClassNumberBig.setText(classNum);
        b.tvClassDivLabel.setText(getString(R.string.class_div_format, classNum, div));
    }

    private void goToClassStudents() {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(requireContext(), R.string.add_class, Toast.LENGTH_SHORT).show();
            if (SessionContext.selectedSchool != null) {
                AppCache.selectedSchool = SessionContext.selectedSchool;
                startActivity(new Intent(requireContext(), ClassSetupActivity.class));
            }
            return;
        }
        Navigation.findNavController(requireView()).navigate(R.id.nav_students);
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
