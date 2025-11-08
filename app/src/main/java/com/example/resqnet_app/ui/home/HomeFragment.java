package com.example.resqnet_app.ui.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.entity.SosAlert;
import com.example.resqnet_app.service.NearbyService;
import com.example.resqnet_app.utils.UserSessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerView activeRecyclerView;
    private SosAlertAdapter adapter;
    private final List<SosAlert> sosAlerts = new ArrayList<>();
    private Button sosButton;

    private NearbyService nearbyService;
    private boolean isBound = false;
    private FusedLocationProviderClient fusedLocationClient;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NearbyService.NearbyBinder binder = (NearbyService.NearbyBinder) service;
            nearbyService = binder.getService();
            isBound = true;

            nearbyService.newSosAlert.observe(getViewLifecycleOwner(), alert -> {
                if (alert != null) {
                    Date now = new Date();
                    if (alert.getDate() == null)
                        alert.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now));
                    if (alert.getTime() == null)
                        alert.setTime(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now));

                    sosAlerts.add(0, alert);
                    adapter.notifyItemInserted(0);
                    activeRecyclerView.scrollToPosition(0);
                    Toast.makeText(requireContext(), "Remote SOS received!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        sosButton = view.findViewById(R.id.sosButton);
        activeRecyclerView = view.findViewById(R.id.activeSosRecyclerView);
        activeRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SosAlertAdapter(sosAlerts, new SosAlertAdapter.OnAlertActionListener() {
            @Override
            public void onHelp(SosAlert sosAlert) {
                Toast.makeText(requireContext(), "Help clicked for: " + sosAlert.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAcknowledge(SosAlert sosAlert) {
                Toast.makeText(requireContext(), "Acknowledged: " + sosAlert.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationClick(SosAlert sosAlert) {
                if (sosAlert.getLatitude() != 0 && sosAlert.getLongitude() != 0) {
                    Bundle bundle = new Bundle();
                    bundle.putDouble("lat", sosAlert.getLatitude());
                    bundle.putDouble("lon", sosAlert.getLongitude());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_bottom_home1_to_bottom_map1, bundle);
                } else {
                    Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        activeRecyclerView.setAdapter(adapter);

        sosButton.setOnClickListener(v -> triggerSosAlert());

        // Bind NearbyService
        Intent intent = new Intent(requireContext(), NearbyService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        return view;
    }

    private void triggerSosAlert() {
        UserSessionManager session = new UserSessionManager(requireContext());
        String username = session.getUsername();

        SosAlert alert = new SosAlert();
        alert.setTitle("SOS ALERT");
        alert.setDescription(username + " needs help!");
        alert.setStatus("active");

        Date now = new Date();
        alert.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now));
        alert.setTime(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now));

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    alert.setLatitude(location.getLatitude());
                    alert.setLongitude(location.getLongitude());
                } else {
                    Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
                }
                addAlertToList(alert);
            });
        } else {
            addAlertToList(alert);
        }
    }

    private void addAlertToList(SosAlert alert) {
        sosAlerts.add(0, alert);
        adapter.notifyItemInserted(0);
        activeRecyclerView.scrollToPosition(0);

        if (isBound && nearbyService != null) {
            nearbyService.sendSOS(alert.getDescription(), alert.getLatitude(), alert.getLongitude());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound && nearbyService != null) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
    }
}
