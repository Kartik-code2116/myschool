package com.kartik.myschool;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import com.kartik.myschool.utils.LocaleHelper;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }
}
