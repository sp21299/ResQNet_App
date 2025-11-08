package com.example.resqnet_app.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.entity.Message;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        // Sender
        holder.sender.setText(message.getSenderName() != null ? message.getSenderName() : "Unknown");

        // Message content
        holder.content.setText(message.getContent() != null ? message.getContent() : "");

        // Timestamp (assuming Message entity has a 'createdAt' field in milliseconds)
        if (message.getTimestamp() > 0) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(message.getTimestamp());
            holder.time.setText(time);
        } else {
            holder.time.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView sender, content, time;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.textSender);
            content = itemView.findViewById(R.id.textMessage);
            time = itemView.findViewById(R.id.textTime);
        }
    }
}
