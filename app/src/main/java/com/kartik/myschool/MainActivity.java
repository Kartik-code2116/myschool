package com.kartik.myschool;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.kartik.myschool.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.room.Room;

import com.kartik.myschool.data.OcrDatabase;
import com.kartik.myschool.data.OcrRecordEntity;
import com.kartik.myschool.ui.HistoryAdapter;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;
    private File currentPhotoFile;
    private String extractedNumbers = "";
    private ExecutorService dbExecutor;
    private OcrDatabase db;
    private long latestRecordId = -1L;

    private static final String TAG = "SmartOCR";
    private HistoryAdapter historyAdapter;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    allGranted = allGranted && granted;
                }
                if (allGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.msg_camera_permission_is_required, Toast.LENGTH_LONG).show();
                }
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ML Kit Text Recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        cameraExecutor = Executors.newSingleThreadExecutor();
        dbExecutor = Executors.newSingleThreadExecutor();

        // Initialize local DB
        initDb();

        // Setup click listeners
        binding.btnCapture.setOnClickListener(v -> captureImage());
        binding.btnCopy.setOnClickListener(v -> copyToClipboard());
        binding.btnGeneratePdf.setOnClickListener(v -> generatePDF());

        setupHistory();

        // Check permissions and start camera
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions();
        }
    }

    private void setupHistory() {
        historyAdapter = new HistoryAdapter(item -> {
            if (item.pdfPath != null && !item.pdfPath.isEmpty()) {
                File pdfDir = new File(getExternalFilesDir(null), "pdfs");
                File pdfFile = new File(pdfDir, item.pdfPath);
                if (pdfFile.exists() && pdfFile.getAbsolutePath().startsWith(pdfDir.getAbsolutePath())) {
                    sharePDF(pdfFile);
                } else {
                    Toast.makeText(this, "PDF file not found.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            extractedNumbers = item.numbers == null ? "" : item.numbers;
            if (!extractedNumbers.isEmpty()) {
                displayResults(extractedNumbers);
            }
        });
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerHistory.setAdapter(historyAdapter);
        refreshHistory();
    }

    private void refreshHistory() {
        if (db == null || dbExecutor == null) return;
        dbExecutor.execute(() -> {
            try {
                var list = db.ocrRecordDao().getAllLatestFirst();
                runOnUiThread(() -> historyAdapter.submit(list));
            } catch (Exception e) {
                Log.e(TAG, "History load failed: " + e.getMessage());
            }
        });
    }

    private void initDb() {
        try {
            db = Room.databaseBuilder(getApplicationContext(), OcrDatabase.class, "ocr.db")
                    .fallbackToDestructiveMigration()
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "DB initialization failed: " + e.getMessage());
            Toast.makeText(this, R.string.msg_database_initialization_failed, Toast.LENGTH_LONG).show();
        }
    }



    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureImage() {
        if (imageCapture == null) return;

        // Show processing status
        showStatus("Capturing image...");

        // Create timestamped file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(getExternalFilesDir(null), "images");
        if (!storageDir.exists()) storageDir.mkdirs();
        currentPhotoFile = new File(storageDir, imageFileName + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(currentPhotoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults output) {
                        showStatus("Processing OCR...");
                        processImage(currentPhotoFile);
                    }

                    @Override
                    public void onError(ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage());
                        hideStatus();
                        Toast.makeText(MainActivity.this, R.string.msg_capture_failed, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void processImage(File imageFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap == null) {
                hideStatus();
                Toast.makeText(this, R.string.msg_failed_to_load_image, Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap ocrBitmap = preprocessForOcr(bitmap);
            InputImage image = InputImage.fromBitmap(ocrBitmap, 0);

            textRecognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String fullText = text.getText();
                        extractedNumbers = extractNumbers(fullText);

                        if (extractedNumbers.isEmpty()) {
                            hideStatus();
                            Toast.makeText(this, R.string.error_no_numbers, Toast.LENGTH_LONG).show();
                        } else {
                            displayResults(extractedNumbers);
                            saveToDatabase(bitmap, extractedNumbers);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "OCR failed: " + e.getMessage());
                        hideStatus();
                        Toast.makeText(this, R.string.msg_ocr_processing_failed, Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Image processing error: " + e.getMessage());
            hideStatus();
        }
    }

    private String extractNumbers(String text) {
        if (text == null) return "";

        // Normalize common OCR confusions (handwriting/low contrast)
        String normalized = text
                .replace('O', '0').replace('o', '0')
                .replace('I', '1').replace('l', '1').replace('|', '1')
                .replace('S', '5').replace('s', '5')
                .replace('B', '8');

        // Keep any digit sequences (including single digits), then de-dupe consecutive repeats.
        StringBuilder numbers = new StringBuilder();
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(normalized);

        String last = null;
        while (matcher.find()) {
            String token = matcher.group();
            if (token.equals(last)) continue;
            last = token;

            if (numbers.length() > 0) numbers.append("\n");
            numbers.append(token);
        }

        return numbers.toString().trim();
    }

    private Bitmap preprocessForOcr(Bitmap src) {
        // Downscale very large images for faster + more stable OCR.
        int maxDim = 1600;
        int w = src.getWidth();
        int h = src.getHeight();
        float scale = Math.min(1f, (float) maxDim / (float) Math.max(w, h));
        Bitmap scaled = src;
        if (scale < 1f) {
            scaled = Bitmap.createScaledBitmap(src, Math.round(w * scale), Math.round(h * scale), true);
        }

        // Grayscale + contrast boost helps handwritten digits.
        Bitmap out = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0f);

        // Contrast: 1.35x with small brightness lift
        float contrast = 1.35f;
        float brightness = 12f;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[]{
                contrast, 0, 0, 0, brightness,
                0, contrast, 0, 0, brightness,
                0, 0, contrast, 0, brightness,
                0, 0, 0, 1, 0
        });
        cm.postConcat(contrastMatrix);

        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(scaled, 0, 0, paint);

        return out;
    }

    private void displayResults(String numbers) {
        hideStatus();
        binding.extractedNumbers.setText(numbers);
        binding.resultsCard.setVisibility(View.VISIBLE);
    }

    private void saveToDatabase(Bitmap image, String numbers) {
        try {
            if (db == null) return;

            long now = System.currentTimeMillis();
            String imageFileName = (currentPhotoFile != null) ? currentPhotoFile.getName() : null;

            OcrRecordEntity record = new OcrRecordEntity();
            record.timestamp = now;
            record.numbers = numbers;
            record.imagePath = imageFileName;
            record.pdfPath = null;

            dbExecutor.execute(() -> {
                try {
                    long id = db.ocrRecordDao().insert(record);
                    latestRecordId = id;
                    runOnUiThread(() ->
                            Toast.makeText(this, R.string.success_saved, Toast.LENGTH_SHORT).show()
                    );
                    refreshHistory();


                } catch (Exception e) {
                    Log.e(TAG, "Database save failed: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Database save failed: " + e.getMessage());
        }
    }

    private void generatePDF() {
        if (extractedNumbers.isEmpty()) {
            Toast.makeText(this, R.string.msg_no_data_to_generate_pdf, Toast.LENGTH_SHORT).show();
            return;
        }

        showStatus("Generating PDF...");

        new Thread(() -> {
            try {
                File pdfDir = new File(getExternalFilesDir(null), "pdfs");
                if (!pdfDir.exists()) pdfDir.mkdirs();

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File pdfFile = new File(pdfDir, "OCR_" + timeStamp + ".pdf");

                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
                document.open();

                // Add title
                document.add(new Paragraph("OCR Scan Results"));
                document.add(new Paragraph("Date: " + new Date().toString()));
                document.add(new Paragraph("\n"));

                // Add extracted numbers
                document.add(new Paragraph("Extracted Numbers:"));
                document.add(new Paragraph(extractedNumbers));
                document.add(new Paragraph("\n"));

                // Add image if available
                if (currentPhotoFile != null && currentPhotoFile.exists()) {
                    com.itextpdf.text.Image pdfImage = com.itextpdf.text.Image.getInstance(currentPhotoFile.getAbsolutePath());
                    pdfImage.scaleToFit(400, 400);
                    document.add(pdfImage);
                }

                document.close();

                // Update DB record with PDF path
                if (db != null) {
                    long recordId = latestRecordId;
                    String pdfFileName = pdfFile.getName();
                    dbExecutor.execute(() -> {
                        try {
                            if (recordId > 0) {
                                db.ocrRecordDao().updatePdfPath(recordId, pdfFileName);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Database update failed: " + e.getMessage());
                        }
                    });
                }

                runOnUiThread(() -> {
                    hideStatus();
                    Toast.makeText(this, R.string.pdf_generated, Toast.LENGTH_SHORT).show();
                    sharePDF(pdfFile);
                    refreshHistory();
                });

            } catch (DocumentException | IOException e) {
                Log.e(TAG, "PDF generation failed: " + e.getMessage());
                runOnUiThread(() -> {
                    hideStatus();
                    Toast.makeText(this, R.string.msg_pdf_generation_failed, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void sharePDF(File pdfFile) {
        Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "OCR Scan Results");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Extracted numbers:\n" + extractedNumbers);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share PDF"));
    }

    private void copyToClipboard() {
        if (extractedNumbers.isEmpty()) return;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Extracted Numbers", extractedNumbers);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    private void showStatus(String message) {
        binding.statusText.setText(message);
        binding.statusCard.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        binding.statusCard.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}