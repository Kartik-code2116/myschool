package com.kartik.myschool.model;

public class Subject {
    public String name;
    public String subjectCode;
    public int maxMarks;

    // Optional customized sub-field max marks (Formative / आकारिक)
    public int maxNirikhshan;
    public int maxTondiKam;
    public int maxPratyakshik;
    public int maxUpkram;
    public int maxPrakalp;
    public int maxChachani;
    public int maxSwadhyay;
    public int maxItar;

    // Optional customized sub-field max marks (Summative / संकलित)
    public int maxTondi;
    public int maxPratyakshikB;
    public int maxLekhi;

    public Subject() {}

    public Subject(String name, int maxMarks) {
        this.name = name;
        this.maxMarks = maxMarks;
        
        // Auto-scale defaults
        int akarikMax  = maxMarks / 2;
        int sanklitMax = maxMarks - akarikMax;

        if (isNonAcademic(name)) {
            akarikMax = maxMarks;
            sanklitMax = 0;
        }

        this.maxNirikhshan   = 0;
        this.maxTondiKam     = akarikMax * 10 / 50;
        this.maxPratyakshik  = akarikMax * 10 / 50;
        this.maxUpkram       = akarikMax * 10 / 50;
        this.maxPrakalp      = 0;
        this.maxChachani     = akarikMax * 20 / 50;
        this.maxSwadhyay     = 0;
        this.maxItar         = 0;

        this.maxTondi        = sanklitMax * 10 / 50;
        this.maxPratyakshikB = sanklitMax * 10 / 50;
        this.maxLekhi        = sanklitMax - this.maxTondi - this.maxPratyakshikB;
    }

    public static boolean isNonAcademic(String subName) {
        if (subName == null) return false;
        String s = subName.toLowerCase();
        return s.contains("art") || s.contains("drawing") || s.contains("कला") 
            || s.contains("work experience") || s.contains("work exp") || s.contains("कार्यानुभव")
            || s.contains("physical education") || s.contains("p.e.") || s.contains("शारीरिक") || s.contains("craft");
    }

    public static java.util.List<Subject> getDefaultSubjectsForClass(String className) {
        java.util.List<Subject> list = new java.util.ArrayList<>();
        if (className == null) return list;
        
        int std = -1;
        try {
            String clean = className.replaceAll("[^0-9]", "");
            if (!clean.isEmpty()) {
                std = Integer.parseInt(clean);
            }
        } catch (Exception ignored) {}

        if (std < 1 || std > 10) {
            // Default fallback if standard is not 1-10
            list.add(new Subject("Marathi", 100));
            list.add(new Subject("English", 100));
            list.add(new Subject("Mathematics", 100));
            list.add(new Subject("Science", 100));
            return list;
        }

        // 1. Marathi (First Language) - All standards (1-10)
        list.add(new Subject("Marathi", 100));

        // 2. Hindi (Second Language) - 5th to 10th
        if (std >= 5) {
            list.add(new Subject("Hindi", 100));
        }

        // 3. English (Third Language) - All standards (1-10)
        list.add(new Subject("English", 100));

        // 4. Sanskrit - 5th to 10th
        if (std >= 5) {
            list.add(new Subject("Sanskrit", 100));
        }

        // 5. Mathematics (Semi-English) - All standards (1-10)
        list.add(new Subject("Mathematics", 100));

        // 6. Science - All standards (1-10)
        list.add(new Subject("Science", 100));

        // 7. History - 5th to 10th
        if (std >= 5) {
            list.add(new Subject("History", 100));
        }

        // 8. Social Science - 5th to 10th
        if (std >= 5) {
            list.add(new Subject("Social Science", 100));
        }

        // 9. Drawing / Art Education - All standards (1-10)
        list.add(new Subject("Drawing", 100));

        // 10. Work Experience - All standards (1-10)
        list.add(new Subject("Work Experience", 100));

        // 11. Physical Education / Health - All standards (1-10)
        list.add(new Subject("Physical Education", 100));

        // 12. Personality Development - All standards (1-10)
        list.add(new Subject("Personality Development", 100));

        // 13. Special Development (Scout/Guide, etc.) - 5th to 10th
        if (std >= 5) {
            list.add(new Subject("Special Development", 100));
        }

        // 14. Information & Comm. Technology (ICT) - 9th & 10th
        if (std >= 9) {
            list.add(new Subject("Information & Comm. Technology (ICT)", 100));
        }

        // 15. Water Security & Environment Studies - 9th & 10th
        if (std >= 9) {
            list.add(new Subject("Water Security & Environment Studies", 100));
        }

        return list;
    }

    public static void sortSubjects(java.util.List<Subject> subjects) {
        if (subjects == null || subjects.isEmpty()) return;
        
        java.util.List<String> order = java.util.Arrays.asList(
            "Marathi",
            "Hindi",
            "English",
            "Sanskrit",
            "Mathematics",
            "Science",
            "History",
            "Social Science",
            "Drawing",
            "Work Experience",
            "Physical Education",
            "Special Development",
            "Personality Development",
            "Information & Comm. Technology (ICT)",
            "Water Security & Environment Studies"
        );
        
        subjects.sort((s1, s2) -> {
            int i1 = order.indexOf(s1.name);
            int i2 = order.indexOf(s2.name);
            if (i1 == -1) i1 = 999;
            if (i2 == -1) i2 = 999;
            return Integer.compare(i1, i2);
        });
    }
}

