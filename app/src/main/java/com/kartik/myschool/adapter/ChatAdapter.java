package com.kartik.myschool.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.myschool.R;
import com.kartik.myschool.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void updateLastMessage(String newText) {
        if (!messages.isEmpty()) {
            messages.get(messages.size() - 1).setText(newText);
            notifyItemChanged(messages.size() - 1);
        }
    }
    
    public void removeLastMessage() {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            messages.remove(lastIndex);
            notifyItemRemoved(lastIndex);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_ai, parent, false);
            return new AiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AiMessageViewHolder) {
            ((AiMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageBody);
        }
        public void bind(ChatMessage message) {
            tvMessage.setText(message.getText());
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        android.content.res.ColorStateList defaultTextColor;
        public AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageBody);
            defaultTextColor = tvMessage.getTextColors();
        }
        public void bind(ChatMessage message) {
            tvMessage.setText(message.getText());
            if (message.isError()) {
                tvMessage.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvMessage.setTextColor(defaultTextColor);
            }
        }
    }
}
