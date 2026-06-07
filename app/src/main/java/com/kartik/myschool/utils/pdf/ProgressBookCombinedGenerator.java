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
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
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
 * Option 15 — Continuous Comprehensive Evaluation (सातत्यपूर्ण सर्वंकष मूल्यमापन)
 * Sem1 and Sem2 combined horizontally on Landscape A4 page.
 */
public class ProgressBookCombinedGenerator {

    public static void generateProgressBookCombined(Context ctx,
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
                        "CCE_Combined_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(15, 15, 20, 20);

                addContent(doc, ctx, school, cls, students, sem1Map, sem2Map);

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    private static void addContent(Document doc, Context ctx, School school, ClassModel cls,
                                   List<Student> students,
                                   Map<String, MarksRecord> sem1Map,
                                   Map<String, MarksRecord> sem2Map) throws Exception {

        // ── 1. Header ─────────────────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(3);
        hdr.setWidthPercentage(100);
        hdr.setWidths(new float[]{1.5f, 2f, 1.5f});

        PdfPCell cL = new PdfPCell();
        cL.setBorder(Rectangle.NO_BORDER);
        String udise = "Udise: " + nvl(school != null ? school.udiseCode : "");
        String sName = nvl(school != null ? school.name : "");
        cL.addElement(new Phrase(udise, fSmallBold));
        cL.addElement(new Phrase(sName, fSmall));
        hdr.addCell(cL);

        PdfPCell cC = new PdfPCell();
        cC.setBorder(Rectangle.NO_BORDER);
        cC.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            com.itextpdf.text.Image titleImg = MarathiText.renderLine("सातत्यपूर्ण सर्वंकष मूल्यमापन", 16, true, android.graphics.Color.BLACK);
            titleImg.setAlignment(Element.ALIGN_CENTER);
            cC.addElement(titleImg);
            com.itextpdf.text.Image subImg = MarathiText.renderLine("प्रथम व द्वितीय सत्र", 10, false, android.graphics.Color.BLACK);
            subImg.setAlignment(Element.ALIGN_CENTER);
            cC.addElement(subImg);
        } catch (Exception e) {
            cC.addElement(new Phrase("सातत्यपूर्ण सर्वंकष मूल्यमापन", fTitle));
            cC.addElement(new Phrase("प्रथम व द्वितीय सत्र", fSmall));
        }
        hdr.addCell(cC);

        PdfPCell cR = new PdfPCell();
        cR.setBorder(Rectangle.NO_BORDER);
        cR.setHorizontalAlignment(Element.ALIGN_RIGHT);
        String year = "सन : " + nvl(cls != null ? cls.academicYearLabel : "");
        String cDiv = "इयत्ता: " + nvl(cls != null ? cls.className : "") + ", तुकडी: " + nvl(cls != null ? cls.division : "-");
        Phrase p1 = new Phrase(year, fSmallBold);
        p1.getFont().setColor(C_DARK);
        Paragraph pr1 = new Paragraph(p1);
        pr1.setAlignment(Element.ALIGN_RIGHT);
        cR.addElement(pr1);
        
        Phrase p2 = new Phrase(cDiv, fSmallBold);
        p2.getFont().setColor(C_DARK);
        Paragraph pr2 = new Paragraph(p2);
        pr2.setAlignment(Element.ALIGN_RIGHT);
        cR.addElement(pr2);
        hdr.addCell(cR);

        hdr.setSpacingAfter(8);
        doc.add(hdr);

        // ── 2. Table Setup ────────────────────────────────────────────────────
        List<Subject> allSubs = cls != null && cls.subjects != null ? cls.subjects : new ArrayList<>();
        int subCols = allSubs.size() * 2;
        int totalCols = 4 + subCols + 3;

        float[] widths = new float[totalCols];
        int c = 0;
        widths[c++] = 0.5f; // अ.नं
        widths[c++] = 2.4f; // विद्यार्थ्याचे नाव
        widths[c++] = 0.45f; // उपस्थिती (vertical)
        widths[c++] = 0.35f; // सत्र (vertical)
        
        for (Subject sub : allSubs) {
            widths[c++] = 0.5f; // गुण
            widths[c++] = 0.5f; // श्रेणी
        }
        
        widths[c++] = 0.5f; // एकूण (vertical)
        widths[c++] = 0.7f; // शेकडा गुण
        widths[c++] = 0.45f; // श्रेणी (vertical)

        PdfPTable tbl = new PdfPTable(widths);
        tbl.setWidthPercentage(100);
        tbl.setHeaderRows(2); // Repeat headers on new pages

        // ── 3. Table Headers (Row 1) ──────────────────────────────────────────
        MarathiText.cell(tbl, "अ.नं", 9, true, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
        MarathiText.cell(tbl, "विद्यार्थ्याचे नाव", 9, true, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "उपस्थिती", fSmallBold, C_HEADER_BG, C_DARK, 1, 2);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "सत्र", fSmallBold, C_HEADER_BG, C_DARK, 1, 2);

        for (Subject sub : allSubs) {
            MarathiText.cell(tbl, nvl(sub.name), 9, true, C_HEADER_BG, C_DARK, 2, 1, Element.ALIGN_CENTER);
        }

        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "एकूण", fSmallBold, C_HEADER_BG, C_DARK, 1, 2);
        MarathiText.cell(tbl, "शेकडा गुण", 9, true, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "श्रेणी", fSmallBold, C_HEADER_BG, C_DARK, 1, 2);

        // ── 4. Table Headers (Row 2) ──────────────────────────────────────────
        for (Subject sub : allSubs) {
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "गुण", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "श्रेणी", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        }

        // ── 5. Data Rows ──────────────────────────────────────────────────────
        int sr = 1;
        boolean alt = false;
        if (students != null) {
            for (Student student : students) {
                BaseColor bg = alt ? C_ROW_ALT : C_WHITE;
                alt = !alt;

                MarksRecord s1 = sem1Map != null ? sem1Map.get(student.id) : null;
                MarksRecord s2 = sem2Map != null ? sem2Map.get(student.id) : null;

                // Calculate total attendance
                int presentDays = 0;
                if (student.monthlyAttendance != null) {
                    for (String m : student.monthlyAttendance.keySet()) {
                        String att = student.monthlyAttendance.get(m);
                        if (att != null && att.contains("/")) {
                            try { presentDays += Integer.parseInt(att.split("/")[0].trim()); } catch (Exception ignored) {}
                        } else if (att != null && !att.isEmpty()) {
                            try { presentDays += Integer.parseInt(att.trim()); } catch (Exception ignored) {}
                        }
                    }
                }
                String attStr = presentDays > 0 ? String.valueOf(presentDays) : "";

                // ROW 1: Sem 1
                PdfPCell cSr = noPadCell(String.valueOf(sr++), bg);
                cSr.setRowspan(2);
                tbl.addCell(cSr);

                PdfPCell cName = noPadCell(nvl(student.name), bg);
                cName.setHorizontalAlignment(Element.ALIGN_LEFT);
                cName.setPaddingLeft(4);
                cName.setRowspan(2);
                tbl.addCell(cName);

                PdfPCell cAtt = noPadCell(attStr, bg);
                cAtt.setRowspan(2);
                tbl.addCell(cAtt);

                tbl.addCell(noPadCell("I", bg));

                for (Subject sub : allSubs) {
                    MarksRecord.SubjectMarksDetail d = s1 != null ? s1.detailedMarks.get(MarksRecord.sanitizeKey(sub.name)) : null;
                    String mStr = d != null && d.grandTotal > 0 ? String.valueOf(d.grandTotal) : "";
                    String gStr = d != null ? nvl(d.grade) : "";
                    tbl.addCell(noPadCell(mStr, bg));
                    tbl.addCell(noPadCellBold(gStr, bg));
                }

                String s1Total = s1 != null && s1.totalObtained > 0 ? String.valueOf((int)s1.totalObtained) : "";
                String s1Perc = s1 != null && s1.percentage > 0 ? String.format(java.util.Locale.US, "%.1f", s1.percentage) : "";
                String s1Grade = s1 != null ? nvl(s1.grade) : "";
                tbl.addCell(noPadCellBold(s1Total, bg));
                tbl.addCell(noPadCellBold(s1Perc, bg));
                tbl.addCell(noPadCellBold(s1Grade, bg));

                // ROW 2: Sem 2
                tbl.addCell(noPadCell("II", bg));

                for (Subject sub : allSubs) {
                    MarksRecord.SubjectMarksDetail d = s2 != null ? s2.detailedMarks.get(MarksRecord.sanitizeKey(sub.name)) : null;
                    String mStr = d != null && d.grandTotal > 0 ? String.valueOf(d.grandTotal) : "";
                    String gStr = d != null ? nvl(d.grade) : "";
                    tbl.addCell(noPadCell(mStr, bg));
                    tbl.addCell(noPadCellBold(gStr, bg));
                }

                String s2Total = s2 != null && s2.totalObtained > 0 ? String.valueOf((int)s2.totalObtained) : "";
                String s2Perc = s2 != null && s2.percentage > 0 ? String.format(java.util.Locale.US, "%.1f", s2.percentage) : "";
                String s2Grade = s2 != null ? nvl(s2.grade) : "";
                tbl.addCell(noPadCellBold(s2Total, bg));
                tbl.addCell(noPadCellBold(s2Perc, bg));
                tbl.addCell(noPadCellBold(s2Grade, bg));
            }
        }

        doc.add(tbl);
    }

    private static PdfPCell noPadCell(String text, BaseColor bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(C_DARK);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingTop(3);
        c.setPaddingBottom(3);
        try {
            com.itextpdf.text.Image img = MarathiText.renderLine(text, 9, false, android.graphics.Color.BLACK);
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, fSmall));
        }
        return c;
    }

    private static PdfPCell noPadCellBold(String text, BaseColor bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(C_DARK);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingTop(3);
        c.setPaddingBottom(3);
        try {
            com.itextpdf.text.Image img = MarathiText.renderLine(text, 9, true, android.graphics.Color.BLACK);
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, fSmallBold));
        }
        return c;
    }
}
