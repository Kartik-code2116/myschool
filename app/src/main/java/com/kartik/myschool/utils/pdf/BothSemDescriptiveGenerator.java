package com.kartik.myschool.utils.pdf;

import android.content.Context;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
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
import java.util.List;
import java.util.Map;

import static com.kartik.myschool.utils.PdfGenerator.*;

/**
 * Option 10 — Both-Semester Descriptive Remarks Sheet
 *
 * Layout: Landscape A4, one page per student.
 * Two equal half-page panels side-by-side:
 * LEFT → ● प्रथम सत्र ●
 * RIGHT → ● द्वितीय सत्र ●
 *
 * Each panel contains:
 * • Title: "● प्रथम/द्वितीय सत्र ●"
 * • Header: नाव | सन | इयत्ता + तुकडी | हजेरी क्रमांक | UDISE
 * • Table cols: अ.नं | विषय | श्रेणी | वर्णनात्मक नोंदी
 * • Signatures: शिक्षक स्वाक्षरी | मुख्याध्यापक स्वाक्षरी
 */
public class BothSemDescriptiveGenerator {

    // ── Remark helpers (mirroring DescriptiveRemarksGenerator) ────────────────

    private static String findRemark(MarksRecord rec, String subjectName) {
        if (rec == null || rec.detailedMarks == null || subjectName == null)
            return "";
        MarksRecord.SubjectMarksDetail d = detail(rec, subjectName);
        if (d != null && d.remark != null && !d.remark.trim().isEmpty())
            return cleanRemark(d.remark);
        // loose match
        String safeName = MarksRecord.sanitizeKey(subjectName).toLowerCase();
        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> e : rec.detailedMarks.entrySet()) {
            String k = e.getKey();
            MarksRecord.SubjectMarksDetail v = e.getValue();
            if (v != null && v.remark != null && !v.remark.trim().isEmpty()) {
                String safeK = k != null ? MarksRecord.sanitizeKey(k).toLowerCase() : "";
                if (safeK.equals(safeName) || safeK.contains(safeName) || safeName.contains(safeK))
                    return cleanRemark(v.remark);
            }
        }
        return "";
    }

    private static String findGrade(MarksRecord rec, String subjectName) {
        if (rec == null || subjectName == null)
            return "-";
        MarksRecord.SubjectMarksDetail d = detail(rec, subjectName);
        if (d != null && d.grade != null && !d.grade.trim().isEmpty())
            return normalizeGrade(d.grade);
        return "-";
    }

    private static String cleanRemark(String raw) {
        return raw == null ? "" : raw.replace("||", ", ").trim();
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void generateBothSemDescriptive(Context ctx,
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
                        "BothSemDescriptive_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(10, 10, 14, 14);

                int selectedSemNum = (com.kartik.myschool.SessionContext.selectedSemester != null)
                        ? com.kartik.myschool.SessionContext.selectedSemester.number
                        : 0;
                boolean isFirst = true;
                if (students != null) {
                    for (Student student : students) {
                        if (!isFirst)
                            doc.newPage();
                        isFirst = false;
                        MarksRecord s1 = (selectedSemNum == 0 || selectedSemNum == 1) && sem1Map != null
                                ? sem1Map.get(student.id)
                                : null;
                        MarksRecord s2 = (selectedSemNum == 0 || selectedSemNum == 2) && sem2Map != null
                                ? sem2Map.get(student.id)
                                : null;

                        addStudentPage(doc, ctx, school, cls, student, s1, s2);
                    }
                }
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    // ── Per-student page ──────────────────────────────────────────────────────

    private static void addStudentPage(Document doc, Context ctx, School school, ClassModel cls,
            Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {

        // Outer 2-column wrapper: left panel | right panel
        PdfPTable outer = new PdfPTable(new float[] { 1f, 1f });
        outer.setWidthPercentage(100);
        outer.setExtendLastRow(true);

        // Shared divider color (teal like screenshot)
        BaseColor C_TEAL = new BaseColor(0, 151, 167);

        // ── LEFT: प्रथम सत्र ────────────────────────────────────────────────
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(8);
        leftCell.setVerticalAlignment(Element.ALIGN_TOP);
        buildSemPanel(leftCell, ctx, school, cls, student, sem1,
                PdfLocalizer.get(ctx, "● प्रथम सत्र ●", "● First Semester ●"));

        // ── RIGHT: द्वितीय सत्र ─────────────────────────────────────────────
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(8);
        rightCell.setVerticalAlignment(Element.ALIGN_TOP);
        buildSemPanel(rightCell, ctx, school, cls, student, sem2,
                PdfLocalizer.get(ctx, "● द्वितीय सत्र ●", "● Second Semester ●"));

        outer.addCell(leftCell);
        outer.addCell(rightCell);
        doc.add(outer);
    }

    // ── Single-semester panel builder ─────────────────────────────────────────

    private static void buildSemPanel(PdfPCell panel, Context ctx, School school, ClassModel cls,
            Student student, MarksRecord rec, String semTitle) throws Exception {

        BaseColor C_HDR = new BaseColor(0, 151, 167); // teal header

        // ── 1. Semester title ─────────────────────────────────────────────────
        PdfPTable titleTbl = new PdfPTable(1);
        titleTbl.setWidthPercentage(100);
        PdfPCell titleCell = rawCell(semTitle, fSmallBold, C_WHITE, C_HDR, Element.ALIGN_CENTER);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(4);
        titleTbl.addCell(titleCell);
        panel.addElement(titleTbl);

        // Spacing
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 3)));

        // ── 2. Student header 3-col table ─────────────────────────────────────
        PdfPTable hdr = new PdfPTable(new float[] { 2f, 1f, 1.4f });
        hdr.setWidthPercentage(100);

        String nameText = PdfLocalizer.get(ctx, "नाव: ", "Name: ") + nvl(student != null ? student.name : null);
        String yearText = PdfLocalizer.get(ctx, "सन: ", "Year: ") + nvl(cls != null ? cls.academicYearLabel : null);
        String clasDiv = PdfLocalizer.get(ctx, "इयत्ता: ", "Class: ") + nvl(cls != null ? cls.className : null)
                + PdfLocalizer.get(ctx, ", तुकडी: ", ", Division: ") + nvl(cls != null ? cls.division : null);
        String rollText = PdfLocalizer.get(ctx, "हजेरी क्रमांक: ", "Roll No: ")
                + nvl(student != null ? student.rollNo : null);
        String udiseText = PdfLocalizer.get(ctx, "युडायस: ", "UDISE: ") + nvl(school != null ? school.udiseCode : null);

        // Row 1: नाव | (empty) | सन
        addHdrCell(hdr, nameText, fSmallBold, Element.ALIGN_LEFT);
        addHdrCell(hdr, "", fSmall, Element.ALIGN_CENTER);
        addHdrCell(hdr, yearText, fSmallBold, Element.ALIGN_RIGHT);

        // Row 2: इयत्ता+तुकडी | हजेरी क्रमांक | UDISE
        addHdrCell(hdr, clasDiv, fSmall, Element.ALIGN_LEFT);
        addHdrCell(hdr, rollText, fSmall, Element.ALIGN_CENTER);
        addHdrCell(hdr, udiseText, fSmall, Element.ALIGN_RIGHT);

        hdr.setSpacingAfter(4);
        panel.addElement(hdr);

        // ── 3. Subject marks table ────────────────────────────────────────────
        // Cols: अ.नं | विषय | श्रेणी | वर्णनात्मक नोंदी
        float[] colWidths = { 0.35f, 1.1f, 0.7f, 3.5f };
        PdfPTable tbl = new PdfPTable(colWidths);
        tbl.setWidthPercentage(100);

        // Header
        addTableHdr(tbl, PdfLocalizer.get(ctx, "अ.नं", "Sr.No."), C_HDR);
        addTableHdr(tbl, PdfLocalizer.get(ctx, "विषय", "Subject"), C_HDR);
        addTableHdr(tbl, PdfLocalizer.get(ctx, "श्रेणी", "Grade"), C_HDR);
        addTableHdr(tbl, PdfLocalizer.get(ctx, "वर्णनात्मक नोंदी", "Descriptive Remarks"), C_HDR);

        List<Subject> subjects = (cls != null && cls.subjects != null) ? cls.subjects : new java.util.ArrayList<>();
        boolean alt = false;
        int rowIdx = 1;

        for (Subject sub : subjects) {
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE;
            alt = !alt;
            String grade = findGrade(rec, sub.name);
            String remark = findRemark(rec, sub.name);

            addDataCell(tbl, String.valueOf(rowIdx++), fSmallBold, bg, Element.ALIGN_CENTER, 28f);
            addDataCell(tbl, PdfLocalizer.translateSubject(ctx, sub.name), fSmall, bg, Element.ALIGN_LEFT, 28f);
            addDataCell(tbl, grade, fSmallBold, bg, Element.ALIGN_CENTER, 28f);
            addRemarkCell(tbl, remark, bg, 28f);
        }

        panel.addElement(tbl);

        // ── 4. Signature row ─────────────────────────────────────────────────
        panel.addElement(new Phrase(" ", new Font(Font.FontFamily.HELVETICA, 14)));
        PdfPTable sigTbl = new PdfPTable(new float[] { 1f, 1f });
        sigTbl.setWidthPercentage(100);

        String teacherName = (cls != null && cls.teacherName != null) ? cls.teacherName : "";
        String principalName = (school != null && school.principalName != null) ? school.principalName : "";

        PdfPCell s1 = rawCell(PdfLocalizer.get(ctx, "शिक्षक स्वाक्षरी\n", "Teacher Signature\n") + teacherName,
                fSmall, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s1.setBorder(Rectangle.NO_BORDER);
        PdfPCell s2 = rawCell(
                PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी\n", "Headmaster Signature\n") + principalName,
                fSmall, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s2.setBorder(Rectangle.NO_BORDER);

        sigTbl.addCell(s1);
        sigTbl.addCell(s2);
        panel.addElement(sigTbl);
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private static void addHdrCell(PdfPTable tbl, String text, Font font, int align) {
        PdfPCell c = rawCell(text, font, BaseColor.WHITE, C_DARK, align);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(2);
        tbl.addCell(c);
    }

    private static void addTableHdr(PdfPTable tbl, String text, BaseColor bg) {
        PdfPCell c = rawCell(text, fSmallBold, bg, C_WHITE, Element.ALIGN_CENTER);
        c.setPadding(4);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tbl.addCell(c);
    }

    private static void addDataCell(PdfPTable tbl, String text, Font font,
            BaseColor bg, int align, float minH) {
        PdfPCell c = rawCell(text != null ? text : "", font, bg, C_DARK, align);
        c.setPadding(4);
        c.setMinimumHeight(minH);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tbl.addCell(c);
    }

    /**
     * Remark cell: parses "Title : body" format to show title bold + body normal.
     * Supports delimiters ":", ":\n", or just plain text.
     */
    private static void addRemarkCell(PdfPTable tbl, String remark, BaseColor bg, float minH) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setMinimumHeight(minH);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (remark == null || remark.trim().isEmpty()) {
            c.addElement(new Phrase(" ", fSmall));
        } else {
            // Try to split on first ":" to make the label bold
            int colonIdx = remark.indexOf(':');
            if (colonIdx > 0 && colonIdx < remark.length() - 1) {
                String labelPart = remark.substring(0, colonIdx + 1).trim();
                String bodyPart = remark.substring(colonIdx + 1).trim();

                com.itextpdf.text.Paragraph para = new com.itextpdf.text.Paragraph();
                para.add(new Phrase(labelPart + "\n", fSmallBold));
                if (!bodyPart.isEmpty())
                    para.add(new Phrase(bodyPart, fSmall));
                c.addElement(para);
            } else {
                c.addElement(new Phrase(remark, fSmall));
            }
        }
        tbl.addCell(c);
    }
}
