package com.example.acordehub.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.databinding.ActivityPublicProfileBinding;
import com.example.acordehub.modelos.UserModel;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PublicProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_UID = "extra_user_uid";
    public static final String EXTRA_MATCH_SCORE = "extra_match_score";

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_ACCEPTED = "accepted";
    private static final String ROLE_PRODUCER = "Productor";

    private ActivityPublicProfileBinding binding;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserModel currentUser;
    private UserModel profileUser;
    private String targetUid;
    private String requestId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPublicProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        targetUid = getIntent().getStringExtra(EXTRA_USER_UID);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnInterest.setEnabled(false);
        binding.btnInterest.setOnClickListener(v -> sendConnectionRequest());

        if (targetUid == null || targetUid.trim().isEmpty()) {
            Toast.makeText(this, "No pudimos abrir este perfil", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProfile();
    }

    private void loadProfile() {
        String currentUid = getCurrentUid();
        if (currentUid == null) {
            Toast.makeText(this, "Inicia sesion para conectar", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        requestId = buildRequestId(currentUid, targetUid);
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(currentSnapshot -> {
                    currentUser = currentSnapshot.toObject(UserModel.class);
                    if (currentUser != null && isBlank(currentUser.getUid())) currentUser.setUid(currentUid);
                    loadTargetProfile();
                })
                .addOnFailureListener(e -> loadTargetProfile());
    }

    private void loadTargetProfile() {
        db.collection("users").document(targetUid).get()
                .addOnSuccessListener(snapshot -> {
                    profileUser = snapshot.toObject(UserModel.class);
                    if (profileUser == null) {
                        Toast.makeText(this, "Perfil no disponible", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    if (isBlank(profileUser.getUid())) profileUser.setUid(snapshot.getId());
                    bindProfile(profileUser);
                    loadRequestState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No pudimos cargar el perfil", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindProfile(UserModel user) {
        boolean producer = isProducer(user);
        binding.tvProfileName.setText(valueOrFallback(user.getName(), "Usuario"));
        binding.tvProfileRole.setText(producer ? "Productor musical" : valueOrFallback(user.getRole(), "Rol musical"));
        binding.tvProfileLocation.setText(valueOrFallback(user.getLocation(), "Ubicacion no definida"));
        binding.tvAboutTitle.setText(producer ? "Portfolio" : "Sobre este perfil");
        binding.tvDescription.setText(valueOrFallback(user.getDescription(), "Todavia no agrego descripcion."));
        binding.tvTagsTitle.setText(producer ? "Servicios y creditos" : "Musica");
        bindPhoto(user.getPhotoUrl());
        bindTags(user, producer);
    }

    private void bindPhoto(String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            binding.imgProfilePhoto.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }

        Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .into(binding.imgProfilePhoto);
    }

    private void bindTags(UserModel user, boolean producer) {
        binding.chipGroupTags.removeAllViews();
        List<String> labels = new ArrayList<>();
        addValues(labels, user.getGenres());
        if (producer) {
            addValues(labels, user.getProducerServices());
            addValues(labels, user.getProducerCredits());
        } else {
            addValues(labels, user.getInstruments());
            addValue(labels, user.getLevel());
        }
        if (labels.isEmpty()) labels.add(producer ? "Portfolio en construccion" : "Perfil musical");

        for (String label : labels) {
            Chip chip = new Chip(this);
            chip.setText(label);
            chip.setTextSize(11);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
            chip.setChipBackgroundColorResource(R.color.black_soft);
            chip.setCheckable(false);
            chip.setClickable(false);
            binding.chipGroupTags.addView(chip);
        }
    }

    private void loadRequestState() {
        db.collection("connectionRequests").document(requestId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        setInterestButton("Me interesa", true);
                        return;
                    }
                    String status = snapshot.getString("status");
                    if (STATUS_ACCEPTED.equals(status)) {
                        setInterestButton("Conexion aceptada", false);
                    } else {
                        setInterestButton("Solicitud enviada", false);
                    }
                })
                .addOnFailureListener(e -> setInterestButton("Me interesa", true));
    }

    private void sendConnectionRequest() {
        String currentUid = getCurrentUid();
        if (currentUid == null || profileUser == null) return;

        setInterestButton("Enviando...", false);
        Map<String, Object> data = new HashMap<>();
        data.put("requesterUid", currentUid);
        data.put("requesterName", currentUser != null ? currentUser.getName() : "Usuario");
        data.put("requesterRole", currentUser != null ? currentUser.getRole() : "");
        data.put("targetUid", profileUser.getUid());
        data.put("targetName", profileUser.getName());
        data.put("targetRole", profileUser.getRole());
        data.put("status", STATUS_PENDING);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("connectionRequests").document(requestId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setInterestButton("Solicitud enviada", false);
                    Toast.makeText(this, "Solicitud enviada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setInterestButton("Me interesa", true);
                    Toast.makeText(this, "No pudimos enviar la solicitud", Toast.LENGTH_SHORT).show();
                });
    }

    private void setInterestButton(String text, boolean enabled) {
        binding.btnInterest.setText(text);
        binding.btnInterest.setEnabled(enabled);
    }

    private String buildRequestId(String currentUid, String targetUid) {
        return currentUid + "_" + targetUid;
    }

    private String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    private boolean isProducer(UserModel user) {
        return user != null && user.getRole() != null && user.getRole().trim().equalsIgnoreCase(ROLE_PRODUCER);
    }

    private void addValues(List<String> labels, List<String> values) {
        if (values == null) return;
        for (String value : values) addValue(labels, value);
    }

    private void addValue(List<String> labels, String value) {
        if (value == null || value.trim().isEmpty()) return;
        String clean = value.trim();
        if (!labels.contains(clean)) labels.add(clean);
    }

    private String valueOrFallback(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
