package com.example.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myschool.HomeActivity;
import com.example.myschool.SessionContext;
import com.example.myschool.adapter.ReportPrintingAdapter;
import com.example.myschool.databinding.FragmentReportPrintingBinding;

public class ReportPrintingFragment extends Fragment {

    private FragmentReportPrintingBinding b;
    private ReportPrintingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentReportPrintingBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        displayHeaderInfo();
    }

    private void setupRecyclerView() {
        b.rvReportCards.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReportPrintingAdapter();
        b.rvReportCards.setAdapter(adapter);
    }

    private void displayHeaderInfo() {
        String yearLabel = SessionContext.getYearLabel();
        b.tvReportPrintingYear.setText("Year: " + yearLabel);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).showCustomToolbarActions(
                    true,
                    v -> Toast.makeText(getContext(), "Report Printing Help & Guidelines", Toast.LENGTH_SHORT).show(),
                    v -> {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        popup.getMenu().add("Page Setup");
                        popup.getMenu().add("Select Margins");
                        popup.getMenu().add("Unicode Settings");
                        popup.setOnMenuItemClickListener(menuItem -> {
                            Toast.makeText(getContext(), menuItem.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
                            return true;
                        });
                        popup.show();
                    }
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).showCustomToolbarActions(false, null, null);
        }
    }
}
