package com.kartik.myschool.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.kartik.myschool.R;
import com.kartik.myschool.adapter.ChatAdapter;
import com.kartik.myschool.model.ChatMessage;

// We try to import the Firebase AI classes based on standard naming
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiAssistantActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private ChatAdapter chatAdapter;

    private GenerativeModelFutures chatModel;
    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);

        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);

        // Initialize Gemini Developer API
        try {
            GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                    .generativeModel("gemini-2.0-flash"); // Updated to latest model
            chatModel = GenerativeModelFutures.from(ai);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to initialize AI: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Add a greeting message
        chatAdapter.addMessage(new ChatMessage("Hello! I am your AI Assistant. How can I help you today?", ChatMessage.TYPE_AI));

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String query = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            return;
        }

        etMessage.setText("");
        
        // Add User Message
        chatAdapter.addMessage(new ChatMessage(query, ChatMessage.TYPE_USER));
        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        if (chatModel == null) {
            chatAdapter.addMessage(new ChatMessage("Error: AI Model is not initialized.", ChatMessage.TYPE_AI, true));
            return;
        }

        // Add loading placeholder for AI
        chatAdapter.addMessage(new ChatMessage("Thinking...", ChatMessage.TYPE_AI));
        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        
        btnSend.setEnabled(false);

        try {
            Content prompt = new Content.Builder()
                    .addText(query)
                    .build();

            ListenableFuture<GenerateContentResponse> response = chatModel.generateContent(prompt);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        String resultText = result.getText();
                        chatAdapter.updateLastMessage(resultText != null ? resultText : "I couldn't generate a response.");
                        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        chatAdapter.updateLastMessage("Error: " + t.getMessage());
                        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    });
                }
            }, executor);

        } catch (Exception e) {
            btnSend.setEnabled(true);
            chatAdapter.updateLastMessage("Error generating content: " + e.getMessage());
        }
    }
}
