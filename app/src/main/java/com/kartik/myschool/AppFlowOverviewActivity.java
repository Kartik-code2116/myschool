package com.kartik.myschool;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.widget.Toolbar;
import com.kartik.myschool.utils.UiAnimations;

public class AppFlowOverviewActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_flow_overview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        Button btnGotIt = findViewById(R.id.btnGotIt);
        btnGotIt.setOnClickListener(v -> finish());

        View cardStep1 = findViewById(R.id.cardStep1);
        View cardStep2 = findViewById(R.id.cardStep2);
        View cardStep3 = findViewById(R.id.cardStep3);
        View cardStep4 = findViewById(R.id.cardStep4);
        View cardStep5 = findViewById(R.id.cardStep5);
        View cardStep6 = findViewById(R.id.cardStep6);
        View cardStep7 = findViewById(R.id.cardStep7);

        View ivStep1 = findViewById(R.id.ivStep1);
        View ivStep2 = findViewById(R.id.ivStep2);
        View ivStep3 = findViewById(R.id.ivStep3);
        View ivStep4 = findViewById(R.id.ivStep4);
        View ivStep5 = findViewById(R.id.ivStep5);
        View ivStep6 = findViewById(R.id.ivStep6);
        View ivStep7 = findViewById(R.id.ivStep7);

        // Staggered fade in animation
        cardStep1.post(() -> {
            UiAnimations.staggerFadeIn(
                cardStep1,
                cardStep2,
                cardStep3,
                cardStep4,
                cardStep5,
                cardStep6,
                cardStep7,
                btnGotIt
            );
            
            // Add subtle pop-in animation to the screenshots
            View[] images = {ivStep1, ivStep2, ivStep3, ivStep4, ivStep5, ivStep6, ivStep7};
            long delay = 100;
            for (View img : images) {
                if (img != null) {
                    img.setScaleX(0.85f);
                    img.setScaleY(0.85f);
                    img.setAlpha(0f);
                    img.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setStartDelay(delay)
                        .setDuration(500)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                        .start();
                    delay += 100;
                }
            }
        });
    }
}
