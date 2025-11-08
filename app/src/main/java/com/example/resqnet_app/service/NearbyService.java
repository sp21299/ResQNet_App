package com.example.resqnet_app.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import com.example.resqnet_app.data.local.entity.SosAlert;
import com.example.resqnet_app.utils.Event;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class NearbyService extends Service {

    private static final String TAG = "NearbyService";
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private final IBinder binder = new NearbyBinder();
    public final MutableLiveData<String> receivedMessage = new MutableLiveData<>();
    public final MutableLiveData<List<String>> connectedDevices = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Event<SosAlert>> newSosAlert = new MutableLiveData<>();

    private String userName = "User";
    private final Map<String, String> endpoints = new HashMap<>();
    private final Set<String> receivedMessageIds = new HashSet<>();
    private final Set<String> sentMessages = new HashSet<>();
    private final Handler handler = new Handler();

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
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
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
            Log.e(TAG, "Error stopping Nearby: " + e.getMessage());
        }
    }

    private void startAdvertising() {
        Nearby.getConnectionsClient(this)
                .startAdvertising(userName, getPackageName(), connectionLifecycleCallback,
                        new AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener(unused -> Log.d(TAG, "Advertising started"))
                .addOnFailureListener(e -> handler.postDelayed(this::startAdvertising, 2000));
    }

    private void startDiscovery() {
        Nearby.getConnectionsClient(this)
                .startDiscovery(getPackageName(), endpointDiscoveryCallback,
                        new DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener(unused -> Log.d(TAG, "Discovery started"))
                .addOnFailureListener(e -> handler.postDelayed(this::startDiscovery, 2000));
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            Log.d(TAG, "Endpoint found: " + info.getEndpointName() + " ID: " + endpointId);
            attemptConnection(endpointId);
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            endpoints.remove(endpointId);
            updateConnectedDevices();
        }
    };

    private void attemptConnection(String endpointId) {
        Nearby.getConnectionsClient(this)
                .requestConnection(userName, endpointId, connectionLifecycleCallback)
                .addOnFailureListener(e -> handler.postDelayed(() -> attemptConnection(endpointId), 2000));
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            endpoints.put(endpointId, info.getEndpointName());
            updateConnectedDevices();
            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            if (!result.getStatus().isSuccess()) {
                endpoints.remove(endpointId);
                updateConnectedDevices();
            } else {
                // Send all unsent messages to this endpoint
                for (String msg : sentMessages) sendPayloadToEndpoint(endpointId, msg);
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            endpoints.remove(endpointId);
            updateConnectedDevices();
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.getType() == Payload.Type.BYTES) {
                String msg = new String(payload.asBytes(), StandardCharsets.UTF_8);

                if (!receivedMessageIds.contains(msg)) {
                    receivedMessageIds.add(msg);

                    if (msg.startsWith("SOS|")) {
                        String[] parts = msg.split("\\|");
                        if (parts.length >= 3) {
                            String desc = parts[1];
                            String[] latLon = parts[2].split(",");
                            double lat = 0, lon = 0;
                            try { lat = Double.parseDouble(latLon[0]); lon = Double.parseDouble(latLon[1]); } catch (Exception ignored) {}
                            SosAlert alert = new SosAlert();
                            alert.setTitle("SOS ALERT");
                            alert.setDescription(desc);
                            alert.setLatitude(lat);
                            alert.setLongitude(lon);
                            alert.setStatus("active");
                            newSosAlert.postValue(new Event<>(alert));
                        }
                    } else {
                        receivedMessage.postValue(msg);
                    }
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {}
    };

    private void sendPayloadToAll(String msg) {
        if (!sentMessages.contains(msg)) sentMessages.add(msg);
        for (String endpointId : endpoints.keySet()) sendPayloadToEndpoint(endpointId, msg);
    }

    private void sendPayloadToEndpoint(String endpointId, String msg) {
        Payload payload = Payload.fromBytes(msg.getBytes(StandardCharsets.UTF_8));
        Nearby.getConnectionsClient(this).sendPayload(endpointId, payload);
    }

    public void sendMessage(String message) {
        sendPayloadToAll(message);
    }

    public void sendSOS(String message, double latitude, double longitude) {
        String sosMessage = "SOS|" + message + "|" + latitude + "," + longitude;
        sendPayloadToAll(sosMessage);
    }

    private void updateConnectedDevices() {
        connectedDevices.postValue(new ArrayList<>(endpoints.values()));
    }
}
