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
import com.example.acordehub.proyectos.ProjectVistaModelo;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AuthVistaModelo authVistaModelo;
    private PerfilVistaModelo perfilVistaModelo;
    private ProjectVistaModelo projectVistaModelo;

    private SelectedArtistAdapter recommendationsAdapter;
    private SelectedArtistAdapter discoverAdapter;
    private ProjectAdapter projectAdapter;

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
        projectVistaModelo = new ViewModelProvider(this).get(ProjectVistaModelo.class);

        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();

        perfilVistaModelo.cargarPerfil();
        projectVistaModelo.escucharProyectosDestacados();
        projectVistaModelo.escucharMisProyectosActivos();
    }

    private void setupRecyclerViews() {
        recommendationsAdapter = new SelectedArtistAdapter(null);
        binding.rvRecommendations.setAdapter(recommendationsAdapter);

        discoverAdapter = new SelectedArtistAdapter(null);
        binding.rvDiscoverMusicians.setAdapter(discoverAdapter);

        projectAdapter = new ProjectAdapter();
        binding.rvProjectFeed.setAdapter(projectAdapter);
    }

    private void setupClickListeners() {
        binding.btnNotifications.setOnClickListener(v ->
                Toast.makeText(getContext(), "Notificaciones (Proximamente)", Toast.LENGTH_SHORT).show());

        binding.btnMessages.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.chatFragment));

        binding.btnQuickActionCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PublishProjectActivity.class)));

        binding.btnQuickActionDiscover.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ExploreMatchActivity.class)));
    }

    private void observeViewModel() {
        perfilVistaModelo.getPerfilLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.tvHeaderGreeting.setText("Hola, " + user.getName() + "!");

                if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                    Glide.with(this)
                            .load(user.getPhotoUrl())
                            .circleCrop()
                            .placeholder(R.drawable.ic_person_placeholder)
                            .into(binding.ivHeaderProfile);
                }

                if (user.getFavoriteArtists() != null) {
                    recommendationsAdapter.setArtists(user.getFavoriteArtists());
                }
            }
        });

        projectVistaModelo.getProjectsLiveData().observe(getViewLifecycleOwner(), projects -> {
            boolean hasProjects = projects != null && !projects.isEmpty();
            projectAdapter.setProjects(projects);
            binding.rvProjectFeed.setVisibility(hasProjects ? View.VISIBLE : View.GONE);
            binding.tvEmptyProjects.setVisibility(hasProjects ? View.GONE : View.VISIBLE);
        });

        projectVistaModelo.getActiveProjectsCountLiveData().observe(getViewLifecycleOwner(), count -> {
            int activeCount = count != null ? count : 0;
            if (activeCount == 1) {
                binding.tvActiveProjectsCount.setText("1 Activo");
            } else {
                binding.tvActiveProjectsCount.setText(activeCount + " Activos");
            }
        });

        projectVistaModelo.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
