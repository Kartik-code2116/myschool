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
 * Option 11 — Subject-wise Marks Register (प्रगती नोंदवही)
 *
 * Layout: Portrait A4, one page per subject.
 * For each subject:
 *   Title: सातत्यपूर्ण सर्वंकष मूल्यमापन
 *   Header: School | Year | Class + Div | Subject | Semester
 *   Table:
 *     Row 1 header: अ.नं | तपशील | आकारिक (अ) × 9 cols | संकलित (ब) × 4 cols | अ+ब | श्रे.गुण | श्रेणी
 *     Row 2 header: sub-names for each col
 *     Row 3 header: max marks from Subject model
 *     Data rows: one per student with their detailed marks
 */
public class MarksRegisterGenerator {

    // 19 data columns (matches Gunapattrak column layout)
    private static final float[] COL_WIDTHS = {
            0.45f, // अ.नं
            1.6f,  // तपशील (name)
            // Formative (9):
            0.55f, 0.55f, 0.55f, 0.55f, 0.55f, 0.55f, 0.55f, 0.55f, 0.6f,
            // Summative (4):
            0.55f, 0.55f, 0.55f, 0.6f,
            // Final 3:
            0.6f,  // अ+ब
            0.6f,  // श्रे.गुण
            0.6f   // श्रेणी
    };

    public static void generateMarksRegister(Context ctx,
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
                        "MarksRegister_" + (isSem2 ? "Sem2" : "Sem1") + "_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(18, 18, 22, 22);

                List<Subject> subjects = cls != null && cls.subjects != null ? cls.subjects : new java.util.ArrayList<>();
                boolean isFirst = true;
                for (Subject sub : subjects) {
                    if (!isFirst) doc.newPage();
                    isFirst = false;
                    addSubjectPage(doc, ctx, school, cls, students, marksMap, sub, isSem2);
                }

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    // Overload for backward compat (sem1 only)
    public static void generateMarksRegister(Context ctx,
                                              School school,
                                              ClassModel cls,
                                              List<Student> students,
                                              Map<String, MarksRecord> sem1Marks,
                                              PdfGenerator.PdfCallback cb) {
        generateMarksRegister(ctx, school, cls, students, sem1Marks, false, cb);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-subject page
    // ─────────────────────────────────────────────────────────────────────────
    private static void addSubjectPage(Document doc, Context ctx, School school, ClassModel cls,
                                        List<Student> students, Map<String, MarksRecord> marksMap,
                                        Subject sub, boolean isSem2) throws Exception {

        // ── 1. Title ─────────────────────────────────────────────────────────
        PdfGenerator.addMarathiParagraph(doc, PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"),
                14, true, C_DARK, 0, 6);

        // ── 2. Meta header ────────────────────────────────────────────────────
        String schoolName = school != null ? nvl(school.name)       : "";
        String yearLabel  = cls   != null ? nvl(cls.academicYearLabel) : "";
        String className  = cls   != null ? nvl(cls.className)         : "";
        String division   = cls   != null ? nvl(cls.division)          : "";
        String semLabel   = isSem2 ? PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester") : PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester");

        // Row 1: school name (left) | empty | year (right)
        PdfPTable hdr1 = new PdfPTable(new float[]{2f, 1f, 1.5f});
        hdr1.setWidthPercentage(100);
        addNoBorder(hdr1, schoolName,         fSmall,     Element.ALIGN_LEFT);
        addNoBorder(hdr1, "",                 fSmall,     Element.ALIGN_CENTER);
        addNoBorder(hdr1, PdfLocalizer.get(ctx, "सन : ", "Year : ") + yearLabel, fSmallBold, Element.ALIGN_RIGHT);
        hdr1.setSpacingAfter(2);
        doc.add(hdr1);

        // Row 2: class+div (left) | subject (center) | semester (right)
        PdfPTable hdr2 = new PdfPTable(new float[]{1.5f, 1f, 1f});
        hdr2.setWidthPercentage(100);
        addNoBorder(hdr2, PdfLocalizer.get(ctx, "इयत्ता: ", "Class: ") + className + PdfLocalizer.get(ctx, ", तुकडी: ", ", Division: ") + division, fSmall,     Element.ALIGN_LEFT);
        addNoBorder(hdr2, PdfLocalizer.translateSubject(ctx, sub.name),                                    fSmallBold, Element.ALIGN_CENTER);
        addNoBorder(hdr2, semLabel,                                         fSmallBold, Element.ALIGN_RIGHT);
        hdr2.setSpacingAfter(5);
        doc.add(hdr2);

        // ── 3. Main table ─────────────────────────────────────────────────────
        PdfPTable tbl = new PdfPTable(COL_WIDTHS);
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(3);

        // ── Header row 1: group spans ─────────────────────────────────────────
        // अ.नं (rowspan 3), तपशील (rowspan 3),
        // आकारिक (अ) colspan 9, संकलित (ब) colspan 4,
        // अ+ब (rowspan 3), श्रे.गुण (rowspan 3), श्रेणी (rowspan 3)
        cellSpan(tbl, PdfLocalizer.get(ctx, "अ. नं", "Sr.No."),   fSmallBold, C_HEADER_BG, C_DARK, 1, 3, Element.ALIGN_CENTER);
        cellSpan(tbl, PdfLocalizer.get(ctx, "तपशील", "Details"),   fSmallBold, C_HEADER_BG, C_DARK, 1, 3, Element.ALIGN_CENTER);
        cellSpan(tbl, PdfLocalizer.get(ctx, "आकारिक (अ)", "Formative (A)"),  fSmallBold, C_HEADER_BG, C_DARK, 9, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, PdfLocalizer.get(ctx, "संकलित (ब)", "Summative (B)"),  fSmallBold, C_HEADER_BG, C_DARK, 4, 1, Element.ALIGN_CENTER);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "अ+ब", "A+B"),     fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "श्रे.गुण", "Total"), fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "श्रेणी", "Grade"),   fSmallBold, C_HEADER_BG, C_DARK, 1, 3);

        // ── Header row 2: sub-column names ────────────────────────────────────
        String[] formNames = {
            PdfLocalizer.get(ctx, "निरीक्षण", "Observation"),
            PdfLocalizer.get(ctx, "तोंडीकाम", "Oral Work"),
            PdfLocalizer.get(ctx, "प्रात्यक्षिक", "Practical"),
            PdfLocalizer.get(ctx, "उपक्रम", "Activity"),
            PdfLocalizer.get(ctx, "प्रकल्प", "Project"),
            PdfLocalizer.get(ctx, "चाचणी", "Test"),
            PdfLocalizer.get(ctx, "स्वाध्याय", "Assignment"),
            PdfLocalizer.get(ctx, "इतर", "Other"),
            PdfLocalizer.get(ctx, "एकूण", "Total")
        };
        for (String f : formNames) GunapattrakGenerator.cellVerticalSpan(tbl, ctx, f, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        
        String[] summNames = {
            PdfLocalizer.get(ctx, "तोंडी", "Oral"),
            PdfLocalizer.get(ctx, "प्रात्य.", "Pract."),
            PdfLocalizer.get(ctx, "लेखी", "Written"),
            PdfLocalizer.get(ctx, "एकूण", "Total")
        };
        for (String s : summNames) GunapattrakGenerator.cellVerticalSpan(tbl, ctx, s, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);

        // ── Header row 3: Max marks row ───────────────────────────────────────
        int formativeMax = sub.maxNirikhshan + sub.maxTondiKam + sub.maxPratyakshik
                + sub.maxUpkram + sub.maxPrakalp + sub.maxChachani + sub.maxSwadhyay + sub.maxItar;
        if (formativeMax == 0 && sub.maxMarks > 0) formativeMax = sub.maxMarks / 2;
        int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
        if (summativeMax == 0 && sub.maxMarks > 0) summativeMax = sub.maxMarks / 2;

        // Formative max cells
        cellSpan(tbl, strBlank(sub.maxNirikhshan),  fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, strBlank(sub.maxTondiKam),    fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, strBlank(sub.maxPratyakshik), fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, strBlank(sub.maxUpkram),      fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, strBlank(sub.maxPrakalp),     fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, strBlank(sub.maxChachani),    fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, strBlank(sub.maxSwadhyay),    fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, strBlank(sub.maxItar),        fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, str(formativeMax),           fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        // Summative max cells
        cellSpan(tbl, strBlank(sub.maxTondi),       fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, strBlank(sub.maxPratyakshikB),fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, strBlank(sub.maxLekhi),       fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, str(summativeMax),           fMicro, C_PRIMARY_LIGHT, C_DARK, 1, 1, Element.ALIGN_CENTER);

        // ── Student data rows ─────────────────────────────────────────────────
        boolean alt = false;
        if (students != null) {
            for (int i = 0; i < students.size(); i++) {
                Student st = students.get(i);
                BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;

                MarksRecord rec = marksMap != null ? marksMap.get(st.id) : null;
                MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);

                cellSpan(tbl, String.valueOf(i + 1), fSmall,     bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(tbl, nvl(st.name),          fSmall,     bg, C_DARK, 1, 1, Element.ALIGN_LEFT);

                if (d != null) {
                    // Formative
                    cellSpan(tbl, strBlank(d.nirikhshan),  fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strBlank(d.tondiKam),    fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strBlank(d.pratyakshik), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strBlank(d.upkram),      fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strBlank(d.prakalp),     fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strBlank(d.chachani),    fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strBlank(d.swadhyay),    fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strBlank(d.itar),        fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, str(d.akarikTotal),     fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    // Summative
                    cellSpan(tbl, strBlank(d.tondi),       fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strBlank(d.pratyakshikB),fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strBlank(d.lekhi),       fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, str(d.sanklit),         fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    // Final
                    cellSpan(tbl, str(d.grandTotal),      fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, str(d.grandTotal),      fSmall,     bg, C_DARK, 1, 1, Element.ALIGN_CENTER); // श्रे.गुण = grandTotal
                    cellSpan(tbl, nvl(normalizeGrade(d.grade != null ? d.grade : "")), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                } else {
                    // 17 empty columns
                    for (int k = 0; k < 17; k++)
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                }
            }
        }
        doc.add(tbl);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static void addNoBorder(PdfPTable tbl, String text, Font font, int align) {
        BaseColor tc = font.getColor() != null ? font.getColor() : C_DARK;
        PdfPCell c = PdfGenerator.rawCell(text, font, BaseColor.WHITE, tc, align);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(2);
        tbl.addCell(c);
    }

    private static String strBlank(int v) {
        return v > 0 ? String.valueOf(v) : "";
    }
}
