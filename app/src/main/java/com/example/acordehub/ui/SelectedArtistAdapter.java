package com.example.acordehub.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.modelos.FavoriteArtist;

import java.util.ArrayList;
import java.util.List;

public class SelectedArtistAdapter extends RecyclerView.Adapter<SelectedArtistAdapter.ViewHolder> {

    private List<FavoriteArtist> artists = new ArrayList<>();
    private final OnRemoveClickListener listener;

    public interface OnRemoveClickListener {
        void onRemoveClick(int position);
    }

    public SelectedArtistAdapter(OnRemoveClickListener listener) {
        this.listener = listener;
    }

    public void setArtists(List<FavoriteArtist> artists) {
        this.artists = artists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_artist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavoriteArtist artist = artists.get(position);
        holder.tvName.setText(artist.getName());
        
        Glide.with(holder.itemView.getContext())
                .load(artist.getImageUrl())
                .circleCrop()
                .placeholder(R.drawable.ic_person_placeholder)
                .into(holder.ivPhoto);

        if (listener != null) {
            holder.btnRemove.setVisibility(View.VISIBLE);
            holder.btnRemove.setOnClickListener(v -> listener.onRemoveClick(position));
        } else {
            holder.btnRemove.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName;
        View btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivArtistPhoto);
            tvName = itemView.findViewById(R.id.tvArtistName);
            btnRemove = itemView.findViewById(R.id.btnRemoveArtist);
        }
    }
}