package com.kartik.myschool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.kartik.myschool.databinding.DialogAttendanceEntryBinding;
import com.kartik.myschool.model.AttendanceRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;

import java.util.HashMap;

public class AttendanceEntryDialog extends DialogFragment {

    public interface OnAttendanceSavedListener {
        void onAttendanceSaved(Student student, AttendanceRecord record);
    }

    private DialogAttendanceEntryBinding b;
    private Student student;
    private AttendanceRecord record;
    private OnAttendanceSavedListener saveListener;

    public static AttendanceEntryDialog newInstance(Student student, AttendanceRecord record) {
        AttendanceEntryDialog dialog = new AttendanceEntryDialog();
        dialog.student = student;
        dialog.record = record;
        return dialog;
    }

    public void setOnAttendanceSavedListener(OnAttendanceSavedListener listener) {
        this.saveListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.AttendanceEntryDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = DialogAttendanceEntryBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (student == null || record == null) {
            dismiss();
            return;
        }

        String rollNo = (student.rollNo != null && !student.rollNo.isEmpty()) ? student.rollNo : "N/A";
        b.tvDialogTitle.setText("उपस्थिती भरा");
        b.tvStudentInfo.setText(rollNo + " - " + student.name);

        prepopulateInputs();

        b.btnBack.setOnClickListener(v -> dismiss());
        b.btnCancel.setOnClickListener(v -> dismiss());
        b.btnSave.setOnClickListener(v -> saveAttendance());
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

    private void prepopulateInputs() {
        prepopPair(b.etJunPresent, b.etJunTotal, "जून", record.monthlyData.get("जून"));
        prepopPair(b.etJulPresent, b.etJulTotal, "जुलै", record.monthlyData.get("जुलै"));
        prepopPair(b.etAugPresent, b.etAugTotal, "ऑगस्ट", record.monthlyData.get("ऑगस्ट"));
        prepopPair(b.etSepPresent, b.etSepTotal, "सप्टें", record.monthlyData.get("सप्टें"));
        prepopPair(b.etOctPresent, b.etOctTotal, "ऑक्टो", record.monthlyData.get("ऑक्टो"));
        prepopPair(b.etNovPresent, b.etNovTotal, "नोव्हे", record.monthlyData.get("नोव्हे"));
        prepopPair(b.etDecPresent, b.etDecTotal, "डिसें", record.monthlyData.get("डिसें"));
        prepopPair(b.etJanPresent, b.etJanTotal, "जाने", record.monthlyData.get("जाने"));
        prepopPair(b.etFebPresent, b.etFebTotal, "फेब्रु", record.monthlyData.get("फेब्रु"));
        prepopPair(b.etMarPresent, b.etMarTotal, "मार्च", record.monthlyData.get("मार्च"));
        prepopPair(b.etAprPresent, b.etAprTotal, "एप्रिल", record.monthlyData.get("एप्रिल"));
        prepopPair(b.etMayPresent, b.etMayTotal, "मे", record.monthlyData.get("मे"));
    }

    private void prepopPair(EditText etPresent, EditText etTotal, String month, String val) {
        String pStr = "0";
        String tStr = "0";
        if (val != null && val.contains("/")) {
            String[] parts = val.split("/");
            if (parts.length == 2) {
                pStr = parts[0].trim();
                tStr = parts[1].trim();
            }
        }

        // Check if class-level working days has a value configured for this month
        Integer classVal = null;
        if (com.kartik.myschool.SessionContext.selectedClass != null
                && com.kartik.myschool.SessionContext.selectedClass.monthlyWorkingDays != null) {
            classVal = com.kartik.myschool.SessionContext.selectedClass.monthlyWorkingDays.get(month);
        }

        if (classVal != null && classVal > 0) {
            tStr = String.valueOf(classVal);
            etTotal.setEnabled(false);
            etTotal.setFocusable(false);
            etTotal.setAlpha(0.6f);
        } else {
            etTotal.setEnabled(true);
            etTotal.setFocusableInTouchMode(true);
            etTotal.setAlpha(1.0f);
        }

        etPresent.setText(pStr);
        etTotal.setText(tStr);
    }

    private void saveAttendance() {
        if (!validateAttendance()) return;

        savePair(b.etJunPresent, b.etJunTotal, "जून", record);
        savePair(b.etJulPresent, b.etJulTotal, "जुलै", record);
        savePair(b.etAugPresent, b.etAugTotal, "ऑगस्ट", record);
        savePair(b.etSepPresent, b.etSepTotal, "सप्टें", record);
        savePair(b.etOctPresent, b.etOctTotal, "ऑक्टो", record);
        savePair(b.etNovPresent, b.etNovTotal, "नोव्हे", record);
        savePair(b.etDecPresent, b.etDecTotal, "डिसें", record);
        savePair(b.etJanPresent, b.etJanTotal, "जाने", record);
        savePair(b.etFebPresent, b.etFebTotal, "फेब्रु", record);
        savePair(b.etMarPresent, b.etMarTotal, "मार्च", record);
        savePair(b.etAprPresent, b.etAprTotal, "एप्रिल", record);
        savePair(b.etMayPresent, b.etMayTotal, "मे", record);

        if (student.monthlyAttendance == null) student.monthlyAttendance = new HashMap<>();
        student.monthlyAttendance.clear();
        student.monthlyAttendance.putAll(record.monthlyData);

        showLoading(true);
        FirebaseRepository.get().saveStudent(student, new FirebaseRepository.OnResult<String>() {
            @Override
            public void onSuccess(String id) {
                showLoading(false);
                if (getContext() != null)
                    Toast.makeText(getContext(), student.name + " ची उपस्थिती जतन झाली!", Toast.LENGTH_SHORT).show();
                com.kartik.myschool.utils.AnalyticsHelper.logAttendanceMarked(student.classId, 
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
                if (saveListener != null) saveListener.onAttendanceSaved(student, record);
                dismiss();
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                if (getContext() != null)
                    Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateAttendance() {
        if (!validatePair(b.etJunPresent, b.etJunTotal, "जून")) return false;
        if (!validatePair(b.etJulPresent, b.etJulTotal, "जुलै")) return false;
        if (!validatePair(b.etAugPresent, b.etAugTotal, "ऑगस्ट")) return false;
        if (!validatePair(b.etSepPresent, b.etSepTotal, "सप्टें")) return false;
        if (!validatePair(b.etOctPresent, b.etOctTotal, "ऑक्टो")) return false;
        if (!validatePair(b.etNovPresent, b.etNovTotal, "नोव्हे")) return false;
        if (!validatePair(b.etDecPresent, b.etDecTotal, "डिसें")) return false;
        if (!validatePair(b.etJanPresent, b.etJanTotal, "जाने")) return false;
        if (!validatePair(b.etFebPresent, b.etFebTotal, "फेब्रु")) return false;
        if (!validatePair(b.etMarPresent, b.etMarTotal, "मार्च")) return false;
        if (!validatePair(b.etAprPresent, b.etAprTotal, "एप्रिल")) return false;
        if (!validatePair(b.etMayPresent, b.etMayTotal, "मे")) return false;
        return true;
    }

    private boolean validatePair(EditText etPresent, EditText etTotal, String monthName) {
        String pStr = etPresent.getText() != null ? etPresent.getText().toString().trim() : "0";
        String tStr = etTotal.getText() != null ? etTotal.getText().toString().trim() : "0";
        if (pStr.isEmpty()) pStr = "0";
        if (tStr.isEmpty()) tStr = "0";
        try {
            int present = Integer.parseInt(pStr);
            int total = Integer.parseInt(tStr);
            if (present > total) {
                String msg = monthName + " मधील उपस्थित दिवस (" + present + ") हे एकूण कामकाजाच्या दिवसांपेक्षा ("
                        + total + ") जास्त असू शकत नाहीत!";
                if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                etPresent.requestFocus();
                etPresent.selectAll();
                return false;
            }
        } catch (NumberFormatException ignored) {}
        return true;
    }

    private void savePair(EditText etPresent, EditText etTotal, String month, AttendanceRecord r) {
        String pStr = etPresent.getText() != null ? etPresent.getText().toString().trim() : "0";
        String tStr = etTotal.getText() != null ? etTotal.getText().toString().trim() : "0";
        if (pStr.isEmpty()) pStr = "0";
        if (tStr.isEmpty()) tStr = "0";
        r.monthlyData.put(month, pStr + "/" + tStr);
    }

    private void showLoading(boolean show) {
        if (b == null) return;
        b.progress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSave.setEnabled(!show);
        b.btnCancel.setEnabled(!show);
    }
}
