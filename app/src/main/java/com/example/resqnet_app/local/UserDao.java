//package com.example.resqnet_app.local;
//
//import androidx.room.Dao;
//import androidx.room.Insert;
//import androidx.room.Query;
//import androidx.room.Update;
//import java.util.List;
//
//@Dao
//public interface UserDao {
//
//    @Insert
//    void insert(User user);
//
//    @Update
//    void update(User user);
//
//    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
//    User getUserByEmail(String email);
//
//    // 🔹 Get users not yet synced to Firestore
//    @Query("SELECT * FROM users WHERE isSynced = 0")
//    List<User> getUnsyncedUsers();
//}
