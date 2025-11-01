package com.example.resqnet_app.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.resqnet_app.data.local.dao.UserDao;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.example.resqnet_app.data.local.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class SyncService extends Service {

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private UserDao userDao;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userDao = AppDatabase.getInstance(this).userDao();
        mainHandler = new Handler(getMainLooper());
        Log.d("SyncService", "🟢 SyncService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isOnline()) {
            Log.d("SyncService", "❌ No internet, skipping sync");
            stopSelf();
            return START_NOT_STICKY;
        }

        new Thread(() -> {
            List<User> unsyncedUsers = userDao.getUnsyncedUsers();
            if (unsyncedUsers.isEmpty()) {
                Log.d("SyncService", "✅ No unsynced users found.");
                stopSelf();
                return;
            }

            for (User user : unsyncedUsers) {
                Log.d("SyncService", "🔄 Syncing user: " + user.getEmail());

                mainHandler.post(() -> {
                    auth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                            .addOnSuccessListener(authResult -> {
                                String uid = authResult.getUser().getUid();

                                user.setFirebaseId(uid);
                                user.setSynced(true);

                                firestore.collection("Users").document(uid)
                                        .set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            new Thread(() -> userDao.update(user)).start();
                                            Log.d("SyncService", "✅ Synced to Firestore: " + user.getEmail());
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e("SyncService", "❌ Firestore save failed: " + user.getEmail(), e));
                            })
                            .addOnFailureListener(e -> {
                                if (e instanceof FirebaseAuthUserCollisionException) {
                                    Log.w("SyncService", "⚠️ User already exists: " + user.getEmail());
                                    user.setSynced(true);
                                    new Thread(() -> userDao.update(user)).start();
                                } else {
                                    Log.e("SyncService", "❌ Firebase Auth failed: " + user.getEmail(), e);
                                }
                            });
                });
            }
        }).start();

        return START_NOT_STICKY;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
