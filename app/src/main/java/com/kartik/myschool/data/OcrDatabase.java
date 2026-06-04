package com.kartik.myschool.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {OcrRecordEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class OcrDatabase extends RoomDatabase {
    public abstract OcrRecordDao ocrRecordDao();
}

