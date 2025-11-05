package com.example.resqnet_app.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resqnet_app.R;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.example.resqnet_app.data.local.entity.Message;
import com.example.resqnet_app.home_activity;
import com.example.resqnet_app.service.NearbyService;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText inputMessage;
    private Button sendButton;
    private TextView connectedDevicesText;

    private ChatAdapter adapter;
    private AppDatabase db;
    private NearbyService nearbyService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewChat);
        inputMessage = view.findViewById(R.id.inputMessage);
        sendButton = view.findViewById(R.id.sendButton);
        connectedDevicesText = view.findViewById(R.id.connectedDeviceName);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        db = AppDatabase.getInstance(requireContext());

        // Observe messages from Room
        db.messageDao().getAllMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.setMessages(messages);
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        });

        // Get NearbyService
        nearbyService = ((home_activity) requireActivity()).getNearbyService();

        if (nearbyService != null) {
            // Observe incoming messages
            nearbyService.receivedMessage.observe(getViewLifecycleOwner(), msg -> {
                if (msg != null && !msg.isEmpty()) {
                    Message received = new Message("Friend", msg, System.currentTimeMillis(), false);
                    new Thread(() -> db.messageDao().insert(received)).start();
                }
            });

            // Observe connected device names
            nearbyService.connectedDevices.observe(getViewLifecycleOwner(), devices -> {
                connectedDevicesText.setText("Connected: " + String.join(", ", devices));
            });
        }

        sendButton.setOnClickListener(v -> sendMessage());

        return view;
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        inputMessage.setText("");
        Message msg = new Message("Me", text, System.currentTimeMillis(), true);
        new Thread(() -> db.messageDao().insert(msg)).start();

        if (nearbyService != null) {
            nearbyService.sendMessage(text);
        } else {
            Toast.makeText(getContext(), "Nearby service not ready", Toast.LENGTH_SHORT).show();
        }
    }
}
