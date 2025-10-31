package com.example.resqnet_app.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.resqnet_app.Adapter.AlertAdapter;
import com.example.resqnet_app.R;
import com.example.resqnet_app.data.Alert;
import com.example.resqnet_app.data.AppDatabase;
import com.example.resqnet_app.databinding.FragmentHomeBinding;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private AlertAdapter alertAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // 1) Inflate binding first
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 2) Setup RecyclerView and adapter BEFORE loading DB
        binding.activeSosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alertAdapter = new AlertAdapter(new ArrayList<>());
        binding.activeSosRecyclerView.setAdapter(alertAdapter);

        // 3) Load saved alerts from Room
        loadAlerts();

        // 4) Toolbar & drawer (defensive null-checks)
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            Toolbar toolbar = activity.findViewById(R.id.toolbar);
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("Home");
            }

            DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
            NavigationView navView = activity.findViewById(R.id.nav_view);
            toolbar.setNavigationOnClickListener(v -> {
                if (drawer != null) drawer.openDrawer(GravityCompat.START);
            });
        } else {
            Log.w(TAG, "Activity is null in onCreateView");
        }

        // 5) Hook up SOS button & See SOS button
        binding.sosButton.setOnClickListener(v -> showSosConfirmation());
        binding.seeSos.setOnClickListener(v -> loadAlerts());

        return root;
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
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

        String userName = "Shravani Patil";
        String userPhone = "+91 9876543210";
        String message = userName + " needs help! Contact: " + userPhone;
        String timestamp = getCurrentTimestamp();

        Alert newAlert = new Alert("SOS Alert!", message, timestamp);

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                db.alertDao().insert(newAlert);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "SOS alert saved!", Toast.LENGTH_SHORT).show();
                    loadAlerts();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving SOS alert", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to save alert: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();
    }

    private void loadAlerts() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<Alert> alerts = db.alertDao().getAllAlerts();

                requireActivity().runOnUiThread(() -> {
                    if (alertAdapter != null) {
                        alertAdapter.updateData(alerts);
                    } else {
                        Log.w(TAG, "alertAdapter is null when updating data");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading alerts from DB", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to load alerts: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release binding to avoid leaks
        binding = null;
    }
}
