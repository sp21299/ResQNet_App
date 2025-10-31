package com.example.resqnet_app.service;

import android.util.Log;
import com.google.android.gms.nearby.connection.*;

public class NearbyService {

    private final ConnectionsClient connectionsClient;
    private final String userName;
    private final String SERVICE_ID = "resqnet_service";
    private final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private String connectedEndpointId = null;
    private OnSOSReceivedListener listener;

    public NearbyService(ConnectionsClient client, String name) {
        this.connectionsClient = client;
        this.userName = name;
    }

    public interface OnSOSReceivedListener {
        void onSOSReceived(String message);
    }

    public void setOnSOSReceivedListener(OnSOSReceivedListener listener) {
        this.listener = listener;
    }

    // Start Advertising
    public void startAdvertising() {
        AdvertisingOptions options = new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startAdvertising(
                        userName,
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        options
                ).addOnSuccessListener(unused -> Log.d("Nearby", "Advertising started"))
                .addOnFailureListener(e -> Log.e("Nearby", "Advertising failed: " + e.getMessage()));
    }

    // Start Discovery
    public void startDiscovery() {
        DiscoveryOptions options = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startDiscovery(
                        SERVICE_ID,
                        endpointDiscoveryCallback,
                        options
                ).addOnSuccessListener(unused -> Log.d("Nearby", "Discovery started"))
                .addOnFailureListener(e -> Log.e("Nearby", "Discovery failed: " + e.getMessage()));
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback);
            connectedEndpointId = endpointId;
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution result) {
            if (result.getStatus().isSuccess()) Log.d("Nearby", "Connected to " + endpointId);
            else Log.e("Nearby", "Connection failed");
        }

        @Override
        public void onDisconnected(String endpointId) {
            Log.d("Nearby", "Disconnected: " + endpointId);
        }
    };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
            Log.d("Nearby", "Found: " + info.getEndpointName());
            connectionsClient.requestConnection(userName, endpointId, connectionLifecycleCallback);
        }

        @Override
        public void onEndpointLost(String endpointId) {
            Log.d("Nearby", "Lost: " + endpointId);
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            String received = new String(payload.asBytes());
            Log.d("Nearby", "Message: " + received);

            if (received.contains("SOS")) {
                if (listener != null) listener.onSOSReceived(received);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {}
    };

    // Send SOS message
    public void sendMessage(String msg) {
        if (connectedEndpointId != null) {
            Payload payload = Payload.fromBytes(msg.getBytes());
            connectionsClient.sendPayload(connectedEndpointId, payload);
        }
    }

    public void stopAll() {
        connectionsClient.stopAllEndpoints();
    }
}
