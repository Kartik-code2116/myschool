package com.kartik.myschool;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;

import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.zoom.ZoomHelper;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Locale;

public class MySchoolApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Global Zoom Helper
        ZoomHelper.initialize(this);

        // Initialize Analytics Helper
        com.kartik.myschool.utils.AnalyticsHelper.init(this);

        // Initialize Firebase App Check
        try {
            com.google.firebase.appcheck.FirebaseAppCheck appCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance();
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                        com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                );
                Log.d("APP_INIT", "App Check: Installed DebugAppCheckProviderFactory");
            } else {
                appCheck.installAppCheckProviderFactory(
                        com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
                );
                Log.d("APP_INIT", "App Check: Installed PlayIntegrityAppCheckProviderFactory");
            }
        } catch (Exception e) {
            Log.e("APP_INIT", "Failed to initialize Firebase App Check: " + e.getMessage());
        }

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(
            !BuildConfig.DEBUG
        );

        // Force FirebaseRepository to re-initialize so it always uses
        // the current google-services.json project (not a stale cached instance)
        FirebaseRepository.resetInstance();
        Log.d("APP_INIT", "FirebaseRepository reset — will use google-services.json project on next call");

        // 1. Restore active selections from session SharedPreferences off the main thread
        java.util.concurrent.ExecutorService bgThread = java.util.concurrent.Executors.newSingleThreadExecutor();
        bgThread.execute(() -> {
            SessionContext.load(this);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                SessionContext.sessionReady.setValue(true);
            });
        });

        // 2. Load and apply persistent app theme
        android.content.SharedPreferences settingsPrefs = getSharedPreferences("myschool_settings_prefs", MODE_PRIVATE);
        int themeMode = settingsPrefs.getInt("theme_mode", 1); // 0 = System, 1 = Light, 2 = Dark
        applyTheme(themeMode);

        // 3. Load and apply persistent locale language
        String lang = settingsPrefs.getString("language", "mr"); // default Marathi
        
        // Force locale immediately for the application context
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        
        androidx.core.os.LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (currentLocales.isEmpty() || !currentLocales.toLanguageTags().equals(lang)) {
            AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(lang));
        }

        // FCM token registration logic linked to Auth State
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(auth -> {
                com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    FirebaseCrashlytics.getInstance().setUserId(user.getUid());
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful() || task.getResult() == null) {
                                Log.w("FCM_INIT", "Fetching FCM registration token failed", task.getException());
                                return;
                            }
                            String token = task.getResult();
                            Log.d("FCM_INIT", "FCM token retrieved: " + token);
                            saveTokenToFirestore(user.getUid(), token);
                        });
                } else {
                    FirebaseCrashlytics.getInstance().setUserId("");
                }
            });
        } catch (Exception e) {
            Log.e("FCM_INIT", "Failed to initialize FCM token registration auth state listener: " + e.getMessage());
        }
    }

    private void saveTokenToFirestore(String uid, String token) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("fcmToken", token);
        updates.put("fcmTokenUpdatedAt", System.currentTimeMillis());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("teachers")
                .document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("FCM_INIT", "FCM token successfully saved to Firestore for uid=" + uid))
                .addOnFailureListener(e -> Log.e("FCM_INIT", "Failed to save FCM token to Firestore", e));
    }

    public static void applyTheme(int themeMode) {
        if (themeMode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themeMode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

}
