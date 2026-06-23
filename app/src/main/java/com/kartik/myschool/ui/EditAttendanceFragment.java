package com.kartik.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.databinding.FragmentEditAttendanceBinding;
import com.kartik.myschool.model.AttendanceRecord;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.repository.FirebaseRepository;

import java.util.HashMap;

public class EditAttendanceFragment extends Fragment {

    private FragmentEditAttendanceBinding b;
    private Student student;
    private AttendanceRecord record;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentEditAttendanceBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());

        student = SessionContext.currentStudentForAttendance;
        record = SessionContext.currentRecordForAttendance;

        if (student == null || record == null) {
            goBack();
            return;
        }

        String rollNo = (student.rollNo != null && !student.rollNo.isEmpty()) ? student.rollNo : "N/A";
        b.tvStudentInfo.setText(rollNo + " - " + student.name);

        prepopulateInputs();

        b.btnCancel.setOnClickListener(v -> goBack());
        b.btnSave.setOnClickListener(v -> saveAttendance());
    }

    private void goBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }



    private void prepopulateInputs() {
        prepopPairWithClass(b.etJunPresent, b.etJunTotal, "जून", record.monthlyData.get("जून"));
        prepopPairWithClass(b.etJulPresent, b.etJulTotal, "जुलै", record.monthlyData.get("जुलै"));
        prepopPairWithClass(b.etAugPresent, b.etAugTotal, "ऑगस्ट", record.monthlyData.get("ऑगस्ट"));
        prepopPairWithClass(b.etSepPresent, b.etSepTotal, "सप्टें", record.monthlyData.get("सप्टें"));
        prepopPairWithClass(b.etOctPresent, b.etOctTotal, "ऑक्टो", record.monthlyData.get("ऑक्टो"));
        prepopPairWithClass(b.etNovPresent, b.etNovTotal, "नोव्हे", record.monthlyData.get("नोव्हे"));
        prepopPairWithClass(b.etDecPresent, b.etDecTotal, "डिसें", record.monthlyData.get("डिसें"));
        prepopPairWithClass(b.etJanPresent, b.etJanTotal, "जाने", record.monthlyData.get("जाने"));
        prepopPairWithClass(b.etFebPresent, b.etFebTotal, "फेब्रु", record.monthlyData.get("फेब्रु"));
        prepopPairWithClass(b.etMarPresent, b.etMarTotal, "मार्च", record.monthlyData.get("मार्च"));
        prepopPairWithClass(b.etAprPresent, b.etAprTotal, "एप्रिल", record.monthlyData.get("एप्रिल"));
        prepopPairWithClass(b.etMayPresent, b.etMayTotal, "मे", record.monthlyData.get("मे"));
    }

    private void prepopPairWithClass(EditText etPresent, EditText etTotal, String month, String val) {
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
        if (SessionContext.selectedClass != null && SessionContext.selectedClass.monthlyWorkingDays != null) {
            classVal = SessionContext.selectedClass.monthlyWorkingDays.get(month);
        }

        if (classVal != null && classVal > 0) {
            tStr = String.valueOf(classVal);
            etTotal.setEnabled(false);
            etTotal.setFocusable(false);
            etTotal.setAlpha(0.6f); // Visually indicate it is read-only
        } else {
            etTotal.setEnabled(true);
            etTotal.setFocusableInTouchMode(true);
            etTotal.setAlpha(1.0f);
        }

        etPresent.setText(pStr);
        etTotal.setText(tStr);
    }

    private void saveAttendance() {
        if (!validateAttendance()) {
            return;
        }

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
                Toast.makeText(getContext(), student.name + " ची उपस्थिती जतन झाली!", Toast.LENGTH_SHORT).show();
                goBack();
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(getContext(), "त्रुटी: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateAttendance() {
        if (!validateMonthPair(b.etJunPresent, b.etJunTotal, "जून")) return false;
        if (!validateMonthPair(b.etJulPresent, b.etJulTotal, "जुलै")) return false;
        if (!validateMonthPair(b.etAugPresent, b.etAugTotal, "ऑगस्ट")) return false;
        if (!validateMonthPair(b.etSepPresent, b.etSepTotal, "सप्टें")) return false;
        if (!validateMonthPair(b.etOctPresent, b.etOctTotal, "ऑक्टो")) return false;
        if (!validateMonthPair(b.etNovPresent, b.etNovTotal, "नोव्हे")) return false;
        if (!validateMonthPair(b.etDecPresent, b.etDecTotal, "डिसें")) return false;
        if (!validateMonthPair(b.etJanPresent, b.etJanTotal, "जाने")) return false;
        if (!validateMonthPair(b.etFebPresent, b.etFebTotal, "फेब्रु")) return false;
        if (!validateMonthPair(b.etMarPresent, b.etMarTotal, "मार्च")) return false;
        if (!validateMonthPair(b.etAprPresent, b.etAprTotal, "एप्रिल")) return false;
        if (!validateMonthPair(b.etMayPresent, b.etMayTotal, "मे")) return false;
        return true;
    }

    private boolean validateMonthPair(EditText etPresent, EditText etTotal, String monthName) {
        String pStr = etPresent.getText() != null ? etPresent.getText().toString().trim() : "0";
        String tStr = etTotal.getText() != null ? etTotal.getText().toString().trim() : "0";
        if (pStr.isEmpty()) pStr = "0";
        if (tStr.isEmpty()) tStr = "0";

        try {
            int present = Integer.parseInt(pStr);
            int total = Integer.parseInt(tStr);
            if (present > total) {
                String errorMsg = monthName + " मधील उपस्थित दिवस (" + present + ") हे एकूण कामकाजाच्या दिवसांपेक्षा (" + total + ") जास्त असू शकत नाहीत!";
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
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

    @Override
    public void onResume() {
        super.onResume();
        com.kartik.myschool.SessionContext.ensureCacheLoaded(requireContext());
    }
}
