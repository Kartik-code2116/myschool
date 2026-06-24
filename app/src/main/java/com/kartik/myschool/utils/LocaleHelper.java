package com.kartik.myschool.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

public class LocaleHelper {

    public static Context wrap(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("myschool_settings_prefs", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "mr"); // default to Marathi

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            Context newContext = context.createConfigurationContext(config);
            return new android.content.ContextWrapper(newContext) {
                @Override
                public Object getSystemService(String name) {
                    if (Context.PRINT_SERVICE.equals(name)) {
                        return context.getSystemService(name);
                    }
                    return super.getSystemService(name);
                }
            };
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }
}
