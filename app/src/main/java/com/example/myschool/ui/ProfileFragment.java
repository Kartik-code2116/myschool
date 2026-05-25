package com.example.myschool.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myschool.AppCache;
import com.example.myschool.HomeActivity;
import com.example.myschool.LoginActivity;
import com.example.myschool.R;
import com.example.myschool.SessionContext;
import com.example.myschool.adapter.ProfileClassAdapter;
import com.example.myschool.databinding.FragmentProfileBinding;
import com.example.myschool.model.AcademicYear;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.ProfileClassItem;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.UiAnimations;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding b;
    private Teacher currentTeacher;
    private ProfileClassAdapter classAdapter;
    private final List<ClassModel> loadedClasses = new ArrayList<>();
    private final List<ProfileClassItem> lastItems = new ArrayList<>();
    private boolean editMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).updateToolbar(
                    getString(R.string.nav_profile),
                    getString(R.string.profile_subtitle));
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(requireContext(), LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            requireActivity().finish();
            return;
        }

        classAdapter = new ProfileClassAdapter();
        classAdapter.setListener(this::selectClass);
        b.rvProfileClasses.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvProfileClasses.setAdapter(classAdapter);
        UiAnimations.setupRecyclerAnimations(b.rvProfileClasses);

        b.btnEditProfile.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnEditProfile);
            setEditMode(true);
        });
        b.btnSaveProfile.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnSaveProfile);
            saveProfile();
        });
        b.btnLogout.setOnClickListener(v -> confirmLogout());

        setViewMode(false);
        UiAnimations.staggerFadeIn(b.cardProfileInfo, b.rvProfileClasses, b.btnLogout);
        loadProfile();
    }

    private void setViewMode(boolean animate) {
        editMode = false;
        b.layoutProfileView.setVisibility(View.VISIBLE);
        b.layoutProfileEdit.setVisibility(View.GONE);
        if (currentTeacher != null) {
            bindSummary(currentTeacher);
        }
        if (animate) {
            UiAnimations.fadeIn(b.layoutProfileView);
        }
    }

    private void setEditMode(boolean on) {
        editMode = on;
        if (on) {
            b.layoutProfileView.setVisibility(View.GONE);
            b.layoutProfileEdit.setVisibility(View.VISIBLE);
            if (currentTeacher != null) {
                b.etFullName.setText(currentTeacher.name != null ? currentTeacher.name : "");
                b.etPhone.setText(currentTeacher.phone != null ? currentTeacher.phone : "");
                b.etUdise.setText(currentTeacher.udiseCode != null ? currentTeacher.udiseCode : "");
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                b.tvProfileEmail.setText(user.getEmail());
            }
            UiAnimations.fadeIn(b.layoutProfileEdit);
        } else {
            setViewMode(true);
        }
    }

    private void bindSummary(Teacher t) {
        String name = t.name != null && !t.name.isEmpty() ? t.name : "—";
        b.tvSummaryName.setText(name);
        b.tvSummaryPhone.setText(getString(R.string.profile_summary_phone,
                t.phone != null && !t.phone.isEmpty() ? t.phone : "—"));
        b.tvSummaryUdise.setText(getString(R.string.profile_summary_udise,
                t.udiseCode != null && !t.udiseCode.isEmpty() ? t.udiseCode : "—"));
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            b.tvProfileEmail.setText(user.getEmail());
        }
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).updateToolbar(
                    getString(R.string.nav_profile), name);
        }
    }

    private void selectClass(ProfileClassItem item) {
        if (!item.hasClass()) return;
        SessionContext.selectedClass = item.classModel;
        SessionContext.syncToAppCache();
        classAdapter.setSelectedClassId(item.classModel.id);
        showActiveClassDetail(item);
        b.cardActiveClassDetail.setVisibility(View.VISIBLE);
        UiAnimations.fadeIn(b.cardActiveClassDetail);
        Toast.makeText(requireContext(), R.string.profile_class_selected, Toast.LENGTH_SHORT).show();
    }

    private void showActiveClassDetail(ProfileClassItem item) {
        ClassModel c = item.classModel;
        if (c == null) {
            b.cardActiveClassDetail.setVisibility(View.GONE);
            return;
        }
        String div = item.getDivision();
        b.tvActiveClassTitle.setText(getString(R.string.class_div_format,
                String.valueOf(item.standard), div));
        b.tvActiveClassStudents.setText(getString(R.string.profile_students_count, item.studentCount));

        String allDivs = buildDivisionsSummaryForStd(item.standard, loadedClasses);
        if (!allDivs.isEmpty()) {
            b.tvActiveClassDivisions.setText(getString(R.string.profile_divisions_in_std, allDivs));
            b.tvActiveClassDivisions.setVisibility(View.VISIBLE);
        } else {
            b.tvActiveClassDivisions.setVisibility(View.GONE);
        }

        StringBuilder extra = new StringBuilder();
        if (c.teacherName != null && !c.teacherName.isEmpty()) {
            extra.append(getString(R.string.profile_teacher_line, c.teacherName));
        }
        if (c.assistantTeacherName != null && !c.assistantTeacherName.isEmpty()) {
            if (extra.length() > 0) extra.append("\n");
            extra.append(getString(R.string.profile_asst_teacher_line, c.assistantTeacherName));
        }
        int totalInStd = totalStudentsForStandard(item.standard);
        if (totalInStd > 0) {
            if (extra.length() > 0) extra.append("\n");
            extra.append(getString(R.string.profile_std_total_students, item.standard, totalInStd));
        }
        if (extra.length() > 0) {
            b.tvActiveClassExtra.setText(extra.toString());
            b.tvActiveClassExtra.setVisibility(View.VISIBLE);
        } else {
            b.tvActiveClassExtra.setVisibility(View.GONE);
        }
    }

    private int totalStudentsForStandard(int std) {
        int total = 0;
        for (ProfileClassItem it : lastItems) {
            if (it.standard == std && it.hasClass()) {
                total += it.studentCount;
            }
        }
        return total;
    }

    private void loadProfile() {
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher teacher) {
                if (!isViewActive()) return;
                currentTeacher = teacher != null ? teacher : new Teacher();
                if (!editMode) {
                    bindSummary(currentTeacher);
                }
                loadClasses();
                FirebaseRepository.get().ensureTeacherSchool(currentTeacher,
                        new FirebaseRepository.OnResult<School>() {
                            @Override public void onSuccess(School s) {
                                SessionContext.selectedSchool = s;
                                AppCache.selectedSchool = s;
                            }
                            @Override public void onError(Exception e) {}
                        });
            }
            @Override public void onError(Exception e) {
                if (isViewActive()) {
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadClasses() {
        AcademicYear y = SessionContext.selectedYear;
        if (y != null && y.id != null) {
            FirebaseRepository.get().getClassesForYear(y.id, new FirebaseRepository.OnResult<List<ClassModel>>() {
                @Override public void onSuccess(List<ClassModel> list) {
                    onClassesLoaded(list);
                }
                @Override public void onError(Exception e) { loadClassesBySchool(); }
            });
        } else {
            loadClassesBySchool();
        }
    }

    private void loadClassesBySchool() {
        if (SessionContext.selectedSchool == null) {
            onClassesLoaded(null);
            return;
        }
        FirebaseRepository.get().getClassesForSchool(SessionContext.selectedSchool.id,
                new FirebaseRepository.OnResult<List<ClassModel>>() {
                    @Override public void onSuccess(List<ClassModel> list) {
                        onClassesLoaded(list);
                    }
                    @Override public void onError(Exception e) {
                        onClassesLoaded(null);
                    }
                });
    }

    private void onClassesLoaded(List<ClassModel> list) {
        loadedClasses.clear();
        if (list != null) loadedClasses.addAll(list);
        loadStudentCountsThenBind();
    }

    private void loadStudentCountsThenBind() {
        FirebaseRepository.get().getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
            @Override public void onSuccess(List<Student> students) {
                if (!isViewActive()) return;
                Map<String, Integer> countByClassId = new HashMap<>();
                if (students != null) {
                    for (Student s : students) {
                        if (s.classId != null) {
                            countByClassId.merge(s.classId, 1, Integer::sum);
                        }
                    }
                }
                List<ProfileClassItem> items = buildItemsForStandards1To12(loadedClasses, countByClassId);
                lastItems.clear();
                lastItems.addAll(items);
                Map<Integer, String> divSummary = buildDivisionsSummaryMap(items);
                bindClassList(items, divSummary);
            }
            @Override public void onError(Exception e) {
                if (!isViewActive()) return;
                List<ProfileClassItem> items = buildItemsForStandards1To12(loadedClasses, new HashMap<>());
                lastItems.clear();
                lastItems.addAll(items);
                bindClassList(items, buildDivisionsSummaryMap(items));
            }
        });
    }

    private static List<ProfileClassItem> buildItemsForStandards1To12(
            List<ClassModel> classes, Map<String, Integer> countByClassId) {
        TreeMap<Integer, List<ClassModel>> byStd = new TreeMap<>();
        if (classes != null) {
            for (ClassModel c : classes) {
                int std = parseStd(c.className);
                if (std < 1 || std > 12) continue;
                byStd.computeIfAbsent(std, k -> new ArrayList<>()).add(c);
            }
        }
        List<ProfileClassItem> items = new ArrayList<>();
        for (int std = 1; std <= 12; std++) {
            List<ClassModel> divs = byStd.get(std);
            if (divs == null || divs.isEmpty()) {
                items.add(new ProfileClassItem(std, null));
            } else {
                divs.sort((a, b) -> {
                    String da = a.division != null ? a.division : "";
                    String db = b.division != null ? b.division : "";
                    return da.compareTo(db);
                });
                for (ClassModel c : divs) {
                    ProfileClassItem item = new ProfileClassItem(std, c);
                    if (c.id != null && countByClassId.containsKey(c.id)) {
                        item.studentCount = countByClassId.get(c.id);
                    }
                    items.add(item);
                }
            }
        }
        return items;
    }

    private static int parseStd(String className) {
        if (className == null) return -1;
        try {
            String num = className.replaceAll("[^0-9]", "");
            if (num.isEmpty()) return -1;
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Map<Integer, String> buildDivisionsSummaryMap(List<ProfileClassItem> items) {
        Map<Integer, List<String>> divs = new TreeMap<>();
        for (ProfileClassItem item : items) {
            if (!item.hasClass()) continue;
            divs.computeIfAbsent(item.standard, k -> new ArrayList<>())
                    .add(item.getDivision());
        }
        Map<Integer, String> out = new HashMap<>();
        for (Map.Entry<Integer, List<String>> e : divs.entrySet()) {
            out.put(e.getKey(), TextUtils.join(", ", e.getValue()));
        }
        return out;
    }

    private static String buildDivisionsSummaryForStd(int std, List<ClassModel> classes) {
        List<String> divs = new ArrayList<>();
        if (classes != null) {
            for (ClassModel c : classes) {
                if (parseStd(c.className) == std && c.division != null && !c.division.isEmpty()) {
                    divs.add(c.division);
                }
            }
        }
        return TextUtils.join(", ", divs);
    }

    private void bindClassList(List<ProfileClassItem> items, Map<Integer, String> divSummary) {
        if (!isViewActive()) return;
        boolean anyClass = false;
        for (ProfileClassItem item : items) {
            if (item.hasClass()) {
                anyClass = true;
                break;
            }
        }
        b.tvNoClasses.setVisibility(anyClass ? View.GONE : View.VISIBLE);
        b.rvProfileClasses.setVisibility(View.VISIBLE);

        classAdapter.setData(items, divSummary);
        String selectedId = SessionContext.selectedClass != null ? SessionContext.selectedClass.id : null;
        classAdapter.setSelectedClassId(selectedId);

        if (selectedId != null) {
            for (ProfileClassItem item : items) {
                if (item.hasClass() && selectedId.equals(item.classModel.id)) {
                    showActiveClassDetail(item);
                    b.cardActiveClassDetail.setVisibility(View.VISIBLE);
                    break;
                }
            }
        } else {
            b.cardActiveClassDetail.setVisibility(View.GONE);
        }
    }

    private void saveProfile() {
        String name = str(b.etFullName);
        if (TextUtils.isEmpty(name)) {
            b.tilName.setError(getString(R.string.error_name_required));
            return;
        }
        b.tilName.setError(null);

        Teacher t = currentTeacher != null ? currentTeacher : new Teacher();
        t.name = name;
        t.phone = str(b.etPhone);
        t.udiseCode = str(b.etUdise);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) t.email = user.getEmail();

        FirebaseRepository.get().saveTeacher(t, new FirebaseRepository.OnResult<Void>() {
            @Override public void onSuccess(Void v) {
                if (!isViewActive()) return;
                currentTeacher = t;
                setViewMode(true);
                Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {
                    @Override public void onSuccess(School s) {
                        SessionContext.selectedSchool = s;
                        AppCache.selectedSchool = s;
                    }
                    @Override public void onError(Exception e) {}
                });
            }
            @Override public void onError(Exception e) {
                if (isViewActive()) {
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.logout, (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(requireContext(), LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    requireActivity().finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String str(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private boolean isViewActive() {
        return b != null && isAdded();
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
