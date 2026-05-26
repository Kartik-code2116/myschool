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
        void onAttendanceClick(Student student, int position);
        void onEditInfoClick(Student student, int position);
        void onDeleteClick(Student student, int position);
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

        String rollDisplay = (s.rollNo != null && !s.rollNo.isEmpty()) ? s.rollNo : "—";
        String regDisplay = (s.registrationNo != null && !s.registrationNo.isEmpty()) ? s.registrationNo : "—";
        h.tvRollClass.setText("Roll No: " + rollDisplay + " | Reg No: " + regDisplay);
        h.tvSchool.setText(""); // Hide school name or any exam related info as requested

        // Photo
        if (s.photoUrl != null && !s.photoUrl.isEmpty()) {
            Glide.with(h.ivPhoto).load(s.photoUrl).circleCrop().into(h.ivPhoto);
        } else {
            h.ivPhoto.setImageResource(R.drawable.ic_person);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s, pos);
        });

        h.btnOptions.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 0, "Evaluation");
            popup.getMenu().add(0, 2, 1, "Attendance");
            popup.getMenu().add(0, 3, 2, "Edit Info");
            popup.getMenu().add(0, 4, 3, "Delete");
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == 1) {
                    if (listener != null) listener.onEnterMarksClick(s, pos);
                    return true;
                } else if (itemId == 2) {
                    if (listener != null) listener.onAttendanceClick(s, pos);
                    return true;
                } else if (itemId == 3) {
                    if (listener != null) listener.onEditInfoClick(s, pos);
                    return true;
                } else if (itemId == 4) {
                    if (listener != null) listener.onDeleteClick(s, pos);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvRollClass, tvSchool;
        android.widget.ImageButton btnOptions;

        VH(@NonNull View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.ivStudentAvatar);
            tvName = v.findViewById(R.id.tvStudentName);
            tvRollClass = v.findViewById(R.id.tvRollClass);
            tvSchool = v.findViewById(R.id.tvSchoolName);
            btnOptions = v.findViewById(R.id.btnOptions);
        }
    }
}
