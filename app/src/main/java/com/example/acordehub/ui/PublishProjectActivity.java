package com.example.acordehub.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.databinding.ActivityPublishProjectBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PublishProjectActivity extends AppCompatActivity {

    private ActivityPublishProjectBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Uri selectedImageUri;
    private Uri selectedDemoUri;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedImageUri = uri;
                binding.ivProjectPreview.clearColorFilter();
                Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(binding.ivProjectPreview);
            });

    private final ActivityResultLauncher<String> demoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedDemoUri = uri;
                Toast.makeText(this, "Demo seleccionada", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPublishProjectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnUploadImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        binding.btnUploadDemo.setOnClickListener(v -> demoPicker.launch("audio/*"));
        binding.btnPublicar.setOnClickListener(v -> publishProject());
    }

    private void publishProject() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Inicia sesion para publicar", Toast.LENGTH_LONG).show();
            return;
        }

        String title = binding.etProjectTitle.getText().toString().trim();
        String description = binding.etProjectDescription.getText().toString().trim();
        String genre = binding.etProjectGenre.getText().toString().trim();

        if (title.isEmpty()) {
            binding.etProjectTitle.setError("Ingresa un titulo");
            return;
        }
        if (description.isEmpty()) {
            binding.etProjectDescription.setError("Ingresa una descripcion");
            return;
        }
        if (genre.isEmpty()) {
            binding.etProjectGenre.setError("Ingresa un genero");
            return;
        }

        setLoading(true);

        Map<String, Object> project = new HashMap<>();
        project.put("ownerUid", user.getUid());
        project.put("ownerName", user.getDisplayName() != null ? user.getDisplayName() : "");
        project.put("title", title);
        project.put("description", description);
        project.put("genre", genre);
        project.put("imageUri", selectedImageUri != null ? selectedImageUri.toString() : "");
        project.put("demoUri", selectedDemoUri != null ? selectedDemoUri.toString() : "");
        project.put("status", "active");
        project.put("createdAt", FieldValue.serverTimestamp());

        db.collection("projects")
                .add(project)
                .addOnSuccessListener(documentReference -> {
                    setLoading(false);
                    Toast.makeText(this, "Proyecto publicado", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "No se pudo publicar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.btnPublicar.setEnabled(!loading);
        binding.btnPublicar.setText(loading ? "Publicando..." : "Publicar");
        binding.btnUploadImage.setEnabled(!loading);
        binding.btnUploadDemo.setEnabled(!loading);
        binding.etProjectTitle.setEnabled(!loading);
        binding.etProjectDescription.setEnabled(!loading);
        binding.etProjectGenre.setEnabled(!loading);
    }
}
