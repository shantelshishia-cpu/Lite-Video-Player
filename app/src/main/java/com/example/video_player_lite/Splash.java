package com.example.video_player_lite;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_splash);
        
        SystemHelper systemHelper = new SystemHelper(this);
        systemHelper.setSystemBars(R.color.black, R.color.black, false);

        View logo = findViewById(R.id.imgLogo);
        View name = findViewById(R.id.txtAppName);
        View footer = findViewById(R.id.footer);

        // Enhanced Sleek Animation
        // Logo: Scale and Fade in with overshoot
        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .start();

        // Text: Slide up and Fade in
        name.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(800)
                .setStartDelay(500)
                .start();

        // Footer: Simple Fade in
        footer.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(1200)
                .start();

        // Hide status bar & navigation bar (true fullscreen)
        WindowInsetsControllerCompat controller =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());

        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }, 3000);
    }
}
