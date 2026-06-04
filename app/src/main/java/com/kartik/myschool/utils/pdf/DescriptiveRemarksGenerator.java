package com.kartik.myschool.utils.pdf;

import android.content.Context;

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

                if (cls.subjects != null) {
                    for (int si = 0; si < cls.subjects.size(); si++) {
                        if (si > 0) doc.newPage();
                        Subject sub = cls.subjects.get(si);

                        // Title
                        Paragraph title = new Paragraph("Descriptive Remarks Register", new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, C_DARK));
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

                        cell(tbl, "Sr.No.",  fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, "Student Name",  fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, "Roll No.",  fBold, C_HEADER_BG, C_DARK, 1, Element.ALIGN_CENTER);
                        cell(tbl, "Descriptive Remarks",   fBold, C_HEADER_BG, C_DARK,  1, Element.ALIGN_CENTER);

                        boolean alt = false;
                        for (int i = 0; i < students.size(); i++) {
                            Student s = students.get(i);
                            BaseColor bg = alt ? C_ROW_ALT : C_WHITE; alt = !alt;

                            String remark = "";
                            MarksRecord rec1 = sem1Marks != null ? sem1Marks.get(s.id) : null;
                            MarksRecord rec2 = sem2Marks != null ? sem2Marks.get(s.id) : null;
                            
                            MarksRecord.SubjectMarksDetail d1 = rec1 != null ? detail(rec1, sub.name) : null;
                            MarksRecord.SubjectMarksDetail d2 = rec2 != null ? detail(rec2, sub.name) : null;
                            
                            String r1 = (d1 != null && d1.remark != null) ? d1.remark.replace("||", ", ").trim() : "";
                            String r2 = (d2 != null && d2.remark != null) ? d2.remark.replace("||", ", ").trim() : "";
                            
                            int activeSem = com.kartik.myschool.SessionContext.selectedSemester != null ? com.kartik.myschool.SessionContext.selectedSemester.number : 1;
                            
                            if (activeSem == 2) {
                                remark = !r2.isEmpty() ? r2 : r1;
                            } else {
                                remark = !r1.isEmpty() ? r1 : r2;
                            }

                            PdfPCell nc = rawCell(String.valueOf(i + 1), fNormal, bg, C_DARK, Element.ALIGN_CENTER); tbl.addCell(nc);
                            PdfPCell lc = rawCell(nvl(s.name), fNormal, bg, C_DARK, Element.ALIGN_LEFT); lc.setMinimumHeight(28f); tbl.addCell(lc);
                            PdfPCell rc = rawCell(nvl(s.rollNo), fNormal, bg, C_DARK, Element.ALIGN_CENTER); rc.setMinimumHeight(28f); tbl.addCell(rc);
                            PdfPCell remCell = rawCell(remark, fNormal, bg, C_DARK, Element.ALIGN_LEFT); remCell.setMinimumHeight(28f); tbl.addCell(remCell);
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
