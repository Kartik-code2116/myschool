package com.kartik.myschool.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kartik.myschool.BuildConfig;
import com.kartik.myschool.repository.FirebaseRepository;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiAgentManager {

    private static final String TAG = "AiAgentManager";
    private static AiAgentManager instance;
    private final OkHttpClient client;
    private final Gson gson;
    private JsonArray history;
    private boolean contextInitialized = false;

    // gemini-2.0-flash-lite has a higher free-tier quota (30 RPM vs 15 RPM)
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=" + API_KEY;
    private static final int MAX_RETRIES = 4;
    // Keep only last 10 messages (5 pairs) to avoid sending too many tokens per request
    private static final int MAX_HISTORY_MESSAGES = 10;
    // Retry delays: 15s, 30s, 60s, 90s - Google rate limit window is 60 seconds
    private static final long[] RETRY_DELAYS_MS = {15000, 30000, 60000, 90000};

    // App knowledge about MySchool (concise to save tokens)
    private static final String APP_KNOWLEDGE =
        "MySchool app features for Indian school teachers:\n" +
        "1. Student Management - add/edit/delete students with photos\n" +
        "2. Attendance - mark daily attendance per class\n" +
        "3. Marks/Grades - enter exam marks per subject/semester\n" +
        "4. PDF Reports - generate student progress reports\n" +
        "5. Parent Linking - share QR code so parents track child progress\n" +
        "6. Class Management - manage classes, academic years, semesters\n" +
        "7. School Profile - set school name, UDISE code\n" +
        "8. Subscription - free and premium plans\n" +
        "Navigation: Home, Students, Attendance, Marks, AI Assistant (bottom bar).\n" +
        "To add student: Students tab → + button. Attendance: Attendance tab → select class/date.\n" +
        "Marks: Marks tab → select class/subject. Report: open student profile → Generate Report.\n" +
        "Parent link: student profile → Parent Link → share QR.";


    private AiAgentManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        history = new JsonArray();
        initContext();
    }

    public static synchronized AiAgentManager getInstance() {
        if (instance == null) {
            instance = new AiAgentManager();
        }
        return instance;
    }

    /** Reset the singleton so a fresh Marathi context is loaded next time. */
    public static synchronized void reset() {
        instance = null;
    }

    private void initContext() {
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
            @Override
            public void onSuccess(com.kartik.myschool.model.Teacher teacher) {
                String teacherInfo = (teacher != null)
                    ? "You are assisting " + teacher.name + " who teaches at " + teacher.schoolName + "."
                    : "You are assisting a school teacher.";

                String systemPrompt =
                    "तुम्ही MySchool AI Agent आहात - MySchool Android app मध्ये बनवलेले एक स्मार्ट आणि उपयुक्त सहाय्यक. " +
                    teacherInfo + "\n\n" +
                    APP_KNOWLEDGE + "\n\n" +
                    "नियम:\n" +
                    "- नेहमी मराठीत उत्तर द्या (Marathi language only).\n" +
                    "- शिक्षकाशी मैत्रीपूर्ण, स्पष्ट आणि मदतगार रहा.\n" +
                    "- App बद्दल प्रश्न असल्यास वरील माहिती वापरून अचूक उत्तर द्या.\n" +
                    "- शिक्षण व शाळेशी संबंधित सर्व प्रश्नांना मदत करा.\n" +
                    "- उत्तरे स्पष्ट व मुद्देसूद ठेवा.";

                addMessageToHistory("user", systemPrompt + "\n\nसमजले का?");
                addMessageToHistory("model", "हो, समजले! मी MySchool AI Agent आहे. तुमच्या शाळेच्या कामात आणि या App बद्दल कोणत्याही प्रश्नासाठी मी मराठीत मदत करण्यास तयार आहे. आज मी तुम्हाला कशी मदत करू?");
                contextInitialized = true;
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to get teacher context", e);
                // Still initialize with generic context in Marathi
                addMessageToHistory("user", "तुम्ही MySchool AI Agent आहात - शाळेतील शिक्षकांसाठी मदतगार सहाय्यक. नेहमी मराठीत उत्तर द्या.\n\n" + APP_KNOWLEDGE + "\n\nसमजले का?");
                addMessageToHistory("model", "हो, समजले! मी तुमच्या मदतीसाठी तयार आहे. आज मी तुम्हाला कशी मदत करू?");
                contextInitialized = true;
            }
        });
    }

    private void addMessageToHistory(String role, String text) {
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("role", role);

        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);

        messageObj.add("parts", parts);
        history.add(messageObj);
    }

    public void sendMessage(String userMessage, OnMessageCallback callback) {
        String msgLower = userMessage.trim().toLowerCase();
        
        // Match common greetings even with punctuation
        if (msgLower.matches("^(hi|hello|hey|हाय|नमस्कार)[!?.]*$" )) {
            addMessageToHistory("user", userMessage);
            String reply = "नमस्कार! मी MySchool AI Agent आहे. आज मी तुम्हाला कशी मदत करू? 🙏";
            addMessageToHistory("model", reply);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (callback != null) callback.onSuccess(reply);
            }, 500);
            return;
        }

        // Intercept suggestion chip questions to respond offline
        String fixedReply = null;
        if (msgLower.contains("विद्यार्थी कसा जोडावा")) {
            fixedReply = "विद्यार्थी जोडण्यासाठी खालील पायऱ्या फॉलो करा:\n\n" +
                    "1. खालील मेनू बारमधील **'Students'** टॅबवर जा.\n" +
                    "2. खाली उजव्या कोपऱ्यात असलेल्या **'+' (Add)** बटणावर क्लिक करा.\n" +
                    "3. विद्यार्थ्याचे नाव, वर्ग, हजेरी क्रमांक (Roll No), पालकांचा फोन नंबर आणि इतर माहिती भरा.\n" +
                    "4. तुम्ही विद्यार्थ्याचा फोटो देखील जोडू शकता.\n" +
                    "5. शेवटी **'Save'** बटणावर क्लिक करा! 💾";
        } else if (msgLower.contains("हजेरी कशी भरावी")) {
            fixedReply = "हजेरी भरण्यासाठी खालील सोप्या पायऱ्या फॉलो करा:\n\n" +
                    "1. खालील मेनू बारमधील **'Attendance'** टॅबवर जा.\n" +
                    "2. तुमचा वर्ग आणि आजची तारीख निवडा.\n" +
                    "3. सर्व विद्यार्थ्यांची यादी दिसेल. प्रत्येक विद्यार्थ्याच्या नावासमोर हजर (Present) किंवा गैरहजर (Absent) टॅप करा.\n" +
                    "4. शेवटी खालील **'Save Attendance'** बटणावर क्लिक करा. ✅";
        } else if (msgLower.contains("गुणपत्रक रिपोर्ट")) {
            fixedReply = "गुणपत्रक (Report Card) रिपोर्ट तयार करण्यासाठी:\n\n" +
                    "1. खालील मेनू बारमधील **'Marks'** किंवा **'Reports'** टॅबवर जा.\n" +
                    "2. ज्या विद्यार्थ्याचा रिपोर्ट हवा आहे, त्याच्या प्रोफाइलवर क्लिक करा.\n" +
                    "3. तिथे तुम्हाला **'Generate Progress Report'** किंवा **'Print PDF'** चा पर्याय मिळेल.\n" +
                    "4. त्यावर क्लिक करून तुम्ही सुंदर रिपोर्ट पीडीएफ (PDF) मध्ये डाऊनलोड किंवा प्रिंट करू शकता. 📊";
        } else if (msgLower.contains("माहिती प्रिंट कशी करावी")) {
            fixedReply = "माहिती प्रिंट करण्यासाठी:\n\n" +
                    "1. तुम्हाला ज्या पानाची माहिती प्रिंट करायची आहे (उदा. विद्यार्थ्यांची यादी किंवा हजेरी) तिथे जा.\n" +
                    "2. वरच्या कोपऱ्यात किंवा प्रोफाइलमध्ये असलेल्या **'Print'** किंवा **'PDF'** आयकॉनवर क्लिक करा.\n" +
                    "3. ते तुम्हाला डायरेक्ट तुमच्या मोबाईलच्या प्रिंटर सेटिंग्जवर घेऊन जाईल, जिथून तुम्ही कागद किंवा पीडीएफ स्वरूपात फाईल सेव्ह करू शकता. 🖨️";
        }

        if (fixedReply != null) {
            addMessageToHistory("user", userMessage);
            addMessageToHistory("model", fixedReply);
            final String reply = fixedReply;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (callback != null) callback.onSuccess(reply);
            }, 600);
            return;
        }

        addMessageToHistory("user", userMessage);
        sendWithRetry(callback, 0);
    }

    private void sendWithRetry(OnMessageCallback callback, int retryCount) {
        // Build a trimmed view of history: keep first 2 messages (system context) + last N recent ones
        JsonArray trimmedHistory = new JsonArray();
        int systemMessages = Math.min(2, history.size()); // first 2 = system init pair
        for (int i = 0; i < systemMessages; i++) {
            trimmedHistory.add(history.get(i));
        }
        int recentStart = Math.max(systemMessages, history.size() - MAX_HISTORY_MESSAGES);
        for (int i = recentStart; i < history.size(); i++) {
            trimmedHistory.add(history.get(i));
        }

        JsonObject payload = new JsonObject();
        payload.add("contents", trimmedHistory);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", 512);
        generationConfig.addProperty("temperature", 0.7);
        payload.add("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(
                gson.toJson(payload),
                MediaType.parse("application/json")
        );

        // Remove key from URL if it's there
        String baseUrl = API_URL.split("\\?")[0];

        Request request = new Request.Builder()
                .url(baseUrl)
                .post(body)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API call failed", e);
                if (history.size() > 0) history.remove(history.size() - 1);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Network error: " + (e.getMessage() != null ? e.getMessage() : "Check your internet connection"))
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // Handle 429 Rate Limit: auto-retry with longer backoff
                if (response.code() == 429) {
                    try {
                        String errBody = response.body() != null ? response.body().string() : "";
                        // Daily quota exhausted: limit=0 means no retrying will help
                        boolean dailyExhausted = errBody.contains("limit: 0") || errBody.contains("PerDay");
                        if (dailyExhausted) {
                            if (history.size() > 0) history.remove(history.size() - 1);
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onError("Daily quota exhausted. Please try again tomorrow or use a different API key from Google AI Studio (ai.google.dev)."));
                            return;
                        }
                    } catch (Exception ignored) {}

                    if (retryCount < MAX_RETRIES) {
                        long delayMs = RETRY_DELAYS_MS[retryCount];
                        int waitSecs = (int)(delayMs / 1000);
                        Log.w(TAG, "Rate limited (429). Retrying in " + waitSecs + "s (attempt " + (retryCount + 1) + ")");
                        // Notify UI to show countdown
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onRetrying("Rate limited. Retrying in " + waitSecs + "s..."));
                        new Handler(Looper.getMainLooper()).postDelayed(() -> sendWithRetry(callback, retryCount + 1), delayMs);
                    } else {
                        if (history.size() > 0) history.remove(history.size() - 1);
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("Still rate limited. Please wait 1-2 minutes then try again."));
                    }
                    return;
                }

                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API Error " + response.code() + ": " + err);
                    if (history.size() > 0) history.remove(history.size() - 1);
                    
                    String userMsg;
                    if (response.code() == 401) {
                        userMsg = "Invalid API Key. Please check your Gemini API key.";
                    } else if (response.code() == 400) {
                        userMsg = "Bad Request. The API key might be malformed or invalid.";
                    } else if (response.code() == 503) {
                        userMsg = "Server is busy, please try again in a moment.";
                    } else {
                        userMsg = "Something went wrong. Please try again.";
                    }
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(userMsg));
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

                    JsonArray candidates = jsonObject.getAsJsonArray("candidates");
                    if (candidates != null && candidates.size() > 0) {
                        JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                        String role = content.get("role").getAsString();
                        JsonArray parts = content.getAsJsonArray("parts");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < parts.size(); i++) {
                            JsonObject p = parts.get(i).getAsJsonObject();
                            if (p.has("text") && !p.has("thought")) {
                                sb.append(p.get("text").getAsString());
                            }
                        }
                        String responseText = sb.toString().trim();
                        if (responseText.isEmpty()) {
                            responseText = parts.get(parts.size() - 1).getAsJsonObject().get("text").getAsString();
                        }

                        addMessageToHistory(role, responseText);
                        final String finalText = responseText;
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(finalText));
                    } else {
                        if (history.size() > 0) history.remove(history.size() - 1);
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("Empty response from AI. Please try again."));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parsing error", e);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to parse response. Please try again."));
                }
            }
        });
    }

    public interface OnMessageCallback {
        void onSuccess(String response);
        void onError(String error);
        /** Called when a 429 rate limit triggers a retry so the UI can show a countdown */
        default void onRetrying(String message) { /* optional override */ }
    }
}
