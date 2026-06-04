package com.kartik.myschool.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kartik.myschool.R;
import com.kartik.myschool.utils.FirebaseUploader;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SubscriptionBottomSheet extends BottomSheetDialogFragment {

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadScreenshot(selectedImageUri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_subscription, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Button btnUpload = view.findViewById(R.id.btnUploadScreenshot);
        btnUpload.setOnClickListener(v -> pickImage());
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Payment Screenshot"));
    }

    private void uploadScreenshot(Uri uri) {
        Toast.makeText(requireContext(), "Uploading screenshot...", Toast.LENGTH_SHORT).show();
        Button btnUpload = getView().findViewById(R.id.btnUploadScreenshot);
        btnUpload.setEnabled(false);
        btnUpload.setText("Uploading...");

        FirebaseUploader.uploadPaymentScreenshot(uri, new FirebaseUploader.UploadCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Screenshot uploaded! Verification pending.", Toast.LENGTH_LONG).show();
                    dismiss();
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnUpload.setEnabled(true);
                    btnUpload.setText("Upload Payment Screenshot");
                }
            }
        });
    }
}
