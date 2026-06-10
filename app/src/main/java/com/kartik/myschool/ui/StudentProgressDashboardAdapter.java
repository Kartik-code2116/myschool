package com.kartik.myschool.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.R;
import com.kartik.myschool.model.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentProgressDashboardAdapter extends RecyclerView.Adapter<StudentProgressDashboardAdapter.ViewHolder> {

    public static class StudentProgressStats {
        public Student student;
        public int totalFormativeSubjects;
        public int totalSummativeSubjects;
        public int totalDescriptiveSubjects;
        public int formativeFilled;
        public int summativeFilled;
        public int descriptiveFilled;

        public StudentProgressStats(Student student, int totalFormativeSubjects, int totalSummativeSubjects, int totalDescriptiveSubjects) {
            this.student = student;
            this.totalFormativeSubjects = totalFormativeSubjects;
            this.totalSummativeSubjects = totalSummativeSubjects;
            this.totalDescriptiveSubjects = totalDescriptiveSubjects;
        }
    }

    private List<StudentProgressStats> dataList = new ArrayList<>();

    public void setData(List<StudentProgressStats> data) {
        this.dataList.clear();
        if (data != null) {
            this.dataList.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_progress, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentProgressStats stat = dataList.get(position);

        holder.tvStudentName.setText((position + 1) + ". " + stat.student.name);
        holder.tvRollNo.setText("Roll No: " + stat.student.rollNo);

        holder.tvFormativeProgress.setText(stat.formativeFilled + "/" + stat.totalFormativeSubjects);
        holder.tvSummativeProgress.setText(stat.summativeFilled + "/" + stat.totalSummativeSubjects);
        holder.tvDescriptiveProgress.setText(stat.descriptiveFilled + "/" + stat.totalDescriptiveSubjects);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvRollNo;
        TextView tvFormativeProgress, tvSummativeProgress, tvDescriptiveProgress;

        ViewHolder(View v) {
            super(v);
            tvStudentName = v.findViewById(R.id.tvStudentName);
            tvRollNo = v.findViewById(R.id.tvRollNo);
            tvFormativeProgress = v.findViewById(R.id.tvFormativeProgress);
            tvSummativeProgress = v.findViewById(R.id.tvSummativeProgress);
            tvDescriptiveProgress = v.findViewById(R.id.tvDescriptiveProgress);
        }
    }
}
