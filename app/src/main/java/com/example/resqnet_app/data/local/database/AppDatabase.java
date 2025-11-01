package com.example.resqnet_app.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.resqnet_app.data.local.dao.UserDao;
import com.example.resqnet_app.data.local.entity.User;

// Add your entity (User)
@Database(entities = {User.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    // DAO reference
    public abstract UserDao userDao();

    // Singleton pattern for single DB instance
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "resqnet_db"
                            )
                            // Optional: If schema changes, deletes old DB to avoid crash
                            .fallbackToDestructiveMigration()
                            // Optional: allow main thread queries (for quick testing)
                            // .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
