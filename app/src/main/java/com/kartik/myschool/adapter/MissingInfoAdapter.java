package com.kartik.myschool.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.R;
import com.kartik.myschool.model.Student;

import java.util.ArrayList;
import java.util.List;

public class MissingInfoAdapter extends RecyclerView.Adapter<MissingInfoAdapter.VH> {

    public static class MissingInfoItem {
        public Student student;
        public List<String> missingDetails;

        public MissingInfoItem(Student student, List<String> missingDetails) {
            this.student = student;
            this.missingDetails = missingDetails != null ? missingDetails : new ArrayList<>();
        }
    }

    private final List<MissingInfoItem> items = new ArrayList<>();
    private final int[] lastAnimatedPos = {-1};

    public void setData(List<MissingInfoItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_missing_info, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MissingInfoItem item = items.get(position);

        String roll = (item.student.rollNo != null && !item.student.rollNo.isEmpty()) ? item.student.rollNo : String.valueOf(position + 1);
        holder.tvRoll.setText(roll);
        holder.tvName.setText(item.student.name != null ? item.student.name : "Unknown");

        if (item.missingDetails.isEmpty()) {
            holder.ivStatus.setImageResource(R.drawable.ic_check_circle);
            holder.ivStatus.setColorFilter(0xFF4CAF50); // Green tick
            holder.tvDetails.setVisibility(View.GONE);
        } else {
            holder.ivStatus.setImageResource(R.drawable.ic_close);
            holder.ivStatus.setColorFilter(0xFFD32F2F); // Red warning
            holder.tvDetails.setVisibility(View.VISIBLE);

            StringBuilder sb = new StringBuilder("Missing: ");
            for (int i = 0; i < item.missingDetails.size(); i++) {
                sb.append(item.missingDetails.get(i));
                if (i < item.missingDetails.size() - 1) sb.append(", ");
            }
            holder.tvDetails.setText(sb.toString());
        }

        com.kartik.myschool.utils.UiAnimations.animateCardPop(holder.itemView, position, lastAnimatedPos);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvRoll, tvName, tvDetails;
        ImageView ivStatus;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvRoll = itemView.findViewById(R.id.tvMissingInfoRoll);
            tvName = itemView.findViewById(R.id.tvMissingInfoName);
            tvDetails = itemView.findViewById(R.id.tvMissingInfoDetails);
            ivStatus = itemView.findViewById(R.id.ivMissingInfoStatus);
        }
    }
}
