package com.example.resqnet_app.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.resqnet_app.R;
import com.example.resqnet_app.viewmodel.SharedViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SharedViewModel sharedViewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Observe updated SOS location from SharedViewModel
        sharedViewModel.sosLocation.observe(getViewLifecycleOwner(), latLng -> {
            if (latLng != null && mMap != null) {
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title("SOS Location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
            }
        });

        // My Location button
        Button btnMyLocation = view.findViewById(R.id.btnMyLocation);
        btnMyLocation.setOnClickListener(v -> moveToMyLocation());

        // Refresh button (optional)
        Button btnRefresh = view.findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            // Refresh map if needed
            if (mMap != null) {
                mMap.clear();
                LatLng current = sharedViewModel.sosLocation.getValue();
                if (current != null) {
                    mMap.addMarker(new MarkerOptions().position(current).title("SOS Location"));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 16f));
                }
            }
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Enable my location if permission granted
        enableMyLocation();

        // Show last known SOS location if available
        LatLng current = sharedViewModel.sosLocation.getValue();
        if (current != null) {
            mMap.addMarker(new MarkerOptions().position(current).title("SOS Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 16f));
        }
    }

    private void enableMyLocation() {
        if (getContext() == null || mMap == null) return;

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void moveToMyLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && mMap != null) {
                LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16f));
            } else {
                Toast.makeText(getContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
