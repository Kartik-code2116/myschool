package com.kartik.myschool.utils.pdf;

import android.content.Context;
import android.graphics.Typeface;

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
import java.util.List;

import static com.kartik.myschool.utils.PdfGenerator.*;

public class GunapattrakGenerator {

    public static void generateGunapattrak(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           Student student,
                                           MarksRecord sem1,
                                           MarksRecord sem2,
                                           PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx), "Gunapattrak_" + PdfGenerator.safeRoll(student) + "_" + PdfGenerator.ts() + ".pdf");
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

        
    public static void cellVerticalSpan(PdfPTable tbl, Context ctx, String text, Font font, BaseColor bg, BaseColor fg, int colspan, int rowspan) {
        PdfPCell c = new PdfPCell();
        if (bg != null) c.setBackgroundColor(bg);
        c.setColspan(colspan);
        c.setRowspan(rowspan);
        c.setPadding(3);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(40); // High resolution for clear PDF printing
            paint.setColor(android.graphics.Color.BLACK);
            if (PdfGenerator.sMarathiTypeface != null) {
                paint.setTypeface(android.graphics.Typeface.create(PdfGenerator.sMarathiTypeface, android.graphics.Typeface.BOLD));
            } else {
                paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            }
            
            float width = paint.measureText(text);
            float height = paint.descent() - paint.ascent();
            
            int bitmapWidth = (int) height + 10;
            int bitmapHeight = (int) width + 10;
            
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(bitmapWidth, bitmapHeight, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
            canvas.translate(bitmapWidth / 2f, bitmapHeight / 2f);
            canvas.rotate(-90);
            
            float textX = -width / 2f;
            float textY = (height / 2f) - paint.descent();
            canvas.drawText(text, textX, textY, paint);
            
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(stream.toByteArray());
            img.setAlignment(Element.ALIGN_CENTER);
            img.scaleToFit(16f, 100f); // Scale down to fit column nicely
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, font)); // Fallback
        }
        tbl.addCell(c);
    }
    public static void cellHorizontalImageSpan(PdfPTable tbl, Context ctx, String text, Font font, BaseColor bg, BaseColor fg, int colspan, int rowspan) {
        PdfPCell c = new PdfPCell();
        if (bg != null) c.setBackgroundColor(bg);
        c.setColspan(colspan);
        c.setRowspan(rowspan);
        c.setPadding(3);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        try {
            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            paint.setColor(android.graphics.Color.rgb(fg.getRed(), fg.getGreen(), fg.getBlue()));
            paint.setTextSize(36); // Good resolution
            
            if (PdfGenerator.sMarathiTypeface != null) {
                int style = font.getStyle() == Font.BOLD ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL;
                paint.setTypeface(android.graphics.Typeface.create(PdfGenerator.sMarathiTypeface, style));
            } else {
                paint.setTypeface(font.getStyle() == Font.BOLD ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
            }
            
            float textWidth = paint.measureText(text);
            android.graphics.Paint.FontMetrics fm = paint.getFontMetrics();
            float textHeight = fm.descent - fm.ascent;
            
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap((int) Math.ceil(textWidth) + 4, (int) Math.ceil(textHeight) + 4, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
            
            canvas.drawText(text, 2, -fm.ascent + 2, paint);
            
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(stream.toByteArray());
            img.scaleToFit(100f, 16f); // Horizontal text scale
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, font)); // Fallback
        }
        tbl.addCell(c);
    }

    public static void addGunapattrakContent(Document doc, Context ctx, School school, ClassModel cls, Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {

        // School Header
        doc.add(buildSchoolHeader(school, cls));

        // Title
        Font titleFont = sMarathiBase != null ? new Font(sMarathiBase, 18, Font.BOLD, C_DARK) : new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, C_DARK);
        Paragraph title = new Paragraph("सातत्यपूर्ण सर्वंकष मूल्यमापन", titleFont);
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
        int activeSem = com.kartik.myschool.SessionContext.selectedSemester != null ? com.kartik.myschool.SessionContext.selectedSemester.number : 1;
        MarksRecord rec = (activeSem == 2) ? (sem2 != null ? sem2 : sem1) : (sem1 != null ? sem1 : sem2);
        String termLabel = (rec == sem2) ? "द्वितीय सत्र" : "प्रथम सत्र";
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
        cellVerticalSpan(tbl, ctx, "अ.नं", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        cellVerticalSpan(tbl, ctx, "तपशील", fSmallBold, C_HEADER_BG, C_DARK, 2, 1);
        cellHorizontalImageSpan(tbl, ctx, "आकारिक (अ)", fSmallBold, C_HEADER_BG, C_DARK, 9, 1); // Horizontal perfect image
        cellHorizontalImageSpan(tbl, ctx, "संकलित (ब)", fSmallBold, C_HEADER_BG, C_DARK, 4, 1); // Horizontal perfect image
        cellVerticalSpan(tbl, ctx, "अ+ब", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        cellVerticalSpan(tbl, ctx, "श्रे.गुण", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        cellVerticalSpan(tbl, ctx, "श्रेणी", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);

        // Row 2
        cellVerticalSpan(tbl, ctx, "विषय", fSmallBold, C_HEADER_BG, C_DARK, 1, 2);
        cellVerticalSpan(tbl, ctx, "गुण", fSmallBold, C_HEADER_BG, C_DARK, 1, 2);
        
        String[] formatives = {"निरीक्षण", "तोंडीकाम", "प्रात्यक्षिक", "उपक्रम", "प्रकल्प", "चाचणी", "स्वाध्याय", "इतर", "एकूण"};
        for (String f : formatives) cellVerticalSpan(tbl, ctx, f, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);
        
        String[] summatives = {"तोंडी", "प्रात्य.", "लेखी", "एकूण"};
        for (String s : summatives) cellVerticalSpan(tbl, ctx, s, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);

        // Row 3 (Numbers)
        String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8"};
        for (String n : numbers) cellSpan(tbl, n, fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER); // Keep numbers horizontal
        cellSpan(tbl, " ", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER); // Formative total empty
        for (int i=0; i<4; i++) cellSpan(tbl, " ", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER); // Summative empty


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
            cellSpan(tbl, "प्राप्त", fMicro, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
            
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
                
                // ROWSPAN = 2 for Totals and Grade
                cellSpan(tbl, str(d.grandTotal), fSmallBold, bg, C_DARK, 1, 2, Element.ALIGN_CENTER);
                cellSpan(tbl, str(d.grandTotal), fSmallBold, bg, C_DARK, 1, 2, Element.ALIGN_CENTER); // Grade marks is just grand total
                cellSpan(tbl, nvl(d.grade), fSmallBold, bg, C_DARK, 1, 2, Element.ALIGN_CENTER);
            } else {
                for (int k = 0; k < 9; k++) cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                if (isNonAcademic) {
                    cellSpan(tbl, " ", fSmall, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                } else {
                    for (int k = 0; k < 4; k++) cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                }
                cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 2, Element.ALIGN_CENTER);
                cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 2, Element.ALIGN_CENTER);
                cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 2, Element.ALIGN_CENTER);
            }

            // Row B: पैकी
            BaseColor paikiBg = new BaseColor(245, 245, 245); // Light grey shading for "पैकी" row
            cellSpan(tbl, "पैकी", fMicro, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
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
}
