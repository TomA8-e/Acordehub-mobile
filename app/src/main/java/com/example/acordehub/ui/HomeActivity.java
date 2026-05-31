// Archivo: app/src/main/java/com/example/acordehub/ui/HomeActivity.java

package com.example.acordehub.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.acordehub.auth.AuthVistaModelo;
import com.example.acordehub.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private AuthVistaModelo authVistaModelo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authVistaModelo = new ViewModelProvider(this).get(AuthVistaModelo.class);

        // Mostrar nombre del usuario logueado
        if (authVistaModelo.getCurrentUser() != null) {
            String displayName = authVistaModelo.getCurrentUser().getDisplayName();
            String email       = authVistaModelo.getCurrentUser().getEmail();
            String nombre = (displayName != null && !displayName.isEmpty()) ? displayName : email;
            binding.tvWelcome.setText("Hola, " +  " 👋");
        }

        // Botón cerrar sesión
        binding.btnLogout.setOnClickListener(v -> {
            authVistaModelo.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}