package com.example.myschool.utils;

import android.content.Context;

import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.model.Subject;
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

                doc.add(buildSchoolHeader(school, cls));

                Paragraph title = para("सातत्यपूर्ण सर्वकष मूल्यमापन — गुणपत्रक", colored(fHeader, C_DARK));
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(4);
                doc.add(title);

                doc.add(buildStudentInfoTable(school, cls, student));

                // Marks table:  अ.नं | विषय | [Sem1: आकारिक|पैकी|संकलित|पैकी|एकूण|श्रेणी] | [Sem2: same]
                PdfPTable tbl = new PdfPTable(new float[]{0.6f, 2.5f, 0.8f,0.5f,0.8f,0.5f,0.8f,0.7f, 0.8f,0.5f,0.8f,0.5f,0.8f,0.7f});
                tbl.setWidthPercentage(100);
                tbl.setSpacingBefore(6);
                tbl.setSpacingAfter(4);

                // Tier-1 headers
                groupCell(tbl, "अ.नं",        C_PRIMARY,       C_WHITE,       1);
                groupCell(tbl, "विषय",         C_PRIMARY,       C_WHITE,       1);
                groupCell(tbl, "प्रथम सत्र",   C_PRIMARY_LIGHT, C_DARK,        6);
                groupCell(tbl, "द्वितीय सत्र", C_PRIMARY,       C_WHITE,       6);

                // Tier-2 sub-headers
                String[] sem1H = {"आकारिक(अ)","पैकी","संकलित(ब)","पैकी","एकूण","श्रेणी"};
                String[] sem2H = {"आकारिक(अ)","पैकी","संकलित(ब)","पैकी","एकूण","श्रेणी"};
                cell(tbl, "", fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                cell(tbl, "", fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                for (String h : sem1H) cell(tbl, h, fSmallBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                for (String h : sem2H) cell(tbl, h, fSmallBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);

                List<Subject> subjects = cls.subjects;
                boolean alt = false;
                for (int i = 0; i < subjects.size(); i++) {
                    Subject sub = subjects.get(i);
                    BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                    cell(tbl, String.valueOf(i + 1), fNormal, bg, C_DARK, 1, Element.ALIGN_CENTER);
                    cell(tbl, sub.name,               fSmall,  bg, C_DARK, 1, Element.ALIGN_LEFT);
                    for (MarksRecord rec : new MarksRecord[]{sem1, sem2}) {
                        MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
                        if (d != null) {
                            cell(tbl, str(d.akarikTotal),  fNormal, bg, C_DARK, 1, Element.ALIGN_CENTER);
                            cell(tbl, akarikMax(sub),      fSmall,  bg, C_GREY, 1, Element.ALIGN_CENTER);
                            cell(tbl, str(d.sanklit),      fNormal, bg, C_DARK, 1, Element.ALIGN_CENTER);
                            cell(tbl, sanklitMax(sub),     fSmall,  bg, C_GREY, 1, Element.ALIGN_CENTER);
                            cell(tbl, str(d.grandTotal),   fBold,   bg, C_DARK, 1, Element.ALIGN_CENTER);
                            cell(tbl, nvl(d.grade),        fBold,   bg, C_DARK, 1, Element.ALIGN_CENTER);
                        } else {
                            for (int k = 0; k < 6; k++) cell(tbl, "—", fSmall, bg, C_GREY, 1, Element.ALIGN_CENTER);
                        }
                    }
                }

                // Total row
                cell(tbl, "",       fBold, C_PRIMARY_LIGHT, C_DARK, 1, Element.ALIGN_CENTER);
                cell(tbl, "एकूण",  fBold, C_PRIMARY_LIGHT, C_DARK, 1, Element.ALIGN_LEFT);
                for (MarksRecord rec : new MarksRecord[]{sem1, sem2}) {
                    if (rec != null) {
                        cell(tbl, str(sumAkarik(rec, subjects)),   fBold, C_PRIMARY_LIGHT, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, "",                              fBold, C_PRIMARY_LIGHT, C_GREY, 1, Element.ALIGN_CENTER);
                        cell(tbl, str(sumSanklit(rec, subjects)),  fBold, C_PRIMARY_LIGHT, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, "",                              fBold, C_PRIMARY_LIGHT, C_GREY, 1, Element.ALIGN_CENTER);
                        cell(tbl, fmt(rec.totalObtained),          fBold, C_PRIMARY_LIGHT, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, nvl(rec.grade),                  fBold, C_PRIMARY_LIGHT, C_DARK, 1, Element.ALIGN_CENTER);
                    } else {
                        for (int k = 0; k < 6; k++) cell(tbl, "—", fSmall, C_PRIMARY_LIGHT, C_GREY, 1, Element.ALIGN_CENTER);
                    }
                }
                doc.add(tbl);
                doc.add(buildAttendanceRow(sem1, sem2));
                doc.add(buildSignatureRow(school, cls));
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

                doc.add(buildSchoolHeader(school, cls));

                Paragraph title = para("सातत्यपूर्ण सर्वकष मूल्यमापन — गुणवर्णनिका", colored(fHeader, C_DARK));
                title.setAlignment(Element.ALIGN_CENTER); title.setSpacingAfter(4);
                doc.add(title);

                doc.add(buildStudentInfoTable(school, cls, student));

                // अ.नं | विषय | Sem1 remark | Sem2 remark
                PdfPTable tbl = new PdfPTable(new float[]{0.5f, 2.2f, 3.1f, 3.1f});
                tbl.setWidthPercentage(100); tbl.setSpacingBefore(6);

                cell(tbl, "अ.नं",  fBold, C_PRIMARY,       C_WHITE, 1, Element.ALIGN_CENTER);
                cell(tbl, "विषय",  fBold, C_PRIMARY,       C_WHITE, 1, Element.ALIGN_CENTER);
                cell(tbl, "प्रथम सत्र — विषयवार वर्णनात्मक नोंद",   fBold, C_PRIMARY_LIGHT, C_DARK,  1, Element.ALIGN_CENTER);
                cell(tbl, "द्वितीय सत्र — विषयवार वर्णनात्मक नोंद", fBold, C_PRIMARY,       C_WHITE, 1, Element.ALIGN_CENTER);

                String[] labels = subjectLabels(cls);
                boolean alt = false;
                for (int i = 0; i < labels.length; i++) {
                    BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                    String r1 = "", r2 = "";
                    if (cls.subjects != null && i < cls.subjects.size()) {
                        String sn = cls.subjects.get(i).name;
                        MarksRecord.SubjectMarksDetail d1 = detail(sem1, sn);
                        MarksRecord.SubjectMarksDetail d2 = detail(sem2, sn);
                        if (d1 != null) r1 = nvl(d1.remark);
                        if (d2 != null) r2 = nvl(d2.remark);
                    }
                    PdfPCell nc = rawCell(String.valueOf(i + 1), fNormal, bg, C_DARK, Element.ALIGN_CENTER); tbl.addCell(nc);
                    PdfPCell lc = rawCell(labels[i], fSmall, bg, C_DARK, Element.ALIGN_LEFT); tbl.addCell(lc);
                    PdfPCell c1 = rawCell(r1, fNormal, bg, C_DARK, Element.ALIGN_LEFT); c1.setMinimumHeight(32f); tbl.addCell(c1);
                    PdfPCell c2 = rawCell(r2, fNormal, bg, C_DARK, Element.ALIGN_LEFT); c2.setMinimumHeight(32f); tbl.addCell(c2);
                }
                doc.add(tbl);
                doc.add(buildSignatureRow(school, cls));
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
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
                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(18, 18, 18, 18);

                List<Subject> subjects = cls.subjects;
                if (subjects == null || subjects.isEmpty()) {
                    doc.close(); cb.onError(new IllegalStateException("No subjects")); return;
                }

                // Per-subject pages
                for (int si = 0; si < subjects.size(); si++) {
                    if (si > 0) doc.newPage();
                    Subject sub = subjects.get(si);
                    doc.add(buildProgressHeader(school, cls, sub.name));

                    // Cols: अ.नं | नाव | [Sem1: आकारिक|पैकी|संकलित|पैकी|एकूण|पैकी|श्रेणी] | [Sem2: same]
                    PdfPTable tbl = new PdfPTable(new float[]{
                            0.4f, 2.0f,
                            0.65f,0.45f, 0.65f,0.45f, 0.7f,0.45f,0.6f,
                            0.65f,0.45f, 0.65f,0.45f, 0.7f,0.45f,0.6f});
                    tbl.setWidthPercentage(100); tbl.setSpacingBefore(3);

                    // Tier 1
                    groupCell(tbl, "अ.नं",        C_HEADER_BG,     C_DARK,  1);
                    groupCell(tbl, "विद्यार्थ्याचे नाव", C_HEADER_BG, C_DARK,  1);
                    groupCell(tbl, "प्रथम सत्र",   C_PRIMARY_LIGHT, C_DARK,  7);
                    groupCell(tbl, "द्वितीय सत्र", C_PRIMARY,       C_WHITE, 7);

                    // Tier 2
                    cell(tbl, "", fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                    cell(tbl, "", fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                    for (int sem = 0; sem < 2; sem++) {
                        for (String h : new String[]{"आकारिक","पैकी","संकलित","पैकी","एकूण","पैकी","श्रेणी"})
                            cell(tbl, h, fSmallBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                    }

                    // Max marks row
                    cell(tbl, "", fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                    cell(tbl, "", fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                    for (int sem = 0; sem < 2; sem++) {
                        cell(tbl, "",                     fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                        cell(tbl, akarikMax(sub),         fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                        cell(tbl, "",                     fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                        cell(tbl, sanklitMax(sub),        fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                        cell(tbl, "",                     fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                        cell(tbl, str(sub.maxMarks),      fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                        cell(tbl, "",                     fSmallBold, C_HEADER_BG, C_GREY, 1, Element.ALIGN_CENTER);
                    }

                    // Data
                    boolean alt = false;
                    for (int ri = 0; ri < students.size(); ri++) {
                        Student st = students.get(ri);
                        BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                        cell(tbl, str(ri + 1),   fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, nvl(st.name),  fSmall, bg, C_DARK, 1, Element.ALIGN_LEFT);
                        for (Map<String, MarksRecord> mp : new Map[]{sem1Marks, sem2Marks}) {
                            MarksRecord rec = mp != null ? mp.get(st.id) : null;
                            MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
                            if (d != null) {
                                cell(tbl, str(d.akarikTotal),  fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
                                cell(tbl, akarikMax(sub),      fSmall, bg, C_GREY, 1, Element.ALIGN_CENTER);
                                cell(tbl, str(d.sanklit),      fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
                                cell(tbl, sanklitMax(sub),     fSmall, bg, C_GREY, 1, Element.ALIGN_CENTER);
                                cell(tbl, str(d.grandTotal),   fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
                                cell(tbl, str(sub.maxMarks),   fSmall, bg, C_GREY, 1, Element.ALIGN_CENTER);
                                cell(tbl, nvl(d.grade),        fSmall, bg, C_DARK, 1, Element.ALIGN_CENTER);
                            } else {
                                for (int k = 0; k < 7; k++) cell(tbl, "—", fSmall, bg, C_GREY, 1, Element.ALIGN_CENTER);
                            }
                        }
                    }
                    doc.add(tbl);
                }

                // Summary page
                doc.newPage();
                doc.add(buildProgressHeader(school, cls, "सर्व विषय — एकत्रित निकाल"));
                doc.add(buildSummaryTable(cls, students, sem1Marks, sem2Marks));

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
                File out = new File(outDir(ctx), "Vyaktimatva_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);

                doc.add(buildSchoolHeader(school, cls));

                Paragraph title = para("सातत्यपूर्ण सर्वकष मूल्यमापन — व्यक्तिमत्व विकास नोंदी", colored(fHeader, C_DARK));
                title.setAlignment(Element.ALIGN_CENTER); title.setSpacingAfter(4);
                doc.add(title);

                doc.add(buildStudentInfoTable(school, cls, student));

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
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LEGACY — single-semester (used by MarksheetActivity)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generate(Context context, School school, ClassModel classModel,
                                Student student, MarksRecord marks, PdfCallback callback) {
        generateGunapattrak(context, school, classModel, student, marks, null, callback);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SHARED BLOCKS
    // ═════════════════════════════════════════════════════════════════════════

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
        return rec.detailedMarks.get(subName);
    }

    private static String akarikMax(Subject sub) { return str(sub.maxMarks / 2); }
    private static String sanklitMax(Subject sub) { return str(sub.maxMarks - sub.maxMarks / 2); }

    private static int sumAkarik(MarksRecord rec, List<Subject> subs) {
        int s = 0;
        if (rec == null || rec.detailedMarks == null) return s;
        for (Subject sub : subs) { MarksRecord.SubjectMarksDetail d = rec.detailedMarks.get(sub.name); if (d!=null) s += d.akarikTotal; }
        return s;
    }
    private static int sumSanklit(MarksRecord rec, List<Subject> subs) {
        int s = 0;
        if (rec == null || rec.detailedMarks == null) return s;
        for (Subject sub : subs) { MarksRecord.SubjectMarksDetail d = rec.detailedMarks.get(sub.name); if (d!=null) s += d.sanklit; }
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

    private static String nvl(String s)   { return s != null ? s : "—"; }
    private static String str(int v)      { return String.valueOf(v); }
    private static String str(double v)   { return fmt(v); }
    private static String fmt(double v)   { return v==Math.floor(v)?str((int)v):String.format(Locale.getDefault(),"%.1f",v); }
    private static String safeRoll(Student s) { return s!=null&&s.rollNo!=null?s.rollNo:"0"; }
}
