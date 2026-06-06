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
import java.util.List;
import java.util.Map;

import static com.kartik.myschool.utils.PdfGenerator.*;

public class MarksRegisterGenerator {

    public static void generateMarksRegister(Context ctx,
                                             School school,
                                             ClassModel cls,
                                             List<Student> students,
                                             Map<String, MarksRecord> sem1Marks,
                                             PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx), "MarksRegister_" + PdfGenerator.ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);

                if (cls.subjects != null) {
                    for (int si = 0; si < cls.subjects.size(); si++) {
                        if (si > 0) doc.newPage();
                        Subject sub = cls.subjects.get(si);
                        
                        Paragraph pageTitle = new Paragraph("Marks Register (Continuous Comprehensive Evaluation)", new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, C_DARK));
                        pageTitle.setAlignment(Element.ALIGN_CENTER);
                        pageTitle.setSpacingAfter(10);
                        doc.add(pageTitle);
                        
                        PdfPTable top = new PdfPTable(3);
                        top.setWidthPercentage(100); top.setSpacingAfter(10);
                        String schoolStr = "School: " + (school != null ? nvl(school.name) : "");
                        String classStr = "Class: " + (cls != null ? nvl(cls.className) : "") + " " + (cls != null ? nvl(cls.division) : "");
                        String yearStr = "Year: " + (cls != null ? nvl(cls.academicYearLabel) : "");
                        
                        cellSpan(top, schoolStr + "\n" + classStr, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_LEFT);
                        cellSpan(top, "Subject: " + sub.name, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_CENTER);
                        cellSpan(top, yearStr, fSmallBold, C_WHITE, C_DARK, 1, 1, Element.ALIGN_RIGHT);
                        doc.add(top);

                        float[] widths = {0.6f, 1.2f, 1.2f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.8f, 0.7f, 0.7f, 0.7f, 0.8f, 0.8f, 0.8f, 0.8f};
                        PdfPTable tbl = new PdfPTable(widths);
                        tbl.setWidthPercentage(100); tbl.setSpacingAfter(10);

                        // Row 1
                        cellSpan(tbl, "Sr.No.", fSmallBold, C_HEADER_BG, C_DARK, 1, 3, Element.ALIGN_CENTER);
                        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "तपशील", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
                        cellSpan(tbl, "Roll No.", fSmallBold, C_HEADER_BG, C_DARK, 1, 3, Element.ALIGN_CENTER);
                        cellSpan(tbl, "Formative (A)", fSmallBold, C_HEADER_BG, C_DARK, 9, 1, Element.ALIGN_CENTER);
                        cellSpan(tbl, "Summative (B)", fSmallBold, C_HEADER_BG, C_DARK, 4, 1, Element.ALIGN_CENTER);
                        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "Total (A+B)", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
                        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "Grade Marks", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);
                        GunapattrakGenerator.cellVerticalSpan(tbl, ctx, "Grade", fSmallBold, C_HEADER_BG, C_DARK, 1, 3);

                        // Row 2 & 3 Headers
                        String[] formatives = {"Observation", "Oral", "Practical", "Activity", "Project", "Test", "Homework", "Other", "Total"};
                        for (String f : formatives) GunapattrakGenerator.cellVerticalSpan(tbl, ctx, f, fSmallBold, C_HEADER_BG, C_DARK, 1, 2);
                        String[] summatives = {"Oral", "Pract.", "Written", "Total"};
                        for (String s : summatives) GunapattrakGenerator.cellVerticalSpan(tbl, ctx, s, fSmallBold, C_HEADER_BG, C_DARK, 1, 2);

                        boolean alt = false;
                        for (int i = 0; i < students.size(); i++) {
                            Student s = students.get(i);
                            BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;
                            cellSpan(tbl, String.valueOf(i+1), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                            cellSpan(tbl, nvl(s.name), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_LEFT);
                            cellSpan(tbl, nvl(s.rollNo), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);

                            MarksRecord rec = null;
                            if (sem1Marks != null) rec = sem1Marks.get(s.id);
                            
                            MarksRecord.SubjectMarksDetail d = rec != null ? detail(rec, sub.name) : null;

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
                                
                                cellSpan(tbl, strZero(d.tondi), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.pratyakshikB), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, strZero(d.lekhi), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, str(d.sanklit), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                
                                cellSpan(tbl, str(d.grandTotal), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, calculatePercentageString(d.grandTotal, sub.maxMarks), fSmall, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                                cellSpan(tbl, nvl(d.grade), fSmallBold, bg, C_DARK, 1, 1, Element.ALIGN_CENTER);
                            } else {
                                for (int k = 0; k < 16; k++) cellSpan(tbl, "-", fSmall, bg, C_GREY, 1, 1, Element.ALIGN_CENTER);
                            }
                        }
                        doc.add(tbl);
                    }
                }

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }
}
