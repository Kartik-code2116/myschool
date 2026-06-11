package com.kartik.myschool.utils.pdf;

import android.content.Context;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.utils.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;

import static com.kartik.myschool.utils.PdfGenerator.*;

public class CoverPageGenerator {

    // ── Design colours ────────────────────────────────────────────────────────
    private static final BaseColor C_INDIGO        = new BaseColor(63,  81, 181);   // #3F51B5
    private static final BaseColor C_INDIGO_DARK   = new BaseColor(40,  53, 147);   // #283593
    private static final BaseColor C_INDIGO_LIGHT  = new BaseColor(197, 202, 233);  // #C5CAE9
    private static final BaseColor C_AMBER         = new BaseColor(255, 193,   7);  // #FFC107
    private static final BaseColor C_AMBER_DARK    = new BaseColor(255, 143,   0);  // #FF8F00
    private static final BaseColor C_WHITE         = BaseColor.WHITE;
    private static final BaseColor C_PALE          = new BaseColor(232, 234, 246);  // very light indigo
    private static final BaseColor C_DARK_TEXT     = new BaseColor(28,  27,  31);
    private static final BaseColor C_GREY_TEXT     = new BaseColor(90,  90, 100);

    public static void generateCoverPage(Context ctx,
                                         School school,
                                         ClassModel cls,
                                         Student student,
                                         MarksRecord sem1,
                                         MarksRecord sem2,
                                         PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx),
                        "CoverPage_" + PdfGenerator.safeRoll(student) + "_" + PdfGenerator.ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(0, 0, 0, 0);
                addCoverPageContent(doc, ctx, school, cls, student, writer);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    // ── Public overload (called from bulk / combined generators) ──────────────
    public static void addCoverPageContent(Document doc, Context ctx, School school,
                                           ClassModel cls, Student student) throws Exception {
        addCoverPageContent(doc, ctx, school, cls, student, null);
    }

    // ── Main layout ──────────────────────────────────────────────────────────
    public static void addCoverPageContent(Document doc, Context ctx, School school,
                                           ClassModel cls, Student student,
                                           PdfWriter writer) throws Exception {

        float pageW = PageSize.A4.getWidth();   // 595 pt
        float pageH = PageSize.A4.getHeight();  // 842 pt

        // ══════════════════════════════════════════════════════════════════════
        // BACKGROUND ARTWORK  (drawn on the PdfContentByte underlay)
        // ══════════════════════════════════════════════════════════════════════
        if (writer != null) {
            PdfContentByte cb = writer.getDirectContentUnder();
            drawBackground(cb, pageW, pageH, ctx);
        }

        // ══════════════════════════════════════════════════════════════════════
        // CONTENT — We use a 0-margin page and simulate margin via table padding
        // ══════════════════════════════════════════════════════════════════════

        // ── 1. TOP HEADER BAND spacer & LOGO SPACE ────────────────────────────
        addSpacer(doc, 98);

        // Invisible placeholder table to preserve the exact height of the original logo (72 + padding)
        PdfPTable spacerRow = new PdfPTable(1);
        spacerRow.setWidthPercentage(100);
        PdfPCell spacerCell = new PdfPCell();
        spacerCell.setBorder(Rectangle.NO_BORDER);
        spacerCell.setMinimumHeight(82f); // 72 logo + 6 top padding + 4 bottom padding
        spacerRow.addCell(spacerCell);
        doc.add(spacerRow);

        addSpacer(doc, 100);


        android.content.SharedPreferences prefs = ctx.getSharedPreferences("myschool_settings_prefs", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "mr");
        boolean isEn = "en".equals(lang);

        String labelGovtSchool = isEn ? "ZILLA PARISHAD PRIMARY SCHOOL" : "जिल्हा परिषद प्राथमिक शाळा";
        String labelSchoolNameFallback = isEn ? "SCHOOL NAME" : "शाळेचे नाव";
        String labelUdise = isEn ? "UDISE : " : "युडायस : ";
        String labelTitle1 = isEn ? "CONTINUOUS & COMPREHENSIVE" : "सातत्यपूर्ण सर्वंकष";
        String labelTitle2 = isEn ? "EVALUATION" : "मूल्यमापन";
        
        String labelYear = isEn ? "Academic Year" : "शैक्षणिक वर्ष";
        String labelClass = isEn ? "Standard" : "इयत्ता";
        String labelDiv = isEn ? "Division" : "तुकडी";
        String labelUdiseCode = isEn ? "UDISE Code" : "युडायस कोड";
        String labelTeacher = isEn ? "Class Teacher" : "वर्गशिक्षक";
        String labelPrincipal = isEn ? "Headmaster" : "मुख्याध्यापक";

        // ── 3. GOVT. LABEL ────────────────────────────────────────────────────
        if (isEn) {
            addCentred(doc, labelGovtSchool,
                    mkFont(13, Font.BOLD, C_GREY_TEXT), 2, 0);
        } else {
            addMarathiCentred(doc, labelGovtSchool,
                    13, true, android.graphics.Color.rgb(90, 90, 100), 2, 0);
        }

        // ── 4. SCHOOL NAME (rendered via MarathiText for correct Devanagari) ───
        String schoolName = (school != null && school.name != null && !school.name.isEmpty())
                ? school.name : labelSchoolNameFallback;
        addMarathiCentred(doc, schoolName.toUpperCase(), 14, true,
                android.graphics.Color.rgb(40, 53, 147), 4, 2);

        // ── 5. UDISE ──────────────────────────────────────────────────────────
        String udise = (school != null && school.udiseCode != null) ? school.udiseCode : "—";
        addCentred(doc, labelUdise + udise,
                mkFont(9, Font.NORMAL, C_GREY_TEXT), 0, 16);

        // ── 6. DECORATIVE AMBER DIVIDER ───────────────────────────────────────
        PdfPTable divider = new PdfPTable(new float[]{1f, 3f, 1f});
        divider.setWidthPercentage(60);
        divider.setSpacingAfter(20);
        addFilledCell(divider, C_AMBER_DARK, 4);
        addFilledCell(divider, C_AMBER, 4);
        addFilledCell(divider, C_AMBER_DARK, 4);
        doc.add(divider);

        // ── 7. BIG MARATHI TITLE (via MarathiText for correct shaping) ────────
        if (isEn) {
            addCentred(doc, labelTitle1,
                    mkFont(24, Font.BOLD, C_INDIGO), 0, 0);
            addCentred(doc, labelTitle2,
                    mkFont(24, Font.BOLD, C_INDIGO), 0, 6);
        } else {
            addMarathiCentred(doc, labelTitle1,
                    34, true, android.graphics.Color.rgb(63, 81, 181), 0, 0);
            addMarathiCentred(doc, labelTitle2,
                    34, true, android.graphics.Color.rgb(63, 81, 181), 0, 6);
        }
        addCentred(doc, "( CCE )",
                mkFont(13, Font.NORMAL, C_GREY_TEXT), 2, 22);

        // ── 8. INFO CARD ──────────────────────────────────────────────────────
        String yearLabel  = (cls != null && cls.academicYearLabel != null) ? cls.academicYearLabel : "2025-26";
        String className  = (cls != null && cls.className  != null) ? cls.className  : "-";
        String division   = (cls != null && cls.division   != null && !cls.division.isEmpty()) ? cls.division : "-";
        String teacher    = (cls != null && cls.teacherName != null && !cls.teacherName.isEmpty()) ? cls.teacherName : "-";
        String principal  = (school != null && school.principalName != null && !school.principalName.isEmpty()) ? school.principalName : "-";

        doc.add(buildInfoCard(yearLabel, className, division, teacher, principal, udise, labelYear, labelClass, labelDiv, labelUdiseCode, labelTeacher, labelPrincipal));

        // ── 9. BOTTOM SPACER (footer band is bg artwork) ─────────────────────
        addSpacer(doc, 10);

        // ── 10. Powered-by footer text ────────────────────────────────────────
        addCentred(doc, "MySchool App — CCE Report",
                mkFont(8, Font.ITALIC, new BaseColor(160, 160, 180)), 0, 0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BACKGROUND ARTWORK
    // ══════════════════════════════════════════════════════════════════════════
    private static void drawBackground(PdfContentByte cb, float W, float H, Context ctx) {
        // ── Full-page pale fill ───────────────────────────────────────────────
        cb.saveState();
        cb.setColorFill(new BaseColor(245, 245, 255));
        cb.rectangle(0, 0, W, H);
        cb.fill();
        cb.restoreState();

        // ── Top indigo header band ────────────────────────────────────────────
        cb.saveState();
        cb.setColorFill(C_INDIGO_DARK);
        cb.rectangle(0, H - 130, W, 130);
        cb.fill();
        cb.restoreState();

        // ── Overlapping lighter stripe across top band ────────────────────────
        cb.saveState();
        cb.setColorFill(C_INDIGO);
        cb.rectangle(0, H - 130, W, 90);
        cb.fill();
        cb.restoreState();

        // ── Amber accent bar at very top ──────────────────────────────────────
        cb.saveState();
        cb.setColorFill(C_AMBER);
        cb.rectangle(0, H - 8, W, 8);
        cb.fill();
        cb.restoreState();

        // ── Top-right decorative circles ─────────────────────────────────────
        drawCircle(cb, W - 40, H - 40, 60, new BaseColor(255, 255, 255, 18));
        drawCircle(cb, W - 20, H - 80, 40, new BaseColor(255, 255, 255, 12));
        drawCircle(cb, W - 70, H - 20, 30, new BaseColor(255, 255, 255, 15));

        // ── Top-left decorative circles ───────────────────────────────────────
        drawCircle(cb, 30, H - 30, 55, new BaseColor(255, 255, 255, 14));
        drawCircle(cb, 60, H - 15, 30, new BaseColor(255, 255, 255, 10));

        // ── White logo circle background (centred at x=W/2, y=H-150) ─────────
        cb.saveState();
        cb.setColorFill(C_WHITE);
        float cx = W / 2f;
        float cy = H - 150;
        cb.circle(cx, cy, 46);
        cb.fill();
        cb.restoreState();

        // ── Amber ring around logo circle ─────────────────────────────────────
        cb.saveState();
        cb.setColorStroke(C_AMBER);
        cb.setLineWidth(3f);
        cb.circle(cx, cy, 48);
        cb.stroke();
        cb.restoreState();

        // ── App Logo perfectly centered in the circle ─────────────────────────
        try {
            android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat
                    .getDrawable(ctx, com.kartik.myschool.R.drawable.app_logo);
            if (d != null) {
                // Use a decent resolution for the bitmap
                int size = Math.max(Math.max(d.getIntrinsicWidth(), d.getIntrinsicHeight()), 200);
                android.graphics.Bitmap bmp = android.graphics.Bitmap
                        .createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                d.setBounds(0, 0, size, size);
                d.draw(canvas);
                
                java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bs);
                com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(bs.toByteArray());
                
                // Scale image to fill the background circle (radius 46 -> diameter 92)
                float imgSize = 92f;
                img.scaleToFit(imgSize, imgSize);
                img.setAbsolutePosition(cx - (imgSize / 2f), cy - (imgSize / 2f));
                
                // Use PDF vector clipping for perfectly smooth circular edges
                cb.saveState();
                cb.circle(cx, cy, 46); // Clip to the exact size of the white circle
                cb.clip();
                cb.newPath();
                cb.addImage(img);
                cb.restoreState();
            }
        } catch (Exception ignored) {}

        // ── Bottom footer band ────────────────────────────────────────────────
        cb.saveState();
        cb.setColorFill(C_INDIGO_DARK);
        cb.rectangle(0, 0, W, 38);
        cb.fill();
        cb.restoreState();

        cb.saveState();
        cb.setColorFill(C_AMBER);
        cb.rectangle(0, 38, W, 5);
        cb.fill();
        cb.restoreState();

        // ── Diagonal stripes in bottom-right corner ───────────────────────────
        cb.saveState();
        cb.setColorFill(new BaseColor(255, 193, 7, 30));
        for (int i = 0; i < 5; i++) {
            float x = W - 60 + i * 14;
            drawDiagStripe(cb, x, 0, 12, 60);
        }
        cb.restoreState();

        // ── Subtle dot grid in centre-right margin ────────────────────────────
        cb.saveState();
        cb.setColorFill(C_INDIGO_LIGHT);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 3; col++) {
                cb.circle(W - 30 - col * 14, H / 2f - 80 + row * 18, 1.5f);
                cb.fill();
            }
        }
        cb.restoreState();

        // ── Subtle dot grid in centre-left margin ────────────────────────────
        cb.saveState();
        cb.setColorFill(C_INDIGO_LIGHT);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 3; col++) {
                cb.circle(18 + col * 14, H / 2f - 80 + row * 18, 1.5f);
                cb.fill();
            }
        }
        cb.restoreState();
    }

    private static void drawCircle(PdfContentByte cb, float x, float y, float r, BaseColor c) {
        cb.saveState();
        cb.setColorFill(c);
        cb.circle(x, y, r);
        cb.fill();
        cb.restoreState();
    }

    private static void drawDiagStripe(PdfContentByte cb, float x, float y, float w, float h) {
        cb.moveTo(x, y);
        cb.lineTo(x + w, y);
        cb.lineTo(x + w + h * 0.4f, y + h);
        cb.lineTo(x + h * 0.4f, y + h);
        cb.closePath();
        cb.fill();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INFO CARD (rounded box with left accent bar per row)
    // ══════════════════════════════════════════════════════════════════════════
    private static PdfPTable buildInfoCard(String year, String cls, String div,
                                           String teacher, String principal, String udise,
                                           String labelYear, String labelClass, String labelDiv,
                                           String labelUdise, String labelTeacher, String labelPrincipal) throws Exception {
        // Outer wrapper — provides left/right padding
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(78);
        outer.setSpacingBefore(4);
        outer.setSpacingAfter(12);
        outer.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell outerCell = new PdfPCell();
        outerCell.setBorder(Rectangle.NO_BORDER);
        outerCell.setCellEvent((cell, pos, canvases) -> {
            // Rounded light-indigo background
            PdfContentByte bgCb = canvases[PdfPTable.BACKGROUNDCANVAS];
            bgCb.saveState();
            bgCb.setColorFill(C_PALE);
            bgCb.roundRectangle(pos.getLeft(), pos.getBottom(),
                    pos.getWidth(), pos.getHeight(), 10f);
            bgCb.fill();
            // Left accent bar
            bgCb.setColorFill(C_INDIGO);
            bgCb.roundRectangle(pos.getLeft(), pos.getBottom() + 4,
                    5f, pos.getHeight() - 8, 2f);
            bgCb.fill();
            bgCb.restoreState();
        });
        outerCell.setPaddingTop(16);
        outerCell.setPaddingBottom(16);
        outerCell.setPaddingLeft(22);
        outerCell.setPaddingRight(16);

        // Inner 2-column table: label | value
        PdfPTable inner = new PdfPTable(new float[]{0.9f, 2.4f});
        inner.setWidthPercentage(100);

        String[][] rows = {
            { labelYear,      year   },
            { labelClass,     cls    },
            { labelDiv,       div    },
            { labelUdise,     udise  },
            { labelTeacher,   teacher },
            { labelPrincipal, principal },
        };

        for (String[] row : rows) {
            // Label — rendered via MarathiText for correct Devanagari shaping
            PdfPCell labelCell = new PdfPCell();
            labelCell.setBorder(Rectangle.BOTTOM);
            labelCell.setBorderColor(C_INDIGO_LIGHT);
            labelCell.setPaddingBottom(8);
            labelCell.setPaddingTop(6);
            labelCell.setBackgroundColor(new BaseColor(0, 0, 0, 0));
            try {
                Image lImg = MarathiText.renderLine(row[0], 10f, true,
                        android.graphics.Color.rgb(40, 53, 147));
                lImg.setAlignment(Image.LEFT);
                labelCell.addElement(lImg);
            } catch (Exception ex) {
                labelCell.setPhrase(new Phrase(row[0], mkFont(10, Font.BOLD, C_INDIGO_DARK)));
            }

            // Value
            PdfPCell valueCell = new PdfPCell();
            valueCell.setBorder(Rectangle.BOTTOM);
            valueCell.setBorderColor(C_INDIGO_LIGHT);
            valueCell.setPaddingBottom(8);
            valueCell.setPaddingTop(6);
            try {
                Image vImg = MarathiText.renderLine(row[1], 10f, false,
                        android.graphics.Color.rgb(28, 27, 31));
                vImg.setAlignment(Image.LEFT);
                valueCell.addElement(vImg);
            } catch (Exception ex) {
                valueCell.setPhrase(new Phrase(row[1], mkFont(10, Font.NORMAL, C_DARK_TEXT)));
            }

            inner.addCell(labelCell);
            inner.addCell(valueCell);
        }

        outerCell.addElement(inner);
        outer.addCell(outerCell);
        return outer;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    private static Font mkFont(int size, int style, BaseColor color) {
        if (sMarathiBase != null)
            return new Font(sMarathiBase, size, style, color);
        return new Font(Font.FontFamily.HELVETICA, size, style, color);
    }

    /**
     * Renders Marathi text as a bitmap image and adds it as a centred paragraph.
     * Uses Android's Harfbuzz engine for correct Devanagari shaping.
     */
    private static void addMarathiCentred(Document doc, String text,
                                           float sizePt, boolean bold,
                                           int androidColor,
                                           float spaceBefore, float spaceAfter) throws Exception {
        try {
            Image img = MarathiText.renderLine(text, sizePt, bold, androidColor);
            img.setAlignment(Image.MIDDLE);
            // Wrap in a 1-cell centred table so it flows like a paragraph
            PdfPTable imgRow = new PdfPTable(1);
            imgRow.setWidthPercentage(100);
            imgRow.setSpacingBefore(spaceBefore);
            imgRow.setSpacingAfter(spaceAfter);
            PdfPCell imgCell = new PdfPCell();
            imgCell.setBorder(Rectangle.NO_BORDER);
            imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            imgCell.addElement(img);
            imgRow.addCell(imgCell);
            doc.add(imgRow);
        } catch (Exception e) {
            // Fallback to standard paragraph
            Paragraph p = new Paragraph(text, mkFont((int) sizePt, Font.BOLD,
                    new BaseColor(
                            (androidColor >> 16) & 0xFF,
                            (androidColor >> 8) & 0xFF,
                            androidColor & 0xFF)));
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingBefore(spaceBefore);
            p.setSpacingAfter(spaceAfter);
            doc.add(p);
        }
    }

    private static void addCentred(Document doc, String text, Font font,
                                   float spaceBefore, float spaceAfter) throws Exception {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(spaceBefore);
        p.setSpacingAfter(spaceAfter);
        doc.add(p);
    }

    private static void addSpacer(Document doc, float height) throws Exception {
        Paragraph sp = new Paragraph(" ");
        sp.setSpacingBefore(height);
        doc.add(sp);
    }

    private static void addFilledCell(PdfPTable t, BaseColor color, float height) {
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBackgroundColor(color);
        c.setBorder(Rectangle.NO_BORDER);
        c.setMinimumHeight(height);
        t.addCell(c);
    }
}
