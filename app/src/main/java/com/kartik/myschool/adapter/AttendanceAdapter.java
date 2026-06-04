package com.kartik.myschool.adapter;

import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.R;
import com.kartik.myschool.SessionContext;
import com.kartik.myschool.databinding.ItemStudentAttendanceBinding;
import com.kartik.myschool.model.AttendanceRecord;
import com.kartik.myschool.model.Student;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {

    public interface OnOptionClickListener {
        void onEdit(Student student, AttendanceRecord record);
        void onDuplicate(Student student, AttendanceRecord record);
        void onDelete(Student student, AttendanceRecord record);
    }

    private final List<Student> students = new ArrayList<>();
    private final OnOptionClickListener listener;

    public AttendanceAdapter(OnOptionClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Student> newStudents) {
        this.students.clear();
        if (newStudents != null) this.students.addAll(newStudents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentAttendanceBinding b = ItemStudentAttendanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Student s = students.get(position);
        ItemStudentAttendanceBinding b = holder.b;

        // Set serial number and name in purple
        b.tvStudentName.setText((position + 1) + ". " + s.name);

        // Retrieve attendance from Student object
        AttendanceRecord r = new AttendanceRecord();
        r.studentId = s.id;
        r.classId = s.classId;
        r.academicYear = SessionContext.getYearLabel();
        if (s.monthlyAttendance != null) {
            r.monthlyData.putAll(s.monthlyAttendance);
        }

        // Recalculate totals
        r.recalculateTotals();

        // Bind months
        bindMonthCell(b.tvJun, r.monthlyData.get("जून"));
        bindMonthCell(b.tvJul, r.monthlyData.get("जुलै"));
        bindMonthCell(b.tvAug, r.monthlyData.get("ऑगस्ट"));
        bindMonthCell(b.tvSep, r.monthlyData.get("सप्टें"));
        bindMonthCell(b.tvOct, r.monthlyData.get("ऑक्टो"));
        bindMonthCell(b.tvNov, r.monthlyData.get("नोव्हे"));
        bindMonthCell(b.tvDec, r.monthlyData.get("डिसें"));
        bindMonthCell(b.tvJan, r.monthlyData.get("जाने"));
        bindMonthCell(b.tvFeb, r.monthlyData.get("फेब्रु"));
        bindMonthCell(b.tvMar, r.monthlyData.get("मार्च"));
        bindMonthCell(b.tvApr, r.monthlyData.get("एप्रिल"));
        bindMonthCell(b.tvMay, r.monthlyData.get("मे"));

        // Bind total in large green font
        b.tvAttendanceTotal.setText(String.valueOf(r.totalPresent));

        final AttendanceRecord finalRecord = r;

        // Context menu options trigger
        b.ivOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), b.ivOptions);
            popup.getMenu().add(0, 1, 0, "बदल करा");
            popup.getMenu().add(0, 2, 1, "डुप्लिकेट");
            popup.getMenu().add(0, 3, 2, "डिलीट करा");

            popup.setOnMenuItemClickListener(item -> {
                if (listener != null) {
                    if (item.getItemId() == 1) {
                        listener.onEdit(s, finalRecord);
                    } else if (item.getItemId() == 2) {
                        listener.onDuplicate(s, finalRecord);
                    } else if (item.getItemId() == 3) {
                        listener.onDelete(s, finalRecord);
                    }
                }
                return true;
            });
            popup.show();
        });
    }

    private void bindMonthCell(TextView tv, String val) {
        if (val != null && !val.equals("0/0")) {
            tv.setText(val);
            tv.setTextColor(0xFF5E35B1); // Highlighted month
        } else {
            tv.setText("—");
            tv.setTextColor(0xFF888888); // Grey placeholder
        }
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemStudentAttendanceBinding b;
        VH(ItemStudentAttendanceBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
