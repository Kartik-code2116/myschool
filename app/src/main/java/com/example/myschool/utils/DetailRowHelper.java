package com.example.myschool.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myschool.R;

public final class DetailRowHelper {

    private DetailRowHelper() {}

    public static void addRow(Context ctx, LinearLayout container, String label, String value) {
        addRow(ctx, container, label, value, true);
    }

    public static void addRow(Context ctx, LinearLayout container, String label, String value,
                              boolean showDividerAfter) {
        View row = LayoutInflater.from(ctx).inflate(R.layout.item_detail_row, container, false);
        TextView tvLabel = row.findViewById(R.id.tvDetailLabel);
        TextView tvValue = row.findViewById(R.id.tvDetailValue);
        tvLabel.setText(label);
        String display = value != null && !value.isEmpty() ? value : "—";
        tvValue.setText(display);
        container.addView(row);
        if (showDividerAfter) {
            View divider = new View(ctx);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(ctx.getColor(R.color.outline_variant));
            container.addView(divider);
        }
    }

    public static void fillRows(Context ctx, LinearLayout container, String[][] labelValuePairs) {
        container.removeAllViews();
        if (labelValuePairs == null || labelValuePairs.length == 0) return;
        for (int i = 0; i < labelValuePairs.length; i++) {
            String label = labelValuePairs[i][0];
            String value = labelValuePairs[i].length > 1 ? labelValuePairs[i][1] : null;
            boolean divider = i < labelValuePairs.length - 1;
            addRow(ctx, container, label, value, divider);
        }
    }
}
