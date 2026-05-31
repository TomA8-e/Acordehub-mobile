// Archivo: app/src/main/java/com/example/acordehub/auth/AuthVistaModelo.java

package com.example.acordehub.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseUser;

public class AuthVistaModelo extends ViewModel {

    private final AuthRepository repository;

    private final MutableLiveData<FirebaseUser> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public LiveData<FirebaseUser> getUserLiveData() { return userLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }
    public LiveData<Boolean> getLoadingLiveData() { return loadingLiveData; }

    public AuthVistaModelo() {
        this.repository = new AuthRepository();
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    public void register(String name, String email, String password) {
        loadingLiveData.setValue(true);
        repository.registerWithEmail(name, email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                loadingLiveData.setValue(false);
                userLiveData.setValue(user);
            }
            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    // ── Login con email ───────────────────────────────────────────────────────

    public void login(String email, String password) {
        loadingLiveData.setValue(true);
        repository.loginWithEmail(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                loadingLiveData.setValue(false);
                userLiveData.setValue(user);
            }
            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    // ── Login con Google ──────────────────────────────────────────────────────

    public void loginWithGoogle(GoogleSignInAccount account) {
        loadingLiveData.setValue(true);
        repository.loginWithGoogle(account, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                loadingLiveData.setValue(false);
                userLiveData.setValue(user);
            }
            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    public void logout() {
        repository.logout();
        userLiveData.setValue(null);
    }

    public FirebaseUser getCurrentUser() {
        return repository.getCurrentUser();
    }
}