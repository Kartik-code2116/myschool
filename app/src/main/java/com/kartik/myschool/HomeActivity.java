package com.kartik.myschool;

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

import com.kartik.myschool.databinding.ActivityHomeBinding;
import com.kartik.myschool.model.Teacher;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;

import java.util.HashSet;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding b;
    private NavController navController;
    private AppBarConfiguration appBarConfig;
    private boolean isSchoolLevelExpanded = false;

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
        if (navHost == null)
            return;
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
        topLevel.add(R.id.nav_school_info);
        topLevel.add(R.id.nav_gender);
        topLevel.add(R.id.nav_cast_category);
        topLevel.add(R.id.nav_class_teacher);
        topLevel.add(R.id.nav_classes);
        topLevel.add(R.id.nav_subject);
        topLevel.add(R.id.nav_default_values);
        topLevel.add(R.id.nav_working_days);
        topLevel.add(R.id.nav_he_she_items);
        topLevel.add(R.id.nav_print_report);
        topLevel.add(R.id.nav_dashboard);

        appBarConfig = new AppBarConfiguration.Builder(topLevel)
                .setOpenableLayout(b.drawerLayout)
                .build();

        b.btnMenu.setOnClickListener(v -> b.drawerLayout.openDrawer(GravityCompat.START));
        b.ivProfilePic.setOnClickListener(v -> navigateToAnimated(R.id.nav_profile));

        b.btnToolbarHelp.setOnClickListener(v -> {
            if (navController != null && navController.getCurrentDestination() != null) {
                int destId = navController.getCurrentDestination().getId();
                String pageKey = "default";
                if (destId == R.id.nav_info_print) {
                    pageKey = "info_print";
                } else if (destId == R.id.nav_dashboard) {
                    pageKey = "stats_dashboard";
                } else if (destId == R.id.nav_class_div) {
                    pageKey = "class_div";
                } else if (destId == R.id.nav_reports || destId == R.id.nav_print_report) {
                    pageKey = "print_report";
                } else if (destId == R.id.nav_profile) {
                    pageKey = "profile";
                } else if (destId == R.id.nav_subjects) {
                    pageKey = "subjects";
                } else if (destId == R.id.nav_weightage) {
                    pageKey = "weightage";
                } else if (destId == R.id.nav_attendance) {
                    pageKey = "attendance";
                } else if (destId == R.id.nav_settings) {
                    pageKey = "settings";
                } else if (destId == R.id.nav_extra) {
                    pageKey = "school_settings";
                } else if (destId == R.id.nav_dropdown || destId == R.id.nav_school_info || destId == R.id.nav_gender || destId == R.id.nav_cast_category || destId == R.id.nav_class_teacher || destId == R.id.nav_classes || destId == R.id.nav_subject || destId == R.id.nav_default_values || destId == R.id.nav_working_days || destId == R.id.nav_he_she_items) {
                    pageKey = "school_settings";
                }
                com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(this, pageKey);
            }
        });

        b.navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_school_level_dropdown) {
                toggleSchoolLevelDropdown();
                return false;
            }
            boolean handled = navigateToAnimated(id);
            if (handled)
                b.drawerLayout.closeDrawer(GravityCompat.START);
            return handled;
        });

        b.btnToolbarMore.setOnClickListener(this::handleHomeMoreClick);

        setupDrawerHeader();
        setupDrawerActions();
        localizeSidebar();

        b.bottomNav.setOnItemSelectedListener(this::navigateBottomItem);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination == null)
                return;
            int id = destination.getId();
            String title = destination.getLabel() != null ? destination.getLabel().toString() : "";
            String subtitle = getString(R.string.subtitle_info_print);
            if (id == R.id.nav_edit_attendance) {
                title = "Upasthiti";
                subtitle = "";
            } else if (id == R.id.nav_students) {
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
            } else if (id == R.id.nav_weightage) {
                title = getString(R.string.menu_declare_weightage);
                subtitle = SessionContext.getClassDivLabel();
            } else if (id == R.id.nav_dropdown) {
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
                String cls = (SessionContext.selectedClass != null && SessionContext.selectedClass.className != null)
                        ? SessionContext.selectedClass.className
                        : "1";
                String div = (SessionContext.selectedClass != null && SessionContext.selectedClass.division != null
                        && !SessionContext.selectedClass.division.isEmpty()) ? SessionContext.selectedClass.division
                                : "1";
                int sem = (SessionContext.selectedSemester != null) ? SessionContext.selectedSemester.number : 1;
                subtitle = "• Class: " + cls + " • Div: " + div + " • Semester: " + sem;
            } else if (id == R.id.nav_settings) {
                title = getString(R.string.drawer_settings);
                subtitle = "App Settings & Configurations";
            } else if (id == R.id.nav_dashboard) {
                title = getString(R.string.title_stats_dashboard);
                String cls = (SessionContext.selectedClass != null && SessionContext.selectedClass.className != null)
                        ? SessionContext.selectedClass.className
                        : "N/A";
                String div = (SessionContext.selectedClass != null && SessionContext.selectedClass.division != null
                        && !SessionContext.selectedClass.division.isEmpty()) ? SessionContext.selectedClass.division
                                : "N/A";
                subtitle = getString(R.string.subtitle_progress_tracker, cls, div);
            }
            updateToolbar(title, subtitle);
            syncBottomNavSelection(id);

            if (id == R.id.nav_info_print || id == R.id.nav_profile || id == R.id.nav_school_info || id == R.id.nav_weightage || id == R.id.nav_extra || id == R.id.nav_attendance) {
                b.btnToolbarMore.setVisibility(View.VISIBLE);
            } else {
                b.btnToolbarMore.setVisibility(View.GONE);
            }

            if (id == R.id.nav_attendance) {
                title = "उपस्थिती";
                b.btnToolbarAdd.setVisibility(View.VISIBLE);
                b.btnToolbarCalc.setVisibility(View.VISIBLE);
            } else {
                b.btnToolbarAdd.setVisibility(View.GONE);
                b.btnToolbarCalc.setVisibility(View.GONE);
            }
        });

        loadTeacherInfo();
        SessionContext.syncFromAppCache();
        handleNavigationIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupDrawerHeader();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent == null || navController == null)
            return;
        int dest = intent.getIntExtra("navigate_to", 0);
        if (dest != 0) {
            navigateTo(dest);
            intent.removeExtra("navigate_to");
        }
    }

    private boolean navigateToAnimated(int destId) {
        if (navController == null)
            return false;
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destId) {
            return true;
        }
        // Drawer navigation uses scale+fade for a premium feel
        navigateFromDrawer(destId);
        return true;
    }

    private boolean navigateBottomItem(android.view.MenuItem item) {
        int id = item.getItemId();
        if (navController == null)
            return false;
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == id) {
            return true;
        }
        // Bottom nav uses slide-up animation for tab switching
        if (navController != null) {
            navController.navigate(id, null, UiAnimations.navBottomTab());
        }
        return true;
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
        if (header == null)
            return;

        // Apply dynamic system window insets (notch / status bar) as top padding
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(
                    v.getPaddingLeft(),
                    statusBarTop + (int) (16 * v.getResources().getDisplayMetrics().density),
                    v.getPaddingRight(),
                    v.getPaddingBottom());
            return insets;
        });

        android.widget.TextView tvName = header.findViewById(R.id.tvDrawerAppName);
        android.widget.TextView tvId = header.findViewById(R.id.tvDrawerTeacherId);

        if (tvName != null) {
            if (SessionContext.selectedSchool != null && SessionContext.selectedSchool.name != null
                    && !SessionContext.selectedSchool.name.isEmpty()) {
                tvName.setText(SessionContext.selectedSchool.name);
            } else {
                tvName.setText(R.string.msg_cce110);
            }
        }

        if (tvId != null) {
            if (SessionContext.selectedSchool != null && SessionContext.selectedSchool.udiseCode != null
                    && !SessionContext.selectedSchool.udiseCode.isEmpty()) {
                tvId.setText(SessionContext.selectedSchool.udiseCode);
            } else {
                tvId.setText("27251208204");
            }
        }
    }

    private void setupDrawerActions() {
        View header = b.navigationView.getHeaderView(0);
        if (header == null)
            return;

        // Force quick action content descriptions to bilingual resources
        View btnStats = header.findViewById(R.id.btnDrawerStats);
        if (btnStats != null)
            btnStats.setContentDescription(getString(R.string.drawer_stats));
        View btnSettings = header.findViewById(R.id.btnDrawerSettings);
        if (btnSettings != null)
            btnSettings.setContentDescription(getString(R.string.drawer_settings));
        View btnPrint = header.findViewById(R.id.btnDrawerPrint);
        if (btnPrint != null)
            btnPrint.setContentDescription(getString(R.string.drawer_print));
        View btnProfile = header.findViewById(R.id.btnDrawerProfile);
        if (btnProfile != null)
            btnProfile.setContentDescription(getString(R.string.profile_title));

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
            navigateToAnimated(R.id.nav_dashboard);
        });
    }

    private void localizeSidebar() {
        android.view.Menu menu = b.navigationView.getMenu();
        if (menu == null)
            return;

        // 1. Group 1: About Student
        if (menu.size() > 0) {
            android.view.MenuItem group1 = menu.getItem(0);
            if (group1 != null) {
                group1.setTitle(getString(R.string.section_about_student));
            }
        }
        android.view.MenuItem itemFormative = menu.findItem(R.id.nav_formative);
        if (itemFormative != null)
            itemFormative.setTitle(getString(R.string.menu_formative_summative));

        android.view.MenuItem itemDescriptive = menu.findItem(R.id.nav_descriptive);
        if (itemDescriptive != null)
            itemDescriptive.setTitle(getString(R.string.menu_descriptive_entries));

        android.view.MenuItem itemAttendance = menu.findItem(R.id.nav_attendance);
        if (itemAttendance != null)
            itemAttendance.setTitle(getString(R.string.menu_attendance));

        // 2. Group 2: Class Level
        if (menu.size() > 1) {
            android.view.MenuItem group2 = menu.getItem(1);
            if (group2 != null) {
                group2.setTitle(getString(R.string.section_class_level));
            }
        }
        android.view.MenuItem itemStudents = menu.findItem(R.id.nav_students);
        if (itemStudents != null)
            itemStudents.setTitle(getString(R.string.menu_student_list));

        android.view.MenuItem itemSubjects = menu.findItem(R.id.nav_subjects);
        if (itemSubjects != null)
            itemSubjects.setTitle(getString(R.string.menu_subjects));

        android.view.MenuItem itemWeightage = menu.findItem(R.id.nav_weightage);
        if (itemWeightage != null)
            itemWeightage.setTitle(getString(R.string.menu_declare_weightage));

        android.view.MenuItem itemDropdown = menu.findItem(R.id.nav_dropdown);
        if (itemDropdown != null)
            itemDropdown.setTitle(getString(R.string.menu_dropdown_list));

        // 3. Group 3: School Level
        if (menu.size() > 2) {
            android.view.MenuItem group3 = menu.getItem(2);
            if (group3 != null) {
                group3.setTitle(getString(R.string.section_school_level));
            }
        }
        android.view.MenuItem itemSchoolLevelDropdown = menu.findItem(R.id.nav_school_level_dropdown);
        if (itemSchoolLevelDropdown != null) {
            itemSchoolLevelDropdown
                    .setTitle(getString(R.string.txt_school_level_info) + (isSchoolLevelExpanded ? " ▴" : " ▾"));
        }
        android.view.MenuItem itemGender = menu.findItem(R.id.nav_gender);
        if (itemGender != null) {
            itemGender.setTitle(getString(R.string.txt_gender));
            itemGender.setVisible(isSchoolLevelExpanded);
        }
        android.view.MenuItem itemSchoolInfo = menu.findItem(R.id.nav_school_info);
        if (itemSchoolInfo != null) {
            itemSchoolInfo.setTitle(getString(R.string.txt_school_information));
            itemSchoolInfo.setVisible(isSchoolLevelExpanded);
        }
        android.view.MenuItem itemCast = menu.findItem(R.id.nav_cast_category);
        if (itemCast != null) {
            itemCast.setTitle(getString(R.string.txt_cast_category));
            itemCast.setVisible(isSchoolLevelExpanded);
        }
        android.view.MenuItem itemTeacher = menu.findItem(R.id.nav_class_teacher);
        if (itemTeacher != null) {
            itemTeacher.setTitle(getString(R.string.txt_class_teacher));
            itemTeacher.setVisible(isSchoolLevelExpanded);
        }
        android.view.MenuItem itemClasses = menu.findItem(R.id.nav_classes);
        if (itemClasses != null) {
            itemClasses.setTitle(getString(R.string.txt_classes));
            itemClasses.setVisible(isSchoolLevelExpanded);
        }
        android.view.MenuItem itemSubj = menu.findItem(R.id.nav_subject);
        if (itemSubj != null) {
            itemSubj.setTitle(getString(R.string.txt_subject));
            itemSubj.setVisible(isSchoolLevelExpanded);
        }
        android.view.MenuItem itemDefaults = menu.findItem(R.id.nav_default_values);
        if (itemDefaults != null) {
            itemDefaults.setTitle(getString(R.string.txt_default_values));
            itemDefaults.setVisible(isSchoolLevelExpanded);
        }
        android.view.MenuItem itemWorking = menu.findItem(R.id.nav_working_days);
        if (itemWorking != null) {
            itemWorking.setTitle(getString(R.string.txt_working_days));
            itemWorking.setVisible(isSchoolLevelExpanded);
        }
        android.view.MenuItem itemHeShe = menu.findItem(R.id.nav_he_she_items);
        if (itemHeShe != null) {
            itemHeShe.setTitle(getString(R.string.txt_he_she_items));
            itemHeShe.setVisible(isSchoolLevelExpanded);
        }

        // 4. Group 4: Bottom Action
        if (menu.size() > 3) {
            android.view.MenuItem group4 = menu.getItem(3);
            if (group4 != null) {
                group4.setTitle(getString(R.string.section_bottom_action));
            }
        }
        android.view.MenuItem itemPrint = menu.findItem(R.id.nav_print_report);
        if (itemPrint != null)
            itemPrint.setTitle(getString(R.string.menu_print_report));
    }

    private void toggleSchoolLevelDropdown() {
        isSchoolLevelExpanded = !isSchoolLevelExpanded;

        android.transition.TransitionManager.beginDelayedTransition(b.navigationView);

        // Clear and re-inflate menu to force NavigationView to rebuild the adapter and
        // reflect visibility changes instantly
        b.navigationView.getMenu().clear();
        b.navigationView.inflateMenu(R.menu.drawer_menu);
        localizeSidebar();

        // Restore active item selection highlight
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentId = navController.getCurrentDestination().getId();
            b.navigationView.setCheckedItem(currentId);
        }
    }

    private void loadTeacherInfo() {
        if (AppCache.cachedTeacherName != null) {
            b.tvToolbarSubtitle.setText(AppCache.cachedTeacherName);
        }
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<Teacher>() {
            @Override
            public void onSuccess(Teacher t) {
                if (t != null && t.name != null) {
                    AppCache.cachedTeacherName = t.name;
                    runOnUiThread(() -> b.tvToolbarSubtitle.setText(t.name));
                }
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    public void updateToolbar(String title, String subtitle) {
        if (title != null && !title.isEmpty())
            b.tvToolbarTitle.setText(title);
        if (subtitle != null && !subtitle.isEmpty())
            b.tvToolbarSubtitle.setText(subtitle);
    }

    public void showCustomToolbarActions(boolean show, View.OnClickListener onNotificationsClick,
            View.OnClickListener onMoreClick) {
        if (b == null)
            return;
        if (show) {
            b.btnToolbarNotifications.setVisibility(View.VISIBLE);
            b.btnToolbarMore.setVisibility(View.VISIBLE);
            b.btnToolbarNotifications.setOnClickListener(onNotificationsClick);
            b.btnToolbarMore.setOnClickListener(onMoreClick);
            b.ivProfilePic.setVisibility(View.GONE);
            b.btnToolbarHelp.setVisibility(View.GONE);
        } else {
            b.btnToolbarNotifications.setVisibility(View.GONE);
            b.btnToolbarNotifications.setImageResource(R.drawable.ic_notifications); // restore default
            b.btnToolbarMore.setOnClickListener(this::handleHomeMoreClick);
            if (navController != null && navController.getCurrentDestination() != null) {
                int destId = navController.getCurrentDestination().getId();
                if (destId == R.id.nav_info_print || destId == R.id.nav_profile || destId == R.id.nav_school_info || destId == R.id.nav_weightage || destId == R.id.nav_extra) {
                    b.btnToolbarMore.setVisibility(View.VISIBLE);
                } else {
                    b.btnToolbarMore.setVisibility(View.GONE);
                }
            } else {
                b.btnToolbarMore.setVisibility(View.GONE);
            }
            b.ivProfilePic.setVisibility(View.VISIBLE);
            b.btnToolbarHelp.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Navigate programmatically (e.g., from intent or internal button) — horizontal
     * slide.
     */
    public void navigateTo(int destId) {
        if (navController == null)
            return;
        navController.navigate(destId, null, UiAnimations.navSlideForward());
    }

    /** Navigate from drawer sidebar — scale + fade for premium feel. */
    private void navigateFromDrawer(int destId) {
        if (navController == null)
            return;
        navController.navigate(destId, null, UiAnimations.navDrawerOpen());
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
            popup.getMenu().add(0, R.id.nav_info_print, 1, "🏠 " + getString(R.string.menu_3dot_home));
            popup.getMenu().add(0, R.id.nav_class_div, 2, "🏫 " + getString(R.string.menu_3dot_classes));
            popup.getMenu().add(0, R.id.nav_students, 3, "👥 " + getString(R.string.menu_3dot_students));
            popup.getMenu().add(0, 801, 4, "💬 " + getString(R.string.menu_3dot_message));

            // Utility options
            popup.getMenu().add(0, 901, 5, "🌐 " + getString(R.string.menu_3dot_language));
            popup.getMenu().add(0, 902, 6, "⭐ " + getString(R.string.menu_3dot_rate));
            popup.getMenu().add(0, 903, 7, "📱 " + getString(R.string.menu_3dot_more_apps));
            popup.getMenu().add(0, 904, 8, "ℹ️ " + getString(R.string.menu_3dot_about));
            popup.getMenu().add(0, 999, 9, "🚪 " + getString(R.string.logout));

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 801) {
                    showAdminMessageDialog();
                    return true;
                } else if (id == 901) {
                    // Fully functional bilingual language selector dialog
                    String[] languages = { "English", "मराठी (Marathi)" };
                    android.content.SharedPreferences prefs = getSharedPreferences("myschool_settings_prefs",
                            MODE_PRIVATE);
                    String currentLang = prefs.getString("language", "mr");
                    int checkedItem = "mr".equals(currentLang) ? 1 : 0;

                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle(R.string.msg_select_language)
                            .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                                String selectedLang = (which == 1) ? "mr" : "en";
                                changeLanguage(selectedLang);
                                dialog.dismiss();
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return true;
                } else if (id == 902) {
                    Toast.makeText(this, R.string.msg_thank_you_for_rating_us_5_star, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == 903) {
                    Toast.makeText(this, R.string.msg_opening_more_apps_on_play_stor, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == 904) {
                    showAboutDeveloperDialog();
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

    private void showAboutDeveloperDialog() {
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage(getString(R.string.msg_checking_messages));
        pd.setCancelable(false);
        pd.show();

        FirebaseRepository.get().getAppConfig(new FirebaseRepository.OnResult<com.kartik.myschool.model.AppConfig>() {
            @Override
            public void onSuccess(com.kartik.myschool.model.AppConfig config) {
                pd.dismiss();

                StringBuilder sb = new StringBuilder();
                sb.append("<h3><b>🧑‍💻 " + config.devName + "</b></h3>");
                sb.append("<p><b>🤖 Version:</b> " + config.appVersion + "</p>");
                if (config.devEmail != null && !config.devEmail.trim().isEmpty()) {
                    sb.append("<p><b>📧 Email:</b> <a href=\"mailto:" + config.devEmail.trim() + "\">"
                            + config.devEmail.trim() + "</a></p>");
                }
                if (config.devWebsite != null && !config.devWebsite.trim().isEmpty()) {
                    sb.append("<p><b>🌐 Website:</b> <a href=\"" + config.devWebsite.trim() + "\">"
                            + config.devWebsite.trim() + "</a></p>");
                }
                if (config.devPhone != null && !config.devPhone.trim().isEmpty()) {
                    sb.append("<p><b>📞 Contact:</b> " + config.devPhone.trim() + "</p>");
                }
                sb.append("<br/>");
                sb.append("<p><i>" + config.aboutText + "</i></p>");

                android.widget.TextView tv = new android.widget.TextView(HomeActivity.this);
                int padding = (int) (18 * getResources().getDisplayMetrics().density);
                tv.setPadding(padding, padding, padding, padding);
                tv.setText(android.text.Html.fromHtml(sb.toString()));
                tv.setTextSize(16);
                tv.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());

                new androidx.appcompat.app.AlertDialog.Builder(HomeActivity.this)
                        .setTitle(R.string.msg_about_developer)
                        .setView(tv)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            @Override
            public void onError(Exception e) {
                pd.dismiss();
                Toast.makeText(HomeActivity.this, "Error fetching developer details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAdminMessageDialog() {
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage(getString(R.string.msg_checking_messages));
        pd.setCancelable(false);
        pd.show();

        FirebaseRepository.get().getTeacherFresh(new FirebaseRepository.OnResult<Teacher>() {
            @Override
            public void onSuccess(Teacher teacher) {
                pd.dismiss();
                String message = (teacher != null && teacher.adminNote != null && !teacher.adminNote.trim().isEmpty())
                        ? teacher.adminNote.trim()
                        : getString(R.string.msg_no_admin_message);

                new androidx.appcompat.app.AlertDialog.Builder(HomeActivity.this)
                        .setTitle("✉️ " + getString(R.string.menu_3dot_message))
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            @Override
            public void onError(Exception e) {
                pd.dismiss();
                Toast.makeText(HomeActivity.this, "Failed to load message: " + e.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
        });
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
        String currentLang = prefs.getString("language", "mr");
        if (currentLang.equals(lang))
            return;

        prefs.edit().putString("language", lang).apply();

        // Apply locale runtime change to application and context resources
        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);
        android.content.res.Resources res = getResources();
        android.content.res.Configuration config = new android.content.res.Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        Toast.makeText(this, R.string.msg_language_updated, Toast.LENGTH_SHORT).show();

        // Recreate activity to force reinflating components with new resource locale
        // bundle
        recreate();
    }
}
