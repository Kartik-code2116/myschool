package com.kartik.myschool.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.R;
import com.kartik.myschool.model.SubscriptionRequest;
import com.kartik.myschool.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class SubscriptionHistoryActivity extends com.kartik.myschool.BaseActivity {

    private RecyclerView rvHistory;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private MaterialButton btnRenew;
    private SubscriptionHistoryAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Screen opened: " + this.getClass().getSimpleName());
        setContentView(R.layout.activity_subscription_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rvHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnRenew = findViewById(R.id.btnRenew);

        adapter = new SubscriptionHistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        btnRenew.setOnClickListener(v -> {
            SubscriptionBottomSheet bottomSheet = new SubscriptionBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "SubscriptionBottomSheet");
        });

        loadHistory();
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        rvHistory.setVisibility(View.GONE);

        String teacherId = FirebaseRepository.get().currentUid();
        if (teacherId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseRepository.get().getSubscriptionHistory(teacherId, new FirebaseRepository.OnResult<List<SubscriptionRequest>>() {
            @Override
            public void onSuccess(List<SubscriptionRequest> result) {
                progressBar.setVisibility(View.GONE);
                if (result == null || result.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    adapter.setRequests(result);
                    rvHistory.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SubscriptionHistoryActivity.this, "Error loading history", Toast.LENGTH_SHORT).show();
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Error loading history: " + e.getMessage());
            }
        });
    }
}
