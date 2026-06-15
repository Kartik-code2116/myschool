package com.kartik.myschool.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingHelper {
    private static final String TAG = "BillingHelper";
    // Default product ID. You MUST create this exactly in the Google Play Console!
    public static final String SUBSCRIPTION_PRODUCT_ID = "premium_monthly";

    private BillingClient billingClient;
    private Context context;
    private ProductDetails subscriptionProductDetails;

    public interface BillingListener {
        void onBillingSetupFinished();
        void onBillingSetupFailed(String error);
        void onPurchaseSuccessful();
        void onPurchaseFailed(String error);
    }

    private BillingListener listener;

    public BillingHelper(Context context, BillingListener listener) {
        this.context = context;
        this.listener = listener;

        PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (Purchase purchase : purchases) {
                        handlePurchase(purchase);
                    }
                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    listener.onPurchaseFailed("Purchase cancelled by user.");
                } else {
                    listener.onPurchaseFailed("Purchase error: " + billingResult.getDebugMessage());
                }
            }
        };

        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
    }

    public void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                } else {
                    if (listener != null) {
                        listener.onBillingSetupFailed("Setup failed: " + billingResult.getDebugMessage());
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to Google Play by calling the startConnection() method.
                Log.w(TAG, "Billing service disconnected.");
            }
        });
    }

    private void queryProductDetails() {
        QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                ImmutableList.of(
                                        QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId(SUBSCRIPTION_PRODUCT_ID)
                                                .setProductType(BillingClient.ProductType.SUBS)
                                                .build()))
                        .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                (billingResult, productDetailsList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        if (productDetailsList != null && !productDetailsList.isEmpty()) {
                            subscriptionProductDetails = productDetailsList.get(0);
                            if (listener != null) {
                                listener.onBillingSetupFinished();
                            }
                        } else {
                            Log.e(TAG, "No product details found for " + SUBSCRIPTION_PRODUCT_ID);
                            if (listener != null) {
                                listener.onBillingSetupFailed("Product '" + SUBSCRIPTION_PRODUCT_ID + "' not found in Play Console.");
                            }
                        }
                    } else {
                        Log.e(TAG, "Query product details failed: " + billingResult.getDebugMessage());
                        if (listener != null) {
                            listener.onBillingSetupFailed("Query failed: " + billingResult.getDebugMessage());
                        }
                    }
                }
        );
    }

    public void launchBillingFlow(Activity activity) {
        if (subscriptionProductDetails == null) {
            Toast.makeText(context, "Product details not loaded yet. Try again in a moment.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the first available offer token
        String offerToken = null;
        if (subscriptionProductDetails.getSubscriptionOfferDetails() != null &&
                !subscriptionProductDetails.getSubscriptionOfferDetails().isEmpty()) {
            offerToken = subscriptionProductDetails.getSubscriptionOfferDetails().get(0).getOfferToken();
        }

        if (offerToken == null) {
            Toast.makeText(context, "No offers available for this product.", Toast.LENGTH_SHORT).show();
            return;
        }

        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(subscriptionProductDetails)
                                .setOfferToken(offerToken)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        billingClient.launchBillingFlow(activity, billingFlowParams);
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase if it hasn't been acknowledged yet.
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        grantSubscriptionToUser(purchase);
                    }
                });
            } else {
                grantSubscriptionToUser(purchase);
            }
        }
    }

    private void grantSubscriptionToUser(Purchase purchase) {
        // Update user status in Firebase
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> update = new HashMap<>();
            update.put("subscriptionStatus", "active");
            update.put("googlePlayPurchaseToken", purchase.getPurchaseToken());
            
            db.collection("users").document(uid)
                    .update(update)
                    .addOnSuccessListener(aVoid -> {
                        if (listener != null) {
                            listener.onPurchaseSuccessful();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update subscription status in DB", e);
                        if (listener != null) {
                            listener.onPurchaseSuccessful(); // Still report successful purchase to UI
                        }
                    });
        }
    }

    public void endConnection() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}
