package com.kartik.myschool.model;

public class Subject {
    public String name;
    public String shortName = "";
    public String subjectCode;
    public int maxMarks;
    // Optional customized sub-field max marks (Formative / आकारिक)
    public int maxNirikhshan;

    public String getDisplayName() {
        if (shortName != null && !shortName.trim().isEmpty()) {
            return shortName.trim();
        }
        return name;
    }
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
            || s.contains("physical education") || s.contains("p.e.") || s.contains("शारीरिक") || s.contains("craft") || s.contains("health")
            || s.contains("play") || s.contains("learn") || s.contains("खेळू");
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
            list.add(new Subject("मराठी", 100));
            list.add(new Subject("इंग्रजी", 100));
            list.add(new Subject("गणित", 100));
            list.add(new Subject("सामान्य विज्ञान", 100));
            return list;
        }

        // 1. First Language (Marathi) - All standards
        list.add(new Subject("मराठी", 100));

        // 2. Second Language (English) - All standards
        list.add(new Subject("इंग्रजी", 100));

        if (std >= 5) {
            // 3. Third Language (Hindi) - 5th to 10th
            list.add(new Subject("हिंदी", 100));
        }

        // 4. Mathematics - All standards
        list.add(new Subject("गणित", 100));

        if (std == 1 || std == 2) {
            // Lower Primary (1-2)
            list.add(new Subject("खेळू, करू, शिकू", 100));
        } else if (std == 3 || std == 4) {
            // Primary (3-4)
            list.add(new Subject("परिसर अभ्यास", 100));
            list.add(new Subject("खेळू, करू, शिकू", 100));
        } else if (std == 5) {
            // Upper Primary Transition (5)
            list.add(new Subject("परिसर अभ्यास भाग १", 100));
            list.add(new Subject("परिसर अभ्यास भाग २", 100));
            list.add(new Subject("आरोग्य व शारीरिक शिक्षण", 100));
            list.add(new Subject("कार्यानुभव", 100));
            list.add(new Subject("कला", 100));
        } else if (std >= 6 && std <= 8) {
            // Middle School (6-8)
            list.add(new Subject("सामान्य विज्ञान", 100));
            list.add(new Subject("इतिहास व नागरिकशास्त्र", 100));
            list.add(new Subject("भूगोल", 100));
            list.add(new Subject("आरोग्य व शारीरिक शिक्षण", 100));
            list.add(new Subject("कार्यानुभव", 100));
            list.add(new Subject("कला", 100));
        } else {
            // High School (9-10)
            list.add(new Subject("सामान्य विज्ञान", 100));
            list.add(new Subject("इतिहास व नागरिकशास्त्र", 100));
            list.add(new Subject("भूगोल", 100));
            list.add(new Subject("आरोग्य व शारीरिक शिक्षण", 100));
            list.add(new Subject("कार्यानुभव", 100));
            list.add(new Subject("कला", 100));
            list.add(new Subject("माहिती व संप्रेषण तंत्रज्ञान (ICT)", 100));
            list.add(new Subject("जलसुरक्षा व पर्यावरण अभ्यास", 100));
        }

        return list;
    }

    public static void sortSubjects(java.util.List<Subject> subjects) {
        if (subjects == null || subjects.size() <= 1) return;
        java.util.Collections.sort(subjects, new java.util.Comparator<Subject>() {
            @Override
            public int compare(Subject s1, Subject s2) {
                boolean s1Desc = isDescriptiveOnly(s1.name);
                boolean s2Desc = isDescriptiveOnly(s2.name);
                if (s1Desc != s2Desc) {
                    return s1Desc ? 1 : -1;
                }

                String c1 = s1.subjectCode != null ? s1.subjectCode.trim() : "";
                String c2 = s2.subjectCode != null ? s2.subjectCode.trim() : "";
                
                // If both are empty, keep original order
                if (c1.isEmpty() && c2.isEmpty()) return 0;
                // Empty goes last
                if (c1.isEmpty()) return 1;
                if (c2.isEmpty()) return -1;
                
                try {
                    // Try parsing as integer/long for correct numerical sorting (e.g. 2 before 10)
                    long n1 = Long.parseLong(c1.replaceAll("[^0-9]", ""));
                    long n2 = Long.parseLong(c2.replaceAll("[^0-9]", ""));
                    return Long.compare(n1, n2);
                } catch (Exception e) {
                    // Fallback to string comparison if not purely numeric
                    return c1.compareTo(c2);
                }
            }
        });
    }

    public static boolean isDescriptiveOnly(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("vishesh") || lower.contains("aavad") || lower.contains("sudharna") || lower.contains("vyaktimatva") ||
               lower.contains("विशेष") || lower.contains("आवड") || lower.contains("सुधारणा") || lower.contains("व्यक्तिमत्व");
    }

    private static boolean isPhysicalEd(String sc) {
        return sc.contains("physical education") || sc.contains("शारीरिक") || sc.contains("शा.शि.") 
            || sc.equals("p.e.") || sc.equals("p.e") || sc.equals("pe")
            || sc.contains("shirik") || sc.contains("health & physical education");
    }

    private static boolean isSocialSci(String sc) {
        return sc.contains("social science") || sc.equals("सामाजिक शास्त्र") || sc.contains("सामाजिक")
            || sc.contains("soc. science") || sc.contains("soc.science") || sc.contains("soc science")
            || sc.contains("history and civics") || sc.contains("history & civics")
            || sc.contains("इतिहास व नागरिकशास्त्र") || sc.contains("भूगोल व सामाजिक शास्त्र");
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, Boolean> sameSubjectCache = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isSameSubject(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        String cacheKey = s1 + "|||" + s2;
        Boolean cached = sameSubjectCache.get(cacheKey);
        if (cached != null) return cached;
        boolean result = isSameSubjectInternal(s1, s2);
        sameSubjectCache.put(cacheKey, result);
        return result;
    }

    private static boolean isSameSubjectInternal(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        if (s1.equalsIgnoreCase(s2)) return true;

        String sc1 = s1.trim().toLowerCase().replaceAll("\\s+", " ");
        String sc2 = s2.trim().toLowerCase().replaceAll("\\s+", " ");
        if (sc1.equals(sc2)) return true;

        // Check English vs Marathi equivalents
        if ((sc1.equals("marathi") || sc1.startsWith("मराठी")) && (sc2.equals("marathi") || sc2.startsWith("मराठी"))) return true;
        if ((sc1.equals("english") || sc1.startsWith("इंग्रजी")) && (sc2.equals("english") || sc2.startsWith("इंग्रजी"))) return true;
        if ((sc1.equals("hindi") || sc1.startsWith("हिंदी")) && (sc2.equals("hindi") || sc2.startsWith("हिंदी"))) return true;
        if ((sc1.equals("sanskrit") || sc1.startsWith("संस्कृत")) && (sc2.equals("sanskrit") || sc2.startsWith("संस्कृत"))) return true;
        if ((sc1.equals("mathematics") || sc1.equals("maths") || sc1.equals("math") || sc1.startsWith("गणित")) && (sc2.equals("mathematics") || sc2.equals("maths") || sc2.equals("math") || sc2.startsWith("गणित"))) return true;
        if ((sc1.equals("science") || sc1.startsWith("विज्ञान") || sc1.startsWith("सामान्य विज्ञान")) && (sc2.equals("science") || sc2.startsWith("विज्ञान") || sc2.startsWith("सामान्य विज्ञान"))) return true;
        if ((sc1.equals("history") || sc1.startsWith("इतिहास")) && (sc2.equals("history") || sc2.startsWith("इतिहास"))) return true;
        if ((sc1.equals("geography") || sc1.startsWith("भूगोल")) && (sc2.equals("geography") || sc2.startsWith("भूगोल"))) return true;
        if ((sc1.equals("civics") || sc1.startsWith("नागरिकशास्त्र")) && (sc2.equals("civics") || sc2.startsWith("नागरिकशास्त्र"))) return true;
        if (isSocialSci(sc1) && isSocialSci(sc2)) return true;
        if ((sc1.equals("art") || sc1.equals("drawing") || sc1.startsWith("कला")) && (sc2.equals("art") || sc2.equals("drawing") || sc2.startsWith("कला"))) return true;
        if ((sc1.equals("work experience") || sc1.startsWith("कार्यानुभव")) && (sc2.equals("work experience") || sc2.startsWith("कार्यानुभव"))) return true;
        if (isPhysicalEd(sc1) && isPhysicalEd(sc2)) return true;
        if ((sc1.contains("play") || sc1.startsWith("खेळू")) && (sc2.contains("play") || sc2.startsWith("खेळू"))) return true;
        if ((sc1.equals("personality development") || sc1.startsWith("व्यक्तिमत्त्व")) && (sc2.equals("personality development") || sc2.startsWith("व्यक्तिमत्त्व"))) return true;
        if ((sc1.contains("information & comm") || sc1.contains("ict") || sc1.startsWith("संप्रेषण")) && (sc2.contains("information & comm") || sc2.contains("ict") || sc2.startsWith("संप्रेषण"))) return true;
        if ((sc1.contains("water security") || sc1.startsWith("जलसुरक्षा")) && (sc2.contains("water security") || sc2.startsWith("जलसुरक्षा"))) return true;

        return false;
    }
}


