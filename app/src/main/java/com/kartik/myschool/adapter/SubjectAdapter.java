package com.kartik.myschool.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import com.kartik.myschool.R;
import com.kartik.myschool.databinding.ItemSubjectCardBinding;
import com.kartik.myschool.model.Subject;
import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.VH> {

    public static class SubjectItem {
        public String name;
        public String code;
        public String serial;
        public String category; // "Academic", "Activities", "Personality"
        public int maxMarks;
        public String detailsLeft1;
        public String detailsLeft2;
        public String detailsRight1;
        public String colorHex;

        public SubjectItem(String name, String code, String serial, String category, int maxMarks,
                           String detailsLeft1, String detailsLeft2, String detailsRight1, String colorHex) {
            this.name = name;
            this.code = code;
            this.serial = serial;
            this.category = category;
            this.maxMarks = maxMarks;
            this.detailsLeft1 = detailsLeft1;
            this.detailsLeft2 = detailsLeft2;
            this.detailsRight1 = detailsRight1;
            this.colorHex = colorHex;
        }
    }

    public interface OnToggleListener {
        void onToggle(SubjectItem item, boolean isActive);
    }

    private final List<SubjectItem> items = new ArrayList<>();
    private final List<String> activeSubjectNames = new ArrayList<>();
    private OnToggleListener toggleListener;

    public void setOnToggleListener(OnToggleListener listener) {
        this.toggleListener = listener;
    }

    public void setData(List<SubjectItem> list, List<Subject> activeList) {
        items.clear();
        if (list != null) items.addAll(list);
        
        activeSubjectNames.clear();
        if (activeList != null) {
            for (Subject s : activeList) {
                if (s.name != null) activeSubjectNames.add(s.name);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSubjectCardBinding b = ItemSubjectCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SubjectItem item = items.get(position);
        ItemSubjectCardBinding b = holder.b;

        b.tvSubjectCode.setText(item.code);
        b.tvSerialNumber.setText(item.serial);
        b.tvSubjectName.setText(item.name);
        b.tvDetailsLeft1.setText(item.detailsLeft1);
        b.tvDetailsLeft2.setText(item.detailsLeft2);
        b.tvDetailsRight1.setText(item.detailsRight1);

        int color = Color.parseColor(item.colorHex);
        b.tvSerialNumber.setTextColor(color);
        b.tvSubjectName.setTextColor(color);

        // Toggle state
        boolean isActive = activeSubjectNames.contains(item.name);
        
        // Prevent trigger during binding
        b.switchApplicable.setOnCheckedChangeListener(null);
        b.switchApplicable.setChecked(isActive);
        
        // Premium toggle Switch ON colors matching subject color!
        ColorStateList thumbStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        color,
                        Color.parseColor("#BDBDBD")
                }
        );
        ColorStateList trackStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        Color.argb(77, Color.red(color), Color.green(color), Color.blue(color)),
                        Color.parseColor("#E0E0E0")
                }
        );
        b.switchApplicable.setThumbTintList(thumbStateList);
        b.switchApplicable.setTrackTintList(trackStateList);

        b.switchApplicable.setOnCheckedChangeListener((btn, checked) -> {
            if (toggleListener != null) {
                toggleListener.onToggle(item, checked);
            }
        });

        // Click card to update subject details
        b.cardSubject.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), com.kartik.myschool.SubjectUpdateActivity.class);
            intent.putExtra("subject_name", item.name);
            intent.putExtra("subject_code", item.code);
            intent.putExtra("subject_serial", item.serial);
            intent.putExtra("subject_category", item.category);
            intent.putExtra("subject_max_marks", item.maxMarks);
            intent.putExtra("details_left_1", item.detailsLeft1);
            intent.putExtra("details_left_2", item.detailsLeft2);
            intent.putExtra("details_right_1", item.detailsRight1);
            v.getContext().startActivity(intent);
        });

        // Popup three-dot menu
        b.btnCardMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add("Details");
            popup.getMenu().add("Edit Max Marks");
            popup.setOnMenuItemClickListener(menuItem -> {
                android.content.Intent intent = new android.content.Intent(v.getContext(), com.kartik.myschool.SubjectUpdateActivity.class);
                intent.putExtra("subject_name", item.name);
                intent.putExtra("subject_code", item.code);
                intent.putExtra("subject_serial", item.serial);
                intent.putExtra("subject_category", item.category);
                intent.putExtra("subject_max_marks", item.maxMarks);
                intent.putExtra("details_left_1", item.detailsLeft1);
                intent.putExtra("details_left_2", item.detailsLeft2);
                intent.putExtra("details_right_1", item.detailsRight1);
                v.getContext().startActivity(intent);
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemSubjectCardBinding b;
        VH(ItemSubjectCardBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
