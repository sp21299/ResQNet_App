package com.example.resqnet_app.data.local.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.resqnet_app.data.local.dao.MessageDao;
import com.example.resqnet_app.data.local.dao.UserDao;
import com.example.resqnet_app.data.local.entity.Message;
import com.example.resqnet_app.data.local.entity.User;

@Database(
        entities = {User.class, Message.class},
        version = 5, // 🔹 Incremented version number
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract MessageDao messageDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "resqnet_db"
                            )
                            .fallbackToDestructiveMigration() // 🔹 Avoids migration crash
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
