package com.kartik.myschool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kartik.myschool.databinding.ActivitySchoolRegisterBinding;
import com.kartik.myschool.model.School;
import com.kartik.myschool.repository.FirebaseRepository;

public class SchoolRegisterActivity extends BaseActivity {

    private ActivitySchoolRegisterBinding b;
    private School editSchool; // non-null when editing existing school
    private boolean isOnboarding = false;

    private static final String[] BOARDS = {"CBSE", "ICSE", "State Board", "IB", "IGCSE", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Screen opened: " + this.getClass().getSimpleName());
        b = ActivitySchoolRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        // Board dropdown
        ArrayAdapter<String> boardAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, BOARDS);
        b.actvBoard.setAdapter(boardAdapter);

        // If editing, pre-fill
        if (getIntent().hasExtra("school_id")) {
            String sid = getIntent().getStringExtra("school_id");
            loadSchool(sid);
        }

        isOnboarding = getIntent().getBooleanExtra("is_onboarding", false);
        if (isOnboarding) {
            b.toolbar.setTitle("Setup Your School");
            b.toolbar.setNavigationIcon(null); // Remove back button for onboarding
            FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
                @Override
                public void onSuccess(com.kartik.myschool.model.Teacher teacher) {
                    if (teacher != null) {
                        if (TextUtils.isEmpty(b.etSchoolName.getText())) {
                            b.etSchoolName.setText(teacher.schoolName);
                        }
                        if (TextUtils.isEmpty(b.etUdise.getText())) {
                            b.etUdise.setText(teacher.udiseCode);
                        }
                    }
                }
                @Override
                public void onError(Exception e) {}
            });
        }

        b.btnSaveSchool.setOnClickListener(v -> saveSchool());
    }

    private void loadSchool(String id) {
        // Just re-use save: if school already exists it will be overwritten by ID
        // For simplicity we pass the School object via a static cache (see AppCache)
        School s = AppCache.selectedSchool;
        if (s != null && s.id != null && s.id.equals(id)) {
            editSchool = s;
            b.etSchoolName.setText(s.name);
            b.etUdise.setText(s.udiseCode);
            b.actvBoard.setText(s.board, false);
            b.etAddress.setText(s.address);
            b.etPrincipal.setText(s.principalName);
            if (s.resultDate != null) b.etResultDate.setText(s.resultDate);
        }
    }

    private void saveSchool() {
        String name      = str(b.etSchoolName);
        String udise     = str(b.etUdise);
        String board     = b.actvBoard.getText().toString().trim();
        String address   = str(b.etAddress);
        String principal = str(b.etPrincipal);
        String resultDate = str(b.etResultDate);

        boolean valid = true;
        if (TextUtils.isEmpty(name))  { b.tilSchoolName.setError("Required"); valid = false; }
        else if (name.length() > 200) { b.tilSchoolName.setError("Name too long (max 200 chars)"); valid = false; }

        if (TextUtils.isEmpty(board)) { b.tilBoard.setError("Select a board"); valid = false; }
        
        if (address.length() > 500) { 
            // We use Toast for address if there is no tilAddress. Assuming it's there.
            if (b.tilAddress != null) b.tilAddress.setError("Address too long (max 500 chars)"); 
            valid = false; 
        }

        if (!valid) return;
        
        b.tilSchoolName.setError(null); b.tilBoard.setError(null); 
        if (b.tilAddress != null) b.tilAddress.setError(null);

        School s = editSchool != null ? editSchool : new School();
        s.name          = name;
        s.udiseCode     = udise;
        s.board         = board;
        s.address       = address;
        s.principalName = principal;
        s.resultDate    = resultDate;

        showLoading(true);
        FirebaseRepository.get().saveSchool(s, new FirebaseRepository.OnResult<String>() {
            @Override public void onSuccess(String id) {
                s.id = id;
                AppCache.selectedSchool = s;
                SessionContext.selectedSchool = s;
                SessionContext.save(SchoolRegisterActivity.this);
                showLoading(false);
                Toast.makeText(SchoolRegisterActivity.this, R.string.msg_school_saved, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                if (isOnboarding) {
                    Intent intent = new Intent(SchoolRegisterActivity.this, ClassSetupActivity.class);
                    intent.putExtra("is_onboarding", true);
                    startActivity(intent);
                }
                finish();
            }
            @Override public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(SchoolRegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        b.schoolProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSaveSchool.setEnabled(!show);
    }

    private String str(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
