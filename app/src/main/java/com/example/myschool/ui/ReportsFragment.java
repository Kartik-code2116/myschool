package com.example.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myschool.databinding.FragmentStudentListBinding;
import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.PdfGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reports Fragment - Class-wide reports and analytics
 * Shows marks summary for a selected class and allows bulk PDF generation
 */
public class ReportsFragment extends Fragment {

    private FragmentStudentListBinding b;
    private List<School> schools = new ArrayList<>();
    private List<ClassModel> classes = new ArrayList<>();
    private List<Student> students = new ArrayList<>();
    private List<MarksRecord> marksRecords = new ArrayList<>();
    private School selectedSchool;
    private ClassModel selectedClass;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentStudentListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI();
        loadSchools();
    }

    private void setupUI() {
        b.etSearch.setVisibility(View.GONE);
        b.btnClearSearch.setVisibility(View.GONE);
        b.chipGroupFilter.setVisibility(View.VISIBLE);
        b.fabAddStudent.setVisibility(View.GONE);

        b.chipAll.setText("Select Class to View Report");
        b.chipAll.setOnClickListener(v -> showClassSelector());
    }

    private void loadSchools() {
        FirebaseRepository.get().getSchools(new FirebaseRepository.OnResult<List<School>>() {
            @Override
            public void onSuccess(List<School> list) {
                schools.clear();
                schools.addAll(list);
                if (getActivity() != null && !list.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        selectedSchool = list.get(0);
                        loadClassesForSchool(selectedSchool.id);
                    });
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    private void loadClassesForSchool(String schoolId) {
        FirebaseRepository.get().getClassesForSchool(schoolId, new FirebaseRepository.OnResult<List<ClassModel>>() {
            @Override
            public void onSuccess(List<ClassModel> list) {
                classes.clear();
                classes.addAll(list);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        addClassChips(list);
                        if (!list.isEmpty()) {
                            selectedClass = list.get(0);
                            loadReportForClass(selectedClass.id);
                        }
                    });
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    private void addClassChips(List<ClassModel> classList) {
        int count = b.chipGroupFilter.getChildCount();
        for (int i = count - 1; i > 0; i--) {
            View child = b.chipGroupFilter.getChildAt(i);
            if (child != null) b.chipGroupFilter.removeView(child);
        }
        for (ClassModel c : classList) {
            com.google.android.material.chip.Chip chip =
                    new com.google.android.material.chip.Chip(requireContext());
            chip.setText(c.getDisplayName());
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                selectedClass = c;
                loadReportForClass(c.id);
            });
            b.chipGroupFilter.addView(chip);
        }
    }

    private void showClassSelector() {
        if (classes.isEmpty()) {
            Toast.makeText(requireContext(), "No classes available. Create a class first.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] classNames = new String[classes.size()];
        for (int i = 0; i < classes.size(); i++) classNames[i] = classes.get(i).getDisplayName();
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Class")
                .setItems(classNames, (dialog, which) -> {
                    selectedClass = classes.get(which);
                    loadReportForClass(selectedClass.id);
                })
                .show();
    }

    private void loadReportForClass(String classId) {
        b.emptyState.setVisibility(View.GONE);
        FirebaseRepository.get().getStudentsForClass(classId, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                students.clear();
                students.addAll(list);
                FirebaseRepository.get().getMarksForClass(classId, new FirebaseRepository.OnResult<List<MarksRecord>>() {
                    @Override
                    public void onSuccess(List<MarksRecord> marksList) {
                        marksRecords.clear();
                        marksRecords.addAll(marksList);
                        if (getActivity() != null) getActivity().runOnUiThread(() -> displayReport());
                    }
                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> displayReport());
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> b.emptyState.setVisibility(View.VISIBLE));
                }
            }
        });
    }

    private void displayReport() {
        int totalStudents = students.size();
        int marksEntered = marksRecords.size();
        int passCount = 0, failCount = 0;
        for (MarksRecord m : marksRecords) {
            if ("PASS".equals(m.result)) passCount++; else failCount++;
        }

        if (totalStudents == 0) {
            b.emptyState.setVisibility(View.VISIBLE);
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("Class: ").append(selectedClass != null ? selectedClass.getDisplayName() : "N/A").append("\n");
        report.append("Exam: ").append(selectedClass != null ? selectedClass.examName : "N/A").append("\n\n");
        report.append("Total Students: ").append(totalStudents).append("\n");
        report.append("Marks Entered: ").append(marksEntered).append("\n");
        report.append("Pending: ").append(totalStudents - marksEntered).append("\n\n");
        report.append("Results:\nPass: ").append(passCount)
              .append("\nFail: ").append(failCount)
              .append("\nNot Entered: ").append(totalStudents - marksEntered);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Class Report")
                .setMessage(report.toString())
                .setPositiveButton("Generate All PDFs", (dialog, which) -> generateAllPdfs())
                .setNegativeButton("Close", null)
                .show();
    }

    private void generateAllPdfs() {
        if (selectedSchool == null || selectedClass == null) {
            Toast.makeText(requireContext(), "Select school and class first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (marksRecords.isEmpty()) {
            Toast.makeText(requireContext(), "No marks records to generate PDFs for", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Generating PDFs...", Toast.LENGTH_SHORT).show();

        // Bug #10 fix: use AtomicInteger to avoid race condition when multiple PDF
        // generation threads call back concurrently and increment the counters.
        final int total = marksRecords.size();
        final AtomicInteger generated = new AtomicInteger(0);
        final AtomicInteger failed    = new AtomicInteger(0);

        for (MarksRecord marks : marksRecords) {
            Student matchedStudent = null;
            for (Student s : students) {
                if (s.id.equals(marks.studentId)) { matchedStudent = s; break; }
            }
            if (matchedStudent == null) {
                // No student record found — count as failed and check completion
                if (failed.incrementAndGet() + generated.get() == total) {
                    showGenerationResult(generated.get(), failed.get());
                }
                continue;
            }

            final Student finalStudent = matchedStudent;
            PdfGenerator.generate(requireContext(), selectedSchool, selectedClass, finalStudent, marks,
                    new PdfGenerator.PdfCallback() {
                        @Override
                        public void onSuccess(File pdfFile) {
                            int g = generated.incrementAndGet();
                            int f = failed.get();
                            if (g + f == total) showGenerationResult(g, f);
                        }
                        @Override
                        public void onError(Exception e) {
                            int f = failed.incrementAndGet();
                            int g = generated.get();
                            if (g + f == total) showGenerationResult(g, f);
                        }
                    });
        }
    }

    private void showGenerationResult(int generated, int failed) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(),
                            "Generated: " + generated + " PDFs" +
                            (failed > 0 ? ", Failed: " + failed : ""),
                            Toast.LENGTH_LONG).show());
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
        if (selectedClass != null) loadReportForClass(selectedClass.id);
    }
}
