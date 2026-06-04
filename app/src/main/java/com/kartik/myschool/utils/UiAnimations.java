package com.kartik.myschool.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.navigation.NavOptions;

import com.kartik.myschool.R;

public final class UiAnimations {

    private UiAnimations() {}

    public static NavOptions navSlideForward() {
        return new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build();
    }

    public static NavOptions navCrossFade() {
        return new NavOptions.Builder()
                .setEnterAnim(R.anim.fade_in)
                .setExitAnim(R.anim.fade_out)
                .setPopEnterAnim(R.anim.fade_in)
                .setPopExitAnim(R.anim.fade_out)
                .build();
    }

    public static void fadeIn(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(view.getResources().getInteger(R.integer.anim_duration_medium))
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    public static void staggerFadeIn(View... views) {
        if (views == null) return;
        long delay = 0L;
        int step = 60;
        for (View v : views) {
            if (v == null) continue;
            View parent = v.getParent() instanceof View ? (View) v.getParent() : null;
            if (parent != null && parent.getAlpha() < 1f) {
                parent.setAlpha(1f);
            }
            v.setAlpha(0f);
            v.setTranslationY(v.getResources().getDisplayMetrics().density * 12f);
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(v.getResources().getInteger(R.integer.anim_duration_medium))
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> {
                        v.setAlpha(1f);
                        v.setTranslationY(0f);
                    })
                    .start();
            delay += step;
        }
    }

    /** Animate picker content when user taps prev/next. */
    public static void animateSelectorChange(View panel, int direction) {
        if (panel == null) return;
        float offset = panel.getResources().getDisplayMetrics().density * 20f * direction;
        panel.animate().cancel();
        panel.setAlpha(0.4f);
        panel.setTranslationX(offset);
        panel.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(panel.getResources().getInteger(R.integer.anim_duration_short))
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    public static void pulse(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(80)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }

    public static void setupRecyclerAnimations(androidx.recyclerview.widget.RecyclerView rv) {
        if (rv == null) return;
        androidx.recyclerview.widget.DefaultItemAnimator animator =
                new androidx.recyclerview.widget.DefaultItemAnimator();
        animator.setAddDuration(rv.getResources().getInteger(R.integer.anim_duration_medium));
        animator.setChangeDuration(rv.getResources().getInteger(R.integer.anim_duration_short));
        animator.setMoveDuration(rv.getResources().getInteger(R.integer.anim_duration_short));
        animator.setRemoveDuration(rv.getResources().getInteger(R.integer.anim_duration_short));
        rv.setItemAnimator(animator);
    }

    public static void playLayoutEnter(View root) {
        if (root == null) return;
        android.view.animation.Animation anim = AnimationUtils.loadAnimation(
                root.getContext(), R.anim.slide_up_fade_in);
        root.startAnimation(anim);
    }
}
