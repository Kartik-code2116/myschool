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
                File out = new File(PdfGenerator.outDir(ctx),
                        "Gunapattrak_" + PdfGenerator.safeRoll(student) + "_" + PdfGenerator.ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter __writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                __writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addGunapattrakContent(doc, ctx, school, cls, student, sem1, sem2);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    public static void cellVerticalSpan(PdfPTable tbl, Context ctx, String text, Font font, BaseColor bg, BaseColor fg,
            int colspan, int rowspan) {
        PdfPCell c = new PdfPCell();
        if (bg != null)
            c.setBackgroundColor(bg);
        c.setColspan(colspan);
        c.setRowspan(rowspan);
        c.setPaddingTop(3);
        c.setPaddingBottom(3);
        c.setPaddingLeft(1);
        c.setPaddingRight(1);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(40); // High resolution for clear PDF printing
            paint.setColor(android.graphics.Color.BLACK);
            if (PdfGenerator.sMarathiTypeface != null) {
                paint.setTypeface(android.graphics.Typeface.create(PdfGenerator.sMarathiTypeface,
                        android.graphics.Typeface.BOLD));
            } else {
                paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            }

            float width = paint.measureText(text);
            float height = paint.descent() - paint.ascent();

            int bitmapWidth = (int) height + 10;
            int bitmapHeight = (int) width + 10;

            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(bitmapWidth, bitmapHeight,
                    android.graphics.Bitmap.Config.ARGB_8888);
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
            img.scaleToFit(12f, 100f); // Scale down to fit column nicely
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, font)); // Fallback
        }
        tbl.addCell(c);
    }

    public static void cellHorizontalImageSpan(PdfPTable tbl, Context ctx, String text, Font font, BaseColor bg,
            BaseColor fg, int colspan, int rowspan) {
        PdfPCell c = new PdfPCell();
        if (bg != null)
            c.setBackgroundColor(bg);
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
                int style = font.getStyle() == Font.BOLD ? android.graphics.Typeface.BOLD
                        : android.graphics.Typeface.NORMAL;
                paint.setTypeface(android.graphics.Typeface.create(PdfGenerator.sMarathiTypeface, style));
            } else {
                paint.setTypeface(font.getStyle() == Font.BOLD ? android.graphics.Typeface.DEFAULT_BOLD
                        : android.graphics.Typeface.DEFAULT);
            }

            float textWidth = paint.measureText(text);
            android.graphics.Paint.FontMetrics fm = paint.getFontMetrics();
            float textHeight = fm.descent - fm.ascent;

            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap((int) Math.ceil(textWidth) + 4,
                    (int) Math.ceil(textHeight) + 4, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);

            canvas.drawText(text, 2, -fm.ascent + 2, paint);

            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(stream.toByteArray());
            img.setAlignment(Element.ALIGN_CENTER);
            img.scaleToFit(100f, 16f); // Horizontal text scale
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, font)); // Fallback
        }
        tbl.addCell(c);
    }

    public static void addGunapattrakContent(Document doc, Context ctx, School school, ClassModel cls, Student student,
            MarksRecord sem1, MarksRecord sem2) throws Exception {
        
        int activeSem = com.kartik.myschool.SessionContext.selectedSemester != null
                ? com.kartik.myschool.SessionContext.selectedSemester.number
                : 1;
                
        MarksRecord rec = (activeSem == 2) ? sem2 : sem1;
        
        printSingleTermGunapattrak(doc, ctx, school, cls, student, rec, activeSem);
    }

    private static void printSingleTermGunapattrak(Document doc, Context ctx, School school, ClassModel cls, Student student,
            MarksRecord rec, int termNumber) throws Exception {

        // Title
        try {
            PdfGenerator.addMarathiParagraph(doc,
                    PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"),
                    18, true, C_DARK, 0, 15);
        } catch (Exception e) {
            Font titleFont = sMarathiBase != null ? new Font(sMarathiBase, 18, Font.BOLD, C_DARK)
                    : new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, C_DARK);
            Paragraph title = new Paragraph(
                    PdfLocalizer.get(ctx, "सातत्यपूर्ण सर्वंकष मूल्यमापन", "Continuous Comprehensive Evaluation"),
                    titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            doc.add(title);
        }

        // Header Info table
        PdfPTable headerTbl = new PdfPTable(3);
        headerTbl.setWidthPercentage(100);
        headerTbl.setWidths(new float[] { 1.5f, 1f, 1f });

        // Row 1: name | empty | year
        PdfPCell c1 = rawCell(PdfLocalizer.get(ctx, "नाव: ", "Name: ") + (student != null ? nvl(student.name) : ""),
                fBold, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = rawCell(" ", fBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        c2.setBorder(Rectangle.NO_BORDER);
        PdfPCell c3 = rawCell(
                PdfLocalizer.get(ctx, "सन : ", "Year : ") + (cls != null ? nvl(cls.academicYearLabel) : "2025-26"),
                fBold, C_WHITE, C_DARK, Element.ALIGN_RIGHT);
        c3.setBorder(Rectangle.NO_BORDER);
        headerTbl.addCell(c1);
        headerTbl.addCell(c2);
        headerTbl.addCell(c3);

        // Row 2
        String termLabel = (termNumber == 2) ? PdfLocalizer.get(ctx, "द्वितीय सत्र", "Second Semester")
                : PdfLocalizer.get(ctx, "प्रथम सत्र", "First Semester");
        PdfPCell c4 = rawCell(
                PdfLocalizer.get(ctx, "इयत्ता: ", "Class: ") + (cls != null ? nvl(cls.className) : "")
                        + PdfLocalizer.get(ctx, ", तुकडी: ", ", Division: ") + (cls != null ? nvl(cls.division) : "-"),
                fBold, C_WHITE, C_DARK, Element.ALIGN_LEFT);
        c4.setBorder(Rectangle.NO_BORDER);
        PdfPCell c5 = rawCell(
                PdfLocalizer.get(ctx, "रोल नं.: ", "Roll No.: ") + (student != null ? nvl(student.rollNo) : ""), fBold,
                C_WHITE, C_DARK, Element.ALIGN_CENTER);
        c5.setBorder(Rectangle.NO_BORDER);
        PdfPCell c6 = rawCell(termLabel, fBold, C_WHITE, C_DARK, Element.ALIGN_RIGHT);
        c6.setBorder(Rectangle.NO_BORDER);
        headerTbl.addCell(c4);
        headerTbl.addCell(c5);
        headerTbl.addCell(c6);

        headerTbl.setSpacingAfter(10);
        doc.add(headerTbl);

        // Marks table (19 columns)
        float[] widths = { 0.6f, 1.8f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.8f, 0.7f, 0.7f, 0.7f,
                0.8f, 0.8f, 0.8f, 0.8f };
        PdfPTable tbl = new PdfPTable(widths);
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(6);
        tbl.setSpacingAfter(4);

        // Row 1
        cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "अ.नं", "Sr.No."), fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        cellHorizontalImageSpan(tbl, ctx, PdfLocalizer.get(ctx, "विषय", "Subject"), fSmallBold, C_HEADER_BG, C_DARK, 1,
                3);
        cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "तपशील", "Details"), fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        cellHorizontalImageSpan(tbl, ctx, PdfLocalizer.get(ctx, "आकारिक (अ)", "Formative (A)"), fSmallBold, C_HEADER_BG,
                C_DARK, 9, 1);
        cellHorizontalImageSpan(tbl, ctx, PdfLocalizer.get(ctx, "संकलित (ब)", "Summative (B)"), fSmallBold, C_HEADER_BG,
                C_DARK, 4, 1);
        cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "अ+ब", "A+B"), fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "श्रे.गुण", "Total"), fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
        cellVerticalSpan(tbl, ctx, PdfLocalizer.get(ctx, "श्रेणी", "Grade"), fSmallBold, C_HEADER_BG, C_DARK, 1, 3);

        // Row 2
        String[] formatives = {
                PdfLocalizer.get(ctx, "निरीक्षण", "Observation"),
                PdfLocalizer.get(ctx, "तोंडीकाम", "Oral Work"),
                PdfLocalizer.get(ctx, "प्रात्यक्षिक", "Practical"),
                PdfLocalizer.get(ctx, "उपक्रम", "Activity"),
                PdfLocalizer.get(ctx, "प्रकल्प", "Project"),
                PdfLocalizer.get(ctx, "चाचणी", "Test"),
                PdfLocalizer.get(ctx, "स्वाध्याय", "Assignment"),
                PdfLocalizer.get(ctx, "इतर", "Other"),
                PdfLocalizer.get(ctx, "एकूण", "Total")
        };
        for (String f : formatives)
            cellVerticalSpan(tbl, ctx, f, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);

        String[] summatives = {
                PdfLocalizer.get(ctx, "तोंडी", "Oral"),
                PdfLocalizer.get(ctx, "प्रात्य.", "Pract."),
                PdfLocalizer.get(ctx, "लेखी", "Written"),
                PdfLocalizer.get(ctx, "एकूण", "Total")
        };
        for (String s : summatives)
            cellVerticalSpan(tbl, ctx, s, fSmallBold, C_HEADER_BG, C_DARK, 1, 1);

        // Row 3 (Numbers)
        String[] numbers = { "1", "2", "3", "4", "5", "6", "7", "8" };
        for (String n : numbers)
            cellSpan(tbl, n, fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER); // Keep numbers horizontal
        cellSpan(tbl, " ", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER); // Formative total empty
        for (int i = 0; i < 4; i++)
            cellSpan(tbl, " ", fSmallBold, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER); // Summative empty

        List<Subject> subjects = cls.subjects;
        int nonAcaCount = 0;
        int studentTotalSum = 0;
        int studentTotalMax = 0;
        if (subjects != null) {
            for (Subject sub : subjects) {
                int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
                boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);
                if (isNonAcademic) {
                    nonAcaCount++;
                } else {
                    studentTotalMax += sub.maxMarks;
                    MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);
                    if (d != null) {
                        studentTotalSum += d.grandTotal;
                    }
                }
            }
        }
        String percentageStr = studentTotalMax > 0
                ? String.format(java.util.Locale.US, "%.1f %%", (studentTotalSum * 100.0) / studentTotalMax)
                : "";
        String overallGrade = studentTotalMax > 0
                ? com.kartik.myschool.utils.GradeCalculator.getGrade((studentTotalSum * 100.0) / studentTotalMax)
                : "";
        int nonAcaPrinted = 0;

        boolean alt = false;
        for (int i = 0; i < (subjects != null ? subjects.size() : 0); i++) {
            Subject sub = subjects.get(i);
            BaseColor bg = alt ? C_ROW_ALT : C_WHITE;
            alt = !alt;
            MarksRecord.SubjectMarksDetail d = detail(rec, sub.name);

            int formativeMax = sub.maxNirikhshan + sub.maxTondiKam + sub.maxPratyakshik + sub.maxUpkram + sub.maxPrakalp
                    + sub.maxChachani + sub.maxSwadhyay + sub.maxItar;
            if (formativeMax == 0)
                formativeMax = sub.maxMarks / 2;
            int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
            boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);

            // Row A: प्राप्त
            cellSpan(tbl, String.valueOf(i + 1), fSmall, bg, C_DARK, 1, 2, Element.ALIGN_CENTER);
            cellSpan(tbl, PdfLocalizer.translateSubject(ctx, sub.name), fSmall, bg, C_DARK, 1, 2, Element.ALIGN_LEFT);
            cellSpan(tbl, PdfLocalizer.get(ctx, "प्राप्त", "Obt."), fMicro, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);

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
                    if (nonAcaPrinted == 0) {
                        String txt = PdfLocalizer.get(ctx, "एकूण गुण", "Total Marks") + " : " + studentTotalSum;
                        com.kartik.myschool.utils.pdf.MarathiText.cell(tbl, txt, 11f, true, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                    } else if (nonAcaPrinted == 1) {
                        String txt = PdfLocalizer.get(ctx, "शे.गुण", "Percentage") + " : " + percentageStr;
                        com.kartik.myschool.utils.pdf.MarathiText.cell(tbl, txt, 11f, true, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                    } else if (nonAcaPrinted == 2) {
                        String txt = PdfLocalizer.get(ctx, "सर्वसाधारण श्रेणी", "Overall Grade") + " : " + overallGrade;
                        com.kartik.myschool.utils.pdf.MarathiText.cell(tbl, txt, 11f, true, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                    } else {
                        cellSpan(tbl, " ", fSmallBold, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                    }
                    nonAcaPrinted++;
                } else {
                    cellSpan(tbl, strZero(d.tondi), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(d.pratyakshikB), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, strZero(d.lekhi), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                    cellSpan(tbl, str(d.sanklit), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                }

                // ROWSPAN = 2 for Totals and Grade
                cellSpan(tbl, str(d.grandTotal), fSmallBold, bg, C_DARK, 1, 2, Element.ALIGN_CENTER);
                cellSpan(tbl, str(d.grandTotal), fSmallBold, bg, C_DARK, 1, 2, Element.ALIGN_CENTER); // Grade marks is
                                                                                                      // just grand
                                                                                                      // total
                cellSpan(tbl, nvl(d.grade), fSmallBold, bg, C_DARK, 1, 2, Element.ALIGN_CENTER);
            } else {
                for (int k = 0; k < 9; k++)
                    cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                if (isNonAcademic) {
                    if (nonAcaPrinted == 0) {
                        String txt = PdfLocalizer.get(ctx, "एकूण गुण", "Total Marks") + " : " + studentTotalSum;
                        com.kartik.myschool.utils.pdf.MarathiText.cell(tbl, txt, 11f, true, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                    } else if (nonAcaPrinted == 1) {
                        String txt = PdfLocalizer.get(ctx, "शे.गुण", "Percentage") + " : " + percentageStr;
                        com.kartik.myschool.utils.pdf.MarathiText.cell(tbl, txt, 11f, true, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                    } else if (nonAcaPrinted == 2) {
                        String txt = PdfLocalizer.get(ctx, "सर्वसाधारण श्रेणी", "Overall Grade") + " : " + overallGrade;
                        com.kartik.myschool.utils.pdf.MarathiText.cell(tbl, txt, 11f, true, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                    } else {
                        cellSpan(tbl, " ", fSmallBold, bg, C_DARK, 4, 2, Element.ALIGN_CENTER);
                    }
                    nonAcaPrinted++;
                } else {
                    for (int k = 0; k < 4; k++)
                        cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                }
                cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 2, Element.ALIGN_CENTER);
                cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 2, Element.ALIGN_CENTER);
                cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 2, Element.ALIGN_CENTER);
            }

            // Row B: पैकी
            BaseColor paikiBg = new BaseColor(245, 245, 245); // Light grey shading for "पैकी" row
            cellSpan(tbl, PdfLocalizer.get(ctx, "पैकी", "Max"), fMicro, paikiBg, C_GREY, 1, 1, Element.ALIGN_CENTER);
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
                cellSpan(tbl, str(summativeMax == 0 ? sub.maxMarks / 2 : summativeMax), fSmallBold, paikiBg, C_GREY, 1,
                        1, Element.ALIGN_CENTER);
            }
        }
        doc.add(tbl);

        // Signature Row at bottom
        PdfPTable sigTbl = new PdfPTable(3);
        sigTbl.setWidthPercentage(100);
        sigTbl.setSpacingBefore(40);

        PdfPCell cSig1 = rawCell(PdfLocalizer.get(ctx, "वर्गशिक्षक स्वाक्षरी : ", "Class Teacher Sign : ")
                + (cls != null ? nvl(cls.teacherName) : ""), fSmallBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        cSig1.setBorder(Rectangle.NO_BORDER);

        PdfPCell cSig2 = new PdfPCell(new Phrase(" ", fSmallBold));
        cSig2.setBorder(Rectangle.NO_BORDER);
        cSig2.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cSig3 = rawCell(
                PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी : ", "Headmaster Sign : ")
                        + (school != null ? nvl(school.principalName) : ""),
                fSmallBold, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        cSig3.setBorder(Rectangle.NO_BORDER);

        sigTbl.addCell(cSig1);
        sigTbl.addCell(cSig2);
        sigTbl.addCell(cSig3);
        doc.add(sigTbl);
    }
}
