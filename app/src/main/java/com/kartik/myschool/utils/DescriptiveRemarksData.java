package com.kartik.myschool.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DescriptiveRemarksData {

    public static Map<String, List<String>> getRemarksForClassAndSubject(String className, String subjectName) {
        if (className == null || subjectName == null) return null;
        
        String cls = className.trim();
        String sub = subjectName.toLowerCase().trim();
        
        if (cls.equals("2") || cls.equals("२")) {
            if (sub.contains("मराठी") || sub.contains("marathi")) {
                return getClass2MarathiRemarks();
            } else if (sub.contains("इंग्रजी") || sub.contains("english")) {
                return getClass2EnglishRemarks();
            } else if (sub.contains("गणित") || sub.contains("math")) {
                return getClass2MathsRemarks();
            } else if (sub.contains("परिसर") || sub.contains("evs") || sub.contains("खेळू")) {
                return getClass2EVSRemarks();
            }
        }
        
        return null;
    }

    private static Map<String, List<String>> getClass2MarathiRemarks() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("उत्कृष्ट", Arrays.asList(
            "वाचन, लेखन व संभाषण कौशल्ये उत्कृष्ट असून भाषेचा प्रभावी वापर करतो/करते.",
            "पाठातील आशय समजून घेऊन आत्मविश्वासाने उत्तर देतो/देते.",
            "कविता, गोष्टी व भाषिक उपक्रमांमध्ये उत्साहाने सहभाग घेतो/घेते.",
            "परिच्छेदाचे आकलन करून स्वतःचे विचार स्पष्टपणे मांडतो/मांडते.",
            "शुद्धलेखन व हस्ताक्षर उत्तम आहे.",
            "भाषिक कौशल्यांमध्ये उल्लेखनीय प्रगती दिसून येते."
        ));
        map.put("चांगली प्रगती", Arrays.asList(
            "वाचन व लेखन कौशल्ये चांगली असून नियमित प्रगती दिसून येते.",
            "नवीन शब्दांचा योग्य वापर करण्याचा प्रयत्न करतो/करते.",
            "वर्गातील चर्चेमध्ये सक्रिय सहभाग घेतो/घेते.",
            "वाचनाचा वेग व आकलन क्षमता सुधारली आहे.",
            "लेखनात अधिक आत्मविश्वास दिसून येतो.",
            "भाषिक उपक्रमांमध्ये सातत्याने सहभाग घेतो/घेते."
        ));
        map.put("समाधानकारक", Arrays.asList(
            "मूलभूत वाचन व लेखन कौशल्ये आत्मसात केली आहेत.",
            "शिक्षकांच्या मार्गदर्शनाखाली भाषिक उपक्रम पूर्ण करतो/करते.",
            "नियमित सरावाने अधिक प्रगती करू शकेल.",
            "अपेक्षित अध्ययन निष्पत्ती साध्य करण्यासाठी प्रयत्नशील आहे.",
            "वाचन व लेखनात सुधारणा होत आहे.",
            "अधिक सरावाची आवश्यकता आहे."
        ));
        return map;
    }

    private static Map<String, List<String>> getClass2EnglishRemarks() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("Excellent", Arrays.asList(
            "Reads simple sentences fluently and understands their meaning.",
            "Participates actively in speaking and listening activities.",
            "Learns new vocabulary quickly and uses it appropriately.",
            "Demonstrates excellent reading and speaking skills.",
            "Uses newly learned words effectively in communication.",
            "Completes language activities independently."
        ));
        map.put("Good Progress", Arrays.asList(
            "Can read and write simple words and sentences.",
            "Shows interest in English learning activities.",
            "Communicates basic ideas confidently.",
            "Reading and writing skills have improved considerably.",
            "Understands classroom instructions well.",
            "Participates actively in language-based activities."
        ));
        map.put("Satisfactory", Arrays.asList(
            "Recognizes familiar words and simple sentences.",
            "Makes sincere efforts to improve language skills.",
            "Needs regular practice for better fluency.",
            "Shows gradual improvement in English language skills.",
            "Requires guidance while reading and writing.",
            "Continuous practice will enhance performance."
        ));
        return map;
    }

    private static Map<String, List<String>> getClass2MathsRemarks() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("उत्कृष्ट", Arrays.asList(
            "अंकज्ञान व गणितीय संकल्पना उत्कृष्टरीत्या समजून घेतो/घेते.",
            "बेरीज, वजाबाकी अचूकपणे सोडवतो/सोडवते.",
            "गणितीय उपक्रमांमध्ये उत्साहाने सहभागी होतो/होते.",
            "समस्या सोडविण्याची क्षमता चांगली विकसित झाली आहे.",
            "गणितीय विचारशक्ती व तर्कशक्ती उत्तम आहे.",
            "सर्व गणितीय उपक्रमांमध्ये उत्कृष्ट कामगिरी करतो/करते."
        ));
        map.put("चांगली प्रगती", Arrays.asList(
            "मूलभूत गणिती क्रिया चांगल्या प्रकारे समजतात.",
            "उदाहरणे सोडविण्यात आत्मविश्वास दाखवतो/दाखवते.",
            "नियमित सरावामुळे चांगली प्रगती होत आहे.",
            "गणितातील अचूकता व वेग वाढला आहे.",
            "विविध संकल्पना सहजपणे आत्मसात करतो/करते.",
            "नियमित सहभागामुळे प्रगती दिसून येते."
        ));
        map.put("समाधानकारक", Arrays.asList(
            "गणितातील मूलभूत संकल्पना समजत आहेत.",
            "मार्गदर्शनाखाली उदाहरणे सोडवतो/सोडवते.",
            "अधिक सरावाची गरज आहे.",
            "गणितीय कौशल्यांमध्ये सुधारणा होत आहे.",
            "काही संकल्पनांसाठी अतिरिक्त सराव आवश्यक आहे.",
            "प्रयत्नशील वृत्ती कौतुकास्पद आहे."
        ));
        return map;
    }

    private static Map<String, List<String>> getClass2EVSRemarks() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("उत्कृष्ट", Arrays.asList(
            "परिसरातील घटकांचे बारकाईने निरीक्षण करतो/करते.",
            "उपक्रमाधारित शिक्षणात उत्साहाने सहभागी होतो/होते.",
            "स्वच्छता व चांगल्या सवयींचे पालन करतो/करते.",
            "पर्यावरण व सामाजिक मूल्यांविषयी चांगली जागरूकता दाखवतो/दाखवते.",
            "अनुभवातून शिकण्याची क्षमता उत्तम आहे.",
            "जबाबदारीची भावना विकसित झाली आहे."
        ));
        map.put("चांगली प्रगती", Arrays.asList(
            "परिसराविषयी चांगली जिज्ञासा दाखवतो/दाखवते.",
            "गटकार्यामध्ये सहकार्याची भावना ठेवतो/ठेवते.",
            "विविध उपक्रमांमध्ये आनंदाने सहभाग घेतो/घेते.",
            "परिसर अभ्यासातील संकल्पना चांगल्या प्रकारे समजतात.",
            "निरीक्षण व सहभाग कौशल्यांमध्ये प्रगती दिसते.",
            "नवीन गोष्टी शिकण्याची उत्सुकता कायम आहे."
        ));
        map.put("समाधानकारक", Arrays.asList(
            "शिक्षकांच्या मार्गदर्शनाखाली उपक्रम पूर्ण करतो/करते.",
            "परिसरातील मूलभूत संकल्पना समजून घेत आहे.",
            "नियमित सहभाग आवश्यक आहे.",
            "उपक्रमांमध्ये सहभाग घेत असून प्रगती करत आहे.",
            "संकल्पना समजण्यासाठी अधिक अनुभवाधारित सराव आवश्यक आहे.",
            "सातत्यपूर्ण प्रयत्न करीत आहे."
        ));
        return map;
    }
}
