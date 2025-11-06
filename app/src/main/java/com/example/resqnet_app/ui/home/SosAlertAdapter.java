package com.example.resqnet_app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.entity.SosAlert;

import java.util.List;

public class SosAlertAdapter extends RecyclerView.Adapter<SosAlertAdapter.SosAlertViewHolder> {

    private final List<SosAlert> sosList;

    public SosAlertAdapter(List<SosAlert> sosList) {
        this.sosList = sosList;
    }

    @NonNull
    @Override
    public SosAlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false); // reuse your item_alert layout
        return new SosAlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SosAlertViewHolder holder, int position) {
        SosAlert alert = sosList.get(position);
        holder.title.setText(alert.title);
        holder.description.setText(alert.description);
        holder.dateTime.setText(alert.date + " " + alert.time);
        holder.location.setText(alert.location);
    }

    @Override
    public int getItemCount() {
        return sosList.size();
    }

    public void addAlert(SosAlert alert) {
        sosList.add(0, alert); // add on top
        notifyItemInserted(0);
    }

    public void updateData(List<SosAlert> newList) {
        sosList.clear();
        sosList.addAll(newList);
        notifyDataSetChanged();
    }

    static class SosAlertViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, dateTime, location;

        public SosAlertViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.alertTitle);
            description = itemView.findViewById(R.id.alertDescription);
            dateTime = itemView.findViewById(R.id.alertTime);
            location = itemView.findViewById(R.id.alertLocation);
        }
    }
}
