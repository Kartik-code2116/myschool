package com.kartik.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.databinding.FragmentExtraMenusBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.AppCache;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Semester;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class ExtraMenusFragment extends Fragment {

    private FragmentExtraMenusBinding b;
    private String menuType = "school_info";
    private final List<String> remarkBankSubjects = new ArrayList<>();
    private final List<String> remarkBankOptions = new ArrayList<>();
    private String selectedRemarkBankSubject = "General";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        b = FragmentExtraMenusBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            menuType = getArguments().getString("menu_type", "school_info");
        }

        displayHeaderInfo();
        toggleDataModule();
        loadDynamicData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if ("school_info".equals(menuType)) {
            // Reload school details in case they were updated
            School school = SessionContext.selectedSchool;
            if (school != null && b != null) {
                if (school.name != null)
                    b.tvSchoolInfoName.setText(getString(R.string.fmt_school_name, school.name));
                if (school.udiseCode != null)
                    b.tvSchoolInfoUdise.setText(getString(R.string.fmt_school_udise, school.udiseCode));
                if (school.board != null)
                    b.tvSchoolInfoBoard.setText(getString(R.string.fmt_school_board, school.board));
                if (school.address != null && !school.address.isEmpty())
                    b.tvSchoolInfoAddress.setText(getString(R.string.fmt_school_address, school.address));
            }
        }
        if ("subject".equals(menuType)) {
            if (getActivity() instanceof HomeActivity) {
                boolean isMr = java.util.Locale.getDefault().getLanguage().equals("mr");
                String title = isMr ? "विषय" : "Subject";
                String subtitle = isMr ? "शाळास्तर सर्व विषयांची यादी" : "School level list of all subjects";
                ((HomeActivity) getActivity()).updateToolbar(title, subtitle);
            }
            ClassModel activeClass = SessionContext.selectedClass;
            if (activeClass != null && schoolSubjectAdapter != null) {
                loadSchoolSubjectsList(activeClass);
            }
        }
        if ("remarks".equals(menuType)) {
            if (getActivity() instanceof HomeActivity) {
                boolean isMr = java.util.Locale.getDefault().getLanguage().equals("mr");
                String title = isMr ? "वर्णनात्मक नोंदी" : "Descriptive Remarks";
                String subtitle = isMr ? "वर्णनात्मक नोंदी बदला" : "Change Descriptive Entries";
                ((HomeActivity) getActivity()).updateToolbar(title, subtitle);
            }
            ClassModel activeClass = SessionContext.selectedClass;
            if (activeClass != null && remarksAdapter != null) {
                loadRemarksSubjectsList(activeClass);
            }
        }
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        String classDiv = SessionContext.getClassDivLabel();
        b.tvExtraContextSubtitle.setText("Year: " + yearLabel + "   " + classDiv);

        String contextTitle = "Student Administration";
        switch (menuType) {
            case "school_info":
                contextTitle = "School Details Module";
                break;
            case "gender":
                contextTitle = "Gender Distribution Module";
                break;
            case "cast_category":
                contextTitle = "Cast Category Module";
                break;
            case "class_teacher":
                contextTitle = "Class Teacher Assignment";
                break;
            case "classes":
                contextTitle = "School Classes Listing";
                break;
            case "subject":
                contextTitle = "Subject Criteria Guidelines";
                break;
            case "default_values":
                contextTitle = "Baseline Settings Module";
                break;
            case "working_days":
                contextTitle = "Holidays & Working Log";
                break;
            case "he_she_items":
                contextTitle = "Pronouns Comment Helper";
                break;
            case "remarks":
                contextTitle = "Descriptive Remarks Options";
                break;
        }
        b.tvExtraContextTitle.setText(contextTitle);
    }

    private void toggleDataModule() {
        b.cvContextHeader.setVisibility(View.VISIBLE);
        b.cvUnifiedContent.setVisibility(View.VISIBLE);
        b.llSubjectSelectionStyle.setVisibility(View.GONE);
        b.llRemarksSelectionStyle.setVisibility(View.GONE);

        b.llSchoolInfo.setVisibility(View.GONE);
        b.llGender.setVisibility(View.GONE);
        b.llCastCategory.setVisibility(View.GONE);
        b.llClassTeacher.setVisibility(View.GONE);
        b.llClasses.setVisibility(View.GONE);
        b.llSubject.setVisibility(View.GONE);
        b.llDefaultValues.setVisibility(View.GONE);
        b.llWorkingDays.setVisibility(View.GONE);
        b.llHeSheItems.setVisibility(View.GONE);

        switch (menuType) {
            case "school_info":
                b.llSchoolInfo.setVisibility(View.VISIBLE);
                break;
            case "gender":
                b.llGender.setVisibility(View.VISIBLE);
                break;
            case "cast_category":
                b.llCastCategory.setVisibility(View.VISIBLE);
                break;
            case "class_teacher":
                b.llClassTeacher.setVisibility(View.VISIBLE);
                break;
            case "classes":
                b.llClasses.setVisibility(View.VISIBLE);
                break;
            case "subject":
                b.cvContextHeader.setVisibility(View.GONE);
                b.cvUnifiedContent.setVisibility(View.GONE);
                b.llSubjectSelectionStyle.setVisibility(View.VISIBLE);
                break;
            case "default_values":
                b.llDefaultValues.setVisibility(View.VISIBLE);
                break;
            case "working_days":
                b.llWorkingDays.setVisibility(View.VISIBLE);
                break;
            case "he_she_items":
                b.llHeSheItems.setVisibility(View.VISIBLE);
                break;
            case "remarks":
                b.cvContextHeader.setVisibility(View.GONE);
                b.cvUnifiedContent.setVisibility(View.GONE);
                b.llRemarksSelectionStyle.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void loadDynamicData() {
        // Load School details
        School school = SessionContext.selectedSchool;
        if (school != null) {
            if (school.name != null)
                b.tvSchoolInfoName.setText(getString(R.string.fmt_school_name, school.name));
            if (school.udiseCode != null)
                b.tvSchoolInfoUdise.setText(getString(R.string.fmt_school_udise, school.udiseCode));
            if (school.board != null)
                b.tvSchoolInfoBoard.setText(getString(R.string.fmt_school_board, school.board));
            if (school.address != null && !school.address.isEmpty())
                b.tvSchoolInfoAddress.setText(getString(R.string.fmt_school_address, school.address));
        }

        b.btnEditSchoolInfo.setOnClickListener(v -> {
            if (school != null) {
                com.kartik.myschool.AppCache.selectedSchool = school;
                android.content.Intent intent = new android.content.Intent(getContext(),
                        com.kartik.myschool.SchoolRegisterActivity.class);
                intent.putExtra("school_id", school.id);
                startActivity(intent);
            }
        });

        // Load Class details
        ClassModel activeClass = SessionContext.selectedClass;
        if (activeClass != null) {
            if (menuType.equals("class_teacher")) {
                setupClassTeacherEditor(activeClass);
            }
            if (menuType.equals("subject")) {
                setupRemarkBankEditor(activeClass);
            }
            if (menuType.equals("working_days")) {
                setupWorkingDaysEditor(activeClass);
            }
            if (menuType.equals("remarks")) {
                setupRemarksEditor(activeClass);
            }
        }

        if (menuType.equals("school_info") || menuType.equals("gender") || menuType.equals("cast_category")) {
            loadSchoolWideStats();
        }

        // Save Default admission baseline click listener
        b.btnSaveDefaults.setOnClickListener(v -> Toast
                .makeText(getContext(), R.string.msg_admission_defaults_baseline_se, Toast.LENGTH_SHORT).show());

        if (menuType.equals("classes")) {
            setupClassesListing();
        }
    }

    private SchoolSubjectAdapter schoolSubjectAdapter;
    private RemarksAdapter remarksAdapter;

    private static class SchoolSubjectItem {
        String name;
        String code;
        String serial;
        String category;
        String colorHex;
        boolean isHeader;
        String type;
        String subjectCode;
        String headerText;
        boolean isExpanded = false; // New field
        String detailsLeft1;
        String detailsLeft2;
        String detailsRight1;

        SchoolSubjectItem(String headerText) {
            this.isHeader = true;
            this.headerText = headerText;
        }

        SchoolSubjectItem(String name, String code, String serial, String category, String colorHex,
                String detailsLeft1, String detailsLeft2, String detailsRight1) {
            this.name = name;
            this.code = code;
            this.serial = serial;
            this.category = category;
            this.colorHex = colorHex;
            this.detailsLeft1 = detailsLeft1;
            this.detailsLeft2 = detailsLeft2;
            this.detailsRight1 = detailsRight1;
            this.isHeader = false;
        }
    }

    public static String getSubjectDisplayName(android.content.Context ctx, String originalName) {
        boolean isMr = java.util.Locale.getDefault().getLanguage().equals("mr");
        if (originalName.equalsIgnoreCase("Vishesh pragati") || originalName.equalsIgnoreCase("Special Development")) {
            return isMr ? "विशेष विकास" : "Special Development";
        }
        if (originalName.equalsIgnoreCase("Aavad, chanda, etc") || originalName.equalsIgnoreCase("Interest, Hobby")) {
            return isMr ? "आवड, छंद, इत्यादी" : "Interest, Hobby";
        }
        if (originalName.equalsIgnoreCase("Sudharna Aavashyaka")
                || originalName.equalsIgnoreCase("Necessary Improvement")) {
            return isMr ? "सुधारणा आवश्यक" : "Necessary Improvement";
        }
        if (originalName.equalsIgnoreCase("Vyaktimatva gun vishgesh")
                || originalName.equalsIgnoreCase("Personality / Features")) {
            return isMr ? "व्यक्तिमत्त्व गुण विशेष" : "Personality / Features";
        }
        return com.kartik.myschool.utils.pdf.PdfLocalizer.translateSubject(ctx, originalName);
    }

    private String getSubjectDetails1(String originalName) {
        if (originalName.equalsIgnoreCase("Vishesh pragati") || originalName.equalsIgnoreCase("Special Development")) {
            return "Special Development";
        }
        if (originalName.equalsIgnoreCase("Aavad, chanda, etc") || originalName.equalsIgnoreCase("Interest, Hobby")) {
            return "Hobby";
        }
        if (originalName.equalsIgnoreCase("Sudharna Aavashyaka")
                || originalName.equalsIgnoreCase("Necessary Improvement")) {
            return "Necessary Improvement";
        }
        if (originalName.equalsIgnoreCase("Vyaktimatva gun vishgesh")
                || originalName.equalsIgnoreCase("Personality / Features")) {
            return "Personality";
        }
        if (originalName.equalsIgnoreCase("Physical Education"))
            return "Physical Edu.";
        if (originalName.equalsIgnoreCase("Work Experience"))
            return "Work Experience";
        return originalName;
    }

    private String getSubjectDetails2(String originalName) {
        if (originalName.equalsIgnoreCase("Vishesh pragati") || originalName.equalsIgnoreCase("Special Development")) {
            return "Special Development";
        }
        if (originalName.equalsIgnoreCase("Aavad, chanda, etc") || originalName.equalsIgnoreCase("Interest, Hobby")) {
            return "Interest, Hobby";
        }
        if (originalName.equalsIgnoreCase("Sudharna Aavashyaka")
                || originalName.equalsIgnoreCase("Necessary Improvement")) {
            return "Necessary Improvement";
        }
        if (originalName.equalsIgnoreCase("Vyaktimatva gun vishgesh")
                || originalName.equalsIgnoreCase("Personality / Features")) {
            return "Personality / Features";
        }
        return originalName;
    }

    private void setupRemarkBankEditor(ClassModel activeClass) {
        if (!isAdded() || activeClass == null)
            return;

        b.rvSchoolSubjectsListFull.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        schoolSubjectAdapter = new SchoolSubjectAdapter();
        b.rvSchoolSubjectsListFull.setAdapter(schoolSubjectAdapter);

        // Bind the header info
        String yearLabel = SessionContext.getYearLabel();
        String classDiv = SessionContext.getClassDivLabel();
        b.tvSubjectHeaderLabel.setText("Year: " + yearLabel + "   " + classDiv);

        loadSchoolSubjectsList(activeClass);
    }

    private void loadSchoolSubjectsList(ClassModel activeClass) {
        List<SchoolSubjectItem> list = new ArrayList<>();

        int serialCounter = 1;
        if (activeClass.subjects != null) {
            List<Subject> cleanSubjects = new ArrayList<>();
            for (Subject s : activeClass.subjects) {
                if (Subject.isNonAcademic(s.name)) {
                    int se = s.maxTondi + s.maxPratyakshikB + s.maxLekhi;
                    if (se > 0) {
                        Subject fixed = new Subject(s.name, s.maxMarks);
                        fixed.subjectCode = s.subjectCode;
                        s = fixed;
                    }
                }
                boolean duplicate = false;
                for (Subject clean : cleanSubjects) {
                    if (Subject.isSameSubject(clean.name, s.name)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    cleanSubjects.add(s);
                }
            }

            for (Subject s : cleanSubjects) {
                if (s != null && s.name != null && !s.name.trim().isEmpty()) {
                    String category = "Academic";
                    String colorHex = "#2196F3";

                    String lower = s.name.toLowerCase();
                    if (lower.contains("drawing") || lower.contains("work experience")
                            || lower.contains("physical education")) {
                        category = "Activities";
                        colorHex = "#4CAF50";
                    } else if (lower.contains("special development") || lower.contains("personality")) {
                        category = "Personality";
                        colorHex = "#009688";
                    } else if (lower.contains("information & comm") || lower.contains("water security")
                            || lower.contains("environment")) {
                        category = "State Board";
                        colorHex = "#FF9800";
                    }

                    int fe = s.maxNirikhshan + s.maxTondiKam + s.maxPratyakshik + s.maxUpkram + s.maxPrakalp
                            + s.maxChachani + s.maxSwadhyay + s.maxItar;
                    int se = s.maxTondi + s.maxPratyakshikB + s.maxLekhi;
                    String det1 = "FE: " + fe;
                    String det2 = se > 0 ? "SE: " + se : "";

                    String serialStr = String.valueOf(serialCounter++);
                    list.add(new SchoolSubjectItem(s.name, s.subjectCode != null ? s.subjectCode : "", serialStr,
                            category, colorHex, det1, det2, ""));
                }
            }
        }

        boolean isMr = java.util.Locale.getDefault().getLanguage().equals("mr");
        String headerText = isMr ? "001 : फक्त वर्णनात्मक नोंदीचे विषय" : "001 : Only Descriptive Entries Subjects";
        list.add(new SchoolSubjectItem(headerText));

        String tealColor = "#009688";
        list.add(new SchoolSubjectItem("Vishesh pragati", "", String.valueOf(serialCounter++), "Personality", tealColor,
                "", "", ""));
        list.add(new SchoolSubjectItem("Aavad, chanda, etc", "", String.valueOf(serialCounter++), "Personality",
                tealColor, "", "", ""));
        list.add(new SchoolSubjectItem("Sudharna Aavashyaka", "", String.valueOf(serialCounter++), "Personality",
                tealColor, "", "", ""));
        list.add(new SchoolSubjectItem("Vyaktimatva gun vishgesh", "", String.valueOf(serialCounter++), "Personality",
                tealColor, "", "", ""));

        schoolSubjectAdapter.setData(list);
    }

    private void setupRemarksEditor(ClassModel activeClass) {
        if (!isAdded() || activeClass == null)
            return;

        b.rvRemarksListFull.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        remarksAdapter = new RemarksAdapter();
        b.rvRemarksListFull.setAdapter(remarksAdapter);

        // Bind the header info
        String yearLabel = SessionContext.getYearLabel();
        String classDiv = SessionContext.getClassDivLabel();
        b.tvRemarksHeaderLabel.setText("Year: " + yearLabel + "   " + classDiv);

        loadRemarksSubjectsList(activeClass);
    }

    private void loadRemarksSubjectsList(ClassModel activeClass) {
        List<SchoolSubjectItem> list = new ArrayList<>();
        int serialCounter = 1;
        if (activeClass.subjects != null) {
            List<Subject> cleanSubjects = new ArrayList<>();
            for (Subject s : activeClass.subjects) {
                if (Subject.isNonAcademic(s.name)) {
                    int se = s.maxTondi + s.maxPratyakshikB + s.maxLekhi;
                    if (se > 0) {
                        Subject fixed = new Subject(s.name, s.maxMarks);
                        fixed.subjectCode = s.subjectCode;
                        s = fixed;
                    }
                }
                boolean duplicate = false;
                for (Subject clean : cleanSubjects) {
                    if (Subject.isSameSubject(clean.name, s.name)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    cleanSubjects.add(s);
                }
            }

            for (Subject s : cleanSubjects) {
                if (s != null && s.name != null && !s.name.trim().isEmpty()) {
                    String category = "Academic";
                    String colorHex = "#2196F3";
                    String lower = s.name.toLowerCase();
                    if (lower.contains("drawing") || lower.contains("work experience")
                            || lower.contains("physical education")) {
                        category = "Activities";
                        colorHex = "#4CAF50";
                    } else if (lower.contains("special development") || lower.contains("personality")) {
                        category = "Personality";
                        colorHex = "#009688";
                    } else if (lower.contains("information & comm") || lower.contains("water security")
                            || lower.contains("environment")) {
                        category = "State Board";
                        colorHex = "#FF9800";
                    }

                    String serialStr = String.valueOf(serialCounter++);
                    list.add(new SchoolSubjectItem(s.name, s.subjectCode != null ? s.subjectCode : "", serialStr,
                            category, colorHex, "", "", ""));
                }
            }
        }

        boolean isMr = java.util.Locale.getDefault().getLanguage().equals("mr");
        String headerText = isMr ? "001 : फक्त वर्णनात्मक नोंदीचे विषय" : "001 : Only Descriptive Entries Subjects";
        list.add(new SchoolSubjectItem(headerText));

        String tealColor = "#009688";
        list.add(new SchoolSubjectItem("Vishesh pragati", "", String.valueOf(serialCounter++), "Personality", tealColor,
                "", "", ""));
        list.add(new SchoolSubjectItem("Aavad, chanda, etc", "", String.valueOf(serialCounter++), "Personality",
                tealColor, "", "", ""));
        list.add(new SchoolSubjectItem("Sudharna Aavashyaka", "", String.valueOf(serialCounter++), "Personality",
                tealColor, "", "", ""));
        list.add(new SchoolSubjectItem("Vyaktimatva gun vishgesh", "", String.valueOf(serialCounter++), "Personality",
                tealColor, "", "", ""));

        remarksAdapter.setData(list);
    }

    private void showRemarksEditDialog(String subjectName) {
        if (!isAdded() || getContext() == null)
            return;

        selectedRemarkBankSubject = subjectName;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_school_subject_remarks, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvDialogRemarksTitle);
        com.google.android.material.chip.ChipGroup cgRemarks = dialogView.findViewById(R.id.cgDialogRemarkOptions);
        com.google.android.material.button.MaterialButton btnAdd = dialogView.findViewById(R.id.btnDialogAddRemark);
        com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnDialogClose);

        boolean isMr = java.util.Locale.getDefault().getLanguage().equals("mr");
        String localizedName = getSubjectDisplayName(getContext(), subjectName);
        tvTitle.setText(isMr ? "विषय : " + localizedName : "Remarks for " + localizedName);

        loadRemarksForDialog(subjectName, cgRemarks);

        btnAdd.setOnClickListener(v -> {
            android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            int padding = (int) (20 * getResources().getDisplayMetrics().density);
            layout.setPadding(padding, padding, padding, padding);

            android.widget.TextView tvLabel = new android.widget.TextView(getContext());
            tvLabel.setText("Select Category:");
            tvLabel.setPadding(0, 0, 0, 8);
            layout.addView(tvLabel);

            android.widget.Spinner spinner = new android.widget.Spinner(getContext());
            String[] categories = new String[] { "उत्कृष्ट", "चांगली प्रगती", "समाधानकारक", "Other" };
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_dropdown_item, categories);
            spinner.setAdapter(adapter);
            layout.addView(spinner);

            android.widget.Space space = new android.widget.Space(getContext());
            space.setLayoutParams(new android.widget.LinearLayout.LayoutParams(1,
                    (int) (16 * getResources().getDisplayMetrics().density)));
            layout.addView(space);

            EditText input = new EditText(getContext());
            input.setHint("Enter remark text");
            input.setSingleLine(false);
            input.setMinLines(2);
            input.setGravity(android.view.Gravity.TOP);
            layout.addView(input);

            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Add remark option")
                    .setView(layout)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Add", (d, which) -> {
                        String text = input.getText() != null ? input.getText().toString().trim() : "";
                        if (text.isEmpty()) {
                            Toast.makeText(getContext(), "Enter a remark option.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String selectedCat = (String) spinner.getSelectedItem();
                        String finalRemark = selectedCat.equals("Other") ? text : "[" + selectedCat + "] " + text;
                        if (!remarkBankOptions.contains(finalRemark)) {
                            remarkBankOptions.add(finalRemark);
                            saveRemarksFromDialog(subjectName, cgRemarks);
                        }
                    })
                    .show();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadRemarksForDialog(String subjectName, com.google.android.material.chip.ChipGroup cgRemarks) {
        String schoolId = getActiveSchoolId();
        ClassModel activeClass = SessionContext.selectedClass;
        String className = activeClass != null && activeClass.className != null ? activeClass.className : "5";
        int semesterNumber = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.number : 1;

        String cacheKey = className + "_sem_" + semesterNumber + "_" + subjectName;
        List<String> cached = AppCache.cachedRemarkBank.get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            remarkBankOptions.clear();
            remarkBankOptions.addAll(cached);
            renderRemarksInDialog(cgRemarks, subjectName);
            return;
        }

        FirebaseRepository.get().getRemarkBank(schoolId, className, semesterNumber, subjectName,
                new FirebaseRepository.OnResult<List<String>>() {
                    @Override
                    public void onSuccess(List<String> options) {
                        if (!isAdded())
                            return;
                        remarkBankOptions.clear();
                        if (options != null) {
                            remarkBankOptions.addAll(options);
                        }
                        String cacheKey = className + "_sem_" + semesterNumber + "_" + subjectName;
                        AppCache.cachedRemarkBank.put(cacheKey, new ArrayList<>(remarkBankOptions));
                        renderRemarksInDialog(cgRemarks, subjectName);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded())
                            return;
                        remarkBankOptions.clear();
                        remarkBankOptions.addAll(
                                com.kartik.myschool.model.RemarkBank.defaultOptionsFor(subjectName, semesterNumber));
                        renderRemarksInDialog(cgRemarks, subjectName);
                    }
                });
    }

    private void renderRemarksInDialog(com.google.android.material.chip.ChipGroup cgRemarks, String subjectName) {
        if (cgRemarks == null)
            return;
        cgRemarks.removeAllViews();
        for (String option : remarkBankOptions) {
            Chip chip = new Chip(requireContext());

            String tempText = option;
            if (option.startsWith("[") && option.contains("]")) {
                tempText = option.substring(option.indexOf("]") + 1).trim();
            }
            final String displayText = tempText;

            chip.setText(displayText);
            chip.setCloseIconVisible(true);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setOnCloseIconClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Remove option?")
                        .setMessage(displayText)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove", (dialog, which) -> {
                            remarkBankOptions.remove(option);
                            saveRemarksFromDialog(subjectName, cgRemarks);
                        })
                        .show();
            });
            cgRemarks.addView(chip);
        }
    }

    private void saveRemarksFromDialog(String subjectName, com.google.android.material.chip.ChipGroup cgRemarks) {
        String schoolId = getActiveSchoolId();
        ClassModel activeClass = SessionContext.selectedClass;
        String className = activeClass != null && activeClass.className != null ? activeClass.className : "5";
        int semesterNumber = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.number : 1;

        FirebaseRepository.get().saveRemarkBank(schoolId, className, semesterNumber, subjectName,
                new ArrayList<>(remarkBankOptions), new FirebaseRepository.OnResult<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (!isAdded())
                            return;
                        String cacheKey = className + "_sem_" + semesterNumber + "_" + subjectName;
                        AppCache.cachedRemarkBank.put(cacheKey, new ArrayList<>(remarkBankOptions));
                        renderRemarksInDialog(cgRemarks, subjectName);

                        // Notify the main adapter so the expanded list updates immediately
                        if (remarksAdapter != null) {
                            remarksAdapter.notifyDataSetChanged();
                        }

                        Toast.makeText(getContext(), "Remarks saved.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded())
                            return;
                        Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private class SchoolSubjectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_SUBJECT = 1;

        private final List<SchoolSubjectItem> items = new ArrayList<>();

        public void setData(List<SchoolSubjectItem> data) {
            items.clear();
            if (data != null) {
                items.addAll(data);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isHeader ? TYPE_HEADER : TYPE_SUBJECT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                android.widget.TextView textView = new android.widget.TextView(parent.getContext());
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                float density = parent.getResources().getDisplayMetrics().density;
                int marginStart = (int) (12 * density);
                int marginVertical = (int) (12 * density);
                lp.setMargins(marginStart, marginVertical, marginStart, marginVertical);
                textView.setLayoutParams(lp);
                textView.setTextSize(14);
                textView.setTextColor(0xFF757575);
                textView.setTypeface(
                        android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                return new HeaderVH(textView);
            } else {
                com.kartik.myschool.databinding.ItemSubjectCardBinding cardB = com.kartik.myschool.databinding.ItemSubjectCardBinding
                        .inflate(
                                LayoutInflater.from(parent.getContext()), parent, false);
                return new SubjectVH(cardB);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            SchoolSubjectItem item = items.get(position);
            if (getItemViewType(position) == TYPE_HEADER) {
                HeaderVH h = (HeaderVH) holder;
                h.textView.setText(item.headerText);
            } else {
                SubjectVH sHolder = (SubjectVH) holder;
                com.kartik.myschool.databinding.ItemSubjectCardBinding b = sHolder.b;

                b.tvSubjectCode.setText(item.code);
                b.tvSerialNumber.setText(item.serial);

                String dispName = getSubjectDisplayName(b.getRoot().getContext(), item.name);
                b.tvSubjectName.setText(dispName);

                b.tvDetailsLeft1.setText(item.detailsLeft1);
                b.tvDetailsLeft2.setText(item.detailsLeft2);
                b.tvDetailsRight1.setText(item.detailsRight1);
                b.tvDetailsRight1.setVisibility(View.VISIBLE);

                b.tvIsApplicableLabel.setVisibility(View.GONE);
                b.switchApplicable.setVisibility(View.GONE);

                int color = android.graphics.Color.parseColor(item.colorHex);
                b.tvSerialNumber.setTextColor(color);
                b.tvSubjectName.setTextColor(color);

                int maxMarks = 100;
                if (com.kartik.myschool.SessionContext.selectedClass != null
                        && com.kartik.myschool.SessionContext.selectedClass.subjects != null) {
                    for (com.kartik.myschool.model.Subject s : com.kartik.myschool.SessionContext.selectedClass.subjects) {
                        if (s.name != null && s.name.equalsIgnoreCase(item.name)) {
                            maxMarks = s.maxMarks;
                            break;
                        }
                    }
                }

                final int finalMaxMarks = maxMarks;
                b.cardSubject.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(v.getContext(),
                            com.kartik.myschool.SubjectUpdateActivity.class);
                    intent.putExtra("subject_name", item.name);
                    intent.putExtra("subject_code", item.code);
                    intent.putExtra("subject_serial", item.serial);
                    intent.putExtra("subject_category", item.category);
                    intent.putExtra("subject_max_marks", finalMaxMarks);
                    intent.putExtra("details_left_1", item.detailsLeft1);
                    intent.putExtra("details_left_2", item.detailsLeft2);
                    v.getContext().startActivity(intent);
                });
                b.btnCardMenu.setOnClickListener(v -> {
                    androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(v.getContext(),
                            v);
                    popup.getMenu().add("Details");
                    popup.getMenu().add("Edit Max Marks");
                    popup.setOnMenuItemClickListener(menuItem -> {
                        android.content.Intent intent = new android.content.Intent(v.getContext(),
                                com.kartik.myschool.SubjectUpdateActivity.class);
                        intent.putExtra("subject_name", item.name);
                        intent.putExtra("subject_code", item.code);
                        intent.putExtra("subject_serial", item.serial);
                        intent.putExtra("subject_category", item.category);
                        intent.putExtra("subject_max_marks", finalMaxMarks);
                        intent.putExtra("details_left_1", item.detailsLeft1);
                        intent.putExtra("details_left_2", item.detailsLeft2);
                        v.getContext().startActivity(intent);
                        return true;
                    });
                    popup.show();
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class HeaderVH extends RecyclerView.ViewHolder {
            android.widget.TextView textView;

            HeaderVH(android.widget.TextView textView) {
                super(textView);
                this.textView = textView;
            }
        }

        class SubjectVH extends RecyclerView.ViewHolder {
            final com.kartik.myschool.databinding.ItemSubjectCardBinding b;

            SubjectVH(com.kartik.myschool.databinding.ItemSubjectCardBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }

    private class RemarksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_SUBJECT = 1;

        private final List<SchoolSubjectItem> items = new ArrayList<>();

        public void setData(List<SchoolSubjectItem> data) {
            items.clear();
            if (data != null) {
                items.addAll(data);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isHeader ? TYPE_HEADER : TYPE_SUBJECT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                android.widget.TextView textView = new android.widget.TextView(parent.getContext());
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                float density = parent.getResources().getDisplayMetrics().density;
                int marginStart = (int) (12 * density);
                int marginVertical = (int) (12 * density);
                lp.setMargins(marginStart, marginVertical, marginStart, marginVertical);
                textView.setLayoutParams(lp);
                textView.setTextSize(14);
                textView.setTextColor(0xFF757575);
                textView.setTypeface(
                        android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                return new HeaderVH(textView);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject_remark_row, parent,
                        false);
                return new RemarkSubjectVH(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            SchoolSubjectItem item = items.get(position);
            if (getItemViewType(position) == TYPE_HEADER) {
                HeaderVH h = (HeaderVH) holder;
                h.textView.setText(item.headerText);
            } else {
                RemarkSubjectVH h = (RemarkSubjectVH) holder;
                h.tvSubjectName.setText(getSubjectDisplayName(h.itemView.getContext(), item.name));

                h.btnAddRemark.setOnClickListener(v -> showRemarksEditDialog(item.name));

                // Set expanded state
                h.llDetailsContainer.setVisibility(item.isExpanded ? View.VISIBLE : View.GONE);
                h.ivExpand.setRotation(item.isExpanded ? 180f : 0f);

                if (item.isExpanded) {
                    loadAndRenderRemarks(h, item.name);
                }

                h.llHeader.setOnClickListener(v -> {
                    item.isExpanded = !item.isExpanded;

                    // Animate rotation
                    h.ivExpand.animate().rotation(item.isExpanded ? 180f : 0f).setDuration(200).start();
                    h.llDetailsContainer.setVisibility(item.isExpanded ? View.VISIBLE : View.GONE);

                    if (item.isExpanded) {
                        loadAndRenderRemarks(h, item.name);
                    }
                });
            }
        }

        private void loadAndRenderRemarks(RemarkSubjectVH h, String subjectName) {
            h.llCategoriesContainer.removeAllViews();
            h.llCategoriesContainer.setVisibility(View.GONE);
            h.tvSummary.setVisibility(View.GONE);
            h.tvNoRemarks.setVisibility(View.VISIBLE);
            h.tvNoRemarks.setText("Loading...");

            String schoolId = getActiveSchoolId();
            ClassModel activeClass = SessionContext.selectedClass;
            String className = activeClass != null && activeClass.className != null ? activeClass.className : "5";
            int semesterNumber = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.number : 1;

            String cacheKey = className + "_sem_" + semesterNumber + "_" + subjectName;
            List<String> cached = AppCache.cachedRemarkBank.get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                renderRemarks(h, cached);
            } else {
                FirebaseRepository.get().getRemarkBank(schoolId, className, semesterNumber, subjectName,
                        new FirebaseRepository.OnResult<List<String>>() {
                            @Override
                            public void onSuccess(List<String> options) {
                                if (!isAdded())
                                    return;
                                List<String> ops = options != null ? options : new ArrayList<>();
                                AppCache.cachedRemarkBank.put(cacheKey, new ArrayList<>(ops));
                                renderRemarks(h, ops);
                            }

                            @Override
                            public void onError(Exception e) {
                                if (!isAdded())
                                    return;
                                List<String> ops = com.kartik.myschool.model.RemarkBank.defaultOptionsFor(subjectName,
                                        semesterNumber);
                                renderRemarks(h, ops);
                            }
                        });
            }
        }

        private void renderRemarks(RemarkSubjectVH h, List<String> options) {
            h.llCategoriesContainer.removeAllViews();
            if (options == null || options.isEmpty()) {
                h.tvNoRemarks.setVisibility(View.VISIBLE);
                h.tvNoRemarks.setText("No remarks added.");
                h.llCategoriesContainer.setVisibility(View.GONE);
                h.tvSummary.setVisibility(View.GONE);
            } else {
                h.tvNoRemarks.setVisibility(View.GONE);
                h.llCategoriesContainer.setVisibility(View.VISIBLE);
                h.tvSummary.setVisibility(View.VISIBLE);
                h.tvSummary.setText(options.size() + " remarks total");

                java.util.Map<String, List<String>> categorized = new java.util.LinkedHashMap<>();
                categorized.put("उत्कृष्ट", new ArrayList<>());
                categorized.put("चांगली प्रगती", new ArrayList<>());
                categorized.put("समाधानकारक", new ArrayList<>());
                List<String> uncategorized = new ArrayList<>();

                for (String option : options) {
                    if (option.startsWith("[") && option.contains("]")) {
                        int endBracket = option.indexOf("]");
                        String cat = option.substring(1, endBracket).trim();
                        String text = option.substring(endBracket + 1).trim();
                        if (!categorized.containsKey(cat)) {
                            categorized.put(cat, new ArrayList<>());
                        }
                        categorized.get(cat).add(text);
                    } else {
                        uncategorized.add(option);
                    }
                }

                for (java.util.Map.Entry<String, List<String>> entry : categorized.entrySet()) {
                    if (entry.getValue().isEmpty())
                        continue;
                    addCategoryHeader(h.itemView.getContext(), h.llCategoriesContainer, entry.getKey());
                    for (String text : entry.getValue()) {
                        addRemarkItem(h.itemView.getContext(), h.llCategoriesContainer, text);
                    }
                }

                if (!uncategorized.isEmpty()) {
                    addCategoryHeader(h.itemView.getContext(), h.llCategoriesContainer, "Other");
                    for (String text : uncategorized) {
                        addRemarkItem(h.itemView.getContext(), h.llCategoriesContainer, text);
                    }
                }
            }
        }

        private void addCategoryHeader(android.content.Context context, android.widget.LinearLayout container,
                String title) {
            android.widget.TextView tv = new android.widget.TextView(context);
            tv.setText(title);
            tv.setTextSize(13);
            tv.setTextColor(0xFF000000); // Black for headers
            tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
            tv.setPadding(0, 12, 0, 4);
            container.addView(tv);
        }

        private void addRemarkItem(android.content.Context context, android.widget.LinearLayout container,
                String text) {
            android.widget.TextView tv = new android.widget.TextView(context);
            tv.setText("• " + text);
            tv.setTextSize(13);
            tv.setTextColor(0xFF333333);
            tv.setPadding(8, 4, 0, 4);
            container.addView(tv);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class HeaderVH extends RecyclerView.ViewHolder {
            android.widget.TextView textView;

            HeaderVH(android.widget.TextView textView) {
                super(textView);
                this.textView = textView;
            }
        }

        class RemarkSubjectVH extends RecyclerView.ViewHolder {
            android.widget.TextView tvSubjectName;
            android.widget.TextView tvSummary;
            android.widget.TextView tvNoRemarks;
            android.widget.LinearLayout llCategoriesContainer;
            com.google.android.material.button.MaterialButton btnAddRemark;
            android.widget.LinearLayout llHeader;
            android.widget.LinearLayout llDetailsContainer;
            android.widget.ImageView ivExpand;

            RemarkSubjectVH(View v) {
                super(v);
                tvSubjectName = v.findViewById(R.id.tvSubjectName);
                tvSummary = v.findViewById(R.id.tvSummary);
                tvNoRemarks = v.findViewById(R.id.tvNoRemarks);
                llCategoriesContainer = v.findViewById(R.id.llCategoriesContainer);
                btnAddRemark = v.findViewById(R.id.btnAddRemark);
                llHeader = v.findViewById(R.id.llHeader);
                llDetailsContainer = v.findViewById(R.id.llDetailsContainer);
                ivExpand = v.findViewById(R.id.ivExpand);
            }
        }
    }

    private String getActiveSchoolId() {
        if (SessionContext.selectedSchool != null && SessionContext.selectedSchool.id != null) {
            return SessionContext.selectedSchool.id;
        }
        ClassModel activeClass = SessionContext.selectedClass;
        return activeClass != null ? activeClass.schoolId : null;
    }

    private List<Semester> semestersList = new java.util.ArrayList<>();
    private ClassModel editingClass = null;

    private void setupClassTeacherEditor(ClassModel activeClass) {
        if (activeClass == null)
            return;

        b.btnSaveTeacherInfo.setEnabled(false);

        String yearId = SessionContext.selectedYear != null ? SessionContext.selectedYear.id : activeClass.yearId;
        if (yearId == null) {
            b.btnSaveTeacherInfo.setEnabled(true);
            return;
        }

        // 1. Get semesters list (cached or query)
        if (AppCache.cachedSemesters != null && !AppCache.cachedSemesters.isEmpty()) {
            semestersList = new java.util.ArrayList<>(AppCache.cachedSemesters);
            fetchClassesAndSetup(activeClass, yearId);
        } else {
            FirebaseRepository.get().getSemestersForYear(yearId, new FirebaseRepository.OnResult<List<Semester>>() {
                @Override
                public void onSuccess(List<Semester> list) {
                    if (list != null) {
                        AppCache.cachedSemesters = list;
                        semestersList = new java.util.ArrayList<>(list);
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> fetchClassesAndSetup(activeClass, yearId));
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            b.btnSaveTeacherInfo.setEnabled(true);
                            Toast.makeText(getContext(), "Failed to load semesters: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }

        // 2. Set click listeners
        b.btnSaveTeacherInfo.setOnClickListener(v -> saveTeacherInfo());
        b.btnEditTeacherInfo.setOnClickListener(v -> setTeacherFieldsEditable(true));
    }

    private void fetchClassesAndSetup(ClassModel activeClass, String yearId) {
        if (AppCache.cachedClasses != null) {
            b.btnSaveTeacherInfo.setEnabled(true);
            populateSemesterSpinner(activeClass);
        } else {
            if (SessionContext.selectedSchool == null)
                return;
            FirebaseRepository.get().getClassesForSchool(SessionContext.selectedSchool.id,
                    new FirebaseRepository.OnResult<List<ClassModel>>() {
                        @Override
                        public void onSuccess(List<ClassModel> list) {
                            if (list != null) {
                                AppCache.cachedClasses = list;
                            } else {
                                AppCache.cachedClasses = new java.util.ArrayList<>();
                            }
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    b.btnSaveTeacherInfo.setEnabled(true);
                                    populateSemesterSpinner(activeClass);
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    b.btnSaveTeacherInfo.setEnabled(true);
                                    Toast.makeText(getContext(), "Failed to load classes: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
        }
    }

    private void populateSemesterSpinner(ClassModel activeClass) {
        if (!isAdded() || getContext() == null)
            return;

        List<String> semNames = new java.util.ArrayList<>();
        int selectedIndex = 0;
        String currentSemesterId = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.id
                : activeClass.semesterId;

        for (int i = 0; i < semestersList.size(); i++) {
            Semester s = semestersList.get(i);
            semNames.add(s.name != null ? s.name : "Semester " + s.number);
            if (s.id != null && s.id.equals(currentSemesterId)) {
                selectedIndex = i;
            }
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                semNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spTeacherSemester.setAdapter(adapter);
        b.spTeacherSemester.setSelection(selectedIndex);

        // Load details for the current selection immediately
        onSemesterSelected(semestersList.get(selectedIndex), activeClass);

        // Post spinner item selection listener to avoid auto-trigger during layout pass
        b.spTeacherSemester.post(() -> {
            if (!isAdded())
                return;
            b.spTeacherSemester.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    Semester selectedSem = semestersList.get(position);
                    onSemesterSelected(selectedSem, activeClass);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        });
    }

    private void onSemesterSelected(Semester selectedSem, ClassModel activeClass) {
        if (selectedSem == null || activeClass == null)
            return;

        // If we are already displaying this semester, do not reload/override
        if (editingClass != null && selectedSem.id != null && selectedSem.id.equals(editingClass.semesterId)) {
            return;
        }

        // Look for existing class document for this semester in AppCache.cachedClasses
        ClassModel matchedClass = null;
        if (AppCache.cachedClasses != null) {
            for (ClassModel c : AppCache.cachedClasses) {
                if (c.className != null && c.className.equals(activeClass.className)
                        && c.division != null && c.division.equals(activeClass.division)
                        && c.semesterId != null && c.semesterId.equals(selectedSem.id)) {
                    matchedClass = c;
                    break;
                }
            }
        }

        if (matchedClass != null) {
            editingClass = matchedClass;
        } else {
            // Instantiate a new ClassModel cloning metadata from activeClass
            editingClass = new ClassModel();
            editingClass.schoolId = activeClass.schoolId;
            editingClass.yearId = activeClass.yearId;
            editingClass.academicYearLabel = activeClass.academicYearLabel;
            editingClass.semesterId = selectedSem.id;
            editingClass.className = activeClass.className;
            editingClass.division = activeClass.division;
            editingClass.examName = selectedSem.name;
            editingClass.year = activeClass.year;
            editingClass.subjects = activeClass.subjects != null ? new java.util.ArrayList<>(activeClass.subjects)
                    : new java.util.ArrayList<>();
            editingClass.studentCount = activeClass.studentCount;
            // Leave teacher fields blank for new creation
            editingClass.teacherName = "";
            editingClass.assistantTeacherName = "";
            editingClass.teacherEmail = "";
            editingClass.teacherPhone = "";
        }

        // Bind editingClass fields to the text fields
        b.etTeacherName.setText(editingClass.teacherName != null ? editingClass.teacherName : "");
        b.etAsstTeacherName.setText(editingClass.assistantTeacherName != null ? editingClass.assistantTeacherName : "");
        b.etTeacherEmail.setText(editingClass.teacherEmail != null ? editingClass.teacherEmail : "");
        b.etTeacherPhone.setText(editingClass.teacherPhone != null ? editingClass.teacherPhone : "");

        boolean hasTeacher = editingClass.teacherName != null && !editingClass.teacherName.trim().isEmpty();
        setTeacherFieldsEditable(!hasTeacher);
    }

    private void setTeacherFieldsEditable(boolean editable) {
        if (b == null)
            return;
        b.etTeacherName.setEnabled(editable);
        b.etAsstTeacherName.setEnabled(editable);
        b.etTeacherEmail.setEnabled(editable);
        b.etTeacherPhone.setEnabled(editable);

        b.etTeacherName.setAlpha(editable ? 1.0f : 0.75f);
        b.etAsstTeacherName.setAlpha(editable ? 1.0f : 0.75f);
        b.etTeacherEmail.setAlpha(editable ? 1.0f : 0.75f);
        b.etTeacherPhone.setAlpha(editable ? 1.0f : 0.75f);

        b.btnSaveTeacherInfo.setVisibility(editable ? View.VISIBLE : View.GONE);
        b.btnEditTeacherInfo.setVisibility(editable ? View.GONE : View.VISIBLE);
    }

    private void saveTeacherInfo() {
        if (editingClass == null)
            return;

        String teacherName = b.etTeacherName.getText() != null ? b.etTeacherName.getText().toString().trim() : "";
        String asstTeacherName = b.etAsstTeacherName.getText() != null ? b.etAsstTeacherName.getText().toString().trim()
                : "";
        String teacherEmail = b.etTeacherEmail.getText() != null ? b.etTeacherEmail.getText().toString().trim() : "";
        String teacherPhone = b.etTeacherPhone.getText() != null ? b.etTeacherPhone.getText().toString().trim() : "";

        if (teacherName.isEmpty()) {
            b.tilTeacherName.setError("Teacher name is required");
            return;
        }
        b.tilTeacherName.setError(null);

        editingClass.teacherName = teacherName;
        editingClass.assistantTeacherName = asstTeacherName;
        editingClass.teacherEmail = teacherEmail;
        editingClass.teacherPhone = teacherPhone;

        b.btnSaveTeacherInfo.setEnabled(false);
        FirebaseRepository.get().saveClass(editingClass, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String classId) {
                if (!isAdded())
                    return;
                editingClass.id = classId;

                // Update AppCache.cachedClasses
                if (AppCache.cachedClasses == null) {
                    AppCache.cachedClasses = new java.util.ArrayList<>();
                }
                boolean found = false;
                for (int i = 0; i < AppCache.cachedClasses.size(); i++) {
                    if (AppCache.cachedClasses.get(i).id != null && AppCache.cachedClasses.get(i).id.equals(classId)) {
                        AppCache.cachedClasses.set(i, editingClass);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    AppCache.cachedClasses.add(editingClass);
                }

                // If this is the active semester and active class, update SessionContext
                ClassModel activeClass = SessionContext.selectedClass;
                String activeSemId = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.id
                        : (activeClass != null ? activeClass.semesterId : null);
                if (activeClass != null && activeSemId != null
                        && activeClass.className != null && activeClass.className.equals(editingClass.className)
                        && activeClass.division != null && activeClass.division.equals(editingClass.division)
                        && activeSemId.equals(editingClass.semesterId)) {
                    SessionContext.selectedClass = editingClass;
                    SessionContext.save(getContext());
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        b.btnSaveTeacherInfo.setEnabled(true);
                        setTeacherFieldsEditable(false);
                        Toast.makeText(getContext(), "Teacher details saved successfully", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        b.btnSaveTeacherInfo.setEnabled(true);
                        Toast.makeText(getContext(), "Error saving details: " + e.getMessage(), Toast.LENGTH_LONG)
                                .show();
                    });
                }
            }
        });
    }

    private void setupWorkingDaysEditor(ClassModel activeClass) {
        if (activeClass == null)
            return;

        prepopulateMonthField(b.etWorkingDaysJun, "जून", activeClass);
        prepopulateMonthField(b.etWorkingDaysJul, "जुलै", activeClass);
        prepopulateMonthField(b.etWorkingDaysAug, "ऑगस्ट", activeClass);
        prepopulateMonthField(b.etWorkingDaysSep, "सप्टें", activeClass);
        prepopulateMonthField(b.etWorkingDaysOct, "ऑक्टो", activeClass);
        prepopulateMonthField(b.etWorkingDaysNov, "नोव्हे", activeClass);
        prepopulateMonthField(b.etWorkingDaysDec, "डिसें", activeClass);
        prepopulateMonthField(b.etWorkingDaysJan, "जाने", activeClass);
        prepopulateMonthField(b.etWorkingDaysFeb, "फेब्रु", activeClass);
        prepopulateMonthField(b.etWorkingDaysMar, "मार्च", activeClass);
        prepopulateMonthField(b.etWorkingDaysApr, "एप्रिल", activeClass);
        prepopulateMonthField(b.etWorkingDaysMay, "मे", activeClass);

        // Check if there is already configured working days
        boolean hasConfiguredDays = false;
        if (activeClass.monthlyWorkingDays != null && !activeClass.monthlyWorkingDays.isEmpty()) {
            for (Integer val : activeClass.monthlyWorkingDays.values()) {
                if (val != null && val > 0) {
                    hasConfiguredDays = true;
                    break;
                }
            }
        }

        // Set initial state
        setWorkingDaysFieldsEditable(!hasConfiguredDays);

        b.btnEditWorkingDays.setOnClickListener(v -> setWorkingDaysFieldsEditable(true));

        b.btnLoadDefaultWorkingDays.setOnClickListener(v -> {
            b.etWorkingDaysJun.setText("13");
            b.etWorkingDaysJul.setText("27");
            b.etWorkingDaysAug.setText("25");
            b.etWorkingDaysSep.setText("24");
            b.etWorkingDaysOct.setText("18");
            b.etWorkingDaysNov.setText("23");
            b.etWorkingDaysDec.setText("26");
            b.etWorkingDaysJan.setText("23");
            b.etWorkingDaysFeb.setText("24");
            b.etWorkingDaysMar.setText("27");
            b.etWorkingDaysApr.setText("18");
            b.etWorkingDaysMay.setText("");
        });

        b.btnSaveWorkingDays.setOnClickListener(v -> {
            activeClass.monthlyWorkingDays.put("जून", parseVal(b.etWorkingDaysJun));
            activeClass.monthlyWorkingDays.put("जुलै", parseVal(b.etWorkingDaysJul));
            activeClass.monthlyWorkingDays.put("ऑगस्ट", parseVal(b.etWorkingDaysAug));
            activeClass.monthlyWorkingDays.put("सप्टें", parseVal(b.etWorkingDaysSep));
            activeClass.monthlyWorkingDays.put("ऑक्टो", parseVal(b.etWorkingDaysOct));
            activeClass.monthlyWorkingDays.put("नोव्हे", parseVal(b.etWorkingDaysNov));
            activeClass.monthlyWorkingDays.put("डिसें", parseVal(b.etWorkingDaysDec));
            activeClass.monthlyWorkingDays.put("जाने", parseVal(b.etWorkingDaysJan));
            activeClass.monthlyWorkingDays.put("फेब्रु", parseVal(b.etWorkingDaysFeb));
            activeClass.monthlyWorkingDays.put("मार्च", parseVal(b.etWorkingDaysMar));
            activeClass.monthlyWorkingDays.put("एप्रिल", parseVal(b.etWorkingDaysApr));
            activeClass.monthlyWorkingDays.put("मे", parseVal(b.etWorkingDaysMay));

            b.btnSaveWorkingDays.setEnabled(false);
            FirebaseRepository.get().saveClass(activeClass, new FirebaseRepository.OnResult<String>() {
                @Override
                public void onSuccess(String classId) {
                    if (!isAdded())
                        return;
                    activeClass.id = classId;

                    // Update AppCache.cachedClasses
                    if (AppCache.cachedClasses == null) {
                        AppCache.cachedClasses = new ArrayList<>();
                    }
                    boolean found = false;
                    for (int i = 0; i < AppCache.cachedClasses.size(); i++) {
                        if (AppCache.cachedClasses.get(i).id != null
                                && AppCache.cachedClasses.get(i).id.equals(classId)) {
                            AppCache.cachedClasses.set(i, activeClass);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        AppCache.cachedClasses.add(activeClass);
                    }

                    // Update SessionContext active class
                    SessionContext.selectedClass = activeClass;
                    SessionContext.save(getContext());

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            b.btnSaveWorkingDays.setEnabled(true);
                            setWorkingDaysFieldsEditable(false);
                            Toast.makeText(getContext(), "कामकाजाचे दिवस यशस्वीरित्या जतन झाले!", Toast.LENGTH_SHORT)
                                    .show();
                        });
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            b.btnSaveWorkingDays.setEnabled(true);
                            Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        });
    }

    private void setWorkingDaysFieldsEditable(boolean editable) {
        EditText[] fields = {
                b.etWorkingDaysJun, b.etWorkingDaysJul, b.etWorkingDaysAug, b.etWorkingDaysSep,
                b.etWorkingDaysOct, b.etWorkingDaysNov, b.etWorkingDaysDec, b.etWorkingDaysJan,
                b.etWorkingDaysFeb, b.etWorkingDaysMar, b.etWorkingDaysApr, b.etWorkingDaysMay
        };
        for (EditText et : fields) {
            et.setEnabled(editable);
            et.setFocusable(editable);
            et.setFocusableInTouchMode(editable);
            et.setAlpha(editable ? 1.0f : 0.65f);
        }
        b.btnSaveWorkingDays.setVisibility(editable ? View.VISIBLE : View.GONE);
        b.btnLoadDefaultWorkingDays.setVisibility(editable ? View.VISIBLE : View.GONE);
        b.btnEditWorkingDays.setVisibility(editable ? View.GONE : View.VISIBLE);
    }

    private void prepopulateMonthField(EditText et, String month, ClassModel activeClass) {
        if (activeClass.monthlyWorkingDays != null) {
            Integer val = activeClass.monthlyWorkingDays.get(month);
            et.setText(val != null && val > 0 ? String.valueOf(val) : "");
        }
    }

    private int parseVal(EditText et) {
        String text = et.getText() != null ? et.getText().toString().trim() : "";
        if (text.isEmpty())
            return 0;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setupClassesListing() {
        School school = SessionContext.selectedSchool;
        if (school == null) {
            FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
                @Override
                public void onSuccess(com.kartik.myschool.model.Teacher t) {
                    FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {
                        @Override
                        public void onSuccess(School s) {
                            SessionContext.selectedSchool = s;
                            AppCache.selectedSchool = s;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> loadClassesForUdise(s.udiseCode));
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            showClassesError("School info not loaded. Please fill UDISE code in profile.");
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    showClassesError("Error loading teacher profile: " + e.getMessage());
                }
            });
        } else {
            loadClassesForUdise(school.udiseCode);
        }
    }

    private void showClassesError(String error) {
        if (!isAdded() || b == null)
            return;
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                b.llClassesListContainer.removeAllViews();
                android.widget.TextView tvError = new android.widget.TextView(requireContext());
                tvError.setText(error);
                tvError.setTextColor(getResources().getColor(R.color.error));
                tvError.setTextSize(14);
                b.llClassesListContainer.addView(tvError);
            });
        }
    }

    private void loadClassesForUdise(String udiseCode) {
        if (udiseCode == null || udiseCode.isEmpty()) {
            showClassesError("UDISE Code is empty. Please set it in profile.");
            return;
        }

        if (AppCache.cachedClasses != null && !AppCache.cachedClasses.isEmpty()) {
            String currentYearLabel = SessionContext.getYearLabel();
            List<ClassModel> filtered = new ArrayList<>();
            for (ClassModel c : AppCache.cachedClasses) {
                if (currentYearLabel.equals(c.academicYearLabel)) {
                    filtered.add(c);
                }
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderClassesList(filtered));
            }
            return;
        }

        b.llClassesListContainer.removeAllViews();
        android.widget.TextView tvLoading = new android.widget.TextView(requireContext());
        tvLoading.setText(requireContext().getString(R.string.msg_processing));
        tvLoading.setTextColor(getResources().getColor(R.color.on_surface_variant));
        tvLoading.setTextSize(14);
        tvLoading.setPadding(0, 16, 0, 16);
        b.llClassesListContainer.addView(tvLoading);

        FirebaseRepository.get().getClassesForUdiseCode(udiseCode, new FirebaseRepository.OnResult<List<ClassModel>>() {
            @Override
            public void onSuccess(List<ClassModel> list) {
                if (!isAdded() || b == null)
                    return;

                AppCache.cachedClasses = list;
                String currentYearLabel = SessionContext.getYearLabel();
                List<ClassModel> filtered = new ArrayList<>();
                for (ClassModel c : list) {
                    if (currentYearLabel.equals(c.academicYearLabel)) {
                        filtered.add(c);
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> renderClassesList(filtered));
                }
            }

            @Override
            public void onError(Exception e) {
                showClassesError("Error loading classes: " + e.getMessage());
            }
        });
    }

    private void renderClassesList(List<ClassModel> classes) {
        if (b == null || !isAdded())
            return;
        b.llClassesListContainer.removeAllViews();

        if (classes.isEmpty()) {
            android.widget.TextView tvEmpty = new android.widget.TextView(requireContext());
            tvEmpty.setText(requireContext().getString(R.string.profile_no_classes));
            tvEmpty.setTextColor(getResources().getColor(R.color.on_surface_variant));
            tvEmpty.setTextSize(14);
            tvEmpty.setPadding(0, 16, 0, 16);
            b.llClassesListContainer.addView(tvEmpty);
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        int padding12 = (int) (12 * density);
        int padding8 = (int) (8 * density);

        for (ClassModel c : classes) {
            android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(padding12, padding12, padding12, padding12);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(getResources().getColor(R.color.surface_variant));
            gd.setCornerRadius(12 * density);
            row.setBackground(gd);

            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, padding12);
            row.setLayoutParams(lp);

            // Left circular badge for Class Number
            android.widget.TextView badge = new android.widget.TextView(requireContext());
            badge.setText(c.className != null ? c.className : "");
            badge.setTextSize(16);
            badge.setTextColor(getResources().getColor(R.color.white));
            badge.setGravity(android.view.Gravity.CENTER);
            badge.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            int badgeSize = (int) (36 * density);
            android.widget.LinearLayout.LayoutParams badgeLp = new android.widget.LinearLayout.LayoutParams(badgeSize,
                    badgeSize);
            badgeLp.setMarginEnd(padding12);
            badge.setLayoutParams(badgeLp);

            android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
            badgeBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            badgeBg.setColor(getResources().getColor(R.color.class_number_purple));
            badge.setBackground(badgeBg);
            row.addView(badge);

            // Class and Division + Class Teacher details
            android.widget.LinearLayout textLayout = new android.widget.LinearLayout(requireContext());
            textLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            android.widget.LinearLayout.LayoutParams textLp = new android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textLayout.setLayoutParams(textLp);

            android.widget.TextView tvClassDiv = new android.widget.TextView(requireContext());
            String classDivText = requireContext().getString(R.string.class_div_format,
                    c.className != null ? c.className : "",
                    c.division != null && !c.division.isEmpty() ? c.division : "-");
            tvClassDiv.setText(classDivText);
            tvClassDiv.setTextSize(15);
            tvClassDiv.setTextColor(getResources().getColor(R.color.on_surface));
            tvClassDiv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            textLayout.addView(tvClassDiv);

            android.widget.TextView tvTeacher = new android.widget.TextView(requireContext());
            String teacherText;
            if (c.teacherName != null && !c.teacherName.isEmpty()) {
                teacherText = requireContext().getString(R.string.profile_teacher_line, c.teacherName);
            } else {
                teacherText = requireContext().getString(R.string.label_class_teacher) + ": -";
            }
            tvTeacher.setText(teacherText);
            tvTeacher.setTextSize(12);
            tvTeacher.setTextColor(getResources().getColor(R.color.on_surface_variant));
            tvTeacher.setPadding(0, 2, 0, 0);
            textLayout.addView(tvTeacher);

            row.addView(textLayout);

            // Right-aligned Active Students Count using localized profile_students_count
            // string
            android.widget.TextView tvCount = new android.widget.TextView(requireContext());
            tvCount.setText(requireContext().getString(R.string.profile_students_count, c.studentCount));
            tvCount.setTextSize(13);
            tvCount.setTextColor(getResources().getColor(R.color.primary));
            tvCount.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvCount.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END);

            android.widget.LinearLayout.LayoutParams countLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            countLp.setMarginStart(padding8);
            tvCount.setLayoutParams(countLp);
            row.addView(tvCount);

            b.llClassesListContainer.addView(row);
        }
    }

    private void loadSchoolWideStats() {
        School school = SessionContext.selectedSchool;
        if (school == null) {
            FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
                @Override
                public void onSuccess(com.kartik.myschool.model.Teacher t) {
                    FirebaseRepository.get().ensureTeacherSchool(t, new FirebaseRepository.OnResult<School>() {
                        @Override
                        public void onSuccess(School s) {
                            SessionContext.selectedSchool = s;
                            AppCache.selectedSchool = s;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> loadSchoolWideStatsForUdise(s.udiseCode));
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                }
            });
        } else {
            loadSchoolWideStatsForUdise(school.udiseCode);
        }
    }

    private void loadSchoolWideStatsForUdise(String udiseCode) {
        if (udiseCode == null || udiseCode.isEmpty()) {
            return;
        }

        ClassModel activeClass = SessionContext.selectedClass;
        if (activeClass == null || activeClass.id == null)
            return;

        if (AppCache.cachedStudents != null && activeClass.id.equals(AppCache.cachedClassIdForStudents)) {
            calculateAndShowStats(AppCache.cachedStudents);
            return;
        }

        FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                if (!isAdded() || b == null)
                    return;
                AppCache.cachedStudents = students;
                AppCache.cachedClassIdForStudents = activeClass.id;
                calculateAndShowStats(students);
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void calculateAndShowStats(List<Student> activeStudents) {
        int boys = 0;
        int girls = 0;
        int generalCount = 0;
        int obcCount = 0;
        int scCount = 0;
        int stCount = 0;

        for (Student s : activeStudents) {
            if (s.gender != null && (s.gender.equalsIgnoreCase("Female")
                    || s.gender.equals("2")
                    || s.gender.equalsIgnoreCase("स्त्री")
                    || s.gender.equalsIgnoreCase("मुलगी"))) {
                girls++;
            } else {
                boys++;
            }

            if (s.cast != null) {
                String c = s.cast.toUpperCase().trim();
                if (c.contains("OBC") || c.equals("5"))
                    obcCount++;
                else if (c.contains("SC") || c.equals("1") || c.equals("0"))
                    scCount++;
                else if (c.contains("ST") || c.equals("2"))
                    stCount++;
                else
                    generalCount++;
            } else {
                generalCount++;
            }
        }

        final int finalBoys = boys;
        final int finalGirls = girls;
        final int finalGen = generalCount;
        final int finalObc = obcCount;
        final int finalSc = scCount;
        final int finalSt = stCount;
        final int finalTotal = activeStudents.size();

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                b.tvGenderBoysCount.setText(String.valueOf(finalBoys));
                b.tvGenderGirlsCount.setText(String.valueOf(finalGirls));
                boolean isMr = java.util.Locale.getDefault().getLanguage().equals("mr");
                if (finalTotal > 0) {
                    double boysPercent = (finalBoys * 100.0) / finalTotal;
                    double girlsPercent = (finalGirls * 100.0) / finalTotal;
                    String ratioText = isMr
                            ? String.format("वर्ग लिंग गुणोत्तर: %.1f%% मुले / %.1f%% मुली", boysPercent, girlsPercent)
                            : String.format("Class Gender Ratio: %.1f%% Boys / %.1f%% Girls", boysPercent,
                                    girlsPercent);
                    b.tvGenderRatio.setText(ratioText);
                } else {
                    b.tvGenderRatio.setText(isMr ? "या वर्गात विद्यार्थी आढळले नाहीत." : "No students in this class.");
                }

                if (isMr) {
                    b.tvCastGeneral.setText("सर्वसाधारण (खुला प्रवर्ग) : " + finalGen + " विद्यार्थी");
                    b.tvCastObc.setText("इतर मागासवर्ग (OBC) : " + finalObc + " विद्यार्थी");
                    b.tvCastSc.setText("अनुसूचित जाती (SC) : " + finalSc + " विद्यार्थी");
                    b.tvCastSt.setText("अनुसूचित जमाती (ST) : " + finalSt + " विद्यार्थी");
                    b.tvSchoolInfoTotalStudents.setText("वर्ग एकूण पटसंख्या: " + finalTotal + " विद्यार्थी");
                } else {
                    b.tvCastGeneral.setText("General (Open) : " + finalGen + " students");
                    b.tvCastObc.setText("OBC Category : " + finalObc + " students");
                    b.tvCastSc.setText("SC Category : " + finalSc + " students");
                    b.tvCastSt.setText("ST Category : " + finalSt + " students");
                    b.tvSchoolInfoTotalStudents.setText("Class total enrollment: " + finalTotal + " students");
                }
            });
        }
    }
}
