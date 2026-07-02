package com.kartik.myschool.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.R;
import com.kartik.myschool.databinding.ItemClassDivCardBinding;
import com.kartik.myschool.model.ClassModel;

import java.util.ArrayList;
import java.util.List;

public class ClassDivAdapter extends RecyclerView.Adapter<ClassDivAdapter.VH> {

    public interface Listener {
        void onClassClick(ClassModel c, int position);
        void onClassLongClick(ClassModel c);
        void onClassOptionsClick(ClassModel c, View anchorView, int position);
    }

    private final List<ClassModel> data = new ArrayList<>();
    private int selectedIndex = 0;
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<ClassModel> list, int selected) {
        data.clear();
        if (list != null) data.addAll(list);
        selectedIndex = selected;
        notifyDataSetChanged();
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemClassDivCardBinding b = ItemClassDivCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ClassModel c = data.get(position);
        ItemClassDivCardBinding b = holder.b;

        String classNum = c.className != null ? c.className : "?";
        String div = c.division != null && !c.division.isEmpty() ? c.division : "-";

        b.tvClassBigNumber.setText(classNum);
        
        // Add proper suffix (st, nd, rd, th) based on class number
        String suffix = "th";
        if (classNum.equals("1")) suffix = "st";
        else if (classNum.equals("2")) suffix = "nd";
        else if (classNum.equals("3")) suffix = "rd";
        else if (classNum.equals("Jr. KG") || classNum.equals("Sr. KG")) suffix = "";
        
        b.tvClassSmallNumber.setText(suffix);
        b.tvClassDivTitle.setText(
                holder.itemView.getContext().getString(R.string.class_div_format, classNum, div));

        String t1 = c.teacherName != null && !c.teacherName.isEmpty() ? c.teacherName : holder.itemView.getContext().getString(R.string.teacher_placeholder);
        b.tvTeacher1.setText("👨‍🏫 Teacher: " + t1);
        
        if (c.assistantTeacherName != null && !c.assistantTeacherName.isEmpty()) {
            b.tvTeacher2.setVisibility(View.VISIBLE);
            b.tvTeacher2.setText("👨‍🏫 Asst: " + c.assistantTeacherName);
        } else {
            b.tvTeacher2.setVisibility(View.GONE);
        }

        int count = c.studentCount;
        b.tvStatTop.setText(count + " Students");
        b.tvStatMid.setVisibility(View.GONE);
        b.tvStatBottom.setVisibility(View.GONE);

        b.cardClassDiv.setCardBackgroundColor(
                holder.itemView.getContext().getColor(
                        position == selectedIndex ? R.color.class_card_selected : R.color.surface));

        b.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onClassClick(c, position);
        });
        b.getRoot().setOnLongClickListener(v -> {
            if (listener != null) listener.onClassLongClick(c);
            return true;
        });
        b.btnClassOptions.setOnClickListener(v -> {
            if (listener != null) listener.onClassOptionsClick(c, v, position);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemClassDivCardBinding b;
        VH(ItemClassDivCardBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
