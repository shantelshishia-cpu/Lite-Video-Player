package com.example.video_player_lite;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<Uri> videos;
    private List<Uri> videosFull;
    private final Context context;
    private final OnVideoClickListener listener;

    public interface OnVideoClickListener {
        void onVideoClick(Uri videoUri);
    }

    public VideoAdapter(List<Uri> videos, Context context, OnVideoClickListener listener) {
        this.videos = videos;
        this.videosFull = new ArrayList<>(videos);
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<Uri> newList) {
        this.videos = newList;
        this.videosFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String text) {
        List<Uri> filteredList = new ArrayList<>();
        for (Uri uri : videosFull) {
            DocumentFile file = DocumentFile.fromSingleUri(context, uri);
            if (file != null && file.getName() != null && file.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(uri);
            }
        }
        this.videos = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Uri videoUri = videos.get(position);

        DocumentFile file = DocumentFile.fromSingleUri(context, videoUri);
        holder.txtTitle.setText(file != null ? file.getName() : "Unknown");

        Glide.with(context)
                .load(videoUri)
                .thumbnail(0.1f)
                .into(holder.imgThumb);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, videoUri);
            String time = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
            );

            long ms = Long.parseLong(time);
            long minutes = (ms / 1000) / 60;
            long seconds = (ms / 1000) % 60;

            holder.txtDuration.setText(
                    String.format(Locale.getDefault(),
                            "%02d:%02d", minutes, seconds)
            );
        } catch (Exception e) {
            holder.txtDuration.setText("--:--");
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                // Ignore
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoClick(videoUri);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videos == null ? 0 : videos.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDuration;
        ImageView imgThumb;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            imgThumb = itemView.findViewById(R.id.imgThumb);
        }
    }
}
