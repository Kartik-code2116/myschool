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
import androidx.appcompat.app.AppCompatActivity;

import com.kartik.myschool.databinding.ActivityRegisterBinding;
import com.kartik.myschool.model.Teacher;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding b;
    private FirebaseAuth auth;
    private final FirebaseRepository repo = FirebaseRepository.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Screen opened: " + this.getClass().getSimpleName());
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = FirebaseAuth.getInstance();

        // Diagnostic: confirm which Firebase project is connected
        if (com.kartik.myschool.BuildConfig.DEBUG) { Log.d("AUTH", "Firebase Auth initialized. App name: " +
                com.google.firebase.FirebaseApp.getInstance().getName() +
                " | Current user: " + (auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "none")); }

        // 1. Staggered Entrance Animation (premium feel)
        b.logoContainer.setAlpha(0f);
        b.tvRegisterTitle.setAlpha(0f);
        b.tvRegisterSubtitle.setAlpha(0f);
        b.tilFullName.setAlpha(0f);
        b.tilPhone.setAlpha(0f);
        b.tilSchoolName.setAlpha(0f);
        b.tilUdiseCode.setAlpha(0f);
        b.tilRegEmail.setAlpha(0f);
        b.tilRegPassword.setAlpha(0f);
        b.tilConfirmPassword.setAlpha(0f);
        b.btnRegister.setAlpha(0f);
        b.tvLoginLink.setAlpha(0f);

        b.getRoot().post(() -> UiAnimations.staggerFadeIn(
                b.logoContainer,
                b.tvRegisterTitle,
                b.tvRegisterSubtitle,
                b.tilFullName,
                b.tilPhone,
                b.tilSchoolName,
                b.tilUdiseCode,
                b.tilRegEmail,
                b.tilRegPassword,
                b.tilConfirmPassword,
                b.btnRegister,
                b.tvLoginLink
        ));

        // 2. Real-time dynamic input error clearing
        setupRealTimeValidation();

        b.tvPrivacyPolicy.setOnClickListener(v -> {
            UiAnimations.pulse(b.tvPrivacyPolicy);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://kartik-28deb.web.app/privacy_policy.html"));
            startActivity(browserIntent);
        });

        boolean isGoogleSignIn = getIntent().getBooleanExtra("isGoogleSignIn", false);
        if (isGoogleSignIn) {
            b.tilRegPassword.setVisibility(View.GONE);
            b.tilConfirmPassword.setVisibility(View.GONE);
            b.tvRegisterSubtitle.setText("Complete your profile to continue");
            
            String googleEmail = getIntent().getStringExtra("googleEmail");
            String googleName = getIntent().getStringExtra("googleName");
            if (!TextUtils.isEmpty(googleEmail)) {
                b.etRegEmail.setText(googleEmail);
                b.etRegEmail.setEnabled(false);
            }
            if (!TextUtils.isEmpty(googleName)) {
                b.etFullName.setText(googleName);
            }
        }

        // 3. Interactive Click Listeners with Micro-animations
        b.btnRegister.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnRegister);
            doRegister();
        });

        b.tvLoginLink.setOnClickListener(v -> {
            UiAnimations.pulse(b.tvLoginLink);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
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

        b.etRegEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilRegEmail.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.etRegPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilRegPassword.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilConfirmPassword.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void doRegister() {
        String name  = str(b.etFullName);
        String phone = str(b.etPhone);
        String schoolName = str(b.etSchoolName);
        String udiseCode = str(b.etUdiseCode);
        String email = str(b.etRegEmail);
        boolean isGoogleSignIn = getIntent().getBooleanExtra("isGoogleSignIn", false);
        String pass  = isGoogleSignIn ? "dummy_pass" : str(b.etRegPassword);
        String conf  = isGoogleSignIn ? "dummy_pass" : str(b.etConfirmPassword);

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

        if (TextUtils.isEmpty(email)) {
            b.tilRegEmail.setError("Email address is required");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilRegEmail.setError("Please enter a valid email address");
            valid = false;
        }

        if (TextUtils.isEmpty(pass)) {
            b.tilRegPassword.setError("Password is required");
            valid = false;
        } else if (pass.length() < 6) {
            b.tilRegPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else if (pass.length() > 128) {
            b.tilRegPassword.setError("Password too long (max 128 characters)");
            valid = false;
        }

        if (TextUtils.isEmpty(conf)) {
            b.tilConfirmPassword.setError("Confirm password is required");
            valid = false;
        } else if (!pass.equals(conf)) {
            b.tilConfirmPassword.setError("Passwords do not match");
            valid = false;
        }

        if (!valid) return;

        clearErrors();
        showLoading(true);

        if (isGoogleSignIn) {
            saveTeacherProfile(name, email, phone, schoolName, udiseCode);
            return;
        }

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    if (com.kartik.myschool.BuildConfig.DEBUG) { Log.d("AUTH", "createUserWithEmailAndPassword: SUCCESS uid=" +
                            (result.getUser() != null ? result.getUser().getUid() : "null")); }
                    saveTeacherProfile(name, email, phone, schoolName, udiseCode);
                })
                .addOnFailureListener(e -> {
                    if (com.kartik.myschool.BuildConfig.DEBUG) { Log.e("AUTH", "createUserWithEmailAndPassword: FAILED", e); }
                    showLoading(false);
                    String userMessage = getFriendlyAuthError(e);
                    showError("Registration Failed", userMessage);
                });
    }

    private void saveTeacherProfile(String name, String email, String phone, String schoolName, String udiseCode) {
        SessionContext.clear(RegisterActivity.this);
        Teacher t = new Teacher();
        t.name  = name;
        t.email = email;
        t.phone = phone;
        t.schoolName = schoolName;
        t.udiseCode = udiseCode;
        repo.saveTeacher(t, new FirebaseRepository.OnResult<Void>() {
            @Override public void onSuccess(Void v) {
                if (com.kartik.myschool.BuildConfig.DEBUG) { Log.d("AUTH", "saveTeacher: SUCCESS"); }
                showLoading(false);
                Intent intent = new Intent(RegisterActivity.this, SchoolRegisterActivity.class);
                intent.putExtra("is_onboarding", true);
                startActivity(intent);
                finishAffinity();
            }
            @Override public void onError(Exception e) {
                if (com.kartik.myschool.BuildConfig.DEBUG) { Log.e("AUTH", "saveTeacher: FAILED", e); }
                showLoading(false);
                showError("Firestore Error", "Account authenticated but profile save failed.\n" + e.getMessage());
            }
        });
    }

    private void clearErrors() {
        b.tilFullName.setError(null);
        b.tilPhone.setError(null);
        b.tilSchoolName.setError(null);
        b.tilUdiseCode.setError(null);
        b.tilRegEmail.setError(null);
        b.tilRegPassword.setError(null);
        b.tilConfirmPassword.setError(null);
    }

    private String getFriendlyAuthError(Exception e) {
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            if (com.kartik.myschool.BuildConfig.DEBUG) { Log.e("AUTH", "Firebase error code: " + code); }
            switch (code) {
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Email/Password sign-in is DISABLED in Firebase Console.\n\n" +
                           "Go to: Firebase Console → Authentication → Sign-in method → " +
                           "Email/Password → Enable → Save.";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "This email address is already registered. Try logging in instead.";
                case "ERROR_WEAK_PASSWORD":
                    return "Password is too weak. Use at least 6 characters.";
                case "ERROR_INVALID_EMAIL":
                    return "The email address is badly formatted.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "No internet connection. Please check your network and try again.";
                default:
                    return "Error (" + code + "): " + e.getMessage();
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error occurred.";
    }

    private void showLoading(boolean show) {
        b.registerProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnRegister.setEnabled(!show);
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
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }
}
