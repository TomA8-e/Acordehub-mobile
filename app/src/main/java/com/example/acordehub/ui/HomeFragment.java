package com.example.acordehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.auth.AuthVistaModelo;
import com.example.acordehub.databinding.FragmentHomeBinding;
import com.example.acordehub.perfil.PerfilVistaModelo;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AuthVistaModelo authVistaModelo;
    private PerfilVistaModelo perfilVistaModelo;
    
    // Adaptadores (usaremos placeholders por ahora)
    private SelectedArtistAdapter recommendationsAdapter;
    private SelectedArtistAdapter discoverAdapter;
    // Agregaremos adaptadores específicos para Feed en el futuro

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authVistaModelo = new ViewModelProvider(this).get(AuthVistaModelo.class);
        perfilVistaModelo = new ViewModelProvider(this).get(PerfilVistaModelo.class);

        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();

        perfilVistaModelo.cargarPerfil();
    }

    private void setupRecyclerViews() {
        // Recomendaciones (Basado en Spotify)
        recommendationsAdapter = new SelectedArtistAdapter(null);
        binding.rvRecommendations.setAdapter(recommendationsAdapter);

        // Descubrir Talentos
        discoverAdapter = new SelectedArtistAdapter(null);
        binding.rvDiscoverMusicians.setAdapter(discoverAdapter);
        
        // El feed de proyectos usará un adaptador distinto en el futuro
    }

    private void setupClickListeners() {
        binding.btnNotifications.setOnClickListener(v -> 
            Toast.makeText(getContext(), "Notificaciones (Próximamente)", Toast.LENGTH_SHORT).show());
        
        binding.btnMessages.setOnClickListener(v ->
            NavHostFragment.findNavController(this).navigate(R.id.chatFragment));

        binding.btnQuickActionCreate.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), PublishProjectActivity.class)));
        
        binding.btnQuickActionDiscover.setOnClickListener(v -> 
            Toast.makeText(getContext(), "Explorar Músicos", Toast.LENGTH_SHORT).show());
        
        binding.btnQuickActionUpload.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), PublishProjectActivity.class)));
    }

    private void observeViewModel() {
        // Saludo y Foto en Header
        perfilVistaModelo.getPerfilLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.tvHeaderGreeting.setText("¡Hola, " + user.getName() + "!");
                
                if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                    Glide.with(this)
                            .load(user.getPhotoUrl())
                            .circleCrop()
                            .placeholder(R.drawable.ic_person_placeholder)
                            .into(binding.ivHeaderProfile);
                }

                // Cargar artistas favoritos en la sección de recomendaciones
                if (user.getFavoriteArtists() != null) {
                    recommendationsAdapter.setArtists(user.getFavoriteArtists());
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
