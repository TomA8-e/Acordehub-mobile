package com.example.acordehub.ui;

import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.databinding.ItemProjectBinding;
import com.example.acordehub.modelos.Project;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private final List<Project> projects = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
    private final OnProjectClickListener onProjectClickListener;
    private MediaPlayer mediaPlayer;
    private String playingProjectKey;

    public ProjectAdapter() {
        this(null);
    }

    public ProjectAdapter(OnProjectClickListener onProjectClickListener) {
        this.onProjectClickListener = onProjectClickListener;
    }

    public void setProjects(List<Project> newProjects) {
        projects.clear();
        if (newProjects != null) projects.addAll(newProjects);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProjectBinding binding = ItemProjectBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ProjectViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        holder.bind(projects.get(position));
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void release() {
        releasePlayer();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {

        private final ItemProjectBinding binding;

        ProjectViewHolder(ItemProjectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Project project) {
            binding.tvProjectTitle.setText(project.getTitle());
            binding.tvProjectOwner.setText(buildOwnerText(project));
            binding.tvProjectDescription.setText(project.getDescription());
            binding.tvProjectGenre.setText(project.getGenre());
            binding.tvProjectDemo.setText(project.hasDemo() ? "Demo disponible" : "Sin demo");
            binding.btnProjectDemoPlay.setVisibility(project.hasDemo() ? View.VISIBLE : View.GONE);
            bindPlayerButton(project);

            if (project.getCreatedAt() != null) {
                binding.tvProjectDate.setText(dateFormat.format(project.getCreatedAt()));
            } else {
                binding.tvProjectDate.setText("Nuevo");
            }

            Glide.with(binding.ivProjectCover)
                    .load(project.getImageUri())
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(binding.ivProjectCover);

            binding.getRoot().setOnClickListener(v -> {
                if (onProjectClickListener != null) {
                    onProjectClickListener.onProjectClick(project);
                }
            });
        }

        private void bindPlayerButton(Project project) {
            String projectKey = getProjectKey(project);
            boolean isPlayingThisProject = projectKey.equals(playingProjectKey)
                    && mediaPlayer != null
                    && mediaPlayer.isPlaying();

            binding.btnProjectDemoPlay.setIconResource(isPlayingThisProject ? R.drawable.ic_stop : R.drawable.ic_play);
            binding.btnProjectDemoPlay.setContentDescription(isPlayingThisProject ? "Detener demo" : "Reproducir demo");
            binding.btnProjectDemoPlay.setEnabled(true);
            binding.btnProjectDemoPlay.setOnClickListener(v -> {
                if (projectKey.equals(playingProjectKey) && mediaPlayer != null) {
                    releasePlayer();
                    notifyDataSetChanged();
                    return;
                }
                playDemo(project, binding);
            });
        }

        private void playDemo(Project project, ItemProjectBinding currentBinding) {
            releasePlayer();
            playingProjectKey = getProjectKey(project);
            currentBinding.tvProjectDemo.setText("Cargando demo...");
            currentBinding.btnProjectDemoPlay.setEnabled(false);

            MediaPlayer player = new MediaPlayer();
            mediaPlayer = player;
            try {
                player.setDataSource(currentBinding.getRoot().getContext(), Uri.parse(project.getDemoUri()));
                player.setOnPreparedListener(preparedPlayer -> {
                    if (mediaPlayer != preparedPlayer || !getProjectKey(project).equals(playingProjectKey)) {
                        preparedPlayer.release();
                        return;
                    }
                    currentBinding.btnProjectDemoPlay.setEnabled(true);
                    preparedPlayer.start();
                    notifyDataSetChanged();
                });
                player.setOnCompletionListener(completedPlayer -> {
                    releasePlayer();
                    notifyDataSetChanged();
                });
                player.setOnErrorListener((errorPlayer, what, extra) -> {
                    Toast.makeText(currentBinding.getRoot().getContext(),
                            "No se pudo reproducir la demo", Toast.LENGTH_SHORT).show();
                    releasePlayer();
                    notifyDataSetChanged();
                    return true;
                });
                player.prepareAsync();
            } catch (Exception e) {
                Toast.makeText(currentBinding.getRoot().getContext(),
                        "No se pudo abrir la demo", Toast.LENGTH_SHORT).show();
                releasePlayer();
                notifyDataSetChanged();
            }
        }

        private String getProjectKey(Project project) {
            if (project.getId() != null && !project.getId().trim().isEmpty()) return project.getId();
            if (project.getDemoUri() != null) return project.getDemoUri();
            return "";
        }

        private String buildOwnerText(Project project) {
            String ownerName = project.getOwnerName();
            if (ownerName == null || ownerName.trim().isEmpty()) return "Publicado por musico";
            return "Publicado por " + ownerName;
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        playingProjectKey = null;
    }

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }
}
