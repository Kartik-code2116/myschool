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
import com.kartik.myschool.utils.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;

import static com.kartik.myschool.utils.PdfGenerator.*;

public class CoverPageGenerator {

    public static void generateCoverPage(Context ctx,
                                         School school,
                                         ClassModel cls,
                                         Student student,
                                         MarksRecord sem1,
                                         MarksRecord sem2,
                                         PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx), "CoverPage_" + PdfGenerator.safeRoll(student) + "_" + PdfGenerator.ts() + ".pdf");
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

    public static void addCoverPageContent(Document doc, Context ctx, School school, ClassModel cls, Student student) throws Exception {
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
}
