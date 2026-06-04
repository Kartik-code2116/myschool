package com.kartik.myschool.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrHelper {

    public interface OcrCallback {
        void onResult(List<String> numbers, String rawText);
        void onError(Exception e);
    }

    private final TextRecognizer recognizer;

    public OcrHelper() {
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void processImage(Bitmap bitmap, OcrCallback callback) {
        Bitmap processed = preprocess(bitmap);
        InputImage image = InputImage.fromBitmap(processed, 0);
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    String raw = result.getText();
                    callback.onResult(extractNumbers(raw), raw);
                })
                .addOnFailureListener(callback::onError);
    }

    // Returns list of number strings found in the text
    public static List<String> extractNumbers(String text) {
        List<String> numbers = new ArrayList<>();
        if (text == null) return numbers;
        String normalized = text
                .replace('O', '0').replace('o', '0')
                .replace('I', '1').replace('l', '1')
                .replace('S', '5').replace('B', '8');
        Pattern p = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher m = p.matcher(normalized);
        while (m.find()) numbers.add(m.group());
        return numbers;
    }

    public static Bitmap preprocess(Bitmap src) {
        int maxDim = 1600;
        float scale = Math.min(1f, (float) maxDim / Math.max(src.getWidth(), src.getHeight()));
        Bitmap scaled = scale < 1f
                ? Bitmap.createScaledBitmap(src, Math.round(src.getWidth() * scale),
                Math.round(src.getHeight() * scale), true)
                : src;
        Bitmap out = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0f);
        float c = 1.35f, b = 12f;
        cm.postConcat(new ColorMatrix(new float[]{c,0,0,0,b, 0,c,0,0,b, 0,0,c,0,b, 0,0,0,1,0}));
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(scaled, 0, 0, paint);
        return out;
    }

    public void close() {
        recognizer.close();
    }
}
