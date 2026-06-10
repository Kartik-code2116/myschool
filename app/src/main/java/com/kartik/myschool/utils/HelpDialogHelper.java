package com.kartik.myschool.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.kartik.myschool.R;
import java.util.Locale;

public class HelpDialogHelper {

    public static void showHelpDialog(Context context, String pageKey) {
        if (context == null) return;
        boolean isMarathi = Locale.getDefault().getLanguage().equals("mr");

        String title;
        String message;

        switch (pageKey != null ? pageKey : "default") {
            case "info_print":
                title = isMarathi ? "माहिती आणि प्रिंट सेटिंग :" : "Info & Print Setting :";
                message = isMarathi
                        ? "❤️ येथे माहिती भरण्यासाठीचे व प्रिंट काढण्यासाठी वर्ष, सत्र, इयत्ता निवडून सेव करण्याचे सेटिंग दिले आहे.\n\n"
                        + "📌 मूल्यमापन वर्ष बदलण्यासाठी येथे दिसणाऱ्या वर्ष वर टॅप करावे.\n\n"
                        + "📌 हवे असणारे सत्र ड्रॅग करून निवडा.\n\n"
                        + "📌 हवी असणारी तुकडी ड्रॅग करून निवडा.\n\n"
                        + "📌 टूलबारवरील 💾 बटन टॅप करा.\n\n"
                        + "📌 ही सेटिंग सेव झाल्याचा मेसेज येईल.\n\n"
                        + "📌 आता या सत्रातील निवडलेल्या तुकडीची माहिती भरता/पीडीएफ करता येईल."
                        : "❤️ This page allows configuring the academic year, semester, and standard/division properties.\n\n"
                        + "📌 To change the academic year, tap on the year selector at the top.\n\n"
                        + "📌 Select your active semester and division.\n\n"
                        + "📌 Tap the 💾 button in the toolbar to save the settings.\n\n"
                        + "📌 A confirmation message will appear once settings are saved.\n\n"
                        + "📌 You can now record marks and export student reports for this class.";
                break;

            case "stats_dashboard":
                title = isMarathi ? "सांख्यिकी डॅशबोर्ड :" : "Stats Dashboard :";
                message = isMarathi
                        ? "❤️ शाळेची आणि वर्गाची आकडेवारी आलेख स्वरूपात पहा.\n\n"
                        + "📌 एकूण नोंदणी आणि विद्यार्थ्यांचे संख्यात्मक वर्गीकरण येथे दिसते.\n\n"
                        + "📌 वर्गनिहाय मुले-मुलींचे प्रमाण (लिंग गुणोत्तर) चार्टमध्ये पहा.\n\n"
                        + "📌 वेगवेगळ्या जात प्रवर्गनिहाय विद्यार्थी वाटप समजून घ्या."
                        : "❤️ View graphical statistics of the school and classes.\n\n"
                        + "📌 Displays total student enrollment across standards.\n\n"
                        + "📌 Analyze boys-to-girls ratio in real-time class charts.\n\n"
                        + "📌 Understand the distribution of students across different caste categories.";
                break;

            case "class_div":
                title = isMarathi ? "वर्ग आणि तुकडी सेटअप :" : "Class & Division Setup :";
                message = isMarathi
                        ? "❤️ तुमच्या शाळेतील सर्व उपलब्ध वर्ग आणि तुकड्यांची यादी व्यवस्थापित करा.\n\n"
                        + "📌 नवीन वर्ग जोडण्यासाठी खालील 'Add Class' वर टॅप करा.\n\n"
                        + "📌 एखाद्या वर्गाचे नाव सुधारण्यासाठी किंवा डिलिट करण्यासाठी ३-बिंदू चिन्हावर क्लिक करा.\n\n"
                        + "📌 वर्गात नवीन विद्यार्थी थेट येथूनही जोडता येतात."
                        : "❤️ Manage the list of all classes and divisions in your school.\n\n"
                        + "📌 Tap 'Add Class' to create a new standard and division.\n\n"
                        + "📌 Click the 3-dots option to edit standard info or delete a class.\n\n"
                        + "📌 Assign teachers and quickly view class size metrics.";
                break;

            case "students":
                title = isMarathi ? "विद्यार्थ्यांची यादी :" : "Student List :";
                message = isMarathi
                        ? "❤️ तुमच्या वर्गातील सर्व विद्यार्थ्यांची यादी पहा व व्यवस्थापित करा.\n\n"
                        + "📌 नवीन विद्यार्थी जोडण्यासाठी खालील '+' बटणावर क्लिक करा.\n\n"
                        + "📌 शोध घेण्यासाठी वर असलेल्या सर्च बारचा वापर करा.\n\n"
                        + "📌 विद्यार्थ्याचे गुण किंवा हजेरी भरण्यासाठी उजव्या बाजूच्या ३-बिंदू मेनूवर क्लिक करा.\n\n"
                        + "📌 एक्सेल (Excel) चिन्हावर क्लिक करून विद्यार्थ्यांचा डेटा आयात (Import) किंवा निर्यात (Export) करा."
                        : "❤️ View and manage all students enrolled in the active class.\n\n"
                        + "📌 Click the '+' button in the bottom right corner to add a new student.\n\n"
                        + "📌 Use the top search bar to find students by name or roll number.\n\n"
                        + "📌 Tap the 3-dots menu on a student card to enter marks, attendance, or edit info.\n\n"
                        + "📌 Tap the Excel icon in the toolbar to import or export student profiles.";
                break;

            case "formative_summative":
                title = isMarathi ? "मूल्यमापन नोंदणी :" : "Evaluation Entry :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्यांचे विषयनिहाय घटक आणि गुण प्रविष्ट करा.\n\n"
                        + "📌 गुणांची नोंद करण्यासाठी विद्यार्थ्याच्या कार्डावर किंवा ३-बिंदू मेनूवर क्लिक करा.\n\n"
                        + "📌 आकारिक आणि संकलित मूल्यमापनाचे गुण वेगळे भरता येतात.\n\n"
                        + "📌 वरील ग्रीड/लिस्ट बटणाद्वारे मांडणी बदलू शकता."
                        : "❤️ Enter and review subject-wise formative and summative marks.\n\n"
                        + "📌 Click on a student's card or use the 3-dots menu to edit subject marks.\n\n"
                        + "📌 Record scores for formative components and written summative tests.\n\n"
                        + "📌 Use the toggle button at the top to switch between list and grid layouts.";
                break;

            case "descriptive":
                title = isMarathi ? "वर्णनात्मक नोंदी शेरे :" : "Descriptive Remarks :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्यांच्या विषयानुरूप प्रगती आणि वर्णनात्मक नोंदी निवडा.\n\n"
                        + "📌 पूर्वनिर्धारित यादीमधून योग्य शेरे निवडा किंवा स्वतःच्या आवडीनुसार बदल करा.\n\n"
                        + "📌 विद्यार्थ्याच्या विशेष आवडी आणि छंद देखील येथे जतन करा.\n\n"
                        + "📌 ३-बिंदू मेनू वापरून जतन केलेले शेरे एकाच वेळी डिलीट किंवा बदलू शकता."
                        : "❤️ Record student progress and qualitative descriptive remarks.\n\n"
                        + "📌 Choose remarks from predefined templates or type customized entries.\n\n"
                        + "📌 Manage special interests, hobby descriptions, and progress comments.\n\n"
                        + "📌 Use the 3-dots menu to bulk delete or adjust gender pronouns in sentences.";
                break;

            case "attendance":
                title = isMarathi ? "मासिक उपस्थिती :" : "Monthly Attendance :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्यांची प्रत्येक महिन्याची हजेरी नोंदवा.\n\n"
                        + "📌 महिना निवडा, एकूण कामकाजाचे दिवस आणि विद्यार्थ्याचे हजर दिवस प्रविष्ट करा.\n\n"
                        + "📌 ३-बिंदू वर क्लिक करून डुप्लिकेट किंवा डिलीट पर्याय निवडू शकता.\n\n"
                        + "📌 वर्गाचा एकूण हजेरी अहवाल आणि सर्वाधिक उपस्थिती असलेला विद्यार्थी पाहण्यासाठी रिपोर्ट आयकॉन दाबा."
                        : "❤️ Record student attendance for each month of the academic year.\n\n"
                        + "📌 Select a month, set total working days, and enter present days for each student.\n\n"
                        + "📌 Click the 3-dots option to duplicate or delete attendance records.\n\n"
                        + "📌 Tap the Report icon to view average class attendance and find best attender.";
                break;

            case "subjects":
                title = isMarathi ? "विषय व्यवस्थापन :" : "Subject Management :";
                message = isMarathi
                        ? "❤️ तुमच्या वर्गात शिकवले जाणारे विषय व्यवस्थापित करा.\n\n"
                        + "📌 नवीन विषय जोडा किंवा जुने विषय दुरुस्त/हटवा.\n\n"
                        + "📌 प्रगतीपत्रकात गुण योग्य रीतीने दिसण्यासाठी कमाल गुण तपासा."
                        : "❤️ Manage the subjects assigned to the active class.\n\n"
                        + "📌 Add new subjects, edit subject properties, or remove unused ones.\n\n"
                        + "📌 Ensure max marks are correctly configured for proper grading.";
                break;

            case "weightage":
                title = isMarathi ? "गुण भारांश घोषित करा :" : "Declare Weightage :";
                message = isMarathi
                        ? "❤️ प्रत्येक विषयासाठी कमाल गुण आणि अंतर्गत उप-घटकांचे गुण घोषित करा.\n\n"
                        + "📌 आकारिक आणि संकलित मूल्यमापन चाचण्यांसाठी स्वतंत्र भारांश ठरवा.\n\n"
                        + "📌 अंतर्गत परीक्षांचे गुण विभागणी प्रमाण येथे बदलता येते."
                        : "❤️ Set max marks and component weightage for formative and summative tests.\n\n"
                        + "📌 Configures how total subject marks are distributed on the report card.\n\n"
                        + "📌 Configure individual component weightage (oral, project, written, etc.) per subject.";
                break;

            case "school_settings":
                title = isMarathi ? "शाळा सेटिंग्ज :" : "School Settings :";
                message = isMarathi
                        ? "❤️ शालेय पातळीवरील विविध सेटिंग्ज आणि माहिती येथे सुधारता येते.\n\n"
                        + "📌 युडायस कोड आधारे शाळेची सांख्यिकी, लिंग गुणोत्तर, जात प्रवर्ग आणि वर्ग शिक्षक पहा.\n\n"
                        + "📌 कामाचे दिवस आणि डीफॉल्ट सेटिंग्ज वर्गांसाठी येथे सेट करा."
                        : "❤️ View and configure school-level statistics and settings.\n\n"
                        + "📌 Displays school-wide totals, gender ratios, caste distribution, and teacher details.\n\n"
                        + "📌 Set working days, default values, and parameter limits across the school.";
                break;

            case "print_report":
                title = isMarathi ? "गुणपत्रक प्रिंट अहवाल :" : "Print Report / Marksheet :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्यांचे अधिकृत प्रगतीपत्रक (गुणपत्रक) तयार करा.\n\n"
                        + "📌 प्रगतीपत्रक पीडीएफ स्वरूपात सेव्ह करा, प्रिंट करा किंवा शेअर करा.\n\n"
                        + "📌 दोन्ही सत्रांचे गुण एकत्रित तपासून अंतिम गुणपत्रिका मिळवा."
                        : "❤️ Generate official report cards (Gunapattrak) for students.\n\n"
                        + "📌 Preview, print, save as PDF, or share the report cards directly.\n\n"
                        + "📌 Consolidates first and second semester records into a final progress sheet.";
                break;

            case "profile":
                title = isMarathi ? "शिक्षकाची प्रोफाइल :" : "Teacher Profile :";
                message = isMarathi
                        ? "❤️ तुमचे शिक्षक प्रोफाइल आणि शाळा जोडणीचे तपशील पहा.\n\n"
                        + "📌 युडायस कोड (UDISE), शाळेचे नाव आणि चालू सत्र दुरुस्त करा.\n\n"
                        + "📌 तुमचे संपर्क तपशील येथे अद्ययावत ठेवा."
                        : "❤️ View and edit teacher profile details and school association.\n\n"
                        + "📌 Configure your UDISE code, school info, and academic year settings.\n\n"
                        + "📌 Ensure your personal and contact details are kept up to date.";
                break;

            case "settings":
                title = isMarathi ? "सेटिंग्ज :" : "Settings :";
                message = isMarathi
                        ? "❤️ ॲप्लिकेशनची भाषा मराठी/इंग्रजीमध्ये बदला.\n\n"
                        + "📌 डेटा सुरक्षित ठेवण्यासाठी बॅकअप घ्या किंवा रिस्टोर करा.\n\n"
                        + "📌 नवीन अपडेट्स तपासा किंवा तांत्रिक मदत मिळवा."
                        : "❤️ Configure app settings, switch display language, and manage account options.\n\n"
                        + "📌 Access database backup, restore, and synchronization tools.\n\n"
                        + "📌 Perform data maintenance tasks or contact developer support.";
                break;

            case "student_profile":
                title = isMarathi ? "विद्यार्थी प्रोफाइल :" : "Student Profile :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्याचे मूलभूत तपशील, कौटुंबिक माहिती, बँक खाते आणि शैक्षणिक आयडी येथे पहा.\n\n"
                        + "📌 बदल करण्यासाठी 'विद्यार्थी संपादित करा' बटणावर क्लिक करा.\n\n"
                        + "📌 प्रगतीपत्रक किंवा गुण नोंदवण्यासाठी 'गुण प्रविष्ट करा' निवडा."
                        : "❤️ View detailed profile info including basic, family, bank, and academic details.\n\n"
                        + "📌 Tap 'Edit Student' to make changes or update details.\n\n"
                        + "📌 Click 'Enter Marks' or 'View Marksheet' to record grades or preview reports.";
                break;

            case "student_edit":
                title = isMarathi ? "माहिती संपादन :" : "Edit Student Info :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्याची नवीन नोंदणी करा किंवा माहिती अद्ययावत करा.\n\n"
                        + "📌 नाव, रोल नंबर, लिंग, जन्मतारीख आणि बँक खात्याची माहिती अचूक भरा.\n\n"
                        + "📌 बदल जतन करण्यासाठी 'विद्यार्थी जतन करा' (Save) वर क्लिक करा."
                        : "❤️ Form to add a new student or edit details of an existing one.\n\n"
                        + "📌 Fill in basic, parent, bank, and academic info carefully.\n\n"
                        + "📌 Press 'Save Student' to commit changes to Firestore database.";
                break;

            case "enter_marks":
                title = isMarathi ? "गुण प्रविष्ट करा :" : "Enter Marks :";
                message = isMarathi
                        ? "❤️ या स्क्रीनवर विद्यार्थ्याचे आकारिक (तोंडी, प्रात्यक्षिक इ.) आणि संकलित गुण भरा.\n\n"
                        + "📌 प्रगत स्कॅनरद्वारे गुणपत्रिका थेट स्कॅन करण्यासाठी 'Scan' वर टॅप करा.\n\n"
                        + "📌 गुण भरून झाल्यावर 'गुण जतन करा' वर क्लिक करा."
                        : "❤️ Record formative and summative marks for this student.\n\n"
                        + "📌 Use the camera/gallery scanner to scan marksheets automatically.\n\n"
                        + "📌 Click 'Save Marks' once finished to update records.";
                break;

            case "enter_descriptive":
                title = isMarathi ? "वर्णनात्मक नोंदी शेरे :" : "Enter Descriptive Remarks :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्याचे वर्तन, आवड आणि संपादणूक शेरे निवडा.\n\n"
                        + "📌 पूर्वनिर्धारित शेरे निवडा किंवा स्वतःचा कीबोर्ड वापरून कस्टम शेरा प्रविष्ट करा.\n\n"
                        + "📌 बदल जतन करण्यासाठी 'Save Remarks' दाबा."
                        : "❤️ Record student qualities, special interests, and qualitative descriptive remarks.\n\n"
                        + "📌 Choose from templates or enter custom text remarks.\n\n"
                        + "📌 Tap 'Save Remarks' to save remarks to Firestore.";
                break;

            default:
                title = isMarathi ? "मदत :" : "Help :";
                message = isMarathi
                        ? "या स्क्रीनबद्दल अधिक माहिती मिळवण्यासाठी तुमच्या ॲडमिनिस्ट्रेटरशी संपर्क साधा."
                        : "For more details on how to use this page, please contact the administrator.";
                break;
        }

        // Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_help, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvHelpTitle);
        TextView tvContent = dialogView.findViewById(R.id.tvHelpContent);
        View btnClose = dialogView.findViewById(R.id.btnHelpClose);
        View layoutWatchVideo = dialogView.findViewById(R.id.layoutWatchVideo);

        tvTitle.setText(title);
        tvContent.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        // Make background transparent to show rounded corners of MaterialCardView
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        final String finalPageKey = pageKey;
        layoutWatchVideo.setOnClickListener(v -> {
            dialog.dismiss();
            Activity activity = getActivity(context);
            if (activity != null) {
                ProductTourHelper.startTour(activity, finalPageKey);
            }
        });

        dialog.show();
    }

    /** Safely unwrap a Context to its parent Activity. */
    private static Activity getActivity(Context context) {
        if (context instanceof Activity) return (Activity) context;
        if (context instanceof ContextWrapper)
            return getActivity(((ContextWrapper) context).getBaseContext());
        return null;
    }
}
