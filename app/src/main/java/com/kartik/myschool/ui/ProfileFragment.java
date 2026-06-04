package com.kartik.myschool.ui;

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

import com.kartik.myschool.AppCache;
import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.LoginActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.adapter.ProfileClassAdapter;
import com.kartik.myschool.databinding.FragmentProfileBinding;
import com.kartik.myschool.model.AcademicYear;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.ProfileClassItem;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Semester;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Teacher;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding b;
    private Teacher currentTeacher;
    private ProfileClassAdapter classAdapter;
    private final List<ClassModel>      loadedClasses = new ArrayList<>();
    private final List<ProfileClassItem> lastItems    = new ArrayList<>();
    private boolean editMode;
    private boolean isFirstLoad = true;

    @Nullable @Override
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
                    getString(R.string.nav_profile), getString(R.string.profile_subtitle));
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

        b.btnEditProfile.setOnClickListener(v -> { UiAnimations.pulse(b.btnEditProfile); setEditMode(true); });
        b.btnSaveProfile.setOnClickListener(v -> { UiAnimations.pulse(b.btnSaveProfile); saveProfile(); });
        b.btnLogout.setOnClickListener(v -> confirmLogout());
        b.fabAddClass.setOnClickListener(v -> {
            UiAnimations.pulse(b.fabAddClass);
            if (SessionContext.selectedSchool == null) {
                Toast.makeText(requireContext(), R.string.select_school_first, Toast.LENGTH_LONG).show();
                return;
            }
            AppCache.selectedSchool = SessionContext.selectedSchool;
            AppCache.selectedClass  = null;
            startActivity(new Intent(requireContext(), com.kartik.myschool.ClassSetupActivity.class));
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        // ── Phase 1: show whatever is already in cache instantly ───────────────
        showCachedDataImmediately();

        setViewMode(false);
        UiAnimations.staggerFadeIn(b.cardProfileInfo, b.rvProfileClasses, b.btnLogout, b.fabAddClass);

        // ── Phase 2: load fresh data in parallel ───────────────────────────────
        loadAllParallel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 1 — instant cache display
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Shows whatever is already in AppCache without any network call.
     * Result: the profile screen feels instant on re-visits.
     */
    private void showCachedDataImmediately() {
        // Teacher name
        if (AppCache.cachedTeacherName != null) {
            b.tvSummaryName.setText(AppCache.cachedTeacherName);
        }
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null && u.getEmail() != null) b.tvProfileEmail.setText(u.getEmail());

        // Class list
        if (AppCache.cachedClasses != null && !AppCache.cachedClasses.isEmpty()) {
            loadedClasses.clear();
            loadedClasses.addAll(AppCache.cachedClasses);
            Map<String, Integer> counts = AppCache.cachedStudentCountByClassId != null
                    ? AppCache.cachedStudentCountByClassId : new HashMap<>();
            List<ProfileClassItem> items = buildItems(loadedClasses, counts);
            lastItems.clear(); lastItems.addAll(items);
            bindClassList(items, buildDivisionsSummaryMap(items));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 2 — parallel network load
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Fires teacher + classes fetches simultaneously.
     * Student counts are fetched only after classes arrive (we need the class IDs).
     * Each result updates the UI independently as soon as it arrives.
     */
    private void loadAllParallel() {
        AtomicReference<Teacher>           teacherRef = new AtomicReference<>();
        AtomicReference<List<ClassModel>>  classesRef = new AtomicReference<>();
        // Count down from 2 (teacher + classes) before merging
        AtomicInteger latch = new AtomicInteger(2);

        Runnable onBothDone = () -> {
            if (latch.decrementAndGet() != 0) return;
            if (!isViewActive()) return;
            Teacher t = teacherRef.get();
            List<ClassModel> cls = classesRef.get();
            if (t != null) { currentTeacher = t; AppCache.cachedTeacherName = t.name; }
            if (cls != null) { loadedClasses.clear(); loadedClasses.addAll(cls); AppCache.cachedClasses = new ArrayList<>(cls); }
            // Update teacher UI immediately
            requireActivity().runOnUiThread(() -> {
                if (!isViewActive()) return;
                if (currentTeacher != null && !editMode) bindSummary(currentTeacher);
            });
            // Now load student counts (needs class IDs)
            loadStudentCounts(loadedClasses);
        };

        // ── Fetch teacher ──────────────────────────────────────────────────────
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher t) {
                teacherRef.set(t);
                // Update school in background while waiting for the latch
                if (t != null) {
                    FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {
                        @Override public void onSuccess(School s) { SessionContext.selectedSchool = s; AppCache.selectedSchool = s; }
                        @Override public void onError(Exception e) {}
                    });
                }
                onBothDone.run();
            }
            @Override public void onError(Exception e) { onBothDone.run(); }
        });

        // ── Fetch classes ──────────────────────────────────────────────────────
        AcademicYear y = SessionContext.selectedYear;
        if (y != null && y.id != null) {
            FirebaseRepository.get().getClassesForYear(y.id, new FirebaseRepository.OnResult<List<ClassModel>>() {
                @Override public void onSuccess(List<ClassModel> list) { classesRef.set(list); onBothDone.run(); }
                @Override public void onError(Exception e)             { fetchClassesBySchool(classesRef, onBothDone); }
            });
        } else {
            fetchClassesBySchool(classesRef, onBothDone);
        }
    }

    private void fetchClassesBySchool(AtomicReference<List<ClassModel>> ref, Runnable done) {
        if (SessionContext.selectedSchool == null) { ref.set(null); done.run(); return; }
        FirebaseRepository.get().getClassesForSchool(SessionContext.selectedSchool.id,
                new FirebaseRepository.OnResult<List<ClassModel>>() {
                    @Override public void onSuccess(List<ClassModel> list) { ref.set(list); done.run(); }
                    @Override public void onError(Exception e)             { ref.set(null); done.run(); }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student count (only after classes are known)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Uses AppCache.cachedStudentCountByClassId if fresh enough.
     * Falls back to getAllStudentsForTeacher only when cache is empty.
     */
    private void loadStudentCounts(List<ClassModel> classes) {
        // Fast path: use cached counts to render immediately
        if (AppCache.cachedStudentCountByClassId != null) {
            buildAndBindList(classes, AppCache.cachedStudentCountByClassId);
        }
        // Always refresh counts in background to keep them accurate
        FirebaseRepository.get().getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
            @Override public void onSuccess(List<Student> students) {
                Map<String, Integer> counts = new HashMap<>();
                if (students != null) {
                    for (Student s : students) {
                        if (s.classId != null) counts.merge(s.classId, 1, Integer::sum);
                    }
                }
                AppCache.cachedStudentCountByClassId = counts;
                if (isViewActive()) buildAndBindList(classes, counts);
            }
            @Override public void onError(Exception e) {
                if (isViewActive() && AppCache.cachedStudentCountByClassId == null) {
                    buildAndBindList(classes, new HashMap<>());
                }
            }
        });
    }

    private void buildAndBindList(List<ClassModel> classes, Map<String, Integer> counts) {
        List<ProfileClassItem> items = buildItems(classes, counts);
        lastItems.clear(); lastItems.addAll(items);
        requireActivity().runOnUiThread(() -> {
            if (!isViewActive()) return;
            bindClassList(items, buildDivisionsSummaryMap(items));
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI binding
    // ─────────────────────────────────────────────────────────────────────────
    private void setViewMode(boolean animate) {
        editMode = false;
        b.layoutProfileView.setVisibility(View.VISIBLE);
        b.layoutProfileEdit.setVisibility(View.GONE);
        if (currentTeacher != null) bindSummary(currentTeacher);
        if (animate) UiAnimations.fadeIn(b.layoutProfileView);
    }

    private void setEditMode(boolean on) {
        editMode = on;
        if (on) {
            b.layoutProfileView.setVisibility(View.GONE);
            b.layoutProfileEdit.setVisibility(View.VISIBLE);
            if (currentTeacher != null) {
                b.etFullName.setText(nvl(currentTeacher.name));
                b.etPhone.setText(nvl(currentTeacher.phone));
                b.etUdise.setText(nvl(currentTeacher.udiseCode));
            }
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u != null && u.getEmail() != null) b.tvProfileEmail.setText(u.getEmail());
            UiAnimations.fadeIn(b.layoutProfileEdit);
        } else { setViewMode(true); }
    }

    private void bindSummary(Teacher t) {
        String name = (t.name != null && !t.name.isEmpty()) ? t.name : "—";
        b.tvSummaryName.setText(name);
        b.tvSummaryPhone.setText(getString(R.string.profile_summary_phone,
                (t.phone != null && !t.phone.isEmpty()) ? t.phone : "—"));
        b.tvSummaryUdise.setText(getString(R.string.profile_summary_udise,
                (t.udiseCode != null && !t.udiseCode.isEmpty()) ? t.udiseCode : "—"));
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null && u.getEmail() != null) b.tvProfileEmail.setText(u.getEmail());
        if (getActivity() instanceof HomeActivity)
            ((HomeActivity) getActivity()).updateToolbar(getString(R.string.nav_profile), name);
    }

    private void bindClassList(List<ProfileClassItem> items, Map<Integer, String> divSummary) {
        if (!isViewActive()) return;
        boolean anyClass = false;
        for (ProfileClassItem item : items) if (item.hasClass()) { anyClass = true; break; }
        b.tvNoClasses.setVisibility(anyClass ? View.GONE : View.VISIBLE);
        b.rvProfileClasses.setVisibility(View.VISIBLE);
        classAdapter.setData(items, divSummary);
        String selectedId = SessionContext.selectedClass != null ? SessionContext.selectedClass.id : null;
        classAdapter.setSelectedClassId(selectedId);
        if (selectedId != null) {
            for (ProfileClassItem item : items) {
                if (item.hasClass() && java.util.Objects.equals(selectedId, item.classModel.id)) {
                    showActiveClassDetail(item);
                    b.cardActiveClassDetail.setVisibility(View.VISIBLE);
                    return;
                }
            }
        }
        b.cardActiveClassDetail.setVisibility(View.GONE);
    }

    private void selectClass(ProfileClassItem item) {
        if (!item.hasClass()) return;
        ClassModel c = item.classModel;
        
        // Preassign default subjects if none exist for the class
        if (c.subjects == null || c.subjects.isEmpty()) {
            c.subjects = new ArrayList<>();
            c.subjects.add(new com.kartik.myschool.model.Subject("English", 100));
            c.subjects.add(new com.kartik.myschool.model.Subject("Mathematics", 100));
            c.subjects.add(new com.kartik.myschool.model.Subject("Science", 100));
            c.subjects.add(new com.kartik.myschool.model.Subject("Marathi", 100));
            FirebaseRepository.get().saveClass(c, new FirebaseRepository.OnResult<String>() {
                @Override public void onSuccess(String id) {}
                @Override public void onError(Exception e) {}
            });
        }
        
        SessionContext.selectedClass = c;
        if (c.yearId != null && !c.yearId.isEmpty()) {
            SessionContext.selectedYear = new AcademicYear();
            SessionContext.selectedYear.id = c.yearId;
            SessionContext.selectedYear.label = c.academicYearLabel != null ? c.academicYearLabel : "";
        }
        if (c.semesterId != null && !c.semesterId.isEmpty()) {
            Semester found = null;
            if (AppCache.cachedSemesters != null) {
                for (Semester s : AppCache.cachedSemesters) {
                    if (java.util.Objects.equals(c.semesterId, s.id)) {
                        found = s;
                        break;
                    }
                }
            }
            if (found != null) {
                SessionContext.selectedSemester = found;
            } else {
                SessionContext.selectedSemester = new Semester();
                SessionContext.selectedSemester.id = c.semesterId;
                SessionContext.selectedSemester.yearId = c.yearId;
                SessionContext.selectedSemester.name = "First Semester"; // fallback
            }
        }
        SessionContext.syncToAppCache();
        SessionContext.save(requireContext());
        classAdapter.setSelectedClassId(c.id);
        showActiveClassDetail(item);
        b.cardActiveClassDetail.setVisibility(View.VISIBLE);
        UiAnimations.fadeIn(b.cardActiveClassDetail);
        Toast.makeText(requireContext(), R.string.profile_class_selected, Toast.LENGTH_SHORT).show();
        if (getActivity() instanceof HomeActivity)
            ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
    }

    private void showActiveClassDetail(ProfileClassItem item) {
        ClassModel c = item.classModel;
        if (c == null) { b.cardActiveClassDetail.setVisibility(View.GONE); return; }
        String div = item.getDivision();
        b.tvActiveClassTitle.setText(getString(R.string.class_div_format, String.valueOf(item.standard), div));
        b.tvActiveClassStudents.setText(getString(R.string.profile_students_count, item.studentCount));

        String allDivs = buildDivisionsSummaryForStd(item.standard, loadedClasses);
        if (!allDivs.isEmpty()) {
            b.tvActiveClassDivisions.setText(getString(R.string.profile_divisions_in_std, allDivs));
            b.tvActiveClassDivisions.setVisibility(View.VISIBLE);
        } else { b.tvActiveClassDivisions.setVisibility(View.GONE); }

        StringBuilder extra = new StringBuilder();
        if (!empty(c.teacherName))          extra.append(getString(R.string.profile_teacher_line, c.teacherName));
        if (!empty(c.assistantTeacherName)) {
            if (extra.length() > 0) extra.append("\n");
            extra.append(getString(R.string.profile_asst_teacher_line, c.assistantTeacherName));
        }
        int total = totalStudentsForStd(item.standard);
        if (total > 0) {
            if (extra.length() > 0) extra.append("\n");
            extra.append(getString(R.string.profile_std_total_students, item.standard, total));
        }
        if (extra.length() > 0) {
            b.tvActiveClassExtra.setText(extra.toString());
            b.tvActiveClassExtra.setVisibility(View.VISIBLE);
        } else { b.tvActiveClassExtra.setVisibility(View.GONE); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save profile
    // ─────────────────────────────────────────────────────────────────────────
    private void saveProfile() {
        String name = str(b.etFullName);
        if (TextUtils.isEmpty(name)) { b.tilName.setError(getString(R.string.error_name_required)); return; }
        b.tilName.setError(null);
        Teacher t = currentTeacher != null ? currentTeacher : new Teacher();
        t.name = name; t.phone = str(b.etPhone); t.udiseCode = str(b.etUdise);
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) t.email = u.getEmail();
        FirebaseRepository.get().saveTeacher(t, new FirebaseRepository.OnResult<Void>() {
            @Override public void onSuccess(Void v) {
                if (!isViewActive()) return;
                currentTeacher = t;
                AppCache.cachedTeacherName = t.name; // keep cache fresh
                setViewMode(true);
                Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {
                    @Override public void onSuccess(School s) { SessionContext.selectedSchool = s; AppCache.selectedSchool = s; }
                    @Override public void onError(Exception e) {}
                });
            }
            @Override public void onError(Exception e) {
                if (isViewActive()) Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout).setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.logout, (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    SessionContext.clear(requireContext());
                    startActivity(new Intent(requireContext(), LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    requireActivity().finish();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data helpers
    // ─────────────────────────────────────────────────────────────────────────
    private static List<ProfileClassItem> buildItems(List<ClassModel> classes, Map<String, Integer> counts) {
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
                divs.sort((a, bb) -> nvlStr(a.division).compareTo(nvlStr(bb.division)));
                for (ClassModel c : divs) {
                    ProfileClassItem item = new ProfileClassItem(std, c);
                    if (c.id != null && counts.containsKey(c.id)) item.studentCount = counts.get(c.id);
                    items.add(item);
                }
            }
        }
        return items;
    }

    private int totalStudentsForStd(int std) {
        int total = 0;
        for (ProfileClassItem it : lastItems) if (it.standard == std && it.hasClass()) total += it.studentCount;
        return total;
    }

    private static Map<Integer, String> buildDivisionsSummaryMap(List<ProfileClassItem> items) {
        Map<Integer, List<String>> divs = new TreeMap<>();
        for (ProfileClassItem item : items) {
            if (!item.hasClass()) continue;
            divs.computeIfAbsent(item.standard, k -> new ArrayList<>()).add(item.getDivision());
        }
        Map<Integer, String> out = new HashMap<>();
        for (Map.Entry<Integer, List<String>> e : divs.entrySet()) out.put(e.getKey(), TextUtils.join(", ", e.getValue()));
        return out;
    }

    private static String buildDivisionsSummaryForStd(int std, List<ClassModel> classes) {
        List<String> divs = new ArrayList<>();
        if (classes != null) for (ClassModel c : classes)
            if (parseStd(c.className) == std && !empty(c.division)) divs.add(c.division);
        return TextUtils.join(", ", divs);
    }

    private static int parseStd(String cn) {
        if (cn == null) return -1;
        try { String n = cn.replaceAll("[^0-9]",""); return n.isEmpty() ? -1 : Integer.parseInt(n); }
        catch (NumberFormatException e) { return -1; }
    }

    private boolean isViewActive()         { return b != null && isAdded(); }
    private static boolean empty(String s) { return s == null || s.isEmpty(); }
    private static String nvlStr(String s) { return s != null ? s : ""; }
    private String nvl(String s)           { return s != null ? s : ""; }
    private String str(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override public void onResume() {
        super.onResume();
        if (isViewActive()) {
            if (isFirstLoad) isFirstLoad = false;
            else loadAllParallel();   // refresh on return from ClassSetupActivity etc.
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
