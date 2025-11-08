package com.example.resqnet_app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.entity.SosAlert;

import java.util.List;

public class SosAlertAdapter extends RecyclerView.Adapter<SosAlertAdapter.SosAlertViewHolder> {

    private final List<SosAlert> sosAlertList;
    private final OnAlertActionListener listener;

    public interface OnAlertActionListener {
        void onHelp(SosAlert sosAlert);
        void onAcknowledge(SosAlert sosAlert);
        void onLocationClick(SosAlert sosAlert);
    }

    public SosAlertAdapter(List<SosAlert> sosAlertList, OnAlertActionListener listener) {
        this.sosAlertList = sosAlertList;
        this.listener = listener;
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
        holder.username.setText(alert.getDescription() != null ? alert.getDescription() : "No details");
        holder.location.setText(alert.getLocationText());
        holder.date.setText(alert.getDate() + " " + alert.getTime());

        holder.helpButton.setOnClickListener(v -> listener.onHelp(alert));
        holder.ackButton.setOnClickListener(v -> listener.onAcknowledge(alert));

        // Only notify listener, do NOT switch tabs
        holder.location.setOnClickListener(v -> {
            listener.onLocationClick(alert);
            Toast.makeText(holder.itemView.getContext(),
                    "Location updated. Switch to Map tab to view.", Toast.LENGTH_SHORT).show();
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
