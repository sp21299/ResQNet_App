package com.example.resqnet_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.maps.model.LatLng;

public class SharedViewModel extends ViewModel {

    // private so only this class can modify it
    public final MutableLiveData<LatLng> sosLocation = new MutableLiveData<>();

    // public getter for observers (MapFragment)
    public LiveData<LatLng> getSosLocation() {
        return sosLocation;
    }

    // public method to update location (HomeFragment)
    public void updateSosLocation(double lat, double lon) {
        sosLocation.postValue(new LatLng(lat, lon));
    }
}
