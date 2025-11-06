package com.example.resqnet_app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sos_alerts")
public class SosAlert {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;         // e.g., "SOS Alert!" or "Received SOS"
    public String description;   // Message content
    public String location;      // User location or "Nearby Device"
    public String date;          // YYYY-MM-DD
    public String time;          // HH:mm:ss
    public String uid;           // "currentUserUID" or "remoteUser"
    public long createdAt;       // timestamp in millis
}
