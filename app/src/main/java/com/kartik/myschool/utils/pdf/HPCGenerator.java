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
import com.kartik.myschool.utils.pdf.MarathiText;
import com.kartik.myschool.utils.pdf.PdfLocalizer;

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
        String title = PdfLocalizer.get(ctx, "सर्वांगीण प्रगतीपत्रक (HPC)", "Holistic Progress Card (HPC)");
        PdfGenerator.addMarathiParagraph(doc, title, 18, true, C_DARK, 0, 15);

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

        String lblStudent = PdfLocalizer.get(ctx, "विद्यार्थ्याचे नाव: ", "Student Name: ");
        String lblRoll = PdfLocalizer.get(ctx, "हजेरी क्र.: ", "Roll No: ");
        String lblClass = PdfLocalizer.get(ctx, "इयत्ता: ", "Class: ");
        String lblYear = PdfLocalizer.get(ctx, "वर्ष: ", "Year: ");
        String lblSchool = PdfLocalizer.get(ctx, "शाळेचे नाव: ", "School: ");
        String lblUdise = PdfLocalizer.get(ctx, "युडायस: ", "UDISE: ");

        String c1Text = lblStudent + sName + "\n" + lblRoll + sRoll;
        String c2Text = lblClass + sClass + "\n" + lblYear + sYear;
        String c3Text = lblSchool + schoolName;
        String c4Text = lblUdise + udise;

        PdfPCell c1 = marathiCell(ctx, c1Text, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = marathiCell(ctx, c2Text, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_RIGHT);
        c2.setBorder(Rectangle.NO_BORDER);

        headerTbl.addCell(c1);
        headerTbl.addCell(c2);
        
        PdfPCell c3 = marathiCell(ctx, c3Text, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        c3.setBorder(Rectangle.NO_BORDER);
        PdfPCell c4 = marathiCell(ctx, c4Text, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_RIGHT);
        c4.setBorder(Rectangle.NO_BORDER);
        
        headerTbl.addCell(c3);
        headerTbl.addCell(c4);
        
        headerTbl.setSpacingAfter(15);
        doc.add(headerTbl);

        // --- 360 DEGREE ASSESSMENT TABLE ---
        String lblNep = PdfLocalizer.get(ctx, "राष्ट्रीय शैक्षणिक धोरण २०२०: ३६० अंश बहुआयामी मूल्यमापन", "NEP 2020: 360 Degree Multidimensional Assessment");
        PdfGenerator.addMarathiParagraph(doc, lblNep, 14, true, C_PRIMARY, 0, 10);
        
        PdfPTable tbl = new PdfPTable(new float[]{0.6f, 3f, 1f, 1f, 1f, 1f});
        tbl.setWidthPercentage(100);
        tbl.setSpacingAfter(20);

        // Header Row
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "अ.क्र.", "Sr."), fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "विकासाची क्षेत्रे", "Dimensions of Development"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "स्वयं\nमूल्यमापन", "Self\nAssessment"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "सहपाठी\nमूल्यमापन", "Peer\nAssessment"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "शिक्षक\nमूल्यमापन", "Teacher\nAssessment"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "पालक\nमूल्यमापन", "Parent\nAssessment"), fSmallBold, C_HEADER_BG, C_DARK, 1, 1);

        String[] dimensions = {
            PdfLocalizer.get(ctx, "बौद्धिक विकास (शैक्षणिक)", "Cognitive Development (Academic)"),
            PdfLocalizer.get(ctx, "सामाजिक-भावनिक विकास", "Socio-Emotional Development"),
            PdfLocalizer.get(ctx, "शारीरिक आणि कारक विकास", "Physical & Motor Development"),
            PdfLocalizer.get(ctx, "सर्जनशील व कलात्मक विकास", "Creative & Artistic Development"),
            PdfLocalizer.get(ctx, "संभाषण आणि भाषा विकास", "Communication & Language"),
            PdfLocalizer.get(ctx, "मूल्ये आणि वृत्ती", "Values & Disposition")
        };

        boolean alt = false;
        for (int i = 0; i < dimensions.length; i++) {
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, String.valueOf(i+1), fSmall, bg, C_DARK, 1, 1);
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, dimensions[i], fSmall, bg, C_DARK, 1, 1);
            // Defaulting all to 3-point scale or smiling faces
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "✓", fSmallBold, bg, C_DARK, 1, 1);
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "✓", fSmallBold, bg, C_DARK, 1, 1);
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "✓", fSmallBold, bg, C_DARK, 1, 1);
            GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "✓", fSmallBold, bg, C_DARK, 1, 1);
        }
        doc.add(tbl);

        // --- TEACHER'S OBSERVATIONS ---
        String lblTeacherRemarks = PdfLocalizer.get(ctx, "शिक्षकांचा अभिप्राय", "Teacher's Qualitative Remarks");
        PdfGenerator.addMarathiParagraph(doc, lblTeacherRemarks, 14, true, C_PRIMARY, 0, 10);
        PdfPTable rTbl = new PdfPTable(1);
        rTbl.setWidthPercentage(100);
        rTbl.setSpacingAfter(20);
        
        String remarks = PdfLocalizer.get(ctx, "विद्यार्थी बौद्धिक कौशल्यांमध्ये उत्तम प्रगती दर्शवत आहे आणि सहकाऱ्यांसोबत चांगले काम करतो. शारीरिक उपक्रमांमध्ये अधिक सहभाग घेण्यास प्रोत्साहित करणे आवश्यक आहे.", "Student is showing excellent progress in cognitive skills and works well with peers. Continues to display strong leadership qualities during group activities. Need to encourage more participation in physical activities.");
        PdfPCell rCell = marathiCell(ctx, remarks, fSmall, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        rCell.setPadding(10);
        rTbl.addCell(rCell);
        doc.add(rTbl);

        // --- SIGNATURES ---
        PdfPTable sigTbl = new PdfPTable(3);
        sigTbl.setWidthPercentage(100);
        sigTbl.setSpacingBefore(40);

        String lblClassTeacher = PdfLocalizer.get(ctx, "वर्गशिक्षक\n", "Class Teacher\n");
        String lblParent = PdfLocalizer.get(ctx, "पालक", "Parent/Guardian");
        String lblHeadmaster = PdfLocalizer.get(ctx, "मुख्याध्यापक\n", "Headmaster\n");

        PdfPCell cSig1 = marathiCell(ctx, lblClassTeacher + (cls != null ? nvl(cls.teacherName) : ""), fSmallBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        cSig1.setBorder(Rectangle.NO_BORDER);

        PdfPCell cSig2 = marathiCell(ctx, lblParent, fSmallBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        cSig2.setBorder(Rectangle.NO_BORDER);

        PdfPCell cSig3 = marathiCell(ctx, lblHeadmaster + (school != null ? nvl(school.principalName) : ""), fSmallBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        cSig3.setBorder(Rectangle.NO_BORDER);

        sigTbl.addCell(cSig1);
        sigTbl.addCell(cSig2);
        sigTbl.addCell(cSig3);
        doc.add(sigTbl);
    }

    private static PdfPCell marathiCell(Context ctx, String text, Font font, BaseColor bg, BaseColor fg, int align) {
        PdfPCell c = new PdfPCell();
        if (bg != null) c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4);
        try {
            boolean bold = (font == fSmallBold || font == fTitle);
            int size = 10;
            if (font == fTitle) size = 18;
            
            int colorInt = android.graphics.Color.rgb(fg.getRed(), fg.getGreen(), fg.getBlue());
            String[] lines = text.split("\n");
            for (String line : lines) {
                com.itextpdf.text.Image img = MarathiText.renderLine(line, size, bold, colorInt);
                if (align == Element.ALIGN_CENTER) img.setAlignment(com.itextpdf.text.Image.MIDDLE);
                else if (align == Element.ALIGN_RIGHT) img.setAlignment(com.itextpdf.text.Image.RIGHT);
                else img.setAlignment(com.itextpdf.text.Image.LEFT);
                c.addElement(img);
            }
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, font));
        }
        return c;
    }
}
