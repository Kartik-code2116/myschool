package com.kartik.myschool.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.R;
import com.kartik.myschool.data.OcrRecordEntity;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Vh> {

    public interface Listener {
        void onItemClick(@NonNull OcrRecordEntity item);
    }

    private final Listener listener;
    private final List<OcrRecordEntity> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public HistoryAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<OcrRecordEntity> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_history, parent, false);
        return new Vh(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Vh holder, int position) {
        OcrRecordEntity item = items.get(position);
        holder.txtTime.setText(timeFormat.format(new Date(item.timestamp)));
        holder.txtPreview.setText(item.numbers == null ? "" : item.numbers);
        holder.chipPdf.setVisibility(item.pdfPath != null && !item.pdfPath.isEmpty() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Vh extends RecyclerView.ViewHolder {
        final TextView txtTime;
        final TextView txtPreview;
        final Chip chipPdf;

        Vh(@NonNull View itemView) {
            super(itemView);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtPreview = itemView.findViewById(R.id.txtPreview);
            chipPdf = itemView.findViewById(R.id.chipPdf);
        }
    }
}

