package com.example.resqnet_app.model;

public class AlertModel {
    private String title;
    private String message;
    private String timestamp;

    public AlertModel(String title, String message, String timestamp) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
