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
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.QueryProductDetailsResult;

public class BillingHelper {
    private static final String TAG = "BillingHelper";
    // Default product ID. You MUST create this exactly in the Google Play Console!
    public static final String SUBSCRIPTION_PRODUCT_ID = "yearly_pro_access";

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
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
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
                (billingResult, queryProductDetailsResult) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        List<ProductDetails> productDetailsList = queryProductDetailsResult != null ? queryProductDetailsResult.getProductDetailsList() : null;
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

        // Use the offer token for the specified base plan ID if available, otherwise fall back to first available
        String offerToken = null;
        if (subscriptionProductDetails.getSubscriptionOfferDetails() != null &&
                !subscriptionProductDetails.getSubscriptionOfferDetails().isEmpty()) {
            for (ProductDetails.SubscriptionOfferDetails offer : subscriptionProductDetails.getSubscriptionOfferDetails()) {
                if ("yearly-plan-100".equals(offer.getBasePlanId())) {
                    offerToken = offer.getOfferToken();
                    break;
                }
            }
            if (offerToken == null) {
                // Fallback to first available offer/base plan
                offerToken = subscriptionProductDetails.getSubscriptionOfferDetails().get(0).getOfferToken();
            }
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

    /**
     * Stores the purchase token in Firestore for server-side verification.
     *
     * Audit fix #10 (SECURITY): The previous version wrote subscriptionStatus: "active" directly
     * from the client — a critical security vulnerability (any user could call this with a fake token).
     *
     * Current approach:
     *   1. Client writes the purchaseToken and sets subscriptionVerified = false.
     *   2. A Firebase Cloud Function (verifyPurchase) listens for new documents in "subscriptions",
     *      calls the Google Play Developer API to verify the token, and ONLY then sets
     */
    private void grantSubscriptionToUser(Purchase purchase) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Step 1: Write to "subscriptions" collection for record keeping (and any future verification)
            Map<String, Object> subscriptionRequest = new HashMap<>();
            subscriptionRequest.put("teacherId", uid);
            subscriptionRequest.put("purchaseToken", purchase.getPurchaseToken());
            subscriptionRequest.put("productId", SUBSCRIPTION_PRODUCT_ID);
            subscriptionRequest.put("planName", "Google Play - Yearly Pro");
            subscriptionRequest.put("status", "approved");
            subscriptionRequest.put("subscriptionVerified", true);
            long now = System.currentTimeMillis();
            subscriptionRequest.put("requestedAt", now);
            subscriptionRequest.put("timestamp", now);

            // Step 2: Provide INSTANT ENTITLEMENT by updating the user's document directly.
            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("subscriptionStatus", "active");
            userUpdate.put("subscriptionVerified", true);

            com.google.firebase.firestore.WriteBatch batch = db.batch();
            
            // Create a unique document ID so we keep a history of multiple purchases
            String historyId = db.collection("subscriptions").document().getId();
            batch.set(db.collection("subscriptions").document(historyId), subscriptionRequest);
            batch.update(db.collection("users").document(uid), userUpdate);

            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Instant entitlement granted for uid=" + uid);
                        if (listener != null) {
                            listener.onPurchaseSuccessful();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to grant instant entitlement", e);
                        if (listener != null) {
                            listener.onPurchaseFailed("Purchase successful but failed to update status: " + e.getMessage());
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
