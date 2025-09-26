package com.example.resqnet_app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AlertDao {

    @Insert
    void insert(Alert alert);

    @Query("SELECT * FROM alerts ORDER BY id DESC")
    List<Alert> getAllAlerts();

    @Query("DELETE FROM alerts")
    void deleteAll();
}
