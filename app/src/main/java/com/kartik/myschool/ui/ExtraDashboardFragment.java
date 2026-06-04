package com.kartik.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.databinding.FragmentExtraDashboardBinding;
import com.kartik.myschool.utils.UiAnimations;

public class ExtraDashboardFragment extends Fragment {

    private FragmentExtraDashboardBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentExtraDashboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Display Header info using Session Context
        String subtitle = "Year: " + SessionContext.getYearLabel() + "   " + SessionContext.getClassDivLabel();
        b.tvDashboardSubtitle.setText(subtitle);

        // Bind Click Listeners with premium click-pulse animation and forward navigation
        b.btnDashSchoolInfo.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnDashSchoolInfo);
            navigateTo(R.id.nav_school_info);
        });

        b.btnDashGender.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnDashGender);
            navigateTo(R.id.nav_gender);
        });

        b.btnDashCastCategory.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnDashCastCategory);
            navigateTo(R.id.nav_cast_category);
        });

        b.btnDashClassTeacher.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnDashClassTeacher);
            navigateTo(R.id.nav_class_teacher);
        });

        b.btnDashClasses.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnDashClasses);
            navigateTo(R.id.nav_classes);
        });

        b.btnDashSubject.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnDashSubject);
            navigateTo(R.id.nav_subject);
        });

        b.btnDashDefaultValues.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnDashDefaultValues);
            navigateTo(R.id.nav_default_values);
        });

        b.btnDashWorkingDays.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnDashWorkingDays);
            navigateTo(R.id.nav_working_days);
        });

        b.btnDashHeSheItems.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnDashHeSheItems);
            navigateTo(R.id.nav_he_she_items);
        });

        // Trigger premium stagger fade-in enter animation for elements to feel elegant and alive
        UiAnimations.staggerFadeIn(
                b.btnDashSchoolInfo, b.btnDashGender,
                b.btnDashCastCategory, b.btnDashClassTeacher,
                b.btnDashClasses, b.btnDashSubject,
                b.btnDashDefaultValues, b.btnDashWorkingDays,
                b.btnDashHeSheItems
        );
    }

    private void navigateTo(int destId) {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).navigateTo(destId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
