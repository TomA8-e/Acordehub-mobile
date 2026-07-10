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
import com.example.acordehub.modelos.Project;
import com.example.acordehub.perfil.PerfilVistaModelo;
import com.example.acordehub.proyectos.ProjectVistaModelo;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AuthVistaModelo authVistaModelo;
    private PerfilVistaModelo perfilVistaModelo;
    private ProjectVistaModelo projectVistaModelo;

    private SelectedArtistAdapter recommendationsAdapter;
    private SelectedArtistAdapter discoverAdapter;
    private ProjectAdapter projectAdapter;
    private final List<Project> allProjects = new ArrayList<>();
    private int selectedProjectFilterId = R.id.chipAllProjects;

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

        projectAdapter = new ProjectAdapter(project -> {
            Intent intent = new Intent(requireContext(), ProjectDetailActivity.class);
            intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.getId());
            startActivity(intent);
        });
        binding.rvProjectFeed.setAdapter(projectAdapter);
    }

    private void setupClickListeners() {
        binding.btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationsActivity.class)));

        binding.btnMessages.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.chatFragment));

        binding.btnQuickActionCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PublishProjectActivity.class)));

        binding.btnCreateProjectFromFeed.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PublishProjectActivity.class)));

        binding.btnCreateProjectEmpty.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PublishProjectActivity.class)));

        binding.btnQuickActionDiscover.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ExploreMatchActivity.class)));

        binding.chipGroupProjects.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedProjectFilterId = checkedIds.isEmpty()
                    ? R.id.chipAllProjects
                    : checkedIds.get(0);
            applyProjectFilter();
        });
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
            allProjects.clear();
            if (projects != null) allProjects.addAll(projects);
            applyProjectFilter();
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

    private void applyProjectFilter() {
        List<Project> filteredProjects = new ArrayList<>();
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        for (Project project : allProjects) {
            if (selectedProjectFilterId == R.id.chipProjectsWithDemo && !project.hasDemo()) {
                continue;
            }
            if (selectedProjectFilterId == R.id.chipMyProjects
                    && (project.getOwnerUid() == null || !project.getOwnerUid().equals(currentUid))) {
                continue;
            }
            filteredProjects.add(project);
        }

        boolean hasProjects = !filteredProjects.isEmpty();
        projectAdapter.setProjects(filteredProjects);
        binding.rvProjectFeed.setVisibility(hasProjects ? View.VISIBLE : View.GONE);
        binding.containerEmptyProjects.setVisibility(hasProjects ? View.GONE : View.VISIBLE);
        updateProjectSectionCopy(filteredProjects.size());
    }

    private void updateProjectSectionCopy(int visibleCount) {
        if (selectedProjectFilterId == R.id.chipProjectsWithDemo) {
            binding.tvProjectFeedMeta.setText(visibleCount + " proyectos con demo para escuchar");
            binding.tvEmptyProjects.setText("No hay proyectos con demo todavia");
            binding.tvEmptyProjectsHint.setText("Publica una demo o vuelve a revisar cuando haya nuevas ideas.");
            return;
        }

        if (selectedProjectFilterId == R.id.chipMyProjects) {
            binding.tvProjectFeedMeta.setText(visibleCount + " proyectos tuyos publicados");
            binding.tvEmptyProjects.setText("Todavia no publicaste proyectos");
            binding.tvEmptyProjectsHint.setText("Crea una idea, subi una demo y dejala lista para colaborar.");
            return;
        }

        binding.tvProjectFeedMeta.setText(allProjects.size() + " ideas recientes con demos para escuchar");
        binding.tvEmptyProjects.setText("Todavia no hay proyectos publicados");
        binding.tvEmptyProjectsHint.setText("Cuando alguien publique una idea con demo, va a aparecer aca.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (projectAdapter != null) projectAdapter.release();
        binding = null;
    }
}
