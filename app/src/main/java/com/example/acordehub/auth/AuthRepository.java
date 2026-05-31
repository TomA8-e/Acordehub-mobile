package com.example.acordehub.auth;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.acordehub.modelos.UserModel;

public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore db;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // ── Registro con email y contraseña ──────────────────────────────────────

    public void registerWithEmail(String name, String email, String password,
                                  AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onError("Error al crear el usuario.");
                        return;
                    }
                    saveUserToFirestore(user.getUid(), name, email, callback);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Login con email y contraseña ──────────────────────────────────────────

    public void loginWithEmail(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Login con Google ──────────────────────────────────────────────────────

    public void loginWithGoogle(GoogleSignInAccount account, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onError("Error con Google Sign-In.");
                        return;
                    }
                    boolean isNewUser = authResult.getAdditionalUserInfo() != null
                            && authResult.getAdditionalUserInfo().isNewUser();
                    if (isNewUser) {
                        String displayName = account.getDisplayName() != null
                                ? account.getDisplayName() : "Usuario";
                        saveUserToFirestore(user.getUid(), displayName, user.getEmail(), callback);
                    } else {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Cerrar sesión ─────────────────────────────────────────────────────────

    public void logout() {
        firebaseAuth.signOut();
    }

    // ── Usuario actual ────────────────────────────────────────────────────────

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    // ── Guardar usuario en Firestore ──────────────────────────────────────────

    private void saveUserToFirestore(String uid, String name, String email,
                                     AuthCallback callback) {
        UserModel user = new UserModel(uid, name, email);
        db.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess(firebaseAuth.getCurrentUser()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Callback ──────────────────────────────────────────────────────────────

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }
}