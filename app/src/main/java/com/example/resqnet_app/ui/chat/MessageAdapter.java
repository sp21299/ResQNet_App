package com.example.resqnet_app.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.entity.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    private final List<Message> items;

    public MessageAdapter(List<Message> items) { this.items = items; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Message m = items.get(position);
        holder.sender.setText(m.getSenderName());
        holder.content.setText(m.getContent());
        // simple visual: right/left alignment could be added but keep simple
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView sender, content;
        VH(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.msg_sender);
            content = itemView.findViewById(R.id.msg_content);
        }
    }
}
