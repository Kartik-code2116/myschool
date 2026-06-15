package com.kartik.myschool.ui;

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
import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.adapter.DeclareWeightageAdapter;
import com.kartik.myschool.databinding.FragmentDeclareWeightageBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;
import java.util.ArrayList;
import java.util.List;

public class DeclareWeightageFragment extends Fragment {

    private FragmentDeclareWeightageBinding b;
    private DeclareWeightageAdapter adapter;
    private ClassModel activeClass;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        b = FragmentDeclareWeightageBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activeClass = SessionContext.selectedClass;

        setupRecyclerView();
        displayHeaderInfo();

        // Defer load slightly to allow smooth fragment transition, then animate in
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                loadActiveSubjects();
            }
        }, 150);

        setupActions();
    }

    private void setupRecyclerView() {
        b.rvWeightageSubjects.setLayoutManager(new LinearLayoutManager(getContext()));

        // Add layout animation to list items
        if (getContext() != null) {
            android.view.animation.LayoutAnimationController animation = android.view.animation.AnimationUtils
                    .loadLayoutAnimation(getContext(), R.anim.layout_fade_in_slide_up);
            b.rvWeightageSubjects.setLayoutAnimation(animation);
        }

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
                    ? activeClass.division
                    : "-";
        }
        b.tvWeightageHeaderContext.setText("Year: " + yearLabel + " | Cls: " + classVal + "-" + divVal);
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
            b.rvWeightageSubjects.scheduleLayoutAnimation();
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
        if (activeClass == null)
            return;

        List<Subject> updatedSubjects = adapter.getSubjectsList();
        if (updatedSubjects == null)
            return;

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

}
