package com.kartik.myschool.utils.pdf;

import android.content.Context;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
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

import static com.kartik.myschool.utils.PdfGenerator.*;

public class HPCGenerator {

    public static void generateHPC(Context ctx,
            School school,
            ClassModel cls,
            Student student,
            MarksRecord sem1,
            MarksRecord sem2,
            PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx),
                        "HPC_" + PdfGenerator.safeRoll(student) + "_" + PdfGenerator.ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addHPCContent(doc, ctx, school, cls, student, sem1, sem2);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    public static void addHPCContent(Document doc, Context ctx, School school, ClassModel cls, Student student,
            MarksRecord sem1, MarksRecord sem2) throws Exception {

        // --- TITLE ---
        PdfGenerator.addMarathiParagraph(doc, "Holistic Progress Card (HPC) / सर्वांगीण प्रगतीपत्रक", 18, true, C_DARK, 0, 15);

        // --- HEADER ---
        PdfPTable headerTbl = new PdfPTable(2);
        headerTbl.setWidthPercentage(100);
        headerTbl.setWidths(new float[] { 1f, 1f });

        String sName = (student != null ? nvl(student.name) : "");
        String sRoll = (student != null ? nvl(student.rollNo) : "");
        String sClass = (cls != null ? nvl(cls.className) : "") + " - " + (cls != null ? nvl(cls.division) : "");
        String sYear = (cls != null ? nvl(cls.academicYearLabel) : "");
        String schoolName = (school != null ? nvl(school.name) : "");
        String udise = (school != null ? nvl(school.udiseCode) : "");

        PdfPCell c1 = rawCell("Student Name: " + sName + "\nRoll No: " + sRoll, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = rawCell("Class: " + sClass + "\nYear: " + sYear, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_RIGHT);
        c2.setBorder(Rectangle.NO_BORDER);

        headerTbl.addCell(c1);
        headerTbl.addCell(c2);
        
        PdfPCell c3 = rawCell("School: " + schoolName, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        c3.setBorder(Rectangle.NO_BORDER);
        PdfPCell c4 = rawCell("UDISE: " + udise, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_RIGHT);
        c4.setBorder(Rectangle.NO_BORDER);
        
        headerTbl.addCell(c3);
        headerTbl.addCell(c4);
        
        headerTbl.setSpacingAfter(15);
        doc.add(headerTbl);

        // --- 360 DEGREE ASSESSMENT TABLE ---
        PdfGenerator.addMarathiParagraph(doc, "NEP 2020: 360 Degree Multidimensional Assessment", 14, true, C_PRIMARY, 0, 10);
        
        PdfPTable tbl = new PdfPTable(new float[]{0.6f, 3f, 1f, 1f, 1f, 1f});
        tbl.setWidthPercentage(100);
        tbl.setSpacingAfter(20);

        // Header Row
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "Sr.", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "Dimensions of Development", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "Self\nAssessment", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "Peer\nAssessment", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "Teacher\nAssessment", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "Parent\nAssessment", fSmallBold, C_HEADER_BG, C_DARK, 1, 1);

        String[] dimensions = {
            "Cognitive Development (Academic)",
            "Socio-Emotional Development",
            "Physical & Motor Development",
            "Creative & Artistic Development",
            "Communication & Language",
            "Values & Disposition"
        };

        boolean alt = false;
        for (int i = 0; i < dimensions.length; i++) {
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
            cellSpan(tbl, String.valueOf(i+1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(tbl, dimensions[i], fSmall, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);
            // Defaulting all to 3-point scale or smiling faces
            cellSpan(tbl, "✓", fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(tbl, "✓", fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(tbl, "✓", fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            cellSpan(tbl, "✓", fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
        }
        doc.add(tbl);

        // --- TEACHER'S OBSERVATIONS ---
        PdfGenerator.addMarathiParagraph(doc, "Teacher's Qualitative Remarks / शिक्षकांचा अभिप्राय", 14, true, C_PRIMARY, 0, 10);
        PdfPTable rTbl = new PdfPTable(1);
        rTbl.setWidthPercentage(100);
        rTbl.setSpacingAfter(20);
        
        String remarks = "Student is showing excellent progress in cognitive skills and works well with peers. Continues to display strong leadership qualities during group activities. Need to encourage more participation in physical activities.";
        PdfPCell rCell = rawCell(remarks, fSmall, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        rCell.setPadding(10);
        rTbl.addCell(rCell);
        doc.add(rTbl);

        // --- SIGNATURES ---
        PdfPTable sigTbl = new PdfPTable(3);
        sigTbl.setWidthPercentage(100);
        sigTbl.setSpacingBefore(40);

        PdfPCell cSig1 = rawCell("Class Teacher\n" + (cls != null ? nvl(cls.teacherName) : ""), fSmallBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        cSig1.setBorder(Rectangle.NO_BORDER);

        PdfPCell cSig2 = rawCell("Parent/Guardian", fSmallBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        cSig2.setBorder(Rectangle.NO_BORDER);

        PdfPCell cSig3 = rawCell("Headmaster\n" + (school != null ? nvl(school.principalName) : ""), fSmallBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        cSig3.setBorder(Rectangle.NO_BORDER);

        sigTbl.addCell(cSig1);
        sigTbl.addCell(cSig2);
        sigTbl.addCell(cSig3);
        doc.add(sigTbl);
    }
}
