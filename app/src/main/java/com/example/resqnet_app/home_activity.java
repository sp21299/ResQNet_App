package com.example.resqnet_app;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.resqnet_app.service.NearbyService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class home_activity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS_NEARBY = 1001;
    private NearbyService nearbyService;
    private DrawerLayout drawerLayout;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NearbyService.NearbyBinder binder = (NearbyService.NearbyBinder) service;
            nearbyService = binder.getService();
            Log.d("home_activity", "NearbyService bound");

            // Start Nearby once service is bound
            if (nearbyService != null) {
                nearbyService.startNearby("ResQNet-" + Build.MODEL);
                Toast.makeText(home_activity.this, "Nearby started ✅", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            nearbyService = null;
            Log.d("home_activity", "NearbyService unbound");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupToolbarAndDrawer();
        setupBottomNavigation();
        setupProfileIcon();

        // Check permissions and bind NearbyService
        ensurePermissionsThenStartNearby();
    }

    private void setupToolbarAndDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.nav_view2);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_home);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.bottom_home1, R.id.bottom_alert1, R.id.bottom_map1, R.id.bottom_chat1
            ).setOpenableLayout(drawerLayout).build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }

    private void setupProfileIcon() {
        ImageView profileIcon = findViewById(R.id.profileIcon);
        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, profile_activity.class);
            intent.putExtra("name", "Shravani Patil");
            intent.putExtra("email", "shravani@example.com");
            intent.putExtra("birth", "01-01-2000");
            intent.putExtra("mobile", "+91 9876543210");
            startActivity(intent);
        });
    }

    private void ensurePermissionsThenStartNearby() {
        List<String> needed = new ArrayList<>();

        // Location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        // Nearby Wi-Fi permission for Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.NEARBY_WIFI_DEVICES")
                    != PackageManager.PERMISSION_GRANTED) needed.add("android.permission.NEARBY_WIFI_DEVICES");
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQ_PERMISSIONS_NEARBY);
        } else {
            bindNearbyService();
        }
    }

    private void bindNearbyService() {
        if (nearbyService == null) {
            Intent intent = new Intent(this, NearbyService.class);
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS_NEARBY) {
            boolean granted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) bindNearbyService();
            else Toast.makeText(this, "Nearby permissions denied ⚠️", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nearbyService != null) nearbyService.stopAll();
        try {
            unbindService(serviceConnection);
        } catch (IllegalArgumentException e) {
            Log.w("home_activity", "Service not bound, cannot unbind", e);
        }
    }

    public NearbyService getNearbyService() {
        return nearbyService;
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (nearbyService != null) {
            nearbyService.receivedMessage.observe(this, msg -> {
                // Handle incoming message (e.g., show toast, update chat UI)
                Toast.makeText(this, "Received: " + msg, Toast.LENGTH_SHORT).show();
            });
        }
    }

}
