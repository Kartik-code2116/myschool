package com.kartik.myschool.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.databinding.ItemEvaluationReportStudentBinding;
import com.kartik.myschool.model.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EvaluationReportAdapter extends RecyclerView.Adapter<EvaluationReportAdapter.VH> {

    public static class StudentEvalItem {
        public Student student;
        public int totalMarks;
        public int outOfMarks;
        public double percentage;
        public String grade;

        public StudentEvalItem(Student student, int totalMarks, int outOfMarks, String grade) {
            this.student = student;
            this.totalMarks = totalMarks;
            this.outOfMarks = outOfMarks;
            this.percentage = outOfMarks > 0 ? ((double) totalMarks / outOfMarks) * 100.0 : 0.0;
            this.grade = grade;
        }
    }

    private final List<StudentEvalItem> items = new ArrayList<>();

    public void setData(List<StudentEvalItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEvaluationReportStudentBinding b = ItemEvaluationReportStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        StudentEvalItem item = items.get(position);
        ItemEvaluationReportStudentBinding b = holder.b;

        // Rank / Roll No
        String roll = (item.student.rollNo != null && !item.student.rollNo.isEmpty()) 
                      ? item.student.rollNo : String.valueOf(position + 1);
        b.tvRank.setText(roll);

        b.tvStudentName.setText(item.student.name);
        b.tvMarksDetail.setText(item.totalMarks + " / " + item.outOfMarks + " Marks");
        b.tvPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", item.percentage));
        b.tvGrade.setText("Grade: " + (item.grade != null ? item.grade : "-"));

        // Progress indicator
        b.progressMarks.setMax(item.outOfMarks > 0 ? item.outOfMarks : 100);
        b.progressMarks.setProgress(item.totalMarks);

        // Color coding based on Grade
        int color;
        if ("A+".equals(item.grade) || "A".equals(item.grade) || "अ-1".equals(item.grade) || "अ-2".equals(item.grade)) {
            color = 0xFF4CAF50; // Green
        } else if ("B+".equals(item.grade) || "B".equals(item.grade) || "ब-1".equals(item.grade) || "ब-2".equals(item.grade)) {
            color = 0xFF00A5CF; // Blue
        } else if ("C".equals(item.grade) || "D".equals(item.grade) || "क-1".equals(item.grade) || "क-2".equals(item.grade)) {
            color = 0xFFFFA000; // Amber
        } else {
            color = 0xFFD32F2F; // Red
        }

        b.tvPercentage.setTextColor(color);
        b.tvGrade.setTextColor(color);
        b.progressMarks.setIndicatorColor(color);
        
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
        final ItemEvaluationReportStudentBinding b;
        VH(ItemEvaluationReportStudentBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
