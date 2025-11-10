package com.example.resqnet_app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sos_alerts")
public class SosAlert {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public String date;
    public String timestamp;

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isHelping = false;
    public boolean isAcknowledged = false;

    // Getters and Setters
    public boolean isHelping() { return isHelping; }
    public void setHelping(boolean helping) { isHelping = helping; }

    public boolean isAcknowledged() { return isAcknowledged; }
    public void setAcknowledged(boolean acknowledged) { isAcknowledged = acknowledged; }

    public String status; // active, acknowledged, etc.

    public double latitude;
    public double longitude;

    public String uuid;

    public boolean isSynced; // false = not yet uploaded to Firestore

    // Getters and Setters
    public int getId() { return id; }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }


    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // For adapter display
    public String getLocationText() {
        if (latitude != 0 && longitude != 0) return "View Location";
        return "Location not available";
    }

    public String getTimestamp() {
        return timestamp;
    }
}
