package com.example.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myschool.databinding.ActivityHomeBinding;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;
import java.util.HashSet;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding b;
    private NavController navController;
    private AppBarConfiguration appBarConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHost == null) return;
        navController = navHost.getNavController();

        Set<Integer> topLevel = new HashSet<>();
        topLevel.add(R.id.nav_info_print);
        topLevel.add(R.id.nav_class_div);
        topLevel.add(R.id.nav_students);
        topLevel.add(R.id.nav_reports);

        appBarConfig = new AppBarConfiguration.Builder(topLevel)
                .setOpenableLayout(b.drawerLayout)
                .build();

        b.btnMenu.setOnClickListener(v -> b.drawerLayout.openDrawer(GravityCompat.START));
        b.ivProfilePic.setOnClickListener(v -> openProfile());

        b.navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                b.drawerLayout.closeDrawer(GravityCompat.START);
                openProfile();
                return true;
            }
            if (item.getItemId() == R.id.nav_print_report) {
                b.drawerLayout.closeDrawer(GravityCompat.START);
                navController.navigate(R.id.nav_reports);
                return true;
            }
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) b.drawerLayout.closeDrawer(GravityCompat.START);
            return handled;
        });

        setupDrawerHeader();
        setupDrawerActions();

        b.bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                openProfile();
                return false;
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            updateToolbar(destination.getLabel() != null ? destination.getLabel().toString() : "",
                    getString(R.string.subtitle_info_print));
        });

        loadTeacherInfo();
        SessionContext.syncFromAppCache();
        handleNavigationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent == null || navController == null) return;
        int dest = intent.getIntExtra("navigate_to", 0);
        if (dest != 0) {
            navController.navigate(dest);
            intent.removeExtra("navigate_to");
        }
    }

    private void openProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void setupDrawerHeader() {
        View header = b.navigationView.getHeaderView(0);
        if (header == null) return;
        String uid = FirebaseRepository.get().currentUid();
        android.widget.TextView tvId = header.findViewById(R.id.tvDrawerTeacherId);
        if (tvId != null && uid != null) {
            tvId.setText(uid.length() > 11 ? uid.substring(0, 11) : uid);
        }
    }

    private void setupDrawerActions() {
        View header = b.navigationView.getHeaderView(0);
        if (header == null) return;
        header.findViewById(R.id.btnDrawerProfile).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            openProfile();
        });
        header.findViewById(R.id.btnDrawerSettings).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            navController.navigate(R.id.nav_info_print);
        });
        header.findViewById(R.id.btnDrawerPrint).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            navController.navigate(R.id.nav_reports);
        });
        header.findViewById(R.id.btnDrawerStats).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            navController.navigate(R.id.nav_info_print);
        });
    }

    private void loadTeacherInfo() {
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher t) {
                if (t != null && t.name != null) {
                    runOnUiThread(() -> b.tvToolbarSubtitle.setText(t.name));
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    public void updateToolbar(String title, String subtitle) {
        if (title != null && !title.isEmpty()) b.tvToolbarTitle.setText(title);
        if (subtitle != null && !subtitle.isEmpty()) b.tvToolbarSubtitle.setText(subtitle);
    }

    public void navigateTo(int destId) {
        navController.navigate(destId);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfig) || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            b.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
