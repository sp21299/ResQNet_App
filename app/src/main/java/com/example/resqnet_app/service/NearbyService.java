package com.example.resqnet_app.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import com.example.resqnet_app.data.local.entity.SosAlert;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.nearby.connection.DiscoveryOptions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearbyService extends Service {

    private static final String TAG = "NearbyService";
    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    private final IBinder binder = new NearbyBinder();
    public final MutableLiveData<String> receivedMessage = new MutableLiveData<>();
    public final MutableLiveData<List<String>> connectedDevices = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<SosAlert> newSosAlert = new MutableLiveData<>();

    private String userName = "User";
    private final Map<String, String> endpoints = new HashMap<>();
    private final List<String> receivedMessageIds = new ArrayList<>();

    public class NearbyBinder extends Binder {
        public NearbyService getService() {
            return NearbyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void startNearby(String name) {
        userName = name;

        // Check runtime permissions
        if (!checkPermissions()) {
            Log.e(TAG, "Missing required permissions!");
            return;
        }

        startAdvertising();
        startDiscovery();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void stopAll() {
        try {
            Nearby.getConnectionsClient(this).stopAllEndpoints();
            Nearby.getConnectionsClient(this).stopAdvertising();
            Nearby.getConnectionsClient(this).stopDiscovery();
            endpoints.clear();
            updateConnectedDevices();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping Nearby activities: " + e.getMessage());
        }
    }

    private void startAdvertising() {
        Nearby.getConnectionsClient(this)
                .startAdvertising(userName, getPackageName(), connectionLifecycleCallback,
                        new AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener(unused -> Log.d(TAG, "Advertising started successfully"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Advertising failed: " + e.getMessage());
                    // Retry after 2 seconds
                    postDelayed(this::startAdvertising, 2000);
                });
    }

    private void startDiscovery() {
        Nearby.getConnectionsClient(this)
                .startDiscovery(getPackageName(), endpointDiscoveryCallback,
                        new DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener(unused -> Log.d(TAG, "Discovery started successfully"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Discovery failed: " + e.getMessage());
                    // Retry after 2 seconds
                    postDelayed(this::startDiscovery, 2000);
                });
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            Log.d(TAG, "Endpoint found: " + info.getEndpointName());
            attemptConnection(endpointId);
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.d(TAG, "Endpoint lost: " + endpoints.get(endpointId));
            endpoints.remove(endpointId);
            updateConnectedDevices();
        }
    };

    private void attemptConnection(String endpointId) {
        Log.d(TAG, "Attempting connection to: " + endpointId);
        Nearby.getConnectionsClient(this)
                .requestConnection(userName, endpointId, connectionLifecycleCallback)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Connection request failed: " + e.getMessage());
                    // Retry after 2 seconds
                    postDelayed(() -> attemptConnection(endpointId), 2000);
                });
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            Log.d(TAG, "Connection initiated with " + info.getEndpointName());
            endpoints.put(endpointId, info.getEndpointName());
            updateConnectedDevices();
            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            if (result.getStatus().isSuccess()) {
                Log.d(TAG, "Connection successful with " + endpoints.get(endpointId));
            } else {
                Log.e(TAG, "Connection failed for " + endpoints.get(endpointId));
                endpoints.remove(endpointId);
                updateConnectedDevices();
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Log.d(TAG, "Disconnected from " + endpoints.get(endpointId));
            endpoints.remove(endpointId);
            updateConnectedDevices();
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.getType() == Payload.Type.BYTES) {
                String msg = new String(payload.asBytes(), StandardCharsets.UTF_8);

                if (msg.startsWith("SOS|")) {
                    String desc = msg.substring(4);
                    SosAlert alert = new SosAlert();
                    alert.setTitle("SOS ALERT");
                    alert.setDescription(desc);
                    alert.setStatus("active");
                    newSosAlert.postValue(alert);
                } else {
                    if (!receivedMessageIds.contains(msg)) {
                        receivedMessageIds.add(msg);
                        receivedMessage.postValue(msg);
                    }
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {}
    };

    public void sendMessage(String message) {
        if (endpoints.isEmpty()) return;
        receivedMessageIds.add(message);
        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        for (String endpointId : endpoints.keySet()) {
            Nearby.getConnectionsClient(this).sendPayload(endpointId, payload);
        }
    }

    public void sendSOS(String message, double latitude, double longitude) {
        if (endpoints.isEmpty()) return;
        String sosMessage = "SOS|" + message + "|" + latitude + "," + longitude;
        Payload payload = Payload.fromBytes(sosMessage.getBytes(StandardCharsets.UTF_8));
        for (String endpointId : endpoints.keySet()) {
            Nearby.getConnectionsClient(this).sendPayload(endpointId, payload);
        }
    }

    private void updateConnectedDevices() {
        connectedDevices.postValue(new ArrayList<>(endpoints.values()));
    }

    private void postDelayed(Runnable runnable, long delayMillis) {
        new android.os.Handler(getMainLooper()).postDelayed(runnable, delayMillis);
    }
}
