package com.kartik.myschool.utils.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.utils.PdfGenerator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.kartik.myschool.utils.PdfGenerator.*;

/**
 * Option 9 — Progress Card Cover (प्रगती पत्रक)
 *
 * Layout: Landscape A4, split into two half-page panels side-by-side.
 *
 * RIGHT PANEL (cover / front):
 *   • Teal header banner with UDISE + "जिल्हा परिषद"
 *   • School name (large, bold, centered)
 *   • School address + academic year
 *   • "प्रगती पत्रक" title
 *   • Student detail rows (name, roll, class, DOB, medium, mother tongue,
 *     mother name, father name, address)
 *
 * LEFT PANEL (inner / back):
 *   • उपस्थिती monthly attendance table (11 months + signature columns)
 *   • Summer vacation note
 *   • आरोग्य विषयक माहिती (weight/height sem1/sem2)
 *   • Grade scale legend at bottom
 *
 * One page per student; all students merged into one PDF file.
 */
public class ProgressCardCoverGenerator {

    // Months in order (Marathi)
    private static final String[] MONTHS_MR = {
            "जून", "जुलै", "ऑगस्ट", "सप्टेंबर", "ऑक्टोबर",
            "नोव्हेंबर", "डिसेंबर", "जानेवारी", "फेब्रुवारी", "मार्च", "एप्रिल", "मे"
    };

    // Grade scale entries: range label → grade label
    private static final String[][] GRADE_SCALE = {
            {"91-100", "अ-1"}, {"81-90", "अ-2"}, {"71-80", "ब-1"}, {"61-70", "ब-2"},
            {"51-60", "क-1"}, {"41-50", "क-2"}, {"33-40", "ड"},   {"21-32", "इ-1"}, {"0-20", "इ-2"}
    };

    // Colors matching the screenshot
    private static final BaseColor C_TEAL      = new BaseColor(0,  151, 167);   // #0097A7
    private static final BaseColor C_TEAL_LITE = new BaseColor(178, 235, 242);  // light teal bg
    private static final BaseColor C_YELLOW    = new BaseColor(255, 241, 118);  // UDISE bubble
    private static final BaseColor C_CREAM     = new BaseColor(255, 253, 231);  // right-panel bg
    private static final BaseColor C_PANEL_BG  = new BaseColor(248, 253, 255);  // left panel bg

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point — generates one landscape page per student
    // ─────────────────────────────────────────────────────────────────────────
    public static void generateProgressCardCover(Context ctx,
                                                  School school,
                                                  ClassModel cls,
                                                  List<Student> students,
                                                  Map<String, MarksRecord> sem1Map,
                                                  Map<String, MarksRecord> sem2Map,
                                                  PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx),
                        "ProgressCard_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(0, 0, 0, 0);

                boolean isFirst = true;
                if (students != null) {
                    for (Student student : students) {
                        if (!isFirst) doc.newPage();
                        isFirst = false;
                        MarksRecord s1 = sem1Map != null ? sem1Map.get(student.id) : null;
                        MarksRecord s2 = sem2Map != null ? sem2Map.get(student.id) : null;
                        addCardContent(doc, ctx, school, cls, student, s1, s2);
                    }
                }
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-student page builder
    // ─────────────────────────────────────────────────────────────────────────
    public static void addCardContent(Document doc, Context ctx, School school, ClassModel cls,
                                       Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {

        // Outer 2-column table: [LEFT panel | RIGHT panel]
        PdfPTable outer = new PdfPTable(new float[]{1f, 1f});
        outer.setWidthPercentage(100);
        outer.setExtendLastRow(true); // fill full page height

        // ── LEFT PANEL ────────────────────────────────────────────────────────
        PdfPCell leftPanel = new PdfPCell();
        leftPanel.setBackgroundColor(C_PANEL_BG);
        leftPanel.setBorder(Rectangle.RIGHT);
        leftPanel.setBorderColor(C_TEAL);
        leftPanel.setBorderWidthRight(1.5f);
        leftPanel.setPadding(10);
        leftPanel.setVerticalAlignment(Element.ALIGN_TOP);

        buildLeftPanel(leftPanel, ctx, student, sem1, sem2);

        // ── RIGHT PANEL ───────────────────────────────────────────────────────
        PdfPCell rightPanel = new PdfPCell();
        rightPanel.setBackgroundColor(C_CREAM);
        rightPanel.setBorder(Rectangle.NO_BORDER);
        rightPanel.setPadding(0);
        rightPanel.setVerticalAlignment(Element.ALIGN_TOP);

        buildRightPanel(rightPanel, ctx, school, cls, student);

        outer.addCell(leftPanel);
        outer.addCell(rightPanel);
        doc.add(outer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEFT PANEL: Attendance + Health + Grade Scale
    // ─────────────────────────────────────────────────────────────────────────
    private static void buildLeftPanel(PdfPCell panel, Context ctx, Student student,
                                        MarksRecord sem1, MarksRecord sem2) throws Exception {

        // ── Section title: उपस्थिती ──────────────────────────────────────────
        panel.addElement(marathiImg(ctx, "उपस्थिती :", 9, true, C_TEAL));
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 3)));

        // ── Monthly attendance table ──────────────────────────────────────────
        // Columns: महिना | कामाचे दिवस | हजर दिवस | स्वाक्षरी (3 merged) → वर्गशिक्षक | पालक | मुख्याध्यापक
        float[] attWidths = {1.1f, 0.8f, 0.75f, 0.85f, 0.6f, 1.0f};
        PdfPTable attTbl = new PdfPTable(attWidths);
        attTbl.setWidthPercentage(100);

        // Header row 1
        cellSpan(attTbl, "महिना",        fSmallBold, C_TEAL, C_WHITE, 1, 2, Element.ALIGN_CENTER);
        cellSpan(attTbl, "कामाचे\nदिवस", fSmallBold, C_TEAL, C_WHITE, 1, 2, Element.ALIGN_CENTER);
        cellSpan(attTbl, "हजर\nदिवस",    fSmallBold, C_TEAL, C_WHITE, 1, 2, Element.ALIGN_CENTER);
        cellSpan(attTbl, "स्वाक्षरी",    fSmallBold, C_TEAL, C_WHITE, 3, 1, Element.ALIGN_CENTER);

        // Header row 2: sub-headers under स्वाक्षरी
        cellSpan(attTbl, "वर्गशिक्षक",    fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);
        cellSpan(attTbl, "पालक",          fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);
        cellSpan(attTbl, "मुख्याध्यापक", fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);

        // Month rows
        boolean alt = false;
        for (String month : MONTHS_MR) {
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;

            // Parse attendance: "present/total" or just "present"
            int workingDays = 0, presentDays = 0;
            if (student.monthlyAttendance != null) {
                String att = student.monthlyAttendance.get(month);
                if (att != null && att.contains("/")) {
                    String[] parts = att.split("/");
                    try { presentDays = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
                    try { workingDays  = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                } else if (att != null && !att.isEmpty()) {
                    try { presentDays = Integer.parseInt(att.trim()); } catch (Exception ignored) {}
                }
            }

            String wdStr = workingDays > 0 ? String.valueOf(workingDays) : "";
            String pdStr = presentDays > 0 ? String.valueOf(presentDays) : "";

            cellSpan(attTbl, month, fSmall, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);
            cellSpan(attTbl, wdStr,  fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(attTbl, pdStr,  fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            // 3 blank signature columns
            cellSpan(attTbl, "", fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(attTbl, "", fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(attTbl, "", fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }
        panel.addElement(attTbl);

        // ── Summer vacation note ──────────────────────────────────────────────
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 4)));
        panel.addElement(marathiImg(ctx, "उन्हाळी सुट्टीनंतर शाळा १५ जून रोजी सुरू होईल.", 7, false, C_DARK));
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 5)));

        // ── Health info ───────────────────────────────────────────────────────
        panel.addElement(marathiImg(ctx, "आरोग्य विषयक माहिती", 8, true, C_TEAL));
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 3)));

        float[] healthWidths = {1.2f, 1f, 1f};
        PdfPTable healthTbl = new PdfPTable(healthWidths);
        healthTbl.setWidthPercentage(70);
        healthTbl.setHorizontalAlignment(Element.ALIGN_LEFT);

        cellSpan(healthTbl, "-",         fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);
        cellSpan(healthTbl, "प्रथम सत्र",  fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);
        cellSpan(healthTbl, "द्वितीय सत्र", fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);

        // Weight row (not stored in MarksRecord — show dash)
        String w1 = "-";
        String w2 = "-";
        cellSpan(healthTbl, "वजन",  fSmall, C_WHITE,    C_DARK, 1, 1, Element.ALIGN_LEFT);
        cellSpan(healthTbl, w1,     fSmall, C_WHITE,    C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(healthTbl, w2,     fSmall, C_WHITE,    C_DARK, 1, 1, Element.ALIGN_CENTER);

        // Height row
        String h1 = "-";
        String h2 = "-";
        cellSpan(healthTbl, "उंची", fSmall, C_ROW_ALT, C_DARK, 1, 1, Element.ALIGN_LEFT);
        cellSpan(healthTbl, h1,     fSmall, C_ROW_ALT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(healthTbl, h2,     fSmall, C_ROW_ALT, C_DARK, 1, 1, Element.ALIGN_CENTER);

        panel.addElement(healthTbl);

        // ── Grade scale at bottom ─────────────────────────────────────────────
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 6)));
        panel.addElement(marathiImg(ctx, "श्रेणी सारा :", 8, true, C_DARK));
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 3)));

        float[] gsWidths = new float[GRADE_SCALE.length];
        Arrays.fill(gsWidths, 1f);
        PdfPTable gsTbl = new PdfPTable(gsWidths);
        gsTbl.setWidthPercentage(100);

        // Range row
        for (String[] entry : GRADE_SCALE) {
            cellSpan(gsTbl, entry[0], fMicro, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }
        // Grade label row
        for (String[] entry : GRADE_SCALE) {
            cellSpan(gsTbl, entry[1], fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);
        }
        panel.addElement(gsTbl);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RIGHT PANEL: Cover / Student info
    // ─────────────────────────────────────────────────────────────────────────
    private static void buildRightPanel(PdfPCell panel, Context ctx, School school,
                                         ClassModel cls, Student student) throws Exception {

        // ── Teal header banner ────────────────────────────────────────────────
        // Contains: "School UDISE: XXXX  जिल्हा परिषद"
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);

        String udiseLine = "School UDISE: " + nvl(school != null ? school.udiseCode : null)
                + "    जिल्हा परिषद";
        PdfPCell bannerCell = new PdfPCell();
        bannerCell.setBackgroundColor(C_TEAL);
        bannerCell.setBorder(Rectangle.NO_BORDER);
        bannerCell.setPadding(6);
        bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        // Render Marathi + ASCII mix as image
        try {
            Image udiseImg = marathiImg(ctx, udiseLine, 9, false, BaseColor.WHITE);
            udiseImg.setAlignment(Element.ALIGN_CENTER);
            bannerCell.addElement(udiseImg);
        } catch (Exception e) {
            bannerCell.addElement(new Phrase(udiseLine, fSmall));
        }
        banner.addCell(bannerCell);

        panel.addElement(banner);

        // ── School name (large, centered) ────────────────────────────────────
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 6)));
        try {
            Image schoolImg = marathiImg(ctx,
                    nvl(school != null ? school.name : "शाळेचे नाव"), 16, true, C_TEAL);
            schoolImg.setAlignment(Element.ALIGN_CENTER);
            panel.addElement(schoolImg);
        } catch (Exception e) {
            panel.addElement(new Phrase(nvl(school != null ? school.name : ""), fHeader));
        }

        // ── School address & year ─────────────────────────────────────────────
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 4)));
        if (school != null && school.address != null && !school.address.isEmpty()) {
            try {
                Image addrImg = marathiImg(ctx, school.address, 8, false, C_DARK);
                addrImg.setAlignment(Element.ALIGN_CENTER);
                panel.addElement(addrImg);
            } catch (Exception e) {
                panel.addElement(new Phrase(school.address, fSmall));
            }
        }

        String yearLabel = cls != null ? nvl(cls.academicYearLabel) : "";
        try {
            Image yearImg = marathiImg(ctx, "सन : " + yearLabel, 9, true, C_DARK);
            yearImg.setAlignment(Element.ALIGN_CENTER);
            panel.addElement(yearImg);
        } catch (Exception e) {
            panel.addElement(new Phrase("सन : " + yearLabel, fSmallBold));
        }

        // ── Large "प्रगती पत्रक" title ────────────────────────────────────────
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 10)));
        try {
            Image titleImg = marathiImg(ctx, "प्रगती पत्रक", 22, true, C_TEAL);
            titleImg.setAlignment(Element.ALIGN_CENTER);
            panel.addElement(titleImg);
        } catch (Exception e) {
            panel.addElement(new Phrase("प्रगती पत्रक", fTitle));
        }
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 10)));

        // ── Student details grid ──────────────────────────────────────────────
        buildStudentDetails(panel, ctx, cls, student);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student details table (bullet-point style rows)
    // ─────────────────────────────────────────────────────────────────────────
    private static void buildStudentDetails(PdfPCell panel, Context ctx,
                                             ClassModel cls, Student student) throws Exception {

        // Compute age string from DOB
        String ageStr = "-";
        if (student.dob != null && !student.dob.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
                java.util.Date dob = sdf.parse(student.dob);
                if (dob != null) {
                    long diffMs = System.currentTimeMillis() - dob.getTime();
                    long years  = diffMs / (365L * 24 * 60 * 60 * 1000);
                    long months = (diffMs % (365L * 24 * 60 * 60 * 1000)) / (30L * 24 * 60 * 60 * 1000);
                    ageStr = years + " व. " + months + " म.";
                }
            } catch (Exception ignored) {}
        }

        String className = cls != null ? nvl(cls.className) : nvl(student.standard);
        String division  = cls != null ? nvl(cls.division)  : nvl(student.division);

        // Two-column detail rows: [bullet + label | colon + value] × 2 side by side
        // Format: • label : value | (optional second col) • label : value
        // Using a 4-column table: label1 | value1 | label2 | value2
        float[] detWidths = {1.2f, 1.8f, 1.2f, 1.8f};
        PdfPTable det = new PdfPTable(detWidths);
        det.setWidthPercentage(96);
        det.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Row 1: नाव (full width)
        addDetailFull(det, ctx, "• नाव", ": " + nvl(student.name), 4);

        // Row 2: स्टुडंट ID | रजि.नंबर
        addDetailPair(det, ctx,
                "• स्टुडंट ID",    ": " + nvl(student.studentIdNumber),
                "  रजि.नंबर",     ": " + nvl(student.registrationNo));

        // Row 3: हजेरी क्रमांक | (blank for extra spacing)
        addDetailPair(det, ctx,
                "• हजेरी क्रमांक", ": " + nvl(student.rollNo),
                "  रजि.नंबर",      ": " + nvl(student.rollNo2));

        // Row 4: इयत्ता | तुकडी
        addDetailPair(det, ctx,
                "• इयत्ता",  ": " + className,
                "  तुकडी",   ": " + division);

        // Row 5: माध्यम | जन्मतारीख
        addDetailPair(det, ctx,
                "• माध्यम",     ": " + nvl(student.medium),
                "  जन्मतारीख",  ": " + nvl(student.dob));

        // Row 6: मातृभाषा | वय
        addDetailPair(det, ctx,
                "• मातृभाषा", ": " + nvl(student.motherTongue),
                "  वय",        ": " + ageStr);

        // Row 7: आईचे नाव (full)
        addDetailFull(det, ctx, "• आईचे नाव", ": " + nvl(student.motherName), 4);

        // Row 8: वडिलांचे नाव (full)
        addDetailFull(det, ctx, "• वडिलांचे नाव", ": " + nvl(student.fatherName), 4);

        // Row 9: पत्ता (full)
        addDetailFull(det, ctx, "• पत्ता", ": " + nvl(student.address), 4);

        panel.addElement(det);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Full-width detail row spanning all columns. */
    private static void addDetailFull(PdfPTable tbl, Context ctx, String label, String value, int totalCols) {
        PdfPCell lc = noBorderCell(label, fSmallBold, Element.ALIGN_LEFT);
        lc.setColspan(1);
        tbl.addCell(lc);
        PdfPCell vc = noBorderCell(value, fSmall, Element.ALIGN_LEFT);
        vc.setColspan(totalCols - 1);
        tbl.addCell(vc);
    }

    /** Two label-value pairs side by side (4 cells). */
    private static void addDetailPair(PdfPTable tbl, Context ctx,
                                       String lbl1, String val1,
                                       String lbl2, String val2) {
        tbl.addCell(noBorderCell(lbl1, fSmallBold, Element.ALIGN_LEFT));
        tbl.addCell(noBorderCell(val1, fSmall,     Element.ALIGN_LEFT));
        tbl.addCell(noBorderCell(lbl2, fSmallBold, Element.ALIGN_LEFT));
        tbl.addCell(noBorderCell(val2, fSmall,     Element.ALIGN_LEFT));
    }

    private static PdfPCell noBorderCell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPaddingBottom(3);
        return c;
    }

    /**
     * Renders a Marathi/Devanagari string into a small Image using Android Canvas
     * so it displays correctly in the iText PDF (bypasses iText's lack of
     * Devanagari shaping support).
     */
    private static Image marathiImg(Context ctx, String text, float ptSize,
                                     boolean bold, BaseColor color) throws Exception {
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        float density = ctx.getResources().getDisplayMetrics().density;
        // Scale up paint to get crisp pixels; we'll scale down in PDF
        float paintPx = ptSize * density * 3f;
        paint.setTextSize(paintPx);
        paint.setColor(android.graphics.Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));

        if (PdfGenerator.sMarathiTypeface != null) {
            paint.setTypeface(android.graphics.Typeface.create(
                    PdfGenerator.sMarathiTypeface,
                    bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        } else {
            paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        }

        android.graphics.Paint.FontMetrics fm = paint.getFontMetrics();
        float textW = paint.measureText(text);
        float textH = fm.descent - fm.ascent;

        Bitmap bmp = Bitmap.createBitmap(
                (int) Math.ceil(textW) + 8, (int) Math.ceil(textH) + 8,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawText(text, 4, -fm.ascent + 4, paint);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
        Image img = Image.getInstance(bos.toByteArray());
        // Scale to PDF points: 1 pt ≈ density*3 px
        img.scalePercent(100f / (density * 3f) * 100f);
        return img;
    }
}
