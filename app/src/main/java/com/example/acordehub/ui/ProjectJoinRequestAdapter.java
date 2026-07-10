package com.example.acordehub.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acordehub.databinding.ItemProjectJoinRequestBinding;
import com.example.acordehub.modelos.ProjectJoinRequest;

import java.util.ArrayList;
import java.util.List;

public class ProjectJoinRequestAdapter extends RecyclerView.Adapter<ProjectJoinRequestAdapter.RequestViewHolder> {

    private final List<ProjectJoinRequest> requests = new ArrayList<>();
    private final OnRequestActionListener listener;
    private String loadingRequestKey;

    public ProjectJoinRequestAdapter(OnRequestActionListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<ProjectJoinRequest> newRequests) {
        requests.clear();
        if (newRequests != null) requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    public void setLoadingRequestKey(String key) {
        loadingRequestKey = key;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProjectJoinRequestBinding binding = ItemProjectJoinRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(requests.get(position));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {

        private final ItemProjectJoinRequestBinding binding;

        RequestViewHolder(ItemProjectJoinRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ProjectJoinRequest request) {
            binding.tvRequesterName.setText(valueOrFallback(request.getRequesterName(), "Musico"));
            binding.tvRequestProject.setText("Quiere unirse a \"" + valueOrFallback(request.getProjectTitle(), "tu proyecto") + "\"");

            boolean loading = buildRequestKey(request).equals(loadingRequestKey);
            binding.btnAccept.setEnabled(!loading);
            binding.btnReject.setEnabled(!loading);
            binding.btnAccept.setText(loading ? "..." : "Aceptar");
            binding.btnReject.setText(loading ? "..." : "Rechazar");
            binding.btnAccept.setOnClickListener(v -> listener.onAccept(request));
            binding.btnReject.setOnClickListener(v -> listener.onReject(request));
        }
    }

    private String buildRequestKey(ProjectJoinRequest request) {
        return request.getProjectId() + "_" + request.getRequesterUid();
    }

    private String valueOrFallback(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    public interface OnRequestActionListener {
        void onAccept(ProjectJoinRequest request);
        void onReject(ProjectJoinRequest request);
    }
}
