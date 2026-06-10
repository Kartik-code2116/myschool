package com.kartik.myschool.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.R;
import com.kartik.myschool.databinding.ItemProfileClassRowBinding;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.ProfileClassItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileClassAdapter extends RecyclerView.Adapter<ProfileClassAdapter.VH> {

    public interface Listener { void onClassClick(ProfileClassItem item); }

    private final List<ProfileClassItem> items = new ArrayList<>();
    private final Map<Integer, String> divisionsByStd = new HashMap<>();
    private Listener listener;
    private String selectedClassId;

    public void setData(List<ProfileClassItem> data, Map<Integer, String> stdDivisionsSummary) {
        items.clear();
        divisionsByStd.clear();
        if (data != null) items.addAll(data);
        if (stdDivisionsSummary != null) divisionsByStd.putAll(stdDivisionsSummary);
        notifyDataSetChanged();
    }

    public void setSelectedClassId(String classId) {
        selectedClassId = classId;
        notifyDataSetChanged();
    }

    public void setListener(Listener l) { this.listener = l; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProfileClassRowBinding row = ItemProfileClassRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(row);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ProfileClassItem item = items.get(position);
        ClassModel c = item.classModel;
        boolean hasClass = item.hasClass();
        boolean selected = hasClass && c.id != null && c.id.equals(selectedClassId);

        holder.b.tvProfileClassNum.setText(String.valueOf(item.standard));

        if (!hasClass) {
            holder.b.tvProfileClassTitle.setText(
                    holder.itemView.getContext().getString(R.string.profile_std_empty, item.standard));
            holder.b.tvProfileClassStatus.setText(R.string.profile_add_from_home);
            holder.b.panelClassExpanded.setVisibility(View.GONE);
            holder.b.ivClassSelected.setVisibility(View.GONE);
            holder.b.cardProfileClass.setAlpha(0.65f);
        } else {
            holder.b.cardProfileClass.setAlpha(1f);
            String div = item.getDivision();
            String title = holder.itemView.getContext().getString(R.string.class_div_format,
                    String.valueOf(item.standard), div);
            if (c != null && c.academicYearLabel != null && !c.academicYearLabel.isEmpty()) {
                title += " (" + c.academicYearLabel + ")";
            }
            holder.b.tvProfileClassTitle.setText(title);
            holder.b.tvProfileClassStatus.setText(
                    holder.itemView.getContext().getString(R.string.profile_students_count,
                            item.studentCount));

            holder.b.ivClassSelected.setVisibility(selected ? View.VISIBLE : View.GONE);
            if (selected) {
                holder.b.ivClassSelected.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#5A4FCF")));
            }
            holder.b.panelClassExpanded.setVisibility(View.GONE);
        }

        if (selected) {
            holder.b.cardProfileClass.setCardBackgroundColor(android.graphics.Color.parseColor("#E8E6FF"));
            holder.b.cardProfileClass.setStrokeColor(android.graphics.Color.parseColor("#5A4FCF"));
            holder.b.cardProfileClass.setStrokeWidth((int) (2 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
            holder.b.tvProfileClassNum.setTextColor(android.graphics.Color.parseColor("#5A4FCF"));
            holder.b.tvProfileClassNum.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            holder.b.tvProfileClassTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            holder.b.tvProfileClassTitle.setTextColor(android.graphics.Color.parseColor("#2E2E2E"));
            holder.b.tvProfileClassStatus.setTextColor(android.graphics.Color.parseColor("#5A4FCF"));
        } else {
            holder.b.cardProfileClass.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surface));
            holder.b.cardProfileClass.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.outline_variant));
            holder.b.cardProfileClass.setStrokeWidth((int) (1 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
            holder.b.tvProfileClassNum.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.class_number_purple));
            holder.b.tvProfileClassNum.setTypeface(android.graphics.Typeface.DEFAULT);
            holder.b.tvProfileClassTitle.setTypeface(android.graphics.Typeface.DEFAULT);
            holder.b.tvProfileClassTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.on_surface));
            holder.b.tvProfileClassStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.on_surface_variant));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && hasClass) listener.onClassClick(item);
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
