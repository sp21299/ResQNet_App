//package com.example.resqnet_app.local;
//
//import android.content.Context;
//
//import androidx.room.Database;
//import androidx.room.Room;
//import androidx.room.RoomDatabase;
//
//// Add your entity (User)
//@Database(entities = {User.class}, version = 1, exportSchema = false)
//public abstract class AppDatabase extends RoomDatabase {
//
//    private static volatile AppDatabase INSTANCE;
//
//    // DAO reference
//    public abstract UserDao userDao();
//
//    // Singleton pattern for single DB instance
//    public static AppDatabase getInstance(Context context) {
//        if (INSTANCE == null) {
//            synchronized (AppDatabase.class) {
//                if (INSTANCE == null) {
//                    INSTANCE = Room.databaseBuilder(
//                                    context.getApplicationContext(),
//                                    AppDatabase.class,
//                                    "resqnet_db"
//                            )
//                            // Optional: If schema changes, deletes old DB to avoid crash
//                            .fallbackToDestructiveMigration()
//                            // Optional: allow main thread queries (for quick testing)
//                            // .allowMainThreadQueries()
//                            .build();
//                }
//            }
//        }
//        return INSTANCE;
//    }
//}
