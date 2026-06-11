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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kartik.myschool.utils.PdfGenerator.*;

/**
 * Generates the Marks-Grade Ledger PDF (Option 8).
 *
 * Layout (landscape A4):
 * Title: सातत्यपूर्ण सर्वंकष मूल्यमापन
 * Meta: UDISE | Semester | Year / Class / Division
 * TOP TABLE — one row per student, columns: Sr | Name | Gender | Present |
 * (marks, grade) × subjects
 * BOTTOM TABLE — one row per subject, columns: Sr | Subject | अ-1..इ-2 grade
 * counts | एकूण
 * Signatures: class teacher | headmaster
 */
public class MarksGradeLedgerGenerator {

    private static final String[] GRADES = { "A-1", "A-2", "B-1", "B-2", "C-1", "C-2", "D", "E-1", "E-2" };
    private static final String[] GRADE_LABELS = { "अ-1", "अ-2", "ब-1", "ब-2", "क-1", "क-2", "ड", "इ-1", "इ-2" };

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────────

    public static void generateMarksGradeLedger(Context ctx,
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
                        "MarksGradeLedger_" + (isSem2 ? "Sem2" : "Sem1") + "_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4.rotate()); // Landscape
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(12, 12, 18, 18);

                addLedgerContent(doc, ctx, school, cls, students, marksMap, isSem2);

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page builder (also callable for Master Report merge)
    // ─────────────────────────────────────────────────────────────────────────

    private static String getGradeLabel(Context ctx, int gi) {
        if (PdfLocalizer.isEnglish(ctx)) {
            return GRADES[gi];
        }
        return GRADE_LABELS[gi];
    }

    public static void addLedgerContent(Document doc, Context ctx, School school, ClassModel cls,
            List<Student> students,
            Map<String, MarksRecord> marksMap,
            boolean isSem2) throws Exception {

        List<Subject> subjects = (cls != null && cls.subjects != null) ? cls.subjects : new ArrayList<>();
        int numSubs = Math.min(subjects.size(), 9); // cap at 9 for layout

        // ── 1. Title ─────────────────────────────────────────────────────────
        PdfGenerator.addMarathiParagraph(doc,
                PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"),
                14, true, C_DARK, 0, 5);

        // ── 2. Meta header ────────────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(3);
        hdr.setWidthPercentage(100);
        hdr.setSpacingAfter(4);

        String udiseLine = PdfLocalizer.get(ctx, "युडायस: ", "UDISE: ") + nvl(school != null ? school.udiseCode : null);
        String schoolLine = PdfLocalizer.get(ctx, "शाळा: ", "School: ") + nvl(school != null ? school.name : null);
        String semText = isSem2
                ? PdfLocalizer.get(ctx, "कार्यात्मक द्वितीय सत्र", "Second Semester")
                : PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester");
        // Wait, the original just had "द्वितीय सत्र" : "प्रथम सत्र"
        semText = isSem2
                ? PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester")
                : PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester");

        String rightText = PdfLocalizer.get(ctx, "सन: ", "Year: ") + nvl(cls != null ? cls.academicYearLabel : null)
                + PdfLocalizer.get(ctx, "\nइयत्ता: ", "\nClass: ") + nvl(cls != null ? cls.className : null)
                + PdfLocalizer.get(ctx, ", तुकडी: ", ", Division: ") + nvl(cls != null ? cls.division : null);

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

        PdfPCell hC = rawCell(semText, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        hC.setBorder(Rectangle.NO_BORDER);
        PdfPCell hR = rawCell(rightText.replace("\n", " | "), fSmallBold, C_WHITE, C_DARK, Element.ALIGN_RIGHT);
        hR.setBorder(Rectangle.NO_BORDER);
        hdr.addCell(hL);
        hdr.addCell(hC);
        hdr.addCell(hR);
        doc.add(hdr);

        // ── 3. TOP TABLE: per-student marks + grade per subject ───────────────
        // Fixed cols: अ.नं | विद्यार्थ्यांचे नाव | लिंग | हजर दिवस
        // Per-subject: प्राप्त | श्रेणी
        // Total cols = 4 + numSubs*2

        int topCols = 4 + numSubs * 2;
        float[] topWidths = new float[topCols];
        topWidths[0] = 0.3f; // अ.नं
        topWidths[1] = 1.4f; // नाव
        topWidths[2] = 0.3f; // लिंग
        topWidths[3] = 0.35f; // हजर दिवस
        for (int i = 4; i < topCols; i++)
            topWidths[i] = 0.38f; // प्राप्त / श्रेणी alternating

        PdfPTable topTbl = new PdfPTable(topWidths);
        topTbl.setWidthPercentage(100);
        topTbl.setSpacingBefore(3);
        topTbl.setSpacingAfter(8);

        // ── Header Row 1: fixed cols (rowspan 2) + subject names (colspan 2 each) ──
        GunapattrakGenerator.cellHorizontalImageSpan(topTbl, ctx, PdfLocalizer.get(ctx, "अ.नं", "Sr.No."), fSmallBold,
                C_HEADER_BG, C_DARK, 1, 2);
        GunapattrakGenerator.cellHorizontalImageSpan(topTbl, ctx,
                PdfLocalizer.get(ctx, "विद्यार्थ्यांचे नाव", "Student Name"), fSmallBold, C_HEADER_BG, C_DARK, 1, 2);
        GunapattrakGenerator.cellVerticalSpan(topTbl, ctx, PdfLocalizer.get(ctx, "लिंग", "Gender"), fSmallBold,
                C_HEADER_BG, C_DARK, 1, 2);
        GunapattrakGenerator.cellVerticalSpan(topTbl, ctx, PdfLocalizer.get(ctx, "हजर दिवस", "Attendance"), fSmallBold,
                C_HEADER_BG, C_DARK, 1, 2);

        for (int si = 0; si < numSubs; si++) {
            String subName = subjects.get(si).name;
            GunapattrakGenerator.cellVerticalSpan(topTbl, ctx, PdfLocalizer.translateSubject(ctx, subName), fSmallBold,
                    C_HEADER_BG, C_DARK, 2, 1);
        }

        // ── Header Row 2: प्र. (marks) + श्रे. (grade) per subject ───────────
        for (int si = 0; si < numSubs; si++) {
            GunapattrakGenerator.cellVerticalSpan(topTbl, ctx, PdfLocalizer.get(ctx, "प्र.", "Obt."), fSmallBold,
                    C_HEADER_BG, C_DARK, 1, 1);
            GunapattrakGenerator.cellVerticalSpan(topTbl, ctx, PdfLocalizer.get(ctx, "श्रे.", "Grd."), fSmallBold,
                    C_HEADER_BG, C_DARK, 1, 1);
        }

        // ── Grade distribution accumulator ────────────────────────────────────
        // gradeCounts[subjectIndex][gradeIndex] = total students with that grade
        int[][] gradeCounts = new int[numSubs][GRADES.length];

        // ── Data rows ─────────────────────────────────────────────────────────
        boolean alt = false;
        if (students != null) {
            for (int rowIdx = 0; rowIdx < students.size(); rowIdx++) {
                Student st = students.get(rowIdx);
                BaseColor bg = alt ? C_ROW_ALT : C_WHITE;
                alt = !alt;

                MarksRecord rec = marksMap != null ? marksMap.get(st.id) : null;

                // Gender short label
                String genderLabel = "-";
                if (st.gender != null) {
                    String g = st.gender.trim().toLowerCase();
                    if (g.contains("female") || g.contains("girl") || g.contains("मुलगी") || g.contains("स्त्री")) {
                        genderLabel = "G";
                    } else if (g.contains("male") || g.contains("boy") || g.contains("मुलगा") || g.contains("पुरुष")) {
                        genderLabel = "B";
                    }
                }

                String presentDays = rec != null && rec.presentDays > 0 ? str(rec.presentDays) : "0";

                cellSpan(topTbl, str(rowIdx + 1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(topTbl, nvl(st.name), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);
                cellSpan(topTbl, genderLabel, fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(topTbl, presentDays, fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);

                for (int si = 0; si < numSubs; si++) {
                    Subject sub = subjects.get(si);
                    MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
                    if (d != null) {
                        // Marks: grandTotal
                        cellSpan(topTbl, d.grandTotal > 0 ? str(d.grandTotal) : "0",
                                fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        // Grade
                        String gradeStr = "-";
                        if (d.grade != null && !d.grade.isEmpty()) {
                            String normG = normalizeGrade(d.grade);
                            gradeStr = normG;
                            for (int gi = 0; gi < GRADES.length; gi++) {
                                if (GRADES[gi].equalsIgnoreCase(normG)) {
                                    gradeStr = getGradeLabel(ctx, gi);
                                    break;
                                }
                            }
                        }
                        cellSpan(topTbl, gradeStr, fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);

                        // Accumulate grade distribution
                        String normG = normalizeGrade(d.grade != null ? d.grade : "");
                        for (int gi = 0; gi < GRADES.length; gi++) {
                            if (GRADES[gi].equalsIgnoreCase(normG)) {
                                gradeCounts[si][gi]++;
                                break;
                            }
                        }
                    } else {
                        cellSpan(topTbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(topTbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    }
                }
            }
        }
        topTbl.setKeepTogether(true);
        doc.add(topTbl);

        // ── 4. BOTTOM TABLE: grade distribution per subject ───────────────────
        // Cols: अ.नं | विषय | अ-1 | अ-2 | ब-1 | ब-2 | क-1 | क-2 | ड | इ-1 | इ-2 | एकूण
        int botCols = 2 + GRADES.length + 1; // 13
        float[] botWidths = new float[botCols];
        botWidths[0] = 0.35f; // अ.नं
        botWidths[1] = 1.2f; // विषय
        for (int i = 2; i < botCols - 1; i++)
            botWidths[i] = 0.35f; // grade cols
        botWidths[botCols - 1] = 0.45f; // एकूण

        PdfPTable botTbl = new PdfPTable(botWidths);
        botTbl.setWidthPercentage(55); // Only use left ~55% width to leave room for signatures
        botTbl.setHorizontalAlignment(Element.ALIGN_LEFT);
        botTbl.setSpacingBefore(4);

        // Header row
        cellSpan(botTbl, PdfLocalizer.get(ctx, "अ.नं", "Sr.No."), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);
        cellSpan(botTbl, PdfLocalizer.get(ctx, "विषय", "Subject"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);
        for (int gi = 0; gi < GRADES.length; gi++) {
            cellSpan(botTbl, getGradeLabel(ctx, gi), fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }
        cellSpan(botTbl, PdfLocalizer.get(ctx, "एकूण", "Total"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);

        // Data rows
        boolean botAlt = false;
        for (int si = 0; si < numSubs; si++) {
            Subject sub = subjects.get(si);
            BaseColor bg = botAlt ? C_ROW_ALT : C_WHITE;
            botAlt = !botAlt;

            cellSpan(botTbl, str(si + 1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(botTbl, PdfLocalizer.translateSubject(ctx, sub.name), fSmall, bg, C_DARK, 1, 1,
                    Element.ALIGN_LEFT);

            int rowTotal = 0;
            for (int gi = 0; gi < GRADES.length; gi++) {
                int cnt = gradeCounts[si][gi];
                rowTotal += cnt;
                cellSpan(botTbl, str(cnt), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            }
            cellSpan(botTbl, str(rowTotal), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }

        // ── 5. Signatures + bottom table side by side ─────────────────────────
        // We use a 2-column parent: left = grade table, right = signatures
        float[] wrapWidths = { 5.5f, 4.5f };
        PdfPTable wrapTbl = new PdfPTable(wrapWidths);
        wrapTbl.setWidthPercentage(100);
        wrapTbl.setSpacingBefore(4);

        // Left: grade distribution table
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(botTbl);
        wrapTbl.addCell(leftCell);

        // Right: signatures
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPaddingLeft(20);
        rightCell.setVerticalAlignment(Element.ALIGN_BOTTOM);

        PdfPTable sigTbl = new PdfPTable(2);
        sigTbl.setWidthPercentage(100);
        sigTbl.setSpacingBefore(30);

        String teacherName = (cls != null && cls.teacherName != null) ? cls.teacherName : "";
        String principalName = (school != null && school.principalName != null) ? school.principalName : "";

        PdfPCell s1 = rawCell(
                PdfLocalizer.get(ctx, "वर्गशिक्षक स्वाक्षरी : ", "Class Teacher Signature : ") + teacherName,
                fSmall, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s1.setBorder(Rectangle.TOP);
        s1.setBorderColorTop(C_DARK);
        s1.setBorderWidthTop(0.5f);
        s1.setPaddingTop(6);

        PdfPCell s2 = rawCell(
                PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी : ", "Headmaster Signature : ") + principalName,
                fSmall, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s2.setBorder(Rectangle.TOP);
        s2.setBorderColorTop(C_DARK);
        s2.setBorderWidthTop(0.5f);
        s2.setPaddingTop(6);

        sigTbl.addCell(s1);
        sigTbl.addCell(s2);
        rightCell.addElement(sigTbl);
        wrapTbl.addCell(rightCell);

        wrapTbl.setKeepTogether(true);
        doc.add(wrapTbl);
    }
}
