package com.example.acordehub.ui;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.databinding.ActivityProjectDetailBinding;
import com.example.acordehub.modelos.Project;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProjectDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PROJECT_ID = "project_id";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private ActivityProjectDetailBinding binding;
    private Project project;
    private String projectId;
    private MediaPlayer mediaPlayer;
    private boolean isPlayingDemo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        if (isBlank(projectId)) {
            Toast.makeText(this, "No pudimos abrir el proyecto", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnDemoPlay.setOnClickListener(v -> toggleDemo());
        binding.btnJoinProject.setOnClickListener(v -> joinProject());

        loadProject();
    }

    private void loadProject() {
        setJoinLoading(true);
        db.collection("projects").document(projectId).get()
                .addOnSuccessListener(snapshot -> {
                    project = snapshot.toObject(Project.class);
                    if (project == null) {
                        Toast.makeText(this, "El proyecto ya no esta disponible", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    project.setId(snapshot.getId());
                    bindProject();
                    checkJoinState();
                })
                .addOnFailureListener(e -> {
                    setJoinLoading(false);
                    Toast.makeText(this, "No pudimos cargar el proyecto", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindProject() {
        binding.tvProjectTitle.setText(project.getTitle());
        binding.tvProjectOwner.setText(buildOwnerText());
        binding.tvProjectDescription.setText(project.getDescription());
        binding.tvProjectGenre.setText(project.getGenre());
        binding.tvProjectStatus.setText(project.isActive() ? "Activo" : "Pausado");
        binding.tvProjectDate.setText(project.getCreatedAt() != null
                ? dateFormat.format(project.getCreatedAt())
                : "Nuevo");

        Glide.with(binding.ivProjectCover)
                .load(project.getImageUri())
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.ivProjectCover);

        boolean hasDemo = project.hasDemo();
        binding.demoCard.setVisibility(hasDemo ? View.VISIBLE : View.GONE);
    }

    private void checkJoinState() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            configureJoinButton(false, "Inicia sesion para unirte");
            return;
        }

        if (user.getUid().equals(project.getOwnerUid())) {
            configureJoinButton(false, "Es tu proyecto");
            return;
        }

        db.collection("projects").document(projectId)
                .collection("joinRequests").document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        configureJoinButton(false, "Solicitud enviada");
                    } else {
                        configureJoinButton(true, "Unirme al proyecto");
                    }
                })
                .addOnFailureListener(e -> configureJoinButton(true, "Unirme al proyecto"));
    }

    private void joinProject() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || project == null || isBlank(projectId)) {
            Toast.makeText(this, "Inicia sesion para unirte", Toast.LENGTH_SHORT).show();
            return;
        }
        if (user.getUid().equals(project.getOwnerUid())) {
            configureJoinButton(false, "Es tu proyecto");
            return;
        }

        setJoinLoading(true);
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    String profileName = snapshot.getString("name");
                    saveJoinRequest(user, isBlank(profileName) ? user.getDisplayName() : profileName);
                })
                .addOnFailureListener(e -> saveJoinRequest(user, user.getDisplayName()));
    }

    private void saveJoinRequest(FirebaseUser user, String requesterName) {
        Map<String, Object> request = new HashMap<>();
        request.put("projectId", projectId);
        request.put("projectTitle", project.getTitle());
        request.put("ownerUid", project.getOwnerUid());
        request.put("requesterUid", user.getUid());
        request.put("requesterName", isBlank(requesterName) ? "Usuario" : requesterName);
        request.put("requesterEmail", user.getEmail() != null ? user.getEmail() : "");
        request.put("status", "pending");
        request.put("createdAt", FieldValue.serverTimestamp());

        db.collection("projects").document(projectId)
                .collection("joinRequests").document(user.getUid())
                .set(request)
                .addOnSuccessListener(unused -> {
                    configureJoinButton(false, "Solicitud enviada");
                    Toast.makeText(this, "Solicitud enviada al creador", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setJoinLoading(false);
                    Toast.makeText(this, "No pudimos enviar la solicitud: "
                            + valueOrFallback(e.getMessage(), "revisa las reglas de Firestore"), Toast.LENGTH_LONG).show();
                });
    }

    private void toggleDemo() {
        if (project == null || !project.hasDemo()) return;
        if (isPlayingDemo) {
            releasePlayer();
            updateDemoButton(false);
            return;
        }
        playDemo();
    }

    private void playDemo() {
        releasePlayer();
        binding.tvDemoHint.setText("Cargando demo...");
        binding.btnDemoPlay.setEnabled(false);

        MediaPlayer player = new MediaPlayer();
        mediaPlayer = player;
        try {
            player.setDataSource(this, Uri.parse(project.getDemoUri()));
            player.setOnPreparedListener(preparedPlayer -> {
                binding.btnDemoPlay.setEnabled(true);
                preparedPlayer.start();
                updateDemoButton(true);
                binding.tvDemoHint.setText("Reproduciendo demo");
            });
            player.setOnCompletionListener(completedPlayer -> {
                releasePlayer();
                updateDemoButton(false);
            });
            player.setOnErrorListener((errorPlayer, what, extra) -> {
                Toast.makeText(this, "No se pudo reproducir la demo", Toast.LENGTH_SHORT).show();
                releasePlayer();
                updateDemoButton(false);
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir la demo", Toast.LENGTH_SHORT).show();
            releasePlayer();
            updateDemoButton(false);
        }
    }

    private void updateDemoButton(boolean playing) {
        isPlayingDemo = playing;
        binding.btnDemoPlay.setEnabled(true);
        binding.btnDemoPlay.setIconResource(playing ? R.drawable.ic_stop : R.drawable.ic_play);
        binding.btnDemoPlay.setContentDescription(playing ? "Detener demo" : "Reproducir demo");
        if (!playing) {
            binding.tvDemoHint.setText("Escucha la idea antes de unirte");
        }
    }

    private void configureJoinButton(boolean enabled, String text) {
        setJoinLoading(false);
        binding.btnJoinProject.setEnabled(enabled);
        binding.btnJoinProject.setText(text);
        binding.btnJoinProject.setAlpha(enabled ? 1f : 0.62f);
    }

    private void setJoinLoading(boolean loading) {
        binding.progressJoin.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnJoinProject.setEnabled(!loading);
    }

    private String buildOwnerText() {
        String ownerName = project.getOwnerName();
        if (isBlank(ownerName)) return "Publicado por musico";
        return "Publicado por " + ownerName;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String valueOrFallback(String value, String fallback) {
        return isBlank(value) ? fallback : value;
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
        isPlayingDemo = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        binding = null;
    }
}
