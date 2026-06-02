package com.example.myschool.utils;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class PdfMerger {
    public static void mergePdfFiles(List<File> files, File outputFile) throws Exception {
        if (files == null || files.isEmpty()) return;

        Document document = new Document();
        PdfCopy copy = new PdfCopy(document, new FileOutputStream(outputFile));
        document.open();

        for (File f : files) {
            if (f != null && f.exists()) {
                PdfReader reader = new PdfReader(f.getAbsolutePath());
                int pages = reader.getNumberOfPages();
                for (int i = 1; i <= pages; i++) {
                    copy.addPage(copy.getImportedPage(reader, i));
                }
                copy.freeReader(reader);
                reader.close();
            }
        }
        document.close();
    }
}
