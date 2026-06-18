package com.kartik.myschool;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;

import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.zoom.ZoomHelper;

import java.util.Locale;

public class MySchoolApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Global Zoom Helper
        ZoomHelper.initialize(this);

        // Force FirebaseRepository to re-initialize so it always uses
        // the current google-services.json project (not a stale cached instance)
        FirebaseRepository.resetInstance();
        Log.d("APP_INIT", "FirebaseRepository reset — will use google-services.json project on next call");

        // 1. Restore active selections from session SharedPreferences
        SessionContext.load(this);

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
