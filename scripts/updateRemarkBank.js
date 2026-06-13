const fs = require('fs');

const class2Marathi = `
        if (isSem1) {
            defaults.add("[उत्कृष्ट] वाचन, लेखन व संभाषण कौशल्ये उत्कृष्ट असून भाषेचा प्रभावी वापर करतो/करते.");
            defaults.add("[उत्कृष्ट] पाठातील आशय समजून घेऊन आत्मविश्वासाने उत्तर देतो/देते.");
            defaults.add("[उत्कृष्ट] कविता, गोष्टी व भाषिक उपक्रमांमध्ये उत्साहाने सहभाग घेतो/घेते.");
            defaults.add("[चांगली प्रगती] वाचन व लेखन कौशल्ये चांगली असून नियमित प्रगती दिसून येते.");
            defaults.add("[चांगली प्रगती] नवीन शब्दांचा योग्य वापर करण्याचा प्रयत्न करतो/करते.");
            defaults.add("[चांगली प्रगती] वर्गातील चर्चेमध्ये सक्रिय सहभाग घेतो/घेते.");
            defaults.add("[समाधानकारक] मूलभूत भाषिक कौशल्ये आत्मसात केली आहेत.");
            defaults.add("[समाधानकारक] शिक्षकांच्या मार्गदर्शनाखाली लेखन व वाचन करतो/करते.");
            defaults.add("[समाधानकारक] अधिक सरावाने प्रगती करू शकेल.");
        } else {
            defaults.add("[उत्कृष्ट] संपूर्ण वर्षात भाषिक कौशल्यांमध्ये उत्कृष्ट प्रगती.");
            defaults.add("[उत्कृष्ट] स्वतःचे विचार स्पष्टपणे व आत्मविश्वासाने मांडतो/मांडते.");
            defaults.add("[उत्कृष्ट] वाचनाची आवड निर्माण झाली आहे.");
            defaults.add("[चांगली प्रगती] द्वितीय सत्रातील प्रगती चांगली व समाधानकारक आहे.");
            defaults.add("[चांगली प्रगती] व्याकरणाची समज वाढत आहे.");
            defaults.add("[चांगली प्रगती] निबंध व कथा लेखनात रस घेतो/घेते.");
            defaults.add("[समाधानकारक] वार्षिक प्रगती समाधानकारक आहे.");
            defaults.add("[समाधानकारक] वाचन व लेखनात अधिक लक्ष देणे आवश्यक.");
            defaults.add("[समाधानकारक] नियमित सरावाची गरज आहे.");
        }
`;

const class3Marathi = `
        defaults.add("[उत्कृष्ट] वाचन, लेखन व संभाषण कौशल्ये उत्कृष्ट आहेत.");
        defaults.add("[उत्कृष्ट] पाठाचा आशय समजून अचूक उत्तरे देतो/देते.");
        defaults.add("[उत्कृष्ट] भाषिक उपक्रमांमध्ये उत्साहाने सहभाग घेतो/घेते.");
        defaults.add("[चांगली प्रगती] वाचन व लेखन कौशल्यांमध्ये चांगली प्रगती दिसून येते.");
        defaults.add("[चांगली प्रगती] नवीन शब्दांचा योग्य वापर करण्याचा प्रयत्न करतो/करते.");
        defaults.add("[चांगली प्रगती] नियमित सरावामुळे भाषेवरील प्रभुत्व वाढत आहे.");
        defaults.add("[समाधानकारक] मूलभूत भाषिक कौशल्ये आत्मसात केली आहेत.");
        defaults.add("[समाधानकारक] शिक्षकांच्या मार्गदर्शनाखाली कार्य पूर्ण करतो/करते.");
        defaults.add("[समाधानकारक] अधिक सरावाने आणखी प्रगती करू शकेल.");
`;

const class2English = `
        if (isSem1) {
            defaults.add("[उत्कृष्ट] इंग्रजी अक्षरे व शब्द अचूक ओळखतो/ओळखते.");
            defaults.add("[उत्कृष्ट] सोपी इंग्रजी वाक्ये आत्मविश्वासाने वाचतो व लिहितो.");
            defaults.add("[उत्कृष्ट] इंग्रजी कविता तालासुरात म्हणतो/म्हणते.");
            defaults.add("[चांगली प्रगती] इंग्रजी वाचन व लेखनात चांगली प्रगती करत आहे.");
            defaults.add("[चांगली प्रगती] नवीन इंग्रजी शब्द शिकण्याचा प्रयत्न करतो/करते.");
            defaults.add("[चांगली प्रगती] इंग्रजी उपक्रमांमध्ये सहभाग घेतो/घेते.");
            defaults.add("[समाधानकारक] मूलभूत इंग्रजी शब्द समजतात.");
            defaults.add("[समाधानकारक] शिक्षकांच्या मदतीने इंग्रजी वाक्ये वाचतो/वाचते.");
            defaults.add("[समाधानकारक] इंग्रजी संभाषणाचा अधिक सराव आवश्यक.");
        } else {
            defaults.add("[उत्कृष्ट] इंग्रजी संभाषणात चांगली सुधारणा झाली आहे.");
            defaults.add("[उत्कृष्ट] सोपे इंग्रजी परिच्छेद अचूक वाचतो/वाचते.");
            defaults.add("[उत्कृष्ट] इंग्रजी विषयाची आवड निर्माण झाली आहे.");
            defaults.add("[चांगली प्रगती] द्वितीय सत्रात इंग्रजी विषयात प्रगती चांगली आहे.");
            defaults.add("[चांगली प्रगती] शब्दसंग्रह वाढला आहे.");
            defaults.add("[चांगली प्रगती] इंग्रजीतून उत्तरे देण्याचा प्रयत्न करतो/करते.");
            defaults.add("[समाधानकारक] इंग्रजी विषयातील प्रगती समाधानकारक आहे.");
            defaults.add("[समाधानकारक] इंग्रजी वाचन व लेखनात अधिक मेहनत हवी.");
            defaults.add("[समाधानकारक] नियमित सरावाची आवश्यकता आहे.");
        }
`;

const class3English = `
        defaults.add("[उत्कृष्ट] Reads, writes and speaks confidently.");
        defaults.add("[उत्कृष्ट] Understands simple passages and responds accurately.");
        defaults.add("[उत्कृष्ट] Participates actively in language activities.");
        defaults.add("[चांगली प्रगती] Reading and writing skills are developing well.");
        defaults.add("[चांगली प्रगती] Uses simple vocabulary appropriately.");
        defaults.add("[चांगली प्रगती] Shows interest in learning English.");
        defaults.add("[समाधानकारक] Can read and understand simple words and sentences.");
        defaults.add("[समाधानकारक] Makes sincere efforts to improve language skills.");
        defaults.add("[समाधानकारक] Needs regular practice for better fluency.");
`;

const class2Math = `
        if (isSem1) {
            defaults.add("[उत्कृष्ट] बेरीज व वजाबाकी अचूक व जलद करतो/करते.");
            defaults.add("[उत्कृष्ट] संख्याज्ञान उत्तम आहे व पाढे पाठ आहेत.");
            defaults.add("[उत्कृष्ट] गणितीय संकल्पना चटकन समजून घेतो/घेते.");
            defaults.add("[चांगली प्रगती] गणितातील मूलभूत क्रिया चांगल्या प्रकारे समजल्या आहेत.");
            defaults.add("[चांगली प्रगती] उदाहरणे सोडवताना अचूकता वाढली आहे.");
            defaults.add("[चांगली प्रगती] गणिताविषयी गोडी निर्माण होत आहे.");
            defaults.add("[समाधानकारक] संख्या ओळखतो/ओळखते.");
            defaults.add("[समाधानकारक] शिक्षकांच्या मदतीने उदाहरणे सोडवतो/सोडवते.");
            defaults.add("[समाधानकारक] बेरीज व वजाबाकीच्या अधिक सरावाची गरज आहे.");
        } else {
            defaults.add("[उत्कृष्ट] गुणाकार व भागाकार संकल्पना स्पष्ट आहेत.");
            defaults.add("[उत्कृष्ट] शाब्दिक उदाहरणे सहज व अचूक सोडवतो/सोडवते.");
            defaults.add("[उत्कृष्ट] गणितातील सर्व क्षमता उत्कृष्टरीत्या प्राप्त केल्या आहेत.");
            defaults.add("[चांगली प्रगती] द्वितीय सत्रात गणितात चांगली समज आली आहे.");
            defaults.add("[चांगली प्रगती] पाढे पाठांतरावर भर देत आहे.");
            defaults.add("[चांगली प्रगती] तर्कशुद्ध विचार करण्याची क्षमता विकसित होत आहे.");
            defaults.add("[समाधानकारक] गणितीय प्रगती समाधानकारक आहे.");
            defaults.add("[समाधानकारक] शाब्दिक उदाहरणे सोडविताना मार्गदर्शनाची आवश्यकता असते.");
            defaults.add("[समाधानकारक] नियमित गणितीय सरावाची गरज आहे.");
        }
`;

const class3Math = `
        defaults.add("[उत्कृष्ट] गणितीय संकल्पना अचूकपणे समजून घेतो/घेते.");
        defaults.add("[उत्कृष्ट] उदाहरणे आत्मविश्वासाने व अचूकपणे सोडवतो/सोडवते.");
        defaults.add("[उत्कृष्ट] तर्कशक्ती व समस्या सोडविण्याची क्षमता उत्कृष्ट आहे.");
        defaults.add("[चांगली प्रगती] गणितातील मूलभूत संकल्पना चांगल्या प्रकारे समजतात.");
        defaults.add("[चांगली प्रगती] नियमित सरावामुळे अचूकता वाढत आहे.");
        defaults.add("[चांगली प्रगती] गणितीय उपक्रमांमध्ये उत्साहाने सहभाग घेतो/घेते.");
        defaults.add("[समाधानकारक] मूलभूत गणितीय कौशल्ये विकसित होत आहेत.");
        defaults.add("[समाधानकारक] मार्गदर्शनाखाली उदाहरणे सोडवतो/सोडवते.");
        defaults.add("[समाधानकारक] अधिक सरावाची आवश्यकता आहे.");
`;

const class2ScienceEVS = `
        if (isSem1) {
            defaults.add("[उत्कृष्ट] परिसरातील घटकांचे उत्तम निरीक्षण करतो/करते.");
            defaults.add("[उत्कृष्ट] परिसर अभ्यासाच्या उपक्रमांमध्ये हिरिरीने सहभाग घेतो/घेते.");
            defaults.add("[उत्कृष्ट] प्रश्न विचारण्याची व माहिती मिळवण्याची वृत्ती चांगली आहे.");
            defaults.add("[चांगली प्रगती] परिसराची माहिती चांगल्या प्रकारे सांगतो/सांगते.");
            defaults.add("[चांगली प्रगती] प्रात्यक्षिके व प्रयोगांमध्ये रस घेतो/घेते.");
            defaults.add("[चांगली प्रगती] निसर्गाविषयी कुतूहल दिसून येते.");
            defaults.add("[समाधानकारक] परिसराची मूलभूत माहिती आहे.");
            defaults.add("[समाधानकारक] शिक्षकांच्या मदतीने उपक्रम पूर्ण करतो/करते.");
            defaults.add("[समाधानकारक] निरीक्षण कौशल्य वाढवण्याची गरज आहे.");
        } else {
            defaults.add("[उत्कृष्ट] विज्ञानावर आधारित संकल्पना सहज समजून घेतो/घेते.");
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
`;

const class3ScienceEVS = `
        defaults.add("[उत्कृष्ट] परिसरातील घटकांचे बारकाईने निरीक्षण करतो/करते.");
        defaults.add("[उत्कृष्ट] पर्यावरण व समाजाविषयी चांगली जागरूकता दाखवतो/दाखवते.");
        defaults.add("[उत्कृष्ट] उपक्रमांमध्ये सक्रिय सहभाग घेतो/घेते.");
        defaults.add("[चांगली प्रगती] परिसर अभ्यासातील संकल्पना चांगल्या प्रकारे समजून घेतो/घेते.");
        defaults.add("[चांगली प्रगती] नवीन गोष्टी शिकण्याची उत्सुकता दिसून येते.");
        defaults.add("[चांगली प्रगती] गटकार्यामध्ये चांगले सहकार्य करतो/करते.");
        defaults.add("[समाधानकारक] मूलभूत संकल्पना समजून घेण्याचा प्रयत्न करतो/करते.");
        defaults.add("[समाधानकारक] शिक्षकांच्या मदतीने उपक्रम पूर्ण करतो/करते.");
        defaults.add("[समाधानकारक] अधिक सहभागाने चांगली प्रगती होईल.");
`;

const class2Kala = `
        if (isSem1) {
            defaults.add("[उत्कृष्ट] कलाकुसरीत चांगली प्रगती.");
            defaults.add("[उत्कृष्ट] चित्रकलेची आवड आहे.");
        } else {
            defaults.add("[उत्कृष्ट] नवनिर्मिती छान केली.");
            defaults.add("[उत्कृष्ट] उपक्रमात उत्स्फूर्त सहभाग.");
        }
`;

const class3Kala = `
        defaults.add("[उत्कृष्ट] सर्जनशीलता व कल्पकता उत्कृष्टरीत्या व्यक्त करतो/करते.");
        defaults.add("[उत्कृष्ट] चित्रकला व हस्तकला उपक्रमांमध्ये विशेष आवड दाखवतो/दाखवते.");
        defaults.add("[उत्कृष्ट] कलाकृती आकर्षक व नीटनेटकी असतात.");
        defaults.add("[चांगली प्रगती] विविध कलात्मक उपक्रमांमध्ये उत्साहाने सहभागी होतो/होते.");
        defaults.add("[चांगली प्रगती] सर्जनशील अभिव्यक्तीमध्ये सातत्याने प्रगती करत आहे.");
        defaults.add("[चांगली प्रगती] नवीन कल्पना मांडण्याचा प्रयत्न करतो/करते.");
        defaults.add("[समाधानकारक] कला उपक्रमांमध्ये सहभाग घेतो/घेते.");
        defaults.add("[समाधानकारक] मार्गदर्शनाखाली चांगले कार्य करतो/करते.");
        defaults.add("[समाधानकारक] अधिक सरावाने कलात्मक कौशल्ये विकसित होतील.");
`;

const class2Sharirik = `
        if (isSem1) {
            defaults.add("[उत्कृष्ट] प्रथम सत्रात खेळांमध्ये उत्साह दिसला.");
            defaults.add("[उत्कृष्ट] मैदानी खेळात चांगली चमक.");
        } else {
            defaults.add("[उत्कृष्ट] शारीरिक क्षमता वाढली.");
            defaults.add("[उत्कृष्ट] खेळाडूवृत्ती उत्तम आहे.");
        }
`;

const class3Sharirik = `
        defaults.add("[उत्कृष्ट] सर्व क्रीडा उपक्रमांमध्ये उत्साहाने सहभाग घेतो/घेते.");
        defaults.add("[उत्कृष्ट] शिस्त, संघभावना व क्रीडाभावना उत्तम आहे.");
        defaults.add("[उत्कृष्ट] शारीरिक तंदुरुस्ती चांगली आहे.");
        defaults.add("[चांगली प्रगती] खेळांमध्ये नियमित सहभाग घेतो/घेते.");
        defaults.add("[चांगली प्रगती] नियमांचे पालन करून खेळतो/खेळते.");
        defaults.add("[चांगली प्रगती] शारीरिक कौशल्यांमध्ये प्रगती दिसून येते.");
        defaults.add("[समाधानकारक] क्रीडा उपक्रमांमध्ये सहभाग घेतो/घेते.");
        defaults.add("[समाधानकारक] शिक्षकांच्या मार्गदर्शनाचे पालन करतो/करते.");
        defaults.add("[समाधानकारक] अधिक सरावाने कामगिरी सुधारू शकेल.");
`;

const class2General = `
        if (isSem1) {
            defaults.add("[उत्कृष्ट] अभ्यासात चांगली गती व उत्साह दिसतो.");
            defaults.add("[चांगली प्रगती] वर्गकार्यात सक्रिय सहभाग नोंदवतो/नोंदवते.");
            defaults.add("[समाधानकारक] नियमित शाळेत उपस्थित राहून प्रगती करत आहे.");
        } else {
            defaults.add("[उत्कृष्ट] संपूर्ण वर्षातील कामगिरी उत्कृष्ट आहे.");
            defaults.add("[चांगली प्रगती] द्वितीय सत्रातील प्रगती चांगली व समाधानकारक आहे.");
            defaults.add("[समाधानकारक] वार्षिक अभ्यासात अधिक मेहनत हवी.");
        }
`;

const class3General = `
        defaults.add("[उत्कृष्ट] सर्व विषयांमध्ये उत्कृष्ट प्रगती करून अपेक्षित अध्ययन निष्पत्ती साध्य केल्या आहेत.");
        defaults.add("[चांगली प्रगती] सर्व विषयांमध्ये चांगली प्रगती असून नियमित प्रयत्न करत आहे.");
        defaults.add("[समाधानकारक] अध्ययनात समाधानकारक प्रगती असून अधिक सरावाने आणखी चांगली कामगिरी करू शकेल.");
`;


const code = \`package com.kartik.myschool.model;

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
        int classNum = 2; // Default
        if (com.kartik.myschool.SessionContext.selectedClass != null) {
            try {
                classNum = Integer.parseInt(com.kartik.myschool.SessionContext.selectedClass.name);
            } catch (Exception e) {}
        }

        if (s.contains("मराठी") || s.contains("marathi") || s.contains("hindi") || s.contains("हिंदी") || s.contains("भाषा")) {
            if (classNum == 3) {
\${class3Marathi}
            } else {
\${class2Marathi}
            }
        } else if (s.contains("english") || s.contains("इंग्रजी")) {
            if (classNum == 3) {
\${class3English}
            } else {
\${class2English}
            }
        } else if (s.contains("math") || s.contains("गणित")) {
            if (classNum == 3) {
\${class3Math}
            } else {
\${class2Math}
            }
        } else if (s.contains("science") || s.contains("विज्ञान") || s.contains("evs") || s.contains("परिसर") || s.contains("history") || s.contains("इतिहास") || s.contains("geography") || s.contains("भूगोल")) {
            if (classNum == 3) {
\${class3ScienceEVS}
            } else {
\${class2ScienceEVS}
            }
        } else if (s.contains("art") || s.contains("कला") || s.contains("work") || s.contains("कार्यानुभव")) {
            if (classNum == 3) {
\${class3Kala}
            } else {
\${class2Kala}
            }
        } else if (s.contains("physical") || s.contains("शारीरिक") || s.contains("खेळ")) {
            if (classNum == 3) {
\${class3Sharirik}
            } else {
\${class2Sharirik}
            }
        } else {
            // Generic defaults
            if (classNum == 3) {
\${class3General}
            } else {
\${class2General}
            }
        }
        return defaults;
    }
}
\`;

fs.writeFileSync('app/src/main/java/com/kartik/myschool/model/RemarkBank.java', code);
console.log("Updated RemarkBank.java successfully.");
