package com.example.myschool;

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

import com.example.myschool.databinding.ActivityRegisterBinding;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.UiAnimations;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding b;
    private FirebaseAuth auth;
    private final FirebaseRepository repo = FirebaseRepository.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = FirebaseAuth.getInstance();

        // Diagnostic: confirm which Firebase project is connected
        Log.d("AUTH", "Firebase Auth initialized. App name: " +
                com.google.firebase.FirebaseApp.getInstance().getName() +
                " | Current user: " + (auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "none"));

        // 1. Staggered Entrance Animation (premium feel)
        b.logoContainer.setAlpha(0f);
        b.tvRegisterTitle.setAlpha(0f);
        b.tvRegisterSubtitle.setAlpha(0f);
        b.tilFullName.setAlpha(0f);
        b.tilPhone.setAlpha(0f);
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
                b.tilRegEmail,
                b.tilRegPassword,
                b.tilConfirmPassword,
                b.btnRegister,
                b.tvLoginLink
        ));

        // 2. Real-time dynamic input error clearing
        setupRealTimeValidation();

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
        String email = str(b.etRegEmail);
        String pass  = str(b.etRegPassword);
        String conf  = str(b.etConfirmPassword);

        // Validation
        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            b.tilFullName.setError("Full name is required");
            valid = false;
        }

        if (TextUtils.isEmpty(phone)) {
            b.tilPhone.setError("Phone number is required");
            valid = false;
        } else if (phone.length() < 10) {
            b.tilPhone.setError("Please enter a valid phone number");
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

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    Log.d("AUTH", "createUserWithEmailAndPassword: SUCCESS uid=" +
                            (result.getUser() != null ? result.getUser().getUid() : "null"));
                    SessionContext.clear(RegisterActivity.this);
                    Teacher t = new Teacher();
                    t.name  = name;
                    t.email = email;
                    t.phone = phone;
                    repo.saveTeacher(t, new FirebaseRepository.OnResult<Void>() {
                        @Override public void onSuccess(Void v) {
                            Log.d("AUTH", "saveTeacher: SUCCESS");
                            showLoading(false);
                            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                            finishAffinity();
                        }
                        @Override public void onError(Exception e) {
                            Log.e("AUTH", "saveTeacher: FAILED", e);
                            showLoading(false);
                            showError("Firestore Error", "Account created but profile save failed.\n" + e.getMessage());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("AUTH", "createUserWithEmailAndPassword: FAILED", e);
                    showLoading(false);
                    String userMessage = getFriendlyAuthError(e);
                    showError("Registration Failed", userMessage);
                });
    }

    private void clearErrors() {
        b.tilFullName.setError(null);
        b.tilPhone.setError(null);
        b.tilRegEmail.setError(null);
        b.tilRegPassword.setError(null);
        b.tilConfirmPassword.setError(null);
    }

    private String getFriendlyAuthError(Exception e) {
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            Log.e("AUTH", "Firebase error code: " + code);
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
