package com.example.myschool;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.myschool.databinding.ActivityHomeBinding;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding b;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Setup Navigation
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHost != null) {
            navController = navHost.getNavController();
            NavigationUI.setupWithNavController(b.bottomNav, navController);
        }

        // Load teacher name into toolbar
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(com.example.myschool.model.Teacher t) {
                if (t != null && t.name != null) {
                    runOnUiThread(() -> b.tvTeacherName.setText(t.name));
                }
            }
            @Override public void onError(Exception e) {}
        });

        b.ivProfilePic.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    // Called by fragments to update toolbar title
    public void setToolbarTitle(String title) {
        b.tvTeacherName.setText(title);
    }
}
