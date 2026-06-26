package com.kartik.myschool.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kartik.myschool.R;
import com.kartik.myschool.model.SubscriptionRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubscriptionHistoryAdapter extends RecyclerView.Adapter<SubscriptionHistoryAdapter.HistoryViewHolder> {

    private final List<SubscriptionRequest> requests = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());

    public void setRequests(List<SubscriptionRequest> newRequests) {
        requests.clear();
        if (newRequests != null) {
            requests.addAll(newRequests);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subscription_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SubscriptionRequest request = requests.get(position);

        // Date
        if (request.timestamp > 0) {
            holder.tvDate.setText(dateFormat.format(new Date(request.timestamp)));
        } else {
            holder.tvDate.setText("Unknown Date");
        }

        // Status Badge
        String status = request.status != null ? request.status.toLowerCase() : "unknown";
        holder.tvStatus.setText(status.toUpperCase());
        
        GradientDrawable bg = (GradientDrawable) holder.tvStatus.getBackground();
        if ("approved".equals(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#166534")); // Dark Green
            bg.setColor(Color.parseColor("#DCFCE7")); // Light Green
        } else if ("rejected".equals(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#991B1B")); // Dark Red
            bg.setColor(Color.parseColor("#FEE2E2")); // Light Red
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#D97706")); // Dark Orange
            bg.setColor(Color.parseColor("#FEF3C7")); // Light Orange
        }

        // Plan Details
        if (request.planName != null || request.amount != null) {
            holder.tvPlanDetails.setVisibility(View.VISIBLE);
            String planStr = request.planName != null ? "Plan: " + request.planName : "Plan: Unknown";
            String amtStr = request.amount != null ? "\nAmount: ₹" + request.amount : "";
            holder.tvPlanDetails.setText(planStr + amtStr);
        } else {
            holder.tvPlanDetails.setVisibility(View.GONE);
        }

        // Transaction ID
        if (request.purchaseToken != null && !request.purchaseToken.isEmpty()) {
            holder.tvTransactionId.setVisibility(View.VISIBLE);
            // Show first few characters of the token as Txn ID or the actual ID if mapped differently
            String prefix = request.purchaseToken.length() > 15 ? request.purchaseToken.substring(0, 15) + "..." : request.purchaseToken;
            holder.tvTransactionId.setText("Txn ID: " + prefix);
        } else {
            holder.tvTransactionId.setVisibility(View.GONE);
        }

        // Rejection Reason
        if ("rejected".equals(status) && request.rejectionReason != null && !request.rejectionReason.isEmpty()) {
            holder.tvRejectionReason.setVisibility(View.VISIBLE);
            holder.tvRejectionReason.setText("Reason: " + request.rejectionReason);
        } else {
            holder.tvRejectionReason.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvStatus;
        TextView tvPlanDetails;
        TextView tvTransactionId;
        TextView tvRejectionReason;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPlanDetails = itemView.findViewById(R.id.tvPlanDetails);
            tvTransactionId = itemView.findViewById(R.id.tvTransactionId);
            tvRejectionReason = itemView.findViewById(R.id.tvRejectionReason);
        }
    }
}
