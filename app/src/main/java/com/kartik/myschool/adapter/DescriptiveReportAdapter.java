package com.kartik.myschool.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.databinding.ItemDescriptiveReportStudentBinding;
import com.kartik.myschool.model.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DescriptiveReportAdapter extends RecyclerView.Adapter<DescriptiveReportAdapter.VH> {

    public static class StudentRemarkItem {
        public Student student;
        public int remarksFilled;
        public int totalAttributes;
        public double percentage;

        public StudentRemarkItem(Student student, int remarksFilled, int totalAttributes) {
            this.student = student;
            this.remarksFilled = remarksFilled;
            this.totalAttributes = totalAttributes;
            this.percentage = totalAttributes > 0 ? ((double) remarksFilled / totalAttributes) * 100.0 : 0.0;
        }
    }

    private final List<StudentRemarkItem> items = new ArrayList<>();

    public void setData(List<StudentRemarkItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDescriptiveReportStudentBinding b = ItemDescriptiveReportStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        StudentRemarkItem item = items.get(position);
        ItemDescriptiveReportStudentBinding b = holder.b;

        // Rank / Roll No
        String roll = (item.student.rollNo != null && !item.student.rollNo.isEmpty()) 
                      ? item.student.rollNo : String.valueOf(position + 1);
        b.tvRank.setText(roll);

        b.tvStudentName.setText(item.student.name);
        b.tvRemarksDetail.setText(item.remarksFilled + " / " + item.totalAttributes + " Remarks Filled");
        b.tvPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", item.percentage));

        // Progress indicator
        b.progressRemarks.setMax(item.totalAttributes > 0 ? item.totalAttributes : 100);
        b.progressRemarks.setProgress(item.remarksFilled);

        // Color coding based on completion
        int color;
        if (item.percentage >= 100) {
            color = 0xFF4CAF50; // Green
        } else if (item.percentage >= 75) {
            color = 0xFF00A5CF; // Blue
        } else if (item.percentage >= 50) {
            color = 0xFFFFA000; // Amber
        } else {
            color = 0xFFD32F2F; // Red
        }

        b.tvPercentage.setTextColor(color);
        b.progressRemarks.setIndicatorColor(color);
        
        // Soft background for rank circle to match color
        b.tvRank.setTextColor(color);
        b.tvRank.setBackgroundTintList(ColorStateList.valueOf(
                androidx.core.graphics.ColorUtils.setAlphaComponent(color, 25)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemDescriptiveReportStudentBinding b;
        VH(ItemDescriptiveReportStudentBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
