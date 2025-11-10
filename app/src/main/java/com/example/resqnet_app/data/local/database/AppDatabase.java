package com.example.resqnet_app.data.local.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.resqnet_app.data.local.dao.AlertDao;
import com.example.resqnet_app.data.local.dao.MessageDao;
import com.example.resqnet_app.data.local.dao.UserDao;
import com.example.resqnet_app.data.local.dao.SosAlertDao;
import com.example.resqnet_app.data.local.entity.Alert;
import com.example.resqnet_app.data.local.entity.Message;
import com.example.resqnet_app.data.local.entity.User;
import com.example.resqnet_app.data.local.entity.SosAlert;

@Database(
        entities = {User.class, Message.class, Alert.class, SosAlert.class}, // Added SosAlert
        version = 12,  // Increment version since we added a new table
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract MessageDao messageDao();
    public abstract AlertDao alertDao();
    public abstract SosAlertDao sosAlertDao(); // New DAO for SOS table

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "resqnet_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
