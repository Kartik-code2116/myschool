package com.kartik.myschool.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.kartik.myschool.model.ClassModel;
import com.kartik.myschool.model.Student;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class ExportUtils {

    public static void exportUdiseToExcel(Context context, List<Student> students, ClassModel classModel) {
        if (students == null || students.isEmpty()) {
            Toast.makeText(context, "No students to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            
            // UDISE+ standard format headers (simplified for common denominator)
            sb.append("Student Name,Gender,Date of Birth,Social Category,Religion,Mother Tongue,Aadhaar Number (UID),");
            sb.append("Mother's Name,Father's Name,Admission Date,Class,Section,Roll Number,Blood Group,Disability Type\n");

            for (Student s : students) {
                sb.append(escapeCsv(s.name)).append(",")
                  .append(escapeCsv(s.gender)).append(",")
                  .append(escapeCsv(s.dob)).append(",")
                  .append(escapeCsv(s.cast)).append(",") // Social Category
                  .append(escapeCsv(s.religion)).append(",")
                  .append(escapeCsv(s.motherTongue)).append(",")
                  .append(escapeCsv(s.uid)).append(",") // Aadhaar
                  .append(escapeCsv(s.motherName)).append(",")
                  .append(escapeCsv(s.fatherName)).append(",")
                  .append(escapeCsv(s.dateOfAdmission)).append(",")
                  .append(escapeCsv(s.standard)).append(",")
                  .append(escapeCsv(s.division)).append(",")
                  .append(escapeCsv(s.rollNo)).append(",")
                  .append(escapeCsv(s.bloodGroup)).append(",")
                  .append(escapeCsv("")) // Disability Type (Add if later supported)
                  .append("\n");
            }

            File cachePath = new File(context.getCacheDir(), "excel_exports");
            cachePath.mkdirs();
            
            String className = classModel != null ? classModel.className : "class";
            String division = classModel != null ? classModel.division : "div";
            File file = new File(cachePath, "UDISE_APAAR_" + className + "_" + division + ".csv");
            
            FileWriter writer = new FileWriter(file);
            writer.write(sb.toString());
            writer.close();

            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "Export UDISE+ / APAAR Format"));
        } catch (Exception e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private static String escapeCsv(String str) {
        if (str == null) return "";
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
