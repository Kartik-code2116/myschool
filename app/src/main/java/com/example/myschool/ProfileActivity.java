package com.example.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myschool.databinding.ActivityProfileBinding;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding b;
    private FirebaseAuth auth;
    private Teacher currentTeacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        auth = FirebaseAuth.getInstance();

        // Check if user is logged in
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupClickListeners();
        loadTeacherProfile();
    }

    private void setupClickListeners() {
        b.btnBack.setOnClickListener(v -> finish());

        b.btnSaveProfile.setOnClickListener(v -> saveProfile());

        b.btnEditPhoto.setOnClickListener(v ->
            Toast.makeText(this, "Photo upload coming soon", Toast.LENGTH_SHORT).show()
        );

        b.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        b.btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
    }

    private void loadTeacherProfile() {
        showLoading(true);

        // Load from Firebase Auth
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            b.etEmail.setText(user.getEmail());
            b.tvProfileEmail.setText(user.getEmail());
        }

        // Load additional data from Firestore
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override
            public void onSuccess(Teacher teacher) {
                showLoading(false);
                if (teacher != null) {
                    currentTeacher = teacher;
                    b.etFullName.setText(teacher.name);
                    b.etPhone.setText(teacher.phone != null ? teacher.phone : "");

                    // Load profile photo
                    if (teacher.photoUrl != null && !teacher.photoUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(teacher.photoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_person)
                                .into(b.ivProfilePhoto);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(ProfileActivity.this,
                        "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String name = b.etFullName.getText() != null ? b.etFullName.getText().toString().trim() : "";
        String phone = b.etPhone.getText() != null ? b.etPhone.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            b.tilFullName.setError("Name is required");
            return;
        }
        b.tilFullName.setError(null);

        showLoading(true);

        // Create or update teacher object
        Teacher teacher = currentTeacher != null ? currentTeacher : new Teacher();
        teacher.name = name;
        teacher.phone = phone;

        // Get current user email
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            teacher.email = user.getEmail();
        }

        FirebaseRepository.get().saveTeacher(teacher, new FirebaseRepository.OnResult<Void>() {
            @Override
            public void onSuccess(Void result) {
                showLoading(false);
                Toast.makeText(ProfileActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                currentTeacher = teacher;
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(ProfileActivity.this,
                        "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangePasswordDialog() {
        // Simple dialog to change password
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter new password (min 6 chars)");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(input)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = input.getText().toString().trim();
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updatePassword(newPass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePassword(String newPassword) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        user.updatePassword(newPassword)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> doLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doLogout() {
        auth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Clear activity stack and go to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        b.btnSaveProfile.setEnabled(!show);
        // You can add a ProgressBar to the layout if needed
    }
}
