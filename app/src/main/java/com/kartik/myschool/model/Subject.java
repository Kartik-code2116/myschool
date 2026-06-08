package com.kartik.myschool.model;

public class Subject {
    public String name;
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

        this.maxNirikhshan   = akarikMax * 10 / 50;
        this.maxTondiKam     = akarikMax * 10 / 50;
        this.maxPratyakshik  = akarikMax * 10 / 50;
        this.maxUpkram       = akarikMax * 5  / 50;
        this.maxPrakalp      = akarikMax * 5  / 50;
        this.maxChachani     = akarikMax * 5  / 50;
        this.maxSwadhyay     = akarikMax * 5  / 50;
        this.maxItar         = akarikMax - this.maxNirikhshan - this.maxTondiKam - this.maxPratyakshik
                - this.maxUpkram - this.maxPrakalp - this.maxChachani - this.maxSwadhyay;

        this.maxTondi        = sanklitMax * 10 / 50;
        this.maxPratyakshikB = sanklitMax * 10 / 50;
        this.maxLekhi        = sanklitMax - this.maxTondi - this.maxPratyakshikB;
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
            list.add(new Subject("Science / EVS", 100));
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

        // 4. Mathematics (Semi-English) - All standards (1-10)
        list.add(new Subject("Mathematics", 100));

        // 5. Science (Semi-English) - 6th to 10th
        if (std >= 6) {
            list.add(new Subject("Science", 100));
        }

        // 6. Science / EVS (Environmental Studies) - 1st to 5th
        if (std <= 5) {
            list.add(new Subject("Science / EVS", 100));
        }

        // 7. Soc. Science (History/Geography) - 5th to 10th
        if (std >= 5) {
            list.add(new Subject("Soc. Science", 100));
        }

        // 8. Drawing / Art Education - All standards (1-10)
        list.add(new Subject("Drawing", 100));

        // 9. Work Experience - All standards (1-10)
        list.add(new Subject("Work Experience", 100));

        // 10. Physical Education / Health - All standards (1-10)
        list.add(new Subject("Physical Education", 100));

        // 11. Personality Development - All standards (1-10)
        list.add(new Subject("Personality Development", 100));

        // 12. Special Development (Scout/Guide, etc.) - 5th to 10th
        if (std >= 5) {
            list.add(new Subject("Special Development", 100));
        }

        // 13. Information & Comm. Technology (ICT) - 9th & 10th
        if (std >= 9) {
            list.add(new Subject("Information & Comm. Technology (ICT)", 100));
        }

        // 14. Water Security & Environment Studies - 9th & 10th
        if (std >= 9) {
            list.add(new Subject("Water Security & Environment Studies", 100));
        }

        return list;
    }
}

