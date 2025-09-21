package com.example.resqnet_app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.resqnet_app.R;
import com.example.resqnet_app.databinding.FragmentHomeBinding;
import com.example.resqnet_app.ui.alerts.AlertsFragment;
import com.example.resqnet_app.ui.chat.ChatFragment;
import com.example.resqnet_app.ui.map.MapFragment;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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

        // SOS button click
        binding.sosButton.setOnClickListener(v -> {
            // TODO: Handle emergency
        });

        // Grid button navigation
        binding.homeButton.setOnClickListener(v -> navigateFragment(new HomeFragment(), R.id.nav_home));
        binding.alertButton.setOnClickListener(v -> navigateFragment(new AlertsFragment(), R.id.nav_alert));
        binding.mapButton.setOnClickListener(v -> navigateFragment(new MapFragment(), R.id.nav_map));
        binding.chatButton.setOnClickListener(v -> navigateFragment(new ChatFragment(), R.id.nav_chat));

        return root;
    }

    private void navigateFragment(Fragment fragment, int menuItemId) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;

        // Replace fragment
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        // Highlight menu item
        NavigationView navView = activity.findViewById(R.id.nav_view);
        navView.setCheckedItem(menuItemId);

        // Close drawer
        DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
