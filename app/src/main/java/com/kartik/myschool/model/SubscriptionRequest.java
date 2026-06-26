package com.kartik.myschool.model;

public class SubscriptionRequest {
    public String id;
    public String teacherId;
    public String status;
    public long timestamp;

    public String purchaseToken;
    public String productId;
    public Boolean subscriptionVerified;
    public Long requestedAt;
    
    // Additional details for UI
    public String rejectionReason;
    public String planName;
    public Double amount;

    public SubscriptionRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(SubscriptionRequest.class)
    }
}
