package com.example.myschool.utils;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class FirebaseUploader {

    public interface UploadCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public static void uploadPaymentScreenshot(Uri fileUri, UploadCallback callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            callback.onError(new Exception("User not authenticated"));
            return;
        }

        String fileName = "payment_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("payment_screenshots/" + uid + "/" + fileName);

        storageRef.putFile(fileUri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                saveSubscriptionRequest(uid, downloadUri.toString(), callback);
            } else {
                callback.onError(task.getException());
            }
        });
    }

    private static void saveSubscriptionRequest(String uid, String downloadUrl, UploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> request = new HashMap<>();
        request.put("teacherId", uid);
        request.put("screenshotUrl", downloadUrl);
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("subscriptions")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    // Update user's subscriptionStatus to pending just in case
                    db.collection("teachers").document(uid)
                            .update("subscriptionStatus", "pending")
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> {
                                Log.e("FirebaseUploader", "Failed to update teacher status", e);
                                callback.onSuccess(); // still report success since request was created
                            });
                })
                .addOnFailureListener(callback::onError);
    }
}
