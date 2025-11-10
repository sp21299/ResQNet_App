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
import com.example.resqnet_app.service.SyncService;
import com.example.resqnet_app.utils.UserSessionManager;
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
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        // Initialize UI
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_Password);
        Button loginButton = findViewById(R.id.login);
        TextView registerRedirect = findViewById(R.id.RegisterRedirectText);

        // Check if already logged in (online or offline)
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedInOffline = prefs.getBoolean("isLoggedInOffline", false);
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null || isLoggedInOffline) {
            startActivity(new Intent(LoginActivity.this, home_activity.class));
            finish();
            return;
        }

        // Show toast if redirected from RegisterActivity
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
                if (isConnected()) {
                    // Online login
                    loginUserOnline(email, password);
                } else {
                    // Offline login
                    loginUserOffline(email, password);
                }
            }
        });
    }

    // -------------------- ONLINE LOGIN --------------------
    // -------------------- ONLINE LOGIN --------------------
    private void loginUserOnline(String email, String password) {
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

                                            // Save user data locally in SharedPreferences
                                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putString("username", name);
                                            editor.putBoolean("isLoggedInOffline", false);
                                            editor.apply();

                                            // Save in session manager
                                            UserSessionManager session = new UserSessionManager(LoginActivity.this);
                                            session.saveUsername(name);

                                            // ✅ Save user in Room DB
                                            new Thread(() -> {
                                                AppDatabase roomDb = AppDatabase.getInstance(getApplicationContext());
                                                UserDao userDao = roomDb.userDao();

                                                // Check if user already exists
                                                User existingUser = userDao.getUserByEmail(email);
                                                if (existingUser == null) {
                                                    User newUser = new User();
                                                    newUser.setName(name);
                                                    newUser.setEmail(email);
                                                    newUser.setPassword(password); // storing password in plain text is not secure; ideally hash it
                                                    userDao.insert(newUser);
                                                } else {
                                                    // Update existing user details
                                                    existingUser.setName(name);
                                                    existingUser.setPassword(password);
                                                    userDao.updateUser(existingUser);
                                                }
                                            }).start();

                                            Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_LONG).show();

                                            // Start sync service
                                            Intent syncIntent = new Intent(LoginActivity.this, SyncService.class);
                                            startService(syncIntent);

                                            // Navigate to home activity
                                            Intent intent = new Intent(LoginActivity.this, home_activity.class);
                                            intent.putExtra("openFragment", "home");
                                            startActivity(intent);
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


    // -------------------- OFFLINE LOGIN --------------------
    private void loginUserOffline(String email, String password) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            UserDao userDao = db.userDao();
            User user = userDao.getUserByEmail(email);

            runOnUiThread(() -> {
                if (user != null && user.getPassword().equals(password)) {
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("username", user.getName());
                    editor.putBoolean("isLoggedInOffline", true); // ✅ mark offline login
                    editor.apply();

                    // ✅ Added: also store username in session manager
                    UserSessionManager session = new UserSessionManager(LoginActivity.this);
                    session.saveUsername(user.getName());

                    Toast.makeText(LoginActivity.this, "Welcome (Offline) " + user.getName(), Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(LoginActivity.this, home_activity.class);
                    intent.putExtra("openFragment", "home");
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid email or password (Offline Mode)", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // -------------------- VALIDATION --------------------
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

    // -------------------- INTERNET CHECK --------------------
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }
}
