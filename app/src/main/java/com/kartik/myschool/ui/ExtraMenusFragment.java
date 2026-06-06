package com.kartik.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;

public class ExtraMenusFragment extends Fragment {

    private FragmentExtraMenusBinding b;
    private String menuType = "school_info";

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

            // Real-time student statistics for active class (Gender & Cast categories)
            FirebaseRepository.get().getStudentsForClass(activeClass.id, new FirebaseRepository.OnResult<List<Student>>() {
                @Override
                public void onSuccess(List<Student> list) {
                    int boys = 0;
                    int girls = 0;
                    int generalCount = 0;
                    int obcCount = 0;
                    int scCount = 0;
                    int stCount = 0;

                    for (Student s : list) {
                        // Gender check
                        if (s.gender != null && s.gender.equalsIgnoreCase("Female")) {
                            girls++;
                        } else {
                            boys++;
                        }

                        // Cast category check
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
                    final int finalTotal = list.size();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Update Gender distribution
                            b.tvGenderBoysCount.setText(String.valueOf(finalBoys));
                            b.tvGenderGirlsCount.setText(String.valueOf(finalGirls));
                            if (finalTotal > 0) {
                                double boysPercent = (finalBoys * 100.0) / finalTotal;
                                double girlsPercent = (finalGirls * 100.0) / finalTotal;
                                b.tvGenderRatio.setText(String.format("Active Class Gender Ratio: %.1f%% Boys / %.1f%% Girls", boysPercent, girlsPercent));
                            } else {
                                b.tvGenderRatio.setText(R.string.msg_no_students_in_active_class);
                            }

                            // Update Cast distribution
                            b.tvCastGeneral.setText("General (Open) : " + finalGen + " students");
                            b.tvCastObc.setText("OBC Category : " + finalObc + " students");
                            b.tvCastSc.setText("SC Category : " + finalSc + " students");
                            b.tvCastSt.setText("ST Category : " + finalSt + " students");
                            b.tvSchoolInfoTotalStudents.setText("Active class enrollment: " + finalTotal + " students");
                        });
                    }
                }

                @Override
                public void onError(Exception e) {}
            });
        }

        // Save Default admission baseline click listener
        b.btnSaveDefaults.setOnClickListener(v -> Toast.makeText(getContext(), R.string.msg_admission_defaults_baseline_se, Toast.LENGTH_SHORT).show());
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
}
