package com.example.resqnet_app.data.local.database;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.resqnet_app.data.local.dao.UserDao;
import com.example.resqnet_app.data.local.entity.UserEntity;

@Database(entities = {UserEntity.class},version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static  AppDatabase instance;
    public abstract UserDao userDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "resqnet_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

}
