package com.example.myschool.utils;

public class GradeCalculator {

    public static String getGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 35) return "D";
        return "F";
    }

    public static String getResult(double percentage) {
        return percentage >= 35 ? "PASS" : "FAIL";
    }

    public static double getPercentage(double obtained, int max) {
        if (max == 0) return 0;
        return (obtained / max) * 100.0;
    }

    // Check if any individual subject is below pass mark (35%)
    public static boolean isPassInAll(java.util.Map<String, Double> obtained,
                                      java.util.Map<String, Integer> max) {
        for (String sub : obtained.keySet()) {
            double o = obtained.get(sub);
            int m = max.containsKey(sub) ? max.get(sub) : 100;
            if (m > 0 && (o / m) * 100 < 35) return false;
        }
        return true;
    }
}
