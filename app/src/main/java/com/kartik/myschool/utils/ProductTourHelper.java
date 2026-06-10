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

import android.widget.ScrollView;

import com.kartik.myschool.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ProductTourHelper — Animated coach mark & guided product tour overlay.
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

    // ────────────────────────────────────────────────
    // Public entry point
    // ────────────────────────────────────────────────

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

    // ────────────────────────────────────────────────
    // TourStep data model
    // ────────────────────────────────────────────────

    public static class TourStep {
        @IdRes public final int targetViewId;   // 0 = no spotlight (full-card intro step)
        public final String title;
        public final String description;

        public TourStep(int targetViewId, String title, String description) {
            this.targetViewId = targetViewId;
            this.title = title;
            this.description = description;
        }
    }

    // ────────────────────────────────────────────────
    // Step definitions per page
    // ────────────────────────────────────────────────

    private static List<TourStep> buildSteps(String pageKey, boolean m) {
        List<TourStep> s = new ArrayList<>();
        switch (pageKey != null ? pageKey : "default") {

            // ── Home / Info-Print Setting ──────────────────────────────
            case "info_print":
                s.add(new TourStep(0,
                        m ? "माहिती व प्रिंट सेटिंग" : "Info & Print Setting",
                        m ? "या स्क्रीनवर वर्ष, सत्र व वर्ग सेट करून सेव करा." : "Set the academic year, semester & class on this screen."));
                s.add(new TourStep(R.id.cardYear,
                        m ? "१. शैक्षणिक वर्ष" : "1. Academic Year",
                        m ? "येथे वर्ष निवडा. बाण दाबून वर्ष बदलता येते." : "Tap to select the academic year. Use arrows to change it."));
                s.add(new TourStep(R.id.cardSemester,
                        m ? "२. सत्र निवडा" : "2. Select Semester",
                        m ? "पहिले किंवा दुसरे सत्र निवडण्यासाठी स्क्रोल करा." : "Swipe left/right to choose the first or second semester."));
                s.add(new TourStep(R.id.panelClass,
                        m ? "३. वर्ग सक्रिय करा" : "3. Activate Class",
                        m ? "आपला वर्ग निवडण्यासाठी येथे टॅप करा आणि 'ACTIVATE' दाबा." : "Tap to select your class and press Activate to confirm."));
                s.add(new TourStep(R.id.btnGoToClass,
                        m ? "४. वर्गावर जा" : "4. Go to Class",
                        m ? "'वर्गावर जा' दाबल्यावर थेट विद्यार्थी यादीत जाल." : "Press 'Go to Class' to open the student list directly."));
                s.add(new TourStep(R.id.btnAllClasses,
                        m ? "५. सर्व वर्ग" : "5. All Classes",
                        m ? "शाळेतील सर्व वर्गांची यादी पाहण्यासाठी येथे टॅप करा." : "Tap here to view the full list of all classes in the school."));
                break;

            // ── Stats Dashboard ────────────────────────────────────────
            case "stats_dashboard":
                s.add(new TourStep(0,
                        m ? "सांख्यिकी डॅशबोर्ड" : "Stats Dashboard",
                        m ? "शाळेची आणि वर्गाची आकडेवारी आलेख स्वरूपात पाहा." : "View graphical school and class statistics here."));
                s.add(new TourStep(R.id.tvTotalStudents,
                        m ? "१. एकूण विद्यार्थी" : "1. Total Students",
                        m ? "शाळेतील एकूण नोंदणीकृत विद्यार्थ्यांची संख्या येथे दिसते." : "Shows total enrolled students across all classes in the school."));
                s.add(new TourStep(R.id.rvDashboard,
                        m ? "२. वर्गनिहाय आलेख" : "2. Class-wise Charts",
                        m ? "प्रत्येक वर्गाचा लिंग गुणोत्तर व जात प्रवर्ग आलेख येथे पहा." : "Tap any class card to view gender ratio & caste distribution charts."));
                break;

            // ── Class & Division List ──────────────────────────────────
            case "class_div":
                s.add(new TourStep(0,
                        m ? "वर्ग आणि तुकडी" : "Class & Division Setup",
                        m ? "शाळेतील सर्व वर्ग व तुकड्या येथे व्यवस्थापित करा." : "Manage all classes and divisions of your school here."));
                s.add(new TourStep(R.id.tvClassDivYear,
                        m ? "१. चालू सत्र माहिती" : "1. Current Session",
                        m ? "सध्या सक्रिय वर्ष व सत्राची माहिती येथे दिसते." : "Displays the currently active academic year and semester."));
                s.add(new TourStep(R.id.rvClassDiv,
                        m ? "२. वर्गांची यादी" : "2. Class List",
                        m ? "सर्व वर्गांची यादी येथे दिसते. ३-बिंदूंवर क्लिक करून संपादन करा." : "All classes are listed here. Tap 3-dots to edit or delete a class."));
                s.add(new TourStep(R.id.fabAddClass,
                        m ? "३. नवीन वर्ग जोडा" : "3. Add New Class",
                        m ? "'जोडा' बटण दाबून नवीन वर्ग आणि तुकडी तयार करा." : "Press the '+' button to create a new class and division."));
                break;

            // ── Student List ───────────────────────────────────────────
            case "students":
                s.add(new TourStep(0,
                        m ? "विद्यार्थ्यांची यादी" : "Student List",
                        m ? "या पानावर विद्यार्थी जोडा, शोधा आणि व्यवस्थापित करा." : "Add, search and manage all students on this screen."));
                s.add(new TourStep(R.id.btnHelp,
                        m ? "१. मदत" : "1. Help",
                        m ? "प्रश्नचिन्ह दाबल्यास या पानाची संपूर्ण मार्गदर्शिका मिळेल." : "Tap the question mark to view the full guide for this page."));
                s.add(new TourStep(R.id.btnExcel,
                        m ? "२. एक्सेल आयात/निर्यात" : "2. Excel Import / Export",
                        m ? "एक्सेल चिन्हावर क्लिक करून विद्यार्थी डेटा आयात किंवा निर्यात करा." : "Tap the Excel icon to import or export student data as CSV."));
                s.add(new TourStep(R.id.btnIdCard,
                        m ? "३. आयडी कार्ड" : "3. ID Card",
                        m ? "विद्यार्थ्यांचे ओळखपत्र तयार करण्यासाठी येथे टॅप करा." : "Tap here to generate student identity cards."));
                s.add(new TourStep(R.id.etSearch,
                        m ? "४. विद्यार्थी शोधा" : "4. Search Students",
                        m ? "नाव किंवा रोल नंबर टाइप करून विद्यार्थी शोधा." : "Type a name or roll number here to quickly find a student."));
                s.add(new TourStep(R.id.fabAddStudent,
                        m ? "५. नवीन विद्यार्थी जोडा" : "5. Add New Student",
                        m ? "खालील '+' बटण दाबून नवीन विद्यार्थ्याची नोंदणी करा." : "Tap the '+' FAB at the bottom-right to register a new student."));
                break;

            // ── Attendance ─────────────────────────────────────────────
            case "attendance":
                s.add(new TourStep(0,
                        m ? "मासिक हजेरी" : "Monthly Attendance",
                        m ? "येथे विद्यार्थ्यांची दरमहा हजेरी नोंदवा." : "Record monthly attendance for each student here."));
                s.add(new TourStep(R.id.ivActionHelp,
                        m ? "१. मदत" : "1. Help",
                        m ? "या चिन्हावर क्लिक केल्यास या पानाची माहिती मिळेल." : "Tap here to view a guide about the Attendance screen."));
                s.add(new TourStep(R.id.ivActionAdd,
                        m ? "२. हजेरी जोडा" : "2. Add Attendance",
                        m ? "'जोडा' बटणावर क्लिक करून महिन्याची हजेरी प्रविष्ट करा." : "Tap Add to enter working days and present days for a month."));
                s.add(new TourStep(R.id.ivActionReport,
                        m ? "३. अहवाल पहा" : "3. View Report",
                        m ? "रिपोर्ट चिन्हावर क्लिक करून सरासरी हजेरी आणि सर्वोत्तम विद्यार्थी पहा." : "Tap the Report icon to view class attendance summary."));
                s.add(new TourStep(R.id.ivActionMore,
                        m ? "४. अधिक पर्याय" : "4. More Options",
                        m ? "३-बिंदू मेनूमधून हजेरी डुप्लिकेट किंवा डिलीट करण्याचे पर्याय मिळतील." : "3-dot menu gives options to duplicate or delete attendance records."));
                s.add(new TourStep(R.id.rvAttendanceStudents,
                        m ? "५. विद्यार्थी यादी" : "5. Student List",
                        m ? "येथे प्रत्येक विद्यार्थ्याच्या हजेरीचे तपशील दिसतात." : "Scroll through each student's monthly attendance records here."));
                break;

            // ── Evaluation (Formative / Summative) ────────────────────
            case "formative_summative":
                s.add(new TourStep(0,
                        m ? "मूल्यमापन नोंदणी" : "Evaluation Entry",
                        m ? "विद्यार्थ्यांचे आकारिक व संकलित गुण येथे भरा." : "Enter formative and summative marks for each student here."));
                s.add(new TourStep(R.id.btnHelpSquare,
                        m ? "१. मदत" : "1. Help",
                        m ? "प्रश्नचिन्ह बटण दाबल्यास या पानाची माहिती मिळेल." : "Tap the question mark to view help for this evaluation page."));
                s.add(new TourStep(R.id.btnGridListToggle,
                        m ? "२. दृश्य बदला" : "2. Toggle View",
                        m ? "ग्रीड किंवा लिस्ट व्ह्यू निवडण्यासाठी येथे टॅप करा." : "Tap here to switch between grid and list view for students."));
                s.add(new TourStep(R.id.btnThreeDotMenu,
                        m ? "३. अधिक पर्याय" : "3. More Options",
                        m ? "३-बिंदू मेनूमध्ये गुण सेव, रीसेट इत्यादी पर्याय आहेत." : "The 3-dot menu has options to save, reset or configure marks."));
                break;

            // ── Descriptive Remarks ────────────────────────────────────
            case "descriptive":
                s.add(new TourStep(0,
                        m ? "वर्णनात्मक नोंदी" : "Descriptive Remarks",
                        m ? "विद्यार्थ्यांचे वर्तन, आवड व प्रगती शेरे येथे भरा." : "Record student behavioral remarks and special interests here."));
                s.add(new TourStep(R.id.btnHelpSquare,
                        m ? "१. मदत" : "1. Help",
                        m ? "या चिन्हावर क्लिक केल्यास या पानाची माहिती मिळेल." : "Tap here to view help information about this page."));
                break;

            // ── Fallback / Generic ─────────────────────────────────────
            default:
                s.add(new TourStep(0,
                        m ? "या पानाबद्दल" : "About This Page",
                        m ? "या पानावरील माहिती व्यवस्थापित करा." : "Use this page to manage the features shown here."));
                break;
        }
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  TourOverlayView  — custom FrameLayout drawn over entire screen
    // ─────────────────────────────────────────────────────────────────────

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

        // ── Tooltip card ─────────────────────────────────────────────

        private void buildTooltipCard(Context ctx) {
            tooltipCard = new CardView(ctx);
            tooltipCard.setRadius(dp(16));
            tooltipCard.setCardElevation(dp(12));
            tooltipCard.setCardBackgroundColor(CARD_BG);
            tooltipCard.setPreventCornerOverlap(true);

            LinearLayout inner = new LinearLayout(ctx);
            inner.setOrientation(LinearLayout.VERTICAL);
            int pad = dp16;
            inner.setPadding(pad, pad, pad, dp(12));

            // Progress indicator strip at top
            View accent = new View(ctx);
            accent.setBackgroundColor(CARD_ACCENT);
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
            tvTitle.setTextColor(0xFF1A237E);
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
            tvDesc.setTextColor(0xFF455A64);
            tvDesc.setLineSpacing(dp(3), 1f);
            LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            descLp.topMargin = dp(8);
            descLp.bottomMargin = dp(12);
            inner.addView(tvDesc, descLp);

            // Divider
            View divider = new View(ctx);
            divider.setBackgroundColor(0xFFEEEEEE);
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
                    android.content.res.ColorStateList.valueOf(CARD_ACCENT));
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

        // ── Step logic ───────────────────────────────────────────────

        void showStep(int index) {
            currentStep = index;
            TourStep step = steps.get(index);
            boolean isMarathi = Locale.getDefault().getLanguage().equals("mr");

            // Update tooltip text
            updateTooltipText(step, index, isMarathi);

            if (step.targetViewId != 0) {
                View target = findTargetView(step.targetViewId);
                if (target != null) {
                    // Scroll the target into view first, then spotlight it
                    scrollToReveal(target, () -> spotlightTarget(target, index == 0));
                } else {
                    // Target not found — show centred tooltip without spotlight
                    spotX = 0; spotY = 0; spotR = 0;
                    centerTooltip();
                    showTooltipAnimated();
                    invalidate();
                }
            } else {
                // Intro/outro full-card step — no spotlight
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
                // No scroll parent – just run immediately
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
            int[] loc = new int[2];
            target.getLocationInWindow(loc);
            float cx = loc[0] + target.getWidth() / 2f;
            float cy = loc[1] + target.getHeight() / 2f;
            float r  = Math.max(target.getWidth(), target.getHeight()) / 2f + dp(20);

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

        // ── Scroll helpers ──────────────────────────────────────────

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

        // ── Spotlight animation ──────────────────────────────────────

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

        // ── Ripple animation ─────────────────────────────────────────

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

        // ── Drawing ──────────────────────────────────────────────────

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

        // ── Tooltip positioning ──────────────────────────────────────

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

        // ── Text binding ─────────────────────────────────────────────

        private void updateTooltipText(TourStep step, int index, boolean isMarathi) {
            TextView tvStep  = tooltipCard.findViewById(R.id.tvTourStepCounter);
            TextView tvTitle = tooltipCard.findViewById(R.id.tvTourTitle);
            TextView tvDesc  = tooltipCard.findViewById(R.id.tvTourDescription);
            TextView btnSkip = tooltipCard.findViewById(R.id.tvTourSkip);
            TextView btnNext = tooltipCard.findViewById(R.id.tvTourNext);

            boolean isLast = (index == steps.size() - 1);

            if (tvStep != null)
                tvStep.setText(String.format(Locale.getDefault(),
                        isMarathi ? "पाऊल %d / %d" : "Step %d of %d",
                        index + 1, steps.size()));
            if (tvTitle != null)
                tvTitle.setText(step.title);
            if (tvDesc != null)
                tvDesc.setText(step.description);
            if (btnSkip != null)
                btnSkip.setText(isMarathi ? "वगळा" : "Skip");
            if (btnNext != null)
                btnNext.setText(isLast
                        ? (isMarathi ? "समजले ✓" : "Got It ✓")
                        : (isMarathi ? "पुढे →" : "Next →"));
        }

        // ── Helpers ──────────────────────────────────────────────────

        private View findTargetView(int viewId) {
            // Search the whole window decor tree
            return getRootView().findViewById(viewId);
        }

        private int dp(float value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }
    }
}
