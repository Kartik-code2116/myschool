package com.example.myschool.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ImageButton;

import com.example.myschool.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myschool.HomeActivity;
import com.example.myschool.SessionContext;
import com.example.myschool.adapter.ReportPrintingAdapter;
import com.example.myschool.databinding.FragmentReportPrintingBinding;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.Student;
import com.example.myschool.repository.FirebaseRepository;
import com.example.myschool.utils.PdfGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ReportPrintingFragment extends Fragment {

    private FragmentReportPrintingBinding b;
    private ReportPrintingAdapter adapter;
    private List<Student> studentsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentReportPrintingBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        displayHeaderInfo();
        loadClassStudents();
        validateAndLoadSemesters();
    }

    private void setupRecyclerView() {
        b.rvReportCards.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReportPrintingAdapter();
        adapter.setOnItemClickListener((template, position) -> handleReportSelection(position));
        b.rvReportCards.setAdapter(adapter);
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        String classLabel = SessionContext.selectedClass != null ? SessionContext.selectedClass.getDisplayName() : "None";
        b.tvReportPrintingYear.setText("Year: " + yearLabel + " | Class: " + classLabel);
    }

    private void loadClassStudents() {
        if (SessionContext.selectedClass == null) return;
        FirebaseRepository.get().getStudentsForClass(SessionContext.selectedClass.id, new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                if (list != null) {
                    studentsList = list;
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleReportSelection(int position) {
        if (SessionContext.selectedClass == null) {
            Toast.makeText(getContext(), "कृपया मुख्यपृष्ठावरून इयत्ता व तुकडी निवडा!", Toast.LENGTH_LONG).show();
            return;
        }

        // Class-level (roster) reports — no student selection needed
        // Positions 1: Index, 4,5,8,9,10,12,14: Class-wide progress and roster charts
        boolean isClassReport = (position == 1 || position == 4 || position == 5 || position == 8 || position == 9 || position == 10 || position == 12 || position == 14);

        if (isClassReport) {
            generateClassRosterReport(position);
        } else {
            // Default to generating for all students directly
            triggerBulkReportGeneration(position);
        }
    }

    private void showStudentSelectionDialog(int reportPosition) {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), "या वर्गात कोणतेही विद्यार्थी आढळले नाहीत", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] studentNames = new String[studentsList.size()];
        for (int i = 0; i < studentsList.size(); i++) {
            studentNames[i] = studentsList.get(i).rollNo + ". " + studentsList.get(i).name;
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("विद्यार्थी निवडा (Select Student)")
                .setItems(studentNames, (dialog, which) -> {
                    Student selectedStudent = studentsList.get(which);
                    generateIndividualReport(selectedStudent, reportPosition);
                })
                .show();
    }

    private String[] getSemesterIds() {
        String sem1Id = "sem_1";
        String sem2Id = "sem_2";
        if (com.example.myschool.AppCache.cachedSemesters != null) {
            for (com.example.myschool.model.Semester sem : com.example.myschool.AppCache.cachedSemesters) {
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

    private void generateIndividualReport(Student student, int reportPosition) {
        Toast.makeText(getContext(), student.name + " चा रिपोर्ट तयार होत आहे...", Toast.LENGTH_SHORT).show();
        
        String classId = SessionContext.selectedClass.id;
        String[] sids = getSemesterIds();
        FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, sids[0], new FirebaseRepository.OnResult<MarksRecord>() {
            @Override
            public void onSuccess(MarksRecord s1) {
                FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, sids[1], new FirebaseRepository.OnResult<MarksRecord>() {
                    @Override
                    public void onSuccess(MarksRecord s2) {
                        triggerIndividualGenerator(student, s1, s2, reportPosition);
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "द्वितीय सत्राचे गुण मिळवण्यात अपयश आले", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "प्रथम सत्राचे गुण मिळवण्यात अपयश आले", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void triggerIndividualGenerator(Student student, MarksRecord s1, MarksRecord s2, int reportPosition) {
        PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
            @Override
            public void onSuccess(File pdfFile) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "रिपोर्ट यशस्वीरीत्या तयार झाला!", Toast.LENGTH_SHORT).show();
                        openPdfFile(pdfFile);
                    });
                }
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "त्रुटी आढळली: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
        };

        switch (reportPosition) {
            case 0:  // 1. मुखपृष्ठ (Cover Page)
                PdfGenerator.generateCoverPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                break;
            case 3:  // 4. वर्णनात्मक नोंदी (Descriptive)
                PdfGenerator.generateDescriptive(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                break;
            case 1:  // 2. अनुक्रमणिका (Index)
            case 2:  // 3. गुणनोंदी (Marks Register)
            case 8:  // 9. प्रगतीपत्रक मुखपृष्ठ (Progress Card Cover)
            case 9:  // 10. प्रगतीपत्रक पृष्ठ (Progress Card Inner)
            case 11: // 12. पाचवी आठवी गुणपत्रक
            case 13: // 14. प्रगतीपत्रक मुखपृष्ठ (duplicate)
                PdfGenerator.generateGunapattrak(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                break;
            default: // Fallback to personality record for any other individual report
                PdfGenerator.generatePersonalityRecord(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
                break;
        }
    }

    private void triggerBulkReportGeneration(int reportPosition) {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), "कोणताही विद्यार्थी उपलब्ध नाही", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getContext(), "पार्श्वभूमीत संपूर्ण वर्गाचे रिपोर्ट तयार होत आहेत, कृपया थांबा...", Toast.LENGTH_LONG).show();
        
        String classId = SessionContext.selectedClass.id;
        String[] sids = getSemesterIds();
        FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[0], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
            @Override
            public void onSuccess(Map<String, MarksRecord> sem1Map) {
                FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[1], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                    @Override
                    public void onSuccess(Map<String, MarksRecord> sem2Map) {
                        generateBulkPdfs(sem1Map, sem2Map, reportPosition);
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "द्वितीय सत्राचे गुण लोड होऊ शकले नाहीत", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "प्रथम सत्राचे गुण लोड होऊ शकले नाहीत", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateBulkPdfs(Map<String, MarksRecord> sem1Map, Map<String, MarksRecord> sem2Map, int reportPosition) {
        PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
            @Override public void onSuccess(File pdfFile) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "संपूर्ण वर्गाचा एकत्रित रिपोर्ट यशस्वीरित्या तयार झाला!", Toast.LENGTH_LONG).show();
                    openPdfFile(pdfFile);
                });
            }
            @Override public void onError(Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
            case 0:  PdfGenerator.generateCoverPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback); break;
            case 3:  PdfGenerator.generateDescriptive(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback); break;
            case 1:
            case 2:
            case 8:
            case 9:
            case 11:
            case 13: PdfGenerator.generateGunapattrak(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback); break;
            default: PdfGenerator.generatePersonalityRecord(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback); break;
        }
        
        latch.await();
        if (err[0] != null) throw err[0];
        return result[0];
    }

    private void generateClassRosterReport(int reportPosition) {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), "या वर्गात कोणतेही विद्यार्थी आढळले नाहीत", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (reportPosition == 0) {
            Toast.makeText(getContext(), "मुखपृष्ठ तयार होत आहे...", Toast.LENGTH_SHORT).show();
            PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
                @Override public void onSuccess(File pdfFile) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "मुखपृष्ठ यशस्वीरित्या तयार झाले!", Toast.LENGTH_SHORT).show();
                        openPdfFile(pdfFile);
                    });
                }
                @Override public void onError(Exception e) {
                    if (getActivity() != null) getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            };
            PdfGenerator.generateCoverPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, null, null, null, cb);
            return;
        }

        if (reportPosition == 1) {
            Toast.makeText(getContext(), "अनुक्रमणिका तयार होत आहे...", Toast.LENGTH_SHORT).show();
            PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
                @Override public void onSuccess(File pdfFile) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "अनुक्रमणिका यशस्वीरित्या तयार झाली!", Toast.LENGTH_SHORT).show();
                        openPdfFile(pdfFile);
                    });
                }
                @Override public void onError(Exception e) {
                    if (getActivity() != null) getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            };
            PdfGenerator.generateIndexPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, cb);
            return;
        }

        Toast.makeText(getContext(), "वर्गवार एकूण निकाल तक्ता तयार होत आहे...", Toast.LENGTH_SHORT).show();
        
        String classId = SessionContext.selectedClass.id;
        String[] sids = getSemesterIds();
        FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[0], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
            @Override
            public void onSuccess(Map<String, MarksRecord> sem1Map) {
                FirebaseRepository.get().getMarksForClassAndSemester(classId, sids[1], new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                    @Override
                    public void onSuccess(Map<String, MarksRecord> sem2Map) {
                        PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
                            @Override public void onSuccess(File pdfFile) {
                                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "निकाल तक्ता यशस्वीरित्या तयार झाला!", Toast.LENGTH_SHORT).show();
                                    openPdfFile(pdfFile);
                                });
                            }
                            @Override public void onError(Exception e) {
                                if (getActivity() != null) getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "तक्ता बनवण्यात त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        };
                        switch (reportPosition) {
                            case 4:
                                PdfGenerator.generateGradeChart(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, false, cb); break;
                            case 6:
                                PdfGenerator.generateGradeChart(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem2Map, true, cb); break;
                            case 5:
                            case 7:
                            case 10:
                            case 12:
                            case 14:
                            default:
                                PdfGenerator.generateProgressBook(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, sem2Map, cb); break;
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "द्वितीय सत्राचे गुण लोड होऊ शकले नाहीत", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "प्रथम सत्राचे गुण लोड होऊ शकले नाहीत", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndLoadSemesters() {
        boolean cacheValid = false;
        if (com.example.myschool.AppCache.cachedSemesters != null && !com.example.myschool.AppCache.cachedSemesters.isEmpty()) {
            cacheValid = true;
            for (com.example.myschool.model.Semester sem : com.example.myschool.AppCache.cachedSemesters) {
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
                FirebaseRepository.get().getSemestersForYear(yearId, new FirebaseRepository.OnResult<List<com.example.myschool.model.Semester>>() {
                    @Override
                    public void onSuccess(List<com.example.myschool.model.Semester> list) {
                        if (list != null) {
                            com.example.myschool.AppCache.cachedSemesters = list;
                        }
                    }
                    @Override public void onError(Exception e) {}
                });
            }
        }
    }

    private void generateAndShowCollectivePdf() {
        generateMasterReportPdf();
    }

    private void generateMasterReportPdf() {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), "या वर्गात कोणतेही विद्यार्थी आढळले नाहीत", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.ProgressDialog pd = new android.app.ProgressDialog(getContext());
        pd.setMessage("सर्व 15 रिपोर्ट तयार होत आहेत. कृपया प्रतीक्षा करा (यास काही मिनिटे लागू शकतात)...");
        pd.setCancelable(false);
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
                                List<File> allFiles = new ArrayList<>();
                                // Generate all 15 reports
                                for (int i = 0; i <= 14; i++) {
                                    File f = generateReportPositionSync(i, sem1Map, sem2Map);
                                    if (f != null && f.exists()) allFiles.add(f);
                                }
                                
                                File masterOut = new File(getContext().getCacheDir(), "Master_Report_Class_" + System.currentTimeMillis() + ".pdf");
                                com.example.myschool.utils.PdfMerger.mergePdfFiles(allFiles, masterOut);
                                
                                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                    pd.dismiss();
                                    Toast.makeText(getContext(), "मास्टर रिपोर्ट तयार झाला!", Toast.LENGTH_LONG).show();
                                    openPdfFile(masterOut);
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
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
        CountDownLatch latch = new CountDownLatch(1);
        final File[] result = new File[1];
        PdfGenerator.PdfCallback cb = new PdfGenerator.PdfCallback() {
            @Override public void onSuccess(File pdfFile) { result[0] = pdfFile; latch.countDown(); }
            @Override public void onError(Exception e) { latch.countDown(); }
        };

        boolean isClassReport = (position == 0 || position == 1 || position == 4 || position == 5 || position == 6
                || position == 7 || position == 10 || position == 12 || position == 14);

        if (position == 0) { // Cover page
            PdfGenerator.generateBulkCombinedPdf(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, sem2Map, 0, cb);
        } else if (position == 1) { // Index
            PdfGenerator.generateIndexPage(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, cb);
        } else if (position == 4) {
            PdfGenerator.generateGradeChart(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, false, cb);
        } else if (position == 6) {
            PdfGenerator.generateGradeChart(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem2Map, true, cb);
        } else if (isClassReport) {
            PdfGenerator.generateProgressBook(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, sem2Map, cb);
        } else {
            PdfGenerator.generateBulkCombinedPdf(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, studentsList, sem1Map, sem2Map, position, cb);
        }
        latch.await();
        return result[0];
    }

    private void openPdfFile(File file) {
        showInAppPdfViewer(file);
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
            
            android.view.View root = android.view.LayoutInflater.from(getContext()).inflate(R.layout.dialog_pdf_viewer, null);
            
            android.widget.ImageButton btnBack = root.findViewById(R.id.btnBack);
            android.widget.ImageButton btnToggleView = root.findViewById(R.id.btnToggleView);
            com.example.myschool.utils.ZoomImageView ivPdfPage = root.findViewById(R.id.ivPdfPage);
            androidx.recyclerview.widget.RecyclerView rvPdfPages = root.findViewById(R.id.rvPdfPages);
            android.widget.TextView tvPageIndicator = root.findViewById(R.id.tvPageIndicator);
            com.google.android.material.button.MaterialButton btnPrev = root.findViewById(R.id.btnPrev);
            com.google.android.material.button.MaterialButton btnNext = root.findViewById(R.id.btnNext);
            com.google.android.material.button.MaterialButton btnDownload = root.findViewById(R.id.btnDownload);
            
            rvPdfPages.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
            PdfPageAdapter adapter = new PdfPageAdapter(renderer, totalPages);
            rvPdfPages.setAdapter(adapter);

            final boolean[] isScrollMode = {false};

            btnToggleView.setOnClickListener(v -> {
                isScrollMode[0] = !isScrollMode[0];
                if (isScrollMode[0]) {
                    ivPdfPage.setVisibility(android.view.View.GONE);
                    tvPageIndicator.setVisibility(android.view.View.GONE);
                    btnPrev.setVisibility(android.view.View.GONE);
                    btnNext.setVisibility(android.view.View.GONE);
                    rvPdfPages.setVisibility(android.view.View.VISIBLE);
                } else {
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
            
            btnDownload.setOnClickListener(v -> downloadOrSharePdfFile(pdfFile));
            
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
                    renderPage.run();
                }
            });
            btnNext.setOnClickListener(v -> {
                if (currentPage[0] < totalPages - 1) {
                    currentPage[0]++;
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
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    getContext(),
                    getContext().getPackageName() + ".fileprovider",
                    file
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
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
            Toast.makeText(getContext(), "त्रुटी: PDF फाईल उघडू शकली नाही", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity activity = (HomeActivity) getActivity();
            activity.showCustomToolbarActions(
                    true,
                    v -> generateAndShowCollectivePdf(),
                    v -> {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        popup.getMenu().add("Page Setup");
                        popup.getMenu().add("Select Margins");
                        popup.getMenu().add("Unicode Settings");
                        popup.setOnMenuItemClickListener(menuItem -> {
                            Toast.makeText(getContext(), menuItem.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
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
                    page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    holder.ivPage.setImageBitmap(bmp);
                    page.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return totalPages;
        }

        static class PdfViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.ImageView ivPage;
            public PdfViewHolder(@androidx.annotation.NonNull android.view.View itemView) {
                super(itemView);
                ivPage = itemView.findViewById(R.id.ivPage);
            }
        }
    }
}
