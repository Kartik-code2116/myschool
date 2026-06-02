package com.example.myschool.ui;

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
import com.example.myschool.R;
import com.example.myschool.model.SubscriptionRequest;

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

        // Screenshot
        if (request.screenshotUrl != null && !request.screenshotUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(request.screenshotUrl)
                    .centerCrop()
                    .into(holder.imgScreenshot);
                    
            holder.imgScreenshot.setOnClickListener(v -> showFullImage(v.getContext(), request.screenshotUrl));
        } else {
            holder.imgScreenshot.setImageDrawable(null);
            holder.imgScreenshot.setBackgroundColor(Color.parseColor("#E2E8F0"));
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    private void showFullImage(android.content.Context context, String url) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        android.widget.RelativeLayout layout = new android.widget.RelativeLayout(context);
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setBackgroundColor(Color.BLACK);
        
        ImageView imgView = new ImageView(context);
        imgView.setLayoutParams(new android.widget.RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.addView(imgView);
        
        ImageView btnClose = new ImageView(context);
        android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(120, 120);
        params.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        params.setMargins(0, 80, 40, 0);
        btnClose.setLayoutParams(params);
        btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClose.setColorFilter(Color.WHITE);
        btnClose.setPadding(20, 20, 20, 20);
        layout.addView(btnClose);
        
        dialog.setContentView(layout);
        
        Glide.with(context).load(url).into(imgView);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        imgView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgScreenshot;
        TextView tvDate;
        TextView tvStatus;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgScreenshot = itemView.findViewById(R.id.imgScreenshot);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
