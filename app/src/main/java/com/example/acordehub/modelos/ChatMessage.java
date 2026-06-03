package com.example.acordehub.modelos;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class ChatMessage {

    private String id;
    private String senderId;
    private String text;

    @ServerTimestamp
    private Date createdAt;

    public ChatMessage() {}

    public ChatMessage(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
