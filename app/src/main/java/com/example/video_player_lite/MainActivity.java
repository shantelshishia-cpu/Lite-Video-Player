package com.example.video_player_lite;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


    public class MainActivity extends AppCompatActivity {

        private static final int PICK_FOLDER = 1001;

        RecyclerView recyclerView;
        FloatingActionButton fab;
        VideoAdapter adapter;
        ArrayList<Uri> videoUris;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);

            recyclerView = findViewById(R.id.recyclerVideos);
            fab = findViewById(R.id.fabOpenFolder);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setItemAnimator(new DefaultItemAnimator());

            videoUris = new ArrayList<>();

            adapter = new VideoAdapter(videoUris, this, uri -> {
                Intent intent = new Intent(this,VideoPlaybackActivity.class);
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
                    loadVideos(treeUri);
                }
            }
        }

        private void loadVideos(Uri treeUri) {
            DocumentFile folder = DocumentFile.fromTreeUri(this, treeUri);
            if (folder == null || !folder.isDirectory()) return;

            videoUris.clear();

            for (DocumentFile file : folder.listFiles()) {
                if (file.isFile() && file.getName() != null) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi")) {
                        videoUris.add(file.getUri());
                    }
                }
            }

            adapter.notifyDataSetChanged();
            Toast.makeText(this,
                    "Loaded " + videoUris.size() + " videos",
                    Toast.LENGTH_SHORT).show();
        }
    }
