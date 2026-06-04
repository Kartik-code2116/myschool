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
                    importStudents(uri);
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
        String lang = settingsPrefs.getString("language", "en");
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

        b.btnExportBackup.setOnClickListener(v -> { UiAnimations.pulse(b.btnExportBackup); exportStudents(); });
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
        String currentLang = settingsPrefs.getString("language", "en");
        if (currentLang.equals(lang)) return;

        settingsPrefs.edit().putString("language", lang).apply();
        updateLanguageUi(lang);

        // Apply locale runtime change
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        requireContext().getResources().updateConfiguration(config, requireContext().getResources().getDisplayMetrics());

        Toast.makeText(requireContext(), R.string.msg_language_updated, Toast.LENGTH_SHORT).show();

        // Recreate activity to force reinflating components with new resource locale bundle
        requireActivity().recreate();
    }

    // ── Student Database Export ──────────────────────────────────────────────
    private void exportStudents() {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(requireContext(), R.string.select_class_first, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseRepository.get().getStudentsForClass(SessionContext.selectedClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                if (list == null || list.isEmpty()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), R.string.msg_no_students_to_backup_in_the_a, Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                try {
                    JSONArray array = new JSONArray();
                    for (Student s : list) {
                        JSONObject obj = new JSONObject();
                        obj.put("name", s.name);
                        obj.put("rollNo", s.rollNo);
                        obj.put("rollNo2", s.rollNo2);
                        obj.put("registrationNo", s.registrationNo);
                        obj.put("dob", s.dob);
                        obj.put("gender", s.gender);
                        obj.put("cast", s.cast);
                        obj.put("parentName", s.parentName);
                        obj.put("motherName", s.motherName);
                        obj.put("motherOccupation", s.motherOccupation);
                        obj.put("motherPhone", s.motherPhone);
                        obj.put("fatherName", s.fatherName);
                        obj.put("fatherOccupation", s.fatherOccupation);
                        obj.put("fatherPhone", s.fatherPhone);
                        obj.put("address", s.address);
                        obj.put("bankAccount", s.bankAccount);
                        obj.put("bankBranch", s.bankBranch);
                        obj.put("bankIfsc", s.bankIfsc);
                        obj.put("bankUid", s.bankUid);
                        obj.put("medium", s.medium);
                        obj.put("motherTongue", s.motherTongue);
                        obj.put("dateOfAdmission", s.dateOfAdmission);
                        obj.put("studentIdNumber", s.studentIdNumber);
                        obj.put("uid", s.uid);
                        array.put(obj);
                    }

                    String jsonString = array.toString(4);

                    // Write to local cache path
                    File cachePath = new File(requireContext().getCacheDir(), "backups");
                    cachePath.mkdirs();
                    File file = new File(cachePath, "students_backup_" + SessionContext.selectedClass.className + "_" + SessionContext.selectedClass.division + ".json");
                    FileWriter writer = new FileWriter(file);
                    writer.write(jsonString);
                    writer.close();

                    // Generate sharable Uri via FileProvider
                    android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName() + ".fileprovider", file);

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    requireActivity().runOnUiThread(() ->
                            startActivity(Intent.createChooser(intent, "Share Student Backup"))
                    );

                } catch (Exception e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }

            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error fetching students: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // ── Student Database Restore ─────────────────────────────────────────────
    private void importStudents(android.net.Uri uri) {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(requireContext(), R.string.select_class_first, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();

            JSONArray array = new JSONArray(sb.toString());
            int count = array.length();
            if (count == 0) {
                Toast.makeText(requireContext(), R.string.msg_backup_file_is_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            android.app.ProgressDialog pd = new android.app.ProgressDialog(requireContext());
            pd.setTitle(R.string.msg_restoring_students);
            pd.setMessage("Saving student records to Firestore...");
            pd.setCancelable(false);
            pd.show();

            ClassModel currentClass = SessionContext.selectedClass;
            School currentSchool = SessionContext.selectedSchool;
            String teacherUid = FirebaseRepository.get().currentUid();

            AtomicInteger savedCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);

            for (int i = 0; i < count; i++) {
                JSONObject obj = array.getJSONObject(i);
                Student s = new Student();
                s.name = obj.optString("name", "");
                s.rollNo = obj.optString("rollNo", "");
                s.rollNo2 = obj.optString("rollNo2", "");
                s.registrationNo = obj.optString("registrationNo", "");
                s.dob = obj.optString("dob", "");
                s.gender = obj.optString("gender", "");
                s.cast = obj.optString("cast", "");
                s.parentName = obj.optString("parentName", "");
                s.motherName = obj.optString("motherName", "");
                s.motherOccupation = obj.optString("motherOccupation", "");
                s.motherPhone = obj.optString("motherPhone", "");
                s.fatherName = obj.optString("fatherName", "");
                s.fatherOccupation = obj.optString("fatherOccupation", "");
                s.fatherPhone = obj.optString("fatherPhone", "");
                s.address = obj.optString("address", "");
                s.bankAccount = obj.optString("bankAccount", "");
                s.bankBranch = obj.optString("bankBranch", "");
                s.bankIfsc = obj.optString("bankIfsc", "");
                s.bankUid = obj.optString("bankUid", "");
                s.medium = obj.optString("medium", "");
                s.motherTongue = obj.optString("motherTongue", "");
                s.dateOfAdmission = obj.optString("dateOfAdmission", "");
                s.studentIdNumber = obj.optString("studentIdNumber", "");
                s.uid = obj.optString("uid", "");

                // Map student strictly to currently active selection parameters
                s.classId = currentClass.id;
                s.className = currentClass.className;
                s.standard = currentClass.className;
                s.division = currentClass.division;
                if (currentSchool != null) {
                    s.schoolId = currentSchool.id;
                    s.schoolName = currentSchool.name;
                }
                s.teacherId = teacherUid;

                FirebaseRepository.get().saveStudent(s, new FirebaseRepository.OnResult<String>() {
                    @Override
                    public void onSuccess(String id) {
                        int sVal = savedCount.incrementAndGet();
                        checkDone(sVal, failedCount.get(), count, pd);
                    }

                    @Override
                    public void onError(Exception e) {
                        int fVal = failedCount.incrementAndGet();
                        checkDone(savedCount.get(), fVal, count, pd);
                    }
                });
            }

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to read backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkDone(int saved, int failed, int total, android.app.ProgressDialog pd) {
        if (saved + failed == total) {
            requireActivity().runOnUiThread(() -> {
                if (pd.isShowing()) pd.dismiss();
                Toast.makeText(requireContext(),
                        "Import Complete! Restored: " + saved + ", Failed: " + failed,
                        Toast.LENGTH_LONG).show();

                // Direct routing to students view to observe imported records
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).navigateTo(R.id.nav_students);
                }
            });
        }
    }

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
