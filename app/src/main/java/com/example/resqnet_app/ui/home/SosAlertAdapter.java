package com.example.resqnet_app.ui.home;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.example.resqnet_app.data.local.entity.SosAlert;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SosAlertAdapter extends RecyclerView.Adapter<SosAlertAdapter.SosAlertViewHolder> {

    private final List<SosAlert> sosAlertList;
    private final OnAlertActionListener listener;
    private final AppDatabase appDatabase;
    private final FirebaseFirestore firestore;

    public interface OnAlertActionListener {
        void onHelp(SosAlert sosAlert);
        void onAcknowledge(SosAlert sosAlert);
        void onLocationClick(SosAlert sosAlert);
    }

    public SosAlertAdapter(List<SosAlert> sosAlertList, OnAlertActionListener listener,
                           AppDatabase appDatabase) {
        this.sosAlertList = sosAlertList;
        this.listener = listener;
        this.appDatabase = appDatabase;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public SosAlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sos_alert, parent, false);
        return new SosAlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SosAlertViewHolder holder, int position) {
        SosAlert alert = sosAlertList.get(position);

        holder.alertTitle.setText(alert.getTitle() != null ? alert.getTitle() : "SOS ALERT");
        holder.username.setText(alert.getDescription() != null ? alert.getDescription() : "Username needs help");
        holder.date.setText(alert.getDate() + " " + alert.getTimestamp());

        holder.location.setText(alert.getLocationText());
        holder.location.setOnClickListener(v -> listener.onLocationClick(alert));

        holder.helpButton.setEnabled(!"helping".equals(alert.getStatus()));
        holder.ackButton.setEnabled(!alert.isAcknowledged());

        holder.helpButton.setOnClickListener(v -> {
            listener.onHelp(alert);
            alert.setStatus("helping");
            holder.helpButton.setEnabled(false);

            // Update Room DB
            new Thread(() -> appDatabase.sosAlertDao().update(alert)).start();

            // Sync only changed fields to Firestore
            syncToFirestore(alert, holder.itemView);
        });

        holder.ackButton.setOnClickListener(v -> {
            listener.onAcknowledge(alert);
            alert.setAcknowledged(true);
            holder.ackButton.setEnabled(false);

            // Update Room DB
            new Thread(() -> appDatabase.sosAlertDao().update(alert)).start();

            // Sync only changed fields to Firestore
            syncToFirestore(alert, holder.itemView);
        });
    }

    private void syncToFirestore(SosAlert alert, View itemView) {
        if (alert.getUuid() == null || alert.getUuid().isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();

        // Update only changed fields
        if (!"helping".equals(alert.previousStatus) && "helping".equals(alert.getStatus())) {
            updates.put("status", alert.getStatus());
        }

        if (!alert.previousAcknowledged && alert.isAcknowledged()) {
            updates.put("isAcknowledged", true);
        }

        if (updates.isEmpty()) return; // Nothing to update

        firestore.collection("sos_alerts")
                .document(alert.getUuid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    alert.setSynced(true);
                    alert.previousStatus = alert.getStatus();
                    alert.previousAcknowledged = alert.isAcknowledged();

                    new Thread(() -> appDatabase.sosAlertDao().update(alert)).start();
                })
                .addOnFailureListener(e -> {
                    alert.setSynced(false);
                    Toast.makeText(itemView.getContext(),
                            "Failed to sync SOS to Firestore", Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreSync", "Error updating alert", e);
                });
    }

    @Override
    public int getItemCount() {
        return sosAlertList.size();
    }

    static class SosAlertViewHolder extends RecyclerView.ViewHolder {
        TextView alertTitle, username, location, date;
        Button helpButton, ackButton;

        public SosAlertViewHolder(@NonNull View itemView) {
            super(itemView);
            alertTitle = itemView.findViewById(R.id.sos_alert);
            username = itemView.findViewById(R.id.sos_username);
            location = itemView.findViewById(R.id.sos_location);
            date = itemView.findViewById(R.id.sos_date);
            helpButton = itemView.findViewById(R.id.button_help);
            ackButton = itemView.findViewById(R.id.button_ack);
        }
    }
}
