package com.kartik.myschool.utils.pdf;

import android.content.Context;
import android.util.Log;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
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

public class DescriptiveRemarksGenerator {

    private static final String TAG = "DESC_PDF";

    /**
     * Finds the remark for a given subject from a MarksRecord.
     * Tries EVERY possible way to match the subject key.
     */
    private static String findRemark(MarksRecord rec, String subjectName) {
        if (rec == null || rec.detailedMarks == null || subjectName == null) return "";

        // Strategy 1: PdfGenerator.detail() with multi-strategy matching
        MarksRecord.SubjectMarksDetail d = detail(rec, subjectName);
        if (d != null && d.remark != null && !d.remark.trim().isEmpty()) {
            return d.remark.replace("||", ", ").trim();
        }

        // Strategy 2: Case-insensitive + loose matching
        String safeName = MarksRecord.sanitizeKey(subjectName).toLowerCase();
        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> entry : rec.detailedMarks.entrySet()) {
            String key = entry.getKey();
            MarksRecord.SubjectMarksDetail val = entry.getValue();
            if (val != null && val.remark != null && !val.remark.trim().isEmpty()) {
                String safeKey = key != null ? MarksRecord.sanitizeKey(key).toLowerCase() : "";
                if (safeKey.equals(safeName) || safeKey.contains(safeName) || safeName.contains(safeKey)) {
                    return val.remark.replace("||", ", ").trim();
                }
            }
        }

        return "";
    }

    /**
     * Builds the BEST possible remarks map for each student by combining:
     * 1. The provided sem1/sem2 maps from getMarksForClassAndSemester
     * 2. AppCache.cachedDescriptiveMarksMap (written by DescriptiveEntriesFragment)
     * 3. AppCache.cachedMarksMap (general marks cache)
     *
     * For each student, picks the MarksRecord that has the most descriptive remarks.
     */
    private static Map<String, MarksRecord> buildBestRemarksMap(
            Map<String, MarksRecord> sem1Marks,
            Map<String, MarksRecord> sem2Marks,
            List<Student> students,
            String classId) {

        java.util.Map<String, MarksRecord> best = new java.util.HashMap<>();

        // Collect ALL candidate records per student
        if (students != null) {
            for (Student s : students) {
                MarksRecord bestRec = null;
                int bestRemarkCount = 0;

                // Candidate 1: sem1 map
                MarksRecord r1 = sem1Marks != null ? sem1Marks.get(s.id) : null;
                int c1 = countRemarks(r1);
                if (c1 > bestRemarkCount) { bestRec = r1; bestRemarkCount = c1; }

                // Candidate 2: sem2 map
                MarksRecord r2 = sem2Marks != null ? sem2Marks.get(s.id) : null;
                int c2 = countRemarks(r2);
                if (c2 > bestRemarkCount) { bestRec = r2; bestRemarkCount = c2; }

                // Candidate 3: AppCache.cachedDescriptiveMarksMap
                if (com.kartik.myschool.AppCache.cachedDescriptiveMarksMap != null
                        && java.util.Objects.equals(classId, com.kartik.myschool.AppCache.cachedDescriptiveClassId)) {
                    MarksRecord r3 = com.kartik.myschool.AppCache.cachedDescriptiveMarksMap.get(s.id);
                    int c3 = countRemarks(r3);
                    if (c3 > bestRemarkCount) { bestRec = r3; bestRemarkCount = c3; }
                }

                // Candidate 4: AppCache.cachedMarksMap
                if (com.kartik.myschool.AppCache.cachedMarksMap != null
                        && java.util.Objects.equals(classId, com.kartik.myschool.AppCache.cachedClassIdForStudents)) {
                    MarksRecord r4 = com.kartik.myschool.AppCache.cachedMarksMap.get(s.id);
                    int c4 = countRemarks(r4);
                    if (c4 > bestRemarkCount) { bestRec = r4; bestRemarkCount = c4; }
                }

                if (bestRec != null) {
                    best.put(s.id, bestRec);
                }

                Log.d(TAG, "Student " + s.name + " (id=" + s.id + "): "
                        + "sem1=" + c1 + " sem2=" + c2
                        + " descCache=" + countRemarks(com.kartik.myschool.AppCache.cachedDescriptiveMarksMap != null ? com.kartik.myschool.AppCache.cachedDescriptiveMarksMap.get(s.id) : null)
                        + " marksCache=" + countRemarks(com.kartik.myschool.AppCache.cachedMarksMap != null ? com.kartik.myschool.AppCache.cachedMarksMap.get(s.id) : null)
                        + " → bestRemarkCount=" + bestRemarkCount);
            }
        }
        return best;
    }

    /**
     * Counts how many subjects have non-empty remarks in a MarksRecord.
     */
    private static int countRemarks(MarksRecord rec) {
        if (rec == null || rec.detailedMarks == null) return 0;
        int count = 0;
        for (MarksRecord.SubjectMarksDetail d : rec.detailedMarks.values()) {
            if (d != null && d.remark != null && !d.remark.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

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
                File out = new File(PdfGenerator.outDir(ctx), "DescriptiveRemarksRegister_" + PdfGenerator.ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);

                String classId = cls != null ? cls.id : null;

                Log.d(TAG, "=== GENERATING DESCRIPTIVE PDF ===");
                Log.d(TAG, "sem1Marks=" + (sem1Marks != null ? sem1Marks.size() : "null")
                        + " sem2Marks=" + (sem2Marks != null ? sem2Marks.size() : "null")
                        + " students=" + (students != null ? students.size() : "null"));

                // Dump ALL keys from sem1 for the first student to debug
                if (sem1Marks != null && students != null && !students.isEmpty()) {
                    MarksRecord firstRec = sem1Marks.get(students.get(0).id);
                    if (firstRec != null && firstRec.detailedMarks != null) {
                        Log.d(TAG, "sem1 first student detailedMarks keys: " + firstRec.detailedMarks.keySet());
                        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> e : firstRec.detailedMarks.entrySet()) {
                            MarksRecord.SubjectMarksDetail v = e.getValue();
                            Log.d(TAG, "  key=" + e.getKey()
                                    + " remark=" + (v != null && v.remark != null ? "'" + v.remark + "'" : "null")
                                    + " akarik=" + (v != null ? v.akarikTotal : 0));
                        }
                    } else {
                        Log.d(TAG, "sem1 first student: " + (firstRec == null ? "NO RECORD" : "detailedMarks=null"));
                    }
                }
                if (sem2Marks != null && students != null && !students.isEmpty()) {
                    MarksRecord firstRec = sem2Marks.get(students.get(0).id);
                    if (firstRec != null && firstRec.detailedMarks != null) {
                        Log.d(TAG, "sem2 first student detailedMarks keys: " + firstRec.detailedMarks.keySet());
                        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> e : firstRec.detailedMarks.entrySet()) {
                            MarksRecord.SubjectMarksDetail v = e.getValue();
                            Log.d(TAG, "  key=" + e.getKey()
                                    + " remark=" + (v != null && v.remark != null ? "'" + v.remark + "'" : "null")
                                    + " akarik=" + (v != null ? v.akarikTotal : 0));
                        }
                    } else {
                        Log.d(TAG, "sem2 first student: " + (firstRec == null ? "NO RECORD" : "detailedMarks=null"));
                    }
                }

                // Build the best possible map by checking all sources
                Map<String, MarksRecord> bestMap = buildBestRemarksMap(sem1Marks, sem2Marks, students, classId);
                Log.d(TAG, "bestMap size=" + bestMap.size());

                if (cls.subjects != null) {
                    for (int si = 0; si < cls.subjects.size(); si++) {
                        if (si > 0) doc.newPage();
                        Subject sub = cls.subjects.get(si);

                        // Title
                        Font titleFont = PdfGenerator.sMarathiBase != null ? new Font(PdfGenerator.sMarathiBase, 16, Font.BOLD, C_DARK) : new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, C_DARK);
                        Paragraph title = new Paragraph("वर्णनात्मक नोंदी (Descriptive Remarks)", titleFont);
                        title.setAlignment(Element.ALIGN_CENTER);
                        title.setSpacingAfter(15);
                        doc.add(title);

                        // Header Info table
                        PdfPTable top = new PdfPTable(3);
                        top.setWidthPercentage(100); top.setSpacingAfter(10);
                        String schoolStr = "School: " + (school != null ? nvl(school.name) : "");
                        String classStr = "Class: " + (cls != null ? nvl(cls.className) : "") + " " + (cls != null ? nvl(cls.division) : "");
                        String yearStr = "Year: " + (cls != null ? nvl(cls.academicYearLabel) : "");

                        cellSpan(top, schoolStr + "\n" + classStr, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_LEFT);
                        cellSpan(top, "Subject: " + sub.name, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(top, yearStr, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_RIGHT);
                        doc.add(top);

                        // Table: Sr No | Student Name | Roll No | Descriptive Remarks
                        PdfPTable tbl = new PdfPTable(new float[]{0.6f, 2.5f, 1.0f, 6.0f});
                        tbl.setWidthPercentage(100); tbl.setSpacingBefore(6);

                        cell(tbl, "अ.क्र.",  fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, "विद्यार्थ्याचे नाव",  fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, "हजेरी क्र.",  fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, "वर्णनात्मक नोंदी",   fBold, C_HEADER_BG, C_DARK,  1, Element.ALIGN_CENTER);

                        boolean alt = false;
                        if (students != null) {
                            for (int i = 0; i < students.size(); i++) {
                                Student s = students.get(i);
                                BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;

                                // Get the best record for this student from combined sources
                                MarksRecord bestRec = bestMap.get(s.id);
                                String remark = findRemark(bestRec, sub.name);

                                // If still empty, try a brute-force: iterate ALL subject details
                                // and try index-based matching (subject at position si)
                                if (remark.isEmpty() && bestRec != null && bestRec.detailedMarks != null) {
                                    int idx = 0;
                                    for (MarksRecord.SubjectMarksDetail val : bestRec.detailedMarks.values()) {
                                        if (idx == si && val != null && val.remark != null && !val.remark.trim().isEmpty()) {
                                            remark = val.remark.replace("||", ", ").trim();
                                            Log.d(TAG, "  FALLBACK index match for " + s.name + " sub=" + sub.name + " at idx=" + si);
                                            break;
                                        }
                                        idx++;
                                    }
                                }

                                if (si == 0) { // Log only for first subject to avoid spam
                                    Log.d(TAG, "Row " + i + " " + s.name + ": remark='" + remark + "'");
                                }

                                PdfPCell nc = rawCell(String.valueOf(i + 1), fNormal, bg, C_DARK, Element.ALIGN_CENTER); tbl.addCell(nc);
                                PdfPCell lc = rawCell(nvl(s.name), fNormal, bg, C_DARK, Element.ALIGN_LEFT); lc.setMinimumHeight(28f); tbl.addCell(lc);
                                PdfPCell rc = rawCell(nvl(s.rollNo), fNormal, bg, C_DARK, Element.ALIGN_CENTER); rc.setMinimumHeight(28f); tbl.addCell(rc);
                                PdfPCell remCell = rawCell(remark.isEmpty() ? "" : remark, fNormal, bg, C_DARK, Element.ALIGN_LEFT); remCell.setMinimumHeight(28f); tbl.addCell(remCell);
                            }
                        }
                        doc.add(tbl);
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
}
