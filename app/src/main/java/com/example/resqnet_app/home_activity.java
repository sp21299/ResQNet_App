package com.example.resqnet_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.resqnet_app.ui.alerts.AlertsFragment;
import com.example.resqnet_app.ui.chat.ChatFragment;
import com.example.resqnet_app.ui.home.HomeFragment;
import com.example.resqnet_app.ui.map.MapFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class home_activity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize the profile icon
        ImageView profileIcon = findViewById(R.id.profileIcon);

        // Set click listener to open ProfileActivity
        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(home_activity.this, profile_activity.class);

            // Pass user data if needed
            intent.putExtra("name", "Shravani Patil");
            intent.putExtra("email", "shravani@example.com");
            intent.putExtra("birth", "01-01-2000");
            intent.putExtra("mobile", "+91 9876543210");

            startActivity(intent);
        });
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.nav_view1); // Make sure this ID is for BottomNavigationView

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            navView.setCheckedItem(R.id.nav_home);
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        // Drawer menu click listener
        navView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (id == R.id.nav_alert) selectedFragment = new AlertsFragment();
            else if (id == R.id.nav_chat) selectedFragment = new ChatFragment();
            else if (id == R.id.nav_map) selectedFragment = new MapFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // Bottom Navigation click listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.bottom_home) selectedFragment = new HomeFragment();
            else if (id == R.id.bottom_alert) selectedFragment = new AlertsFragment();
            else if (id == R.id.bottom_chat) selectedFragment = new ChatFragment();
            else if (id == R.id.bottom_map) selectedFragment = new MapFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }
}
