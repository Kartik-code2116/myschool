package com.example.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myschool.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = FirebaseAuth.getInstance();

        b.btnLogin.setOnClickListener(v -> doLogin());
        b.tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        b.tvForgotPassword.setOnClickListener(v -> sendReset());
        b.btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show());
    }

    private void doLogin() {
        String email = str(b.etEmail);
        String pass = str(b.etPassword);
        if (TextUtils.isEmpty(email)) { b.tilEmail.setError("Required"); return; }
        if (TextUtils.isEmpty(pass))  { b.tilPassword.setError("Required"); return; }
        b.tilEmail.setError(null); b.tilPassword.setError(null);
        showLoading(true);
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    showLoading(false);
                    startActivity(new Intent(this, HomeActivity.class));
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void sendReset() {
        String email = str(b.etEmail);
        if (TextUtils.isEmpty(email)) { b.tilEmail.setError("Enter email first"); return; }
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> Toast.makeText(this, "Reset email sent", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showLoading(boolean show) {
        b.loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnLogin.setEnabled(!show);
    }

    private String str(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
