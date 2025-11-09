package com.example.resqnet_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.resqnet_app.data.local.dao.UserDao;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.example.resqnet_app.data.local.entity.User;
import com.example.resqnet_app.utils.UserSessionManager;

public class profile_activity extends AppCompatActivity {

    private EditText profile_name, email_tv, birth_tv, mobile_tv, address;
    private Button edit_button;
    private AppDatabase db;
    private UserDao userDao;
    private User currentUser;
    private UserSessionManager sessionManager;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Initialize views
        profile_name = findViewById(R.id.user_name);
        email_tv = findViewById(R.id.user_email);
        birth_tv = findViewById(R.id.user_birth);
        mobile_tv = findViewById(R.id.user_phone);
        address = findViewById(R.id.user_address);
        edit_button = findViewById(R.id.editbutton);

        // Disable editing initially
        setFieldsEditable(false);

        // Initialize Room DB
        db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        // Initialize Session Manager
        sessionManager = new UserSessionManager(this);
        String loggedInUsername = sessionManager.getUsername();

        Button logoutButton = findViewById(R.id.logout_button);

        logoutButton.setOnClickListener(v -> {
            // Clear session
            sessionManager.clearSession();

            // Go back to login screen
            Intent intent = new Intent(profile_activity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Fetch logged-in user from Room
        new Thread(() -> {
            currentUser = userDao.getUserByUsername(loggedInUsername);
            if (currentUser != null) {
                runOnUiThread(() -> showUserData(currentUser));
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show());
            }
        }).start();

        // Edit / Save button logic
        edit_button.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "User data not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            if (edit_button.getText().equals("Edit Profile")) {
                // Enable editing
                setFieldsEditable(true);
                edit_button.setText("Save");
            } else {
                // Save updates
                currentUser.setName(profile_name.getText().toString());
                currentUser.setEmail(email_tv.getText().toString());
                currentUser.setBirth(birth_tv.getText().toString());
                currentUser.setMobile(mobile_tv.getText().toString());
                currentUser.setAddress(address.getText().toString());

                new Thread(() -> {
                    userDao.updateUser(currentUser);
                    runOnUiThread(() -> {
                        Toast.makeText(profile_activity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                        setFieldsEditable(false);
                        edit_button.setText("Edit Profile");
                    });
                }).start();
            }
        });

        // Handle window insets (status/navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Enable or disable EditText fields
    private void setFieldsEditable(boolean editable) {
        profile_name.setEnabled(editable);
        email_tv.setEnabled(editable);
        birth_tv.setEnabled(editable);
        mobile_tv.setEnabled(editable);
        address.setEnabled(editable);
    }

    // Populate profile screen with user data
    private void showUserData(User user) {
        profile_name.setText(user.getName());
        email_tv.setText(user.getEmail());
        birth_tv.setText(user.getBirth());
        mobile_tv.setText(user.getMobile());
        address.setText(user.getAddress());
    }
}
