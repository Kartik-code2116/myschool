public class TestRegex {
    public static void main(String[] args) {
        String[] subjects = {"Mathematics", "English", "Health & Physical Education", "Play, Do, Learn"};
        for(String s : subjects) {
            String safe = s.replaceAll("[^a-zA-Z0-9\\u0900-\\u097F]", "_");
            System.out.println(s + " -> " + safe);
        }
    }
}
