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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.kartik.myschool.utils.PdfGenerator.*;

/**
 * Generates the Roster Grade Table PDF (Option 7).
 * Landscape A4, Marathi headers, per-subject boys/girls count per grade (अ-1 …
 * इ-2),
 * a bottom summary table (पट / उपस्थिती) and signature row.
 */
public class RosterGradeTableGenerator {

    /** Ordered grade keys exactly as shown on the screenshot. */
    private static final String[] GRADES = { "A-1", "A-2", "B-1", "B-2", "C-1", "C-2", "D", "E-1", "E-2" };

    /** Marathi labels for the nine grades (अ-1 … इ-2). */
    private static final String[] GRADE_LABELS = { "अ-1", "अ-2", "ब-1", "ब-2", "क-1", "क-2", "ड", "इ-1", "इ-2" };

    // ─────────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates the Roster Grade Table PDF.
     *
     * @param ctx      Android context
     * @param school   Selected school
     * @param cls      Selected class (must have subjects list)
     * @param students Ordered list of students
     * @param marksMap student-id → MarksRecord (for one semester)
     * @param isSem2   true = द्वितीय सत्र, false = प्रथम सत्र
     * @param cb       Success / error callback
     */
    public static void generateRosterGradeTable(Context ctx,
            School school,
            ClassModel cls,
            List<Student> students,
            Map<String, MarksRecord> marksMap,
            boolean isSem2,
            PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx),
                        "RosterGradeTable_" + (isSem2 ? "Sem2" : "Sem1") + "_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4.rotate()); // Landscape
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(15, 15, 20, 20);

                addRosterContent(doc, ctx, school, cls, students, marksMap, isSem2);

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main page builder
    // ─────────────────────────────────────────────────────────────────────────

    private static String getGradeLabel(Context ctx, int gi) {
        if (PdfLocalizer.isEnglish(ctx)) {
            return GRADES[gi];
        }
        return GRADE_LABELS[gi];
    }

    public static void addRosterContent(Document doc, Context ctx, School school, ClassModel cls,
            List<Student> students, Map<String, MarksRecord> marksMap,
            boolean isSem2) throws Exception {

        List<Subject> subjects = (cls != null && cls.subjects != null) ? cls.subjects : new ArrayList<>();

        // ── 1. Main title ────────────────────────────────────────────────────
        PdfGenerator.addMarathiParagraph(doc,
                PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"),
                14, true, C_DARK, 0, 6);

        // ── 2. Meta header: UDISE | Semester | Year / Class ──────────────────
        PdfPTable hdr = new PdfPTable(3);
        hdr.setWidthPercentage(100);
        hdr.setSpacingAfter(5);

        String udiseLine = PdfLocalizer.get(ctx, "युडायस: ", "UDISE: ") + nvl(school != null ? school.udiseCode : null);
        String schoolLine = PdfLocalizer.get(ctx, "शाळा: ", "School: ") + nvl(school != null ? school.name : null);
        String semText = isSem2
                ? PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester")
                : PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester");
        String rightText = PdfLocalizer.get(ctx, "सन: ", "Year: ") + nvl(cls != null ? cls.academicYearLabel : null)
                + PdfLocalizer.get(ctx, "\nइयत्ता: ", "\nClass: ") + nvl(cls != null ? cls.className : null)
                + PdfLocalizer.get(ctx, ", तुकडी: ", ", Division: ") + nvl(cls != null ? cls.division : null);

        // Use MarathiText for correct Devanagari rendering in header
        PdfPCell hL = new PdfPCell();
        hL.setBorder(Rectangle.NO_BORDER);
        try {
            com.itextpdf.text.Image uImg = com.kartik.myschool.utils.pdf.MarathiText.renderLine(udiseLine, 9, true, android.graphics.Color.BLACK);
            uImg.setAlignment(Element.ALIGN_LEFT);
            hL.addElement(uImg);
            com.itextpdf.text.Image sImg = com.kartik.myschool.utils.pdf.MarathiText.renderLine(schoolLine, 9, true, android.graphics.Color.BLACK);
            sImg.setAlignment(Element.ALIGN_LEFT);
            hL.addElement(sImg);
        } catch (Exception e) {
            hL.addElement(new Phrase(udiseLine + "\n" + schoolLine, fSmallBold));
        }

        PdfPCell hC = new PdfPCell();
        hC.setBorder(Rectangle.NO_BORDER);
        hC.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            com.itextpdf.text.Image semImg = com.kartik.myschool.utils.pdf.MarathiText.renderLine(semText, 11, true,
                    android.graphics.Color.BLACK);
            semImg.setAlignment(Element.ALIGN_CENTER);
            hC.addElement(semImg);
        } catch (Exception e) {
            hC.addElement(new Phrase(semText, fSmallBold));
        }

        PdfPCell hR = new PdfPCell();
        hR.setBorder(Rectangle.NO_BORDER);
        hR.setHorizontalAlignment(Element.ALIGN_RIGHT);
        try {
            com.itextpdf.text.Image rImg = com.kartik.myschool.utils.pdf.MarathiText
                    .renderLine(rightText.replace("\n", " | "), 9, true, android.graphics.Color.BLACK);
            rImg.setAlignment(Element.ALIGN_RIGHT);
            hR.addElement(rImg);
        } catch (Exception e) {
            hR.addElement(new Phrase(rightText, fSmallBold));
        }

        hdr.addCell(hL);
        hdr.addCell(hC);
        hdr.addCell(hR);
        doc.add(hdr);

        // ── 3. Compute grade distribution ────────────────────────────────────
        // gradeData[subjectIndex][gradeIndex][0] = boys, [1] = girls
        int numSubs = subjects.size();
        int[][][] gradeData = new int[numSubs][GRADES.length][2];

        // Attendance / patt totals
        int totalBoys = 0, totalGirls = 0;
        int patBoys = 0, patGirls = 0; // enrolled (पट)
        int attBoys = 0, attGirls = 0; // present (उपस्थिती)

        if (students != null) {
            for (Student st : students) {
                boolean isFemale = st.gender != null
                        && (st.gender.equalsIgnoreCase("Female")
                                || st.gender.equalsIgnoreCase("मुलगी")
                                || st.gender.equalsIgnoreCase("स्त्री"));

                if (isFemale)
                    totalGirls++;
                else
                    totalBoys++;

                MarksRecord rec = marksMap != null ? marksMap.get(st.id) : null;

                // Patt always 1 per enrolled student
                if (isFemale)
                    patGirls++;
                else
                    patBoys++;

                // Attendance from marksRecord if available
                if (rec != null && rec.presentDays > 0) {
                    if (isFemale)
                        attGirls += rec.presentDays;
                    else
                        attBoys += rec.presentDays;
                }

                // Per-subject grade counts
                for (int si = 0; si < numSubs; si++) {
                    Subject sub = subjects.get(si);
                    MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
                    if (d != null && d.grade != null) {
                        String normG = PdfGenerator.normalizeGrade(d.grade);
                        for (int gi = 0; gi < GRADES.length; gi++) {
                            if (GRADES[gi].equalsIgnoreCase(normG)) {
                                gradeData[si][gi][isFemale ? 1 : 0]++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // ── 4. Main data table ───────────────────────────────────────────────
        // Columns: अ.नं | विषय | (अ-1 मुले/मुली) × 9 grades | एकूण मुले/मुली
        // = 2 + 9*2 + 2 = 22 columns
        int numGrades = GRADES.length;
        int totalCols = 2 + (numGrades * 2) + 2; // 22

        float[] widths = new float[totalCols];
        widths[0] = 0.35f; // अ.नं
        widths[1] = 1.1f; // विषय
        for (int i = 2; i < 2 + numGrades * 2; i++)
            widths[i] = 0.35f; // grade cols
        widths[totalCols - 2] = 0.4f; // एकूण मुले
        widths[totalCols - 1] = 0.4f; // एकूण मुली

        PdfPTable tbl = new PdfPTable(widths);
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(4);

        GunapattrakGenerator.cellHorizontalImageSpan(tbl, ctx, PdfLocalizer.get(ctx, "अ.नं", "Sr.No."), fSmallBold,
                C_HEADER_BG, C_DARK, 1, 2);
        GunapattrakGenerator.cellHorizontalImageSpan(tbl, ctx, PdfLocalizer.get(ctx, "विषय", "Subject"), fSmallBold,
                C_HEADER_BG, C_DARK, 1, 2);

        for (int gi = 0; gi < numGrades; gi++) {
            GunapattrakGenerator.cellHorizontalImageSpan(tbl, ctx, getGradeLabel(ctx, gi), fSmallBold, C_HEADER_BG,
                    C_DARK, 2, 1);
        }
        GunapattrakGenerator.cellHorizontalImageSpan(tbl, ctx, PdfLocalizer.get(ctx, "एकूण", "Total"), fSmallBold,
                C_HEADER_BG, C_DARK, 2, 1);

        // ── Row 2: मुले / मुली under each grade + under एकूण ──────────────────
        for (int gi = 0; gi <= numGrades; gi++) { // numGrades grades + 1 total column
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "मुले", "Boys"), fSmallBold,
                    C_HEADER_BG, C_DARK, 1, 1);
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "मुली", "Girls"), fSmallBold,
                    C_HEADER_BG, C_DARK, 1, 1);
        }

        // ── Data rows — one per subject ───────────────────────────────────────
        boolean alt = false;
        for (int si = 0; si < numSubs; si++) {
            Subject sub = subjects.get(si);
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE;
            alt = !alt;

            cellSpan(tbl, String.valueOf(si + 1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(tbl, PdfLocalizer.translateSubject(ctx, sub.name), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);

            int rowBoys = 0, rowGirls = 0;
            for (int gi = 0; gi < numGrades; gi++) {
                int boys = gradeData[si][gi][0];
                int girls = gradeData[si][gi][1];
                rowBoys += boys;
                rowGirls += girls;
                cellSpan(tbl, boys > 0 ? str(boys) : "0", fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(tbl, girls > 0 ? str(girls) : "0", fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            }
            // Row totals
            cellSpan(tbl, str(rowBoys), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(tbl, str(rowGirls), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }

        doc.add(tbl);

        // ── 5. Bottom: summary table (left) + signatures (right) ─────────────
        float[] bottomWidths = { 4.5f, 5.5f };
        PdfPTable bottomTbl = new PdfPTable(bottomWidths);
        bottomTbl.setWidthPercentage(100);
        bottomTbl.setSpacingBefore(10);

        // Left: पट / उपस्थिती summary
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);

        // Summary table: तपशील | मुले | मुली | एकूण
        float[] sumWidths = { 2.0f, 0.7f, 0.7f, 0.7f };
        PdfPTable sumTbl = new PdfPTable(sumWidths);
        sumTbl.setWidthPercentage(80);

        cellSpan(sumTbl, PdfLocalizer.get(ctx, "तपशील", "Detail"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);
        cellSpan(sumTbl, PdfLocalizer.get(ctx, "मुले", "Boys"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);
        cellSpan(sumTbl, PdfLocalizer.get(ctx, "मुली", "Girls"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);
        cellSpan(sumTbl, PdfLocalizer.get(ctx, "एकूण", "Total"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);

        // पट row
        cellSpan(sumTbl, PdfLocalizer.get(ctx, "पट", "Enrolled"), fSmall, C_WHITE, C_DARK, 1, 1, Element.ALIGN_LEFT);
        cellSpan(sumTbl, str(patBoys), fSmall, C_WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(sumTbl, str(patGirls), fSmall, C_WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(sumTbl, str(patBoys + patGirls), fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);

        // उपस्थिती row
        cellSpan(sumTbl, PdfLocalizer.get(ctx, "उपस्थिती", "Attendance"), fSmall, C_ROW_ALT, C_DARK, 1, 1,
                Element.ALIGN_LEFT);
        cellSpan(sumTbl, str(attBoys), fSmall, C_ROW_ALT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(sumTbl, str(attGirls), fSmall, C_ROW_ALT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(sumTbl, str(attBoys + attGirls), fSmallBold, C_ROW_ALT, C_DARK, 1, 1, Element.ALIGN_CENTER);

        leftCell.addElement(sumTbl);
        bottomTbl.addCell(leftCell);

        // Right: Signatures
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPaddingLeft(20);

        PdfPTable sigTbl = new PdfPTable(2);
        sigTbl.setWidthPercentage(100);
        sigTbl.setSpacingBefore(20);

        String teacherName = cls != null && cls.teacherName != null ? cls.teacherName : "";
        String principalName = school != null && school.principalName != null ? school.principalName : "";

        PdfPCell s1 = rawCell(
                PdfLocalizer.get(ctx, "वर्गशिक्षक स्वाक्षरी : ", "Class Teacher Signature : ") + teacherName, fSmall,
                C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s1.setBorder(Rectangle.TOP);
        s1.setBorderColorTop(C_DARK);
        s1.setBorderWidthTop(0.5f);
        s1.setPaddingTop(4);

        PdfPCell s2 = rawCell(
                PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी : ", "Headmaster Signature : ") + principalName, fSmall,
                C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s2.setBorder(Rectangle.TOP);
        s2.setBorderColorTop(C_DARK);
        s2.setBorderWidthTop(0.5f);
        s2.setPaddingTop(4);

        sigTbl.addCell(s1);
        sigTbl.addCell(s2);
        rightCell.addElement(sigTbl);
        bottomTbl.addCell(rightCell);

        bottomTbl.setKeepTogether(true);
        doc.add(bottomTbl);
    }
}
