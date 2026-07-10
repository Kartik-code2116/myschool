package com.kartik.myschool.utils;

import com.kartik.myschool.AppCache;
import com.kartik.myschool.model.Student;

import java.util.Collections;
import java.util.List;

public class StudentSortUtils {

    public static void sortStudents(List<Student> list) {
        if (list == null) return;
        Collections.sort(list, (s1, s2) -> {
            if (AppCache.currentSortType == AppCache.SORT_BY_ROLL_ASC) {
                return compareRollNumbers(s1.rollNo, s2.rollNo);
            } else if (AppCache.currentSortType == AppCache.SORT_BY_ROLL_DESC) {
                return compareRollNumbers(s2.rollNo, s1.rollNo);
            } else if (AppCache.currentSortType == AppCache.SORT_BY_NAME_ASC) {
                String n1 = s1.name != null ? s1.name : "";
                String n2 = s2.name != null ? s2.name : "";
                return n1.compareToIgnoreCase(n2);
            } else if (AppCache.currentSortType == AppCache.SORT_BY_NAME_DESC) {
                String n1 = s1.name != null ? s1.name : "";
                String n2 = s2.name != null ? s2.name : "";
                return n2.compareToIgnoreCase(n1);
            }
            return 0;
        });
    }

    private static int compareRollNumbers(String r1, String r2) {
        if (r1 == null) r1 = "";
        if (r2 == null) r2 = "";
        r1 = r1.trim();
        r2 = r2.trim();
        if (r1.isEmpty() && r2.isEmpty()) return 0;
        if (r1.isEmpty()) return 1;
        if (r2.isEmpty()) return -1;

        boolean isNum1 = r1.matches("\\d+");
        boolean isNum2 = r2.matches("\\d+");
        if (isNum1 && isNum2) {
            try {
                return Integer.compare(Integer.parseInt(r1), Integer.parseInt(r2));
            } catch (NumberFormatException ignored) {}
        }
        return r1.compareToIgnoreCase(r2);
    }
}
