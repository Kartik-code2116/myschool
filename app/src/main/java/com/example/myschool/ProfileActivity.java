package com.example.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myschool.adapter.ProfileClassAdapter;
import com.example.myschool.databinding.ActivityProfileBinding;
import com.example.myschool.model.AcademicYear;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.School;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding b;
    private FirebaseAuth auth;
    private Teacher currentTeacher;
    private ProfileClassAdapter classAdapter;
    private boolean editMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        editMode = getIntent().getBooleanExtra("edit_mode", true);

        classAdapter = new ProfileClassAdapter();
        classAdapter.setListener(c -> {
            SessionContext.selectedClass = c;
            SessionContext.syncToAppCache();
            startActivity(new Intent(this, HomeActivity.class)
                    .putExtra("navigate_to", R.id.nav_class_div)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        b.rvProfileClasses.setLayoutManager(new LinearLayoutManager(this));
        b.rvProfileClasses.setAdapter(classAdapter);

        b.btnBack.setOnClickListener(v -> finish());
        b.btnEditProfile.setOnClickListener(v -> setFieldsEnabled(true));
        b.btnSaveProfile.setOnClickListener(v -> saveProfile());
        b.btnLogout.setOnClickListener(v -> confirmLogout());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher teacher) {
                currentTeacher = teacher != null ? teacher : new Teacher();
                bindTeacher(currentTeacher);
                loadClasses();
                FirebaseRepository.get().ensureTeacherSchool(currentTeacher,
                        new FirebaseRepository.OnResult<School>() {
                            @Override public void onSuccess(School s) {
                                SessionContext.selectedSchool = s;
                                AppCache.selectedSchool = s;
                            }
                            @Override public void onError(Exception e) {}
                        });
            }
            @Override public void onError(Exception e) {
                Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindTeacher(Teacher t) {
        b.etFullName.setText(t.name != null ? t.name : "");
        b.etPhone.setText(t.phone != null ? t.phone : "");
        b.etUdise.setText(t.udiseCode != null ? t.udiseCode : "");
        String year = t.academicYearLabel != null ? t.academicYearLabel : SessionContext.getYearLabel();
        b.etYear.setText(year);
        b.etPasteLink.setText(t.pasteLinkResult != null ? t.pasteLinkResult : "");
        setFieldsEnabled(editMode);
    }

    private void setFieldsEnabled(boolean enabled) {
        b.etFullName.setEnabled(enabled);
        b.etPhone.setEnabled(enabled);
        b.etUdise.setEnabled(enabled);
        b.etYear.setEnabled(enabled);
        b.etPasteLink.setEnabled(enabled);
        b.btnSaveProfile.setVisibility(enabled ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void loadClasses() {
        AcademicYear y = SessionContext.selectedYear;
        if (y != null && y.id != null) {
            FirebaseRepository.get().getClassesForYear(y.id, new FirebaseRepository.OnResult<List<ClassModel>>() {
                @Override public void onSuccess(List<ClassModel> list) {
                    if (!isFinishing() && !isDestroyed()) {
                        classAdapter.setData(list);
                    }
                }
                @Override public void onError(Exception e) { loadClassesBySchool(); }
            });
        } else {
            loadClassesBySchool();
        }
    }

    private void loadClassesBySchool() {
        if (SessionContext.selectedSchool == null) return;
        FirebaseRepository.get().getClassesForSchool(SessionContext.selectedSchool.id,
                new FirebaseRepository.OnResult<List<ClassModel>>() {
                    @Override public void onSuccess(List<ClassModel> list) {
                        if (!isFinishing() && !isDestroyed()) {
                            classAdapter.setData(list);
                        }
                    }
                    @Override public void onError(Exception e) {}
                });
    }

    private void saveProfile() {
        String name = str(b.etFullName);
        if (TextUtils.isEmpty(name)) {
            b.tilName.setError("Required");
            return;
        }
        b.tilName.setError(null);

        Teacher t = currentTeacher != null ? currentTeacher : new Teacher();
        t.name = name;
        t.phone = str(b.etPhone);
        t.udiseCode = str(b.etUdise);
        t.academicYearLabel = str(b.etYear);
        t.pasteLinkResult = str(b.etPasteLink);
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) t.email = user.getEmail();

        FirebaseRepository.get().saveTeacher(t, new FirebaseRepository.OnResult<Void>() {
            @Override public void onSuccess(Void v) {
                currentTeacher = t;
                SessionContext.selectedYear = null;
                Toast.makeText(ProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {
                    @Override public void onSuccess(School s) {
                        SessionContext.selectedSchool = s;
                        AppCache.selectedSchool = s;
                    }
                    @Override public void onError(Exception e) {}
                });
                setFieldsEnabled(false);
            }
            @Override public void onError(Exception e) {
                Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton(R.string.logout, (d, w) -> {
                    auth.signOut();
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String str(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClasses();
    }
}
