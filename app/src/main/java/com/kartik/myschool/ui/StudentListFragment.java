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
import java.io.FileWriter;
import org.json.JSONArray;
import org.json.JSONObject;

public class StudentListFragment extends Fragment {

    private FragmentStudentListBinding b;
    private StudentAdapter studentAdapter;
    private List<Student> allStudents = new ArrayList<>();
    private List<Student> filteredStudents = new ArrayList<>();
    private List<School> schools = new ArrayList<>();
    private List<ClassModel> classes = new ArrayList<>();
    private School selectedSchool = null;
    private ClassModel selectedClass = null;
    private String lastLoadedClassId = null; // Fix 2: Cache class ID to prevent redundant fetches

    // Sort options and state
    // Removed local sort variables, now using AppCache

    private final androidx.activity.result.ActivityResultLauncher<String> csvPickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    importStudentsFromCsv(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        b = FragmentStudentListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());

        setupRecycler();
        setupSearch();
        setupFab();
        setupCustomToolbar();
        setupSortFilterButton();
        UiAnimations.staggerFadeIn(b.etSearch, b.rvStudents, b.fabAddStudent);

        applySessionFilterIfAny();
    }

    private void setupCustomToolbar() {
        if (b.tvCustomSubtitle != null) {
            b.tvCustomSubtitle.setText(SessionContext.getClassDivSemSubtitle(requireContext()));
        }
        if (b.tvHeaderSessionInfo != null) {
            b.tvHeaderSessionInfo.setText(SessionContext.getClassDivSemSubtitle(requireContext()));
        }

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

        if (b.btnDashboard != null) {
            b.btnDashboard.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(v).navigate(R.id.nav_dashboard);
            });
        }

        if (b.btnMoreOptions != null) {
            b.btnMoreOptions.setOnClickListener(v -> {
                showStudentContextPopupMenu(v);
            });
        }
    }

    private void showStudentContextPopupMenu(View v) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 1, 0, getString(R.string.menu_promote_students));
        popup.getMenu().add(0, 2, 1, getString(R.string.menu_export_students));
        popup.getMenu().add(0, 3, 2, getString(R.string.menu_import_students));
        popup.getMenu().add(0, 4, 3, getString(R.string.menu_switch_class));
        popup.getMenu().add(0, 5, 4, getString(R.string.menu_app_wide_main));
        popup.getMenu().add(0, 6, 5, "Transfer Students");
        popup.getMenu().add(0, 7, 6, "Import via Transfer Code");
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                if (SessionContext.selectedClass == null) {
                    android.widget.Toast.makeText(requireContext(), "Please select a class first.", android.widget.Toast.LENGTH_SHORT).show();
                    return true;
                }
                startActivity(new Intent(requireContext(), com.kartik.myschool.PromoteStudentsActivity.class));
                return true;
            } else if (itemId == 2) {
                exportStudentsToExcel();
                return true;
            } else if (itemId == 3) {
                importStudentsFromExcel();
                return true;
            } else if (itemId == 4) {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).navigateTo(R.id.nav_class_div);
                }
                return true;
            } else if (itemId == 5) {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).showHomeMoreMenu(v);
                }
                return true;
            } else if (itemId == 6) {
                startTransferMode();
                return true;
            } else if (itemId == 7) {
                showImportTransferCodeDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void startTransferMode() {
        if (SessionContext.selectedClass == null) {
            android.widget.Toast.makeText(requireContext(), "Please select a class first.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (filteredStudents.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "No students to transfer.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        studentAdapter.setMultiSelectMode(true);
        b.fabAddStudent.setVisibility(View.GONE);
        b.fabConfirmTransfer.setVisibility(View.VISIBLE);
        android.widget.Toast.makeText(requireContext(), "Select students to transfer", android.widget.Toast.LENGTH_LONG).show();
    }

    private void showImportTransferCodeDialog() {
        if (SessionContext.selectedClass == null) {
            android.widget.Toast.makeText(requireContext(), "Please select a class first to import students into.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Enter 6-digit code");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setMaxLines(1);
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Import Students")
            .setMessage("Enter the 6-digit transfer code provided by the other teacher.")
            .setView(input)
            .setPositiveButton("Import", (d, w) -> {
                String code = input.getText().toString().trim();
                if (code.length() != 6) {
                    android.widget.Toast.makeText(requireContext(), "Invalid code format", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                processTransferCode(code);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void processTransferCode(String code) {
        b.shimmerViewContainer.setVisibility(View.VISIBLE);
        b.shimmerViewContainer.startShimmer();
        
        // Find transfer request
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("transfer_requests")
            .whereEqualTo("transferCode", code)
            .whereEqualTo("isClaimed", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    b.shimmerViewContainer.stopShimmer();
                    b.shimmerViewContainer.setVisibility(View.GONE);
                    android.widget.Toast.makeText(requireContext(), "Invalid or expired transfer code", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                com.kartik.myschool.model.TransferRequest req = doc.toObject(com.kartik.myschool.model.TransferRequest.class);
                if (req == null || req.studentIds == null || req.studentIds.isEmpty()) {
                    b.shimmerViewContainer.stopShimmer();
                    b.shimmerViewContainer.setVisibility(View.GONE);
                    android.widget.Toast.makeText(requireContext(), "Transfer request is empty or corrupt", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Get current teacher
                FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
                    @Override
                    public void onSuccess(com.kartik.myschool.model.Teacher currentTeacher) {
                        if (currentTeacher == null) {
                            b.shimmerViewContainer.stopShimmer();
                            b.shimmerViewContainer.setVisibility(View.GONE);
                            return;
                        }
                        
                        // Perform batch update
                        com.google.firebase.firestore.WriteBatch batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();
                        
                        for (String sId : req.studentIds) {
                            com.google.firebase.firestore.DocumentReference studentRef = 
                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("students").document(sId);
                            batch.update(studentRef, "teacherId", currentTeacher.id);
                            batch.update(studentRef, "classId", SessionContext.selectedClass.id);
                            batch.update(studentRef, "className", SessionContext.selectedClass.className);
                            batch.update(studentRef, "standard", SessionContext.selectedClass.className);
                            batch.update(studentRef, "division", SessionContext.selectedClass.division);
                            if (SessionContext.selectedSchool != null) {
                                batch.update(studentRef, "schoolId", SessionContext.selectedSchool.id);
                                batch.update(studentRef, "schoolName", SessionContext.selectedSchool.name);
                            }
                        }
                        
                        // Mark code as claimed
                        batch.update(doc.getReference(), "isClaimed", true);
                        
                        batch.commit().addOnSuccessListener(aVoid -> {
                            b.shimmerViewContainer.stopShimmer();
                            b.shimmerViewContainer.setVisibility(View.GONE);
                            android.widget.Toast.makeText(requireContext(), req.studentIds.size() + " students successfully imported!", android.widget.Toast.LENGTH_LONG).show();
                            
                            // Refresh list
                            AppCache.cachedClassIdForStudents = null; // force refresh
                            applySessionFilterIfAny();
                        }).addOnFailureListener(e -> {
                            b.shimmerViewContainer.stopShimmer();
                            b.shimmerViewContainer.setVisibility(View.GONE);
                            android.widget.Toast.makeText(requireContext(), "Failed to import students: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        b.shimmerViewContainer.stopShimmer();
                        b.shimmerViewContainer.setVisibility(View.GONE);
                        android.widget.Toast.makeText(requireContext(), "Failed to get teacher profile", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .addOnFailureListener(e -> {
                b.shimmerViewContainer.stopShimmer();
                b.shimmerViewContainer.setVisibility(View.GONE);
                android.widget.Toast.makeText(requireContext(), "Error finding transfer code: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            });
    }

    private void applySessionFilterIfAny() {
        SessionContext.syncFromAppCache();
        if (SessionContext.selectedClass != null && SessionContext.selectedClass.id != null) {
            // Fix 2: Instant 0ms load if cached persistently
            if (SessionContext.selectedClass.id.equals(AppCache.cachedClassIdForStudents) && AppCache.cachedStudents != null && !AppCache.cachedStudents.isEmpty()) {
                allStudents.clear();
                allStudents.addAll(AppCache.cachedStudents);
                filterStudents(b.etSearch.getText().toString());
                // Continue with network call in background to catch any updates silently
            } else if (SessionContext.selectedClass.id.equals(lastLoadedClassId) && !allStudents.isEmpty()) {
                filterStudents(b.etSearch.getText().toString());
                return;
            }
            
            lastLoadedClassId = SessionContext.selectedClass.id;
            
            b.rvStudents.setVisibility(View.GONE);
            b.shimmerViewContainer.setVisibility(View.VISIBLE);
            b.shimmerViewContainer.startShimmer();
            
            FirebaseRepository.get().getStudentsForClass(SessionContext.selectedClass.id,
                    new FirebaseRepository.OnResult<List<Student>>() {
                        @Override
                        public void onSuccess(List<Student> list) {
                            AppCache.cachedStudents = list;
                            AppCache.cachedClassIdForStudents = SessionContext.selectedClass.id;
                            SessionContext.save(requireContext());
                            
                            allStudents.clear();
                            allStudents.addAll(list);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (b == null) return;
                                    b.shimmerViewContainer.stopShimmer();
                                    b.shimmerViewContainer.setVisibility(View.GONE);
                                    b.rvStudents.setVisibility(View.VISIBLE);
                                    filterStudents(b.etSearch.getText().toString());
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (b != null) {
                                        b.shimmerViewContainer.stopShimmer();
                                        b.shimmerViewContainer.setVisibility(View.GONE);
                                        b.rvStudents.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                            loadAllStudents();
                        }
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
                    if (AppCache.selectedClass == null)
                        return;
                    Intent intent = new Intent(requireContext(), EnterMarksActivity.class);
                    startActivity(intent);
                });
            }

            @Override
            public void onAttendanceClick(Student student, int position) {
                AppCache.selectedStudent = student;
                loadClassForStudent(student, () -> {
                    if (AppCache.selectedClass == null)
                        return;
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
                if (getActivity() != null) {
                    getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
                }
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
                                            if (b == null) return;
                                            allStudents.remove(student);
                                            filteredStudents.remove(student);
                                            studentAdapter.setData(filteredStudents);
                                            b.emptyState.setVisibility(
                                                    filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
                                            b.rvStudents.setVisibility(
                                                    filteredStudents.isEmpty() ? View.GONE : View.VISIBLE);
                                            com.google.android.material.snackbar.Snackbar.make(
                                                    b.getRoot(),
                                                    student.name + " deleted",
                                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                                                    .setAction("UNDO", view -> {
                                                        FirebaseRepository.get().saveStudent(student, new FirebaseRepository.OnResult<String>() {
                                                            @Override
                                                            public void onSuccess(String newId) {
                                                                if (getActivity() != null) {
                                                                    getActivity().runOnUiThread(() -> {
                                                                        if (b == null) return;
                                                                        allStudents.add(student);
                                                                        filteredStudents.add(student);
                                                                        studentAdapter.setData(filteredStudents);
                                                                        b.emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
                                                                        b.rvStudents.setVisibility(filteredStudents.isEmpty() ? View.GONE : View.VISIBLE);
                                                                    });
                                                                }
                                                            }
                                                            @Override
                                                            public void onError(Exception ex) { }
                                                        });
                                                    }).show();
                                        });
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            android.widget.Toast.makeText(requireContext(), "Error: " + e.getMessage(),
                                                    android.widget.Toast.LENGTH_SHORT).show();
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
                b.btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
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
                        FirebaseRepository.get()
                                .getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
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
        
        b.fabConfirmTransfer.setOnClickListener(v -> {
            List<String> selected = studentAdapter.getSelectedStudentIds();
            if (selected.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "No students selected", android.widget.Toast.LENGTH_SHORT).show();
                
                // Cancel mode
                studentAdapter.setMultiSelectMode(false);
                b.fabConfirmTransfer.setVisibility(View.GONE);
                b.fabAddStudent.setVisibility(View.VISIBLE);
                return;
            }
            
            b.fabConfirmTransfer.setEnabled(false);
            
            FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
                @Override
                public void onSuccess(com.kartik.myschool.model.Teacher teacher) {
                    if (teacher == null) return;
                    
                    String code = String.format(java.util.Locale.US, "%06d", new java.util.Random().nextInt(999999));
                    
                    com.kartik.myschool.model.TransferRequest req = new com.kartik.myschool.model.TransferRequest();
                    req.id = java.util.UUID.randomUUID().toString();
                    req.fromTeacherId = teacher.id;
                    req.transferCode = code;
                    req.studentIds = selected;
                    req.createdAt = System.currentTimeMillis();
                    req.isClaimed = false;
                    
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("transfer_requests")
                        .document(req.id)
                        .set(req)
                        .addOnSuccessListener(aVoid -> {
                            if (getActivity() == null) return;
                            b.fabConfirmTransfer.setEnabled(true);
                            studentAdapter.setMultiSelectMode(false);
                            b.fabConfirmTransfer.setVisibility(View.GONE);
                            b.fabAddStudent.setVisibility(View.VISIBLE);
                            
                            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Transfer Code Generated!")
                                .setMessage("Share this 6-digit code with the new teacher:\n\n" + code + "\n\nThey can use the 'Import via Transfer Code' option to claim these " + selected.size() + " students.")
                                .setPositiveButton("OK", null)
                                .setCancelable(false)
                                .show();
                        })
                        .addOnFailureListener(e -> {
                            if (getActivity() == null) return;
                            b.fabConfirmTransfer.setEnabled(true);
                            android.widget.Toast.makeText(requireContext(), "Failed to generate code", android.widget.Toast.LENGTH_SHORT).show();
                        });
                }
                @Override
                public void onError(Exception e) {
                    b.fabConfirmTransfer.setEnabled(true);
                }
            });
        });
    }

    private void openAddStudent() {
        AppCache.selectedStudent = new Student();
        android.content.Intent intent = new android.content.Intent(requireContext(), StudentEditActivity.class)
                .putExtra("new_student", true);

        if (b != null && b.fabAddStudent != null && getActivity() != null) {
            androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat.makeScaleUpAnimation(
                    b.fabAddStudent,
                    b.fabAddStudent.getWidth() / 2,
                    b.fabAddStudent.getHeight() / 2,
                    0,
                    0);
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    private void loadAllStudents() {
        b.emptyState.setVisibility(View.GONE);
        
        // Instant 0ms load if cached persistently
        if ("ALL".equals(AppCache.cachedClassIdForStudents) && AppCache.cachedStudents != null && !AppCache.cachedStudents.isEmpty()) {
            allStudents.clear();
            allStudents.addAll(AppCache.cachedStudents);
            filterStudents(b.etSearch.getText().toString());
            // Continue with background load
        } else if ("ALL".equals(lastLoadedClassId) && !allStudents.isEmpty()) {
            filterStudents(b.etSearch.getText().toString());
            return;
        }
        lastLoadedClassId = "ALL";
        
        FirebaseRepository.get().getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
            @Override
            public void onSuccess(List<Student> list) {
                AppCache.cachedStudents = list;
                AppCache.cachedClassIdForStudents = "ALL";
                SessionContext.save(requireContext());
                
                allStudents.clear();
                allStudents.addAll(list);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (b == null) return;
                        b.shimmerViewContainer.stopShimmer();
                        b.shimmerViewContainer.setVisibility(View.GONE);
                        b.rvStudents.setVisibility(View.VISIBLE);
                        filterStudents(b.etSearch.getText().toString());
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (b != null) {
                            b.shimmerViewContainer.stopShimmer();
                            b.shimmerViewContainer.setVisibility(View.GONE);
                            b.rvStudents.setVisibility(View.GONE);
                            b.emptyState.setVisibility(View.VISIBLE);
                        }
                    });
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
        sortStudents(filteredStudents);
        studentAdapter.setData(filteredStudents);
        boolean isEmpty = filteredStudents.isEmpty();
        b.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        b.rvStudents.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setupSortFilterButton() {
        if (b.btnSortFilter != null) {
            b.btnSortFilter.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), v);
                popup.getMenu().add(0, AppCache.SORT_BY_ROLL_ASC, 0, "Roll Number (Ascending)")
                        .setCheckable(true)
                        .setChecked(AppCache.currentSortType == AppCache.SORT_BY_ROLL_ASC);
                popup.getMenu().add(0, AppCache.SORT_BY_ROLL_DESC, 1, "Roll Number (Descending)")
                        .setCheckable(true)
                        .setChecked(AppCache.currentSortType == AppCache.SORT_BY_ROLL_DESC);
                popup.getMenu().add(0, AppCache.SORT_BY_NAME_ASC, 2, "Name (Ascending)")
                        .setCheckable(true)
                        .setChecked(AppCache.currentSortType == AppCache.SORT_BY_NAME_ASC);
                popup.getMenu().add(0, AppCache.SORT_BY_NAME_DESC, 3, "Name (Descending)")
                        .setCheckable(true)
                        .setChecked(AppCache.currentSortType == AppCache.SORT_BY_NAME_DESC);
                
                popup.setOnMenuItemClickListener(item -> {
                    AppCache.currentSortType = item.getItemId();
                    SessionContext.save(requireContext()); // Persist the new sort order
                    filterStudents(b.etSearch.getText().toString());
                    return true;
                });
                popup.show();
            });
        }
    }

    private void sortStudents(List<Student> list) {
        com.kartik.myschool.utils.StudentSortUtils.sortStudents(list);
    }

    // Bug #7 fix: always clear AppCache.selectedClass before searching to prevent
    // stale data from a previous student leaking into the next navigation action.
    private void loadClassForStudent(Student student, Runnable onComplete) {
        AppCache.selectedClass = null; // clear before search
        FirebaseRepository.get().getClassesForSchool(student.schoolId,
                new FirebaseRepository.OnResult<List<ClassModel>>() {
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
            if (AppCache.selectedClass == null)
                return;
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
                        public void onError(Exception e) {
                        }
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Restore parent top app bar and scrolling behavior:
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.VISIBLE);
            }

            View navHost = ha.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost
                    .getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost
                        .getLayoutParams();
                params.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
                float density = getResources().getDisplayMetrics().density;
                params.bottomMargin = (int) (64 * density);
                navHost.setLayoutParams(params);
            }
        }
        b = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());
        SessionContext.syncFromAppCache();

        // Hide parent top app bar & fix CoordinatorLayout scrolling behavior offset
        // bug:
        if (getActivity() instanceof HomeActivity) {
            HomeActivity ha = (HomeActivity) getActivity();
            View appBar = ha.findViewById(R.id.appBarLayout);
            if (appBar != null) {
                appBar.setVisibility(View.GONE);
            }

            View navHost = ha.findViewById(R.id.navHostFragment);
            if (navHost != null && navHost
                    .getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) navHost
                        .getLayoutParams();
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
            classVal = (SessionContext.selectedClass.className != null && !SessionContext.selectedClass.className.trim().isEmpty()) ? SessionContext.selectedClass.className : "1";
            divVal = (SessionContext.selectedClass.division != null && !SessionContext.selectedClass.division.trim().isEmpty())
                    ? SessionContext.selectedClass.division
                    : "-";
        }

        if (b.tvCustomSubtitle != null) {
            b.tvCustomSubtitle.setText(SessionContext.getClassDivSemSubtitle(requireContext()));
        }

        if (b.tvHeaderSessionInfo != null) {
            b.tvHeaderSessionInfo.setText(SessionContext.getClassDivSemSubtitle(requireContext()));
        }

        if (SessionContext.selectedClass != null) {
            applySessionFilterIfAny();
        } else {
            loadAllStudents();
        }
    }



    private void showExcelOptionsPopupMenu(View v) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 1, 0, R.string.action_export_students);
        popup.getMenu().add(0, 3, 1, "Export UDISE+ / APAAR format");
        popup.getMenu().add(0, 2, 2, R.string.action_import_students);
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                exportStudentsToExcel();
                return true;
            } else if (itemId == 3) {
                com.kartik.myschool.utils.ExportUtils.exportUdiseToExcel(requireContext(), filteredStudents, SessionContext.selectedClass);
                return true;
            } else if (itemId == 2) {
                importStudentsFromExcel();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showInformationSavedMessage() {
        android.widget.Toast.makeText(requireContext(), "All information saved successfully", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void exportStudentsToExcel() {
        if (filteredStudents.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "No students in this class to export.",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append(
                    "Student Name,Standard,Division,Roll No 1,Roll No 2,Gender,Caste,Registration No,Date of Birth,Birth Place,Religion,Blood Group,Mother's Name,Mother's Occupation,Mother's Phone,Father's Name,Father's Occupation,Father's Phone,Address,Bank Name,Account No,Branch,IFSC,Bank UID,Medium,Mother Tongue,Date of Admission,Student Id,UID,Height Sem 1,Weight Sem 1,Height Sem 2,Weight Sem 2\n");

            for (Student s : filteredStudents) {
                sb.append(escapeCsv(s.name != null ? s.name : "")).append(",")
                        .append(escapeCsv(s.standard != null ? s.standard : "")).append(",")
                        .append(escapeCsv(s.division != null ? s.division : "")).append(",")
                        .append(escapeCsv(s.rollNo != null ? s.rollNo : "")).append(",")
                        .append(escapeCsv(s.rollNo2 != null ? s.rollNo2 : "")).append(",")
                        .append(escapeCsv(s.gender != null ? s.gender : "")).append(",")
                        .append(escapeCsv(s.cast != null ? s.cast : "")).append(",")
                        .append(escapeCsv(s.registrationNo != null ? s.registrationNo : "")).append(",")
                        .append(escapeCsv(s.dob != null ? s.dob : "")).append(",")
                        .append(escapeCsv(s.birthPlace != null ? s.birthPlace : "")).append(",")
                        .append(escapeCsv(s.religion != null ? s.religion : "")).append(",")
                        .append(escapeCsv(s.bloodGroup != null ? s.bloodGroup : "")).append(",")
                        .append(escapeCsv(s.motherName != null ? s.motherName : "")).append(",")
                        .append(escapeCsv(s.motherOccupation != null ? s.motherOccupation : "")).append(",")
                        .append(escapeCsv(s.motherPhone != null ? s.motherPhone : "")).append(",")
                        .append(escapeCsv(s.fatherName != null ? s.fatherName : "")).append(",")
                        .append(escapeCsv(s.fatherOccupation != null ? s.fatherOccupation : "")).append(",")
                        .append(escapeCsv(s.fatherPhone != null ? s.fatherPhone : "")).append(",")
                        .append(escapeCsv(s.address != null ? s.address : "")).append(",")
                        .append(escapeCsv(s.bankName != null ? s.bankName : "")).append(",")
                        .append(escapeCsv(s.bankAccount != null ? s.bankAccount : "")).append(",")
                        .append(escapeCsv(s.bankBranch != null ? s.bankBranch : "")).append(",")
                        .append(escapeCsv(s.bankIfsc != null ? s.bankIfsc : "")).append(",")
                        .append(escapeCsv(s.bankUid != null ? s.bankUid : "")).append(",")
                        .append(escapeCsv(s.medium != null ? s.medium : "")).append(",")
                        .append(escapeCsv(s.motherTongue != null ? s.motherTongue : "")).append(",")
                        .append(escapeCsv(s.dateOfAdmission != null ? s.dateOfAdmission : "")).append(",")
                        .append(escapeCsv(s.studentIdNumber != null ? s.studentIdNumber : "")).append(",")
                        .append(escapeCsv(s.uid != null ? s.uid : "")).append(",")
                        .append(escapeCsv(s.heightSem1 != null ? s.heightSem1 : "")).append(",")
                        .append(escapeCsv(s.weightSem1 != null ? s.weightSem1 : "")).append(",")
                        .append(escapeCsv(s.heightSem2 != null ? s.heightSem2 : "")).append(",")
                        .append(escapeCsv(s.weightSem2 != null ? s.weightSem2 : "")).append("\n");
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
            android.widget.Toast
                    .makeText(requireContext(), "Export failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private String escapeCsv(String str) {
        if (str == null)
            return "";
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private int getCasteCategoryIndex(String caste) {
        if (caste == null)
            return 5;
        String c = caste.toUpperCase().trim();
        if (c.contains("SC"))
            return 0;
        if (c.contains("ST"))
            return 1;
        if (c.contains("VJ") || c.contains("विमुक्त"))
            return 2;
        if (c.contains("NT") || c.contains("भटक्या"))
            return 3;
        if (c.contains("OBC"))
            return 4;
        return 5;
    }

    private void importStudentsFromExcel() {
        if (SessionContext.selectedClass == null) {
            android.widget.Toast
                    .makeText(requireContext(), R.string.select_class_first, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        csvPickerLauncher.launch("*/*");
    }

    private void importStudentsFromCsv(android.net.Uri uri) {
        try {
            List<List<String>> rows;
            if (isXlsxFile(uri)) {
                rows = parseXlsx(uri);
            } else {
                rows = parseCsv(uri);
            }

            if (rows == null || rows.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "No student records found in file.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> headers = rows.get(0);
            java.util.Map<String, Integer> headerMap = new java.util.HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                headerMap.put(headers.get(i).toLowerCase().trim(), i);
            }

            int count = rows.size() - 1;
            if (count <= 0) {
                android.widget.Toast.makeText(requireContext(), "No student records found in file.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
                @Override
                public void onSuccess(com.kartik.myschool.model.Teacher teacher) {
                    if (teacher != null && !"active".equals(teacher.subscriptionStatus)) {
                        FirebaseRepository.get().getAllStudentsForTeacher(new FirebaseRepository.OnResult<List<Student>>() {
                            @Override
                            public void onSuccess(List<Student> students) {
                                int currentCount = students != null ? students.size() : 0;
                                if (currentCount + count > 3) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            android.widget.Toast.makeText(requireContext(),
                                                    "Free account limit exceeded. You can only have up to 3 students. Please subscribe to import more.",
                                                    android.widget.Toast.LENGTH_LONG).show();
                                            SubscriptionBottomSheet bottomSheet = new SubscriptionBottomSheet();
                                            bottomSheet.show(getParentFragmentManager(), "SubscriptionBottomSheet");
                                        });
                                    }
                                } else {
                                    proceedWithImport(rows, count, headerMap);
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                proceedWithImport(rows, count, headerMap);
                            }
                        });
                    } else {
                        proceedWithImport(rows, count, headerMap);
                    }
                }

                @Override
                public void onError(Exception e) {
                    proceedWithImport(rows, count, headerMap);
                }
            });
        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(), "Failed to read file: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void proceedWithImport(List<List<String>> rows, int count, java.util.Map<String, Integer> headerMap) {
        try {
            com.kartik.myschool.utils.LoadingDialog pd = new com.kartik.myschool.utils.LoadingDialog(requireContext(), getString(R.string.msg_restoring_students), "Saving student records to Firestore...");
            pd.show();

            ClassModel currentClass = SessionContext.selectedClass;
            School currentSchool = SessionContext.selectedSchool;
            String teacherUid = FirebaseRepository.get().currentUid();

            java.util.concurrent.atomic.AtomicInteger savedCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger failedCount = new java.util.concurrent.atomic.AtomicInteger(0);

            for (int r = 1; r < rows.size(); r++) {
                List<String> cellsList = rows.get(r);
                String[] cells = cellsList.toArray(new String[0]);
                Student s = new Student();

                String parsedId = getCell(cells, new String[] { "id", "ID" }, headerMap, "");
                parsedId = parsedId.trim();
                
                // ALWAYS generate a new Firebase ID to prevent cross-account Permission Denied errors
                s.id = null; 
                
                s.name = getCell(cells, new String[] { "name", "student name", "student full name", "full name", "नाव",
                        "विद्यार्थ्याचे पूर्ण नाव", "studentname", "fullname" }, headerMap, "");
                s.standard = getCell(cells, new String[] { "std", "standard", "मानक", "वर्ग", "class", "std/class" },
                        headerMap, currentClass.className);
                s.division = getCell(cells, new String[] { "div", "division", "विभाग", "विभागणी" }, headerMap,
                        currentClass.division);
                s.rollNo = getCell(cells, new String[] { "roll1", "rollno", "roll no", "roll no 1", "roll no. 1",
                        "roll number", "रोल क्रमांक १", "रोल नंबर", "अनुक्रमांक", "rollnumber" }, headerMap, "");
                s.rollNo2 = getCell(cells,
                        new String[] { "roll2", "rollno2", "roll no 2", "roll no. 2", "रोल क्रमांक २" }, headerMap, "");

                String rawGender = getCell(cells, new String[] { "gender", "sex", "लिंग" }, headerMap, "Male");
                if (rawGender.equalsIgnoreCase("Female") || rawGender.equals("2")
                        || rawGender.equalsIgnoreCase("स्त्री") || rawGender.equalsIgnoreCase("मुलगी")) {
                    s.gender = "Female";
                } else {
                    s.gender = "Male";
                }

                String rawCategory = getCell(cells,
                        new String[] { "category", "cast", "caste", "कास्ट", "जात", "वर्गवारी" }, headerMap, "Open");
                s.cast = getCasteCategoryName(rawCategory);

                s.registrationNo = getCell(cells, new String[] { "reg", "registrationno", "registration no", "reg no",
                        "नोंदणी क्र", "नोंदणी क्रमांक", "नोंदणी क्र.", "registrationnumber" }, headerMap, "");
                s.dob = getCell(cells,
                        new String[] { "dob", "year", "dateofbirth", "date of birth", "जन्मतारीख", "जन्म तारीख" },
                        headerMap, "");
                s.motherName = getCell(cells,
                        new String[] { "mothername", "mother's name", "mother name", "आईचे नाव", "mother_name" },
                        headerMap, "");
                s.motherOccupation = getCell(cells,
                        new String[] { "motheroccupation", "mother's occupation", "mother occupation", "आईचा व्यवसाय" },
                        headerMap, "");
                s.motherPhone = getCell(cells,
                        new String[] { "motherphone", "mother's phone", "mother phone", "आईचा फोन" }, headerMap, "");
                s.fatherName = getCell(cells, new String[] { "fathername", "fname", "father's name", "father name",
                        "वडिलांचे नाव", "fname", "father_name" }, headerMap, "");
                s.fatherOccupation = getCell(cells, new String[] { "fatheroccupation", "fwork", "father's occupation",
                        "father occupation", "वडिलांचा व्यवसाय", "fwork", "occupation" }, headerMap, "");
                s.fatherPhone = getCell(cells, new String[] { "fatherphone", "fphone", "father's phone", "father phone",
                        "वडिलांचा फोन", "fphone", "phone" }, headerMap, "");
                s.address = getCell(cells, new String[] { "address", "home address", "पत्ता" }, headerMap, "");
                s.bankName = getCell(cells, new String[] { "bankname", "bank name", "बँकेचे नाव", "bank_name" }, headerMap, "");
                s.bankAccount = getCell(
                        cells, new String[] { "bankaccount", "account", "account no", "account number", "bank account",
                                "bank account no", "खाते क्र", "खाते क्रमांक", "खाते क्र.", "account_no" },
                        headerMap, "");
                s.bankBranch = getCell(cells,
                        new String[] { "bankbranch", "branch", "bank branch", "शाखा", "bank_branch" }, headerMap, "");
                s.bankIfsc = getCell(cells, new String[] { "bankifsc", "ifsc", "ifsc code", "bank ifsc", "ifsc_code" },
                        headerMap, "");
                s.bankUid = getCell(cells, new String[] { "bankuid", "bank uid", "बँक uid", "bank_uid" }, headerMap,
                        "");
                s.medium = getCell(cells, new String[] { "medium", "मध्यम" }, headerMap, "");
                s.motherTongue = getCell(cells,
                        new String[] { "mothertongue", "tongue", "mother tongue", "मातृभाषा", "mother_tongue" }, headerMap, "");
                s.dateOfAdmission = getCell(cells, new String[] { "dateofadmission", "admissiondate",
                        "date of admission", "date_of_admission", "प्रवेशाची तारीख", "प्रवेश तारीख" }, headerMap, "");
                s.studentIdNumber = getCell(cells, new String[] { "studentidnumber", "studentid", "student id",
                        "student id number", "student_id", "saralid", "saral id", "saral_id", "विद्यार्थी आयडी", "विद्यार्थी आयडी क्र" }, headerMap, "");
                
                // If the 'id' column was a custom ID, use it for studentIdNumber
                if (s.id == null && !parsedId.isEmpty()) {
                    if (s.studentIdNumber == null || s.studentIdNumber.isEmpty()) {
                        s.studentIdNumber = parsedId;
                    }
                }

                s.uid = getCell(cells, new String[] { "uid", "aadhar", "aadhar no", "aadhar number", "uid",
                        "आधार कार्ड", "आधार नंबर", "आधार क्रमांक" }, headerMap, "");
                s.birthPlace = getCell(cells, new String[] { "birthplace", "birth place", "जन्मस्थान", "जन्माचे ठिकाण" }, headerMap, "");
                s.religion = getCell(cells, new String[] { "religion", "धर्म" }, headerMap, "");
                s.bloodGroup = getCell(cells, new String[] { "blood", "bloodgroup", "blood group", "रक्तगट" }, headerMap, "");
                s.heightSem1 = getCell(cells, new String[] { "height1", "heightsem1", "height sem 1", "उंची १", "height_sem_1" }, headerMap, "");
                s.weightSem1 = getCell(cells, new String[] { "weight1", "weightsem1", "weight sem 1", "वजन १", "weight_sem_1" }, headerMap, "");
                s.heightSem2 = getCell(cells, new String[] { "height2", "heightsem2", "height sem 2", "उंची २", "height_sem_2" }, headerMap, "");
                s.weightSem2 = getCell(cells, new String[] { "weight2", "weightsem2", "weight sem 2", "वजन २", "weight_sem_2" }, headerMap, "");

                s.parentName = s.fatherName != null && !s.fatherName.isEmpty() ? s.fatherName
                        : (s.motherName != null ? s.motherName : "");

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
                        android.util.Log.e("IMPORT_ERROR", "Failed to save student: " + s.name + " - " + e.getMessage(), e);
                        int fVal = failedCount.incrementAndGet();
                        checkImportDone(savedCount.get(), fVal, count, pd);
                    }
                });
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(), "Failed to save student data: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private String getCell(String[] cells, String[] aliases, java.util.Map<String, Integer> headerMap,
            String fallback) {
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

    private void checkImportDone(int saved, int failed, int total, com.kartik.myschool.utils.LoadingDialog pd) {
        if (saved + failed == total) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (pd.isShowing())
                        pd.dismiss();
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
        if (categoryStr == null || categoryStr.isEmpty())
            return "Open";
        try {
            int index = Integer.parseInt(categoryStr.trim());
            switch (index) {
                case 0:
                case 1:
                    return "SC (Scheduled Castes)";
                case 2:
                    return "ST (Scheduled Tribes)";
                case 3:
                    return "VJ (Vimukt Jati)";
                case 4:
                    return "NT (Nomadic Tribes)";
                case 5:
                    return "OBC (Other Backward Classes)";
                case 6:
                    return "SBC (Special Backward Class)";
                case 7:
                default:
                    return "Open";
            }
        } catch (NumberFormatException e) {
            String upper = categoryStr.toUpperCase().trim();
            if (upper.equals("SC") || upper.contains("SCHEDULED CASTES") || upper.contains("अनुसूचित जाती")) {
                return "SC (Scheduled Castes)";
            } else if (upper.equals("ST") || upper.contains("SCHEDULED TRIBES") || upper.contains("अनुसूचित जमाती")) {
                return "ST (Scheduled Tribes)";
            } else if (upper.equals("VJ") || upper.contains("VIMUKT JATI") || upper.contains("VIMUKT")
                    || upper.contains("विमुक्त")) {
                return "VJ (Vimukt Jati)";
            } else if (upper.equals("NT") || upper.contains("NOMADIC TRIBES") || upper.contains("BHATKYA")
                    || upper.contains("भटक्या")) {
                return "NT (Nomadic Tribes)";
            } else if (upper.equals("OBC") || upper.contains("OTHER BACKWARD CLASSES") || upper.contains("SBC")
                    || upper.contains("इतर मागास")) {
                return "OBC (Other Backward Classes)";
            } else if (upper.equalsIgnoreCase("Open") || upper.equalsIgnoreCase("General") || upper.contains("खुला")) {
                return "Open";
            }
            return categoryStr;
        }
    }

    private boolean isXlsxFile(android.net.Uri uri) {
        try (java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) return false;
            byte[] signature = new byte[4];
            int read = is.read(signature);
            if (read == 4) {
                return signature[0] == 0x50 && signature[1] == 0x4B && signature[2] == 0x03 && signature[3] == 0x04;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private List<List<String>> parseCsv(android.net.Uri uri) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        try (java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) throw new java.io.IOException("Unable to open input stream");
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty())
                        continue;
                    rows.add(parseCsvLine(line));
                }
            }
        }
        return rows;
    }

    private List<List<String>> parseXlsx(android.net.Uri uri) throws Exception {
        List<String> sharedStrings = new ArrayList<>();
        // Pass 1: Extract shared strings
        try (java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) throw new java.io.IOException("Unable to open input stream");
            try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(is)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if ("xl/sharedStrings.xml".equals(entry.getName())) {
                        org.xmlpull.v1.XmlPullParser parser = android.util.Xml.newPullParser();
                        parser.setInput(zip, "UTF-8");
                        int eventType = parser.getEventType();
                        StringBuilder currentString = new StringBuilder();
                        boolean insideT = false;
                        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                            String name = parser.getName();
                            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                                if ("si".equals(name)) {
                                    currentString.setLength(0);
                                } else if ("t".equals(name)) {
                                    insideT = true;
                                }
                            } else if (eventType == org.xmlpull.v1.XmlPullParser.TEXT) {
                                if (insideT) {
                                    currentString.append(parser.getText());
                                }
                            } else if (eventType == org.xmlpull.v1.XmlPullParser.END_TAG) {
                                if ("t".equals(name)) {
                                    insideT = false;
                                } else if ("si".equals(name)) {
                                    sharedStrings.add(currentString.toString());
                                }
                            }
                            eventType = parser.next();
                        }
                        break;
                    }
                }
            }
        }

        // Pass 2: Extract sheet rows
        List<List<String>> rows = new ArrayList<>();
        try (java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) throw new java.io.IOException("Unable to open input stream");
            try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(is)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if ("xl/worksheets/sheet1.xml".equals(entry.getName())) {
                        org.xmlpull.v1.XmlPullParser parser = android.util.Xml.newPullParser();
                        parser.setInput(zip, "UTF-8");
                        int eventType = parser.getEventType();
                        List<String> currentRow = null;
                        String cellRef = null;
                        String cellType = null;
                        StringBuilder cellValue = new StringBuilder();
                        boolean insideV = false;
                        
                        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                            String name = parser.getName();
                            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                                if ("row".equals(name)) {
                                    currentRow = new ArrayList<>();
                                } else if ("c".equals(name)) {
                                    cellRef = parser.getAttributeValue(null, "r"); // e.g. "A1"
                                    cellType = parser.getAttributeValue(null, "t"); // e.g. "s"
                                    cellValue.setLength(0);
                                } else if ("v".equals(name) || "t".equals(name)) {
                                    insideV = true;
                                }
                            } else if (eventType == org.xmlpull.v1.XmlPullParser.TEXT) {
                                if (insideV) {
                                    cellValue.append(parser.getText());
                                }
                            } else if (eventType == org.xmlpull.v1.XmlPullParser.END_TAG) {
                                if ("v".equals(name) || "t".equals(name)) {
                                    insideV = false;
                                } else if ("c".equals(name)) {
                                    if (currentRow != null && cellRef != null) {
                                        String colLetter = cellRef.replaceAll("[0-9]", "");
                                        int colIndex = columnLetterToIndex(colLetter);
                                        while (currentRow.size() <= colIndex) {
                                            currentRow.add("");
                                        }
                                        String value = cellValue.toString();
                                        if ("s".equals(cellType)) {
                                            try {
                                                int idx = Integer.parseInt(value);
                                                if (idx >= 0 && idx < sharedStrings.size()) {
                                                    value = sharedStrings.get(idx);
                                                }
                                            } catch (NumberFormatException ignored) {}
                                        } else {
                                            // Numeric cell value cleanup
                                            if (value.contains(".") || value.contains("E") || value.contains("e")) {
                                                try {
                                                    double d = Double.parseDouble(value);
                                                    if (d == (long) d) {
                                                        value = String.valueOf((long) d);
                                                    } else {
                                                        value = String.valueOf(d);
                                                    }
                                                } catch (NumberFormatException ignored) {}
                                            }
                                        }
                                        currentRow.set(colIndex, value);
                                    }
                                } else if ("row".equals(name)) {
                                    if (currentRow != null) {
                                        rows.add(currentRow);
                                    }
                                }
                            }
                            eventType = parser.next();
                        }
                        break;
                    }
                }
            }
        }
        return rows;
    }

    private int columnLetterToIndex(String letter) {
        int index = 0;
        for (int i = 0; i < letter.length(); i++) {
            index = index * 26 + (letter.toUpperCase().charAt(i) - 'A' + 1);
        }
        return index - 1;
    }
}
