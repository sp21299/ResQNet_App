package com.example.resqnet_app.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.resqnet_app.R;
import com.example.resqnet_app.viewmodel.SharedViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SharedViewModel sharedViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Observe updated SOS location from SharedViewModel
        sharedViewModel.sosLocation.observe(getViewLifecycleOwner(), latLng -> {
            if (latLng != null && mMap != null) { // use mMap here
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title("SOS Location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
            }
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Show last known SOS location if available
        LatLng current = sharedViewModel.sosLocation.getValue();
        if (current != null) {
            mMap.addMarker(new MarkerOptions().position(current).title("SOS Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 16f));
        }
    }
}
