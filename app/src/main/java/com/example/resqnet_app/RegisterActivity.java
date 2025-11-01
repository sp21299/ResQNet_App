package com.example.resqnet_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.resqnet_app.data.local.entity.User;
import com.example.resqnet_app.data.local.dao.UserDao;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPassword;
    private Button btnRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private AppDatabase localDb;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Room Database
        localDb = AppDatabase.getInstance(this);
        userDao = localDb.userDao();

        // Initialize UI elements
        editName = findViewById(R.id.name);
        editEmail = findViewById(R.id.email);
        editPassword = findViewById(R.id.password);
        btnRegister = findViewById(R.id.create_account);
        TextView loginRedirect = findViewById(R.id.loginRedirectText);

        // Redirect to LoginActivity
        loginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        // Register button click
        btnRegister.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (validateName(name) && validateEmail(email) && validatePassword(password)) {
                registerUser(name, email, password);
            }
        });
    }

    private void registerUser(String name, String email, String password) {
        btnRegister.setEnabled(false);

        if (isOnline()) {
            // 🔹 ONLINE: Register in Firebase + Save locally
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        btnRegister.setEnabled(true);

                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();

                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("name", name);
                                userMap.put("email", email);
                                userMap.put("uid", userId);

                                db.collection("Users").document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener(aVoid -> {
                                            // ✅ Save locally as synced
                                            new Thread(() -> {
                                                User localUser = new User();
                                                localUser.setName(name);
                                                localUser.setEmail(email);
                                                localUser.password = password;
                                                localUser.isSynced = true;
                                                userDao.insert(localUser);
                                            }).start();

                                            // Store username locally for quick access
                                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                            prefs.edit().putString("username", name).apply();

                                            Toast.makeText(this, "Registered Online & Saved Locally", Toast.LENGTH_LONG).show();
                                            clearInputFields();

                                            startActivity(new Intent(this, LoginActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        } else {
                            Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // 🔸 OFFLINE: Save only in Room Database
            new Thread(() -> {
                User localUser = new User();
                localUser.name = name;
                localUser.email = email;
                localUser.password = password;
                localUser.isSynced = false;
                userDao.insert(localUser);
            }).start();

            btnRegister.setEnabled(true);
            Toast.makeText(this, "Offline — user saved locally", Toast.LENGTH_SHORT).show();
            // ✅ Clear fields after offline registration
            clearInputFields();
        }
    }

    // 🔹 Clear input fields
    private void clearInputFields() {
        editName.setText("");
        editEmail.setText("");
        editPassword.setText("");
    }
    // 🔹 Helper method: Check internet connectivity
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    // Validation methods
    private boolean validateName(String name) {
        if (name.isEmpty()) {
            editName.setError("Name can't be empty");
            return false;
        }
        editName.setError(null);
        return true;
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            editEmail.setError("Email can't be empty");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Invalid email format");
            return false;
        }
        editEmail.setError(null);
        return true;
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            editPassword.setError("Password can't be empty");
            return false;
        } else if (password.length() < 6) {
            editPassword.setError("Password must be at least 6 characters");
            return false;
        }
        editPassword.setError(null);
        return true;
    }
}
