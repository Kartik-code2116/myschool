package com.kartik.myschool.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.R;
import com.kartik.myschool.model.Student;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentPromoteAdapter extends RecyclerView.Adapter<StudentPromoteAdapter.Holder> {

    private final List<Student> students = new ArrayList<>();
    private final Set<String> selectedStudentIds = new HashSet<>();
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int checkedCount, int totalCount);
    }

    public void setListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void setData(List<Student> list) {
        this.students.clear();
        this.selectedStudentIds.clear();
        if (list != null) {
            this.students.addAll(list);
            for (Student s : list) {
                if (s.id != null) {
                    this.selectedStudentIds.add(s.id);
                }
            }
        }
        notifyDataSetChanged();
        triggerListener();
    }

    public void selectAll(boolean select) {
        selectedStudentIds.clear();
        if (select) {
            for (Student s : students) {
                if (s.id != null) {
                    selectedStudentIds.add(s.id);
                }
            }
        }
        notifyDataSetChanged();
        triggerListener();
    }

    public List<Student> getSelectedStudents() {
        List<Student> selected = new ArrayList<>();
        for (Student s : students) {
            if (s.id != null && selectedStudentIds.contains(s.id)) {
                selected.add(s);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_promote_select, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Student s = students.get(position);
        holder.tvName.setText(s.name != null ? s.name : "Unknown");
        holder.tvRoll.setText(s.rollNo != null ? s.rollNo : "-");
        
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(s.id != null && selectedStudentIds.contains(s.id));
        
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (s.id != null) {
                if (isChecked) {
                    selectedStudentIds.add(s.id);
                } else {
                    selectedStudentIds.remove(s.id);
                }
                triggerListener();
            }
        });

        holder.itemView.setOnClickListener(v -> holder.checkBox.toggle());
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    private void triggerListener() {
        if (listener != null) {
            listener.onSelectionChanged(selectedStudentIds.size(), students.size());
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvRoll;
        TextView tvName;

        Holder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbSelect);
            tvRoll = itemView.findViewById(R.id.tvRollNo);
            tvName = itemView.findViewById(R.id.tvStudentName);
        }
    }
}
