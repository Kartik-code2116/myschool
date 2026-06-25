package com.kartik.myschool.utils.pdf;

import android.content.Context;
import android.graphics.Color;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.kartik.myschool.R;
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

public class StudentSpecialCardGenerator {

    // Draws double rounded borders around the page
    private static class BorderEvent extends PdfPageEventHelper {
        private final BaseColor color;

        public BorderEvent(BaseColor color) {
            this.color = color;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle rect = document.getPageSize();

            // Outer round border
            canvas.setColorStroke(color);
            canvas.setLineWidth(3f);
            canvas.roundRectangle(25, 25, rect.getWidth() - 50, rect.getHeight() - 50, 18);
            canvas.stroke();

            // Inner thin round border
            canvas.setColorStroke(color);
            canvas.setLineWidth(1f);
            canvas.roundRectangle(30, 30, rect.getWidth() - 60, rect.getHeight() - 60, 14);
            canvas.stroke();
        }
    }

    // Draws rounded cell backgrounds for cards
    private static class RoundedBackgroundEvent implements PdfPCellEvent {
        private final BaseColor bgColor;
        private final BaseColor strokeColor;
        private final float radius;

        public RoundedBackgroundEvent(BaseColor bgColor, BaseColor strokeColor, float radius) {
            this.bgColor = bgColor;
            this.strokeColor = strokeColor;
            this.radius = radius;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            if (bgColor != null) {
                canvas.setColorFill(bgColor);
                canvas.roundRectangle(position.getLeft(), position.getBottom(), position.getWidth(),
                        position.getHeight(), radius);
                canvas.fill();
            }
            if (strokeColor != null) {
                canvas.setColorStroke(strokeColor);
                canvas.setLineWidth(1.5f);
                canvas.roundRectangle(position.getLeft(), position.getBottom(), position.getWidth(),
                        position.getHeight(), radius);
                canvas.stroke();
            }
        }
    }

    // Draws images as cell backgrounds that cover the cell bounds (center-crop scale type)
    private static class ImageBackgroundEvent implements PdfPCellEvent {
        private final com.itextpdf.text.Image image;
        private final BaseColor strokeColor;
        private final float radius;

        public ImageBackgroundEvent(com.itextpdf.text.Image image, BaseColor strokeColor, float radius) {
            this.image = image;
            this.strokeColor = strokeColor;
            this.radius = radius;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            if (image != null) {
                canvas.saveState();
                // Define rounded rectangle clipping path matching the card borders
                canvas.roundRectangle(position.getLeft(), position.getBottom(), position.getWidth(),
                        position.getHeight(), radius);
                canvas.clip();
                canvas.newPath(); // Clear the path from being stroked/filled

                try {
                    com.itextpdf.text.Image imgCopy = com.itextpdf.text.Image.getInstance(image);
                    float cellWidth = position.getWidth();
                    float cellHeight = position.getHeight();
                    float imgWidth = imgCopy.getWidth();
                    float imgHeight = imgCopy.getHeight();

                    float scale;
                    float dx = 0;
                    float dy = 0;

                    // Compute aspect-ratio preserving center crop scale and offsets
                    if (imgWidth / imgHeight > cellWidth / cellHeight) {
                        scale = cellHeight / imgHeight;
                        float scaledWidth = imgWidth * scale;
                        dx = (cellWidth - scaledWidth) / 2f;
                    } else {
                        scale = cellWidth / imgWidth;
                        float scaledHeight = imgHeight * scale;
                        dy = (cellHeight - scaledHeight) / 2f;
                    }

                    imgCopy.scaleAbsolute(imgWidth * scale, imgHeight * scale);
                    imgCopy.setAbsolutePosition(position.getLeft() + dx, position.getBottom() + dy);
                    canvas.addImage(imgCopy);
                } catch (Exception e) {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                }

                canvas.restoreState();
            }

            // Draw rounded stroke outline border
            if (strokeColor != null) {
                canvas.setColorStroke(strokeColor);
                canvas.setLineWidth(1.5f);
                canvas.roundRectangle(position.getLeft(), position.getBottom(), position.getWidth(),
                        position.getHeight(), radius);
                canvas.stroke();
            }
        }
    }

    public static void generateSpecialCard(Context ctx,
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
                        "SpecialCard_" + PdfGenerator.safeRoll(student) + "_" + PdfGenerator.ts() + ".pdf");
                Document doc = new Document(PageSize.A4);

                // Select theme color based on gender
                BaseColor primaryColor;
                BaseColor secondaryBgColor;
                String gender = student.gender != null ? student.gender.toLowerCase().trim() : "";
                if (gender.contains("female") || gender.contains("स्त्री") || gender.contains("मुलगी")
                        || gender.contains("मुलि") || gender.equals("2")) {
                    primaryColor = new BaseColor(194, 24, 91); // Crimson Rose
                    secondaryBgColor = new BaseColor(252, 228, 236); // Soft Pink
                } else if (gender.contains("male") || gender.contains("पुरुष") || gender.contains("मुलगा")
                        || gender.contains("मुलगे") || gender.equals("1")) {
                    primaryColor = new BaseColor(21, 101, 192); // Royal Blue
                    secondaryBgColor = new BaseColor(227, 242, 253); // Ice Blue
                } else {
                    primaryColor = new BaseColor(46, 125, 50); // Emerald Green
                    secondaryBgColor = new BaseColor(232, 245, 233); // Soft Mint
                }

                PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                writer.setPageEvent(new BorderEvent(primaryColor));

                doc.open();
                doc.setMargins(40, 40, 45, 45);

                addSpecialCardContent(doc, ctx, school, cls, student, sem1, sem2, primaryColor, secondaryBgColor);

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    private static void addSpecialCardContent(Document doc, Context ctx, School school, ClassModel cls, Student student,
            MarksRecord sem1, MarksRecord sem2, BaseColor primaryColor, BaseColor secondaryBgColor) throws Exception {

        // ── 1. School Header Banner ──────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(1);
        hdr.setWidthPercentage(100);

        String udise = nvl(school != null ? school.udiseCode : "");
        if (!udise.isEmpty()) {
            addCenterText(hdr, PdfLocalizer.get(ctx, "युडायस क्रमांक: ", "UDISE CODE: ") + udise, fSmallBold);
        }

        String prefix = PdfLocalizer.get(ctx, "जिल्हा परिषद प्राथमिक शाळा, ", "ZILLA PARISHAD PRIMARY SCHOOL, ");
        String sName = prefix + nvl(school != null ? school.name : "");
        addCenterText(hdr, sName.toUpperCase(), fTitle);

        String addr = nvl(school != null ? school.address : "");
        if (!addr.isEmpty()) {
            addCenterText(hdr, addr, fSmall);
        }

        String yLabel = PdfLocalizer.get(ctx, "सन: ", "Year: ");
        String yVal = nvl(cls != null ? cls.academicYearLabel : "2025-26");
        addCenterText(hdr, yLabel + yVal, fSmallBold);
        hdr.setSpacingAfter(8);
        doc.add(hdr);

        // ── 2. "विद्यार्थी प्रगती पत्रक" Pill Title ─────────────────────────────────
        PdfPTable pillTbl = new PdfPTable(1);
        pillTbl.setWidthPercentage(55);
        PdfPCell pillCell = new PdfPCell();
        pillCell.setBorder(Rectangle.NO_BORDER);
        pillCell.setCellEvent(new RoundedBackgroundEvent(secondaryBgColor, primaryColor, 12f));
        pillCell.setPaddingTop(8);
        pillCell.setPaddingBottom(10);
        pillCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        try {
            int mrColor = Color.rgb(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue());
            com.itextpdf.text.Image img = MarathiText.renderLine(
                    PdfLocalizer.get(ctx, "विद्यार्थी प्रगती पत्रक", "STUDENT PROGRESS CARD"), 16, true, mrColor);
            img.setAlignment(Element.ALIGN_CENTER);
            pillCell.addElement(img);
        } catch (Exception e) {
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, primaryColor);
            pillCell.setPhrase(
                    new Phrase(PdfLocalizer.get(ctx, "विद्यार्थी प्रगती पत्रक", "STUDENT PROGRESS CARD"), titleFont));
        }
        pillTbl.addCell(pillCell);
        pillTbl.setSpacingAfter(12);
        doc.add(pillTbl);

        // ── 3. Student Details Info Grid (2 Columns: Data Card & Avatar) ───────
        PdfPTable profileGrid = new PdfPTable(new float[] { 3.2f, 1f });
        profileGrid.setWidthPercentage(100);

        // Data card wrapper cell
        PdfPCell dataCardCell = new PdfPCell();
        dataCardCell.setBorder(Rectangle.NO_BORDER);
        dataCardCell.setPaddingRight(12);

        // Inner table inside Data card
        PdfPTable dataTable = new PdfPTable(new float[] { 1f, 1.6f });
        dataTable.setWidthPercentage(100);

        // Add rounded box background event to details table
        BaseColor detailsBg = new BaseColor(250, 250, 250);
        dataTable.getDefaultCell()
                .setCellEvent(new RoundedBackgroundEvent(detailsBg, new BaseColor(220, 220, 220), 8f));
        dataTable.getDefaultCell().setPadding(6);

        addDetailRow(dataTable, ctx, "नाव (Name)", nvl(student.name), primaryColor);
        addDetailRow(dataTable, ctx, "इयत्ता/तुकडी (Class/Div)", nvl(student.standard) + " - " + nvl(student.division),
                primaryColor);
        addDetailRow(dataTable, ctx, "हजेरी क्र. १ (Roll 1)", nvl(student.rollNo), primaryColor);
        addDetailRow(dataTable, ctx, "जन्मतारीख (D.O.B.)", nvl(student.dob), primaryColor);
        addDetailRow(dataTable, ctx, "जन्मस्थान (Birth Place)", nvl(student.birthPlace), primaryColor);
        addDetailRow(dataTable, ctx, "धर्म (Religion)", nvl(student.religion), primaryColor);
        addDetailRow(dataTable, ctx, "रक्तगट (Blood Group)", nvl(student.bloodGroup), primaryColor);
        addDetailRow(dataTable, ctx, "माध्यम (Medium)", nvl(student.medium), primaryColor);

        dataCardCell.addElement(dataTable);
        profileGrid.addCell(dataCardCell);

        // Avatar wrapper cell (Zero padding so the image covers the entire card box)
        PdfPCell avatarCell = new PdfPCell();
        avatarCell.setBorder(Rectangle.NO_BORDER);
        avatarCell.setPadding(0);

        com.itextpdf.text.Image photoImg = null;
        try {
            if (student.photoUrl != null && !student.photoUrl.isEmpty()) {
                if (student.photoUrl.startsWith("data:image")) {
                    try {
                        String base64Data = student.photoUrl.substring(student.photoUrl.indexOf(",") + 1);
                        byte[] decodedString = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                        photoImg = com.itextpdf.text.Image.getInstance(decodedString);
                    } catch (Exception e) {
                        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                    }
                } else if (student.photoUrl.startsWith("http://") || student.photoUrl.startsWith("https://")) {
                    // Download from network URL (Firebase Storage / HTTPS)
                    try {
                        java.net.URL url = new java.net.URL(student.photoUrl);
                        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(15000);
                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("Accept", "image/*");
                        connection.setInstanceFollowRedirects(true);
                        // Follow HTTPS to HTTPS redirects
                        int responseCode = connection.getResponseCode();
                        if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                            java.io.InputStream in = connection.getInputStream();
                            java.io.ByteArrayOutputStream outStream = new java.io.ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                outStream.write(buffer, 0, bytesRead);
                            }
                            in.close();
                            byte[] imageBytes = outStream.toByteArray();
                            if (imageBytes.length > 0) {
                                photoImg = com.itextpdf.text.Image.getInstance(imageBytes);
                            }
                        }
                        connection.disconnect();
                    } catch (Exception e) {
                        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                    }
                } else {
                    // Try as a local file path
                    try {
                        java.io.File file = new java.io.File(student.photoUrl);
                        if (file.exists()) {
                            photoImg = com.itextpdf.text.Image.getInstance(student.photoUrl);
                        }
                    } catch (Exception e) {
                        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                    }
                }
            }

            if (photoImg == null) {
                int avatarRes = R.drawable.ic_person;
                String g = student.gender != null ? student.gender.toLowerCase() : "";
                if (g.contains("female") || g.contains("स्त्री") || g.contains("मुलगी") || g.equals("2")) {
                    avatarRes = R.drawable.ic_girl_avatar;
                } else {
                    avatarRes = R.drawable.ic_boy_avatar;
                }
                photoImg = getItextImageFromDrawable(ctx, avatarRes);
            }
        } catch (Exception e) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
        }

        if (photoImg != null) {
            avatarCell.setCellEvent(new ImageBackgroundEvent(photoImg, primaryColor, 15f));
        } else {
            // Last fallback: rounded border event with text symbol inside
            avatarCell.setCellEvent(new RoundedBackgroundEvent(secondaryBgColor, primaryColor, 15f));
            avatarCell.setPadding(12);
            Font bigF = new Font(Font.FontFamily.HELVETICA, 36, Font.BOLD, primaryColor);
            boolean isF = student.gender != null && (student.gender.startsWith("F")
                    || student.gender.startsWith("f")
                    || student.gender.equals("2")
                    || student.gender.contains("स्त्री")
                    || student.gender.contains("मुलगी"));
            Paragraph p = new Paragraph(isF ? "♀" : "♂", bigF);
            p.setAlignment(Element.ALIGN_CENTER);
            avatarCell.addElement(p);
        }
        profileGrid.addCell(avatarCell);
        profileGrid.setSpacingAfter(14);
        doc.add(profileGrid);

        // ── 4. Calculate Statistics (Total Academic Marks & Percentage) ─────────
        List<Subject> subjects = cls.subjects;
        int grandObtained = 0;
        int grandMax = 0;
        if (subjects != null) {
            for (Subject sub : subjects) {
                int summativeMax = sub.maxTondi + sub.maxPratyakshikB + sub.maxLekhi;
                boolean isNonAcademic = (summativeMax == 0 && sub.maxMarks > 0);
                if (!isNonAcademic) {
                    grandMax += sub.maxMarks * 2; // Total of both semesters
                    MarksRecord.SubjectMarksDetail d1 = detail(sem1, sub.name);
                    MarksRecord.SubjectMarksDetail d2 = detail(sem2, sub.name);
                    if (d1 != null)
                        grandObtained += d1.grandTotal;
                    if (d2 != null)
                        grandObtained += d2.grandTotal;
                }
            }
        }

        double percentage = grandMax > 0 ? (grandObtained * 100.0) / grandMax : 0.0;
        String overallGrade = grandMax > 0 ? com.kartik.myschool.utils.GradeCalculator.getGrade(percentage) : "-";
        String resultStrMr = percentage >= 35.0 ? "उत्तीर्ण (PASS)" : "अनुत्तीर्ण (FAIL)";

        // ── 5. Stat Metric Cards (4 Columns: Total, Percentage, Grade, Result) ─
        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setWidths(new float[] { 1f, 1f, 1f, 1f });

        addStatCard(statsTable, ctx, "एकूण गुण (Total)", grandObtained + " / " + grandMax, primaryColor,
                secondaryBgColor);
        addStatCard(statsTable, ctx, "टक्केवारी (Percent)", String.format(java.util.Locale.US, "%.2f %%", percentage),
                primaryColor, secondaryBgColor);
        addStatCard(statsTable, ctx, "श्रेणी (Grade)", overallGrade, primaryColor, secondaryBgColor);
        addStatCard(statsTable, ctx, "निकाल (Result)", resultStrMr, primaryColor, secondaryBgColor);

        statsTable.setSpacingAfter(16);
        doc.add(statsTable);

        // ── 6. High-Legibility Marks Summary Table ────────────────────────────
        float[] w = { 0.5f, 2.5f, 2f, 2f, 1.2f, 1f };
        PdfPTable tbl = new PdfPTable(w);
        tbl.setWidthPercentage(100);

        // Table headers (Primary accent backgrounds)
        addTableHeaderCell(tbl, ctx, "क्र.", primaryColor);
        addTableHeaderCell(tbl, ctx, "विषय (Subject)", primaryColor);
        addTableHeaderCell(tbl, ctx, "प्रथम सत्र (Semester 1)", primaryColor);
        addTableHeaderCell(tbl, ctx, "द्वितीय सत्र (Semester 2)", primaryColor);
        addTableHeaderCell(tbl, ctx, "एकूण (Total)", primaryColor);
        addTableHeaderCell(tbl, ctx, "श्रेणी", primaryColor);

        if (subjects != null) {
            boolean alt = false;
            for (int i = 0; i < subjects.size(); i++) {
                Subject sub = subjects.get(i);
                BaseColor bg = alt ? new BaseColor(248, 249, 250) : C_WHITE;
                alt = !alt;

                MarksRecord.SubjectMarksDetail d1 = detail(sem1, sub.name);
                MarksRecord.SubjectMarksDetail d2 = detail(sem2, sub.name);

                // Sr. No & Subject
                addTableCell(tbl, ctx, String.valueOf(i + 1), false, bg, Element.ALIGN_CENTER);
                addTableCell(tbl, ctx, PdfLocalizer.translateSubject(ctx, sub.name), true, bg, Element.ALIGN_LEFT);

                // Sem 1 Details
                if (d1 != null) {
                    String detailText = String.format(java.util.Locale.US, "आकारिक: %s | संकलित: %s\nएकूण: %s (%s)",
                            strZero(d1.akarikTotal), strZero(d1.sanklit), str(d1.grandTotal), nvl(d1.grade));
                    addTableCell(tbl, ctx, detailText, false, bg, Element.ALIGN_CENTER);
                } else {
                    addTableCell(tbl, ctx, "-", false, bg, Element.ALIGN_CENTER);
                }

                // Sem 2 Details
                if (d2 != null) {
                    String detailText = String.format(java.util.Locale.US, "आकारिक: %s | संकलित: %s\nएकूण: %s (%s)",
                            strZero(d2.akarikTotal), strZero(d2.sanklit), str(d2.grandTotal), nvl(d2.grade));
                    addTableCell(tbl, ctx, detailText, false, bg, Element.ALIGN_CENTER);
                } else {
                    addTableCell(tbl, ctx, "-", false, bg, Element.ALIGN_CENTER);
                }

                // Grand Total & Year Grade
                int subTotal = 0;
                int subMax = sub.maxMarks * 2;
                if (d1 != null)
                    subTotal += d1.grandTotal;
                if (d2 != null)
                    subTotal += d2.grandTotal;

                String grandStr = subTotal + " / " + subMax;
                addTableCell(tbl, ctx, grandStr, true, bg, Element.ALIGN_CENTER);

                double subPercent = subMax > 0 ? (subTotal * 100.0) / subMax : 0.0;
                String subGrade = subMax > 0 ? com.kartik.myschool.utils.GradeCalculator.getGrade(subPercent) : "-";
                addTableCell(tbl, ctx, subGrade, true, bg, Element.ALIGN_CENTER);
            }
        }
        tbl.setSpacingAfter(18);
        doc.add(tbl);

        // ── 7. Signature Blocks (Parent, Teacher, Headmaster cards) ───────────
        PdfPTable sigTable = new PdfPTable(3);
        sigTable.setWidthPercentage(100);
        sigTable.setWidths(new float[] { 1f, 1f, 1f });

        addSignatureCard(sigTable, ctx, "पालक सही\n(Parent's Signature)", primaryColor);
        addSignatureCard(sigTable, ctx, "वर्गशिक्षक सही\n" + (cls != null ? nvl(cls.teacherName) : ""), primaryColor);
        addSignatureCard(sigTable, ctx, "मुख्याध्यापक सही\n" + (school != null ? nvl(school.principalName) : ""),
                primaryColor);

        doc.add(sigTable);
    }

    private static void addDetailRow(PdfPTable table, Context ctx, String label, String val, BaseColor primaryColor) {
        PdfPCell labelCell = new PdfPCell();
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        try {
            int mrColor = Color.rgb(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue());
            labelCell.addElement(MarathiText.renderLine(label + " : ", 9.5f, true, mrColor));
        } catch (Exception e) {
            labelCell.setPhrase(new Phrase(label + " : ", fSmallBold));
        }
        table.addCell(labelCell);

        PdfPCell valCell = new PdfPCell();
        valCell.setBorder(Rectangle.NO_BORDER);
        valCell.setPadding(4);
        try {
            valCell.addElement(MarathiText.renderLine(val, 9.5f, false, Color.BLACK));
        } catch (Exception e) {
            valCell.setPhrase(new Phrase(val, fSmall));
        }
        table.addCell(valCell);
    }

    private static void addStatCard(PdfPTable table, Context ctx, String metric, String val, BaseColor primaryColor,
            BaseColor secondaryBgColor) {
        PdfPCell cardCell = new PdfPCell();
        cardCell.setBorder(Rectangle.NO_BORDER);
        cardCell.setPadding(5);

        // Inner vertical table for the stat representation
        PdfPTable stat = new PdfPTable(1);
        stat.setWidthPercentage(100);

        PdfPCell container = new PdfPCell();
        container.setCellEvent(new RoundedBackgroundEvent(secondaryBgColor, primaryColor, 8f));
        container.setPaddingTop(8);
        container.setPaddingBottom(8);
        container.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Metric name (Marathi rendered line)
        try {
            int mrColor = Color.rgb(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue());
            com.itextpdf.text.Image metricImg = MarathiText.renderLine(metric.toUpperCase(), 8.5f, true, mrColor);
            metricImg.setAlignment(Element.ALIGN_CENTER);
            container.addElement(metricImg);
        } catch (Exception e) {
            Paragraph p = new Paragraph(metric.toUpperCase(), fMicro);
            p.setAlignment(Element.ALIGN_CENTER);
            container.addElement(p);
        }

        // Value text
        try {
            com.itextpdf.text.Image valImg = MarathiText.renderLine(val, 11f, true, Color.BLACK);
            valImg.setAlignment(Element.ALIGN_CENTER);
            container.addElement(valImg);
        } catch (Exception e) {
            Paragraph p = new Paragraph(val, fSmallBold);
            p.setAlignment(Element.ALIGN_CENTER);
            container.addElement(p);
        }

        stat.addCell(container);
        cardCell.addElement(stat);
        table.addCell(cardCell);
    }

    private static void addTableHeaderCell(PdfPTable table, Context ctx, String text, BaseColor primaryColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(primaryColor);
        cell.setBorderColor(C_WHITE);
        cell.setBorderWidth(1f);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {
            com.itextpdf.text.Image img = MarathiText.renderLine(text, 10f, true, Color.WHITE);
            img.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(img);
        } catch (Exception e) {
            cell.setPhrase(new Phrase(text, fSmallBold));
        }
        table.addCell(cell);
    }

    private static void addTableCell(PdfPTable table, Context ctx, String text, boolean bold, BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        cell.setBorderWidth(0.5f);
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {
            // Check if multi-line is needed
            if (text.contains("\n")) {
                MarathiText.paraCell(table, text, 9f, bold, bg, C_DARK, 1, 140f);
                return;
            }
            com.itextpdf.text.Image img = MarathiText.renderLine(text, 9f, bold, Color.BLACK);
            img.setAlignment(align == Element.ALIGN_LEFT ? com.itextpdf.text.Image.LEFT
                    : align == Element.ALIGN_RIGHT ? com.itextpdf.text.Image.RIGHT : com.itextpdf.text.Image.MIDDLE);
            cell.addElement(img);
        } catch (Exception e) {
            Font font = bold ? fSmallBold : fSmall;
            cell.setPhrase(new Phrase(text, font));
        }
        table.addCell(cell);
    }

    private static void addSignatureCard(PdfPTable table, Context ctx, String label, BaseColor primaryColor) {
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(8);

        PdfPTable sig = new PdfPTable(1);
        sig.setWidthPercentage(100);

        PdfPCell c = new PdfPCell();
        c.setCellEvent(new RoundedBackgroundEvent(C_WHITE, new BaseColor(200, 200, 200), 8f));
        c.setPaddingTop(36); // Spacer for physical signature placement
        c.setPaddingBottom(6);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);

        try {
            int mrColor = Color.rgb(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue());
            com.itextpdf.text.Image labelImg = MarathiText.renderLine(label, 8.5f, true, mrColor);
            labelImg.setAlignment(Element.ALIGN_CENTER);
            c.addElement(labelImg);
        } catch (Exception e) {
            Paragraph p = new Paragraph(label, fMicro);
            p.setAlignment(Element.ALIGN_CENTER);
            c.addElement(p);
        }
        sig.addCell(c);
        wrapper.addElement(sig);
        table.addCell(wrapper);
    }

    private static void addCenterText(PdfPTable tbl, String text, Font font) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingBottom(4);
        try {
            boolean bold = (font == fSmallBold || font == fTitle);
            int size = (font == fTitle) ? 18 : 10;
            com.itextpdf.text.Image img = MarathiText.renderLine(text, size, bold, android.graphics.Color.BLACK);
            img.setAlignment(Element.ALIGN_CENTER);
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, font));
        }
        tbl.addCell(c);
    }

    private static com.itextpdf.text.Image getItextImageFromDrawable(Context ctx, int drawableResId) {
        try {
            android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(ctx, drawableResId);
            if (d == null) return null;
            android.graphics.Bitmap bitmap;
            if (d instanceof android.graphics.drawable.BitmapDrawable) {
                bitmap = ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
            } else {
                int width = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : 200;
                int height = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 200;
                bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                d.draw(canvas);
            }
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            return com.itextpdf.text.Image.getInstance(byteArray);
        } catch (Exception e) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            return null;
        }
    }
}
