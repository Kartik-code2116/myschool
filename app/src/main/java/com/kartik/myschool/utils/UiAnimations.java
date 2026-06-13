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

    /** Used for bottom navigation tab switches — content slides up smoothly. */
    public static NavOptions navBottomTab() {
        return new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_up_in)
                .setExitAnim(R.anim.slide_down_out)
                .setPopEnterAnim(R.anim.slide_up_in)
                .setPopExitAnim(R.anim.slide_down_out)
                .build();
    }

    /** Used for sidebar drawer navigation — scale + fade for a premium feel. */
    public static NavOptions navDrawerOpen() {
        return new NavOptions.Builder()
                .setEnterAnim(R.anim.scale_fade_in)
                .setExitAnim(R.anim.scale_fade_out)
                .setPopEnterAnim(R.anim.scale_fade_in)
                .setPopExitAnim(R.anim.scale_fade_out)
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

    /**
     * Scroll-reveal animation: each item slides up and fades in as it appears
     * while the user scrolls down. Call this from onBindViewHolder().
     *
     * @param view         The item root view to animate
     * @param position     The adapter position (used to stagger delays)
     * @param lastPosition Mutable int[] of size 1: holds the last animated position.
     *                     Pass the same array instance across all bind calls.
     */
    public static void animateScrollReveal(View view, int position, int[] lastPosition) {
        if (view == null) return;
        if (position > lastPosition[0]) {
            lastPosition[0] = position;

            // Cap animation on scroll to prevent main-thread layout lag for long lists
            if (position >= 10) {
                return;
            }

            float density = view.getResources().getDisplayMetrics().density;
            float translateY = 48 * density;

            view.setAlpha(0f);
            view.setTranslationY(translateY);
            view.setScaleX(0.97f);
            view.setScaleY(0.97f);

            // Stagger each item slightly so they cascade in as you scroll
            long delay = Math.min(position * 40L, 200L); // cap stagger at 200ms

            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(delay)
                    .setDuration(320)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f))
                    .withEndAction(() -> {
                        view.setAlpha(1f);
                        view.setTranslationY(0f);
                        view.setScaleX(1f);
                        view.setScaleY(1f);
                    })
                    .start();
        }
    }

    /**
     * Card pop animation: a subtle scale-bounce that makes a card feel alive
     * when it first appears. Use for report/card-style list items.
     *
     * @param view         The card root view
     * @param position     The adapter position
     * @param lastPosition Mutable int[] of size 1
     */
    public static void animateCardPop(View view, int position, int[] lastPosition) {
        if (view == null) return;
        if (position > lastPosition[0]) {
            lastPosition[0] = position;

            float density = view.getResources().getDisplayMetrics().density;

            view.setAlpha(0f);
            view.setTranslationY(32 * density);
            view.setScaleX(0.92f);
            view.setScaleY(0.92f);

            long delay = Math.min(position * 50L, 250L);

            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .setStartDelay(delay)
                    .setDuration(260)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                            .start())
                    .start();
        }
    }
}
