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

public class SchoolRegisterActivity extends AppCompatActivity {

    private ActivitySchoolRegisterBinding b;
    private School editSchool; // non-null when editing existing school

    private static final String[] BOARDS = {"CBSE", "ICSE", "State Board", "IB", "IGCSE", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        }
    }

    private void saveSchool() {
        String name      = str(b.etSchoolName);
        String udise     = str(b.etUdise);
        String board     = b.actvBoard.getText().toString().trim();
        String address   = str(b.etAddress);
        String principal = str(b.etPrincipal);

        if (TextUtils.isEmpty(name))  { b.tilSchoolName.setError("Required"); return; }
        if (TextUtils.isEmpty(board)) { b.tilBoard.setError("Select a board"); return; }
        b.tilSchoolName.setError(null); b.tilBoard.setError(null);

        School s = editSchool != null ? editSchool : new School();
        s.name          = name;
        s.udiseCode     = udise;
        s.board         = board;
        s.address       = address;
        s.principalName = principal;

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
