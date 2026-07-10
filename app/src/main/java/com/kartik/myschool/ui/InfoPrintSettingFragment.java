package com.kartik.myschool.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.kartik.myschool.AppCache;
import com.kartik.myschool.ClassSetupActivity;
import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.databinding.FragmentInfoPrintSettingBinding;
import com.kartik.myschool.model.AcademicYear;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Semester;
import com.kartik.myschool.model.Teacher;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;

import java.util.ArrayList;
import java.util.List;

public class InfoPrintSettingFragment extends Fragment {

    private FragmentInfoPrintSettingBinding b;
    private final List<AcademicYear> years     = new ArrayList<>();
    private final List<Semester>     semesters = new ArrayList<>();
    private final List<ClassModel>   classes   = new ArrayList<>();
    private int yearIndex = 0, semesterIndex = 0, classIndex = 0;
    private boolean entrancePlayed;
    private boolean isFirstLoad = true;
    private boolean isSwipeOngoing = false;
    private boolean isSecondPosterShowing = false;

    // Animation coordinates caching and tracking
    private float bookOrigX, bookOrigY;
    private float studentsOrigX, studentsOrigY;
    private float chartOrigX, chartOrigY;
    private float calendarOrigX, calendarOrigY;

    private float calculatorOrigX, calculatorOrigY;
    private float checkCircleOrigX, checkCircleOrigY;
    private float documentOrigX, documentOrigY;
    private float printOrigX, printOrigY;

    private boolean origValsCaptured = false;
    private boolean isCombineAnimRunning = false;
    private final List<Animator> headerAnimators = new ArrayList<>();

    private final android.os.Handler animHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable combineLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (isViewActive()) {
                if (!isCombineAnimRunning && b != null && b.headerBand.isEnabled()) {
                    playCombineAndLogoAnimation();
                }
                animHandler.postDelayed(this, 15000);
            }
        }
    };


    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentInfoPrintSettingBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).updateToolbar(
                    getString(R.string.nav_home), getString(R.string.subtitle_info_print));
        }

        b.btnYearPrev.setOnClickListener(v     -> cycleYear(-1));
        b.btnYearNext.setOnClickListener(v     -> cycleYear(1));
        b.btnSemesterPrev.setOnClickListener(v -> cycleSemester(-1));
        b.btnSemesterNext.setOnClickListener(v -> cycleSemester(1));
        b.btnClassPrev.setOnClickListener(v    -> cycleClass(-1));
        b.btnClassNext.setOnClickListener(v    -> cycleClass(1));

        b.headerBand.setOnClickListener(v -> togglePoster());
        b.ivAnimSchool.setOnClickListener(v -> playCombineAndLogoAnimation());
        b.ivAnimReports.setOnClickListener(v -> playCombineAndLogoAnimation());

        b.btnGoToClass.setOnClickListener(v -> { UiAnimations.pulse(b.btnGoToClass); goToClassStudents(); });
        b.btnAllClasses.setOnClickListener(v -> { UiAnimations.pulse(b.btnAllClasses); navigateWithAnim(R.id.nav_profile); });
        b.btnHowToUse.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnHowToUse);
            Bundle args = new Bundle();
            args.putString("url", "https://edu-report.in/how-to-use");
            args.putString("title", "?? ??? ????????");
            navigateWithAnim(R.id.nav_web_guide, args);
        });
        b.btnOnlineHelp.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnOnlineHelp);
            Bundle args = new Bundle();
            args.putString("url", "https://kartik-28deb.web.app/myschool_overview.mp4");
            args.putString("title", "??? (Help)");
            navigateWithAnim(R.id.nav_web_guide, args);
        });

        setupInteractiveSwipeListener(b.cardYear, b.cardYear,
                () -> cycleYear(1), () -> cycleYear(-1), this::showYearPickerDialog);
        setupInteractiveSwipeListener(b.cardSemester, b.cardSemester,
                () -> cycleSemester(1), () -> cycleSemester(-1), this::showSemesterPickerDialog);
        setupInteractiveSwipeListener(b.panelClass, b.panelClass,
                () -> cycleClass(1), () -> cycleClass(-1), () -> {
                    if (!classes.isEmpty()) { UiAnimations.pulse(b.panelClass); goToClassStudents(); }
                });
        b.panelClass.setOnLongClickListener(v -> { showClassPickerDialog(); return true; });



        playEntranceIfNeeded();
        loadTeacherName();   // instant from cache, then background refresh
        initSessionData();   // instant from AppCache, then background network sync
    }



    // ── Teacher name ──────────────────────────────────────────────────────────
    private void updateWelcomeHeader(String teacherName) {
        if (b != null && isAdded() && teacherName != null) {
            b.tvTeacherNameHeader.setText(getString(R.string.welcome_back_name, teacherName));
        }
    }

    /**
     * Two-phase load:
     * Phase 1 (sync, 0ms): read AppCache.cachedTeacherName → set TextView immediately.
     * Phase 2 (async):     Firestore call (hits FirebaseRepository in-memory cache after
     *                      first load, so also ~0ms from the second visit onward).
     */
    private void loadTeacherName() {
        // Phase 1 — instant
        if (AppCache.cachedTeacherName != null && b != null) {
            updateWelcomeHeader(AppCache.cachedTeacherName);
        }
        // Phase 2 — background refresh
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher t) {
                if (t == null || t.name == null) return;
                boolean wasBlank = (AppCache.cachedTeacherName == null);
                AppCache.cachedTeacherName = t.name;
                if (b != null) {
                    requireActivity().runOnUiThread(() -> {
                        updateWelcomeHeader(t.name);
                        if (wasBlank) UiAnimations.fadeIn(b.tvTeacherNameHeader);
                    });
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    // ── Session data init ─────────────────────────────────────────────────────
    /**
     * Show whatever is already in AppCache synchronously (instant UI), then fire
     * network calls to refresh. On a cache hit in FirebaseRepository the network
     * calls also complete near-instantly.
     */
    private void initSessionData() {
        // Restore from AppCache (instant)
        restoreYearsFromCache();
        restoreSemestersFromCache();
        restoreClassesFromCache();
        // Fire background refreshes
        ensureSchoolAndLoadData();
    }

    private void restoreYearsFromCache() {
        if (AppCache.cachedYears != null && !AppCache.cachedYears.isEmpty()) {
            years.clear(); years.addAll(AppCache.cachedYears);
            boolean found = false;
            if (SessionContext.selectedYear != null) {
                for (int i = 0; i < years.size(); i++) {
                    if (years.get(i).id != null && SessionContext.selectedYear.id != null && years.get(i).id.equals(SessionContext.selectedYear.id)) {
                        yearIndex = i; found = true; break;
                    }
                }
                if (!found) {
                    for (int i = 0; i < years.size(); i++) {
                        if (years.get(i).label != null && SessionContext.selectedYear.label != null && years.get(i).label.equals(SessionContext.selectedYear.label)) {
                            yearIndex = i; found = true; break;
                        }
                    }
                }
            }
            if (!found) {
                int foundIdx = 0;
                for (int i = 0; i < years.size(); i++) {
                    if (years.get(i).label != null && years.get(i).label.equals("2026-27")) {
                        foundIdx = i; break;
                    }
                }
                yearIndex = foundIdx;
                if (yearIndex < years.size()) {
                    SessionContext.selectedYear = years.get(yearIndex);
                    SessionContext.save(getContext());
                }
            }
        } else if (SessionContext.selectedYear != null) {
            if (SessionContext.selectedYear.label == null || SessionContext.selectedYear.label.trim().isEmpty()) {
                SessionContext.selectedYear.label = "2026-27";
            }
            years.clear(); years.add(SessionContext.selectedYear);
            yearIndex = 0;
        } else {
            years.clear();
            years.add(new AcademicYear("2026-27", 2026, 2027));
            yearIndex = 0;
            SessionContext.selectedYear = years.get(0);
            SessionContext.save(getContext());
        }
        applyYear(0);
    }

    private void restoreSemestersFromCache() {
        if (AppCache.cachedSemesters != null && !AppCache.cachedSemesters.isEmpty()) {
            semesters.clear(); semesters.addAll(AppCache.cachedSemesters);
            if (SessionContext.selectedSemester != null) {
                for (int i = 0; i < semesters.size(); i++) {
                    if (semesters.get(i).id != null && semesters.get(i).id.equals(SessionContext.selectedSemester.id)) {
                        semesterIndex = i; break;
                    }
                }
            } else {
                semesterIndex = 0;
                SessionContext.selectedSemester = semesters.get(semesterIndex);
                SessionContext.save(getContext());
            }
        } else if (SessionContext.selectedSemester != null) {
            if (SessionContext.selectedSemester.name == null || SessionContext.selectedSemester.name.trim().isEmpty()) {
                SessionContext.selectedSemester.name = "First Semester";
            }
            semesters.clear(); semesters.add(SessionContext.selectedSemester);
            semesterIndex = 0;
        } else {
            seedFallbackSemesters();
            semesterIndex = 0;
            SessionContext.selectedSemester = semesters.get(semesterIndex);
            SessionContext.save(getContext());
        }
        applySemester(0);
    }

    private void restoreClassesFromCache() {
        if (AppCache.cachedClasses != null && !AppCache.cachedClasses.isEmpty()) {
            classes.clear(); classes.addAll(AppCache.cachedClasses);
            if (SessionContext.selectedClass != null) {
                for (int i = 0; i < classes.size(); i++) {
                    if (classes.get(i).id != null && classes.get(i).id.equals(SessionContext.selectedClass.id)) {
                        classIndex = i; break;
                    }
                }
            }
        } else if (SessionContext.selectedClass != null) {
            classes.clear(); classes.add(SessionContext.selectedClass);
            classIndex = 0;
        }
        applyClass(0);
    }

    private void ensureSchoolAndLoadData() {
        if (SessionContext.selectedSchool != null) {
            loadYears();
        } else {
            FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
                @Override public void onSuccess(Teacher t) {
                    if (t != null && t.name != null) {
                        AppCache.cachedTeacherName = t.name;
                        if (b != null) {
                            requireActivity().runOnUiThread(() -> updateWelcomeHeader(t.name));
                        }
                    }
                    FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {
                        @Override public void onSuccess(School s) {
                            SessionContext.selectedSchool = s;
                            AppCache.selectedSchool = s;
                            SessionContext.save(getContext());
                            loadYears();
                        }
                        @Override public void onError(Exception e) { loadYears(); }
                    });
                }
                @Override public void onError(Exception e) { loadYears(); }
            });
        }
    }

    // ── Year loading chain ────────────────────────────────────────────────────
    private void loadYears() {
        FirebaseRepository.get().getAcademicYears(new FirebaseRepository.OnResult<List<AcademicYear>>() {
            @Override public void onSuccess(List<AcademicYear> list) {
                if (list == null || list.isEmpty()) {
                    FirebaseRepository.get().ensureDefaultYearAndSemesters(
                            new FirebaseRepository.OnResult<AcademicYear>() {
                                @Override public void onSuccess(AcademicYear y) {
                                    List<AcademicYear> s = new ArrayList<>(); s.add(y); bindYears(s);
                                }
                                @Override public void onError(Exception e) { bindYears(null); }
                            });
                } else { bindYears(list); }
            }
            @Override public void onError(Exception e) { bindYears(null); }
        });
    }

    private void bindYears(List<AcademicYear> list) {
        if (list == null) list = new ArrayList<>();
        
        // Filter out malformed/invalid years (like "2027" without a hyphen)
        for (int i = list.size() - 1; i >= 0; i--) {
            String lbl = list.get(i).label;
            if (lbl == null || !lbl.contains("-")) {
                list.remove(i);
            }
        }
        
        int currentCalendarYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        if (currentCalendarYear < 2024) currentCalendarYear = 2026;
        
        for (int i = -1; i <= 3; i++) {
            int start = currentCalendarYear + i;
            int end = start + 1;
            String label = start + "-" + String.valueOf(end).substring(2);
            boolean found = false;
            for (AcademicYear y : list) {
                if (y.label != null && y.label.equals(label)) { found = true; break; }
            }
            if (!found) {
                list.add(new AcademicYear(label, start, end));
            }
        }
        
        java.util.Collections.sort(list, (a, b) -> Integer.compare(b.startYear, a.startYear));

        years.clear();
        years.addAll(list);
        AppCache.cachedYears = new ArrayList<>(years);
        
        boolean found = false;
        if (SessionContext.selectedYear != null) {
            for (int i = 0; i < years.size(); i++) {
                if (years.get(i).id != null && SessionContext.selectedYear.id != null && years.get(i).id.equals(SessionContext.selectedYear.id)) {
                    yearIndex = i; found = true; break;
                }
            }
            if (!found) {
                for (int i = 0; i < years.size(); i++) {
                    if (years.get(i).label != null && SessionContext.selectedYear.label != null && years.get(i).label.equals(SessionContext.selectedYear.label)) {
                        yearIndex = i; found = true; break;
                    }
                }
            }
        }
        
        if (!found) {
            int foundIdx = 0;
            for (int i = 0; i < years.size(); i++) {
                if (years.get(i).label != null && years.get(i).label.equals("2026-27")) {
                    foundIdx = i; break;
                }
            }
            yearIndex = foundIdx;
            if (yearIndex < years.size()) {
                SessionContext.selectedYear = years.get(yearIndex);
                SessionContext.save(getContext());
            }
        }
        if (isViewActive()) { applyYear(0); loadSemesters(); }
    }

    private void loadSemesters() {
        AcademicYear y = getCurrentYear();
        if (y == null || y.id == null) {
            if (semesters.isEmpty()) {
                seedFallbackSemesters();
                semesterIndex = 0;
                SessionContext.selectedSemester = semesters.get(semesterIndex);
                SessionContext.save(getContext());
            }
            loadClasses(); return;
        }
        FirebaseRepository.get().getSemestersForYear(y.id, new FirebaseRepository.OnResult<List<Semester>>() {
            @Override public void onSuccess(List<Semester> list) {
                if (list == null || list.isEmpty()) {
                    if (semesters.isEmpty()) {
                        seedFallbackSemesters();
                        semesterIndex = 0;
                        SessionContext.selectedSemester = semesters.get(semesterIndex);
                        SessionContext.save(getContext());
                        if (isViewActive()) { applySemester(0); loadClasses(); }
                    } else {
                        loadClasses();
                    }
                    return;
                }
                semesters.clear();
                semesters.addAll(list);
                AppCache.cachedSemesters = new ArrayList<>(semesters);
                
                // Preserve selection if possible
                if (SessionContext.selectedSemester != null) {
                    for (int i = 0; i < semesters.size(); i++) {
                        if (semesters.get(i).id != null && semesters.get(i).id.equals(SessionContext.selectedSemester.id)) {
                            semesterIndex = i; break;
                        }
                    }
                } else {
                    semesterIndex = 0;
                    SessionContext.selectedSemester = semesters.get(semesterIndex);
                    SessionContext.save(getContext());
                }
                
                if (isViewActive()) { applySemester(0); loadClasses(); }
            }
            @Override public void onError(Exception e) {
                if (semesters.isEmpty()) {
                    seedFallbackSemesters();
                    semesterIndex = 0;
                    SessionContext.selectedSemester = semesters.get(semesterIndex);
                    SessionContext.save(getContext());
                    if (isViewActive()) { applySemester(0); loadClasses(); }
                } else {
                    loadClasses();
                }
            }
        });
    }

    private void seedFallbackSemesters() {
        semesters.clear();
        semesters.add(new Semester(1, "प्रथम सत्र",  "Easy Reports"));
        semesters.add(new Semester(2, "द्वितीय सत्र", "Final Reports"));
        AppCache.cachedSemesters = new ArrayList<>(semesters);
    }

    private void loadClasses() {
        loadClassesForSchool();
    }

    private void loadClassesForSchool() {
        if (SessionContext.selectedSchool == null) {
            if (classes.isEmpty()) {
                classes.clear(); AppCache.cachedClasses = new ArrayList<>(); applyClass(0);
            }
            return;
        }
        FirebaseRepository.get().getClassesForSchool(SessionContext.selectedSchool.id,
                new FirebaseRepository.OnResult<List<ClassModel>>() {
                    @Override public void onSuccess(List<ClassModel> list) {
                        if (list == null || list.isEmpty()) {
                            if (classes.isEmpty()) {
                                classes.clear(); AppCache.cachedClasses = new ArrayList<>(); applyClass(0);
                            }
                            return;
                        }
                        classes.clear(); classes.addAll(list);
                        AppCache.cachedClasses = new ArrayList<>(classes);
                        
                        // Preserve selection if possible
                        if (SessionContext.selectedClass != null) {
                            boolean found = false;
                            for (int i = 0; i < classes.size(); i++) {
                                if (classes.get(i).id != null && classes.get(i).id.equals(SessionContext.selectedClass.id)) {
                                    classIndex = i; found = true; break;
                                }
                            }
                            if (!found) classIndex = 0;
                        } else {
                            classIndex = 0;
                        }
                        
                        if (isViewActive()) { applyClass(0); }
                    }
                    @Override public void onError(Exception e) {
                        if (classes.isEmpty()) {
                            classes.clear(); AppCache.cachedClasses = new ArrayList<>();
                            if (isViewActive()) applyClass(0);
                        }
                    }
                });
    }

    // ── Cycle / apply ─────────────────────────────────────────────────────────
    private AcademicYear getCurrentYear() {
        if (years.isEmpty()) return null;
        yearIndex = Math.max(0, Math.min(yearIndex, years.size() - 1));
        return years.get(yearIndex);
    }

    private void processYearSelection(int index, int animDelta) {
        if (index < 0 || index >= years.size()) return;
        yearIndex = index;
        AcademicYear selected = years.get(index);
        
        if (selected.id == null) {
            FirebaseRepository.get().saveAcademicYear(selected, new FirebaseRepository.OnResult<String>() {
                @Override public void onSuccess(String newId) {
                    selected.id = newId;
                    FirebaseRepository.get().seedSemesters(newId, () -> {
                        if (isViewActive()) { applyYear(animDelta); loadSemesters(); }
                    });
                }
                @Override public void onError(Exception e) {
                    if (isViewActive()) { applyYear(animDelta); loadSemesters(); }
                }
            });
        } else {
            applyYear(animDelta);
            loadSemesters();
        }
    }

    private void cycleYear(int d) {
        if (years.size() <= 1) return;
        int nextIndex = (yearIndex + d + years.size()) % years.size();
        processYearSelection(nextIndex, d);
    }

    private void cycleSemester(int d) {
        if (semesters.size() <= 1) return;
        semesterIndex = (semesterIndex + d + semesters.size()) % semesters.size();
        applySemester(d);
    }

    private void cycleClass(int d) {
        if (classes.size() <= 1) return;
        classIndex = (classIndex + d + classes.size()) % classes.size();
        applyClass(d);
    }

    private void applyYear(int dir) {
        AcademicYear y = getCurrentYear(); if (y == null) return;
        SessionContext.selectedYear = y;
        SessionContext.save(getContext());
        if (!isViewActive()) return;
        b.tvYearLabel.setText(y.label != null ? y.label : getString(R.string.year_label, "—"));
        
        boolean showArrows = years.size() > 1;
        b.btnYearPrev.setVisibility(showArrows ? View.VISIBLE : View.INVISIBLE);
        b.btnYearNext.setVisibility(showArrows ? View.VISIBLE : View.INVISIBLE);
        
        if (dir != 0 && !isSwipeOngoing) UiAnimations.animateSelectorChange(b.tvYearLabel, dir);
    }

    private void applySemester(int dir) {
        if (semesters.isEmpty()) return;
        semesterIndex = Math.max(0, Math.min(semesterIndex, semesters.size() - 1));
        Semester s = semesters.get(semesterIndex);
        SessionContext.selectedSemester = s;
        SessionContext.save(getContext());
        if (!isViewActive()) return;
        String semName = s.name;
        if (semName != null) {
            if (semName.equals("First Semester") || semName.equals("प्रथम सत्र") || semName.toLowerCase().contains("first") || semName.contains("१") || semName.contains("1")) {
                semName = getString(R.string.txt_semester_1);
            } else if (semName.equals("Second Semester") || semName.equals("द्वितीय सत्र") || semName.toLowerCase().contains("second") || semName.contains("२") || semName.contains("2")) {
                semName = getString(R.string.txt_semester_2);
            }
        }
        b.tvSemesterName.setText(semName != null ? semName : "");
        
        boolean showArrows = semesters.size() > 1;
        b.btnSemesterPrev.setVisibility(showArrows ? View.VISIBLE : View.INVISIBLE);
        b.btnSemesterNext.setVisibility(showArrows ? View.VISIBLE : View.INVISIBLE);
        
        if (dir != 0 && !isSwipeOngoing) UiAnimations.animateSelectorChange(b.panelSemester, dir);
    }

    private void applyClass(int dir) {
        if (!isViewActive()) return;
        boolean empty = classes.isEmpty();
        b.tvNoClassHint.setVisibility(empty ? View.VISIBLE : View.GONE);
        b.btnGoToClass.setEnabled(!empty);
        if (empty) {
            SessionContext.selectedClass = null;
            SessionContext.save(getContext());
            b.tvClassNumberBig.setText("—");
            b.tvClassNumberBig.setTextColor(Color.parseColor("#5E35B1"));
            b.tvClassDivLabel.setText(getString(R.string.home_no_class_hint));
            b.frameClassNumCircle.setBackground(
                    requireContext().getDrawable(R.drawable.bg_class_num_normal));
            b.panelClass.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#E8EAF6")));
            b.tvClassStatusBadge.setText(R.string.msg_activate_it);
            b.tvClassStatusBadge.setTextColor(Color.parseColor("#E65100"));
            b.tvClassStatusBadge.setBackground(
                    requireContext().getDrawable(R.drawable.bg_pill_activate));
            b.ivClassArrow.setImageTintList(ColorStateList.valueOf(Color.parseColor("#C5CAE9")));
            
            b.btnClassPrev.setVisibility(View.INVISIBLE);
            b.btnClassNext.setVisibility(View.INVISIBLE);
            return;
        }
        classIndex = Math.max(0, Math.min(classIndex, classes.size() - 1));
        ClassModel c = classes.get(classIndex);

        // Check if this class is the globally activated session class
        boolean isActivated = SessionContext.selectedClass != null
                && java.util.Objects.equals(SessionContext.selectedClass.id, c.id);

        // Activate it when sliding to a new class (sets session class)
        SessionContext.selectedClass = c;
        SessionContext.syncToAppCache();
        SessionContext.save(getContext());

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).updateToolbar(getString(R.string.nav_home), SessionContext.getClassDivLabel());
        }

        String num = c.className != null ? c.className : "—";
        String div = c.division  != null && !c.division.isEmpty() ? c.division : "-";
        b.tvClassNumberBig.setText(num);
        b.tvClassDivLabel.setText(getString(R.string.class_div_format, num, div));

        // Always activated when we select it via slider — mark green
        // (First slide always activates, which is the desired behavior)
        b.frameClassNumCircle.setBackground(
                requireContext().getDrawable(R.drawable.bg_class_num_activated));
        b.tvClassNumberBig.setTextColor(Color.parseColor("#2E7D32"));
        b.panelClass.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#A5D6A7")));
        b.panelClass.setCardBackgroundColor(Color.parseColor("#F9FFF9"));
        b.tvClassStatusBadge.setText(R.string.msg_activated);
        b.tvClassStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
        b.tvClassStatusBadge.setBackground(
                requireContext().getDrawable(R.drawable.bg_pill_activated));
        b.ivClassArrow.setImageTintList(ColorStateList.valueOf(Color.parseColor("#66BB6A")));

        boolean showArrows = classes.size() > 1;
        b.btnClassPrev.setVisibility(showArrows ? View.VISIBLE : View.INVISIBLE);
        b.btnClassNext.setVisibility(showArrows ? View.VISIBLE : View.INVISIBLE);

        if (dir != 0 && !isSwipeOngoing) UiAnimations.animateSelectorChange(b.panelClass, dir);
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void navigateWithAnim(int destId) {
        navigateWithAnim(destId, null);
    }
    
    private void navigateWithAnim(int destId, Bundle args) {
        if (getActivity() instanceof HomeActivity)
            ((HomeActivity) getActivity()).navigateTo(destId, args);
        else
            Navigation.findNavController(requireView()).navigate(destId, args, UiAnimations.navSlideForward());
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

    // ── Entrance animation ────────────────────────────────────────────────────
    private void playEntranceIfNeeded() {
        if (entrancePlayed || b == null) return;
        entrancePlayed = true;
        b.homeRoot.setAlpha(1f);
        b.homeRoot.post(() -> {
            if (!isViewActive()) return;
            UiAnimations.fadeIn(b.headerBand);
            UiAnimations.fadeIn(b.cardMain);
            UiAnimations.staggerFadeIn(
                b.layoutYearSection,
                b.layoutSemesterSection,
                b.layoutClassSection,
                b.rowButtons1,
                b.rowButtons2,
                b.panelBottomInstructions
            );
            captureOriginalTranslations();
            startHeaderAnimations();
        });
    }

    // ── Header illustration animations ───────────────────────────────────────
    private void captureOriginalTranslations() {
        if (origValsCaptured || b == null) return;
        bookOrigX = b.frameBook.getTranslationX();
        bookOrigY = b.frameBook.getTranslationY();
        studentsOrigX = b.frameStudents.getTranslationX();
        studentsOrigY = b.frameStudents.getTranslationY();
        chartOrigX = b.frameChart.getTranslationX();
        chartOrigY = b.frameChart.getTranslationY();
        calendarOrigX = b.frameCalendar.getTranslationX();
        calendarOrigY = b.frameCalendar.getTranslationY();

        calculatorOrigX = b.frameCalculator.getTranslationX();
        calculatorOrigY = b.frameCalculator.getTranslationY();
        checkCircleOrigX = b.frameCheckCircle.getTranslationX();
        checkCircleOrigY = b.frameCheckCircle.getTranslationY();
        documentOrigX = b.frameDocument.getTranslationX();
        documentOrigY = b.frameDocument.getTranslationY();
        printOrigX = b.framePrint.getTranslationX();
        printOrigY = b.framePrint.getTranslationY();

        origValsCaptured = true;
    }

    // ── Header illustration animations ───────────────────────────────────────
    private void startHeaderAnimations() {
        if (!isViewActive()) return;

        // Ensure clean state before running loops
        stopHeaderAnimators();

        // --- FIRST POSTER ANIMATIONS ---
        // 1. Outer ring: slow continuous pulse scale
        ObjectAnimator outerScaleX = ObjectAnimator.ofFloat(b.ivRingOuter, "scaleX", 0.85f, 1.12f);
        ObjectAnimator outerScaleY = ObjectAnimator.ofFloat(b.ivRingOuter, "scaleY", 0.85f, 1.12f);
        ObjectAnimator outerAlpha  = ObjectAnimator.ofFloat(b.ivRingOuter, "alpha",  0.3f, 0.65f);
        outerScaleX.setDuration(2000); outerScaleX.setRepeatMode(ValueAnimator.REVERSE); outerScaleX.setRepeatCount(ValueAnimator.INFINITE);
        outerScaleY.setDuration(2000); outerScaleY.setRepeatMode(ValueAnimator.REVERSE); outerScaleY.setRepeatCount(ValueAnimator.INFINITE);
        outerAlpha.setDuration(2000);  outerAlpha.setRepeatMode(ValueAnimator.REVERSE);  outerAlpha.setRepeatCount(ValueAnimator.INFINITE);
        AnimatorSet outerSet = new AnimatorSet();
        outerSet.playTogether(outerScaleX, outerScaleY, outerAlpha);
        outerSet.setInterpolator(new AccelerateDecelerateInterpolator());
        outerSet.start();
        headerAnimators.add(outerSet);

        // 2. Inner ring: counter-phase pulse (offset timing)
        ObjectAnimator innerScaleX = ObjectAnimator.ofFloat(b.ivRingInner, "scaleX", 0.9f, 1.08f);
        ObjectAnimator innerScaleY = ObjectAnimator.ofFloat(b.ivRingInner, "scaleY", 0.9f, 1.08f);
        innerScaleX.setDuration(1600); innerScaleX.setRepeatMode(ValueAnimator.REVERSE); innerScaleX.setRepeatCount(ValueAnimator.INFINITE);
        innerScaleY.setDuration(1600); innerScaleY.setRepeatMode(ValueAnimator.REVERSE); innerScaleY.setRepeatCount(ValueAnimator.INFINITE);
        innerScaleX.setStartDelay(400); innerScaleY.setStartDelay(400);
        AnimatorSet innerSet = new AnimatorSet();
        innerSet.playTogether(innerScaleX, innerScaleY);
        innerSet.setInterpolator(new AccelerateDecelerateInterpolator());
        innerSet.start();
        headerAnimators.add(innerSet);

        // 3. School icon: slow breathe scale
        ObjectAnimator schoolBreath = ObjectAnimator.ofFloat(b.ivAnimSchool, "scaleX", 0.92f, 1.08f);
        ObjectAnimator schoolBreathY = ObjectAnimator.ofFloat(b.ivAnimSchool, "scaleY", 0.92f, 1.08f);
        schoolBreath.setDuration(1800);  schoolBreath.setRepeatMode(ValueAnimator.REVERSE);  schoolBreath.setRepeatCount(ValueAnimator.INFINITE);
        schoolBreathY.setDuration(1800); schoolBreathY.setRepeatMode(ValueAnimator.REVERSE); schoolBreathY.setRepeatCount(ValueAnimator.INFINITE);
        AnimatorSet schoolSet = new AnimatorSet();
        schoolSet.playTogether(schoolBreath, schoolBreathY);
        schoolSet.setInterpolator(new AccelerateDecelerateInterpolator());
        schoolSet.start();
        headerAnimators.add(schoolSet);

        // 4. Floating satellite icons — staggered bob up/down (relative to original XML positions)
        float density = getResources().getDisplayMetrics().density;
        float[] offsets = {0f, 500f, 900f, 1300f};
        View[] floaters = {b.frameBook, b.frameStudents, b.frameChart, b.frameCalendar};
        float[] origYs = {bookOrigY, studentsOrigY, chartOrigY, calendarOrigY};
        float[] amplitudes = {-8f * density, -6f * density, -7f * density, -5f * density};
        int[] durations   = {2000,  1700,  2300,  1900};
        
        for (int i = 0; i < floaters.length; i++) {
            ObjectAnimator bobY = ObjectAnimator.ofFloat(floaters[i], "translationY", origYs[i], origYs[i] + amplitudes[i]);
            bobY.setDuration(durations[i]);
            bobY.setRepeatMode(ValueAnimator.REVERSE);
            bobY.setRepeatCount(ValueAnimator.INFINITE);
            bobY.setStartDelay((long) offsets[i]);
            bobY.setInterpolator(new AccelerateDecelerateInterpolator());
            bobY.start();
            headerAnimators.add(bobY);

            // Pop-in is only executed during the initial entrance flow.
            // On subsequent start calls, we reset state to normal float position if not animating.
            if (!entrancePlayed) {
                floaters[i].setScaleX(0f); floaters[i].setScaleY(0f); floaters[i].setAlpha(0f);
                ObjectAnimator popX = ObjectAnimator.ofFloat(floaters[i], "scaleX", 0f, 1f);
                ObjectAnimator popY = ObjectAnimator.ofFloat(floaters[i], "scaleY", 0f, 1f);
                ObjectAnimator popA = ObjectAnimator.ofFloat(floaters[i], "alpha",  0f, 1f);
                popX.setDuration(500); popY.setDuration(500); popA.setDuration(400);
                popX.setStartDelay(300 + (long)(i * 120));
                popY.setStartDelay(300 + (long)(i * 120));
                popA.setStartDelay(300 + (long)(i * 120));
                popX.setInterpolator(new OvershootInterpolator(1.5f));
                popY.setInterpolator(new OvershootInterpolator(1.5f));
                AnimatorSet popSet = new AnimatorSet();
                popSet.playTogether(popX, popY, popA);
                popSet.start();
            } else {
                if (!isCombineAnimRunning) {
                    floaters[i].setScaleX(1f);
                    floaters[i].setScaleY(1f);
                    floaters[i].setAlpha(1f);
                    floaters[i].setTranslationX(i == 0 ? bookOrigX : (i == 1 ? studentsOrigX : (i == 2 ? chartOrigX : calendarOrigX)));
                }
            }
        }

        // 5. Sparkle dots — twinkle alpha
        View[] dots = {b.dotSpark1, b.dotSpark2, b.dotSpark3};
        long[] dotDelays = {0, 700, 1200};
        int[] dotDurations = {1200, 900, 1500};
        for (int i = 0; i < dots.length; i++) {
            ObjectAnimator twinkle = ObjectAnimator.ofFloat(dots[i], "alpha", 0.1f, 1.0f);
            twinkle.setDuration(dotDurations[i]);
            twinkle.setRepeatMode(ValueAnimator.REVERSE);
            twinkle.setRepeatCount(ValueAnimator.INFINITE);
            twinkle.setStartDelay(dotDelays[i]);
            twinkle.setInterpolator(new AccelerateDecelerateInterpolator());
            twinkle.start();
            headerAnimators.add(twinkle);

            ObjectAnimator twinkleScale = ObjectAnimator.ofFloat(dots[i], "scaleX", 0.5f, 1.3f);
            ObjectAnimator twinkleScaleY = ObjectAnimator.ofFloat(dots[i], "scaleY", 0.5f, 1.3f);
            twinkleScale.setDuration(dotDurations[i]);  twinkleScale.setRepeatMode(ValueAnimator.REVERSE);  twinkleScale.setRepeatCount(ValueAnimator.INFINITE); twinkleScale.setStartDelay(dotDelays[i]);
            twinkleScaleY.setDuration(dotDurations[i]); twinkleScaleY.setRepeatMode(ValueAnimator.REVERSE); twinkleScaleY.setRepeatCount(ValueAnimator.INFINITE); twinkleScaleY.setStartDelay(dotDelays[i]);
            twinkleScale.start(); twinkleScaleY.start();
            headerAnimators.add(twinkleScale);
            headerAnimators.add(twinkleScaleY);
        }

        // --- SECOND POSTER ANIMATIONS ---
        // 1. Outer ring second: slow continuous pulse scale
        ObjectAnimator outerScaleXSecond = ObjectAnimator.ofFloat(b.ivRingOuterSecond, "scaleX", 0.85f, 1.12f);
        ObjectAnimator outerScaleYSecond = ObjectAnimator.ofFloat(b.ivRingOuterSecond, "scaleY", 0.85f, 1.12f);
        ObjectAnimator outerAlphaSecond  = ObjectAnimator.ofFloat(b.ivRingOuterSecond, "alpha",  0.3f, 0.65f);
        outerScaleXSecond.setDuration(2000); outerScaleXSecond.setRepeatMode(ValueAnimator.REVERSE); outerScaleXSecond.setRepeatCount(ValueAnimator.INFINITE);
        outerScaleYSecond.setDuration(2000); outerScaleYSecond.setRepeatMode(ValueAnimator.REVERSE); outerScaleYSecond.setRepeatCount(ValueAnimator.INFINITE);
        outerAlphaSecond.setDuration(2000);  outerAlphaSecond.setRepeatMode(ValueAnimator.REVERSE);  outerAlphaSecond.setRepeatCount(ValueAnimator.INFINITE);
        AnimatorSet outerSetSecond = new AnimatorSet();
        outerSetSecond.playTogether(outerScaleXSecond, outerScaleYSecond, outerAlphaSecond);
        outerSetSecond.setInterpolator(new AccelerateDecelerateInterpolator());
        outerSetSecond.start();
        headerAnimators.add(outerSetSecond);

        // 2. Inner ring second: counter-phase pulse (offset timing)
        ObjectAnimator innerScaleXSecond = ObjectAnimator.ofFloat(b.ivRingInnerSecond, "scaleX", 0.9f, 1.08f);
        ObjectAnimator innerScaleYSecond = ObjectAnimator.ofFloat(b.ivRingInnerSecond, "scaleY", 0.9f, 1.08f);
        innerScaleXSecond.setDuration(1600); innerScaleXSecond.setRepeatMode(ValueAnimator.REVERSE); innerScaleXSecond.setRepeatCount(ValueAnimator.INFINITE);
        innerScaleYSecond.setDuration(1600); innerScaleYSecond.setRepeatMode(ValueAnimator.REVERSE); innerScaleYSecond.setRepeatCount(ValueAnimator.INFINITE);
        innerScaleXSecond.setStartDelay(400); innerScaleYSecond.setStartDelay(400);
        AnimatorSet innerSetSecond = new AnimatorSet();
        innerSetSecond.playTogether(innerScaleXSecond, innerScaleYSecond);
        innerSetSecond.setInterpolator(new AccelerateDecelerateInterpolator());
        innerSetSecond.start();
        headerAnimators.add(innerSetSecond);

        // 3. Reports icon second: slow breathe scale
        ObjectAnimator reportsBreath = ObjectAnimator.ofFloat(b.ivAnimReports, "scaleX", 0.92f, 1.08f);
        ObjectAnimator reportsBreathY = ObjectAnimator.ofFloat(b.ivAnimReports, "scaleY", 0.92f, 1.08f);
        reportsBreath.setDuration(1800);  reportsBreath.setRepeatMode(ValueAnimator.REVERSE);  reportsBreath.setRepeatCount(ValueAnimator.INFINITE);
        reportsBreathY.setDuration(1800); reportsBreathY.setRepeatMode(ValueAnimator.REVERSE); reportsBreathY.setRepeatCount(ValueAnimator.INFINITE);
        AnimatorSet reportsSet = new AnimatorSet();
        reportsSet.playTogether(reportsBreath, reportsBreathY);
        reportsSet.setInterpolator(new AccelerateDecelerateInterpolator());
        reportsSet.start();
        headerAnimators.add(reportsSet);

        // 4. Floating satellite icons for second poster (relative to original XML positions)
        View[] floatersSecond = {b.frameCalculator, b.frameCheckCircle, b.frameDocument, b.framePrint};
        float[] origYsSecond = {calculatorOrigY, checkCircleOrigY, documentOrigY, printOrigY};
        for (int i = 0; i < floatersSecond.length; i++) {
            ObjectAnimator bobY = ObjectAnimator.ofFloat(floatersSecond[i], "translationY", origYsSecond[i], origYsSecond[i] + amplitudes[i]);
            bobY.setDuration(durations[i]);
            bobY.setRepeatMode(ValueAnimator.REVERSE);
            bobY.setRepeatCount(ValueAnimator.INFINITE);
            bobY.setStartDelay((long) offsets[i]);
            bobY.setInterpolator(new AccelerateDecelerateInterpolator());
            bobY.start();
            headerAnimators.add(bobY);

            if (!entrancePlayed) {
                floatersSecond[i].setScaleX(0f); floatersSecond[i].setScaleY(0f); floatersSecond[i].setAlpha(0f);
                ObjectAnimator popX = ObjectAnimator.ofFloat(floatersSecond[i], "scaleX", 0f, 1f);
                ObjectAnimator popY = ObjectAnimator.ofFloat(floatersSecond[i], "scaleY", 0f, 1f);
                ObjectAnimator popA = ObjectAnimator.ofFloat(floatersSecond[i], "alpha",  0f, 1f);
                popX.setDuration(500); popY.setDuration(500); popA.setDuration(400);
                popX.setStartDelay(300 + (long)(i * 120));
                popY.setStartDelay(300 + (long)(i * 120));
                popA.setStartDelay(300 + (long)(i * 120));
                popX.setInterpolator(new OvershootInterpolator(1.5f));
                popY.setInterpolator(new OvershootInterpolator(1.5f));
                AnimatorSet popSet = new AnimatorSet();
                popSet.playTogether(popX, popY, popA);
                popSet.start();
            } else {
                if (!isCombineAnimRunning) {
                    floatersSecond[i].setScaleX(1f);
                    floatersSecond[i].setScaleY(1f);
                    floatersSecond[i].setAlpha(1f);
                    floatersSecond[i].setTranslationX(i == 0 ? calculatorOrigX : (i == 1 ? checkCircleOrigX : (i == 2 ? documentOrigX : printOrigX)));
                }
            }
        }

        // 5. Sparkle dots for second poster
        View[] dotsSecond = {b.dotSpark1Second, b.dotSpark2Second, b.dotSpark3Second};
        for (int i = 0; i < dotsSecond.length; i++) {
            ObjectAnimator twinkle = ObjectAnimator.ofFloat(dotsSecond[i], "alpha", 0.1f, 1.0f);
            twinkle.setDuration(dotDurations[i]);
            twinkle.setRepeatMode(ValueAnimator.REVERSE);
            twinkle.setRepeatCount(ValueAnimator.INFINITE);
            twinkle.setStartDelay(dotDelays[i]);
            twinkle.setInterpolator(new AccelerateDecelerateInterpolator());
            twinkle.start();
            headerAnimators.add(twinkle);

            ObjectAnimator twinkleScale = ObjectAnimator.ofFloat(dotsSecond[i], "scaleX", 0.5f, 1.3f);
            ObjectAnimator twinkleScaleY = ObjectAnimator.ofFloat(dotsSecond[i], "scaleY", 0.5f, 1.3f);
            twinkleScale.setDuration(dotDurations[i]);  twinkleScale.setRepeatMode(ValueAnimator.REVERSE);  twinkleScale.setRepeatCount(ValueAnimator.INFINITE); twinkleScale.setStartDelay(dotDelays[i]);
            twinkleScaleY.setDuration(dotDurations[i]); twinkleScaleY.setRepeatMode(ValueAnimator.REVERSE); twinkleScaleY.setRepeatCount(ValueAnimator.INFINITE); twinkleScaleY.setStartDelay(dotDelays[i]);
            twinkleScale.start(); twinkleScaleY.start();
            headerAnimators.add(twinkleScale);
            headerAnimators.add(twinkleScaleY);
        }
    }

    private void stopHeaderAnimators() {
        for (Animator anim : headerAnimators) {
            if (anim != null) {
                anim.cancel();
            }
        }
        headerAnimators.clear();
    }

    private void playCombineAndLogoAnimation() {
        if (isCombineAnimRunning || b == null) return;
        isCombineAnimRunning = true;

        // Temporarily lock header poster toggling to prevent layout collision
        b.headerBand.setEnabled(false);

        // Cancel running continuous bobbing/pulsing animations
        stopHeaderAnimators();

        final boolean isSecond = isSecondPosterShowing;
        final View centerIcon = isSecond ? b.ivAnimReports : b.ivAnimSchool;
        final View appLogo = isSecond ? b.ivAnimLogoSecond : b.ivAnimLogo;
        final View[] floaters = isSecond
                ? new View[]{b.frameCalculator, b.frameCheckCircle, b.frameDocument, b.framePrint}
                : new View[]{b.frameBook, b.frameStudents, b.frameChart, b.frameCalendar};

        final float[] origXs = isSecond
                ? new float[]{calculatorOrigX, checkCircleOrigX, documentOrigX, printOrigX}
                : new float[]{bookOrigX, studentsOrigX, chartOrigX, calendarOrigX};

        final float[] origYs = isSecond
                ? new float[]{calculatorOrigY, checkCircleOrigY, documentOrigY, printOrigY}
                : new float[]{bookOrigY, studentsOrigY, chartOrigY, calendarOrigY};

        // 1. Implode floating views and center main icon to the center
        List<Animator> implodeAnims = new ArrayList<>();

        // Center main icon shrinks to 0 and fades out
        implodeAnims.add(ObjectAnimator.ofFloat(centerIcon, "scaleX", 1f, 0f));
        implodeAnims.add(ObjectAnimator.ofFloat(centerIcon, "scaleY", 1f, 0f));
        implodeAnims.add(ObjectAnimator.ofFloat(centerIcon, "alpha", 1f, 0f));

        // Floating badges glide to center (translationX/Y -> 0), shrink and fade
        for (int i = 0; i < floaters.length; i++) {
            implodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "translationX", floaters[i].getTranslationX(), 0f));
            implodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "translationY", floaters[i].getTranslationY(), 0f));
            implodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "scaleX", floaters[i].getScaleX(), 0f));
            implodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "scaleY", floaters[i].getScaleY(), 0f));
            implodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "alpha", floaters[i].getAlpha(), 0f));
        }

        AnimatorSet implodeSet = new AnimatorSet();
        implodeSet.playTogether(implodeAnims);
        implodeSet.setDuration(500);
        implodeSet.setInterpolator(new AccelerateDecelerateInterpolator());
        implodeSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (b == null) return;
                
                // 2. Reveal app logo with an Overshoot bounce effect
                appLogo.setVisibility(View.VISIBLE);
                appLogo.setScaleX(0f);
                appLogo.setScaleY(0f);
                appLogo.setAlpha(0f);

                List<Animator> logoRevealAnims = new ArrayList<>();
                logoRevealAnims.add(ObjectAnimator.ofFloat(appLogo, "scaleX", 0f, 1f));
                logoRevealAnims.add(ObjectAnimator.ofFloat(appLogo, "scaleY", 0f, 1f));
                logoRevealAnims.add(ObjectAnimator.ofFloat(appLogo, "alpha", 0f, 1f));

                AnimatorSet logoRevealSet = new AnimatorSet();
                logoRevealSet.playTogether(logoRevealAnims);
                logoRevealSet.setDuration(500);
                logoRevealSet.setInterpolator(new OvershootInterpolator(1.4f));
                logoRevealSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (b == null) return;

                        // 3. Hold logo centered for 1500ms before returning to normal poster state
                        b.headerBand.postDelayed(() -> {
                            if (b == null) return;

                            // 4. Explode all elements back outwards and fade out the logo
                            List<Animator> explodeAnims = new ArrayList<>();

                            explodeAnims.add(ObjectAnimator.ofFloat(appLogo, "scaleX", 1f, 0f));
                            explodeAnims.add(ObjectAnimator.ofFloat(appLogo, "scaleY", 1f, 0f));
                            explodeAnims.add(ObjectAnimator.ofFloat(appLogo, "alpha", 1f, 0f));

                            explodeAnims.add(ObjectAnimator.ofFloat(centerIcon, "scaleX", 0f, 1f));
                            explodeAnims.add(ObjectAnimator.ofFloat(centerIcon, "scaleY", 0f, 1f));
                            explodeAnims.add(ObjectAnimator.ofFloat(centerIcon, "alpha", 0f, 1f));

                            for (int i = 0; i < floaters.length; i++) {
                                explodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "translationX", 0f, origXs[i]));
                                explodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "translationY", 0f, origYs[i]));
                                explodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "scaleX", 0f, 1f));
                                explodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "scaleY", 0f, 1f));
                                explodeAnims.add(ObjectAnimator.ofFloat(floaters[i], "alpha", 0f, 1f));
                            }

                            AnimatorSet explodeSet = new AnimatorSet();
                            explodeSet.playTogether(explodeAnims);
                            explodeSet.setDuration(600);
                            explodeSet.setInterpolator(new OvershootInterpolator(1.3f));
                            explodeSet.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (b == null) return;
                                    appLogo.setVisibility(View.GONE);
                                    isCombineAnimRunning = false;
                                    b.headerBand.setEnabled(true);
                                    
                                    // Resume standard floating and breathing animations
                                    startHeaderAnimations();
                                }
                            });
                            explodeSet.start();

                        }, 1500);
                    }
                });
                logoRevealSet.start();
            }
        });
        implodeSet.start();
    }

    private void togglePoster() {
        if (b == null) return;
        b.headerBand.setEnabled(false); // disable during animation to prevent overlapping animation cycles
        
        float width = b.frameHeaderAnim.getWidth() > 0 ? b.frameHeaderAnim.getWidth() : 500f;
        float offset = width + 50f;

        if (!isSecondPosterShowing) {
            // Slide current poster left and fade out
            b.frameHeaderAnim.animate()
                    .translationX(-offset)
                    .alpha(0f)
                    .setDuration(450)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        b.frameHeaderAnim.setVisibility(View.GONE);
                    })
                    .start();

            // Slide new poster in from right and fade in
            b.frameHeaderAnimSecond.setVisibility(View.VISIBLE);
            b.frameHeaderAnimSecond.setTranslationX(offset);
            b.frameHeaderAnimSecond.setAlpha(0f);
            b.frameHeaderAnimSecond.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .setInterpolator(new OvershootInterpolator(0.9f))
                    .withEndAction(() -> {
                        isSecondPosterShowing = true;
                        b.headerBand.setEnabled(true);
                    })
                    .start();
        } else {
            // Slide current poster right and fade out
            b.frameHeaderAnimSecond.animate()
                    .translationX(offset)
                    .alpha(0f)
                    .setDuration(450)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        b.frameHeaderAnimSecond.setVisibility(View.GONE);
                    })
                    .start();

            // Slide original poster in from left and fade in
            b.frameHeaderAnim.setVisibility(View.VISIBLE);
            b.frameHeaderAnim.setTranslationX(-offset);
            b.frameHeaderAnim.setAlpha(0f);
            b.frameHeaderAnim.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .setInterpolator(new OvershootInterpolator(0.9f))
                    .withEndAction(() -> {
                        isSecondPosterShowing = false;
                        b.headerBand.setEnabled(true);
                    })
                    .start();
        }
    }

    // ── Picker dialogs ────────────────────────────────────────────────────────
    private void showYearPickerDialog() {
        if (years.isEmpty()) return;
        String[] names = new String[years.size()];
        for (int i = 0; i < years.size(); i++) {
            names[i] = years.get(i).label != null ? years.get(i).label : "—";
        }
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.msg_select_year)
                .setItems(names, (d, w) -> processYearSelection(w, 0)).show();
    }

    private void showSemesterPickerDialog() {
        if (semesters.isEmpty()) return;
        String[] names = new String[semesters.size()];
        for (int i = 0; i < semesters.size(); i++) {
            String semName = semesters.get(i).name;
            if (semName != null) {
                if (semName.equals("First Semester") || semName.equals("प्रथम सत्र") || semName.toLowerCase().contains("first") || semName.contains("१") || semName.contains("1")) {
                    semName = getString(R.string.txt_semester_1);
                } else if (semName.equals("Second Semester") || semName.equals("द्वितीय सत्र") || semName.toLowerCase().contains("second") || semName.contains("२") || semName.contains("2")) {
                    semName = getString(R.string.txt_semester_2);
                }
            }
            names[i] = semName;
        }
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.msg_select_semester).setItems(names, (d, w) -> { semesterIndex = w; applySemester(0); }).show();
    }

    private void showClassPickerDialog() {
        if (classes.isEmpty()) return;
        String[] names = new String[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            String n = classes.get(i).className != null ? classes.get(i).className : "";
            String d = classes.get(i).division  != null && !classes.get(i).division.isEmpty() ? classes.get(i).division : "-";
            names[i] = getString(R.string.class_div_format, n, d);
        }
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.msg_select_class).setItems(names, (d, w) -> { classIndex = w; applyClass(0); }).show();
    }

    // ── Swipe gesture helper ──────────────────────────────────────────────────
    private void setupInteractiveSwipeListener(View swipeTarget, View animationTarget, Runnable onLeftSwipe, Runnable onRightSwipe, Runnable onTap) {
        swipeTarget.setOnTouchListener(new View.OnTouchListener() {
            private float startX = 0f;
            private float startY = 0f;
            private boolean isDragging = false;
            private static final int SWIPE_THRESHOLD_DP = 70;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                float density = v.getResources().getDisplayMetrics().density;
                float threshold = SWIPE_THRESHOLD_DP * density;

                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        isDragging = false;
                        animationTarget.animate().cancel();
                        return true;

                    case android.view.MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - startX;
                        float deltaY = event.getRawY() - startY;

                        // Check if we have enough items to drag/scroll
                        boolean canSwipe = true;
                        if (swipeTarget == b.cardYear && years.size() <= 1) canSwipe = false;
                        if (swipeTarget == b.cardSemester && semesters.size() <= 1) canSwipe = false;
                        if (swipeTarget == b.panelClass && classes.size() <= 1) canSwipe = false;

                        if (!canSwipe) {
                            return true;
                        }

                        // Detect drag start if horizontal movement is prominent
                        if (!isDragging && Math.abs(deltaX) > 10 * density && Math.abs(deltaX) > Math.abs(deltaY)) {
                            isDragging = true;
                            // Disallow parent scroll interception so drag is smooth
                            if (v.getParent() != null) {
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }

                        if (isDragging) {
                            // Move the card under the finger with a slight resistance
                            float translation = deltaX * 0.75f;
                            animationTarget.setTranslationX(translation);
                            
                            // Fade slightly as we drag it away
                            float alpha = 1.0f - (Math.abs(translation) / (v.getWidth() * 1.5f));
                            animationTarget.setAlpha(Math.max(0.5f, alpha));
                        }
                        return true;

                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        float finalDeltaX = event.getRawX() - startX;
                        float finalDeltaY = event.getRawY() - startY;

                        if (isDragging) {
                            if (Math.abs(finalDeltaX) > threshold) {
                                // Swipe confirmed!
                                boolean isLeft = finalDeltaX < 0;
                                // 1. Animate off-screen
                                float exitX = isLeft ? -v.getWidth() : v.getWidth();
                                animationTarget.animate()
                                        .translationX(exitX)
                                        .alpha(0f)
                                        .setDuration(220)
                                        .withEndAction(() -> {
                                            // 2. Trigger value change
                                            isSwipeOngoing = true;
                                            if (isLeft) {
                                                onLeftSwipe.run();
                                            } else {
                                                onRightSwipe.run();
                                            }
                                            isSwipeOngoing = false;
                                            // 3. Reset to opposite side off-screen, then slide in
                                            animationTarget.setTranslationX(isLeft ? v.getWidth() : -v.getWidth());
                                            animationTarget.animate()
                                                    .translationX(0f)
                                                    .alpha(1f)
                                                    .setDuration(350)
                                                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                                                    .start();
                                        })
                                        .start();
                            } else {
                                // Snap back to center
                                animationTarget.animate()
                                        .translationX(0f)
                                        .alpha(1f)
                                        .setDuration(280)
                                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                        .start();
                            }
                        } else {
                            // It's a tap
                            float clickThreshold = 8 * density;
                            if (Math.abs(finalDeltaX) < clickThreshold && Math.abs(finalDeltaY) < clickThreshold) {
                                v.performClick();
                                onTap.run();
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private boolean isViewActive() { return b != null && isAdded(); }

    @Override public void onResume() {
        super.onResume();
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());
        if (isViewActive()) {
            if (isFirstLoad) {
                isFirstLoad = false;
            } else {
                loadClasses();
                if (!isCombineAnimRunning) {
                    startHeaderAnimations();
                }
            }
            animHandler.removeCallbacks(combineLoopRunnable);
            animHandler.postDelayed(combineLoopRunnable, 12000);
        }
    }

    @Override public void onPause() {
        super.onPause();
        animHandler.removeCallbacks(combineLoopRunnable);
        stopHeaderAnimators();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        animHandler.removeCallbacks(combineLoopRunnable);
        stopHeaderAnimators();
        b = null;
    }
}
