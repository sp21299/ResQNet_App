package com.example.resqnet_app.ui.alerts;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.entity.Alert;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private AlertAdapter adapter;
    private FirebaseFirestore db;

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

        db = FirebaseFirestore.getInstance();

        fetchAlertsFromFirestore();

        return root;
    }

    private void fetchAlertsFromFirestore() {
        db.collection("useralerts") // match your Firestore collection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            showEmptyMessage("Failed to load alerts.");
                            return;
                        }

                        if (value != null && !value.isEmpty()) {
                            List<Alert> alertList = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : value) {
                                Alert alert = new Alert();
                                alert.title = doc.getString("title");
                                alert.description = doc.getString("description");
                                alert.location = doc.getString("location");
                                alert.date = doc.getString("date");
                                alert.time = doc.getString("time");
                                alert.uid = doc.getString("uid");
                                alert.createdAt = doc.getTimestamp("createdAt") != null
                                        ? doc.getTimestamp("createdAt").toDate().getTime()
                                        : 0;

                                alertList.add(alert);
                            }

                            updateRecyclerView(alertList);
                        } else {
                            showEmptyMessage("No alerts available.");
                        }
                    }
                });
    }

    private void updateRecyclerView(List<Alert> alerts) {
        if (alerts.isEmpty()) {
            showEmptyMessage("No alerts available.");
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new AlertAdapter(alerts);
            recyclerView.setAdapter(adapter);
        }
    }

    private void showEmptyMessage(String message) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
}
