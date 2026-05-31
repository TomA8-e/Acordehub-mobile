// Archivo: app/src/main/java/com/example/acordehub/ui/LoginActivity.java
// CAMBIO: navigateToHome() ahora va a MainActivity (que contiene los fragments)

package com.example.acordehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.example.acordehub.R;
import com.example.acordehub.auth.AuthVistaModelo;
import com.example.acordehub.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthVistaModelo authVistaModelo;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    authVistaModelo.loginWithGoogle(account);
                } catch (ApiException e) {
                    Toast.makeText(this, "Google Sign-In cancelado", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViewModel();
        setupGoogleSignIn();
        setupClickListeners();
        observeViewModel();
    }

    private void setupViewModel() {
        authVistaModelo = new ViewModelProvider(this).get(AuthVistaModelo.class);
        if (authVistaModelo.getCurrentUser() != null) {
            navigateToMain();
        }
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            if (!validateInputs(email, password)) return;
            authVistaModelo.login(email, password);
        });

        binding.btnGoogle.setOnClickListener(v ->
                googleSignInClient.signOut().addOnCompleteListener(task -> {
                    googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
                })
        );

        binding.tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void observeViewModel() {
        authVistaModelo.getUserLiveData().observe(this, user -> {
            if (user != null) navigateToMain();
        });

        authVistaModelo.getErrorLiveData().observe(this, error -> {
            if (error != null) Toast.makeText(this, translateError(error), Toast.LENGTH_LONG).show();
        });

        authVistaModelo.getLoadingLiveData().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!isLoading);
            binding.btnGoogle.setEnabled(!isLoading);
        });
    }

    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) { binding.tilEmail.setError("Ingresá tu email"); return false; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Email inválido"); return false;
        }
        if (password.isEmpty()) { binding.tilPassword.setError("Ingresá tu contraseña"); return false; }
        if (password.length() < 6) { binding.tilPassword.setError("Mínimo 6 caracteres"); return false; }
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        return true;
    }

    private String translateError(String error) {
        if (error.contains("no user record"))      return "No existe una cuenta con ese email.";
        if (error.contains("password is invalid")) return "Contraseña incorrecta.";
        if (error.contains("network"))             return "Sin conexión. Revisá tu red.";
        if (error.contains("blocked"))             return "Demasiados intentos. Intentá más tarde.";
        return "Error: " + error;
    }

    // ── Navega a MainActivity (que tiene los fragments) ──────────────────────
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}