package com.kartik.myschool.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kartik.myschool.utils.UiAnimations;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kartik.myschool.R;
import com.kartik.myschool.model.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.VH> {

    public interface OnStudentClick {
        void onClick(View view, Student student, int position);
        void onEnterMarksClick(Student student, int position);
        void onAttendanceClick(Student student, int position);
        void onEditInfoClick(Student student, int position);
        void onDeleteClick(Student student, int position);
    }

    private final List<Student> items = new ArrayList<>();
    private OnStudentClick listener;
    // Tracks last animated position so each item animates only once per load
    private final int[] lastAnimatedPos = {-1};

    private boolean isMultiSelectMode = false;
    private final java.util.Set<String> selectedStudentIds = new java.util.HashSet<>();

    public void setMultiSelectMode(boolean mode) {
        this.isMultiSelectMode = mode;
        if (!mode) selectedStudentIds.clear();
        notifyDataSetChanged();
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void toggleSelection(String studentId) {
        if (selectedStudentIds.contains(studentId)) {
            selectedStudentIds.remove(studentId);
        } else {
            selectedStudentIds.add(studentId);
        }
        notifyDataSetChanged();
    }

    public List<String> getSelectedStudentIds() {
        return new ArrayList<>(selectedStudentIds);
    }

    public void setData(List<Student> data) {
        items.clear();
        if (data != null) items.addAll(data);
        lastAnimatedPos[0] = -1; // Reset so new data animates fresh
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
        h.tvName.setText(s.name != null ? s.name : "kartik");

        String roll1Display = (s.rollNo != null && !s.rollNo.isEmpty()) ? s.rollNo : "1";
        String roll2Display = (s.rollNo2 != null && !s.rollNo2.isEmpty()) ? s.rollNo2 : "1";
        String regDisplay = (s.registrationNo != null && !s.registrationNo.isEmpty()) ? s.registrationNo : "1011021645";
        String dobDisplay = (s.dob != null && !s.dob.isEmpty()) ? s.dob : "25-05-2012";
        String uidDisplay = (s.uid != null && !s.uid.isEmpty()) ? s.uid : (s.studentIdNumber != null && !s.studentIdNumber.isEmpty() ? s.studentIdNumber : "215454");
        String stdDisplay = (s.standard != null && !s.standard.isEmpty()) ? s.standard : "1";
        String divDisplay = (s.division != null && !s.division.isEmpty()) ? s.division : "1";

        h.tvRoll1.setText("Roll 1 : " + roll1Display);
        h.tvRoll2.setText("Roll 2 : " + roll2Display);
        h.tvDob.setText("DOB - " + dobDisplay);
        h.tvRegNo.setText("Reg No: " + uidDisplay);
        
        h.tvAvatarRegNo.setText(regDisplay);
        h.tvAvatarStd.setText(stdDisplay);
        
        h.tvTag1.setText(stdDisplay);
        h.tvTag2.setText(divDisplay);

        // Photo / Avatar binding
        bindPhoto(h, s);

        // Multi-select visuals
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) h.itemView;
        if (isMultiSelectMode) {
            h.btnOptions.setVisibility(View.GONE);
            if (selectedStudentIds.contains(s.id)) {
                card.setStrokeWidth(6);
                card.setStrokeColor(android.graphics.Color.parseColor("#4CAF50")); // Green highlight
            } else {
                card.setStrokeWidth(3);
                card.setStrokeColor(card.getContext().getResources().getColor(R.color.outline_variant));
            }
        } else {
            h.btnOptions.setVisibility(View.VISIBLE);
            card.setStrokeWidth(3);
            card.setStrokeColor(card.getContext().getResources().getColor(R.color.outline_variant));
        }

        h.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                toggleSelection(s.id);
            } else {
                if (listener != null) listener.onClick(v, s, pos);
            }
        });

        h.btnOptions.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 0, v.getContext().getString(R.string.menu_student_evaluation));
            popup.getMenu().add(0, 2, 1, v.getContext().getString(R.string.menu_student_attendance));
            popup.getMenu().add(0, 3, 2, v.getContext().getString(R.string.menu_student_edit_info));
            popup.getMenu().add(0, 4, 3, v.getContext().getString(R.string.menu_student_delete));
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
        // Scroll-reveal animation: slides up + fades in as new items appear
        UiAnimations.animateScrollReveal(h.itemView, pos, lastAnimatedPos);
    }

    private void bindPhoto(VH h, Student s) {
        if (s.photoUrl != null && !s.photoUrl.isEmpty()) {
            h.ivPhoto.setPadding(0, 0, 0, 0);
            h.ivPhoto.setImageTintList(null);
            if (s.photoUrl.startsWith("data:image")) {
                if (s.cachedPhotoBitmap != null) {
                    h.ivPhoto.setImageBitmap(s.cachedPhotoBitmap);
                } else {
                    try {
                        String base64Data = s.photoUrl.substring(s.photoUrl.indexOf(",") + 1);
                        byte[] decodedString = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                        android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        s.cachedPhotoBitmap = decodedByte;
                        h.ivPhoto.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        bindPlaceholder(h, s);
                    }
                }
            } else {
                Glide.with(h.ivPhoto.getContext())
                        .load(s.photoUrl)
                        .centerCrop()
                        .placeholder(getPlaceholderRes(s))
                        .into(h.ivPhoto);
            }
        } else {
            bindPlaceholder(h, s);
        }
    }

    private int getPlaceholderRes(Student s) {
        boolean isFemale = s.gender != null && (s.gender.equalsIgnoreCase("Female") 
                || s.gender.equalsIgnoreCase("स्त्री") 
                || s.gender.equalsIgnoreCase("मुलगी")
                || s.gender.equals("2"));
        return isFemale ? R.drawable.ic_girl_avatar : R.drawable.ic_boy_avatar;
    }

    private void bindPlaceholder(VH h, Student s) {
        h.ivPhoto.setImageResource(getPlaceholderRes(s));
        h.ivPhoto.setPadding(0, 0, 0, 0);
        h.ivPhoto.setImageTintList(null);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvRoll1, tvRoll2, tvDob, tvRegNo;
        TextView tvAvatarRegNo, tvAvatarStd;
        TextView tvTag1, tvTag2;
        android.widget.ImageButton btnOptions;

        VH(@NonNull View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.ivStudentAvatar);
            tvName = v.findViewById(R.id.tvStudentName);
            tvRoll1 = v.findViewById(R.id.tvRoll1);
            tvRoll2 = v.findViewById(R.id.tvRoll2);
            tvDob = v.findViewById(R.id.tvDob);
            tvRegNo = v.findViewById(R.id.tvRegNo);
            tvAvatarRegNo = v.findViewById(R.id.tvAvatarRegNo);
            tvAvatarStd = v.findViewById(R.id.tvAvatarStd);
            tvTag1 = v.findViewById(R.id.tvTag1);
            tvTag2 = v.findViewById(R.id.tvTag2);
            btnOptions = v.findViewById(R.id.btnOptions);
        }
    }
}
