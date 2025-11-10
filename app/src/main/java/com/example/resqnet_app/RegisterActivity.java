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

import androidx.appcompat.app.AppCompatActivity;

import com.example.resqnet_app.data.local.dao.UserDao;
import com.example.resqnet_app.data.local.database.AppDatabase;
import com.example.resqnet_app.data.local.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPassword;
    private Button btnRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editName = findViewById(R.id.name);
        editEmail = findViewById(R.id.email);
        editPassword = findViewById(R.id.password);
        btnRegister = findViewById(R.id.create_account);
        TextView loginRedirect = findViewById(R.id.loginRedirectText);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        userDao = AppDatabase.getInstance(this).userDao();

        loginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (validateInputs(name, email, password)) {
                btnRegister.setEnabled(false);
                if (isOnline()) {
                    registerOnline(name, email, password);
                } else {
                    saveOffline(name, email, password);
                }
            }
        });
    }

    // ---------------- Online Registration ----------------
    private void registerOnline(String name, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("name", name);
                            userMap.put("email", email);
                            userMap.put("uid", uid);

                            firestore.collection("Users").document(uid)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        saveUserLocally(name, email, password, true);
                                        saveUsernameLocally(name);
                                        Toast.makeText(this,
                                                "Registered Online & Saved Locally", Toast.LENGTH_LONG).show();
                                        clearFields();
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        saveUserLocally(name, email, password, false);
                                        Toast.makeText(this,
                                                "Firestore error, saved locally only", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ---------------- Offline Registration ----------------
    private void saveOffline(String name, String email, String password) {
        saveUserLocally(name, email, password, false);
        Toast.makeText(this, "Offline — saved locally", Toast.LENGTH_SHORT).show();
        clearFields();
    }

    // ---------------- Helper Methods ----------------
    private void saveUserLocally(String name, String email, String password, boolean isSynced) {
        new Thread(() -> {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setSynced(isSynced);
            userDao.insert(user);
        }).start();
    }

    private void saveUsernameLocally(String name) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putString("username", name).apply();
    }

    private boolean validateInputs(String name, String email, String password) {
        if (name.isEmpty()) {
            editName.setError("Name required");
            return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Valid email required");
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            editPassword.setError("Password ≥ 6 chars");
            return false;
        }
        return true;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
        return false;
    }

    private void clearFields() {
        editName.setText("");
        editEmail.setText("");
        editPassword.setText("");
    }
}
