package com.example.resqnet_app.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;

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

    private String userName = "User";
    private final Map<String, String> endpoints = new HashMap<>(); // endpointId -> friendlyName

    public class NearbyBinder extends Binder {
        public NearbyService getService() {
            return NearbyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // Start advertising & discovery
    public void startNearby(String name) {
        userName = name;

        // Advertising
        Nearby.getConnectionsClient(this)
                .startAdvertising(userName, getPackageName(), connectionLifecycleCallback,
                        new AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener(unused -> Log.d(TAG, "Advertising started"))
                .addOnFailureListener(e -> Log.e(TAG, "Advertising failed", e));

        // Discovery
        Nearby.getConnectionsClient(this)
                .startDiscovery(getPackageName(), endpointDiscoveryCallback,
                        new DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener(unused -> Log.d(TAG, "Discovery started"))
                .addOnFailureListener(e -> Log.e(TAG, "Discovery failed", e));
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            Log.d(TAG, "Endpoint found: " + info.getEndpointName() + " (" + endpointId + ")");
            // Automatically request connection
            Nearby.getConnectionsClient(getApplicationContext())
                    .requestConnection(userName, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener(unused -> Log.d(TAG, "Connection requested"))
                    .addOnFailureListener(e -> Log.e(TAG, "Connection request failed", e));
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.d(TAG, "Endpoint lost: " + endpointId);
            endpoints.remove(endpointId);
            updateConnectedDevices();
        }
    };

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            Log.d(TAG, "Connection initiated: " + info.getEndpointName());
            endpoints.put(endpointId, info.getEndpointName());
            updateConnectedDevices();

            Nearby.getConnectionsClient(getApplicationContext())
                    .acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            if (result.getStatus().isSuccess()) {
                Log.d(TAG, "Connected to: " + endpoints.get(endpointId));
            } else {
                endpoints.remove(endpointId);
                updateConnectedDevices();
                Log.e(TAG, "Connection failed with endpoint: " + endpointId);
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Log.d(TAG, "Disconnected: " + endpointId);
            endpoints.remove(endpointId);
            updateConnectedDevices();
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.getType() == Payload.Type.BYTES) {
                String msg = new String(payload.asBytes(), StandardCharsets.UTF_8);
                Log.d(TAG, "Message received from " + endpoints.get(endpointId) + ": " + msg);
                receivedMessage.postValue(msg);
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) { }
    };

    public void sendMessage(String message) {
        if (endpoints.isEmpty()) return;

        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        for (String endpointId : endpoints.keySet()) {
            Nearby.getConnectionsClient(this).sendPayload(endpointId, payload)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to send message to " + endpoints.get(endpointId), e));
        }
    }

    private void updateConnectedDevices() {
        connectedDevices.postValue(new ArrayList<>(endpoints.values()));
    }

    public void stopAll() {
        Nearby.getConnectionsClient(this).stopAllEndpoints();
        Nearby.getConnectionsClient(this).stopAdvertising();
        Nearby.getConnectionsClient(this).stopDiscovery();
        endpoints.clear();
        updateConnectedDevices();
        Log.d(TAG, "Nearby stopped");
    }
}
