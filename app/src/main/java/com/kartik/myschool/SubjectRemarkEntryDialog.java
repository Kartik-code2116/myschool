package com.kartik.myschool;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.chip.Chip;
import com.kartik.myschool.databinding.DialogSubjectRemarkEntryBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import android.widget.TextView;
import android.widget.CheckBox;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class SubjectRemarkEntryDialog extends DialogFragment {

    public interface OnRemarkSavedListener {
        void onRemarkSaved(String studentId, MarksRecord record);
    }

    private DialogSubjectRemarkEntryBinding b;
    private Student student;
    private ClassModel classModel;
    private MarksRecord existingMarks;
    private String subjectName;
    private int subjectIndex;
    private final List<String> bankOptions = new ArrayList<>();
    private final LinkedHashSet<String> selectedRemarks = new LinkedHashSet<>();
    private OnRemarkSavedListener saveListener;

    public static SubjectRemarkEntryDialog newInstance(Student student, Subject subject, int subjectIndex, ClassModel classModel, MarksRecord existingMarks) {
        SubjectRemarkEntryDialog dialog = new SubjectRemarkEntryDialog();
        dialog.student = student;
        dialog.classModel = classModel;
        dialog.subjectName = subject != null ? subject.name : null;
        dialog.subjectIndex = subjectIndex;
        dialog.existingMarks = existingMarks;
        return dialog;
    }

    public void setOnRemarkSavedListener(OnRemarkSavedListener listener) {
        this.saveListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.SingleSubjectMarksDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = DialogSubjectRemarkEntryBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (student == null || classModel == null || subjectName == null || subjectName.trim().isEmpty()) {
            Toast.makeText(getContext(), "Missing student or subject details.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        bindHeader();
        loadExistingSelection();
        renderSelection();
        loadRemarkBank();

        b.btnChooseRemarks.setOnClickListener(v -> showRemarkDialog());
        b.btnSaveRemark.setOnClickListener(v -> saveSubjectRemark());
        b.btnCancel.setOnClickListener(v -> dismiss());
        b.btnBack.setOnClickListener(v -> dismiss());
        b.dialogRoot.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
    }

    private void bindHeader() {
        String name = student.name != null ? student.name : "Student";
        String roll = student.rollNo != null ? student.rollNo : "-";
        
        b.tvDialogTitle.setText("Remarks: " + subjectName);
        b.tvStudentName.setText("Student: " + name + " (Roll: " + roll + ")\nClass: " + classModel.getDisplayName());
    }

    private void loadExistingSelection() {
        MarksRecord.SubjectMarksDetail detail = getCurrentSubjectDetail(false);
        if (detail == null || detail.remark == null || detail.remark.trim().isEmpty()) {
            return;
        }
        for (String part : detail.remark.split("\\|\\|")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                selectedRemarks.add(trimmed);
            }
        }
    }

    private void loadRemarkBank() {
        String schoolId = classModel.schoolId != null ? classModel.schoolId
                : SessionContext.selectedSchool != null ? SessionContext.selectedSchool.id : null;
        
        String className = classModel.className != null ? classModel.className : "5";
        int semesterNumber = SessionContext.selectedSemester != null ? SessionContext.selectedSemester.number : 1;
        
        String cacheKey = className + "_sem_" + semesterNumber + "_" + subjectName;
        List<String> cached = AppCache.cachedRemarkBank.get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            bankOptions.clear();
            bankOptions.addAll(cached);
            renderSelection();
            return;
        }

        FirebaseRepository.get().getRemarkBank(schoolId, className, semesterNumber, subjectName,
                new FirebaseRepository.OnResult<List<String>>() {
                    @Override
                    public void onSuccess(List<String> options) {
                        bankOptions.clear();
                        if (options != null) {
                            bankOptions.addAll(options);
                            AppCache.cachedRemarkBank.put(cacheKey, new ArrayList<>(options));
                        }
                        renderSelection();
                    }

                    @Override
                    public void onError(Exception e) {
                        bankOptions.clear();
                        bankOptions.addAll(com.kartik.myschool.model.RemarkBank.defaultOptionsFor(subjectName, semesterNumber));
                        renderSelection();
                    }
                });
    }

    private void showRemarkDialog() {
        if (bankOptions.isEmpty()) {
            int semesterNumber = com.kartik.myschool.SessionContext.selectedSemester != null ? com.kartik.myschool.SessionContext.selectedSemester.number : 1;
            bankOptions.addAll(com.kartik.myschool.model.RemarkBank.defaultOptionsFor(subjectName, semesterNumber));
        }

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_categorized_remarks, null);
        
        RecyclerView rv = sheetView.findViewById(R.id.rvRemarks);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        List<Object> items = new ArrayList<>();
        Map<String, List<String>> categorized = new LinkedHashMap<>();
        List<String> uncategorized = new ArrayList<>();
        
        for (String option : bankOptions) {
            if (option.startsWith("[") && option.contains("]")) {
                int endBracket = option.indexOf("]");
                String cat = option.substring(1, endBracket).trim();
                String text = option.substring(endBracket + 1).trim();
                if (!categorized.containsKey(cat)) {
                    categorized.put(cat, new ArrayList<>());
                }
                categorized.get(cat).add(text);
            } else {
                uncategorized.add(option);
            }
        }
        
        for (Map.Entry<String, List<String>> entry : categorized.entrySet()) {
            items.add(new CategoryHeader(entry.getKey()));
            for (String val : entry.getValue()) {
                items.add(new RemarkItem(val, entry.getKey()));
            }
        }
        if (!uncategorized.isEmpty()) {
            items.add(new CategoryHeader("Other"));
            for (String val : uncategorized) {
                items.add(new RemarkItem(val, null));
            }
        }

        CategorizedRemarkAdapter adapter = new CategorizedRemarkAdapter(items, selectedRemarks, () -> {
            renderSelection();
        });
        rv.setAdapter(adapter);

        sheet.setContentView(sheetView);
        sheet.show();
    }

    private static class CategoryHeader {
        String title;
        CategoryHeader(String title) { this.title = title; }
    }

    private static class RemarkItem {
        String text;
        String category;
        RemarkItem(String text, String category) { this.text = text; this.category = category; }
        String getFullText() {
            return category != null ? "[" + category + "] " + text : text;
        }
    }

    private static class CategorizedRemarkAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        private List<Object> items;
        private LinkedHashSet<String> selected;
        private Runnable onChange;

        CategorizedRemarkAdapter(List<Object> items, LinkedHashSet<String> selected, Runnable onChange) {
            this.items = items;
            this.selected = selected;
            this.onChange = onChange;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof CategoryHeader ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_remark_category_header, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selectable_remark, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_HEADER) {
                CategoryHeader header = (CategoryHeader) items.get(position);
                TextView tv = holder.itemView.findViewById(R.id.tvCategoryName);
                tv.setText(header.title);
            } else {
                RemarkItem item = (RemarkItem) items.get(position);
                TextView tv = holder.itemView.findViewById(R.id.tvRemarkText);
                CheckBox cb = holder.itemView.findViewById(R.id.cbRemark);
                
                String fullText = item.getFullText();
                tv.setText(item.text);
                cb.setChecked(selected.contains(fullText));
                
                holder.itemView.setOnClickListener(v -> {
                    if (selected.contains(fullText)) {
                        selected.remove(fullText);
                        cb.setChecked(false);
                    } else {
                        selected.add(fullText);
                        cb.setChecked(true);
                    }
                    if (onChange != null) onChange.run();
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private void renderSelection() {
        if (getContext() == null || b == null) return;
        b.cgSelectedRemarks.removeAllViews();
        if (selectedRemarks.isEmpty()) {
            b.tvSelectedSummary.setText("No remarks selected");
        } else {
            b.tvSelectedSummary.setText(selectedRemarks.size() + " remark(s) selected");
        }

        for (String remark : selectedRemarks) {
            Chip chip = new Chip(requireContext());
            chip.setText(remark);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                selectedRemarks.remove(remark);
                renderSelection();
            });
            chip.setEnsureMinTouchTargetSize(false);
            b.cgSelectedRemarks.addView(chip);
        }
    }

    private void saveSubjectRemark() {
        String custom = b.etCustomRemark.getText() != null ? b.etCustomRemark.getText().toString().trim() : "";
        LinkedHashSet<String> finalRemarks = new LinkedHashSet<>(selectedRemarks);
        if (!custom.isEmpty()) {
            finalRemarks.add(custom);
        }

        MarksRecord record = existingMarks != null ? existingMarks : new MarksRecord();
        record.studentId = student.id;
        record.classId = classModel.id;
        record.examName = classModel.examName;
        if (record.subjectMarks == null) record.subjectMarks = new java.util.HashMap<>();
        if (record.subjectMax == null) record.subjectMax = new java.util.HashMap<>();
        if (record.detailedMarks == null) record.detailedMarks = new java.util.HashMap<>();

        if (SessionContext.selectedSemester != null && SessionContext.selectedSemester.id != null) {
            record.semesterId = SessionContext.selectedSemester.id;
            record.semesterNumber = String.valueOf(SessionContext.selectedSemester.number);
        } else {
            record.semesterId = classModel.semesterId != null ? classModel.semesterId : "sem_1";
            record.semesterNumber = record.semesterId.contains("2") ? "2" : "1";
        }

        MarksRecord.SubjectMarksDetail detail = getCurrentSubjectDetail(true, record);
        detail.remark = joinRemarks(finalRemarks);
        String safeKey = MarksRecord.sanitizeKey(subjectName);
        record.detailedMarks.put(safeKey, detail);

        showLoading(true);
        FirebaseRepository.get().saveMarks(record, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String id) {
                record.id = id;
                existingMarks = record;
                AppCache.selectedMarks = record;
                if (getActivity() != null) {
                    getActivity().getSharedPreferences("marks_doc_ids", Context.MODE_PRIVATE)
                            .edit()
                            .putString(getMarksDocPrefKey(record), id)
                            .putString("marks_doc_" + record.studentId + "_" + record.classId, id)
                            .apply();
                }

                AppCache.descriptiveJustSaved = true;
                AppCache.descriptiveJustSavedStudentId = student.id;
                AppCache.descriptiveJustSavedRecord = record;
                syncCaches(record);
                FirebaseRepository.get().updateMarksInCache(record.classId, record.semesterId, student.id, record);

                if (saveListener != null) {
                    saveListener.onRemarkSaved(student.id, record);
                }

                showLoading(false);
                Toast.makeText(getContext(), "Remark saved successfully.", Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private MarksRecord.SubjectMarksDetail getCurrentSubjectDetail(boolean create) {
        return getCurrentSubjectDetail(create, existingMarks);
    }

    private MarksRecord.SubjectMarksDetail getCurrentSubjectDetail(boolean create, MarksRecord record) {
        if (record == null || record.detailedMarks == null) {
            return create ? newDetailForSubject() : null;
        }

        String safeKey = MarksRecord.sanitizeKey(subjectName);
        MarksRecord.SubjectMarksDetail detail = record.detailedMarks.get(safeKey);
        if (detail != null) return detail;

        detail = record.detailedMarks.get(subjectName);
        if (detail != null) return detail;

        for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : record.detailedMarks.entrySet()) {
            if (entry.getKey() != null && Objects.equals(MarksRecord.sanitizeKey(entry.getKey()), safeKey)) {
                return entry.getValue();
            }
        }
        return create ? newDetailForSubject() : null;
    }

    private MarksRecord.SubjectMarksDetail newDetailForSubject() {
        MarksRecord.SubjectMarksDetail detail = new MarksRecord.SubjectMarksDetail();
        if (classModel.subjects != null && subjectIndex >= 0 && subjectIndex < classModel.subjects.size()) {
            Subject subject = classModel.subjects.get(subjectIndex);
            if (subject != null) {
                detail.maxMarks = subject.maxMarks;
            }
        }
        return detail;
    }

    private String joinRemarks(LinkedHashSet<String> remarks) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String remark : remarks) {
            if (i > 0) sb.append("||");
            sb.append(remark);
            i++;
        }
        return sb.toString();
    }

    private String getMarksDocPrefKey(MarksRecord record) {
        return "marks_doc_" + record.studentId + "_" + record.classId + "_" + record.semesterId;
    }

    private void syncCaches(MarksRecord record) {
        if (AppCache.cachedDescriptiveMarksMap == null
                || !Objects.equals(record.classId, AppCache.cachedDescriptiveClassId)
                || !Objects.equals(record.semesterId, AppCache.cachedDescriptiveSemesterId)) {
            AppCache.cachedDescriptiveMarksMap = new java.util.HashMap<>();
            AppCache.cachedDescriptiveClassId = record.classId;
            AppCache.cachedDescriptiveSemesterId = record.semesterId;
            AppCache.cachedDescriptiveMarksComplete = true;
        }
        AppCache.cachedDescriptiveMarksMap.put(student.id, record);

        if (AppCache.cachedMarksMap == null) {
            AppCache.cachedMarksMap = new java.util.HashMap<>();
        }
        AppCache.cachedMarksMap.put(student.id, record);
        AppCache.cachedClassIdForStudents = record.classId;
        AppCache.cachedSemesterIdForMarks = record.semesterId;
    }

    private void showLoading(boolean show) {
        if (b == null) return;
        b.progress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSaveRemark.setEnabled(!show);
        b.btnChooseRemarks.setEnabled(!show);
        b.btnCancel.setEnabled(!show);
        b.btnBack.setEnabled(!show);
    }
}
