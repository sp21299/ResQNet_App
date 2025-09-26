package com.example.resqnet_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class profile_activity extends AppCompatActivity {

    TextView profile_name, email_tv, birth_tv, mobile_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Initialize TextViews
        profile_name = findViewById(R.id.user_name);
        email_tv = findViewById(R.id.user_email);
        birth_tv = findViewById(R.id.user_birth); // Make sure you add this TextView in XML
        mobile_tv = findViewById(R.id.user_phone);

        // Handle window insets (status/navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Load user data
        showUserData();
    }

    private void showUserData() {
        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String email = intent.getStringExtra("email");
        String birth = intent.getStringExtra("birth");
        String mobile = intent.getStringExtra("mobile");

        if (name != null) profile_name.setText(name);
        if (email != null) email_tv.setText(email);
        if (birth != null) birth_tv.setText(birth);
        if (mobile != null) mobile_tv.setText(mobile);
    }
}
