package com.example.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myschool.databinding.ActivityRegisterBinding;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;
import com.google.firebase.auth.FirebaseAuth;

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

        b.btnRegister.setOnClickListener(v -> doRegister());
        b.tvLoginLink.setOnClickListener(v -> finish());
    }

    private void doRegister() {
        String name  = str(b.etFullName);
        String phone = str(b.etPhone);
        String email = str(b.etRegEmail);
        String pass  = str(b.etRegPassword);
        String conf  = str(b.etConfirmPassword);

        if (TextUtils.isEmpty(name))  { b.tilFullName.setError("Required"); return; }
        if (TextUtils.isEmpty(email)) { b.tilRegEmail.setError("Required"); return; }
        if (TextUtils.isEmpty(pass))  { b.tilRegPassword.setError("Required"); return; }
        if (!pass.equals(conf))       { b.tilConfirmPassword.setError("Passwords do not match"); return; }
        if (pass.length() < 6)        { b.tilRegPassword.setError("Min 6 characters"); return; }

        clearErrors();
        showLoading(true);

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    Teacher t = new Teacher();
                    t.name  = name;
                    t.email = email;
                    t.phone = phone;
                    repo.saveTeacher(t, new FirebaseRepository.OnResult<Void>() {
                        @Override public void onSuccess(Void v) {
                            showLoading(false);
                            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                            finishAffinity();
                        }
                        @Override public void onError(Exception e) {
                            showLoading(false);
                            Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearErrors() {
        b.tilFullName.setError(null);
        b.tilRegEmail.setError(null);
        b.tilRegPassword.setError(null);
        b.tilConfirmPassword.setError(null);
    }

    private void showLoading(boolean show) {
        b.registerProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnRegister.setEnabled(!show);
    }

    private String str(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
