package com.kartik.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.kartik.myschool.R;
import com.kartik.myschool.utils.BillingHelper;

public class SubscriptionBottomSheet extends BottomSheetDialogFragment implements BillingHelper.BillingListener {

    private BillingHelper billingHelper;
    private Button btnSubscribePlay;
    private ProgressBar progressBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billingHelper = new BillingHelper(requireContext(), this);
        billingHelper.startConnection();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_subscription, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        btnSubscribePlay = view.findViewById(R.id.btnSubscribePlay);
        progressBar = view.findViewById(R.id.progressBar);

        // Initially disable until billing is set up
        btnSubscribePlay.setEnabled(false);

        btnSubscribePlay.setOnClickListener(v -> {
            billingHelper.launchBillingFlow(requireActivity());
        });
    }

    @Override
    public void onBillingSetupFinished() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                btnSubscribePlay.setEnabled(true);
            });
        }
    }

    @Override
    public void onBillingSetupFailed(String error) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                btnSubscribePlay.setEnabled(true); // Allow them to click it so they can see the error
                btnSubscribePlay.setOnClickListener(v -> {
                    Toast.makeText(requireContext(), "Billing not available: " + error, Toast.LENGTH_LONG).show();
                });
            });
        }
    }

    @Override
    public void onPurchaseSuccessful() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                android.content.Intent intent = new android.content.Intent(requireActivity(), SubscriptionSuccessActivity.class);
                startActivity(intent);
                dismiss();
            });
        }
    }

    @Override
    public void onPurchaseFailed(String error) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Purchase failed: " + error, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (billingHelper != null) {
            billingHelper.endConnection();
        }
    }
}
