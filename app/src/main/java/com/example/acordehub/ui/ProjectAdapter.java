package com.example.acordehub.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

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
        }

        private String buildOwnerText(Project project) {
            String ownerName = project.getOwnerName();
            if (ownerName == null || ownerName.trim().isEmpty()) return "Publicado por musico";
            return "Publicado por " + ownerName;
        }
    }
}
