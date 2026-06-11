public class TestRegex {
    public static void main(String[] args) {
        String[] roots = {
            "कर", "दे", "घे", "शिक", "वाच", "लिहि", "बोल", "सांग", "सोडव", 
            "दाखव", "ओळख", "वापर", "जप", "जोपास", "वाढव", "मांड", "ऐक", 
            "निवड", "रेखाट", "रंगव", "खेळ", "धाव", "हो", "जिंक", "राह", 
            "ठेव", "पाड", "विचार"
        };
        
        String rootPattern = String.join("|", roots);
        String toFemaleRegex = "(?U)\\b(" + rootPattern + ")तो\\b";
        String toMaleRegex = "(?U)\\b(" + rootPattern + ")ते\\b";
        
        String[] tests = {
            "वाचन, लेखन व संभाषण कौशल्ये उत्कृष्ट असून भाषेचा प्रभावी वापर करतो.",
            "पाठाचा आशय समजून अचूक उत्तरे देतो.",
            "खेळतो, धावतो आणि जिंकतो!",
            "ती मुलगी छान खेळते.",
            "प्रश्न विचारते व उत्तरे देते."
        };
        
        System.out.println("--- TO FEMALE ---");
        for(String text : tests) {
            String updated = text.replaceAll(toFemaleRegex, "$1ते");
            System.out.println(updated);
        }
        
        System.out.println("\n--- TO MALE ---");
        for(String text : tests) {
            String updated = text.replaceAll(toMaleRegex, "$1तो");
            System.out.println(updated);
        }
    }
}
