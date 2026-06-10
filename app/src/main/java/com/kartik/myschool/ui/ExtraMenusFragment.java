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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        String classDiv = SessionContext.getClassDivLabel();
        b.tvExtraContextSubtitle.setText("Year: " + yearLabel + "   " + classDiv);

        String contextTitle = "Student Administration";
        switch (menuType) {
            case "school_info": contextTitle = "School Details Module"; break;
            case "gender": contextTitle = "Gender Distribution Module"; break;
            case "cast_category": contextTitle = "Cast Category Module"; break;
            case "class_teacher": contextTitle = "Class Teacher Assignment"; break;
            case "classes": contextTitle = "School Classes Listing"; break;
            case "subject": contextTitle = "Subject Criteria Guidelines"; break;
            case "default_values": contextTitle = "Baseline Settings Module"; break;
            case "working_days": contextTitle = "Holidays & Working Log"; break;
            case "he_she_items": contextTitle = "Pronouns Comment Helper"; break;
        }
        b.tvExtraContextTitle.setText(contextTitle);
    }

    private void toggleDataModule() {
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
            case "school_info": b.llSchoolInfo.setVisibility(View.VISIBLE); break;
            case "gender": b.llGender.setVisibility(View.VISIBLE); break;
            case "cast_category": b.llCastCategory.setVisibility(View.VISIBLE); break;
            case "class_teacher": b.llClassTeacher.setVisibility(View.VISIBLE); break;
            case "classes": b.llClasses.setVisibility(View.VISIBLE); break;
            case "subject": b.llSubject.setVisibility(View.VISIBLE); break;
            case "default_values": b.llDefaultValues.setVisibility(View.VISIBLE); break;
            case "working_days": b.llWorkingDays.setVisibility(View.VISIBLE); break;
            case "he_she_items": b.llHeSheItems.setVisibility(View.VISIBLE); break;
        }
    }

    private void loadDynamicData() {
        // Load School details
        School school = SessionContext.selectedSchool;
        if (school != null) {
            if (school.name != null) b.tvSchoolInfoName.setText(getString(R.string.fmt_school_name, school.name));
            if (school.udiseCode != null) b.tvSchoolInfoUdise.setText(getString(R.string.fmt_school_udise, school.udiseCode));
            if (school.board != null) b.tvSchoolInfoBoard.setText(getString(R.string.fmt_school_board, school.board));
            if (school.address != null && !school.address.isEmpty()) b.tvSchoolInfoAddress.setText(getString(R.string.fmt_school_address, school.address));
        }

        b.btnEditSchoolInfo.setOnClickListener(v -> {
            if (school != null) {
                com.kartik.myschool.AppCache.selectedSchool = school;
                android.content.Intent intent = new android.content.Intent(getContext(), com.kartik.myschool.SchoolRegisterActivity.class);
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
        }

        if (menuType.equals("school_info") || menuType.equals("gender") || menuType.equals("cast_category")) {
            loadSchoolWideStats();
        }

        // Save Default admission baseline click listener
        b.btnSaveDefaults.setOnClickListener(v -> Toast.makeText(getContext(), R.string.msg_admission_defaults_baseline_se, Toast.LENGTH_SHORT).show());

        if (menuType.equals("classes")) {
            setupClassesListing();
        }
    }

    private void setupRemarkBankEditor(ClassModel activeClass) {
        if (!isAdded() || activeClass == null) return;

        remarkBankSubjects.clear();
        remarkBankSubjects.add("General");
        if (activeClass.subjects != null) {
            for (Subject subject : activeClass.subjects) {
                if (subject != null && subject.name != null && !subject.name.trim().isEmpty()) {
                    remarkBankSubjects.add(subject.name);
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                remarkBankSubjects
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spRemarkBankSubject.setAdapter(adapter);
        b.spRemarkBankSubject.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedRemarkBankSubject = remarkBankSubjects.get(position);
                loadRemarkBankOptions();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        b.btnAddRemarkBankOption.setOnClickListener(v -> showAddRemarkOptionDialog());
        loadRemarkBankOptions();
    }

    private void loadRemarkBankOptions() {
        String schoolId = getActiveSchoolId();
        List<String> cached = AppCache.cachedRemarkBank.get(selectedRemarkBankSubject);
        if (cached != null && !cached.isEmpty()) {
            remarkBankOptions.clear();
            remarkBankOptions.addAll(cached);
            renderRemarkBankOptions();
            return;
        }

        FirebaseRepository.get().getRemarkBank(schoolId, selectedRemarkBankSubject,
                new FirebaseRepository.OnResult<List<String>>() {
                    @Override
                    public void onSuccess(List<String> options) {
                        if (!isAdded()) return;
                        remarkBankOptions.clear();
                        if (options != null) {
                            remarkBankOptions.addAll(options);
                        }
                        AppCache.cachedRemarkBank.put(selectedRemarkBankSubject, new ArrayList<>(remarkBankOptions));
                        renderRemarkBankOptions();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        remarkBankOptions.clear();
                        remarkBankOptions.addAll(com.kartik.myschool.model.RemarkBank.defaultOptionsFor(selectedRemarkBankSubject));
                        renderRemarkBankOptions();
                    }
                });
    }

    private void renderRemarkBankOptions() {
        if (b == null) return;
        b.cgRemarkBankOptions.removeAllViews();
        for (String option : remarkBankOptions) {
            Chip chip = new Chip(requireContext());
            chip.setText(option);
            chip.setCloseIconVisible(true);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setOnCloseIconClickListener(v -> confirmRemoveRemarkOption(option));
            chip.setOnLongClickListener(v -> {
                confirmRemoveRemarkOption(option);
                return true;
            });
            b.cgRemarkBankOptions.addView(chip);
        }
    }

    private void showAddRemarkOptionDialog() {
        if (!isAdded()) return;
        EditText input = new EditText(requireContext());
        input.setHint("Remark option");
        input.setSingleLine(false);
        input.setMinLines(2);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add remark option")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (dialog, which) -> {
                    String text = input.getText() != null ? input.getText().toString().trim() : "";
                    if (text.isEmpty()) {
                        Toast.makeText(getContext(), "Enter a remark option.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!remarkBankOptions.contains(text)) {
                        remarkBankOptions.add(text);
                    }
                    saveRemarkBankOptions("Option added.");
                })
                .show();
    }

    private void confirmRemoveRemarkOption(String option) {
        if (!isAdded()) return;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Remove option?")
                .setMessage(option)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> {
                    remarkBankOptions.remove(option);
                    saveRemarkBankOptions("Option removed.");
                })
                .show();
    }

    private void saveRemarkBankOptions(String message) {
        String schoolId = getActiveSchoolId();
        b.btnAddRemarkBankOption.setEnabled(false);
        FirebaseRepository.get().saveRemarkBank(schoolId, selectedRemarkBankSubject,
                new ArrayList<>(remarkBankOptions), new FirebaseRepository.OnResult<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (!isAdded()) return;
                        AppCache.cachedRemarkBank.put(selectedRemarkBankSubject, new ArrayList<>(remarkBankOptions));
                        b.btnAddRemarkBankOption.setEnabled(true);
                        renderRemarkBankOptions();
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        b.btnAddRemarkBankOption.setEnabled(true);
                        Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        loadRemarkBankOptions();
                    }
                });
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
        if (activeClass == null) return;

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
                            Toast.makeText(getContext(), "Failed to load semesters: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }

        // 2. Set save click listener
        b.btnSaveTeacherInfo.setOnClickListener(v -> saveTeacherInfo());
    }

    private void fetchClassesAndSetup(ClassModel activeClass, String yearId) {
        if (AppCache.cachedClasses != null) {
            b.btnSaveTeacherInfo.setEnabled(true);
            populateSemesterSpinner(activeClass);
        } else {
            FirebaseRepository.get().getClassesForYear(yearId, new FirebaseRepository.OnResult<List<ClassModel>>() {
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
                            Toast.makeText(getContext(), "Failed to load classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }

    private void populateSemesterSpinner(ClassModel activeClass) {
        if (!isAdded() || getContext() == null) return;

        List<String> semNames = new java.util.ArrayList<>();
        int selectedIndex = 0;
        String currentSemesterId = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.id : activeClass.semesterId;

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
                semNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spTeacherSemester.setAdapter(adapter);
        b.spTeacherSemester.setSelection(selectedIndex);

        // Load details for the current selection immediately
        onSemesterSelected(semestersList.get(selectedIndex), activeClass);

        // Post spinner item selection listener to avoid auto-trigger during layout pass
        b.spTeacherSemester.post(() -> {
            if (!isAdded()) return;
            b.spTeacherSemester.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    Semester selectedSem = semestersList.get(position);
                    onSemesterSelected(selectedSem, activeClass);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        });
    }

    private void onSemesterSelected(Semester selectedSem, ClassModel activeClass) {
        if (selectedSem == null || activeClass == null) return;

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
            editingClass.subjects = activeClass.subjects != null ? new java.util.ArrayList<>(activeClass.subjects) : new java.util.ArrayList<>();
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
    }

    private void saveTeacherInfo() {
        if (editingClass == null) return;

        String teacherName = b.etTeacherName.getText() != null ? b.etTeacherName.getText().toString().trim() : "";
        String asstTeacherName = b.etAsstTeacherName.getText() != null ? b.etAsstTeacherName.getText().toString().trim() : "";
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
                if (!isAdded()) return;
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
                String activeSemId = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.id : (activeClass != null ? activeClass.semesterId : null);
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
                        Toast.makeText(getContext(), "Teacher details saved successfully", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        b.btnSaveTeacherInfo.setEnabled(true);
                        Toast.makeText(getContext(), "Error saving details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void setupWorkingDaysEditor(ClassModel activeClass) {
        if (activeClass == null) return;

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
                    if (!isAdded()) return;
                    activeClass.id = classId;

                    // Update AppCache.cachedClasses
                    if (AppCache.cachedClasses == null) {
                        AppCache.cachedClasses = new ArrayList<>();
                    }
                    boolean found = false;
                    for (int i = 0; i < AppCache.cachedClasses.size(); i++) {
                        if (AppCache.cachedClasses.get(i).id != null && AppCache.cachedClasses.get(i).id.equals(classId)) {
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
                            Toast.makeText(getContext(), "कामकाजाचे दिवस यशस्वीरित्या जतन झाले!", Toast.LENGTH_SHORT).show();
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
        if (text.isEmpty()) return 0;
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
        if (!isAdded() || b == null) return;
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
                if (!isAdded() || b == null) return;
                
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
        if (b == null || !isAdded()) return;
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
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
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
            android.widget.LinearLayout.LayoutParams badgeLp = new android.widget.LinearLayout.LayoutParams(badgeSize, badgeSize);
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
            android.widget.LinearLayout.LayoutParams textLp = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
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
            
            // Right-aligned Active Students Count using localized profile_students_count string
            android.widget.TextView tvCount = new android.widget.TextView(requireContext());
            tvCount.setText(requireContext().getString(R.string.profile_students_count, c.studentCount));
            tvCount.setTextSize(13);
            tvCount.setTextColor(getResources().getColor(R.color.primary));
            tvCount.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvCount.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END);
            
            android.widget.LinearLayout.LayoutParams countLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
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
                        public void onError(Exception e) {}
                    });
                }
                @Override
                public void onError(Exception e) {}
            });
        } else {
            loadSchoolWideStatsForUdise(school.udiseCode);
        }
    }

    private void loadSchoolWideStatsForUdise(String udiseCode) {
        if (udiseCode == null || udiseCode.isEmpty()) {
            return;
        }

        FirebaseRepository.get().getClassesForUdiseCode(udiseCode, new FirebaseRepository.OnResult<List<ClassModel>>() {
            @Override
            public void onSuccess(List<ClassModel> classes) {
                if (!isAdded() || b == null) return;
                
                String currentYearLabel = SessionContext.getYearLabel();
                java.util.Set<String> activeClassIds = new java.util.HashSet<>();
                for (ClassModel c : classes) {
                    if (currentYearLabel.equals(c.academicYearLabel)) {
                        activeClassIds.add(c.id);
                    }
                }

                FirebaseRepository.get().getStudentsForUdiseCode(udiseCode, new FirebaseRepository.OnResult<List<Student>>() {
                    @Override
                    public void onSuccess(List<Student> students) {
                        if (!isAdded() || b == null) return;

                        List<Student> activeStudents = new ArrayList<>();
                        for (Student s : students) {
                            if (s.classId != null && activeClassIds.contains(s.classId)) {
                                activeStudents.add(s);
                            }
                        }

                        int boys = 0;
                        int girls = 0;
                        int generalCount = 0;
                        int obcCount = 0;
                        int scCount = 0;
                        int stCount = 0;

                        for (Student s : activeStudents) {
                            if (s.gender != null && s.gender.equalsIgnoreCase("Female")) {
                                girls++;
                            } else {
                                boys++;
                            }

                            if (s.cast != null) {
                                String c = s.cast.toUpperCase();
                                if (c.contains("OBC")) obcCount++;
                                else if (c.contains("SC")) scCount++;
                                else if (c.contains("ST")) stCount++;
                                else generalCount++;
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
                                        ? String.format("शाळा लिंग गुणोत्तर: %.1f%% मुले / %.1f%% मुली", boysPercent, girlsPercent)
                                        : String.format("School Gender Ratio: %.1f%% Boys / %.1f%% Girls", boysPercent, girlsPercent);
                                    b.tvGenderRatio.setText(ratioText);
                                } else {
                                    b.tvGenderRatio.setText(isMr ? "या शैक्षणिक वर्षात विद्यार्थी आढळले नाहीत." : "No students in this academic year.");
                                }

                                if (isMr) {
                                    b.tvCastGeneral.setText("सर्वसाधारण (खुला प्रवर्ग) : " + finalGen + " विद्यार्थी");
                                    b.tvCastObc.setText("इतर मागासवर्ग (OBC) : " + finalObc + " विद्यार्थी");
                                    b.tvCastSc.setText("अनुसूचित जाती (SC) : " + finalSc + " विद्यार्थी");
                                    b.tvCastSt.setText("अनुसूचित जमाती (ST) : " + finalSt + " विद्यार्थी");
                                    b.tvSchoolInfoTotalStudents.setText("शाळा एकूण पटसंख्या: " + finalTotal + " विद्यार्थी");
                                } else {
                                    b.tvCastGeneral.setText("General (Open) : " + finalGen + " students");
                                    b.tvCastObc.setText("OBC Category : " + finalObc + " students");
                                    b.tvCastSc.setText("SC Category : " + finalSc + " students");
                                    b.tvCastSt.setText("ST Category : " + finalSt + " students");
                                    b.tvSchoolInfoTotalStudents.setText("School-wide total enrollment: " + finalTotal + " students");
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {}
                });
            }

            @Override
            public void onError(Exception e) {}
        });
    }
}
