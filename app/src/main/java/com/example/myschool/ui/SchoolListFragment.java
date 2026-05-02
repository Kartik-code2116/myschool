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
import com.example.myschool.repository.FirebaseRepository;

import java.util.List;

/**
 * School List Fragment - Displays all schools for the teacher
 * and allows adding new schools or managing existing ones.
 */
public class SchoolListFragment extends Fragment {

    private FragmentDashboardBinding b;  // Reuse dashboard layout (has rvRecentSchools)
    private SchoolAdapter schoolAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Create a simple layout programmatically since we need a dedicated school list layout
        // Using the dashboard layout which has the school cards and recycler
        b = FragmentDashboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide the stats cards and quick actions, show only schools section
        b.cardSchools.setVisibility(View.GONE);
        b.cardStudents.setVisibility(View.GONE);

        // Hide quick actions but keep add school
        b.cardAddStudent.setVisibility(View.GONE);
        b.cardEnterMarks.setVisibility(View.GONE);

        // Keep the schools recycler

        setupRecycler();
        setupClickListeners();
        loadSchools();
    }

    private void setupRecycler() {
        schoolAdapter = new SchoolAdapter();
        schoolAdapter.setListener(school -> {
            AppCache.selectedSchool = school;
            showSchoolOptions(school);
        });
        b.rvRecentSchools.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvRecentSchools.setAdapter(schoolAdapter);
    }

    private void setupClickListeners() {
        b.cardAddSchool.setOnClickListener(v -> {
            AppCache.selectedSchool = null;
            startActivity(new Intent(requireContext(), SchoolRegisterActivity.class));
        });
    }

    private void showSchoolOptions(School school) {
        String[] options = {"Add Class", "View Classes", "Add Student", "Edit School"};
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(school.name)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Add Class
                            AppCache.selectedSchool = school;
                            AppCache.selectedClass = null;
                            startActivity(new Intent(requireContext(), ClassSetupActivity.class));
                            break;
                        case 1: // View Classes
                            AppCache.selectedSchool = school;
                            // Navigate to students tab where classes can be seen
                            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
                            if (bottomNav instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                                ((com.google.android.material.bottomnavigation.BottomNavigationView) bottomNav)
                                        .setSelectedItemId(R.id.nav_students);
                            }
                            break;
                        case 2: // Add Student
                            AppCache.selectedSchool = school;
                            AppCache.selectedStudent = null;
                            startActivity(new Intent(requireContext(), StudentRegisterActivity.class));
                            break;
                        case 3: // Edit School
                            AppCache.selectedSchool = school;
                            Intent intent = new Intent(requireContext(), SchoolRegisterActivity.class);
                            intent.putExtra("school_id", school.id);
                            startActivity(intent);
                            break;
                    }
                })
                .show();
    }

    private void loadSchools() {
        FirebaseRepository.get().getSchools(new FirebaseRepository.OnResult<List<School>>() {
            @Override
            public void onSuccess(List<School> list) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        schoolAdapter.setData(list);
                        // Update label
                        if (b != null) {
                            // Could add a text view showing count
                        }
                    });
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSchools();
    }
}
