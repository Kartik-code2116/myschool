package com.kartik.myschool.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ImageButton;

import com.kartik.myschool.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.adapter.ReportPrintingAdapter;
import com.kartik.myschool.databinding.FragmentReportPrintingBinding;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.PdfGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ReportPrintingFragment extends Fragment {

    private FragmentReportPrintingBinding b;
    private ReportPrintingAdapter adapter;
    private List<Student> studentsList = new ArrayList<>();
    private android.print.PrintDocumentAdapter mCurrentPrintAdapter;
    private android.app.Dialog mPdfViewerDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentReportPrintingBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());

        setupRecyclerView();
        displayHeaderInfo();
        loadClassStudents();
        validateAndLoadSemesters();
    }


    private void setupRecyclerView() {
        b.rvReportCards.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        adapter = new ReportPrintingAdapter(getContext());
        adapter.setOnItemClickListener(new ReportPrintingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ReportPrintingAdapter.ReportTemplate template, int position) {
                handleReportSelection(position, getString(template.titleResId));
            }

            @Override
            public void onSettingsClick(ReportPrintingAdapter.ReportTemplate template, int position) {
                showReportSettingsDialog(false, position, getString(template.titleResId));
            }
        });
        b.rvReportCards.setAdapter(adapter);
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        b.tvReportPrintingYear.setText("Year: " + yearLabel + " | " + SessionContext.getClassDivSemSubtitle(requireContext()));
        
        b.btnHelpSquare.setOnClickListener(v -> {
            com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(requireContext(), "print_report");
        });
        
        b.btnWatchSquare.setOnClickListener(v -> {
            if (getActivity() != null) {
                com.kartik.myschool.utils.ProductTourHelper.startTour(getActivity(), "print_report");
            }
        });
    }

    private void loadClassStudents() {
        if (SessionContext.selectedClass == null) return;
        FirebaseRepository.get().getStudentsForClass(SessionContext.selectedClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                if (list != null) {
                    studentsList = list;
                    java.util.Collections.sort(studentsList, new java.util.Comparator<Student>() {
                        @Override
                        public int compare(Student s1, Student s2) {
                            int r1 = 0;
                            int r2 = 0;
                            try { r1 = Integer.parseInt(s1.rollNo); } catch (Exception ignored) {}
                            try { r2 = Integer.parseInt(s2.rollNo); } catch (Exception ignored) {}
                            return Integer.compare(r1, r2);
                        }
                    });
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ensureSemestersThen(Runnable action) {
        if (com.kartik.myschool.AppCache.cachedSemesters != null && !com.kartik.myschool.AppCache.cachedSemesters.isEmpty()) {
            action.run();
            return;
        }
        
        String yearId = SessionContext.selectedYear != null ? SessionContext.selectedYear.id : 
                        (SessionContext.selectedClass != null ? SessionContext.selectedClass.yearId : null);
        if (yearId == null) {
            Toast.makeText(getContext(), "Academic year not selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.kartik.myschool.utils.LoadingDialog pd = new com.kartik.myschool.utils.LoadingDialog(requireContext(), null, "सत्र माहिती लोड होत आहे...");
        pd.show();
        
        FirebaseRepository.get().getSemestersForYear(yearId, new FirebaseRepository.OnResult<List<com.kartik.myschool.model.Semester>>() {
            @Override
            public void onSuccess(List<com.kartik.myschool.model.Semester> list) {
                pd.dismiss();
                if (list != null && !list.isEmpty()) {
                    com.kartik.myschool.AppCache.cachedSemesters = list;
                    action.run();
                } else {
                    Toast.makeText(getContext(), "सत्र माहिती सापडली नाही", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(Exception e) {
                pd.dismiss();
                Toast.makeText(getContext(), "लोडिंग त्रुटी: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isFifthOrEighthClass() {
        if (SessionContext.selectedClass == null || SessionContext.selectedClass.className == null) return false;
        String name = SessionContext.selectedClass.className.trim();
        return name.equals("5") || name.equals("8") || 
               name.equalsIgnoreCase("V") || name.equalsIgnoreCase("VIII") || 
               name.equals("५") || name.equals("८");
    }

    private void handleReportSelection(int position, String reportName) {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(getContext(), R.string.msg_empty_10, Toast.LENGTH_LONG).show();
            return;
        }

        if (position == 11 || position == 12) {
            if (!isFifthOrEighthClass()) {
                Toast.makeText(getContext(), "हा रिपोर्ट फक्त इयत्ता ५ वी किंवा ८ वी साठी उपलब्ध आहे.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        ensureSemestersThen(() -> {
            // Class-level (roster) reports — no student selection needed
            // Positions 0: Cover page, 1: Index, 3: Descriptive Remarks, 4,5,7,8,9,10,12,14: Class-wide progress and roster charts
            boolean isClassReport = (position == 0 || position == 1 || position == 3 || position == 4 || position == 5 || position == 6 || position == 7 || position == 8 || position == 9 || position == 10 || position == 11 || position == 12 || position == 13 || position == 14 || position == 15 || position == 16 || position == 17);

            if (isClassReport) {
                generateClassRosterReport(position, reportName);
            } else {
                // Default to generating for all students directly
                triggerBulkReportGeneration(position, reportName);
            }
        });
    }

    private void showStudentSelectionDialog(int reportPosition, String reportName) {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), R.string.msg_empty_11, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] studentNames = new String[studentsList.size()];
        for (int i = 0; i < studentsList.size(); i++) {
            studentNames[i] = studentsList.get(i).rollNo + ". " + studentsList.get(i).name;
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle(R.string.msg_select_student)
                .setItems(studentNames, (dialog, which) -> {
                    Student selectedStudent = studentsList.get(which);
                    generateIndividualReport(selectedStudent, reportPosition, reportName);
                })
                .show();
    }

    private String[] getSemesterIds() {
        String sem1Id = "sem_1";
        String sem2Id = "sem_2";
        if (com.kartik.myschool.AppCache.cachedSemesters != null) {
            for (com.kartik.myschool.model.Semester sem : com.kartik.myschool.AppCache.cachedSemesters) {
                if (sem.number == 1 && sem.id != null && !sem.id.isEmpty()) {
                    sem1Id = sem.id;
                } else if (sem.number == 2 && sem.id != null && !sem.id.isEmpty()) {
                    sem2Id = sem.id;
                }
            }
        }
        if (sem1Id == null || sem1Id.isEmpty()) sem1Id = "sem_1";
        if (sem2Id == null || sem2Id.isEmpty()) sem2Id = "sem_2";
        return new String[] { sem1Id, sem2Id };
    }

    private boolean isSelectedSemesterTwo() {
        return SessionContext.selectedSemester != null && SessionContext.selectedSemester.number == 2;
    }

    private MarksRecord selectedSemesterRecord(MarksRecord sem1, MarksRecord sem2) {
        return isSelectedSemesterTwo() ? sem2 : sem1;
    }

    private Map<String, MarksRecord> selectedSemesterMap(Map<String, MarksRecord> sem1Map, Map<String, MarksRecord> sem2Map) {
        return isSelectedSemesterTwo() ? sem2Map : sem1Map;
    }

    private void generateIndividualReport(Student student, int reportPosition, String reportName) {
        com.kartik.myschool.utils.LoadingDialog pd = new com.kartik.myschool.utils.LoadingDialog(requireContext(), null, student.name + " चा " + reportName + " तयार होत आहे. कृपया प्रतीक्षा करा...");
        pd.show();
        
        String classId = SessionContext.selectedClass.id;
        String[] sids = getSemesterIds();
        FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, sids[0], new FirebaseRepository.OnResult<MarksRecord>() {
            @Override
            public void onSuccess(MarksRecord s1) {
                FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, sids[1], new FirebaseRepository.OnResult<MarksRecord>() {
                    @Override
                    public void onSuccess(MarksRecord s2) {
                        triggerIndividualGenerator(student, s1, s2, reportPosition, reportName, pd);
                    }
                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> {
                            pd.dismiss();
                            Toast.makeText(getContext(), R.string.msg_empty_12, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(getContext(), R.string.msg_empty_13, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void triggerIndividualGenerator(Student student, MarksRecord s1, MarksRecord s2, int reportPosition, String reportName, com.kartik.myschool.utils.LoadingDialog pd) {
        long startTime = System.currentTimeMillis();
        com.kartik.myschool.utils.pdf.DynamicMarginHelper.currentReportIndex = reportPosition;
        PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
            @Override
            public void onSuccess(File pdfFile) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (pd != null) pd.dismiss();
                        long duration = System.currentTimeMillis() - startTime;
                        float sec = duration / 1000f;
                        Toast.makeText(getContext(), getString(R.string.msg_empty_3) + " (" + sec + "s)", Toast.LENGTH_SHORT).show();
                        openPdfFile(pdfFile);
                    });
                }
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (pd != null) pd.dismiss();
                        Toast.makeText(getContext(), "त्रुटी आढळली: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        };

        switch (reportPosition) {
            case 0:  // 1. मुखपृष्ठ (Cover Page)
                com.kartik.myschool.utils.pdf.CoverPageGenerator.generateCoverPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                break;
            case 3:  // 4. वर्णनात्मक नोंदी (Descriptive Remarks)
            case 7:  // 8. वर्णनात्मक नोंदी
                java.util.Map<String, com.kartik.myschool.model.MarksRecord> map1 = s1 != null ? java.util.Collections.singletonMap(student.id, s1) : new java.util.HashMap<>();
                java.util.Map<String, com.kartik.myschool.model.MarksRecord> map2 = s2 != null ? java.util.Collections.singletonMap(student.id, s2) : new java.util.HashMap<>();
                com.kartik.myschool.utils.pdf.DescriptiveRemarksGenerator.generateDescriptive(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), map1, map2, cb);
                break;
            case 11: // 12. Annual Marksheet
                java.util.Map<String, com.kartik.myschool.model.MarksRecord> map2For11 = s2 != null ? java.util.Collections.singletonMap(student.id, s2) : new java.util.HashMap<>();
                com.kartik.myschool.utils.pdf.AnnualMarksheetGenerator.generateAnnualMarksheet(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), map2For11, cb);
                break;
            case 12: // 13. Result Sheet
                java.util.Map<String, com.kartik.myschool.model.MarksRecord> map2For12 = s2 != null ? java.util.Collections.singletonMap(student.id, s2) : new java.util.HashMap<>();
                com.kartik.myschool.utils.pdf.ResultSheetGenerator.generateResultSheet(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), map2For12, cb);
                break;
            case 13: // 14. Gunapattrak (Progress Card Inner)
                com.kartik.myschool.utils.pdf.ProgressCardPortraitGenerator.generateProgressCardPortrait(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), cb);
                break;
            case 1:  // 2. Index
                PdfGenerator.generatePersonalityRecord(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                break;
            case 2:  // 3. Marks Register
                com.kartik.myschool.utils.pdf.MarksRegisterGenerator.generateSingleStudentRegister(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                break;
            case 8:  // 9. Progress Card Cover
                com.kartik.myschool.utils.pdf.ProgressCardCoverGenerator.generateProgressCardCover(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), java.util.Collections.singletonMap(student.id, s1), java.util.Collections.singletonMap(student.id, s2), cb);
                break;
            case 9:  // 10. Progress Card Inner
                com.kartik.myschool.utils.pdf.BothSemDescriptiveGenerator.generateBothSemDescriptive(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), java.util.Collections.singletonMap(student.id, s1), java.util.Collections.singletonMap(student.id, s2), cb);
                break;
            case 17: // 18. Progress Card First Sem
                com.kartik.myschool.utils.pdf.ProgressCardFirstSemGenerator.generateProgressCardFirstSem(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), java.util.Collections.singletonMap(student.id, s1), cb);
                break;
            case 18: // HPC
                com.kartik.myschool.utils.pdf.HPCGenerator.generateHPC(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                break;
            default: // Fallback to personality record for any other individual report
                PdfGenerator.generatePersonalityRecord(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                break;
        }
    }

    private void triggerBulkReportGeneration(int reportPosition, String reportName) {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), R.string.msg_empty_14, Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.kartik.myschool.utils.pdf.DynamicMarginHelper.currentReportIndex = reportPosition;
        com.kartik.myschool.utils.LoadingDialog pd = new com.kartik.myschool.utils.LoadingDialog(requireContext(), null, reportName + " तयार होत आहे. कृपया प्रतीक्षा करा...");
        pd.show();
        
        String classId = SessionContext.selectedClass.id;
        String[] sids = getSemesterIds();
        FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[0], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
            @Override
            public void onSuccess(Map<String, MarksRecord> sem1Map) {
                FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[1], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                    @Override
                    public void onSuccess(Map<String, MarksRecord> sem2Map) {
                        generateBulkPdfs(sem1Map, sem2Map, reportPosition, pd);
                    }
                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> {
                            pd.dismiss();
                            Toast.makeText(getContext(), R.string.msg_empty_16, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(getContext(), R.string.msg_empty_17, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void generateBulkPdfs(Map<String, MarksRecord> sem1Map, Map<String, MarksRecord> sem2Map, int reportPosition, com.kartik.myschool.utils.LoadingDialog pd) {
        long startTime = System.currentTimeMillis();
        PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
            @Override public void onSuccess(File pdfFile) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    if (pd != null) pd.dismiss();
                    long duration = System.currentTimeMillis() - startTime;
                    float sec = duration / 1000f;
                    Toast.makeText(getContext(), getString(R.string.msg_empty_18) + " (" + sec + "s)", Toast.LENGTH_LONG).show();
                    openPdfFile(pdfFile);
                });
            }
            @Override public void onError(Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    if (pd != null) pd.dismiss();
                    Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        };
        PdfGenerator.generateBulkCombinedPdf(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, sem2Map, reportPosition, cb);
    }

    private File generateReportSync(Student student, MarksRecord s1, MarksRecord s2, int reportPosition) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final File[] result = new File[1];
        final Exception[] err = new Exception[1];
        
        PdfGenerator.PdfCallback callback = new PdfGenerator.PdfCallback() {
            @Override public void onSuccess(File pdfFile) { result[0] = pdfFile; latch.countDown(); }
            @Override public void onError(Exception e)   { err[0] = e;           latch.countDown(); }
        };
        
        switch (reportPosition) {
            case 0:  com.kartik.myschool.utils.pdf.CoverPageGenerator.generateCoverPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback); break;
            case 11:
                java.util.Map<String, com.kartik.myschool.model.MarksRecord> m2For11 = s2 != null ? java.util.Collections.singletonMap(student.id, s2) : new java.util.HashMap<>();
                com.kartik.myschool.utils.pdf.AnnualMarksheetGenerator.generateAnnualMarksheet(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), m2For11, callback); 
                break;
            case 12:
                java.util.Map<String, com.kartik.myschool.model.MarksRecord> m2For12 = s2 != null ? java.util.Collections.singletonMap(student.id, s2) : new java.util.HashMap<>();
                com.kartik.myschool.utils.pdf.ResultSheetGenerator.generateResultSheet(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), m2For12, callback); 
                break;
            case 13:
                com.kartik.myschool.utils.pdf.ProgressCardPortraitGenerator.generateProgressCardPortrait(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), callback);
                break;
            case 1:
                PdfGenerator.generatePersonalityRecord(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback); break;
            case 2:
                com.kartik.myschool.utils.pdf.MarksRegisterGenerator.generateSingleStudentRegister(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback); break;
            case 8:
                com.kartik.myschool.utils.pdf.ProgressCardCoverGenerator.generateProgressCardCover(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), java.util.Collections.singletonMap(student.id, s1), java.util.Collections.singletonMap(student.id, s2), callback); break;
            case 9:
                com.kartik.myschool.utils.pdf.BothSemDescriptiveGenerator.generateBothSemDescriptive(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), java.util.Collections.singletonMap(student.id, s1), java.util.Collections.singletonMap(student.id, s2), callback); break;
            case 17:
                com.kartik.myschool.utils.pdf.ProgressCardFirstSemGenerator.generateProgressCardFirstSem(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, java.util.Collections.singletonList(student), java.util.Collections.singletonMap(student.id, s1), callback); break;
            default: PdfGenerator.generatePersonalityRecord(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback); break;
        }
        
        latch.await();
        if (err[0] != null) throw err[0];
        return result[0];
    }

    private void generateClassRosterReport(int reportPosition, String reportName) {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), R.string.msg_empty_11, Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.kartik.myschool.utils.pdf.DynamicMarginHelper.currentReportIndex = reportPosition;
        com.kartik.myschool.utils.LoadingDialog pd = new com.kartik.myschool.utils.LoadingDialog(requireContext(), null, reportName + " तयार होत आहे. कृपया प्रतीक्षा करा...");
        pd.show();
        
        if (reportPosition == 0) {
            long startTime = System.currentTimeMillis();
            PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
                @Override public void onSuccess(File pdfFile) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        pd.dismiss();
                        long duration = System.currentTimeMillis() - startTime;
                        float sec = duration / 1000f;
                        Toast.makeText(getContext(), getString(R.string.msg_empty_20) + " (" + sec + "s)", Toast.LENGTH_SHORT).show();
                        openPdfFile(pdfFile);
                    });
                }
                @Override public void onError(Exception e) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        pd.dismiss();
                        Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            };
            com.kartik.myschool.utils.pdf.CoverPageGenerator.generateCoverPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, null, null, null, cb);
            return;
        }

        if (reportPosition == 1) {
            long startTime = System.currentTimeMillis();
            PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
                @Override public void onSuccess(File pdfFile) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        pd.dismiss();
                        long duration = System.currentTimeMillis() - startTime;
                        float sec = duration / 1000f;
                        Toast.makeText(getContext(), getString(R.string.msg_empty_22) + " (" + sec + "s)", Toast.LENGTH_SHORT).show();
                        openPdfFile(pdfFile);
                    });
                }
                @Override public void onError(Exception e) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        pd.dismiss();
                        Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            };
            com.kartik.myschool.utils.pdf.IndexPageGenerator.generateIndexPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, cb);
            return;
        }

        // For descriptive remarks (position 3/7), clear internal marks cache
        // to force a fresh Firestore fetch so the PDF has the latest remarks
        if (reportPosition == 3 || reportPosition == 7) {
            FirebaseRepository.get().clearMarksCache();
        }

        String classId = SessionContext.selectedClass.id;
        String[] sids = getSemesterIds();
        long startTime = System.currentTimeMillis();
        FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[0], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
            @Override
            public void onSuccess(Map<String, MarksRecord> sem1Map) {
                FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[1], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                    @Override
                    public void onSuccess(Map<String, MarksRecord> sem2Map) {
                        PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
                            @Override public void onSuccess(File pdfFile) {
                                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                    pd.dismiss();
                                    long duration = System.currentTimeMillis() - startTime;
                                    float sec = duration / 1000f;
                                    Toast.makeText(getContext(), getString(R.string.msg_empty_24) + " (" + sec + "s)", Toast.LENGTH_SHORT).show();
                                    openPdfFile(pdfFile);
                                });
                            }
                            @Override public void onError(Exception e) {
                                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                    pd.dismiss();
                                    Toast.makeText(getContext(), "तक्ता बनवण्यात त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        };
                        switch (reportPosition) {
                            case 3:
                                com.kartik.myschool.utils.pdf.DescriptiveRemarksGenerator.generateDescriptive(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, sem2Map, cb); break;
                            case 7: {
                                // Option 8 – Marks-Grade Ledger
                                boolean isSem2_7 = SessionContext.selectedSemester != null && SessionContext.selectedSemester.number == 2;
                                Map<String, MarksRecord> ledgerMap = isSem2_7 ? sem2Map : sem1Map;
                                com.kartik.myschool.utils.pdf.MarksGradeLedgerGenerator.generateMarksGradeLedger(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, ledgerMap, isSem2_7, cb);
                                break;
                            }
                            case 8:
                                // Option 9 – Progress Card Cover (one page per student, landscape)
                                com.kartik.myschool.utils.pdf.ProgressCardCoverGenerator.generateProgressCardCover(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, sem1Map, sem2Map, cb);
                                break;
                            case 9:
                                // Option 10 – Both-Semester Descriptive Remarks (landscape, side-by-side)
                                com.kartik.myschool.utils.pdf.BothSemDescriptiveGenerator.generateBothSemDescriptive(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, sem1Map, sem2Map, cb);
                                break;
                            case 4:
                                boolean isSem2_4 = SessionContext.selectedSemester != null && SessionContext.selectedSemester.number == 2;
                                Map<String, MarksRecord> gradeChartMap = isSem2_4 ? sem2Map : sem1Map;
                                PdfGenerator.generateGradeChart(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, gradeChartMap, isSem2_4, cb); break;
                            case 6:
                                // Option 7 – Roster Grade Table (semester-wide boys/girls per grade)
                                boolean isSem2_6 = SessionContext.selectedSemester != null && SessionContext.selectedSemester.number == 2;
                                Map<String, MarksRecord> rosterMap = isSem2_6 ? sem2Map : sem1Map;
                                com.kartik.myschool.utils.pdf.RosterGradeTableGenerator.generateRosterGradeTable(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, rosterMap, isSem2_6, cb);
                                break;
                            case 5:
                            case 14:
                            default:
                                PdfGenerator.generateProgressBook(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, sem2Map, cb); break;
                            case 15: {
                                // Option 16 - Continuous Comprehensive Evaluation
                                com.kartik.myschool.utils.pdf.ProgressBookCombinedGenerator.generateProgressBookCombined(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, sem1Map, sem2Map, cb);
                                break;
                            }
                            case 16: {
                                // Option 17 - Caste Grade Table
                                boolean isSem2_17 = SessionContext.selectedSemester != null && SessionContext.selectedSemester.number == 2;
                                Map<String, MarksRecord> marksMap = isSem2_17 ? sem2Map : sem1Map;
                                com.kartik.myschool.utils.pdf.CasteGradeTableGenerator.generateCasteGradeTable(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, marksMap, isSem2_17, cb);
                                break;
                            }
                            case 13: {
                                // Option 14 – Progress Card Portrait (प्रगती पत्रक)
                                com.kartik.myschool.utils.pdf.ProgressCardPortraitGenerator.generateProgressCardPortrait(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, cb);
                                break;
                            }
                            case 12: {
                                // Option 13 – Result Sheet (निकालपत्रक)
                                com.kartik.myschool.utils.pdf.ResultSheetGenerator.generateResultSheet(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, sem2Map, cb);
                                break;
                            }
                            case 11: {
                                // Option 12 – Annual Marksheet (वार्षिक परीक्षा गुणपत्रक)
                                com.kartik.myschool.utils.pdf.AnnualMarksheetGenerator.generateAnnualMarksheet(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, sem2Map, cb);
                                break;
                            }
                            case 10: {
                                // Option 11 – Subject-wise Marks Register
                                boolean isSem2_10 = SessionContext.selectedSemester != null && SessionContext.selectedSemester.number == 2;
                                Map<String, MarksRecord> regMap = isSem2_10 ? sem2Map : sem1Map;
                                com.kartik.myschool.utils.pdf.MarksRegisterGenerator.generateMarksRegister(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, regMap, isSem2_10, cb);
                                break;
                            }
                            case 17: {
                                // Option 18 – Progress Card First Sem
                                com.kartik.myschool.utils.pdf.ProgressCardFirstSemGenerator.generateProgressCardFirstSem(
                                        getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                        studentsList, sem1Map, cb);
                                break;
                            }
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> {
                            pd.dismiss();
                            Toast.makeText(getContext(), R.string.msg_empty_16, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(getContext(), R.string.msg_empty_17, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void validateAndLoadSemesters() {
        boolean cacheValid = false;
        if (com.kartik.myschool.AppCache.cachedSemesters != null && !com.kartik.myschool.AppCache.cachedSemesters.isEmpty()) {
            cacheValid = true;
            for (com.kartik.myschool.model.Semester sem : com.kartik.myschool.AppCache.cachedSemesters) {
                if (sem.id == null || sem.id.isEmpty()) {
                    cacheValid = false;
                    break;
                }
            }
        }
        if (!cacheValid) {
            String yearId = SessionContext.selectedYear != null ? SessionContext.selectedYear.id : 
                            (SessionContext.selectedClass != null ? SessionContext.selectedClass.yearId : null);
            if (yearId != null) {
                FirebaseRepository.clearCache();
                FirebaseRepository.get().getSemestersForYear(yearId, new FirebaseRepository.OnResult<List<com.kartik.myschool.model.Semester>>() {
                    @Override
                    public void onSuccess(List<com.kartik.myschool.model.Semester> list) {
                        if (list != null) {
                            com.kartik.myschool.AppCache.cachedSemesters = list;
                        }
                    }
                    @Override public void onError(Exception e) {}
                });
            }
        }
    }

    private void generateAndShowCollectivePdf() {
        ensureSemestersThen(() -> generateMasterReportPdf());
    }

    private void generateMasterReportPdf() {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), R.string.msg_empty_11, Toast.LENGTH_SHORT).show();
            return;
        }

        com.kartik.myschool.utils.pdf.DynamicMarginHelper.currentReportIndex = -1; // Use global margins for master report
        com.kartik.myschool.utils.LoadingDialog pd = new com.kartik.myschool.utils.LoadingDialog(requireContext(), null, "सर्व 18 रिपोर्ट तयार होत आहेत. कृपया प्रतीक्षा करा (यास काही मिनिटे लागू शकतात)...");
        pd.show();

        String classId = SessionContext.selectedClass.id;
        String[] sids = getSemesterIds();
        FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[0], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
            @Override
            public void onSuccess(Map<String, MarksRecord> sem1Map) {
                FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[1], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                    @Override
                    public void onSuccess(Map<String, MarksRecord> sem2Map) {
                        new Thread(() -> {
                            try {
                                long startTime = System.currentTimeMillis();
                                List<File> allFiles = new ArrayList<>();
                                // Generate all 18 reports
                                for (int i = 0; i <= 17; i++) {
                                    File f = generateReportPositionSync(i, sem1Map, sem2Map);
                                    if (f != null && f.exists()) allFiles.add(f);
                                }
                                
                                File masterOut = new File(getContext().getCacheDir(), "Master_Report_Class_" + System.currentTimeMillis() + ".pdf");
                                com.kartik.myschool.utils.PdfMerger.mergePdfFiles(allFiles, masterOut);
                                
                                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                    pd.dismiss();
                                    long duration = System.currentTimeMillis() - startTime;
                                    float sec = duration / 1000f;
                                    Toast.makeText(getContext(), getString(R.string.msg_empty_25) + " (" + sec + "s)", Toast.LENGTH_LONG).show();
                                    openPdfFile(masterOut);
                                });
                            } catch (Exception e) {
                                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                    pd.dismiss();
                                    Toast.makeText(getContext(), "मास्टर रिपोर्ट तयार करण्यात त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        }).start();
                    }
                    @Override public void onError(Exception e) { pd.dismiss(); }
                });
            }
            @Override public void onError(Exception e) { pd.dismiss(); }
        });
    }

    private File generateReportPositionSync(int position, Map<String, MarksRecord> sem1Map, Map<String, MarksRecord> sem2Map) throws Exception {
        if ((position == 11 || position == 12) && !isFifthOrEighthClass()) {
            return null;
        }
        com.kartik.myschool.utils.pdf.DynamicMarginHelper.currentReportIndex = position;
        CountDownLatch latch = new CountDownLatch(1);
        final File[] result = new File[1];
        PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
            @Override public void onSuccess(File pdfFile) { result[0] = pdfFile; latch.countDown(); }
            @Override public void onError(Exception e) { latch.countDown(); }
        };

        boolean isClassReport = (position == 0 || position == 1 || position == 4 || position == 5 || position == 6
                || position == 7 || position == 10 || position == 11 || position == 12 || position == 13 || position == 14 || position == 15 || position == 16);

        if (position == 0) { // Cover page
            com.kartik.myschool.utils.pdf.CoverPageGenerator.generateCoverPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, null, null, null, cb);
        } else if (position == 1) { // Index
            com.kartik.myschool.utils.pdf.IndexPageGenerator.generateIndexPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, cb);
        } else if (position == 4) {
            PdfGenerator.generateGradeChart(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, false, cb);
        } else if (position == 6) {
            // Option 7 – Roster Grade Table (Semester 2 for Master Report)
            com.kartik.myschool.utils.pdf.RosterGradeTableGenerator.generateRosterGradeTable(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem2Map, true, cb);
        } else if (position == 7) {
            // Option 8 – Marks-Grade Ledger (Semester 1 for Master Report)
            com.kartik.myschool.utils.pdf.MarksGradeLedgerGenerator.generateMarksGradeLedger(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem1Map, false, cb);
        } else if (position == 8) {
            // Option 9 – Progress Card Cover
            com.kartik.myschool.utils.pdf.ProgressCardCoverGenerator.generateProgressCardCover(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem1Map, sem2Map, cb);
        } else if (position == 9) {
            // Option 10 – Both-Semester Descriptive Remarks
            com.kartik.myschool.utils.pdf.BothSemDescriptiveGenerator.generateBothSemDescriptive(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem1Map, sem2Map, cb);
        } else if (position == 5 || position == 14) {
            PdfGenerator.generateProgressBook(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, sem2Map, cb);
        } else if (position == 10) {
            // Option 11 – Subject-wise Marks Register (Sem 1 for Master Report)
            com.kartik.myschool.utils.pdf.MarksRegisterGenerator.generateMarksRegister(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem1Map, false, cb);
        } else if (position == 11) {
            // Option 12 – Annual Marksheet (Sem 2 marks usually)
            com.kartik.myschool.utils.pdf.AnnualMarksheetGenerator.generateAnnualMarksheet(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem2Map, cb);
        } else if (position == 12) {
            // Option 13 – Result Sheet
            com.kartik.myschool.utils.pdf.ResultSheetGenerator.generateResultSheet(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem2Map, cb);
        } else if (position == 13) {
            // Option 14 – Progress Card Portrait
            com.kartik.myschool.utils.pdf.ProgressCardPortraitGenerator.generateProgressCardPortrait(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, cb);
        } else if (position == 15) {
            // Option 16 - Continuous Comprehensive Evaluation
            com.kartik.myschool.utils.pdf.ProgressBookCombinedGenerator.generateProgressBookCombined(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem1Map, sem2Map, cb);
        } else if (position == 16) {
            // Option 17 - Caste Grade Table
            com.kartik.myschool.utils.pdf.CasteGradeTableGenerator.generateCasteGradeTable(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem2Map, true, cb);
        } else if (position == 17) {
            // Option 18 - Progress Card First Sem
            com.kartik.myschool.utils.pdf.ProgressCardFirstSemGenerator.generateProgressCardFirstSem(
                    getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                    studentsList, sem1Map, cb);
        } else {
            PdfGenerator.generateBulkCombinedPdf(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, sem2Map, position, cb);
        }
        latch.await();
        return result[0];
    }

    private void openPdfFile(File file) {
        showInAppPdfViewer(file);
        if (getActivity() != null) {
            com.kartik.myschool.utils.AnalyticsHelper.logPdfGenerated("report_printing");
            com.kartik.myschool.utils.ReviewHelper.incrementPdfCountAndCheck(getActivity());
        }
    }

    private void showInAppPdfViewer(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists() || getContext() == null) return;
        
        try {
            android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(
                    android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            );
            
            int totalPages = renderer.getPageCount();
            final int[] currentPage = {0};
            
            android.app.Dialog dialog = new android.app.Dialog(getContext(), android.R.style.Theme_Light_NoTitleBar_Fullscreen);
            mPdfViewerDialog = dialog;
            
            android.view.View root = android.view.LayoutInflater.from(getContext()).inflate(R.layout.dialog_pdf_viewer, null);
            
            android.widget.ImageButton btnBack = root.findViewById(R.id.btnBack);
            android.widget.ImageButton btnToggleView = root.findViewById(R.id.btnToggleView);
            android.widget.ImageView ivPdfPage = root.findViewById(R.id.ivPdfPage);
            androidx.recyclerview.widget.RecyclerView rvPdfPages = root.findViewById(R.id.rvPdfPages);
            android.widget.TextView tvPageIndicator = root.findViewById(R.id.tvPageIndicator);
            com.google.android.material.button.MaterialButton btnPrev = root.findViewById(R.id.btnPrev);
            com.google.android.material.button.MaterialButton btnNext = root.findViewById(R.id.btnNext);
            com.google.android.material.button.MaterialButton btnDownload = root.findViewById(R.id.btnDownload);
            
            rvPdfPages.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
            PdfPageAdapter adapter = new PdfPageAdapter(renderer, totalPages);
            rvPdfPages.setAdapter(adapter);

            final boolean[] isScrollMode = {true}; // START IN SCROLL MODE (User requested this to be default)

            // Apply initial visibility state
            btnToggleView.setImageResource(R.drawable.ic_document);
            ivPdfPage.setVisibility(android.view.View.GONE);
            tvPageIndicator.setVisibility(android.view.View.GONE);
            btnPrev.setVisibility(android.view.View.GONE);
            btnNext.setVisibility(android.view.View.GONE);
            rvPdfPages.setVisibility(android.view.View.VISIBLE);

            btnToggleView.setOnClickListener(v -> {
                isScrollMode[0] = !isScrollMode[0];
                com.kartik.myschool.utils.zoom.ZoomLayout zoomLayout = root.findViewById(R.id.zoomLayout);
                if (zoomLayout != null) {
                    zoomLayout.zoomTo(1.0f, 0f, 0f, false);
                }
                if (isScrollMode[0]) {
                    btnToggleView.setImageResource(R.drawable.ic_document);
                    ivPdfPage.setVisibility(android.view.View.GONE);
                    tvPageIndicator.setVisibility(android.view.View.GONE);
                    btnPrev.setVisibility(android.view.View.GONE);
                    btnNext.setVisibility(android.view.View.GONE);
                    rvPdfPages.setVisibility(android.view.View.VISIBLE);
                } else {
                    btnToggleView.setImageResource(R.drawable.ic_list);
                    rvPdfPages.setVisibility(android.view.View.GONE);
                    ivPdfPage.setVisibility(android.view.View.VISIBLE);
                    tvPageIndicator.setVisibility(android.view.View.VISIBLE);
                    btnPrev.setVisibility(android.view.View.VISIBLE);
                    btnNext.setVisibility(android.view.View.VISIBLE);
                }
            });
            
            btnBack.setOnClickListener(v -> {
                renderer.close();
                dialog.dismiss();
            });
            
            btnDownload.setOnClickListener(v -> {
                int totalPagesCount = renderer.getPageCount();
                android.content.SharedPreferences prefs = getContext().getSharedPreferences("myschool_settings_prefs", android.content.Context.MODE_PRIVATE);
                String lang = prefs.getString("language", "mr");
                boolean isEn = "en".equals(lang);

                android.content.Context dialogContext = dialog.getContext();
                com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
                        new com.google.android.material.bottomsheet.BottomSheetDialog(dialogContext);
                android.view.View sheetView = android.view.LayoutInflater.from(dialogContext).inflate(R.layout.dialog_print_options, null);
                
                // Localization text binding
                android.widget.TextView tvPrintTitle = sheetView.findViewById(R.id.tvPrintTitle);
                tvPrintTitle.setText(isEn ? "Print Options" : "प्रिंट पर्याय");

                android.widget.TextView tvPrintAllTitle = sheetView.findViewById(R.id.tvPrintAllTitle);
                tvPrintAllTitle.setText(isEn ? "Print All Pages" : "सर्व पाने प्रिंट करा");
                android.widget.TextView tvPrintAllDesc = sheetView.findViewById(R.id.tvPrintAllDesc);
                tvPrintAllDesc.setText(isEn ? "Directly spools all pages to the printer." : "सर्व पाने थेट प्रिंटरला पाठवते.");

                android.widget.TextView tvPrintBunch5Title = sheetView.findViewById(R.id.tvPrintBunch5Title);
                tvPrintBunch5Title.setText(isEn ? "Print in sets of 5 (5-5 Bunch)" : "५-५ च्या सेटमध्ये प्रिंट करा (5-5 Bunch)");
                android.widget.TextView tvPrintBunch5Desc = sheetView.findViewById(R.id.tvPrintBunch5Desc);
                tvPrintBunch5Desc.setText(isEn ? 
                        "Displays a secondary dialog listing ranges in sets of 5 pages (e.g. Pages 1 - 5, Pages 6 - 10)." : 
                        "५-५ पानांच्या श्रेणींची यादी दर्शवणारे दुय्यम डायलॉग उघडतो (उदा. पाने १ - ५, पाने ६ - १०).");

                android.widget.TextView tvPrintBunch10Title = sheetView.findViewById(R.id.tvPrintBunch10Title);
                tvPrintBunch10Title.setText(isEn ? "Print in sets of 10 (10-10 Bunch)" : "१०-१० च्या सेटमध्ये प्रिंट करा (10-10 Bunch)");
                android.widget.TextView tvPrintBunch10Desc = sheetView.findViewById(R.id.tvPrintBunch10Desc);
                tvPrintBunch10Desc.setText(isEn ? 
                        "Displays a secondary dialog listing ranges in sets of 10 pages (e.g. Pages 1 - 10, Pages 11 - 20)." : 
                        "१०-१० पानांच्या श्रेणींची यादी दर्शवणारे दुय्यम डायलॉग उघडतो (उदा. पाने १ - १०, पाने ११ - २०).");

                android.widget.TextView tvPrintCustomTitle = sheetView.findViewById(R.id.tvPrintCustomTitle);
                tvPrintCustomTitle.setText(isEn ? "Custom Page Range" : "सानुकूल पान श्रेणी (Custom Range)");
                android.widget.TextView tvPrintCustomDesc = sheetView.findViewById(R.id.tvPrintCustomDesc);
                tvPrintCustomDesc.setText(isEn ? 
                        "Prompts start and end page fields to enter a custom range of your choosing." : 
                        "तुमच्या आवडीची सानुकूल श्रेणी प्रविष्ट करण्यासाठी सुरुवातीचे व शेवटचे पान विचारते.");

                // Set up click listeners
                sheetView.findViewById(R.id.cardPrintAll).setOnClickListener(view -> {
                    bottomSheet.dismiss();
                    printPdfFile(pdfFile);
                });
                sheetView.findViewById(R.id.cardPrintBunch5).setOnClickListener(view -> {
                    bottomSheet.dismiss();
                    showBunchDialog(pdfFile, totalPagesCount, 5, isEn);
                });
                sheetView.findViewById(R.id.cardPrintBunch10).setOnClickListener(view -> {
                    bottomSheet.dismiss();
                    showBunchDialog(pdfFile, totalPagesCount, 10, isEn);
                });
                sheetView.findViewById(R.id.cardPrintCustom).setOnClickListener(view -> {
                    bottomSheet.dismiss();
                    showCustomRangeDialog(pdfFile, totalPagesCount, isEn);
                });

                bottomSheet.setContentView(sheetView);
                bottomSheet.show();
            });
            
            Runnable renderPage = () -> {
                try {
                    synchronized (renderer) {
                        android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(currentPage[0]);
                        int w = page.getWidth() * 3; // Render at 3x scale for zooming clarity
                        int h = page.getHeight() * 3;
                        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
                        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        
                        ivPdfPage.setImageBitmap(bmp);
                        tvPageIndicator.setText("पृष्ठ: " + (currentPage[0] + 1) + " / " + totalPages);
                        btnPrev.setEnabled(currentPage[0] > 0);
                        btnNext.setEnabled(currentPage[0] < totalPages - 1);
                        page.close();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Render page failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };
            
            btnPrev.setOnClickListener(v -> {
                if (currentPage[0] > 0) {
                    currentPage[0]--;
                    com.kartik.myschool.utils.zoom.ZoomLayout zoomLayout = root.findViewById(R.id.zoomLayout);
                    if (zoomLayout != null) {
                        zoomLayout.zoomTo(1.0f, 0f, 0f, false);
                    }
                    renderPage.run();
                }
            });
            btnNext.setOnClickListener(v -> {
                if (currentPage[0] < totalPages - 1) {
                    currentPage[0]++;
                    com.kartik.myschool.utils.zoom.ZoomLayout zoomLayout = root.findViewById(R.id.zoomLayout);
                    if (zoomLayout != null) {
                        zoomLayout.zoomTo(1.0f, 0f, 0f, false);
                    }
                    renderPage.run();
                }
            });
            
            renderPage.run();
            
            dialog.setContentView(root);
            dialog.setOnDismissListener(d -> {
                try { renderer.close(); } catch (Exception ignored) {}
            });
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "PDF उघडण्यात त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void downloadOrSharePdfFile(File file) {
        if (file == null || !file.exists()) return;
        try {
            java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            java.io.File destFile = new java.io.File(downloadsDir, file.getName());
            
            java.io.FileInputStream in = new java.io.FileInputStream(file);
            java.io.FileOutputStream out = new java.io.FileOutputStream(destFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            
            Toast.makeText(getContext(), "PDF Downloads फोल्डरमध्ये सेव्ह केले!", Toast.LENGTH_LONG).show();
            
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    getContext(),
                    getContext().getPackageName() + ".fileprovider",
                    destFile
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            sharePdfFile(file);
        }
    }

    private void sharePdfFile(File file) {
        if (file == null || !file.exists()) return;
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    getContext(),
                    getContext().getPackageName() + ".fileprovider",
                    file
            );
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share Report PDF"));
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.msg_pdf, Toast.LENGTH_LONG).show();
        }
    }
    // ═══════════════════════════════════════════════════════════════════
    //  PRINTING LOGIC — Uses Intent + FileProvider for maximum reliability.
    //  Every Android device has a PDF viewer (Google Drive PDF Viewer, etc.)
    //  that includes a native print button in its toolbar.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Dismiss the PDF viewer dialog (if showing) and then launch the system
     * print dialog after a short delay so the Activity window is fully
     * foregrounded. PrintManager.print() requires an Activity context and
     * the Activity must be the topmost window.
     */
    private void printPdfFile(File file) {
        if (file == null || !file.exists()) {
            if (getContext() != null) Toast.makeText(getContext(), "फाईल अस्तित्वात नाही.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dismiss the fullscreen PDF viewer so the Activity becomes the top window.
        if (mPdfViewerDialog != null && mPdfViewerDialog.isShowing()) {
            mPdfViewerDialog.dismiss();
            mPdfViewerDialog = null;
        }

        // Post with a delay so the dialog window is fully torn down before
        // we ask the PrintManager to show its UI.
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // Guard against detached Fragment / destroyed Activity during the delay
            if (!isAdded()) return;
            android.app.Activity activity = getActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

            android.print.PrintManager printManager = (android.print.PrintManager)
                    activity.getSystemService(android.content.Context.PRINT_SERVICE);
            if (printManager == null) {
                Toast.makeText(activity, "Print service not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // [CRITICAL BUG FIX]: Android ContextWrappers (used for localization) break PrintManager
            // because PrintManager verifies that its context `instanceof Activity`.
            // We use reflection to inject the raw Activity context back into PrintManager.
            try {
                java.lang.reflect.Field contextField = printManager.getClass().getDeclaredField("mContext");
                contextField.setAccessible(true);
                contextField.set(printManager, activity);
            } catch (Exception ignored) {
                // Ignore reflection errors; if it fails, it will crash natively like before,
                // but this works on the vast majority of Android versions.
            }

            // Calculate page count for the print preview
            int pageCount = android.print.PrintDocumentInfo.PAGE_COUNT_UNKNOWN;
            try {
                android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(
                        android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY));
                pageCount = renderer.getPageCount();
                renderer.close();
            } catch (Exception e) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            }
            final int finalPageCount = pageCount;
            String jobName = activity.getString(R.string.app_name) + " Document";

            // Keep a strong reference so the adapter is not garbage-collected
            mCurrentPrintAdapter = new android.print.PrintDocumentAdapter() {
                @Override
                public void onLayout(android.print.PrintAttributes oldAttributes,
                                     android.print.PrintAttributes newAttributes,
                                     android.os.CancellationSignal cancellationSignal,
                                     LayoutResultCallback callback, Bundle metadata) {
                    if (cancellationSignal.isCanceled()) { callback.onLayoutCancelled(); return; }
                    android.print.PrintDocumentInfo pdi = new android.print.PrintDocumentInfo.Builder(file.getName())
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(finalPageCount)
                            .build();
                    callback.onLayoutFinished(pdi, !newAttributes.equals(oldAttributes));
                }

                @Override
                public void onWrite(android.print.PageRange[] pages,
                                    android.os.ParcelFileDescriptor destination,
                                    android.os.CancellationSignal cancellationSignal,
                                    WriteResultCallback callback) {
                    if (cancellationSignal.isCanceled()) { callback.onWriteCancelled(); return; }
                    java.io.InputStream input = null;
                    java.io.OutputStream output = null;
                    try {
                        input = new java.io.FileInputStream(file);
                        output = new java.io.FileOutputStream(destination.getFileDescriptor());
                        byte[] buf = new byte[16384];
                        int bytesRead;
                        while ((bytesRead = input.read(buf)) > 0) {
                            if (cancellationSignal.isCanceled()) { callback.onWriteCancelled(); return; }
                            output.write(buf, 0, bytesRead);
                        }
                        callback.onWriteFinished(new android.print.PageRange[]{android.print.PageRange.ALL_PAGES});
                    } catch (Exception e) {
                        callback.onWriteFailed(e.toString());
                    } finally {
                        try { if (input != null) input.close(); } catch (Exception ignored) {}
                        try { if (output != null) output.close(); } catch (Exception ignored) {}
                    }
                }

                @Override
                public void onFinish() {
                    mCurrentPrintAdapter = null;
                    super.onFinish();
                }
            };

            printManager.print(jobName, mCurrentPrintAdapter, null);
        }, 350); // 350ms is enough for dialog window teardown
    }

    /**
     * Extracts a range of pages from a PDF on a background thread,
     * then opens the result for printing.
     */
    private void extractAndPrintPdfPages(File sourceFile, int startPage, int endPage) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        // Build a simple progress dialog using AlertDialog (non-deprecated)
        android.app.AlertDialog progressDialog = new android.app.AlertDialog.Builder(ctx)
                .setMessage("पाने वेगळे करत आहे...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            File tempFile = null;
            Exception error = null;
            try {
                tempFile = new File(ctx.getCacheDir(),
                        "print_pages_" + startPage + "_" + endPage + "_" + System.currentTimeMillis() + ".pdf");

                com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                com.itextpdf.text.pdf.PdfCopy copy = new com.itextpdf.text.pdf.PdfCopy(
                        document, new java.io.FileOutputStream(tempFile));
                document.open();

                com.itextpdf.text.pdf.PdfReader reader =
                        new com.itextpdf.text.pdf.PdfReader(sourceFile.getAbsolutePath());
                for (int i = startPage; i <= endPage; i++) {
                    copy.addPage(copy.getImportedPage(reader, i));
                }
                copy.freeReader(reader);
                reader.close();
                document.close();
            } catch (Exception e) {
                error = e;
            }

            final File resultFile = tempFile;
            final Exception finalError = error;

            // Return to main thread
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                try { progressDialog.dismiss(); } catch (Exception ignored) {}

                if (finalError != null || resultFile == null || !resultFile.exists()) {
                    if (getContext() != null)
                        Toast.makeText(getContext(),
                                "पान वेगळे करण्यात त्रुटी: " +
                                (finalError != null ? finalError.getMessage() : "unknown"),
                                Toast.LENGTH_SHORT).show();
                    return;
                }
                printPdfFile(resultFile);
            });
        }).start();
    }

    /**
     * Shows a dialog listing page ranges in sets of {@code pageSize}.
     * Selecting a set extracts those pages and opens them for printing.
     */
    private void showBunchDialog(File pdfFile, int totalPages, int pageSize, boolean isEn) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        List<String> options = new ArrayList<>();
        List<int[]> ranges = new ArrayList<>();

        int start = 1;
        while (start <= totalPages) {
            int end = Math.min(start + pageSize - 1, totalPages);
            options.add(isEn ? "Pages " + start + " - " + end : "पाने " + start + " ते " + end);
            ranges.add(new int[]{start, end});
            start += pageSize;
        }

        new android.app.AlertDialog.Builder(ctx)
                .setTitle(isEn ? "Select Page Set (" + pageSize + " pages)" : "पानांचा सेट निवडा (" + pageSize + " पाने)")
                .setItems(options.toArray(new String[0]), (dialogInterface, index) -> {
                    int[] range = ranges.get(index);
                    dialogInterface.dismiss();
                    extractAndPrintPdfPages(pdfFile, range[0], range[1]);
                })
                .setNegativeButton(isEn ? "Cancel" : "रद्द करा", null)
                .show();
    }

    /**
     * Shows a dialog prompting for a custom page range,
     * then extracts those pages and opens them for printing.
     */
    private void showCustomRangeDialog(File pdfFile, int totalPages, boolean isEn) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        android.widget.LinearLayout layout = new android.widget.LinearLayout(ctx);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        android.widget.EditText etStart = new android.widget.EditText(ctx);
        etStart.setHint(isEn ? "Start Page (1 to " + totalPages + ")" : "सुरुवातीचे पान (1 ते " + totalPages + ")");
        etStart.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etStart);

        android.widget.EditText etEnd = new android.widget.EditText(ctx);
        etEnd.setHint(isEn ? "End Page (1 to " + totalPages + ")" : "शेवटचे पान (1 ते " + totalPages + ")");
        etEnd.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etEnd);

        new android.app.AlertDialog.Builder(ctx)
                .setTitle(isEn ? "Enter Custom Page Range" : "सानुकूल पान श्रेणी प्रविष्ट करा")
                .setView(layout)
                .setPositiveButton(isEn ? "Print" : "प्रिंट करा", (dialog, which) -> {
                    String startStr = etStart.getText().toString().trim();
                    String endStr = etEnd.getText().toString().trim();
                    if (startStr.isEmpty() || endStr.isEmpty()) {
                        Toast.makeText(ctx, isEn ? "Please enter both page numbers" : "कृपया दोन्ही पानांचे क्रमांक टाका", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int start = Integer.parseInt(startStr);
                        int end = Integer.parseInt(endStr);
                        if (start < 1 || end > totalPages || start > end) {
                            Toast.makeText(ctx, isEn ? "Invalid page range" : "अवैध पान श्रेणी", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        extractAndPrintPdfPages(pdfFile, start, end);
                    } catch (NumberFormatException e) {
                        Toast.makeText(ctx, isEn ? "Please enter valid numbers" : "कृपया वैध क्रमांक टाका", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(isEn ? "Cancel" : "रद्द करा", null)
                .show();
    }

    private void showReportSettingsDialog(boolean isGlobal, int reportIndex, String title) {
        if (getContext() == null) return;
        
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View sheet = LayoutInflater.from(getContext()).inflate(R.layout.dialog_report_settings, null);
        
        android.widget.TextView tvSubtitle = sheet.findViewById(R.id.tvSettingsSubtitle);
        tvSubtitle.setText(title);

        android.content.SharedPreferences prefs = getContext().getSharedPreferences("report_margins", android.content.Context.MODE_PRIVATE);
        String prefix = isGlobal ? "global_" : "report_" + reportIndex + "_";

        // Helper to load int with fallback
        java.util.function.Function<String, Integer> loadInt = (keySuffix) -> {
            int val = prefs.getInt(prefix + keySuffix, -1);
            if (val == -1 && !isGlobal) {
                val = prefs.getInt("global_" + keySuffix, -1); // fallback to global
            }
            return val;
        };

        // UI elements
        com.google.android.material.textfield.TextInputEditText etPageNumber = sheet.findViewById(R.id.etPageNumber);
        com.google.android.material.textfield.TextInputEditText etTopMargin = sheet.findViewById(R.id.etTopMargin);
        com.google.android.material.textfield.TextInputEditText etBottomMargin = sheet.findViewById(R.id.etBottomMargin);
        com.google.android.material.textfield.TextInputEditText etLeftMargin = sheet.findViewById(R.id.etLeftMargin);
        com.google.android.material.textfield.TextInputEditText etRightMargin = sheet.findViewById(R.id.etRightMargin);
        
        com.google.android.material.materialswitch.MaterialSwitch switchShowAttendance = sheet.findViewById(R.id.switchShowAttendance);
        com.google.android.material.materialswitch.MaterialSwitch switchShowReligion = sheet.findViewById(R.id.switchShowReligion);
        com.google.android.material.materialswitch.MaterialSwitch switchShowFormative = sheet.findViewById(R.id.switchShowFormative);

        Runnable loadValuesForPage = () -> {
            String pageStr = etPageNumber.getText().toString().trim();
            if (pageStr.isEmpty()) return;
            
            String pPrefix = "p" + pageStr + "_";
            
            // Default heuristics based on page number (like the old screenshot defaults)
            int defaultTop = 4; int defaultLeft = 9; int defaultRight = 4; int defaultBottom = 4;
            if (pageStr.equals("2")) { defaultTop = 5; defaultLeft = 10; defaultRight = 5; defaultBottom = 5; }
            if (pageStr.equals("3")) { defaultTop = 6; defaultLeft = 11; defaultRight = 6; defaultBottom = 6; }
            
            etTopMargin.setText(String.valueOf(loadInt.apply(pPrefix + "top") != -1 ? loadInt.apply(pPrefix + "top") : defaultTop));
            etLeftMargin.setText(String.valueOf(loadInt.apply(pPrefix + "left") != -1 ? loadInt.apply(pPrefix + "left") : defaultLeft));
            etRightMargin.setText(String.valueOf(loadInt.apply(pPrefix + "right") != -1 ? loadInt.apply(pPrefix + "right") : defaultRight));
            etBottomMargin.setText(String.valueOf(loadInt.apply(pPrefix + "bottom") != -1 ? loadInt.apply(pPrefix + "bottom") : defaultBottom));
            
            // Load toggles (global per report type, not per-page)
            switchShowAttendance.setChecked(prefs.getBoolean(prefix + "show_attendance", true));
            switchShowReligion.setChecked(prefs.getBoolean(prefix + "show_religion", true));
            switchShowFormative.setChecked(prefs.getBoolean(prefix + "show_formative", true));
        };

        // Initial load for Page 1
        loadValuesForPage.run();

        // Listen for page number changes to dynamically load margins
        etPageNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                loadValuesForPage.run();
            }
        });

        // Reset action (clears specifically for the currently typed page)
        sheet.findViewById(R.id.btnResetAll).setOnClickListener(v -> {
            String pageStr = etPageNumber.getText().toString().trim();
            if (pageStr.isEmpty()) return;
            
            int defaultTop = 4; int defaultLeft = 9; int defaultRight = 4; int defaultBottom = 4;
            if (pageStr.equals("2")) { defaultTop = 5; defaultLeft = 10; defaultRight = 5; defaultBottom = 5; }
            if (pageStr.equals("3")) { defaultTop = 6; defaultLeft = 11; defaultRight = 6; defaultBottom = 6; }
            
            etTopMargin.setText(String.valueOf(defaultTop));
            etLeftMargin.setText(String.valueOf(defaultLeft));
            etRightMargin.setText(String.valueOf(defaultRight));
            etBottomMargin.setText(String.valueOf(defaultBottom));
            
            switchShowAttendance.setChecked(true);
            switchShowReligion.setChecked(true);
            switchShowFormative.setChecked(true);
        });

        // Save action
        sheet.findViewById(R.id.btnSaveSetting).setOnClickListener(v -> {
            String pageStr = etPageNumber.getText().toString().trim();
            if (pageStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a valid Page Number", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String pPrefix = "p" + pageStr + "_";
            android.content.SharedPreferences.Editor editor = prefs.edit();
            try {
                editor.putInt(prefix + pPrefix + "top", Integer.parseInt(etTopMargin.getText().toString()));
                editor.putInt(prefix + pPrefix + "left", Integer.parseInt(etLeftMargin.getText().toString()));
                editor.putInt(prefix + pPrefix + "right", Integer.parseInt(etRightMargin.getText().toString()));
                editor.putInt(prefix + pPrefix + "bottom", Integer.parseInt(etBottomMargin.getText().toString()));
                
                editor.putBoolean(prefix + "show_attendance", switchShowAttendance.isChecked());
                editor.putBoolean(prefix + "show_religion", switchShowReligion.isChecked());
                editor.putBoolean(prefix + "show_formative", switchShowFormative.isChecked());
                
                editor.apply();
                Toast.makeText(getContext(), "Settings for Report saved!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setContentView(sheet);
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());
        displayHeaderInfo();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) getActivity();
            activity.showCustomToolbarActions(
                    true,
                    v -> generateAndShowCollectivePdf(),
                    v -> {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        popup.getMenu().add("Select Margins");
                        popup.getMenu().add("Check Remaining Info");
                        popup.setOnMenuItemClickListener(menuItem -> {
                            if (menuItem.getTitle().equals("Select Margins")) {
                                showReportSettingsDialog(true, -1, "All Reports (Global)");
                            } else if (menuItem.getTitle().equals("Check Remaining Info")) {
                                showMissingInfoDialog();
                            }
                            return true;
                        });
                        popup.show();
                    }
            );
            ImageButton btn = activity.findViewById(R.id.btnToolbarNotifications);
            if (btn != null) {
                btn.setImageResource(R.drawable.ic_pdf);
                btn.setContentDescription("निकालपत्रक एकत्रित पहा");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).showCustomToolbarActions(false, null, null);
        }
    }

    private static class PdfPageAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<PdfPageAdapter.PdfViewHolder> {
        private final android.graphics.pdf.PdfRenderer renderer;
        private final int totalPages;

        public PdfPageAdapter(android.graphics.pdf.PdfRenderer renderer, int totalPages) {
            this.renderer = renderer;
            this.totalPages = totalPages;
        }

        @androidx.annotation.NonNull
        @Override
        public PdfViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
            return new PdfViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull PdfViewHolder holder, int position) {
            try {
                synchronized (renderer) {
                    android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(position);
                    int w = page.getWidth() * 2; // Render at 2x scale for scroll mode efficiency
                    int h = page.getHeight() * 2;
                    android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
                    // MUST FILL WITH WHITE FIRST! PdfRenderer does not draw a background,
                    // which causes severe text rendering artifacts when scaled if left transparent.
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                    canvas.drawColor(android.graphics.Color.WHITE);
                    page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    holder.ivPage.setImageBitmap(bmp);
                    page.close();
                }
            } catch (Exception e) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            }
        }

        @Override
        public int getItemCount() {
            return totalPages;
        }

        @Override
        public void onViewRecycled(@androidx.annotation.NonNull PdfViewHolder holder) {
            super.onViewRecycled(holder);
            android.graphics.drawable.Drawable drawable = holder.ivPage.getDrawable();
            if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                android.graphics.Bitmap bmp = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                }
            }
            holder.ivPage.setImageDrawable(null);
        }

        static class PdfViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.ImageView ivPage;
            public PdfViewHolder(@androidx.annotation.NonNull android.view.View itemView) {
                super(itemView);
                ivPage = itemView.findViewById(R.id.ivPage);
            }
        }
    }

    private void showMissingInfoDialog() {
        if (getContext() == null || com.kartik.myschool.SessionContext.selectedClass == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        android.view.View sheet = android.view.LayoutInflater.from(getContext()).inflate(R.layout.dialog_missing_info, null);
        dialog.setContentView(sheet);

        androidx.recyclerview.widget.RecyclerView rv = sheet.findViewById(R.id.rvMissingInfo);
        android.widget.ProgressBar progress = sheet.findViewById(R.id.progressMissingInfo);
        android.widget.ImageButton btnClose = sheet.findViewById(R.id.btnCloseMissingInfo);

        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        com.kartik.myschool.adapter.MissingInfoAdapter adapter = new com.kartik.myschool.adapter.MissingInfoAdapter();
        rv.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        String classId = com.kartik.myschool.SessionContext.selectedClass.id;
        String[] sids = getSemesterIds();
        String semesterId = sids[0]; // Currently just using sem 1 for missing checks, can be expanded
        
        com.kartik.myschool.repository.FirebaseRepository.get().getStudentsForClass(classId, new com.kartik.myschool.repository.FirebaseRepository.OnResult<java.util.List<com.kartik.myschool.model.Student>>() {
            @Override
            public void onSuccess(java.util.List<com.kartik.myschool.model.Student> students) {
                if (!isAdded()) return;

                com.kartik.myschool.repository.FirebaseRepository.get().getMarksForClassAndSemester(classId, semesterId, new com.kartik.myschool.repository.FirebaseRepository.OnResult<java.util.Map<String, com.kartik.myschool.model.MarksRecord>>() {
                    @Override
                    public void onSuccess(java.util.Map<String, com.kartik.myschool.model.MarksRecord> marksMap) {
                        if (!isAdded()) return;

                        java.util.List<com.kartik.myschool.adapter.MissingInfoAdapter.MissingInfoItem> items = new java.util.ArrayList<>();
                        java.util.List<com.kartik.myschool.model.Subject> classSubjects = com.kartik.myschool.SessionContext.selectedClass.subjects;

                        for (com.kartik.myschool.model.Student s : students) {
                            java.util.List<String> missing = new java.util.ArrayList<>();
                            
                            // Check profile
                            if (s.dob == null || s.dob.trim().isEmpty()) missing.add("DOB");
                            if (s.motherName == null || s.motherName.trim().isEmpty()) missing.add("Mother Name");
                            if (s.gender == null || s.gender.trim().isEmpty()) missing.add("Gender");
                            if (s.cast == null || s.cast.trim().isEmpty()) missing.add("Caste");
                            if (s.religion == null || s.religion.trim().isEmpty()) missing.add("Religion");

                            // Check marks & remarks
                            com.kartik.myschool.model.MarksRecord record = marksMap.get(s.id);
                            
                            if (classSubjects != null) {
                                // 1. Check Marks for all academic subjects
                                for (com.kartik.myschool.model.Subject sub : classSubjects) {
                                    boolean hasDetails = record != null && record.detailedMarks != null && record.detailedMarks.containsKey(sub.name);
                                    if (!hasDetails || (record.detailedMarks.get(sub.name).grandTotal == 0 && (record.detailedMarks.get(sub.name).grade == null || record.detailedMarks.get(sub.name).grade.trim().isEmpty()))) {
                                        missing.add(sub.name + " (Marks)");
                                    }
                                }

                                // 2. Check Descriptive Remarks for all academic subjects + 4 special ones
                                java.util.List<String> descSubjects = new java.util.ArrayList<>();
                                for (com.kartik.myschool.model.Subject sub : classSubjects) descSubjects.add(sub.name);
                                descSubjects.add("Vishesh pragati");
                                descSubjects.add("Aavad, chanda, etc");
                                descSubjects.add("Sudharna Aavashyaka");
                                descSubjects.add("Vyaktimatva gun vishgesh");

                                for (String subName : descSubjects) {
                                    boolean hasDetails = record != null && record.detailedMarks != null && record.detailedMarks.containsKey(subName);
                                    if (!hasDetails || record.detailedMarks.get(subName).remark == null || record.detailedMarks.get(subName).remark.trim().isEmpty()) {
                                        missing.add(subName + " (Remarks)");
                                    }
                                }
                            }

                            items.add(new com.kartik.myschool.adapter.MissingInfoAdapter.MissingInfoItem(s, missing));
                        }

                        // Sort: Students with missing info first, then by roll number
                        items.sort((a, b) -> {
                            boolean aEmpty = a.missingDetails.isEmpty();
                            boolean bEmpty = b.missingDetails.isEmpty();
                            if (aEmpty != bEmpty) return aEmpty ? 1 : -1;
                            
                            try {
                                int r1 = Integer.parseInt(a.student.rollNo);
                                int r2 = Integer.parseInt(b.student.rollNo);
                                return Integer.compare(r1, r2);
                            } catch (Exception e) {
                                return 0;
                            }
                        });

                        progress.setVisibility(android.view.View.GONE);
                        rv.setVisibility(android.view.View.VISIBLE);
                        adapter.setData(items);
                    }

                    @Override
                    public void onError(Exception e) {
                        progress.setVisibility(android.view.View.GONE);
                        android.widget.Toast.makeText(getContext(), "Failed to load marks", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                progress.setVisibility(android.view.View.GONE);
                android.widget.Toast.makeText(getContext(), "Failed to load students", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
}

