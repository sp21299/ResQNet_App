package com.example.resqnet_app.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alerts")
public class Alert {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "type")
    public String type;   // "SMS", "CALL", "ADMIN_ALERT", "BUZZER"

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "status")
    public String status; // "sent", "failed", "pending_sync"
}
