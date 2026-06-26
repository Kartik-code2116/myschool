package com.kartik.myschool.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ScrollView;

import com.kartik.myschool.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ProductTourHelper â€” Animated coach mark & guided product tour overlay.
 *
 * When a user taps "WATCH" in the custom help dialog, this class attaches a full-screen
 * transparent overlay to the activity window, paints a spotlight "punchout" over target
 * UI elements, and guides the user through a step-by-step feature walkthrough with:
 *  - Smooth animated spotlight transitions (ValueAnimator, 450ms decelerate)
 *  - Expanding pulsing ripple rings around the active target
 *  - Beautifully styled floating tooltip cards (above or below the target)
 *  - Localized English / Marathi step descriptions
 *  - Skip + Next / Got It navigation controls
 */
public class ProductTourHelper {

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Public entry point
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void startTour(Activity activity, String pageKey) {
        if (activity == null || activity.isFinishing()) return;

        boolean isMarathi = Locale.getDefault().getLanguage().equals("mr");
        List<TourStep> steps = buildSteps(pageKey, isMarathi);
        if (steps.isEmpty()) return;

        ViewGroup root = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        if (root == null) return;

        TourOverlayView overlay = new TourOverlayView(activity, steps);
        root.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(300).start();
        overlay.showStep(0);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // TourStep data model
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static class TourStep {
        @IdRes public final int targetViewId;   // 0 = no spotlight (full-card intro step)
        public final int targetChildIndex;      // >=0 for RecyclerView child targeting
        @IdRes public final int targetChildViewId; // 0 = target the whole child, else target this ID inside the child
        public final String title;
        public final String description;

        public TourStep(int targetViewId, String title, String description) {
            this(targetViewId, -1, 0, title, description);
        }

        public TourStep(int targetViewId, int targetChildIndex, String title, String description) {
            this(targetViewId, targetChildIndex, 0, title, description);
        }

        public TourStep(int targetViewId, int targetChildIndex, int targetChildViewId, String title, String description) {
            this.targetViewId = targetViewId;
            this.targetChildIndex = targetChildIndex;
            this.targetChildViewId = targetChildViewId;
            this.title = title;
            this.description = description;
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Step definitions per page
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static List<TourStep> buildSteps(String pageKey, boolean m) {
        List<TourStep> s = new ArrayList<>();
        switch (pageKey != null ? pageKey : "default") {

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 1. Home / Info-Print Setting  (8 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "info_print":
                s.add(new TourStep(0,
                        m ? "ðŸ  à¤®à¥à¤–à¥à¤¯ à¤¸à¥à¤•à¥à¤°à¥€à¤¨" : "ðŸ  Home Screen",
                        m ? "à¤¯à¥‡à¤¥à¥‡ à¤µà¤°à¥à¤·, à¤¸à¤¤à¥à¤° à¤µ à¤µà¤°à¥à¤— à¤¸à¥‡à¤Ÿ à¤•à¤°à¤¾. 'à¤µà¤°à¥à¤—à¤¾à¤µà¤° à¤œà¤¾' à¤¦à¤¾à¤¬à¤²à¥à¤¯à¤¾à¤¸ à¤¥à¥‡à¤Ÿ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¯à¤¾à¤¦à¥€à¤¤ à¤œà¤¾à¤²."
                          : "Set your academic year, semester & class here, then go straight to the student list."));
                s.add(new TourStep(R.id.tvTeacherNameHeader,
                        m ? "à¥§. à¤¶à¤¿à¤•à¥à¤·à¤•à¤¾à¤šà¥‡ à¤¨à¤¾à¤µ" : "1. Teacher Name",
                        m ? "à¤†à¤ªà¤²à¥‡ à¤¨à¤¾à¤µ à¤µ à¤¶à¤¾à¤³à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡. à¤ªà¥à¤°à¥‹à¤«à¤¾à¤‡à¤² à¤¸à¥à¤•à¥à¤°à¥€à¤¨à¤®à¤§à¥‚à¤¨ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤…à¤¦à¥à¤¯à¤¤à¤¨ à¤•à¤°à¤¾."
                          : "Your name and school appear here. Update them from the Profile screen."));
                s.add(new TourStep(R.id.cardYear,
                        m ? "à¥¨. à¤¶à¥ˆà¤•à¥à¤·à¤£à¤¿à¤• à¤µà¤°à¥à¤·" : "2. Academic Year",
                        m ? "â—€ â–¶ à¤¬à¤¾à¤£ à¤¦à¤¾à¤¬à¥‚à¤¨ à¤¶à¥ˆà¤•à¥à¤·à¤£à¤¿à¤• à¤µà¤°à¥à¤· à¤¬à¤¦à¤²à¤¾. à¤‰à¤¦à¤¾. 2024-25."
                          : "Tap â—€ â–¶ arrows to change the academic year, e.g. 2024-25."));
                s.add(new TourStep(R.id.panelSemester,
                        m ? "à¥©. à¤¸à¤¤à¥à¤° à¤¨à¤¿à¤µà¤¡à¤¾" : "3. Select Semester",
                        m ? "â—€ â–¶ à¤¦à¤¾à¤¬à¥‚à¤¨ à¤ªà¤¹à¤¿à¤²à¥‡ à¤•à¤¿à¤‚à¤µà¤¾ à¤¦à¥à¤¸à¤°à¥‡ à¤¸à¤¤à¥à¤° à¤¨à¤¿à¤µà¤¡à¤¾."
                          : "Tap â—€ â–¶ to choose between Semester 1 and Semester 2."));
                s.add(new TourStep(R.id.panelClass,
                        m ? "à¥ª. à¤µà¤°à¥à¤— à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤•à¤°à¤¾" : "4. Activate Your Class",
                        m ? "â—€ â–¶ à¤¦à¤¾à¤¬à¥‚à¤¨ à¤µà¤°à¥à¤— à¤¬à¤¦à¤²à¤¾. à¤²à¥‰à¤— à¤Ÿà¤¿à¤•à¥‚à¤¨ à¤°à¤¾à¤¹à¤¤à¥‹ â€” à¤ªà¥à¤¢à¤šà¥à¤¯à¤¾ à¤µà¥‡à¤³à¥€ à¤†à¤ªà¥‹à¤†à¤ª à¤²à¥‹à¤¡ à¤¹à¥‹à¤ˆà¤²."
                          : "Use â—€ â–¶ to pick your class. It is remembered for next time."));
                s.add(new TourStep(R.id.btnGoToClass,
                        m ? "à¥«. à¤µà¤°à¥à¤—à¤¾à¤µà¤° à¤œà¤¾" : "5. Go to Class",
                        m ? "à¤¹à¥‡ à¤¬à¤Ÿà¤£ à¤¦à¤¾à¤¬à¤²à¥à¤¯à¤¾à¤µà¤° à¤¥à¥‡à¤Ÿ à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤µà¤°à¥à¤—à¤¾à¤šà¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¯à¤¾à¤¦à¥€à¤¤ à¤œà¤¾à¤²."
                          : "Press this to jump directly to the student list of your active class."));
                s.add(new TourStep(R.id.btnAllClasses,
                        m ? "à¥¬. à¤¸à¤°à¥à¤µ à¤µà¤°à¥à¤—" : "6. All Classes",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤¤à¥€à¤² à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤°à¥à¤—à¤¾à¤šà¥€ à¤¯à¤¾à¤¦à¥€ à¤à¤•à¤¤à¥à¤° à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap here to browse every class in the school at once."));
                s.add(new TourStep(R.id.btnHowToUse,
                        m ? "à¥­. à¤•à¤¸à¥‡ à¤µà¤¾à¤ªà¤°à¤¾à¤µà¥‡?" : "7. How to Use?",
                        m ? "à¥²à¤ªà¥à¤²à¤¿à¤•à¥‡à¤¶à¤¨ à¤•à¤¸à¥‡ à¤µà¤¾à¤ªà¤°à¤¾à¤µà¥‡ à¤¹à¥‡ à¤µà¥à¤¹à¤¿à¤¡à¤¿à¤“ à¤®à¤¾à¤°à¥à¤—à¤¦à¤°à¥à¤¶à¤¿à¤•à¥‡à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap here for a video walkthrough on how to use the app."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 2. Stats Dashboard  (4 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "stats_dashboard":
                s.add(new TourStep(0,
                        m ? "ðŸ“Š à¤¸à¤¾à¤‚à¤–à¥à¤¯à¤¿à¤•à¥€ à¤¡à¥…à¤¶à¤¬à¥‹à¤°à¥à¤¡" : "ðŸ“Š Stats Dashboard",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤šà¥€ à¤µ à¤µà¤°à¥à¤—à¤¾à¤šà¥€ à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤†à¤•à¤¡à¥‡à¤µà¤¾à¤°à¥€ à¤à¤•à¤¾à¤š à¤ à¤¿à¤•à¤¾à¤£à¥€ à¤ªà¤¾à¤¹à¤¾."
                          : "View all school and class statistics in one place."));
                s.add(new TourStep(R.id.tvHeaderStripInfo,
                        m ? "à¥§. à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤¸à¤¤à¥à¤° à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Active Session Info",
                        m ? "à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤µà¤°à¥à¤·, à¤¸à¤¤à¥à¤° à¤µ à¤µà¤°à¥à¤—à¤¾à¤¸à¤¾à¤ à¥€ à¤†à¤•à¤¡à¥‡à¤µà¤¾à¤°à¥€ à¤¦à¤¾à¤–à¤µà¤¤ à¤†à¤¹à¥‡ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows which year, semester and class the statistics are for."));
                s.add(new TourStep(R.id.tvTotalStudents,
                        m ? "à¥¨. à¤à¤•à¥‚à¤£ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€" : "2. Total Students",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤¤à¥€à¤² à¤¸à¤°à¥à¤µ à¤µà¤°à¥à¤—à¤¾à¤‚à¤®à¤§à¥à¤¯à¥‡ à¤à¤•à¥‚à¤£ à¤•à¤¿à¤¤à¥€ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¨à¥‹à¤‚à¤¦à¤£à¥€à¤•à¥ƒà¤¤ à¤†à¤¹à¥‡à¤¤ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Total number of students enrolled across all classes in the school."));
                s.add(new TourStep(R.id.rvDashboard,
                        m ? "à¥©. à¤µà¤°à¥à¤—à¤¨à¤¿à¤¹à¤¾à¤¯ à¤†à¤²à¥‡à¤–" : "3. Class-wise Charts",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤°à¥à¤—à¤¾à¤šà¥à¤¯à¤¾ à¤•à¤¾à¤°à¥à¤¡à¤¾à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¤°à¤¾ â€” à¤²à¤¿à¤‚à¤— à¤µ à¤œà¤¾à¤¤ à¤ªà¥à¤°à¤µà¤°à¥à¤— à¤†à¤²à¥‡à¤– à¤¦à¤¿à¤¸à¥‡à¤²."
                          : "Tap any class card to see gender ratio and caste distribution charts."));
                s.add(new TourStep(R.id.btnStudentDashboard,
                        m ? "à¥ª. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤ªà¥à¤°à¤—à¤¤à¥€ (à¤Ÿà¥‰à¤—à¤²)" : "4. Student Progress (Toggle)",
                        m ? "à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¾à¤¬à¥‚à¤¨ à¤¤à¥à¤®à¥à¤¹à¥€ à¤µà¤¿à¤·à¤¯à¤¨à¤¿à¤¹à¤¾à¤¯ à¤†à¤£à¤¿ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€à¤¨à¤¿à¤¹à¤¾à¤¯ à¤†à¤•à¤¡à¥‡à¤µà¤¾à¤°à¥€à¤®à¤§à¥à¤¯à¥‡ à¤…à¤¦à¤²à¤¾à¤¬à¤¦à¤² à¤•à¤°à¥‚ à¤¶à¤•à¤¤à¤¾."
                          : "Tap here to instantly toggle between subject-wise and student-wise completion progress."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 3. Class & Division List  (4 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "class_div":
                s.add(new TourStep(0,
                        m ? "ðŸ« à¤µà¤°à¥à¤— à¤µ à¤¤à¥à¤•à¤¡à¥€" : "ðŸ« Class & Division",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤¤à¥€à¤² à¤¸à¤°à¥à¤µ à¤µà¤°à¥à¤— à¤†à¤£à¤¿ à¤¤à¥à¤•à¤¡à¥à¤¯à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤µà¥à¤¯à¤µà¤¸à¥à¤¥à¤¾à¤ªà¤¿à¤¤ à¤•à¤°à¤¾."
                          : "Manage all classes and divisions of your school here."));
                s.add(new TourStep(R.id.tvClassDivYear,
                        m ? "à¥§. à¤šà¤¾à¤²à¥‚ à¤¶à¥ˆà¤•à¥à¤·à¤£à¤¿à¤• à¤µà¤°à¥à¤·" : "1. Current Academic Year",
                        m ? "à¤¸à¤§à¥à¤¯à¤¾ à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤µà¤°à¥à¤· à¤µ à¤¸à¤¤à¥à¤°à¤¾à¤šà¥€ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the currently active academic year and semester."));
                s.add(new TourStep(R.id.rvClassDiv,
                        m ? "à¥¨. à¤µà¤°à¥à¤—à¤¾à¤‚à¤šà¥€ à¤¯à¤¾à¤¦à¥€" : "2. Class List",
                        m ? "à¤¸à¤°à¥à¤µ à¤µà¤°à¥à¤— à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¤¾à¤¤. à¤•à¤¾à¤°à¥à¤¡à¤¾à¤šà¥à¤¯à¤¾ â‹® à¤¬à¤Ÿà¤£à¤¾à¤µà¤° à¤¦à¤¾à¤¬à¥‚à¤¨ à¤¸à¤‚à¤ªà¤¾à¤¦à¤¨ / à¤¡à¤¿à¤²à¥€à¤Ÿ à¤•à¤°à¤¾."
                          : "All classes appear here. Tap â‹® on a card to edit or delete it."));
                s.add(new TourStep(R.id.fabAddClass,
                        m ? "à¥©. à¤¨à¤µà¥€à¤¨ à¤µà¤°à¥à¤— à¤œà¥‹à¤¡à¤¾ +" : "3. Add New Class +",
                        m ? "'+' FAB à¤¦à¤¾à¤¬à¥‚à¤¨ à¤¨à¤µà¥€à¤¨ à¤µà¤°à¥à¤— (à¤‰à¤¦à¤¾. à¥«-A) à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤¾."
                          : "Tap the '+' button to create a new class, e.g. Class 5-A."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 4. Student List  (9 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "students":
                s.add(new TourStep(0,
                        m ? "ðŸ‘¨â€ðŸŽ“ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¯à¤¾à¤¦à¥€" : "ðŸ‘¨â€ðŸŽ“ Student List",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤¸à¤°à¥à¤µ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¤¾à¤¤. à¤¶à¥‹à¤§, à¤†à¤¯à¤¾à¤¤, à¤¨à¤¿à¤°à¥à¤¯à¤¾à¤¤ à¤µ à¤œà¥‹à¤¡à¤£à¥€ à¤•à¤°à¤¾."
                          : "All students in the class are listed here. Search, import, export and add."));
                s.add(new TourStep(R.id.tvHeaderSessionInfo,
                        m ? "à¥§. à¤¸à¤¤à¥à¤° à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Session Info",
                        m ? "à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤µà¤°à¥à¤·à¤¾à¤šà¥€ à¤µ à¤µà¤°à¥à¤—à¤¾à¤šà¥€ à¤¯à¤¾à¤¦à¥€ à¤ªà¤¾à¤¹à¤¤ à¤†à¤¹à¤¾à¤¤ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows which year and class student list you are viewing."));
                s.add(new TourStep(R.id.btnHelp,
                        m ? "à¥¨. à¤®à¤¦à¤¤ ?" : "2. Help ?",
                        m ? "à¤ªà¥à¤°à¤¶à¥à¤¨à¤šà¤¿à¤¨à¥à¤¹à¤¾à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¥‡à¤²à¥à¤¯à¤¾à¤¸ à¤¯à¤¾ à¤ªà¤¾à¤¨à¤¾à¤šà¥€ à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤®à¤¾à¤°à¥à¤—à¤¦à¤°à¥à¤¶à¤¿à¤•à¤¾ à¤µ à¤µà¥à¤¹à¤¿à¤¡à¤¿à¤“ à¤®à¤¿à¤³à¥‡à¤²."
                          : "Tap '?' to open the complete guide and video for this page."));
                s.add(new TourStep(R.id.btnExcel,
                        m ? "à¥©. à¤à¤•à¥à¤¸à¥‡à¤² ðŸ“Š" : "3. Excel ðŸ“Š",
                        m ? "à¤¯à¤¾ à¤šà¤¿à¤¨à¥à¤¹à¤¾à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¥‡à¤²à¥à¤¯à¤¾à¤¸ CSV à¤†à¤¯à¤¾à¤¤ à¤•à¤¿à¤‚à¤µà¤¾ CSV à¤¨à¤¿à¤°à¥à¤¯à¤¾à¤¤ à¤ªà¤°à¥à¤¯à¤¾à¤¯ à¤¦à¤¿à¤¸à¥‡à¤²."
                          : "Tap to choose between importing students from CSV or exporting to CSV."));
                s.add(new TourStep(R.id.btnDashboard,
                        m ? "à¥ª. à¤¸à¤¾à¤‚à¤–à¥à¤¯à¤¿à¤•à¥€ à¤¡à¥…à¤¶à¤¬à¥‹à¤°à¥à¤¡ ðŸ“Š" : "4. Stats Dashboard ðŸ“Š",
                        m ? "à¤¤à¥à¤®à¤šà¥à¤¯à¤¾ à¤µà¤°à¥à¤—à¤¾à¤šà¥€ à¤®à¥à¤²à¥‡-à¤®à¥à¤²à¥€à¤‚à¤šà¥‡ à¤ªà¥à¤°à¤®à¤¾à¤£, à¤œà¤¾à¤¤ à¤ªà¥à¤°à¤µà¤°à¥à¤— à¤†à¤£à¤¿ à¤‡à¤¤à¤° à¤¸à¤¾à¤‚à¤–à¥à¤¯à¤¿à¤•à¥€ à¤†à¤²à¥‡à¤– à¤¸à¥à¤µà¤°à¥‚à¤ªà¤¾à¤¤ à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap here to view the graphical statistics and caste/gender ratio charts for your class."));
                s.add(new TourStep(R.id.btnMoreOptions,
                        m ? "à¥«. à¤…à¤§à¤¿à¤• à¤ªà¤°à¥à¤¯à¤¾à¤¯ â‹®" : "5. More Options â‹®",
                        m ? "â‹® à¤®à¥‡à¤¨à¥‚à¤®à¤§à¥à¤¯à¥‡: à¤¸à¤°à¥à¤µ à¤—à¥à¤£ à¤°à¥€à¤¸à¥‡à¤Ÿ, à¤•à¥à¤°à¤® à¤¬à¤¦à¤²à¤£à¥‡ à¤‡à¤¤à¥à¤¯à¤¾à¤¦à¥€ à¤ªà¤°à¥à¤¯à¤¾à¤¯ à¤†à¤¹à¥‡à¤¤."
                          : "â‹® menu has options to reset all marks, reorder students and more."));
                s.add(new TourStep(R.id.etSearch,
                        m ? "à¥¬. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¶à¥‹à¤§à¤¾ ðŸ”" : "6. Search Student ðŸ”",
                        m ? "à¤¨à¤¾à¤µ à¤•à¤¿à¤‚à¤µà¤¾ à¤°à¥‹à¤² à¤¨à¤‚à¤¬à¤° à¤Ÿà¤¾à¤‡à¤ª à¤•à¥‡à¤²à¥à¤¯à¤¾à¤¸ à¤¯à¤¾à¤¦à¥€ à¤¤à¥à¤µà¤°à¤¿à¤¤ à¤«à¤¿à¤²à¥à¤Ÿà¤° à¤¹à¥‹à¤¤à¥‡."
                          : "Type a name or roll number â€” the list filters instantly as you type."));
                s.add(new TourStep(R.id.rvStudents,
                        m ? "à¥­. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤•à¤¾à¤°à¥à¤¡à¥à¤¸" : "7. Student Cards",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤•à¤¾à¤°à¥à¤¡à¤¾à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¥‡à¤²à¥à¤¯à¤¾à¤¸ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤ªà¥à¤°à¥‹à¤«à¤¾à¤‡à¤² à¤‰à¤˜à¤¡à¤¤à¥‡. à¤¦à¥€à¤°à¥à¤˜-à¤¦à¤¾à¤¬ = à¤ªà¤°à¥à¤¯à¤¾à¤¯ à¤®à¥‡à¤¨à¥‚."
                          : "Tap a card to open student profile. Long-press for quick options."));
                s.add(new TourStep(R.id.fabAddStudent,
                        m ? "à¥®. à¤¨à¤µà¥€à¤¨ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ +" : "8. Add New Student +",
                        m ? "'+' FAB à¤¦à¤¾à¤¬à¥‚à¤¨ à¤¨à¤µà¥€à¤¨ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥€ à¤¨à¥‹à¤‚à¤¦à¤£à¥€ à¤«à¥‰à¤°à¥à¤® à¤‰à¤˜à¤¡à¤¾."
                          : "Tap '+' to open the registration form for a new student."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 5. Attendance  (6 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "attendance":
                s.add(new TourStep(0,
                        m ? "ðŸ“… à¤®à¤¾à¤¸à¤¿à¤• à¤¹à¤œà¥‡à¤°à¥€" : "ðŸ“… Monthly Attendance",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤®à¤¹à¤¿à¤¨à¥à¤¯à¤¾à¤šà¥€ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€à¤¨à¤¿à¤¹à¤¾à¤¯ à¤¹à¤œà¥‡à¤°à¥€ à¤¯à¥‡à¤¥à¥‡ à¤¨à¥‹à¤‚à¤¦à¤µà¤¾ à¤µ à¤ªà¤¾à¤¹à¤¾."
                          : "Record and view monthly attendance for each student here."));
                s.add(new TourStep(R.id.tvAttendanceContext,
                        m ? "à¥§. à¤¸à¤¤à¥à¤° à¤µ à¤µà¤°à¥à¤— à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Session & Class Info",
                        m ? "à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤µà¤°à¥à¤·, à¤¸à¤¤à¥à¤° à¤µ à¤µà¤°à¥à¤—à¤¾à¤šà¥€ à¤¹à¤œà¥‡à¤°à¥€ à¤ªà¤¾à¤¹à¤¤ à¤†à¤¹à¤¾à¤¤ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the year, semester and class whose attendance you are viewing."));
                s.add(new TourStep(R.id.btnToolbarHelp,
                        m ? "à¥¨. à¤®à¤¦à¤¤ ?" : "2. Help ?",
                        m ? "à¤¹à¤œà¥‡à¤°à¥€ à¤ªà¤¾à¤¨à¤¾à¤šà¥€ à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤®à¤¾à¤°à¥à¤—à¤¦à¤°à¥à¤¶à¤¿à¤•à¤¾ à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap to open the complete guide for the Attendance page."));
                s.add(new TourStep(R.id.btnToolbarAdd,
                        m ? "à¥©. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤µà¥à¤¯à¤µà¤¸à¥à¤¥à¤¾à¤ªà¤¨ âž•" : "3. Manage Students âž•",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤®à¤§à¥à¤¯à¥‡ à¤¨à¤µà¥€à¤¨ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤œà¥‹à¤¡à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤•à¤¿à¤‚à¤µà¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥€ à¤¯à¤¾à¤¦à¥€ à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap this button to go to the Student List page to add or manage student details."));
                s.add(new TourStep(R.id.btnToolbarCalc,
                        m ? "à¥ª. à¤¹à¤œà¥‡à¤°à¥€ à¤…à¤¹à¤µà¤¾à¤² ðŸ§®" : "4. Attendance Report ðŸ§®",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤šà¤¾ à¤à¤•à¥‚à¤£ à¤¹à¤œà¥‡à¤°à¥€ à¤…à¤¹à¤µà¤¾à¤² à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¤¾ à¤•à¥…à¤²à¥à¤•à¥à¤¯à¥à¤²à¥‡à¤Ÿà¤° à¤šà¤¿à¤¨à¥à¤¹à¤¾à¤µà¤° à¤¦à¤¾à¤¬à¤¾."
                          : "Tap this calculator icon to view the class attendance report."));
                s.add(new TourStep(R.id.btnSearchStudents,
                        m ? "à¥«. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¶à¥‹à¤§à¤¾ ðŸ”" : "5. Search Student ðŸ”",
                        m ? "à¤µà¤¿à¤¶à¤¿à¤·à¥à¤Ÿ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥€ à¤¹à¤œà¥‡à¤°à¥€ à¤¤à¤ªà¤¾à¤¸à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¤à¥à¤¯à¤¾à¤šà¥‡ à¤¨à¤¾à¤µ à¤•à¤¿à¤‚à¤µà¤¾ à¤°à¥‹à¤² à¤¨à¤‚à¤¬à¤° à¤¶à¥‹à¤§à¤¾."
                          : "Type a name or roll number to search and filter for a specific student."));
                s.add(new TourStep(R.id.rvAttendanceStudents,
                        m ? "à¥¬. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¹à¤œà¥‡à¤°à¥€ à¤¯à¤¾à¤¦à¥€" : "6. Student Attendance List",
                        m ? "à¤¯à¥‡à¤¥à¥‡ à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¤¾ à¤¹à¤œà¥‡à¤°à¥€ à¤¬à¥‰à¤•à¥à¤¸ à¤¦à¤¿à¤¸à¤¤à¥‹. à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤¬à¥‰à¤•à¥à¤¸à¤®à¤§à¥à¤¯à¥‡ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤µà¤¾à¤°à¥à¤·à¤¿à¤• à¤¹à¤œà¥‡à¤°à¥€à¤šà¤¾ à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤¤à¤ªà¤¶à¥€à¤² à¤…à¤¸à¤¤à¥‹. à¤–à¤¾à¤²à¥€à¤² à¤¸à¥à¤Ÿà¥‡à¤ªà¥à¤¸à¤®à¤§à¥à¤¯à¥‡ à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤­à¤¾à¤— à¤¸à¤®à¤œà¥‚à¤¨ à¤˜à¥à¤¯à¤¾."
                          : "Each student has their own attendance box here. It shows the full yearly attendance summary. The next steps will explain each part of the box."));
                s.add(new TourStep(R.id.rvAttendanceStudents, 0, R.id.tvAttendanceTotal,
                        m ? "à¥­. à¤à¤•à¥‚à¤£ à¤¹à¤œà¤° à¤¦à¤¿à¤µà¤¸ ðŸŸ¢" : "7. Total Present Days ðŸŸ¢",
                        m ? "à¤¬à¥‰à¤•à¥à¤¸à¤šà¥à¤¯à¤¾ à¤¡à¤¾à¤µà¥à¤¯à¤¾ à¤¬à¤¾à¤œà¥‚à¤²à¤¾ à¤®à¥‹à¤ à¤¾ à¤¹à¤¿à¤°à¤µà¤¾ à¤†à¤•à¤¡à¤¾ à¤¦à¤¿à¤¸à¤¤à¥‹.\n\nà¤¹à¤¾ à¤†à¤•à¤¡à¤¾ à¤®à¥à¤¹à¤£à¤œà¥‡ à¤¤à¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤µà¤°à¥à¤·à¤­à¤°à¤¾à¤¤à¥€à¤² à¤à¤•à¥‚à¤£ 'à¤¹à¤œà¤° à¤¦à¤¿à¤µà¤¸' (Total Present Days) à¤¹à¥‹à¤¯.\n\nà¤œà¤¾à¤¸à¥à¤¤ à¤†à¤•à¤¡à¤¾ = à¤œà¤¾à¤¸à¥à¤¤ à¤‰à¤ªà¤¸à¥à¤¥à¤¿à¤¤à¥€. ðŸŽ‰"
                          : "The large green number on the left side of the box shows the student's Total Present Days for the entire year.\n\nHigher number = Better attendance! ðŸŽ‰"));
                s.add(new TourStep(R.id.rvAttendanceStudents, 0, R.id.tvJun,
                        m ? "à¥®. à¥§à¥¨ à¤®à¤¹à¤¿à¤¨à¥à¤¯à¤¾à¤‚à¤šà¤¾ à¤¤à¤•à¥à¤¤à¤¾ ðŸ“Š" : "8. 12 Months Grid ðŸ“Š",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤¬à¥‰à¤•à¥à¤¸à¤®à¤§à¥à¤¯à¥‡ à¤œà¥‚à¤¨ à¤¤à¥‡ à¤®à¥‡ à¤ªà¤°à¥à¤¯à¤‚à¤¤ à¥§à¥¨ à¤®à¤¹à¤¿à¤¨à¥à¤¯à¤¾à¤‚à¤šà¤¾ à¤¤à¤•à¥à¤¤à¤¾ à¤…à¤¸à¤¤à¥‹.\n\nà¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤®à¤¹à¤¿à¤¨à¥à¤¯à¤¾à¤¤ 'à¤¹à¤œà¤° / à¤à¤•à¥‚à¤£' (à¤‰à¤¦à¤¾. 24/26) à¤…à¤¸à¥‡ à¤²à¤¿à¤¹à¤¿à¤²à¥‡à¤²à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡.\nâ€¢ à¤ªà¤¹à¤¿à¤²à¤¾ à¤†à¤•à¤¡à¤¾ = à¤¤à¥à¤¯à¤¾ à¤®à¤¹à¤¿à¤¨à¥à¤¯à¤¾à¤¤ à¤¹à¤œà¤° à¤¦à¤¿à¤µà¤¸\nâ€¢ à¤¦à¥à¤¸à¤°à¤¾ à¤†à¤•à¤¡à¤¾ = à¤à¤•à¥‚à¤£ à¤•à¤¾à¤®à¤•à¤¾à¤œà¤¾à¤šà¥‡ à¤¦à¤¿à¤µà¤¸\n\nà¤¤à¤•à¥à¤¤à¥à¤¯à¤¾à¤µà¤° à¤¸à¥à¤•à¥à¤°à¥‹à¤² à¤•à¤°à¥‚à¤¨ à¤¸à¤°à¥à¤µ à¤®à¤¹à¤¿à¤¨à¥‡ à¤ªà¤¾à¤¹à¤¤à¤¾ à¤¯à¥‡à¤¤à¤¾à¤¤."
                          : "Each box contains a 12-month grid from June to May.\n\nEach cell shows 'Present / Total' (e.g. 24/26):\nâ€¢ First number = Days present that month\nâ€¢ Second number = Total working days that month\n\nScroll the grid to see all months."));
                s.add(new TourStep(R.id.rvAttendanceStudents, 0, R.id.cardStudentAttendance,
                        m ? "à¥¯. à¤¹à¤œà¥‡à¤°à¥€ à¤¬à¤¦à¤²à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¬à¥‰à¤•à¥à¤¸à¤µà¤° à¤•à¥à¤²à¤¿à¤• à¤•à¤°à¤¾ âœï¸" : "9. Tap Box to Edit Attendance âœï¸",
                        m ? "à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾à¤¹à¥€ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤¹à¤œà¥‡à¤°à¥€ à¤¬à¥‰à¤•à¥à¤¸à¤µà¤° à¤•à¥à¤²à¤¿à¤• à¤•à¤°à¤¾.\n\nà¤à¤• 'à¤¹à¤œà¥‡à¤°à¥€ à¤­à¤°à¤¾' à¤ªà¥‰à¤ª-à¤…à¤ª à¤‰à¤˜à¤¡à¥‡à¤². à¤¤à¥à¤¯à¤¾à¤¤:\nâ€¢ à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤®à¤¹à¤¿à¤¨à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¹à¤œà¤° à¤¦à¤¿à¤µà¤¸ à¤µ à¤à¤•à¥‚à¤£ à¤•à¤¾à¤®à¤•à¤¾à¤œ à¤¦à¤¿à¤µà¤¸ à¤Ÿà¤¾à¤•à¤¾.\nâ€¢ 'à¤œà¤¤à¤¨ à¤•à¤°à¤¾' à¤¦à¤¾à¤¬à¥‚à¤¨ à¤¹à¤œà¥‡à¤°à¥€ à¤¸à¥‡à¤µà¥à¤¹ à¤•à¤°à¤¾.\n\nà¤¹à¥‡ à¤…à¤—à¤¦à¥€ à¤¤à¤¶à¤¾à¤š à¤ªà¤¦à¥à¤§à¤¤à¥€à¤¨à¥‡ à¤•à¤¾à¤® à¤•à¤°à¤¤à¥‡ à¤œà¤¸à¥‡ à¤†à¤•à¤¾à¤°à¤¿à¤•-à¤¸à¤‚à¤•à¤²à¤¿à¤¤ à¤—à¥à¤£ à¤­à¤°à¤¤à¤¾à¤¨à¤¾ à¤•à¤°à¤¤à¤¾à¤¤."
                          : "Tap anywhere on any student's attendance box to open the Edit Attendance popup.\n\nIn the popup:\nâ€¢ Enter Present Days and Working Days for each month.\nâ€¢ Tap 'Save' to save the attendance.\n\nThis works exactly like the marks entry popup on the Formative/Summative page."));
                s.add(new TourStep(R.id.rvAttendanceStudents, 0, R.id.ivOptions,
                        m ? "à¥§à¥¦. â‹® à¤®à¥‡à¤¨à¥à¤¯à¥‚: Duplicate à¤µ Delete" : "10. â‹® Menu: Duplicate & Delete",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤¹à¤œà¥‡à¤°à¥€ à¤¬à¥‰à¤•à¥à¤¸à¤šà¥à¤¯à¤¾ à¤‰à¤œà¤µà¥à¤¯à¤¾ à¤•à¥‹à¤ªà¤±à¥à¤¯à¤¾à¤¤ (â‹®) à¤®à¥‡à¤¨à¥à¤¯à¥‚ à¤…à¤¸à¤¤à¥‹.\n\nâ€¢ 'Duplicate' â†’ à¤à¤•à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥€ à¤¹à¤œà¥‡à¤°à¥€ à¤¦à¥à¤¸à¤±à¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤µà¤° à¤•à¥‰à¤ªà¥€ à¤•à¤°à¤¾ (à¤‰à¤¦à¤¾. à¤µà¤°à¥à¤—à¤¾à¤šà¥€ à¤¸à¤®à¤¾à¤¨ à¤¹à¤œà¥‡à¤°à¥€ à¤¸à¤°à¥à¤µà¤¾à¤‚à¤¸à¤¾à¤ à¥€ à¤²à¤¾à¤µà¤¾à¤¯à¤šà¥€ à¤…à¤¸à¤²à¥à¤¯à¤¾à¤¸).\nâ€¢ 'Delete' â†’ à¤¤à¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥€ à¤¸à¤°à¥à¤µ à¤¹à¤œà¥‡à¤°à¥€ à¤¨à¤·à¥à¤Ÿ à¤•à¤°à¤¾.\n\nâš ï¸ Delete à¤•à¥‡à¤²à¥à¤¯à¤¾à¤µà¤° à¤¹à¤œà¥‡à¤°à¥€ à¤ªà¤°à¤¤ à¤¯à¥‡à¤¤ à¤¨à¤¾à¤¹à¥€, à¤•à¤¾à¤³à¤œà¥€ à¤˜à¥à¤¯à¤¾!"
                          : "Each attendance box has a â‹® (3-dots) menu in the top-right corner.\n\nâ€¢ 'Duplicate' â†’ Copy this student's attendance to another student (useful when multiple students have the same attendance).\nâ€¢ 'Delete' â†’ Permanently remove all attendance data for this student.\n\nâš ï¸ Deleted attendance cannot be recovered!"));
                break;


            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 6. Formative & Summative Evaluation  (8 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "formative_summative":
                s.add(new TourStep(0,
                        m ? "ðŸ“ à¤†à¤•à¤¾à¤°à¤¿à¤• / à¤¸à¤‚à¤•à¤²à¤¿à¤¤ à¤®à¥‚à¤²à¥à¤¯à¤®à¤¾à¤ªà¤¨" : "ðŸ“ Formative / Summative Evaluation",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥‡ à¤†à¤•à¤¾à¤°à¤¿à¤• (FA) à¤µ à¤¸à¤‚à¤•à¤²à¤¿à¤¤ (SA) à¤®à¥‚à¤²à¥à¤¯à¤®à¤¾à¤ªà¤¨ à¤¯à¥‡à¤¥à¥‡ à¤µà¥à¤¯à¤µà¤¸à¥à¤¥à¤¾à¤ªà¤¿à¤¤ à¤•à¤°à¤¾."
                          : "Manage Formative (FA) and Summative (SA) evaluations for the class."));
                s.add(new TourStep(R.id.tvHeaderStripInfo,
                        m ? "à¥§. à¤¸à¤¤à¥à¤° à¤µ à¤ªà¤°à¥€à¤•à¥à¤·à¤¾ à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Session & Exam Info",
                        m ? "à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤µà¤°à¥à¤·, à¤¸à¤¤à¥à¤°, à¤µà¤°à¥à¤— à¤µ à¤ªà¤°à¥€à¤•à¥à¤·à¥‡à¤¸à¤¾à¤ à¥€ à¤—à¥à¤£ à¤¨à¥‹à¤‚à¤¦à¤µà¤¤ à¤†à¤¹à¤¾à¤¤ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the year, semester, class and exam you are entering marks for."));
                s.add(new TourStep(R.id.btnHelpSquare,
                        m ? "à¥¨. à¤®à¤¦à¤¤ ?" : "2. Help ?",
                        m ? "à¤¯à¤¾ à¤ªà¤¾à¤¨à¤¾à¤šà¥€ à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤®à¤¾à¤°à¥à¤—à¤¦à¤°à¥à¤¶à¤¿à¤•à¤¾ à¤µ à¤ªà¥à¤²à¥‡ à¤¬à¤Ÿà¤£ à¤¯à¥‡à¤¥à¥‡ à¤†à¤¹à¥‡."
                          : "Tap '?' to open the complete guide and animated tour for this page."));
                s.add(new TourStep(R.id.btnAddSquare,
                        m ? "à¥©. à¤®à¥‚à¤²à¥à¤¯à¤®à¤¾à¤ªà¤¨ à¤œà¥‹à¤¡à¤¾ +" : "3. Add Evaluation +",
                        m ? "à¤¨à¤µà¥€à¤¨ FA à¤•à¤¿à¤‚à¤µà¤¾ SA à¤®à¥‚à¤²à¥à¤¯à¤®à¤¾à¤ªà¤¨ à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ '+' à¤¦à¤¾à¤¬à¤¾."
                          : "Tap '+' to create a new Formative or Summative evaluation entry."));
                s.add(new TourStep(R.id.btnCalcSquare,
                        m ? "à¥ª. à¤à¤•à¤¤à¥à¤°à¤¿à¤¤ à¤—à¥à¤£ à¤…à¤¹à¤µà¤¾à¤² ðŸ“Š" : "4. Full Marks Report ðŸ“Š",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤šà¤¾ à¤à¤•à¥‚à¤£ à¤¸à¤°à¤¾à¤¸à¤°à¥€ à¤¨à¤¿à¤•à¤¾à¤², à¤‰à¤¤à¥à¤¤à¥€à¤°à¥à¤£-à¤…à¤¨à¥à¤¤à¥à¤¤à¥€à¤°à¥à¤£ à¤ªà¥à¤°à¤®à¤¾à¤£ à¤†à¤£à¤¿ à¤¸à¤°à¥à¤µà¤¾à¤§à¤¿à¤• à¤—à¥à¤£ à¤®à¤¿à¤³à¤µà¤£à¤¾à¤°à¥‡ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¯à¤¾à¤‚à¤šà¤¾ à¤à¤•à¤¤à¥à¤°à¤¿à¤¤ à¤…à¤¹à¤µà¤¾à¤² à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap this icon to view the full class marks report, class average, pass/fail status, and top scorers."));
                s.add(new TourStep(R.id.btnSearchStudents,
                        m ? "à¥«. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¶à¥‹à¤§à¤¾ ðŸ”" : "5. Search Students ðŸ”",
                        m ? "à¤¨à¤¾à¤µ à¤•à¤¿à¤‚à¤µà¤¾ à¤°à¥‹à¤² à¤¨à¤‚à¤¬à¤° à¤Ÿà¤¾à¤•à¥‚à¤¨ à¤¤à¥à¤µà¤°à¤¿à¤¤ à¤µà¤¿à¤¶à¤¿à¤·à¥à¤Ÿ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¶à¥‹à¤§à¤¾."
                          : "Type a name or roll number to quickly find a specific student."));
                s.add(new TourStep(R.id.btnGridListToggle,
                        m ? "à¥¬. à¤¦à¥ƒà¤¶à¥à¤¯ à¤¬à¤¦à¤²à¤¾ ðŸ”„" : "6. Toggle View ðŸ”„",
                        m ? "à¤—à¥à¤°à¥€à¤¡ à¤¦à¥ƒà¤¶à¥à¤¯ (à¤…à¤¨à¥‡à¤• à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤à¤•à¤¤à¥à¤°) à¤•à¤¿à¤‚à¤µà¤¾ à¤²à¤¿à¤¸à¥à¤Ÿ à¤¦à¥ƒà¤¶à¥à¤¯ à¤¨à¤¿à¤µà¤¡à¤¾."
                          : "Switch between Grid view (compact) and List view (detailed)."));
                s.add(new TourStep(R.id.btnThreeDotMenu,
                        m ? "à¥­. à¤…à¤§à¤¿à¤• à¤ªà¤°à¥à¤¯à¤¾à¤¯ â‹®" : "7. More Options â‹®",
                        m ? "â‹® à¤®à¥‡à¤¨à¥‚à¤®à¤§à¥à¤¯à¥‡: à¤—à¥à¤£ à¤¸à¥‡à¤µà¥à¤¹, à¤°à¥€à¤¸à¥‡à¤Ÿ, à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤•à¥à¤°à¤® à¤¬à¤¦à¤²à¤£à¥‡ à¤‡à¤¤à¥à¤¯à¤¾à¤¦à¥€ à¤ªà¤°à¥à¤¯à¤¾à¤¯ à¤†à¤¹à¥‡à¤¤."
                          : "â‹® menu has save, reset marks and reorder student options."));
                s.add(new TourStep(R.id.rvEvaluationStudents,
                        m ? "à¥®. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤®à¥‚à¤²à¥à¤¯à¤®à¤¾à¤ªà¤¨ à¤¯à¤¾à¤¦à¥€" : "8. Student Evaluation List",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤°à¤¾à¤‚à¤—à¥‡à¤¤ à¤—à¥à¤£ à¤­à¤°à¤¾. à¤°à¤‚à¤—à¥€à¤¤ à¤—à¥à¤£ = à¤¸à¥‡à¤µà¥à¤¹ à¤à¤¾à¤²à¥‡."
                          : "Enter marks in each student row. Coloured marks are already saved."));
                s.add(new TourStep(R.id.swipeRefreshLayout,
                        m ? "à¥¯. à¤–à¤¾à¤²à¥€ à¤“à¤¢à¥‚à¤¨ à¤°à¤¿à¤«à¥à¤°à¥‡à¤¶ à¤•à¤°à¤¾" : "9. Pull to Refresh",
                        m ? "à¤¯à¤¾à¤¦à¥€ à¤–à¤¾à¤²à¥€ à¤“à¤¢à¤²à¥à¤¯à¤¾à¤¸ à¤¤à¤¾à¤œà¥à¤¯à¤¾ à¤¡à¥‡à¤Ÿà¤¾à¤¨à¥‡ à¤°à¤¿à¤«à¥à¤°à¥‡à¤¶ à¤¹à¥‹à¤¤à¥‡."
                          : "Pull the list down to refresh and reload the latest student data."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 7. Descriptive Remarks List  (6 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "descriptive":
                s.add(new TourStep(0,
                        m ? "ðŸ’¬ à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¨à¥‹à¤‚à¤¦à¥€" : "ðŸ’¬ Descriptive Remarks",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥‡ à¤µà¤°à¥à¤¤à¤¨, à¤•à¥Œà¤¶à¤²à¥à¤¯à¥‡ à¤µ à¤µà¤¿à¤¶à¥‡à¤· à¤¸à¤‚à¤ªà¤¾à¤¦à¤£à¥‚à¤• à¤¶à¥‡à¤°à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¨à¥‹à¤‚à¤¦à¤µà¤¾."
                          : "Record student behavior, skills and special achievement remarks."));
                s.add(new TourStep(R.id.tvHeaderStripInfo,
                        m ? "à¥§. à¤¸à¤¤à¥à¤° à¤µ à¤µà¤°à¥à¤— à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Session & Class Info",
                        m ? "à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤µà¤°à¥à¤— à¤µ à¤¸à¤¤à¥à¤°à¤¾à¤¸à¤¾à¤ à¥€ à¤¶à¥‡à¤°à¥‡ à¤¨à¥‹à¤‚à¤¦à¤µà¤¤ à¤†à¤¹à¤¾à¤¤ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows which class and semester the remarks are being entered for."));
                s.add(new TourStep(R.id.btnHelpSquare,
                        m ? "à¥¨. à¤®à¤¦à¤¤ ?" : "2. Help ?",
                        m ? "à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¨à¥‹à¤‚à¤¦à¥€ à¤ªà¤¾à¤¨à¤¾à¤šà¥€ à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤®à¤¾à¤°à¥à¤—à¤¦à¤°à¥à¤¶à¤¿à¤•à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤®à¤¿à¤³à¥‡à¤²."
                          : "Tap '?' for the complete guide on how to use Descriptive Remarks."));
                s.add(new TourStep(R.id.btnAddSquare,
                        m ? "à¥©. à¤¶à¥‡à¤°à¥‡ à¤œà¥‹à¤¡à¤¾ +" : "3. Add Remarks +",
                        m ? "à¤¨à¤µà¥€à¤¨ à¤¶à¥‡à¤±à¥à¤¯à¤¾à¤šà¥€ à¤¨à¥‹à¤‚à¤¦ à¤•à¤°à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ '+' à¤¬à¤Ÿà¤£ à¤¦à¤¾à¤¬à¤¾."
                          : "Tap '+' to begin adding a new descriptive remark entry."));
                s.add(new TourStep(R.id.btnCalcSquare,
                        m ? "à¥ª. à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¨à¥‹à¤‚à¤¦à¥€ à¤…à¤¹à¤µà¤¾à¤² ðŸ“Š" : "4. Remarks Report ðŸ“Š",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥‡ à¤à¤•à¥‚à¤£ à¤¶à¥‡à¤°à¥‡ à¤†à¤£à¤¿ à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥‡ à¤¶à¥‡à¤°à¥‡ à¤…à¤ªà¥‚à¤°à¥à¤£ à¤†à¤¹à¥‡à¤¤, à¤¯à¤¾à¤šà¤¾ à¤à¤•à¤¤à¥à¤°à¤¿à¤¤ à¤…à¤¹à¤µà¤¾à¤² à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap this icon to view the full descriptive remarks report, total remarks entered, and student progress status."));
                s.add(new TourStep(R.id.btnSearchStudents,
                        m ? "à¥«. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¶à¥‹à¤§à¤¾ ðŸ”" : "5. Search Students ðŸ”",
                        m ? "à¤¨à¤¾à¤µ à¤•à¤¿à¤‚à¤µà¤¾ à¤°à¥‹à¤² à¤¨à¤‚à¤¬à¤° à¤Ÿà¤¾à¤•à¥‚à¤¨ à¤¤à¥à¤µà¤°à¤¿à¤¤ à¤µà¤¿à¤¶à¤¿à¤·à¥à¤Ÿ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¶à¥‹à¤§à¤¾."
                          : "Type a name or roll number to quickly find a specific student."));
                s.add(new TourStep(R.id.btnGridListToggle,
                        m ? "à¥¬. à¤¦à¥ƒà¤¶à¥à¤¯ à¤¬à¤¦à¤²à¤¾ ðŸ”„" : "6. Toggle View ðŸ”„",
                        m ? "à¤—à¥à¤°à¥€à¤¡ à¤¦à¥ƒà¤¶à¥à¤¯ (à¤…à¤¨à¥‡à¤• à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤à¤•à¤¤à¥à¤°) à¤•à¤¿à¤‚à¤µà¤¾ à¤²à¤¿à¤¸à¥à¤Ÿ à¤¦à¥ƒà¤¶à¥à¤¯ à¤¨à¤¿à¤µà¤¡à¤¾."
                          : "Switch between Grid view (compact) and List view (detailed)."));
                s.add(new TourStep(R.id.btnThreeDotMenu,
                        m ? "à¥­. à¤…à¤§à¤¿à¤• à¤ªà¤°à¥à¤¯à¤¾à¤¯ â‹®" : "7. More Options â‹®",
                        m ? "â‹® à¤®à¥‡à¤¨à¥‚à¤®à¤§à¥à¤¯à¥‡: à¤¶à¥‡à¤°à¥‡ à¤¡à¤¿à¤²à¥€à¤Ÿ, 'à¤¤à¥‹'â†’'à¤¤à¥€' (à¤²à¤¿à¤‚à¤— à¤¬à¤¦à¤²), à¤°à¥€à¤¸à¥‡à¤Ÿ à¤ªà¤°à¥à¤¯à¤¾à¤¯ à¤†à¤¹à¥‡à¤¤."
                          : "â‹® has delete entries, swap gender (heâ†’she) and reset options."));
                s.add(new TourStep(R.id.rvDescriptiveStudents,
                        m ? "à¥®. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¯à¤¾à¤¦à¥€" : "8. Student List",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤•à¤¾à¤°à¥à¤¡à¤¾à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¥‡à¤²à¥à¤¯à¤¾à¤¸ à¤¤à¥à¤¯à¤¾à¤šà¥‡ à¤¶à¥‡à¤°à¥‡ à¤¸à¤‚à¤ªà¤¾à¤¦à¤¿à¤¤ à¤¹à¥‹à¤¤à¤¾à¤¤."
                          : "Tap a student card to open and edit their descriptive remarks."));
                s.add(new TourStep(R.id.swipeRefreshLayout,
                        m ? "à¥¯. à¤–à¤¾à¤²à¥€ à¤“à¤¢à¥‚à¤¨ à¤°à¤¿à¤«à¥à¤°à¥‡à¤¶ à¤•à¤°à¤¾" : "9. Pull to Refresh",
                        m ? "à¤¯à¤¾à¤¦à¥€ à¤–à¤¾à¤²à¥€ à¤“à¤¢à¤²à¥à¤¯à¤¾à¤¸ à¤¤à¤¾à¤œà¥à¤¯à¤¾ à¤¡à¥‡à¤Ÿà¤¾à¤¨à¥‡ à¤°à¤¿à¤«à¥à¤°à¥‡à¤¶ à¤¹à¥‹à¤¤à¥‡."
                          : "Pull the list down to refresh and reload the latest student data."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 8. Subjects  (4 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "subjects":
                s.add(new TourStep(0,
                        m ? "ðŸ“š à¤µà¤¿à¤·à¤¯ à¤µà¥à¤¯à¤µà¤¸à¥à¤¥à¤¾à¤ªà¤¨" : "ðŸ“š Subject Management",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤ à¤¶à¤¿à¤•à¤µà¤²à¥‡ à¤œà¤¾à¤£à¤¾à¤°à¥‡ à¤µà¤¿à¤·à¤¯ à¤¯à¥‡à¤¥à¥‡ à¤œà¥‹à¤¡à¤¾, à¤¸à¤‚à¤ªà¤¾à¤¦à¤¿à¤¤ à¤•à¤°à¤¾ à¤µ à¤•à¥à¤°à¤® à¤²à¤¾à¤µà¤¾."
                          : "Add, edit and arrange the subjects taught in the active class."));
                s.add(new TourStep(R.id.tvHeaderLabel,
                        m ? "à¥§. à¤µà¤°à¥à¤— à¤†à¤£à¤¿ à¤¸à¤¤à¥à¤° à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Class & Session Info",
                        m ? "à¤¤à¥à¤®à¥à¤¹à¥€ à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤µà¤°à¥à¤·à¤¾à¤¸à¤¾à¤ à¥€ à¤†à¤£à¤¿ à¤µà¤°à¥à¤—à¤¾à¤¸à¤¾à¤ à¥€ à¤µà¤¿à¤·à¤¯ à¤µà¥à¤¯à¤µà¤¸à¥à¤¥à¤¾à¤ªà¤¿à¤¤ à¤•à¤°à¤¤ à¤†à¤¹à¤¾à¤¤ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¸à¥à¤ªà¤·à¥à¤Ÿ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the specific academic year and class you are currently managing subjects for."));
                s.add(new TourStep(R.id.rvSubjectsList,
                        m ? "à¥¨. à¤µà¤¿à¤·à¤¯à¤¾à¤‚à¤šà¥€ à¤¯à¤¾à¤¦à¥€" : "2. Subjects List",
                        m ? "à¤µà¤¿à¤·à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤¨à¤¾à¤µà¤¾à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¤°à¤¾ â†’ à¤¤à¤ªà¤¶à¥€à¤², à¤•à¤®à¤¾à¤² à¤—à¥à¤£ à¤µ à¤®à¤¾à¤§à¥à¤¯à¤® à¤¸à¤‚à¤ªà¤¾à¤¦à¤¿à¤¤ à¤•à¤°à¤¾."
                          : "Tap a subject name to edit its details, max marks and medium."));
                s.add(new TourStep(R.id.rvSubjectsList, 0, R.id.tvSubjectCode,
                        m ? "à¥©. à¤µà¤¿à¤·à¤¯ à¤•à¥‹à¤¡" : "3. Subject Code",
                        m ? "à¤•à¥‹à¤¡à¤šà¤¾ à¤ªà¤¹à¤¿à¤²à¤¾ à¤­à¤¾à¤— à¤‡à¤¯à¤¤à¥à¤¤à¤¾ (à¤‰à¤¦à¤¾. 101) à¤†à¤£à¤¿ à¤¦à¥à¤¸à¤°à¤¾ à¤­à¤¾à¤— à¤µà¤¿à¤·à¤¯ (à¤‰à¤¦à¤¾. 101) à¤¦à¤°à¥à¤¶à¤µà¤¤à¥‹. à¤¤à¥à¤®à¥à¤¹à¥€ à¤µà¤¿à¤·à¤¯à¤¾à¤µà¤° à¤•à¥à¤²à¤¿à¤• à¤•à¤°à¥‚à¤¨ à¤¸à¥à¤µà¤¤à¤ƒà¤šà¤¾ à¤•à¤¸à¥à¤Ÿà¤® à¤•à¥‹à¤¡ à¤¦à¥‡à¤–à¥€à¤² à¤¸à¥‡à¤Ÿ à¤•à¤°à¥‚ à¤¶à¤•à¤¤à¤¾."
                          : "The first part of the code represents the standard (e.g. 101) and the second part represents the subject (e.g. 101). You can also edit a subject to set your own custom code."));
                s.add(new TourStep(R.id.rvSubjectsList, 0, R.id.tvDetailsLeft1,
                        m ? "à¥ª. FE à¤µ SE à¤—à¥à¤£" : "4. FE & SE Marks",
                        m ? "FE (à¤†à¤•à¤¾à¤°à¤¿à¤•) à¤†à¤£à¤¿ SE (à¤¸à¤‚à¤•à¤²à¤¿à¤¤) à¤®à¥‚à¤²à¥à¤¯à¤®à¤¾à¤ªà¤¨à¤¾à¤šà¥‡ à¤à¤•à¥‚à¤£ à¤•à¤®à¤¾à¤² à¤—à¥à¤£ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¤¾à¤¤."
                          : "Total max marks for Formative (FE) and Summative (SE) evaluations are shown here."));
                s.add(new TourStep(R.id.rvSubjectsList, 0, R.id.btnCardMenu,
                        m ? "à¥«. à¤…à¤§à¤¿à¤• à¤ªà¤°à¥à¤¯à¤¾à¤¯ (à¥©-à¤¬à¤¿à¤‚à¤¦à¥‚)" : "5. More Options (3-dots)",
                        m ? "à¤¯à¥‡à¤¥à¥‡ à¤•à¥à¤²à¤¿à¤• à¤•à¤°à¥‚à¤¨ à¤¤à¥à¤®à¥à¤¹à¥€ à¤µà¤¿à¤·à¤¯à¤¾à¤šà¥‡ à¤•à¤®à¤¾à¤² à¤—à¥à¤£ à¤†à¤£à¤¿ à¤…à¤‚à¤¤à¤°à¥à¤—à¤¤ à¤­à¤¾à¤°à¤¾à¤‚à¤¶ à¤¸à¤µà¤¿à¤¸à¥à¤¤à¤° à¤¸à¤‚à¤ªà¤¾à¤¦à¤¿à¤¤ à¤•à¤°à¥‚ à¤¶à¤•à¤¤à¤¾."
                          : "Tap here to edit the subject's max marks and internal evaluation weightage in detail."));
                s.add(new TourStep(R.id.fabAddSubject,
                        m ? "à¥¬. à¤¨à¤µà¥€à¤¨ à¤µà¤¿à¤·à¤¯ à¤œà¥‹à¤¡à¤¾ +" : "6. Add New Subject +",
                        m ? "à¤¯à¤¾ '+' à¤¬à¤Ÿà¤£à¤¾à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¤°à¥‚à¤¨ à¤¤à¥à¤®à¥à¤¹à¥€ à¤µà¤°à¥à¤—à¤¾à¤¸à¤¾à¤ à¥€ à¤•à¥‹à¤£à¤¤à¤¾à¤¹à¥€ à¤¨à¤µà¥€à¤¨ à¤•à¤¿à¤‚à¤µà¤¾ à¤•à¤¸à¥à¤Ÿà¤® à¤µà¤¿à¤·à¤¯ à¤œà¥‹à¤¡à¥‚ à¤¶à¤•à¤¤à¤¾."
                          : "Tap this '+' button to add any new or custom subject to the class."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 9. Declare Weightage  (5 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "weightage":
                s.add(new TourStep(0,
                        m ? "âš–ï¸ à¤—à¥à¤£ à¤­à¤¾à¤°à¤¾à¤‚à¤¶" : "âš–ï¸ Declare Weightage",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤·à¤¯à¤¾à¤šà¥‡ à¤†à¤•à¤¾à¤°à¤¿à¤• à¤µ à¤¸à¤‚à¤•à¤²à¤¿à¤¤ à¤‰à¤ª-à¤˜à¤Ÿà¤•à¤¾à¤‚à¤šà¥‡ à¤­à¤¾à¤°à¤¾à¤‚à¤¶ à¤¯à¥‡à¤¥à¥‡ à¤ à¤°à¤µà¤¾."
                          : "Define the weightage for each subject's formative and summative components."));
                s.add(new TourStep(R.id.tvWeightageHeaderContext,
                        m ? "à¥§. à¤¸à¤¤à¥à¤° à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Session Info",
                        m ? "à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤µà¤°à¥à¤· à¤µ à¤¸à¤¤à¥à¤°à¤¾à¤¸à¤¾à¤ à¥€ à¤­à¤¾à¤°à¤¾à¤‚à¤¶ à¤ à¤°à¤µà¤¤ à¤†à¤¹à¤¾à¤¤ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¸à¥à¤ªà¤·à¥à¤Ÿ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Confirms which academic year and semester this weightage applies to."));
                s.add(new TourStep(R.id.rvWeightageSubjects, 0, R.id.etMaxMarks,
                        m ? "à¥¨. à¤à¤•à¥‚à¤£ à¤•à¤®à¤¾à¤² à¤—à¥à¤£" : "2. Total Max Marks",
                        m ? "à¤¯à¥‡à¤¥à¥‡ à¤µà¤¿à¤·à¤¯à¤¾à¤šà¥‡ à¤à¤•à¥‚à¤£ à¤•à¤®à¤¾à¤² à¤—à¥à¤£ à¤­à¤°à¤¾. à¤¹à¥‡ à¤¬à¤¦à¤²à¤²à¥à¤¯à¤¾à¤¸ à¤…à¤‚à¤¤à¤°à¥à¤—à¤¤ à¤—à¥à¤£ à¤†à¤ªà¥‹à¤†à¤ª à¤µà¤¿à¤­à¤¾à¤—à¤²à¥‡ à¤œà¤¾à¤¤à¥€à¤²."
                          : "Enter the total max marks for the subject here. Internal marks will auto-scale accordingly."));
                s.add(new TourStep(R.id.rvWeightageSubjects, 0, R.id.layoutHeader,
                        m ? "à¥©. à¤¸à¤µà¤¿à¤¸à¥à¤¤à¤° à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤‰à¤˜à¤¡à¤¾" : "3. Open Details",
                        m ? "à¤µà¤¿à¤·à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤•à¤¾à¤°à¥à¤¡à¤¾à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¥‡à¤²à¥à¤¯à¤¾à¤¸ à¤…à¤‚à¤¤à¤°à¥à¤—à¤¤ à¤—à¥à¤£à¤¾à¤‚à¤šà¥€ (à¤‰à¤¦à¤¾. à¤²à¥‡à¤–à¥€, à¤¤à¥‹à¤‚à¤¡à¥€, à¤ªà¥à¤°à¤•à¤²à¥à¤ª) à¤¸à¤µà¤¿à¤¸à¥à¤¤à¤° à¤µà¤¿à¤­à¤¾à¤—à¤£à¥€ à¤‰à¤˜à¤¡à¥‡à¤²."
                          : "Tap on the subject card to expand and see the detailed breakdown (Written, Oral, Project, etc.)."));
                s.add(new TourStep(R.id.rvWeightageSubjects, 0, R.id.tvWeightageBreakdown,
                        m ? "à¥ª. à¤—à¥à¤£ à¤µà¤¿à¤­à¤¾à¤—à¤£à¥€ à¤¬à¥‡à¤°à¥€à¤œ" : "4. Marks Distribution",
                        m ? "à¤¯à¥‡à¤¥à¥‡ à¤¤à¥à¤®à¥à¤¹à¤¾à¤²à¤¾ à¤†à¤•à¤¾à¤°à¤¿à¤• (FA) à¤†à¤£à¤¿ à¤¸à¤‚à¤•à¤²à¤¿à¤¤ (SA) à¤—à¥à¤£à¤¾à¤‚à¤šà¥€ à¤à¤•à¥‚à¤£ à¤¬à¥‡à¤°à¥€à¤œ à¤¨à¥‡à¤¹à¤®à¥€ à¤¦à¤¿à¤¸à¥‡à¤²."
                          : "Here you can always see the total sum of Formative (FA) and Summative (SA) marks."));
                s.add(new TourStep(R.id.btnSaveWeightage,
                        m ? "à¥«. à¤­à¤¾à¤°à¤¾à¤‚à¤¶ à¤œà¤¤à¤¨ à¤•à¤°à¤¾ ðŸ’¾" : "5. Save Weightage ðŸ’¾",
                        m ? "à¤¸à¤°à¥à¤µ à¤­à¤¾à¤°à¤¾à¤‚à¤¶ à¤­à¤°à¤²à¥à¤¯à¤¾à¤µà¤° 'à¤œà¤¤à¤¨ à¤•à¤°à¤¾' à¤¦à¤¾à¤¬à¤¾. à¤à¤•à¤¦à¤¾à¤š à¤¸à¥‡à¤µà¥à¤¹ à¤•à¥‡à¤²à¥à¤¯à¤¾à¤¸ à¤ªà¥à¤°à¥‡à¤¸à¥‡."
                          : "Press Save after filling all fields. Saving once applies to all students."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 10. School Settings Dashboard  (9 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "school_settings":
                s.add(new TourStep(0,
                        m ? "ðŸ« à¤¶à¤¾à¤³à¤¾ à¤¸à¥‡à¤Ÿà¤¿à¤‚à¤—à¥à¤œ" : "ðŸ« School Settings",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤šà¥‡ à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤µà¥à¤¯à¤µà¤¸à¥à¤¥à¤¾à¤ªà¤¨ â€” à¤®à¤¾à¤¹à¤¿à¤¤à¥€, à¤²à¤¿à¤‚à¤—, à¤œà¤¾à¤¤, à¤¶à¤¿à¤•à¥à¤·à¤•, à¤µà¤°à¥à¤—, à¤µà¤¿à¤·à¤¯ à¤µ à¤¦à¤¿à¤µà¤¸."
                          : "Full school management â€” info, gender, caste, teachers, classes, subjects & days."));
                s.add(new TourStep(R.id.btnDashSchoolInfo,
                        m ? "à¥§. ðŸ¢ à¤¶à¤¾à¤³à¥‡à¤šà¥€ à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. ðŸ¢ School Info",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤šà¥‡ à¤¨à¤¾à¤µ, à¤¯à¥à¤¡à¤¾à¤¯à¤¸ à¤•à¥‹à¤¡, à¤œà¤¿à¤²à¥à¤¹à¤¾, à¤ªà¤¤à¥à¤¤à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤¸à¤‚à¤ªà¤¾à¤¦à¤¿à¤¤ à¤•à¤°à¤¾."
                          : "Edit school name, UDISE code, district and full address."));
                s.add(new TourStep(R.id.btnDashGender,
                        m ? "à¥¨. ðŸ‘« à¤²à¤¿à¤‚à¤— à¤¸à¤¾à¤‚à¤–à¥à¤¯à¤¿à¤•à¥€" : "2. ðŸ‘« Gender Stats",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤®à¥à¤²à¥‡ à¤µ à¤®à¥à¤²à¥€à¤‚à¤šà¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾ à¤†à¤£à¤¿ à¤—à¥à¤£à¥‹à¤¤à¥à¤¤à¤° à¤¯à¥‡à¤¥à¥‡ à¤ªà¤¾à¤¹à¤¾."
                          : "View the count of boys and girls and their ratio in the active class."));
                s.add(new TourStep(R.id.btnDashCastCategory,
                        m ? "à¥©. ðŸ“Š à¤œà¤¾à¤¤ à¤ªà¥à¤°à¤µà¤°à¥à¤—" : "3. ðŸ“Š Caste Category",
                        m ? "à¤¸à¤°à¥à¤µ à¤œà¤¾à¤¤ à¤ªà¥à¤°à¤µà¤°à¥à¤—à¤¾à¤‚à¤®à¤§à¥€à¤² à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¸à¤‚à¤–à¥à¤¯à¥‡à¤šà¥‡ à¤µà¤¿à¤¤à¤°à¤£ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "See student distribution across all caste categories in the school."));
                s.add(new TourStep(R.id.btnDashClassTeacher,
                        m ? "à¥ª. ðŸ‘©â€ðŸ« à¤µà¤°à¥à¤— à¤¶à¤¿à¤•à¥à¤·à¤•" : "4. ðŸ‘©â€ðŸ« Class Teacher",
                        m ? "à¤µà¤°à¥à¤— à¤¶à¤¿à¤•à¥à¤·à¤•, à¤¸à¤¹à¤¾à¤¯à¥à¤¯à¤• à¤¶à¤¿à¤•à¥à¤·à¤•, à¤ˆà¤®à¥‡à¤² à¤µ à¤«à¥‹à¤¨ à¤¯à¥‡à¤¥à¥‡ à¤¨à¥‹à¤‚à¤¦à¤µà¤¾."
                          : "Enter class teacher, assistant teacher, email and phone details."));
                s.add(new TourStep(R.id.btnDashClasses,
                        m ? "à¥«. ðŸ“‹ à¤µà¤°à¥à¤— à¤¯à¤¾à¤¦à¥€" : "5. ðŸ“‹ Classes List",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤¤à¥€à¤² à¤¸à¤°à¥à¤µ à¤µà¤°à¥à¤—à¤¾à¤‚à¤šà¥€ à¤¯à¤¾à¤¦à¥€ à¤µ à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤ªà¤¾à¤¹à¤¾."
                          : "View all classes with their student counts across the whole school."));
                s.add(new TourStep(R.id.btnDashSubject,
                        m ? "à¥¬. ðŸ“– à¤¶à¥‡à¤°à¤¾ à¤¬à¤à¤•" : "6. ðŸ“– Remark Bank",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤·à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¤à¤¯à¤¾à¤°-à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¶à¥‡à¤°à¤¾ à¤ªà¤°à¥à¤¯à¤¾à¤¯ à¤¯à¥‡à¤¥à¥‡ à¤µà¥à¤¯à¤µà¤¸à¥à¤¥à¤¾à¤ªà¤¿à¤¤ à¤•à¤°à¤¾."
                          : "Manage the pre-written descriptive remark options for each subject."));
                s.add(new TourStep(R.id.btnDashDefaultValues,
                        m ? "à¥­. âš™ï¸ à¤¡à¤¿à¤«à¥‰à¤²à¥à¤Ÿ à¤®à¥‚à¤²à¥à¤¯à¥‡" : "7. âš™ï¸ Default Values",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¨à¥‹à¤‚à¤¦à¤£à¥€à¤¸à¤¾à¤ à¥€ à¤¡à¤¿à¤«à¥‰à¤²à¥à¤Ÿ à¤®à¤¾à¤§à¥à¤¯à¤®, à¤œà¤¾à¤¤ à¤µ à¤‡à¤¤à¤° à¤®à¥‚à¤²à¥à¤¯à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¸à¥‡à¤Ÿ à¤•à¤°à¤¾."
                          : "Set default medium, caste and other values for new student registration."));
                s.add(new TourStep(R.id.btnDashWorkingDays,
                        m ? "à¥®. ðŸ“† à¤•à¤¾à¤®à¤•à¤¾à¤œà¤¾à¤šà¥‡ à¤¦à¤¿à¤µà¤¸" : "8. ðŸ“† Working Days",
                        m ? "à¤œà¥‚à¤¨ à¤¤à¥‡ à¤®à¥‡ à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤®à¤¹à¤¿à¤¨à¥à¤¯à¤¾à¤šà¥‡ à¤à¤•à¥‚à¤£ à¤•à¤¾à¤®à¤•à¤¾à¤œà¤¾à¤šà¥‡ à¤¦à¤¿à¤µà¤¸ à¤¯à¥‡à¤¥à¥‡ à¤¸à¥‡à¤Ÿ à¤•à¤°à¤¾."
                          : "Set total working days for each month from June to May."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 11. Print Report / Marksheet  (4 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "print_report":
                s.add(new TourStep(0,
                        m ? "ðŸ–¨ï¸ à¤—à¥à¤£à¤ªà¤¤à¥à¤°à¤• à¤…à¤¹à¤µà¤¾à¤²" : "ðŸ–¨ï¸ Print Report / Marksheet",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥‡ à¤…à¤§à¤¿à¤•à¥ƒà¤¤ à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¤¤à¥à¤°à¤• PDF à¤¸à¥à¤µà¤°à¥‚à¤ªà¤¾à¤¤ à¤¯à¥‡à¤¥à¥‡ à¤¤à¤¯à¤¾à¤° à¤µ à¤ªà¥à¤°à¤¿à¤‚à¤Ÿ à¤•à¤°à¤¾."
                          : "Generate and print official student report cards as PDF here."));
                s.add(new TourStep(R.id.tvReportPrintingYear,
                        m ? "à¥§. à¤µà¤°à¥à¤· à¤µ à¤¸à¤¤à¥à¤°" : "1. Year & Semester",
                        m ? "à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤µà¤°à¥à¤· à¤µ à¤¸à¤¤à¥à¤°à¤¾à¤¸à¤¾à¤ à¥€ à¤—à¥à¤£à¤ªà¤¤à¥à¤°à¤• à¤¤à¤¯à¤¾à¤° à¤¹à¥‹à¤¤ à¤†à¤¹à¥‡ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Confirms which academic year and semester marksheets are for."));
                s.add(new TourStep(R.id.btnWatchSquare,
                        m ? "à¥¨. à¤µà¥à¤¹à¤¿à¤¡à¤¿à¤“ ðŸŽ¥" : "2. Watch Video ðŸŽ¥",
                        m ? "à¥²à¤¨à¤¿à¤®à¥‡à¤Ÿà¥‡à¤¡ à¤µà¥à¤¹à¤¿à¤¡à¤¿à¤“ à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¤¾ à¤šà¤¿à¤¨à¥à¤¹à¤¾à¤µà¤° à¤•à¥à¤²à¤¿à¤• à¤•à¤°à¤¾."
                          : "Tap this icon to watch the animated guide video for this page."));
                s.add(new TourStep(R.id.btnHelpSquare,
                        m ? "à¥©. à¤®à¤¦à¤¤ ?" : "3. Help ?",
                        m ? "à¤¯à¤¾ à¤ªà¥‡à¤œà¤µà¤¿à¤·à¤¯à¥€ à¤¸à¤°à¥à¤µ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤®à¤¿à¤³à¤µà¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ '?' à¤µà¤° à¤•à¥à¤²à¤¿à¤• à¤•à¤°à¤¾."
                          : "Tap '?' to get all information about this report printing page."));
                s.add(new TourStep(R.id.rvReportCards, 0, R.id.btnReportSettings,
                        m ? "à¥ª. âš™ï¸ à¤¸à¥‡à¤Ÿà¤¿à¤‚à¤— à¤šà¤¿à¤¨à¥à¤¹" : "4. âš™ï¸ Settings Icon",
                        m ? "à¤•à¤¾à¤¹à¥€ à¤…à¤¹à¤µà¤¾à¤²à¤¾à¤‚à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¥‚à¤¨ à¤…à¤¤à¤¿à¤°à¤¿à¤•à¥à¤¤ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤­à¤°à¤¤à¤¾ à¤¯à¥‡à¤¤à¥‡ (à¤‰à¤¦à¤¾. à¤¶à¥‡à¤°à¥‡, à¤¤à¤¾à¤°à¤–à¤¾)." 
                          : "Tap here for reports that require additional settings or custom text."));
                s.add(new TourStep(R.id.rvReportCards, 0, R.id.btnReportAction,
                        m ? "à¥«. ðŸ–¨ï¸ à¤ªà¥à¤°à¤¿à¤‚à¤Ÿ à¤šà¤¿à¤¨à¥à¤¹" : "5. ðŸ–¨ï¸ Print Icon",
                        m ? "à¤¹à¤¾ à¤…à¤¹à¤µà¤¾à¤² à¤¤à¥à¤µà¤°à¤¿à¤¤ PDF à¤¸à¥à¤µà¤°à¥‚à¤ªà¤¾à¤¤ à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾." 
                          : "Tap this print icon to instantly generate the PDF for this report."));
                s.add(new TourStep(R.id.rvReportCards, 0,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§: à¥§. à¤®à¥à¤–à¤ªà¥ƒà¤·à¥à¤ " : "Report 1: 1. Cover Page",
                        m ? "à¤¨à¥‹à¤‚à¤¦à¤µà¤¹à¥€à¤šà¥‡ à¤†à¤•à¤°à¥à¤·à¤• à¤®à¥à¤–à¤ªà¥ƒà¤·à¥à¤ " : "Attractive cover page of register"));
                s.add(new TourStep(R.id.rvReportCards, 1,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥¨: à¥¨. à¤…à¤¨à¥à¤•à¥à¤°à¤®à¤£à¤¿à¤•à¤¾" : "Report 2: 2. Index",
                        m ? "à¤¸à¤¤à¥à¤° à¤à¤• à¤µà¤¾ à¤¦à¥‹à¤¨ à¤…à¤¨à¥à¤¸à¤¾à¤° à¤…à¤¨à¥à¤•à¥à¤°à¤®à¤£à¤¿à¤•à¤¾" : "Index according to semester 1 or 2"));
                s.add(new TourStep(R.id.rvReportCards, 2,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥©: à¥©. à¤—à¥à¤£à¤¨à¥‹à¤‚à¤¦à¥€" : "Report 3: 3. Marks Register",
                        m ? "à¤¤à¤‚à¤¤à¥à¤°à¥‡ à¤µ à¤¶à¥à¤°à¥‡à¤£à¥€à¤¸à¤¹à¤¿à¤¤ à¤†à¤•à¤¾à¤°à¤¿à¤•-à¤¸à¤‚à¤•à¤²à¤¿à¤¤ à¤—à¥à¤£à¤¨à¥‹à¤‚à¤¦à¥€" : "Formative-Summative marks register with tools & grades"));
                s.add(new TourStep(R.id.rvReportCards, 3,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥ª: à¥ª. à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¨à¥‹à¤‚à¤¦à¥€" : "Report 4: 4. Descriptive Entries",
                        m ? "à¤¸à¤¤à¥à¤°à¤¾à¤¨à¥à¤¸à¤¾à¤° à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥à¤¯à¤¾ à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¨à¥‹à¤‚à¤¦à¥€" : "Semester-wise descriptive remarks of students"));
                s.add(new TourStep(R.id.rvReportCards, 4,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥«: à¥«. à¤¶à¥à¤°à¥‡à¤£à¥€ à¤¤à¤•à¥à¤•à¤¾" : "Report 5: 5. Grade Table",
                        m ? "à¤¸à¤¤à¥à¤° à¤µ à¤µà¤¿à¤·à¤¯à¤¨à¥à¤¸à¤¾à¤° à¤—à¥à¤£ à¤µ à¤¶à¥à¤°à¥‡à¤£à¥€ à¤¤à¤•à¥à¤•à¤¾" : "Marks & grade table according to semester and subject"));
                s.add(new TourStep(R.id.rvReportCards, 5,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥¬: à¥¬. à¤¸à¤°à¥à¤µà¤¸à¤¾à¤®à¤¾à¤µà¥‡à¤¶à¤• à¤¨à¤¿à¤•à¤¾à¤²" : "Report 6: 6. Comprehensive Result",
                        m ? "à¤†à¤•à¤¾à¤°à¤¿à¤•-à¤¸à¤‚à¤•à¤²à¤¿à¤¤ à¤—à¤£ à¤¶à¥à¤°à¥‡à¤£à¥€à¤¯à¥à¤•à¥à¤¤" : "Formative-Summative total grade sheet"));
                s.add(new TourStep(R.id.rvReportCards, 6,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥­: à¥­. à¤¶à¥à¤°à¥‡à¤£à¥€ à¤¤à¤•à¥à¤•à¤¾" : "Report 7: 7. Roster Grade Table",
                        m ? "à¤¸à¤¤à¥à¤°, à¤µà¤°à¥à¤—à¤µà¤¾à¤° à¤®à¥à¤²à¥‡-à¤®à¥à¤²à¥€ à¤¶à¥à¤°à¥‡à¤£à¥€ à¤¤à¤•à¥à¤•à¤¾" : "Semester and class-wide boys-girls grade table"));
                s.add(new TourStep(R.id.rvReportCards, 7,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥®: à¥®. à¤—à¥à¤£-à¤¶à¥à¤°à¥‡à¤£à¥€à¤¯à¥à¤•à¥à¤¤ à¤¨à¤¿à¤•à¤¾à¤²à¤ªà¤¤à¥à¤°à¤•" : "Report 8: 8. Marks-Grade Ledger",
                        m ? "à¤µà¤¿à¤·à¤¯à¤µà¤¾à¤° à¤à¤•à¥‚à¤£ à¤—à¥à¤£ à¤µ à¤¶à¥à¤°à¥‡à¤£à¥€à¤¯à¥à¤•à¥à¤¤" : "Subject-wise total marks & grades sheet"));
                s.add(new TourStep(R.id.rvReportCards, 8,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥¯: à¥¯. à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¤¤à¥à¤°à¤• à¤®à¥à¤–à¤ªà¥ƒà¤·à¥à¤ " : "Report 9: 9. Progress Card Cover",
                        m ? "A4 à¤¸à¤¾à¤ˆà¤œ à¤•à¤²à¤°à¤«à¥à¤² à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¤¤à¥à¤°à¤•" : "A4 size colorful progress card cover"));
                s.add(new TourStep(R.id.rvReportCards, 9,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥¦: à¥§à¥¦. à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¤¤à¥à¤°à¤• à¤ªà¥ƒà¤·à¥à¤ " : "Report 10: 10. Progress Card Inner",
                        m ? "A4 à¤¸à¤¾à¤ˆà¤œ à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¤¤à¥à¤°à¤• à¤†à¤¤à¥€à¤² à¤ªà¥ƒà¤·à¥à¤ " : "A4 size progress card inner page"));
                s.add(new TourStep(R.id.rvReportCards, 10,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥§: à¥§à¥§. à¤‰à¤ªà¤¯à¥à¤•à¥à¤¤ à¤°à¤¿à¤ªà¥‹à¤°à¥à¤Ÿ" : "Report 11: 11. Useful Reports",
                        m ? "à¤µà¤¿à¤·à¤¯à¤µà¤¾à¤° à¤—à¥à¤£à¤¤à¤•à¥à¤•à¥‡" : "Subject-wise marks tables"));
                s.add(new TourStep(R.id.rvReportCards, 11,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥¨: à¥§à¥¨. à¤ªà¤¾à¤šà¤µà¥€ à¤†à¤ à¤µà¥€ à¤—à¥à¤£à¤ªà¤¤à¥à¤°à¤•" : "Report 12: 12. 5th & 8th Marksheet",
                        m ? "à¤‡à¤¯à¤¤à¥à¤¤à¤¾ à¤ªà¤¾à¤šà¤µà¥€ / à¤†à¤ à¤µà¥€ à¤—à¥à¤£à¤ªà¤¤à¥à¤°à¤•" : "5th / 8th standard marksheet"));
                s.add(new TourStep(R.id.rvReportCards, 12,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥©: à¥§à¥©. à¤ªà¤¾à¤šà¤µà¥€ à¤†à¤ à¤µà¥€ à¤µà¤¾à¤°à¥à¤·à¤¿à¤• à¤¤à¤•à¥à¤•à¥‡" : "Report 13: 13. 5th & 8th Annual Tables",
                        m ? "à¤‡à¤¯à¤¤à¥à¤¤à¤¾ à¤ªà¤¾à¤šà¤µà¥€ / à¤†à¤ à¤µà¥€ à¤—à¥à¤£à¤¤à¤•à¥à¤•à¤¾" : "5th / 8th standard annual tables"));
                s.add(new TourStep(R.id.rvReportCards, 13,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥ª: à¥§à¥ª. à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¤¤à¥à¤°à¤• à¤®à¥à¤–à¤ªà¥ƒà¤·à¥à¤ " : "Report 14: 14. Progress Card Cover",
                        m ? "A4 à¤¸à¤¾à¤ˆà¤œ à¤•à¤²à¤°à¤«à¥à¤² à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¤¤à¥à¤°à¤•" : "A4 size colorful progress card cover"));
                s.add(new TourStep(R.id.rvReportCards, 14,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥«: à¥§à¥«. à¤µà¤¾à¤°à¥à¤·à¤¿à¤• à¤¨à¤¿à¤•à¤¾à¤²à¤ªà¤¤à¥à¤°à¤•" : "Report 15: 15. Annual Result Ledger",
                        m ? "à¤¸à¤¤à¥à¤° à¤µ à¤µà¤¿à¤·à¤¯à¤¨à¥à¤¸à¤¾à¤° à¤—à¥à¤£ à¤µ à¤¶à¥à¤°à¥‡à¤£à¥€ à¤¤à¤•à¥à¤•à¤¾" : "Marks & grade table according to semester and subject"));
                s.add(new TourStep(R.id.rvReportCards, 15,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥¬: à¥§à¥¬. à¤µà¤¾à¤°à¥à¤·à¤¿à¤• à¤¨à¤¿à¤•à¤¾à¤²à¤ªà¤¤à¥à¤°à¤•" : "Report 16: 16. Annual Marksheet",
                        m ? "à¤¦à¥‹à¤¨à¥à¤¹à¥€ à¤¸à¤¤à¥à¤° à¤à¤•à¤¤à¥à¤° à¤¸à¤°à¥à¤µà¤¸à¤®à¤¾à¤µà¥‡à¤¶à¥€à¤¤ à¤¤à¤•à¥à¤¤à¤¾" : "Both semesters combined comprehensive table"));
                s.add(new TourStep(R.id.rvReportCards, 16,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥­: à¥§à¥­. à¤œà¤¾à¤¤ à¤¶à¥à¤°à¥‡à¤£à¥€ à¤¤à¤•à¥à¤¤à¤¾" : "Report 17: 17. Caste Grade Table",
                        m ? "à¤¸à¤¤à¥à¤°, à¤µà¤°à¥à¤—à¤µà¤¾à¤°, à¤œà¤¾à¤¤à¤µà¤¾à¤° à¤®à¥à¤²à¥‡-à¤®à¥à¤²à¥€ à¤¶à¥à¤°à¥‡à¤£à¥€ à¤¤à¤•à¥à¤¤à¤¾." : "Semester, Class-wise, Caste-wise Boys-Girls Grade Table."));
                s.add(new TourStep(R.id.rvReportCards, 17,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥®: à¥§à¥®. à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¤¤à¥à¤°à¤• à¤®à¥à¤–à¤ªà¥ƒà¤·à¥à¤ " : "Report 18: 18. Progress Card Cover",
                        m ? "A4 à¤ªà¥à¤°à¤¥à¤® à¤¸à¤¤à¥à¤° à¤ªà¥à¤°à¤—à¤¤à¥€à¤ªà¤¤à¥à¤°à¤•" : "A4 First Semester Progress Card"));
                s.add(new TourStep(R.id.rvReportCards, 18,
                        m ? "à¤…à¤¹à¤µà¤¾à¤² à¥§à¥¯: à¥§à¥¯. à¤¹à¥‹à¤²à¤¿à¤¸à¥à¤Ÿà¤¿à¤• à¤ªà¥à¤°à¥‹à¤—à¥à¤°à¥‡à¤¸ à¤•à¤¾à¤°à¥à¤¡ (HPC)" : "Report 19: 19. Holistic Progress Card (HPC)",
                        m ? "NEP 2020 360-à¤¡à¤¿à¤—à¥à¤°à¥€ à¤®à¥‚à¤²à¥à¤¯à¤®à¤¾à¤ªà¤¨ à¤…à¤¹à¤µà¤¾à¤²" : "NEP 2020 360-degree assessment report"));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 12. Teacher Profile  (8 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "profile":
                s.add(new TourStep(0,
                        m ? "ðŸ‘¤ à¤¶à¤¿à¤•à¥à¤·à¤•à¤¾à¤šà¥€ à¤ªà¥à¤°à¥‹à¤«à¤¾à¤‡à¤²" : "ðŸ‘¤ Teacher Profile",
                        m ? "à¤†à¤ªà¤²à¥€ à¤ªà¥à¤°à¥‹à¤«à¤¾à¤‡à¤², à¤¶à¤¾à¤³à¥‡à¤šà¥‡ à¤¯à¥à¤¡à¤¾à¤¯à¤¸ à¤•à¥‹à¤¡, à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤µà¤°à¥à¤— à¤µ à¤¨à¤µà¥€à¤¨ à¤µà¤°à¥à¤— à¤œà¥‹à¤¡à¤£à¥€ à¤¯à¥‡à¤¥à¥‡ à¤•à¤°à¤¾."
                          : "Manage your profile, UDISE code, active class info and add new classes here."));
                s.add(new TourStep(R.id.cardProfileInfo,
                        m ? "à¥§. à¤ªà¥à¤°à¥‹à¤«à¤¾à¤‡à¤² à¤•à¤¾à¤°à¥à¤¡" : "1. Profile Card",
                        m ? "à¤†à¤ªà¤²à¥‡ à¤¨à¤¾à¤µ, à¤ˆà¤®à¥‡à¤², à¤«à¥‹à¤¨ à¤¨à¤‚à¤¬à¤° à¤µ à¤¶à¤¾à¤³à¥‡à¤šà¤¾ à¤¯à¥à¤¡à¤¾à¤¯à¤¸ à¤•à¥‹à¤¡ à¤¯à¥‡à¤¥à¥‡ à¤à¤•à¤¾ à¤¨à¤œà¤°à¥‡à¤¤ à¤¦à¤¿à¤¸à¤¤à¥‹."
                          : "Your name, email, phone and school UDISE code shown at a glance."));
                s.add(new TourStep(R.id.btnEditProfile,
                        m ? "à¥¨. à¤ªà¥à¤°à¥‹à¤«à¤¾à¤‡à¤² à¤¸à¤‚à¤ªà¤¾à¤¦à¤¿à¤¤ à¤•à¤°à¤¾ âœï¸" : "2. Edit Profile âœï¸",
                        m ? "à¤¨à¤¾à¤µ, à¤«à¥‹à¤¨ à¤•à¤¿à¤‚à¤µà¤¾ à¤¯à¥à¤¡à¤¾à¤¯à¤¸ à¤•à¥‹à¤¡ à¤¬à¤¦à¤²à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ 'à¤¸à¤‚à¤ªà¤¾à¤¦à¤¿à¤¤ à¤•à¤°à¤¾' à¤¦à¤¾à¤¬à¤¾."
                          : "Tap Edit to update your name, phone number or UDISE code."));
                s.add(new TourStep(R.id.tvSummaryUdise,
                        m ? "à¥©. à¤¯à¥à¤¡à¤¾à¤¯à¤¸ à¤•à¥‹à¤¡" : "3. UDISE Code",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤šà¤¾ à¥§à¥§-à¤…à¤‚à¤•à¥€ à¤¯à¥à¤¡à¤¾à¤¯à¤¸ à¤•à¥‹à¤¡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‹. à¤¹à¤¾ à¤•à¥‹à¤¡ à¤…à¤šà¥‚à¤• à¤…à¤¸à¤£à¥‡ à¤†à¤µà¤¶à¥à¤¯à¤• à¤†à¤¹à¥‡."
                          : "Your school's 11-digit UDISE code appears here â€” keep it accurate."));
                s.add(new TourStep(R.id.cardActiveClassDetail,
                        m ? "à¥ª. à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤µà¤°à¥à¤— ðŸ«" : "4. Active Class ðŸ«",
                        m ? "à¤¸à¤§à¥à¤¯à¤¾ à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤•à¥‡à¤²à¥‡à¤²à¥à¤¯à¤¾ à¤µà¤°à¥à¤—à¤¾à¤šà¥€ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ â€” à¤¨à¤¾à¤µ, à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾ à¤µ à¤¤à¥à¤•à¤¡à¥€ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows your currently active class â€” name, student count and division."));
                s.add(new TourStep(R.id.tvActiveClassStudents,
                        m ? "à¥«. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾" : "5. Student Count",
                        m ? "à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤à¤•à¥‚à¤£ à¤¨à¥‹à¤‚à¤¦à¤£à¥€à¤•à¥ƒà¤¤ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Total registered students in the currently active class are shown here."));
                s.add(new TourStep(R.id.rvProfileClasses,
                        m ? "à¥¬. à¤¸à¤°à¥à¤µ à¤µà¤°à¥à¤—à¤¾à¤‚à¤šà¥€ à¤¯à¤¾à¤¦à¥€ ðŸ“‹" : "6. All Classes List ðŸ“‹",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤¤à¥€à¤² à¤¸à¤°à¥à¤µ à¤µà¤°à¥à¤—à¤¾à¤‚à¤šà¥€ à¤¯à¤¾à¤¦à¥€ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡. à¤µà¤°à¥à¤—à¤¾à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¥‡à¤²à¥à¤¯à¤¾à¤¸ à¤¤à¥‹ à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤¹à¥‹à¤¤à¥‹."
                          : "All classes in the school are listed here. Tap a class to make it active."));

                s.add(new TourStep(R.id.fabAddClass,
                        m ? "à¥­. à¤¨à¤µà¥€à¤¨ à¤µà¤°à¥à¤— à¤œà¥‹à¤¡à¤¾ +" : "7. Add New Class +",
                        m ? "'+' FAB à¤¦à¤¾à¤¬à¥‚à¤¨ à¤¨à¤µà¥€à¤¨ à¤µà¤°à¥à¤— à¤µ à¤¤à¥à¤•à¤¡à¥€ (à¤‰à¤¦à¤¾. à¥«-A) à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤¾. à¤²à¤—à¥‡à¤š à¤¯à¤¾à¤¦à¥€à¤¤ à¤¦à¤¿à¤¸à¥‡à¤²."
                          : "Tap '+' to create a new class and division (e.g. Class 5-A). It appears instantly."));
                break;


            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 13. App Settings  (7 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "settings":
                s.add(new TourStep(0,
                        m ? "âš™ï¸ à¤¸à¥‡à¤Ÿà¤¿à¤‚à¤—à¥à¤œ" : "âš™ï¸ App Settings",
                        m ? "à¤­à¤¾à¤·à¤¾, à¤¥à¥€à¤®, à¤¬à¥…à¤•à¤…à¤ª, à¤•à¥…à¤¶à¥‡ à¤µ à¤¸à¤¦à¤¸à¥à¤¯à¤¤à¤¾ à¤¸à¥‡à¤Ÿà¤¿à¤‚à¤—à¥à¤œ à¤¯à¥‡à¤¥à¥‡ à¤¬à¤¦à¤²à¤¾."
                          : "Change language, theme, backup, cache and subscription settings."));
                s.add(new TourStep(R.id.cardTheme,
                        m ? "à¥§. ðŸŒ™ à¤¥à¥€à¤®" : "1. ðŸŒ™ App Theme",
                        m ? "à¤¸à¤¿à¤¸à¥à¤Ÿà¤®, à¤²à¤¾à¤‡à¤Ÿ à¤•à¤¿à¤‚à¤µà¤¾ à¤¡à¤¾à¤°à¥à¤• à¤¥à¥€à¤® à¤¨à¤¿à¤µà¤¡à¤¾. à¤¡à¤¾à¤°à¥à¤• à¤¥à¥€à¤® à¤¡à¥‹à¤³à¥à¤¯à¤¾à¤‚à¤¨à¤¾ à¤†à¤°à¤¾à¤®à¤¦à¤¾à¤¯à¥€."
                          : "Choose System, Light or Dark theme. Dark mode is easier on the eyes."));
                s.add(new TourStep(R.id.cardLanguage,
                        m ? "à¥¨. ðŸŒ à¤­à¤¾à¤·à¤¾" : "2. ðŸŒ Language",
                        m ? "à¤®à¤°à¤¾à¤ à¥€ à¤•à¤¿à¤‚à¤µà¤¾ à¤‡à¤‚à¤—à¥à¤°à¤œà¥€ â€” à¤ªà¤¸à¤‚à¤¤à¥€à¤šà¥€ à¤­à¤¾à¤·à¤¾ à¤¨à¤¿à¤µà¤¡à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap to switch the app language between Marathi and English."));
                s.add(new TourStep(R.id.cardBackup,
                        m ? "à¥©. ðŸ’¾ à¤¬à¥…à¤•à¤…à¤ª" : "3. ðŸ’¾ Backup & Restore",
                        m ? "à¤¡à¥‡à¤Ÿà¤¾ à¤¸à¥à¤°à¤•à¥à¤·à¤¿à¤¤ à¤ à¥‡à¤µà¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¨à¤¿à¤¯à¤®à¤¿à¤¤ à¤¬à¥…à¤•à¤…à¤ª à¤˜à¥à¤¯à¤¾. à¤«à¥‹à¤¨ à¤¬à¤¦à¤²à¤¤à¤¾à¤¨à¤¾ à¤‰à¤ªà¤¯à¥à¤•à¥à¤¤."
                          : "Take regular backups to keep data safe â€” especially before changing phones."));
                s.add(new TourStep(R.id.btnExportBackup,
                        m ? "à¥ª. â¬†ï¸ à¤¬à¥…à¤•à¤…à¤ª à¤¨à¤¿à¤°à¥à¤¯à¤¾à¤¤ à¤•à¤°à¤¾" : "4. â¬†ï¸ Export Backup",
                        m ? "'à¤¨à¤¿à¤°à¥à¤¯à¤¾à¤¤' à¤¦à¤¾à¤¬à¤²à¥à¤¯à¤¾à¤µà¤° à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤¡à¥‡à¤Ÿà¤¾à¤šà¤¾ .zip à¤¬à¥…à¤•à¤…à¤ª à¤«à¤¾à¤‡à¤² à¤¤à¤¯à¤¾à¤° à¤¹à¥‹à¤¤à¥‡."
                          : "Press Export to create a complete .zip backup file of all your data."));
                s.add(new TourStep(R.id.btnImportBackup,
                        m ? "à¥«. â¬‡ï¸ à¤¬à¥…à¤•à¤…à¤ª à¤†à¤¯à¤¾à¤¤ à¤•à¤°à¤¾" : "5. â¬‡ï¸ Import Backup",
                        m ? "'à¤†à¤¯à¤¾à¤¤' à¤¦à¤¾à¤¬à¤²à¥à¤¯à¤¾à¤µà¤° à¤œà¥à¤¨à¤¾ à¤¬à¥…à¤•à¤…à¤ª à¤¨à¤¿à¤µà¤¡à¤¾ â€” à¤¡à¥‡à¤Ÿà¤¾ à¤ªà¥à¤¨à¤°à¥à¤¸à¤‚à¤šà¤¯à¤¿à¤¤ à¤¹à¥‹à¤ˆà¤²."
                          : "Press Import and select a backup file to restore all your previous data."));
                s.add(new TourStep(R.id.cardCache,
                        m ? "à¥¬. ðŸ—‘ï¸ à¤•à¥…à¤¶à¥‡ à¤¸à¤¾à¤« à¤•à¤°à¤¾" : "6. ðŸ—‘ï¸ Clear Cache",
                        m ? "à¥²à¤ªà¥à¤²à¤¿à¤•à¥‡à¤¶à¤¨ à¤®à¤‚à¤¦ à¤à¤¾à¤²à¥à¤¯à¤¾à¤¸ à¤•à¥…à¤¶à¥‡ à¤¸à¤¾à¤« à¤•à¤°à¤¾ à¤•à¤¿à¤‚à¤µà¤¾ à¤¸à¤•à¥à¤°à¤¿à¤¯ à¤¸à¤¤à¥à¤° à¤°à¥€à¤¸à¥‡à¤Ÿ à¤•à¤°à¤¾."
                          : "If the app feels slow, clear cache or reset the active session here."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 14. Student Profile  (10 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "student_profile":
                s.add(new TourStep(0,
                        m ? "ðŸ“‹ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤ªà¥à¤°à¥‹à¤«à¤¾à¤‡à¤²" : "ðŸ“‹ Student Profile",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥€ à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ â€” à¤®à¥‚à¤²à¤­à¥‚à¤¤, à¤•à¥à¤Ÿà¥à¤‚à¤¬, à¤¬à¤à¤•, à¤¶à¥ˆà¤•à¥à¤·à¤£à¤¿à¤•."
                          : "Complete student details â€” basic, family, bank and academic info."));
                s.add(new TourStep(R.id.layoutHeaderBanner,
                        m ? "à¥§. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¬à¥…à¤¨à¤°" : "1. Student Banner",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤¨à¤¾à¤µ, à¤µà¤°à¥à¤— à¤µ à¤¨à¥‹à¤‚à¤¦à¤£à¥€ à¤•à¥à¤°à¤®à¤¾à¤‚à¤• à¤¯à¥‡à¤¥à¥‡ à¤ à¤³à¤•à¤ªà¤£à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‹."
                          : "Student's name, class and registration number shown prominently."));
                s.add(new TourStep(R.id.ivStudentPhoto,
                        m ? "à¥¨. à¤«à¥‹à¤Ÿà¥‹ ðŸ“·" : "2. Photo ðŸ“·",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤«à¥‹à¤Ÿà¥‹à¤µà¤° à¤Ÿà¥…à¤ª à¤•à¤°à¥‚à¤¨ à¤¨à¤µà¥€à¤¨ à¤«à¥‹à¤Ÿà¥‹ à¤œà¥‹à¤¡à¤¾ à¤•à¤¿à¤‚à¤µà¤¾ à¤¬à¤¦à¤²à¤¾."
                          : "Tap the photo to add or change the student's profile picture."));
                s.add(new TourStep(R.id.cardStats,
                        m ? "à¥©. à¤¤à¥à¤µà¤°à¤¿à¤¤ à¤¤à¤ªà¤¶à¥€à¤²" : "3. Quick Stats",
                        m ? "à¤°à¥‹à¤² à¤¨à¤‚., à¤œà¤¨à¥à¤®à¤¤à¤¾à¤°à¥€à¤–, à¤²à¤¿à¤‚à¤— à¤µ à¤œà¤¾à¤¤ à¤ªà¥à¤°à¤µà¤°à¥à¤— à¤à¤•à¤¾ à¤¨à¤œà¤°à¥‡à¤¤ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¤¾à¤¤."
                          : "Roll number, DOB, gender and caste category shown at a glance."));
                s.add(new TourStep(R.id.btnNavBasic,
                        m ? "à¥ª. à¤®à¥‚à¤²à¤­à¥‚à¤¤ à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "4. Basic Details",
                        m ? "à¤®à¥‚à¤²à¤­à¥‚à¤¤ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ â€” à¤¨à¤¾à¤µ, à¤°à¥‹à¤² à¤¨à¤‚., à¤œà¤¨à¥à¤®à¤¤à¤¾à¤°à¥€à¤–, à¤²à¤¿à¤‚à¤—, à¤œà¤¾à¤¤ à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap to view basic info â€” name, roll no., DOB, gender and caste."));
                s.add(new TourStep(R.id.btnNavFamily,
                        m ? "à¥«. à¤•à¥à¤Ÿà¥à¤‚à¤¬ à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "5. Family Details",
                        m ? "à¤µà¤¡à¥€à¤², à¤†à¤ˆ, à¤µà¥à¤¯à¤µà¤¸à¤¾à¤¯, à¤«à¥‹à¤¨ à¤µ à¤ªà¤¤à¥à¤¤à¤¾ à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ 'à¤•à¥à¤Ÿà¥à¤‚à¤¬' à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap Family to view father, mother, occupation, phone and address."));
                s.add(new TourStep(R.id.btnNavBank,
                        m ? "à¥¬. à¤¬à¤à¤• à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "6. Bank Details",
                        m ? "à¤¬à¤à¤• à¤–à¤¾à¤¤à¥‡, à¤¶à¤¾à¤–à¤¾, IFSC à¤µ UID à¤ªà¤¾à¤¹à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ 'à¤¬à¤à¤•' à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap Bank to see account number, branch, IFSC code and UID."));
                s.add(new TourStep(R.id.btnEditStudent,
                        m ? "à¥­. à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤¸à¤‚à¤ªà¤¾à¤¦à¤¿à¤¤ à¤•à¤°à¤¾ âœï¸" : "7. Edit Student âœï¸",
                        m ? "'à¤¸à¤‚à¤ªà¤¾à¤¦à¤¿à¤¤ à¤•à¤°à¤¾' à¤¦à¤¾à¤¬à¥‚à¤¨ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥€ à¤•à¥‹à¤£à¤¤à¥€à¤¹à¥€ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤¬à¤¦à¤²à¤¾."
                          : "Press Edit to update any field of this student's profile."));
                s.add(new TourStep(R.id.btnGenerateReport,
                        m ? "à¥®. à¤µà¤¿à¤¶à¥‡à¤· à¤ªà¥à¤°à¤—à¤¤à¥€ à¤ªà¤¤à¥à¤°à¤• ðŸ“„" : "8. Special Progress Card ðŸ“„",
                        m ? "à¤µà¤¿à¤¶à¥‡à¤· à¤†à¤£à¤¿ à¤…à¤¤à¥à¤¯à¤‚à¤¤ à¤†à¤•à¤°à¥à¤·à¤• à¤¡à¤¿à¤à¤¾à¤ˆà¤¨ à¤…à¤¸à¤²à¥‡à¤²à¥‡ à¤ªà¥à¤°à¤—à¤¤à¥€ à¤ªà¤¤à¥à¤°à¤• PDF à¤¸à¥à¤µà¤°à¥‚à¤ªà¤¾à¤¤ à¤¤à¤¯à¤¾à¤° à¤µ à¤¡à¤¾à¤Šà¤¨à¤²à¥‹à¤¡ à¤•à¤°à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¯à¥‡à¤¥à¥‡ à¤Ÿà¥…à¤ª à¤•à¤°à¤¾."
                          : "Tap to generate and view a beautifully styled personalized special progress card PDF."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 15. Student Add / Edit  (10 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "student_edit":
                s.add(new TourStep(0,
                        m ? "âœï¸ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¨à¥‹à¤‚à¤¦à¤£à¥€ / à¤¸à¤‚à¤ªà¤¾à¤¦à¤¨" : "âœï¸ Student Register / Edit",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥€ à¤¨à¤µà¥€à¤¨ à¤¨à¥‹à¤‚à¤¦à¤£à¥€ à¤•à¤°à¤¾ à¤•à¤¿à¤‚à¤µà¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤®à¤¾à¤¨ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤…à¤¦à¥à¤¯à¤¯à¤¾à¤µà¤¤ à¤•à¤°à¤¾."
                          : "Register a new student or update an existing student's details."));
                s.add(new TourStep(R.id.etName,
                        m ? "à¥§. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤¨à¤¾à¤µ *" : "1. Student Name *",
                        m ? "à¤ªà¥‚à¤°à¥à¤£ à¤¨à¤¾à¤µ (à¤†à¤¡à¤¨à¤¾à¤µ à¤ªà¥à¤°à¤¥à¤®) à¤…à¤šà¥‚à¤• à¤Ÿà¤¾à¤‡à¤ª à¤•à¤°à¤¾. à¤¹à¥‡ à¤…à¤¨à¤¿à¤µà¤¾à¤°à¥à¤¯ à¤†à¤¹à¥‡."
                          : "Type full name (surname first) accurately. This is mandatory."));
                s.add(new TourStep(R.id.etRoll1,
                        m ? "à¥¨. à¤°à¥‹à¤² à¤¨à¤‚à¤¬à¤° (à¤µà¤°à¥à¤—)" : "2. Class Roll Number",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¤¾ à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤°à¥‹à¤² à¤¨à¤‚à¤¬à¤° à¤¯à¥‡à¤¥à¥‡ à¤­à¤°à¤¾."
                          : "Enter the student's roll number within the class."));
                s.add(new TourStep(R.id.etRegNo,
                        m ? "à¥©. à¤¨à¥‹à¤‚à¤¦à¤£à¥€ à¤•à¥à¤°à¤®à¤¾à¤‚à¤•" : "3. Registration Number",
                        m ? "à¤¶à¤¾à¤³à¥‡à¤šà¤¾ à¤…à¤§à¤¿à¤•à¥ƒà¤¤ à¤¨à¥‹à¤‚à¤¦à¤£à¥€ / à¤ªà¥à¤°à¤µà¥‡à¤¶ à¤•à¥à¤°à¤®à¤¾à¤‚à¤• à¤¯à¥‡à¤¥à¥‡ à¤­à¤°à¤¾."
                          : "Enter the official school registration / admission number."));
                s.add(new TourStep(R.id.etDob,
                        m ? "à¥ª. à¤œà¤¨à¥à¤®à¤¤à¤¾à¤°à¥€à¤– ðŸ“…" : "4. Date of Birth ðŸ“…",
                        m ? "à¤œà¤¨à¥à¤®à¤¤à¤¾à¤°à¥€à¤– DD/MM/YYYY à¤¸à¥à¤µà¤°à¥‚à¤ªà¤¾à¤¤ à¤­à¤°à¤¾ à¤•à¤¿à¤‚à¤µà¤¾ à¤•à¥…à¤²à¥‡à¤‚à¤¡à¤°à¤®à¤§à¥‚à¤¨ à¤¨à¤¿à¤µà¤¡à¤¾."
                          : "Enter DOB in DD/MM/YYYY format or pick from the calendar."));
                s.add(new TourStep(R.id.etGender,
                        m ? "à¥«. à¤²à¤¿à¤‚à¤— â–¼" : "5. Gender â–¼",
                        m ? "à¤¡à¥à¤°à¥‰à¤ªà¤¡à¤¾à¤‰à¤¨à¤®à¤§à¥‚à¤¨ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤²à¤¿à¤‚à¤— à¤¨à¤¿à¤µà¤¡à¤¾ â€” à¤®à¥à¤²à¤—à¤¾ / à¤®à¥à¤²à¤—à¥€ / à¤‡à¤¤à¤°."
                          : "Select gender from the dropdown â€” Boy / Girl / Other."));
                s.add(new TourStep(R.id.etCast,
                        m ? "à¥¬. à¤œà¤¾à¤¤ à¤ªà¥à¤°à¤µà¤°à¥à¤— â–¼" : "6. Caste Category â–¼",
                        m ? "à¤¡à¥à¤°à¥‰à¤ªà¤¡à¤¾à¤‰à¤¨à¤®à¤§à¥‚à¤¨ à¤œà¤¾à¤¤ à¤ªà¥à¤°à¤µà¤°à¥à¤— à¤¨à¤¿à¤µà¤¡à¤¾ â€” à¤¸à¤°à¥à¤µà¤¸à¤¾à¤§à¤¾à¤°à¤£ / OBC / SC / ST."
                          : "Select caste category from dropdown â€” General / OBC / SC / ST."));
                s.add(new TourStep(R.id.etFatherName,
                        m ? "à¥­. à¤µà¤¡à¤¿à¤²à¤¾à¤‚à¤šà¥‡ à¤¨à¤¾à¤µ" : "7. Father's Name",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤µà¤¡à¤¿à¤²à¤¾à¤‚à¤šà¥‡ à¤ªà¥‚à¤°à¥à¤£ à¤¨à¤¾à¤µ à¤¯à¥‡à¤¥à¥‡ à¤­à¤°à¤¾."
                          : "Enter the full name of the student's father here."));
                s.add(new TourStep(R.id.etMotherName,
                        m ? "à¥®. à¤†à¤ˆà¤šà¥‡ à¤¨à¤¾à¤µ" : "8. Mother's Name",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤†à¤ˆà¤šà¥‡ à¤ªà¥‚à¤°à¥à¤£ à¤¨à¤¾à¤µ à¤¯à¥‡à¤¥à¥‡ à¤­à¤°à¤¾."
                          : "Enter the full name of the student's mother here."));
                s.add(new TourStep(R.id.btnSaveStudent,
                        m ? "à¥¯. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤œà¤¤à¤¨ à¤•à¤°à¤¾ ðŸ’¾" : "9. Save Student ðŸ’¾",
                        m ? "à¤¸à¤°à¥à¤µ à¤…à¤¨à¤¿à¤µà¤¾à¤°à¥à¤¯ à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤­à¤°à¤²à¥à¤¯à¤¾à¤µà¤° 'à¤œà¤¤à¤¨ à¤•à¤°à¤¾' à¤¦à¤¾à¤¬à¤¾ â€” à¤¡à¥‡à¤Ÿà¤¾ à¤¸à¥‡à¤µà¥à¤¹ à¤¹à¥‹à¤ˆà¤²."
                          : "After filling all required fields, press Save to store student data."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 16. Enter Marks  (6 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "enter_marks":
                s.add(new TourStep(0,
                        m ? "ðŸŽ¯ à¤—à¥à¤£ à¤ªà¥à¤°à¤µà¤¿à¤·à¥à¤Ÿà¥€" : "ðŸŽ¯ Enter Marks",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤µà¤¿à¤·à¤¯à¤¨à¤¿à¤¹à¤¾à¤¯ à¤†à¤•à¤¾à¤°à¤¿à¤• à¤µ à¤¸à¤‚à¤•à¤²à¤¿à¤¤ à¤—à¥à¤£ à¤¯à¤¾ à¤¸à¥à¤•à¥à¤°à¥€à¤¨à¤µà¤° à¤­à¤°à¤¾."
                          : "Enter this student's subject-wise formative and summative marks."));
                s.add(new TourStep(R.id.cardStudentAvatarMarks,
                        m ? "à¥§. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Student Info",
                        m ? "à¤—à¥à¤£ à¤­à¤°à¤¤ à¤…à¤¸à¤²à¥‡à¤²à¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤¨à¤¾à¤µ, à¤°à¥‹à¤² à¤¨à¤‚à¤¬à¤° à¤µ à¤µà¤°à¥à¤— à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‹."
                          : "Shows the name, roll number and class of the student you are marking."));
                s.add(new TourStep(R.id.btnScanMarksheet,
                        m ? "à¥¨. à¤—à¥à¤£à¤ªà¤¤à¥à¤°à¤¿à¤•à¤¾ à¤¸à¥à¤•à¥…à¤¨ ðŸ“¸" : "2. Scan Marksheet ðŸ“¸",
                        m ? "à¤•à¤¾à¤—à¤¦à¥€ à¤—à¥à¤£à¤ªà¤¤à¥à¤°à¤¿à¤•à¥‡à¤šà¤¾ à¤«à¥‹à¤Ÿà¥‹ à¤•à¤¾à¤¢à¤¾ â€” OCR à¤¦à¥à¤µà¤¾à¤°à¥‡ à¤—à¥à¤£ à¤†à¤ªà¥‹à¤†à¤ª à¤­à¤°à¤²à¥‡ à¤œà¤¾à¤¤à¤¾à¤¤."
                          : "Photograph a printed marksheet â€” OCR auto-fills all subject marks."));
                s.add(new TourStep(R.id.cardMarksTable,
                        m ? "à¥©. à¤—à¥à¤£ à¤¤à¤•à¥à¤¤à¤¾ ðŸ“Š" : "3. Marks Table ðŸ“Š",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤·à¤¯à¤¾à¤šà¥à¤¯à¤¾ FA à¤µ SA à¤‰à¤ª-à¤˜à¤Ÿà¤•à¤¾à¤‚à¤šà¥‡ à¤—à¥à¤£ à¤¯à¥‡à¤¥à¥‡ à¤¹à¤¾à¤¤à¤¾à¤¨à¥‡ à¤­à¤°à¤¾."
                          : "Manually enter FA and SA sub-component marks for each subject."));
                s.add(new TourStep(R.id.btnSaveMarks,
                        m ? "à¥ª. à¤—à¥à¤£ à¤œà¤¤à¤¨ à¤•à¤°à¤¾ ðŸ’¾" : "4. Save Marks ðŸ’¾",
                        m ? "à¤¸à¤°à¥à¤µ à¤—à¥à¤£ à¤­à¤°à¤²à¥à¤¯à¤¾à¤µà¤° 'à¤œà¤¤à¤¨ à¤•à¤°à¤¾' à¤¦à¤¾à¤¬à¤¾. à¤¬à¤¦à¤² à¤•à¥à¤²à¤¾à¤‰à¤¡à¤µà¤° à¤¸à¥‡à¤µà¥à¤¹ à¤¹à¥‹à¤¤à¤¾à¤¤."
                          : "Press Save Marks â€” all entered data is saved to the cloud."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 17. Enter Descriptive Remarks (per student)  (5 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "enter_descriptive":
                s.add(new TourStep(0,
                        m ? "ðŸ’¬ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¶à¥‡à¤°à¥‡" : "ðŸ’¬ Student Descriptive Remarks",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤µà¤°à¥à¤¤à¤¨, à¤†à¤µà¤¡ à¤µ à¤•à¥Œà¤¶à¤²à¥à¤¯à¤¾à¤‚à¤šà¥‡ à¤¶à¥‡à¤°à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¨à¤¿à¤µà¤¡à¤¾."
                          : "Select behavioral remarks and skill notes for this student."));
                s.add(new TourStep(R.id.cardStudentAvatarMarks,
                        m ? "à¥§. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Student Info",
                        m ? "à¤¶à¥‡à¤°à¥‡ à¤¨à¥‹à¤‚à¤¦à¤µà¤¤ à¤…à¤¸à¤²à¥‡à¤²à¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤¨à¤¾à¤µ, à¤°à¥‹à¤² à¤¨à¤‚à¤¬à¤° à¤µ à¤µà¤°à¥à¤— à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Displays the name, roll number and class of the student being edited."));
                s.add(new TourStep(R.id.llRemarkRows,
                        m ? "à¥¨. à¤¶à¥‡à¤°à¤¾ à¤ªà¤°à¥à¤¯à¤¾à¤¯ ðŸ’­" : "2. Remark Options ðŸ’­",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤·à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¶à¥‡à¤°à¤¾ à¤ªà¤°à¥à¤¯à¤¾à¤¯ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¤¾à¤¤. à¤šà¤¿à¤ª à¤¨à¤¿à¤µà¤¡à¤¾ â†’ à¤¶à¥‡à¤°à¤¾ à¤œà¥‹à¤¡à¤²à¤¾ à¤œà¤¾à¤¤à¥‹."
                          : "Remark chips are shown per subject. Tap a chip to select that remark."));
                s.add(new TourStep(R.id.btnSaveRemarks,
                        m ? "à¥©. à¤¶à¥‡à¤°à¥‡ à¤œà¤¤à¤¨ à¤•à¤°à¤¾ ðŸ’¾" : "3. Save Remarks ðŸ’¾",
                        m ? "à¤¸à¤°à¥à¤µ à¤ªà¤¸à¤‚à¤¤à¥€à¤šà¥‡ à¤¶à¥‡à¤°à¥‡ à¤¨à¤¿à¤µà¤¡à¤²à¥à¤¯à¤¾à¤µà¤° 'à¤œà¤¤à¤¨ à¤•à¤°à¤¾' à¤¦à¤¾à¤¬à¥‚à¤¨ à¤•à¥à¤²à¤¾à¤‰à¤¡à¤µà¤° à¤¸à¥‡à¤µà¥à¤¹ à¤•à¤°à¤¾."
                          : "After selecting all preferred remarks, press Save to store them."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // Fallback
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "promote_students":
                s.add(new TourStep(0,
                        m ? "ðŸŽ“ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤µà¤°à¥à¤—à¥‹à¤¨à¥à¤¨à¤¤à¥€ à¤†à¤£à¤¿ à¤¬à¤¦à¤²à¥€" : "ðŸŽ“ Student Promotion & Transfer",
                        m ? "à¤¯à¥‡à¤¥à¥‡ à¤¤à¥à¤®à¥à¤¹à¥€ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤¨à¤¾ à¤ªà¥à¤¢à¥€à¤² à¤µà¤°à¥à¤—à¤¾à¤¤ à¤µà¤°à¥à¤—à¥‹à¤¨à¥à¤¨à¤¤à¥€ à¤¦à¥‡à¤Š à¤¶à¤•à¤¤à¤¾ à¤•à¤¿à¤‚à¤µà¤¾ à¤¤à¥à¤•à¤¡à¥€ à¤¬à¤¦à¤²à¥‚ à¤¶à¤•à¤¤à¤¾."
                          : "Batch promote students into next classes or transfer divisions easily here."));
                s.add(new TourStep(R.id.spTargetYear,
                        m ? "à¥§. à¤¶à¥ˆà¤•à¥à¤·à¤£à¤¿à¤• à¤µà¤°à¥à¤· à¤¨à¤¿à¤µà¤¡à¤¾" : "1. Select Target Year",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤œà¥à¤¯à¤¾ à¤¨à¤µà¥€à¤¨ à¤¶à¥ˆà¤•à¥à¤·à¤£à¤¿à¤• à¤µà¤°à¥à¤·à¤¾à¤¤ à¤ªà¥à¤°à¤®à¥‹à¤Ÿ à¤•à¤°à¤¾à¤¯à¤šà¥‡ à¤†à¤¹à¥‡à¤¤ à¤¤à¥‡ à¤µà¤°à¥à¤· à¤¨à¤¿à¤µà¤¡à¤¾."
                          : "Select the new target academic year for the promotion."));
                s.add(new TourStep(R.id.btnAddNewYear,
                        m ? "à¥¨. à¤¨à¤µà¥€à¤¨ à¤µà¤°à¥à¤· à¤œà¥‹à¤¡à¤¾ +" : "2. Add New Year +",
                        m ? "à¤¨à¤µà¥€à¤¨ à¤µà¤°à¥à¤· à¤‰à¤ªà¤²à¤¬à¥à¤§ à¤¨à¤¸à¥‡à¤² à¤¤à¤° à¤¯à¥‡à¤¥à¥‡ à¤•à¥à¤²à¤¿à¤• à¤•à¤°à¥‚à¤¨ à¤¤à¥‡ à¤µà¤°à¥à¤· à¤µ à¤¸à¤¤à¥à¤°à¥‡ à¤¤à¥à¤µà¤°à¤¿à¤¤ à¤¤à¤¯à¤¾à¤° à¤•à¤°à¤¾."
                          : "Tap here to register a new academic year and auto-generate its semesters."));
                s.add(new TourStep(R.id.spTargetClass,
                        m ? "à¥©. à¤¨à¤µà¥€à¤¨ à¤‡à¤¯à¤¤à¥à¤¤à¤¾ à¤¨à¤¿à¤µà¤¡à¤¾" : "3. Select Target Class",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤¨à¤¾ à¤œà¥à¤¯à¤¾ à¤¨à¤µà¥€à¤¨ à¤‡à¤¯à¤¤à¥à¤¤à¥‡à¤¤ à¤ªà¤¾à¤ à¤µà¤¾à¤¯à¤šà¥‡ à¤†à¤¹à¥‡ à¤¤à¥€ à¤¨à¤¿à¤µà¤¡à¤¾ (à¤‰à¤¦à¤¾. à¥­ à¤µà¤°à¥‚à¤¨ à¥®)."
                          : "Select the target class standard for the students."));
                s.add(new TourStep(R.id.spTargetDivision,
                        m ? "à¥ª. à¤¨à¤µà¥€à¤¨ à¤¤à¥à¤•à¤¡à¥€ à¤¨à¤¿à¤µà¤¡à¤¾" : "4. Select Target Division",
                        m ? "à¤¹à¤µà¥€ à¤…à¤¸à¤£à¤¾à¤°à¥€ à¤¨à¤µà¥€à¤¨ à¤¤à¥à¤•à¤¡à¥€ à¤¨à¤¿à¤µà¤¡à¤¾."
                          : "Select the target division for the students."));
                s.add(new TourStep(R.id.rgMode,
                        m ? "à¥«. à¤®à¥‹à¤¡ à¤¨à¤¿à¤µà¤¡à¤¾ (Promote/Transfer)" : "5. Choose Adjustment Mode",
                        m ? "à¤¨à¤µà¥€à¤¨ à¤µà¤°à¥à¤·à¤¾à¤¸à¤¾à¤ à¥€ 'Promote' (à¤•à¥‰à¤ªà¥€) à¤†à¤£à¤¿ à¤šà¤¾à¤²à¥‚ à¤µà¤°à¥à¤·à¤¾à¤¤à¥€à¤² à¤¤à¥à¤•à¤¡à¥€ à¤¬à¤¦à¤²à¤¾à¤¸à¤¾à¤ à¥€ 'Transfer' (à¤®à¥‚à¤µà¥à¤¹) à¤¨à¤¿à¤µà¤¡à¤¾."
                          : "Choose 'Promote' to copy student documents for a new year, or 'Transfer' to move current pointers."));
                s.add(new TourStep(R.id.cbSelectAll,
                        m ? "à¥¬. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¨à¤¿à¤µà¤¡à¤¾" : "6. Select Students",
                        m ? "à¤œà¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤¨à¤¾ à¤ªà¥à¤°à¤®à¥‹à¤Ÿ à¤•à¤°à¤¾à¤¯à¤šà¥‡ à¤†à¤¹à¥‡ à¤¤à¥à¤¯à¤¾à¤‚à¤¨à¤¾ à¤šà¥‡à¤• à¤•à¤°à¤¾. à¤¸à¤°à¥à¤µ à¤¨à¤¿à¤µà¤¡à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ 'Select All' à¤µà¤¾à¤ªà¤°à¤¾."
                          : "Check the checkboxes for students to promote, or check 'Select All'."));
                s.add(new TourStep(R.id.btnProcessPromotion,
                        m ? "à¥­. à¤ªà¥à¤°à¤•à¥à¤°à¤¿à¤¯à¤¾ à¤ªà¥‚à¤°à¥à¤£ à¤•à¤°à¤¾" : "7. Execute Adjustment",
                        m ? "à¤¨à¤¿à¤µà¤¡à¤²à¥‡à¤²à¥‡ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¨à¤µà¥€à¤¨ à¤µà¤°à¥à¤—à¤¾à¤¤ à¤ªà¤¾à¤ à¤µà¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤¹à¥‡ à¤¬à¤Ÿà¤£ à¤¦à¤¾à¤¬à¤¾."
                          : "Press this button to commit the promotion or transfer process."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 18. Attendance Report (8 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "attendance_report":
                s.add(new TourStep(0,
                        m ? "ðŸ“Š à¤¹à¤œà¥‡à¤°à¥€ à¤…à¤¹à¤µà¤¾à¤²" : "ðŸ“Š Attendance Report",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥à¤¯à¤¾ à¤®à¤¾à¤¸à¤¿à¤• à¤†à¤£à¤¿ à¤µà¤¾à¤°à¥à¤·à¤¿à¤• à¤‰à¤ªà¤¸à¥à¤¥à¤¿à¤¤à¥€à¤šà¥‡ à¤µà¤¿à¤¶à¥à¤²à¥‡à¤·à¤£ à¤¯à¥‡à¤¥à¥‡ à¤ªà¤¹à¤¾."
                          : "View detailed monthly and yearly class attendance statistics."));
                s.add(new TourStep(R.id.tvReportContext,
                        m ? "à¥§. à¤¸à¤¤à¥à¤° à¤µ à¤µà¤°à¥à¤— à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Active Session",
                        m ? "à¤¹à¤¾ à¤…à¤¹à¤µà¤¾à¤² à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤¶à¥ˆà¤•à¥à¤·à¤£à¤¿à¤• à¤µà¤°à¥à¤· à¤†à¤£à¤¿ à¤‡à¤¯à¤¤à¥à¤¤à¥‡à¤¸à¤¾à¤ à¥€ à¤†à¤¹à¥‡ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Confirms the year, standard and division of the report data."));
                s.add(new TourStep(R.id.tvTotalStudents,
                        m ? "à¥¨. à¤à¤•à¥‚à¤£ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€" : "2. Total Students",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤ à¤à¤•à¥‚à¤£ à¤•à¤¿à¤¤à¥€ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤†à¤¹à¥‡à¤¤ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Displays total number of students enrolled in the class."));
                s.add(new TourStep(R.id.tvAvgAttendance,
                        m ? "à¥©. à¤¸à¤°à¤¾à¤¸à¤°à¥€ à¤‰à¤ªà¤¸à¥à¤¥à¤¿à¤¤à¥€" : "3. Avg Attendance",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤¸à¤°à¥à¤µ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥€ à¤à¤•à¥‚à¤£ à¤¸à¤°à¤¾à¤¸à¤°à¥€ à¤‰à¤ªà¤¸à¥à¤¥à¤¿à¤¤à¥€ à¤Ÿà¤•à¥à¤•à¥‡à¤µà¤¾à¤°à¥€ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the overall average attendance percentage of the entire class."));
                s.add(new TourStep(R.id.tvTotalPresent,
                        m ? "à¥ª. à¤à¤•à¥‚à¤£ à¤¹à¤œà¥‡à¤°à¥€ à¤¦à¤¿à¤µà¤¸" : "4. Total Present Days",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤¸à¤°à¥à¤µ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥à¤¯à¤¾ à¤à¤•à¥‚à¤£ à¤¹à¤œà¤° à¤¦à¤¿à¤µà¤¸à¤¾à¤‚à¤šà¥€ à¤¬à¥‡à¤°à¥€à¤œ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Sum of all present days of all students combined."));
                s.add(new TourStep(R.id.tvTotalWorking,
                        m ? "à¥«. à¤à¤•à¥‚à¤£ à¤•à¤¾à¤®à¤•à¤¾à¤œà¤¾à¤šà¥‡ à¤¦à¤¿à¤µà¤¸" : "5. Total Working Days",
                        m ? "à¤¸à¤§à¥à¤¯à¤¾à¤šà¥à¤¯à¤¾ à¤¸à¤¤à¥à¤°à¤¾à¤¤à¥€à¤² à¤à¤•à¥‚à¤£ à¤•à¤¾à¤®à¤•à¤¾à¤œà¤¾à¤šà¥à¤¯à¤¾ à¤¦à¤¿à¤µà¤¸à¤¾à¤‚à¤šà¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Sum of all working/school days in the selected period."));
                s.add(new TourStep(R.id.tvBestAttender,
                        m ? "à¥¬. à¤¸à¤°à¥à¤µà¤¾à¤§à¤¿à¤• à¤‰à¤ªà¤¸à¥à¤¥à¤¿à¤¤à¥€ à¤…à¤¸à¤²à¥‡à¤²à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€" : "6. Best Attending Student",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤œà¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥€ à¤‰à¤ªà¤¸à¥à¤¥à¤¿à¤¤à¥€ à¤Ÿà¤•à¥à¤•à¥‡à¤µà¤¾à¤°à¥€ à¤¸à¤°à¥à¤µà¤¾à¤§à¤¿à¤• à¤†à¤¹à¥‡ à¤¤à¥à¤¯à¤¾à¤šà¥‡ à¤¨à¤¾à¤µ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Highlights the student with the highest attendance percentage."));
                s.add(new TourStep(R.id.rvReportStudents,
                        m ? "à¥­. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¯à¤¾à¤¦à¥€ à¤…à¤¹à¤µà¤¾à¤²" : "7. Student Roster",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¤¾ à¤°à¥‹à¤² à¤¨à¤‚à¤¬à¤°, à¤¨à¤¾à¤µ, à¤à¤•à¥‚à¤£ à¤¹à¤œà¤°/à¤•à¤¾à¤®à¤•à¤¾à¤œà¤¾à¤šà¥‡ à¤¦à¤¿à¤µà¤¸ à¤†à¤£à¤¿ à¤‰à¤ªà¤¸à¥à¤¥à¤¿à¤¤à¥€ à¤Ÿà¤•à¥à¤•à¥‡à¤µà¤¾à¤°à¥€ à¤‰à¤¤à¤°à¤¤à¥à¤¯à¤¾ à¤•à¥à¤°à¤®à¤¾à¤¨à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Displays each student's name, roll number, present days, working days, and attendance percentage. Sorted from highest to lowest."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 19. Evaluation Report / Marks Report (8 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "evaluation_report":
                s.add(new TourStep(0,
                        m ? "ðŸ“Š à¤®à¥‚à¤²à¥à¤¯à¤®à¤¾à¤ªà¤¨ à¤…à¤¹à¤µà¤¾à¤²" : "ðŸ“Š Evaluation Report",
                        m ? "à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥‡ à¤µà¤¿à¤·à¤¯à¤¨à¤¿à¤¹à¤¾à¤¯ à¤à¤•à¥‚à¤£ à¤—à¥à¤£, à¤¸à¤°à¤¾à¤¸à¤°à¥€ à¤¶à¥à¤°à¥‡à¤£à¥€ à¤†à¤£à¤¿ à¤¨à¤¿à¤•à¤¾à¤² à¤¸à¤¦à¥à¤¯à¤¸à¥à¤¥à¤¿à¤¤à¥€ à¤¯à¥‡à¤¥à¥‡ à¤ªà¤¹à¤¾."
                          : "View subject-wise overall marks, average grade, pass/fail status, and top scorers."));
                s.add(new TourStep(R.id.tvReportContext,
                        m ? "à¥§. à¤¸à¤¤à¥à¤° à¤µ à¤µà¤°à¥à¤— à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Active Session",
                        m ? "à¤¹à¤¾ à¤…à¤¹à¤µà¤¾à¤² à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤¶à¥ˆà¤•à¥à¤·à¤£à¤¿à¤• à¤µà¤°à¥à¤· à¤†à¤£à¤¿ à¤‡à¤¯à¤¤à¥à¤¤à¥‡à¤¸à¤¾à¤ à¥€ à¤†à¤¹à¥‡ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Confirms the academic year, standard and division."));
                s.add(new TourStep(R.id.tvTotalStudents,
                        m ? "à¥¨. à¤à¤•à¥‚à¤£ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€" : "2. Total Students",
                        m ? "à¤ªà¤°à¥€à¤•à¥à¤·à¥‡à¤²à¤¾ à¤¬à¤¸à¤²à¥‡à¤²à¥à¤¯à¤¾ à¤à¤•à¥‚à¤£ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Displays total number of students who appeared for exams."));
                s.add(new TourStep(R.id.tvAvgGrade,
                        m ? "à¥©. à¤¸à¤°à¤¾à¤¸à¤°à¥€ à¤¶à¥à¤°à¥‡à¤£à¥€ (Grade)" : "3. Class Average Grade",
                        m ? "à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤µà¤°à¥à¤—à¤¾à¤šà¥€ à¤¸à¤°à¤¾à¤¸à¤°à¥€ à¤Ÿà¤•à¥à¤•à¥‡à¤µà¤¾à¤°à¥€ à¤†à¤£à¤¿ à¤¤à¥à¤¯à¤¾à¤¨à¥à¤¸à¤¾à¤° à¤®à¤¿à¤³à¤£à¤¾à¤°à¥€ à¤¸à¤°à¤¾à¤¸à¤°à¥€ à¤¶à¥à¤°à¥‡à¤£à¥€ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the overall average grade and percentage of the entire class."));
                s.add(new TourStep(R.id.tvTotalPass,
                        m ? "à¥ª. à¤‰à¤¤à¥à¤¤à¥€à¤°à¥à¤£ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾" : "4. Passed Students",
                        m ? "à¤¸à¤°à¥à¤µ à¤ªà¤°à¥€à¤•à¥à¤·à¤¾à¤‚à¤®à¤§à¥à¤¯à¥‡ à¤‰à¤¤à¥à¤¤à¥€à¤°à¥à¤£ à¤à¤¾à¤²à¥‡à¤²à¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥€ à¤à¤•à¥‚à¤£ à¤¸à¤‚à¤–à¥à¤¯à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the count of students who cleared all evaluations."));
                s.add(new TourStep(R.id.tvTotalFail,
                        m ? "à¥«. à¤¸à¥à¤§à¤¾à¤°à¤£à¤¾ à¤†à¤µà¤¶à¥à¤¯à¤• à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€" : "5. Needs Improvement",
                        m ? "à¤œà¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥€ à¤ªà¥à¤°à¤—à¤¤à¥€ à¤…à¤¸à¤®à¤¾à¤§à¤¾à¤¨à¤•à¤¾à¤°à¤• à¤†à¤¹à¥‡ à¤¤à¥à¤¯à¤¾à¤‚à¤šà¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the count of students who need additional improvement."));
                s.add(new TourStep(R.id.tvTopScorer,
                        m ? "à¥¬. à¤ªà¥à¤°à¤¥à¤® à¤•à¥à¤°à¤®à¤¾à¤‚à¤•à¤¾à¤šà¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€" : "6. Top Scorer",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤®à¤§à¥à¤¯à¥‡ à¤à¤•à¥‚à¤£ à¤—à¥à¤£à¤¾à¤‚à¤®à¤§à¥à¤¯à¥‡ à¤¸à¤°à¥à¤µà¤¾à¤§à¤¿à¤• à¤Ÿà¤•à¥à¤•à¥‡à¤µà¤¾à¤°à¥€ à¤®à¤¿à¤³à¤µà¤¿à¤²à¥‡à¤²à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‹."
                          : "Highlights the student with the highest overall percentage."));
                s.add(new TourStep(R.id.rvReportStudents,
                        m ? "à¥­. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤…à¤¹à¤µà¤¾à¤² à¤¯à¤¾à¤¦à¥€" : "7. Student Marks Roster",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤à¤•à¥‚à¤£ à¤®à¤¿à¤³à¤¾à¤²à¥‡à¤²à¥‡ à¤—à¥à¤£, à¤à¤•à¥‚à¤£ à¤•à¤®à¤¾à¤² à¤—à¥à¤£, à¤Ÿà¤•à¥à¤•à¥‡à¤µà¤¾à¤°à¥€ à¤µ à¤¶à¥à¤°à¥‡à¤£à¥€ à¤‰à¤¤à¤°à¤¤à¥à¤¯à¤¾ à¤•à¥à¤°à¤®à¤¾à¤¨à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Lists students with their individual total marks, out-of marks, grade, and percentage. Sorted from highest to lowest."));
                break;

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 20. Descriptive Remarks Report (8 steps)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            case "descriptive_report":
                s.add(new TourStep(0,
                        m ? "ðŸ“Š à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¨à¥‹à¤‚à¤¦à¥€ à¤…à¤¹à¤µà¤¾à¤²" : "ðŸ“Š Descriptive Remarks Report",
                        m ? "à¤µà¤¿à¤µà¤¿à¤§ à¤µà¤¿à¤·à¤¯à¤¾à¤‚à¤®à¤§à¥€à¤² à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥à¤¯à¤¾ à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¨à¥‹à¤‚à¤¦à¥€ à¤ªà¥‚à¤°à¥à¤£ à¤à¤¾à¤²à¥à¤¯à¤¾à¤šà¥€ à¤¸à¤¦à¥à¤¯à¤¸à¥à¤¥à¤¿à¤¤à¥€ à¤¯à¥‡à¤¥à¥‡ à¤¤à¤ªà¤¾à¤¸à¤¾."
                          : "Track completion progress of qualitative remarks and descriptors across subjects."));
                s.add(new TourStep(R.id.tvReportContext,
                        m ? "à¥§. à¤¸à¤¤à¥à¤° à¤µ à¤µà¤°à¥à¤— à¤®à¤¾à¤¹à¤¿à¤¤à¥€" : "1. Active Session",
                        m ? "à¤¹à¤¾ à¤…à¤¹à¤µà¤¾à¤² à¤•à¥‹à¤£à¤¤à¥à¤¯à¤¾ à¤¶à¥ˆà¤•à¥à¤·à¤£à¤¿à¤• à¤µà¤°à¥à¤· à¤†à¤£à¤¿ à¤‡à¤¯à¤¤à¥à¤¤à¥‡à¤¸à¤¾à¤ à¥€ à¤†à¤¹à¥‡ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Confirms academic year, standard and division."));
                s.add(new TourStep(R.id.tvTotalStudents,
                        m ? "à¥¨. à¤à¤•à¥‚à¤£ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€" : "2. Total Students",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤à¤•à¥‚à¤£ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤šà¥€ à¤¸à¤‚à¤–à¥à¤¯à¤¾ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Displays total number of students in the class."));
                s.add(new TourStep(R.id.tvTotalSubjects,
                        m ? "à¥©. à¤à¤•à¥‚à¤£ à¤®à¥‚à¤²à¥à¤¯à¤®à¤¾à¤ªà¤¨ à¤˜à¤Ÿà¤•" : "3. Tracked Attributes",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¸à¤¾à¤ à¥€ à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¨à¥‹à¤‚à¤¦à¥€ à¤­à¤°à¤¾à¤¯à¤šà¥‡ à¤à¤•à¥‚à¤£ à¤µà¤¿à¤·à¤¯ à¤†à¤£à¤¿ à¤‡à¤¤à¤° à¤—à¥à¤£à¤µà¥ˆà¤¶à¤¿à¤·à¥à¤Ÿà¥à¤¯à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¤¾à¤¤."
                          : "Shows the total number of subjects and attributes tracked for remarks."));
                s.add(new TourStep(R.id.tvRemarksFilled,
                        m ? "à¥ª. à¤à¤•à¥‚à¤£ à¤­à¤°à¤²à¥‡à¤²à¥‡ à¤¶à¥‡à¤°à¥‡" : "4. Remarks Filled",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤¸à¤°à¥à¤µ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤‚à¤¸à¤¾à¤ à¥€ à¤…à¤¬à¤¤à¤ªà¤°à¥à¤¯à¤‚à¤¤ à¤­à¤°à¤²à¥‡à¤²à¥à¤¯à¤¾ à¤à¤•à¥‚à¤£ à¤¶à¥‡à¤±à¥à¤¯à¤¾à¤‚à¤šà¥€ à¤¬à¥‡à¤°à¥€à¤œ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the total count of remarks successfully entered across the class."));
                s.add(new TourStep(R.id.tvCompletion,
                        m ? "à¥«. à¤ªà¥‚à¤°à¥à¤£à¤¤à¥à¤µ à¤Ÿà¤•à¥à¤•à¥‡à¤µà¤¾à¤°à¥€" : "5. Completion Percentage",
                        m ? "à¤¸à¤‚à¤ªà¥‚à¤°à¥à¤£ à¤µà¤°à¥à¤—à¤¾à¤šà¥‡ à¤µà¤°à¥à¤£à¤¨à¤¾à¤¤à¥à¤®à¤• à¤¶à¥‡à¤°à¥‡ à¤­à¤°à¤£à¥‡ à¤•à¤¿à¤¤à¥€ à¤Ÿà¤•à¥à¤•à¥‡ à¤ªà¥‚à¤°à¥à¤£ à¤à¤¾à¤²à¥‡ à¤†à¤¹à¥‡ à¤¤à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Shows the completion progress of remarks for the entire class."));
                s.add(new TourStep(R.id.tvTopStudent,
                        m ? "à¥¬. à¤¸à¤°à¥à¤µà¤¾à¤§à¤¿à¤• à¤¨à¥‹à¤‚à¤¦à¥€ à¤…à¤¸à¤²à¥‡à¤²à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€" : "6. Most Complete Profile",
                        m ? "à¤µà¤°à¥à¤—à¤¾à¤¤à¥€à¤² à¤œà¥à¤¯à¤¾ à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤¸à¤°à¥à¤µà¤¾à¤§à¤¿à¤• à¤µà¤¿à¤·à¤¯à¤¾à¤‚à¤šà¥‡ à¤¶à¥‡à¤°à¥‡ à¤­à¤°à¥‚à¤¨ à¤ªà¥‚à¤°à¥à¤£ à¤à¤¾à¤²à¥‡ à¤†à¤¹à¥‡à¤¤, à¤¤à¥à¤¯à¤¾à¤šà¥‡ à¤¨à¤¾à¤µ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Highlights the student with the most complete descriptive remarks profile."));
                s.add(new TourStep(R.id.rvReportStudents,
                        m ? "à¥­. à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥€ à¤¶à¥‡à¤°à¤¾ à¤¸à¤¦à¥à¤¯à¤¸à¥à¤¥à¤¿à¤¤à¥€" : "7. Completion Roster",
                        m ? "à¤ªà¥à¤°à¤¤à¥à¤¯à¥‡à¤• à¤µà¤¿à¤¦à¥à¤¯à¤¾à¤°à¥à¤¥à¥à¤¯à¤¾à¤šà¥‡ à¤à¤•à¥‚à¤£ à¤˜à¤Ÿà¤•à¤¾à¤‚à¤ªà¥ˆà¤•à¥€ à¤•à¤¿à¤¤à¥€ à¤µà¤¿à¤·à¤¯à¤¾à¤‚à¤šà¥‡ à¤¶à¥‡à¤°à¥‡ à¤­à¤°à¤²à¥‡ à¤†à¤¹à¥‡à¤¤ (à¤‰à¤¦à¤¾. à¥­/à¥§à¥ª) à¤¤à¥‡ à¤Ÿà¤•à¥à¤•à¥‡à¤µà¤¾à¤°à¥€à¤¸à¤¹ à¤‰à¤¤à¤°à¤¤à¥à¤¯à¤¾ à¤•à¥à¤°à¤®à¤¾à¤¨à¥‡ à¤¯à¥‡à¤¥à¥‡ à¤¦à¤¿à¤¸à¤¤à¥‡."
                          : "Lists each student showing how many remarks are completed out of total attributes. Sorted by completion percentage."));
                break;

            default:
                s.add(new TourStep(0,
                        m ? "â„¹ï¸ à¤¯à¤¾ à¤ªà¤¾à¤¨à¤¾à¤¬à¤¦à¥à¤¦à¤²" : "â„¹ï¸ About This Page",
                        m ? "à¤¯à¤¾ à¤ªà¤¾à¤¨à¤¾à¤µà¤°à¥€à¤² à¤®à¤¾à¤¹à¤¿à¤¤à¥€ à¤µà¥à¤¯à¤µà¤¸à¥à¤¥à¤¾à¤ªà¤¿à¤¤ à¤•à¤°à¤£à¥à¤¯à¤¾à¤¸à¤¾à¤ à¥€ à¤µà¤°à¥€à¤² à¤¬à¤Ÿà¤£à¥‡ à¤µà¤¾à¤ªà¤°à¤¾."
                          : "Use the buttons above to manage the features shown on this page."));
                break;
        }
        return s;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  TourOverlayView  â€” custom FrameLayout drawn over entire screen
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    static class TourOverlayView extends FrameLayout {

        private static final int OVERLAY_COLOR  = 0xCC08091E;   // rich dark navy
        private static final int RING_COLOR     = 0xFFFFC107;   // amber/gold ring
        private static final int RIPPLE_COLOR   = 0x55FFC107;   // semi-transparent amber
        private static final int CARD_BG        = 0xFFFFFFFF;
        private static final int CARD_ACCENT    = 0xFF5A4FCF;   // indigo accent
        private static final int RADIUS_DP      = 10;           // corner radius for rect targets

        private final List<TourStep> steps;
        private int currentStep = 0;

        // Spotlight geometry (animated)
        private float spotX, spotY, spotR;
        private float nextX, nextY, nextR;

        // Ripple animation
        private float rippleRadius = 0f;
        private float rippleAlpha  = 0f;
        private ValueAnimator rippleAnim;

        // Transition animator
        private ValueAnimator transitionAnim;

        // Paint objects
        private final Paint overlayPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint clearPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ripplePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Tooltip card view
        private CardView tooltipCard;

        private final int dp8;
        private final int dp16;
        private final int dp20;
        private final int screenW;
        private final int screenH;

        TourOverlayView(Context context, List<TourStep> steps) {
            super(context);
            this.steps = steps;
            setWillNotDraw(false);
            setLayerType(LAYER_TYPE_HARDWARE, null);
            setClickable(true); // consume touches

            dp8  = dp(8);
            dp16 = dp(16);
            dp20 = dp(20);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            screenW = dm.widthPixels;
            screenH = dm.heightPixels;

            setupPaints();
            buildTooltipCard(context);
        }

        private void setupPaints() {
            overlayPaint.setColor(OVERLAY_COLOR);
            overlayPaint.setStyle(Paint.Style.FILL);

            clearPaint.setColor(Color.TRANSPARENT);
            clearPaint.setStyle(Paint.Style.FILL);
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

            ringPaint.setColor(RING_COLOR);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(dp(3));

            ripplePaint.setColor(RIPPLE_COLOR);
            ripplePaint.setStyle(Paint.Style.STROKE);
            ripplePaint.setStrokeWidth(dp(2));
        }

        // â”€â”€ Tooltip card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        
        private int getThemeColor(Context context, int attrResId, int defaultColor) {
            android.content.res.TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attrResId});
            try {
                return a.getColor(0, defaultColor);
            } finally {
                a.recycle();
            }
        }

        private void buildTooltipCard(Context ctx) {
            int cardBg = getThemeColor(ctx, com.google.android.material.R.attr.colorSurface, CARD_BG);
            int textColorTitle = getThemeColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0xFF1A237E);
            int textColorDesc = getThemeColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF455A64);
            int dividerColor = getThemeColor(ctx, com.google.android.material.R.attr.colorOutlineVariant, 0xFFEEEEEE);
            int primaryColor = getThemeColor(ctx, androidx.appcompat.R.attr.colorPrimary, CARD_ACCENT);

            tooltipCard = new CardView(ctx);
            tooltipCard.setRadius(dp(16));
            tooltipCard.setCardElevation(dp(12));
            tooltipCard.setCardBackgroundColor(cardBg);
            tooltipCard.setPreventCornerOverlap(true);

            LinearLayout inner = new LinearLayout(ctx);
            inner.setOrientation(LinearLayout.VERTICAL);
            int pad = dp16;
            inner.setPadding(pad, pad, pad, dp(12));

            // Progress indicator strip at top
            View accent = new View(ctx);
            accent.setBackgroundColor(primaryColor);
            LinearLayout.LayoutParams stripLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(4));
            stripLp.bottomMargin = dp(12);
            inner.addView(accent, stripLp);

            // Step counter  e.g. "Step 1 of 4"
            TextView tvStep = new TextView(ctx);
            tvStep.setId(R.id.tvTourStepCounter);
            tvStep.setTextSize(10);
            tvStep.setTextColor(0xFF90A4AE);
            tvStep.setTypeface(Typeface.DEFAULT_BOLD);
            tvStep.setLetterSpacing(0.1f);
            inner.addView(tvStep, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            // Title
            TextView tvTitle = new TextView(ctx);
            tvTitle.setId(R.id.tvTourTitle);
            tvTitle.setTextSize(16);
            tvTitle.setTextColor(textColorTitle);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            titleLp.topMargin = dp(4);
            inner.addView(tvTitle, titleLp);

            // Description
            TextView tvDesc = new TextView(ctx);
            tvDesc.setId(R.id.tvTourDescription);
            tvDesc.setTextSize(13.5f);
            tvDesc.setTextColor(textColorDesc);
            tvDesc.setLineSpacing(dp(3), 1f);
            LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            descLp.topMargin = dp(8);
            descLp.bottomMargin = dp(12);
            inner.addView(tvDesc, descLp);

            // Divider
            View divider = new View(ctx);
            divider.setBackgroundColor(dividerColor);
            inner.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));

            // Buttons row
            LinearLayout btnRow = new LinearLayout(ctx);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = dp(10);

            // Skip button
            TextView btnSkip = new TextView(ctx);
            btnSkip.setId(R.id.tvTourSkip);
            btnSkip.setTextSize(13);
            btnSkip.setTextColor(0xFF90A4AE);
            btnSkip.setPadding(0, dp(4), 0, dp(4));
            btnSkip.setText("Skip");
            btnSkip.setClickable(true);
            btnSkip.setOnClickListener(v -> dismiss());
            LinearLayout.LayoutParams skipLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            btnRow.addView(btnSkip, skipLp);

            // Next / Got It button
            TextView btnNext = new TextView(ctx);
            btnNext.setId(R.id.tvTourNext);
            btnNext.setTextSize(13);
            btnNext.setTextColor(Color.WHITE);
            btnNext.setTypeface(Typeface.DEFAULT_BOLD);
            btnNext.setPadding(dp(20), dp(8), dp(20), dp(8));
            btnNext.setBackgroundResource(R.drawable.bg_pill_info);
            btnNext.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(primaryColor));
            btnNext.setClickable(true);
            btnNext.setOnClickListener(v -> nextStep());
            btnRow.addView(btnNext, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            inner.addView(btnRow, rowLp);
            tooltipCard.addView(inner);

            LayoutParams cardLp = new LayoutParams(
                    (int)(screenW * 0.85f), LayoutParams.WRAP_CONTENT);
            cardLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            addView(tooltipCard, cardLp);
            tooltipCard.setVisibility(INVISIBLE);
        }

        // â”€â”€ Step logic â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        void showStep(int index) {
            currentStep = index;
            TourStep step = steps.get(index);
            boolean isMarathi = Locale.getDefault().getLanguage().equals("mr");

            // Update tooltip text
            updateTooltipText(step, index, isMarathi);

            if (step.targetViewId != 0) {
                View target = findTargetView(step.targetViewId);
                if (target != null) {
                    if (target instanceof RecyclerView && step.targetChildIndex >= 0) {
                        RecyclerView rv = (RecyclerView) target;
                        rv.smoothScrollToPosition(step.targetChildIndex);
                        postDelayed(() -> {
                            View child = rv.getLayoutManager() != null ? rv.getLayoutManager().findViewByPosition(step.targetChildIndex) : null;
                            if (child != null) {
                                if (step.targetChildViewId != 0) {
                                    View innerTarget = child.findViewById(step.targetChildViewId);
                                    spotlightTarget(innerTarget != null ? innerTarget : child, index == 0);
                                } else {
                                    spotlightTarget(child, index == 0);
                                }
                            } else {
                                spotlightTarget(rv, index == 0);
                            }
                        }, 400);
                    } else {
                        // Scroll the target into view first, then spotlight it
                        scrollToReveal(target, () -> spotlightTarget(target, index == 0));
                    }
                } else {
                    // Target not found â€” show centred tooltip without spotlight
                    spotX = 0; spotY = 0; spotR = 0;
                    centerTooltip();
                    showTooltipAnimated();
                    invalidate();
                }
            } else {
                // Intro/outro full-card step â€” no spotlight
                stopRipple();
                spotX = 0; spotY = 0; spotR = 0;
                centerTooltip();
                showTooltipAnimated();
                invalidate();
            }
        }

        /**
         * Walks up the view hierarchy to find a NestedScrollView or ScrollView parent.
         * Smoothly scrolls it so the target view is vertically centred in the visible area.
         * Waits 350 ms for the scroll to settle, then calls onReady.
         */
        private void scrollToReveal(View target, Runnable onReady) {
            // Find the nearest scrollable ancestor
            ViewGroup scrollParent = findScrollParent(target);
            if (scrollParent == null) {
                // No scroll parent â€“ just run immediately
                postDelayed(onReady, 0);
                return;
            }

            // Compute target's top relative to the scroll parent content
            int[] targetLoc = new int[2];
            int[] parentLoc = new int[2];
            target.getLocationOnScreen(targetLoc);
            scrollParent.getLocationOnScreen(parentLoc);

            int targetRelativeTop = targetLoc[1] - parentLoc[1] + getScrollY(scrollParent);
            int targetRelativeCenter = targetRelativeTop + target.getHeight() / 2;
            int parentVisibleHeight = scrollParent.getHeight();

            // Desired scroll: put target centre at 40 % from top of visible area
            int desiredScrollY = targetRelativeCenter - (int)(parentVisibleHeight * 0.40f);
            desiredScrollY = Math.max(0, desiredScrollY);

            int currentScrollY = getScrollY(scrollParent);
            boolean needsScroll = Math.abs(desiredScrollY - currentScrollY) > dp(32);

            if (!needsScroll) {
                postDelayed(onReady, 0);
                return;
            }

            // Animate the scroll
            final int from = currentScrollY;
            final int to   = desiredScrollY;
            ValueAnimator scrollAnim = ValueAnimator.ofInt(from, to);
            scrollAnim.setDuration(380);
            scrollAnim.setInterpolator(new DecelerateInterpolator(2f));
            scrollAnim.addUpdateListener(a -> {
                int v = (int) a.getAnimatedValue();
                smoothScrollTo(scrollParent, v);
            });
            scrollAnim.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    // Wait one more frame for layout to settle, then spotlight
                    postDelayed(onReady, 80);
                }
            });
            scrollAnim.start();
        }

        /** Actually move spotlight onto the (already scrolled-into-view) target. */
        private void spotlightTarget(View target, boolean firstSpot) {
            // Guard: if the view is invisible / GONE or not yet laid out, fall back
            // to the centred no-spotlight tooltip so we never draw a 0-px circle.
            if (target.getWidth() == 0 || target.getHeight() == 0
                    || target.getVisibility() != VISIBLE) {
                spotX = 0; spotY = 0; spotR = 0;
                centerTooltip();
                showTooltipAnimated();
                invalidate();
                return;
            }

            int[] loc = new int[2];
            target.getLocationInWindow(loc);
            float cx = loc[0] + target.getWidth() / 2f;
            float cy = loc[1] + target.getHeight() / 2f;
            // Use the half-diagonal so the entire view (even wide chip/banner shapes)
            // fits inside the spotlight circle. Enforce a minimum radius of 56 dp so
            // tiny views (small chips, icons) are always clearly visible.
            float halfDiag = (float) Math.sqrt(
                    (target.getWidth()  / 2.0) * (target.getWidth()  / 2.0) +
                    (target.getHeight() / 2.0) * (target.getHeight() / 2.0));
            float r = Math.max(halfDiag + dp(22), dp(56));

            if (firstSpot || (spotX == 0 && spotY == 0)) {
                spotX = cx; spotY = cy; spotR = r;
                positionTooltip(loc, target);
                showTooltipAnimated();
                startRippleAnimation();
                invalidate();
            } else {
                animateSpotlightTo(cx, cy, r, () -> {
                    positionTooltip(loc, target);
                    showTooltipAnimated();
                    startRippleAnimation();
                });
            }
        }

        // â”€â”€ Scroll helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        /** Walk up parent chain to find first NestedScrollView or ScrollView. */
        private ViewGroup findScrollParent(View v) {
            android.view.ViewParent p = v.getParent();
            while (p != null) {
                if (p instanceof NestedScrollView || p instanceof ScrollView) {
                    return (ViewGroup) p;
                }
                p = p.getParent();
            }
            return null;
        }

        private int getScrollY(ViewGroup vg) {
            if (vg instanceof NestedScrollView) return ((NestedScrollView) vg).getScrollY();
            if (vg instanceof ScrollView)       return ((ScrollView) vg).getScrollY();
            return 0;
        }

        private void smoothScrollTo(ViewGroup vg, int y) {
            if (vg instanceof NestedScrollView) ((NestedScrollView) vg).scrollTo(0, y);
            else if (vg instanceof ScrollView)  ((ScrollView) vg).scrollTo(0, y);
        }

        private void nextStep() {
            if (currentStep + 1 < steps.size()) {
                hideTooltipThen(() -> showStep(currentStep + 1));
            } else {
                dismiss();
            }
        }

        private void dismiss() {
            stopRipple();
            if (transitionAnim != null) transitionAnim.cancel();
            animate().alpha(0f).setDuration(250).withEndAction(() -> {
                ViewGroup parent = (ViewGroup) getParent();
                if (parent != null) parent.removeView(this);
            }).start();
        }

        // â”€â”€ Spotlight animation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        private void animateSpotlightTo(float tx, float ty, float tr, Runnable onDone) {
            if (transitionAnim != null) transitionAnim.cancel();
            stopRipple();

            final float sx = spotX, sy = spotY, sr = spotR;
            transitionAnim = ValueAnimator.ofFloat(0f, 1f);
            transitionAnim.setDuration(450);
            transitionAnim.setInterpolator(new DecelerateInterpolator(2f));
            transitionAnim.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                spotX = sx + (tx - sx) * t;
                spotY = sy + (ty - sy) * t;
                spotR = sr + (tr - sr) * t;
                invalidate();
            });
            transitionAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    spotX = tx; spotY = ty; spotR = tr;
                    if (onDone != null) onDone.run();
                }
            });
            transitionAnim.start();
        }

        // â”€â”€ Ripple animation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        private void startRippleAnimation() {
            stopRipple();
            rippleAnim = ValueAnimator.ofFloat(0f, 1f);
            rippleAnim.setDuration(1200);
            rippleAnim.setRepeatCount(ValueAnimator.INFINITE);
            rippleAnim.setRepeatMode(ValueAnimator.RESTART);
            rippleAnim.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                rippleRadius = spotR + dp(16) * t;
                rippleAlpha  = 1f - t;
                invalidate();
            });
            rippleAnim.start();
        }

        private void stopRipple() {
            if (rippleAnim != null) { rippleAnim.cancel(); rippleAnim = null; }
            rippleRadius = 0f; rippleAlpha = 0f;
        }

        // â”€â”€ Drawing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // 1. Dark overlay
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            if (spotR > 0) {
                // 2. Ripple ring (drawn before clear so it's on top of overlay)
                if (rippleAlpha > 0 && rippleRadius > 0) {
                    ripplePaint.setAlpha((int)(rippleAlpha * 160));
                    canvas.drawCircle(spotX, spotY, rippleRadius, ripplePaint);
                }

                // 3. Clear circular spotlight
                canvas.drawCircle(spotX, spotY, spotR, clearPaint);

                // 4. Gold ring border around spotlight
                canvas.drawCircle(spotX, spotY, spotR, ringPaint);
            }
        }

        // â”€â”€ Tooltip positioning â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        private void positionTooltip(int[] targetLoc, View target) {
            int cardW = (int)(screenW * 0.85f);
            int cardMargin = dp(20);

            boolean placeBelow = targetLoc[1] + target.getHeight() + dp(60) < screenH / 2f
                    + dp(80);

            int topY;
            if (placeBelow) {
                // Place tooltip below the target
                topY = targetLoc[1] + target.getHeight() + dp(16);
            } else {
                // Place tooltip above the target
                topY = Math.max(dp(40), targetLoc[1] - dp(200));
            }

            int leftX = (screenW - cardW) / 2;

            LayoutParams lp = (LayoutParams) tooltipCard.getLayoutParams();
            lp.leftMargin = leftX;
            lp.topMargin  = topY;
            lp.gravity    = Gravity.NO_GRAVITY;
            tooltipCard.setLayoutParams(lp);
        }

        private void centerTooltip() {
            int cardW = (int)(screenW * 0.85f);
            int leftX = (screenW - cardW) / 2;
            int topY  = (int)(screenH * 0.28f);

            LayoutParams lp = (LayoutParams) tooltipCard.getLayoutParams();
            lp.leftMargin = leftX;
            lp.topMargin  = topY;
            lp.gravity    = Gravity.NO_GRAVITY;
            tooltipCard.setLayoutParams(lp);
        }

        private void showTooltipAnimated() {
            tooltipCard.setAlpha(0f);
            tooltipCard.setTranslationY(dp(20));
            tooltipCard.setVisibility(VISIBLE);
            tooltipCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(320)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
        }

        private void hideTooltipThen(Runnable action) {
            tooltipCard.animate()
                    .alpha(0f)
                    .translationY(-dp(12))
                    .setDuration(200)
                    .withEndAction(() -> {
                        tooltipCard.setVisibility(INVISIBLE);
                        if (action != null) action.run();
                    })
                    .start();
        }

        // â”€â”€ Text binding â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        private void updateTooltipText(TourStep step, int index, boolean isMarathi) {
            TextView tvStep  = tooltipCard.findViewById(R.id.tvTourStepCounter);
            TextView tvTitle = tooltipCard.findViewById(R.id.tvTourTitle);
            TextView tvDesc  = tooltipCard.findViewById(R.id.tvTourDescription);
            TextView btnSkip = tooltipCard.findViewById(R.id.tvTourSkip);
            TextView btnNext = tooltipCard.findViewById(R.id.tvTourNext);

            boolean isLast = (index == steps.size() - 1);

            if (tvStep != null)
                tvStep.setText(String.format(Locale.getDefault(),
                        isMarathi ? "à¤ªà¤¾à¤Šà¤² %d / %d" : "Step %d of %d",
                        index + 1, steps.size()));
            if (tvTitle != null)
                tvTitle.setText(step.title);
            if (tvDesc != null)
                tvDesc.setText(step.description);
            if (btnSkip != null)
                btnSkip.setText(isMarathi ? "à¤µà¤—à¤³à¤¾" : "Skip");
            if (btnNext != null)
                btnNext.setText(isLast
                        ? (isMarathi ? "à¤¸à¤®à¤œà¤²à¥‡ âœ“" : "Got It âœ“")
                        : (isMarathi ? "à¤ªà¥à¤¢à¥‡ â†’" : "Next â†’"));
        }

        // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        private View findTargetView(int viewId) {
            // Search the whole window decor tree
            return getRootView().findViewById(viewId);
        }

        private int dp(float value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }
    }
}
