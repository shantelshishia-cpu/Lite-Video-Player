package com.example.video_player_lite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class VideoPlaybackActivity extends AppCompatActivity {

    private VideoView videoView;
    private ImageButton btnPlayPause, btnShare;
    private SeekBar seekBar;
    private TextView txtTime, txtVideoTitle;
    private FrameLayout videoContainer;
    private LinearLayout videoControls;
    private RecyclerView recyclerRelated;
    private View divider, labelMore, playbackFooter;
    private Uri currentVideoUri;

    private Handler handler = new Handler();
    private Runnable updateSeekBar, hideControlsRunnable;
    private ImageButton btnFullscreen;
    private boolean isFullscreen = false;

    private static final int CONTROLS_HIDE_DELAY = 3000;
    private static final String PREFS_NAME = "VideoPlayerPrefs";
    private static final String KEY_FOLDER_URI = "last_folder_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play_back);

        SystemHelper systemHelper = new SystemHelper(this);
        systemHelper.setSystemBars(R.color.black, R.color.black, false);

        // Bind views
        videoView = findViewById(R.id.videoView);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnShare = findViewById(R.id.btnShare);
        seekBar = findViewById(R.id.seekBar);
        txtTime = findViewById(R.id.txtTime);
        txtVideoTitle = findViewById(R.id.txtVideoTitle);
        videoControls = findViewById(R.id.videoControls);
        videoContainer = findViewById(R.id.videoContainer);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        recyclerRelated = findViewById(R.id.recyclerRelated);
        divider = findViewById(R.id.divider);
        labelMore = findViewById(R.id.labelMore);
        playbackFooter = findViewById(R.id.playbackFooter);

        currentVideoUri = getIntent().getData();
        if (currentVideoUri != null) {
            playVideo(currentVideoUri);
        }

        setupVideoControls();
        setupSeekBarUpdater();
        setupAutoHideControls();
        setupRelatedVideos(currentVideoUri);

        btnShare.setOnClickListener(v -> shareVideo(currentVideoUri));
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

        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
    }

    private void playVideo(Uri uri) {
        currentVideoUri = uri;
        videoView.setVideoURI(uri);
        DocumentFile file = DocumentFile.fromSingleUri(this, uri);
        txtVideoTitle.setText(file != null ? file.getName() : "Unknown Video");
        videoView.start();
        setupRelatedVideos(uri); // Refresh related videos list
    }

    private void shareVideo(Uri uri) {
        if (uri == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("video/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Video via"));
    }

    private void setupRelatedVideos(Uri currentUri) {
        recyclerRelated.setLayoutManager(new LinearLayoutManager(this));
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uriString = prefs.getString(KEY_FOLDER_URI, null);
        
        ArrayList<Uri> relatedUris = new ArrayList<>();
        if (uriString != null) {
            Uri treeUri = Uri.parse(uriString);
            DocumentFile folder = DocumentFile.fromTreeUri(this, treeUri);
            if (folder != null && folder.isDirectory()) {
                for (DocumentFile file : folder.listFiles()) {
                    if (file.isFile() && file.getName() != null) {
                        String name = file.getName().toLowerCase();
                        if ((name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi")) 
                            && !file.getUri().equals(currentUri)) {
                            relatedUris.add(file.getUri());
                        }
                    }
                }
            }
        }

        VideoAdapter adapter = new VideoAdapter(relatedUris, this, this::playVideo);
        recyclerRelated.setAdapter(adapter);
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
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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
        ConstraintSet set = new ConstraintSet();
        set.clone(root);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            set.clear(R.id.videoContainer, ConstraintSet.TOP);
            set.clear(R.id.videoContainer, ConstraintSet.BOTTOM);
            set.connect(R.id.videoContainer, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            set.connect(R.id.videoContainer, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            set.setDimensionRatio(R.id.videoContainer, null);

            txtVideoTitle.setVisibility(View.GONE);
            btnShare.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
            labelMore.setVisibility(View.GONE);
            recyclerRelated.setVisibility(View.GONE);
            playbackFooter.setVisibility(View.GONE);

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            set.clear(R.id.videoContainer, ConstraintSet.BOTTOM);
            set.connect(R.id.videoContainer, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            set.connect(R.id.videoContainer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            set.connect(R.id.videoContainer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            set.setDimensionRatio(R.id.videoContainer, "16:9");

            txtVideoTitle.setVisibility(View.VISIBLE);
            btnShare.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
            labelMore.setVisibility(View.VISIBLE);
            recyclerRelated.setVisibility(View.VISIBLE);
            playbackFooter.setVisibility(View.VISIBLE);

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

    private void toggleFullscreen() {
        if (!isFullscreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);
            isFullscreen = true;
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            btnFullscreen.setImageResource(R.drawable.ic_fullscreen);
            isFullscreen = false;
        }
    }
}
