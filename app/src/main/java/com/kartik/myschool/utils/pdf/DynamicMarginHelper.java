package com.kartik.myschool.utils.pdf;

import android.content.Context;
import android.content.SharedPreferences;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

public class DynamicMarginHelper extends PdfPageEventHelper {
    public static int currentReportIndex = -1; // -1 means global

    private Context ctx;

    public DynamicMarginHelper(Context ctx) {
        this.ctx = ctx;
    }

    // Convert millimeters to points (1 mm = 2.83465 points)
    // We treat the inputs from UI as millimeters.
    public static float mmToPts(int mm) {
        return mm * 2.83465f;
    }

    public static void applyMarginsForPage(Context ctx, Document document, int pageNum) {
        SharedPreferences prefs = ctx.getSharedPreferences("report_margins", Context.MODE_PRIVATE);
        String prefix = currentReportIndex == -1 ? "global_" : "report_" + currentReportIndex + "_";

        int top = prefs.getInt(prefix + "p" + pageNum + "_top", -1);
        if (top == -1 && currentReportIndex != -1) {
            top = prefs.getInt("global_p" + pageNum + "_top", -1);
        }
        
        int left = prefs.getInt(prefix + "p" + pageNum + "_left", -1);
        if (left == -1 && currentReportIndex != -1) left = prefs.getInt("global_p" + pageNum + "_left", -1);
        
        int right = prefs.getInt(prefix + "p" + pageNum + "_right", -1);
        if (right == -1 && currentReportIndex != -1) right = prefs.getInt("global_p" + pageNum + "_right", -1);
        
        int bottom = prefs.getInt(prefix + "p" + pageNum + "_bottom", -1);
        if (bottom == -1 && currentReportIndex != -1) bottom = prefs.getInt("global_p" + pageNum + "_bottom", -1);

        // Apply if any were found, otherwise keep existing
        if (top != -1 && left != -1 && right != -1 && bottom != -1) {
            document.setMargins(mmToPts(left), mmToPts(right), mmToPts(top), mmToPts(bottom));
        }
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        // Apply for page 1
        applyMarginsForPage(ctx, document, 1);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        // When page N ends, apply margins for page N+1
        int nextPage = writer.getPageNumber() + 1;
        applyMarginsForPage(ctx, document, nextPage);
    }
}
