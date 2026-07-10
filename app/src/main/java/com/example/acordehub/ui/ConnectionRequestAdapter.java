package com.example.acordehub.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acordehub.databinding.ItemConnectionRequestBinding;
import com.example.acordehub.modelos.ConnectionRequest;

import java.util.ArrayList;
import java.util.List;

public class ConnectionRequestAdapter extends RecyclerView.Adapter<ConnectionRequestAdapter.RequestViewHolder> {

    public interface OnConnectionActionListener {
        void onAccept(ConnectionRequest request);
        void onReject(ConnectionRequest request);
    }

    private final List<ConnectionRequest> requests = new ArrayList<>();
    private final OnConnectionActionListener listener;
    private final boolean inboxMode;
    private String loadingRequestId;

    public ConnectionRequestAdapter(boolean inboxMode, OnConnectionActionListener listener) {
        this.inboxMode = inboxMode;
        this.listener = listener;
    }

    public void setRequests(List<ConnectionRequest> newRequests) {
        requests.clear();
        if (newRequests != null) requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    public void setLoadingRequestId(String requestId) {
        loadingRequestId = requestId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConnectionRequestBinding binding = ItemConnectionRequestBinding.inflate(
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
        private final ItemConnectionRequestBinding binding;

        RequestViewHolder(ItemConnectionRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ConnectionRequest request) {
            boolean accepted = "accepted".equals(request.getStatus());
            String name = accepted ? request.getTargetName() : (inboxMode ? request.getRequesterName() : request.getTargetName());
            String role = accepted ? request.getTargetRole() : (inboxMode ? request.getRequesterRole() : request.getTargetRole());

            binding.tvConnectionName.setText(valueOrFallback(name, "Usuario"));
            binding.tvConnectionDetail.setText(accepted
                    ? valueOrFallback(name, "El perfil") + " acepto tu solicitud"
                    : "Quiere conectar contigo" + roleSuffix(role));
            binding.tvConnectionStatus.setText(accepted ? "Aceptada" : "Pendiente");
            binding.actionsContainer.setVisibility(inboxMode && !accepted ? View.VISIBLE : View.GONE);

            boolean loading = request.getId() != null && request.getId().equals(loadingRequestId);
            binding.btnAcceptConnection.setEnabled(!loading);
            binding.btnRejectConnection.setEnabled(!loading);
            binding.btnAcceptConnection.setText(loading ? "..." : "Aceptar");
            binding.btnRejectConnection.setText(loading ? "..." : "Rechazar");
            binding.btnAcceptConnection.setOnClickListener(v -> listener.onAccept(request));
            binding.btnRejectConnection.setOnClickListener(v -> listener.onReject(request));
        }
    }

    private String roleSuffix(String role) {
        return role != null && !role.trim().isEmpty() ? " (" + role.trim() + ")" : "";
    }

    private String valueOrFallback(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }
}
