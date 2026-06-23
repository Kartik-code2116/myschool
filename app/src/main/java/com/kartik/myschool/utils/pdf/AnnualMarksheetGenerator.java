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
 * Option 12 — Annual Marksheet (वार्षिक परीक्षा गुणपत्रक)
 */
public class AnnualMarksheetGenerator {

    private static final BaseColor C_PINK = new BaseColor(216, 27, 96); // ● गुणपत्रक ●

    // Custom Page Event to draw the rounded border
    private static class RoundedBorderEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            canvas.setColorStroke(BaseColor.BLACK);
            canvas.setLineWidth(2f);
            
            // Draw the box dynamically based on the current document margins!
            // We draw it 10 points outside the content area.
            float left = document.left() - 10f;
            float bottom = document.bottom() - 10f;
            float width = document.right() - document.left() + 20f;
            float height = document.top() - document.bottom() + 20f;
            
            canvas.roundRectangle(left, bottom, width, height, 15f);
            canvas.stroke();
        }
    }

    public static void generateAnnualMarksheet(Context ctx,
                                               School school,
                                               ClassModel cls,
                                               List<Student> students,
                                               Map<String, MarksRecord> marksMap,
                                               PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx),
                        "AnnualMarksheet_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
                PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                writer.setPageEvent(new RoundedBorderEvent());
                doc.open();

                boolean isFirst = true;
                if (students != null) {
                    for (Student student : students) {
                        if (!isFirst) doc.newPage();
                        isFirst = false;
                        MarksRecord rec = marksMap != null ? marksMap.get(student.id) : null;
                        addStudentPage(doc, ctx, school, cls, student, rec);
                    }
                }
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    private static void addStudentPage(Document doc, Context ctx, School school, ClassModel cls,
                                       Student student, MarksRecord rec) throws Exception {

        // ── 1. Titles ─────────────────────────────────────────────────────────
        PdfPTable titleTbl = new PdfPTable(1);
        titleTbl.setWidthPercentage(100);

        Font pinkFont = sMarathiBase != null ? new Font(sMarathiBase, 18, Font.BOLD, C_PINK)
                                             : new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, C_PINK);
        
        PdfPCell c1 = PdfGenerator.rawCell(PdfLocalizer.get(ctx, "● गुणपत्रक ●", "● MARKSHEET ●"), pinkFont, BaseColor.WHITE, C_PINK, Element.ALIGN_CENTER);
        c1.setBorder(Rectangle.NO_BORDER);
        titleTbl.addCell(c1);

        PdfPCell c2 = PdfGenerator.rawCell(PdfLocalizer.get(ctx, "वार्षिक परीक्षा", "Annual Examination"), fNormal, BaseColor.WHITE, C_DARK, Element.ALIGN_CENTER);
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPaddingBottom(15);
        titleTbl.addCell(c2);

        doc.add(titleTbl);

        // ── 2. Header Info ────────────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(new float[]{3f, 1.5f});
        hdr.setWidthPercentage(100);

        String schoolName = PdfLocalizer.get(ctx, "शाळेचे नाव : ", "School Name: ") + nvl(school != null ? school.name : "");
        String udiseText  = PdfLocalizer.get(ctx, "युडायस : ", "UDISE : ") + nvl(school != null ? school.udiseCode : "");
        String stuName    = PdfLocalizer.get(ctx, "नाव : ", "Name: ") + nvl(student != null ? student.name : "");
        String yearText   = PdfLocalizer.get(ctx, "सन : ", "Year: ") + nvl(cls != null ? cls.academicYearLabel : "");
        String rollText   = PdfLocalizer.get(ctx, "हजेरी क्रमांक : ", "Roll No: ") + nvl(student != null ? student.rollNo : "");
        String clsDiv     = PdfLocalizer.get(ctx, "इयत्ता: ", "Class: ") + nvl(cls != null ? cls.className : "") 
                + PdfLocalizer.get(ctx, ", तुकडी: ", ", Division: ") + nvl(cls != null ? cls.division : "");

        addHdrCell(hdr, schoolName, Element.ALIGN_LEFT);
        addHdrCell(hdr, udiseText,  Element.ALIGN_RIGHT);
        addHdrCell(hdr, stuName,    Element.ALIGN_LEFT);
        addHdrCell(hdr, yearText,   Element.ALIGN_RIGHT);
        addHdrCell(hdr, rollText,   Element.ALIGN_LEFT);
        addHdrCell(hdr, clsDiv,     Element.ALIGN_RIGHT);

        hdr.setSpacingAfter(10);
        doc.add(hdr);

        // ── 3. Marks Table ────────────────────────────────────────────────────
        float[] widths = {0.8f, 2.5f, 1.5f, 1.5f, 1.2f, 1.5f};
        PdfPTable tbl = new PdfPTable(widths);
        tbl.setWidthPercentage(100);

        BaseColor bgHeader = new BaseColor(218, 230, 243); // Light blue header

        addTh(tbl, PdfLocalizer.get(ctx, "अ.नं.", "Sr.No."), bgHeader);
        addTh(tbl, PdfLocalizer.get(ctx, "विषय", "Subject"), bgHeader);
        addTh(tbl, PdfLocalizer.get(ctx, "किमान\nआवश्यक गुण", "Min\nRequired Marks"), bgHeader);
        
        // Find academic max marks dynamically for header (assume first academic subject's max marks)
        int acaMax = 50;
        List<Subject> academicSubs = new ArrayList<>();
        List<Subject> nonAcSubs = new ArrayList<>();
        if (cls != null && cls.subjects != null) {
            for (Subject s : cls.subjects) {
                if (isNonAcademic(s.name)) nonAcSubs.add(s);
                else academicSubs.add(s);
            }
        }
        if (!academicSubs.isEmpty() && academicSubs.get(0).maxMarks > 0) {
            acaMax = academicSubs.get(0).maxMarks;
        }
        addTh(tbl, PdfLocalizer.get(ctx, "एकूण गुण\n(" + acaMax + " पैकी)\nप्राप्त", "Total Marks\n(Out of " + acaMax + ")\nObtained"), bgHeader);
        addTh(tbl, PdfLocalizer.get(ctx, "सवलतीचे\nगुण", "Grace\nMarks"), bgHeader);
        addTh(tbl, PdfLocalizer.get(ctx, "शेरा\n(उत्तीर्ण/\nअनुत्तीर्ण)", "Remark\n(Pass/\nFail)"), bgHeader);

        int rowIdx = 1;
        int totalObtained = 0;
        int maxTotal = 0;
        boolean hasFailed = false;

        // Academic Subjects
        for (Subject sub : academicSubs) {
            MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
            int obt = d != null ? d.grandTotal : 0;
            totalObtained += obt;
            maxTotal += sub.maxMarks > 0 ? sub.maxMarks : 50;
            
            int minPass = sub.maxMarks > 0 ? (int) Math.ceil(sub.maxMarks * 0.35) : 18;
            boolean passed = obt >= minPass;
            if (!passed) hasFailed = true;

            addTd(tbl, String.valueOf(rowIdx++), Element.ALIGN_CENTER);
            addTd(tbl, PdfLocalizer.translateSubject(ctx, sub.name), Element.ALIGN_LEFT);
            addTd(tbl, String.valueOf(minPass),  Element.ALIGN_CENTER);
            addTdBold(tbl, String.valueOf(obt),  Element.ALIGN_CENTER);
            addTd(tbl, "-",                      Element.ALIGN_CENTER);
            addTd(tbl, passed ? PdfLocalizer.get(ctx, "उत्तीर्ण", "Pass") : PdfLocalizer.get(ctx, "अनुत्तीर्ण", "Fail"), Element.ALIGN_CENTER);
        }

        // Non-Academic Subjects
        for (int i = 0; i < nonAcSubs.size(); i++) {
            Subject sub = nonAcSubs.get(i);
            MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
            String grade = d != null && d.grade != null && !d.grade.isEmpty() ? normalizeGrade(d.grade) : "-";

            addTd(tbl, String.valueOf(rowIdx++), Element.ALIGN_CENTER);
            addTd(tbl, PdfLocalizer.translateSubject(ctx, sub.name), Element.ALIGN_LEFT);
            addTd(tbl, PdfLocalizer.get(ctx, "श्रेणी", "Grade"),                 Element.ALIGN_CENTER);
            addTdBold(tbl, grade,                Element.ALIGN_CENTER);

            // Instead of rowspan which breaks pagination and overflows the box, merge cols 5 and 6 for this row
            PdfPCell empty = new PdfPCell(new Phrase(" ", fSmall));
            empty.setColspan(2);
            empty.setBorderColor(C_DARK);
            if (nonAcSubs.size() == 1) {
                empty.setBorder(Rectangle.BOX);
            } else if (i == 0) {
                empty.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.TOP);
            } else if (i == nonAcSubs.size() - 1) {
                empty.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
            } else {
                empty.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
            }
            tbl.addCell(empty);
        }

        tbl.setSpacingAfter(15);
        doc.add(tbl);

        // ── 4. Summary Row (Totals) ───────────────────────────────────────────
        PdfPTable sumTbl = new PdfPTable(2);
        sumTbl.setWidthPercentage(100);
        
        String percStr = maxTotal > 0 ? String.format(java.util.Locale.US, "%.1f", (totalObtained * 100.0f) / maxTotal) + "%" : "0.0%";
        String passResult = hasFailed ? PdfLocalizer.get(ctx, "अनुत्तीर्ण", "Fail") : PdfLocalizer.get(ctx, "उत्तीर्ण", "Pass");

        addNoBorderCell(sumTbl, PdfLocalizer.get(ctx, "एकूण गुण : ", "Total Marks: ") + totalObtained, fBold, Element.ALIGN_LEFT);
        addNoBorderCell(sumTbl, PdfLocalizer.get(ctx, "शेकडा गुण : ", "Percentage: ") + percStr, fBold, Element.ALIGN_RIGHT);
        
        // Date formatting: default 1 May of the next year, or from school settings
        String academicYear = cls != null && cls.academicYearLabel != null ? cls.academicYearLabel : "2025-26";
        String nextYear = "";
        if (academicYear.contains("-")) {
            String[] parts = academicYear.split("-");
            if (parts.length > 1) {
                String y2 = parts[1].trim();
                if (y2.length() == 2) {
                    nextYear = "20" + y2;
                } else {
                    nextYear = y2;
                }
            }
        }
        if (nextYear.isEmpty()) {
            nextYear = String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
        }
        String defaultDate = "01-05-" + nextYear;
        String dateStr = (school != null && school.resultDate != null && !school.resultDate.isEmpty()) ? school.resultDate : defaultDate;
        
        addNoBorderCell(sumTbl, PdfLocalizer.get(ctx, "निकाल दिनांक : ", "Result Date: ") + dateStr, fNormal, Element.ALIGN_LEFT);
        addNoBorderCell(sumTbl, PdfLocalizer.get(ctx, "शेरा : ", "Remark: ") + passResult, fBold, Element.ALIGN_RIGHT);

        sumTbl.setSpacingAfter(40);
        doc.add(sumTbl);

        // ── 5. Signatures ─────────────────────────────────────────────────────
        PdfPTable sigTbl = new PdfPTable(2);
        sigTbl.setWidthPercentage(100);

        String teacherName   = cls != null ? nvl(cls.teacherName) : "";
        String principalName = school != null ? nvl(school.principalName) : "";

        addNoBorderCell(sigTbl, PdfLocalizer.get(ctx, "वर्गशिक्षक स्वाक्षरी\nश्रीम . ", "Class Teacher Signature\n") + teacherName, fSmall, Element.ALIGN_CENTER);
        addNoBorderCell(sigTbl, PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी\nश्री ", "Headmaster Signature\n") + principalName, fSmall, Element.ALIGN_CENTER);

        doc.add(sigTbl);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void addHdrCell(PdfPTable tbl, String text, int align) {
        PdfPCell c = PdfGenerator.rawCell(text, fNormal, BaseColor.WHITE, C_DARK, align);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(4);
        tbl.addCell(c);
    }

    private static void addTh(PdfPTable tbl, String text, BaseColor bg) {
        PdfPCell c = PdfGenerator.rawCell(text, fSmallBold, bg, C_DARK, Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        c.setMinimumHeight(40);
        tbl.addCell(c);
    }

    private static void addTd(PdfPTable tbl, String text, int align) {
        PdfPCell c = PdfGenerator.rawCell(text, fSmall, BaseColor.WHITE, C_DARK, align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        c.setMinimumHeight(25);
        tbl.addCell(c);
    }

    private static void addTdBold(PdfPTable tbl, String text, int align) {
        PdfPCell c = PdfGenerator.rawCell(text, fSmallBold, BaseColor.WHITE, C_DARK, align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        c.setMinimumHeight(25);
        tbl.addCell(c);
    }

    private static void addNoBorderCell(PdfPTable tbl, String text, Font font, int align) {
        BaseColor tc = font.getColor() != null ? font.getColor() : C_DARK;
        PdfPCell c = PdfGenerator.rawCell(text, font, BaseColor.WHITE, tc, align);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingTop(5);
        tbl.addCell(c);
    }

    private static boolean isNonAcademic(String sub) {
        if (sub == null) return false;
        String s = sub.trim().toLowerCase();
        return s.contains("कला") || s.contains("कार्यानुभव") || s.contains("शा.शि.") || s.contains("शारीरिक")
            || s.contains("art") || s.contains("work experience") || s.contains("p.e.") || s.contains("physical education") || s.contains("craft");
    }
}
