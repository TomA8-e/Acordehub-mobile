package com.example.acordehub.perfil;

import android.net.Uri;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.acordehub.modelos.UserModel;

import java.util.HashMap;
import java.util.Map;

public class PerfilRepository {

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;

    public PerfilRepository() {
        this.db      = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.auth    = FirebaseAuth.getInstance();
    }

    // ── Obtener perfil del usuario actual ─────────────────────────────────────

    public void getPerfil(PerfilCallback callback) {
        String uid = getCurrentUid();
        if (uid == null) { callback.onError("Usuario no autenticado"); return; }

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        UserModel user = snapshot.toObject(UserModel.class);
                        callback.onSuccess(user);
                    } else {
                        UserModel user = createDefaultUser();
                        db.collection("users").document(uid)
                                .set(user)
                                .addOnSuccessListener(unused -> callback.onSuccess(user))
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Guardar perfil en Firestore ───────────────────────────────────────────

    public void guardarPerfil(UserModel user, SimpleCallback callback) {
        String uid = getCurrentUid();
        if (uid == null) { callback.onError("Usuario no autenticado"); return; }

        FirebaseUser currentUser = auth.getCurrentUser();
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        if (currentUser != null && currentUser.getEmail() != null) {
            data.put("email", currentUser.getEmail());
        }
        data.put("name", user.getName());
        data.put("role", user.getRole());
        data.put("genres", user.getGenres());
        data.put("instruments", user.getInstruments());
        data.put("level", user.getLevel());
        data.put("description", user.getDescription());
        data.put("location", user.getLocation());
        data.put("photoUrl", user.getPhotoUrl());
        data.put("favoriteArtists", user.getFavoriteArtists());

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Subir foto de perfil a Firebase Storage ───────────────────────────────

    public void subirFoto(Uri imageUri, UrlCallback callback) {
        String uid = getCurrentUid();
        if (uid == null) { callback.onError("Usuario no autenticado"); return; }

        StorageReference ref = storage.getReference()
                .child("profile_photos/" + uid + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                                .addOnFailureListener(e -> callback.onError(e.getMessage()))
                )
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    private UserModel createDefaultUser() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        String uid = firebaseUser != null ? firebaseUser.getUid() : "";
        String email = firebaseUser != null && firebaseUser.getEmail() != null
                ? firebaseUser.getEmail()
                : "";
        String name = firebaseUser != null && firebaseUser.getDisplayName() != null
                ? firebaseUser.getDisplayName()
                : "Usuario";
        return new UserModel(uid, name, email);
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public interface PerfilCallback {
        void onSuccess(UserModel user);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface UrlCallback {
        void onSuccess(String url);
        void onError(String error);
    }
}
