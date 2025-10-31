package com.example.resqnet_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FlashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 1000; // 1 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_flash);

        ImageView logo = findViewById(R.id.logo);

        // Load and start the animation
        Animation fadeZoom = AnimationUtils.loadAnimation(this, R.anim.fade_zoom);
        logo.startAnimation(fadeZoom);
        // Delay for 3 seconds, then open MainActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(FlashActivity.this, home_activity.class);
            startActivity(intent);
            finish();
        }, SPLASH_TIME);
    }
}