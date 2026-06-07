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
            Rectangle rect = document.getPageSize();
            canvas.setColorStroke(BaseColor.BLACK);
            canvas.setLineWidth(2f);
            // x, y, width, height, radius
            canvas.roundRectangle(20, 20, rect.getWidth() - 40, rect.getHeight() - 40, 15);
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

                Document doc = new Document(PageSize.A4);
                PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                writer.setPageEvent(new RoundedBorderEvent());
                doc.open();
                doc.setMargins(40, 40, 50, 50);

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
        
        PdfPCell c1 = new PdfPCell(new Phrase("● गुणपत्रक ●", pinkFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleTbl.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase("वार्षिक परीक्षा", fNormal));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        c2.setPaddingBottom(15);
        titleTbl.addCell(c2);

        doc.add(titleTbl);

        // ── 2. Header Info ────────────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(new float[]{3f, 1.5f});
        hdr.setWidthPercentage(100);

        String schoolName = "शाळेचे नाव : " + nvl(school != null ? school.name : "");
        String udiseText  = "Udise: " + nvl(school != null ? school.udiseCode : "");
        String stuName    = "नाव : " + nvl(student != null ? student.name : "");
        String yearText   = "सन : " + nvl(cls != null ? cls.academicYearLabel : "");
        String rollText   = "हजेरी क्रमांक : " + nvl(student != null ? student.rollNo : "");
        String clsDiv     = "इयत्ता: " + nvl(cls != null ? cls.className : "") + ", तुकडी: " + nvl(cls != null ? cls.division : "");

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

        addTh(tbl, "अ.नं.", bgHeader);
        addTh(tbl, "विषय", bgHeader);
        addTh(tbl, "किमान\nआवश्यक गुण", bgHeader);
        
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
        addTh(tbl, "एकूण गुण\n(" + acaMax + " पैकी)\nप्राप्त", bgHeader);
        addTh(tbl, "सवलतीचे\nगुण", bgHeader);
        addTh(tbl, "शेरा\n(उत्तीर्ण/\nअनुत्तीर्ण)", bgHeader);

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
            addTd(tbl, nvl(sub.name),            Element.ALIGN_LEFT);
            addTd(tbl, String.valueOf(minPass),  Element.ALIGN_CENTER);
            addTdBold(tbl, String.valueOf(obt),  Element.ALIGN_CENTER);
            addTd(tbl, "-",                      Element.ALIGN_CENTER);
            addTd(tbl, passed ? "उत्तीर्ण" : "अनुत्तीर्ण", Element.ALIGN_CENTER);
        }

        // Non-Academic Subjects
        for (int i = 0; i < nonAcSubs.size(); i++) {
            Subject sub = nonAcSubs.get(i);
            MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
            String grade = d != null && d.grade != null && !d.grade.isEmpty() ? normalizeGrade(d.grade) : "-";

            addTd(tbl, String.valueOf(rowIdx++), Element.ALIGN_CENTER);
            addTd(tbl, nvl(sub.name),            Element.ALIGN_LEFT);
            addTd(tbl, "श्रेणी",                 Element.ALIGN_CENTER);
            addTdBold(tbl, grade,                Element.ALIGN_CENTER);

            // For the first non-academic subject, add the merged cell for cols 5 and 6
            if (i == 0) {
                PdfPCell mergedCell = new PdfPCell(new Phrase(" ", fSmall));
                mergedCell.setColspan(2);
                mergedCell.setRowspan(nonAcSubs.size());
                mergedCell.setBorder(Rectangle.BOX);
                mergedCell.setBorderColor(C_DARK);
                tbl.addCell(mergedCell);
            }
        }

        tbl.setSpacingAfter(15);
        doc.add(tbl);

        // ── 4. Summary Row (Totals) ───────────────────────────────────────────
        PdfPTable sumTbl = new PdfPTable(2);
        sumTbl.setWidthPercentage(100);
        
        String percStr = maxTotal > 0 ? String.format(java.util.Locale.US, "%.1f", (totalObtained * 100.0f) / maxTotal) + "%" : "0.0%";
        String passResult = hasFailed ? "अनुत्तीर्ण" : "उत्तीर्ण";

        addNoBorderCell(sumTbl, "एकूण गुण : " + totalObtained, fBold, Element.ALIGN_LEFT);
        addNoBorderCell(sumTbl, "शेकडा गुण : " + percStr, fBold, Element.ALIGN_RIGHT);
        
        // Date formatting: today's date or static if preferred
        String dateStr = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        addNoBorderCell(sumTbl, "निकाल दिनांक : " + dateStr, fNormal, Element.ALIGN_LEFT);
        addNoBorderCell(sumTbl, "शेरा : " + passResult, fBold, Element.ALIGN_RIGHT);

        sumTbl.setSpacingAfter(40);
        doc.add(sumTbl);

        // ── 5. Signatures ─────────────────────────────────────────────────────
        PdfPTable sigTbl = new PdfPTable(2);
        sigTbl.setWidthPercentage(100);

        String teacherName   = cls != null ? nvl(cls.teacherName) : "";
        String principalName = school != null ? nvl(school.principalName) : "";

        addNoBorderCell(sigTbl, "वर्गशिक्षक स्वाक्षरी\nश्रीम . " + teacherName, fSmall, Element.ALIGN_CENTER);
        addNoBorderCell(sigTbl, "मुख्याध्यापक स्वाक्षरी\nश्री " + principalName, fSmall, Element.ALIGN_CENTER);

        doc.add(sigTbl);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void addHdrCell(PdfPTable tbl, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, fNormal));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPaddingBottom(4);
        tbl.addCell(c);
    }

    private static void addTh(PdfPTable tbl, String text, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, fSmallBold));
        c.setBackgroundColor(bg);
        c.setBorderColor(C_DARK);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        c.setMinimumHeight(40);
        tbl.addCell(c);
    }

    private static void addTd(PdfPTable tbl, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, fSmall));
        c.setBorderColor(C_DARK);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        c.setMinimumHeight(25);
        tbl.addCell(c);
    }

    private static void addTdBold(PdfPTable tbl, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, fSmallBold));
        c.setBorderColor(C_DARK);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        c.setMinimumHeight(25);
        tbl.addCell(c);
    }

    private static void addNoBorderCell(PdfPTable tbl, String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPaddingTop(5);
        tbl.addCell(c);
    }

    private static boolean isNonAcademic(String sub) {
        if (sub == null) return false;
        String s = sub.trim();
        return s.contains("कला") || s.contains("कार्यानुभव") || s.contains("शा.शि.") || s.contains("शारीरिक");
    }
}
