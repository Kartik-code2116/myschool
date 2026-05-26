package com.example.myschool.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myschool.databinding.ItemReportCardBinding;

import java.util.ArrayList;
import java.util.List;

public class ReportPrintingAdapter extends RecyclerView.Adapter<ReportPrintingAdapter.VH> {

    public static class ReportTemplate {
        public String category;
        public String title;
        public String description;

        public ReportTemplate(String category, String title, String description) {
            this.category = category;
            this.title = title;
            this.description = description;
        }
    }

    private final List<ReportTemplate> items = new ArrayList<>();

    public ReportPrintingAdapter() {
        // Pre-populate the 4 customized templates as described
        items.add(new ReportTemplate("नोंदवही", "1. गुणपत्रक", "नोंदवही आकाराचे गुणपत्रक"));
        items.add(new ReportTemplate("निकालपत्रक", "2. गुणवर्णनिका", "सत्र वर्तन गुणपत्रक"));
        items.add(new ReportTemplate("गुणवत्तक", "3. गुणवंत", "A4 साईज प्रगतीपुस्तक"));
        items.add(new ReportTemplate("तक्ते", "4. व्यक्तिमत्व विकास नोंदी", "A4 साईज कटलेले प्रगतीपुस्तक"));
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportCardBinding b = ItemReportCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReportTemplate item = items.get(position);
        ItemReportCardBinding b = holder.b;

        b.tvReportCategory.setText(item.category);
        b.tvReportTitle.setText(item.title);
        b.tvReportDescription.setText(item.description);

        // Click card
        b.cardReportItem.setOnClickListener(v -> Toast.makeText(v.getContext(), 
                "Generating print preview for " + item.title + "...", 
                Toast.LENGTH_SHORT).show());

        // Top Outlined Action
        b.btnReportAction.setOnClickListener(v -> Toast.makeText(v.getContext(), 
                "Action triggered for " + item.title, 
                Toast.LENGTH_SHORT).show());

        // Settings gear icon click
        b.btnReportSettings.setOnClickListener(v -> Toast.makeText(v.getContext(), 
                "Settings for " + item.title, 
                Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemReportCardBinding b;
        VH(ItemReportCardBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
