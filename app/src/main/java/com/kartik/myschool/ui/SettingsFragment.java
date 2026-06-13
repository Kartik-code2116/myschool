package com.kartik.myschool.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kartik.myschool.AppCache;
import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.MySchoolApplication;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.databinding.FragmentSettingsBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding b;
    private SharedPreferences settingsPrefs;

    private final androidx.activity.result.ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    com.kartik.myschool.utils.BackupRestoreHelper.importFullAccount(requireContext(), uri, () -> {
                        if (getActivity() instanceof HomeActivity) {
                            ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
                        }
                    });
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentSettingsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsPrefs = requireContext().getSharedPreferences("myschool_settings_prefs", android.content.Context.MODE_PRIVATE);

        // Fetch Subscription info
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
            @Override
            public void onSuccess(com.kartik.myschool.model.Teacher teacher) {
                if (teacher != null && b != null) {
                    b.tvSubscriptionStatus.setText("Status: " + teacher.subscriptionStatus.toUpperCase());
                    if (teacher.subscriptionExpiry > 0) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());
                        b.tvSubscriptionExpiry.setText("Valid till: " + sdf.format(new java.util.Date(teacher.subscriptionExpiry)));
                    } else {
                        b.tvSubscriptionExpiry.setText("Valid till: N/A");
                    }
                }
            }
            @Override
            public void onError(Exception e) {
                if (b != null) b.tvSubscriptionStatus.setText("Status: Error fetching");
            }
        });

        // 1. Initial State UI Restoration
        int themeMode = settingsPrefs.getInt("theme_mode", 0);
        String lang = settingsPrefs.getString("language", "mr");
        updateThemeUi(themeMode);
        updateLanguageUi(lang);

        // 2. Click Listeners
        if (b.btnViewHistory != null) {
            b.btnViewHistory.setOnClickListener(v -> {
                UiAnimations.pulse(b.btnViewHistory);
                startActivity(new Intent(requireContext(), SubscriptionHistoryActivity.class));
            });
        }

        b.btnThemeSystem.setOnClickListener(v -> { UiAnimations.pulse(b.btnThemeSystem); selectTheme(0); });
        b.btnThemeLight.setOnClickListener(v -> { UiAnimations.pulse(b.btnThemeLight); selectTheme(1); });
        b.btnThemeDark.setOnClickListener(v -> { UiAnimations.pulse(b.btnThemeDark); selectTheme(2); });

        b.btnLangEn.setOnClickListener(v -> { UiAnimations.pulse(b.btnLangEn); selectLanguage("en"); });
        b.btnLangMr.setOnClickListener(v -> { UiAnimations.pulse(b.btnLangMr); selectLanguage("mr"); });

        b.btnExportBackup.setOnClickListener(v -> { UiAnimations.pulse(b.btnExportBackup); com.kartik.myschool.utils.BackupRestoreHelper.exportFullAccount(requireContext()); });
        b.btnImportBackup.setOnClickListener(v -> { UiAnimations.pulse(b.btnImportBackup); filePickerLauncher.launch("application/json"); });

        b.btnResetSession.setOnClickListener(v -> { UiAnimations.pulse(b.btnResetSession); resetSession(); });
        b.btnClearCache.setOnClickListener(v -> { UiAnimations.pulse(b.btnClearCache); clearCache(); });

        UiAnimations.staggerFadeIn(b.cardTheme, b.cardLanguage, b.cardBackup, b.cardCache);
    }

    // ── Theme UI ─────────────────────────────────────────────────────────────
    private void updateThemeUi(int mode) {
        int activeColor = getResources().getColor(R.color.primary, null);
        int inactiveColor = getResources().getColor(R.color.outline_variant, null);

        b.btnThemeSystem.setStrokeColor(mode == 0 ? activeColor : inactiveColor);
        b.btnThemeSystem.setStrokeWidth(mode == 0 ? dpToPx(2) : dpToPx(1));

        b.btnThemeLight.setStrokeColor(mode == 1 ? activeColor : inactiveColor);
        b.btnThemeLight.setStrokeWidth(mode == 1 ? dpToPx(2) : dpToPx(1));

        b.btnThemeDark.setStrokeColor(mode == 2 ? activeColor : inactiveColor);
        b.btnThemeDark.setStrokeWidth(mode == 2 ? dpToPx(2) : dpToPx(1));
    }

    private void selectTheme(int mode) {
        settingsPrefs.edit().putInt("theme_mode", mode).apply();
        updateThemeUi(mode);
        MySchoolApplication.applyTheme(mode);
        Toast.makeText(requireContext(), R.string.msg_theme_updated_successfully, Toast.LENGTH_SHORT).show();
    }

    // ── Language UI ──────────────────────────────────────────────────────────
    private void updateLanguageUi(String lang) {
        int activeColor = getResources().getColor(R.color.primary, null);
        int inactiveColor = getResources().getColor(R.color.outline_variant, null);

        b.btnLangEn.setStrokeColor("en".equals(lang) ? activeColor : inactiveColor);
        b.btnLangEn.setStrokeWidth("en".equals(lang) ? dpToPx(2) : dpToPx(1));

        b.btnLangMr.setStrokeColor("mr".equals(lang) ? activeColor : inactiveColor);
        b.btnLangMr.setStrokeWidth("mr".equals(lang) ? dpToPx(2) : dpToPx(1));
    }

    private void selectLanguage(String lang) {
        String currentLang = settingsPrefs.getString("language", "mr");
        if (currentLang.equals(lang)) return;

        settingsPrefs.edit().putString("language", lang).apply();
        com.kartik.myschool.utils.pdf.PdfLocalizer.clearCache();
        updateLanguageUi(lang);

        // Apply locale runtime change using AppCompatDelegate
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(lang)
        );

        Toast.makeText(requireContext(), R.string.msg_language_updated, Toast.LENGTH_SHORT).show();

        // Recreate activity to force reinflating components with new resource locale bundle
        requireActivity().recreate();
    }

    // ── Full Account Database Export & Restore ──────────────────────────────────────────────
    // Handled by com.kartik.myschool.utils.BackupRestoreHelper


    // ── Reset & Cache utilities ──────────────────────────────────────────────
    private void resetSession() {
        SessionContext.clear(requireContext());
        Toast.makeText(requireContext(), R.string.msg_active_session_reset_successfu, Toast.LENGTH_LONG).show();
    }

    private void clearCache() {
        SessionContext.clear(requireContext());
        Toast.makeText(requireContext(), R.string.msg_cache_cleared_successfully, Toast.LENGTH_SHORT).show();
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
