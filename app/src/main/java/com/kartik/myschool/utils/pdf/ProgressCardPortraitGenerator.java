package com.kartik.myschool.utils.pdf;

import android.content.Context;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.utils.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static com.kartik.myschool.utils.PdfGenerator.*;

/**
 * Option 14 — Progress Card Portrait (प्रगती पत्रक - Portrait)
 */
public class ProgressCardPortraitGenerator {

    private static final BaseColor C_BLUE_BORDER = new BaseColor(96, 150, 180); // Border color
    private static final BaseColor C_PINK_BG = new BaseColor(252, 228, 236); // Light pink
    private static final BaseColor C_PINK_FG = new BaseColor(216, 27, 96); // Magenta text
    private static final BaseColor C_GREY_BG = new BaseColor(240, 242, 245); // Light grey block
    private static final BaseColor C_BULLET = new BaseColor(41, 128, 185); // Blue bullet

    private static final String[] MONTHS_EN = {"JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC", "JAN", "FEB", "MAR", "APR", "MAY"};
    private static final String[] MONTHS_MR = {"जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे", "डिसें", "जाने", "फेब्रु", "मार्च", "एप्रिल", "मे"};

    // Draws the outer rounded rectangle
    private static class BorderEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle rect = document.getPageSize();
            canvas.setColorStroke(C_BLUE_BORDER);
            canvas.setLineWidth(2f);
            canvas.roundRectangle(25, 25, rect.getWidth() - 50, rect.getHeight() - 50, 15);
            canvas.stroke();
        }
    }

    // Draws rounded background for the grey block
    private static class RoundedBackgroundEvent implements PdfPCellEvent {
        private BaseColor bgColor;
        public RoundedBackgroundEvent(BaseColor bgColor) { this.bgColor = bgColor; }
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            canvas.setColorFill(bgColor);
            canvas.roundRectangle(position.getLeft(), position.getBottom(), position.getWidth(), position.getHeight(), 10);
            canvas.fill();
        }
    }

    public static void generateProgressCardPortrait(Context ctx,
                                                    School school,
                                                    ClassModel cls,
                                                    List<Student> students,
                                                    PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx),
                        "ProgressCardPortrait_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4);
                PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                writer.setPageEvent(new BorderEvent());
                doc.open();
                doc.setMargins(40, 40, 50, 50);

                boolean isFirst = true;
                if (students != null) {
                    for (Student student : students) {
                        if (!isFirst) doc.newPage();
                        isFirst = false;
                        addStudentPage(doc, ctx, school, cls, student);
                    }
                }
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    private static void addStudentPage(Document doc, Context ctx, School school, ClassModel cls, Student student) throws Exception {

        // ── 1. Header ─────────────────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(1);
        hdr.setWidthPercentage(100);

        String udise = PdfLocalizer.get(ctx, "School UDISE: ", "School UDISE: ") + nvl(school != null ? school.udiseCode : "");
        addCenterText(hdr, udise, fSmall);
        
        String prefix = PdfLocalizer.get(ctx, "जिल्हा परिषद प्राथमिक शाळा ", "Zilla Parishad Primary School ");
        String sName = prefix + nvl(school != null ? school.name : "");
        addCenterText(hdr, sName, fTitle); // Large bold

        String addr = nvl(school != null ? school.address : "");
        addCenterText(hdr, addr, fSmall);

        String year = PdfLocalizer.get(ctx, "सन: ", "Year: ") + nvl(cls != null ? cls.academicYearLabel : "");
        addCenterText(hdr, year, fSmallBold);
        
        hdr.setSpacingAfter(10);
        doc.add(hdr);

        // ── 2. "प्रगती पत्रक" Pill ──────────────────────────────────────────
        PdfPTable pillTbl = new PdfPTable(1);
        pillTbl.setWidthPercentage(45);
        PdfPCell pillCell = new PdfPCell();
        pillCell.setBorder(Rectangle.NO_BORDER);
        pillCell.setCellEvent(new RoundedBackgroundEvent(C_PINK_BG));
        pillCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        pillCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        pillCell.setPaddingTop(10);
        pillCell.setPaddingBottom(12);

        try {
            com.itextpdf.text.Image img = MarathiText.renderLine(PdfLocalizer.get(ctx, "प्रगती पत्रक", "PROGRESS CARD"), 22, true, android.graphics.Color.rgb(C_PINK_FG.getRed(), C_PINK_FG.getGreen(), C_PINK_FG.getBlue()));
            img.setAlignment(Element.ALIGN_CENTER);
            pillCell.addElement(img);
        } catch (Exception e) {
            Font pinkFont = sMarathiBase != null ? new Font(sMarathiBase, 20, Font.BOLD, C_PINK_FG)
                                                 : new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, C_PINK_FG);
            pillCell.setPhrase(new Phrase(PdfLocalizer.get(ctx, "प्रगती पत्रक", "PROGRESS CARD"), pinkFont));
        }
        pillTbl.addCell(pillCell);
        pillTbl.setSpacingAfter(20);
        doc.add(pillTbl);

        // ── 3. Student Details Block ──────────────────────────────────────────
        PdfPTable block = new PdfPTable(1);
        block.setWidthPercentage(90);
        
        PdfPCell blockCell = new PdfPCell();
        blockCell.setBorder(Rectangle.NO_BORDER);
        blockCell.setCellEvent(new RoundedBackgroundEvent(C_GREY_BG));
        blockCell.setPadding(20);

        PdfPTable det = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2f});
        det.setWidthPercentage(100);

        String className = cls != null ? nvl(cls.className) : "";
        String division  = cls != null ? nvl(cls.division) : "-";

        String ageStr = "-";
        if (student.dob != null && !student.dob.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
                java.util.Date dob = sdf.parse(student.dob);
                if (dob != null) {
                    long diffMs = System.currentTimeMillis() - dob.getTime();
                    long years  = diffMs / (365L * 24 * 60 * 60 * 1000);
                    long months = (diffMs % (365L * 24 * 60 * 60 * 1000)) / (30L * 24 * 60 * 60 * 1000);
                    ageStr = years + PdfLocalizer.get(ctx, " व., ", " Y., ") + months + PdfLocalizer.get(ctx, " म.", " M.");
                }
            } catch (Exception ignored) {}
        }

        addRowFull(det, PdfLocalizer.get(ctx, "नाव", "Name"), ": " + nvl(student.name));
        addPairEmptyRight(det, PdfLocalizer.get(ctx, "स्टुडंट ID", "Student ID"), ": " + nvl(student.studentIdNumber));
        addPair(det, PdfLocalizer.get(ctx, "हजेरी क्रमांक", "Roll No."), ": " + nvl(student.rollNo), PdfLocalizer.get(ctx, "रजि.नंबर", "Reg. No."), ": " + nvl(student.registrationNo));
        addPair(det, PdfLocalizer.get(ctx, "इयत्ता", "Class"), ": " + className, PdfLocalizer.get(ctx, "तुकडी", "Division"), ": " + division);
        addPair(det, PdfLocalizer.get(ctx, "माध्यम", "Medium"), ": " + nvl(student.medium), PdfLocalizer.get(ctx, "जन्मतारीख", "Date of Birth"), ": " + nvl(student.dob));
        addPair(det, PdfLocalizer.get(ctx, "मातृभाषा", "Mother Tongue"), ": " + nvl(student.motherTongue), PdfLocalizer.get(ctx, "वय", "Age"), ": " + ageStr);
        addRowFull(det, PdfLocalizer.get(ctx, "आईचे नाव", "Mother's Name"), ": " + nvl(student.motherName));
        addRowFull(det, PdfLocalizer.get(ctx, "वडिलांचे नाव", "Father's Name"), ": " + nvl(student.fatherName));
        addRowFull(det, PdfLocalizer.get(ctx, "पत्ता", "Address"), ": " + nvl(student.address));

        blockCell.addElement(det);
        block.addCell(blockCell);
        block.setSpacingAfter(15);
        doc.add(block);

        // ── 4. Notice Text ────────────────────────────────────────────────────
        PdfPTable notice = new PdfPTable(1);
        notice.setWidthPercentage(90);
        PdfPCell nCell = new PdfPCell();
        nCell.setBorder(Rectangle.NO_BORDER);
        nCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        try {
            com.itextpdf.text.Image nImg = MarathiText.renderLine(PdfLocalizer.get(ctx, "उन्हाळी सुट्टीनंतर शाळा १५ जून २०२६ रोजी सुरू होईल .", "School will reopen on June 15, 2026 after summer vacation."), 10, false, android.graphics.Color.BLACK);
            nCell.addElement(nImg);
        } catch (Exception e) {
            nCell.setPhrase(new Phrase(PdfLocalizer.get(ctx, "उन्हाळी सुट्टीनंतर शाळा १५ जून २०२६ रोजी सुरू होईल .", "School will reopen on June 15, 2026 after summer vacation."), fSmall));
        }
        notice.addCell(nCell);
        notice.setSpacingAfter(30); // push attendance table down a bit
        doc.add(notice);

        // ── 5. Attendance ─────────────────────────────────────────────────────
        PdfPTable attTitle = new PdfPTable(1);
        attTitle.setWidthPercentage(95);
        PdfPCell attTitleCell = new PdfPCell();
        attTitleCell.setBorder(Rectangle.NO_BORDER);
        try {
            com.itextpdf.text.Image aImg = MarathiText.renderLine(PdfLocalizer.get(ctx, "उपस्थिती :", "Attendance :"), 12, true, android.graphics.Color.rgb(C_PINK_FG.getRed(), C_PINK_FG.getGreen(), C_PINK_FG.getBlue()));
            attTitleCell.addElement(aImg);
        } catch (Exception e) {
            attTitleCell.setPhrase(new Phrase(PdfLocalizer.get(ctx, "उपस्थिती :", "Attendance :"), fSmallBold));
        }
        attTitle.addCell(attTitleCell);
        attTitle.setSpacingAfter(5);
        doc.add(attTitle);

        PdfPTable attTbl = new PdfPTable(13); // महिना + 12 months
        attTbl.setWidthPercentage(95);
        
        // Month headers
        MarathiText.cell(attTbl, PdfLocalizer.get(ctx, "महिना", "Month"), 9, true, C_GREY_BG, android.graphics.Color.BLACK, 1, 1, Element.ALIGN_CENTER);
        
        String[] monthsEN = MONTHS_EN;
        String[] monthsMR = MONTHS_MR;
        boolean isEn = PdfLocalizer.isEnglish(ctx);
        String[] activeMonths = isEn ? monthsEN : monthsMR;
        
        for (String m : activeMonths) {
            PdfPCell mc = new PdfPCell();
            mc.setHorizontalAlignment(Element.ALIGN_CENTER);
            mc.setVerticalAlignment(Element.ALIGN_MIDDLE);
            mc.setBackgroundColor(C_WHITE);
            mc.setBorderColor(C_BLUE_BORDER);
            mc.setPaddingTop(5);
            mc.setPaddingBottom(5);
            try {
                com.itextpdf.text.Image img = com.kartik.myschool.utils.pdf.MarathiText.renderLine(m, 10, true, android.graphics.Color.BLACK);
                img.setAlignment(Element.ALIGN_CENTER);
                mc.addElement(img);
            } catch (Exception e) {
                mc.setPhrase(new Phrase(m, fSmallBold));
            }
            attTbl.addCell(mc);
        }

        // Parse attendance to integer arrays
        int[] wd = new int[12];
        int[] pd = new int[12];
        if (student.monthlyAttendance != null) {
            for (int i = 0; i < 12; i++) {
                String att = student.monthlyAttendance.get(monthsMR[i]);
                if (att != null && att.contains("/")) {
                    String[] parts = att.split("/");
                    try { pd[i] = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
                    try { wd[i] = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                } else if (att != null && !att.isEmpty()) {
                    try { pd[i] = Integer.parseInt(att.trim()); } catch (Exception ignored) {}
                }
            }
        }

        // Working Days row
        MarathiText.cell(attTbl, PdfLocalizer.get(ctx, "कामाचे दिवस", "Working Days"), 9, true, C_GREY_BG, android.graphics.Color.BLACK, 1, 1, Element.ALIGN_CENTER);
        for (int i = 0; i < 12; i++) {
            String val = wd[i] > 0 ? String.valueOf(wd[i]) : "";
            PdfPCell wc = new PdfPCell(new Phrase(val, fSmall));
            wc.setHorizontalAlignment(Element.ALIGN_CENTER);
            wc.setBorderColor(C_BLUE_BORDER);
            wc.setPaddingTop(5);
            wc.setPaddingBottom(5);
            attTbl.addCell(wc);
        }

        // Present Days row
        MarathiText.cell(attTbl, PdfLocalizer.get(ctx, "हजर दिवस", "Present Days"), 9, true, C_GREY_BG, android.graphics.Color.BLACK, 1, 1, Element.ALIGN_CENTER);
        for (int i = 0; i < 12; i++) {
            String val = pd[i] > 0 ? String.valueOf(pd[i]) : (wd[i] > 0 ? "0" : "");
            PdfPCell pc = new PdfPCell(new Phrase(val, fSmall));
            pc.setHorizontalAlignment(Element.ALIGN_CENTER);
            pc.setBorderColor(C_BLUE_BORDER);
            pc.setPaddingTop(5);
            pc.setPaddingBottom(5);
            attTbl.addCell(pc);
        }

        doc.add(attTbl);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void addCenterText(PdfPTable tbl, String text, Font font) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingBottom(4);
        try {
            boolean bold = (font == fSmallBold || font == fTitle);
            int size = (font == fTitle) ? 18 : 10;
            com.itextpdf.text.Image img = MarathiText.renderLine(text, size, bold, android.graphics.Color.BLACK);
            img.setAlignment(Element.ALIGN_CENTER);
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, font));
        }
        tbl.addCell(c);
    }

    private static void addRowFull(PdfPTable tbl, String label, String value) {
        addLabel(tbl, label);
        addValue(tbl, value, 3);
    }

    private static void addPair(PdfPTable tbl, String l1, String v1, String l2, String v2) {
        addLabel(tbl, l1);
        addValue(tbl, v1, 1);
        addLabel(tbl, l2);
        addValue(tbl, v2, 1);
    }

    private static void addPairEmptyRight(PdfPTable tbl, String l1, String v1) {
        addLabel(tbl, l1);
        addValue(tbl, v1, 1);
        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(Rectangle.NO_BORDER);
        empty.setColspan(2);
        tbl.addCell(empty);
    }

    private static void addLabel(PdfPTable tbl, String text) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setPaddingBottom(6);
        try {
            // Add blue bullet
            Paragraph p = new Paragraph();
            p.add(new com.itextpdf.text.Chunk("● ", new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, C_BULLET)));
            com.itextpdf.text.Image img = MarathiText.renderLine(text, 10, false, android.graphics.Color.BLACK);
            p.add(new com.itextpdf.text.Chunk(img, 0, -2, true));
            c.addElement(p);
        } catch (Exception e) {
            c.setPhrase(new Phrase("● " + text, fSmall));
        }
        tbl.addCell(c);
    }

    private static void addValue(PdfPTable tbl, String text, int colspan) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setColspan(colspan);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setPaddingBottom(6);
        try {
            com.itextpdf.text.Image img = MarathiText.renderLine(text, 10, true, android.graphics.Color.BLACK);
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, fSmallBold));
        }
        tbl.addCell(c);
    }
}
