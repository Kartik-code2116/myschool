package com.kartik.myschool.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface OcrRecordDao {
    @Insert
    long insert(OcrRecordEntity record);

    @Update
    int update(OcrRecordEntity record);

    @Query("SELECT * FROM ocr_records ORDER BY timestamp DESC LIMIT 1")
    OcrRecordEntity getLatest();

    @Query("SELECT * FROM ocr_records ORDER BY timestamp DESC")
    List<OcrRecordEntity> getAllLatestFirst();

    @Query("UPDATE ocr_records SET pdf_path = :pdfPath WHERE id = :id")
    int updatePdfPath(long id, String pdfPath);
}

