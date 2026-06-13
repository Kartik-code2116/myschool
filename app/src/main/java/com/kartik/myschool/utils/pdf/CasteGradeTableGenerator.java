package com.kartik.myschool.utils.pdf;

import android.content.Context;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
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
import com.kartik.myschool.utils.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import static com.kartik.myschool.utils.PdfGenerator.C_DARK;
import static com.kartik.myschool.utils.PdfGenerator.C_HEADER_BG;
import static com.kartik.myschool.utils.PdfGenerator.C_ROW_ALT;
import static com.kartik.myschool.utils.PdfGenerator.C_WHITE;
import static com.kartik.myschool.utils.PdfGenerator.fSmall;
import static com.kartik.myschool.utils.PdfGenerator.fSmallBold;
import static com.kartik.myschool.utils.PdfGenerator.fTitle;
import static com.kartik.myschool.utils.PdfGenerator.nvl;
import static com.kartik.myschool.utils.PdfGenerator.str;

/**
 * Option 17 — Caste-wise Boys-Girls Grade Table (जात श्रेणी तक्ता)
 */
public class CasteGradeTableGenerator {

    public static void generateCasteGradeTable(Context ctx,
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
                        "CasteGradeTable_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(20, 20, 30, 30);

                addContent(doc, ctx, school, cls, students, marksMap, isSem2);

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    private static void addContent(Document doc, Context ctx, School school, ClassModel cls,
                                   List<Student> students, Map<String, MarksRecord> marksMap, boolean isSem2) throws Exception {

        // ── Header ─────────────────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(3);
        hdr.setWidthPercentage(100);
        hdr.setWidths(new float[]{1.5f, 2f, 1.5f});

        PdfPCell cL = new PdfPCell();
        cL.setBorder(Rectangle.NO_BORDER);
        String udise = PdfLocalizer.get(ctx, "युडायस: ", "UDISE: ") + nvl(school != null ? school.udiseCode : "");
        String sName = PdfLocalizer.get(ctx, "शाळा: ", "School: ") + nvl(school != null ? school.name : "");
        try {
            com.itextpdf.text.Image uImg = MarathiText.renderLine(udise, 9, true, android.graphics.Color.BLACK);
            cL.addElement(uImg);
            com.itextpdf.text.Image sImg = MarathiText.renderLine(sName, 9, false, android.graphics.Color.BLACK);
            cL.addElement(sImg);
        } catch (Exception e) {
            cL.addElement(new Phrase(udise, fSmallBold));
            cL.addElement(new Phrase(sName, fSmall));
        }
        hdr.addCell(cL);

        PdfPCell cC = new PdfPCell();
        cC.setBorder(Rectangle.NO_BORDER);
        cC.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            com.itextpdf.text.Image titleImg = MarathiText.renderLine(PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"), 16, true, android.graphics.Color.BLACK);
            titleImg.setAlignment(Element.ALIGN_CENTER);
            cC.addElement(titleImg);
            String semStr = isSem2 ? PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester") : PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester");
            com.itextpdf.text.Image subImg = MarathiText.renderLine(semStr, 10, false, android.graphics.Color.BLACK);
            subImg.setAlignment(Element.ALIGN_CENTER);
            cC.addElement(subImg);
        } catch (Exception e) {
            cC.addElement(new Phrase(PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"), fTitle));
            cC.addElement(new Phrase(isSem2 ? PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester") : PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester"), fSmall));
        }
        hdr.addCell(cC);

        PdfPCell cR = new PdfPCell();
        cR.setBorder(Rectangle.NO_BORDER);
        cR.setHorizontalAlignment(Element.ALIGN_RIGHT);
        String yearVal = PdfLocalizer.get(ctx, "सन : ", "Year : ") + nvl(cls != null ? cls.academicYearLabel : "");
        String cDivVal = PdfLocalizer.get(ctx, "इयत्ता: ", "Class: ") + nvl(cls != null ? cls.className : "") + PdfLocalizer.get(ctx, ", तुकडी: ", ", Division: ") + nvl(cls != null ? cls.division : "-");
        try {
            com.itextpdf.text.Image yImg = MarathiText.renderLine(yearVal, 9, true, android.graphics.Color.BLACK);
            yImg.setAlignment(Element.ALIGN_RIGHT);
            cR.addElement(yImg);
            com.itextpdf.text.Image dImg = MarathiText.renderLine(cDivVal, 9, true, android.graphics.Color.BLACK);
            dImg.setAlignment(Element.ALIGN_RIGHT);
            cR.addElement(dImg);
        } catch (Exception e) {
            Phrase pYear = new Phrase(yearVal, fSmallBold);
            com.itextpdf.text.Paragraph prYear = new com.itextpdf.text.Paragraph(pYear);
            prYear.setAlignment(Element.ALIGN_RIGHT);
            cR.addElement(prYear);

            Phrase pDiv = new Phrase(cDivVal, fSmallBold);
            com.itextpdf.text.Paragraph prDiv = new com.itextpdf.text.Paragraph(pDiv);
            prDiv.setAlignment(Element.ALIGN_RIGHT);
            cR.addElement(prDiv);
        }
        hdr.addCell(cR);

        hdr.setSpacingAfter(10);
        doc.add(hdr);

        // ── Data Aggregation ──────────────────────────────────────────────────
        // casteIndex -> gradeIndex -> genderIndex
        // casteIndex: 0: SC, 1: ST, 2: VJ, 3: NT, 4: OBC, 5: OPEN, 6: TOTAL
        // gradeIndex: 0: A1, 1: A2, 2: B1, 3: B2, 4: C1, 5: C2, 6: D, 7: E1, 8: E2, 9: TOTAL
        // genderIndex: 0: Boys, 1: Girls
        int[][][] counts = new int[7][10][2];
        int presentBoys = 0, presentGirls = 0;
        int enrolledBoys = 0, enrolledGirls = 0;

        if (students != null) {
            for (Student s : students) {
                boolean isGirl = s.gender != null && (s.gender.equalsIgnoreCase("Female") 
                        || s.gender.equalsIgnoreCase("मुली") 
                        || s.gender.equalsIgnoreCase("मुलगी")
                        || s.gender.equals("2"));
                int gIdx = isGirl ? 1 : 0;
                int cIdx = getCasteCategoryIndex(s.cast);

                if (isGirl) enrolledGirls++;
                else enrolledBoys++;

                // Track attendance
                int presentDays = 0;
                if (s.monthlyAttendance != null) {
                    for (String m : s.monthlyAttendance.keySet()) {
                        String att = s.monthlyAttendance.get(m);
                        if (att != null && att.contains("/")) {
                            try { presentDays += Integer.parseInt(att.split("/")[0].trim()); } catch (Exception ignored) {}
                        } else if (att != null && !att.isEmpty()) {
                            try { presentDays += Integer.parseInt(att.trim()); } catch (Exception ignored) {}
                        }
                    }
                }
                if (presentDays > 0) {
                    if (isGirl) presentGirls++;
                    else presentBoys++;
                }

                MarksRecord rec = marksMap != null ? marksMap.get(s.id) : null;
                String grade = rec != null ? nvl(rec.grade) : "";
                int gradeIdx = getGradeIndex(grade);

                if (gradeIdx != -1) {
                    counts[cIdx][gradeIdx][gIdx]++;
                    counts[cIdx][9][gIdx]++; // Caste Total
                    counts[6][gradeIdx][gIdx]++; // Grade Total
                    counts[6][9][gIdx]++; // Grand Total
                }
            }
        }

        // ── Main Table ──────────────────────────────────────────────────────
        // 2 cols for Sr/Caste, plus 10 pairs (Boys/Girls) = 22 columns
        float[] widths = new float[22];
        widths[0] = 0.5f; // अ.नं
        widths[1] = 2.0f; // जात संवर्ग
        for (int i = 2; i < 22; i++) {
            widths[i] = 0.5f;
        }

        PdfPTable tbl = new PdfPTable(widths);
        tbl.setWidthPercentage(100);

        // Header Row 1
        MarathiText.cell(tbl, PdfLocalizer.get(ctx, "अ.नं", "Sr.No."), 10, true, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
        MarathiText.cell(tbl, PdfLocalizer.get(ctx, "जात संवर्ग", "Caste Category"), 10, true, C_HEADER_BG, C_DARK, 1, 2, Element.ALIGN_CENTER);
        
        String[] gradeHeaders = PdfLocalizer.isEnglish(ctx)
                ? new String[]{"A-1", "A-2", "B-1", "B-2", "C-1", "C-2", "D", "E-1", "E-2", "Total"}
                : new String[]{"अ-1", "अ-2", "ब-1", "ब-2", "क-1", "क-2", "ड", "इ-1", "इ-2", "एकूण"};
        for (String g : gradeHeaders) {
            MarathiText.cell(tbl, g, 10, true, C_HEADER_BG, C_DARK, 2, 1, Element.ALIGN_CENTER);
        }

        // Header Row 2
        for (int i = 0; i < 10; i++) {
            MarathiText.cell(tbl, PdfLocalizer.get(ctx, "मुले", "Boys"), 9, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
            MarathiText.cell(tbl, PdfLocalizer.get(ctx, "मुली", "Girls"), 9, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }

        // Data Rows
        String[] casteNames = PdfLocalizer.isEnglish(ctx)
                ? new String[]{"SC", "ST", "VJ", "NT", "OBC", "OPEN", "Total"}
                : new String[]{"अनु.जाती", "अनु.जमाती", "विमुक्त जाती", "भटक्या जाती", "इतर मागासवर्ग", "बिगर मागास", "एकूण"};
        for (int c = 0; c < 7; c++) {
            BaseColor bg = (c % 2 == 1) ? C_ROW_ALT : C_WHITE;
            
            PdfPCell cellSr = new PdfPCell(new Phrase(String.valueOf(c + 1), fSmall));
            cellSr.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellSr.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellSr.setBackgroundColor(bg);
            cellSr.setPadding(4);
            tbl.addCell(cellSr);

            MarathiText.cell(tbl, casteNames[c], 9, false, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);

            for (int g = 0; g < 10; g++) {
                int boys = counts[c][g][0];
                int girls = counts[c][g][1];
                tbl.addCell(numCell(boys, bg));
                tbl.addCell(numCell(girls, bg));
            }
        }
        
        doc.add(tbl);
        
        // ── Sub Table (Stats) ───────────────────────────────────────────────
        PdfPTable statTbl = new PdfPTable(4);
        statTbl.setWidthPercentage(40);
        statTbl.setHorizontalAlignment(Element.ALIGN_LEFT);
        statTbl.setSpacingBefore(10f);
        statTbl.setWidths(new float[]{1.5f, 1f, 1f, 1f});

        MarathiText.cell(statTbl, PdfLocalizer.get(ctx, "तपशील", "Details"), 10, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        MarathiText.cell(statTbl, PdfLocalizer.get(ctx, "मुले", "Boys"), 10, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        MarathiText.cell(statTbl, PdfLocalizer.get(ctx, "मुली", "Girls"), 10, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        MarathiText.cell(statTbl, PdfLocalizer.get(ctx, "एकूण", "Total"), 10, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);

        // Enrolled
        MarathiText.cell(statTbl, PdfLocalizer.get(ctx, "पट", "Roll"), 9, false, C_WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);
        statTbl.addCell(numCell(enrolledBoys, C_WHITE));
        statTbl.addCell(numCell(enrolledGirls, C_WHITE));
        statTbl.addCell(numCell(enrolledBoys + enrolledGirls, C_WHITE));

        // Present
        MarathiText.cell(statTbl, PdfLocalizer.get(ctx, "उपस्थिती", "Attendance"), 9, false, C_WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);
        statTbl.addCell(numCell(presentBoys, C_WHITE));
        statTbl.addCell(numCell(presentGirls, C_WHITE));
        statTbl.addCell(numCell(presentBoys + presentGirls, C_WHITE));

        doc.add(statTbl);

        // ── Signatures ──────────────────────────────────────────────────────
        PdfPTable sigTbl = new PdfPTable(2);
        sigTbl.setWidthPercentage(80);
        sigTbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sigTbl.setSpacingBefore(30f);

        PdfPCell sig1 = new PdfPCell();
        sig1.setBorder(Rectangle.NO_BORDER);
        sig1.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            com.itextpdf.text.Image img1 = MarathiText.renderLine(PdfLocalizer.get(ctx, "वर्गशिक्षक स्वाक्षरी", "Class Teacher Signature"), 10, false, android.graphics.Color.BLACK);
            img1.setAlignment(Element.ALIGN_CENTER);
            sig1.addElement(img1);
            if (cls != null && cls.teacherName != null && !cls.teacherName.isEmpty()) {
                com.itextpdf.text.Image img2 = MarathiText.renderLine(cls.teacherName, 10, false, android.graphics.Color.BLACK);
                img2.setAlignment(Element.ALIGN_CENTER);
                sig1.addElement(img2);
            }
        } catch (Exception e) {
            sig1.addElement(new Phrase(PdfLocalizer.get(ctx, "वर्गशिक्षक स्वाक्षरी\n", "Class Teacher Signature\n") + (cls != null ? nvl(cls.teacherName) : ""), fSmall));
        }
        sigTbl.addCell(sig1);

        PdfPCell sig2 = new PdfPCell();
        sig2.setBorder(Rectangle.NO_BORDER);
        sig2.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            com.itextpdf.text.Image img1 = MarathiText.renderLine(PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी", "Headmaster Signature"), 10, false, android.graphics.Color.BLACK);
            img1.setAlignment(Element.ALIGN_CENTER);
            sig2.addElement(img1);
        } catch (Exception e) {
            sig2.addElement(new Phrase(PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी", "Headmaster Signature"), fSmall));
        }
        sigTbl.addCell(sig2);

        doc.add(sigTbl);
    }

    private static int getCasteCategoryIndex(String caste) {
        if (caste == null) return 5; // Default: बिगर मागास
        String c = caste.toUpperCase().trim();
        if (c.contains("SC") || c.contains("अनु.जाती") || c.contains("अनुसुचित जाती") || c.equals("1") || c.equals("0")) return 0;
        if (c.contains("ST") || c.contains("अनु.जमाती") || c.contains("अनुसुचित जमाती") || c.equals("2")) return 1;
        if (c.contains("VJ") || c.contains("विमुक्त") || c.equals("3")) return 2;
        if (c.contains("NT") || c.contains("भटक्या") || c.equals("4")) return 3;
        if (c.contains("OBC") || c.contains("SBC") || c.contains("इतर") || c.contains("मागास") || c.equals("5") || c.equals("6")) return 4;
        return 5;
    }

    private static int getGradeIndex(String grade) {
        if (grade == null) return -1;
        String g = grade.toUpperCase().trim();
        if (g.equals("A-1") || g.equals("A1") || g.equals("अ-1") || g.equals("अ१")) return 0;
        if (g.equals("A-2") || g.equals("A2") || g.equals("अ-2") || g.equals("अ२")) return 1;
        if (g.equals("B-1") || g.equals("B1") || g.equals("ब-1") || g.equals("ब१")) return 2;
        if (g.equals("B-2") || g.equals("B2") || g.equals("ब-2") || g.equals("ब२")) return 3;
        if (g.equals("C-1") || g.equals("C1") || g.equals("क-1") || g.equals("क१")) return 4;
        if (g.equals("C-2") || g.equals("C2") || g.equals("क-2") || g.equals("क२")) return 5;
        if (g.equals("D") || g.equals("ड")) return 6;
        if (g.equals("E-1") || g.equals("E1") || g.equals("इ-1") || g.equals("इ१") || g.equals("ई-1")) return 7;
        if (g.equals("E-2") || g.equals("E2") || g.equals("इ-2") || g.equals("इ२") || g.equals("ई-2")) return 8;
        return -1;
    }

    private static PdfPCell numCell(int val, BaseColor bg) {
        String str = val > 0 ? String.valueOf(val) : "-";
        PdfPCell c = new PdfPCell(new Phrase(str, fSmall));
        c.setBackgroundColor(bg);
        c.setBorderColor(C_DARK);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4);
        return c;
    }
}
