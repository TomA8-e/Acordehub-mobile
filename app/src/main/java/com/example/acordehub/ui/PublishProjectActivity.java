package com.example.acordehub.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.databinding.ActivityPublishProjectBinding;
import com.example.acordehub.subscription.PlanConfig;
import com.example.acordehub.subscription.PlanLimits;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PublishProjectActivity extends AppCompatActivity {

    private ActivityPublishProjectBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri selectedImageUri;
    private Uri selectedDemoUri;
    private PlanConfig currentPlan = PlanLimits.get("free");

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedImageUri = uri;
                binding.ivProjectPreview.setImageTintList(null);
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
                String validationError = validateDemoForCurrentPlan(uri);
                if (validationError != null) {
                    Toast.makeText(this, validationError, Toast.LENGTH_LONG).show();
                    return;
                }
                selectedDemoUri = uri;
                binding.tvDemoFileName.setText(getFileName(uri));
                binding.tvDemoFileName.setVisibility(View.VISIBLE);
                binding.tvDemoUploadHint.setText("Demo lista para subir");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPublishProjectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnUploadImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        binding.btnUploadDemo.setOnClickListener(v -> demoPicker.launch("audio/*"));
        binding.btnPublicar.setOnClickListener(v -> publishProject());
        loadCurrentPlan();
    }

    private void loadCurrentPlan() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    currentPlan = PlanLimits.get(snapshot.getString("plan"));
                    binding.tvDemoUploadHint.setText("Subir demo "
                            + currentPlan.getAllowedFileExtensions().toString().toUpperCase()
                            + " · max " + currentPlan.getMaxFileSizeMB() + " MB");
                });
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
        if (selectedDemoUri == null) {
            Toast.makeText(this, "Subi una demo compatible con tu plan para que puedan escucharte", Toast.LENGTH_LONG).show();
            return;
        }
        String validationError = validateDemoForCurrentPlan(selectedDemoUri);
        if (validationError != null) {
            Toast.makeText(this, validationError, Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);

        String fallbackOwnerName = user.getDisplayName() != null ? user.getDisplayName() : "";
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    currentPlan = PlanLimits.get(snapshot.getString("plan"));
                    String profileName = snapshot.getString("name");
                    checkActiveProjectLimit(user, title, description, genre,
                            isNotEmpty(profileName) ? profileName : fallbackOwnerName);
                })
                .addOnFailureListener(e -> checkActiveProjectLimit(user, title, description, genre, fallbackOwnerName));
    }

    private void checkActiveProjectLimit(FirebaseUser user, String title, String description,
                                         String genre, String ownerName) {
        db.collection("projects")
                .whereEqualTo("ownerUid", user.getUid())
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.size() >= currentPlan.getMaxActiveProjects()) {
                        setLoading(false);
                        Toast.makeText(this, "Actualizá tu plan para tener más proyectos activos.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    saveProject(user, title, description, genre, ownerName);
                })
                .addOnFailureListener(e -> saveProject(user, title, description, genre, ownerName));
    }

    private void saveProject(FirebaseUser user, String title, String description,
                             String genre, String ownerName) {
        DocumentReference projectRef = db.collection("projects").document();
        String projectId = projectRef.getId();

        uploadFile(user.getUid(), projectId, "cover", selectedImageUri)
                .continueWithTask(imageTask -> {
                    if (!imageTask.isSuccessful()) {
                        Exception exception = imageTask.getException();
                        throw exception != null ? exception : new Exception("No se pudo subir la imagen");
                    }
                    String imageUrl = imageTask.getResult();
                    return uploadFile(user.getUid(), projectId, "demo", selectedDemoUri)
                            .continueWithTask(demoTask -> {
                                if (!demoTask.isSuccessful()) {
                                    Exception exception = demoTask.getException();
                                    throw exception != null ? exception : new Exception("No se pudo subir la demo");
                                }
                                return saveProjectDocument(projectRef, user, title, description, genre,
                                        ownerName, imageUrl, demoTask.getResult());
                            });
                })
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Proyecto publicado", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "No se pudo publicar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private Task<Void> saveProjectDocument(DocumentReference projectRef, FirebaseUser user, String title,
                                           String description, String genre, String ownerName,
                                           String imageUrl, String demoUrl) {
        Map<String, Object> project = new HashMap<>();
        project.put("ownerUid", user.getUid());
        project.put("ownerName", ownerName);
        project.put("title", title);
        project.put("description", description);
        project.put("genre", genre);
        project.put("imageUri", imageUrl);
        project.put("demoUri", demoUrl);
        project.put("status", "active");
        project.put("createdAt", FieldValue.serverTimestamp());

        return projectRef.set(project);
    }

    private Task<String> uploadFile(String uid, String projectId, String kind, Uri uri) {
        if (uri == null) return Tasks.forResult("");

        String extension = getFileExtension(uri, kind);
        StorageReference reference = storage.getReference()
                .child("project_uploads")
                .child(uid)
                .child(projectId)
                .child(kind + "." + extension);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(getContentType(uri, kind))
                .build();

        return reference.putFile(uri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        throw new Exception("No se pudo subir " + getKindLabel(kind)
                                + ": " + (exception != null ? exception.getMessage() : "error desconocido"));
                    }
                    return task.getResult().getStorage().getDownloadUrl();
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        throw new Exception("No se pudo obtener la URL de " + getKindLabel(kind)
                                + ": " + (exception != null ? exception.getMessage() : "error desconocido"));
                    }
                    return task.getResult().toString();
                });
    }

    private String getContentType(Uri uri, String kind) {
        String fileName = getFileName(uri).toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".wav")) return "audio/wav";
        if (fileName.endsWith(".mp3")) return "audio/mpeg";

        String resolverType = getContentResolver().getType(uri);
        if (resolverType != null && !resolverType.trim().isEmpty()) return resolverType;

        return "cover".equals(kind) ? "image/jpeg" : "audio/mpeg";
    }

    private String getKindLabel(String kind) {
        if ("cover".equals(kind)) return "la portada";
        if ("demo".equals(kind)) return "la demo";
        return "el archivo";
    }

    private boolean isSupportedAudio(Uri uri) {
        return currentPlan.allowsExtension(getFileExtension(uri, "demo"));
    }

    private String validateDemoForCurrentPlan(Uri uri) {
        String extension = getFileExtension(uri, "demo");
        if (!currentPlan.allowsExtension(extension)) {
            if ("wav".equals(extension)) return "Actualizá tu plan para subir archivos WAV.";
            if (currentPlan.getPlan() == com.example.acordehub.subscription.UserPlan.FREE) {
                return "El plan Free solo permite subir archivos MP3.";
            }
            return "Actualizá tu plan para subir este tipo de archivo.";
        }

        long sizeBytes = getFileSizeBytes(uri);
        long maxBytes = currentPlan.getMaxFileSizeMB() * 1024L * 1024L;
        if (sizeBytes > 0 && sizeBytes > maxBytes) {
            return "El archivo supera el tamaño permitido para tu plan.";
        }
        return null;
    }

    private String getFileExtension(Uri uri, String kind) {
        String fileName = getFileName(uri).toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".stems")) return "stems";
        if (fileName.endsWith(".stem")) return "stem";
        if (fileName.endsWith(".zip")) return "zip";
        if (fileName.endsWith(".wav")) return "wav";
        if (fileName.endsWith(".mp3")) return "mp3";
        if ("cover".equals(kind)) return "jpg";
        return "mp3";
    }

    private String getFileName(Uri uri) {
        String fileName = "archivo seleccionado";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (displayNameIndex >= 0) {
                    String displayName = cursor.getString(displayNameIndex);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        fileName = displayName;
                    }
                }
            }
        }
        return fileName;
    }

    private long getFileSizeBytes(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) return cursor.getLong(sizeIndex);
            }
        }
        return -1;
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void setLoading(boolean loading) {
        binding.btnPublicar.setEnabled(!loading);
        binding.btnPublicar.setText(loading ? "Publicando..." : "Publicar");
        binding.btnUploadImage.setEnabled(!loading);
        binding.btnUploadDemo.setEnabled(!loading);
        binding.etProjectTitle.setEnabled(!loading);
        binding.etProjectDescription.setEnabled(!loading);
        binding.etProjectGenre.setEnabled(!loading);
        binding.progressPublish.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
