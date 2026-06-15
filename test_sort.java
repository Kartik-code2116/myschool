import java.util.*;

class Subject {
    public String name;
    public String subjectCode;
    public Subject(String n, String c) { this.name = n; this.subjectCode = c; }
    public String toString() { return name + ":" + subjectCode; }

    public static void sortSubjects(java.util.List<Subject> subjects) {
        if (subjects == null || subjects.size() <= 1) return;
        java.util.Collections.sort(subjects, new java.util.Comparator<Subject>() {
            @Override
            public int compare(Subject s1, Subject s2) {
                String c1 = s1.subjectCode != null ? s1.subjectCode.trim() : "";
                String c2 = s2.subjectCode != null ? s2.subjectCode.trim() : "";
                
                // If both are empty, keep original order
                if (c1.isEmpty() && c2.isEmpty()) return 0;
                // Empty goes last
                if (c1.isEmpty()) return 1;
                if (c2.isEmpty()) return -1;
                
                try {
                    long n1 = Long.parseLong(c1.replaceAll("[^0-9]", ""));
                    long n2 = Long.parseLong(c2.replaceAll("[^0-9]", ""));
                    return Long.compare(n1, n2);
                } catch (Exception e) {
                    return c1.compareTo(c2);
                }
            }
        });
    }
}

public class test_sort {
    public static void main(String[] args) {
        List<Subject> list = new ArrayList<>();
        list.add(new Subject("Marathi", "101104"));
        list.add(new Subject("English", "101102"));
        list.add(new Subject("Math", "101103"));
        Subject.sortSubjects(list);
        for(Subject s : list) System.out.println(s);
    }
}
