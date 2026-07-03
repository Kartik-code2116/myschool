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
    private ImageView btnClose;
    private LinearLayout llTypingIndicator;
    
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChat = view.findViewById(R.id.rv_chat);
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send);
        btnClose = view.findViewById(R.id.btn_close);
        llTypingIndicator = view.findViewById(R.id.ll_typing_indicator);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);

        // Add intro message
        chatMessages.add(new ChatMessage("Hello! I am your MySchool AI Assistant. How can I help you today?", true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);

        btnClose.setOnClickListener(v -> dismiss());

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String msg = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) return;

        etMessage.setText("");
        
        // Add user message
        chatMessages.add(new ChatMessage(msg, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChat.scrollToPosition(chatMessages.size() - 1);
        
        llTypingIndicator.setVisibility(View.VISIBLE);
        rvChat.scrollToPosition(chatMessages.size() - 1);

        AiAgentManager.getInstance().sendMessage(msg, new AiAgentManager.OnMessageCallback() {
            @Override
            public void onSuccess(String response) {
                if (getContext() == null) return;
                llTypingIndicator.setVisibility(View.GONE);
                chatMessages.add(new ChatMessage(response, true));
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                rvChat.scrollToPosition(chatMessages.size() - 1);
            }

            @Override
            public void onError(String error) {
                if (getContext() == null) return;
                llTypingIndicator.setVisibility(View.GONE);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
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
