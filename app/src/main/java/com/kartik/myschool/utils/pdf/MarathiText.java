package com.kartik.myschool.utils.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.kartik.myschool.utils.PdfGenerator;

import java.io.ByteArrayOutputStream;

/**
 * Renders Marathi/Devanagari text correctly using Android's native text shaping
 * (StaticLayout → Bitmap → iText Image).
 *
 * iText 5 cannot apply OpenType GSUB/GPOS tables, so complex matras and
 * conjuncts break when embedded as raw font + text. This utility bypasses that
 * by asking Android's own text engine to do the shaping at high DPI, then
 * embedding the result as a PNG image inside the PDF cell.
 *
 * Usage:
 *   // Single-line cell (most table headers / labels)
 *   MarathiText.cell(table, "शाळेचे नाव", 11, bold, bgColor, colSpan, rowSpan, alignment);
 *
 *   // Multi-line / paragraph cell (remarks, descriptions)
 *   MarathiText.paraCell(table, "लंबी नोंद ...", 10, normal, bgColor, colSpan, 1, width_pt);
 */
public class MarathiText {

    /** Pixels per point — render at 3× for crisp output on 150–300 dpi prints. */
    private static final float SCALE = 3.0f;

    // ──────────────────────────────────────────────────────────────────────────
    // Public: add a table cell whose text is rendered by Android's Marathi engine
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Renders Marathi text as a bitmap and adds it to a PdfPTable cell.
     *
     * @param table     Target PdfPTable
     * @param text      Marathi (or mixed) text
     * @param textSizePt Desired text size in PDF points (e.g. 9, 11)
     * @param bold      true for bold style
     * @param bgColor   Cell background colour (null = white)
     * @param textColor Android Color int (e.g. Color.BLACK)
     * @param colSpan   Column span
     * @param rowSpan   Row span
     * @param align     Element.ALIGN_CENTER / ALIGN_LEFT / ALIGN_RIGHT
     */
    public static void cell(PdfPTable table,
                             String text,
                             float textSizePt,
                             boolean bold,
                             BaseColor bgColor,
                             int textColor,
                             int colSpan,
                             int rowSpan,
                             int align) {
        PdfPCell c = new PdfPCell();
        if (bgColor != null) c.setBackgroundColor(bgColor);
        c.setBorderColor(PdfGenerator.C_BORDER);
        c.setBorderWidth(0.5f);
        c.setColspan(colSpan);
        c.setRowspan(rowSpan);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3f);

        try {
            Image img = renderLine(text, textSizePt, bold, textColor);
            img.setAlignment(align == Element.ALIGN_LEFT ? Image.LEFT
                    : align == Element.ALIGN_RIGHT ? Image.RIGHT : Image.MIDDLE);
            c.addElement(img);
        } catch (Exception e) {
            // Fallback: use iText font (may render incorrectly for complex Devanagari)
            Font fallback = PdfGenerator.sMarathiBase != null
                    ? new Font(PdfGenerator.sMarathiBase, textSizePt, bold ? Font.BOLD : Font.NORMAL)
                    : new Font(Font.FontFamily.HELVETICA, textSizePt, bold ? Font.BOLD : Font.NORMAL);
            c.setPhrase(new com.itextpdf.text.Phrase(text, fallback));
        }
        table.addCell(c);
    }

    /**
     * Shorthand using PdfGenerator colour constants.
     */
    public static void cell(PdfPTable table,
                             String text,
                             float textSizePt,
                             boolean bold,
                             BaseColor bgColor,
                             BaseColor fgColor,
                             int colSpan,
                             int rowSpan,
                             int align) {
        int androidColor = fgColor == null ? Color.BLACK
                : Color.rgb(fgColor.getRed(), fgColor.getGreen(), fgColor.getBlue());
        cell(table, text, textSizePt, bold, bgColor, androidColor, colSpan, rowSpan, align);
    }

    /**
     * Renders a multi-line paragraph of Marathi text at a given column width
     * (in PDF points). Use this for remarks / descriptions.
     *
     * @param widthPt Width of the column in PDF points (approximate — used for text wrap)
     */
    public static void paraCell(PdfPTable table,
                                 String text,
                                 float textSizePt,
                                 boolean bold,
                                 BaseColor bgColor,
                                 BaseColor fgColor,
                                 int colSpan,
                                 float widthPt) {
        PdfPCell c = new PdfPCell();
        if (bgColor != null) c.setBackgroundColor(bgColor);
        c.setBorderColor(PdfGenerator.C_BORDER);
        c.setBorderWidth(0.5f);
        c.setColspan(colSpan);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_TOP);
        c.setPadding(4f);

        int androidFg = fgColor == null ? Color.BLACK
                : Color.rgb(fgColor.getRed(), fgColor.getGreen(), fgColor.getBlue());
        try {
            Image img = renderMultiLine(text, textSizePt, bold, androidFg, widthPt);
            c.addElement(img);
        } catch (Exception e) {
            Font fallback = PdfGenerator.sMarathiBase != null
                    ? new Font(PdfGenerator.sMarathiBase, textSizePt, bold ? Font.BOLD : Font.NORMAL)
                    : new Font(Font.FontFamily.HELVETICA, textSizePt);
            c.setPhrase(new com.itextpdf.text.Phrase(text, fallback));
        }
        table.addCell(c);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Render helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Renders a single line of text to an iText Image using Android Canvas.
     * Android's Paint/Typeface pipeline applies full Devanagari shaping.
     */
    public static Image renderLine(String text,
                                    float textSizePt,
                                    boolean bold,
                                    int androidColor) throws Exception {
        if (text == null || text.isEmpty()) text = " ";

        Paint paint = buildPaint(textSizePt, bold, androidColor);

        float textWidth  = paint.measureText(text);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;

        int bW = Math.max(1, (int) Math.ceil(textWidth)  + 4);
        int bH = Math.max(1, (int) Math.ceil(textHeight) + 4);

        Bitmap bmp = Bitmap.createBitmap(bW, bH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawText(text, 2, -fm.ascent + 2, paint);

        return bitmapToImage(bmp, textSizePt, bW, bH);
    }

    /**
     * Renders multi-line wrapped text using StaticLayout for proper Devanagari line breaking.
     */
    public static Image renderMultiLine(String text,
                                          float textSizePt,
                                          boolean bold,
                                          int androidColor,
                                          float widthPt) throws Exception {
        if (text == null || text.isEmpty()) text = " ";

        TextPaint tp = buildTextPaint(textSizePt, bold, androidColor);
        int widthPx = Math.max(1, (int) (widthPt * SCALE));

        StaticLayout sl = new StaticLayout(
                text, tp, widthPx,
                Layout.Alignment.ALIGN_NORMAL,
                1.2f, 0f, false);

        int bW = widthPx;
        int bH = Math.max(1, sl.getHeight() + 4);

        Bitmap bmp = Bitmap.createBitmap(bW, bH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        sl.draw(canvas);

        return bitmapToImage(bmp, textSizePt, bW, bH);
    }

    private static Image bitmapToImage(Bitmap bmp,
                                        float textSizePt,
                                        int bW, int bH) throws Exception {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bs);
        Image img = Image.getInstance(bs.toByteArray());

        // Scale from render-DPI back to PDF points:
        // rendered at SCALE × textSizePt px per pt, so divide by SCALE
        float displayW = bW / SCALE;
        float displayH = bH / SCALE;
        img.scaleAbsolute(displayW, displayH);
        return img;
    }

    private static Paint buildPaint(float textSizePt, boolean bold, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(color);
        p.setTextSize(textSizePt * SCALE);
        p.setTypeface(getTypeface(bold));
        return p;
    }

    private static TextPaint buildTextPaint(float textSizePt, boolean bold, int color) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        tp.setColor(color);
        tp.setTextSize(textSizePt * SCALE);
        tp.setTypeface(getTypeface(bold));
        return tp;
    }

    private static Typeface getTypeface(boolean bold) {
        if (PdfGenerator.sMarathiTypeface != null) {
            return Typeface.create(PdfGenerator.sMarathiTypeface,
                    bold ? Typeface.BOLD : Typeface.NORMAL);
        }
        return bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
    }
}
