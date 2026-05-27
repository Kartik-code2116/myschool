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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    public void openDrawer() {
        if (b != null && b.drawerLayout != null) {
            b.drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SessionContext.load(this);
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
        topLevel.add(R.id.nav_settings);
        topLevel.add(R.id.nav_formative);
        topLevel.add(R.id.nav_descriptive);
        topLevel.add(R.id.nav_attendance);
        topLevel.add(R.id.nav_subjects);
        topLevel.add(R.id.nav_weightage);
        topLevel.add(R.id.nav_dropdown);
        topLevel.add(R.id.nav_extra);
        topLevel.add(R.id.nav_print_report);

        appBarConfig = new AppBarConfiguration.Builder(topLevel)
                .setOpenableLayout(b.drawerLayout)
                .build();

        b.btnMenu.setOnClickListener(v -> b.drawerLayout.openDrawer(GravityCompat.START));
        b.ivProfilePic.setOnClickListener(v -> navigateToAnimated(R.id.nav_profile));

        b.navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            boolean handled = navigateToAnimated(id);
            if (handled) b.drawerLayout.closeDrawer(GravityCompat.START);
            return handled;
        });

        b.btnToolbarMore.setOnClickListener(this::handleHomeMoreClick);

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
            } else if (id == R.id.nav_school_info) {
                title = "School Information";
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_gender) {
                title = "Gender";
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_cast_category) {
                title = "Cast Category";
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_class_teacher) {
                title = "Class Teacher";
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_classes) {
                title = "Classes";
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_subject) {
                title = "Subject";
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_default_values) {
                title = "Default Values";
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_working_days) {
                title = "Working Days";
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_he_she_items) {
                title = "He/She Items";
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_extra) {
                title = getString(R.string.menu_extra_menus);
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_print_report) {
                title = "Report Printing";
                String cls = (SessionContext.selectedClass != null && SessionContext.selectedClass.className != null) ? SessionContext.selectedClass.className : "1";
                String div = (SessionContext.selectedClass != null && SessionContext.selectedClass.division != null && !SessionContext.selectedClass.division.isEmpty()) ? SessionContext.selectedClass.division : "1";
                int sem = (SessionContext.selectedSemester != null) ? SessionContext.selectedSemester.number : 1;
                subtitle = "• Class: " + cls + " • Div: " + div + " • Semester: " + sem;
            } else if (id == R.id.nav_settings) {
                title = getString(R.string.drawer_settings);
                subtitle = "App Settings & Configurations";
            }
            updateToolbar(title, subtitle);
            syncBottomNavSelection(id);

            if (id == R.id.nav_info_print || id == R.id.nav_profile || id == R.id.nav_extra) {
                b.btnToolbarMore.setVisibility(View.VISIBLE);
                b.ivProfilePic.setVisibility(View.VISIBLE);
            } else {
                b.btnToolbarMore.setVisibility(View.GONE);
                b.ivProfilePic.setVisibility(View.VISIBLE);
            }
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
                || destinationId == R.id.nav_students
                || destinationId == R.id.nav_extra) {
            b.bottomNav.setSelectedItemId(destinationId);
        }
    }

    private void setupDrawerHeader() {
        View header = b.navigationView.getHeaderView(0);
        if (header == null) return;

        // Apply dynamic system window insets (notch / status bar) as top padding
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(
                v.getPaddingLeft(),
                statusBarTop + (int) (16 * v.getResources().getDisplayMetrics().density),
                v.getPaddingRight(),
                v.getPaddingBottom()
            );
            return insets;
        });

        android.widget.TextView tvName = header.findViewById(R.id.tvDrawerAppName);
        android.widget.TextView tvId = header.findViewById(R.id.tvDrawerTeacherId);

        if (tvName != null) {
            if (SessionContext.selectedSchool != null && SessionContext.selectedSchool.name != null && !SessionContext.selectedSchool.name.isEmpty()) {
                tvName.setText(SessionContext.selectedSchool.name);
            } else {
                tvName.setText("CCE110");
            }
        }

        if (tvId != null) {
            if (SessionContext.selectedSchool != null && SessionContext.selectedSchool.udiseCode != null && !SessionContext.selectedSchool.udiseCode.isEmpty()) {
                tvId.setText(SessionContext.selectedSchool.udiseCode);
            } else {
                tvId.setText("27251208204");
            }
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
            navigateToAnimated(R.id.nav_settings);
        });
        header.findViewById(R.id.btnDrawerPrint).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            navigateToAnimated(R.id.nav_print_report);
        });
        header.findViewById(R.id.btnDrawerStats).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            navigateToAnimated(R.id.nav_info_print);
        });
    }

    private void loadTeacherInfo() {
        if (AppCache.cachedTeacherName != null) {
            b.tvToolbarSubtitle.setText(AppCache.cachedTeacherName);
        }
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override public void onSuccess(Teacher t) {
                if (t != null && t.name != null) {
                    AppCache.cachedTeacherName = t.name;
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
            b.btnToolbarMore.setOnClickListener(this::handleHomeMoreClick);
            if (navController != null && navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() == R.id.nav_info_print) {
                b.btnToolbarMore.setVisibility(View.VISIBLE);
            } else {
                b.btnToolbarMore.setVisibility(View.GONE);
            }
            b.ivProfilePic.setVisibility(View.VISIBLE);
        }
    }

    public void navigateTo(int destId) {
        if (navController == null) return;
        if (destId == R.id.nav_info_print || destId == R.id.nav_profile || destId == R.id.nav_students || destId == R.id.nav_extra) {
            navController.navigate(destId, null, UiAnimations.navCrossFade());
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

    public void showHomeMoreMenu(View anchor) {
        if (navController != null) {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
            
            // Core options
            popup.getMenu().add(0, R.id.nav_info_print, 1, "🏠 Home");
            popup.getMenu().add(0, R.id.nav_class_div, 2, "🏫 Classes");
            popup.getMenu().add(0, R.id.nav_students, 3, "👥 Students");
            
            // Utility options
            popup.getMenu().add(0, 901, 4, "🌐 Language setting");
            popup.getMenu().add(0, 902, 5, "⭐ Rate app");
            popup.getMenu().add(0, 903, 6, "📱 More apps");
            popup.getMenu().add(0, 904, 7, "ℹ️ About developers");
            popup.getMenu().add(0, 999, 8, "🚪 " + getString(R.string.logout));

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 901) {
                    // Fully functional bilingual language selector dialog
                    String[] languages = {"English", "मराठी (Marathi)"};
                    android.content.SharedPreferences prefs = getSharedPreferences("myschool_settings_prefs", MODE_PRIVATE);
                    String currentLang = prefs.getString("language", "en");
                    int checkedItem = "mr".equals(currentLang) ? 1 : 0;

                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Select Language / भाषा निवडा")
                            .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                                String selectedLang = (which == 1) ? "mr" : "en";
                                changeLanguage(selectedLang);
                                dialog.dismiss();
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return true;
                } else if (id == 902) {
                    Toast.makeText(this, "Thank you for rating us 5 stars!", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == 903) {
                    Toast.makeText(this, "Opening More Apps on Play Store...", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == 904) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("About Developer")
                            .setMessage("Developed with ❤️ by Sanjay Gore\nVersion 24.04.14")
                            .setPositiveButton(android.R.string.ok, null).show();
                    return true;
                } else if (id == 999) {
                    confirmLogout();
                    return true;
                }
                navigateToAnimated(id);
                return true;
            });
            popup.show();
        }
    }

    private void handleHomeMoreClick(View v) {
        showHomeMoreMenu(v);
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.logout).setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.logout, (d, w) -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    SessionContext.clear(this);
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    public void changeLanguage(String lang) {
        android.content.SharedPreferences prefs = getSharedPreferences("myschool_settings_prefs", MODE_PRIVATE);
        String currentLang = prefs.getString("language", "en");
        if (currentLang.equals(lang)) return;

        prefs.edit().putString("language", lang).apply();

        // Apply locale runtime change to application and context resources
        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);
        android.content.res.Resources res = getResources();
        android.content.res.Configuration config = new android.content.res.Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        Toast.makeText(this, "Language updated / भाषा बदलली", Toast.LENGTH_SHORT).show();

        // Recreate activity to force reinflating components with new resource locale bundle
        recreate();
    }
}
