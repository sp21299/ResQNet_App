package com.example.resqnet_app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.resqnet_app.data.local.entity.SosAlert;

import java.util.List;

@Dao
public interface SosAlertDao {

    // Insert a new SOS alert
    @Insert
    void insert(SosAlert alert);

    // Get all alerts ordered by time descending (LiveData for automatic updates)
    @Query("SELECT * FROM sos_alerts ORDER BY date DESC, time DESC")
    LiveData<List<SosAlert>> getAllAlertsLive();

    // Get all alerts manually
    @Query("SELECT * FROM sos_alerts ORDER BY date DESC, time DESC")
    List<SosAlert> getAllAlerts();

    // Update the status of an alert by id
    @Query("UPDATE sos_alerts SET status = :status WHERE id = :id")
    void updateStatus(int id, String status);

    // Update location, latitude, and longitude of an alert by id
    @Query("UPDATE sos_alerts SET latitude = :latitude, longitude = :longitude WHERE id = :id")
    void updateLocation(int id, double latitude, double longitude);

    // Update full SOS alert (all fields)
    @Update
    void update(SosAlert alert);

    // Delete all alerts
    @Query("DELETE FROM sos_alerts")
    void deleteAll();

    // Get alerts by status
    @Query("SELECT * FROM sos_alerts WHERE status = :status ORDER BY date DESC, time DESC")
    List<SosAlert> getAlertsByStatus(String status);

    // Optional: Get alert by ID
    @Query("SELECT * FROM sos_alerts WHERE id = :id")
    SosAlert getAlertById(int id);
}
