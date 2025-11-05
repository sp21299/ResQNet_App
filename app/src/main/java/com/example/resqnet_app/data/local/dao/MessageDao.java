package com.example.resqnet_app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.resqnet_app.data.local.entity.Message;

import java.util.List;
@Dao
public interface MessageDao {

    @Insert
    void insert(Message message);

    @Query("SELECT * FROM message ORDER BY timestamp ASC")
    LiveData<List<Message>> getAllMessages();

    // Delete messages older than 1 day
    @Query("DELETE FROM Message WHERE timestamp < :timeLimit")
    void deleteOldMessages(long timeLimit);

    // Get unsent messages (sentByMe = true but not sent yet)
    @Query("SELECT * FROM Message WHERE isSentByMe = 1")
    List<Message> getUnsentMessages();

    // Get all messages synchronously (for sending previous messages in background)
    @Query("SELECT * FROM Message ORDER BY timestamp ASC")
    List<Message> getAllMessagesSync();
}
