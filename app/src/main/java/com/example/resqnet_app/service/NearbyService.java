package com.example.resqnet_app.service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.example.resqnet_app.data.local.entity.SosAlert;
import com.example.resqnet_app.utils.Event;
import com.example.resqnet_app.utils.NetworkUtils;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NearbyService extends Service {

    private static final String TAG = "NearbyService";
    private static final String CHANNEL_ID = "NearbyServiceChannel";
    private static final String SERVICE_ID = "com.example.resqnet_app.NEARBY_SERVICE";
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private final IBinder binder = new NearbyBinder();
    private ConnectionsClient connectionsClient;
    private final Map<String, String> connectedEndpoints = new HashMap<>();

    public final MutableLiveData<String> receivedMessage = new MutableLiveData<>();
    public final MutableLiveData<List<String>> connectedDevices = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Event<SosAlert>> newSosAlert = new MutableLiveData<>();

    private String localUserName = "User";
    private boolean isAdvertising = false;
    private boolean isDiscovering = false;

    // Camera & Flash
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;

    // SOS
    private MediaPlayer sosPlayer;
    private Vibrator vibrator;

    // Room DB
    private AppDatabase appDatabase;

    // Firestore
    private FirebaseFirestore firestore;

    // Retry Nearby
    private final Handler nearbyRetryHandler = new Handler(Looper.getMainLooper());
    private final Runnable nearbyRetryRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isDiscovering || connectedEndpoints.isEmpty()) {
                Log.d(TAG, "Retrying Nearby discovery...");
                startAdvertising();
                startDiscovery();
                nearbyRetryHandler.postDelayed(this, 5000);
            } else {
                nearbyRetryHandler.removeCallbacks(this);
            }
        }
    };

    private void scheduleNearbyRetry() {
        nearbyRetryHandler.removeCallbacks(nearbyRetryRunnable);
        nearbyRetryHandler.postDelayed(nearbyRetryRunnable, 5000);
    }

    // SOS auto-stop
    private final Handler sosStopHandler = new Handler(Looper.getMainLooper());
    private final Runnable sosStopRunnable = this::stopSOSAlert;

    private void scheduleSOSAutoStop() {
        sosStopHandler.removeCallbacks(sosStopRunnable);
        sosStopHandler.postDelayed(sosStopRunnable, 60000); // 1 min
    }

    public void sendMessage(String text) {
        if (text == null || text.isEmpty()) return;

        if (connectionsClient == null || connectedEndpoints.isEmpty()) {
            Log.w(TAG, "No connected devices to send message");
            return;
        }

        Payload payload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));

        // Send the payload to all connected endpoints
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
        appDatabase = AppDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();

        createNotificationChannel();
        startForegroundSafe();

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

        restartNearbyIfNeeded();
    }

    @Override
    public IBinder onBind(android.content.Intent intent) {
        return binder;
    }

    // ---------------- Nearby Start/Stop ----------------
    public void startNearby(String username) {
        this.localUserName = username != null ? username : "User";
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

    private void startAdvertising() {
        if (isAdvertising) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
            return;

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            return;

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

    // ---------------- Connection Callbacks ----------------
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            connectionsClient.requestConnection(localUserName, endpointId, connectionLifecycleCallback);
            nearbyRetryHandler.removeCallbacks(nearbyRetryRunnable);
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            connectedEndpoints.remove(endpointId);
            updateConnectedDevices();
            if (connectedEndpoints.isEmpty()) scheduleNearbyRetry();
        }
    };

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
            connectedEndpoints.put(endpointId, connectionInfo.getEndpointName());
            updateConnectedDevices();
            connectionsClient.acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            int code = result.getStatus().getStatusCode();
            if (code != ConnectionsStatusCodes.STATUS_OK) {
                connectedEndpoints.remove(endpointId);
                updateConnectedDevices();
                scheduleNearbyRetry();
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            connectedEndpoints.remove(endpointId);
            updateConnectedDevices();
            if (connectedEndpoints.isEmpty()) scheduleNearbyRetry();
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.asBytes() != null) {
                String message = new String(payload.asBytes(), StandardCharsets.UTF_8);
                try {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(message).getAsJsonObject();
                    if (json.has("uuid")) { // SOS message check
                        SosAlert alert = new SosAlert();
                        alert.setTitle(json.get("title").getAsString());
                        alert.setDescription(json.get("description").getAsString());
                        alert.setLatitude(json.get("latitude").getAsDouble());
                        alert.setLongitude(json.get("longitude").getAsDouble());
                        alert.setDate(json.get("date").getAsString());
                        alert.setTimestamp(json.get("time").getAsString());
                        alert.setUuid(json.get("uuid").getAsString());
                        alert.setStatus("active");

                        postSosAlert(alert); // notify UI
                    } else {
                        receivedMessage.postValue(message);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Received non-SOS message: " + message);
                    receivedMessage.postValue(message);
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {}
    };


    // ---------------- SOS Methods ----------------
    public void sendSOS(String username, double latitude, double longitude) {
        SosAlert alert = new SosAlert();
        alert.setUuid(UUID.randomUUID().toString());

        // Date & Time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alert.setDate(LocalDate.now().toString());
            alert.setTimestamp(LocalTime.now().toString());
        } else {
            alert.setDate(new java.util.Date().toString());
            alert.setTimestamp(String.valueOf(System.currentTimeMillis()));
        }

        alert.setTitle("SOS ALERT");
        alert.setDescription(username + " needs help");
        alert.setLatitude(latitude);
        alert.setLongitude(longitude);
        alert.setStatus("active");
        alert.setSynced(false);

        // Save to Room DB & Firestore
        new Thread(() -> {
            appDatabase.sosAlertDao().insert(alert);

            if (NetworkUtils.isOnline(getApplicationContext())) {
                firestore.collection("sos_alerts")
                        .document(alert.getUuid())
                        .set(alert)
                        .addOnSuccessListener(unused -> {
                            alert.setSynced(true);
                            appDatabase.sosAlertDao().update(alert);
                        });
            } else {
                sendSOSNearby(alert); // send to nearby devices if offline
            }
        }).start();

        triggerSOSAlert(username + " needs help");
    }

    private void sendSOSNearby(SosAlert alert) {
        try {
            Map<String, Object> sosData = new HashMap<>();
            sosData.put("title", alert.getTitle());
            sosData.put("description", alert.getDescription());
            sosData.put("latitude", alert.getLatitude());
            sosData.put("longitude", alert.getLongitude());
            sosData.put("date", alert.getDate());
            sosData.put("time", alert.getTimestamp());
            sosData.put("uuid", alert.getUuid());

            String json = new com.google.gson.Gson().toJson(sosData);
            Payload payload = Payload.fromBytes(json.getBytes(StandardCharsets.UTF_8));

            for (String endpointId : new ArrayList<>(connectedEndpoints.keySet())) {
                connectionsClient.sendPayload(endpointId, payload);
            }
        } catch (Exception e) {
            Log.e(TAG, "sendSOSNearby error", e);
        }
    }


    private void postSosAlert(SosAlert alert) {
        newSosAlert.postValue(new Event<>(alert));
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
                sosPlayer.setLooping(true);
            }
            if (!sosPlayer.isPlaying()) sosPlayer.start();
        } catch (Exception e) {
            Log.w(TAG, "SOS media error", e);
        }

        scheduleSOSAutoStop();
    }

    public void stopSOSAlert() {
        try {
            if (sosPlayer != null && sosPlayer.isPlaying()) {
                sosPlayer.stop();
                sosPlayer.release();
                sosPlayer = null;
            }
        } catch (Exception e) { Log.w(TAG, "SOS stop error", e); }
        if (vibrator != null) vibrator.cancel();
        sosStopHandler.removeCallbacks(sosStopRunnable);
        turnFlashlightOff();
    }

    // ---------------- Helpers ----------------
    private void updateConnectedDevices() {
        connectedDevices.postValue(new ArrayList<>(connectedEndpoints.values()));
    }

    private void restartNearbyIfNeeded() {
        if (!isAdvertising && !isDiscovering) {
            startAdvertising();
            startDiscovery();
        }
    }

    private void startForegroundSafe() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ResQNet Nearby Service")
                .setContentText("Running background connection service...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, b.build());
        } else startForeground(1, b.build());
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

    private void turnFlashlightOff() {
        try {
            if (cameraManager != null && cameraId != null && isFlashOn) {
                cameraManager.setTorchMode(cameraId, false);
                isFlashOn = false;
            }
        } catch (Exception ignored) {}
    }

    private void runOnUiThread(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAll();
        stopSOSAlert();
        nearbyRetryHandler.removeCallbacks(nearbyRetryRunnable);
        sosStopHandler.removeCallbacks(sosStopRunnable);
    }
}
