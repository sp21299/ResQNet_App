package com.example.resqnet_app.ui.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.entity.SosAlert;
import com.example.resqnet_app.service.NearbyService;
import com.example.resqnet_app.utils.UserSessionManager;

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

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NearbyService.NearbyBinder binder = (NearbyService.NearbyBinder) service;
            nearbyService = binder.getService();
            isBound = true;

            // Observe incoming SOS from nearby devices
            nearbyService.newSosAlert.observe(getViewLifecycleOwner(), alert -> {
                if (alert != null) {
                    // Add current date/time if not already set
                    if (alert.getDate() == null || alert.getTime() == null) {
                        Date now = new Date();
                        alert.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now));
                        alert.setTime(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now));
                    }

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

        // Local SOS
        SosAlert alert = new SosAlert();
        alert.setTitle("SOS ALERT");
        alert.setDescription(username + " needs help!");
        alert.setStatus("active");

        // Set actual date and time
        Date now = new Date();
        alert.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now));
        alert.setTime(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now));

        sosAlerts.add(0, alert);
        adapter.notifyItemInserted(0);
        activeRecyclerView.scrollToPosition(0);

        // Send SOS to nearby devices
        if (isBound && nearbyService != null) {
            nearbyService.sendSOS(alert.getDescription());
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
