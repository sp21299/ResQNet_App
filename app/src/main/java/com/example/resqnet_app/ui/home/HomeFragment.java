package com.example.resqnet_app.ui.home;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.example.resqnet_app.data.local.entity.SosAlert;
import com.example.resqnet_app.service.NearbyService;
import com.example.resqnet_app.utils.UserSessionManager;
import com.example.resqnet_app.viewmodel.SharedViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private AppDatabase appDatabase;
    private static final String TAG = "HomeFragment";
    private NearbyService nearbyService;
    private boolean isBound = false;
    private Button sosButton;
    private SharedViewModel sharedViewModel;
    private SosAlertAdapter activeAdapter, resolvedAdapter;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;

    private final List<SosAlert> activeAlerts = new ArrayList<>();
    private final List<SosAlert> resolvedAlerts = new ArrayList<>();

    // ---------------- Service Connection ----------------
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NearbyService.NearbyBinder binder = (NearbyService.NearbyBinder) service;
            nearbyService = binder.getService();
            isBound = true;
            Log.d(TAG, "NearbyService bound");

            UserSessionManager session = new UserSessionManager(requireContext());
            String username = session.getUsername();
            nearbyService.startNearby(username);

            nearbyService.newSosAlert.observe(getViewLifecycleOwner(), event -> {
                if (event != null) {
                    SosAlert alert = event.getContentIfNotHandled();
                    if (alert != null) {
                        activeAlerts.add(alert);
                        activeAdapter.notifyItemInserted(activeAlerts.size() - 1);
                        Toast.makeText(requireContext(),
                                "🚨 SOS received from " + alert.getDescription(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            nearbyService = null;
            isBound = false;
            Log.d(TAG, "NearbyService disconnected");
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        super.onViewCreated(view, savedInstanceState);
        appDatabase = AppDatabase.getInstance(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Load previously saved SOS alerts
        loadSavedAlerts();

        sosButton = view.findViewById(R.id.sosButton);

        RecyclerView activeRecycler = view.findViewById(R.id.activeSosRecyclerView);
        RecyclerView resolvedRecycler = view.findViewById(R.id.resolvedSosRecyclerView);
        activeRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        resolvedRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        // ---------------- Adapter listener ----------------
        SosAlertAdapter.OnAlertActionListener listener = new SosAlertAdapter.OnAlertActionListener() {
            @Override
            public void onHelp(SosAlert sosAlert) {
                if (!"helping".equals(sosAlert.getStatus())) {
                    sosAlert.setStatus("helping"); // update status in DB
                    new Thread(() -> appDatabase.sosAlertDao().update(sosAlert)).start();

                    // Move alert from active to resolved
                    activeAlerts.remove(sosAlert);
                    resolvedAlerts.add(sosAlert);

                    // Notify adapters
                    activeAdapter.notifyDataSetChanged();
                    resolvedAdapter.notifyDataSetChanged();

                    Toast.makeText(requireContext(), "Helping! ✅", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAcknowledge(SosAlert sosAlert) {
                if (!sosAlert.isAcknowledged()) {
                    sosAlert.setAcknowledged(true);
                    sosAlert.setStatus("resolved"); // mark as resolved
                    new Thread(() -> appDatabase.sosAlertDao().update(sosAlert)).start();

                    // Move alert from active to resolved
                    activeAlerts.remove(sosAlert);
                    resolvedAlerts.add(sosAlert);

                    // Notify adapters
                    activeAdapter.notifyDataSetChanged();
                    resolvedAdapter.notifyDataSetChanged();

                    Toast.makeText(requireContext(), "Acknowledged ✅", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLocationClick(SosAlert sosAlert) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
                    return;
                }

                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                sharedViewModel.updateSosLocation(location.getLatitude(), location.getLongitude());
                                Toast.makeText(requireContext(), "📍 Location updated. Open Map tab to view.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "Unable to fetch location.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        };
        // --------------------------------------------------------

        activeAdapter = new SosAlertAdapter(activeAlerts, listener, appDatabase);
        resolvedAdapter = new SosAlertAdapter(resolvedAlerts, listener, appDatabase);
        activeRecycler.setAdapter(activeAdapter);
        resolvedRecycler.setAdapter(resolvedAdapter);

        setupPermissionLauncher();
        requestNearbyPermissions();

        sosButton.setOnClickListener(v -> {
            if (nearbyService != null && isBound) {
                UserSessionManager session = new UserSessionManager(requireContext());
                String username = session.getUsername();
                if (username == null || username.isEmpty()) username = "Unknown User";

                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
                    return;
                }

                String finalUsername = username;
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener(location -> {
                            double lat = 0, lon = 0;
                            if (location != null) {
                                lat = location.getLatitude();
                                lon = location.getLongitude();
                            }

                            nearbyService.sendSOS(finalUsername, lat, lon);

                            SosAlert alert = new SosAlert();
                            alert.setTitle("SOS ALERT");
                            alert.setDescription(finalUsername + " needs help");
                            alert.setStatus("active");
                            alert.setLatitude(lat);
                            alert.setLongitude(lon);

                            activeAlerts.add(alert);
                            activeAdapter.notifyItemInserted(activeAlerts.size() - 1);

                            new Thread(() -> appDatabase.sosAlertDao().insert(alert)).start();

                            Toast.makeText(requireContext(), "🚨 SOS sent to nearby devices!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SOS sent by " + finalUsername + " at " + lat + ", " + lon);
                        });
            } else {
                Toast.makeText(requireContext(), "NearbyService not ready yet.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "SOS button clicked but service not bound");
            }
        });
    }

    private void loadSavedAlerts() {
        new Thread(() -> {
            List<SosAlert> allAlerts = appDatabase.sosAlertDao().getAllAlerts();
            requireActivity().runOnUiThread(() -> {
                activeAlerts.clear();
                resolvedAlerts.clear();
                for (SosAlert alert : allAlerts) {
                    if ("active".equalsIgnoreCase(alert.getStatus()) || "helping".equalsIgnoreCase(alert.getStatus())) {
                        activeAlerts.add(alert);
                    } else {
                        resolvedAlerts.add(alert);
                    }
                }
                activeAdapter.notifyDataSetChanged();
                resolvedAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    @Override
    public void onStart() {
        super.onStart();
        startAndBindService();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound) {
            requireActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }

    // ---------------- Permissions ----------------
    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) allGranted = false;
                    }
                    if (!allGranted) {
                        Toast.makeText(requireContext(), "Permissions required for Nearby to work.", Toast.LENGTH_LONG).show();
                    } else startAndBindService();
                }
        );
    }

    private void requestNearbyPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        List<String> toRequest = new ArrayList<>();
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED)
                toRequest.add(p);
        }

        if (!toRequest.isEmpty()) permissionLauncher.launch(toRequest.toArray(new String[0]));
        else startAndBindService();
    }

    private void startAndBindService() {
        Intent serviceIntent = new Intent(requireContext(), NearbyService.class);
        requireActivity().startService(serviceIntent);
        requireActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
}
