package com.kartik.myschool.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.databinding.ItemAttendanceReportStudentBinding;
import com.kartik.myschool.model.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceReportAdapter extends RecyclerView.Adapter<AttendanceReportAdapter.VH> {

    public static class StudentReportItem {
        public Student student;
        public int totalPresent;
        public int totalWorking;
        public double percentage;

        public StudentReportItem(Student student, int totalPresent, int totalWorking) {
            this.student = student;
            this.totalPresent = totalPresent;
            this.totalWorking = totalWorking;
            this.percentage = totalWorking > 0 ? ((double) totalPresent / totalWorking) * 100.0 : 0.0;
        }
    }

    private final List<StudentReportItem> items = new ArrayList<>();

    public void setData(List<StudentReportItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttendanceReportStudentBinding b = ItemAttendanceReportStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        StudentReportItem item = items.get(position);
        ItemAttendanceReportStudentBinding b = holder.b;

        // Rank / Roll No (Using Roll No or fallback to position)
        String roll = (item.student.rollNo != null && !item.student.rollNo.isEmpty()) 
                      ? item.student.rollNo : String.valueOf(position + 1);
        b.tvRank.setText(roll);

        b.tvStudentName.setText(item.student.name);
        b.tvAttendanceDays.setText(item.totalPresent + " / " + item.totalWorking + " days present");
        b.tvPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", item.percentage));

        // Progress indicator
        b.progressAttendance.setMax(item.totalWorking > 0 ? item.totalWorking : 100);
        b.progressAttendance.setProgress(item.totalPresent);

        // Color coding based on percentage
        int color;
        if (item.percentage >= 90) {
            color = 0xFF4CAF50; // Green
        } else if (item.percentage >= 75) {
            color = 0xFF00A5CF; // Blue
        } else if (item.percentage >= 50) {
            color = 0xFFFFA000; // Amber
        } else {
            color = 0xFFD32F2F; // Red
        }

        b.tvPercentage.setTextColor(color);
        b.progressAttendance.setIndicatorColor(color);
        
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
        final ItemAttendanceReportStudentBinding b;
        VH(ItemAttendanceReportStudentBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
