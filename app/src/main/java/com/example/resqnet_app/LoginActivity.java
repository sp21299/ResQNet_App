package com.example.resqnet_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText loginEmail, loginPassword;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_Password);
        Button loginButton = findViewById(R.id.login);
        TextView registerRedirect = findViewById(R.id.RegisterRedirectText);

        // Check if user is already logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, home_activity.class));
            finish();
            return;
        }

        // Show toast from RegisterActivity if available
        String toastMsg = getIntent().getStringExtra("showToast");
        if (toastMsg != null) {
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
        }

        // Redirect to RegisterActivity
        registerRedirect.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        // Login button click
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (validateEmail(email) && validatePassword(password)) {
                loginUser(email, password);
            }
        });
    }

    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();

                            db.collection("Users").document(userId)
                                    .get()
                                    .addOnSuccessListener(document -> {
                                        if (document.exists()) {
                                            String name = document.getString("name");

                                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                            prefs.edit().putString("username", name).apply();

                                            Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_LONG).show();

                                            startActivity(new Intent(LoginActivity.this,sidebar_menu.class));
                                            finish();
                                        } else {
                                            Toast.makeText(LoginActivity.this, "User profile not found!", Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(LoginActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                    );
                        }
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(LoginActivity.this,
                                "Login Failed: " + (e != null ? e.getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            loginEmail.setError("Email can't be empty");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError("Invalid Email");
            return false;
        }
        loginEmail.setError(null);
        return true;
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            loginPassword.setError("Password can't be empty");
            return false;
        } else if (password.length() < 6) {
            loginPassword.setError("Password must be at least 6 characters");
            return false;
        }
        loginPassword.setError(null);
        return true;
    }
}
