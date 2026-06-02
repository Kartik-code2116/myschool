package com.example.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.myschool.HomeActivity;
import com.example.myschool.R;
import com.example.myschool.SessionContext;
import com.example.myschool.adapter.DeclareWeightageAdapter;
import com.example.myschool.databinding.FragmentDeclareWeightageBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.Subject;
import com.example.myschool.repository.FirebaseRepository;
import java.util.ArrayList;
import java.util.List;

public class DeclareWeightageFragment extends Fragment {

    private FragmentDeclareWeightageBinding b;
    private DeclareWeightageAdapter adapter;
    private ClassModel activeClass;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentDeclareWeightageBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activeClass = SessionContext.selectedClass;

        setupRecyclerView();
        displayHeaderInfo();
        loadActiveSubjects();
        setupActions();
    }

    private void setupRecyclerView() {
        b.rvWeightageSubjects.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeclareWeightageAdapter();
        b.rvWeightageSubjects.setAdapter(adapter);
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        String classVal = "5";
        String divVal = "1";
        if (activeClass != null) {
            classVal = activeClass.className != null ? activeClass.className : "5";
            divVal = activeClass.division != null && !activeClass.division.isEmpty() 
                    ? activeClass.division : "1";
        }
        b.tvWeightageHeaderContext.setText("सन : " + yearLabel + " | इयत्ता: " + classVal + " | तुकडी: " + divVal);
    }

    private void loadActiveSubjects() {
        if (activeClass == null) {
            b.layoutEmptyState.setVisibility(View.VISIBLE);
            b.rvWeightageSubjects.setVisibility(View.GONE);
            b.btnSaveWeightage.setEnabled(false);
            Toast.makeText(getContext(), R.string.msg_empty_8, Toast.LENGTH_LONG).show();
            return;
        }

        List<Subject> activeList = activeClass.subjects;
        if (activeList == null || activeList.isEmpty()) {
            b.layoutEmptyState.setVisibility(View.VISIBLE);
            b.rvWeightageSubjects.setVisibility(View.GONE);
            b.btnSaveWeightage.setEnabled(false);
        } else {
            b.layoutEmptyState.setVisibility(View.GONE);
            b.rvWeightageSubjects.setVisibility(View.VISIBLE);
            b.btnSaveWeightage.setEnabled(true);
            adapter.setData(activeList);
        }
    }

    private void setupActions() {
        // Go to subjects list to select subjects
        b.btnGotoSubjects.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_subjects);
        });

        // Save active weights back to Firestore
        b.btnSaveWeightage.setOnClickListener(v -> saveWeightage());
    }

    private void saveWeightage() {
        if (activeClass == null) return;

        List<Subject> updatedSubjects = adapter.getSubjectsList();
        if (updatedSubjects == null) return;

        // Perform validation to ensure weightage makes sense (at least > 0)
        for (Subject s : updatedSubjects) {
            if (s.maxMarks <= 0) {
                Toast.makeText(getContext(), s.name + " चे पैकी गुण ० पेक्षा जास्त असावेत", Toast.LENGTH_LONG).show();
                return;
            }
        }

        activeClass.subjects = new ArrayList<>(updatedSubjects);

        b.btnSaveWeightage.setEnabled(false);
        FirebaseRepository.get().saveClass(activeClass, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String id) {
                if (getContext() != null) {
                    SessionContext.syncToAppCache();
                    Toast.makeText(getContext(), R.string.msg_empty_9, Toast.LENGTH_SHORT).show();
                    b.btnSaveWeightage.setEnabled(true);
                    loadActiveSubjects();
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    b.btnSaveWeightage.setEnabled(true);
                }
            }
        });
    }

    // ---------- Enforcing screen fullscreen custom Top Bar visibility ----------

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.GONE);
            }
            View bottomNav = ha.findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            
            // Fix CoordinatorLayout scrolling behavior offset bug:
            View navHost = ha.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                        (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost.getLayoutParams();
                params.setBehavior(null);
                params.bottomMargin = 0;
                navHost.setLayoutParams(params);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.VISIBLE);
            }
            View bottomNav = ha.findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
            
            // Restore CoordinatorLayout scrolling behavior and margins:
            View navHost = ha.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                        (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost.getLayoutParams();
                params.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
                float density = getResources().getDisplayMetrics().density;
                params.bottomMargin = (int) (64 * density);
                navHost.setLayoutParams(params);
            }
        }
    }
}
