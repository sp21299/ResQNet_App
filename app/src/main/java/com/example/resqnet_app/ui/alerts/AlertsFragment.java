package com.example.resqnet_app.ui.alerts;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.Adapter.AlertAdapter;
import com.example.resqnet_app.R;

import java.util.ArrayList;


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



        return root;
    }


    }
