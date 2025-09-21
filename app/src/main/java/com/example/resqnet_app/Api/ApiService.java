package com.example.resqnet_app.Api;

import com.example.resqnet_app.model.AlertModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("alerts") // your endpoint
    Call<List<AlertModel>> getAlerts();
}
