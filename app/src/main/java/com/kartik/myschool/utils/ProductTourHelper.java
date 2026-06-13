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

    // ────────────────────────────────────────────────
    // Step definitions per page
    // ────────────────────────────────────────────────

    private static List<TourStep> buildSteps(String pageKey, boolean m) {
        List<TourStep> s = new ArrayList<>();
        switch (pageKey != null ? pageKey : "default") {

            // ════════════════════════════════════════════════════════════
            // 1. Home / Info-Print Setting  (8 steps)
            // ════════════════════════════════════════════════════════════
            case "info_print":
                s.add(new TourStep(0,
                        m ? "🏠 मुख्य स्क्रीन" : "🏠 Home Screen",
                        m ? "येथे वर्ष, सत्र व वर्ग सेट करा. 'वर्गावर जा' दाबल्यास थेट विद्यार्थी यादीत जाल."
                          : "Set your academic year, semester & class here, then go straight to the student list."));
                s.add(new TourStep(R.id.tvTeacherNameHeader,
                        m ? "१. शिक्षकाचे नाव" : "1. Teacher Name",
                        m ? "आपले नाव व शाळा येथे दिसते. प्रोफाइल स्क्रीनमधून माहिती अद्यतन करा."
                          : "Your name and school appear here. Update them from the Profile screen."));
                s.add(new TourStep(R.id.cardYear,
                        m ? "२. शैक्षणिक वर्ष" : "2. Academic Year",
                        m ? "◀ ▶ बाण दाबून शैक्षणिक वर्ष बदला. उदा. 2024-25."
                          : "Tap ◀ ▶ arrows to change the academic year, e.g. 2024-25."));
                s.add(new TourStep(R.id.panelSemester,
                        m ? "३. सत्र निवडा" : "3. Select Semester",
                        m ? "◀ ▶ दाबून पहिले किंवा दुसरे सत्र निवडा."
                          : "Tap ◀ ▶ to choose between Semester 1 and Semester 2."));
                s.add(new TourStep(R.id.panelClass,
                        m ? "४. वर्ग सक्रिय करा" : "4. Activate Your Class",
                        m ? "◀ ▶ दाबून वर्ग बदला. लॉग टिकून राहतो — पुढच्या वेळी आपोआप लोड होईल."
                          : "Use ◀ ▶ to pick your class. It is remembered for next time."));
                s.add(new TourStep(R.id.btnGoToClass,
                        m ? "५. वर्गावर जा" : "5. Go to Class",
                        m ? "हे बटण दाबल्यावर थेट सक्रिय वर्गाच्या विद्यार्थी यादीत जाल."
                          : "Press this to jump directly to the student list of your active class."));
                s.add(new TourStep(R.id.btnAllClasses,
                        m ? "६. सर्व वर्ग" : "6. All Classes",
                        m ? "शाळेतील प्रत्येक वर्गाची यादी एकत्र पाहण्यासाठी येथे टॅप करा."
                          : "Tap here to browse every class in the school at once."));
                s.add(new TourStep(R.id.btnHowToUse,
                        m ? "७. कसे वापरावे?" : "7. How to Use?",
                        m ? "ॲप्लिकेशन कसे वापरावे हे व्हिडिओ मार्गदर्शिकेसाठी येथे टॅप करा."
                          : "Tap here for a video walkthrough on how to use the app."));
                break;

            // ════════════════════════════════════════════════════════════
            // 2. Stats Dashboard  (4 steps)
            // ════════════════════════════════════════════════════════════
            case "stats_dashboard":
                s.add(new TourStep(0,
                        m ? "📊 सांख्यिकी डॅशबोर्ड" : "📊 Stats Dashboard",
                        m ? "शाळेची व वर्गाची संपूर्ण आकडेवारी एकाच ठिकाणी पाहा."
                          : "View all school and class statistics in one place."));
                s.add(new TourStep(R.id.tvHeaderStripInfo,
                        m ? "१. सक्रिय सत्र माहिती" : "1. Active Session Info",
                        m ? "कोणत्या वर्ष, सत्र व वर्गासाठी आकडेवारी दाखवत आहे ते येथे दिसते."
                          : "Shows which year, semester and class the statistics are for."));
                s.add(new TourStep(R.id.tvTotalStudents,
                        m ? "२. एकूण विद्यार्थी" : "2. Total Students",
                        m ? "शाळेतील सर्व वर्गांमध्ये एकूण किती विद्यार्थी नोंदणीकृत आहेत ते येथे दिसते."
                          : "Total number of students enrolled across all classes in the school."));
                s.add(new TourStep(R.id.rvDashboard,
                        m ? "३. वर्गनिहाय आलेख" : "3. Class-wise Charts",
                        m ? "प्रत्येक वर्गाच्या कार्डावर टॅप करा — लिंग व जात प्रवर्ग आलेख दिसेल."
                          : "Tap any class card to see gender ratio and caste distribution charts."));
                s.add(new TourStep(R.id.btnStudentDashboard,
                        m ? "४. विद्यार्थी प्रगती (टॉगल)" : "4. Student Progress (Toggle)",
                        m ? "येथे दाबून तुम्ही विषयनिहाय आणि विद्यार्थीनिहाय आकडेवारीमध्ये अदलाबदल करू शकता."
                          : "Tap here to instantly toggle between subject-wise and student-wise completion progress."));
                break;

            // ════════════════════════════════════════════════════════════
            // 3. Class & Division List  (4 steps)
            // ════════════════════════════════════════════════════════════
            case "class_div":
                s.add(new TourStep(0,
                        m ? "🏫 वर्ग व तुकडी" : "🏫 Class & Division",
                        m ? "शाळेतील सर्व वर्ग आणि तुकड्या येथे व्यवस्थापित करा."
                          : "Manage all classes and divisions of your school here."));
                s.add(new TourStep(R.id.tvClassDivYear,
                        m ? "१. चालू शैक्षणिक वर्ष" : "1. Current Academic Year",
                        m ? "सध्या सक्रिय वर्ष व सत्राची माहिती येथे दिसते."
                          : "Shows the currently active academic year and semester."));
                s.add(new TourStep(R.id.rvClassDiv,
                        m ? "२. वर्गांची यादी" : "2. Class List",
                        m ? "सर्व वर्ग येथे दिसतात. कार्डाच्या ⋮ बटणावर दाबून संपादन / डिलीट करा."
                          : "All classes appear here. Tap ⋮ on a card to edit or delete it."));
                s.add(new TourStep(R.id.fabAddClass,
                        m ? "३. नवीन वर्ग जोडा +" : "3. Add New Class +",
                        m ? "'+' FAB दाबून नवीन वर्ग (उदा. ५-A) तयार करा."
                          : "Tap the '+' button to create a new class, e.g. Class 5-A."));
                break;

            // ════════════════════════════════════════════════════════════
            // 4. Student List  (9 steps)
            // ════════════════════════════════════════════════════════════
            case "students":
                s.add(new TourStep(0,
                        m ? "👨‍🎓 विद्यार्थी यादी" : "👨‍🎓 Student List",
                        m ? "वर्गातील सर्व विद्यार्थी येथे दिसतात. शोध, आयात, निर्यात व जोडणी करा."
                          : "All students in the class are listed here. Search, import, export and add."));
                s.add(new TourStep(R.id.tvHeaderSessionInfo,
                        m ? "१. सत्र माहिती" : "1. Session Info",
                        m ? "कोणत्या वर्षाची व वर्गाची यादी पाहत आहात ते येथे दिसते."
                          : "Shows which year and class student list you are viewing."));
                s.add(new TourStep(R.id.btnHelp,
                        m ? "२. मदत ?" : "2. Help ?",
                        m ? "प्रश्नचिन्हावर टॅप केल्यास या पानाची संपूर्ण मार्गदर्शिका व व्हिडिओ मिळेल."
                          : "Tap '?' to open the complete guide and video for this page."));
                s.add(new TourStep(R.id.btnExcel,
                        m ? "३. एक्सेल 📊" : "3. Excel 📊",
                        m ? "या चिन्हावर टॅप केल्यास CSV आयात किंवा CSV निर्यात पर्याय दिसेल."
                          : "Tap to choose between importing students from CSV or exporting to CSV."));
                s.add(new TourStep(R.id.btnSave,
                        m ? "४. डेटा जतन 💾" : "4. Save Data 💾",
                        m ? "वर्गातील सर्व विद्यार्थ्यांची माहिती बॅकअप म्हणून जतन करा."
                          : "Save a backup of all student information in the class."));
                s.add(new TourStep(R.id.btnMoreOptions,
                        m ? "५. अधिक पर्याय ⋮" : "5. More Options ⋮",
                        m ? "⋮ मेनूमध्ये: सर्व गुण रीसेट, क्रम बदलणे इत्यादी पर्याय आहेत."
                          : "⋮ menu has options to reset all marks, reorder students and more."));
                s.add(new TourStep(R.id.etSearch,
                        m ? "६. विद्यार्थी शोधा 🔍" : "6. Search Student 🔍",
                        m ? "नाव किंवा रोल नंबर टाइप केल्यास यादी त्वरित फिल्टर होते."
                          : "Type a name or roll number — the list filters instantly as you type."));
                s.add(new TourStep(R.id.rvStudents,
                        m ? "७. विद्यार्थी कार्ड्स" : "7. Student Cards",
                        m ? "प्रत्येक कार्डावर टॅप केल्यास विद्यार्थी प्रोफाइल उघडते. दीर्घ-दाब = पर्याय मेनू."
                          : "Tap a card to open student profile. Long-press for quick options."));
                s.add(new TourStep(R.id.fabAddStudent,
                        m ? "८. नवीन विद्यार्थी +" : "8. Add New Student +",
                        m ? "'+' FAB दाबून नवीन विद्यार्थ्याची नोंदणी फॉर्म उघडा."
                          : "Tap '+' to open the registration form for a new student."));
                break;

            // ════════════════════════════════════════════════════════════
            // 5. Attendance  (6 steps)
            // ════════════════════════════════════════════════════════════
            case "attendance":
                s.add(new TourStep(0,
                        m ? "📅 मासिक हजेरी" : "📅 Monthly Attendance",
                        m ? "प्रत्येक महिन्याची विद्यार्थीनिहाय हजेरी येथे नोंदवा व पाहा."
                          : "Record and view monthly attendance for each student here."));
                s.add(new TourStep(R.id.tvAttendanceContext,
                        m ? "१. सत्र व वर्ग माहिती" : "1. Session & Class Info",
                        m ? "कोणत्या वर्ष, सत्र व वर्गाची हजेरी पाहत आहात ते येथे दिसते."
                          : "Shows the year, semester and class whose attendance you are viewing."));
                s.add(new TourStep(R.id.btnToolbarHelp,
                        m ? "२. मदत ?" : "2. Help ?",
                        m ? "हजेरी पानाची संपूर्ण मार्गदर्शिका पाहण्यासाठी येथे टॅप करा."
                          : "Tap to open the complete guide for the Attendance page."));
                s.add(new TourStep(R.id.btnToolbarAdd,
                        m ? "३. हजेरी जोडा +" : "3. Add Attendance +",
                        m ? "'जोडा' दाबल्यावर महिना निवडा, एकूण दिवस व हजर दिवस प्रविष्ट करा."
                          : "Tap Add to select a month and enter working days + present days."));
                s.add(new TourStep(R.id.btnToolbarCalc,
                        m ? "४. हजेरी अहवाल 📋" : "4. Attendance Report 📋",
                        m ? "वर्गाची सरासरी हजेरी टक्केवारी व सर्वोत्तम विद्यार्थी येथे पाहा."
                          : "View class average attendance percentage and top attending students."));
                s.add(new TourStep(R.id.btnToolbarMore,
                        m ? "५. अधिक पर्याय ⋮" : "5. More Options ⋮",
                        m ? "⋮ मेनूमध्ये: हजेरी कॉपी करणे, डिलीट करणे इत्यादी पर्याय आहेत."
                          : "⋮ menu has options to copy attendance to another month or delete."));
                s.add(new TourStep(R.id.rvAttendanceStudents,
                        m ? "६. विद्यार्थी हजेरी यादी" : "6. Student Attendance List",
                        m ? "येथे प्रत्येक विद्यार्थ्याचा हजेरी बॉक्स दिसतो. प्रत्येक बॉक्समध्ये विद्यार्थ्याच्या वार्षिक हजेरीचा संपूर्ण तपशील असतो. खालील स्टेप्समध्ये प्रत्येक भाग समजून घ्या."
                          : "Each student has their own attendance box here. It shows the full yearly attendance summary. The next steps will explain each part of the box."));
                s.add(new TourStep(R.id.rvAttendanceStudents,
                        m ? "७. एकूण हजर दिवस 🟢" : "7. Total Present Days 🟢",
                        m ? "बॉक्सच्या डाव्या बाजूला मोठा हिरवा आकडा दिसतो.\n\nहा आकडा म्हणजे त्या विद्यार्थ्याचे वर्षभरातील एकूण 'हजर दिवस' (Total Present Days) होय.\n\nजास्त आकडा = जास्त उपस्थिती. 🎉"
                          : "The large green number on the left side of the box shows the student's Total Present Days for the entire year.\n\nHigher number = Better attendance! 🎉"));
                s.add(new TourStep(R.id.rvAttendanceStudents,
                        m ? "८. १२ महिन्यांचा तक्ता 📊" : "8. 12 Months Grid 📊",
                        m ? "प्रत्येक बॉक्समध्ये जून ते मे पर्यंत १२ महिन्यांचा तक्ता असतो.\n\nप्रत्येक महिन्यात 'हजर / एकूण' (उदा. 24/26) असे लिहिलेले दिसते.\n• पहिला आकडा = त्या महिन्यात हजर दिवस\n• दुसरा आकडा = एकूण कामकाजाचे दिवस\n\nतक्त्यावर स्क्रोल करून सर्व महिने पाहता येतात."
                          : "Each box contains a 12-month grid from June to May.\n\nEach cell shows 'Present / Total' (e.g. 24/26):\n• First number = Days present that month\n• Second number = Total working days that month\n\nScroll the grid to see all months."));
                s.add(new TourStep(R.id.rvAttendanceStudents,
                        m ? "९. हजेरी बदलण्यासाठी बॉक्सवर क्लिक करा ✏️" : "9. Tap Box to Edit Attendance ✏️",
                        m ? "कोणत्याही विद्यार्थ्याच्या हजेरी बॉक्सवर क्लिक करा.\n\nएक 'हजेरी भरा' पॉप-अप उघडेल. त्यात:\n• प्रत्येक महिन्यासाठी हजर दिवस व एकूण कामकाज दिवस टाका.\n• 'जतन करा' दाबून हजेरी सेव्ह करा.\n\nहे अगदी तशाच पद्धतीने काम करते जसे आकारिक-संकलित गुण भरताना करतात."
                          : "Tap anywhere on any student's attendance box to open the Edit Attendance popup.\n\nIn the popup:\n• Enter Present Days and Working Days for each month.\n• Tap 'Save' to save the attendance.\n\nThis works exactly like the marks entry popup on the Formative/Summative page."));
                s.add(new TourStep(R.id.rvAttendanceStudents,
                        m ? "१०. ⋮ मेन्यू: Duplicate व Delete" : "10. ⋮ Menu: Duplicate & Delete",
                        m ? "प्रत्येक हजेरी बॉक्सच्या उजव्या कोपऱ्यात (⋮) मेन्यू असतो.\n\n• 'Duplicate' → एका विद्यार्थ्याची हजेरी दुसऱ्या विद्यार्थ्यावर कॉपी करा (उदा. वर्गाची समान हजेरी सर्वांसाठी लावायची असल्यास).\n• 'Delete' → त्या विद्यार्थ्याची सर्व हजेरी नष्ट करा.\n\n⚠️ Delete केल्यावर हजेरी परत येत नाही, काळजी घ्या!"
                          : "Each attendance box has a ⋮ (3-dots) menu in the top-right corner.\n\n• 'Duplicate' → Copy this student's attendance to another student (useful when multiple students have the same attendance).\n• 'Delete' → Permanently remove all attendance data for this student.\n\n⚠️ Deleted attendance cannot be recovered!"));
                break;


            // ════════════════════════════════════════════════════════════
            // 6. Formative & Summative Evaluation  (8 steps)
            // ════════════════════════════════════════════════════════════
            case "formative_summative":
                s.add(new TourStep(0,
                        m ? "📝 आकारिक / संकलित मूल्यमापन" : "📝 Formative / Summative Evaluation",
                        m ? "वर्गातील विद्यार्थ्यांचे आकारिक (FA) व संकलित (SA) मूल्यमापन येथे व्यवस्थापित करा."
                          : "Manage Formative (FA) and Summative (SA) evaluations for the class."));
                s.add(new TourStep(R.id.tvHeaderStripInfo,
                        m ? "१. सत्र व परीक्षा माहिती" : "1. Session & Exam Info",
                        m ? "कोणत्या वर्ष, सत्र, वर्ग व परीक्षेसाठी गुण नोंदवत आहात ते येथे दिसते."
                          : "Shows the year, semester, class and exam you are entering marks for."));
                s.add(new TourStep(R.id.btnHelpSquare,
                        m ? "२. मदत ?" : "2. Help ?",
                        m ? "या पानाची संपूर्ण मार्गदर्शिका व प्ले बटण येथे आहे."
                          : "Tap '?' to open the complete guide and animated tour for this page."));
                s.add(new TourStep(R.id.btnAddSquare,
                        m ? "३. मूल्यमापन जोडा +" : "3. Add Evaluation +",
                        m ? "नवीन FA किंवा SA मूल्यमापन तयार करण्यासाठी '+' दाबा."
                          : "Tap '+' to create a new Formative or Summative evaluation entry."));
                s.add(new TourStep(R.id.btnCalcSquare,
                        m ? "४. कॅल्क्युलेटर 🧮" : "4. Calculator 🧮",
                        m ? "गुणांची गणना तपासण्यासाठी बिल्ट-इन कॅल्क्युलेटर वापरा."
                          : "Use the built-in calculator to cross-check and verify marks."));
                s.add(new TourStep(R.id.btnGridListToggle,
                        m ? "५. दृश्य बदला 🔄" : "5. Toggle View 🔄",
                        m ? "ग्रीड दृश्य (अनेक विद्यार्थी एकत्र) किंवा लिस्ट दृश्य निवडा."
                          : "Switch between Grid view (compact) and List view (detailed)."));
                s.add(new TourStep(R.id.btnThreeDotMenu,
                        m ? "६. अधिक पर्याय ⋮" : "6. More Options ⋮",
                        m ? "⋮ मेनूमध्ये: गुण सेव्ह, रीसेट, विद्यार्थी क्रम बदलणे इत्यादी पर्याय आहेत."
                          : "⋮ menu has save, reset marks and reorder student options."));
                s.add(new TourStep(R.id.rvEvaluationStudents,
                        m ? "७. विद्यार्थी मूल्यमापन यादी" : "7. Student Evaluation List",
                        m ? "प्रत्येक विद्यार्थ्याच्या रांगेत गुण भरा. रंगीत गुण = सेव्ह झाले."
                          : "Enter marks in each student row. Coloured marks are already saved."));
                s.add(new TourStep(R.id.swipeRefreshLayout,
                        m ? "८. खाली ओढून रिफ्रेश करा" : "8. Pull to Refresh",
                        m ? "यादी खाली ओढल्यास ताज्या डेटाने रिफ्रेश होते."
                          : "Pull the list down to refresh and reload the latest student data."));
                break;

            // ════════════════════════════════════════════════════════════
            // 7. Descriptive Remarks List  (6 steps)
            // ════════════════════════════════════════════════════════════
            case "descriptive":
                s.add(new TourStep(0,
                        m ? "💬 वर्णनात्मक नोंदी" : "💬 Descriptive Remarks",
                        m ? "विद्यार्थ्यांचे वर्तन, कौशल्ये व विशेष संपादणूक शेरे येथे नोंदवा."
                          : "Record student behavior, skills and special achievement remarks."));
                s.add(new TourStep(R.id.tvHeaderStripInfo,
                        m ? "१. सत्र व वर्ग माहिती" : "1. Session & Class Info",
                        m ? "कोणत्या वर्ग व सत्रासाठी शेरे नोंदवत आहात ते येथे दिसते."
                          : "Shows which class and semester the remarks are being entered for."));
                s.add(new TourStep(R.id.btnHelpSquare,
                        m ? "२. मदत ?" : "2. Help ?",
                        m ? "वर्णनात्मक नोंदी पानाची संपूर्ण मार्गदर्शिका येथे मिळेल."
                          : "Tap '?' for the complete guide on how to use Descriptive Remarks."));
                s.add(new TourStep(R.id.btnAddSquare,
                        m ? "३. शेरे जोडा +" : "3. Add Remarks +",
                        m ? "नवीन शेऱ्याची नोंद करण्यासाठी '+' बटण दाबा."
                          : "Tap '+' to begin adding a new descriptive remark entry."));
                s.add(new TourStep(R.id.btnThreeDotMenu,
                        m ? "४. अधिक पर्याय ⋮" : "4. More Options ⋮",
                        m ? "⋮ मेनूमध्ये: शेरे डिलीट, 'तो'→'ती' (लिंग बदल), रीसेट पर्याय आहेत."
                          : "⋮ has delete entries, swap gender (he→she) and reset options."));
                s.add(new TourStep(R.id.rvDescriptiveStudents,
                        m ? "५. विद्यार्थी यादी" : "5. Student List",
                        m ? "प्रत्येक विद्यार्थ्याच्या कार्डावर टॅप केल्यास त्याचे शेरे संपादित होतात."
                          : "Tap a student card to open and edit their descriptive remarks."));
                break;

            // ════════════════════════════════════════════════════════════
            // 8. Subjects  (4 steps)
            // ════════════════════════════════════════════════════════════
            case "subjects":
                s.add(new TourStep(0,
                        m ? "📚 विषय व्यवस्थापन" : "📚 Subject Management",
                        m ? "वर्गात शिकवले जाणारे विषय येथे जोडा, संपादित करा व क्रम लावा."
                          : "Add, edit and arrange the subjects taught in the active class."));
                s.add(new TourStep(R.id.tvHeaderLabel,
                        m ? "१. वर्ग आणि सत्र माहिती" : "1. Class & Session Info",
                        m ? "तुम्ही कोणत्या वर्षासाठी आणि वर्गासाठी विषय व्यवस्थापित करत आहात ते येथे स्पष्ट दिसते."
                          : "Shows the specific academic year and class you are currently managing subjects for."));
                s.add(new TourStep(R.id.rvSubjectsList,
                        m ? "२. विषयांची यादी" : "2. Subjects List",
                        m ? "विषयाच्या नावावर टॅप करा → तपशील, कमाल गुण व माध्यम संपादित करा."
                          : "Tap a subject name to edit its details, max marks and medium."));
                s.add(new TourStep(R.id.rvSubjectsList, 0, R.id.tvSubjectCode,
                        m ? "३. विषय कोड" : "3. Subject Code",
                        m ? "कोडचा पहिला भाग इयत्ता (उदा. 101) आणि दुसरा भाग विषय (उदा. 101) दर्शवतो. तुम्ही विषयावर क्लिक करून स्वतःचा कस्टम कोड देखील सेट करू शकता."
                          : "The first part of the code represents the standard (e.g. 101) and the second part represents the subject (e.g. 101). You can also edit a subject to set your own custom code."));
                s.add(new TourStep(R.id.rvSubjectsList, 0, R.id.tvDetailsLeft1,
                        m ? "४. FE व SE गुण" : "4. FE & SE Marks",
                        m ? "FE (आकारिक) आणि SE (संकलित) मूल्यमापनाचे एकूण कमाल गुण येथे दिसतात."
                          : "Total max marks for Formative (FE) and Summative (SE) evaluations are shown here."));
                s.add(new TourStep(R.id.rvSubjectsList, 0, R.id.btnCardMenu,
                        m ? "५. अधिक पर्याय (३-बिंदू)" : "5. More Options (3-dots)",
                        m ? "येथे क्लिक करून तुम्ही विषयाचे कमाल गुण आणि अंतर्गत भारांश सविस्तर संपादित करू शकता."
                          : "Tap here to edit the subject's max marks and internal evaluation weightage in detail."));
                s.add(new TourStep(R.id.fabAddSubject,
                        m ? "६. नवीन विषय जोडा +" : "6. Add New Subject +",
                        m ? "या '+' बटणावर टॅप करून तुम्ही वर्गासाठी कोणताही नवीन किंवा कस्टम विषय जोडू शकता."
                          : "Tap this '+' button to add any new or custom subject to the class."));
                break;

            // ════════════════════════════════════════════════════════════
            // 9. Declare Weightage  (5 steps)
            // ════════════════════════════════════════════════════════════
            case "weightage":
                s.add(new TourStep(0,
                        m ? "⚖️ गुण भारांश" : "⚖️ Declare Weightage",
                        m ? "प्रत्येक विषयाचे आकारिक व संकलित उप-घटकांचे भारांश येथे ठरवा."
                          : "Define the weightage for each subject's formative and summative components."));
                s.add(new TourStep(R.id.tvWeightageHeaderContext,
                        m ? "१. सत्र माहिती" : "1. Session Info",
                        m ? "कोणत्या वर्ष व सत्रासाठी भारांश ठरवत आहात ते येथे स्पष्ट दिसते."
                          : "Confirms which academic year and semester this weightage applies to."));
                s.add(new TourStep(R.id.rvWeightageSubjects, 0, R.id.etMaxMarks,
                        m ? "२. एकूण कमाल गुण" : "2. Total Max Marks",
                        m ? "येथे विषयाचे एकूण कमाल गुण भरा. हे बदलल्यास अंतर्गत गुण आपोआप विभागले जातील."
                          : "Enter the total max marks for the subject here. Internal marks will auto-scale accordingly."));
                s.add(new TourStep(R.id.rvWeightageSubjects, 0, R.id.layoutHeader,
                        m ? "३. सविस्तर माहिती उघडा" : "3. Open Details",
                        m ? "विषयाच्या कार्डावर टॅप केल्यास अंतर्गत गुणांची (उदा. लेखी, तोंडी, प्रकल्प) सविस्तर विभागणी उघडेल."
                          : "Tap on the subject card to expand and see the detailed breakdown (Written, Oral, Project, etc.)."));
                s.add(new TourStep(R.id.rvWeightageSubjects, 0, R.id.tvWeightageBreakdown,
                        m ? "४. गुण विभागणी बेरीज" : "4. Marks Distribution",
                        m ? "येथे तुम्हाला आकारिक (FA) आणि संकलित (SA) गुणांची एकूण बेरीज नेहमी दिसेल."
                          : "Here you can always see the total sum of Formative (FA) and Summative (SA) marks."));
                s.add(new TourStep(R.id.btnSaveWeightage,
                        m ? "५. भारांश जतन करा 💾" : "5. Save Weightage 💾",
                        m ? "सर्व भारांश भरल्यावर 'जतन करा' दाबा. एकदाच सेव्ह केल्यास पुरेसे."
                          : "Press Save after filling all fields. Saving once applies to all students."));
                break;

            // ════════════════════════════════════════════════════════════
            // 10. School Settings Dashboard  (9 steps)
            // ════════════════════════════════════════════════════════════
            case "school_settings":
                s.add(new TourStep(0,
                        m ? "🏫 शाळा सेटिंग्ज" : "🏫 School Settings",
                        m ? "शाळेचे संपूर्ण व्यवस्थापन — माहिती, लिंग, जात, शिक्षक, वर्ग, विषय व दिवस."
                          : "Full school management — info, gender, caste, teachers, classes, subjects & days."));
                s.add(new TourStep(R.id.btnDashSchoolInfo,
                        m ? "१. 🏢 शाळेची माहिती" : "1. 🏢 School Info",
                        m ? "शाळेचे नाव, युडायस कोड, जिल्हा, पत्ता येथे संपादित करा."
                          : "Edit school name, UDISE code, district and full address."));
                s.add(new TourStep(R.id.btnDashGender,
                        m ? "२. 👫 लिंग सांख्यिकी" : "2. 👫 Gender Stats",
                        m ? "वर्गातील मुले व मुलींची संख्या आणि गुणोत्तर येथे पाहा."
                          : "View the count of boys and girls and their ratio in the active class."));
                s.add(new TourStep(R.id.btnDashCastCategory,
                        m ? "३. 📊 जात प्रवर्ग" : "3. 📊 Caste Category",
                        m ? "सर्व जात प्रवर्गांमधील विद्यार्थी संख्येचे वितरण येथे दिसते."
                          : "See student distribution across all caste categories in the school."));
                s.add(new TourStep(R.id.btnDashClassTeacher,
                        m ? "४. 👩‍🏫 वर्ग शिक्षक" : "4. 👩‍🏫 Class Teacher",
                        m ? "वर्ग शिक्षक, सहाय्यक शिक्षक, ईमेल व फोन येथे नोंदवा."
                          : "Enter class teacher, assistant teacher, email and phone details."));
                s.add(new TourStep(R.id.btnDashClasses,
                        m ? "५. 📋 वर्ग यादी" : "5. 📋 Classes List",
                        m ? "शाळेतील सर्व वर्गांची यादी व प्रत्येक वर्गातील विद्यार्थी संख्या येथे पाहा."
                          : "View all classes with their student counts across the whole school."));
                s.add(new TourStep(R.id.btnDashSubject,
                        m ? "६. 📖 शेरा बँक" : "6. 📖 Remark Bank",
                        m ? "प्रत्येक विषयासाठी तयार-वर्णनात्मक शेरा पर्याय येथे व्यवस्थापित करा."
                          : "Manage the pre-written descriptive remark options for each subject."));
                s.add(new TourStep(R.id.btnDashDefaultValues,
                        m ? "७. ⚙️ डिफॉल्ट मूल्ये" : "7. ⚙️ Default Values",
                        m ? "विद्यार्थी नोंदणीसाठी डिफॉल्ट माध्यम, जात व इतर मूल्ये येथे सेट करा."
                          : "Set default medium, caste and other values for new student registration."));
                s.add(new TourStep(R.id.btnDashWorkingDays,
                        m ? "८. 📆 कामकाजाचे दिवस" : "8. 📆 Working Days",
                        m ? "जून ते मे प्रत्येक महिन्याचे एकूण कामकाजाचे दिवस येथे सेट करा."
                          : "Set total working days for each month from June to May."));
                break;

            // ════════════════════════════════════════════════════════════
            // 11. Print Report / Marksheet  (4 steps)
            // ════════════════════════════════════════════════════════════
            case "print_report":
                s.add(new TourStep(0,
                        m ? "🖨️ गुणपत्रक अहवाल" : "🖨️ Print Report / Marksheet",
                        m ? "विद्यार्थ्यांचे अधिकृत प्रगतीपत्रक PDF स्वरूपात येथे तयार व प्रिंट करा."
                          : "Generate and print official student report cards as PDF here."));
                s.add(new TourStep(R.id.tvReportPrintingYear,
                        m ? "१. वर्ष व सत्र" : "1. Year & Semester",
                        m ? "कोणत्या वर्ष व सत्रासाठी गुणपत्रक तयार होत आहे ते येथे दिसते."
                          : "Confirms which academic year and semester marksheets are for."));
                s.add(new TourStep(R.id.rvReportCards, 0, R.id.btnReportSettings,
                        m ? "⚙️ सेटिंग चिन्ह" : "⚙️ Settings Icon",
                        m ? "काही अहवालांसाठी येथे टॅप करून अतिरिक्त माहिती भरता येते (उदा. शेरे, तारखा)." 
                          : "Tap here for reports that require additional settings or custom text."));
                s.add(new TourStep(R.id.rvReportCards, 0, R.id.btnReportAction,
                        m ? "🖨️ प्रिंट चिन्ह" : "🖨️ Print Icon",
                        m ? "हा अहवाल त्वरित PDF स्वरूपात तयार करण्यासाठी येथे टॅप करा." 
                          : "Tap this print icon to instantly generate the PDF for this report."));
                s.add(new TourStep(R.id.rvReportCards, 0,
                        m ? "अहवाल १: १. मुखपृष्ठ" : "Report 1: 1. Cover Page",
                        m ? "नोंदवहीचे आकर्षक मुखपृष्ठ" : "Attractive cover page of register"));
                s.add(new TourStep(R.id.rvReportCards, 1,
                        m ? "अहवाल २: २. अनुक्रमणिका" : "Report 2: 2. Index",
                        m ? "सत्र एक वा दोन अनुसार अनुक्रमणिका" : "Index according to semester 1 or 2"));
                s.add(new TourStep(R.id.rvReportCards, 2,
                        m ? "अहवाल ३: ३. गुणनोंदी" : "Report 3: 3. Marks Register",
                        m ? "तंत्रे व श्रेणीसहित आकारिक-संकलित गुणनोंदी" : "Formative-Summative marks register with tools & grades"));
                s.add(new TourStep(R.id.rvReportCards, 3,
                        m ? "अहवाल ४: ४. वर्णनात्मक नोंदी" : "Report 4: 4. Descriptive Entries",
                        m ? "सत्रानुसार विद्यार्थ्यांच्या वर्णनात्मक नोंदी" : "Semester-wise descriptive remarks of students"));
                s.add(new TourStep(R.id.rvReportCards, 4,
                        m ? "अहवाल ५: ५. श्रेणी तक्का" : "Report 5: 5. Grade Table",
                        m ? "सत्र व विषयनुसार गुण व श्रेणी तक्का" : "Marks & grade table according to semester and subject"));
                s.add(new TourStep(R.id.rvReportCards, 5,
                        m ? "अहवाल ६: ६. सर्वसामावेशक निकाल" : "Report 6: 6. Comprehensive Result",
                        m ? "आकारिक-संकलित गण श्रेणीयुक्त" : "Formative-Summative total grade sheet"));
                s.add(new TourStep(R.id.rvReportCards, 6,
                        m ? "अहवाल ७: ७. श्रेणी तक्का" : "Report 7: 7. Roster Grade Table",
                        m ? "सत्र, वर्गवार मुले-मुली श्रेणी तक्का" : "Semester and class-wide boys-girls grade table"));
                s.add(new TourStep(R.id.rvReportCards, 7,
                        m ? "अहवाल ८: ८. गुण-श्रेणीयुक्त निकालपत्रक" : "Report 8: 8. Marks-Grade Ledger",
                        m ? "विषयवार एकूण गुण व श्रेणीयुक्त" : "Subject-wise total marks & grades sheet"));
                s.add(new TourStep(R.id.rvReportCards, 8,
                        m ? "अहवाल ९: ९. प्रगतीपत्रक मुखपृष्ठ" : "Report 9: 9. Progress Card Cover",
                        m ? "A4 साईज कलरफुल प्रगतीपत्रक" : "A4 size colorful progress card cover"));
                s.add(new TourStep(R.id.rvReportCards, 9,
                        m ? "अहवाल १०: १०. प्रगतीपत्रक पृष्ठ" : "Report 10: 10. Progress Card Inner",
                        m ? "A4 साईज प्रगतीपत्रक आतील पृष्ठ" : "A4 size progress card inner page"));
                s.add(new TourStep(R.id.rvReportCards, 10,
                        m ? "अहवाल ११: ११. उपयुक्त रिपोर्ट" : "Report 11: 11. Useful Reports",
                        m ? "विषयवार गुणतक्के" : "Subject-wise marks tables"));
                s.add(new TourStep(R.id.rvReportCards, 11,
                        m ? "अहवाल १२: १२. पाचवी आठवी गुणपत्रक" : "Report 12: 12. 5th & 8th Marksheet",
                        m ? "इयत्ता पाचवी / आठवी गुणपत्रक" : "5th / 8th standard marksheet"));
                s.add(new TourStep(R.id.rvReportCards, 12,
                        m ? "अहवाल १३: १३. पाचवी आठवी वार्षिक तक्के" : "Report 13: 13. 5th & 8th Annual Tables",
                        m ? "इयत्ता पाचवी / आठवी गुणतक्का" : "5th / 8th standard annual tables"));
                s.add(new TourStep(R.id.rvReportCards, 13,
                        m ? "अहवाल १४: १४. प्रगतीपत्रक मुखपृष्ठ" : "Report 14: 14. Progress Card Cover",
                        m ? "A4 साईज कलरफुल प्रगतीपत्रक" : "A4 size colorful progress card cover"));
                s.add(new TourStep(R.id.rvReportCards, 14,
                        m ? "अहवाल १५: १५. वार्षिक निकालपत्रक" : "Report 15: 15. Annual Result Ledger",
                        m ? "सत्र व विषयनुसार गुण व श्रेणी तक्का" : "Marks & grade table according to semester and subject"));
                s.add(new TourStep(R.id.rvReportCards, 15,
                        m ? "अहवाल १६: १६. वार्षिक निकालपत्रक" : "Report 16: 16. Annual Marksheet",
                        m ? "दोन्ही सत्र एकत्र सर्वसमावेशीत तक्ता" : "Both semesters combined comprehensive table"));
                s.add(new TourStep(R.id.rvReportCards, 16,
                        m ? "अहवाल १७: १७. जात श्रेणी तक्ता" : "Report 17: 17. Caste Grade Table",
                        m ? "सत्र, वर्गवार, जातवार मुले-मुली श्रेणी तक्ता." : "Semester, Class-wise, Caste-wise Boys-Girls Grade Table."));
                s.add(new TourStep(R.id.rvReportCards, 17,
                        m ? "अहवाल १८: १८. प्रगतीपत्रक मुखपृष्ठ" : "Report 18: 18. Progress Card Cover",
                        m ? "A4 प्रथम सत्र प्रगतीपत्रक" : "A4 First Semester Progress Card"));
                break;

            // ════════════════════════════════════════════════════════════
            // ════════════════════════════════════════════════════════════
            // 12. Teacher Profile  (8 steps)
            // ════════════════════════════════════════════════════════════
            case "profile":
                s.add(new TourStep(0,
                        m ? "👤 शिक्षकाची प्रोफाइल" : "👤 Teacher Profile",
                        m ? "आपली प्रोफाइल, शाळेचे युडायस कोड, सक्रिय वर्ग व नवीन वर्ग जोडणी येथे करा."
                          : "Manage your profile, UDISE code, active class info and add new classes here."));
                s.add(new TourStep(R.id.cardProfileInfo,
                        m ? "१. प्रोफाइल कार्ड" : "1. Profile Card",
                        m ? "आपले नाव, ईमेल, फोन नंबर व शाळेचा युडायस कोड येथे एका नजरेत दिसतो."
                          : "Your name, email, phone and school UDISE code shown at a glance."));
                s.add(new TourStep(R.id.btnEditProfile,
                        m ? "२. प्रोफाइल संपादित करा ✏️" : "2. Edit Profile ✏️",
                        m ? "नाव, फोन किंवा युडायस कोड बदलण्यासाठी 'संपादित करा' दाबा."
                          : "Tap Edit to update your name, phone number or UDISE code."));
                s.add(new TourStep(R.id.tvSummaryUdise,
                        m ? "३. युडायस कोड" : "3. UDISE Code",
                        m ? "शाळेचा ११-अंकी युडायस कोड येथे दिसतो. हा कोड अचूक असणे आवश्यक आहे."
                          : "Your school's 11-digit UDISE code appears here — keep it accurate."));
                s.add(new TourStep(R.id.cardActiveClassDetail,
                        m ? "४. सक्रिय वर्ग 🏫" : "4. Active Class 🏫",
                        m ? "सध्या सक्रिय केलेल्या वर्गाची माहिती — नाव, विद्यार्थी संख्या व तुकडी येथे दिसते."
                          : "Shows your currently active class — name, student count and division."));
                s.add(new TourStep(R.id.tvActiveClassStudents,
                        m ? "५. विद्यार्थी संख्या" : "5. Student Count",
                        m ? "सक्रिय वर्गातील एकूण नोंदणीकृत विद्यार्थ्यांची संख्या येथे दिसते."
                          : "Total registered students in the currently active class are shown here."));
                s.add(new TourStep(R.id.rvProfileClasses,
                        m ? "६. सर्व वर्गांची यादी 📋" : "6. All Classes List 📋",
                        m ? "शाळेतील सर्व वर्गांची यादी येथे दिसते. वर्गावर टॅप केल्यास तो सक्रिय होतो."
                          : "All classes in the school are listed here. Tap a class to make it active."));
                s.add(new TourStep(R.id.fabPromoteStudents,
                        m ? "७. विद्यार्थी वर्गोन्नती 🎓" : "7. Promote Students 🎓",
                        m ? "नवीन शैक्षणिक वर्षात किंवा तुकडीमध्ये एकाच वेळी अनेक विद्यार्थ्यांची वर्गोन्नती किंवा बदली करण्यासाठी येथे टॅप करा."
                          : "Tap here to batch promote or transfer students into a new academic year or division."));
                s.add(new TourStep(R.id.fabAddClass,
                        m ? "८. नवीन वर्ग जोडा +" : "8. Add New Class +",
                        m ? "'+' FAB दाबून नवीन वर्ग व तुकडी (उदा. ५-A) तयार करा. लगेच यादीत दिसेल."
                          : "Tap '+' to create a new class and division (e.g. Class 5-A). It appears instantly."));
                break;


            // ════════════════════════════════════════════════════════════
            // 13. App Settings  (7 steps)
            // ════════════════════════════════════════════════════════════
            case "settings":
                s.add(new TourStep(0,
                        m ? "⚙️ सेटिंग्ज" : "⚙️ App Settings",
                        m ? "भाषा, थीम, बॅकअप, कॅशे व सदस्यता सेटिंग्ज येथे बदला."
                          : "Change language, theme, backup, cache and subscription settings."));
                s.add(new TourStep(R.id.cardTheme,
                        m ? "१. 🌙 थीम" : "1. 🌙 App Theme",
                        m ? "सिस्टम, लाइट किंवा डार्क थीम निवडा. डार्क थीम डोळ्यांना आरामदायी."
                          : "Choose System, Light or Dark theme. Dark mode is easier on the eyes."));
                s.add(new TourStep(R.id.cardLanguage,
                        m ? "२. 🌐 भाषा" : "2. 🌐 Language",
                        m ? "मराठी किंवा इंग्रजी — पसंतीची भाषा निवडण्यासाठी येथे टॅप करा."
                          : "Tap to switch the app language between Marathi and English."));
                s.add(new TourStep(R.id.cardBackup,
                        m ? "३. 💾 बॅकअप" : "3. 💾 Backup & Restore",
                        m ? "डेटा सुरक्षित ठेवण्यासाठी नियमित बॅकअप घ्या. फोन बदलताना उपयुक्त."
                          : "Take regular backups to keep data safe — especially before changing phones."));
                s.add(new TourStep(R.id.btnExportBackup,
                        m ? "४. ⬆️ बॅकअप निर्यात करा" : "4. ⬆️ Export Backup",
                        m ? "'निर्यात' दाबल्यावर संपूर्ण डेटाचा .zip बॅकअप फाइल तयार होते."
                          : "Press Export to create a complete .zip backup file of all your data."));
                s.add(new TourStep(R.id.btnImportBackup,
                        m ? "५. ⬇️ बॅकअप आयात करा" : "5. ⬇️ Import Backup",
                        m ? "'आयात' दाबल्यावर जुना बॅकअप निवडा — डेटा पुनर्संचयित होईल."
                          : "Press Import and select a backup file to restore all your previous data."));
                s.add(new TourStep(R.id.cardCache,
                        m ? "६. 🗑️ कॅशे साफ करा" : "6. 🗑️ Clear Cache",
                        m ? "ॲप्लिकेशन मंद झाल्यास कॅशे साफ करा किंवा सक्रिय सत्र रीसेट करा."
                          : "If the app feels slow, clear cache or reset the active session here."));
                break;

            // ════════════════════════════════════════════════════════════
            // 14. Student Profile  (10 steps)
            // ════════════════════════════════════════════════════════════
            case "student_profile":
                s.add(new TourStep(0,
                        m ? "📋 विद्यार्थी प्रोफाइल" : "📋 Student Profile",
                        m ? "विद्यार्थ्याची संपूर्ण माहिती — मूलभूत, कुटुंब, बँक, शैक्षणिक."
                          : "Complete student details — basic, family, bank and academic info."));
                s.add(new TourStep(R.id.layoutHeaderBanner,
                        m ? "१. विद्यार्थी बॅनर" : "1. Student Banner",
                        m ? "विद्यार्थ्याचे नाव, वर्ग व नोंदणी क्रमांक येथे ठळकपणे दिसतो."
                          : "Student's name, class and registration number shown prominently."));
                s.add(new TourStep(R.id.ivStudentPhoto,
                        m ? "२. फोटो 📷" : "2. Photo 📷",
                        m ? "विद्यार्थ्याच्या फोटोवर टॅप करून नवीन फोटो जोडा किंवा बदला."
                          : "Tap the photo to add or change the student's profile picture."));
                s.add(new TourStep(R.id.cardStats,
                        m ? "३. त्वरित तपशील" : "3. Quick Stats",
                        m ? "रोल नं., जन्मतारीख, लिंग व जात प्रवर्ग एका नजरेत येथे दिसतात."
                          : "Roll number, DOB, gender and caste category shown at a glance."));
                s.add(new TourStep(R.id.btnNavBasic,
                        m ? "४. मूलभूत माहिती" : "4. Basic Details",
                        m ? "मूलभूत माहिती — नाव, रोल नं., जन्मतारीख, लिंग, जात पाहण्यासाठी टॅप करा."
                          : "Tap to view basic info — name, roll no., DOB, gender and caste."));
                s.add(new TourStep(R.id.btnNavFamily,
                        m ? "५. कुटुंब माहिती" : "5. Family Details",
                        m ? "वडील, आई, व्यवसाय, फोन व पत्ता पाहण्यासाठी 'कुटुंब' टॅप करा."
                          : "Tap Family to view father, mother, occupation, phone and address."));
                s.add(new TourStep(R.id.btnNavBank,
                        m ? "६. बँक माहिती" : "6. Bank Details",
                        m ? "बँक खाते, शाखा, IFSC व UID पाहण्यासाठी 'बँक' टॅप करा."
                          : "Tap Bank to see account number, branch, IFSC code and UID."));
                s.add(new TourStep(R.id.btnEditStudent,
                        m ? "८. माहिती संपादित करा ✏️" : "8. Edit Student ✏️",
                        m ? "'संपादित करा' दाबून विद्यार्थ्याची कोणतीही माहिती बदला."
                          : "Press Edit to update any field of this student's profile."));
                s.add(new TourStep(R.id.btnSpecialCard,
                        m ? "९. विशेष प्रगती पत्रक 📄" : "9. Special Progress Card 📄",
                        m ? "विशेष आणि अत्यंत आकर्षक डिझाईन असलेले प्रगती पत्रक PDF स्वरूपात तयार व डाऊनलोड करण्यासाठी येथे टॅप करा."
                          : "Tap to generate and view a beautifully styled personalized special progress card PDF."));
                break;

            // ════════════════════════════════════════════════════════════
            // 15. Student Add / Edit  (10 steps)
            // ════════════════════════════════════════════════════════════
            case "student_edit":
                s.add(new TourStep(0,
                        m ? "✏️ विद्यार्थी नोंदणी / संपादन" : "✏️ Student Register / Edit",
                        m ? "विद्यार्थ्याची नवीन नोंदणी करा किंवा विद्यमान माहिती अद्ययावत करा."
                          : "Register a new student or update an existing student's details."));
                s.add(new TourStep(R.id.etName,
                        m ? "१. विद्यार्थ्याचे नाव *" : "1. Student Name *",
                        m ? "पूर्ण नाव (आडनाव प्रथम) अचूक टाइप करा. हे अनिवार्य आहे."
                          : "Type full name (surname first) accurately. This is mandatory."));
                s.add(new TourStep(R.id.etRoll1,
                        m ? "२. रोल नंबर (वर्ग)" : "2. Class Roll Number",
                        m ? "विद्यार्थ्याचा वर्गातील रोल नंबर येथे भरा."
                          : "Enter the student's roll number within the class."));
                s.add(new TourStep(R.id.etRegNo,
                        m ? "३. नोंदणी क्रमांक" : "3. Registration Number",
                        m ? "शाळेचा अधिकृत नोंदणी / प्रवेश क्रमांक येथे भरा."
                          : "Enter the official school registration / admission number."));
                s.add(new TourStep(R.id.etDob,
                        m ? "४. जन्मतारीख 📅" : "4. Date of Birth 📅",
                        m ? "जन्मतारीख DD/MM/YYYY स्वरूपात भरा किंवा कॅलेंडरमधून निवडा."
                          : "Enter DOB in DD/MM/YYYY format or pick from the calendar."));
                s.add(new TourStep(R.id.etGender,
                        m ? "५. लिंग ▼" : "5. Gender ▼",
                        m ? "ड्रॉपडाउनमधून विद्यार्थ्याचे लिंग निवडा — मुलगा / मुलगी / इतर."
                          : "Select gender from the dropdown — Boy / Girl / Other."));
                s.add(new TourStep(R.id.etCast,
                        m ? "६. जात प्रवर्ग ▼" : "6. Caste Category ▼",
                        m ? "ड्रॉपडाउनमधून जात प्रवर्ग निवडा — सर्वसाधारण / OBC / SC / ST."
                          : "Select caste category from dropdown — General / OBC / SC / ST."));
                s.add(new TourStep(R.id.etFatherName,
                        m ? "७. वडिलांचे नाव" : "7. Father's Name",
                        m ? "विद्यार्थ्याच्या वडिलांचे पूर्ण नाव येथे भरा."
                          : "Enter the full name of the student's father here."));
                s.add(new TourStep(R.id.etMotherName,
                        m ? "८. आईचे नाव" : "8. Mother's Name",
                        m ? "विद्यार्थ्याच्या आईचे पूर्ण नाव येथे भरा."
                          : "Enter the full name of the student's mother here."));
                s.add(new TourStep(R.id.btnSaveStudent,
                        m ? "९. विद्यार्थी जतन करा 💾" : "9. Save Student 💾",
                        m ? "सर्व अनिवार्य माहिती भरल्यावर 'जतन करा' दाबा — डेटा सेव्ह होईल."
                          : "After filling all required fields, press Save to store student data."));
                break;

            // ════════════════════════════════════════════════════════════
            // 16. Enter Marks  (6 steps)
            // ════════════════════════════════════════════════════════════
            case "enter_marks":
                s.add(new TourStep(0,
                        m ? "🎯 गुण प्रविष्टी" : "🎯 Enter Marks",
                        m ? "विद्यार्थ्याचे विषयनिहाय आकारिक व संकलित गुण या स्क्रीनवर भरा."
                          : "Enter this student's subject-wise formative and summative marks."));
                s.add(new TourStep(R.id.cardStudentAvatarMarks,
                        m ? "१. विद्यार्थी माहिती" : "1. Student Info",
                        m ? "गुण भरत असलेल्या विद्यार्थ्याचे नाव, रोल नंबर व वर्ग येथे दिसतो."
                          : "Shows the name, roll number and class of the student you are marking."));
                s.add(new TourStep(R.id.btnScanMarksheet,
                        m ? "२. गुणपत्रिका स्कॅन 📸" : "2. Scan Marksheet 📸",
                        m ? "कागदी गुणपत्रिकेचा फोटो काढा — OCR द्वारे गुण आपोआप भरले जातात."
                          : "Photograph a printed marksheet — OCR auto-fills all subject marks."));
                s.add(new TourStep(R.id.cardMarksTable,
                        m ? "३. गुण तक्ता 📊" : "3. Marks Table 📊",
                        m ? "प्रत्येक विषयाच्या FA व SA उप-घटकांचे गुण येथे हाताने भरा."
                          : "Manually enter FA and SA sub-component marks for each subject."));
                s.add(new TourStep(R.id.btnSaveMarks,
                        m ? "४. गुण जतन करा 💾" : "4. Save Marks 💾",
                        m ? "सर्व गुण भरल्यावर 'जतन करा' दाबा. बदल क्लाउडवर सेव्ह होतात."
                          : "Press Save Marks — all entered data is saved to the cloud."));
                break;

            // ════════════════════════════════════════════════════════════
            // 17. Enter Descriptive Remarks (per student)  (5 steps)
            // ════════════════════════════════════════════════════════════
            case "enter_descriptive":
                s.add(new TourStep(0,
                        m ? "💬 विद्यार्थी शेरे" : "💬 Student Descriptive Remarks",
                        m ? "विद्यार्थ्याचे वर्तन, आवड व कौशल्यांचे शेरे येथे निवडा."
                          : "Select behavioral remarks and skill notes for this student."));
                s.add(new TourStep(R.id.cardStudentAvatarMarks,
                        m ? "१. विद्यार्थी माहिती" : "1. Student Info",
                        m ? "शेरे नोंदवत असलेल्या विद्यार्थ्याचे नाव, रोल नंबर व वर्ग येथे दिसते."
                          : "Displays the name, roll number and class of the student being edited."));
                s.add(new TourStep(R.id.llRemarkRows,
                        m ? "२. शेरा पर्याय 💭" : "2. Remark Options 💭",
                        m ? "प्रत्येक विषयासाठी शेरा पर्याय येथे दिसतात. चिप निवडा → शेरा जोडला जातो."
                          : "Remark chips are shown per subject. Tap a chip to select that remark."));
                s.add(new TourStep(R.id.btnSaveRemarks,
                        m ? "३. शेरे जतन करा 💾" : "3. Save Remarks 💾",
                        m ? "सर्व पसंतीचे शेरे निवडल्यावर 'जतन करा' दाबून क्लाउडवर सेव्ह करा."
                          : "After selecting all preferred remarks, press Save to store them."));
                break;

            // ════════════════════════════════════════════════════════════
            // Fallback
            // ════════════════════════════════════════════════════════════
            case "promote_students":
                s.add(new TourStep(0,
                        m ? "🎓 विद्यार्थी वर्गोन्नती आणि बदली" : "🎓 Student Promotion & Transfer",
                        m ? "येथे तुम्ही विद्यार्थ्यांना पुढील वर्गात वर्गोन्नती देऊ शकता किंवा तुकडी बदलू शकता."
                          : "Batch promote students into next classes or transfer divisions easily here."));
                s.add(new TourStep(R.id.spTargetYear,
                        m ? "१. शैक्षणिक वर्ष निवडा" : "1. Select Target Year",
                        m ? "विद्यार्थी ज्या नवीन शैक्षणिक वर्षात प्रमोट करायचे आहेत ते वर्ष निवडा."
                          : "Select the new target academic year for the promotion."));
                s.add(new TourStep(R.id.btnAddNewYear,
                        m ? "२. नवीन वर्ष जोडा +" : "2. Add New Year +",
                        m ? "नवीन वर्ष उपलब्ध नसेल तर येथे क्लिक करून ते वर्ष व सत्रे त्वरित तयार करा."
                          : "Tap here to register a new academic year and auto-generate its semesters."));
                s.add(new TourStep(R.id.spTargetClass,
                        m ? "३. नवीन इयत्ता निवडा" : "3. Select Target Class",
                        m ? "विद्यार्थ्यांना ज्या नवीन इयत्तेत पाठवायचे आहे ती निवडा (उदा. ७ वरून ८)."
                          : "Select the target class standard for the students."));
                s.add(new TourStep(R.id.spTargetDivision,
                        m ? "४. नवीन तुकडी निवडा" : "4. Select Target Division",
                        m ? "हवी असणारी नवीन तुकडी निवडा."
                          : "Select the target division for the students."));
                s.add(new TourStep(R.id.rgMode,
                        m ? "५. मोड निवडा (Promote/Transfer)" : "5. Choose Adjustment Mode",
                        m ? "नवीन वर्षासाठी 'Promote' (कॉपी) आणि चालू वर्षातील तुकडी बदलासाठी 'Transfer' (मूव्ह) निवडा."
                          : "Choose 'Promote' to copy student documents for a new year, or 'Transfer' to move current pointers."));
                s.add(new TourStep(R.id.cbSelectAll,
                        m ? "६. विद्यार्थी निवडा" : "6. Select Students",
                        m ? "ज्या विद्यार्थ्यांना प्रमोट करायचे आहे त्यांना चेक करा. सर्व निवडण्यासाठी 'Select All' वापरा."
                          : "Check the checkboxes for students to promote, or check 'Select All'."));
                s.add(new TourStep(R.id.btnProcessPromotion,
                        m ? "७. प्रक्रिया पूर्ण करा" : "7. Execute Adjustment",
                        m ? "निवडलेले विद्यार्थी नवीन वर्गात पाठवण्यासाठी हे बटण दाबा."
                          : "Press this button to commit the promotion or transfer process."));
                break;

            default:
                s.add(new TourStep(0,
                        m ? "ℹ️ या पानाबद्दल" : "ℹ️ About This Page",
                        m ? "या पानावरील माहिती व्यवस्थापित करण्यासाठी वरील बटणे वापरा."
                          : "Use the buttons above to manage the features shown on this page."));
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
