package com.example.resqnet_app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.resqnet_app.data.local.entity.SosAlert;

import java.util.List;

@Dao
public interface SosAlertDao {

    @Insert
    void insert(SosAlert alert);

    @Query("SELECT * FROM sos_alerts ORDER BY createdAt DESC")
    List<SosAlert> getAll();

    @Query("DELETE FROM sos_alerts")
    void deleteAll();
}
