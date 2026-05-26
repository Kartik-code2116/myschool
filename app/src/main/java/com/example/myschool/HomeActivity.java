package com.example.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myschool.databinding.ActivityHomeBinding;
import com.example.myschool.model.Teacher;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.UiAnimations;

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
        topLevel.add(R.id.nav_profile);
        topLevel.add(R.id.nav_class_div);
        topLevel.add(R.id.nav_students);

        appBarConfig = new AppBarConfiguration.Builder(topLevel)
                .setOpenableLayout(b.drawerLayout)
                .build();

        b.btnMenu.setOnClickListener(v -> b.drawerLayout.openDrawer(GravityCompat.START));
        b.ivProfilePic.setOnClickListener(v -> navigateToAnimated(R.id.nav_profile));

        b.navigationView.setNavigationItemSelectedListener(item -> {
            boolean handled = navigateToAnimated(item.getItemId());
            if (handled) b.drawerLayout.closeDrawer(GravityCompat.START);
            return handled;
        });

        setupDrawerHeader();
        setupDrawerActions();

        b.bottomNav.setOnItemSelectedListener(this::navigateBottomItem);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination == null) return;
            int id = destination.getId();
            String title = destination.getLabel() != null ? destination.getLabel().toString() : "";
            String subtitle = getString(R.string.subtitle_info_print);
            if (id == R.id.nav_students) {
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_class_div) {
                subtitle = getString(R.string.subtitle_class_div);
            } else if (id == R.id.nav_reports) {
                subtitle = getString(R.string.nav_reports);
            } else if (id == R.id.nav_info_print) {
                title = getString(R.string.nav_home);
            } else if (id == R.id.nav_profile) {
                subtitle = getString(R.string.profile_subtitle);
            } else if (id == R.id.nav_subjects) {
                title = "Subject";
                subtitle = SessionContext.getClassDivLabel();
            }
            updateToolbar(title, subtitle);
            syncBottomNavSelection(id);
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
            navigateTo(dest);
            intent.removeExtra("navigate_to");
        }
    }

    private boolean navigateToAnimated(int destId) {
        if (navController == null) return false;
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destId) {
            return true;
        }
        navigateTo(destId);
        return true;
    }

    private boolean navigateBottomItem(android.view.MenuItem item) {
        int id = item.getItemId();
        boolean handled = navigateToAnimated(id);
        return handled;
    }

    private void syncBottomNavSelection(int destinationId) {
        if (destinationId == R.id.nav_info_print
                || destinationId == R.id.nav_profile
                || destinationId == R.id.nav_students) {
            b.bottomNav.setSelectedItemId(destinationId);
        }
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
            navigateToAnimated(R.id.nav_profile);
        });
        header.findViewById(R.id.btnDrawerSettings).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            navController.navigate(R.id.nav_info_print);
        });
        header.findViewById(R.id.btnDrawerPrint).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(HomeActivity.this, "Reports feature coming soon!", Toast.LENGTH_SHORT).show();
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

    public void showCustomToolbarActions(boolean show, View.OnClickListener onNotificationsClick, View.OnClickListener onMoreClick) {
        if (b == null) return;
        if (show) {
            b.btnToolbarNotifications.setVisibility(View.VISIBLE);
            b.btnToolbarMore.setVisibility(View.VISIBLE);
            b.btnToolbarNotifications.setOnClickListener(onNotificationsClick);
            b.btnToolbarMore.setOnClickListener(onMoreClick);
            b.ivProfilePic.setVisibility(View.GONE);
        } else {
            b.btnToolbarNotifications.setVisibility(View.GONE);
            b.btnToolbarMore.setVisibility(View.GONE);
            b.ivProfilePic.setVisibility(View.VISIBLE);
        }
    }

    public void navigateTo(int destId) {
        if (navController == null) return;
        if (destId == R.id.nav_info_print || destId == R.id.nav_profile) {
            navController.navigate(destId);
        } else {
            navController.navigate(destId, null, UiAnimations.navSlideForward());
        }
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
