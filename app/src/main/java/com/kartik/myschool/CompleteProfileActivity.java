package com.kartik.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.kartik.myschool.databinding.ActivityCompleteProfileBinding;
import com.kartik.myschool.model.Teacher;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;
import com.google.firebase.auth.FirebaseAuth;

public class CompleteProfileActivity extends BaseActivity {

    private static final String TAG = "CompleteProfile";

    private ActivityCompleteProfileBinding b;
    private final FirebaseRepository repo = FirebaseRepository.get();

    private String googleEmail;
    private String googleName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Screen opened: " + this.getClass().getSimpleName());
        b = ActivityCompleteProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Get data passed from LoginActivity
        googleEmail = getIntent().getStringExtra("googleEmail");
        googleName = getIntent().getStringExtra("googleName");

        // Show which Google account they signed in with
        if (!TextUtils.isEmpty(googleEmail)) {
            b.tvWelcomeEmail.setText("Signed in as: " + googleEmail);
        }

        // Pre-fill name from Google if available
        if (!TextUtils.isEmpty(googleName)) {
            b.etFullName.setText(googleName);
        }

        // Staggered Entrance Animation
        b.logoContainer.setAlpha(0f);
        b.tvCompleteTitle.setAlpha(0f);
        b.tvCompleteSubtitle.setAlpha(0f);
        b.cardCompleteForm.setAlpha(0f);

        b.getRoot().post(() -> UiAnimations.staggerFadeIn(
                b.logoContainer,
                b.tvCompleteTitle,
                b.tvCompleteSubtitle,
                b.cardCompleteForm
        ));

        // Real-time error clearing
        setupRealTimeValidation();

        // Privacy policy link
        b.tvPrivacyPolicy.setOnClickListener(v -> {
            UiAnimations.pulse(b.tvPrivacyPolicy);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://kartik-28deb.web.app/privacy_policy.html"));
            startActivity(browserIntent);
        });

        // Complete profile button
        b.btnCompleteProfile.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnCompleteProfile);
            doCompleteProfile();
        });
    }

    private void setupRealTimeValidation() {
        b.etFullName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilFullName.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.etPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilPhone.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.etSchoolName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilSchoolName.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.etUdiseCode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilUdiseCode.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void doCompleteProfile() {
        String name = str(b.etFullName);
        String phone = str(b.etPhone);
        String schoolName = str(b.etSchoolName);
        String udiseCode = str(b.etUdiseCode);
        String email = !TextUtils.isEmpty(googleEmail) ? googleEmail : "";

        // Validation
        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            b.tilFullName.setError("Full name is required");
            valid = false;
        } else if (name.length() > 100) {
            b.tilFullName.setError("Name too long (max 100 characters)");
            valid = false;
        }

        if (TextUtils.isEmpty(phone)) {
            b.tilPhone.setError("Phone number is required");
            valid = false;
        } else if (phone.length() < 10) {
            b.tilPhone.setError("Please enter a valid phone number");
            valid = false;
        } else if (phone.length() > 20) {
            b.tilPhone.setError("Phone number too long (max 20 characters)");
            valid = false;
        }

        if (!TextUtils.isEmpty(schoolName) && schoolName.length() > 200) {
            b.tilSchoolName.setError("School name too long (max 200 characters)");
            valid = false;
        }

        if (!TextUtils.isEmpty(udiseCode) && udiseCode.length() != 11) {
            b.tilUdiseCode.setError("UDISE code must be exactly 11 digits");
            valid = false;
        }

        if (!valid) return;

        // Clear errors and show loading
        b.tilFullName.setError(null);
        b.tilPhone.setError(null);
        b.tilSchoolName.setError(null);
        b.tilUdiseCode.setError(null);
        showLoading(true);

        // Save teacher profile
        SessionContext.clear(CompleteProfileActivity.this);
        Teacher t = new Teacher();
        t.name = name;
        t.email = email;
        t.phone = phone;
        t.schoolName = schoolName;
        t.udiseCode = udiseCode;

        repo.saveTeacher(t, new FirebaseRepository.OnResult<Void>() {
            @Override
            public void onSuccess(Void v) {
                if (com.kartik.myschool.BuildConfig.DEBUG) {
                    Log.d(TAG, "saveTeacher: SUCCESS");
                }
                showLoading(false);
                // Go to SchoolRegisterActivity (same as normal signup onboarding)
                Intent intent = new Intent(CompleteProfileActivity.this, SchoolRegisterActivity.class);
                intent.putExtra("is_onboarding", true);
                startActivity(intent);
                finishAffinity();
            }

            @Override
            public void onError(Exception e) {
                if (com.kartik.myschool.BuildConfig.DEBUG) {
                    Log.e(TAG, "saveTeacher: FAILED", e);
                }
                showLoading(false);
                showError("Profile Save Failed", "Could not save your profile.\n" + e.getMessage());
            }
        });
    }

    private void showLoading(boolean show) {
        b.completeProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnCompleteProfile.setEnabled(!show);
    }

    private void showError(String title, String message) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private String str(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public void onBackPressed() {
        // Sign out if they back out without completing the profile
        FirebaseAuth.getInstance().signOut();
        super.onBackPressed();
    }
}
