package com.example.resqnet_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.resqnet_app.data.local.entity.SosAlert;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<SosAlert>> sosAlertsLive = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<SosAlert>> getSosAlertsLive() {
        return sosAlertsLive;
    }

    public List<SosAlert> getSosAlerts() {
        return sosAlertsLive.getValue();
    }

    public void addAlert(SosAlert alert) {
        List<SosAlert> currentList = sosAlertsLive.getValue();
        if (currentList == null) currentList = new ArrayList<>();

        // Add to the top
        currentList.add(0, alert);
        sosAlertsLive.setValue(currentList);
    }
}
