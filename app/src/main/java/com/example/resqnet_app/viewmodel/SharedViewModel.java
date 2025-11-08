package com.example.resqnet_app.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.maps.model.LatLng;

public class SharedViewModel extends ViewModel {
    public MutableLiveData<LatLng> sosLocation = new MutableLiveData<>();
}
