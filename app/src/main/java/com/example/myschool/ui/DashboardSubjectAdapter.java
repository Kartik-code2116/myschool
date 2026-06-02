package com.example.myschool.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myschool.R;
import com.example.myschool.model.Subject;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class DashboardSubjectAdapter extends RecyclerView.Adapter<DashboardSubjectAdapter.ViewHolder> {

    private List<SubjectStats> statsList = new ArrayList<>();

    public static class SubjectStats {
        public Subject subject;
        public int number;
        public int totalStudents;
        public int formativeFilled;
        public int summativeFilled;
        public int descriptiveFilled;

        public SubjectStats(Subject subject, int number, int totalStudents) {
            this.subject = subject;
            this.number = number;
            this.totalStudents = totalStudents;
        }
    }

    public void setData(List<SubjectStats> list) {
        this.statsList = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_subject_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubjectStats stat = statsList.get(position);
        
        holder.tvSubjectName.setText(stat.number + ". " + stat.subject.name);
        
        holder.tvFormativeCount.setText(stat.formativeFilled + " / " + stat.totalStudents);
        holder.tvSummativeCount.setText(stat.summativeFilled + " / " + stat.totalStudents);
        holder.tvDescriptiveCount.setText(stat.descriptiveFilled + " / " + stat.totalStudents);
        
        int totalMax = stat.totalStudents * 3;
        int totalFilled = stat.formativeFilled + stat.summativeFilled + stat.descriptiveFilled;
        int overallPercentage = totalMax > 0 ? (totalFilled * 100) / totalMax : 0;
        
        holder.chipCompletionStatus.setText(overallPercentage + "%");
        if (overallPercentage == 100) {
            holder.chipCompletionStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
        } else if (overallPercentage > 0) {
            holder.chipCompletionStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
        } else {
            holder.chipCompletionStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
        }
        
        if (stat.totalStudents > 0) {
            holder.pbFormative.setProgress((stat.formativeFilled * 100) / stat.totalStudents);
            holder.pbSummative.setProgress((stat.summativeFilled * 100) / stat.totalStudents);
            holder.pbDescriptive.setProgress((stat.descriptiveFilled * 100) / stat.totalStudents);
        } else {
            holder.pbFormative.setProgress(0);
            holder.pbSummative.setProgress(0);
            holder.pbDescriptive.setProgress(0);
        }
    }

    @Override
    public int getItemCount() {
        return statsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName;
        Chip chipCompletionStatus;
        TextView tvFormativeCount;
        TextView tvSummativeCount;
        TextView tvDescriptiveCount;
        ProgressBar pbFormative;
        ProgressBar pbSummative;
        ProgressBar pbDescriptive;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            chipCompletionStatus = itemView.findViewById(R.id.chipCompletionStatus);
            tvFormativeCount = itemView.findViewById(R.id.tvFormativeCount);
            tvSummativeCount = itemView.findViewById(R.id.tvSummativeCount);
            tvDescriptiveCount = itemView.findViewById(R.id.tvDescriptiveCount);
            pbFormative = itemView.findViewById(R.id.pbFormative);
            pbSummative = itemView.findViewById(R.id.pbSummative);
            pbDescriptive = itemView.findViewById(R.id.pbDescriptive);
        }
    }
}
