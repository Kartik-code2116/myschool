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
import com.kartik.myschool.model.School;
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
            if (activeClass.teacherName != null) {
                b.tvTeacherInfoPrimary.setText("Class Teacher: " + activeClass.teacherName);
            }
            if (activeClass.assistantTeacherName != null) {
                b.tvTeacherInfoAsst.setText("Assistant Teacher: " + activeClass.assistantTeacherName);
            } else {
                b.tvTeacherInfoAsst.setText(R.string.msg_assistant_teacher_not_assigned);
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
}
