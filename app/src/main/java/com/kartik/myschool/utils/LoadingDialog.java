package com.kartik.myschool.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.kartik.myschool.R;

/**
 * LoadingDialog — a Material Design 3 replacement for the deprecated android.app.ProgressDialog.
 *
 * Audit fix: ProgressDialog was deprecated since API 26. This uses MaterialAlertDialogBuilder
 * with a LinearProgressIndicator (indeterminate), which is the MD3-compliant approach.
 *
 * Usage:
 *   LoadingDialog dialog = new LoadingDialog(context, "Title", "Processing...");
 *   dialog.show();
 *   dialog.setMessage("New message...");
 *   dialog.dismiss();
 */
public class LoadingDialog {

    private final AlertDialog dialog;
    private TextView messageView;
    private TextView titleView;

    public LoadingDialog(@NonNull Context context, @Nullable String title, @Nullable String message) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        messageView = view.findViewById(R.id.loading_message);
        titleView = view.findViewById(R.id.loading_title);

        if (title != null) {
            titleView.setText(title);
            titleView.setVisibility(View.VISIBLE);
        } else {
            titleView.setVisibility(View.GONE);
        }

        if (message != null) {
            messageView.setText(message);
        }

        dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();
    }

    public void setMessage(@NonNull String message) {
        if (messageView != null) {
            messageView.post(() -> messageView.setText(message));
        }
    }

    public void show() {
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }
}
