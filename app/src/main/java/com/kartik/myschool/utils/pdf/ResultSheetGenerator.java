package com.kartik.myschool.utils.pdf;

import android.content.Context;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.kartik.myschool.utils.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.kartik.myschool.utils.PdfGenerator.*;

/**
 * Option 13 — Result Sheet (निकालपत्रक)
 * Landscape A4, dynamic columns for academic and non-academic subjects.
 */
public class ResultSheetGenerator {

    private static final BaseColor C_PINK = new BaseColor(216, 27, 96); // ● निकालपत्रक ●
    private static final BaseColor C_HEADER_BG = new BaseColor(218, 230, 243); // Light blue header

    // Custom Page Event to draw the rectangular border
    private static class BorderEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle rect = document.getPageSize();
            canvas.setColorStroke(BaseColor.BLACK);
            canvas.setLineWidth(1f);
            canvas.rectangle(20, 20, rect.getWidth() - 40, rect.getHeight() - 40);
            canvas.stroke();
        }
    }

    public static void generateResultSheet(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           List<Student> students,
                                           Map<String, MarksRecord> marksMap,
                                           PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx),
                        "ResultSheet_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                writer.setPageEvent(new BorderEvent());
                doc.open();
                doc.setMargins(30, 30, 40, 40);

                // Split subjects
                List<Subject> acaSubs = new ArrayList<>();
                List<Subject> nonAcaSubs = new ArrayList<>();
                int maxTotal = 0;
                if (cls != null && cls.subjects != null) {
                    for (Subject s : cls.subjects) {
                        if (isNonAcademic(s.name)) {
                            nonAcaSubs.add(s);
                        } else {
                            acaSubs.add(s);
                            maxTotal += s.maxMarks > 0 ? s.maxMarks : 50;
                        }
                    }
                }

                // Chunk students to fit on pages if needed, but PdfPTable handles automatic pagination.
                // However, we want to repeat headers on new pages.
                addPageContent(doc, ctx, school, cls, students, marksMap, acaSubs, nonAcaSubs, maxTotal);

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    private static void addPageContent(Document doc, Context ctx, School school, ClassModel cls,
                                       List<Student> students, Map<String, MarksRecord> marksMap,
                                       List<Subject> acaSubs, List<Subject> nonAcaSubs, int maxTotal) throws Exception {

        // ── 1. Titles ─────────────────────────────────────────────────────────
        PdfPTable titleTbl = new PdfPTable(1);
        titleTbl.setWidthPercentage(100);

        PdfPCell c1 = new PdfPCell();
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            com.itextpdf.text.Image img = MarathiText.renderLine("● निकालपत्रक ●", 16, true, android.graphics.Color.rgb(C_PINK.getRed(), C_PINK.getGreen(), C_PINK.getBlue()));
            img.setAlignment(com.itextpdf.text.Image.MIDDLE);
            c1.addElement(img);
        } catch (Exception e) {
            Font pinkFont = sMarathiBase != null ? new Font(sMarathiBase, 16, Font.BOLD, C_PINK)
                                                 : new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, C_PINK);
            c1.setPhrase(new Phrase("● निकालपत्रक ●", pinkFont));
        }
        titleTbl.addCell(c1);

        String className = cls != null ? nvl(cls.className) : "";
        PdfPCell c2 = new PdfPCell();
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        c2.setPaddingBottom(10);
        try {
            com.itextpdf.text.Image img = MarathiText.renderLine("वार्षिक परीक्षा " + className, 10, true, android.graphics.Color.BLACK);
            img.setAlignment(com.itextpdf.text.Image.MIDDLE);
            c2.addElement(img);
        } catch (Exception e) {
            c2.setPhrase(new Phrase("वार्षिक परीक्षा " + className, fSmallBold));
        }
        titleTbl.addCell(c2);

        doc.add(titleTbl);

        // ── 2. Header Info ────────────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(new float[]{3f, 1.5f});
        hdr.setWidthPercentage(100);

        String schoolName = "शाळेचे नाव : " + nvl(school != null ? school.name : "");
        String udiseText  = "Udise: " + nvl(school != null ? school.udiseCode : "");
        String clsDiv     = "इयत्ता: " + className + ", तुकडी: " + nvl(cls != null ? cls.division : "-");
        String yearText   = "सन : " + nvl(cls != null ? cls.academicYearLabel : "");

        addHdrCell(hdr, schoolName, Element.ALIGN_LEFT);
        addHdrCell(hdr, udiseText,  Element.ALIGN_RIGHT);
        addHdrCell(hdr, clsDiv,     Element.ALIGN_LEFT);
        addHdrCell(hdr, yearText,   Element.ALIGN_RIGHT);

        hdr.setSpacingAfter(8);
        doc.add(hdr);

        // ── 3. Table Setup ────────────────────────────────────────────────────
        int totalCols = 3 + (acaSubs.size() * 3) + nonAcaSubs.size() + 3;
        float[] widths = new float[totalCols];
        int c = 0;
        widths[c++] = 0.6f; // अ.नं.
        widths[c++] = 2.8f; // नाव
        widths[c++] = 0.8f; // हजर दिन
        for (int i = 0; i < acaSubs.size(); i++) {
            widths[c++] = 0.7f; // गुण
            widths[c++] = 0.7f; // ग्रेस
            widths[c++] = 0.7f; // शेरा
        }
        for (int i = 0; i < nonAcaSubs.size(); i++) {
            widths[c++] = 1.0f; // श्रेणी
        }
        widths[c++] = 0.9f; // एकूण 300
        widths[c++] = 0.9f; // शेकडा गुण
        widths[c++] = 1.0f; // शेरा

        PdfPTable tbl = new PdfPTable(widths);
        tbl.setWidthPercentage(100);
        tbl.setHeaderRows(2); // Repeat headers on new pages

        // ── 4. Table Headers (Row 1) ──────────────────────────────────────────
        addTh(tbl, "अ.नं.", 1, 2);
        addTh(tbl, "नाव", 1, 2);
        addTh(tbl, "हजर दिन", 1, 2);
        
        for (Subject sub : acaSubs) {
            addTh(tbl, nvl(sub.name), 3, 1);
        }
        for (Subject sub : nonAcaSubs) {
            addTh(tbl, nvl(sub.name), 1, 1);
        }
        
        addTh(tbl, "एकूण " + maxTotal, 1, 2);
        addTh(tbl, "शेकडा गुण", 1, 2);
        addTh(tbl, "शेरा", 1, 2);

        // ── 5. Table Headers (Row 2) ──────────────────────────────────────────
        for (int i = 0; i < acaSubs.size(); i++) {
            addTh(tbl, "गुण", 1, 1);
            addTh(tbl, "ग्रेस", 1, 1);
            addTh(tbl, "शेरा", 1, 1);
        }
        for (int i = 0; i < nonAcaSubs.size(); i++) {
            addTh(tbl, "श्रेणी", 1, 1);
        }

        // ── 6. Student Data ───────────────────────────────────────────────────
        if (students != null) {
            int rowIdx = 1;
            for (Student student : students) {
                MarksRecord rec = marksMap != null ? marksMap.get(student.id) : null;
                
                int totalObtained = 0;
                boolean hasFailed = false;

                addTd(tbl, String.valueOf(rowIdx++), Element.ALIGN_CENTER);
                addTd(tbl, nvl(student.name),        Element.ALIGN_LEFT);
                addTd(tbl, rec != null ? String.valueOf(rec.presentDays) : "-", Element.ALIGN_CENTER);

                // Academic Subjects
                for (Subject sub : acaSubs) {
                    MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
                    int obt = d != null ? d.grandTotal : 0;
                    totalObtained += obt;
                    
                    int minPass = sub.maxMarks > 0 ? (int) Math.ceil(sub.maxMarks * 0.35) : 18;
                    boolean passed = obt >= minPass;
                    if (!passed) hasFailed = true;

                    addTd(tbl, String.valueOf(obt), Element.ALIGN_CENTER);
                    addTd(tbl, "-",                 Element.ALIGN_CENTER); // ग्रेस
                    addTd(tbl, passed ? "P" : "F",  Element.ALIGN_CENTER); // शेरा
                }

                // Non-Academic Subjects
                for (Subject sub : nonAcaSubs) {
                    MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
                    String grade = d != null && d.grade != null && !d.grade.isEmpty() ? normalizeGrade(d.grade) : "-";
                    addTd(tbl, grade, Element.ALIGN_CENTER);
                }

                // Totals
                String percStr = maxTotal > 0 ? String.format(java.util.Locale.US, "%.1f", (totalObtained * 100.0f) / maxTotal) : "0.0";
                String passResult = hasFailed ? "अनुत्तीर्ण" : "उत्तीर्ण";

                addTd(tbl, String.valueOf(totalObtained), Element.ALIGN_CENTER);
                addTd(tbl, percStr,                       Element.ALIGN_CENTER);
                addTd(tbl, passResult,                    Element.ALIGN_CENTER);
            }
        }
        
        doc.add(tbl);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void addHdrCell(PdfPTable tbl, String text, int align) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPaddingBottom(4);
        try {
            com.itextpdf.text.Image img = MarathiText.renderLine(text, 10, true, android.graphics.Color.BLACK);
            img.setAlignment(align == Element.ALIGN_LEFT ? com.itextpdf.text.Image.LEFT : com.itextpdf.text.Image.RIGHT);
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, fSmallBold));
        }
        tbl.addCell(c);
    }

    private static void addTh(PdfPTable tbl, String text, int colspan, int rowspan) {
        MarathiText.cell(tbl, text, 9, true, C_HEADER_BG, C_DARK, colspan, rowspan, Element.ALIGN_CENTER);
    }

    private static void addTd(PdfPTable tbl, String text, int align) {
        MarathiText.cell(tbl, text, 9, false, null, C_DARK, 1, 1, align);
    }

    private static boolean isNonAcademic(String sub) {
        if (sub == null) return false;
        String s = sub.trim();
        return s.contains("कला") || s.contains("कार्यानुभव") || s.contains("शा.शि.") || s.contains("शारीरिक");
    }
}
