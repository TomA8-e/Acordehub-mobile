package com.example.acordehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.acordehub.chat.ChatRepository;
import com.example.acordehub.databinding.ActivityNotificationsBinding;
import com.example.acordehub.modelos.ConnectionRequest;
import com.example.acordehub.modelos.ProjectJoinRequest;
import com.example.acordehub.proyectos.ProjectRepository;
import com.example.acordehub.proyectos.ProjectVistaModelo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationsActivity extends AppCompatActivity {

    private ActivityNotificationsBinding binding;
    private ProjectVistaModelo projectVistaModelo;
    private ProjectJoinRequestAdapter joinRequestAdapter;
    private ConnectionRequestAdapter connectionRequestAdapter;
    private final ChatRepository chatRepository = new ChatRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String loadingRequestKey;
    private String loadingConnectionRequestId;
    private boolean hasConnectionNotifications;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        projectVistaModelo = new ViewModelProvider(this).get(ProjectVistaModelo.class);
        binding.btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        observeViewModel();
        setLoading(true);
        projectVistaModelo.escucharSolicitudesRecibidas();
        loadConnectionRequests();
    }

    private void setupRecyclerView() {
        connectionRequestAdapter = new ConnectionRequestAdapter(true, new ConnectionRequestAdapter.OnConnectionActionListener() {
            @Override
            public void onAccept(ConnectionRequest request) {
                respondToConnectionRequest(request, "accepted");
            }

            @Override
            public void onReject(ConnectionRequest request) {
                respondToConnectionRequest(request, "rejected");
            }
        });
        binding.rvConnectionRequests.setAdapter(connectionRequestAdapter);

        joinRequestAdapter = new ProjectJoinRequestAdapter(new ProjectJoinRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(ProjectJoinRequest request) {
                respondToJoinRequest(request, "accepted");
            }

            @Override
            public void onReject(ProjectJoinRequest request) {
                respondToJoinRequest(request, "rejected");
            }
        });
        binding.rvJoinRequests.setAdapter(joinRequestAdapter);
    }

    private void observeViewModel() {
        projectVistaModelo.getJoinRequestsLiveData().observe(this, requests -> {
            setLoading(false);
            boolean hasRequests = requests != null && !requests.isEmpty();
            joinRequestAdapter.setRequests(requests);
            binding.rvJoinRequests.setVisibility(hasRequests && !hasConnectionNotifications ? View.VISIBLE : View.GONE);
            binding.emptyStateContainer.setVisibility(hasRequests || hasConnectionNotifications ? View.GONE : View.VISIBLE);
            if (!hasConnectionNotifications && hasRequests) {
                int count = requests.size();
                binding.tvNotificationsSubtitle.setText(count == 1
                        ? "1 solicitud pendiente para tus proyectos"
                        : count + " solicitudes pendientes para tus proyectos");
            } else {
                binding.tvNotificationsSubtitle.setText("Solicitudes y novedades de tus proyectos");
            }
        });

        projectVistaModelo.getErrorLiveData().observe(this, error -> {
            setLoading(false);
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadConnectionRequests() {
        String uid = getCurrentUid();
        if (uid == null) return;

        db.collection("connectionRequests")
                .whereEqualTo("targetUid", uid)
                .get()
                .addOnSuccessListener(inboxSnapshot -> {
                    java.util.List<ConnectionRequest> requests = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot document : inboxSnapshot.getDocuments()) {
                        ConnectionRequest request = document.toObject(ConnectionRequest.class);
                        if (request != null && "pending".equals(request.getStatus())) {
                            request.setId(document.getId());
                            requests.add(request);
                        }
                    }
                    loadAcceptedConnectionRequests(uid, requests);
                })
                .addOnFailureListener(e -> loadAcceptedConnectionRequests(uid, new java.util.ArrayList<>()));
    }

    private void loadAcceptedConnectionRequests(String uid, java.util.List<ConnectionRequest> requests) {
        db.collection("connectionRequests")
                .whereEqualTo("requesterUid", uid)
                .get()
                .addOnSuccessListener(acceptedSnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot document : acceptedSnapshot.getDocuments()) {
                        ConnectionRequest request = document.toObject(ConnectionRequest.class);
                        if (request != null && "accepted".equals(request.getStatus())) {
                            request.setId(document.getId());
                            requests.add(request);
                        }
                    }
                    bindConnectionRequests(requests);
                })
                .addOnFailureListener(e -> bindConnectionRequests(requests));
    }

    private void bindConnectionRequests(java.util.List<ConnectionRequest> requests) {
        hasConnectionNotifications = requests != null && !requests.isEmpty();
        connectionRequestAdapter.setRequests(requests);
        binding.rvConnectionRequests.setVisibility(hasConnectionNotifications ? View.VISIBLE : View.GONE);
        if (hasConnectionNotifications) {
            binding.rvJoinRequests.setVisibility(View.GONE);
            binding.emptyStateContainer.setVisibility(View.GONE);
            int count = requests.size();
            binding.tvNotificationsSubtitle.setText(count == 1
                    ? "1 solicitud de conexion"
                    : count + " solicitudes de conexion");
        }
    }

    private void respondToConnectionRequest(ConnectionRequest request, String status) {
        if (request == null || request.getId() == null || loadingConnectionRequestId != null) return;

        loadingConnectionRequestId = request.getId();
        connectionRequestAdapter.setLoadingRequestId(request.getId());
        db.collection("connectionRequests").document(request.getId())
                .update("status", status, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> {
                    if ("accepted".equals(status)) {
                        createChatForConnectionRequest(request);
                    } else {
                        clearConnectionLoading();
                        Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show();
                        loadConnectionRequests();
                    }
                })
                .addOnFailureListener(e -> {
                    clearConnectionLoading();
                    Toast.makeText(this, "No pudimos responder la solicitud", Toast.LENGTH_SHORT).show();
                });
    }

    private void createChatForConnectionRequest(ConnectionRequest request) {
        chatRepository.startChatWithUid(request.getRequesterUid(), new ChatRepository.StartChatCallback() {
            @Override
            public void onSuccess(String chatId, String title) {
                clearConnectionLoading();
                Toast.makeText(NotificationsActivity.this, "Solicitud aceptada", Toast.LENGTH_SHORT).show();
                loadConnectionRequests();
                Intent intent = new Intent(NotificationsActivity.this, ChatDetailActivity.class);
                intent.putExtra(ChatDetailActivity.EXTRA_CHAT_ID, chatId);
                intent.putExtra(ChatDetailActivity.EXTRA_CHAT_TITLE, title);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                clearConnectionLoading();
                Toast.makeText(NotificationsActivity.this, "Solicitud aceptada, pero no pudimos abrir el chat", Toast.LENGTH_LONG).show();
                loadConnectionRequests();
            }
        });
    }

    private void respondToJoinRequest(ProjectJoinRequest request, String status) {
        if (request == null || loadingRequestKey != null) return;

        loadingRequestKey = buildRequestKey(request);
        joinRequestAdapter.setLoadingRequestKey(loadingRequestKey);
        projectVistaModelo.responderSolicitud(request, status, new ProjectRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if ("accepted".equals(status)) {
                    createChatForAcceptedRequest(request);
                } else {
                    clearRequestLoading();
                    Toast.makeText(NotificationsActivity.this, "Solicitud rechazada", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                clearRequestLoading();
                Toast.makeText(NotificationsActivity.this, "No pudimos responder la solicitud", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createChatForAcceptedRequest(ProjectJoinRequest request) {
        chatRepository.startChatWithUid(request.getRequesterUid(), new ChatRepository.StartChatCallback() {
            @Override
            public void onSuccess(String chatId, String title) {
                clearRequestLoading();
                Toast.makeText(NotificationsActivity.this, "Solicitud aceptada", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(NotificationsActivity.this, ChatDetailActivity.class);
                intent.putExtra(ChatDetailActivity.EXTRA_CHAT_ID, chatId);
                intent.putExtra(ChatDetailActivity.EXTRA_CHAT_TITLE, title);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                clearRequestLoading();
                Toast.makeText(NotificationsActivity.this,
                        "Solicitud aceptada, pero no pudimos abrir el chat", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.rvJoinRequests.setVisibility(View.GONE);
            binding.rvConnectionRequests.setVisibility(View.GONE);
            binding.emptyStateContainer.setVisibility(View.GONE);
        }
    }

    private void clearRequestLoading() {
        loadingRequestKey = null;
        if (joinRequestAdapter != null) joinRequestAdapter.setLoadingRequestKey(null);
    }

    private void clearConnectionLoading() {
        loadingConnectionRequestId = null;
        if (connectionRequestAdapter != null) connectionRequestAdapter.setLoadingRequestId(null);
    }

    private String buildRequestKey(ProjectJoinRequest request) {
        return request.getProjectId() + "_" + request.getRequesterUid();
    }

    private String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
