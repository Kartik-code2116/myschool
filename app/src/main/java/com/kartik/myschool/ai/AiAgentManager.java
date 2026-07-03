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

    // gemini-2.0-flash is fast, stable and has no thinking-token limits causing 503
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    // gemini-2.0-flash-lite has a higher free-tier quota (30 RPM vs 15 RPM)
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=" + API_KEY;
    private static final int MAX_RETRIES = 3;

    // Detailed knowledge about the MySchool app
    private static final String APP_KNOWLEDGE =
        "Here is detailed knowledge about the MySchool app you are an agent for:\n\n" +
        "## MySchool App Features\n" +
        "**MySchool** is an Android app for Indian school teachers to manage their classwork digitally.\n\n" +
        "### Core Features:\n" +
        "1. **Student Management** - Add, edit, delete students with photos. Assign them to a class.\n" +
        "2. **Attendance Tracking** - Mark daily attendance for each student. View attendance history per class.\n" +
        "3. **Marks/Grades Entry** - Enter exam marks for students across subjects and semesters.\n" +
        "4. **Report Generation** - Generate PDF progress reports for students to share with parents.\n" +
        "5. **Class Management** - Create and manage multiple classes and academic years/semesters.\n" +
        "6. **QR Code / Parent Linking** - Share a QR code link so parents can track their child's progress.\n" +
        "7. **School Profile** - Set up the school name, UDISE code, and teacher profile.\n" +
        "8. **Subscription** - The app has free and premium subscription plans for full access.\n" +
        "9. **AI Assistant (You!)** - This AI chat assistant to help teachers with school-related queries.\n\n" +
        "### Navigation:\n" +
        "- The bottom navigation bar has: Home, Students, Attendance, Marks, and AI Assistant tabs.\n" +
        "- The Home screen shows a dashboard with quick stats.\n\n" +
        "### How to use key features:\n" +
        "- **Add a student**: Go to Students tab → tap the + button → fill in name and details.\n" +
        "- **Mark attendance**: Go to Attendance tab → select class and date → mark each student.\n" +
        "- **Enter marks**: Go to Marks tab → select class and subject → enter marks for each student.\n" +
        "- **Generate report**: Open a student profile → tap 'Generate Report' button.\n" +
        "- **Link parent**: Open a student profile → tap 'Parent Link' → share the QR code with parent.\n\n" +
        "Always answer questions about this app helpfully and accurately. If a teacher asks how to do something in the app, guide them step by step.";

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

    private void initContext() {
        FirebaseRepository.get().getTeacher(new FirebaseRepository.OnResult<com.kartik.myschool.model.Teacher>() {
            @Override
            public void onSuccess(com.kartik.myschool.model.Teacher teacher) {
                String teacherInfo = (teacher != null)
                    ? "You are assisting " + teacher.name + " who teaches at " + teacher.schoolName + "."
                    : "You are assisting a school teacher.";

                String systemPrompt =
                    "You are MySchool AI Agent, a smart and helpful assistant built into the MySchool Android app. " +
                    teacherInfo + "\n\n" +
                    APP_KNOWLEDGE + "\n\n" +
                    "Rules:\n" +
                    "- Always be friendly, concise, and helpful.\n" +
                    "- If asked about app features, answer accurately using the knowledge above.\n" +
                    "- You can also help with general teaching and education questions.\n" +
                    "- Format responses clearly using bullet points where needed.";

                addMessageToHistory("user", systemPrompt + "\n\nUnderstood?");
                addMessageToHistory("model", "Understood! I am the MySchool AI Agent, ready to help you manage your school and answer any questions about the app. How can I assist you today?");
                contextInitialized = true;
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to get teacher context", e);
                // Still initialize with generic context
                addMessageToHistory("user", "You are MySchool AI Agent, a helpful assistant for school teachers using the MySchool app.\n\n" + APP_KNOWLEDGE + "\n\nUnderstood?");
                addMessageToHistory("model", "Understood! I am ready to help. How can I assist you today?");
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
        addMessageToHistory("user", userMessage);
        sendWithRetry(callback, 0);
    }

    private void sendWithRetry(OnMessageCallback callback, int retryCount) {
        JsonObject payload = new JsonObject();
        payload.add("contents", history);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", 1024);
        generationConfig.addProperty("temperature", 0.7);
        payload.add("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(
                gson.toJson(payload),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .header("Content-Type", "application/json")
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
                // Handle 429 Rate Limit: auto-retry with exponential backoff
                if (response.code() == 429) {
                    if (retryCount < MAX_RETRIES) {
                        long delayMs = (long) Math.pow(2, retryCount) * 2000; // 2s, 4s, 8s
                        Log.w(TAG, "Rate limited (429). Retrying in " + delayMs + "ms (attempt " + (retryCount + 1) + ")");
                        new Handler(Looper.getMainLooper()).postDelayed(() -> sendWithRetry(callback, retryCount + 1), delayMs);
                    } else {
                        if (history.size() > 0) history.remove(history.size() - 1);
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("Too many requests. Please wait a moment and try again."));
                    }
                    return;
                }

                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API Error " + response.code() + ": " + err);
                    if (history.size() > 0) history.remove(history.size() - 1);
                    String userMsg = response.code() == 503
                        ? "Server is busy, please try again in a moment."
                        : "Something went wrong. Please try again.";
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
    }
}
