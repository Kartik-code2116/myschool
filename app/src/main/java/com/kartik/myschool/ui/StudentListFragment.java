package com.kartik.myschool.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kartik.myschool.AppCache;
import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.EnterMarksActivity;
import com.kartik.myschool.MarksheetActivity;
import com.kartik.myschool.ClassSetupActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.StudentEditActivity;
import com.kartik.myschool.StudentProfileActivity;
import com.kartik.myschool.adapter.StudentAdapter;
import com.kartik.myschool.databinding.FragmentStudentListBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.UiAnimations;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class StudentListFragment extends Fragment {

    private FragmentStudentListBinding b;
    private StudentAdapter studentAdapter;
    private List<Student> allStudents = new ArrayList<>();
    private List<Student> filteredStudents = new ArrayList<>();
    private List<School> schools = new ArrayList<>();
    private List<ClassModel> classes = new ArrayList<>();
    private School selectedSchool = null;
    private ClassModel selectedClass = null;

    private final androidx.activity.result.ActivityResultLauncher<String> csvPickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    importStudentsFromCsv(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentStudentListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecycler();
        setupSearch();
        setupFab();
        setupCustomToolbar();
        UiAnimations.staggerFadeIn(b.etSearch, b.rvStudents, b.fabAddStudent);

        applySessionFilterIfAny();
    }

    private void setupCustomToolbar() {
        if (b.btnCustomMenu != null) {
            b.btnCustomMenu.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).openDrawer();
                }
            });
        }

        if (b.btnHelp != null) {
            b.btnHelp.setOnClickListener(v -> {
                com.kartik.myschool.utils.HelpDialogHelper.showHelpDialog(requireContext(), "students");
            });
        }

        if (b.btnExcel != null) {
            b.btnExcel.setOnClickListener(v -> {
                showExcelOptionsPopupMenu(v);
            });
        }

        if (b.btnIdCard != null) {
            b.btnIdCard.setOnClickListener(v -> {
                android.widget.Toast.makeText(requireContext(), R.string.msg_empty_27, android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        if (b.btnMoreOptions != null) {
            b.btnMoreOptions.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).showHomeMoreMenu(v);
                }
            });
        }
    }

    private void applySessionFilterIfAny() {
        SessionContext.syncFromAppCache();
        if (SessionContext.selectedClass != null && SessionContext.selectedClass.id != null) {
            FirebaseRepository.get().getStudentsForClass(SessionContext.selectedClass.id,
                    new FirebaseRepository.OnResult<List<Student>>() {
                        @Override public void onSuccess(List<Student> list) {
                            allStudents.clear();
                            allStudents.addAll(list);
                            filteredStudents.clear();
                            filteredStudents.addAll(list);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    studentAdapter.setData(filteredStudents);
                                    b.emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
                                });
                            }
                        }
                        @Override public void onError(Exception e) { loadAllStudents(); }
                    });
        }
    }

    private void setupRecycler() {
        studentAdapter = new StudentAdapter();
        studentAdapter.setListener(new StudentAdapter.OnStudentClick() {
            @Override
            public void onClick(Student student, int position) {
                AppCache.selectedStudent = student;
                startActivity(new Intent(requireContext(), StudentProfileActivity.class));
            }

            @Override
            public void onEnterMarksClick(Student student, int position) {
                AppCache.selectedStudent = student;
                loadClassForStudent(student, () -> {
                    if (AppCache.selectedClass == null) return;
                    Intent intent = new Intent(requireContext(), EnterMarksActivity.class);
                    startActivity(intent);
                });
            }

            @Override
            public void onAttendanceClick(Student student, int position) {
                AppCache.selectedStudent = student;
                loadClassForStudent(student, () -> {
                    if (AppCache.selectedClass == null) return;
                    SessionContext.selectedClass = AppCache.selectedClass;
                    SessionContext.save(requireContext());
                    if (getActivity() instanceof HomeActivity) {
                        ((HomeActivity) getActivity()).navigateTo(R.id.nav_attendance);
                    }
                });
            }

            @Override
            public void onEditInfoClick(Student student, int position) {
                AppCache.selectedStudent = student;
                startActivity(new Intent(requireContext(), StudentEditActivity.class)
                        .putExtra("new_student", false));
            }

            @Override
            public void onDeleteClick(Student student, int position) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.msg_delete_student)
                        .setMessage("Are you sure you want to delete " + student.name + "?")
                        .setPositiveButton("Delete", (d, w) -> {
                            FirebaseRepository.get().deleteStudent(student.id, new FirebaseRepository.OnResult<Void>() {
                                @Override
                                public void onSuccess(Void v) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            allStudents.remove(student);
                                            filteredStudents.remove(student);
                                            studentAdapter.setData(filteredStudents);
                                            b.emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
                                            android.widget.Toast.makeText(requireContext(), R.string.msg_student_deleted_successfully, android.widget.Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }
                                @Override
                                public void onError(Exception e) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            android.widget.Toast.makeText(requireContext(), "Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        b.rvStudents.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvStudents.setAdapter(studentAdapter);
        UiAnimations.setupRecyclerAnimations(b.rvStudents);
    }

    private void setupSearch() {
        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
                b.btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        b.btnClearSearch.setOnClickListener(v -> {
            b.etSearch.setText("");
            filterStudents("");
        });
    }

    private void setupFab() {
        b.fabAddStudent.setOnClickListener(v -> {
            FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
                @Override
                public void onSuccess(com.kartik.myschool.model.Teacher teacher) {
                    if (teacher != null && !"active".equals(teacher.subscriptionStatus)) {
                        FirebaseRepository.get().getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
                            @Override
                            public void onSuccess(List<Student> students) {
                                if (students != null && students.size() >= 3) {
                                    SubscriptionBottomSheet bottomSheet = new SubscriptionBottomSheet();
                                    bottomSheet.show(getParentFragmentManager(), "SubscriptionBottomSheet");
                                } else {
                                    openAddStudent();
                                }
                            }
                            @Override
                            public void onError(Exception e) {
                                openAddStudent();
                            }
                        });
                    } else {
                        openAddStudent();
                    }
                }
                @Override
                public void onError(Exception e) {
                    openAddStudent();
                }
            });
        });
    }

    private void openAddStudent() {
        AppCache.selectedStudent = new Student();
        startActivity(new Intent(requireContext(), StudentEditActivity.class)
                .putExtra("new_student", true));
    }

    private void loadAllStudents() {
        b.emptyState.setVisibility(View.GONE);
        FirebaseRepository.get().getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                allStudents.clear();
                allStudents.addAll(list);
                filteredStudents.clear();
                filteredStudents.addAll(list);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        studentAdapter.setData(filteredStudents);
                        b.emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                }
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> b.emptyState.setVisibility(View.VISIBLE));
                }
            }
        });
    }

    private void filterStudents(String query) {
        filteredStudents.clear();
        if (query.isEmpty()) {
            filteredStudents.addAll(allStudents);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Student s : allStudents) {
                if (s.name != null && s.name.toLowerCase().contains(lowerQuery)) {
                    filteredStudents.add(s);
                } else if (s.rollNo != null && s.rollNo.toLowerCase().contains(lowerQuery)) {
                    filteredStudents.add(s);
                }
            }
        }
        studentAdapter.setData(filteredStudents);
        b.emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Bug #7 fix: always clear AppCache.selectedClass before searching to prevent
    // stale data from a previous student leaking into the next navigation action.
    private void loadClassForStudent(Student student, Runnable onComplete) {
        AppCache.selectedClass = null;  // clear before search
        FirebaseRepository.get().getClassesForSchool(student.schoolId, new FirebaseRepository.OnResult<List<ClassModel>>() {
            @Override
            public void onSuccess(List<ClassModel> list) {
                for (ClassModel c : list) {
                    if (java.util.Objects.equals(c.id, student.classId)) {
                        AppCache.selectedClass = c;
                        break;
                    }
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(onComplete);
                }
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(onComplete);
                }
            }
        });
    }

    private void loadMarksForStudent(Student student) {
        loadClassForStudent(student, () -> {
            if (AppCache.selectedClass == null) return;
            String semId = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.id : "sem_1";
            FirebaseRepository.get().getMarksForStudentAndSemester(student.id, student.classId, semId,
                    new FirebaseRepository.OnResult<MarksRecord>() {
                        @Override
                        public void onSuccess(MarksRecord m) {
                            if (m != null) {
                                AppCache.selectedMarks = m;
                                startActivity(new Intent(requireContext(), MarksheetActivity.class));
                            } else {
                                // No marks yet, go to enter marks
                                startActivity(new Intent(requireContext(), EnterMarksActivity.class));
                            }
                        }
                        @Override
                        public void onError(Exception e) {}
                    });
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
        SessionContext.syncFromAppCache();

        // Hide parent top app bar & fix CoordinatorLayout scrolling behavior offset bug:
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.GONE);
            }
            
            View navHost = ha.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                        (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost.getLayoutParams();
                params.setBehavior(null);
                float density = getResources().getDisplayMetrics().density;
                params.bottomMargin = (int) (64 * density);
                navHost.setLayoutParams(params);
            }
        }

        // Dynamically set sub-text and header section details
        String yearLabel = SessionContext.getYearLabel();
        String classVal = "1";
        String divVal = "1";
        if (SessionContext.selectedClass != null) {
            classVal = SessionContext.selectedClass.className != null ? SessionContext.selectedClass.className : "1";
            divVal = SessionContext.selectedClass.division != null && !SessionContext.selectedClass.division.isEmpty() 
                    ? SessionContext.selectedClass.division : "1";
        }
        
        if (b.tvCustomSubtitle != null) {
            b.tvCustomSubtitle.setText("• Class: " + classVal + " • Div: " + divVal + " •");
        }
        
        if (b.tvHeaderSessionInfo != null) {
            b.tvHeaderSessionInfo.setText("Year: " + yearLabel + " Class: " + classVal + ", Div: " + divVal);
        }

        if (SessionContext.selectedClass != null) {
            applySessionFilterIfAny();
        } else {
            loadAllStudents();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Restore parent top app bar and scrolling behavior:
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.VISIBLE);
            }
            
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

    private void showExcelOptionsPopupMenu(View v) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 1, 0, R.string.action_export_students);
        popup.getMenu().add(0, 2, 1, R.string.action_import_students);
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                exportStudentsToExcel();
                return true;
            } else if (itemId == 2) {
                importStudentsFromExcel();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void exportStudentsToExcel() {
        if (filteredStudents.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "No students in this class to export.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("ID,Student Name,Standard,Division,Roll No 1,Roll No 2,Gender,Caste,Registration No,Date of Birth,Mother's Name,Mother's Occupation,Mother's Phone,Father's Name,Father's Occupation,Father's Phone,Address,Account No,Branch,IFSC,Bank UID,Medium,Mother Tongue,Date of Admission,Student Id,UID\n");

            for (Student s : filteredStudents) {
                sb.append(escapeCsv(s.id != null ? s.id : "")).append(",")
                  .append(escapeCsv(s.name != null ? s.name : "")).append(",")
                  .append(escapeCsv(s.standard != null ? s.standard : "")).append(",")
                  .append(escapeCsv(s.division != null ? s.division : "")).append(",")
                  .append(escapeCsv(s.rollNo != null ? s.rollNo : "")).append(",")
                  .append(escapeCsv(s.rollNo2 != null ? s.rollNo2 : "")).append(",")
                  .append(escapeCsv(s.gender != null ? s.gender : "")).append(",")
                  .append(escapeCsv(s.cast != null ? s.cast : "")).append(",")
                  .append(escapeCsv(s.registrationNo != null ? s.registrationNo : "")).append(",")
                  .append(escapeCsv(s.dob != null ? s.dob : "")).append(",")
                  .append(escapeCsv(s.motherName != null ? s.motherName : "")).append(",")
                  .append(escapeCsv(s.motherOccupation != null ? s.motherOccupation : "")).append(",")
                  .append(escapeCsv(s.motherPhone != null ? s.motherPhone : "")).append(",")
                  .append(escapeCsv(s.fatherName != null ? s.fatherName : "")).append(",")
                  .append(escapeCsv(s.fatherOccupation != null ? s.fatherOccupation : "")).append(",")
                  .append(escapeCsv(s.fatherPhone != null ? s.fatherPhone : "")).append(",")
                  .append(escapeCsv(s.address != null ? s.address : "")).append(",")
                  .append(escapeCsv(s.bankAccount != null ? s.bankAccount : "")).append(",")
                  .append(escapeCsv(s.bankBranch != null ? s.bankBranch : "")).append(",")
                  .append(escapeCsv(s.bankIfsc != null ? s.bankIfsc : "")).append(",")
                  .append(escapeCsv(s.bankUid != null ? s.bankUid : "")).append(",")
                  .append(escapeCsv(s.medium != null ? s.medium : "")).append(",")
                  .append(escapeCsv(s.motherTongue != null ? s.motherTongue : "")).append(",")
                  .append(escapeCsv(s.dateOfAdmission != null ? s.dateOfAdmission : "")).append(",")
                  .append(escapeCsv(s.studentIdNumber != null ? s.studentIdNumber : "")).append(",")
                  .append(escapeCsv(s.uid != null ? s.uid : "")).append("\n");
            }

            File cachePath = new File(requireContext().getCacheDir(), "excel_exports");
            cachePath.mkdirs();
            String className = SessionContext.selectedClass != null ? SessionContext.selectedClass.className : "class";
            String division = SessionContext.selectedClass != null ? SessionContext.selectedClass.division : "div";
            File file = new File(cachePath, "Student_" + className + "_Std_" + division + ".csv");
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(sb.toString());
            writer.close();

            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Export Students"));
        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private int getCasteCategoryIndex(String caste) {
        if (caste == null) return 5;
        String c = caste.toUpperCase().trim();
        if (c.contains("SC")) return 0;
        if (c.contains("ST")) return 1;
        if (c.contains("VJ") || c.contains("विमुक्त")) return 2;
        if (c.contains("NT") || c.contains("भटक्या")) return 3;
        if (c.contains("OBC")) return 4;
        return 5;
    }

    private void importStudentsFromExcel() {
        if (SessionContext.selectedClass == null) {
            android.widget.Toast.makeText(requireContext(), R.string.select_class_first, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        csvPickerLauncher.launch("*/*");
    }

    private void importStudentsFromCsv(android.net.Uri uri) {
        try {
            java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) return;
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            List<String[]> lines = new ArrayList<>();
            String line;
            String headerLine = reader.readLine();
            if (headerLine == null) {
                android.widget.Toast.makeText(requireContext(), "Empty CSV file.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            List<String> headers = parseCsvLine(headerLine);
            java.util.Map<String, Integer> headerMap = new java.util.HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                headerMap.put(headers.get(i).toLowerCase().trim(), i);
            }

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<String> cells = parseCsvLine(line);
                lines.add(cells.toArray(new String[0]));
            }
            reader.close();
            is.close();

            int count = lines.size();
            if (count == 0) {
                android.widget.Toast.makeText(requireContext(), "No student records found in CSV.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            android.app.ProgressDialog pd = new android.app.ProgressDialog(requireContext());
            pd.setTitle(R.string.msg_restoring_students);
            pd.setMessage("Saving student records to Firestore...");
            pd.setCancelable(false);
            pd.show();

            ClassModel currentClass = SessionContext.selectedClass;
            School currentSchool = SessionContext.selectedSchool;
            String teacherUid = FirebaseRepository.get().currentUid();

            java.util.concurrent.atomic.AtomicInteger savedCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger failedCount = new java.util.concurrent.atomic.AtomicInteger(0);

            for (String[] cells : lines) {
                Student s = new Student();
                
                s.id = getCell(cells, new String[]{"id", "ID"}, headerMap, "");
                s.name = getCell(cells, new String[]{"name", "student name", "student full name", "full name", "नाव", "विद्यार्थ्याचे पूर्ण नाव", "studentname", "fullname"}, headerMap, "");
                s.standard = getCell(cells, new String[]{"std", "standard", "मानक", "वर्ग", "class", "std/class"}, headerMap, currentClass.className);
                s.division = getCell(cells, new String[]{"div", "division", "विभाग", "विभागणी"}, headerMap, currentClass.division);
                s.rollNo = getCell(cells, new String[]{"roll1", "rollno", "roll no", "roll no 1", "roll no. 1", "roll number", "रोल क्रमांक १", "रोल नंबर", "अनुक्रमांक", "rollnumber"}, headerMap, "");
                s.rollNo2 = getCell(cells, new String[]{"roll2", "rollno2", "roll no 2", "roll no. 2", "रोल क्रमांक २"}, headerMap, "");

                String rawGender = getCell(cells, new String[]{"gender", "sex", "लिंग"}, headerMap, "Male");
                if (rawGender.equalsIgnoreCase("Female") || rawGender.equals("2") || rawGender.equalsIgnoreCase("स्त्री") || rawGender.equalsIgnoreCase("मुलगी")) {
                    s.gender = "Female";
                } else {
                    s.gender = "Male";
                }

                String rawCategory = getCell(cells, new String[]{"category", "cast", "caste", "कास्ट", "जात", "वर्गवारी"}, headerMap, "Open");
                s.cast = getCasteCategoryName(rawCategory);

                s.registrationNo = getCell(cells, new String[]{"reg", "registrationno", "registration no", "reg no", "नोंदणी क्र", "नोंदणी क्रमांक", "नोंदणी क्र.", "registrationnumber"}, headerMap, "");
                s.dob = getCell(cells, new String[]{"dob", "year", "dateofbirth", "date of birth", "जन्मतारीख", "जन्म तारीख"}, headerMap, "");
                s.motherName = getCell(cells, new String[]{"mothername", "mother's name", "mother name", "आईचे नाव", "mother_name"}, headerMap, "");
                s.motherOccupation = getCell(cells, new String[]{"motheroccupation", "mother's occupation", "mother occupation", "आईचा व्यवसाय"}, headerMap, "");
                s.motherPhone = getCell(cells, new String[]{"motherphone", "mother's phone", "mother phone", "आईचा फोन"}, headerMap, "");
                s.fatherName = getCell(cells, new String[]{"fathername", "fname", "father's name", "father name", "वडिलांचे नाव", "fname", "father_name"}, headerMap, "");
                s.fatherOccupation = getCell(cells, new String[]{"fatheroccupation", "fwork", "father's occupation", "father occupation", "वडिलांचा व्यवसाय", "fwork", "occupation"}, headerMap, "");
                s.fatherPhone = getCell(cells, new String[]{"fatherphone", "fphone", "father's phone", "father phone", "वडिलांचा फोन", "fphone", "phone"}, headerMap, "");
                s.address = getCell(cells, new String[]{"address", "home address", "पत्ता"}, headerMap, "");
                s.bankAccount = getCell(cells, new String[]{"bankaccount", "account no", "account number", "bank account", "bank account no", "खाते क्र", "खाते क्रमांक", "खाते क्र.", "account_no"}, headerMap, "");
                s.bankBranch = getCell(cells, new String[]{"bankbranch", "branch", "bank branch", "शाखा", "bank_branch"}, headerMap, "");
                s.bankIfsc = getCell(cells, new String[]{"bankifsc", "ifsc", "ifsc code", "bank ifsc", "ifsc_code"}, headerMap, "");
                s.bankUid = getCell(cells, new String[]{"bankuid", "bank uid", "बँक uid", "bank_uid"}, headerMap, "");
                s.medium = getCell(cells, new String[]{"medium", "मध्यम"}, headerMap, "");
                s.motherTongue = getCell(cells, new String[]{"mothertongue", "mother tongue", "मातृभाषा", "mother_tongue"}, headerMap, "");
                s.dateOfAdmission = getCell(cells, new String[]{"dateofadmission", "admissiondate", "date of admission", "date_of_admission", "प्रवेशाची तारीख", "प्रवेश तारीख"}, headerMap, "");
                s.studentIdNumber = getCell(cells, new String[]{"studentidnumber", "studentid", "student id", "student id number", "student_id", "विद्यार्थी आयडी", "विद्यार्थी आयडी क्र"}, headerMap, "");
                s.uid = getCell(cells, new String[]{"uid", "aadhar", "aadhar no", "aadhar number", "uid", "आधार कार्ड", "आधार नंबर", "आधार क्रमांक"}, headerMap, "");

                s.parentName = s.fatherName != null && !s.fatherName.isEmpty() ? s.fatherName : (s.motherName != null ? s.motherName : "");
                
                s.classId = currentClass.id;
                s.className = currentClass.className;
                if (currentSchool != null) {
                    s.schoolId = currentSchool.id;
                    s.schoolName = currentSchool.name;
                }
                s.teacherId = teacherUid;

                FirebaseRepository.get().saveStudent(s, new FirebaseRepository.OnResult<String>() {
                    @Override
                    public void onSuccess(String id) {
                        int sVal = savedCount.incrementAndGet();
                        checkImportDone(sVal, failedCount.get(), count, pd);
                    }

                    @Override
                    public void onError(Exception e) {
                        int fVal = failedCount.incrementAndGet();
                        checkImportDone(savedCount.get(), fVal, count, pd);
                    }
                });
            }

        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(), "Failed to read CSV: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private String getCell(String[] cells, String[] aliases, java.util.Map<String, Integer> headerMap, String fallback) {
        for (String alias : aliases) {
            String key = alias.toLowerCase().trim();
            if (headerMap.containsKey(key)) {
                int idx = headerMap.get(key);
                if (idx >= 0 && idx < cells.length) {
                    return cells[idx];
                }
            }
        }
        return fallback;
    }

    private void checkImportDone(int saved, int failed, int total, android.app.ProgressDialog pd) {
        if (saved + failed == total) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (pd.isShowing()) pd.dismiss();
                    android.widget.Toast.makeText(requireContext(),
                            "Import Complete! Imported: " + saved + ", Failed: " + failed,
                            android.widget.Toast.LENGTH_LONG).show();
                    applySessionFilterIfAny();
                });
            }
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                cells.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        cells.add(sb.toString().trim());
        return cells;
    }

    private String getCasteCategoryName(String categoryStr) {
        if (categoryStr == null || categoryStr.isEmpty()) return "Open";
        try {
            int index = Integer.parseInt(categoryStr.trim());
            switch (index) {
                case 0: return "SC (Scheduled Castes)";
                case 1: return "ST (Scheduled Tribes)";
                case 2: return "VJ (Vimukt Jati)";
                case 3: return "NT (Nomadic Tribes)";
                case 4: return "OBC (Other Backward Classes)";
                default: return "Open";
            }
        } catch (NumberFormatException e) {
            String upper = categoryStr.toUpperCase().trim();
            if (upper.equals("SC") || upper.contains("SCHEDULED CASTES") || upper.contains("अनुसूचित जाती")) {
                return "SC (Scheduled Castes)";
            } else if (upper.equals("ST") || upper.contains("SCHEDULED TRIBES") || upper.contains("अनुसूचित जमाती")) {
                return "ST (Scheduled Tribes)";
            } else if (upper.equals("VJ") || upper.contains("VIMUKT JATI") || upper.contains("VIMUKT") || upper.contains("विमुक्त")) {
                return "VJ (Vimukt Jati)";
            } else if (upper.equals("NT") || upper.contains("NOMADIC TRIBES") || upper.contains("BHATKYA") || upper.contains("भटक्या")) {
                return "NT (Nomadic Tribes)";
            } else if (upper.equals("OBC") || upper.contains("OTHER BACKWARD CLASSES") || upper.contains("SBC") || upper.contains("इतर मागास")) {
                return "OBC (Other Backward Classes)";
            } else if (upper.equalsIgnoreCase("Open") || upper.equalsIgnoreCase("General") || upper.contains("खुला")) {
                return "Open";
            }
            return categoryStr;
        }
    }
}
