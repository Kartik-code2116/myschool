package com.example.myschool.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.myschool.model.ClassModel;
import com.example.myschool.model.MarksRecord;
import com.example.myschool.model.School;
import com.example.myschool.model.Student;
import com.example.myschool.model.Subject;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfGenerator {

    public interface PdfCallback {
        void onSuccess(File pdfFile);
        void onError(Exception e);
    }

    private static final BaseColor PRIMARY = new BaseColor(21, 101, 192);
    private static final BaseColor PRIMARY_LIGHT = new BaseColor(187, 222, 251);
    private static final BaseColor WHITE = BaseColor.WHITE;
    private static final BaseColor DARK = new BaseColor(28, 27, 31);
    private static final BaseColor GREY = new BaseColor(117, 117, 117);

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, WHITE);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, DARK);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, DARK);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GREY);
    private static final Font BOLD_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, DARK);
    private static final Font RESULT_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, PRIMARY);

    public static void generate(Context context, School school, ClassModel classModel,
                                Student student, MarksRecord marks, PdfCallback callback) {
        new Thread(() -> {
            try {
                File dir = new File(context.getExternalFilesDir(null), "marksheets");
                if (!dir.exists()) dir.mkdirs();
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File out = new File(dir, "Marksheet_" + student.rollNo + "_" + ts + ".pdf");

                Document doc = new Document(new Rectangle(595, 842)); // A4
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(36, 36, 36, 36);

                // --- School header bar ---
                PdfPTable headerTable = new PdfPTable(1);
                headerTable.setWidthPercentage(100);
                PdfPCell headerCell = new PdfPCell();
                headerCell.setBackgroundColor(PRIMARY);
                headerCell.setPadding(12);
                headerCell.setBorder(Rectangle.NO_BORDER);
                Paragraph headerPara = new Paragraph();
                String schoolName = (school != null && school.name != null) ? school.name.toUpperCase() : "School";
                headerPara.add(new Phrase(schoolName + "\n", TITLE_FONT));
                Font smallWhite = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(255,255,255,200));
                String address = (school != null && school.address != null) ? school.address : "";
                String board = (school != null && school.board != null) ? school.board : "";
                headerPara.add(new Phrase(address + " | Board: " + board, smallWhite));
                headerCell.addElement(headerPara);
                headerTable.addCell(headerCell);
                doc.add(headerTable);

                // --- Exam title ---
                Paragraph examTitle = new Paragraph(classModel.examName.toUpperCase() + " - REPORT CARD", HEADER_FONT);
                examTitle.setAlignment(Element.ALIGN_CENTER);
                examTitle.setSpacingBefore(10);
                examTitle.setSpacingAfter(6);
                doc.add(examTitle);

                // --- Student info table ---
                PdfPTable infoTable = new PdfPTable(2);
                infoTable.setWidthPercentage(100);
                infoTable.setSpacingAfter(8);
                addInfoRow(infoTable, "Student Name:", student.name);
                addInfoRow(infoTable, "Roll No:", student.rollNo);
                addInfoRow(infoTable, "Class:", classModel.getDisplayName());
                addInfoRow(infoTable, "School:", school.name);
                addInfoRow(infoTable, "Date of Birth:", student.dob);
                addInfoRow(infoTable, "Academic Year:", String.valueOf(classModel.year));
                doc.add(infoTable);

                // --- Marks table ---
                PdfPTable marksTable = new PdfPTable(new float[]{3f, 1f, 1.2f, 1f});
                marksTable.setWidthPercentage(100);
                marksTable.setSpacingAfter(6);
                addTableHeader(marksTable, "Subject", "Max Marks", "Marks Obtained", "Grade");

                boolean alt = false;
                for (Subject sub : classModel.subjects) {
                    double obtained = marks.subjectMarks.containsKey(sub.name)
                            ? marks.subjectMarks.get(sub.name) : 0;
                    double pct = GradeCalculator.getPercentage(obtained, sub.maxMarks);
                    String grade = GradeCalculator.getGrade(pct);
                    BaseColor rowColor = alt ? new BaseColor(245, 247, 250) : WHITE;
                    addMarksRow(marksTable, sub.name, String.valueOf(sub.maxMarks),
                            formatMark(obtained), grade, rowColor);
                    alt = !alt;
                }

                // Total row
                PdfPCell totalLabel = styledCell("TOTAL", BOLD_FONT, PRIMARY_LIGHT, Element.ALIGN_LEFT);
                PdfPCell totalMax = styledCell(String.valueOf(marks.totalMax), BOLD_FONT, PRIMARY_LIGHT, Element.ALIGN_CENTER);
                PdfPCell totalObt = styledCell(formatMark(marks.totalObtained), BOLD_FONT, PRIMARY_LIGHT, Element.ALIGN_CENTER);
                PdfPCell totalGrade = styledCell(marks.grade, BOLD_FONT, PRIMARY_LIGHT, Element.ALIGN_CENTER);
                marksTable.addCell(totalLabel);
                marksTable.addCell(totalMax);
                marksTable.addCell(totalObt);
                marksTable.addCell(totalGrade);
                doc.add(marksTable);

                // --- Result summary ---
                PdfPTable resultTable = new PdfPTable(2);
                resultTable.setWidthPercentage(60);
                resultTable.setHorizontalAlignment(Element.ALIGN_LEFT);
                resultTable.setSpacingAfter(20);
                addInfoRow(resultTable, "Percentage:", String.format(Locale.getDefault(), "%.2f%%", marks.percentage));
                addInfoRow(resultTable, "Result:", marks.result);
                doc.add(resultTable);

                // --- Signatures ---
                PdfPTable sigTable = new PdfPTable(2);
                sigTable.setWidthPercentage(100);
                PdfPCell sig1 = styledCell("_________________\nClass Teacher", SMALL_FONT, WHITE, Element.ALIGN_CENTER);
                sig1.setBorderWidthTop(0.5f);
                sig1.setPaddingTop(6);
                PdfPCell sig2 = styledCell("_________________\nPrincipal", SMALL_FONT, WHITE, Element.ALIGN_CENTER);
                sig2.setBorderWidthTop(0.5f);
                sig2.setPaddingTop(6);
                sigTable.addCell(sig1);
                sigTable.addCell(sig2);
                doc.add(sigTable);

                doc.close();
                callback.onSuccess(out);
            } catch (DocumentException | IOException e) {
                callback.onError(e);
            }
        }).start();
    }

    private static void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, SMALL_FONT));
        lCell.setBorder(Rectangle.BOTTOM);
        lCell.setBorderColor(new BaseColor(224, 224, 224));
        lCell.setPadding(4);
        PdfPCell vCell = new PdfPCell(new Phrase(value != null ? value : "--", NORMAL_FONT));
        vCell.setBorder(Rectangle.BOTTOM);
        vCell.setBorderColor(new BaseColor(224, 224, 224));
        vCell.setPadding(4);
        table.addCell(lCell);
        table.addCell(vCell);
    }

    private static void addTableHeader(PdfPTable table, String... cols) {
        for (String col : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(col, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, WHITE)));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private static void addMarksRow(PdfPTable table, String subject, String max,
                                    String obtained, String grade, BaseColor bg) {
        table.addCell(styledCell(subject, NORMAL_FONT, bg, Element.ALIGN_LEFT));
        table.addCell(styledCell(max, NORMAL_FONT, bg, Element.ALIGN_CENTER));
        table.addCell(styledCell(obtained, BOLD_FONT, bg, Element.ALIGN_CENTER));
        table.addCell(styledCell(grade, BOLD_FONT, bg, Element.ALIGN_CENTER));
    }

    private static PdfPCell styledCell(String text, Font font, BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(new BaseColor(224, 224, 224));
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private static String formatMark(double val) {
        if (val == Math.floor(val)) return String.valueOf((int) val);
        return String.format(Locale.getDefault(), "%.1f", val);
    }
}
