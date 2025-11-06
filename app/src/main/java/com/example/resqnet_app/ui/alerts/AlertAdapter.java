package com.example.resqnet_app.ui.alerts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.entity.Alert;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private List<Alert> alertList;

    public AlertAdapter(List<Alert> alertList) {
        this.alertList = alertList;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alert alert = alertList.get(position);

        holder.titleText.setText(alert.title);
        holder.descriptionText.setText(alert.description);
        holder.locationText.setText(alert.location);

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String formattedTime = sdf.format(new Date(alert.createdAt));
        holder.timeText.setText(formattedTime);

        // You can show date and time fields separately if needed
        holder.dateText.setText(alert.date + " | " + alert.time);
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public void updateData(List<Alert> alerts) {

    }

    public static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, descriptionText, locationText, timeText, dateText;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.alertTitle);
            descriptionText = itemView.findViewById(R.id.alertDescription);
            locationText = itemView.findViewById(R.id.alertLocation);
            timeText = itemView.findViewById(R.id.alertTime);
            dateText = itemView.findViewById(R.id.alertDate);
        }
    }
}
