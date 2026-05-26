package com.example.myschool.model;

import java.util.HashMap;
import java.util.Map;

public class AttendanceRecord {
    public String id;
    public String studentId;
    public String classId;
    public String academicYear; // e.g., "2025-26"
    
    // Maps month name (e.g., "जून", "जुलै") to attendance string "present/total" (e.g., "23/24")
    public Map<String, String> monthlyData = new HashMap<>();
    
    public int totalPresent;
    public int totalWorking;

    public AttendanceRecord() {
        // Initialize default months with placeholder data so it renders beautifully out-of-the-box
        String[] months = {"जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे", "डिसें", "जाने", "फेब्रु", "मार्च", "एप्रिल", "मे"};
        for (String m : months) {
            monthlyData.put(m, "0/0");
        }
    }

    public void recalculateTotals() {
        totalPresent = 0;
        totalWorking = 0;
        for (String val : monthlyData.values()) {
            if (val != null && val.contains("/")) {
                String[] parts = val.split("/");
                if (parts.length == 2) {
                    try {
                        totalPresent += Integer.parseInt(parts[0].trim());
                        totalWorking += Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }
}
