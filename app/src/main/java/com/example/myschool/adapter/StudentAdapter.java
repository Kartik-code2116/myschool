package com.example.myschool.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myschool.R;
import com.example.myschool.model.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.VH> {

    public interface OnStudentClick {
        void onClick(Student student, int position);
        void onEnterMarksClick(Student student, int position);
        void onViewMarksheetClick(Student student, int position);
    }

    private final List<Student> items = new ArrayList<>();
    private OnStudentClick listener;

    public void setData(List<Student> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void setListener(OnStudentClick l) {
        this.listener = l;
    }

    public List<Student> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Student s = items.get(pos);
        h.tvName.setText(s.name);

        // Bug #5 fix: display denormalized class name and school name instead of raw IDs
        String classDisplay = (s.className != null && !s.className.isEmpty())
                ? s.className : "Class N/A";
        h.tvRollClass.setText("Roll: " + (s.rollNo != null ? s.rollNo : "—") + " | " + classDisplay);
        h.tvSchool.setText(s.schoolName != null && !s.schoolName.isEmpty() ? s.schoolName : "");

        // Status chip
        if (s.marksEntered) {
            h.chipStatus.setText("Done");
            h.chipStatus.setChipBackgroundColorResource(R.color.success_container);
            h.chipStatus.setTextColor(h.itemView.getContext().getResources().getColor(R.color.success, null));
        } else {
            h.chipStatus.setText("Pending");
            h.chipStatus.setChipBackgroundColorResource(R.color.surface_variant);
            h.chipStatus.setTextColor(h.itemView.getContext().getResources().getColor(R.color.on_surface_variant, null));
        }

        // Photo
        if (s.photoUrl != null && !s.photoUrl.isEmpty()) {
            Glide.with(h.ivPhoto).load(s.photoUrl).circleCrop().into(h.ivPhoto);
        } else {
            h.ivPhoto.setImageResource(R.drawable.ic_person);
        }

        // Bug #4 fix: route tap based on marksEntered status so onEnterMarksClick is actually reachable.
        // Tap: if marks not entered → enter marks; if done → view marksheet.
        // Long press: edit student details.
        h.itemView.setOnClickListener(v -> {
            if (listener == null) return;
            if (s.marksEntered) {
                listener.onViewMarksheetClick(s, pos);
            } else {
                listener.onEnterMarksClick(s, pos);
            }
        });

        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onClick(s, pos);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvRollClass, tvSchool;
        com.google.android.material.chip.Chip chipStatus;

        VH(@NonNull View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.ivStudentAvatar);
            tvName = v.findViewById(R.id.tvStudentName);
            tvRollClass = v.findViewById(R.id.tvRollClass);
            tvSchool = v.findViewById(R.id.tvSchoolName);
            chipStatus = v.findViewById(R.id.chipMarksStatus);
        }
    }
}
