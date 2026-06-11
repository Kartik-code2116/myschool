package com.kartik.myschool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore model for a teacher's custom remark option bank per subject.
 * Document ID: schoolId + "_" + sanitizedSubjectName  (or "general")
 * Collection: remarkBanks
 */
public class RemarkBank {
    public String id;            // Firestore document ID
    public String schoolId;
    public String subjectName;   // Subject this applies to; "general" for cross-subject defaults
    public List<String> options = new ArrayList<>();
    public long updatedAt;

    // Required no-arg constructor for Firestore deserialization
    public RemarkBank() {
    }

    public RemarkBank(String schoolId, String subjectName, List<String> options) {
        this.schoolId = schoolId;
        this.subjectName = subjectName;
        this.options = options != null ? options : new ArrayList<>();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Default remark options shown when no custom bank has been saved.
     */
    public static List<String> defaultOptionsFor(String subjectName, int semesterNumber) {
        List<String> defaults = new ArrayList<>();
        if (subjectName == null) subjectName = "";
        String s = subjectName.toLowerCase();

        boolean isSem1 = (semesterNumber == 1);

        if (s.contains("मराठी") || s.contains("marathi") || s.contains("hindi") || s.contains("हिंदी") || s.contains("भाषा")) {
            if (isSem1) {
                defaults.add("[उत्कृष्ट] वाचन, लेखन व संभाषण कौशल्ये उत्कृष्ट असून भाषेचा प्रभावी वापर करतो/करते.");
                defaults.add("[उत्कृष्ट] पाठातील आशय समजून घेऊन आत्मविश्वासाने उत्तर देतो/देते.");
                defaults.add("[उत्कृष्ट] कविता, गोष्टी व भाषिक उपक्रमांमध्ये उत्साहाने सहभाग घेतो/घेते.");

                defaults.add("[चांगली प्रगती] वाचन व लेखन कौशल्ये चांगली असून नियमित प्रगती दिसून येते.");
                defaults.add("[चांगली प्रगती] नवीन शब्दांचा योग्य वापर करण्याचा प्रयत्न करतो/करते.");
                defaults.add("[चांगली পণ্ডিতी] वर्गातील चर्चेमध्ये सक्रिय सहभाग घेतो/घेते."); // Fixed spelling typo in user prompt "चांगली प्रगती"
                defaults.remove(defaults.size() - 1); // remove the typo
                defaults.add("[चांगली प्रगती] वर्गातील चर्चेमध्ये सक्रिय सहभाग घेतो/घेते.");

                defaults.add("[समाधानकारक] मूलभूत वाचन व लेखन कौशल्ये आत्मसात केली आहेत.");
                defaults.add("[समाधानकारक] शिक्षकांच्या मार्गदर्शनाखाली भाषिक उपक्रम पूर्ण करतो/करते.");
                defaults.add("[समाधानकारक] नियमित सरावाने अधिक प्रगती करू शकेल.");
            } else {
                defaults.add("[उत्कृष्ट] परिच्छेदाचे आकलन करून स्वतःचे विचार स्पष्टपणे मांडतो/मांडते.");
                defaults.add("[उत्कृष्ट] शुद्धलेखन व हस्ताक्षर उत्तम आहे.");
                defaults.add("[उत्कृष्ट] भाषिक कौशल्यांमध्ये उल्लेखनीय प्रगती दिसून येते.");

                defaults.add("[चांगली प्रगती] वाचनाचा वेग व आकलन क्षमता सुधारली आहे.");
                defaults.add("[चांगली प्रगती] लेखनात अधिक आत्मविश्वास दिसून येतो.");
                defaults.add("[चांगली प्रगती] भाषिक उपक्रमांमध्ये सातत्याने सहभाग घेतो/घेते.");

                defaults.add("[समाधानकारक] अपेक्षित अध्ययन निष्पत्ती साध्य करण्यासाठी प्रयत्नशील आहे.");
                defaults.add("[समाधानकारक] वाचन व लेखनात सुधारणा होत आहे.");
                defaults.add("[समाधानकारक] अधिक सरावाची आवश्यकता आहे.");
            }
        } else if (s.contains("english") || s.contains("इंग्रजी")) {
            if (isSem1) {
                defaults.add("[उत्कृष्ट] Reads simple sentences fluently and understands their meaning.");
                defaults.add("[उत्कृष्ट] Participates actively in speaking and listening activities.");
                defaults.add("[उत्कृष्ट] Learns new vocabulary quickly and uses it appropriately.");

                defaults.add("[चांगली प्रगती] Can read and write simple words and sentences.");
                defaults.add("[चांगली प्रगती] Shows interest in English learning activities.");
                defaults.add("[चांगली प्रगती] Communicates basic ideas confidently.");

                defaults.add("[समाधानकारक] Recognizes familiar words and simple sentences.");
                defaults.add("[समाधानकारक] Makes sincere efforts to improve language skills.");
                defaults.add("[समाधानकारक] Needs regular practice for better fluency.");
            } else {
                defaults.add("[उत्कृष्ट] Demonstrates excellent reading and speaking skills.");
                defaults.add("[उत्कृष्ट] Uses newly learned words effectively in communication.");
                defaults.add("[उत्कृष्ट] Completes language activities independently.");

                defaults.add("[चांगली प्रगती] Reading and writing skills have improved considerably.");
                defaults.add("[चांगली प्रगती] Understands classroom instructions well.");
                defaults.add("[चांगली प्रगती] Participates actively in language-based activities.");

                defaults.add("[समाधानकारक] Shows gradual improvement in English language skills.");
                defaults.add("[समाधानकारक] Requires guidance while reading and writing.");
                defaults.add("[समाधानकारक] Continuous practice will enhance performance.");
            }
        } else if (s.contains("गणित") || s.contains("math")) {
            if (isSem1) {
                defaults.add("[उत्कृष्ट] अंकज्ञान व गणितीय संकल्पना उत्कृष्टरीत्या समजून घेतो/घेते.");
                defaults.add("[उत्कृष्ट] बेरीज, वजाबाकी अचूकपणे सोडवतो/सोडवते.");
                defaults.add("[उत्कृष्ट] गणितीय उपक्रमांमध्ये उत्साहाने सहभागी होतो/होते.");

                defaults.add("[चांगली प्रगती] मूलभूत गणिती क्रिया चांगल्या प्रकारे समजतात.");
                defaults.add("[चांगली प्रगती] उदाहरणे सोडविण्यात आत्मविश्वास दाखवतो/दाखवते.");
                defaults.add("[चांगली प्रगती] नियमित सरावामुळे चांगली प्रगती होत आहे.");

                defaults.add("[समाधानकारक] गणितातील मूलभूत संकल्पना समजत आहेत.");
                defaults.add("[समाधानकारक] मार्गदर्शनाखाली उदाहरणे सोडवतो/सोडवते.");
                defaults.add("[समाधानकारक] अधिक सरावाची गरज आहे.");
            } else {
                defaults.add("[उत्कृष्ट] समस्या सोडविण्याची क्षमता चांगली विकसित झाली आहे.");
                defaults.add("[उत्कृष्ट] गणितीय विचारशक्ती व तर्कशक्ती उत्तम आहे.");
                defaults.add("[उत्कृष्ट] सर्व गणितीय उपक्रमांमध्ये उत्कृष्ट कामगिरी करतो/करते.");

                defaults.add("[चांगली प्रगती] गणितातील अचूकता व वेग वाढला आहे.");
                defaults.add("[चांगली प्रगती] विविध संकल्पना सहजपणे आत्मसात करतो/करते.");
                defaults.add("[चांगली प्रगती] नियमित सहभागामुळे प्रगती दिसून येते.");

                defaults.add("[समाधानकारक] गणितीय कौशल्यांमध्ये सुधारणा होत आहे.");
                defaults.add("[समाधानकारक] काही संकल्पनांसाठी अतिरिक्त सराव आवश्यक आहे.");
                defaults.add("[समाधानकारक] प्रयत्नशील वृत्ती कौतुकास्पद आहे.");
            }
        } else if (s.contains("विज्ञान") || s.contains("science") || s.contains("परिसर") || s.contains("खेळ")) {
            if (isSem1) {
                defaults.add("[उत्कृष्ट] परिसरातील घटकांचे बारकाईने निरीक्षण करतो/करते.");
                defaults.add("[उत्कृष्ट] उपक्रमाधारित शिक्षणात उत्साहाने सहभागी होतो/होते.");
                defaults.add("[उत्कृष्ट] स्वच्छता व चांगल्या सवयींचे पालन करतो/करते.");

                defaults.add("[चांगली प्रगती] परिसराविषयी चांगली जिज्ञासा दाखवतो/दाखवते.");
                defaults.add("[चांगली प्रगती] गटकार्यामध्ये सहकार्याची भावना ठेवतो/ठेवते.");
                defaults.add("[चांगली प्रगती] विविध उपक्रमांमध्ये आनंदाने सहभाग घेतो/घेते.");

                defaults.add("[समाधानकारक] शिक्षकांच्या मार्गदर्शनाखाली उपक्रम पूर्ण करतो/करते.");
                defaults.add("[समाधानकारक] परिसरातील मूलभूत संकल्पना समजून घेत आहे.");
                defaults.add("[समाधानकारक] नियमित सहभाग आवश्यक आहे.");
            } else {
                defaults.add("[उत्कृष्ट] पर्यावरण व सामाजिक मूल्यांविषयी चांगली जागरूकता दाखवतो/दाखवते.");
                defaults.add("[उत्कृष्ट] अनुभवातून शिकण्याची क्षमता उत्तम आहे.");
                defaults.add("[उत्कृष्ट] जबाबदारीची भावना विकसित झाली आहे.");

                defaults.add("[चांगली प्रगती] परिसर अभ्यासातील संकल्पना चांगल्या प्रकारे समजतात.");
                defaults.add("[चांगली प्रगती] निरीक्षण व सहभाग कौशल्यांमध्ये प्रगती दिसते.");
                defaults.add("[चांगली प्रगती] नवीन गोष्टी शिकण्याची उत्सुकता कायम आहे.");

                defaults.add("[समाधानकारक] उपक्रमांमध्ये सहभाग घेत असून प्रगती करत आहे.");
                defaults.add("[समाधानकारक] संकल्पना समजण्यासाठी अधिक अनुभवाधारित सराव आवश्यक आहे.");
                defaults.add("[समाधानकारक] सातत्यपूर्ण प्रयत्न करीत आहे.");
            }
        } else {
            // Generic defaults if subject doesn't match
            if (isSem1) {
                defaults.add("[उत्कृष्ट] अभ्यासात चांगली गती व उत्साह दिसतो.");
                defaults.add("[चांगली प्रगती] वर्गकार्यात सक्रिय सहभाग नोंदवतो/नोंदवते.");
                defaults.add("[समाधानकारक] नियमित शाळेत उपस्थित राहून प्रगती करत आहे.");
            } else {
                defaults.add("[उत्कृष्ट] संपूर्ण वर्षातील कामगिरी उत्कृष्ट आहे.");
                defaults.add("[चांगली प्रगती] द्वितीय सत्रातील प्रगती चांगली व समाधानकारक आहे.");
                defaults.add("[समाधानकारक] वार्षिक अभ्यासात अधिक मेहनत हवी.");
            }
        }
        return defaults;
    }
}
