import re
import os

filepath = "app/src/main/java/com/example/myschool/utils/PdfGenerator.java"
with open(filepath, "r", encoding="utf-8") as f:
    code = f.read()

gunapattrak_match = re.search(r'public static void generateGunapattrak\(.*?try \{(.*?)(?=doc\.close\(\);).*?catch', code, re.DOTALL)
descriptive_match = re.search(r'public static void generateDescriptive\(.*?try \{(.*?)(?=doc\.close\(\);).*?catch', code, re.DOTALL)
personality_match = re.search(r'public static void generatePersonalityRecord\(.*?try \{(.*?)(?=doc\.close\(\);).*?catch', code, re.DOTALL)

def extract_content(match_text):
    lines = match_text.split('\n')
    filtered = []
    for line in lines:
        if 'ensureFonts' in line or 'File out =' in line or 'Document doc =' in line or 'PdfWriter.getInstance' in line or 'doc.open()' in line or 'doc.setMargins' in line:
            continue
        filtered.append(line)
    return '\n'.join(filtered)

gunapattrak_content = extract_content(gunapattrak_match.group(1))
descriptive_content = extract_content(descriptive_match.group(1))
personality_content = extract_content(personality_match.group(1))

new_generate = """
    // ═════════════════════════════════════════════════════════════════════════
    //  COMBINED SINGLE-STUDENT REPORT (used by MarksheetActivity)
    // ═════════════════════════════════════════════════════════════════════════
    public static void generate(Context ctx, School school, ClassModel cls,
                                Student student, MarksRecord marks, PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "CombinedReport_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);

                // --- Page 1: Cover Page ---
                addCoverPageContent(doc, ctx, school, cls, student);
                doc.newPage();

                // --- Page 2: Gunapattrak ---
                addGunapattrakContent(doc, ctx, school, cls, student, marks, null);
                
                // --- Page 3: Descriptive ---
                doc.newPage();
                addDescriptiveContent(doc, ctx, school, cls, student, marks, null);

                // --- Page 4: Personality ---
                doc.newPage();
                addPersonalityContent(doc, ctx, school, cls, student, marks, null);

                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }

    private static void addCoverPageContent(Document doc, Context ctx, School school, ClassModel cls, Student student) throws Exception {
        doc.add(buildAppHeader(ctx));
        
        Paragraph title1 = para("सातत्यपूर्ण सर्वकष मूल्यमापन", colored(fHeader, C_PRIMARY));
        title1.setAlignment(Element.ALIGN_CENTER);
        title1.setSpacingBefore(100);
        title1.setSpacingAfter(100);
        doc.add(title1);

        PdfPTable tbl = new PdfPTable(2);
        tbl.setWidthPercentage(80);
        
        cell(tbl, "युडायस", fBold, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        cell(tbl, ": " + (school != null ? nvl(school.udiseCode) : ""), fNormal, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        
        cell(tbl, "शाळा", fBold, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        cell(tbl, ": " + (school != null ? nvl(school.name) : ""), fNormal, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        
        cell(tbl, "वर्गशिक्षक", fBold, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        cell(tbl, ": " + (cls != null ? nvl(cls.classTeacher) : ""), fNormal, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        
        cell(tbl, "इयत्ता", fBold, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        cell(tbl, ": " + (cls != null ? nvl(cls.className) : ""), fNormal, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        
        cell(tbl, "तुकडी", fBold, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        cell(tbl, ": -", fNormal, C_WHITE, C_DARK, 1, Element.ALIGN_LEFT);
        
        doc.add(tbl);
    }

    private static void addGunapattrakContent(Document doc, Context ctx, School school, ClassModel cls, Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {
""" + gunapattrak_content + """
    }

    private static void addDescriptiveContent(Document doc, Context ctx, School school, ClassModel cls, Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {
""" + descriptive_content + """
    }

    private static void addPersonalityContent(Document doc, Context ctx, School school, ClassModel cls, Student student, MarksRecord sem1, MarksRecord sem2) throws Exception {
""" + personality_content + """
    }
"""

legacy_generate_match = re.search(r'    // ═════════════════════════════════════════════════════════════════════════\s*//  LEGACY — single-semester \(used by MarksheetActivity\)\s*// ═════════════════════════════════════════════════════════════════════════\s*public static void generate\(Context.*?callback\);.*?\}', code, re.DOTALL)
if legacy_generate_match:
    code = code.replace(legacy_generate_match.group(0), new_generate)

new_generateGunapattrak = """public static void generateGunapattrak(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           Student student,
                                           MarksRecord sem1,
                                           MarksRecord sem2,
                                           PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "Gunapattrak_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addGunapattrakContent(doc, ctx, school, cls, student, sem1, sem2);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }"""
code = code.replace(re.search(r'public static void generateGunapattrak\(.*?catch.*?\}\)\.start\(\);\n    \}', code, re.DOTALL).group(0), new_generateGunapattrak)

new_generateDescriptive = """public static void generateDescriptive(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           Student student,
                                           MarksRecord sem1,
                                           MarksRecord sem2,
                                           PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "Descriptive_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addDescriptiveContent(doc, ctx, school, cls, student, sem1, sem2);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }"""
code = code.replace(re.search(r'public static void generateDescriptive\(.*?catch.*?\}\)\.start\(\);\n    \}', code, re.DOTALL).group(0), new_generateDescriptive)

new_generatePersonality = """public static void generatePersonalityRecord(Context ctx,
                                           School school,
                                           ClassModel cls,
                                           Student student,
                                           MarksRecord sem1,
                                           MarksRecord sem2,
                                           PdfCallback cb) {
        new Thread(() -> {
            try {
                ensureFonts(ctx);
                File out = new File(outDir(ctx), "Personality_" + safeRoll(student) + "_" + ts() + ".pdf");
                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                doc.setMargins(30, 30, 30, 30);
                addPersonalityContent(doc, ctx, school, cls, student, sem1, sem2);
                doc.close();
                cb.onSuccess(out);
            } catch (Exception e) { cb.onError(e); }
        }).start();
    }"""
code = code.replace(re.search(r'public static void generatePersonalityRecord\(.*?catch.*?\}\)\.start\(\);\n    \}', code, re.DOTALL).group(0), new_generatePersonality)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(code)

print("Refactoring complete.")
