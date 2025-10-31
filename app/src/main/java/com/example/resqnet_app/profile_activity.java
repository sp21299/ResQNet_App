package com.example.resqnet_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.resqnet_app.data.local.entity.UserEntity;

public class profile_activity extends AppCompatActivity {

    EditText profile_name, email_tv, birth_tv, mobile_tv, address;

    Button edit_button;
    private AppDatabase db;
    private UserDao userDao;
    private UserEntity currentUser;
    @SuppressLint("WrongViewCast")
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
        address = findViewById(R.id.user_address);
        edit_button = findViewById(R.id.editbutton);

        // Disable editing initially
        setFieldsEditable(false);

        // Initialize Room DB
        db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        // Load user from DB (for example, user with id=1)
        new Thread(() -> {
            currentUser = userDao.getUserById(1); // replace 1 with actual logged-in user ID
            if (currentUser != null) {
                runOnUiThread(() -> showUserData(currentUser));
            }
        }).start();

        // Handle edit/save button
        edit_button.setOnClickListener(v -> {
            if (edit_button.getText().equals("Edit Profile")) {
                // Enable editing
                setFieldsEditable(true);
                edit_button.setText("Save");

                Toast.makeText(this, "gjggjgjgjgj", Toast.LENGTH_SHORT).show();

                //Log.e("Clicked..",currentUser.toString());
                if (currentUser == null) {
                    currentUser.setName(profile_name.getText().toString());
                    currentUser.setEmail(email_tv.getText().toString());
                    currentUser.setBirth(birth_tv.getText().toString());
                    currentUser.setMobile(mobile_tv.getText().toString());
                    currentUser.setAddress(address.getText().toString());

                    // Update in Room DB
                    new Thread(() -> userDao.updateUser(currentUser)).start();

                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();

                    // Disable editing again
                    setFieldsEditable(false);
                    edit_button.setText("Edit Profile");
                }
            } else {
                // Save updates


            }
        });

        // Handle window insets (status/navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Load user data
//        showUserData(currentUser);
    }

    // Enable or disable EditText fields
    private void setFieldsEditable(boolean editable) {
        profile_name.setEnabled(editable);
        email_tv.setEnabled(editable);
        birth_tv.setEnabled(editable);
        mobile_tv.setEnabled(editable);
        address.setEnabled(editable);
    }

    private void showUserData(UserEntity user) {
        profile_name.setText(user.getName());
        email_tv.setText(user.getEmail());
        birth_tv.setText(user.getBirth());
        mobile_tv.setText(user.getMobile());
        address.setText(user.getAddress());
    }


}
