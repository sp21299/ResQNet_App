package com.example.resqnet_app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.model.AlertModel;
import com.example.resqnet_app.R;

import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private List<AlertModel> alertList;

    public AlertAdapter(List<AlertModel> alertList) {
        this.alertList = alertList;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        AlertModel alert = alertList.get(position);
        holder.title.setText(alert.getTitle());
        holder.message.setText(alert.getMessage());
        holder.timestamp.setText(alert.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, timestamp;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.alertTitle);
            message = itemView.findViewById(R.id.alertMessage);
            timestamp = itemView.findViewById(R.id.alertTimestamp);
        }
    }
}
