package com.example.video_player_lite;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_FOLDER = 1001;
    private static final int PERMISSION_REQUEST_CODE = 2002;
    private static final String PREFS_NAME = "VideoPlayerPrefs";
    private static final String KEY_FOLDER_URI = "last_folder_uri";

    RecyclerView recyclerView;
    FloatingActionButton fab;
    VideoAdapter adapter;
    ArrayList<Uri> videoUris;
    Toolbar toolbar;
    TextView txtFolder;
    ProgressBar downloadProgress;
    private String pendingDownloadUrl;

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            downloadProgress.setVisibility(ProgressBar.GONE);
            Toast.makeText(context, "Download Finished!", Toast.LENGTH_SHORT).show();
            // Optionally reload videos if they were downloaded to the currently opened folder
            restoreLastFolder();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        SystemHelper systemHelper = new SystemHelper(this);
        systemHelper.setSystemBars(R.color.black, R.color.black, false);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtFolder = findViewById(R.id.txtFolder);
        downloadProgress = findViewById(R.id.downloadProgress);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar),
                (view, insets) -> {
                    int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    view.setPadding(
                            view.getPaddingLeft(),
                            statusBarHeight,
                            view.getPaddingRight(),
                            view.getPaddingBottom()
                    );
                    return insets;
                });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fabOpenFolder),
                (view, insets) -> {
                    int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    view.setTranslationY(-navBarHeight);
                    return insets;
                });

        recyclerView = findViewById(R.id.recyclerVideos);
        fab = findViewById(R.id.fabOpenFolder);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        videoUris = new ArrayList<>();

        adapter = new VideoAdapter(videoUris, this, uri -> {
            Intent intent = new Intent(this, VideoPlaybackActivity.class);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            );
            startActivityForResult(intent, PICK_FOLDER);
        });

        restoreLastFolder();
        
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void restoreLastFolder() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uriString = prefs.getString(KEY_FOLDER_URI, null);
        if (uriString != null) {
            Uri treeUri = Uri.parse(uriString);
            loadVideos(treeUri);
        }
    }

    private void saveLastFolder(Uri uri) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manu_main, menu);

        // Force icons to show in the overflow menu (the 3 dots)
        if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
            try {
                Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                m.setAccessible(true);
                m.invoke(menu, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search videos...");

        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchEditText != null) {
            searchEditText.setTextColor(Color.WHITE);
            searchEditText.setHintTextColor(Color.LTGRAY);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_download) {
            showDownloadDialog();
            return true;
        } else if (id == R.id.action_exit) {
            showExitConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to quit VividPlayer?")
                .setPositiveButton("Yes, Exit", (dialog, which) -> finish())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        showExitConfirmation();
    }

    private void showDownloadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Download Video");
        builder.setMessage("Enter video URL or link:");

        final EditText input = new EditText(this);
        input.setHint("https://example.com/video.mp4");
        builder.setView(input);

        builder.setPositiveButton("Download", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty() && URLUtil.isValidUrl(url)) {
                checkPermissionAndDownload(url);
            } else {
                Toast.makeText(MainActivity.this, "Invalid URL", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void checkPermissionAndDownload(String url) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingDownloadUrl = url;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }
        startDownload(url);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingDownloadUrl != null) {
                    startDownload(pendingDownloadUrl);
                    pendingDownloadUrl = null;
                }
            } else {
                Toast.makeText(this, "Permission denied. Cannot download video.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startDownload(String url) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Downloading Video");
            request.setDescription("VividPlayer is downloading your video...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            String fileName = URLUtil.guessFileName(url, null, null);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                downloadProgress.setVisibility(ProgressBar.VISIBLE);
                Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                getContentResolver().takePersistableUriPermission(
                        treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                saveLastFolder(treeUri);
                loadVideos(treeUri);
            }
        }
    }

    private void loadVideos(Uri treeUri) {
        DocumentFile folder = DocumentFile.fromTreeUri(this, treeUri);
        if (folder == null || !folder.isDirectory()) return;

        txtFolder.setText("📂 " + folder.getName());
        videoUris.clear();

        for (DocumentFile file : folder.listFiles()) {
            if (file.isFile() && file.getName() != null) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi")) {
                    videoUris.add(file.getUri());
                }
            }
        }

        adapter.updateList(videoUris);
        Toast.makeText(this, "Loaded " + videoUris.size() + " videos", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }
}
