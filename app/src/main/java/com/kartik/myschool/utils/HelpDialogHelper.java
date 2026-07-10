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
        android.content.SharedPreferences prefs = context.getSharedPreferences("myschool_settings_prefs", Context.MODE_PRIVATE);
        boolean isMarathi = prefs.getString("language", "mr").equals("mr");

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
                        + "📌 एक्सेल (Excel) चिन्हावर क्लिक करून विद्यार्थ्यांचा डेटा आयात (Import) किंवा निर्यात (Export) करा.\n\n"
                        + "📌 वर्गाचे आलेख आणि सांख्यिकी पाहण्यासाठी टूलबारमधील चार्ट (📊) चिन्हावर टॅप करा."
                        : "❤️ View and manage all students enrolled in the active class.\n\n"
                        + "📌 Click the '+' button in the bottom right corner to add a new student.\n\n"
                        + "📌 Use the top search bar to find students by name or roll number.\n\n"
                        + "📌 Tap the 3-dots menu on a student card to enter marks, attendance, or edit info.\n\n"
                        + "📌 Tap the Excel icon in the toolbar to import or export student profiles.\n\n"
                        + "📌 Tap the Chart (📊) icon in the toolbar to view class statistics and charts.";
                break;

            case "formative_summative":
                title = isMarathi ? "मूल्यमापन नोंदणी :" : "Evaluation Entry :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्यांचे विषयनिहाय आकारिक आणि संकलित गुण प्रविष्ट करा.\n\n"
                        + "📌 शोध (🔍) चिन्हाचा वापर करून विशिष्ट विद्यार्थी त्वरित शोधा.\n\n"
                        + "📌 गुणांची बेरीज तपासण्यासाठी कॅल्क्युलेटर (🧮) वापरा.\n\n"
                        + "📌 गुणांची नोंद करण्यासाठी विद्यार्थ्याच्या कार्डावर किंवा ३-बिंदू मेनूवर क्लिक करा.\n\n"
                        + "📌 वरील 'ग्रीड/लिस्ट' (🔄) बटणाद्वारे मांडणी बदलू शकता."
                        : "❤️ Enter and review subject-wise formative and summative marks.\n\n"
                        + "📌 Use the Search (🔍) icon to quickly find a specific student.\n\n"
                        + "📌 Use the Calculator (🧮) to verify marks addition.\n\n"
                        + "📌 Click on a student's card or use the 3-dots menu to edit subject marks.\n\n"
                        + "📌 Use the toggle button (🔄) at the top to switch between list and grid layouts.";
                break;

            case "descriptive":
                title = isMarathi ? "वर्णनात्मक नोंदी शेरे :" : "Descriptive Remarks :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्यांच्या विषयानुरूप प्रगती आणि वर्णनात्मक नोंदी निवडा.\n\n"
                        + "📌 शोध (🔍) चिन्हाचा वापर करून विशिष्ट विद्यार्थी त्वरित शोधा.\n\n"
                        + "📌 पूर्वनिर्धारित यादीमधून योग्य शेरे निवडा किंवा स्वतःच्या आवडीनुसार बदल करा.\n\n"
                        + "📌 वरील 'ग्रीड/लिस्ट' (🔄) बटणाद्वारे मांडणी बदलू शकता.\n\n"
                        + "📌 ३-बिंदू मेनू वापरून जतन केलेले शेरे एकाच वेळी डिलीट किंवा तो/ती (लिंग) बदलू शकता."
                        : "❤️ Record student progress and qualitative descriptive remarks.\n\n"
                        + "📌 Use the Search (🔍) icon to quickly find a specific student.\n\n"
                        + "📌 Choose remarks from predefined templates or type customized entries.\n\n"
                        + "📌 Use the toggle button (🔄) at the top to switch between list and grid layouts.\n\n"
                        + "📌 Use the 3-dots menu to bulk delete or adjust gender pronouns in sentences.";
                break;

            case "attendance":
                title = isMarathi ? "मासिक उपस्थिती :" : "Monthly Attendance :";
                message = isMarathi
                        ? "❤️ विद्यार्थ्यांची प्रत्येक महिन्याची हजेरी नोंदवा.\n\n"
                        + "📌 विद्यार्थी जोडण्यासाठी किंवा यादी पाहण्यासाठी '+' चिन्हावर क्लिक करा.\n\n"
                        + "📌 शोध (🔍) चिन्हाचा वापर करून विशिष्ट विद्यार्थी त्वरित शोधा.\n\n"
                        + "📌 प्रत्येक विद्यार्थ्याच्या बॉक्समध्ये डावीकडे एकूण 'हजर दिवस' (हिरवा आकडा) दिसतो.\n\n"
                        + "📌 बॉक्समधील तक्त्यात प्रत्येक महिन्याचे 'हजर/एकूण' (उदा. 24/26) दिवस दिसतात.\n\n"
                        + "📌 प्रत्येक विद्यार्थ्याच्या बॉक्समधील ३-बिंदू (⋮) वर क्लिक करून डुप्लिकेट किंवा डिलीट पर्याय निवडू शकता.\n\n"
                        + "📌 वर्गाचा एकूण हजेरी अहवाल पाहण्यासाठी 'कॅल्क्युलेटर' (🧮) दाबा."
                        : "❤️ Record student attendance for each month of the academic year.\n\n"
                        + "📌 Tap the '+' icon to go to the Student List page to add or manage student details.\n\n"
                        + "📌 Use the Search (🔍) icon to filter for a specific student.\n\n"
                        + "📌 Each student box displays the total 'Present Days' on the left.\n\n"
                        + "📌 The grid inside the box shows 'Present/Total' days (e.g., 24/26) for each month.\n\n"
                        + "📌 Click the 3-dots (⋮) option on a student card to duplicate or delete attendance records.\n\n"
                        + "📌 Tap the Calculator (🧮) icon to view the class attendance report.";
                break;

            case "subjects":
                title = isMarathi ? "विषय व्यवस्थापन :" : "Subject Management :";
                message = isMarathi
                        ? "❤️ तुमच्या वर्गात शिकवले जाणारे विषय व्यवस्थापित करा.\n\n"
                        + "📌 नवीन विषय जोडा किंवा जुने विषय दुरुस्त/हटवा.\n\n"
                        + "📌 प्रगतीपत्रकात गुण योग्य रीतीने दिसण्यासाठी कमाल गुण तपासा.\n\n"
                        + "📌 कोडचा पहिला भाग इयत्ता (उदा. 101) आणि दुसरा भाग विषय (उदा. 101) दर्शवतो. तुम्ही विषयावर क्लिक करून स्वतःचा कस्टम कोड देखील सेट करू शकता."
                        : "❤️ Manage the subjects assigned to the active class.\n\n"
                        + "📌 Add new subjects, edit subject properties, or remove unused ones.\n\n"
                        + "📌 Ensure max marks are correctly configured for proper grading.\n\n"
                        + "📌 The first part of the code represents the standard (e.g. 101) and the second part represents the subject (e.g. 101). You can also edit a subject to set your own custom code.";
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
                        ? "❤️ विद्यार्थ्यांचे अधिकृत प्रगतीपत्रक (गुणपत्रक) व इतर अहवाल पीडीएफ स्वरूपात तयार करा.\n\n"
                        + "📌 विशिष्ट अहवालाची PDF काढण्यासाठी 'प्रिंट' (🖨️) चिन्हावर टॅप करा.\n\n"
                        + "📌 अतिरिक्त माहिती भरण्यासाठी 'सेटिंग्ज' (⚙️) चिन्हावर टॅप करा.\n\n"
                        + "१. मुखपृष्ठ : प्रगतीपुस्तकाचे मुख्य कव्हर पेज.\n"
                        + "२. अनुक्रमणिका : वर्गातील विद्यार्थ्यांची यादी व इंडेक्स.\n"
                        + "३. गुणनोंदी : विषयनिहाय विद्यार्थ्यांचे गुण.\n"
                        + "४. वर्णनात्मक नोंदी : विद्यार्थ्यांचे शेरे व नोंदी.\n"
                        + "५. श्रेणी तक्का : गुण व श्रेणी यांचे कोष्टक.\n"
                        + "६. सर्वसामावेशक निकाल : सर्वांगीण मूल्यमापन तक्ता.\n"
                        + "७. श्रेणी तक्का (Roster) : वर्गाचा एकत्रित श्रेणी तक्ता.\n"
                        + "८. गुण-श्रेणीयुक्त निकालपत्रक : गुण व श्रेणीसह निकाल.\n"
                        + "९. प्रगतीपत्रक मुखपृष्ठ : प्रगतीपत्रकाचे फ्रंट पेज.\n"
                        + "१०. प्रगतीपत्रक पृष्ठ : विद्यार्थ्यांचे गुणपत्रक व शेरे.\n"
                        + "११. उपयुक्त रिपोर्ट : इतर शालेय कामासाठी रिपोर्ट्स.\n"
                        + "१२. पाचवी आठवी गुणपत्रक : ५ वी व ८ वी चे विशेष निकालपत्रक.\n"
                        + "१३. पाचवी आठवी वार्षिक तक्के : ५ वी व ८ वी वार्षिक तक्ते.\n"
                        + "१४. प्रगतीपत्रक मुखपृष्ठ : वार्षिक कव्हर पेज.\n"
                        + "१५. वार्षिक निकालपत्रक : संपूर्ण वर्षाचा एकत्रित निकाल.\n"
                        + "१६. वार्षिक निकालपत्रक (Alternative) : दुसरा फॉरमॅट.\n"
                        + "१७. जात श्रेणी तक्ता : जातनिहाय विद्यार्थी श्रेणी तक्ता.\n"
                        + "१८. प्रगतीपत्रक मुखपृष्ठ : प्रगतीपत्रक (इतर फॉरमॅट)."
                        : "❤️ Generate official report cards and PDFs.\n\n"
                        + "📌 Tap the 'Print' (🖨️) icon next to any report to instantly generate the PDF.\n\n"
                        + "📌 Tap the 'Settings' (⚙️) icon to configure custom text for that report.\n\n"
                        + "1. Cover Page : Progress book cover.\n"
                        + "2. Index : Student roster index.\n"
                        + "3. Marks Register : Subject-wise marks.\n"
                        + "4. Descriptive Entries : Remarks and entries.\n"
                        + "5. Grade Table : Grades lookup table.\n"
                        + "6. Comprehensive Result : Overall evaluation.\n"
                        + "7. Roster Grade Table : Class-wide grades.\n"
                        + "8. Marks-Grade Ledger : Ledger with marks and grades.\n"
                        + "9. Progress Card Cover : Progress card front.\n"
                        + "10. Progress Card Inner : Marksheet and remarks.\n"
                        + "11. Useful Reports : Reports for administrative use.\n"
                        + "12. 5th & 8th Marksheet : Specific marksheets for 5th/8th.\n"
                        + "13. 5th & 8th Annual Tables : Annual tables for 5th/8th.\n"
                        + "14. Progress Card Cover : Annual cover page.\n"
                        + "15. Annual Result Ledger : Complete yearly ledger.\n"
                        + "16. Annual Marksheet : Alternative format marksheet.\n"
                        + "17. Caste Grade Table : Caste-wise grades table.\n"
                        + "18. Progress Card Cover : Progress card (alternative).";
                break;

            case "profile":
                title = isMarathi ? "शिक्षकाची प्रोफाइल :" : "Teacher Profile :";
                message = isMarathi
                        ? "❤️ तुमचे शिक्षक प्रोफाइल आणि शाळा जोडणीचे तपशील पहा.\n\n"
                        + "📌 युडायस कोड (UDISE), शाळेचे नाव आणि चालू सत्र दुरुस्त करा.\n\n"
                        + "📌 तुमचे संपर्क तपशील येथे अद्ययावत ठेवा.\n\n"
                        + "📌 नवीन शैक्षणिक वर्षात किंवा तुकडीमध्ये विद्यार्थी वर्गोन्नती/बदलासाठी 'Promote Students' वर टॅप करा."
                        : "❤️ View and edit teacher profile details and school association.\n\n"
                        + "📌 Configure your UDISE code, school info, and academic year settings.\n\n"
                        + "📌 Ensure your personal and contact details are kept up to date.\n\n"
                        + "📌 Tap 'Promote Students' to batch promote/transfer students into a new academic year or division.";
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

            case "promote_students":
                title = isMarathi ? "विद्यार्थी वर्गोन्नती आणि बदली :" : "Student Promotion & Transfer :";
                message = isMarathi
                        ? "❤️ एकाच वेळी अनेक विद्यार्थ्यांची पुढील वर्गात वर्गोन्नती किंवा बदली करा.\n\n"
                        + "📌 चालू वर्ग आणि तुकडीची माहिती वरील पहिल्या कार्डमध्ये दिसेल.\n\n"
                        + "📌 नवीन शैक्षणिक वर्ष (उदा. 2027-28), इयत्ता आणि तुकडी निवडा.\n\n"
                        + "📌 नवीन वर्ष उपलब्ध नसेल तर '+' दाबून त्वरित नवीन वर्ष तयार करा.\n\n"
                        + "📌 ॲडजस्टमेंट मोड निवडा: 'वर्गोन्नती' (विद्यार्थ्याची नवीन कॉपी तयार करणे) किंवा 'बदली' (चालू विद्यार्थ्याचा वर्ग बदलणे).\n\n"
                        + "📌 विद्यार्थ्यांची नावे निवडा आणि 'Execute Adjustment' बटण दाबा."
                        : "❤️ Batch promote or transfer multiple students into another class at once.\n\n"
                        + "📌 The top card displays the active previous class, standard, and division.\n\n"
                        + "📌 Select the new target Academic Year, Class standard, and Division.\n\n"
                        + "📌 Tap the '+' icon to easily create/register a new academic year on the fly.\n\n"
                        + "📌 Choose an Adjustment Mode: 'Promote (Copy)' to copy profiles to the new year, or 'Transfer (Move)' to update class pointers for division transfers.\n\n"
                        + "📌 Check/select the students in the roster checklist, and tap 'Execute Adjustment'.";
                break;

            case "attendance_report":
                title = isMarathi ? "हजेरी अहवाल :" : "Attendance Report :";
                message = isMarathi
                        ? "❤️ संपूर्ण वर्गाचा मासिक व वार्षिक हजेरी अहवाल एकाच ठिकाणी पहा.\n\n"
                        + "📌 एकूण विद्यार्थी संख्या आणि वर्गाची सरासरी उपस्थिती टक्केवारी येथे दिसते.\n\n"
                        + "📌 वर्षभरातील एकूण हजर दिवस आणि एकूण कामकाजाचे दिवस यांचे प्रमाण तपासा.\n\n"
                        + "📌 सर्वाधिक उपस्थिती असणारा विद्यार्थी ठळकपणे दिसतो.\n\n"
                        + "📌 सर्व विद्यार्थ्यांची यादी त्यांच्या उपस्थितीच्या उतरत्या क्रमाने दिसते."
                        : "❤️ View class-wide attendance summary, totals, and percentages.\n\n"
                        + "📌 Displays total student count and overall class attendance percentage.\n\n"
                        + "📌 Compare total present days against total working days.\n\n"
                        + "📌 See the student with the best attendance record.\n\n"
                        + "📌 List of all students sorted by their attendance percentage.";
                break;

            case "evaluation_report":
                title = isMarathi ? "गुण अहवाल :" : "Marks Report :";
                message = isMarathi
                        ? "❤️ आकारिक आणि संकलित मूल्यमापनाचे विषयनिहाय एकूण गुण आणि टक्केवारी अहवाल पहा.\n\n"
                        + "📌 वर्गाची सरासरी श्रेणी (Grade) आणि एकूण सरासरी टक्केवारी येथे दिसते.\n\n"
                        + "📌 उत्तीर्ण आणि सुधारणा आवश्यक (Needs Improvement) विद्यार्थ्यांचे प्रमाण तपासा.\n\n"
                        + "📌 वर्गातील प्रथम क्रमांकाचा (Top Scorer) विद्यार्थी पहा.\n\n"
                        + "📌 सर्व विद्यार्थ्यांची यादी त्यांच्या एकूण गुणांच्या टक्केवारीच्या उतरत्या क्रमाने पहा."
                        : "❤️ View subject-wise overall formative and summative marks report.\n\n"
                        + "📌 Displays class average grade and class overall passing percentage.\n\n"
                        + "📌 View pass and fail (needs improvement) student counts.\n\n"
                        + "📌 Find the top-scoring student in the class.\n\n"
                        + "📌 List of all students sorted by their final total percentage.";
                break;

            case "descriptive_report":
                title = isMarathi ? "वर्णनात्मक नोंदी अहवाल :" : "Descriptive Remarks Report :";
                message = isMarathi
                        ? "❤️ वर्गातील विद्यार्थ्यांच्या वर्णनात्मक नोंदी पूर्ण झाल्याची सद्यस्थिती पहा.\n\n"
                        + "📌 एकूण विद्यार्थी आणि वर्गातील एकूण विषय संख्या एका दृष्टीक्षेपात तपासा.\n\n"
                        + "📌 आतापर्यंत भरलेल्या एकूण नोंदींचे प्रमाण आणि टक्केवारी पहा.\n\n"
                        + "📌 सर्वाधिक नोंदी पूर्ण झालेला विद्यार्थी पहा.\n\n"
                        + "📌 प्रत्येक विद्यार्थ्याच्या किती नोंदी पूर्ण झाल्या आहेत याची यादी पहा (उदा. 7/10 विषय पूर्ण)."
                        : "❤️ Check completion status of qualitative remarks and descriptors.\n\n"
                        + "📌 View the total number of students and total subjects in the class.\n\n"
                        + "📌 Track the total descriptive remarks entered vs. total capacity.\n\n"
                        + "📌 Find students with the most complete profiles.\n\n"
                        + "📌 Roster showing exactly how many remarks are completed for each student (e.g., 7 / 10 subjects completed).";
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
