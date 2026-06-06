package com.kartik.myschool.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.databinding.ItemReportCardBinding;
import com.kartik.myschool.R;

import java.util.ArrayList;
import java.util.List;

public class ReportPrintingAdapter extends RecyclerView.Adapter<ReportPrintingAdapter.VH> {

    public static class ReportTemplate {
        public int categoryResId;
        public int titleResId;
        public int descriptionResId;

        public ReportTemplate(int categoryResId, int titleResId, int descriptionResId) {
            this.categoryResId = categoryResId;
            this.titleResId = titleResId;
            this.descriptionResId = descriptionResId;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(ReportTemplate template, int position);
    }

    private final List<ReportTemplate> items = new ArrayList<>();
    private OnItemClickListener listener;

    public ReportPrintingAdapter(android.content.Context ctx) {
        items.add(new ReportTemplate(R.string.report_cat_register, R.string.report_title_1, R.string.report_desc_1));
        items.add(new ReportTemplate(R.string.report_cat_register, R.string.report_title_2, R.string.report_desc_2));
        items.add(new ReportTemplate(R.string.report_cat_register, R.string.report_title_3, R.string.report_desc_3));
        items.add(new ReportTemplate(R.string.report_cat_register, R.string.report_title_4, R.string.report_desc_4));
        items.add(new ReportTemplate(R.string.report_cat_register, R.string.report_title_5, R.string.report_desc_5));
        
        items.add(new ReportTemplate(R.string.report_cat_result, R.string.report_title_6, R.string.report_desc_6));
        items.add(new ReportTemplate(R.string.report_cat_result, R.string.report_title_7, R.string.report_desc_7));
        items.add(new ReportTemplate(R.string.report_cat_result, R.string.report_title_8, R.string.report_desc_8));
        
        items.add(new ReportTemplate(R.string.report_cat_marksheet, R.string.report_title_9, R.string.report_desc_9));
        items.add(new ReportTemplate(R.string.report_cat_marksheet, R.string.report_title_10, R.string.report_desc_10));
        
        items.add(new ReportTemplate(R.string.report_cat_tables, R.string.report_title_11, R.string.report_desc_11));
        
        items.add(new ReportTemplate(R.string.report_cat_progressbook, R.string.report_title_12, R.string.report_desc_12));
        
        items.add(new ReportTemplate(R.string.report_cat_tables, R.string.report_title_13, R.string.report_desc_13));
        
        items.add(new ReportTemplate(R.string.report_cat_marksheet, R.string.report_title_14, R.string.report_desc_14));
        
        items.add(new ReportTemplate(R.string.report_cat_result, R.string.report_title_15, R.string.report_desc_15));
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

        b.tvReportCategory.setText(item.categoryResId);
        b.tvReportTitle.setText(item.titleResId);
        b.tvReportDescription.setText(item.descriptionResId);

        // Category colour-coding matches the screenshots
        int categoryColor;
        if (item.categoryResId == R.string.report_cat_register) {
            categoryColor = 0xFF5A4FCF; // indigo
        } else if (item.categoryResId == R.string.report_cat_result) {
            categoryColor = 0xFFD32F2F; // red
        } else if (item.categoryResId == R.string.report_cat_marksheet) {
            categoryColor = 0xFF6A5ACD; // purple
        } else if (item.categoryResId == R.string.report_cat_tables) {
            categoryColor = 0xFF757575; // grey
        } else if (item.categoryResId == R.string.report_cat_progressbook) {
            categoryColor = 0xFF388E3C; // green
        } else {
            categoryColor = 0xFF888888;
        }
        b.tvReportCategory.setTextColor(categoryColor);

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
