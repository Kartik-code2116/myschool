package com.example.myschool.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myschool.R;
import com.example.myschool.databinding.ItemProfileClassRowBinding;
import com.example.myschool.model.ClassModel;

import java.util.ArrayList;
import java.util.List;

public class ProfileClassAdapter extends RecyclerView.Adapter<ProfileClassAdapter.VH> {

    public interface Listener { void onClassClick(ClassModel c); }

    private final List<ClassModel> items = new ArrayList<>();
    private Listener listener;

    public void setData(List<ClassModel> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void setListener(Listener l) { this.listener = l; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProfileClassRowBinding b = ItemProfileClassRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ClassModel c = items.get(position);
        String classNum = c.className != null ? c.className : "?";
        String div = c.division != null && !c.division.isEmpty() ? c.division : "-";
        holder.b.tvProfileClassNum.setText(classNum);
        holder.b.tvProfileClassTitle.setText(
                holder.itemView.getContext().getString(R.string.class_div_format, classNum, div));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClassClick(c);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemProfileClassRowBinding b;
        VH(ItemProfileClassRowBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
