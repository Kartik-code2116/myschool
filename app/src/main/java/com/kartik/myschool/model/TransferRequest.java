package com.kartik.myschool.model;

import java.util.List;

public class TransferRequest {
    public String id;
    public String fromTeacherId;
    public String transferCode;
    public long createdAt;
    public List<String> studentIds;
    public boolean isClaimed;

    public TransferRequest() {}
}
