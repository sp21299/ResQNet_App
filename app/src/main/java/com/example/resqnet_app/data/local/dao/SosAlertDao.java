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

    @Insert
    void insert(SosAlert alert);

    @Query("SELECT * FROM sos_alerts ORDER BY createdAt DESC")
    LiveData<List<SosAlert>> getAllAlertsLive();  // 🔥 Live updates

    @Query("SELECT * FROM sos_alerts ORDER BY createdAt DESC")
    List<SosAlert> getAllAlerts(); // Optional (for manual loading)

    @Query("UPDATE sos_alerts SET status = :status WHERE id = :id")
    void updateStatus(int id, String status);

    @Query("DELETE FROM sos_alerts")
    void deleteAll();

    @Query("SELECT * FROM sos_alerts ORDER BY createdAt DESC")
    List<SosAlert> getAllSosAlerts();



}
