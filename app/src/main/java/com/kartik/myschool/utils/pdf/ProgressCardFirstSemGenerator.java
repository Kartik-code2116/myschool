package com.kartik.myschool.utils.pdf;

import android.content.Context;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
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

import static com.kartik.myschool.utils.PdfGenerator.C_DARK;
import static com.kartik.myschool.utils.PdfGenerator.C_HEADER_BG;
import static com.kartik.myschool.utils.PdfGenerator.C_WHITE;
import static com.kartik.myschool.utils.PdfGenerator.fSmall;
import static com.kartik.myschool.utils.PdfGenerator.fSmallBold;
import static com.kartik.myschool.utils.PdfGenerator.fTitle;
import static com.kartik.myschool.utils.PdfGenerator.nvl;
import static com.kartik.myschool.utils.PdfGenerator.sMarathiBase;

public class ProgressCardFirstSemGenerator {

    private static final BaseColor C_BLUE_BORDER = new BaseColor(96, 150, 180);
    private static final BaseColor C_PINK_BG = new BaseColor(252, 228, 236);
    private static final BaseColor C_PINK_FG = new BaseColor(216, 27, 96);
    private static final BaseColor C_GREY_BG = new BaseColor(240, 242, 245);
    private static final BaseColor C_BULLET = new BaseColor(41, 128, 185);

    private static final String[] MONTHS_EN = {"JUN", "JUL", "AUG", "SEP", "OCT", "NOV"};
    private static final String[] MONTHS_MR = {"जून", "जुलै", "ऑगस्ट", "सप्टें", "ऑक्टो", "नोव्हे"};

    private static class BorderEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle rect = document.getPageSize();
            canvas.setColorStroke(C_BLUE_BORDER);
            canvas.setLineWidth(2f);
            canvas.roundRectangle(25, 25, rect.getWidth() - 50, rect.getHeight() - 50, 15);
            canvas.stroke();
        }
    }

    private static class RoundedBackgroundEvent implements PdfPCellEvent {
        private BaseColor bgColor;
        public RoundedBackgroundEvent(BaseColor bgColor) { this.bgColor = bgColor; }
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            canvas.setColorFill(bgColor);
            canvas.roundRectangle(position.getLeft(), position.getBottom(), position.getWidth(), position.getHeight(), 10);
            canvas.fill();
        }
    }

    public static void generateProgressCardFirstSem(Context ctx,
                                                    School school,
                                                    ClassModel cls,
                                                    List<Student> students,
                                                    Map<String, MarksRecord> marksMap,
                                                    PdfGenerator.PdfCallback cb) {
        new Thread(() -> {
            try {
                PdfGenerator.ensureFonts(ctx);
                File out = new File(PdfGenerator.outDir(ctx),
                        "ProgressCardFirstSem_" + PdfGenerator.ts() + ".pdf");

                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
                writer.setPageEvent(new com.kartik.myschool.utils.pdf.DynamicMarginHelper(ctx));
                com.kartik.myschool.utils.pdf.DynamicMarginHelper.applyMarginsForPage(ctx, doc, 1);
                writer.setPageEvent(new BorderEvent());
                doc.open();
                doc.setMargins(40, 40, 45, 45);

                boolean isFirst = true;
                if (students != null) {
                    for (Student student : students) {
                        if (!isFirst) doc.newPage();
                        isFirst = false;
                        MarksRecord rec = marksMap != null ? marksMap.get(student.id) : null;
                        
                        // Strictly use the passed marksMap which represents Semester 1 marks.
                        // Do not fallback to AppCache, as AppCache might hold Semester 2 data if the user navigated to Semester 2.
                        MarksRecord bestRemarkRec = rec;

                        addStudentPage(doc, ctx, school, cls, student, rec, bestRemarkRec);
                    }
                }
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) {
                cb.onError(e);
            }
        }).start();
    }

    private static void addStudentPage(Document doc, Context ctx, School school, ClassModel cls, Student student, MarksRecord rec, MarksRecord bestRemarkRec) throws Exception {
        PdfPTable outer = new PdfPTable(new float[]{1f, 1f});
        outer.setWidthPercentage(100);
        
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPaddingRight(15);
        buildLeftPanel(leftCell, ctx, school, cls, student);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPaddingLeft(15);
        buildRightPanel(rightCell, ctx, cls, rec, bestRemarkRec);

        outer.addCell(leftCell);
        outer.addCell(rightCell);
        doc.add(outer);
    }

    private static void buildLeftPanel(PdfPCell panel, Context ctx, School school, ClassModel cls, Student student) throws Exception {
        // ── 1. Header ─────────────────────────────────────────────────────────
        PdfPTable hdr = new PdfPTable(1);
        hdr.setWidthPercentage(100);

        String uLabel = PdfLocalizer.get(ctx, "युडायस: ", "UDISE: ");
        String uVal = nvl(school != null ? school.udiseCode : "");
        addCenterTextSplit(hdr, uLabel, uVal, fSmall);
        
        String prefix = PdfLocalizer.get(ctx, "जिल्हा परिषद प्राथमिक शाळा ", "Zilla Parishad Primary School ");
        String sName = prefix + nvl(school != null ? school.name : "");
        addCenterText(hdr, sName, fTitle); // Large bold

        String addr = nvl(school != null ? school.address : "");
        addCenterText(hdr, addr, fSmall);

        String yLabel = PdfLocalizer.get(ctx, "सन: ", "Year: ");
        String yVal = nvl(cls != null ? cls.academicYearLabel : "");
        addCenterTextSplit(hdr, yLabel, yVal, fSmallBold);
        
        hdr.setSpacingAfter(10);
        panel.addElement(hdr);

        // ── 2. "प्रगती पत्रक" Pill ──────────────────────────────────────────
        PdfPTable pillTbl = new PdfPTable(1);
        pillTbl.setWidthPercentage(55);
        PdfPCell pillCell = new PdfPCell();
        pillCell.setBorder(Rectangle.NO_BORDER);
        pillCell.setCellEvent(new RoundedBackgroundEvent(C_PINK_BG));
        pillCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        pillCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        pillCell.setPaddingTop(8);
        pillCell.setPaddingBottom(10);

        try {
            com.itextpdf.text.Image img = MarathiText.renderLine(PdfLocalizer.get(ctx, "प्रगती पत्रक", "PROGRESS CARD"), 18, true, android.graphics.Color.rgb(C_PINK_FG.getRed(), C_PINK_FG.getGreen(), C_PINK_FG.getBlue()));
            img.setAlignment(Element.ALIGN_CENTER);
            pillCell.addElement(img);
        } catch (Exception e) {
            Font pinkFont = sMarathiBase != null ? new Font(sMarathiBase, 16, Font.BOLD, C_PINK_FG)
                                                 : new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, C_PINK_FG);
            pillCell.setPhrase(new Phrase(PdfLocalizer.get(ctx, "प्रगती पत्रक", "PROGRESS CARD"), pinkFont));
        }
        pillTbl.addCell(pillCell);
        pillTbl.setSpacingAfter(15);
        panel.addElement(pillTbl);

        // ── 3. Student Details Block ──────────────────────────────────────────
        PdfPTable block = new PdfPTable(1);
        block.setWidthPercentage(100);
        
        PdfPCell blockCell = new PdfPCell();
        blockCell.setBorder(Rectangle.NO_BORDER);
        blockCell.setCellEvent(new RoundedBackgroundEvent(C_GREY_BG));
        blockCell.setPadding(15);

        PdfPTable det = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2f});
        det.setWidthPercentage(100);

        String className = cls != null ? nvl(cls.className) : "";
        String division  = cls != null ? nvl(cls.division) : "-";

        String ageStr = "-";
        if (student.dob != null && !student.dob.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
                java.util.Date dob = sdf.parse(student.dob);
                if (dob != null) {
                    long diffMs = System.currentTimeMillis() - dob.getTime();
                    long years  = diffMs / (365L * 24 * 60 * 60 * 1000);
                    long months = (diffMs % (365L * 24 * 60 * 60 * 1000)) / (30L * 24 * 60 * 60 * 1000);
                    ageStr = years + PdfLocalizer.get(ctx, " व., ", " Y., ") + months + PdfLocalizer.get(ctx, " म.", " M.");
                }
            } catch (Exception ignored) {}
        }

        addRowFull(det, PdfLocalizer.get(ctx, "नाव", "Name"), ": " + nvl(student.name));
        addPairEmptyRight(det, PdfLocalizer.get(ctx, "स्टुडंट ID", "Student ID"), ": " + nvl(student.studentIdNumber));
        addPair(det, PdfLocalizer.get(ctx, "हजेरी क्रमांक", "Roll No."), ": " + nvl(student.rollNo), PdfLocalizer.get(ctx, "रजि.नंबर", "Reg. No."), ": " + nvl(student.registrationNo));
        addPair(det, PdfLocalizer.get(ctx, "इयत्ता", "Class"), ": " + className, PdfLocalizer.get(ctx, "तुकडी", "Division"), ": " + division);
        addPair(det, PdfLocalizer.get(ctx, "माध्यम", "Medium"), ": " + nvl(student.medium), PdfLocalizer.get(ctx, "जन्मतारीख", "Date of Birth"), ": " + nvl(student.dob));
        addPair(det, PdfLocalizer.get(ctx, "मातृभाषा", "Mother Tongue"), ": " + nvl(student.motherTongue), PdfLocalizer.get(ctx, "वय", "Age"), ": " + ageStr);
        addRowFull(det, PdfLocalizer.get(ctx, "आईचे नाव", "Mother's Name"), ": " + nvl(student.motherName));
        addRowFull(det, PdfLocalizer.get(ctx, "वडिलांचे नाव", "Father's Name"), ": " + nvl(student.fatherName));
        addRowFull(det, PdfLocalizer.get(ctx, "पत्ता", "Address"), ": " + nvl(student.address));

        blockCell.addElement(det);
        block.addCell(blockCell);
        block.setSpacingAfter(20);
        panel.addElement(block);

        // ── 4. Attendance ─────────────────────────────────────────────────────
        PdfPTable attTitle = new PdfPTable(1);
        attTitle.setWidthPercentage(100);
        PdfPCell attTitleCell = new PdfPCell();
        attTitleCell.setBorder(Rectangle.NO_BORDER);
        try {
            com.itextpdf.text.Image aImg = MarathiText.renderLine(PdfLocalizer.get(ctx, "उपस्थिती", "Attendance"), 12, true, android.graphics.Color.rgb(C_PINK_FG.getRed(), C_PINK_FG.getGreen(), C_PINK_FG.getBlue()));
            attTitleCell.addElement(aImg);
        } catch (Exception e) {
            attTitleCell.setPhrase(new Phrase(PdfLocalizer.get(ctx, "उपस्थिती", "Attendance"), fSmallBold));
        }
        attTitle.addCell(attTitleCell);
        attTitle.setSpacingAfter(5);
        panel.addElement(attTitle);

        PdfPTable attTbl = new PdfPTable(new float[]{2f, 1f, 1f, 1f, 1f, 1f, 1f}); // महिना + 6 months
        attTbl.setWidthPercentage(100);
        
        MarathiText.cell(attTbl, PdfLocalizer.get(ctx, "महिना", "Month"), 9, true, C_GREY_BG, android.graphics.Color.BLACK, 1, 1, Element.ALIGN_CENTER);
        
        String[] monthsEN = MONTHS_EN;
        String[] monthsMR = MONTHS_MR;
        boolean isEn = PdfLocalizer.isEnglish(ctx);
        String[] activeMonths = isEn ? monthsEN : monthsMR;
        
        for (String m : activeMonths) {
            PdfPCell mc = new PdfPCell();
            mc.setHorizontalAlignment(Element.ALIGN_CENTER);
            mc.setVerticalAlignment(Element.ALIGN_MIDDLE);
            mc.setBackgroundColor(C_WHITE);
            mc.setBorderColor(C_BLUE_BORDER);
            mc.setPaddingTop(5);
            mc.setPaddingBottom(5);
            try {
                com.itextpdf.text.Image img = com.kartik.myschool.utils.pdf.MarathiText.renderLine(m, 10, true, android.graphics.Color.BLACK);
                img.setAlignment(Element.ALIGN_CENTER);
                mc.addElement(img);
            } catch (Exception e) {
                mc.setPhrase(new Phrase(m, fSmallBold));
            }
            attTbl.addCell(mc);
        }

        int[] wd = new int[6];
        int[] pd = new int[6];
        if (student.monthlyAttendance != null) {
            for (int i = 0; i < 6; i++) {
                String att = student.monthlyAttendance.get(monthsMR[i]);
                if (att != null && att.contains("/")) {
                    String[] parts = att.split("/");
                    try { pd[i] = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
                    try { wd[i] = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                } else if (att != null && !att.isEmpty()) {
                    try { pd[i] = Integer.parseInt(att.trim()); } catch (Exception ignored) {}
                }
            }
        }

        MarathiText.cell(attTbl, PdfLocalizer.get(ctx, "कामाचे दिवस", "Working Days"), 9, true, C_GREY_BG, android.graphics.Color.BLACK, 1, 1, Element.ALIGN_CENTER);
        for (int i = 0; i < 6; i++) {
            String val = wd[i] > 0 ? String.valueOf(wd[i]) : "";
            PdfPCell wc = new PdfPCell(new Phrase(val, new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK)));
            wc.setHorizontalAlignment(Element.ALIGN_CENTER);
            wc.setBorderColor(C_BLUE_BORDER);
            wc.setPaddingTop(4);
            wc.setPaddingBottom(4);
            attTbl.addCell(wc);
        }

        MarathiText.cell(attTbl, PdfLocalizer.get(ctx, "हजर दिवस", "Present Days"), 9, true, C_GREY_BG, android.graphics.Color.BLACK, 1, 1, Element.ALIGN_CENTER);
        for (int i = 0; i < 6; i++) {
            String val = pd[i] > 0 ? String.valueOf(pd[i]) : (wd[i] > 0 ? "0" : "");
            PdfPCell pc = new PdfPCell(new Phrase(val, new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK)));
            pc.setHorizontalAlignment(Element.ALIGN_CENTER);
            pc.setBorderColor(C_BLUE_BORDER);
            pc.setPaddingTop(4);
            pc.setPaddingBottom(4);
            attTbl.addCell(pc);
        }

        panel.addElement(attTbl);
    }

    private static void buildRightPanel(PdfPCell panel, Context ctx, ClassModel cls, MarksRecord rec, MarksRecord bestRemarkRec) throws Exception {
        PdfPTable titleTbl = new PdfPTable(1);
        titleTbl.setWidthPercentage(100);
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        try {
            com.itextpdf.text.Image img = MarathiText.renderLine(PdfLocalizer.get(ctx, "प्रथम सत्र : श्रेणी व नोंदी", "First Semester: Grades & Remarks"), 12, true, android.graphics.Color.rgb(C_PINK_FG.getRed(), C_PINK_FG.getGreen(), C_PINK_FG.getBlue()));
            titleCell.addElement(img);
        } catch (Exception e) {
            titleCell.setPhrase(new Phrase(PdfLocalizer.get(ctx, "प्रथम सत्र : श्रेणी व नोंदी", "First Semester: Grades & Remarks"), fSmallBold));
        }
        titleTbl.addCell(titleCell);
        titleTbl.setSpacingAfter(10);
        panel.addElement(titleTbl);

        // Cols: अ.नं. | विषय | श्रेणी | वर्णनात्मक नोंदी
        PdfPTable tbl = new PdfPTable(new float[]{0.6f, 1.8f, 1.0f, 4f});
        tbl.setWidthPercentage(100);
        
        MarathiText.cell(tbl, PdfLocalizer.get(ctx, "अ.नं.", "Sr.No."), 10, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        MarathiText.cell(tbl, PdfLocalizer.get(ctx, "विषय", "Subject"), 10, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        MarathiText.cell(tbl, PdfLocalizer.get(ctx, "श्रेणी", "Grade"), 10, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);
        MarathiText.cell(tbl, PdfLocalizer.get(ctx, "वर्णनात्मक नोंदी", "Descriptive Remarks"), 10, true, C_HEADER_BG, C_DARK, 1, 1, Element.ALIGN_CENTER);

        List<Subject> subjects = (cls != null && cls.subjects != null) ? cls.subjects : new java.util.ArrayList<>();
        
        String[] defaultSubsMR = {"मराठी", "हिंदी", "इंग्रजी", "गणित", "सा.वि./ प.अ.", "स.शा.", "कला", "कार्यानुभव", "शा.शि."};
        String[] defaultSubsEN = {"Marathi", "Hindi", "English", "Maths", "Science", "Social Sci.", "Art", "Work Exp.", "P.E."};
        String[] defaultSubs = PdfLocalizer.isEnglish(ctx) ? defaultSubsEN : defaultSubsMR;
        
        String[] remLabels = {
            PdfLocalizer.get(ctx, "विशेष प्रगती : ", "Special Progress: "),
            PdfLocalizer.get(ctx, "आवड, छंद इ. : ", "Interests & Hobbies: "),
            PdfLocalizer.get(ctx, "सुधारणा आवश्यक : ", "Improvement Needed: ")
        };
        String[] remKeys = {"विशेष प्रगती", "आवड", "सुधारणा"};
        
        for (int i = 0; i < 9; i++) {
            String rawName = i < subjects.size() ? subjects.get(i).name : (i < defaultSubs.length ? defaultSubs[i] : "");
            String sName = PdfLocalizer.translateSubject(ctx, rawName);
            String grade = "-";
            if (rec != null && rawName != null && !rawName.isEmpty()) {
                MarksRecord.SubjectMarksDetail d = PdfGenerator.detail(rec, rawName);
                if (d != null && d.grade != null && !d.grade.isEmpty()) {
                    grade = PdfGenerator.normalizeGrade(d.grade);
                }
            }

            // SR NO
            String srNoStr = (i < subjects.size()) ? String.valueOf(i + 1) : "-";
            PdfPCell c1 = new PdfPCell(new Phrase(srNoStr, fSmall));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c1.setBorderColor(C_BLUE_BORDER);
            c1.setPaddingTop(12);
            c1.setPaddingBottom(12);
            tbl.addCell(c1);

            // SUBJECT
            PdfPCell c2 = new PdfPCell();
            c2.setHorizontalAlignment(Element.ALIGN_CENTER);
            c2.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c2.setBorderColor(C_BLUE_BORDER);
            try {
                com.itextpdf.text.Image img = MarathiText.renderLine(sName, 10, false, android.graphics.Color.BLACK);
                img.setAlignment(Element.ALIGN_CENTER);
                c2.addElement(img);
            } catch (Exception e) {
                c2.setPhrase(new Phrase(sName, fSmall));
            }
            tbl.addCell(c2);

            // GRADE
            PdfPCell c3 = new PdfPCell();
            c3.setHorizontalAlignment(Element.ALIGN_CENTER);
            c3.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c3.setBorderColor(C_BLUE_BORDER);
            try {
                com.itextpdf.text.Image img = MarathiText.renderLine(grade, 10, true, android.graphics.Color.BLACK);
                img.setAlignment(Element.ALIGN_CENTER);
                c3.addElement(img);
            } catch (Exception e) {
                c3.setPhrase(new Phrase(grade, fSmallBold));
            }
            tbl.addCell(c3);

            // REMARK
            String rText = findRemark(bestRemarkRec != null ? bestRemarkRec : rec, rawName);
            PdfPCell c4 = new PdfPCell();
            c4.setBorderColor(C_BLUE_BORDER);
            c4.setVerticalAlignment(Element.ALIGN_TOP);
            c4.setPadding(6);
            
            try {
                if (rText != null && !rText.isEmpty()) {
                    com.itextpdf.text.Image imgM = MarathiText.renderMultiLine(rText, 10, false, android.graphics.Color.BLACK, 180f);
                    imgM.setAlignment(Element.ALIGN_LEFT);
                    c4.addElement(imgM);
                } else {
                    c4.setPhrase(new Phrase("-", fSmall));
                }
            } catch (Exception e) {
                c4.setPhrase(new Phrase(rText, fSmall));
            }
            tbl.addCell(c4);
        }
        panel.addElement(tbl);

        // Signatures
        PdfPTable sigTbl = new PdfPTable(3);
        sigTbl.setWidthPercentage(100);
        sigTbl.setSpacingBefore(30f);

        PdfPCell s1 = rawCell(PdfLocalizer.get(ctx, "शिक्षक स्वाक्षरी", "Teacher Signature"), fSmall, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s1.setBorder(Rectangle.NO_BORDER);
        PdfPCell s2 = rawCell(PdfLocalizer.get(ctx, "पालक स्वाक्षरी", "Parent Signature"), fSmall, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s2.setBorder(Rectangle.NO_BORDER);
        PdfPCell s3 = rawCell(PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी", "Headmaster Signature"), fSmall, C_WHITE, C_DARK, Element.ALIGN_CENTER);
        s3.setBorder(Rectangle.NO_BORDER);

        try {
            s1 = new PdfPCell(); s1.setBorder(Rectangle.NO_BORDER); s1.setHorizontalAlignment(Element.ALIGN_CENTER);
            com.itextpdf.text.Image img1 = MarathiText.renderLine(PdfLocalizer.get(ctx, "शिक्षक स्वाक्षरी", "Teacher Signature"), 10, false, android.graphics.Color.BLACK);
            img1.setAlignment(Element.ALIGN_CENTER); s1.addElement(img1);
            
            s2 = new PdfPCell(); s2.setBorder(Rectangle.NO_BORDER); s2.setHorizontalAlignment(Element.ALIGN_CENTER);
            com.itextpdf.text.Image img2 = MarathiText.renderLine(PdfLocalizer.get(ctx, "पालक स्वाक्षरी", "Parent Signature"), 10, false, android.graphics.Color.BLACK);
            img2.setAlignment(Element.ALIGN_CENTER); s2.addElement(img2);

            s3 = new PdfPCell(); s3.setBorder(Rectangle.NO_BORDER); s3.setHorizontalAlignment(Element.ALIGN_CENTER);
            com.itextpdf.text.Image img3 = MarathiText.renderLine(PdfLocalizer.get(ctx, "मुख्याध्यापक स्वाक्षरी", "Headmaster Signature"), 10, false, android.graphics.Color.BLACK);
            img3.setAlignment(Element.ALIGN_CENTER); s3.addElement(img3);
        } catch(Exception ignored){}

        sigTbl.addCell(s1);
        sigTbl.addCell(s2);
        sigTbl.addCell(s3);
        panel.addElement(sigTbl);
    }

    private static String findRemark(MarksRecord rec, String key) {
        if (rec == null || rec.detailedMarks == null) return "";
        MarksRecord.SubjectMarksDetail d = PdfGenerator.detail(rec, key);
        if (d != null && d.remark != null && !d.remark.trim().isEmpty())
            return d.remark.replace("||", ", ").trim();
        
        String safeName = MarksRecord.sanitizeKey(key).toLowerCase();
        for (Map.Entry<String, MarksRecord.SubjectMarksDetail> e : rec.detailedMarks.entrySet()) {
            String k = e.getKey(); MarksRecord.SubjectMarksDetail v = e.getValue();
            if (v != null && v.remark != null && !v.remark.trim().isEmpty()) {
                String safeK = k != null ? MarksRecord.sanitizeKey(k).toLowerCase() : "";
                if (safeK.equals(safeName) || safeK.contains(safeName) || safeName.contains(safeK))
                    return v.remark.replace("||", ", ").trim();
            }
        }
        return "";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void addCenterText(PdfPTable tbl, String text, Font font) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingBottom(4);
        try {
            boolean bold = (font == fSmallBold || font == fTitle);
            int size = (font == fTitle) ? 16 : 10;
            com.itextpdf.text.Image img = MarathiText.renderLine(text, size, bold, android.graphics.Color.BLACK);
            img.setAlignment(Element.ALIGN_CENTER);
            c.addElement(img);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, font));
        }
        tbl.addCell(c);
    }
    
    private static void addCenterTextSplit(PdfPTable tbl, String label, String value, Font font) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingBottom(4);
        try {
            boolean bold = (font == fSmallBold || font == fTitle);
            int size = (font == fTitle) ? 16 : 10;
            com.itextpdf.text.Image img = MarathiText.renderLine(label + value, size, bold, android.graphics.Color.BLACK);
            img.setAlignment(Element.ALIGN_CENTER);
            c.addElement(img);
        } catch(Exception e) {
            Paragraph p = new Paragraph(label + value, font);
            p.setAlignment(Element.ALIGN_CENTER);
            c.addElement(p);
        }
        tbl.addCell(c);
    }

    private static void addRowFull(PdfPTable tbl, String label, String value) {
        addLabel(tbl, label);
        addValue(tbl, value, 3);
    }

    private static void addPair(PdfPTable tbl, String l1, String v1, String l2, String v2) {
        addLabel(tbl, l1);
        addValue(tbl, v1, 1);
        addLabel(tbl, l2);
        addValue(tbl, v2, 1);
    }
    
    private static void addPairEmptyRight(PdfPTable tbl, String l1, String v1) {
        addLabel(tbl, l1);
        addValue(tbl, v1, 1);
        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(Rectangle.NO_BORDER);
        empty.setColspan(2);
        tbl.addCell(empty);
    }

    private static void addLabel(PdfPTable tbl, String text) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingBottom(4);
        try {
            Paragraph p = new Paragraph();
            p.add(new com.itextpdf.text.Chunk("● ", new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, C_BULLET)));
            com.itextpdf.text.Image img = MarathiText.renderLine(text, 10, false, android.graphics.Color.BLACK);
            p.add(new com.itextpdf.text.Chunk(img, 0, -2, true));
            p.setAlignment(Element.ALIGN_LEFT);
            c.addElement(p);
        } catch (Exception e) {
            c.setPhrase(new Phrase("● " + text, fSmall));
        }
        tbl.addCell(c);
    }

    private static void addValue(PdfPTable tbl, String text, int colspan) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setColspan(colspan);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingBottom(4);
        try {
            com.itextpdf.text.Image img = MarathiText.renderLine(text, 10, true, android.graphics.Color.BLACK);
            Paragraph p = new Paragraph();
            p.add(new com.itextpdf.text.Chunk(img, 0, -2, true));
            p.setAlignment(Element.ALIGN_LEFT);
            c.addElement(p);
        } catch (Exception e) {
            c.setPhrase(new Phrase(text, fSmallBold));
        }
        tbl.addCell(c);
    }

    private static PdfPCell rawCell(String text, Font font, BaseColor bg, BaseColor border, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setBorderColor(border);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }
}
