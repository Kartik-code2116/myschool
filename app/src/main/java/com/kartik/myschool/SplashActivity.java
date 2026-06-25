package com.kartik.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * SplashActivity — shows the splash screen briefly while checking auth state.
 *
 * Previously had a hardcoded 1800ms delay (audit issue #4).
 * Now navigates immediately after auth check (≤150ms render delay).
 * This saves ~1.65 seconds on every cold start.
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Screen opened: " + this.getClass().getSimpleName());
        
        // Fix 1: No layout inflation, no handler delay. Navigate instantly.
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
