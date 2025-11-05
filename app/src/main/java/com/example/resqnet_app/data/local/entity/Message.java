package com.example.resqnet_app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Message {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String senderName;
    private String content;
    private long timestamp;
    private boolean isSentByMe;

    // Default constructor required by Room
    public Message() {}

    // Constructor for creating a new message
    public Message(String senderName, String content, long timestamp, boolean isSentByMe) {
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSentByMe() {
        return isSentByMe;
    }

    public void setSentByMe(boolean sentByMe) {
        isSentByMe = sentByMe;
    }

    public String getText() {
        return null;
    }
}
