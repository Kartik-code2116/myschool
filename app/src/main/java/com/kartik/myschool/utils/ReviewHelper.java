package com.kartik.myschool.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.gms.tasks.Task;

public final class ReviewHelper {

    private static final String PREFS_NAME = "myschool_review_prefs";
    private static final String KEY_PDF_COUNT = "pdf_generation_count";
    private static final String KEY_REVIEW_SHOWN = "review_shown";

    private ReviewHelper() {}

    public static void incrementPdfCountAndCheck(Activity activity) {
        if (activity == null) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean reviewShown = prefs.getBoolean(KEY_REVIEW_SHOWN, false);
        if (reviewShown) return;

        int count = prefs.getInt(KEY_PDF_COUNT, 0) + 1;
        prefs.edit().putInt(KEY_PDF_COUNT, count).apply();

        if (count >= 3) {
            requestInAppReview(activity);
        }
    }

    private static void requestInAppReview(Activity activity) {
        ReviewManager manager = ReviewManagerFactory.create(activity);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(flowTask -> {
                    // Mark as shown so we don't prompt repeatedly
                    SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putBoolean(KEY_REVIEW_SHOWN, true).apply();
                });
            }
        });
    }
}
