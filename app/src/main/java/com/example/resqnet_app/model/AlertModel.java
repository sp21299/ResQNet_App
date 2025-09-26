package com.example.resqnet_app.model;

import com.example.resqnet_app.data.Alert;

import java.util.List;

public class AlertModel {
    private String title;
    private String message;
    private String timestamp;

    public AlertModel(String title, String message, String timestamp) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static List<Alert> toAlertList(List<AlertModel> alertModels) {
        return alertModels.stream()
                .map(alertModel -> new Alert(alertModel.getTitle(), alertModel.getMessage(), alertModel.getTimestamp()))
                .toList();
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
