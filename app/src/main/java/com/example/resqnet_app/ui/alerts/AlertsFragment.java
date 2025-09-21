package com.example.resqnet_app.ui.alerts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.Adapter.AlertAdapter;
import com.example.resqnet_app.Api.ApiService;
import com.example.resqnet_app.model.AlertModel;
import com.example.resqnet_app.R;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AlertsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_alerts, container, false);

        recyclerView = root.findViewById(R.id.alertsRecyclerView);
        emptyText = root.findViewById(R.id.emptyAlertsText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fetchAlerts();

        return root;
    }

    private void fetchAlerts() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://yourwebsite.com/api/") // replace with your base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getAlerts().enqueue(new Callback<List<AlertModel>>() {
            @Override
            public void onResponse(Call<List<AlertModel>> call, Response<List<AlertModel>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AlertAdapter adapter = new AlertAdapter(response.body());
                    recyclerView.setAdapter(adapter);
                    emptyText.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<AlertModel>> call, Throwable t) {
                Toast.makeText(getContext(), "Failed to fetch alerts", Toast.LENGTH_SHORT).show();
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }
}
