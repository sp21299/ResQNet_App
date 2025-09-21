package com.example.resqnet_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

                            // Store user in Firestore
                            db.collection("Users").document(userId)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        // Save locally
                                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                        prefs.edit().putString("username", name).apply();

                                        // Show toast
                                        Toast.makeText(RegisterActivity.this,
                                                "Account created successfully!", Toast.LENGTH_LONG).show();

                                        // Go to LoginActivity after slight delay so toast shows
                                        editName.postDelayed(() -> {
                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            finish();
                                        }, 500);
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(RegisterActivity.this,
                                                    "Firestore Error: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show());
                        }
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + (e != null ? e.getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
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
