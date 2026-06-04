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
import com.kartik.myschool.model.School;
import com.kartik.myschool.model.Student;
import com.kartik.myschool.utils.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static com.kartik.myschool.utils.PdfGenerator.*;

public class IndexPageGenerator {

    public static void generateIndexPage(Context ctx,
                                         School school,
                                         ClassModel cls,
                                         List<Student> students,
                                         PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx), "IndexPage_" + (cls != null ? nvl(cls.className).replaceAll("[^a-zA-Z0-9_-]", "") : "Class") + "_" + ts() + ".pdf");
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

    public static void addIndexPageContent(Document doc, Context ctx, School school, ClassModel cls, List<Student> students) throws Exception {
        // School Header
        doc.add(buildSchoolHeader(school, cls));

        // Title
        Font titleFont = sMarathiBase != null ? new Font(sMarathiBase, 22, Font.BOLD) : new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD);
        Paragraph title = new Paragraph("अनुक्रमणिका", colored(titleFont, new BaseColor(40, 40, 90)));
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

        String[] headers = {"Sr.No.", "Student Name", "Roll No.", "Birth Date", "Page No."};
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
                row[2] = new PdfPCell(new Phrase(nvl(s.rollNo), fNormal)); // Using rollNo here
                row[3] = new PdfPCell(new Phrase(nvl(s.dob), fNormal));    // Using dob instead of birthDate property? The previous code used s.dob. Oh wait, it was s.registrationNo and s.dob in original.
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
}
