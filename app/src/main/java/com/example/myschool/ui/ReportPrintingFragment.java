package com.example.myschool.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

        if (position == 2) {
            // Class consolidated progress book roster
            generateClassRosterReport();
        } else {
            // Individual student templates (Gunapattrak, Descriptive Remarks, Personality)
            String[] options = {"१. एक विद्यार्थी निवडा (Select Single Student)", "२. संपूर्ण वर्गाचे रिपोर्ट्स बनवा (Bulk Class PDFs)"};
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("रिपोर्ट प्रकार निवडा (Choose Report Type)")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showStudentSelectionDialog(position);
                        } else {
                            triggerBulkReportGeneration(position);
                        }
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

    private void generateIndividualReport(Student student, int reportPosition) {
        Toast.makeText(getContext(), student.name + " चा रिपोर्ट तयार होत आहे...", Toast.LENGTH_SHORT).show();
        
        String classId = SessionContext.selectedClass.id;
        FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, "sem_1", new FirebaseRepository.OnResult<MarksRecord>() {
            @Override
            public void onSuccess(MarksRecord s1) {
                FirebaseRepository.get().getMarksForStudentAndSemester(student.id, classId, "sem_2", new FirebaseRepository.OnResult<MarksRecord>() {
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
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "त्रुटी आढळली: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        };

        if (reportPosition == 0) {
            PdfGenerator.generateGunapattrak(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
        } else if (reportPosition == 1) {
            PdfGenerator.generateDescriptive(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
        } else if (reportPosition == 3) {
            PdfGenerator.generatePersonalityRecord(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, cb);
        }
    }

    private void triggerBulkReportGeneration(int reportPosition) {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), "कोणताही विद्यार्थी उपलब्ध नाही", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getContext(), "पार्श्वभूमीत संपूर्ण वर्गाचे रिपोर्ट तयार होत आहेत, कृपया थांबा...", Toast.LENGTH_LONG).show();
        
        String classId = SessionContext.selectedClass.id;
        FirebaseRepository.get().getMarksForClassAndSemester(classId, "sem_1", new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
            @Override
            public void onSuccess(Map<String, MarksRecord> sem1Map) {
                FirebaseRepository.get().getMarksForClassAndSemester(classId, "sem_2", new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
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
        new Thread(() -> {
            int successCount = 0;
            File lastFile = null;
            
            for (Student student : studentsList) {
                MarksRecord s1 = sem1Map.get(student.id);
                MarksRecord s2 = sem2Map.get(student.id);
                
                try {
                    File file = generateReportSync(student, s1, s2, reportPosition);
                    if (file != null) {
                        successCount++;
                        lastFile = file;
                    }
                } catch (Exception ignored) {}
            }
            
            int finalSuccessCount = successCount;
            File finalLastFile = lastFile;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "यशस्वीरित्या " + finalSuccessCount + " विद्यार्थ्यांचे रिपोर्ट तयार झाले!", Toast.LENGTH_LONG).show();
                    if (finalLastFile != null) {
                        openPdfFile(finalLastFile);
                    }
                });
            }
        }).start();
    }

    private File generateReportSync(Student student, MarksRecord s1, MarksRecord s2, int reportPosition) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final File[] result = new File[1];
        final Exception[] err = new Exception[1];
        
        PdfGenerator.PdfCallback callback = new PdfGenerator.PdfCallback() {
            @Override
            public void onSuccess(File pdfFile) {
                result[0] = pdfFile;
                latch.countDown();
            }
            @Override
            public void onError(Exception e) {
                err[0] = e;
                latch.countDown();
            }
        };
        
        if (reportPosition == 0) {
            PdfGenerator.generateGunapattrak(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback);
        } else if (reportPosition == 1) {
            PdfGenerator.generateDescriptive(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback);
        } else if (reportPosition == 3) {
            PdfGenerator.generatePersonalityRecord(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass, student, s1, s2, callback);
        }
        
        latch.await();
        if (err[0] != null) throw err[0];
        return result[0];
    }

    private void generateClassRosterReport() {
        if (studentsList == null || studentsList.isEmpty()) {
            Toast.makeText(getContext(), "या वर्गात कोणतेही विद्यार्थी आढळले नाहीत", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getContext(), "वर्गवार एकूण निकाल तक्ता तयार होत आहे...", Toast.LENGTH_SHORT).show();
        
        String classId = SessionContext.selectedClass.id;
        FirebaseRepository.get().getMarksForClassAndSemester(classId, "sem_1", new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
            @Override
            public void onSuccess(Map<String, MarksRecord> sem1Map) {
                FirebaseRepository.get().getMarksForClassAndSemester(classId, "sem_2", new FirebaseRepository.OnResult<Map<String, MarksRecord>>() {
                    @Override
                    public void onSuccess(Map<String, MarksRecord> sem2Map) {
                        PdfGenerator.generateProgressBook(getContext(), SessionContext.selectedSchool, SessionContext.selectedClass,
                                studentsList, sem1Map, sem2Map, new PdfGenerator.PdfCallback() {
                                    @Override
                                    public void onSuccess(File pdfFile) {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "निकाल तक्ता यशस्वीरित्या तयार झाला!", Toast.LENGTH_SHORT).show();
                                                openPdfFile(pdfFile);
                                            });
                                        }
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "तक्ता बनवण्यात त्रुटी आढळली: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                        }
                                    }
                                });
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

    private void openPdfFile(File file) {
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
            ((HomeActivity) getActivity()).showCustomToolbarActions(
                    true,
                    v -> Toast.makeText(getContext(), "माहिती व रिपोर्ट प्रिंट मार्गदर्शक तत्त्वे", Toast.LENGTH_SHORT).show(),
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
