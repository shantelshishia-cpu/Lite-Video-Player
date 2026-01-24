package com.example.video_player_lite;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.documentfile.provider.DocumentFile;

import android.view.View;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class VideoPlaybackActivity extends AppCompatActivity {

    private VideoView videoView;
    private ImageButton btnPlayPause;
    private SeekBar seekBar;
    private TextView txtTime, txtVideoTitle;
    private FrameLayout videoContainer;
    private LinearLayout videoControls;

    private Handler handler = new Handler();
    private Runnable updateSeekBar, hideControlsRunnable;
    private ImageButton btnFullscreen;
    private boolean isFullscreen = false;

    private static final int CONTROLS_HIDE_DELAY = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play_back);

        SystemHelper systemHelper = new SystemHelper(this);
        systemHelper.setSystemBars(R.color.black,R.color.black,false);

        // SAF permission
        getIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Bind views
        videoView = findViewById(R.id.videoView);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        seekBar = findViewById(R.id.seekBar);
        txtTime = findViewById(R.id.txtTime);
        txtVideoTitle = findViewById(R.id.txtVideoTitle);
        videoControls = findViewById(R.id.videoControls);
        videoContainer = findViewById(R.id.videoContainer);
        btnFullscreen = findViewById(R.id.btnFullscreen);

        Uri videoUri = getIntent().getData();
        if (videoUri != null) {
            videoView.setVideoURI(videoUri);

            // Optional: display filename as title
            DocumentFile file = DocumentFile.fromSingleUri(this, videoUri);
            txtVideoTitle.setText(file != null ? file.getName() : "Unknown Video");
        }

        setupVideoControls();
        setupSeekBarUpdater();
        setupAutoHideControls();

        // Tap anywhere to toggle controls
        videoContainer.setOnClickListener(v -> toggleControls());

        videoView.setOnPreparedListener(mp -> {
            videoView.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            seekBar.setMax(videoView.getDuration());
            handler.post(updateSeekBar);
            scheduleHideControls();
        });

        videoView.setOnCompletionListener(mp -> {
            btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
            seekBar.setProgress(0);
        });

        btnFullscreen.setOnClickListener(v->toggleFullscreen());
    }

    private void setupVideoControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
            } else {
                videoView.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                scheduleHideControls();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                    txtTime.setText(formatTime(progress) + " / " + formatTime(videoView.getDuration()));
                    scheduleHideControls();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupSeekBarUpdater() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (videoView.isPlaying()) {
                    int current = videoView.getCurrentPosition();
                    seekBar.setProgress(current);
                    txtTime.setText(formatTime(current) + " / " + formatTime(videoView.getDuration()));
                }
                handler.postDelayed(this, 500);
            }
        };
    }

    // Auto-hide controls like YouTube
    private void setupAutoHideControls() {
        hideControlsRunnable = () -> videoControls.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> videoControls.setVisibility(View.GONE))
                .start();
    }

    private void scheduleHideControls() {
        videoControls.setVisibility(View.VISIBLE);
        videoControls.setAlpha(1f);
        handler.removeCallbacks(hideControlsRunnable);
        handler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_DELAY);
    }

    private void toggleControls() {
        if (videoControls.getVisibility() == View.VISIBLE) {
            hideControlsRunnable.run();
        } else {
            videoControls.setVisibility(View.VISIBLE);
            videoControls.setAlpha(1f);
            scheduleHideControls();
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        ConstraintLayout root = findViewById(R.id.main);
        FrameLayout videoContainer = findViewById(R.id.videoContainer);

        ConstraintSet set = new ConstraintSet();
        set.clone(root);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            // FULLSCREEN VIDEO
            set.clear(R.id.videoContainer, ConstraintSet.TOP);
            set.clear(R.id.videoContainer, ConstraintSet.BOTTOM);

            set.connect(R.id.videoContainer, ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            set.connect(R.id.videoContainer, ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

            set.setDimensionRatio(R.id.videoContainer, null);

            // Hide system bars
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );

        } else {

            // BACK TO 16:9 PORTRAIT
            set.clear(R.id.videoContainer, ConstraintSet.BOTTOM);

            set.connect(R.id.videoContainer, ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            set.connect(R.id.videoContainer, ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START);
            set.connect(R.id.videoContainer, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END);

            set.setDimensionRatio(R.id.videoContainer, "16:9");

            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        set.applyTo(root);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateSeekBar != null) handler.removeCallbacks(updateSeekBar);
        if (hideControlsRunnable != null) handler.removeCallbacks(hideControlsRunnable);
    }

    private String formatTime(int millis) {
        return String.format(Locale.getDefault(),
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
    }

    /////screen toggle
    private void toggleFullscreen() {
        if (!isFullscreen) {
            // ENTER fullscreen
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );

            btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);
            isFullscreen = true;

        } else {
            // EXIT fullscreen
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

            btnFullscreen.setImageResource(R.drawable.ic_fullscreen);
            isFullscreen = false;
        }
    }



}

