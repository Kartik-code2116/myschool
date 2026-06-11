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

    private static String[] getMonths(Context ctx) {
        if (PdfLocalizer.isEnglish(ctx)) {
            return new String[]{"June", "July", "August", "September", "October", "November", "December", "January", "February", "March", "April", "May"};
        }
        return MONTHS_MR;
    }

    // Grade scale entries: range label → grade label
    private static final String[][] GRADE_SCALE = {
            {"91-100", "अ-1"}, {"81-90", "अ-2"}, {"71-80", "ब-1"}, {"61-70", "ब-2"},
            {"51-60", "क-1"}, {"41-50", "क-2"}, {"33-40", "ड"},   {"21-32", "इ-1"}, {"0-20", "इ-2"}
    };

    private static String[][] getGradeScale(Context ctx) {
        if (PdfLocalizer.isEnglish(ctx)) {
            return new String[][]{
                {"91-100", "A-1"}, {"81-90", "A-2"}, {"71-80", "B-1"}, {"61-70", "B-2"},
                {"51-60", "C-1"}, {"41-50", "C-2"}, {"33-40", "D"},   {"21-32", "E-1"}, {"0-20", "E-2"}
            };
        }
        return GRADE_SCALE;
    }

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
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
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

    private static class PanelBorderEvent implements com.itextpdf.text.pdf.PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, com.itextpdf.text.pdf.PdfContentByte[] canvases) {
            com.itextpdf.text.pdf.PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
            cb.saveState();
            cb.setColorStroke(new BaseColor(150, 160, 170));
            cb.setLineWidth(1.5f);
            cb.roundRectangle(position.getLeft() + 15, position.getBottom() + 15, position.getWidth() - 30, position.getHeight() - 30, 8);
            cb.stroke();
            cb.restoreState();
        }
    }

    private static class RightPanelBackgroundEvent implements com.itextpdf.text.pdf.PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, com.itextpdf.text.pdf.PdfContentByte[] canvases) {
            com.itextpdf.text.pdf.PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS];
            cb.saveState();
            float x = position.getLeft() + 15;
            float y = position.getBottom() + 15;
            float w = position.getWidth() - 30;
            float h = position.getHeight() - 30;

            // Background white
            cb.setColorFill(BaseColor.WHITE);
            cb.rectangle(x, y, w, h);
            cb.fill();

            // Light cyan/blue elements at top
            cb.setColorFill(new BaseColor(135, 225, 235)); // Cyan left
            cb.roundRectangle(x - 5, y + h * 0.75f, w * 0.2f, h * 0.3f, 20);
            cb.fill();
            
            cb.setColorFill(new BaseColor(13, 140, 200)); // Blue right
            cb.roundRectangle(x + w * 0.85f, y + h * 0.8f, w * 0.2f, h * 0.2f, 20);
            cb.fill();
            
            cb.setColorFill(new BaseColor(41, 193, 234)); // Light Blue right
            cb.roundRectangle(x + w * 0.8f, y + h * 0.7f, w * 0.25f, h * 0.25f, 30);
            cb.fill();

            // Main Yellow Background
            cb.setColorFill(new BaseColor(254, 221, 101));
            cb.roundRectangle(x + w * 0.08f, y + h * 0.58f, w * 0.84f, h * 0.40f, 20);
            cb.fill();

            // Decorative swirl at bottom right of yellow box
            cb.moveTo(x + w * 0.7f, y + h * 0.58f);
            cb.curveTo(x + w * 0.72f, y + h * 0.5f, x + w * 0.85f, y + h * 0.48f, x + w * 0.82f, y + h * 0.58f);
            cb.fill();

            cb.restoreState();
        }
    }

    public static void addCardContent(Document doc, Context ctx, School school, ClassModel cls,
                                       Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {

        // Outer 2-column table: [LEFT panel | RIGHT panel]
        PdfPTable outer = new PdfPTable(new float[]{1f, 1f});
        outer.setWidthPercentage(100);
        outer.setExtendLastRow(true); // fill full page height

        // ── LEFT PANEL ────────────────────────────────────────────────────────
        PdfPCell leftPanel = new PdfPCell();
        leftPanel.setBackgroundColor(BaseColor.WHITE);
        leftPanel.setBorder(Rectangle.NO_BORDER);
        leftPanel.setCellEvent(new PanelBorderEvent());
        leftPanel.setPadding(25);
        leftPanel.setVerticalAlignment(Element.ALIGN_TOP);

        buildLeftPanel(leftPanel, ctx, student, sem1, sem2);

        // ── RIGHT PANEL ───────────────────────────────────────────────────────
        PdfPCell rightPanel = new PdfPCell();
        rightPanel.setBackgroundColor(BaseColor.WHITE);
        rightPanel.setBorder(Rectangle.NO_BORDER);
        rightPanel.setCellEvent(new PanelBorderEvent());
        rightPanel.setPadding(25);
        rightPanel.setVerticalAlignment(Element.ALIGN_TOP);
        
        // Add a nested table with the background event to ensure borders draw over background
        PdfPTable innerRight = new PdfPTable(1);
        innerRight.setWidthPercentage(100);
        PdfPCell innerRightCell = new PdfPCell();
        innerRightCell.setBorder(Rectangle.NO_BORDER);
        innerRightCell.setCellEvent(new RightPanelBackgroundEvent());
        innerRightCell.setPadding(10);
        buildRightPanel(innerRightCell, ctx, school, cls, student);
        innerRight.addCell(innerRightCell);
        
        rightPanel.addElement(innerRight);

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
        panel.addElement(marathiImg(ctx, PdfLocalizer.get(ctx, "उपस्थिती :", "Attendance :"), 9, true, C_TEAL));
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 3)));

        // ── Monthly attendance table ──────────────────────────────────────────
        // Columns: महिना | कामाचे दिवस | हजर दिवस | स्वाक्षरी (3 merged) → वर्गशिक्षक | पालक | मुख्याध्यापक
        float[] attWidths = {1.1f, 0.8f, 0.75f, 0.85f, 0.6f, 1.0f};
        PdfPTable attTbl = new PdfPTable(attWidths);
        attTbl.setWidthPercentage(100);

        // Header row 1
        BaseColor headerBg = new BaseColor(254, 235, 150); // Light Yellow
        cellSpan(attTbl, PdfLocalizer.get(ctx, "महिना", "Month"),        fSmallBold, headerBg, C_DARK, 1, 2, Element.ALIGN_CENTER);
        cellSpan(attTbl, PdfLocalizer.get(ctx, "कामाचे\nदिवस", "Working\nDays"), fSmallBold, headerBg, C_DARK, 1, 2, Element.ALIGN_CENTER);
        cellSpan(attTbl, PdfLocalizer.get(ctx, "हजर\nदिवस", "Present\nDays"),    fSmallBold, headerBg, C_DARK, 1, 2, Element.ALIGN_CENTER);
        cellSpan(attTbl, PdfLocalizer.get(ctx, "स्वाक्षरी", "Signature"),    fSmallBold, headerBg, C_DARK, 3, 1, Element.ALIGN_CENTER);

        // Header row 2: sub-headers under स्वाक्षरी
        cellSpan(attTbl, PdfLocalizer.get(ctx, "वर्गशिक्षक", "Class Teacher"),    fSmallBold, headerBg, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(attTbl, PdfLocalizer.get(ctx, "पालक", "Parent"),          fSmallBold, headerBg, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(attTbl, PdfLocalizer.get(ctx, "मुख्याध्यापक", "Headmaster"), fSmallBold, headerBg, C_DARK, 1, 1, Element.ALIGN_CENTER);

        // Month rows
        boolean alt = false;
        String[] months = getMonths(ctx);
        for (String month : months) {
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
        panel.addElement(marathiImg(ctx, PdfLocalizer.get(ctx, "उन्हाळी सुट्टीनंतर शाळा १५ जून रोजी सुरू होईल.", "School will reopen on June 15 after summer vacation."), 7, false, C_DARK));
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 5)));

        // ── Health info ───────────────────────────────────────────────────────
        panel.addElement(marathiImg(ctx, PdfLocalizer.get(ctx, "आरोग्य विषयक माहिती", "Health Information"), 8, true, C_TEAL));
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 3)));

        float[] healthWidths = {1.2f, 1f, 1f};
        PdfPTable healthTbl = new PdfPTable(healthWidths);
        healthTbl.setWidthPercentage(70);
        healthTbl.setHorizontalAlignment(Element.ALIGN_LEFT);

        cellSpan(healthTbl, "-",         fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);
        cellSpan(healthTbl, PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester"),  fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);
        cellSpan(healthTbl, PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester"), fSmallBold, C_TEAL, C_WHITE, 1, 1, Element.ALIGN_CENTER);

        // Weight row (not stored in MarksRecord — show dash)
        String w1 = "-";
        String w2 = "-";
        cellSpan(healthTbl, PdfLocalizer.get(ctx, "वजन", "Weight"),  fSmall, C_WHITE,    C_DARK, 1, 1, Element.ALIGN_LEFT);
        cellSpan(healthTbl, w1,     fSmall, C_WHITE,    C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(healthTbl, w2,     fSmall, C_WHITE,    C_DARK, 1, 1, Element.ALIGN_CENTER);

        // Height row
        String h1 = "-";
        String h2 = "-";
        cellSpan(healthTbl, PdfLocalizer.get(ctx, "उंची", "Height"), fSmall, C_ROW_ALT, C_DARK, 1, 1, Element.ALIGN_LEFT);
        cellSpan(healthTbl, h1,     fSmall, C_ROW_ALT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(healthTbl, h2,     fSmall, C_ROW_ALT, C_DARK, 1, 1, Element.ALIGN_CENTER);

        panel.addElement(healthTbl);

        // ── Grade scale at bottom ─────────────────────────────────────────────
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 15)));

        // Title with rounded background (left-aligned)
        PdfPTable gsTitleTbl = new PdfPTable(1);
        gsTitleTbl.setWidthPercentage(30);
        gsTitleTbl.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell gsTitleCell = new PdfPCell();
        gsTitleCell.setBorder(Rectangle.NO_BORDER);
        gsTitleCell.setBackgroundColor(new BaseColor(254, 221, 101));
        try {
            Image img = marathiImg(ctx, PdfLocalizer.get(ctx, "श्रेणी तक्ता :", "Grade Scale :"), 8, true, C_DARK);
            img.setAlignment(Element.ALIGN_CENTER);
            gsTitleCell.addElement(img);
        } catch (Exception e) {}
        gsTitleTbl.addCell(gsTitleCell);
        panel.addElement(gsTitleTbl);
        
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 2)));

        String[][] scale = getGradeScale(ctx);
        float[] gsWidths = new float[scale.length];
        Arrays.fill(gsWidths, 1f);
        PdfPTable gsTbl = new PdfPTable(gsWidths);
        gsTbl.setWidthPercentage(100);

        // Range row
        for (String[] entry : scale) {
            cellSpan(gsTbl, entry[0], fMicro, BaseColor.WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }
        // Grade label row
        for (String[] entry : scale) {
            cellSpan(gsTbl, entry[1], fSmallBold, new BaseColor(240, 245, 250), C_DARK, 1, 1, Element.ALIGN_CENTER);
        }
        panel.addElement(gsTbl);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RIGHT PANEL: Cover / Student info
    // ─────────────────────────────────────────────────────────────────────────
    private static void buildRightPanel(PdfPCell panel, Context ctx, School school,
                                         ClassModel cls, Student student) throws Exception {

        // Push text down into the yellow area
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 30)));

        String udiseLine = PdfLocalizer.get(ctx, "School UDISE: ", "School UDISE: ") + nvl(school != null ? school.udiseCode : null) 
                + PdfLocalizer.get(ctx, "\nजिल्हा परिषद", "\nZilla Parishad");
        try {
            Image udiseImg = marathiImg(ctx, udiseLine, 9, false, C_DARK);
            udiseImg.setAlignment(Element.ALIGN_CENTER);
            panel.addElement(udiseImg);
        } catch (Exception e) {}

        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 8)));

        try {
            Image schoolImg = marathiImg(ctx, nvl(school != null ? school.name : PdfLocalizer.get(ctx, "शाळेचे नाव", "SCHOOL NAME")), 20, true, C_DARK);
            schoolImg.setAlignment(Element.ALIGN_CENTER);
            panel.addElement(schoolImg);
        } catch (Exception e) {}

        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 6)));

        if (school != null && school.address != null && !school.address.isEmpty()) {
            try {
                Image addrImg = marathiImg(ctx, school.address, 10, false, C_DARK);
                addrImg.setAlignment(Element.ALIGN_CENTER);
                panel.addElement(addrImg);
            } catch (Exception e) {}
        }

        String yearLabel = cls != null ? nvl(cls.academicYearLabel) : "";
        try {
            Image yearImg = marathiImg(ctx, PdfLocalizer.get(ctx, "सन: ", "Year: ") + yearLabel, 11, true, C_DARK);
            yearImg.setAlignment(Element.ALIGN_CENTER);
            panel.addElement(yearImg);
        } catch (Exception e) {}

        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 15)));

        try {
            Image titleImg = marathiImg(ctx, PdfLocalizer.get(ctx, "प्रगती पत्रक", "PROGRESS CARD"), 28, true, C_DARK);
            titleImg.setAlignment(Element.ALIGN_CENTER);
            panel.addElement(titleImg);
        } catch (Exception e) {}

        // Push student details grid below the yellow area
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 65)));

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
                    ageStr = years + PdfLocalizer.get(ctx, " व. ", " Y. ") + months + PdfLocalizer.get(ctx, " म.", " M.");
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
        addDetailFull(det, ctx, PdfLocalizer.get(ctx, "• नाव", "• Name"), ": " + nvl(student.name), 4);

        // Row 2: स्टुडंट ID | रजि.नंबर
        addDetailPair(det, ctx,
                PdfLocalizer.get(ctx, "• स्टुडंट ID", "• Student ID"),    ": " + nvl(student.studentIdNumber),
                PdfLocalizer.get(ctx, "  रजि.नंबर", "  Reg. No."),     ": " + nvl(student.registrationNo));

        // Row 3: हजेरी क्रमांक | (blank for extra spacing)
        addDetailPair(det, ctx,
                PdfLocalizer.get(ctx, "• हजेरी क्रमांक", "• Roll No."), ": " + nvl(student.rollNo),
                PdfLocalizer.get(ctx, "  रजि.नंबर", "  Reg. No."),      ": " + nvl(student.rollNo2));

        // Row 4: इयत्ता | तुकडी
        addDetailPair(det, ctx,
                PdfLocalizer.get(ctx, "• इयत्ता", "• Class"),  ": " + className,
                PdfLocalizer.get(ctx, "  तुकडी", "  Division"),   ": " + division);

        // Row 5: माध्यम | जन्मतारीख
        addDetailPair(det, ctx,
                PdfLocalizer.get(ctx, "• माध्यम", "• Medium"),     ": " + nvl(student.medium),
                PdfLocalizer.get(ctx, "  जन्मतारीख", "  Date of Birth"),  ": " + nvl(student.dob));

        // Row 6: मातृभाषा | वय
        addDetailPair(det, ctx,
                PdfLocalizer.get(ctx, "• मातृभाषा", "• Mother Tongue"), ": " + nvl(student.motherTongue),
                PdfLocalizer.get(ctx, "  वय", "  Age"),        ": " + ageStr);

        // Row 7: आईचे नाव (full)
        addDetailFull(det, ctx, PdfLocalizer.get(ctx, "• आईचे नाव", "• Mother's Name"), ": " + nvl(student.motherName), 4);

        // Row 8: वडिलांचे नाव (full)
        addDetailFull(det, ctx, PdfLocalizer.get(ctx, "• वडिलांचे नाव", "• Father's Name"), ": " + nvl(student.fatherName), 4);

        // Row 9: पत्ता (full)
        addDetailFull(det, ctx, PdfLocalizer.get(ctx, "• पत्ता", "• Address"), ": " + nvl(student.address), 4);

        det.setKeepTogether(true);
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
        PdfPCell c = PdfGenerator.rawCell(text, font, BaseColor.WHITE, C_DARK, align);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(3);
        return c;
    }

    /**
     * Renders a Marathi/Devanagari string into a small Image.
     */
    private static Image marathiImg(Context ctx, String text, float ptSize,
                                     boolean bold, BaseColor color) throws Exception {
        return com.kartik.myschool.utils.pdf.MarathiText.renderLine(
                text, ptSize, bold,
                android.graphics.Color.rgb(color.getRed(), color.getGreen(), color.getBlue())
        );
    }
}
