package com.example.resqnet_app.ui.alerts;

import android.annotation.SuppressLint;
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
import com.example.resqnet_app.data.Alert;
import com.example.resqnet_app.model.AlertModel;
import com.example.resqnet_app.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AlertsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private AlertAdapter adapter;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_alerts, container, false);

        recyclerView = root.findViewById(R.id.alertsRecyclerView);
        emptyText = root.findViewById(R.id.emptyAlertsText);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlertAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        fetchAlerts();

        return root;
    }

    private void fetchAlerts() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://yourwebsite.com/api/") // ✅ Replace with your actual base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        apiService.getAlerts().enqueue(new Callback<List<AlertModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<AlertModel>> call,
                                   @NonNull Response<List<AlertModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AlertModel> alerts = response.body();

                    if (!alerts.isEmpty()) {
                        adapter.updateData(AlertModel.toAlertList(alerts));
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyText.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                    }
                } else {
                    recyclerView.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AlertModel>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Failed to fetch alerts", Toast.LENGTH_SHORT).show();
                recyclerView.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }
}
