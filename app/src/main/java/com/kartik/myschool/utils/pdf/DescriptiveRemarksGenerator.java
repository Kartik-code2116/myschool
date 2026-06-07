package com.kartik.myschool.utils.pdf;

import android.content.Context;
import android.util.Log;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
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
import java.util.List;
import java.util.Map;

import static com.kartik.myschool.utils.PdfGenerator.*;

/**
 * Generates the Descriptive Remarks PDF (Option 4).
 *
 * Layout: ONE PAGE PER STUDENT.
 * Each page contains:
 *  - Page title: "सातत्यपूर्ण सर्वंकष मूल्यमापन"
 *  - Student header: Name | Year | Class, Div | Roll No | Semester
 *  - 3-column table: Sr. No. | Subject | Descriptive Remark
 *    -> Rows for every class subject
 *    -> Extra fixed rows for विशेष प्रगती, आवड/छंद, सुधारणा आवश्यक, व्यक्तिमत्व गुण
 */
public class DescriptiveRemarksGenerator {

    private static final String TAG = "DESC_PDF";

    // ── Extra fixed subject rows appended after class subjects ─────────────────
    private static final String[] EXTRA_LABELS = {
            "विशेष प्रगती",
            "आवड, छंद कला, क्रीडा, साहित्य इ.",
            "सुधारणा आवश्यक",
            "व्यक्तिमत्व गुण विशेष\n(अभिवृत्ती, कल, मूल्ये, स्वभाव गुणविशेष)"
    };
    private static final String[] EXTRA_KEYS = { "विशेष", "आवड", "सुधारणा", "व्यक्तिमत्व" };

    // ── Remark lookup helpers ──────────────────────────────────────────────────

    /** Finds the remark for a given subject from a MarksRecord, using multi-strategy matching. */
    private static String findRemark(MarksRecord rec, String subjectName) {
        if (rec == null || rec.detailedMarks == null || subjectName == null) return "";

        // Strategy 1: exact/normalised via PdfGenerator.detail()
        MarksRecord.SubjectMarksDetail d = detail(rec, subjectName);
        if (d != null && d.remark != null && !d.remark.trim().isEmpty()) {
            return cleanRemark(d.remark);
        }

        // Strategy 2: case-insensitive loose matching
        String safeName = MarksRecord.sanitizeKey(subjectName).toLowerCase();
        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : rec.detailedMarks.entrySet()) {
            String key = entry.getKey();
            MarksRecord.SubjectMarksDetail val = entry.getValue();
            if (val != null && val.remark != null && !val.remark.trim().isEmpty()) {
                String safeKey = key != null ? MarksRecord.sanitizeKey(key).toLowerCase() : "";
                if (safeKey.equals(safeName) || safeKey.contains(safeName) || safeName.contains(safeKey)) {
                    return cleanRemark(val.remark);
                }
            }
        }
        return "";
    }

    /** Finds a remark from a record whose key contains `keyword` (used for extra rows). */
    private static String remarkContaining(MarksRecord rec, String keyword) {
        if (rec == null || rec.detailedMarks == null) return "";
        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> e : rec.detailedMarks.entrySet()) {
            if (e.getKey() != null && e.getKey().contains(keyword) && e.getValue() != null
                    && e.getValue().remark != null && !e.getValue().remark.trim().isEmpty()) {
                return cleanRemark(e.getValue().remark);
            }
        }
        return "";
    }

    private static String cleanRemark(String raw) {
        return raw == null ? "" : raw.replace("||", ", ").trim();
    }



    // ── Public entry point ─────────────────────────────────────────────────────

    public static void generateDescriptive(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           List<Student> students,
                                           Map<String, MarksRecord> sem1Marks,
                                           Map<String, MarksRecord> sem2Marks,
                                           PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);

                File out = new File(PdfGenerator.outDir(ctx),
                        "DescriptiveRemarksRegister_" + PdfGenerator.ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(28, 28, 32, 28);

                String classId = cls != null ? cls.id : null;

                // Detect active semester label
                int semNum = com.kartik.myschool.SessionContext.selectedSemester != null
                        ? com.kartik.myschool.SessionContext.selectedSemester.number : 1;
                String semLabel = semNum == 2 
                        ? PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester") 
                        : PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester");

                // Choose the semester marks map to use as primary
                Map<String, MarksRecord> activeSemMarks = semNum == 2 ? sem2Marks : sem1Marks;

                Log.d(TAG, "=== GENERATING DESCRIPTIVE PDF === sem=" + semNum
                        + " students=" + (students != null ? students.size() : "null"));

                if (students != null && !students.isEmpty()) {
                    for (int si = 0; si < students.size(); si++) {
                        if (si > 0) doc.newPage();
                        Student student = students.get(si);
                        MarksRecord rec = activeSemMarks != null ? activeSemMarks.get(student.id) : null;

                        addStudentPage(doc, ctx, school, cls, student, rec, semLabel, si + 1);
                    }
                }

                doc.close();
                Log.d(TAG, "=== PDF GENERATED SUCCESSFULLY ===");
                cb.onSuccess(out);
            } catch (Exception e) {
                Log.e(TAG, "PDF generation FAILED", e);
                cb.onError(e);
            }
        }).start();
    }

    // ── Per-student page renderer ──────────────────────────────────────────────

    private static void addStudentPage(Document doc, Context ctx, School school, ClassModel cls,
                                       Student student, MarksRecord rec,
                                       String semLabel, int pageIndex)
            throws DocumentException {

        Font fTitle  = sMarathiBase != null ? new Font(sMarathiBase, 15, Font.BOLD,  C_DARK)
                                            : new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, C_DARK);
        Font fMeta   = sMarathiBase != null ? new Font(sMarathiBase, 10, Font.NORMAL, C_DARK)
                                            : new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, C_DARK);
        Font fMetaB  = sMarathiBase != null ? new Font(sMarathiBase, 10, Font.BOLD,   C_DARK)
                                            : new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, C_DARK);
        Font fHdr    = sMarathiBase != null ? new Font(sMarathiBase, 10, Font.BOLD,   C_WHITE)
                                            : new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, C_WHITE);
        Font fRow    = sMarathiBase != null ? new Font(sMarathiBase,  9, Font.NORMAL, C_DARK)
                                            : new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, C_DARK);
        Font fRowB   = sMarathiBase != null ? new Font(sMarathiBase,  9, Font.BOLD,   C_DARK)
                                            : new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD, C_DARK);

        // ── 1. Page Title (rendered via MarathiText for correct Devanagari) ────
        try {
            PdfGenerator.addMarathiParagraph(doc, PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"),
                    15, true, C_DARK, 0, 8);
        } catch (Exception e) {
            Paragraph titlePara = new Paragraph(PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"), fTitle);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(8);
            doc.add(titlePara);
        }

        // ── 2. Student Info Header ─────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(new float[]{2.5f, 0.2f, 1.5f});
        hdr.setWidthPercentage(100);
        hdr.setSpacingAfter(6);

        // Row 1: Name  |  |  Year
        addNoBorderCell(hdr, PdfLocalizer.get(ctx, "नाव: ", "Name: ") + nvl(student != null ? student.name : null), fMetaB, Element.ALIGN_LEFT);
        addNoBorderCell(hdr, "", fMeta, Element.ALIGN_CENTER);
        addNoBorderCell(hdr, PdfLocalizer.get(ctx, "सन: ", "Year: ") + (cls != null ? nvl(cls.academicYearLabel) : "-"), fMetaB, Element.ALIGN_RIGHT);

        // Row 2: Class, Div  |  Roll No  |  Semester
        String classDiv = PdfLocalizer.get(ctx, "इयत्ता: ", "Class: ") + (cls != null ? nvl(cls.className) : "") + PdfLocalizer.get(ctx, ", तुकडी: ", ", Division: ") + (cls != null ? nvl(cls.division) : "-");
        String rollStr  = PdfLocalizer.get(ctx, "रोल नं.: ", "Roll No.: ") + nvl(student != null ? student.rollNo : null);
        addNoBorderCell(hdr, classDiv, fMeta, Element.ALIGN_LEFT);
        addNoBorderCell(hdr, rollStr,  fMeta, Element.ALIGN_CENTER);
        addNoBorderCell(hdr, semLabel, fMetaB, Element.ALIGN_RIGHT);

        doc.add(hdr);

        // ── 3. Table: Sr.No | Subject | Remark ──────────────────────────────
        PdfPTable tbl = new PdfPTable(new float[]{0.5f, 2.5f, 5.0f});
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(4);

        // Header row
        addHeaderCell(tbl, PdfLocalizer.get(ctx, "अ.नं", "Sr.No."), fHdr, C_PRIMARY);
        addHeaderCell(tbl, PdfLocalizer.get(ctx, "विषय", "Subject"), fHdr, C_PRIMARY);
        addHeaderCell(tbl, PdfLocalizer.get(ctx, "विषयवार वर्णनात्मक नोंद", "Subject-wise Descriptive Remark"), fHdr, C_PRIMARY);

        int rowIdx = 1;
        boolean alt = false;

        // Class subjects
        if (cls != null && cls.subjects != null) {
            for (Subject sub : cls.subjects) {
                BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                String remark = findRemark(rec, sub.name);

                addDataCell(tbl, String.valueOf(rowIdx++), fRowB, bg, Element.ALIGN_CENTER, 32f);
                addDataCell(tbl, PdfLocalizer.translateSubject(ctx, sub.name), fRow,  bg, Element.ALIGN_LEFT,   32f);
                addDataCell(tbl, remark,                   fRow,  bg, Element.ALIGN_LEFT,   32f);
            }
        }

        // Extra fixed rows
        for (int i = 0; i < EXTRA_KEYS.length; i++) {
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
            String remark = remarkContaining(rec, EXTRA_KEYS[i]);

            String label;
            switch(i) {
                case 0:
                    label = PdfLocalizer.get(ctx, "विशेष प्रगती", "Special Progress");
                    break;
                case 1:
                    label = PdfLocalizer.get(ctx, "आवड, छंद कला, क्रीडा, साहित्य इ.", "Interests & Hobbies (Arts, Sports, Literature etc.)");
                    break;
                case 2:
                    label = PdfLocalizer.get(ctx, "सुधारणा आवश्यक", "Improvement Needed");
                    break;
                case 3:
                default:
                    label = PdfLocalizer.get(ctx, "व्यक्तिमत्व गुण विशेष\n(अभिवृत्ती, कल, मूल्ये, स्वभाव गुणविशेष)", "Personality Traits\n(Attitude, Aptitude, Values, Personality Details)");
                    break;
            }

            addDataCell(tbl, String.valueOf(rowIdx++), fRowB, bg, Element.ALIGN_CENTER, 40f);
            addDataCell(tbl, label,                   fRow,  bg, Element.ALIGN_LEFT,   40f);
            addDataCell(tbl, remark,                   fRow,  bg, Element.ALIGN_LEFT,   40f);
        }

        doc.add(tbl);

        // ── 4. Signature row ──────────────────────────────────────────────────
        try {
            doc.add(PdfGenerator.buildSignatureRow(ctx, school, cls));
        } catch (Exception ignored) {}
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private static void addNoBorderCell(PdfPTable tbl, String text, Font font, int align) {
        PdfPCell c = PdfGenerator.rawCell(text, font, BaseColor.WHITE, C_DARK, align);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(2f);
        tbl.addCell(c);
    }

    private static void addHeaderCell(PdfPTable tbl, String text, Font font, BaseColor bg) {
        PdfPCell c = PdfGenerator.rawCell(text, font, bg, C_WHITE, Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
        tbl.addCell(c);
    }

    private static void addDataCell(PdfPTable tbl, String text, Font font,
                                    BaseColor bg, int align, float minH) {
        PdfPCell c = PdfGenerator.rawCell(text != null ? text : "", font, bg, C_DARK, align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
        c.setMinimumHeight(minH);
        tbl.addCell(c);
    }
}
