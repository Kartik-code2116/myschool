package com.example.myschool.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Minimal MongoDB Atlas Data API uploader.
 *
 * How to configure (in USER gradle.properties, not in code):
 * - ATLAS_DATA_API_URL=https://data.mongodb-api.com/app/<app-id>/endpoint/data/v1/action/insertOne
 * - ATLAS_DATA_API_KEY=<your api key>
 * - ATLAS_DATA_SOURCE=<cluster name>
 * - ATLAS_DB=<db name>
 * - ATLAS_COLLECTION=<collection name>
 */
public final class AtlasDataApiClient {
    private static final String TAG = "AtlasDataApi";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final String apiUrl;
    private final String apiKey;
    private final String dataSource;
    private final String db;
    private final String collection;

    public AtlasDataApiClient(
            @NonNull String apiUrl,
            @NonNull String apiKey,
            @NonNull String dataSource,
            @NonNull String db,
            @NonNull String collection
    ) {
        this.apiUrl = apiUrl.trim();
        this.apiKey = apiKey.trim();
        this.dataSource = dataSource.trim();
        this.db = db.trim();
        this.collection = collection.trim();

        this.http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public boolean isConfigured() {
        return !apiUrl.isEmpty()
                && !apiKey.isEmpty()
                && !dataSource.isEmpty()
                && !db.isEmpty()
                && !collection.isEmpty();
    }

    /**
     * Upload one OCR record to Atlas. Returns true on 2xx.
     */
    public boolean uploadRecord(@NonNull OcrRecordEntity record, @Nullable String deviceId) {
        if (!isConfigured()) return false;

        try {
            JSONObject doc = new JSONObject();
            doc.put("timestamp", record.timestamp);
            doc.put("numbers", record.numbers == null ? "" : record.numbers);
            if (record.imagePath != null) doc.put("imagePath", record.imagePath);
            if (record.pdfPath != null) doc.put("pdfPath", record.pdfPath);
            if (deviceId != null) doc.put("deviceId", deviceId);

            JSONObject payload = new JSONObject();
            payload.put("dataSource", dataSource);
            payload.put("database", db);
            payload.put("collection", collection);
            payload.put("document", doc);

            Request req = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("api-key", apiKey)
                    .post(RequestBody.create(payload.toString(), JSON))
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    String body = resp.body() != null ? resp.body().string() : "";
                    Log.e(TAG, "Upload failed: " + resp.code() + " " + body);
                    return false;
                }
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Upload IO error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Upload error: " + e.getMessage());
            return false;
        }
    }
}

