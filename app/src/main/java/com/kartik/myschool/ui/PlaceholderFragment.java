package com.kartik.myschool.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kartik.myschool.HomeActivity;
import com.kartik.myschool.R;
import com.kartik.myschool.databinding.FragmentPlaceholderBinding;

public class PlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";

    public static PlaceholderFragment newInstance(String title) {
        PlaceholderFragment f = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentPlaceholderBinding b = FragmentPlaceholderBinding.inflate(inflater, container, false);
        String title = "";
        if (getArguments() != null) {
            title = getArguments().getString("title", "");
            if (title.isEmpty()) title = getArguments().getString(ARG_TITLE, "");
        }
        b.tvPlaceholderTitle.setText(title.isEmpty() ? getString(R.string.nav_reports) : title);
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).updateToolbar(title, "");
        }
        return b.getRoot();
    }
}
