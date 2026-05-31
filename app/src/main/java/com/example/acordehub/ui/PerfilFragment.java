package com.example.acordehub.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.auth.AuthVistaModelo;
import com.example.acordehub.databinding.FragmentPerfilBinding;
import com.example.acordehub.modelos.FavoriteArtist;
import com.example.acordehub.modelos.UserModel;
import com.example.acordehub.perfil.PerfilVistaModelo;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private PerfilVistaModelo perfilVistaModelo;
    private AuthVistaModelo authVistaModelo;
    private String currentPhotoUrl = "";
    private List<FavoriteArtist> selectedArtists = new ArrayList<>();
    private SelectedArtistAdapter artistsAdapter;

    // Launcher para Buscador de Artistas (Spotify)
    private final ActivityResultLauncher<Intent> artistSearchLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String artistName = result.getData().getStringExtra("ARTIST_NAME");
                    String artistImage = result.getData().getStringExtra("ARTIST_IMAGE");
                    
                    if (artistName != null) {
                        // Evitar duplicados
                        boolean exists = false;
                        for (FavoriteArtist fa : selectedArtists) {
                            if (fa.getName().equals(artistName)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            selectedArtists.add(new FavoriteArtist(artistName, artistImage));
                            artistsAdapter.setArtists(selectedArtists);
                        }
                    }
                }
            });

    // ── Roles, géneros, instrumentos y niveles disponibles ────────────────────
    private static final String[] ROLES        = {"Músico", "Productor", "Cantante", "Guitarrista", "Bajista", "Baterista", "Pianista", "DJ"};
    private static final String[] GENEROS      = {"Rock", "Pop", "Metal", "R&B", "Rap", "Jazz", "Cumbia", "Electrónica", "Folklore", "Clásica"};
    private static final String[] INSTRUMENTOS = {"Guitarra", "Bajo", "Batería", "Piano", "Voz", "Violín", "Trompeta", "Saxofón", "Teclado"};
    private static final String[] NIVELES      = {"Principiante", "Intermedio", "Avanzado"};

    // Launcher para seleccionar imagen de galería
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        // Mostrar preview inmediato
                        Glide.with(this).load(imageUri).circleCrop().into(binding.imgFotoPerfil);
                        // Subir a Firebase Storage
                        perfilVistaModelo.subirFoto(imageUri);
                    }
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        perfilVistaModelo = new ViewModelProvider(this).get(PerfilVistaModelo.class);
        authVistaModelo = new ViewModelProvider(this).get(AuthVistaModelo.class);

        setupChips();
        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();

        perfilVistaModelo.cargarPerfil();
    }

    // ── Setup de chips ────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        artistsAdapter = new SelectedArtistAdapter(position -> {
            selectedArtists.remove(position);
            artistsAdapter.setArtists(selectedArtists);
        });
        binding.rvArtistasFavoritos.setAdapter(artistsAdapter);
    }

    private void setupChips() {
        // Roles (selección única)
        for (String rol : ROLES) {
            Chip chip = crearChip(rol, false);
            binding.chipGroupRol.addView(chip);
        }

        // Géneros (selección múltiple)
        for (String genero : GENEROS) {
            Chip chip = crearChip(genero, true);
            binding.chipGroupGeneros.addView(chip);
        }

        // Instrumentos (selección múltiple)
        for (String instrumento : INSTRUMENTOS) {
            Chip chip = crearChip(instrumento, true);
            binding.chipGroupInstrumentos.addView(chip);
        }

        // Niveles (selección única)
        for (String nivel : NIVELES) {
            Chip chip = crearChip(nivel, false);
            binding.chipGroupNivel.addView(chip);
        }
    }

    private Chip crearChip(String texto, boolean multiSelect) {
        Chip chip = new Chip(requireContext());
        chip.setText(texto);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);

        // Estilo visual: negro cuando está seleccionado, blanco transparente cuando no
        chip.setChipBackgroundColorResource(R.color.chip_selector);
        chip.setTextColor(requireContext().getColorStateList(R.color.chip_text_selector));
        chip.setChipStrokeColorResource(R.color.black_primary);
        chip.setChipStrokeWidth(1.5f);

        return chip;
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void setupClickListeners() {
        // Seleccionar foto
        binding.imgFotoPerfil.setOnClickListener(v -> abrirGaleria());
        binding.btnCambiarFoto.setOnClickListener(v -> abrirGaleria());

        // Guardar perfil
        binding.btnGuardar.setOnClickListener(v -> guardarPerfil());

        // Spotify (Abre el buscador directamente)
        binding.btnVincularSpotify.setOnClickListener(v -> abrirBuscadorArtistas());

        // Cerrar sesión
        binding.btnLogout.setOnClickListener(v -> cerrarSesion());
    }

    private void cerrarSesion() {
        authVistaModelo.logout();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void guardarPerfil() {
        String name        = binding.etNombre.getText().toString().trim();
        String description = binding.etDescripcion.getText().toString().trim();
        String location    = binding.etUbicacion.getText().toString().trim();

        if (name.isEmpty()) {
            binding.tilNombre.setError("Ingresá tu nombre");
            return;
        }

        String role  = getChipSeleccionado(binding.chipGroupRol);
        String level = getChipSeleccionado(binding.chipGroupNivel);
        List<String> genres      = getChipsSeleccionados(binding.chipGroupGeneros);
        List<String> instruments = getChipsSeleccionados(binding.chipGroupInstrumentos);

        perfilVistaModelo.guardarPerfil(name, role, genres, instruments,
                level, description, location, currentPhotoUrl, selectedArtists);
    }

    // ── Spotify Logic ────────────────────────────────────────────────────────

    private void abrirBuscadorArtistas() {
        Intent intent = new Intent(requireContext(), SpotifyArtistSearchActivity.class);
        artistSearchLauncher.launch(intent);
    }

    // ── Observar ViewModel ────────────────────────────────────────────────────

    private void observeViewModel() {
        // Perfil cargado → poblar la UI
        perfilVistaModelo.getPerfilLiveData().observe(getViewLifecycleOwner(), this::poblarUI);

        // URL de foto subida
        perfilVistaModelo.getFotoUrlLiveData().observe(getViewLifecycleOwner(), url -> {
            if (url != null) {
                currentPhotoUrl = url;
                Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show();
            }
        });

        // Guardado exitoso
        perfilVistaModelo.getGuardadoLiveData().observe(getViewLifecycleOwner(), ok -> {
            if (ok != null && ok) {
                Toast.makeText(requireContext(), "Perfil guardado ✓", Toast.LENGTH_SHORT).show();
                // Navegar al Home después de guardar
                NavController navController = androidx.navigation.fragment.NavHostFragment.findNavController(this);
                navController.navigate(R.id.homeFragment);
            }
        });

        // Errores
        perfilVistaModelo.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
        });

        // Loading
        perfilVistaModelo.getLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnGuardar.setEnabled(!isLoading);
        });
    }

    // ── Poblar UI con datos del perfil ────────────────────────────────────────

    private void poblarUI(UserModel user) {
        if (user == null) return;

        binding.etNombre.setText(user.getName());
        binding.etDescripcion.setText(user.getDescription());
        binding.etUbicacion.setText(user.getLocation());
        currentPhotoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl() : "";

        // Foto de perfil
        if (!currentPhotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentPhotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(binding.imgFotoPerfil);
        }

        // Marcar rol
        marcarChip(binding.chipGroupRol, user.getRole());

        // Marcar nivel
        marcarChip(binding.chipGroupNivel, user.getLevel());

        // Marcar géneros
        if (user.getGenres() != null) {
            for (String g : user.getGenres()) marcarChip(binding.chipGroupGeneros, g);
        }

        // Marcar instrumentos
        if (user.getInstruments() != null) {
            for (String i : user.getInstruments()) marcarChip(binding.chipGroupInstrumentos, i);
        }

        // Mostrar artistas favoritos
        selectedArtists.clear();
        if (user.getFavoriteArtists() != null) {
            selectedArtists.addAll(user.getFavoriteArtists());
        }
        artistsAdapter.setArtists(selectedArtists);
    }

    // ── Helpers para chips ────────────────────────────────────────────────────

    private void marcarChip(com.google.android.material.chip.ChipGroup group, String texto) {
        if (texto == null || texto.isEmpty()) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
            if (chip.getText().toString().equals(texto)) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private String getChipSeleccionado(com.google.android.material.chip.ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) return "";
        Chip chip = group.findViewById(id);
        return chip != null ? chip.getText().toString() : "";
    }

    private List<String> getChipsSeleccionados(com.google.android.material.chip.ChipGroup group) {
        List<String> seleccionados = new ArrayList<>();
        List<Integer> ids = group.getCheckedChipIds();
        for (int id : ids) {
            Chip chip = group.findViewById(id);
            if (chip != null) seleccionados.add(chip.getText().toString());
        }
        return seleccionados;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
