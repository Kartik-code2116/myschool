package com.kartik.myschool.utils;

import android.content.Context;

import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.MarksRecord;
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.model.Subject;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates all 4 Myschool CCE report PDFs:
 *  1. generateGunapattrak   — per-student marks sheet  (both semesters)
 *  2. generateDescriptive   — per-student descriptive remarks
 *  3. generateProgressBook  — whole-class landscape table
 *  4. generatePersonalityRecord — compact personality record
 *
 * Font: tries NotoSansDevanagari from assets/fonts/; falls back to Helvetica
 * so the app never crashes if the font file is absent.
 */
public class PdfGenerator {

    // ── Callback ─────────────────────────────────────────────────────────────
    public interface PdfCallback {
        void onSuccess(File pdfFile);
        void onError(Exception e);
    }

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final BaseColor C_PRIMARY       = new BaseColor(21, 101, 192);
    private static final BaseColor C_PRIMARY_LIGHT = new BaseColor(187, 222, 251);
    private static final BaseColor C_HEADER_BG     = new BaseColor(236, 239, 241);
    private static final BaseColor C_ROW_ALT       = new BaseColor(245, 247, 250);
    private static final BaseColor C_WHITE         = BaseColor.WHITE;
    private static final BaseColor C_DARK          = new BaseColor(28, 27, 31);
    private static final BaseColor C_GREY          = new BaseColor(117, 117, 117);
    private static final BaseColor C_BORDER        = new BaseColor(224, 224, 224);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static BaseFont sMarathiBase;
    private static boolean sFontsInitDone = false;

    private static Font fTitle, fTitleSub, fHeader, fNormal, fSmall, fBold, fSmallBold;

    private static synchronized void ensureFonts(Context ctx) {
        if (sFontsInitDone) return;
        sFontsInitDone = true;
        try {
            File fontFile = new File(ctx.getFilesDir(), "noto_dev.ttf");
            if (!fontFile.exists()) {
                InputStream is = ctx.getAssets().open("fonts/NotoSansDevanagari-Regular.ttf");
                FileOutputStream os = new FileOutputStream(fontFile);
                byte[] buf = new byte[4096]; int len;
                while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
                is.close(); os.close();
            }
            sMarathiBase = BaseFont.createFont(fontFile.getAbsolutePath(),
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception ignored) {
            sMarathiBase = null;
        }
        buildFonts();
    }

    private static void buildFonts() {
        if (sMarathiBase != null) {
            fTitle     = new Font(sMarathiBase, 13, Font.BOLD);
            fTitleSub  = new Font(sMarathiBase,  9, Font.NORMAL);
            fHeader    = new Font(sMarathiBase, 11, Font.BOLD);
            fNormal    = new Font(sMarathiBase, 10, Font.NORMAL);
            fSmall     = new Font(sMarathiBase,  9, Font.NORMAL);
            fBold      = new Font(sMarathiBase, 10, Font.BOLD);
            fSmallBold = new Font(sMarathiBase,  8, Font.BOLD);
        } else {
            fTitle     = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);
            fTitleSub  = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL);
            fHeader    = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            fNormal    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            fSmall     = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL);
            fBold      = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            fSmallBold = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD);
        }
    }

    // ── Output dir ────────────────────────────────────────────────────────────
    private static File outDir(Context ctx) {
        File d = new File(ctx.getExternalFilesDir(null), "myschool_reports");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static String ts() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  REPORT 1 — गुणपत्रक  (per-student, both semesters)
    // ═════════════════════════════════════════════════════════════════════════
    //  COVER PAGE (Used by ReportPrintingFragment option 0)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generateCoverPage(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           Student student,
                                           MarksRecord sem1,
                                           MarksRecord sem2,
                                           PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "CoverPage_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addCoverPageContent(doc, ctx, school, cls, student);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  INDEX PAGE (Used by ReportPrintingFragment option 1)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generateIndexPage(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           java.util.List<Student> students,
                                           PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "IndexPage_" + (cls != null ? nvl(cls.className).replaceAll("[^a-zA-Z0-9_-]", "") : "Class") + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addIndexPageContent(doc, ctx, school, cls, students);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    public static void generateGunapattrak(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           Student student,
                                           MarksRecord sem1,
                                           MarksRecord sem2,
                                           PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "Gunapattrak_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addGunapattrakContent(doc, ctx, school, cls, student, sem1, sem2);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  REPORT 2 — गुणवर्णनिका  (per-student descriptive remarks)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generateDescriptive(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           Student student,
                                           MarksRecord sem1,
                                           MarksRecord sem2,
                                           PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "Descriptive_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addDescriptiveContent(doc, ctx, school, cls, student, sem1, sem2);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BULK COMBINED PDF GENERATOR (One file, Multiple Pages)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generateBulkCombinedPdf(Context ctx,
                                               School school,
                                               ClassModel cls,
                                               java.util.List<Student> students,
                                               java.util.Map<String, MarksRecord> sem1Map,
                                               java.util.Map<String, MarksRecord> sem2Map,
                                               int reportPosition,
                                               PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                String rName;
                if (reportPosition == 2 || reportPosition == 6 || reportPosition == 13) rName = "Gunapattrak";
                else if (reportPosition == 3 || reportPosition == 7) rName = "Descriptive";
                else if (reportPosition == 0) rName = "CoverPage";
                else rName = "Personality";
                File out = new File(outDir(ctx), "Bulk_" + rName + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                
                boolean isFirst = true;
                for (Student student : students) {
                    if (!isFirst) doc.newPage();
                    isFirst = false;
                    doc.setMargins(30, 30, 30, 30);
                    MarksRecord s1 = sem1Map != null ? sem1Map.get(student.id) : null;
                    MarksRecord s2 = sem2Map != null ? sem2Map.get(student.id) : null;
                    
                    if (reportPosition == 2 || reportPosition == 6 || reportPosition == 13) {
                        addGunapattrakContent(doc, ctx, school, cls, student, s1, s2);
                    } else if (reportPosition == 3 || reportPosition == 7) {
                        addDescriptiveContent(doc, ctx, school, cls, student, s1, s2);
                    } else if (reportPosition == 0) {
                        addCoverPageContent(doc, ctx, school, cls, student);
                    } else {
                        addPersonalityContent(doc, ctx, school, cls, student, s1, s2);
                    }
                }
                
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GRADE CHART (श्रेणी तक्का) — Class-wide report
    // ═════════════════════════════════════════════════════════════════════════
    public static void generateGradeChart(Context ctx,
                                          School school,
                                          ClassModel cls,
                                          List<Student> students,
                                          Map<String, MarksRecord> marksMap,
                                          boolean isSem2,
                                          PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "GradeChart_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(15, 15, 30, 30); // Narrow margins for 20 columns
                
                // Title
                Paragraph title = new Paragraph("सातत्यपूर्ण सर्वंकष मूल्यमापन", new Font(sMarathiBase, 16, Font.BOLD, C_DARK));
                title.setAlignment(Element.ALIGN_CENTER);
                doc.add(title);
                
                // Header Tbl
                PdfPTable hTbl = new PdfPTable(3);
                hTbl.setWidthPercentage(100);
                hTbl.setSpacingBefore(10); hTbl.setSpacingAfter(5);
                
                String udise = "Udise: " + nvl(school.udiseCode) + "\n" + nvl(school.name);
                String term = isSem2 ? "द्वितीय सत्र" : "प्रथम सत्र";
                String rightTxt = "सन : " + nvl(cls.academicYearLabel) + "\nइयत्ता: " + nvl(cls.className) + ", तुकडी: " + nvl(cls.division);
                
                PdfPCell cL = new PdfPCell(new Phrase(udise, fSmallBold)); cL.setBorder(Rectangle.NO_BORDER); cL.setHorizontalAlignment(Element.ALIGN_LEFT);
                PdfPCell cC = new PdfPCell(new Phrase(term, fSmallBold)); cC.setBorder(Rectangle.NO_BORDER); cC.setHorizontalAlignment(Element.ALIGN_CENTER); cC.setVerticalAlignment(Element.ALIGN_BOTTOM);
                PdfPCell cR = new PdfPCell(new Phrase(rightTxt, fSmallBold)); cR.setBorder(Rectangle.NO_BORDER); cR.setHorizontalAlignment(Element.ALIGN_RIGHT);
                
                hTbl.addCell(cL); hTbl.addCell(cC); hTbl.addCell(cR);
                doc.add(hTbl);
                
                // Main Table
                List<Subject> subjects = cls.subjects != null ? cls.subjects : new ArrayList<>();
                int numSubjects = Math.min(subjects.size(), 9); // Max 9 subjects fit properly on Portrait A4
                
                int numCols = 2 + (numSubjects * 2);
                float[] widths = new float[numCols];
                widths[0] = 0.5f; // SrNo
                widths[1] = 1.6f; // Name
                for (int i = 0; i < numSubjects; i++) {
                    widths[2 + (i*2)] = 0.4f; // Marks
                    widths[2 + (i*2) + 1] = 0.4f; // Grade
                }
                
                PdfPTable tbl = new PdfPTable(widths);
                tbl.setWidthPercentage(100);
                
                // Row 1
                cellSpan(tbl, "अ.नं", fSmallBold, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
                cellSpan(tbl, "विद्यार्थ्याचे नाव", fSmallBold, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
                for (int i = 0; i < numSubjects; i++) {
                    cellSpan(tbl, subjects.get(i).name, fSmallBold, C_HEADER_BG, C_DARK, 2, 1, Element.ALIGN_CENTER);
                }
                
                // Row 2 (Vertical subheaders)
                for (int i = 0; i < numSubjects; i++) {
                    cellVerticalSpan(tbl, "गुण", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
                    cellVerticalSpan(tbl, "श्रेणी", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
                }
                
                // Data Rows
                boolean alt = false;
                for (int sIdx = 0; sIdx < students.size(); sIdx++) {
                    Student student = students.get(sIdx);
                    BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                    
                    cellSpan(tbl, String.valueOf(sIdx + 1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, nvl(student.name), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);
                    
                    MarksRecord rec = marksMap != null ? marksMap.get(student.id) : null;
                    
                    for (int i = 0; i < numSubjects; i++) {
                        MarksRecord.SubjectMarksDetail d = detail(rec, subjects.get(i).name);
                        if (d != null) {
                            cellSpan(tbl, str(d.grandTotal), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                            cellSpan(tbl, nvl(d.grade), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        } else {
                            cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                            cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        }
                    }
                }
                
                doc.add(tbl);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  REPORT 3 — प्रगतीपुस्तक  (whole-class, landscape, one page per subject)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generateProgressBook(Context ctx,
                                            School school,
                                            ClassModel cls,
                                            List<Student> students,
                                            Map<String, MarksRecord> sem1Marks,
                                            Map<String, MarksRecord> sem2Marks,
                                            PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "Pragati_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);

                // Page 1: Index (अनुक्रमणिका)
                Paragraph indexTitle = new Paragraph("अनुक्रमणिका", new Font(sMarathiBase, 18, Font.BOLD, C_DARK));
                indexTitle.setAlignment(Element.ALIGN_CENTER);
                doc.add(indexTitle);
                
                PdfPTable indexTop = new PdfPTable(3);
                indexTop.setWidthPercentage(100); indexTop.setSpacingBefore(10); indexTop.setSpacingAfter(10);
                String udiseStr = "युडायस: " + (school.udiseCode!=null?school.udiseCode:"");
                String schoolStr = "शाळा: " + (school.name!=null?school.name:"");
                cellSpan(indexTop, udiseStr + "\n" + schoolStr, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_LEFT);
                cellSpan(indexTop, "प्रथम सत्र", fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);
                String yearStr = "सन: " + (cls.academicYearLabel!=null?cls.academicYearLabel:"");
                String classStr = "इयत्ता: " + (cls.className!=null?cls.className:"");
                cellSpan(indexTop, yearStr + "\n" + classStr, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_RIGHT);
                doc.add(indexTop);

                PdfPTable indexTbl = new PdfPTable(new float[]{0.6f, 3.0f, 1.0f, 1.2f, 0.8f});
                indexTbl.setWidthPercentage(100);
                cellSpan(indexTbl, "अ.नं", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(indexTbl, "विद्यार्थ्याचे नाव", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(indexTbl, "रजि.नं.", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(indexTbl, "जन्मतारीख", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(indexTbl, "पृष्ठ क्र.", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);

                boolean alt = false;
                for (int i=0; i<students.size(); i++) {
                    Student s = students.get(i);
                    BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                    cellSpan(indexTbl, String.valueOf(i+1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(indexTbl, nvl(s.name), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);
                    cellSpan(indexTbl, "-", fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(indexTbl, "-", fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(indexTbl, String.valueOf(i+1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                }
                doc.add(indexTbl);

                // Pages 2+: Subjects
                if (cls.subjects != null) {
                    for (int si = 0; si < cls.subjects.size(); si++) {
                        doc.newPage();
                        Subject sub = cls.subjects.get(si);
                        
                        Paragraph pageTitle = new Paragraph("सातत्यपूर्ण सर्वंकष मूल्यमापन", new Font(sMarathiBase, 18, Font.BOLD, C_DARK));
                        pageTitle.setAlignment(Element.ALIGN_CENTER);
                        pageTitle.setSpacingAfter(10);
                        doc.add(pageTitle);
                        
                        PdfPTable top = new PdfPTable(3);
                        top.setWidthPercentage(100); top.setSpacingAfter(5);
                        cellSpan(top, schoolStr + "\n" + classStr, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_LEFT);
                        cellSpan(top, sub.name, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(top, yearStr + "\n" + "प्रथम सत्र", fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_RIGHT);
                        doc.add(top);

                        float[] widths = {0.6f, 2.5f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.8f, 0.7f, 0.7f, 0.7f, 0.8f, 0.8f, 0.8f, 0.8f};
                        PdfPTable tbl = new PdfPTable(widths);
                        tbl.setWidthPercentage(100); tbl.setSpacingAfter(10);

                        cellSpan(tbl, "अ.नं", fSmallBold, C_HEADER_BG, C_DARK, 1, 3, Element.ALIGN_CENTER);
                        cellSpan(tbl, "तपशील", fSmallBold, C_HEADER_BG, C_DARK, 1, 3, Element.ALIGN_CENTER);
                        cellSpan(tbl, "आकारिक (अ)", fSmallBold, C_HEADER_BG, C_DARK, 9, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, "संकलित (ब)", fSmallBold, C_HEADER_BG, C_DARK, 4, 1, Element.ALIGN_CENTER);
                        cellVerticalSpan(tbl, "अ+ब", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
                        cellVerticalSpan(tbl, "शे.गुण", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
                        cellVerticalSpan(tbl, "श्रेणी", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);

                        String[] formatives = {"निरीक्षण", "तोंडीकाम", "प्रात्यक्षिक", "उपक्रम", "प्रकल्प", "चाचणी", "स्वाध्याय", "इतर", "एकूण"};
                        for (String f : formatives) cellVerticalSpan(tbl, f, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
                        String[] summatives = {"तोंडी", "प्रात्य.", "लेखी", "एकूण"};
                        for (String s : summatives) cellVerticalSpan(tbl, s, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);

                        // Row 3: max marks
                        String[] maxMarksForm = {strZero(sub.maxNirikhshan), strZero(sub.maxTondiKam), strZero(sub.maxPratyakshik), strZero(sub.maxUpkram), strZero(sub.maxPrakalp), strZero(sub.maxChachani), strZero(sub.maxSwadhyay), strZero(sub.maxItar), str(sub.maxMarks / 2)};
                        for (String m : maxMarksForm) cellSpan(tbl, m, fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        String[] maxMarksSumm = {strZero(sub.maxTondi), strZero(sub.maxPratyakshikB), strZero(sub.maxLekhi), str(sub.maxMarks - sub.maxMarks / 2)};
                        for (String m : maxMarksSumm) cellSpan(tbl, m, fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);

                        alt = false;
                        for (int i = 0; i < students.size(); i++) {
                            Student s = students.get(i);
                            BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                            cellSpan(tbl, String.valueOf(i+1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                            cellSpan(tbl, nvl(s.name), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);

                            MarksRecord rec = null;
                            if (sem1Marks != null) rec = sem1Marks.get(s.id);
                            if (rec == null && sem2Marks != null) rec = sem2Marks.get(s.id);
                            
                            MarksRecord.SubjectMarksDetail d = rec != null ? detail(rec, sub.name) : null;

                            if (d != null) {
                                cellSpan(tbl, strZero(d.nirikhshan), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.tondiKam), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.pratyakshik), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.upkram), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.prakalp), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.chachani), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.swadhyay), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.itar), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, str(d.akarikTotal), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                
                                cellSpan(tbl, strZero(d.tondi), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.pratyakshikB), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.lekhi), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, str(d.sanklit), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                
                                cellSpan(tbl, str(d.grandTotal), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, calculatePercentageString(d.grandTotal, sub.maxMarks), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, nvl(d.grade), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                            } else {
                                for (int k = 0; k < 16; k++) cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                            }
                        }
                        doc.add(tbl);
                    }
                }

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  REPORT 4 — व्यक्तिमत्व नोंदी  (compact personality record)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generatePersonalityRecord(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           Student student,
                                           MarksRecord sem1,
                                           MarksRecord sem2,
                                           PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "Personality_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addPersonalityContent(doc, ctx, school, cls, student, sem1, sem2);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }


    // ═════════════════════════════════════════════════════════════════════════
    //  COMBINED SINGLE-STUDENT REPORT (used by MarksheetActivity)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generate(Context ctx, School school, ClassModel cls,
                                Student student, MarksRecord marks, PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "CombinedReport_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);

                // --- Page 1: Cover Page ---
                addCoverPageContent(doc, ctx, school, cls, student);
                doc.newPage();

                // --- Page 2: Gunapattrak ---
                addGunapattrakContent(doc, ctx, school, cls, student, marks, null);
                
                // --- Page 3: Descriptive ---
                doc.newPage();
                addDescriptiveContent(doc, ctx, school, cls, student, marks, null);

                // --- Page 4: Personality ---
                doc.newPage();
                addPersonalityContent(doc, ctx, school, cls, student, marks, null);

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    private static void addCoverPageContent(Document doc, Context ctx, School school, ClassModel cls, Student student) throws Exception {
        // 1. Logo and Header
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);

        try {
            android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(ctx, com.kartik.myschool.R.drawable.ic_school);
            if (d != null) {
                int w = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : 100;
                int h = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 100;
                android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                d.draw(canvas);

                java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(stream.toByteArray());
                img.setAlignment(Element.ALIGN_CENTER);
                img.scaleToFit(60, 60);
                c.addElement(img);
            }
        } catch (Exception e) { e.printStackTrace(); }

        Paragraph p1 = new Paragraph("जिल्हा परिषद", colored(fHeader, C_DARK));
        p1.setAlignment(Element.ALIGN_CENTER);
        p1.setSpacingBefore(10);
        c.addElement(p1);
        t.addCell(c);
        doc.add(t);
        
        // 2. Year (सन)
        String yearLabel = cls != null && cls.academicYearLabel != null ? cls.academicYearLabel : "2025-26";
        Paragraph yearPara = new Paragraph("सन : " + yearLabel, colored(fHeader, C_DARK));
        yearPara.setAlignment(Element.ALIGN_CENTER);
        yearPara.setSpacingBefore(5);
        yearPara.setSpacingAfter(40);
        doc.add(yearPara);

        // 3. Large Red Title
        Font titleFont;
        if (sMarathiBase != null) {
            titleFont = new Font(sMarathiBase, 38, Font.BOLD, new BaseColor(244, 131, 145));
        } else {
            titleFont = new Font(Font.FontFamily.HELVETICA, 38, Font.BOLD, new BaseColor(244, 131, 145));
        }
        Paragraph title1 = new Paragraph("सातत्यपूर्ण सर्वंकष", titleFont);
        title1.setAlignment(Element.ALIGN_CENTER);
        doc.add(title1);
        
        Paragraph title2 = new Paragraph("मूल्यमापन", titleFont);
        title2.setAlignment(Element.ALIGN_CENTER);
        title2.setSpacingAfter(20);
        doc.add(title2);

        // Decorative light-blue divider
        com.itextpdf.text.pdf.draw.LineSeparator ls = new com.itextpdf.text.pdf.draw.LineSeparator();
        ls.setLineColor(new BaseColor(173, 216, 230)); // Light blue
        ls.setLineWidth(1.5f);
        ls.setPercentage(40);
        ls.setAlignment(Element.ALIGN_CENTER);
        doc.add(ls);
        
        // Additional spacing
        Paragraph space = new Paragraph(" ");
        space.setSpacingAfter(40);
        doc.add(space);

        // 4. White Information Box (clean left-aligned list)
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(85);
        box.setSpacingBefore(30);
        
        PdfPCell boxCell = new PdfPCell();
        boxCell.setBorder(Rectangle.NO_BORDER);
        boxCell.setCellEvent(new com.itextpdf.text.pdf.PdfPCellEvent() {
            @Override
            public void cellLayout(PdfPCell cell, Rectangle position, com.itextpdf.text.pdf.PdfContentByte[] canvases) {
                com.itextpdf.text.pdf.PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
                canvas.saveState();
                canvas.setColorFill(new BaseColor(242, 245, 249)); // Light shaded background
                canvas.roundRectangle(position.getLeft(), position.getBottom(), position.getWidth(), position.getHeight(), 12f);
                canvas.fill();
                canvas.restoreState();
            }
        });
        boxCell.setPaddingTop(20);
        boxCell.setPaddingBottom(20);
        boxCell.setPaddingLeft(50);
        boxCell.setPaddingRight(20);

        PdfPTable tbl = new PdfPTable(new float[]{0.15f, 1.2f, 0.1f, 3.5f});
        tbl.setWidthPercentage(100);

        String[] keys = {"युडायस", "शाळा", "वर्गशिक्षक", "इयत्ता", "तुकडी"};
        String[] vals = {
            school != null ? nvl(school.udiseCode) : "",
            school != null ? nvl(school.name) : "",
            cls != null ? nvl(cls.teacherName) : "",
            cls != null ? nvl(cls.className) : "",
            cls != null ? nvl(cls.division) : "-"
        };

        for (int i = 0; i < keys.length; i++) {
            PdfPCell c1 = new PdfPCell(new Phrase("●", colored(fHeader, C_DARK))); c1.setBorder(Rectangle.NO_BORDER); c1.setPaddingBottom(18);
            PdfPCell c2 = new PdfPCell(new Phrase(keys[i], colored(fHeader, C_DARK))); c2.setBorder(Rectangle.NO_BORDER); c2.setPaddingBottom(18);
            PdfPCell c3 = new PdfPCell(new Phrase(":", colored(fHeader, C_DARK))); c3.setBorder(Rectangle.NO_BORDER); c3.setPaddingBottom(18);
            PdfPCell c4 = new PdfPCell(new Phrase(vals[i], colored(fHeader, C_DARK))); c4.setBorder(Rectangle.NO_BORDER); c4.setPaddingBottom(18);
            tbl.addCell(c1); tbl.addCell(c2); tbl.addCell(c3); tbl.addCell(c4);
        }

        boxCell.addElement(tbl);
        box.addCell(boxCell);
        doc.add(box);
    }

    private static void addIndexPageContent(Document doc, Context ctx, School school, ClassModel cls, java.util.List<Student> students) throws Exception {
        // Title
        Paragraph title = new Paragraph("अनुक्रमणिका", colored(new Font(sMarathiBase, 22, Font.BOLD), new BaseColor(40, 40, 90)));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        doc.add(title);

        // Header Info table
        PdfPTable headerTbl = new PdfPTable(3);
        headerTbl.setWidthPercentage(100);
        headerTbl.setWidths(new float[]{1.5f, 1f, 1f});

        // Row 1
        PdfPCell c1 = new PdfPCell(new Phrase("युडायस: " + (school != null ? nvl(school.udiseCode) : ""), fBold)); c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase("प्रथम सत्र", fBold)); c2.setBorder(Rectangle.NO_BORDER); c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell c3 = new PdfPCell(new Phrase("सन : " + (cls != null ? nvl(cls.academicYearLabel) : "2025-26"), fBold)); c3.setBorder(Rectangle.NO_BORDER); c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c1); headerTbl.addCell(c2); headerTbl.addCell(c3);

        // Row 2
        PdfPCell c4 = new PdfPCell(new Phrase("शाळा: " + (school != null ? nvl(school.name) : ""), fBold)); c4.setBorder(Rectangle.NO_BORDER); c4.setColspan(2);
        PdfPCell c5 = new PdfPCell(new Phrase("इयत्ता: " + (cls != null ? nvl(cls.className) : "") + ", तुकडी: " + (cls != null ? nvl(cls.division) : "-"), fBold)); c5.setBorder(Rectangle.NO_BORDER); c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c4); headerTbl.addCell(c5);
        
        headerTbl.setSpacingAfter(15);
        doc.add(headerTbl);

        // Students Table
        PdfPTable tbl = new PdfPTable(5);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{0.8f, 3.5f, 1.2f, 1.5f, 1f});

        BaseColor headerBg = new BaseColor(218, 233, 245); // Light blue

        String[] headers = {"अ.नं", "विद्यार्थ्याचे नाव", "रजि.नं.", "जन्मतारीख", "पृष्ठ क्र."};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, fBold));
            c.setBackgroundColor(headerBg);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(8);
            c.setBorderColor(C_DARK);
            c.setBorderWidth(0.5f);
            tbl.addCell(c);
        }

        if (students != null) {
            int pageNo = 1;
            boolean alt = false;
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                
                BaseColor bg = alt ? new BaseColor(245, 247, 250) : BaseColor.WHITE;
                alt = !alt;
                
                PdfPCell[] row = new PdfPCell[5];
                row[0] = new PdfPCell(new Phrase(String.valueOf(i + 1), fNormal));
                row[1] = new PdfPCell(new Phrase(nvl(s.name), fNormal));
                row[2] = new PdfPCell(new Phrase(nvl(s.registrationNo), fNormal));
                row[3] = new PdfPCell(new Phrase(nvl(s.dob), fNormal));
                row[4] = new PdfPCell(new Phrase(String.valueOf(pageNo++), fNormal));
                
                for (PdfPCell cell : row) {
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(8);
                    cell.setBorderColor(C_DARK);
                    cell.setBorderWidth(0.5f);
                    cell.setBackgroundColor(bg);
                }
                row[1].setHorizontalAlignment(Element.ALIGN_LEFT); // Align name to left
                row[1].setPaddingLeft(10);
                
                for (PdfPCell cell : row) tbl.addCell(cell);
            }
        }
        
        doc.add(tbl);
    }

    private static void cellVerticalSpan(PdfPTable tbl, String text, Font font, BaseColor bg, BaseColor border, int colSpan, int rowSpan) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        if (bg != null) c.setBackgroundColor(bg);
        if (border != null) c.setBorderColor(border);
        c.setColspan(colSpan);
        c.setRowspan(rowSpan);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4);
        c.setRotation(90);
        c.setMinimumHeight(80f);
        tbl.addCell(c);
    }

    private static void addGunapattrakContent(Document doc, Context ctx, School school, ClassModel cls, Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {

        // Title
        Paragraph title = new Paragraph("सातत्यपूर्ण सर्वंकष मूल्यमापन", new Font(sMarathiBase, 18, Font.BOLD, C_DARK));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        doc.add(title);

        // Header Info table
        PdfPTable headerTbl = new PdfPTable(3);
        headerTbl.setWidthPercentage(100);
        headerTbl.setWidths(new float[]{1.5f, 1f, 1f});

        // Row 1
        PdfPCell c1 = new PdfPCell(new Phrase("नाव: " + (student != null ? nvl(student.name) : ""), fBold)); c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(" ", fBold)); c2.setBorder(Rectangle.NO_BORDER); // Empty middle
        PdfPCell c3 = new PdfPCell(new Phrase("सन : " + (cls != null ? nvl(cls.academicYearLabel) : "2025-26"), fBold)); c3.setBorder(Rectangle.NO_BORDER); c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c1); headerTbl.addCell(c2); headerTbl.addCell(c3);

        // Row 2
        String termLabel = sem1 != null ? "प्रथम सत्र" : "द्वितीय सत्र";
        PdfPCell c4 = new PdfPCell(new Phrase("इयत्ता: " + (cls != null ? nvl(cls.className) : "") + ", तुकडी: " + (cls != null ? nvl(cls.division) : "-"), fBold)); c4.setBorder(Rectangle.NO_BORDER);
        PdfPCell c5 = new PdfPCell(new Phrase("रोल नं.: " + (student != null ? nvl(student.rollNo) : ""), fBold)); c5.setBorder(Rectangle.NO_BORDER); c5.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell c6 = new PdfPCell(new Phrase(termLabel, fBold)); c6.setBorder(Rectangle.NO_BORDER); c6.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c4); headerTbl.addCell(c5); headerTbl.addCell(c6);
        
        headerTbl.setSpacingAfter(10);
        doc.add(headerTbl);

        // Marks table (19 columns)
        float[] widths = {0.6f, 1.8f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.8f, 0.7f, 0.7f, 0.7f, 0.8f, 0.8f, 0.8f, 0.8f};
        PdfPTable tbl = new PdfPTable(widths);
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(6);
        tbl.setSpacingAfter(4);

        // Row 1
        cellSpan(tbl, "अ.नं", fSmallBold, C_HEADER_BG, C_DARK, 1, 3, Element.ALIGN_CENTER);
        cellSpan(tbl, "तपशील", fSmallBold, C_HEADER_BG, C_DARK, 2, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, "आकारिक (अ)", fSmallBold, C_HEADER_BG, C_DARK, 9, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, "संकलित (ब)", fSmallBold, C_HEADER_BG, C_DARK, 4, 1, Element.ALIGN_CENTER);
        cellVerticalSpan(tbl, "अ+ब", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        cellVerticalSpan(tbl, "श्रे.गुण", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        cellVerticalSpan(tbl, "श्रेणी", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);

        // Row 2
        cellSpan(tbl, "विषय", fSmallBold, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
        cellSpan(tbl, "गुण", fSmallBold, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
        
        String[] formatives = {"निरीक्षण", "तोंडीकाम", "प्रात्यक्षिक", "उपक्रम", "प्रकल्प", "चाचणी", "स्वाध्याय", "इतर", "एकूण"};
        for (String f : formatives) cellVerticalSpan(tbl, f, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        
        String[] summatives = {"तोंडी", "प्रात्य.", "लेखी", "एकूण"};
        for (String s : summatives) cellVerticalSpan(tbl, s, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);

        // Row 3
        String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8"};
        for (String n : numbers) cellSpan(tbl, n, fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        cellSpan(tbl, " ", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER); // Formative total empty
        for (int i=0; i<4; i++) cellSpan(tbl, " ", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER); // Summative empty

                MarksRecord rec = sem1 != null ? sem1 : sem2;
                List<Subject> subjects = cls.subjects;
                boolean alt = false;
                for (int i = 0; i < (subjects != null ? subjects.size() : 0); i++) {
                    Subject sub = subjects.get(i);
                    BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                    MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);

                    int formativeMax = sub.maxNirikhshan + sub.maxTondiKam + sub.maxPratyakshik + sub.maxUpkram + sub.maxPrakalp + sub.maxChachani + sub.maxSwadhyay + sub.maxItar;
                    if (formativeMax == 0) formativeMax = sub.maxMarks / 2;
                    int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
                    boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);

                    // Row A: प्राप्त
                    cellSpan(tbl, String.valueOf(i + 1), fSmall, bg, C_DARK, 1, 2, Element.ALIGN_CENTER);
                    cellSpan(tbl, sub.name, fSmall, bg, C_DARK, 1, 2, Element.ALIGN_LEFT);
                    cellSpan(tbl, "प्राप्त", fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    
                    if (d != null) {
                        cellSpan(tbl, strZero(d.nirikhshan), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, strZero(d.tondiKam), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, strZero(d.pratyakshik), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, strZero(d.upkram), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, strZero(d.prakalp), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, strZero(d.chachani), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, strZero(d.swadhyay), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, strZero(d.itar), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, str(d.akarikTotal), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        
                        if (isNonAcademic) {
                            cellSpan(tbl, " ", fSmall, bg, C_DARK, 4, 2, Element.ALIGN_CENTER); // Merge summative columns and both rows
                        } else {
                            cellSpan(tbl, strZero(d.tondi), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                            cellSpan(tbl, strZero(d.pratyakshikB), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                            cellSpan(tbl, strZero(d.lekhi), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                            cellSpan(tbl, str(d.sanklit), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        }
                        
                        cellSpan(tbl, str(d.grandTotal), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, str(d.grandTotal), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER); // Grade marks is just grand total
                        cellSpan(tbl, nvl(d.grade), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    } else {
                        for (int k = 0; k < 9; k++) cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        if (isNonAcademic) {
                            cellSpan(tbl, " ", fSmall, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                        } else {
                            for (int k = 0; k < 4; k++) cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        }
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    }

                    // Row B: पैकी
                    BaseColor paikiBg = new BaseColor(245, 245, 245); // Light grey shading for "पैकी" row
                    cellSpan(tbl, "पैकी", fSmallBold, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(sub.maxNirikhshan), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(sub.maxTondiKam), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(sub.maxPratyakshik), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(sub.maxUpkram), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(sub.maxPrakalp), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(sub.maxChachani), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(sub.maxSwadhyay), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(sub.maxItar), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, str(formativeMax), fSmallBold, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    
                    if (!isNonAcademic) {
                        cellSpan(tbl, strZero(sub.maxTondi), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, strZero(sub.maxPratyakshikB), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, strZero(sub.maxLekhi), fSmall, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, str(summativeMax == 0 ? sub.maxMarks / 2 : summativeMax), fSmallBold, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    }
                    
                    cellSpan(tbl, str(sub.maxMarks), fSmallBold, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, " ", fSmallBold, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER); // Empty bottom row for Grade Marks
                    cellSpan(tbl, " ", fSmallBold, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER); // Empty bottom row for Grade
                }
                doc.add(tbl);
                
                // Signature Row at bottom
                PdfPTable sigTbl = new PdfPTable(3);
                sigTbl.setWidthPercentage(100);
                sigTbl.setSpacingBefore(40);
                
                PdfPCell cSig1 = new PdfPCell(new Phrase("वर्गशिक्षक स्वाक्षरी\n" + (cls != null ? nvl(cls.teacherName) : ""), fSmallBold));
                cSig1.setBorder(Rectangle.NO_BORDER); cSig1.setHorizontalAlignment(Element.ALIGN_CENTER);
                
                PdfPCell cSig2 = new PdfPCell(new Phrase(" ", fSmallBold));
                cSig2.setBorder(Rectangle.NO_BORDER); cSig2.setHorizontalAlignment(Element.ALIGN_CENTER);
                
                PdfPCell cSig3 = new PdfPCell(new Phrase("मुख्याध्यापक स्वाक्षरी\n" + (school != null ? nvl(school.principalName) : ""), fSmallBold));
                cSig3.setBorder(Rectangle.NO_BORDER); cSig3.setHorizontalAlignment(Element.ALIGN_CENTER);
                
                sigTbl.addCell(cSig1); sigTbl.addCell(cSig2); sigTbl.addCell(cSig3);
                doc.add(sigTbl);
    }

    private static void addDescriptiveContent(Document doc, Context ctx, School school, ClassModel cls, Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {


        // Title
        Paragraph title = new Paragraph("सातत्यपूर्ण सर्वंकष मूल्यमापन", new Font(sMarathiBase, 18, Font.BOLD, C_DARK));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        doc.add(title);

        // Header Info table (Matching Gunapattrak exactly)
        PdfPTable headerTbl = new PdfPTable(3);
        headerTbl.setWidthPercentage(100);
        headerTbl.setWidths(new float[]{1.5f, 1f, 1f});

        PdfPCell c1 = new PdfPCell(new Phrase("नाव: " + (student != null ? nvl(student.name) : ""), fBold)); c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(" ", fBold)); c2.setBorder(Rectangle.NO_BORDER);
        PdfPCell c3 = new PdfPCell(new Phrase("सन : " + (cls != null ? nvl(cls.academicYearLabel) : "2025-26"), fBold)); c3.setBorder(Rectangle.NO_BORDER); c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c1); headerTbl.addCell(c2); headerTbl.addCell(c3);

        String termLabel = sem1 != null ? "प्रथम सत्र" : "द्वितीय सत्र";
        PdfPCell c4 = new PdfPCell(new Phrase("इयत्ता: " + (cls != null ? nvl(cls.className) : "") + ", तुकडी: " + (cls != null ? nvl(cls.division) : "-"), fBold)); c4.setBorder(Rectangle.NO_BORDER);
        PdfPCell c5 = new PdfPCell(new Phrase("रोल नं.: " + (student != null ? nvl(student.rollNo) : ""), fBold)); c5.setBorder(Rectangle.NO_BORDER); c5.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell c6 = new PdfPCell(new Phrase(termLabel, fBold)); c6.setBorder(Rectangle.NO_BORDER); c6.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c4); headerTbl.addCell(c5); headerTbl.addCell(c6);
        headerTbl.setSpacingAfter(10);
        doc.add(headerTbl);

                // अ.नं | विषय | विषयवार वर्णनात्मक नोंद
                PdfPTable tbl = new PdfPTable(new float[]{0.6f, 2.5f, 6.0f});
                tbl.setWidthPercentage(100); tbl.setSpacingBefore(6);

                cell(tbl, "अ.नं",  fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                cell(tbl, "विषय",  fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                cell(tbl, "विषयवार वर्णनात्मक नोंद",   fBold, C_HEADER_BG, C_DARK,  1, Element.ALIGN_CENTER);

                String[] labels = subjectLabels(cls);
                boolean alt = false;
                for (int i = 0; i < labels.length; i++) {
                    BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                    String remark = "";
                    if (cls.subjects != null && i < cls.subjects.size()) {
                        String sn = cls.subjects.get(i).name;
                        MarksRecord.SubjectMarksDetail d = detail(sem1 != null ? sem1 : sem2, sn);
                        if (d != null && d.remark != null) {
                            remark = d.remark.replace("||", ", ");
                        }
                    }
                    PdfPCell nc = rawCell(String.valueOf(i + 1), fNormal, bg, C_DARK, Element.ALIGN_CENTER); tbl.addCell(nc);
                    PdfPCell lc = rawCell(labels[i], fNormal, bg, C_DARK, Element.ALIGN_LEFT); lc.setMinimumHeight(28f); tbl.addCell(lc);
                    PdfPCell rc = rawCell(remark, fNormal, bg, C_DARK, Element.ALIGN_LEFT); rc.setMinimumHeight(28f); tbl.addCell(rc);
                }
                doc.add(tbl);
                doc.add(buildSignatureRow(school, cls));
                
    }

    private static void addPersonalityContent(Document doc, Context ctx, School school, ClassModel cls, Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {


        // Title
        Paragraph title = new Paragraph("सातत्यपूर्ण सर्वंकष मूल्यमापन", new Font(sMarathiBase, 18, Font.BOLD, C_DARK));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        doc.add(title);

        PdfPTable headerTbl = new PdfPTable(3);
        headerTbl.setWidthPercentage(100);
        headerTbl.setWidths(new float[]{1.5f, 1f, 1f});

        PdfPCell c1 = new PdfPCell(new Phrase("नाव: " + (student != null ? nvl(student.name) : ""), fBold)); c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(" ", fBold)); c2.setBorder(Rectangle.NO_BORDER);
        PdfPCell c3 = new PdfPCell(new Phrase("सन : " + (cls != null ? nvl(cls.academicYearLabel) : "2025-26"), fBold)); c3.setBorder(Rectangle.NO_BORDER); c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c1); headerTbl.addCell(c2); headerTbl.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase("इयत्ता: " + (cls != null ? nvl(cls.className) : "") + ", तुकडी: " + (cls != null ? nvl(cls.division) : "-"), fBold)); c4.setBorder(Rectangle.NO_BORDER);
        PdfPCell c5 = new PdfPCell(new Phrase("रोल नं.: " + (student != null ? nvl(student.rollNo) : ""), fBold)); c5.setBorder(Rectangle.NO_BORDER); c5.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell c6 = new PdfPCell(new Phrase(" ", fBold)); c6.setBorder(Rectangle.NO_BORDER); c6.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c4); headerTbl.addCell(c5); headerTbl.addCell(c6);
        headerTbl.setSpacingAfter(10);
        doc.add(headerTbl);

                String[] rowLabels = {
                        "आवड, छंद कला, क्रीडा, साहित्य इ.",
                        "सुधारणा आवश्यक",
                        "विशेष प्रगती",
                        "व्यक्तिमत्व गुण विशेष\n(अभिवृत्ती, कल, मूल्ये, स्वभाव गुणविशेष)"
                };
                String[] keys = {"आवड", "सुधारणा", "विशेष", "व्यक्तिमत्व"};

                PdfPTable tbl = new PdfPTable(new float[]{0.5f, 2.5f, 3.2f, 3.2f});
                tbl.setWidthPercentage(100); tbl.setSpacingBefore(6);

                cell(tbl, "अ.नं",   fBold, C_PRIMARY,       C_WHITE, 1, Element.ALIGN_CENTER);
                cell(tbl, "तपशील",  fBold, C_PRIMARY,       C_WHITE, 1, Element.ALIGN_CENTER);
                cell(tbl, "प्रथम सत्र",   fBold, C_PRIMARY_LIGHT, C_DARK,  1, Element.ALIGN_CENTER);
                cell(tbl, "द्वितीय सत्र", fBold, C_PRIMARY,       C_WHITE, 1, Element.ALIGN_CENTER);

                boolean alt = false;
                for (int i = 0; i < rowLabels.length; i++) {
                    BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                    String v1 = remarkContaining(sem1, keys[i]);
                    String v2 = remarkContaining(sem2, keys[i]);
                    PdfPCell nc = rawCell(str(i + 1), fNormal, bg, C_DARK, Element.ALIGN_CENTER); tbl.addCell(nc);
                    PdfPCell lc = rawCell(rowLabels[i], fSmall, bg, C_DARK, Element.ALIGN_LEFT); lc.setMinimumHeight(36f); tbl.addCell(lc);
                    PdfPCell r1 = rawCell(v1, fNormal, bg, C_DARK, Element.ALIGN_LEFT); r1.setMinimumHeight(36f); tbl.addCell(r1);
                    PdfPCell r2 = rawCell(v2, fNormal, bg, C_DARK, Element.ALIGN_LEFT); r2.setMinimumHeight(36f); tbl.addCell(r2);
                }
                doc.add(tbl);
                doc.add(buildMarksSummaryBox(sem1, sem2));
                doc.add(buildSignatureRow(school, cls));
                
    }


    // ═════════════════════════════════════════════════════════════════════════
    //  SHARED BLOCKS
    // ═════════════════════════════════════════════════════════════════════════

    private static PdfPTable buildAppHeader(Context ctx) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);

        try {
            android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(ctx, com.kartik.myschool.R.drawable.ic_school);
            if (d != null) {
                int w = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : 100;
                int h = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 100;
                android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                d.draw(canvas);

                java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(stream.toByteArray());
                img.setAlignment(Element.ALIGN_CENTER);
                img.scaleToFit(50, 50);
                c.addElement(img);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Paragraph p = new Paragraph("MySchool", colored(fTitle, C_DARK));
        p.setAlignment(Element.ALIGN_CENTER);
        c.addElement(p);
        t.addCell(c);
        return t;
    }

    private static PdfPTable buildSchoolHeader(School school, ClassModel cls) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100); t.setSpacingAfter(4);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(C_PRIMARY); c.setPadding(10); c.setBorder(Rectangle.NO_BORDER);
        Paragraph p = new Paragraph();
        String name = (school != null && school.name != null) ? school.name.toUpperCase() : "SCHOOL";
        p.add(new Phrase(name + "\n", colored(fTitle, C_WHITE)));
        String udise = (school != null && school.udiseCode != null) ? "युडायस: " + school.udiseCode : "";
        String year  = (cls != null && cls.academicYearLabel != null) ? "  सन: " + cls.academicYearLabel : "";
        p.add(new Phrase(udise + year, colored(fTitleSub, new BaseColor(255,255,255,200))));
        c.addElement(p); t.addCell(c);
        return t;
    }

    private static PdfPTable buildStudentInfoTable(School school, ClassModel cls, Student student)
            throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1f, 2f, 1f, 2f});
        t.setWidthPercentage(100); t.setSpacingAfter(4);
        infoRow4(t, "नाव:", nvl(student.name), "रोल नं.:", nvl(student.rollNo));
        infoRow4(t, "इयत्ता:", cls != null ? nvl(cls.className) : "—",
                    "वर्गशिक्षक:", cls != null ? nvl(cls.teacherName) : "—");
        infoRow4(t, "तुकडी:", cls != null ? nvl(cls.division) : "—",
                    "जन्मतारीख:", nvl(student.dob));
        return t;
    }

    private static void infoRow4(PdfPTable t, String l1, String v1, String l2, String v2) {
        for (String[] p : new String[][]{{l1, v1}, {l2, v2}}) {
            PdfPCell lc = rawCell(p[0], fSmall, C_WHITE, C_GREY, Element.ALIGN_LEFT);
            lc.setBorder(Rectangle.BOTTOM); lc.setBorderColor(C_BORDER); lc.setPadding(3);
            PdfPCell vc = rawCell(p[1], fNormal, C_WHITE, C_DARK, Element.ALIGN_LEFT);
            vc.setBorder(Rectangle.BOTTOM); vc.setBorderColor(C_BORDER); vc.setPadding(3);
            t.addCell(lc); t.addCell(vc);
        }
    }

    private static PdfPTable buildAttendanceRow(MarksRecord s1, MarksRecord s2) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{2f,0.8f,0.6f, 0.2f, 2f,0.8f,0.6f});
        t.setWidthPercentage(100); t.setSpacingBefore(4); t.setSpacingAfter(4);
        cell(t, "प्रथम सत्र — हजर दिवस:", fSmall,  C_HEADER_BG, C_DARK, 1, Element.ALIGN_LEFT);
        cell(t, s1!=null?str(s1.presentDays):"—",   fBold,   C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, s1!=null?"/ "+s1.totalDays:"",       fSmall,  C_HEADER_BG, C_GREY, 1, Element.ALIGN_LEFT);
        cell(t, "",                                  fSmall,  C_WHITE,     C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, "द्वितीय सत्र — हजर दिवस:",         fSmall,  C_HEADER_BG, C_DARK, 1, Element.ALIGN_LEFT);
        cell(t, s2!=null?str(s2.presentDays):"—",   fBold,   C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, s2!=null?"/ "+s2.totalDays:"",       fSmall,  C_HEADER_BG, C_GREY, 1, Element.ALIGN_LEFT);
        return t;
    }

    private static PdfPTable buildSignatureRow(School school, ClassModel cls) throws DocumentException {
        PdfPTable t = new PdfPTable(2); t.setWidthPercentage(100); t.setSpacingBefore(28);
        String teacher   = cls!=null && cls.teacherName!=null ? cls.teacherName : "वर्गशिक्षक";
        String principal = school!=null && school.principalName!=null ? school.principalName : "मुख्याध्यापक";
        PdfPCell s1 = rawCell("वर्गशिक्षक स्वाक्षरी\n" + teacher, fSmall, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        s1.setBorderWidthTop(0.5f); s1.setBorderColorTop(C_DARK); s1.setPaddingTop(6);
        PdfPCell s2 = rawCell("मुख्याध्यापक स्वाक्षरी\n" + principal, fSmall, C_WHITE, C_DARK, Element.ALIGN_RIGHT);
        s2.setBorderWidthTop(0.5f); s2.setBorderColorTop(C_DARK); s2.setPaddingTop(6);
        t.addCell(s1); t.addCell(s2);
        return t;
    }

    private static PdfPTable buildProgressHeader(School school, ClassModel cls, String subName)
            throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{3f, 1f});
        t.setWidthPercentage(100); t.setSpacingAfter(3);
        String sName = school!=null && school.name!=null ? school.name : "School";
        String yr    = cls!=null && cls.academicYearLabel!=null ? cls.academicYearLabel : "";
        String cInfo = cls!=null ? "इयत्ता: " + nvl(cls.className) + "  तुकडी: " + nvl(cls.division) : "";
        Paragraph lp = new Paragraph();
        lp.add(new Phrase(sName + "\n", colored(fBold, C_DARK)));
        lp.add(new Phrase(cInfo + "  |  विषय: " + subName, colored(fSmall, C_GREY)));
        PdfPCell lc = new PdfPCell(); lc.addElement(lp);
        lc.setBorder(Rectangle.BOTTOM); lc.setBorderColor(C_PRIMARY); lc.setPadding(4);
        t.addCell(lc);
        Paragraph rp = new Paragraph();
        rp.add(new Phrase("सन: " + yr + "\nप्रथम सत्र", colored(fSmall, C_GREY)));
        PdfPCell rc = new PdfPCell(); rc.addElement(rp);
        rc.setBorder(Rectangle.BOTTOM); rc.setBorderColor(C_PRIMARY); rc.setPadding(4);
        rc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(rc);
        return t;
    }

    private static PdfPTable buildSummaryTable(ClassModel cls, List<Student> students,
                                               Map<String, MarksRecord> s1, Map<String, MarksRecord> s2)
            throws DocumentException {
        int sc = cls.subjects != null ? cls.subjects.size() : 0;
        float[] widths = new float[2 + sc * 2 + 2];
        widths[0] = 0.4f; widths[1] = 1.8f;
        for (int i = 0; i < sc * 2; i++) widths[2 + i] = 0.55f;
        widths[widths.length - 2] = 0.65f; widths[widths.length - 1] = 0.65f;

        PdfPTable t = new PdfPTable(widths); t.setWidthPercentage(100);
        cell(t, "अ.नं", fSmallBold, C_PRIMARY, C_WHITE, 1, Element.ALIGN_CENTER);
        cell(t, "नाव",  fSmallBold, C_PRIMARY, C_WHITE, 1, Element.ALIGN_CENTER);
        if (cls.subjects != null) {
            for (Subject sub : cls.subjects) {
                cell(t, sub.name+"\nस1", fSmallBold, C_PRIMARY_LIGHT, C_DARK,  1, Element.ALIGN_CENTER);
                cell(t, sub.name+"\nस2", fSmallBold, C_PRIMARY,       C_WHITE, 1, Element.ALIGN_CENTER);
            }
        }
        cell(t, "एकूण\nस1", fSmallBold, C_PRIMARY_LIGHT, C_DARK,  1, Element.ALIGN_CENTER);
        cell(t, "एकूण\nस2", fSmallBold, C_PRIMARY,       C_WHITE, 1, Element.ALIGN_CENTER);

        boolean alt = false;
        for (int i = 0; i < students.size(); i++) {
            Student st = students.get(i);
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
            MarksRecord r1 = s1 != null ? s1.get(st.id) : null;
            MarksRecord r2 = s2 != null ? s2.get(st.id) : null;
            cell(t, str(i + 1),   fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
            cell(t, nvl(st.name), fSmall, bg, C_DARK, 1, Element.ALIGN_LEFT);
            if (cls.subjects != null) {
                for (Subject sub : cls.subjects) {
                    MarksRecord.SubjectMarksDetail d1 = detail(r1, sub.name);
                    MarksRecord.SubjectMarksDetail d2 = detail(r2, sub.name);
                    cell(t, d1!=null?str(d1.grandTotal):"—", fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
                    cell(t, d2!=null?str(d2.grandTotal):"—", fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
                }
            }
            cell(t, r1!=null?fmt(r1.totalObtained):"—", fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
            cell(t, r2!=null?fmt(r2.totalObtained):"—", fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
        }
        return t;
    }

    private static PdfPTable buildMarksSummaryBox(MarksRecord s1, MarksRecord s2) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1.8f,0.8f,0.6f, 0.3f, 1.8f,0.8f,0.6f});
        t.setWidthPercentage(100); t.setSpacingBefore(8);
        cell(t, "प्रथम सत्र — एकूण:", fSmall, C_HEADER_BG, C_DARK, 1, Element.ALIGN_LEFT);
        cell(t, s1!=null?fmt(s1.totalObtained)+"/"+s1.totalMax:"—", fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, s1!=null?nvl(s1.grade):"—",                          fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, "",                                                   fSmall,C_WHITE,     C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, "द्वितीय सत्र — एकूण:", fSmall, C_HEADER_BG, C_DARK, 1, Element.ALIGN_LEFT);
        cell(t, s2!=null?fmt(s2.totalObtained)+"/"+s2.totalMax:"—", fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, s2!=null?nvl(s2.grade):"—",                          fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        return t;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CELL & FONT HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private static void cell(PdfPTable t, String text, Font font, BaseColor bg,
                             BaseColor textColor, int colspan, int align) {
        PdfPCell c = rawCell(text, font, bg, textColor, align);
        c.setColspan(colspan);
        t.addCell(c);
    }

    private static void groupCell(PdfPTable t, String text, BaseColor bg, BaseColor textColor, int colspan) {
        PdfPCell c = rawCell(text, fBold, bg, textColor, Element.ALIGN_CENTER);
        c.setColspan(colspan);
        t.addCell(c);
    }

    private static PdfPCell rawCell(String text, Font font, BaseColor bg, BaseColor textColor, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, colored(font, textColor)));
        c.setBackgroundColor(bg);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(4);
        c.setHorizontalAlignment(align);
        return c;
    }

    private static void cellSpan(PdfPTable t, String text, Font font, BaseColor bg, BaseColor textColor, int colspan, int rowspan, int align) {
        PdfPCell c = rawCell(text, font, bg, textColor, align);
        if (colspan > 1) c.setColspan(colspan);
        if (rowspan > 1) c.setRowspan(rowspan);
        t.addCell(c);
    }

    private static String calculatePercentageString(int obtained, int max) {
        if (max == 0) return "-";
        double p = (obtained * 100.0) / max;
        return fmt(p);
    }

    private static Font colored(Font src, BaseColor color) {
        Font f = new Font(src); f.setColor(color); return f;
    }

    private static Paragraph para(String text, Font font) {
        return new Paragraph(text, font);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DATA HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private static MarksRecord.SubjectMarksDetail detail(MarksRecord rec, String subName) {
        if (rec == null || rec.detailedMarks == null) return null;
        return rec.detailedMarks.get(MarksRecord.sanitizeKey(subName));
    }

    private static String akarikMax(Subject sub) { return str(sub.maxMarks / 2); }
    private static String sanklitMax(Subject sub) { return str(sub.maxMarks - sub.maxMarks / 2); }

    private static int sumAkarik(MarksRecord rec, List<Subject> subs) {
        int s = 0;
        if (rec == null || rec.detailedMarks == null) return s;
        for (Subject sub : subs) { MarksRecord.SubjectMarksDetail d = rec.detailedMarks.get(MarksRecord.sanitizeKey(sub.name)); if (d!=null) s += d.akarikTotal; }
        return s;
    }
    private static int sumSanklit(MarksRecord rec, List<Subject> subs) {
        int s = 0;
        if (rec == null || rec.detailedMarks == null) return s;
        for (Subject sub : subs) { MarksRecord.SubjectMarksDetail d = rec.detailedMarks.get(MarksRecord.sanitizeKey(sub.name)); if (d!=null) s += d.sanklit; }
        return s;
    }

    private static String remarkContaining(MarksRecord rec, String keyword) {
        if (rec == null || rec.detailedMarks == null) return "";
        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> e : rec.detailedMarks.entrySet()) {
            if (e.getKey() != null && e.getKey().contains(keyword) && e.getValue() != null)
                return nvl(e.getValue().remark);
        }
        return "";
    }

    private static String[] subjectLabels(ClassModel cls) {
        if (cls.subjects != null && !cls.subjects.isEmpty()) {
            String[] l = new String[cls.subjects.size()];
            for (int i = 0; i < l.length; i++) l[i] = cls.subjects.get(i).name;
            return l;
        }
        return new String[]{"प्रथम भाषा: मराठी","द्वितीय भाषा: हिंदी","तृतीय भाषा: इंग्रजी",
                "गणित","सामान्य विज्ञान / परिसर अभ्यास 1","सामाजिक शास्त्रे / परिसर अभ्यास 2",
                "कला","कार्यानुभव","शारीरिक शिक्षण व आरोग्य","विशेष प्रगती",
                "आवड, छंद कला, क्रीडा","सुधारणा आवश्यक","व्यक्तिमत्व गुण विशेष"};
    }

    private static String strZero(int v)  { return v > 0 ? String.valueOf(v) : "-"; }
    private static String nvl(String s)   { return s != null && !s.isEmpty() ? s : "-"; }
    private static String str(int v)      { return String.valueOf(v); }
    private static String str(double v)   { return fmt(v); }
    private static String fmt(double v)   { return v==Math.floor(v)?str((int)v):String.format(Locale.getDefault(),"%.1f",v); }
    private static String safeRoll(Student s) { return s!=null&&s.rollNo!=null?s.rollNo:"0"; }
}
