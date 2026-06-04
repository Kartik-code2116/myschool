package com.kartik.myschool.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kartik.myschool.R;
import com.kartik.myschool.model.School;

import java.util.ArrayList;
import java.util.List;

public class SchoolAdapter extends RecyclerView.Adapter<SchoolAdapter.VH> {

    public interface OnSchoolClick { void onClick(School school); }

    private final List<School> items = new ArrayList<>();
    private OnSchoolClick listener;

    public void setData(List<School> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void setListener(OnSchoolClick l) { this.listener = l; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_school_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        School s = items.get(pos);
        h.tvName.setText(s.name);
        h.tvBoard.setText(s.board != null ? s.board : "");
        h.tvCount.setText(s.studentCount + " students");
        if (s.logoUrl != null && !s.logoUrl.isEmpty()) {
            Glide.with(h.ivLogo).load(s.logoUrl).circleCrop().into(h.ivLogo);
        }
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(s); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivLogo;
        TextView tvName, tvBoard, tvCount;
        VH(@NonNull View v) {
            super(v);
            ivLogo  = v.findViewById(R.id.ivSchoolLogo);
            tvName  = v.findViewById(R.id.tvSchoolCardName);
            tvBoard = v.findViewById(R.id.tvSchoolCardBoard);
            tvCount = v.findViewById(R.id.tvSchoolCardStudentCount);
        }
    }
}
