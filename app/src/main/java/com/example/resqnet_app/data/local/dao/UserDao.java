package com.example.resqnet_app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.resqnet_app.data.local.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void insert(UserEntity user);
    @Update
    void updateUser(UserEntity user);
    @Delete
    void deleteUser(UserEntity user);
    @Query("SELECT * FROM users")
    List<UserEntity> getAllUsers();
    @Query("SELECT * FROM users WHERE id = :id")
    UserEntity getUserById(int id);
}
