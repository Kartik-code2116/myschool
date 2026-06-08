package com.kartik.myschool;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.kartik.myschool.databinding.DialogSingleSubjectMarksBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.repository.FirebaseRepository;
import com.kartik.myschool.utils.GradeCalculator;

public class SingleSubjectMarksDialog extends DialogFragment {

    public interface OnMarksSavedListener {
        void onMarksSaved(String studentId, MarksRecord record);
    }

    private DialogSingleSubjectMarksBinding b;
    private Student student;
    private Subject subject;
    private ClassModel classModel;
    private MarksRecord existingMarks;
    private OnMarksSavedListener saveListener;

    private int akarikMax = 50;
    private int sanklitMax = 50;
    private int nirikhshanMax, tondiKamMax, pratyakshikAMax, upkramMax, prakalpMax, chachaniMax, swadhyayMax, itarMax;
    private int tondiBMax, pratyakshikBMax, lekhiMax;
    private int totalMax;

    public static SingleSubjectMarksDialog newInstance(Student student, Subject subject, ClassModel classModel, MarksRecord existingMarks) {
        SingleSubjectMarksDialog dialog = new SingleSubjectMarksDialog();
        dialog.student = student;
        dialog.subject = subject;
        dialog.classModel = classModel;
        dialog.existingMarks = existingMarks;
        return dialog;
    }

    public void setOnMarksSavedListener(OnMarksSavedListener listener) {
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
        b = DialogSingleSubjectMarksBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (student == null || subject == null || classModel == null) {
            dismiss();
            return;
        }

        setupUIInfo();
        computeMaxMarks();
        setupMaxLabels();
        setupValidators();
        fillExistingValues();
        setupTextWatchers();

        b.btnBack.setOnClickListener(v -> dismiss());
        b.btnCancel.setOnClickListener(v -> dismiss());
        b.btnClear.setOnClickListener(v -> clearFields());
        b.btnSave.setOnClickListener(v -> saveSubjectMarks());
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

    private void setupUIInfo() {
        b.tvDialogTitle.setText("Edit Marks: " + subject.name);
        b.tvStudentName.setText("Student: " + student.name + " (Roll: " + student.rollNo + ")");
    }

    private void computeMaxMarks() {
        nirikhshanMax = subject.maxNirikhshan;
        tondiKamMax = subject.maxTondiKam;
        pratyakshikAMax = subject.maxPratyakshik;
        upkramMax = subject.maxUpkram;
        prakalpMax = subject.maxPrakalp;
        chachaniMax = subject.maxChachani;
        swadhyayMax = subject.maxSwadhyay;
        itarMax = subject.maxItar;

        tondiBMax = subject.maxTondi;
        pratyakshikBMax = subject.maxPratyakshikB;
        lekhiMax = subject.maxLekhi;

        // Fallback scaling to 50-50 if max sub-fields are not initialized
        if (nirikhshanMax == 0 && tondiKamMax == 0 && pratyakshikAMax == 0 &&
                upkramMax == 0 && prakalpMax == 0 && chachaniMax == 0 && swadhyayMax == 0 &&
                tondiBMax == 0 && pratyakshikBMax == 0 && lekhiMax == 0) {

            int aMax = subject.maxMarks / 2;
            int sMax = subject.maxMarks - aMax;

            nirikhshanMax = aMax * 10 / 50;
            tondiKamMax = aMax * 10 / 50;
            pratyakshikAMax = aMax * 10 / 50;
            upkramMax = aMax * 5 / 50;
            prakalpMax = aMax * 5 / 50;
            chachaniMax = aMax * 5 / 50;
            swadhyayMax = aMax * 5 / 50;
            itarMax = aMax - nirikhshanMax - tondiKamMax - pratyakshikAMax
                    - upkramMax - prakalpMax - chachaniMax - swadhyayMax;

            tondiBMax = sMax * 10 / 50;
            pratyakshikBMax = sMax * 10 / 50;
            lekhiMax = sMax - tondiBMax - pratyakshikBMax;
        }

        akarikMax = nirikhshanMax + tondiKamMax + pratyakshikAMax + upkramMax + prakalpMax + chachaniMax
                + swadhyayMax + itarMax;
        sanklitMax = tondiBMax + pratyakshikBMax + lekhiMax;
        totalMax = akarikMax + sanklitMax;
    }

    private void setupMaxLabels() {
        b.tvAkarikMax.setText("Max: " + akarikMax);
        b.tvSanklitMax.setText("Max: " + sanklitMax);

        b.tvNirikhshanMax.setText("/" + nirikhshanMax);
        b.tvTondiKamMax.setText("/" + tondiKamMax);
        b.tvPratyakshikAMax.setText("/" + pratyakshikAMax);
        b.tvUpkramMax.setText("/" + upkramMax);
        b.tvPrakalpMax.setText("/" + prakalpMax);
        b.tvChachaniMax.setText("/" + chachaniMax);
        b.tvSwadhyayMax.setText("/" + swadhyayMax);
        b.tvItarMax.setText(itarMax > 0 ? "/" + itarMax : "/—");

        b.tvTondiBMax.setText("/" + tondiBMax);
        b.tvPratyakshikBMax.setText("/" + pratyakshikBMax);
        b.tvLekhiMax.setText("/" + lekhiMax);
    }

    private void setupValidators() {
        setupMaxValidation(b.etNirikhshan, nirikhshanMax);
        setupMaxValidation(b.etTondiKam, tondiKamMax);
        setupMaxValidation(b.etPratyakshik, pratyakshikAMax);
        setupMaxValidation(b.etUpkram, upkramMax);
        setupMaxValidation(b.etPrakalp, prakalpMax);
        setupMaxValidation(b.etChachani, chachaniMax);
        setupMaxValidation(b.etSwadhyay, swadhyayMax);
        setupMaxValidation(b.etItar, itarMax);

        setupMaxValidation(b.etTondiB, tondiBMax);
        setupMaxValidation(b.etPratyakshikB, pratyakshikBMax);
        setupMaxValidation(b.etLekhi, lekhiMax);
    }

    private void setupMaxValidation(EditText et, int max) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s == null || s.toString().isEmpty()) return;
                try {
                    int val = Integer.parseInt(s.toString());
                    if (val > max) {
                        et.setError("Max marks is " + max);
                        et.setText(String.valueOf(max));
                        et.setSelection(et.getText().length());
                    }
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private void fillExistingValues() {
        if (existingMarks == null || existingMarks.detailedMarks == null) {
            updateTotals();
            return;
        }

        String safeKey = MarksRecord.sanitizeKey(subject.name);
        MarksRecord.SubjectMarksDetail d = existingMarks.detailedMarks.get(safeKey);
        if (d != null) {
            if (d.nirikhshan > 0) b.etNirikhshan.setText(String.valueOf(d.nirikhshan));
            if (d.tondiKam > 0)   b.etTondiKam.setText(String.valueOf(d.tondiKam));
            if (d.pratyakshik > 0) b.etPratyakshik.setText(String.valueOf(d.pratyakshik));
            if (d.upkram > 0)     b.etUpkram.setText(String.valueOf(d.upkram));
            if (d.prakalp > 0)    b.etPrakalp.setText(String.valueOf(d.prakalp));
            if (d.chachani > 0)   b.etChachani.setText(String.valueOf(d.chachani));
            if (d.swadhyay > 0)   b.etSwadhyay.setText(String.valueOf(d.swadhyay));
            if (d.itar > 0)       b.etItar.setText(String.valueOf(d.itar));

            if (d.tondi > 0)      b.etTondiB.setText(String.valueOf(d.tondi));
            if (d.pratyakshikB > 0) b.etPratyakshikB.setText(String.valueOf(d.pratyakshikB));
            if (d.lekhi > 0)      b.etLekhi.setText(String.valueOf(d.lekhi));
        }
        updateTotals();
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotals();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        b.etNirikhshan.addTextChangedListener(watcher);
        b.etTondiKam.addTextChangedListener(watcher);
        b.etPratyakshik.addTextChangedListener(watcher);
        b.etUpkram.addTextChangedListener(watcher);
        b.etPrakalp.addTextChangedListener(watcher);
        b.etChachani.addTextChangedListener(watcher);
        b.etSwadhyay.addTextChangedListener(watcher);
        b.etItar.addTextChangedListener(watcher);

        b.etTondiB.addTextChangedListener(watcher);
        b.etPratyakshikB.addTextChangedListener(watcher);
        b.etLekhi.addTextChangedListener(watcher);
    }

    private void updateTotals() {
        int nirikhshan = getInt(b.etNirikhshan);
        int tondiKam = getInt(b.etTondiKam);
        int pratyakshik = getInt(b.etPratyakshik);
        int upkram = getInt(b.etUpkram);
        int prakalp = getInt(b.etPrakalp);
        int chachani = getInt(b.etChachani);
        int swadhyay = getInt(b.etSwadhyay);
        int itar = getInt(b.etItar);

        int akarikTotal = nirikhshan + tondiKam + pratyakshik + upkram + prakalp + chachani + swadhyay + itar;
        b.tvAkarikTotal.setText(akarikTotal + " / " + akarikMax);

        int tondi = getInt(b.etTondiB);
        int pratyakshikB = getInt(b.etPratyakshikB);
        int lekhi = getInt(b.etLekhi);

        int sanklitTotal = tondi + pratyakshikB + lekhi;
        b.tvSanklitTotal.setText(sanklitTotal + " / " + sanklitMax);

        int grandTotal = akarikTotal + sanklitTotal;
        b.tvGrandTotal.setText(grandTotal + " / " + totalMax);
    }

    private int getInt(EditText et) {
        String val = et.getText().toString().trim();
        if (val.isEmpty()) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void clearFields() {
        b.etNirikhshan.setText("");
        b.etTondiKam.setText("");
        b.etPratyakshik.setText("");
        b.etUpkram.setText("");
        b.etPrakalp.setText("");
        b.etChachani.setText("");
        b.etSwadhyay.setText("");
        b.etItar.setText("");

        b.etTondiB.setText("");
        b.etPratyakshikB.setText("");
        b.etLekhi.setText("");
        updateTotals();
        Toast.makeText(getContext(), "Fields cleared", Toast.LENGTH_SHORT).show();
    }

    private void saveSubjectMarks() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), R.string.msg_empty_1, Toast.LENGTH_LONG).show();
            return;
        }

        MarksRecord m = existingMarks != null ? existingMarks : new MarksRecord();
        m.studentId = student.id;
        m.classId = classModel.id;
        m.examName = classModel.examName;

        if (m.subjectMarks == null) m.subjectMarks = new java.util.HashMap<>();
        if (m.subjectMax == null)   m.subjectMax = new java.util.HashMap<>();
        if (m.detailedMarks == null) m.detailedMarks = new java.util.HashMap<>();

        // Keep attendance intact or fallback
        if (existingMarks != null) {
            m.presentDays = existingMarks.presentDays;
            m.totalDays = existingMarks.totalDays;
        }

        // Semester
        if (SessionContext.selectedSemester != null && SessionContext.selectedSemester.id != null) {
            m.semesterId = SessionContext.selectedSemester.id;
            m.semesterNumber = String.valueOf(SessionContext.selectedSemester.number);
        } else {
            m.semesterId = classModel.semesterId != null ? classModel.semesterId : "sem_1";
            m.semesterNumber = m.semesterId.contains("2") ? "2" : "1";
        }

        // Compile details & recalculate grand total across all subjects
        double overallObtained = 0;
        int overallMax = 0;

        for (Subject sub : classModel.subjects) {
            String safeKey = MarksRecord.sanitizeKey(sub.name);
            MarksRecord.SubjectMarksDetail d;

            if (sub.name.equalsIgnoreCase(subject.name)) {
                // This is the subject we are editing!
                d = new MarksRecord.SubjectMarksDetail();
                d.nirikhshan = getInt(b.etNirikhshan);
                d.tondiKam = getInt(b.etTondiKam);
                d.pratyakshik = getInt(b.etPratyakshik);
                d.upkram = getInt(b.etUpkram);
                d.prakalp = getInt(b.etPrakalp);
                d.chachani = getInt(b.etChachani);
                d.swadhyay = getInt(b.etSwadhyay);
                d.itar = getInt(b.etItar);
                d.akarikTotal = d.nirikhshan + d.tondiKam + d.pratyakshik + d.upkram
                        + d.prakalp + d.chachani + d.swadhyay + d.itar;

                d.tondi = getInt(b.etTondiB);
                d.pratyakshikB = getInt(b.etPratyakshikB);
                d.lekhi = getInt(b.etLekhi);
                d.sanklit = d.tondi + d.pratyakshikB + d.lekhi;

                d.grandTotal = d.akarikTotal + d.sanklit;
                d.maxMarks = sub.maxMarks;
                d.grade = GradeCalculator.getMyschoolGrade(d.grandTotal, d.maxMarks);

                // Preserve remark
                d.remark = "";
                if (existingMarks != null && existingMarks.detailedMarks != null) {
                    MarksRecord.SubjectMarksDetail old = existingMarks.detailedMarks.get(safeKey);
                    if (old != null && old.remark != null) {
                        d.remark = old.remark;
                    }
                }
            } else {
                // Keep other subjects as they are
                if (existingMarks != null && existingMarks.detailedMarks != null && existingMarks.detailedMarks.containsKey(safeKey)) {
                    d = existingMarks.detailedMarks.get(safeKey);
                } else {
                    d = new MarksRecord.SubjectMarksDetail();
                    d.maxMarks = sub.maxMarks;
                    d.grade = "—";
                }
            }

            m.detailedMarks.put(safeKey, d);
            m.subjectMarks.put(safeKey, (double) d.grandTotal);
            m.subjectMax.put(safeKey, sub.maxMarks);

            overallObtained += d.grandTotal;
            overallMax += sub.maxMarks;
        }

        m.totalObtained = overallObtained;
        m.totalMax = overallMax;
        m.percentage = GradeCalculator.getPercentage(overallObtained, overallMax);
        m.grade = GradeCalculator.getMyschoolGrade(overallObtained, overallMax);
        m.result = GradeCalculator.getResult(m.percentage);

        showLoading(true);
        FirebaseRepository.get().saveMarks(m, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String id) {
                m.id = id;
                AppCache.selectedMarks = m;

                // Sync SharedPreferences doc ID
                String prefKey = "marks_doc_" + m.studentId + "_" + m.classId + "_" + m.semesterId;
                String legacyPrefKey = "marks_doc_" + m.studentId + "_" + m.classId;
                if (getActivity() != null) {
                    getActivity().getSharedPreferences("marks_doc_ids", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString(prefKey, id)
                            .putString(legacyPrefKey, id)
                            .apply();
                }

                // Sync caches
                if (AppCache.cachedMarksMap == null) {
                    AppCache.cachedMarksMap = new java.util.HashMap<>();
                }
                AppCache.cachedMarksMap.put(student.id, m);
                AppCache.cachedClassIdForStudents = classModel.id;
                AppCache.cachedSemesterIdForMarks = m.semesterId;

                // Sync descriptive entries cache
                if (AppCache.cachedDescriptiveMarksMap == null
                        || !java.util.Objects.equals(classModel.id, AppCache.cachedDescriptiveClassId)
                        || !java.util.Objects.equals(m.semesterId, AppCache.cachedDescriptiveSemesterId)) {
                    AppCache.cachedDescriptiveMarksMap = new java.util.HashMap<>();
                    AppCache.cachedDescriptiveClassId = classModel.id;
                    AppCache.cachedDescriptiveSemesterId = m.semesterId;
                    AppCache.cachedDescriptiveMarksComplete = true;
                }
                AppCache.cachedDescriptiveMarksMap.put(student.id, m);

                // Set flags for FormativeSummativeFragment
                AppCache.marksJustSaved = true;
                AppCache.marksJustSavedStudentId = student.id;
                AppCache.marksJustSavedRecord = m;

                FirebaseRepository.get().clearMarksCache();

                student.marksEntered = true;
                FirebaseRepository.get().saveStudent(student, new FirebaseRepository.OnResult<String>() {
                    @Override public void onSuccess(String i) {}
                    @Override public void onError(Exception e) {}
                });

                if (saveListener != null) {
                    saveListener.onMarksSaved(student.id, m);
                }

                showLoading(false);
                Toast.makeText(getContext(), "Subject marks saved successfully.", Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        b.progress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnBack.setEnabled(!show);
        b.btnSave.setEnabled(!show);
        b.btnClear.setEnabled(!show);
        b.btnCancel.setEnabled(!show);
    }
}
