package com.kartik.myschool.utils;

import static com.kartik.myschool.utils.pdf.GunapattrakGenerator.cellVerticalSpan;
import static com.kartik.myschool.utils.pdf.GunapattrakGenerator.cellHorizontalImageSpan;

import android.content.Context;
import com.kartik.myschool.utils.pdf.PdfLocalizer;

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
 * Generates all 4 Edu Report CCE report PDFs:
 * 1. generateGunapattrak — per-student marks sheet (both semesters)
 * 2. generateDescriptive — per-student descriptive remarks
 * 3. generateProgressBook — whole-class landscape table
 * 4. generatePersonalityRecord — compact personality record
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

    private static void triggerVibration(Context ctx) {
        if (ctx == null) return;
        try {
            android.os.Vibrator v = (android.os.Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(50);
                }
            }
        } catch (Exception ignored) {}
    }

    // ── Colours ───────────────────────────────────────────────────────────────
    public static final BaseColor C_PRIMARY = new BaseColor(21, 101, 192);
    public static final BaseColor C_PRIMARY_LIGHT = new BaseColor(187, 222, 251);
    public static final BaseColor C_HEADER_BG = new BaseColor(236, 239, 241);
    public static final BaseColor C_ROW_ALT = new BaseColor(245, 247, 250);
    public static final BaseColor C_WHITE = BaseColor.WHITE;
    public static final BaseColor C_DARK = new BaseColor(28, 27, 31);
    public static final BaseColor C_GREY = new BaseColor(117, 117, 117);
    public static final BaseColor C_BORDER = new BaseColor(224, 224, 224);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    public static BaseFont sMarathiBase;
    public static android.graphics.Typeface sMarathiTypeface;
    public static boolean sFontsInitDone = false;

    public static Font fTitle, fTitleSub, fHeader, fNormal, fSmall, fMicro, fBold, fSmallBold;

    public static synchronized void ensureFonts(Context ctx) {
        if (sMarathiBase != null)
            return;

        // ── Step 1: Load Android Typeface for bitmap rendering (MarathiText) ──
        // NotoSansDevanagari has the broadest Unicode Devanagari coverage and
        // renders ALL matras/conjuncts correctly via Android's Harfbuzz engine.
        try {
            File notoFile = new File(ctx.getFilesDir(), "noto_dev.ttf");
            InputStream nis = ctx.getAssets().open("fonts/NotoSansDevanagari-Regular.ttf");
            FileOutputStream nos = new FileOutputStream(notoFile);
            byte[] buf2 = new byte[4096];
            int len2;
            while ((len2 = nis.read(buf2)) > 0)
                nos.write(buf2, 0, len2);
            nis.close();
            nos.close();
            sMarathiTypeface = android.graphics.Typeface.createFromFile(notoFile);
            android.util.Log.d("PDF_FONT", "Loaded NotoSansDevanagari Typeface for rendering");
        } catch (Exception e) {
            android.util.Log.w("PDF_FONT", "NotoSans load failed, will use system typeface", e);
        }

        // ── Step 2: Load iText BaseFont (TiroDevanagariHindi) for embedded PDF font ──
        // This is used for numbers, ASCII and fallback text in iText Phrases.
        try {
            File fontFile = new File(ctx.getFilesDir(), "tiro_dev.ttf");
            InputStream is = ctx.getAssets().open("fonts/TiroDevanagariHindi-Regular.ttf");
            FileOutputStream os = new FileOutputStream(fontFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0)
                os.write(buf, 0, len);
            is.close();
            os.close();
            sMarathiBase = BaseFont.createFont(fontFile.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            android.util.Log.d("PDF_FONT", "Loaded TiroDevanagariHindi iText BaseFont");
        } catch (Exception e) {
            android.util.Log.e("PDF_FONT", "TiroDevanagari load failed, trying system fonts", e);
            // Fallback: Try system Devanagari fonts for BaseFont
            String[] systemFonts = {
                    "/system/fonts/NotoSansDevanagari-Regular.ttf",
                    "/system/fonts/NotoSansDevanagari-UI-Regular.ttf",
                    "/system/fonts/DroidSansDevanagari-Regular.ttf",
                    "/system/fonts/DroidSansFallback.ttf"
            };
            for (String sysFont : systemFonts) {
                try {
                    if (new File(sysFont).exists()) {
                        sMarathiBase = BaseFont.createFont(sysFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                        if (sMarathiTypeface == null)
                            sMarathiTypeface = android.graphics.Typeface.createFromFile(sysFont);
                        android.util.Log.d("PDF_FONT", "Loaded system font: " + sysFont);
                        break;
                    }
                } catch (Exception ignored) {
                    android.util.Log.e("PDF_FONT", "Failed system font: " + sysFont);
                }
            }
        }
        buildFonts();
    }

    private static void buildFonts() {
        if (sMarathiBase != null) {
            fTitle = new Font(sMarathiBase, 13, Font.BOLD);
            fTitleSub = new Font(sMarathiBase, 9, Font.NORMAL);
            fHeader = new Font(sMarathiBase, 11, Font.BOLD);
            fNormal = new Font(sMarathiBase, 10, Font.NORMAL);
            fSmall = new Font(sMarathiBase, 9, Font.NORMAL);
            fMicro = new Font(sMarathiBase, 7, Font.NORMAL);
            fBold = new Font(sMarathiBase, 10, Font.BOLD);
            fSmallBold = new Font(sMarathiBase, 8, Font.BOLD);
        } else {
            fTitle = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);
            fTitleSub = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            fHeader = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            fNormal = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            fSmall = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            fMicro = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL);
            fBold = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            fSmallBold = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
        }
    }

    public static Phrase createMixedPhrase(String text, Font baseFont) {
        Phrase phrase = new Phrase();
        Font englishFont = new Font(Font.FontFamily.HELVETICA, baseFont.getSize(), baseFont.getStyle(), baseFont.getColor());
        
        StringBuilder currentText = new StringBuilder();
        boolean currentIsEnglish = true;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isEnglish = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '-' || c == '/' || c == ':' || c == '.' || c == ',';
            if (c == ' ') {
                isEnglish = currentIsEnglish;
            }
            
            if (i == 0) {
                currentIsEnglish = isEnglish;
            } else if (currentIsEnglish != isEnglish) {
                phrase.add(new com.itextpdf.text.Chunk(currentText.toString(), currentIsEnglish ? englishFont : baseFont));
                currentText.setLength(0);
                currentIsEnglish = isEnglish;
            }
            currentText.append(c);
        }
        if (currentText.length() > 0) {
            phrase.add(new com.itextpdf.text.Chunk(currentText.toString(), currentIsEnglish ? englishFont : baseFont));
        }
        return phrase;
    }

    // ── Output dir ────────────────────────────────────────────────────────────
    public static File outDir(Context ctx) {
        File d = new File(ctx.getExternalFilesDir(null), "myschool_reports");
        if (!d.exists())
            d.mkdirs();
        return d;
    }

    public static String ts() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    public static void syncClassYear(ClassModel cls) {
        if (cls != null && com.kartik.myschool.SessionContext.selectedYear != null && com.kartik.myschool.SessionContext.selectedYear.label != null) {
            cls.academicYearLabel = com.kartik.myschool.SessionContext.selectedYear.label;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BULK COMBINED PDF GENERATOR (One file, Multiple Pages)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generateBulkCombinedPdf(Context ctx,
            School school,
            ClassModel cls,
            java.util.List<Student> students,
            java.util.Map<String, MarksRecord> sem1Map,
            java.util.Map<String, MarksRecord> sem2Map,
            int reportPosition,
            PdfCallback cb) {
        syncClassYear(cls);
        new Thread(() -> {
            com.google.firebase.perf.metrics.Trace pdfTrace = com.google.firebase.perf.FirebasePerformance.getInstance().newTrace("pdf_generation_bulk");
            pdfTrace.putAttribute("report_position", String.valueOf(reportPosition));
            pdfTrace.putAttribute("student_count", String.valueOf(students.size()));
            pdfTrace.start();
            try {
                ensureFonts(ctx);
                String rName;
                if (reportPosition == 2 || reportPosition == 6 || reportPosition == 13)
                    rName = "Gunapattrak";
                else if (reportPosition == 3 || reportPosition == 7)
                    rName = "Descriptive";
                else if (reportPosition == 0)
                    rName = "CoverPage";
                else
                    rName = "Personality";
                File out = new File(outDir(ctx), "Bulk_" + rName + "_" + ts() + ".pdf");
                File tempFile = File.createTempFile("pdf_tmp_", ".pdf", outDir(ctx));
                Document doc = new Document(PageSize.A4);
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(tempFile));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();

                boolean isFirst = true;
                for (Student student : students) {
                    if (!isFirst)
                        doc.newPage();
                    isFirst = false;
                    doc.setMargins(30, 30, 30, 30);
                    MarksRecord s1 = sem1Map != null ? sem1Map.get(student.id) : null;
                    MarksRecord s2 = sem2Map != null ? sem2Map.get(student.id) : null;

                    if (reportPosition == 2 || reportPosition == 6 || reportPosition == 13) {
                        com.kartik.myschool.utils.pdf.GunapattrakGenerator.addGunapattrakContent(doc, ctx, school, cls,
                                student, s1, s2);
                    } else if (reportPosition == 3 || reportPosition == 7) {
                        // Deprecated for bulk generation, handled in Class reports
                    } else if (reportPosition == 0) {
                        com.kartik.myschool.utils.pdf.CoverPageGenerator.addCoverPageContent(doc, ctx, school, cls,
                                student);
                    } else {
                        addPersonalityContent(doc, ctx, school, cls, student, s1, s2);
                    }
                }

                doc.close();
                pdfTrace.stop();
                triggerVibration(ctx);
                if (tempFile.renameTo(out)) {
                    cb.onSuccess(out);
                } else {
                    cb.onError(new java.io.IOException("Failed to rename temp PDF to destination"));
                }
            } catch (Exception e) {
                pdfTrace.stop();
                cb.onError(e);
            }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GRADE CHART (श्रेणी तक्का) — Class-wide report
    // ═════════════════════════════════════════════════════════════════════════
    public static void generateGradeChart(Context ctx,
            School school,
            ClassModel cls,
            List<Student> students,
            Map<String, MarksRecord> marksMap,
            boolean isSem2,
            PdfCallback cb) {
        syncClassYear(cls);
        new Thread(() -> {
            com.google.firebase.perf.metrics.Trace pdfTrace = com.google.firebase.perf.FirebasePerformance.getInstance().newTrace("pdf_generation_grade_chart");
            pdfTrace.putAttribute("student_count", String.valueOf(students.size()));
            pdfTrace.start();
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "GradeChart_" + ts() + ".pdf");
                File tempFile = File.createTempFile("pdf_tmp_", ".pdf", outDir(ctx));
                Document doc = new Document(PageSize.A4);
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(tempFile));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(15, 15, 30, 30); // Narrow margins for 20 columns

                // Title: render through Android shaping so Marathi header text displays
                // correctly.
                addMarathiParagraph(
                        doc,
                        PdfLocalizer.get(ctx,
                                "\u0938\u093e\u0924\u0924\u094d\u092f\u092a\u0942\u0930\u094d\u0923 \u0938\u0930\u094d\u0935\u0902\u0915\u0937 \u092e\u0942\u0932\u094d\u092f\u092e\u093e\u092a\u0928",
                                "Continuous Comprehensive Evaluation"),
                        16f,
                        true,
                        C_DARK,
                        0f,
                        4f);

                // Header Tbl
                PdfPTable hTbl = new PdfPTable(3);
                hTbl.setWidthPercentage(100);
                hTbl.setSpacingBefore(10);
                hTbl.setSpacingAfter(5);

                String lLine1 = "Udise: " + nvl(school.udiseCode);
                String lLine2 = nvl(school.name);
                String term = isSem2
                        ? PdfLocalizer.get(ctx, "\u0926\u094d\u0935\u093f\u0924\u0940\u092f \u0938\u0924\u094d\u0930",
                                "Second Semester")
                        : PdfLocalizer.get(ctx, "\u092a\u094d\u0930\u0925\u092e \u0938\u0924\u094d\u0930",
                                "First Semester");
                String rLine1 = "सन : " + nvl(cls.academicYearLabel);
                String rLine2 = "इयत्ता: " + nvl(cls.className) + ", तुकडी: " + nvl(cls.division);

                PdfPCell cL = new PdfPCell();
                cL.setBorder(Rectangle.NO_BORDER);
                cL.setHorizontalAlignment(Element.ALIGN_LEFT);
                try {
                    int androidColor = android.graphics.Color.rgb(C_DARK.getRed(), C_DARK.getGreen(), C_DARK.getBlue());
                    com.itextpdf.text.Image img1 = com.kartik.myschool.utils.pdf.MarathiText.renderLine(lLine1, 9f, true, androidColor);
                    img1.setAlignment(com.itextpdf.text.Image.LEFT);
                    com.itextpdf.text.Image img2 = com.kartik.myschool.utils.pdf.MarathiText.renderLine(lLine2, 9f, true, androidColor);
                    img2.setAlignment(com.itextpdf.text.Image.LEFT);
                    cL.addElement(img1);
                    cL.addElement(img2);
                } catch (Exception e) {
                    cL.setPhrase(new Phrase(lLine1 + "\n" + lLine2, fSmallBold));
                }

                PdfPCell cC = noBorderMarathiCell(term, 10f, true, C_DARK, Element.ALIGN_CENTER);
                cC.setVerticalAlignment(Element.ALIGN_BOTTOM);

                PdfPCell cR = new PdfPCell();
                cR.setBorder(Rectangle.NO_BORDER);
                cR.setHorizontalAlignment(Element.ALIGN_RIGHT);
                try {
                    int androidColor = android.graphics.Color.rgb(C_DARK.getRed(), C_DARK.getGreen(), C_DARK.getBlue());
                    com.itextpdf.text.Image img1 = com.kartik.myschool.utils.pdf.MarathiText.renderLine(rLine1, 9f, true, androidColor);
                    img1.setAlignment(com.itextpdf.text.Image.RIGHT);
                    com.itextpdf.text.Image img2 = com.kartik.myschool.utils.pdf.MarathiText.renderLine(rLine2, 9f, true, androidColor);
                    img2.setAlignment(com.itextpdf.text.Image.RIGHT);
                    cR.addElement(img1);
                    cR.addElement(img2);
                } catch (Exception e) {
                    cR.setPhrase(new Phrase(rLine1 + "\n" + rLine2, fSmallBold));
                }

                hTbl.addCell(cL);
                hTbl.addCell(cC);
                hTbl.addCell(cR);
                doc.add(hTbl);

                // Main Table
                List<Subject> subjects = cls.subjects != null ? cls.subjects : new ArrayList<>();
                int numSubjects = Math.min(subjects.size(), 9); // Max 9 subjects fit properly on Portrait A4

                int numCols = 2 + (numSubjects * 2);
                float[] widths = new float[numCols];
                widths[0] = 0.5f; // SrNo
                widths[1] = 1.6f; // Name (increased to fit horizontal header)
                for (int i = 0; i < numSubjects; i++) {
                    widths[2 + (i * 2)] = 0.4f; // Marks
                    widths[2 + (i * 2) + 1] = 0.4f; // Grade
                }

                PdfPTable tbl = new PdfPTable(widths);
                tbl.setWidthPercentage(100);

                // Row 1
                com.kartik.myschool.utils.pdf.MarathiText.cell(tbl, "अ.नं", 9, true, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
                com.kartik.myschool.utils.pdf.MarathiText.cell(tbl, com.kartik.myschool.utils.pdf.PdfLocalizer.get(ctx, "विद्यार्थ्याचे नाव", "Student Name"), 9, true, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
                for (int i = 0; i < numSubjects; i++) {
                    cellVerticalSpan(tbl, ctx, PdfLocalizer.translateSubject(ctx, subjects.get(i).name), fSmallBold,
                            C_HEADER_BG, C_DARK, 2, 1);
                }

                // Row 2 (Vertical subheaders)
                for (int i = 0; i < numSubjects; i++) {
                    cellVerticalSpan(tbl, ctx, "गुण", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
                    cellVerticalSpan(tbl, ctx, "श्रेणी", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
                }

                // Data Rows
                boolean alt = false;
                for (int sIdx = 0; sIdx < students.size(); sIdx++) {
                    Student student = students.get(sIdx);
                    BaseColor bg = alt ? C_ROW_ALT : C_WHITE;
                    alt = !alt;

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
                pdfTrace.stop();
                triggerVibration(ctx);
                if (tempFile.renameTo(out)) {
                    cb.onSuccess(out);
                } else {
                    cb.onError(new java.io.IOException("Failed to rename temp PDF to destination"));
                }
            } catch (Exception e) {
                pdfTrace.stop();
                cb.onError(e);
            }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // REPORT 3 — प्रगतीपुस्तक (whole-class, landscape, one page per subject)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generateProgressBook(Context ctx,
            School school,
            ClassModel cls,
            List<Student> students,
            Map<String, MarksRecord> sem1Marks,
            Map<String, MarksRecord> sem2Marks,
            PdfCallback cb) {
        syncClassYear(cls);
        new Thread(() -> {
            com.google.firebase.perf.metrics.Trace pdfTrace = com.google.firebase.perf.FirebasePerformance.getInstance().newTrace("pdf_generation_progress_book");
            pdfTrace.putAttribute("student_count", String.valueOf(students.size()));
            pdfTrace.start();
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "Pragati_" + ts() + ".pdf");
                File tempFile = File.createTempFile("pdf_tmp_", ".pdf", outDir(ctx));
                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(tempFile));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(15, 15, 15, 15);

                int selectedSemNum = 0;
                if (com.kartik.myschool.SessionContext.selectedSemester != null) {
                    selectedSemNum = com.kartik.myschool.SessionContext.selectedSemester.number;
                }

                boolean hasPage = false;
                if (selectedSemNum == 1) {
                    addProgressBookPage(doc, ctx, school, cls, students,
                            sem1Marks != null ? sem1Marks : new java.util.HashMap<>(), "First Semester");
                } else if (selectedSemNum == 2) {
                    addProgressBookPage(doc, ctx, school, cls, students,
                            sem2Marks != null ? sem2Marks : new java.util.HashMap<>(), "Second Semester");
                } else {
                    if (sem1Marks != null && !sem1Marks.isEmpty()) {
                        addProgressBookPage(doc, ctx, school, cls, students, sem1Marks, "First Semester");
                        hasPage = true;
                    }
                    if (sem2Marks != null && !sem2Marks.isEmpty()) {
                        if (hasPage) {
                            doc.newPage();
                        }
                        addProgressBookPage(doc, ctx, school, cls, students, sem2Marks, "Second Semester");
                        hasPage = true;
                    }
                    if (!hasPage) {
                        addProgressBookPage(doc, ctx, school, cls, students, new java.util.HashMap<>(),
                                "First Semester");
                    }
                }

                doc.close();
                pdfTrace.stop();
                triggerVibration(ctx);
                if (tempFile.renameTo(out)) {
                    cb.onSuccess(out);
                } else {
                    cb.onError(new java.io.IOException("Failed to rename temp PDF to destination"));
                }
            } catch (Exception e) {
                pdfTrace.stop();
                cb.onError(e);
            }
        }).start();
    }

    public static String normalizeGrade(String g) {
        if (g == null)
            return "";
        String s = g.trim().toUpperCase().replace(" ", "").replace("-", "");

        // Map Marathi numbers to English
        s = s.replace("१", "1").replace("२", "2");

        // Map Marathi letters to English
        if (s.equals("अ1"))
            return "A-1";
        if (s.equals("अ2"))
            return "A-2";
        if (s.equals("ब1"))
            return "B-1";
        if (s.equals("ब2"))
            return "B-2";
        if (s.equals("क1"))
            return "C-1";
        if (s.equals("क2"))
            return "C-2";
        if (s.equals("ड"))
            return "D";
        if (s.equals("इ1") || s.equals("ई1"))
            return "E-1";
        if (s.equals("इ2") || s.equals("ई2"))
            return "E-2";

        if (s.equals("A1"))
            return "A-1";
        if (s.equals("A2"))
            return "A-2";
        if (s.equals("B1"))
            return "B-1";
        if (s.equals("B2"))
            return "B-2";
        if (s.equals("C1"))
            return "C-1";
        if (s.equals("C2"))
            return "C-2";
        if (s.equals("D"))
            return "D";
        if (s.equals("E1"))
            return "E-1";
        if (s.equals("E2"))
            return "E-2";
        return g;
    }

    private static void addProgressBookPage(Document doc, Context ctx, School school, ClassModel cls,
            List<Student> students, Map<String, MarksRecord> marksMap,
            String termLabel) throws Exception {
        // 1. Title
        try {
            addMarathiParagraph(doc,
                    PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"),
                    16, true, C_DARK, 0, 4);
        } catch (Exception e) {
            Font titleFont = (sMarathiBase != null) ? new Font(sMarathiBase, 16, Font.BOLD, C_DARK)
                    : new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, C_DARK);
            Paragraph title = new Paragraph(
                    PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"),
                    titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            doc.add(title);
        }

        // 2. Header / Metadata Table (3 columns)
        float[] headerWidths = { 3f, 2f, 3f };
        PdfPTable hTbl = new PdfPTable(headerWidths);
        hTbl.setWidthPercentage(100);
        hTbl.setSpacingAfter(8);

        String schoolName = school != null && school.name != null ? school.name : "-";
        String udiseCode = school != null && school.udiseCode != null ? school.udiseCode : "-";
        String yearLabel = cls != null && cls.academicYearLabel != null ? cls.academicYearLabel : "-";
        String className = cls != null && cls.className != null ? cls.className : "-";
        String division = cls != null && cls.division != null ? cls.division : "-";

        // Left Cell
        PdfPCell cL = new PdfPCell();
        cL.setBorder(Rectangle.NO_BORDER);
        try {
            com.itextpdf.text.Image udiseImg = com.kartik.myschool.utils.pdf.MarathiText.renderLine(
                    PdfLocalizer.get(ctx, "युडायस: ", "UDISE: ") + udiseCode, 9, true, android.graphics.Color.BLACK);
            cL.addElement(udiseImg);
            com.itextpdf.text.Image schoolImg = com.kartik.myschool.utils.pdf.MarathiText.renderLine(
                    PdfLocalizer.get(ctx, "शाळेचे नाव: ", "School: ") + schoolName, 9, false,
                    android.graphics.Color.BLACK);
            cL.addElement(schoolImg);
        } catch (Exception e) {
            Paragraph pL = new Paragraph();
            pL.add(new Phrase(PdfLocalizer.get(ctx, "युडायस: ", "UDISE: ") + udiseCode + "\n", fSmallBold));
            pL.add(new Phrase(PdfLocalizer.get(ctx, "शाळेचे नाव: ", "Name of School: ") + schoolName, fSmallBold));
            cL.addElement(pL);
        }

        // Center Cell
        PdfPCell cC = new PdfPCell();
        cC.setBorder(Rectangle.NO_BORDER);
        String localizedTerm = termLabel;
        if ("First Semester".equals(termLabel)) {
            localizedTerm = PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester");
        } else if ("Second Semester".equals(termLabel)) {
            localizedTerm = PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester");
        }
        try {
            com.itextpdf.text.Image termImg = com.kartik.myschool.utils.pdf.MarathiText.renderLine(
                    localizedTerm,
                    12f,
                    true,
                    android.graphics.Color.rgb(C_DARK.getRed(), C_DARK.getGreen(), C_DARK.getBlue()));
            termImg.setAlignment(com.itextpdf.text.Image.MIDDLE);
            cC.addElement(termImg);
        } catch (Exception e) {
            Font termFont = (sMarathiBase != null) ? new Font(sMarathiBase, 12, Font.BOLD, C_DARK)
                    : new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, C_DARK);
            Paragraph pC = new Paragraph(localizedTerm, termFont);
            pC.setAlignment(Element.ALIGN_CENTER);
            cC.addElement(pC);
        }

        // Right Cell
        PdfPCell cR = new PdfPCell();
        cR.setBorder(Rectangle.NO_BORDER);
        try {
            com.itextpdf.text.Image yImg = com.kartik.myschool.utils.pdf.MarathiText.renderLine(
                    PdfLocalizer.get(ctx, "शैक्षणिक वर्ष: ", "Year: ") + yearLabel, 9, true,
                    android.graphics.Color.BLACK);
            yImg.setAlignment(Element.ALIGN_RIGHT);
            cR.addElement(yImg);
            com.itextpdf.text.Image dImg = com.kartik.myschool.utils.pdf.MarathiText.renderLine(
                    PdfLocalizer.get(ctx, "इयत्ता: ", "Class: ") + className
                            + PdfLocalizer.get(ctx, ", तुकडी: ", ", Div: ") + division,
                    9, true, android.graphics.Color.BLACK);
            dImg.setAlignment(Element.ALIGN_RIGHT);
            cR.addElement(dImg);
        } catch (Exception e) {
            Paragraph pR = new Paragraph();
            pR.setAlignment(Element.ALIGN_RIGHT);
            pR.add(new Phrase(PdfLocalizer.get(ctx, "शैक्षणिक वर्ष: ", "Year: ") + yearLabel + "\n", fSmallBold));
            pR.add(new Phrase(PdfLocalizer.get(ctx, "इयत्ता: ", "Class: ") + className
                    + PdfLocalizer.get(ctx, ", तुकडी: ", ", Div: ") + division, fSmallBold));
            cR.addElement(pR);
        }

        hTbl.addCell(cL);
        hTbl.addCell(cC);
        hTbl.addCell(cR);
        doc.add(hTbl);

        // 3. Prepare Subjects
        List<Subject> subjects = cls != null && cls.subjects != null ? cls.subjects : new ArrayList<>();
        int numCols = 2; // Sr, Student Name
        for (Subject sub : subjects) {
            int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
            boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);
            numCols += isNonAcademic ? 2 : 4;
        }

        float[] widths = new float[numCols];
        widths[0] = 0.3f; // Sr.
        widths[1] = 1.0f; // Name of Student
        int idx = 2;
        for (Subject sub : subjects) {
            int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
            boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);
            if (isNonAcademic) {
                widths[idx++] = 0.3f;
                widths[idx++] = 0.3f;
            } else {
                widths[idx++] = 0.3f;
                widths[idx++] = 0.3f;
                widths[idx++] = 0.3f;
                widths[idx++] = 0.3f;
            }
        }

        PdfPTable tbl = new PdfPTable(widths);
        tbl.setWidthPercentage(100);

        // Row 1 Headers
        cellSpan(tbl, PdfLocalizer.get(ctx, "अ.नं.", "Sr."), fSmallBold, C_HEADER_BG, C_DARK, 1, 3,
                Element.ALIGN_CENTER);
        cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "विद्यार्थ्याचे नाव", "Name of Student"), fSmallBold,
                C_HEADER_BG, C_DARK, 1, 3);
        for (Subject sub : subjects) {
            int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
            boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);
            int colspan = isNonAcademic ? 2 : 4;
            cellSpan(tbl, PdfLocalizer.translateSubject(ctx, sub.name), fSmallBold, C_HEADER_BG, C_DARK, colspan, 1,
                    Element.ALIGN_CENTER);
        }

        // Row 2 Headers
        for (Subject sub : subjects) {
            int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
            boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);
            if (isNonAcademic) {
                cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "आकारिक", "Formative"), fSmallBold, C_HEADER_BG,
                        C_DARK, 1, 1);
                cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "श्रेणी", "Grade"), fSmallBold, C_HEADER_BG, C_DARK, 1,
                        1);
            } else {
                cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "आकारिक", "Formative"), fSmallBold, C_HEADER_BG,
                        C_DARK, 1, 1);
                cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "संकलित", "Summative"), fSmallBold, C_HEADER_BG,
                        C_DARK, 1, 1);
                cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "एकूण (अ+ब)", "Total (A+B)"), fSmallBold, C_HEADER_BG,
                        C_DARK, 1, 1);
                cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "श्रेणी", "Grade"), fSmallBold, C_HEADER_BG, C_DARK, 1,
                        1);
            }
        }

        // Row 3 Headers
        for (Subject sub : subjects) {
            int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
            boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);
            int akarikMaxVal = sub.maxMarks / 2;
            int sanklitMaxVal = sub.maxMarks - akarikMaxVal;
            if (isNonAcademic) {
                cellSpan(tbl, str(sub.maxMarks), fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(tbl, " ", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
            } else {
                cellSpan(tbl, str(akarikMaxVal), fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(tbl, str(sanklitMaxVal), fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(tbl, str(sub.maxMarks), fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
                cellSpan(tbl, " ", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
            }
        }

        // Grade frequency counters
        Map<String, Map<String, Integer>> gradeCounts = new java.util.HashMap<>();
        for (Subject sub : subjects) {
            Map<String, Integer> counts = new java.util.HashMap<>();
            for (String g : new String[] { "A-1", "A-2", "B-1", "B-2", "C-1", "C-2", "D", "E-1", "E-2" }) {
                counts.put(g, 0);
            }
            gradeCounts.put(sub.name, counts);
        }

        // Data Rows
        boolean alt = false;
        for (int sIdx = 0; sIdx < students.size(); sIdx++) {
            Student student = students.get(sIdx);
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE;
            alt = !alt;

            cellSpan(tbl, str(sIdx + 1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(tbl, nvl(student.name), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);

            MarksRecord rec = marksMap != null ? marksMap.get(student.id) : null;

            for (Subject sub : subjects) {
                int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
                boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);

                MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
                if (d != null) {
                    // Use blank for zero values so the PDF doesn't show "0" for un-entered marks
                    String aStr = d.akarikTotal > 0 ? str(d.akarikTotal) : "";
                    String sStr = d.sanklit > 0 ? str(d.sanklit) : "";
                    String gTot = d.grandTotal > 0 ? str(d.grandTotal)
                            : (d.akarikTotal > 0 || d.sanklit > 0) ? str(d.akarikTotal + d.sanklit) : "";
                    String grade = d.grade != null && !d.grade.isEmpty() ? d.grade : "";
                    if (isNonAcademic) {
                        cellSpan(tbl, aStr, fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, grade, fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    } else {
                        cellSpan(tbl, aStr, fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, sStr, fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, gTot, fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, grade, fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    }

                    if (d.grade != null) {
                        String normG = normalizeGrade(d.grade);
                        Map<String, Integer> counts = gradeCounts.get(sub.name);
                        if (counts != null && counts.containsKey(normG)) {
                            counts.put(normG, counts.get(normG) + 1);
                        }
                    }
                } else {
                    if (isNonAcademic) {
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    } else {
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                    }
                }
            }
        }
        doc.add(tbl);

        // 4. Summary Table and Signatures (Side-by-side using a parent 2-column table)
        float[] bottomWidths = { 6.5f, 3.5f };
        PdfPTable bottomTbl = new PdfPTable(bottomWidths);
        bottomTbl.setWidthPercentage(100);
        bottomTbl.setSpacingBefore(12);

        // Left Cell: Summary Table
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);

        float[] sumWidths = { 0.4f, 2.0f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.6f };
        PdfPTable sumTbl = new PdfPTable(sumWidths);
        sumTbl.setWidthPercentage(100);

        cellSpan(sumTbl, PdfLocalizer.get(ctx, "अ.नं.", "Sr."), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);
        cellSpan(sumTbl, PdfLocalizer.get(ctx, "विषय", "Subject"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);
        String[] gradesList = { "A-1", "A-2", "B-1", "B-2", "C-1", "C-2", "D", "E-1", "E-2" };
        for (String g : gradesList) {
            cellSpan(sumTbl, g, fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }
        cellSpan(sumTbl, PdfLocalizer.get(ctx, "एकूण", "Total"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1,
                Element.ALIGN_CENTER);

        boolean sumAlt = false;
        for (int i = 0; i < subjects.size(); i++) {
            Subject sub = subjects.get(i);
            BaseColor sBg = sumAlt ? C_ROW_ALT : C_WHITE;
            sumAlt = !sumAlt;
            cellSpan(sumTbl, str(i + 1), fSmall, sBg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(sumTbl, PdfLocalizer.translateSubject(ctx, sub.name), fSmall, sBg, C_DARK, 1, 1,
                    Element.ALIGN_LEFT);

            int totalGradeCount = 0;
            Map<String, Integer> counts = gradeCounts.get(sub.name);
            for (String g : gradesList) {
                int count = counts != null ? counts.get(g) : 0;
                totalGradeCount += count;
                cellSpan(sumTbl, str(count), fSmall, sBg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            }
            cellSpan(sumTbl, str(totalGradeCount), fSmallBold, sBg, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }
        leftCell.addElement(sumTbl);
        bottomTbl.addCell(leftCell);

        // Right Cell: Signatures
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPaddingLeft(15);

        PdfPTable sigTbl = new PdfPTable(2);
        sigTbl.setWidthPercentage(100);
        sigTbl.setSpacingBefore(30);

        PdfPCell s1 = rawCell(PdfLocalizer.get(ctx, "वर्गशिक्षकाची सही : ", "Class Teacher Signature : ")
                + nvl(cls != null ? cls.teacherName : null), fSmall, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s1.setBorder(Rectangle.TOP);
        s1.setBorderColorTop(C_DARK);
        s1.setBorderWidthTop(0.5f);
        s1.setPaddingTop(4);

        PdfPCell s2 = rawCell(
                PdfLocalizer.get(ctx, "मुख्याध्यापकाची सही : ", "Headmaster Signature : ")
                        + nvl(school != null ? school.principalName : null),
                fSmall, C_WHITE, C_DARK, Element.ALIGN_CENTER);
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

    // ═════════════════════════════════════════════════════════════════════════
    // REPORT 4 — व्यक्तिमत्व नोंदी (compact personality record)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generatePersonalityRecord(Context ctx,
            School school,
            ClassModel cls,
            Student student,
            MarksRecord sem1,
            MarksRecord sem2,
            PdfCallback cb) {
        syncClassYear(cls);
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "Personality_" + safeRoll(student) + "_" + ts() + ".pdf");
                File tempFile = File.createTempFile("pdf_tmp_", ".pdf", outDir(ctx));
                Document doc = new Document(PageSize.A4);
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(tempFile));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addPersonalityContent(doc, ctx, school, cls, student, sem1, sem2);
                doc.close();
                triggerVibration(ctx);
                if (tempFile.renameTo(out)) {
                    cb.onSuccess(out);
                } else {
                    cb.onError(new java.io.IOException("Failed to rename temp PDF to destination"));
                }
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // COMBINED SINGLE-STUDENT REPORT (used by MarksheetActivity)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generate(Context ctx, School school, ClassModel cls,
            Student student, MarksRecord marks, PdfCallback cb) {
        syncClassYear(cls);
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "CombinedReport_" + safeRoll(student) + "_" + ts() + ".pdf");
                File tempFile = File.createTempFile("pdf_tmp_", ".pdf", outDir(ctx));
                Document doc = new Document(PageSize.A4);
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(tempFile));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(30, 30, 30, 30);

                // --- Page 1: Cover Page ---
                com.kartik.myschool.utils.pdf.CoverPageGenerator.addCoverPageContent(doc, ctx, school, cls, student);
                doc.newPage();

                // --- Page 2: Gunapattrak ---
                com.kartik.myschool.utils.pdf.GunapattrakGenerator.addGunapattrakContent(doc, ctx, school, cls, student,
                        marks, null);

                // --- Page 3: Descriptive ---
                // doc.newPage();
                // addDescriptiveContent(doc, ctx, school, cls, student, marks, null); //
                // Deprecated for individual reports

                // --- Page 4: Personality ---
                doc.newPage();
                addPersonalityContent(doc, ctx, school, cls, student, marks, null);

                doc.close();
                triggerVibration(ctx);
                if (tempFile.renameTo(out)) {
                    cb.onSuccess(out);
                } else {
                    cb.onError(new java.io.IOException("Failed to rename temp PDF to destination"));
                }
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    private static void addPersonalityContent(Document doc, Context ctx, School school, ClassModel cls, Student student,
            MarksRecord sem1, MarksRecord sem2) throws Exception {

        // Title
        Paragraph title = new Paragraph("सातत्यपूर्ण सर्वंकष मूल्यमापन", new Font(sMarathiBase, 18, Font.BOLD, C_DARK));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        doc.add(title);

        PdfPTable headerTbl = new PdfPTable(3);
        headerTbl.setWidthPercentage(100);
        headerTbl.setWidths(new float[] { 1.5f, 1f, 1f });

        PdfPCell c1 = new PdfPCell(new Phrase("नाव: " + (student != null ? nvl(student.name) : ""), fBold));
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(" ", fBold));
        c2.setBorder(Rectangle.NO_BORDER);
        PdfPCell c3 = new PdfPCell(new Phrase("सन : " + (cls != null ? nvl(cls.academicYearLabel) : "2025-26"), fBold));
        c3.setBorder(Rectangle.NO_BORDER);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c1);
        headerTbl.addCell(c2);
        headerTbl.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase("इयत्ता: " + (cls != null ? nvl(cls.className) : "") + ", तुकडी: "
                + (cls != null ? nvl(cls.division) : "-"), fBold));
        c4.setBorder(Rectangle.NO_BORDER);
        PdfPCell c5 = new PdfPCell(new Phrase("रोल नं.: " + (student != null ? nvl(student.rollNo) : ""), fBold));
        c5.setBorder(Rectangle.NO_BORDER);
        c5.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell c6 = new PdfPCell(new Phrase(" ", fBold));
        c6.setBorder(Rectangle.NO_BORDER);
        c6.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTbl.addCell(c4);
        headerTbl.addCell(c5);
        headerTbl.addCell(c6);
        headerTbl.setSpacingAfter(10);
        doc.add(headerTbl);

        String[] rowLabels = {
                "आवड, छंद कला, क्रीडा, साहित्य इ.",
                "सुधारणा आवश्यक",
                "विशेष प्रगती",
                "व्यक्तिमत्व गुण विशेष\n(अभिवृत्ती, कल, मूल्ये, स्वभाव गुणविशेष)"
        };
        String[] keys = { "आवड", "सुधारणा", "विशेष", "व्यक्तिमत्व" };

        PdfPTable tbl = new PdfPTable(new float[] { 0.5f, 2.5f, 3.2f, 3.2f });
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(6);

        cell(tbl, "अ.नं", fBold, C_PRIMARY, C_WHITE, 1, Element.ALIGN_CENTER);
        cellVerticalSpan(tbl, ctx, "तपशील", fBold, C_PRIMARY, C_WHITE, 1, 1);
        cell(tbl, "प्रथम सत्र", fBold, C_PRIMARY_LIGHT, C_DARK, 1, Element.ALIGN_CENTER);
        cell(tbl, "द्वितीय सत्र", fBold, C_PRIMARY, C_WHITE, 1, Element.ALIGN_CENTER);

        boolean alt = false;
        for (int i = 0; i < rowLabels.length; i++) {
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE;
            alt = !alt;
            String v1 = remarkContaining(sem1, keys[i]);
            String v2 = remarkContaining(sem2, keys[i]);
            PdfPCell nc = rawCell(str(i + 1), fNormal, bg, C_DARK, Element.ALIGN_CENTER);
            tbl.addCell(nc);
            PdfPCell lc = rawCell(rowLabels[i], fSmall, bg, C_DARK, Element.ALIGN_LEFT);
            lc.setMinimumHeight(36f);
            tbl.addCell(lc);
            PdfPCell r1 = rawCell(v1, fNormal, bg, C_DARK, Element.ALIGN_LEFT);
            r1.setMinimumHeight(36f);
            tbl.addCell(r1);
            PdfPCell r2 = rawCell(v2, fNormal, bg, C_DARK, Element.ALIGN_LEFT);
            r2.setMinimumHeight(36f);
            tbl.addCell(r2);
        }
        doc.add(tbl);
        doc.add(buildMarksSummaryBox(sem1, sem2));
        doc.add(buildSignatureRow(school, cls));

    }

    // ═════════════════════════════════════════════════════════════════════════
    // SHARED BLOCKS
    // ═════════════════════════════════════════════════════════════════════════

    public static PdfPTable buildAppHeader(Context ctx) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);

        try {
            android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(ctx,
                    com.kartik.myschool.R.drawable.ic_school);
            if (d != null) {
                int w = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : 100;
                int h = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 100;
                android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h,
                        android.graphics.Bitmap.Config.ARGB_8888);
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

        Paragraph p = new Paragraph("Edu Report", colored(fTitle, C_DARK));
        p.setAlignment(Element.ALIGN_CENTER);
        c.addElement(p);
        t.addCell(c);
        return t;
    }

    public static PdfPTable buildSchoolHeader(School school, ClassModel cls) throws DocumentException {
        return buildSchoolHeader(null, school, cls);
    }

    public static PdfPTable buildSchoolHeader(Context ctx, School school, ClassModel cls) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(C_PRIMARY);
        c.setPadding(10);
        c.setBorder(Rectangle.NO_BORDER);
        Paragraph p = new Paragraph();
        String name = (school != null && school.name != null) ? school.name.toUpperCase() : "SCHOOL";
        p.add(new Phrase(name + "\n", colored(fTitle, C_WHITE)));
        String udise = (school != null && school.udiseCode != null)
                ? PdfLocalizer.get(ctx, "युडायस: ", "UDISE: ") + school.udiseCode
                : "";
        String year = (cls != null && cls.academicYearLabel != null)
                ? PdfLocalizer.get(ctx, "  सन: ", "  Year: ") + cls.academicYearLabel
                : "";
        p.add(new Phrase(udise + year, colored(fTitleSub, new BaseColor(255, 255, 255, 200))));
        c.addElement(p);
        t.addCell(c);
        return t;
    }

    public static PdfPTable buildStudentInfoTable(School school, ClassModel cls, Student student)
            throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] { 1f, 2f, 1f, 2f });
        t.setWidthPercentage(100);
        t.setSpacingAfter(4);
        infoRow4(t, "नाव:", nvl(student.name), "रोल नं.:", nvl(student.rollNo));
        infoRow4(t, "इयत्ता:", cls != null ? nvl(cls.className) : "-",
                "वर्गशिक्षक:", cls != null ? nvl(cls.teacherName) : "-");
        infoRow4(t, "तुकडी:", cls != null ? nvl(cls.division) : "-",
                "जन्मतारीख:", nvl(student.dob));
        return t;
    }

    public static void infoRow4(PdfPTable t, String l1, String v1, String l2, String v2) {
        for (String[] p : new String[][] { { l1, v1 }, { l2, v2 } }) {
            PdfPCell lc = rawCell(p[0], fSmall, C_WHITE, C_GREY, Element.ALIGN_LEFT);
            lc.setBorder(Rectangle.BOTTOM);
            lc.setBorderColor(C_BORDER);
            lc.setPadding(3);
            PdfPCell vc = rawCell(p[1], fNormal, C_WHITE, C_DARK, Element.ALIGN_LEFT);
            vc.setBorder(Rectangle.BOTTOM);
            vc.setBorderColor(C_BORDER);
            vc.setPadding(3);
            t.addCell(lc);
            t.addCell(vc);
        }
    }

    private static PdfPTable buildAttendanceRow(MarksRecord s1, MarksRecord s2) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] { 2f, 0.8f, 0.6f, 0.2f, 2f, 0.8f, 0.6f });
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        t.setSpacingAfter(4);
        cell(t, "प्रथम सत्र — हजर दिवस:", fSmall, C_HEADER_BG, C_DARK, 1, Element.ALIGN_LEFT);
        cell(t, s1 != null ? str(s1.presentDays) : "—", fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, s1 != null ? "/ " + s1.totalDays : "", fSmall, C_HEADER_BG, C_GREY, 1, Element.ALIGN_LEFT);
        cell(t, "", fSmall, C_WHITE, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, "द्वितीय सत्र — हजर दिवस:", fSmall, C_HEADER_BG, C_DARK, 1, Element.ALIGN_LEFT);
        cell(t, s2 != null ? str(s2.presentDays) : "—", fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, s2 != null ? "/ " + s2.totalDays : "", fSmall, C_HEADER_BG, C_GREY, 1, Element.ALIGN_LEFT);
        return t;
    }

    public static PdfPTable buildSignatureRow(School school, ClassModel cls) throws DocumentException {
        return buildSignatureRow(null, school, cls);
    }

    public static PdfPTable buildSignatureRow(Context ctx, School school, ClassModel cls) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingBefore(28);
        String defaultTeacher = PdfLocalizer.get(ctx, "वर्गशिक्षक", "Class Teacher");
        String defaultPrincipal = PdfLocalizer.get(ctx, "मुख्याध्यापक", "Headmaster");
        String teacher = cls != null && cls.teacherName != null ? cls.teacherName : defaultTeacher;
        String principal = school != null && school.principalName != null ? school.principalName : defaultPrincipal;
        String teacherSig = PdfLocalizer.get(ctx, "वर्गशिक्षक स्वाक्षरी : ", "Class Teacher Signature : ");
        String principalSig = PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी : ", "Headmaster Signature : ");

        PdfPCell s1 = rawCell(teacherSig + teacher, fSmall, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        s1.setBorderWidthTop(0.5f);
        s1.setBorderColorTop(C_DARK);
        s1.setPaddingTop(6);
        PdfPCell s2 = rawCell(principalSig + principal, fSmall, C_WHITE, C_DARK, Element.ALIGN_RIGHT);
        s2.setBorderWidthTop(0.5f);
        s2.setBorderColorTop(C_DARK);
        s2.setPaddingTop(6);
        t.addCell(s1);
        t.addCell(s2);
        return t;
    }

    private static PdfPTable buildProgressHeader(School school, ClassModel cls, String subName)
            throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] { 3f, 1f });
        t.setWidthPercentage(100);
        t.setSpacingAfter(3);
        String sName = school != null && school.name != null ? school.name : "School";
        String yr = cls != null && cls.academicYearLabel != null ? cls.academicYearLabel : "";
        String cInfo = cls != null ? "इयत्ता: " + nvl(cls.className) + "  तुकडी: " + nvl(cls.division) : "";
        Paragraph lp = new Paragraph();
        lp.add(new Phrase(sName + "\n", colored(fBold, C_DARK)));
        lp.add(new Phrase(cInfo + "  |  विषय: " + subName, colored(fSmall, C_GREY)));
        PdfPCell lc = new PdfPCell();
        lc.addElement(lp);
        lc.setBorder(Rectangle.BOTTOM);
        lc.setBorderColor(C_PRIMARY);
        lc.setPadding(4);
        t.addCell(lc);
        Paragraph rp = new Paragraph();
        rp.add(new Phrase("सन: " + yr + "\nप्रथम सत्र", colored(fSmall, C_GREY)));
        PdfPCell rc = new PdfPCell();
        rc.addElement(rp);
        rc.setBorder(Rectangle.BOTTOM);
        rc.setBorderColor(C_PRIMARY);
        rc.setPadding(4);
        rc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(rc);
        return t;
    }

    private static PdfPTable buildSummaryTable(ClassModel cls, List<Student> students,
            Map<String, MarksRecord> s1, Map<String, MarksRecord> s2)
            throws DocumentException {
        int sc = cls.subjects != null ? cls.subjects.size() : 0;
        float[] widths = new float[2 + sc * 2 + 2];
        widths[0] = 0.4f;
        widths[1] = 1.8f;
        for (int i = 0; i < sc * 2; i++)
            widths[2 + i] = 0.55f;
        widths[widths.length - 2] = 0.65f;
        widths[widths.length - 1] = 0.65f;

        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        cell(t, "अ.नं", fSmallBold, C_PRIMARY, C_WHITE, 1, Element.ALIGN_CENTER);
        cell(t, "नाव", fSmallBold, C_PRIMARY, C_WHITE, 1, Element.ALIGN_CENTER);
        if (cls.subjects != null) {
            for (Subject sub : cls.subjects) {
                cell(t, PdfLocalizer.translateSubject(null, sub.name) + "\nस1", fSmallBold, C_PRIMARY_LIGHT, C_DARK, 1,
                        Element.ALIGN_CENTER);
                cell(t, PdfLocalizer.translateSubject(null, sub.name) + "\nस2", fSmallBold, C_PRIMARY, C_WHITE, 1,
                        Element.ALIGN_CENTER);
            }
        }
        cell(t, "एकूण\nस1", fSmallBold, C_PRIMARY_LIGHT, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, "एकूण\nस2", fSmallBold, C_PRIMARY, C_WHITE, 1, Element.ALIGN_CENTER);

        boolean alt = false;
        for (int i = 0; i < students.size(); i++) {
            Student st = students.get(i);
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE;
            alt = !alt;
            MarksRecord r1 = s1 != null ? s1.get(st.id) : null;
            MarksRecord r2 = s2 != null ? s2.get(st.id) : null;
            cell(t, str(i + 1), fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
            cell(t, nvl(st.name), fSmall, bg, C_DARK, 1, Element.ALIGN_LEFT);
            if (cls.subjects != null) {
                for (Subject sub : cls.subjects) {
                    MarksRecord.SubjectMarksDetail d1 = detail(r1, sub.name);
                    MarksRecord.SubjectMarksDetail d2 = detail(r2, sub.name);
                    cell(t, d1 != null ? str(d1.grandTotal) : "—", fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
                    cell(t, d2 != null ? str(d2.grandTotal) : "—", fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
                }
            }
            cell(t, r1 != null ? fmt(r1.totalObtained) : "—", fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
            cell(t, r2 != null ? fmt(r2.totalObtained) : "—", fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
        }
        return t;
    }

    private static PdfPTable buildMarksSummaryBox(MarksRecord s1, MarksRecord s2) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] { 1.8f, 0.8f, 0.6f, 0.3f, 1.8f, 0.8f, 0.6f });
        t.setWidthPercentage(100);
        t.setSpacingBefore(8);
        cell(t, "प्रथम सत्र — एकूण:", fSmall, C_HEADER_BG, C_DARK, 1, Element.ALIGN_LEFT);
        cell(t, s1 != null ? fmt(s1.totalObtained) + "/" + s1.totalMax : "—", fBold, C_HEADER_BG, C_DARK, 1,
                Element.ALIGN_CENTER);
        cell(t, s1 != null ? nvl(s1.grade) : "—", fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, "", fSmall, C_WHITE, C_DARK, 1, Element.ALIGN_CENTER);
        cell(t, "द्वितीय सत्र — एकूण:", fSmall, C_HEADER_BG, C_DARK, 1, Element.ALIGN_LEFT);
        cell(t, s2 != null ? fmt(s2.totalObtained) + "/" + s2.totalMax : "—", fBold, C_HEADER_BG, C_DARK, 1,
                Element.ALIGN_CENTER);
        cell(t, s2 != null ? nvl(s2.grade) : "—", fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
        return t;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CELL & FONT HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    public static void cell(PdfPTable t, String text, Font font, BaseColor bg,
            BaseColor textColor, int colspan, int align) {
        PdfPCell c = rawCell(text, font, bg, textColor, align);
        c.setColspan(colspan);
        t.addCell(c);
    }

    public static void groupCell(PdfPTable t, String text, BaseColor bg, BaseColor textColor, int colspan) {
        PdfPCell c = rawCell(text, fBold, bg, textColor, Element.ALIGN_CENTER);
        c.setColspan(colspan);
        t.addCell(c);
    }

    public static PdfPCell rawCell(String text, Font font, BaseColor bg, BaseColor textColor, int align) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(4);
        c.setHorizontalAlignment(align);

        // Use MarathiText bitmap rendering for any text containing Devanagari,
        // so matras, conjuncts and half-forms render correctly via Android's
        // Harfbuzz shaping engine (iText 5 cannot do this on its own).
        if (text != null && containsDevanagari(text) && sMarathiTypeface != null) {
            try {
                boolean bold = (font.getStyle() & Font.BOLD) != 0;
                float size = font.getSize() > 0 ? font.getSize() : 9f;
                int androidColor = android.graphics.Color.rgb(
                        textColor.getRed(), textColor.getGreen(), textColor.getBlue());
                com.itextpdf.text.Image img = com.kartik.myschool.utils.pdf.MarathiText
                        .renderLine(text, size, bold, androidColor);
                int imgAlign = align == Element.ALIGN_LEFT ? com.itextpdf.text.Image.LEFT
                        : align == Element.ALIGN_RIGHT ? com.itextpdf.text.Image.RIGHT
                                : com.itextpdf.text.Image.MIDDLE;
                img.setAlignment(imgAlign);
                c.addElement(img);
                return c;
            } catch (Exception ignored) {
                // Fall through to iText rendering
            }
        }
        // Fallback: iText font rendering (fine for numbers/ASCII/Latin)
        Font englishFont = new Font(Font.FontFamily.HELVETICA, font.getSize(), font.getStyle(), textColor);
        c.setPhrase(new Phrase(text, englishFont));
        return c;
    }

    /**
     * Returns true if text contains any Devanagari Unicode character
     * (U+0900–U+097F).
     */
    private static PdfPCell noBorderMarathiCell(String text, float sizePt, boolean bold,
            BaseColor textColor, int align) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            int androidColor = android.graphics.Color.rgb(
                    textColor.getRed(), textColor.getGreen(), textColor.getBlue());
            com.itextpdf.text.Image img = com.kartik.myschool.utils.pdf.MarathiText
                    .renderLine(text, sizePt, bold, androidColor);
            img.setAlignment(align == Element.ALIGN_LEFT ? com.itextpdf.text.Image.LEFT
                    : align == Element.ALIGN_RIGHT ? com.itextpdf.text.Image.RIGHT
                            : com.itextpdf.text.Image.MIDDLE);
            c.addElement(img);
        } catch (Exception e) {
            Font fallback = sMarathiBase != null
                    ? new Font(sMarathiBase, sizePt, bold ? Font.BOLD : Font.NORMAL, textColor)
                    : new Font(Font.FontFamily.HELVETICA, sizePt, bold ? Font.BOLD : Font.NORMAL, textColor);
            c.setPhrase(new Phrase(text, fallback));
        }
        return c;
    }

    public static boolean containsDevanagari(String text) {
        if (text == null)
            return false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 0x0900 && ch <= 0x097F)
                return true;
        }
        return false;
    }

    /**
     * Adds a centred Marathi paragraph to the document, rendered as a bitmap
     * image for correct Devanagari shaping. Falls back to iText Paragraph if
     * the typeface is unavailable.
     */
    public static void addMarathiParagraph(Document doc, String text,
            float sizePt, boolean bold,
            BaseColor color,
            float spaceBefore, float spaceAfter) throws Exception {
        if (text != null && containsDevanagari(text) && sMarathiTypeface != null) {
            try {
                int androidColor = android.graphics.Color.rgb(
                        color.getRed(), color.getGreen(), color.getBlue());
                com.itextpdf.text.Image img = com.kartik.myschool.utils.pdf.MarathiText
                        .renderLine(text, sizePt, bold, androidColor);
                img.setAlignment(com.itextpdf.text.Image.MIDDLE);
                PdfPTable wrap = new PdfPTable(1);
                wrap.setWidthPercentage(100);
                wrap.setSpacingBefore(spaceBefore);
                wrap.setSpacingAfter(spaceAfter);
                PdfPCell wc = new PdfPCell();
                wc.setBorder(Rectangle.NO_BORDER);
                wc.setHorizontalAlignment(Element.ALIGN_CENTER);
                wc.addElement(img);
                wrap.addCell(wc);
                doc.add(wrap);
                return;
            } catch (Exception ignored) {
            }
        }
        // Fallback
        Font f = sMarathiBase != null
                ? new Font(sMarathiBase, sizePt, bold ? Font.BOLD : Font.NORMAL, color)
                : new Font(Font.FontFamily.HELVETICA, sizePt, bold ? Font.BOLD : Font.NORMAL, color);
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(spaceBefore);
        p.setSpacingAfter(spaceAfter);
        doc.add(p);
    }

    public static void cellSpan(PdfPTable t, String text, Font font, BaseColor bg, BaseColor textColor, int colspan,
            int rowspan, int align) {
        PdfPCell c = rawCell(text, font, bg, textColor, align);
        if (colspan > 1)
            c.setColspan(colspan);
        if (rowspan > 1)
            c.setRowspan(rowspan);
        t.addCell(c);
    }

    public static String calculatePercentageString(int obtained, int max) {
        if (max == 0)
            return "-";
        double p = (obtained * 100.0) / max;
        return fmt(p);
    }

    public static Font colored(Font src, BaseColor color) {
        Font f = new Font(src);
        f.setColor(color);
        return f;
    }

    /**
     * Shorthand: add a properly-shaped Marathi table cell.
     * Uses MarathiText (Android Canvas bitmap) so all glyphs render correctly.
     */
    public static void cellM(PdfPTable t, String text, boolean bold, BaseColor bg,
            BaseColor fg, int colSpan, int rowSpan, int align) {
        com.kartik.myschool.utils.pdf.MarathiText.cell(
                t, text, bold ? 10f : 9f, bold, bg, fg, colSpan, rowSpan, align);
    }

    private static Paragraph para(String text, Font font) {
        return new Paragraph(text, font);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DATA HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    public static MarksRecord.SubjectMarksDetail detail(MarksRecord rec, String subName) {
        if (rec == null || rec.detailedMarks == null || subName == null)
            return null;

        // Strategy 1: sanitized key (most common)
        String safeKey = MarksRecord.sanitizeKey(subName);
        MarksRecord.SubjectMarksDetail d = rec.detailedMarks.get(safeKey);
        if (d != null)
            return d;

        // Strategy 2: raw subject name (in case it was stored unsanitized)
        d = rec.detailedMarks.get(subName);
        if (d != null)
            return d;

        // Strategy 3: iterate all keys and compare sanitized forms
        for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : rec.detailedMarks.entrySet()) {
            String key = entry.getKey();
            if (key != null && MarksRecord.sanitizeKey(key).equals(safeKey)) {
                return entry.getValue();
            }
        }

        // Strategy 4: language-aware equivalence (English ↔ Marathi subject names).
        // Handles cases where admin changed subject sequence or language after marks were saved.
        for (java.util.Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : rec.detailedMarks.entrySet()) {
            String key = entry.getKey();
            if (key != null && com.kartik.myschool.model.Subject.isSameSubject(key, subName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static String akarikMax(Subject sub) {
        return str(sub.maxMarks / 2);
    }

    private static String sanklitMax(Subject sub) {
        return str(sub.maxMarks - sub.maxMarks / 2);
    }

    private static int sumAkarik(MarksRecord rec, List<Subject> subs) {
        int s = 0;
        if (rec == null || rec.detailedMarks == null)
            return s;
        for (Subject sub : subs) {
            MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
            if (d != null)
                s += d.akarikTotal;
        }
        return s;
    }

    private static int sumSanklit(MarksRecord rec, List<Subject> subs) {
        int s = 0;
        if (rec == null || rec.detailedMarks == null)
            return s;
        for (Subject sub : subs) {
            MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
            if (d != null)
                s += d.sanklit;
        }
        return s;
    }

    private static String remarkContaining(MarksRecord rec, String keyword) {
        if (rec == null || rec.detailedMarks == null)
            return "";
        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> e : rec.detailedMarks.entrySet()) {
            if (e.getKey() != null && e.getKey().contains(keyword) && e.getValue() != null)
                return nvl(e.getValue().remark);
        }
        return "";
    }

    private static String[] subjectLabels(ClassModel cls) {
        if (cls.subjects != null && !cls.subjects.isEmpty()) {
            String[] l = new String[cls.subjects.size()];
            for (int i = 0; i < l.length; i++)
                l[i] = cls.subjects.get(i).name;
            return l;
        }
        return new String[] { "मराठी", "हिंदी", "इंग्रजी",
                "गणित", "सामान्य विज्ञान / परिसर अभ्यास 1", "सामाजिक शास्त्रे / परिसर अभ्यास 2",
                "कला", "कार्यानुभव", "शारीरिक शिक्षण व आरोग्य", "विशेष प्रगती",
                "आवड, छंद कला, क्रीडा", "सुधारणा आवश्यक", "व्यक्तिमत्व गुण विशेष" };
    }

    public static String strZero(int v) {
        return v > 0 ? String.valueOf(v) : "-";
    }

    public static String nvl(String s) {
        return s != null && !s.isEmpty() ? s : "-";
    }

    public static String str(int v) {
        return String.valueOf(v);
    }

    public static String str(double v) {
        return fmt(v);
    }

    public static String fmt(double v) {
        return v == Math.floor(v) ? str((int) v) : String.format(Locale.getDefault(), "%.1f", v);
    }

    public static String safeRoll(Student s) {
        return s != null && s.rollNo != null ? s.rollNo : "0";
    }
}
