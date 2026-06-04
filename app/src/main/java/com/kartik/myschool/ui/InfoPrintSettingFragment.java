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

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentInfoPrintSettingBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        b.btnGoToClass.setOnClickListener(v -> { UiAnimations.pulse(b.btnGoToClass); goToClassStudents(); });
        b.btnAllClasses.setOnClickListener(v -> { UiAnimations.pulse(b.btnAllClasses); navigateWithAnim(R.id.nav_class_div); });
        b.btnHowToUse.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnHowToUse);
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.msg_how_to_use)
                    .setMessage(getString(R.string.hint_question_mark))
                    .setPositiveButton(android.R.string.ok, null).show();
        });
        b.btnOnlineHelp.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnOnlineHelp);
            Toast.makeText(requireContext(), R.string.msg_opening_online_help_portal, Toast.LENGTH_SHORT).show();
        });

        setupSwipeListener(b.panelSemester,
                () -> cycleSemester(1), () -> cycleSemester(-1), this::showSemesterPickerDialog);
        setupSwipeListener(b.panelClass,
                () -> cycleClass(1), () -> cycleClass(-1), () -> {
                    if (!classes.isEmpty()) { UiAnimations.pulse(b.panelClass); goToClassStudents(); }
                });
        b.panelClass.setOnLongClickListener(v -> { showClassPickerDialog(); return true; });

        b.scrollHome.setOnScrollChangeListener(new androidx.core.widget.NestedScrollView.OnScrollChangeListener() {
            private boolean headerVanished = false;
            private int initialHeaderHeight = -1;

            @Override
            public void onScrollChange(androidx.core.widget.NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (initialHeaderHeight == -1 && b != null && b.headerBand != null) {
                    initialHeaderHeight = b.headerBand.getHeight();
                    if (initialHeaderHeight <= 0) {
                        initialHeaderHeight = (int) (200 * v.getContext().getResources().getDisplayMetrics().density);
                    }
                }

                if (scrollY > 40 && !headerVanished) {
                    headerVanished = true;
                    animateHeader(initialHeaderHeight, 0, 1f, 0f);
                } else if (scrollY <= 10 && headerVanished) {
                    headerVanished = false;
                    animateHeader(0, initialHeaderHeight, 0f, 1f);
                }
            }
        });

        playEntranceIfNeeded();
        loadTeacherName();   // instant from cache, then background refresh
        initSessionData();   // instant from AppCache, then background network sync
    }

    private void animateHeader(int startHeight, int endHeight, float startAlpha, float endAlpha) {
        if (b == null || b.headerBand == null) return;
        b.headerBand.animate().cancel();
        
        // 1. Height animation
        ValueAnimator heightAnim = ValueAnimator.ofInt(startHeight, endHeight);
        heightAnim.addUpdateListener(animation -> {
            if (b != null && b.headerBand != null) {
                int val = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams lp = b.headerBand.getLayoutParams();
                lp.height = val;
                b.headerBand.setLayoutParams(lp);
            }
        });
        heightAnim.setDuration(350);
        heightAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        heightAnim.start();

        // 2. Alpha & TranslationY animation
        b.headerBand.animate()
                .alpha(endAlpha)
                .translationY(endAlpha == 0f ? -60f : 0f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    // ── Teacher name ──────────────────────────────────────────────────────────
    /**
     * Two-phase load:
     * Phase 1 (sync, 0ms): read AppCache.cachedTeacherName → set TextView immediately.
     * Phase 2 (async):     Firestore call (hits FirebaseRepository in-memory cache after
     *                      first load, so also ~0ms from the second visit onward).
     */
    private void loadTeacherName() {
        // Phase 1 — instant
        if (AppCache.cachedTeacherName != null && b != null) {
            b.tvTeacherNameHeader.setText(AppCache.cachedTeacherName);
        }
        // Phase 2 — background refresh
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher t) {
                if (t == null || t.name == null) return;
                boolean wasBlank = (AppCache.cachedTeacherName == null);
                AppCache.cachedTeacherName = t.name;
                if (b != null) {
                    requireActivity().runOnUiThread(() -> {
                        b.tvTeacherNameHeader.setText(t.name);
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
            if (SessionContext.selectedYear != null) {
                for (int i = 0; i < years.size(); i++) {
                    if (years.get(i).id != null && years.get(i).id.equals(SessionContext.selectedYear.id)) {
                        yearIndex = i; break;
                    }
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
            SessionContext.selectedSemester = semesters.get(0);
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
                            requireActivity().runOnUiThread(() -> b.tvTeacherNameHeader.setText(t.name));
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
        if (list == null || list.isEmpty()) {
            if (years.isEmpty()) {
                years.add(new AcademicYear("2026-27", 2026, 2027));
                AppCache.cachedYears = new ArrayList<>(years);
                if (isViewActive()) { applyYear(0); loadSemesters(); }
            } else {
                if (isViewActive()) loadSemesters();
            }
            return;
        }
        years.clear();
        years.addAll(list);
        AppCache.cachedYears = new ArrayList<>(years);
        if (SessionContext.selectedYear != null) {
            for (int i = 0; i < years.size(); i++) {
                if (years.get(i).id != null && years.get(i).id.equals(SessionContext.selectedYear.id)) {
                    yearIndex = i; break;
                }
            }
        }
        if (isViewActive()) { applyYear(0); loadSemesters(); }
    }

    private void loadSemesters() {
        AcademicYear y = getCurrentYear();
        if (y == null || y.id == null) {
            if (semesters.isEmpty()) seedFallbackSemesters();
            loadClasses(); return;
        }
        FirebaseRepository.get().getSemestersForYear(y.id, new FirebaseRepository.OnResult<List<Semester>>() {
            @Override public void onSuccess(List<Semester> list) {
                if (list == null || list.isEmpty()) {
                    if (semesters.isEmpty()) {
                        seedFallbackSemesters();
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
                }
                
                if (isViewActive()) { applySemester(0); loadClasses(); }
            }
            @Override public void onError(Exception e) {
                if (semesters.isEmpty()) {
                    seedFallbackSemesters();
                    if (isViewActive()) { applySemester(0); loadClasses(); }
                } else {
                    loadClasses();
                }
            }
        });
    }

    private void seedFallbackSemesters() {
        semesters.clear();
        semesters.add(new Semester(1, "First Semester",  "Easy Reports"));
        semesters.add(new Semester(2, "Second Semester", "Final Reports"));
        AppCache.cachedSemesters = new ArrayList<>(semesters);
    }

    private void loadClasses() {
        AcademicYear y = getCurrentYear();
        if (y == null || y.id == null) { loadClassesForSchool(); return; }
        FirebaseRepository.get().getClassesForYear(y.id, new FirebaseRepository.OnResult<List<ClassModel>>() {
            @Override public void onSuccess(List<ClassModel> list) {
                if (list == null || list.isEmpty()) { loadClassesForSchool(); return; }
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
            @Override public void onError(Exception e) { loadClassesForSchool(); }
        });
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

    private void cycleYear(int d) {
        if (years.isEmpty()) return;
        yearIndex = (yearIndex + d + years.size()) % years.size();
        applyYear(d); loadSemesters();
    }

    private void cycleSemester(int d) {
        if (semesters.isEmpty()) return;
        semesterIndex = (semesterIndex + d + semesters.size()) % semesters.size();
        applySemester(d);
    }

    private void cycleClass(int d) {
        if (classes.isEmpty()) return;
        classIndex = (classIndex + d + classes.size()) % classes.size();
        applyClass(d);
    }

    private void applyYear(int dir) {
        AcademicYear y = getCurrentYear(); if (y == null) return;
        SessionContext.selectedYear = y;
        SessionContext.save(getContext());
        if (!isViewActive()) return;
        b.tvYearLabel.setText(y.label != null ? y.label : getString(R.string.year_label, "—"));
        if (dir != 0) UiAnimations.animateSelectorChange(b.tvYearLabel, dir);
    }

    private void applySemester(int dir) {
        if (semesters.isEmpty()) return;
        semesterIndex = Math.max(0, Math.min(semesterIndex, semesters.size() - 1));
        Semester s = semesters.get(semesterIndex);
        SessionContext.selectedSemester = s;
        SessionContext.save(getContext());
        if (!isViewActive()) return;
        b.tvSemesterName.setText(s.name != null ? s.name : "");
        if (dir != 0) UiAnimations.animateSelectorChange(b.panelSemester, dir);
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

        if (dir != 0) UiAnimations.animateSelectorChange(b.panelClass, dir);
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void navigateWithAnim(int destId) {
        if (getActivity() instanceof HomeActivity)
            ((HomeActivity) getActivity()).navigateTo(destId);
        else
            Navigation.findNavController(requireView()).navigate(destId, null, UiAnimations.navSlideForward());
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
            UiAnimations.staggerFadeIn(b.headerBand, b.cardMain, b.btnGoToClass, b.btnAllClasses);
            startHeaderAnimations();
        });
    }

    // ── Header illustration animations ───────────────────────────────────────
    private void startHeaderAnimations() {
        if (!isViewActive()) return;

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

        // 3. School icon: slow breathe scale + gentle rotation
        ObjectAnimator schoolBreath = ObjectAnimator.ofFloat(b.ivAnimSchool, "scaleX", 0.92f, 1.08f);
        ObjectAnimator schoolBreathY = ObjectAnimator.ofFloat(b.ivAnimSchool, "scaleY", 0.92f, 1.08f);
        schoolBreath.setDuration(1800);  schoolBreath.setRepeatMode(ValueAnimator.REVERSE);  schoolBreath.setRepeatCount(ValueAnimator.INFINITE);
        schoolBreathY.setDuration(1800); schoolBreathY.setRepeatMode(ValueAnimator.REVERSE); schoolBreathY.setRepeatCount(ValueAnimator.INFINITE);
        AnimatorSet schoolSet = new AnimatorSet();
        schoolSet.playTogether(schoolBreath, schoolBreathY);
        schoolSet.setInterpolator(new AccelerateDecelerateInterpolator());
        schoolSet.start();

        // 4. Floating satellite icons — staggered bob up/down
        float[] offsets = {0f, 500f, 900f, 1300f};
        View[] floaters = {b.frameBook, b.frameStudents, b.frameChart, b.frameCalendar};
        float[] amplitudes = {-16f, -12f, -14f, -10f};
        int[] durations   = {2000,  1700,  2300,  1900};
        for (int i = 0; i < floaters.length; i++) {
            ObjectAnimator bobY = ObjectAnimator.ofFloat(floaters[i], "translationY", 0f, amplitudes[i]);
            bobY.setDuration(durations[i]);
            bobY.setRepeatMode(ValueAnimator.REVERSE);
            bobY.setRepeatCount(ValueAnimator.INFINITE);
            bobY.setStartDelay((long) offsets[i]);
            bobY.setInterpolator(new AccelerateDecelerateInterpolator());
            bobY.start();

            // Entrance pop-in for each floater
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

            ObjectAnimator twinkleScale = ObjectAnimator.ofFloat(dots[i], "scaleX", 0.5f, 1.3f);
            ObjectAnimator twinkleScaleY = ObjectAnimator.ofFloat(dots[i], "scaleY", 0.5f, 1.3f);
            twinkleScale.setDuration(dotDurations[i]);  twinkleScale.setRepeatMode(ValueAnimator.REVERSE);  twinkleScale.setRepeatCount(ValueAnimator.INFINITE); twinkleScale.setStartDelay(dotDelays[i]);
            twinkleScaleY.setDuration(dotDurations[i]); twinkleScaleY.setRepeatMode(ValueAnimator.REVERSE); twinkleScaleY.setRepeatCount(ValueAnimator.INFINITE); twinkleScaleY.setStartDelay(dotDelays[i]);
            twinkleScale.start(); twinkleScaleY.start();
        }
    }

    // ── Picker dialogs ────────────────────────────────────────────────────────
    private void showSemesterPickerDialog() {
        if (semesters.isEmpty()) return;
        String[] names = new String[semesters.size()];
        for (int i = 0; i < semesters.size(); i++) names[i] = semesters.get(i).name;
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
    private void setupSwipeListener(View v, Runnable onLeft, Runnable onRight, Runnable onTap) {
        v.setOnTouchListener(new View.OnTouchListener() {
            private float sx, sy;
            @Override public boolean onTouch(View vv, android.view.MotionEvent e) {
                switch (e.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN: sx = e.getX(); sy = e.getY(); return true;
                    case android.view.MotionEvent.ACTION_UP:
                        float dx = e.getX() - sx, dy = e.getY() - sy;
                        if (Math.abs(dx) > 100 && Math.abs(dx) > Math.abs(dy)) {
                            if (dx < 0) onLeft.run(); else onRight.run();
                        } else if (Math.abs(dx) < 15 && Math.abs(dy) < 15) {
                            onTap.run();
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
        if (isViewActive()) {
            if (isFirstLoad) isFirstLoad = false;
            else loadClasses();
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
