package com.example.resqnet_app.ui.home;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.example.resqnet_app.data.local.dao.SosAlertDao;
import com.example.resqnet_app.data.local.entity.SosAlert;
import com.example.resqnet_app.databinding.FragmentHomeBinding;
import com.example.resqnet_app.service.NearbyService;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private SosAlertDao sosAlertDao;

    private SosAlertAdapter sentAdapter;     // SOS sent by current user
    private SosAlertAdapter receivedAdapter; // SOS received from nearby users

    private NearbyService nearbyService;
    private boolean isBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            NearbyService.NearbyBinder nearbyBinder = (NearbyService.NearbyBinder) binder;
            nearbyService = nearbyBinder.getService();
            isBound = true;

            nearbyService.receivedMessage.observe(getViewLifecycleOwner(), msg -> showIncomingSos(msg));
            nearbyService.startNearby("Shravani"); // replace with dynamic username
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, android.os.Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // RecyclerViews
        binding.activeSosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.resolvedSosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        sentAdapter = new SosAlertAdapter(new ArrayList<>());
        receivedAdapter = new SosAlertAdapter(new ArrayList<>());

        binding.activeSosRecyclerView.setAdapter(sentAdapter);
        binding.resolvedSosRecyclerView.setAdapter(receivedAdapter);

        // Initialize SOS DAO
        sosAlertDao = AppDatabase.getInstance(requireContext()).sosAlertDao();

        // Load existing SOS alerts
        loadSosFromRoom();

        // Toolbar & Drawer setup
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            Toolbar toolbar = activity.findViewById(R.id.toolbar);
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null)
                activity.getSupportActionBar().setTitle("Home");

            DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
            NavigationView navView = activity.findViewById(R.id.nav_view);
            toolbar.setNavigationOnClickListener(v -> {
                if (drawer != null) drawer.openDrawer(GravityCompat.START);
            });
        }

        // Buttons
        binding.sosButton.setOnClickListener(v -> showSosConfirmation());
        binding.seeSos.setOnClickListener(v -> loadSosFromRoom());

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getContext(), NearbyService.class);
        getContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBound) {
            getContext().unbindService(connection);
            isBound = false;
        }
    }

    private void showSosConfirmation() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Send SOS")
                .setMessage("Are you sure you want to send an emergency alert?")
                .setPositiveButton("Yes", (dialog, which) -> sendSosAlert())
                .setNegativeButton("No", null)
                .show();
    }

    private void sendSosAlert() {
        if (getContext() == null) return;

        SosAlert newAlert = new SosAlert();
        newAlert.title = "SOS Alert!";
        newAlert.description = "User needs help!";
        newAlert.location = "Unknown";
        newAlert.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        newAlert.time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        newAlert.uid = "currentUserUID";
        newAlert.createdAt = System.currentTimeMillis();

        new Thread(() -> {
            try {
                sosAlertDao.insert(newAlert);
                requireActivity().runOnUiThread(() -> sentAdapter.addAlert(newAlert));

                // Send via mesh
                if (nearbyService != null && isBound) {
                    String msg = newAlert.title + " | " + newAlert.description;
                    nearbyService.sendMessage(msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving SOS alert", e);
            }
        }).start();
    }

    private void showIncomingSos(String message) {
        new Thread(() -> {
            SosAlert receivedAlert = new SosAlert();

            if (message.startsWith("FORWARD|")) {
                receivedAlert.title = "Forwarded SOS";
                receivedAlert.description = message.substring(8);
            } else {
                receivedAlert.title = "Received SOS";
                receivedAlert.description = message;
            }

            receivedAlert.location = "Nearby Device";
            receivedAlert.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            receivedAlert.time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            receivedAlert.uid = "remoteUser";
            receivedAlert.createdAt = System.currentTimeMillis();

            try {
                sosAlertDao.insert(receivedAlert);
            } catch (Exception e) {
                Log.e(TAG, "Error saving incoming SOS to Room", e);
            }

            requireActivity().runOnUiThread(() -> {
                receivedAdapter.addAlert(receivedAlert);
                triggerAlertEffects();
            });
        }).start();
    }

    @SuppressLint("NewApi")
    private void triggerAlertEffects() {
        if (getContext() == null) return;

        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 1000}, 0));

        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.buzzer_sound);
        mp.start();

        CameraManager cm = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cm.getCameraIdList()[0];
            cm.setTorchMode(cameraId, true);
            new Handler().postDelayed(() -> {
                try {
                    cm.setTorchMode(cameraId, false);
                } catch (Exception ignored) {}
            }, 3000);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadSosFromRoom() {
        new Thread(() -> {
            try {
                List<SosAlert> allAlerts = sosAlertDao.getAll();
                List<SosAlert> sent = new ArrayList<>();
                List<SosAlert> received = new ArrayList<>();

                for (SosAlert a : allAlerts) {
                    if ("currentUserUID".equals(a.uid)) sent.add(a);
                    else received.add(a);
                }

                requireActivity().runOnUiThread(() -> {
                    sentAdapter.updateData(sent);
                    receivedAdapter.updateData(received);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading SOS alerts", e);
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
