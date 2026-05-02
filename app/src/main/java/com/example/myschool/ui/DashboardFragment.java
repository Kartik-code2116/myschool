package com.example.myschool.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myschool.AppCache;
import com.example.myschool.ClassSetupActivity;
import com.example.myschool.R;
import com.example.myschool.SchoolRegisterActivity;
import com.example.myschool.StudentRegisterActivity;
import com.example.myschool.adapter.SchoolAdapter;
import com.example.myschool.databinding.FragmentDashboardBinding;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.repository.FirebaseRepository;

import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding b;
    private SchoolAdapter schoolAdapter;
    private int schoolCount = 0;
    private int studentCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentDashboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupSchoolRecycler();
        setupClickListeners();
        loadStats();
        loadSchools();
    }

    private void setupSchoolRecycler() {
        schoolAdapter = new SchoolAdapter();
        schoolAdapter.setListener(school -> {
            AppCache.selectedSchool = school;
            // Navigate to school detail or class setup
            Intent intent = new Intent(requireContext(), ClassSetupActivity.class);
            startActivity(intent);
        });
        b.rvRecentSchools.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvRecentSchools.setAdapter(schoolAdapter);
    }

    private void setupClickListeners() {
        b.cardAddSchool.setOnClickListener(v -> {
            AppCache.selectedSchool = null;
            startActivity(new Intent(requireContext(), SchoolRegisterActivity.class));
        });

        b.cardAddStudent.setOnClickListener(v -> {
            AppCache.selectedStudent = null;
            startActivity(new Intent(requireContext(), StudentRegisterActivity.class));
        });

        b.cardEnterMarks.setOnClickListener(v -> {
            // Navigate to student list to select a student for marks entry
            if (getActivity() instanceof com.example.myschool.HomeActivity) {
                ((com.example.myschool.HomeActivity) getActivity()).setToolbarTitle(getString(R.string.nav_students));
            }
            // Navigate to students tab using bottom nav
            if (b.getRoot().getParent() instanceof View) {
                View bottomNav = requireActivity().findViewById(R.id.bottomNav);
                if (bottomNav instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                    ((com.google.android.material.bottomnavigation.BottomNavigationView) bottomNav).setSelectedItemId(R.id.nav_students);
                }
            }
        });

        b.cardSchools.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.myschool.HomeActivity) {
                ((com.example.myschool.HomeActivity) getActivity()).setToolbarTitle(getString(R.string.nav_schools));
            }
            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
            if (bottomNav instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                ((com.google.android.material.bottomnavigation.BottomNavigationView) bottomNav).setSelectedItemId(R.id.nav_schools);
            }
        });

        b.cardStudents.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.myschool.HomeActivity) {
                ((com.example.myschool.HomeActivity) getActivity()).setToolbarTitle(getString(R.string.nav_students));
            }
            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
            if (bottomNav instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                ((com.google.android.material.bottomnavigation.BottomNavigationView) bottomNav).setSelectedItemId(R.id.nav_students);
            }
        });
    }

    private void loadStats() {
        // Load school count
        FirebaseRepository.get().getSchools(new FirebaseRepository.OnResult<List<School>>() {
            @Override
            public void onSuccess(List<School> list) {
                schoolCount = list.size();
                updateSchoolCount();
            }
            @Override
            public void onError(Exception e) {}
        });

        // Load student count
        FirebaseRepository.get().getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                studentCount = list.size();
                updateStudentCount();
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    private void loadSchools() {
        FirebaseRepository.get().getSchools(new FirebaseRepository.OnResult<List<School>>() {
            @Override
            public void onSuccess(List<School> list) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        schoolAdapter.setData(list);
                    });
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    private void updateSchoolCount() {
        if (b != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> b.tvSchoolCount.setText(String.valueOf(schoolCount)));
        }
    }

    private void updateStudentCount() {
        if (b != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> b.tvStudentCount.setText(String.valueOf(studentCount)));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when coming back
        loadStats();
        loadSchools();
    }
}
