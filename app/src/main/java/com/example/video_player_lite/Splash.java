package com.example.video_player_lite;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class Splash extends AppCompatActivity {

    protected void  onCreate(Bundle savedInstanceBundle){
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_splash);
        SystemHelper systemHelper = new SystemHelper(this);
        systemHelper.setSystemBars(R.color.black,R.color.black,false);

        // Hide status bar & navigation bar (true fullscreen)
        WindowInsetsControllerCompat controller =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());

        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        ////delay and move
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            startActivity(new Intent(getApplicationContext(),MainActivity.class));
            finish();
        },2000);


    }
}
