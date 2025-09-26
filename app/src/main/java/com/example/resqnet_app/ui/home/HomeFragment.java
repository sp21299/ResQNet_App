package com.example.resqnet_app.ui.home;

import android.os.Bundle;
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

    private FragmentHomeBinding binding;
    private AlertAdapter alertAdapter; // ✅ keep one adapter

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup RecyclerView
        binding.activeSosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alertAdapter = new AlertAdapter(new ArrayList<>());
        binding.activeSosRecyclerView.setAdapter(alertAdapter);

        // Load saved alerts from Room
        loadAlerts();

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return root;

        // Set toolbar
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Home");
        }

        // Drawer layout
        DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
        NavigationView navView = activity.findViewById(R.id.nav_view);

        // Navigation icon opens drawer
        toolbar.setNavigationOnClickListener(v -> drawer.openDrawer(GravityCompat.START));

        // SOS button click → show confirmation dialog
        binding.sosButton.setOnClickListener(v -> showSosConfirmation());

        // See SOS button click → reload from DB
        binding.seeSos.setOnClickListener(v -> loadAlerts());

        return root;
    }

    private void navigateFragment(Fragment fragment, int menuItemId) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;

        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        NavigationView navView = activity.findViewById(R.id.nav_view);
        navView.setCheckedItem(menuItemId);

        DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showSosConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Send SOS")
                .setMessage("Are you sure you want to send an emergency alert?")
                .setPositiveButton("Yes", (dialog, which) -> sendSosAlert())
                .setNegativeButton("No", null)
                .show();
    }

    private void sendSosAlert() {
        String userName = "Shravani Patil";
        String userPhone = "+91 9876543210";
        String message = userName + " needs help! Contact: " + userPhone;
        String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());

        Alert newAlert = new Alert("SOS Alert!", message, timestamp);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.alertDao().insert(newAlert);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "SOS alert saved!", Toast.LENGTH_SHORT).show();
                loadAlerts(); // ✅ refresh list instantly
            });
        }).start();
    }

    private void loadAlerts() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<Alert> alerts = db.alertDao().getAllAlerts();

            requireActivity().runOnUiThread(() -> alertAdapter.updateData(alerts));
        }).start();
    }
}
