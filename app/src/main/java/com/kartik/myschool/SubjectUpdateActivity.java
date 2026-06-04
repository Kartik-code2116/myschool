package com.kartik.myschool;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kartik.myschool.databinding.ActivitySubjectUpdateBinding;

public class SubjectUpdateActivity extends AppCompatActivity {

    private ActivitySubjectUpdateBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySubjectUpdateBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Extract passed details
        String name = getIntent().getStringExtra("subject_name");
        String code = getIntent().getStringExtra("subject_code");
        String serial = getIntent().getStringExtra("subject_serial");
        String category = getIntent().getStringExtra("subject_category");
        int maxMarks = getIntent().getIntExtra("subject_max_marks", 100);

        // Pre-fill input fields
        if (code != null) {
            b.etSubjectCode.setText(code);
        } else {
            b.etSubjectCode.setText("1");
        }

        b.etTeacherId.setText(R.string.msg_null);

        if (name != null) {
            b.etSubjectNameRegular.setText(name);
            b.etSubjectNameShort.setText(name);
            b.etSubjectNameLong.setText("First Language: " + name);
        }

        // Split or default formative and summative values based on academic vs activities
        if (category != null && (category.equalsIgnoreCase("Activities") || category.equalsIgnoreCase("Personality"))) {
            b.etFormativeEval.setText("100");
            b.etSummativeEval.setText("0");
        } else {
            b.etFormativeEval.setText("70");
            b.etSummativeEval.setText("30");
        }

        b.etDropdownNotes.setText(R.string.msg_i_1);
        b.etDropdownNoteMedium.setText("1");

        // Action Buttons Setup
        b.llHeaderBar.setNavigationOnClickListener(v -> finish());
        b.btnCancel.setOnClickListener(v -> finish());

        b.btnClear.setOnClickListener(v -> {
            b.etSubjectCode.setText("");
            b.etTeacherId.setText("");
            b.etFormativeEval.setText("");
            b.etSummativeEval.setText("");
            b.etSubjectNameRegular.setText("");
            b.etSubjectNameShort.setText("");
            b.etSubjectNameLong.setText("");
            b.etDropdownNotes.setText("");
            b.etDropdownNoteMedium.setText("");
            Toast.makeText(this, R.string.msg_form_cleared, Toast.LENGTH_SHORT).show();
        });

        b.btnUpdate.setOnClickListener(v -> {
            String updatedName = b.etSubjectNameRegular.getText().toString().trim();
            if (updatedName.isEmpty()) {
                b.tilSubjectNameRegular.setError("Subject name is required");
                return;
            }
            b.tilSubjectNameRegular.setError(null);

            Toast.makeText(this, "Subject " + updatedName + " updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
