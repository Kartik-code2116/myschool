package com.example.myschool.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
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

    public interface OnItemClickListener {
        void onItemClick(ReportTemplate template, int position);
    }

    private final List<ReportTemplate> items = new ArrayList<>();
    private OnItemClickListener listener;

    public ReportPrintingAdapter() {
        items.add(new ReportTemplate("गुणपत्रक", "१. प्रगती व गुणपत्रक", "प्रथम व द्वितीय सत्र गुणांचे एकत्रित गुणपत्रक (A4)"));
        items.add(new ReportTemplate("गुणवर्णनिका", "२. वर्णनात्मक नोंद वही", "विषयनिहाय शिक्षकांच्या गुणात्मक नोंदी (वर्णनिका)"));
        items.add(new ReportTemplate("प्रगतीपुस्तक", "३. एकत्रित वर्ग निकाल तक्ता", "इयत्ता तुकडीनुसार वार्षिक वर्गवार निकाल पत्रक (Roster)"));
        items.add(new ReportTemplate("नोंदी", "४. व्यक्तिमत्व विकास पुस्तिका", "विद्यार्थी मूलभूत, बँक व कौटुंबिक माहिती पुस्तिका (A5)"));
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
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

        b.cardReportItem.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });

        b.btnReportAction.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });

        b.btnReportSettings.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });
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
