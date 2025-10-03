package com.example.resqnet_app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.resqnet_app.data.local.entity.Alert;
import java.util.List;

@Dao
public interface AlertDao {
    @Insert
    void insert(Alert alert);

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    List<Alert> getAll();
}
