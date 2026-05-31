// Archivo: app/src/main/java/com/example/acordehub/ui/RegisterActivity.java

package com.example.acordehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.acordehub.auth.AuthVistaModelo;
import com.example.acordehub.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthVistaModelo authVistaModelo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authVistaModelo = new ViewModelProvider(this).get(AuthVistaModelo.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> {
            String name     = binding.etName.getText().toString().trim();
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirm  = binding.etConfirmPassword.getText().toString().trim();
            if (!validateInputs(name, email, password, confirm)) return;
            authVistaModelo.register(name, email, password);
        });

        binding.tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        // Registro exitoso → ir al home
        authVistaModelo.getUserLiveData().observe(this, user -> {
            if (user != null) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        // Errores
        authVistaModelo.getErrorLiveData().observe(this, error -> {
            if (error != null) Toast.makeText(this, translateError(error), Toast.LENGTH_LONG).show();
        });

        // Loading
        authVistaModelo.getLoadingLiveData().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!isLoading);
        });
    }

    private boolean validateInputs(String name, String email, String password, String confirm) {
        if (name.isEmpty()) {
            binding.tilName.setError("Ingresá tu nombre"); return false;
        }
        if (email.isEmpty()) {
            binding.tilEmail.setError("Ingresá tu email"); return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Email inválido"); return false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Mínimo 6 caracteres"); return false;
        }
        if (!password.equals(confirm)) {
            binding.tilConfirmPassword.setError("Las contraseñas no coinciden"); return false;
        }
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
        return true;
    }

    private String translateError(String error) {
        if (error.contains("email address is already in use")) return "Ya existe una cuenta con ese email.";
        if (error.contains("badly formatted"))                 return "El formato del email no es válido.";
        if (error.contains("network"))                         return "Sin conexión. Revisá tu red.";
        return "Error: " + error;
    }
}