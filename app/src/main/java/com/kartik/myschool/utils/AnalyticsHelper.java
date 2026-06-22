package com.kartik.myschool.utils;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;

public final class AnalyticsHelper {

    private static FirebaseAnalytics firebaseAnalytics;

    private AnalyticsHelper() {}

    public static void init(Context context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.getApplicationContext());
        }
    }

    public static void logPdfGenerated(String reportType) {
        if (firebaseAnalytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("report_type", reportType);
        firebaseAnalytics.logEvent("pdf_generated", bundle);
    }

    public static void logMarksEntered(String classId, String studentId) {
        if (firebaseAnalytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("class_id", classId);
        bundle.putString("student_id", studentId);
        firebaseAnalytics.logEvent("marks_entered", bundle);
    }

    public static void logAttendanceMarked(String classId, String date) {
        if (firebaseAnalytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("class_id", classId);
        bundle.putString("date", date);
        firebaseAnalytics.logEvent("attendance_marked", bundle);
    }
}
