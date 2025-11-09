package com.example.resqnet_app.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.entity.SosAlert;
import com.example.resqnet_app.utils.Event;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearbyService extends Service {
    private static final String TAG = "NearbyService";
    private static final String CHANNEL_ID = "NearbyServiceChannel";
    private static final String SERVICE_ID = "com.example.resqnet_app.NEARBY_SERVICE";
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    // Camera & Flash
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;

    // Flash blinking
    private final Handler flashHandler = new Handler(Looper.getMainLooper());
    private boolean flashOn = false;
    private final long[] flashPattern = {0, 300, 200, 300}; // blink pattern
    private int flashIndex = 0;
    private final Runnable flashRunnable = new Runnable() {
        @Override
        public void run() {
            if (cameraManager != null && cameraId != null) {
                try {
                    flashOn = !flashOn;
                    cameraManager.setTorchMode(cameraId, flashOn);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to toggle flashlight", e);
                }
            }
            flashIndex = (flashIndex + 1) % flashPattern.length;
            flashHandler.postDelayed(this, flashPattern[flashIndex]);
        }
    };

    private final IBinder binder = new NearbyBinder();
    private ConnectionsClient connectionsClient;
    private final Map<String, String> connectedEndpoints = new HashMap<>();

    public final MutableLiveData<String> receivedMessage = new MutableLiveData<>();
    public final MutableLiveData<List<String>> connectedDevices = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Event<SosAlert>> newSosAlert = new MutableLiveData<>();

    private String localUserName = "User";
    private boolean isAdvertising = false;
    private boolean isDiscovering = false;

    private MediaPlayer sosPlayer;
    private Vibrator vibrator;

    // ---------------- Retry Nearby ----------------
    private final Handler nearbyRetryHandler = new Handler(Looper.getMainLooper());
    private final Runnable nearbyRetryRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isDiscovering || connectedEndpoints.isEmpty()) {
                Log.d(TAG, "Retrying Nearby discovery...");
                startAdvertising();
                startDiscovery();
                nearbyRetryHandler.postDelayed(this, 5000); // retry every 5 seconds
            } else {
                nearbyRetryHandler.removeCallbacks(this);
            }
        }
    };

    private void scheduleNearbyRetry() {
        nearbyRetryHandler.removeCallbacks(nearbyRetryRunnable);
        nearbyRetryHandler.postDelayed(nearbyRetryRunnable, 5000);
    }

    // ---------------- SOS Auto-stop ----------------
    private final Handler sosStopHandler = new Handler(Looper.getMainLooper());
    private final Runnable sosStopRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-stopping SOS buzzer after 1 minute");
            stopSOSAlert();
        }
    };

    private void scheduleSOSAutoStop() {
        sosStopHandler.removeCallbacks(sosStopRunnable);
        sosStopHandler.postDelayed(sosStopRunnable, 60000); // 1 min
    }

    // ---------------- Messaging ----------------
    public void sendMessage(String text) {
        if (text == null || text.isEmpty()) return;

        if (connectionsClient == null || connectedEndpoints.isEmpty()) {
            Log.w(TAG, "No connected devices to send message");
            return;
        }

        Payload payload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));

        for (String endpointId : new ArrayList<>(connectedEndpoints.keySet())) {
            try {
                connectionsClient.sendPayload(endpointId, payload)
                        .addOnSuccessListener(unused -> Log.d(TAG, "Message sent to " + endpointId))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to send message to " + endpointId, e));
            } catch (Exception e) {
                Log.w(TAG, "sendMessage exception for " + endpointId, e);
            }
        }
    }

    public class NearbyBinder extends Binder {
        public NearbyService getService() {
            return NearbyService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connectionsClient = Nearby.getConnectionsClient(this);
        createNotificationChannel();
        startForegroundSafe();

        // Camera/Flash
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                if (cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                    cameraId = id;
                    break;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Flashlight not available", e);
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Log.d(TAG, "NearbyService created");
        restartNearbyIfNeeded();
    }

    @Override
    public IBinder onBind(android.content.Intent intent) {
        return binder;
    }

    // ---------------- Start/Stop Nearby ----------------
    public void startNearby(String username) {
        this.localUserName = username != null ? username : "User";
        Log.d(TAG, "Nearby started for user: " + this.localUserName);
        startAdvertising();
        startDiscovery();
    }

    public void stopAll() {
        try {
            if (connectionsClient != null) {
                connectionsClient.stopAdvertising();
                connectionsClient.stopDiscovery();
                connectionsClient.stopAllEndpoints();
            }
        } catch (Exception e) {
            Log.w(TAG, "stopAll exception", e);
        }
        isAdvertising = false;
        isDiscovering = false;
        nearbyRetryHandler.removeCallbacks(nearbyRetryRunnable);
    }

    // ---------------- Foreground/Notification ----------------
    private void startForegroundSafe() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ResQNet Nearby Service")
                .setContentText("Running background connection service...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        Notification notification = b.build();

        if (Build.VERSION.SDK_INT >= 34) {
            try {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            } catch (SecurityException se) {
                Log.w(TAG, "startForeground fallback", se);
                startForeground(1, notification);
            }
        } else {
            startForeground(1, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Nearby Service", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void turnFlashlightOn() {
        if (cameraManager != null && cameraId != null && !isFlashOn) {
            try {
                cameraManager.setTorchMode(cameraId, true);
                isFlashOn = true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to turn on flashlight", e);
            }
        }
    }

    private void turnFlashlightOff() {
        if (cameraManager != null && cameraId != null && isFlashOn) {
            try {
                cameraManager.setTorchMode(cameraId, false);
                isFlashOn = false;
            } catch (Exception e) {
                Log.w(TAG, "Failed to turn off flashlight", e);
            }
        }
    }

    // ---------------- Advertising / Discovery ----------------
    private void startAdvertising() {
        if (isAdvertising) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) return;

        isAdvertising = true;
        connectionsClient.startAdvertising(
                        localUserName,
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        new AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
                ).addOnSuccessListener(unused -> Log.d(TAG, "Advertising started"))
                .addOnFailureListener(e -> {
                    isAdvertising = false;
                    Log.e(TAG, "Advertising failed", e);
                    scheduleNearbyRetry();
                });
    }

    private void startDiscovery() {
        if (isDiscovering) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) return;

        isDiscovering = true;
        connectionsClient.startDiscovery(
                        SERVICE_ID,
                        endpointDiscoveryCallback,
                        new DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
                ).addOnSuccessListener(unused -> Log.d(TAG, "Discovery started"))
                .addOnFailureListener(e -> {
                    isDiscovering = false;
                    Log.e(TAG, "Discovery failed", e);
                    scheduleNearbyRetry();
                });
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            Log.d(TAG, "Endpoint found: " + info.getEndpointName() + " id=" + endpointId);
            connectionsClient.requestConnection(localUserName, endpointId, connectionLifecycleCallback);
            nearbyRetryHandler.removeCallbacks(nearbyRetryRunnable); // stop retry
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.d(TAG, "Endpoint lost: " + endpointId);
            connectedEndpoints.remove(endpointId);
            updateConnectedDevices();
            if (connectedEndpoints.isEmpty()) scheduleNearbyRetry();
        }
    };

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
            String remoteName = connectionInfo.getEndpointName();
            connectedEndpoints.put(endpointId, remoteName);
            updateConnectedDevices();
            connectionsClient.acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            int code = result.getStatus().getStatusCode();
            if (code != ConnectionsStatusCodes.STATUS_OK) {
                connectedEndpoints.remove(endpointId);
                updateConnectedDevices();
                Log.e(TAG, "Connection failed with code " + code);
                scheduleNearbyRetry();
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            connectedEndpoints.remove(endpointId);
            updateConnectedDevices();
            if (connectedEndpoints.isEmpty()) scheduleNearbyRetry();
            Log.d(TAG, "Disconnected from " + endpointId);
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.asBytes() != null) {
                String message = new String(payload.asBytes(), StandardCharsets.UTF_8);
                Log.d(TAG, "Message received: " + message);

                if (message.startsWith("SOS|")) {
                    String[] parts = message.split("\\|");
                    SosAlert alert = new SosAlert();
                    alert.setTitle("SOS ALERT");
                    if (parts.length >= 2) alert.setDescription(parts[1]);
                    if (parts.length >= 3) {
                        String[] latLon = parts[2].split(",");
                        try {
                            alert.setLatitude(Double.parseDouble(latLon[0]));
                            alert.setLongitude(Double.parseDouble(latLon[1]));
                        } catch (Exception ignored) {}
                    }
                    alert.setStatus("active");
                    postSosAlert(alert);
                } else {
                    receivedMessage.postValue(message);
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {}
    };

    // ---------------- Public SOS Methods ----------------
    public void sendSOS(String message, double latitude, double longitude) {
        String sosMessage = "SOS|" + message + "|" + latitude + "," + longitude;
        sendSOSMessage(sosMessage);
    }

    public void sendSOSMessage(String message) {
        if (connectedEndpoints.isEmpty()) {
            Log.w(TAG, "No connected devices to send SOS");
            return;
        }
        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        for (String endpointId : new ArrayList<>(connectedEndpoints.keySet())) {
            try {
                connectionsClient.sendPayload(endpointId, payload)
                        .addOnSuccessListener(unused -> Log.d(TAG, "SOS sent to " + endpointId))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to send SOS to " + endpointId, e));
            } catch (Exception e) {
                Log.w(TAG, "sendSOS payload exception", e);
            }
        }
    }

    // ---------------- SOS Alert Helpers ----------------
    private void postSosAlert(SosAlert alert) {
        newSosAlert.postValue(new Event<>(alert));
        runOnUiThread(() -> triggerSOSAlert(alert.getDescription()));
    }

    private void triggerSOSAlert(String message) {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 600, 400, 600}, 0));
            } else vibrator.vibrate(new long[]{0, 600, 400, 600}, 0);
        }

        try {
            if (sosPlayer == null) {
                sosPlayer = MediaPlayer.create(this, R.raw.buzzer_sound);
                if (sosPlayer != null) sosPlayer.setLooping(true);
            }
            if (sosPlayer != null && !sosPlayer.isPlaying()) sosPlayer.start();
        } catch (Exception e) {
            Log.w(TAG, "Failed to start sosPlayer", e);
        }

        // Start blinking flashlight
        flashIndex = 0;
        flashHandler.removeCallbacks(flashRunnable);
        flashHandler.post(flashRunnable);

        try {
            NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("🚨 SOS Alert Received!")
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(2, b.build());
        } catch (Exception e) {
            Log.w(TAG, "Failed to show SOS notification", e);
        }

        scheduleSOSAutoStop(); // schedule auto-stop after 1 min
        Log.d(TAG, "SOS ALERT triggered: " + message);
    }

    public void stopSOSAlert() {
        try {
            if (sosPlayer != null) {
                if (sosPlayer.isPlaying()) sosPlayer.stop();
                sosPlayer.release();
                sosPlayer = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "stopSOSAlert media error", e);
        }

        if (vibrator != null) vibrator.cancel();

        // Stop blinking flashlight
        flashHandler.removeCallbacks(flashRunnable);
        turnFlashlightOff();

        sosStopHandler.removeCallbacks(sosStopRunnable);
    }

    // ---------------- Helpers ----------------
    private void restartNearbyIfNeeded() {
        if (!isAdvertising && !isDiscovering) {
            Log.d(TAG, "Restarting Nearby automatically...");
            startAdvertising();
            startDiscovery();
        }
    }

    private void updateConnectedDevices() {
        List<String> names = new ArrayList<>(connectedEndpoints.values());
        connectedDevices.postValue(names);
    }

    private void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAll();
        stopSOSAlert();
        nearbyRetryHandler.removeCallbacks(nearbyRetryRunnable);
        sosStopHandler.removeCallbacks(sosStopRunnable);
        Log.d(TAG, "NearbyService destroyed");
    }
}
