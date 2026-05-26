package com.example.myschool;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class MySchoolApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Restore active selections from session SharedPreferences
        SessionContext.load(this);

        // 2. Load and apply persistent app theme
        android.content.SharedPreferences settingsPrefs = getSharedPreferences("myschool_settings_prefs", MODE_PRIVATE);
        int themeMode = settingsPrefs.getInt("theme_mode", 0); // 0 = System, 1 = Light, 2 = Dark
        applyTheme(themeMode);

        // 3. Load and apply persistent locale language
        String lang = settingsPrefs.getString("language", "en"); // default English
        applyLocale(lang);
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

    public void applyLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }
}
