package com.kartik.myschool.utils.pdf;

import android.content.Context;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
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
 * Option 15 — Continuous Comprehensive Evaluation (सातत्यपूर्ण सर्वंकष
 * मूल्यमापन)
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
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
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

        int selectedSemNum = (com.kartik.myschool.SessionContext.selectedSemester != null)
                ? com.kartik.myschool.SessionContext.selectedSemester.number
                : 0;

        // ── 1. Header ─────────────────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(3);
        hdr.setWidthPercentage(100);
        hdr.setWidths(new float[] { 1.5f, 2f, 1.5f });

        PdfPCell cL = new PdfPCell();
        cL.setBorder(Rectangle.NO_BORDER);
        String udiseLabel = PdfLocalizer.get(ctx, "युडायस क्रमांक: ", "UDISE: ");
        String udiseVal = nvl(school != null ? school.udiseCode : "");
        String sNameLabel = PdfLocalizer.get(ctx, "शाळेचे नाव: ", "School Name: ");
        String sName = nvl(school != null ? school.name : "");
        try {
            com.itextpdf.text.Image uImg = MarathiText.renderLine(udiseLabel + udiseVal, 9, true, android.graphics.Color.BLACK);
            uImg.setAlignment(Element.ALIGN_LEFT);
            cL.addElement(uImg);
            
            com.itextpdf.text.Image sImg = MarathiText.renderLine(sNameLabel + sName, 9, false, android.graphics.Color.BLACK);
            sImg.setAlignment(Element.ALIGN_LEFT);
            cL.addElement(sImg);
        } catch (Exception e) {
            cL.addElement(new Phrase(udiseLabel + udiseVal, fSmallBold));
            cL.addElement(new Phrase(sNameLabel + sName, fSmall));
        }
        hdr.addCell(cL);

        PdfPCell cC = new PdfPCell();
        cC.setBorder(Rectangle.NO_BORDER);
        cC.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            com.itextpdf.text.Image titleImg = MarathiText.renderLine(
                    PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"), 16,
                    true, android.graphics.Color.BLACK);
            titleImg.setAlignment(Element.ALIGN_CENTER);
            cC.addElement(titleImg);
            String termText = (selectedSemNum == 1) ? PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester")
                    : (selectedSemNum == 2) ? PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester")
                            : PdfLocalizer.get(ctx, "प्रथम व द्वितीय सत्र", "First & Second Semester");
            com.itextpdf.text.Image subImg = MarathiText.renderLine(termText, 10, false, android.graphics.Color.BLACK);
            subImg.setAlignment(Element.ALIGN_CENTER);
            cC.addElement(subImg);
        } catch (Exception e) {
            String termText = (selectedSemNum == 1) ? PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester")
                    : (selectedSemNum == 2) ? PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester")
                            : PdfLocalizer.get(ctx, "प्रथम व द्वितीय सत्र", "First & Second Semester");
            cC.addElement(new Phrase(
                    PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"),
                    fTitle));
            cC.addElement(new Phrase(termText, fSmall));
        }
        hdr.addCell(cC);

        PdfPCell cR = new PdfPCell();
        cR.setBorder(Rectangle.NO_BORDER);
        cR.setHorizontalAlignment(Element.ALIGN_RIGHT);
        String yearLabel = PdfLocalizer.get(ctx, "सन", "Year");
        String yearVal = " : " + nvl(cls != null ? cls.academicYearLabel : "");
        String cDivLabel1 = PdfLocalizer.get(ctx, "इयत्ता", "Class");
        String cDivVal1 = " : " + nvl(cls != null ? cls.className : "");
        String cDivLabel2 = PdfLocalizer.get(ctx, "तुकडी", "Division");
        String cDivVal2 = " : " + nvl(cls != null ? cls.division : "-");
        try {
            PdfPTable rightTbl = new PdfPTable(2);
            rightTbl.setWidthPercentage(100);
            rightTbl.setWidths(new float[]{1f, 1f});

            // Row 1: Year
            PdfPCell cellYearLabel = new PdfPCell();
            cellYearLabel.setBorder(Rectangle.NO_BORDER);
            cellYearLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            com.itextpdf.text.Image yImg = MarathiText.renderLine(yearLabel, 9, false, android.graphics.Color.BLACK);
            yImg.setAlignment(Element.ALIGN_RIGHT);
            cellYearLabel.addElement(yImg);
            rightTbl.addCell(cellYearLabel);

            PdfPCell cellYearVal = new PdfPCell(new Phrase(yearVal, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.BLACK)));
            cellYearVal.setBorder(Rectangle.NO_BORDER);
            cellYearVal.setHorizontalAlignment(Element.ALIGN_LEFT);
            rightTbl.addCell(cellYearVal);

            // Row 2: Class and Division
            PdfPCell cellClassLabel = new PdfPCell();
            cellClassLabel.setBorder(Rectangle.NO_BORDER);
            cellClassLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            com.itextpdf.text.Image dImg1 = MarathiText.renderLine(cDivLabel1, 9, false, android.graphics.Color.BLACK);
            dImg1.setAlignment(Element.ALIGN_RIGHT);
            cellClassLabel.addElement(dImg1);
            rightTbl.addCell(cellClassLabel);

            PdfPTable divTbl = new PdfPTable(3);
            divTbl.setWidthPercentage(100);
            divTbl.setWidths(new float[]{0.5f, 0.5f, 0.5f});
            
            PdfPCell c1 = new PdfPCell(new Phrase(cDivVal1 + ", ", new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.BLACK)));
            c1.setBorder(Rectangle.NO_BORDER);
            divTbl.addCell(c1);

            PdfPCell c2 = new PdfPCell();
            c2.setBorder(Rectangle.NO_BORDER);
            com.itextpdf.text.Image dImg2 = MarathiText.renderLine(cDivLabel2, 9, false, android.graphics.Color.BLACK);
            dImg2.setAlignment(Element.ALIGN_RIGHT);
            c2.addElement(dImg2);
            divTbl.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase(cDivVal2, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.BLACK)));
            c3.setBorder(Rectangle.NO_BORDER);
            divTbl.addCell(c3);

            PdfPCell cellClassValWrapper = new PdfPCell(divTbl);
            cellClassValWrapper.setBorder(Rectangle.NO_BORDER);
            cellClassValWrapper.setHorizontalAlignment(Element.ALIGN_LEFT);
            rightTbl.addCell(cellClassValWrapper);

            cR.addElement(rightTbl);
        } catch (Exception e) {
            Phrase p1 = new Phrase(yearLabel + yearVal, fSmallBold);
            p1.getFont().setColor(C_DARK);
            Paragraph pr1 = new Paragraph(p1);
            pr1.setAlignment(Element.ALIGN_RIGHT);
            cR.addElement(pr1);

            Phrase p2 = new Phrase(cDivLabel1 + cDivVal1 + ", " + cDivLabel2 + cDivVal2, fSmallBold);
            p2.getFont().setColor(C_DARK);
            Paragraph pr2 = new Paragraph(p2);
            pr2.setAlignment(Element.ALIGN_RIGHT);
            cR.addElement(pr2);
        }
        hdr.addCell(cR);

        hdr.setSpacingAfter(8);
        doc.add(hdr);

        List<Subject> originalSubs = cls != null && cls.subjects != null ? cls.subjects : new ArrayList<>();
        List<Subject> allSubs = new ArrayList<>();
        for (Subject sub : originalSubs) {
            String subNameLower = sub.name != null ? sub.name.toLowerCase() : "";
            if (subNameLower.contains("vishesh") || subNameLower.contains("aavad") || subNameLower.contains("sudharna") || subNameLower.contains("vyaktimatva") ||
                subNameLower.contains("विशेष") || subNameLower.contains("आवड") || subNameLower.contains("सुधारणा") || subNameLower.contains("व्यक्तिमत्व")) {
                continue; // Skip pseudo-subjects
            }
            allSubs.add(sub);
        }
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
        MarathiText.cell(tbl, PdfLocalizer.get(ctx, "अ.नं", "Sr.No."), 9, true, C_HEADER_BG, C_DARK, 1, 2,
                Element.ALIGN_CENTER);
        MarathiText.cell(tbl, PdfLocalizer.get(ctx, "विद्यार्थ्याचे नाव", "Student Name"), 9, true, C_HEADER_BG, C_DARK,
                1, 2, Element.ALIGN_CENTER);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "उपस्थिती", "Attendance"), fSmallBold,
                C_HEADER_BG, C_DARK, 1, 2);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "सत्र", "Term"), fSmallBold, C_HEADER_BG,
                C_DARK, 1, 2);

        for (Subject sub : allSubs) {
            MarathiText.cell(tbl, PdfLocalizer.translateSubject(ctx, sub.name), 9, true, C_HEADER_BG, C_DARK, 2, 1,
                    Element.ALIGN_CENTER);
        }

        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "एकूण", "Total"), fSmallBold, C_HEADER_BG,
                C_DARK, 1, 2);
        MarathiText.cell(tbl, PdfLocalizer.get(ctx, "शेकडा गुण", "Percentage"), 9, true, C_HEADER_BG, C_DARK, 1, 2,
                Element.ALIGN_CENTER);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "श्रेणी", "Grade"), fSmallBold,
                C_HEADER_BG, C_DARK, 1, 2);

        // ── 4. Table Headers (Row 2) ──────────────────────────────────────────
        for (Subject sub : allSubs) {
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "गुण", "Marks"), fSmallBold,
                    C_HEADER_BG, C_DARK, 1, 1);
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "श्रेणी", "Grade"), fSmallBold,
                    C_HEADER_BG, C_DARK, 1, 1);
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

                // Calculate total attendance from student's monthly attendance
                int presentDays = 0;
                if (student.monthlyAttendance != null) {
                    for (String m : student.monthlyAttendance.keySet()) {
                        String att = student.monthlyAttendance.get(m);
                        if (att != null && att.contains("/")) {
                            try {
                                presentDays += Integer.parseInt(att.split("/")[0].trim());
                            } catch (Exception ignored) {
                            }
                        } else if (att != null && !att.isEmpty()) {
                            try {
                                presentDays += Integer.parseInt(att.trim());
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                // Also use presentDays from the marks record if student-level attendance is
                // missing
                if (presentDays == 0 && s1 != null && s1.presentDays > 0)
                    presentDays = s1.presentDays;
                if (presentDays == 0 && s2 != null && s2.presentDays > 0)
                    presentDays = s2.presentDays;
                String attStr = presentDays > 0 ? String.valueOf(presentDays) : "";

                int rowspan = (selectedSemNum == 0) ? 2 : 1;

                // ROW 1: Sem 1 (only if selectedSemNum is 0/both or 1)
                if (selectedSemNum == 0 || selectedSemNum == 1) {
                    PdfPCell cSr = noPadCell(String.valueOf(sr++), bg);
                    cSr.setRowspan(rowspan);
                    tbl.addCell(cSr);

                    PdfPCell cName = noPadCellMultiLine(nvl(student.name), bg, 90f);
                    cName.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cName.setPaddingLeft(0);
                    cName.setRowspan(rowspan);
                    tbl.addCell(cName);

                    PdfPCell cAtt = noPadCell(attStr, bg);
                    cAtt.setRowspan(rowspan);
                    tbl.addCell(cAtt);

                    tbl.addCell(noPadCell("I", bg));

                    for (Subject sub : allSubs) {
                        MarksRecord.SubjectMarksDetail d = getSubjectDetail(s1, sub.name);
                        String mStr = getMarksStr(d, s1, sub.name);
                        String gStr = d != null ? nvl(d.grade) : "";
                        tbl.addCell(noPadCell(mStr, bg));
                        tbl.addCell(noPadCellBold(gStr, bg));
                    }

                    String s1Total = s1 != null && s1.totalObtained > 0 ? String.valueOf((int) s1.totalObtained) : "";
                    String s1Perc = s1 != null && s1.percentage > 0
                            ? String.format(java.util.Locale.US, "%.1f", s1.percentage)
                            : "";
                    String s1Grade = s1 != null ? nvl(s1.grade) : "";
                    tbl.addCell(noPadCellBold(s1Total, bg));
                    tbl.addCell(noPadCellBold(s1Perc, bg));
                    tbl.addCell(noPadCellBold(s1Grade, bg));
                }

                // ROW 2: Sem 2 (only if selectedSemNum is 0/both or 2)
                if (selectedSemNum == 0 || selectedSemNum == 2) {
                    if (selectedSemNum == 2) {
                        // Only Sem 2 selected: add Sr, Name and Attendance columns for this row
                        PdfPCell cSr = noPadCell(String.valueOf(sr++), bg);
                        cSr.setRowspan(rowspan);
                        tbl.addCell(cSr);

                        PdfPCell cName = noPadCellMultiLine(nvl(student.name), bg, 90f);
                        cName.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cName.setPaddingLeft(0);
                        cName.setRowspan(rowspan);
                        tbl.addCell(cName);

                        PdfPCell cAtt = noPadCell(attStr, bg);
                        cAtt.setRowspan(rowspan);
                        tbl.addCell(cAtt);
                    }

                    tbl.addCell(noPadCell("II", bg));

                    for (Subject sub : allSubs) {
                        MarksRecord.SubjectMarksDetail d = getSubjectDetail(s2, sub.name);
                        String mStr = getMarksStr(d, s2, sub.name);
                        String gStr = d != null ? nvl(d.grade) : "";
                        tbl.addCell(noPadCell(mStr, bg));
                        tbl.addCell(noPadCellBold(gStr, bg));
                    }

                    String s2Total = s2 != null && s2.totalObtained > 0 ? String.valueOf((int) s2.totalObtained) : "";
                    String s2Perc = s2 != null && s2.percentage > 0
                            ? String.format(java.util.Locale.US, "%.1f", s2.percentage)
                            : "";
                    String s2Grade = s2 != null ? nvl(s2.grade) : "";
                    tbl.addCell(noPadCellBold(s2Total, bg));
                    tbl.addCell(noPadCellBold(s2Perc, bg));
                    tbl.addCell(noPadCellBold(s2Grade, bg));
                }
            }
        }

        doc.add(tbl);
    }

    /**
     * Looks up the SubjectMarksDetail for a subject name, trying:
     * 1. Sanitized key (MarksRecord.sanitizeKey(name))
     * 2. Raw name as-is
     * 3. Lowercase raw name
     */
    private static MarksRecord.SubjectMarksDetail getSubjectDetail(MarksRecord rec, String subName) {
        if (rec == null || rec.detailedMarks == null || subName == null)
            return null;
        // Try sanitized key first
        String sanitizedKey = MarksRecord.sanitizeKey(subName);
        MarksRecord.SubjectMarksDetail d = rec.detailedMarks.get(sanitizedKey);
        if (d != null)
            return d;
        // Try raw name
        d = rec.detailedMarks.get(subName);
        if (d != null)
            return d;
        // Try trimmed name
        d = rec.detailedMarks.get(subName.trim());
        if (d != null)
            return d;
        // Try sanitized lowercase
        d = rec.detailedMarks.get(sanitizedKey.toLowerCase());
        if (d != null)
            return d;
        return null;
    }

    /**
     * Gets marks string for a subject: uses grandTotal from detail,
     * falls back to subjectMarks map if detail is missing or grandTotal==0.
     */
    private static String getMarksStr(MarksRecord.SubjectMarksDetail d, MarksRecord rec, String subName) {
        // If we have a valid detail with grandTotal > 0, use it
        if (d != null && d.grandTotal > 0)
            return String.valueOf(d.grandTotal);
        // If detail exists but grandTotal == 0, try akarikTotal + sanklit
        if (d != null && (d.akarikTotal > 0 || d.sanklit > 0)) {
            int total = d.akarikTotal + d.sanklit;
            return total > 0 ? String.valueOf(total) : "";
        }
        // Fallback to subjectMarks map
        if (rec != null && rec.subjectMarks != null) {
            String sanitizedKey = MarksRecord.sanitizeKey(subName);
            Double val = rec.subjectMarks.get(sanitizedKey);
            if (val == null)
                val = rec.subjectMarks.get(subName);
            if (val != null && val > 0)
                return String.valueOf(val.intValue());
        }
        return "";
    }

    private static PdfPCell noPadCellMultiLine(String text, BaseColor bg, float widthPt) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(C_DARK);
        c.setBorderWidth(0.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingTop(2f);
        c.setPaddingBottom(4f);
        c.setPaddingLeft(2f);
        c.setPaddingRight(2f);

        if (text != null && text.matches("^[A-Za-z0-9\\s\\-\\.,/\\:\\+\\(\\)]*$")) {
            c.setPhrase(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK)));
        } else {
            try {
                com.itextpdf.text.Image img = MarathiText.renderMultiLine(text, 9, false, android.graphics.Color.BLACK, widthPt);
                img.setAlignment(Element.ALIGN_CENTER);
                c.addElement(img);
            } catch (Exception e) {
                c.setPhrase(new Phrase(text, fSmall));
            }
        }
        return c;
    }

    private static PdfPCell noPadCell(String text, BaseColor bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(C_DARK);
        c.setBorderWidth(0.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingTop(3);
        c.setPaddingBottom(3);
        if (text != null && text.matches("^[A-Za-z0-9\\s\\-\\.,/\\:\\+\\(\\)]*$")) {
            c.setPhrase(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK)));
        } else {
            try {
                com.itextpdf.text.Image img = MarathiText.renderLine(text, 9, false, android.graphics.Color.BLACK);
                c.addElement(img);
            } catch (Exception e) {
                c.setPhrase(new Phrase(text, fSmall));
            }
        }
        return c;
    }

    private static PdfPCell noPadCellBold(String text, BaseColor bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(C_DARK);
        c.setBorderWidth(0.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingTop(3);
        c.setPaddingBottom(3);
        if (text != null && text.matches("^[A-Za-z0-9\\s\\-\\.,/\\:\\+\\(\\)]*$")) {
            c.setPhrase(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.BLACK)));
        } else {
            try {
                com.itextpdf.text.Image img = MarathiText.renderLine(text, 9, true, android.graphics.Color.BLACK);
                c.addElement(img);
            } catch (Exception e) {
                c.setPhrase(new Phrase(text, fSmallBold));
            }
        }
        return c;
    }
}
