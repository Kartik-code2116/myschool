package com.kartik.myschool.model;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;

    private String text;
    private int type; // 0 for User, 1 for AI
    private boolean isError;

    public ChatMessage(String text, int type) {
        this.text = text;
        this.type = type;
        this.isError = false;
    }
    
    public ChatMessage(String text, int type, boolean isError) {
        this.text = text;
        this.type = type;
        this.isError = isError;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getType() {
        return type;
    }

    public boolean isError() {
        return isError;
    }
}
