package com.example.resqnet_app.ui.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.dao.SosAlertDao;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.example.resqnet_app.data.local.entity.SosAlert;
import com.example.resqnet_app.service.NearbyService;
import com.example.resqnet_app.utils.Event;
import com.example.resqnet_app.utils.UserSessionManager;
import com.example.resqnet_app.viewmodel.SharedViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView activeRecyclerView;
    private SosAlertAdapter adapter;
    private Button sosButton;

    private NearbyService nearbyService;
    private boolean isBound = false;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedViewModel sharedViewModel;

    private AppDatabase db;
    private SosAlertDao sosAlertDao;
    private FirebaseFirestore firestore;

    private final List<SosAlert> sosAlertList = new ArrayList<>();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NearbyService.NearbyBinder binder = (NearbyService.NearbyBinder) service;
            nearbyService = binder.getService();
            isBound = true;

            // Observe remote SOS alerts wrapped in Event
            nearbyService.newSosAlert.observe(getViewLifecycleOwner(), event -> {
                if (event != null) {
                    SosAlert alert = event.getContentIfNotHandled(); // Only handle once
                    if (alert != null) {
                        addSosAlert(alert, true); // remote alert
                        Toast.makeText(requireContext(), "Remote SOS received!", Toast.LENGTH_SHORT).show();
                    }
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
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                          @Nullable android.view.ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        android.view.View view = inflater.inflate(R.layout.fragment_home, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        db = AppDatabase.getInstance(requireContext());
        sosAlertDao = db.sosAlertDao();

        firestore = FirebaseFirestore.getInstance();

        sosButton = view.findViewById(R.id.sosButton);
        activeRecyclerView = view.findViewById(R.id.activeSosRecyclerView);
        activeRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SosAlertAdapter(sosAlertList, new SosAlertAdapter.OnAlertActionListener() {
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
                    sharedViewModel.sosLocation.setValue(new LatLng(sosAlert.getLatitude(), sosAlert.getLongitude()));
                    Toast.makeText(requireContext(), "Location updated. Check Map tab.", Toast.LENGTH_SHORT).show();
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

        // Load previous SOS alerts from Room
        loadSavedSosAlerts();

        return view;
    }

    /** Load previous SOS alerts from Room DB */
    private void loadSavedSosAlerts() {
        sosAlertDao.getAllAlertsLive().observe(getViewLifecycleOwner(), alerts -> {
            sosAlertList.clear();
            sosAlertList.addAll(alerts);
            adapter.notifyDataSetChanged();
        });
    }

    /** Trigger local SOS */
    private void triggerSosAlert() {
        UserSessionManager session = new UserSessionManager(requireContext());
        String username = session.getUsername();

        SosAlert alert = new SosAlert();
        alert.setTitle("SOS ALERT");
        alert.setDescription(username + " needs help!");
        alert.setStatus("active");

        Date now = new Date();
        alert.setDate(new SimpleDateFormat("yyyy-MM-dd").format(now));
        alert.setTime(new SimpleDateFormat("HH:mm:ss").format(now));

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    alert.setLatitude(location.getLatitude());
                    alert.setLongitude(location.getLongitude());
                }
                addSosAlert(alert, false);
            });
        } else {
            addSosAlert(alert, false);
        }
    }

    /** Add SOS to local list, DB, Firestore, and optionally send to nearby */
    private void addSosAlert(SosAlert alert, boolean isRemote) {
        // 1️⃣ Add to list and update RecyclerView
        sosAlertList.add(0, alert);
        adapter.notifyItemInserted(0);
        activeRecyclerView.scrollToPosition(0);

        // 2️⃣ Save to Room DB asynchronously
        saveSosToDatabase(alert);

        // 3️⃣ Save to Firestore (if online)
        if (!isRemote) {
            saveSosToFirestore(alert);
            sendSOS(alert); // send via NearbyService
        }
    }

    /** Save SOS alert to Room DB asynchronously */
    private void saveSosToDatabase(SosAlert alert) {
        new Thread(() -> sosAlertDao.insert(alert)).start();
    }

    /** Save SOS alert to Firestore */
    private void saveSosToFirestore(SosAlert alert) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", alert.getTitle());
        data.put("description", alert.getDescription());
        data.put("date", alert.getDate());
        data.put("time", alert.getTime());
        data.put("status", alert.getStatus());
        data.put("latitude", alert.getLatitude());
        data.put("longitude", alert.getLongitude());

        firestore.collection("sos_alerts")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(requireContext(), "SOS uploaded to Firestore", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to upload SOS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /** Send SOS to nearby devices */
    private void sendSOS(SosAlert alert) {
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
