package com.example.resqnet_app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.resqnet_app.data.local.entity.Alert;

import java.util.List;

@Dao
public interface AlertDao {

    // Insert alert into Room. If ID already exists, replace it
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Alert alert);

    // Get all alerts ordered by createdAt descending
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    List<Alert> getAll();

    // Optional: get alerts by uid
    @Query("SELECT * FROM alerts WHERE uid = :uid ORDER BY createdAt DESC")
    List<Alert> getAlertsByUid(String uid);

    // Optional: delete all alerts
    @Query("DELETE FROM alerts")
    void deleteAll();
}
