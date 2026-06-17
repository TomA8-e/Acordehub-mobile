package com.example.acordehub.ui;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.databinding.ItemMatchCandidateBinding;
import com.example.acordehub.match.MatchCandidate;
import com.example.acordehub.modelos.UserModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class MatchCandidateAdapter extends RecyclerView.Adapter<MatchCandidateAdapter.MatchCandidateViewHolder> {

    public interface OnCandidateActionListener {
        void onInterestClick(MatchCandidate candidate);
    }

    private final List<MatchCandidate> candidates = new ArrayList<>();
    private final OnCandidateActionListener listener;
    private String actionLoadingUid;

    public MatchCandidateAdapter(OnCandidateActionListener listener) {
        this.listener = listener;
    }

    public void setCandidates(List<MatchCandidate> newCandidates) {
        candidates.clear();
        if (newCandidates != null) candidates.addAll(newCandidates);
        notifyDataSetChanged();
    }

    public void setActionLoadingUid(String uid) {
        actionLoadingUid = uid;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MatchCandidateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMatchCandidateBinding binding = ItemMatchCandidateBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MatchCandidateViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchCandidateViewHolder holder, int position) {
        holder.bind(candidates.get(position));
    }

    @Override
    public int getItemCount() {
        return candidates.size();
    }

    class MatchCandidateViewHolder extends RecyclerView.ViewHolder {

        private final ItemMatchCandidateBinding binding;

        MatchCandidateViewHolder(ItemMatchCandidateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MatchCandidate candidate) {
            UserModel user = candidate.getUser();
            binding.tvCandidateName.setText(valueOrFallback(user.getName(), "Musico"));
            binding.tvCandidateRole.setText(valueOrFallback(user.getRole(), "Musico"));
            binding.tvCandidateLocation.setText(valueOrFallback(user.getLocation(), "Ubicacion no definida"));
            binding.tvCandidateDescription.setText(valueOrFallback(user.getDescription(), "Todavia no agrego descripcion."));
            binding.tvMatchScore.setText(candidate.getScore() + "%");
            binding.tvMatchReason.setText(candidate.getReason());
            binding.tvInitials.setText(buildInitials(user.getName()));
            bindPhoto(user.getPhotoUrl());
            bindChips(user);

            boolean loading = user.getUid() != null && user.getUid().equals(actionLoadingUid);
            binding.btnInterest.setEnabled(!loading);
            binding.btnInterest.setText(loading ? "Enviando..." : "Me interesa");
            binding.btnInterest.setOnClickListener(v -> listener.onInterestClick(candidate));
        }

        private void bindPhoto(String photoUrl) {
            if (photoUrl == null || photoUrl.trim().isEmpty()) {
                Glide.with(binding.ivCandidatePhoto).clear(binding.ivCandidatePhoto);
                binding.ivCandidatePhoto.setVisibility(View.GONE);
                return;
            }

            binding.ivCandidatePhoto.setVisibility(View.VISIBLE);
            Glide.with(binding.ivCandidatePhoto)
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .into(binding.ivCandidatePhoto);
        }

        private void bindChips(UserModel user) {
            binding.chipGenres.removeAllViews();
            List<String> labels = new ArrayList<>();
            addCleanLabels(labels, user.getGenres(), 3);
            if (labels.size() < 3) addCleanLabels(labels, user.getInstruments(), 3 - labels.size());
            if (labels.isEmpty()) labels.add(valueOrFallback(user.getLevel(), "Perfil musical"));

            for (String label : labels) {
                Chip chip = new Chip(binding.chipGenres.getContext());
                chip.setText(label);
                chip.setTextSize(11);
                chip.setTextColor(ContextCompat.getColor(chip.getContext(), R.color.white));
                chip.setChipBackgroundColorResource(R.color.black_soft);
                chip.setChipMinHeight(28);
                chip.setCloseIconVisible(false);
                chip.setCheckable(false);
                chip.setClickable(false);
                binding.chipGenres.addView(chip);
            }
        }

        private void addCleanLabels(List<String> target, List<String> source, int maxToAdd) {
            if (source == null || maxToAdd <= 0) return;
            int added = 0;
            for (String value : source) {
                if (value != null && !value.trim().isEmpty() && !target.contains(value.trim())) {
                    target.add(value.trim());
                    added++;
                    if (added == maxToAdd) break;
                }
            }
        }

        private String buildInitials(String name) {
            if (name == null || name.trim().isEmpty()) return "AH";
            String[] parts = name.trim().split("\\s+");
            String first = parts[0].substring(0, 1);
            String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
            return (first + second).toUpperCase();
        }

        private String valueOrFallback(String value, String fallback) {
            return !TextUtils.isEmpty(value) && !value.trim().isEmpty() ? value : fallback;
        }
    }
}
