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

import com.kartik.myschool.databinding.ActivityLoginBinding;
import com.kartik.myschool.model.Teacher;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.CredentialManagerCallback;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;

public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding b;
    private FirebaseAuth auth;
    private final FirebaseRepository repo = FirebaseRepository.get();
    private CredentialManager credentialManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Screen opened: " + this.getClass().getSimpleName());
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // Staggered Entrance Animation
        b.logoContainer.setAlpha(0f);
        b.tvLoginTitle.setAlpha(0f);
        b.tvLoginSubtitle.setAlpha(0f);
        b.tilEmail.setAlpha(0f);
        b.tilPassword.setAlpha(0f);
        b.tvForgotPassword.setAlpha(0f);
        b.btnLogin.setAlpha(0f);
        b.layoutOrDivider.setAlpha(0f);
        b.btnGoogle.setAlpha(0f);
        b.tvRegisterLink.setAlpha(0f);

        b.getRoot().post(() -> UiAnimations.staggerFadeIn(
                b.logoContainer,
                b.tvLoginTitle,
                b.tvLoginSubtitle,
                b.tilEmail,
                b.tilPassword,
                b.tvForgotPassword,
                b.btnLogin,
                b.layoutOrDivider,
                b.btnGoogle,
                b.tvRegisterLink
        ));

        setupRealTimeValidation();

        b.tvPrivacyPolicy.setOnClickListener(v -> {
            UiAnimations.pulse(b.tvPrivacyPolicy);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://kartik-28deb.web.app/privacy_policy.html"));
            startActivity(browserIntent);
        });

        b.btnLogin.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnLogin);
            doLogin();
        });

        b.tvRegisterLink.setOnClickListener(v -> {
            UiAnimations.pulse(b.tvRegisterLink);
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        b.tvForgotPassword.setOnClickListener(v -> {
            UiAnimations.pulse(b.tvForgotPassword);
            sendReset();
        });

        b.btnGoogle.setOnClickListener(v -> {
            UiAnimations.pulse(b.btnGoogle);
            doGoogleSignIn();
        });
    }

    private void setupRealTimeValidation() {
        b.etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilEmail.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.tilPassword.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void doLogin() {
        String email = str(b.etEmail);
        String pass  = str(b.etPassword);

        // --- Input validation ---
        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            b.tilEmail.setError("Email address is required");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.setError("Please enter a valid email address");
            valid = false;
        }
        if (TextUtils.isEmpty(pass)) {
            b.tilPassword.setError("Password is required");
            valid = false;
        } else if (pass.length() < 6) {
            b.tilPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else if (pass.length() > 128) {
            b.tilPassword.setError("Password too long (max 128 characters)");
            valid = false;
        }
        if (!valid) return;

        b.tilEmail.setError(null);
        b.tilPassword.setError(null);
        showLoading(true);

        // --- Step 1: Authenticate with Firebase Auth ---
        // Only succeeds if this exact email+password exists in Firebase Authentication.
        // If the account does not exist, Firebase returns ERROR_USER_NOT_FOUND.
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser() != null ? result.getUser().getUid() : null;
                    if (com.kartik.myschool.BuildConfig.DEBUG) { Log.d(TAG, "Firebase Auth SUCCESS uid=" + uid); }

                    if (uid == null) {
                        // Should never happen, but guard anyway
                        auth.signOut();
                        showLoading(false);
                        showError("Login Failed", "Authentication error: no user ID returned. Please try again.");
                        return;
                    }

                    // --- Step 2: Verify teacher profile exists in Firestore ---
                    // Prevents login if the account exists in Auth but has no app profile.
                    repo.getTeacher(new FirebaseRepository.OnResult<Teacher>() {
                        @Override
                        public void onSuccess(Teacher teacher) {
                            showLoading(false);
                            if (teacher == null) {
                                if (com.kartik.myschool.BuildConfig.DEBUG) { Log.w(TAG, "No teacher profile in Firestore for uid=" + uid + ". Auto-repairing."); }
                                Teacher newTeacher = new Teacher();
                                newTeacher.id = uid;
                                newTeacher.email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : email;
                                newTeacher.name = "Teacher";
                                SessionContext.clear(LoginActivity.this);
                                repo.saveTeacher(newTeacher, new FirebaseRepository.OnResult<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        SessionContext.clear(LoginActivity.this);
                                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                        finishAffinity();
                                    }
                                    @Override
                                    public void onError(Exception ex) {
                                        auth.signOut();
                                        showError("Account Incomplete", "Your account is missing a profile and auto-repair failed: " + ex.getMessage());
                                    }
                                });
                                return;
                            }
                            // Both Auth + Firestore OK — proceed to app
                            if (com.kartik.myschool.BuildConfig.DEBUG) { Log.d(TAG, "Teacher profile found: " + teacher.name + ". Navigating to Home."); }
                            SessionContext.clear(LoginActivity.this);
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finishAffinity();
                        }

                        @Override
                        public void onError(Exception e) {
                            if (com.kartik.myschool.BuildConfig.DEBUG) { Log.e(TAG, "Firestore getTeacher FAILED", e); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Firestore getTeacher FAILED"); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e); }
                            auth.signOut();
                            showLoading(false);
                            showError("Login Failed",
                                    "Could not load your profile from the database.\n" + e.getMessage());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (com.kartik.myschool.BuildConfig.DEBUG) { Log.e(TAG, "Firebase Auth FAILED", e); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Firebase Auth FAILED"); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e); }
                    showLoading(false);
                    showError("Login Failed", getFriendlyLoginError(e));
                });
    }

    /**
     * Maps Firebase Auth error codes to clear, user-friendly messages.
     */
    private String getFriendlyLoginError(Exception e) {
        // FirebaseAuthInvalidUserException covers: user not found, disabled, email changed
        if (e instanceof FirebaseAuthInvalidUserException) {
            String code = ((FirebaseAuthInvalidUserException) e).getErrorCode();
            if (com.kartik.myschool.BuildConfig.DEBUG) { Log.e(TAG, "Auth error code: " + code); }
            switch (code) {
                case "ERROR_USER_NOT_FOUND":
                    return "No account found with this email address.\n\nPlease check the email or register a new account.";
                case "ERROR_USER_DISABLED":
                    return "This account has been disabled. Please contact support.";
                default:
                    return "Account error: " + e.getMessage();
            }
        }
        // FirebaseAuthInvalidCredentialsException covers: wrong password, invalid email format
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if (com.kartik.myschool.BuildConfig.DEBUG) { Log.e(TAG, "Auth error code: " + code); }
            switch (code) {
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL":
                    return "Incorrect password. Please try again or use Forgot Password.";
                case "ERROR_INVALID_EMAIL":
                    return "The email address format is invalid.";
                default:
                    return "Invalid credentials: " + e.getMessage();
            }
        }
        // General FirebaseAuthException
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            if (com.kartik.myschool.BuildConfig.DEBUG) { Log.e(TAG, "Auth error code: " + code); }
            switch (code) {
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "No internet connection. Please check your network and try again.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Too many failed login attempts. Account temporarily locked.\nTry again later or reset your password.";
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Email/Password login is disabled.\n\nEnable it in Firebase Console → Authentication → Sign-in method.";
                default:
                    return "Error (" + code + "): " + e.getMessage();
            }
        }
        return e.getMessage() != null ? e.getMessage() : "An unknown error occurred. Please try again.";
    }

    private void doGoogleSignIn() {
        showLoading(true);
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new android.os.CancellationSignal(),
                Runnable::run,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleSignInResult(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        showLoading(false);
                        if (!e.getType().equals(androidx.credentials.exceptions.GetCredentialCancellationException.TYPE_GET_CREDENTIAL_CANCELLATION_EXCEPTION)) {
                            showError("Google Sign-In", e.getMessage());
                        }
                    }
                }
        );
    }

    private void handleGoogleSignInResult(GetCredentialResponse result) {
        androidx.credentials.Credential credential = result.getCredential();
        if (credential instanceof CustomCredential &&
                ((CustomCredential) credential).getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            try {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
                String idToken = googleIdTokenCredential.getIdToken();

                com.google.firebase.auth.AuthCredential authCredential = GoogleAuthProvider.getCredential(idToken, null);
                auth.signInWithCredential(authCredential)
                        .addOnSuccessListener(authResult -> {
                            String uid = authResult.getUser().getUid();
                            String email = authResult.getUser().getEmail();
                            String name = authResult.getUser().getDisplayName();
                            checkIfTeacherExists(uid, email, name);
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            showError("Firebase Auth Failed", getFriendlyLoginError(e));
                        });
            } catch (Exception e) {
                showLoading(false);
                showError("Google Sign-In", "Failed to parse Google ID token: " + e.getMessage());
            }
        } else {
            showLoading(false);
            showError("Google Sign-In", "Unexpected credential type");
        }
    }

    private void checkIfTeacherExists(String uid, String email, String name) {
        repo.getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override
            public void onSuccess(Teacher t) {
                if (t != null) {
                    SessionContext.clear(LoginActivity.this);
                    showLoading(false);
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finishAffinity();
                } else {
                    // Direct sign-up: create the profile immediately
                    Teacher newTeacher = new Teacher();
                    newTeacher.id = uid;
                    newTeacher.email = email;
                    newTeacher.name = name != null ? name : "Teacher";
                    newTeacher.phone = ""; // Phone not provided by Google
                    
                    SessionContext.clear(LoginActivity.this);
                    repo.saveTeacher(newTeacher, new FirebaseRepository.OnResult<Void>() {
                        @Override
                        public void onSuccess(Void v) {
                            SessionContext.clear(LoginActivity.this);
                            showLoading(false);
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finishAffinity();
                        }

                        @Override
                        public void onError(Exception e) {
                            showLoading(false);
                            showError("Database Error", "Failed to create profile: " + e.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                showError("Database Error", "Failed to check profile: " + e.getMessage());
            }
        });
    }

    private void sendReset() {
        String email = str(b.etEmail);
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.setError("Enter a valid email address first");
            return;
        }
        showLoading(true);
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> {
                    showLoading(false);
                    showError("Reset Email Sent", "A password reset link has been sent to:\n" + email);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Reset Failed", getFriendlyLoginError(e));
                });
    }

    private void showLoading(boolean show) {
        b.loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnLogin.setEnabled(!show);
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
}

