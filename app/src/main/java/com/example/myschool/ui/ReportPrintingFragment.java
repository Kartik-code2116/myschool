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
        // positions: 0 (मुखपृष्ठ), 1 (अनुक्रमणिका), 4 (श्रेणी तक्का), 5 (सर्वसामावेशक), 6 (श्रेणी तक्का), 7 (गुण-श्रेणी), 10 (उपयुक्त), 12 (वार्षिक तक्के), 14 (वार्षिक निकाल)
        boolean isClassReport = (position == 0 || position == 1 || position == 4 || position == 5 || position == 6
                || position == 7 || position == 10 || position == 12 || position == 14);

        if (isClassReport) {
            generateClassRosterReport(position);
        } else {
            String[] options = {"१. एक विद्यार्थी निवडा (Select Single Student)",
                               "२. संपूर्ण वर्गाचे रिपोर्ट्स बनवा (Bulk Class PDFs)"};
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("रिपोर्ट प्रकार निवडा (Choose Report Type)")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) showStudentSelectionDialog(position);
                        else triggerBulkReportGeneration(position);
                    })
                    .show();
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
                            case 5:
                            case 6:
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
        generateClassRosterReport(14); // Default: वार्षिक निकालपत्रक
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
            
            android.widget.LinearLayout root = new android.widget.LinearLayout(getContext());
            root.setOrientation(android.widget.LinearLayout.VERTICAL);
            root.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F7"));
            root.setPadding(24, 24, 24, 24);
            
            // Header
            android.widget.RelativeLayout header = new android.widget.RelativeLayout(getContext());
            header.setPadding(0, 16, 0, 16);
            
            android.widget.ImageButton btnBack = new android.widget.ImageButton(getContext());
            btnBack.setImageResource(R.drawable.ic_chevron_left);
            btnBack.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnBack.setPadding(12, 12, 12, 12);
            btnBack.setColorFilter(android.graphics.Color.parseColor("#5A4FCF"));
            btnBack.setOnClickListener(v -> {
                renderer.close();
                dialog.dismiss();
            });
            
            android.widget.TextView tvTitle = new android.widget.TextView(getContext());
            tvTitle.setText("निकालपत्रक पहा (Preview Report)");
            tvTitle.setTextSize(18);
            tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(android.graphics.Color.parseColor("#1C1C1E"));
            
            android.widget.RelativeLayout.LayoutParams lpBack = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            lpBack.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
            lpBack.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
            header.addView(btnBack, lpBack);
            
            android.widget.RelativeLayout.LayoutParams lpTitle = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            lpTitle.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
            header.addView(tvTitle, lpTitle);
            
            root.addView(header);
            
            // Main image card view
            android.widget.ImageView ivPdfPage = new android.widget.ImageView(getContext());
            ivPdfPage.setAdjustViewBounds(true);
            ivPdfPage.setBackgroundResource(R.drawable.bg_pdf_mock_border);
            
            android.widget.ScrollView scrollView = new android.widget.ScrollView(getContext());
            android.widget.LinearLayout.LayoutParams lpScroll = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    0, 1.0f
            );
            lpScroll.setMargins(0, 16, 0, 16);
            scrollView.addView(ivPdfPage);
            root.addView(scrollView, lpScroll);
            
            // Page counter
            android.widget.TextView tvPageIndicator = new android.widget.TextView(getContext());
            tvPageIndicator.setText("पृष्ठ: 1 / " + totalPages);
            tvPageIndicator.setGravity(android.view.Gravity.CENTER);
            tvPageIndicator.setTextColor(android.graphics.Color.parseColor("#8E8E93"));
            tvPageIndicator.setTextSize(14);
            root.addView(tvPageIndicator);
            
            // Controls panel
            android.widget.LinearLayout controls = new android.widget.LinearLayout(getContext());
            controls.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            controls.setGravity(android.view.Gravity.CENTER_VERTICAL);
            controls.setPadding(0, 16, 0, 16);
            
            com.google.android.material.button.MaterialButton btnPrev = new com.google.android.material.button.MaterialButton(getContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton);
            btnPrev.setText("मागील");
            btnPrev.setTextColor(android.graphics.Color.parseColor("#5A4FCF"));
            
            com.google.android.material.button.MaterialButton btnNext = new com.google.android.material.button.MaterialButton(getContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton);
            btnNext.setText("पुढील");
            btnNext.setTextColor(android.graphics.Color.parseColor("#5A4FCF"));
            
            com.google.android.material.button.MaterialButton btnDownload = new com.google.android.material.button.MaterialButton(getContext());
            btnDownload.setText("डाउनलोड करा");
            btnDownload.setIcon(androidx.core.content.ContextCompat.getDrawable(getContext(), R.drawable.ic_print));
            btnDownload.setBackgroundColor(android.graphics.Color.parseColor("#8BC34A"));
            btnDownload.setTextColor(android.graphics.Color.WHITE);
            btnDownload.setCornerRadius(24);
            btnDownload.setPadding(16, 16, 16, 16);
            btnDownload.setOnClickListener(v -> downloadOrSharePdfFile(pdfFile));
            
            android.widget.LinearLayout.LayoutParams lpBtn = new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            controls.addView(btnPrev, lpBtn);
            
            android.widget.LinearLayout.LayoutParams lpDl = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lpDl.setMargins(16, 0, 16, 0);
            controls.addView(btnDownload, lpDl);
            
            controls.addView(btnNext, lpBtn);
            
            android.widget.LinearLayout.LayoutParams lpControls = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            float density = getResources().getDisplayMetrics().density;
            lpControls.setMargins(0, (int) (16 * density), 0, (int) (48 * density));
            root.addView(controls, lpControls);
            
            Runnable renderPage = () -> {
                try {
                    android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(currentPage[0]);
                    int w = page.getWidth() * 2;
                    int h = page.getHeight() * 2;
                    android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
                    page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    
                    ivPdfPage.setImageBitmap(bmp);
                    tvPageIndicator.setText("पृष्ठ: " + (currentPage[0] + 1) + " / " + totalPages);
                    btnPrev.setEnabled(currentPage[0] > 0);
                    btnNext.setEnabled(currentPage[0] < totalPages - 1);
                    page.close();
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
            dialog.setOnDismissListener(d -> renderer.close());
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
}
