package com.kartik.myschool.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ocr_records")
public class OcrRecordEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @NonNull
    @ColumnInfo(name = "numbers")
    public String numbers = "";

    @ColumnInfo(name = "image_path")
    public String imagePath;

    @ColumnInfo(name = "pdf_path")
    public String pdfPath;
}

