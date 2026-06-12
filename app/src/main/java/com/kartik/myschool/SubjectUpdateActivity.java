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

        // Determine CURRENT formative and summative values
        boolean foundCustom = false;
        if (com.kartik.myschool.SessionContext.selectedClass != null && com.kartik.myschool.SessionContext.selectedClass.subjects != null) {
            for (com.kartik.myschool.model.Subject s : com.kartik.myschool.SessionContext.selectedClass.subjects) {
                if (s.name != null && s.name.equalsIgnoreCase(name)) {
                    int fe = s.maxNirikhshan + s.maxTondiKam + s.maxPratyakshik + s.maxUpkram + s.maxPrakalp + s.maxChachani + s.maxSwadhyay + s.maxItar;
                    int se = s.maxTondi + s.maxPratyakshikB + s.maxLekhi;
                    b.etFormativeEval.setText(String.valueOf(fe));
                    b.etSummativeEval.setText(String.valueOf(se));
                    foundCustom = true;
                    break;
                }
            }
        }
        
        if (!foundCustom) {
            String detailsFe = getIntent().getStringExtra("details_left_1");
            String detailsSe = getIntent().getStringExtra("details_left_2");
            
            try {
                if (detailsFe != null && detailsFe.contains("FE:")) {
                    b.etFormativeEval.setText(detailsFe.replace("FE:", "").trim());
                } else if (category != null && (category.equalsIgnoreCase("Activities") || category.equalsIgnoreCase("Personality"))) {
                    b.etFormativeEval.setText("100");
                } else {
                    b.etFormativeEval.setText("50");
                }
                
                if (detailsSe != null && detailsSe.contains("SE:")) {
                    b.etSummativeEval.setText(detailsSe.replace("SE:", "").trim());
                } else if (category != null && (category.equalsIgnoreCase("Activities") || category.equalsIgnoreCase("Personality"))) {
                    b.etSummativeEval.setText("0");
                } else {
                    b.etSummativeEval.setText("50");
                }
            } catch (Exception e) {
                b.etFormativeEval.setText("50");
                b.etSummativeEval.setText("50");
            }
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

            int formative = 0;
            int summative = 0;
            try { formative = Integer.parseInt(b.etFormativeEval.getText().toString().trim()); } catch(Exception ignored){}
            try { summative = Integer.parseInt(b.etSummativeEval.getText().toString().trim()); } catch(Exception ignored){}

            // Update in SessionContext
            if (com.kartik.myschool.SessionContext.selectedClass != null && com.kartik.myschool.SessionContext.selectedClass.subjects != null) {
                for (com.kartik.myschool.model.Subject s : com.kartik.myschool.SessionContext.selectedClass.subjects) {
                    if (s.name != null && s.name.equalsIgnoreCase(name)) { // compare with original name
                        s.name = updatedName;
                        s.subjectCode = b.etSubjectCode.getText().toString().trim();
                        s.maxMarks = formative + summative;

                        // Re-scale sub-fields based on new formative (akarik) and summative (sanklit) totals
                        s.maxNirikhshan   = formative * 10 / 50;
                        s.maxTondiKam     = formative * 10 / 50;
                        s.maxPratyakshik  = formative * 10 / 50;
                        s.maxUpkram       = formative * 5  / 50;
                        s.maxPrakalp      = formative * 5  / 50;
                        s.maxChachani     = formative * 5  / 50;
                        s.maxSwadhyay     = formative * 5  / 50;
                        s.maxItar         = formative - s.maxNirikhshan - s.maxTondiKam - s.maxPratyakshik
                                - s.maxUpkram - s.maxPrakalp - s.maxChachani - s.maxSwadhyay;

                        s.maxTondi        = summative * 10 / 50;
                        s.maxPratyakshikB = summative * 10 / 50;
                        s.maxLekhi        = summative - s.maxTondi - s.maxPratyakshikB;
                        break;
                    }
                }
                
                // Save to local cache and Firestore
                com.kartik.myschool.SessionContext.save(this);
                com.kartik.myschool.AppCache.selectedClass = com.kartik.myschool.SessionContext.selectedClass;
                com.kartik.myschool.repository.FirebaseRepository.get().saveClass(com.kartik.myschool.SessionContext.selectedClass, new com.kartik.myschool.repository.FirebaseRepository.OnResult<String>() {
                    @Override public void onSuccess(String result) {
                        Toast.makeText(SubjectUpdateActivity.this, "Subject " + updatedName + " updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override public void onError(Exception e) {
                        Toast.makeText(SubjectUpdateActivity.this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } else {
                Toast.makeText(this, "Subject " + updatedName + " updated locally!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
