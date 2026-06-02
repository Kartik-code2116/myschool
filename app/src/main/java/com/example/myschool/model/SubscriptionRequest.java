package com.example.myschool.model;

public class SubscriptionRequest {
    public String id;
    public String teacherId;
    public String status;
    public String screenshotUrl;
    public long timestamp;

    public SubscriptionRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(SubscriptionRequest.class)
    }
}
