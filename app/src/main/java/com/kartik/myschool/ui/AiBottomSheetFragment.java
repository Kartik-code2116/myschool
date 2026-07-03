package com.kartik.myschool.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.HorizontalScrollView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.kartik.myschool.R;
import com.kartik.myschool.ai.AiAgentManager;

import java.util.ArrayList;
import java.util.List;

public class AiBottomSheetFragment extends BottomSheetDialogFragment {

    private RecyclerView rvChat;
    private EditText etMessage;
    private FrameLayout btnSend;
    private LinearLayout llTypingIndicator;
    private TextView tvTypingText;
    
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private LinearLayout llHeader;
    private View dragHandle;
    private ImageView btnClose;
    private BottomSheetBehavior<?> behavior;
    
    private HorizontalScrollView hsvSuggestions;
    private TextView chipAddStudent, chipAttendance, chipReport, chipPrint;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // We will initialize behavior in onStart where the view is fully attached
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // Set initial height to 65% of screen height
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
                lp.height = (int) (screenHeight * 0.65);
                bottomSheet.setLayoutParams(lp);

                behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
            if (d.getWindow() != null) {
                d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Reset the agent so it starts with a fresh Marathi context each time the sheet is opened
        AiAgentManager.reset();
        return inflater.inflate(R.layout.fragment_ai_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChat = view.findViewById(R.id.rv_chat);
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send);
        llTypingIndicator = view.findViewById(R.id.ll_typing_indicator);
        tvTypingText = view.findViewById(R.id.tv_typing_text);
        llHeader = view.findViewById(R.id.ll_header);
        dragHandle = view.findViewById(R.id.drag_handle);
        btnClose = view.findViewById(R.id.btn_close);
        
        btnClose.setOnClickListener(v -> dismiss());

        hsvSuggestions = view.findViewById(R.id.hsv_suggestions);
        chipAddStudent = view.findViewById(R.id.chip_add_student);
        chipAttendance = view.findViewById(R.id.chip_attendance);
        chipReport = view.findViewById(R.id.chip_report);
        chipPrint = view.findViewById(R.id.chip_print);

        setupSuggestionClick(chipAddStudent, "विद्यार्थी कसा जोडावा? 👤");
        setupSuggestionClick(chipAttendance, "हजेरी कशी भरावी? 📝");
        setupSuggestionClick(chipReport, "गुणपत्रक रिपोर्ट कसा काढावा? 📊");
        setupSuggestionClick(chipPrint, "माहिती प्रिंट कशी करावी? 🖨️");

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);

        // Add intro message
        chatMessages.add(new ChatMessage("नमस्कार! मी तुमचा MySchool AI सहाय्यक आहे. तुम्हाला कशी मदत करू? 🙏", true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void setupSuggestionClick(TextView chip, String query) {
        chip.setOnClickListener(v -> {
            // Trim any emojis from search query before sending to AI to keep it clean, 
            // but the UI text is fine. Actually, sending with emoji is also fine.
            etMessage.setText(query);
            sendMessage();
        });
    }

    private void sendMessage() {
        String msg = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) return;

        etMessage.setText("");
        
        // Expand to full screen when interacting
        if (behavior != null) {
            Dialog dialog = getDialog();
            if (dialog instanceof BottomSheetDialog) {
                BottomSheetDialog d = (BottomSheetDialog) dialog;
                FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    bottomSheet.setLayoutParams(lp);
                }
            }
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            llHeader.setVisibility(View.VISIBLE);
            dragHandle.setVisibility(View.GONE);
            hsvSuggestions.setVisibility(View.GONE);
            View root = getView();
            if (root != null) {
                root.setBackgroundResource(R.color.background);
            }
        }
        
        // Add user message
        chatMessages.add(new ChatMessage(msg, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChat.scrollToPosition(chatMessages.size() - 1);
        
        llTypingIndicator.setVisibility(View.VISIBLE);
        rvChat.scrollToPosition(chatMessages.size() - 1);

        AiAgentManager.getInstance().sendMessage(msg, new AiAgentManager.OnMessageCallback() {
            @Override
            public void onSuccess(String response) {
                if (!isAdded()) return;
                llTypingIndicator.setVisibility(View.GONE);
                if (tvTypingText != null) tvTypingText.setText("AI is typing...");
                chatMessages.add(new ChatMessage(response, true));
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                rvChat.scrollToPosition(chatMessages.size() - 1);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                llTypingIndicator.setVisibility(View.GONE);
                if (tvTypingText != null) tvTypingText.setText("AI is typing...");
                // Show error as a chat bubble instead of a Toast
                chatMessages.add(new ChatMessage("⚠️ " + error, true));
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                rvChat.scrollToPosition(chatMessages.size() - 1);
            }

            @Override
            public void onRetrying(String message) {
                if (!isAdded()) return;
                // Keep typing indicator visible but update text with countdown
                llTypingIndicator.setVisibility(View.VISIBLE);
                if (tvTypingText != null) tvTypingText.setText(message);
            }
        });
    }

    // --- Adapter and Model ---

    private static class ChatMessage {
        String text;
        boolean isAi;

        ChatMessage(String text, boolean isAi) {
            this.text = text;
            this.isAi = isAi;
        }
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_ai, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            if (msg.isAi) {
                holder.llAiMsg.setVisibility(View.VISIBLE);
                holder.llUserMsg.setVisibility(View.GONE);
                holder.tvAiText.setText(msg.text);
            } else {
                holder.llAiMsg.setVisibility(View.GONE);
                holder.llUserMsg.setVisibility(View.VISIBLE);
                holder.tvUserText.setText(msg.text);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            LinearLayout llAiMsg, llUserMsg;
            TextView tvAiText, tvUserText;

            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                llAiMsg = itemView.findViewById(R.id.ll_ai_msg);
                llUserMsg = itemView.findViewById(R.id.ll_user_msg);
                tvAiText = itemView.findViewById(R.id.tv_ai_text);
                tvUserText = itemView.findViewById(R.id.tv_user_text);
            }
        }
    }
}
