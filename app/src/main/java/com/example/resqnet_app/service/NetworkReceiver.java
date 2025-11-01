package com.example.resqnet_app.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnected()) {
            Log.d("NetworkReceiver", "🌐 Internet connected — syncing offline users...");
            // Start the SyncService properly
            Intent serviceIntent = new Intent(context, SyncService.class);
            context.startService(serviceIntent);
        } else {
            Log.d("NetworkReceiver", "❌ No internet connection.");
        }
    }
}
