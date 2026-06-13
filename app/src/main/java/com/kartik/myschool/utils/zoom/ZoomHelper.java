package com.kartik.myschool.utils.zoom;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.kartik.myschool.MainActivity;
import com.kartik.myschool.SplashActivity;

public class ZoomHelper {

    public static void initialize(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                // Not wrapping here because setContentView might not have been called yet.
            }

            @Override
            public void onActivityStarted(Activity activity) {
                wrapActivityIfNeeded(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                // Ensure wrapping is applied if it wasn't done in onActivityStarted
                wrapActivityIfNeeded(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }

    private static void wrapActivityIfNeeded(Activity activity) {
        // Exclude specific activities where zooming is not desired
        if (activity instanceof SplashActivity || activity instanceof MainActivity) {
            return;
        }

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        // If no child is loaded yet, wait.
        if (content.getChildCount() == 0) return;

        View firstChild = content.getChildAt(0);

        // Already wrapped
        if (firstChild instanceof ZoomLayout) {
            return;
        }

        // Remove the child, wrap it in ZoomLayout, and put it back
        content.removeView(firstChild);

        ZoomLayout zoomLayout = new ZoomLayout(activity);
        zoomLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Keep original layout parameters of the child
        ViewGroup.LayoutParams originalParams = firstChild.getLayoutParams();

        zoomLayout.addView(firstChild, originalParams != null ? originalParams : new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        content.addView(zoomLayout);
    }
}
