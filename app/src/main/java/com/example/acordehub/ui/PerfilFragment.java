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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.auth.AuthVistaModelo;
import com.example.acordehub.databinding.FragmentPerfilBinding;
import com.example.acordehub.modelos.FavoriteArtist;
import com.example.acordehub.modelos.UserModel;
import com.example.acordehub.perfil.PerfilVistaModelo;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private PerfilVistaModelo perfilVistaModelo;
    private AuthVistaModelo authVistaModelo;
    private String currentPhotoUrl = "";
    private final List<FavoriteArtist> selectedArtists = new ArrayList<>();
    private SelectedArtistAdapter artistsAdapter;
    private UserModel currentUser;
    private final SimpleDateFormat memberSinceFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
    private static final String ROLE_PRODUCER = "Productor";

    private static final String[] ROLES = {"Musico", "Productor", "Cantante", "Guitarrista", "Bajista", "Baterista", "Pianista", "DJ"};
    private static final String[] GENEROS = {"Rock", "Pop", "Metal", "R&B", "Rap", "Jazz", "Cumbia", "Electronica", "Folklore", "Clasica"};
    private static final String[] INSTRUMENTOS = {"Guitarra", "Bajo", "Bateria", "Piano", "Voz", "Violin", "Trompeta", "Saxofon", "Teclado"};
    private static final String[] NIVELES = {"Principiante", "Intermedio", "Avanzado"};

    private final ActivityResultLauncher<Intent> artistSearchLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String artistName = result.getData().getStringExtra("ARTIST_NAME");
                    String artistImage = result.getData().getStringExtra("ARTIST_IMAGE");

                    if (artistName != null) {
                        boolean exists = false;
                        for (FavoriteArtist artist : selectedArtists) {
                            if (artist.getName() != null && artist.getName().equalsIgnoreCase(artistName)) {
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

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        Glide.with(this).load(imageUri).circleCrop().into(binding.imgFotoPerfil);
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

    private void setupRecyclerViews() {
        artistsAdapter = new SelectedArtistAdapter(position -> {
            selectedArtists.remove(position);
            artistsAdapter.setArtists(selectedArtists);
        });
        binding.rvArtistasFavoritos.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvArtistasFavoritos.setAdapter(artistsAdapter);
    }

    private void setupChips() {
        for (String role : ROLES) binding.chipGroupRol.addView(createEditableChip(role));
        for (String genre : GENEROS) binding.chipGroupGeneros.addView(createEditableChip(genre));
        for (String instrument : INSTRUMENTOS) binding.chipGroupInstrumentos.addView(createEditableChip(instrument));
        for (String level : NIVELES) binding.chipGroupNivel.addView(createEditableChip(level));
    }

    private Chip createEditableChip(String text) {
        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColorResource(R.color.chip_selector);
        chip.setTextColor(requireContext().getColorStateList(R.color.chip_text_selector));
        chip.setChipStrokeColorResource(R.color.black_primary);
        chip.setChipStrokeWidth(1.5f);
        return chip;
    }

    private void setupClickListeners() {
        binding.btnEditProfile.setOnClickListener(v -> setEditMode(true));
        binding.btnCancelEdit.setOnClickListener(v -> {
            if (currentUser != null) populateUi(currentUser);
            setEditMode(false);
        });
        binding.imgFotoPerfil.setOnClickListener(v -> openGallery());
        binding.btnCambiarFoto.setOnClickListener(v -> openGallery());
        binding.btnGuardar.setOnClickListener(v -> saveProfile());
        binding.btnVincularSpotify.setOnClickListener(v -> openArtistSearch());
        binding.cardPremium.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PremiumActivity.class)));
        binding.btnLogout.setOnClickListener(v -> logout());

        binding.chipGroupRol.setOnCheckedStateChangeListener((group, checkedIds) ->
                updateEditModeForRole(getSelectedChip(binding.chipGroupRol)));
    }

    private void observeViewModel() {
        perfilVistaModelo.getPerfilLiveData().observe(getViewLifecycleOwner(), this::populateUi);

        perfilVistaModelo.getFotoUrlLiveData().observe(getViewLifecycleOwner(), url -> {
            if (url != null) {
                currentPhotoUrl = url;
                Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show();
            }
        });

        perfilVistaModelo.getGuardadoLiveData().observe(getViewLifecycleOwner(), ok -> {
            if (ok != null && ok) {
                Toast.makeText(requireContext(), "Perfil guardado", Toast.LENGTH_SHORT).show();
                setEditMode(false);
                perfilVistaModelo.cargarPerfil();
            }
        });

        perfilVistaModelo.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
        });

        perfilVistaModelo.getLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = isLoading != null && isLoading;
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnGuardar.setEnabled(!loading);
            binding.btnEditProfile.setEnabled(!loading);
        });
    }

    private void populateUi(UserModel user) {
        if (user == null) return;
        currentUser = user;

        binding.etNombre.setText(user.getName());
        binding.etDescripcion.setText(user.getDescription());
        binding.etUbicacion.setText(user.getLocation());
        binding.etProducerServices.setText(joinValues(user.getProducerServices()));
        binding.etProducerCredits.setText(joinValues(user.getProducerCredits()));
        currentPhotoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl() : "";

        bindPhotos(currentPhotoUrl);
        clearEditableChips();
        markChip(binding.chipGroupRol, user.getRole());
        markChip(binding.chipGroupNivel, user.getLevel());
        updateEditModeForRole(user.getRole());

        if (user.getGenres() != null) {
            for (String genre : user.getGenres()) markChip(binding.chipGroupGeneros, genre);
        }
        if (user.getInstruments() != null) {
            for (String instrument : user.getInstruments()) markChip(binding.chipGroupInstrumentos, instrument);
        }

        selectedArtists.clear();
        if (user.getFavoriteArtists() != null) selectedArtists.addAll(user.getFavoriteArtists());
        artistsAdapter.setArtists(selectedArtists);

        populateProfileView(user);
    }

    private void populateProfileView(UserModel user) {
        boolean isProducer = isProducerRole(user.getRole());
        binding.tvProfileName.setText(valueOrFallback(user.getName(), "Usuario"));
        binding.tvProfileRole.setText(isProducer
                ? "Productor musical"
                : valueOrFallback(user.getRole(), "Rol musical sin definir"));
        binding.tvProfileLocation.setText(valueOrFallback(user.getLocation(), "Ubicacion no definida"));
        binding.tvAboutDescription.setText(valueOrFallback(user.getDescription(),
                isProducer ? "Todavia no agregaste una bio de productor." : "Todavia no agregaste una descripcion."));
        binding.tvProfileLevel.setText(valueOrFallback(user.getLevel(), "Sin definir"));
        binding.tvProfileEmail.setText(valueOrFallback(user.getEmail(), "Sin email"));

        binding.cardProducerPortfolio.setVisibility(isProducer ? View.VISIBLE : View.GONE);
        binding.cardProfileInstruments.setVisibility(isProducer ? View.GONE : View.VISIBLE);
        binding.cardProfileLevel.setVisibility(isProducer ? View.GONE : View.VISIBLE);
        binding.cardProfileArtists.setVisibility(isProducer ? View.GONE : View.VISIBLE);

        bindDisplayChips(binding.chipGroupProfileGenres, user.getGenres(),
                isProducer ? "Sin generos de referencia" : "Sin generos definidos");
        if (isProducer) {
            bindDisplayChips(binding.chipGroupProducerServices, user.getProducerServices(), "Sin servicios cargados");
            bindDisplayChips(binding.chipGroupProducerCredits, user.getProducerCredits(), "Sin creditos cargados");
        } else {
            bindDisplayChips(binding.chipGroupProfileInstruments, user.getInstruments(), "Sin instrumentos definidos");
            bindArtistChips(user.getFavoriteArtists());
        }

        binding.tvStatGenres.setText(String.valueOf(countValues(user.getGenres())));
        binding.tvStatArtistsLabel.setText(isProducer ? "Creditos" : "Artistas favoritos");
        binding.tvStatArtists.setText(String.valueOf(isProducer
                ? countValues(user.getProducerCredits())
                : user.getFavoriteArtists() != null ? user.getFavoriteArtists().size() : 0));
        binding.tvMemberSince.setText(user.getCreatedAt() != null ? memberSinceFormat.format(user.getCreatedAt()) : "Nuevo");
    }

    private void bindPhotos(String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            binding.imgFotoPerfil.setImageResource(R.drawable.ic_person_placeholder);
            binding.imgProfilePhotoView.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }

        Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person_placeholder)
                .into(binding.imgFotoPerfil);
        Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person_placeholder)
                .into(binding.imgProfilePhotoView);
    }

    private void bindDisplayChips(ChipGroup group, List<String> values, String fallback) {
        group.removeAllViews();
        boolean added = false;
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    group.addView(createDisplayChip(value.trim()));
                    added = true;
                }
            }
        }
        if (!added) group.addView(createDisplayChip(fallback));
    }

    private void bindArtistChips(List<FavoriteArtist> artists) {
        binding.chipGroupProfileArtists.removeAllViews();
        boolean added = false;
        if (artists != null) {
            for (FavoriteArtist artist : artists) {
                if (artist.getName() != null && !artist.getName().trim().isEmpty()) {
                    binding.chipGroupProfileArtists.addView(createDisplayChip(artist.getName().trim()));
                    added = true;
                }
            }
        }
        if (!added) binding.chipGroupProfileArtists.addView(createDisplayChip("Sin artistas vinculados"));
    }

    private Chip createDisplayChip(String text) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setTextSize(11);
        chip.setTextColor(requireContext().getColor(R.color.white));
        chip.setChipBackgroundColorResource(R.color.black_soft);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setCloseIconVisible(false);
        chip.setChipMinHeight(28);
        return chip;
    }

    private void saveProfile() {
        String name = binding.etNombre.getText().toString().trim();
        String description = binding.etDescripcion.getText().toString().trim();
        String location = binding.etUbicacion.getText().toString().trim();

        if (name.isEmpty()) {
            binding.tilNombre.setError("Ingresa tu nombre");
            return;
        }
        binding.tilNombre.setError(null);

        String role = getSelectedChip(binding.chipGroupRol);
        String level = getSelectedChip(binding.chipGroupNivel);
        List<String> genres = getSelectedChips(binding.chipGroupGeneros);
        boolean isProducer = isProducerRole(role);
        List<String> instruments = isProducer ? new ArrayList<>() : getSelectedChips(binding.chipGroupInstrumentos);
        List<FavoriteArtist> favoriteArtists = isProducer ? new ArrayList<>() : selectedArtists;
        List<String> producerServices = isProducer
                ? splitValues(binding.etProducerServices.getText() != null ? binding.etProducerServices.getText().toString() : "")
                : new ArrayList<>();
        List<String> producerCredits = isProducer
                ? splitValues(binding.etProducerCredits.getText() != null ? binding.etProducerCredits.getText().toString() : "")
                : new ArrayList<>();

        perfilVistaModelo.guardarPerfil(name, role, genres, instruments,
                isProducer ? "" : level, description, location, currentPhotoUrl,
                favoriteArtists, producerServices, producerCredits);
    }

    private void logout() {
        authVistaModelo.logout();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void openArtistSearch() {
        Intent intent = new Intent(requireContext(), SpotifyArtistSearchActivity.class);
        artistSearchLauncher.launch(intent);
    }

    private void setEditMode(boolean editing) {
        binding.profileViewContainer.setVisibility(editing ? View.GONE : View.VISIBLE);
        binding.editProfileContainer.setVisibility(editing ? View.VISIBLE : View.GONE);
        if (editing) updateEditModeForRole(getSelectedChip(binding.chipGroupRol));
    }

    private void updateEditModeForRole(String role) {
        boolean isProducer = isProducerRole(role);
        binding.producerPortfolioEditContainer.setVisibility(isProducer ? View.VISIBLE : View.GONE);
        binding.tvLabelInstrumentos.setVisibility(isProducer ? View.GONE : View.VISIBLE);
        binding.chipGroupInstrumentos.setVisibility(isProducer ? View.GONE : View.VISIBLE);
        binding.tvLabelNivel.setVisibility(isProducer ? View.GONE : View.VISIBLE);
        binding.chipGroupNivel.setVisibility(isProducer ? View.GONE : View.VISIBLE);
        binding.favoriteArtistsEditContainer.setVisibility(isProducer ? View.GONE : View.VISIBLE);
    }

    private boolean isProducerRole(String role) {
        return role != null && role.trim().equalsIgnoreCase(ROLE_PRODUCER);
    }

    private List<String> splitValues(String rawValue) {
        List<String> values = new ArrayList<>();
        if (rawValue == null || rawValue.trim().isEmpty()) return values;

        String[] parts = rawValue.split(",");
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty() && !values.contains(value)) values.add(value);
        }
        return values;
    }

    private String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append(", ");
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private void clearEditableChips() {
        clearChipGroup(binding.chipGroupRol);
        clearChipGroup(binding.chipGroupNivel);
        clearChipGroup(binding.chipGroupGeneros);
        clearChipGroup(binding.chipGroupInstrumentos);
    }

    private void clearChipGroup(ChipGroup group) {
        group.clearCheck();
        for (int i = 0; i < group.getChildCount(); i++) {
            ((Chip) group.getChildAt(i)).setChecked(false);
        }
    }

    private void markChip(ChipGroup group, String text) {
        if (text == null || text.trim().isEmpty()) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(text.trim())) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private String getSelectedChip(ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) return "";
        Chip chip = group.findViewById(id);
        return chip != null ? chip.getText().toString() : "";
    }

    private List<String> getSelectedChips(ChipGroup group) {
        List<String> selected = new ArrayList<>();
        for (int id : group.getCheckedChipIds()) {
            Chip chip = group.findViewById(id);
            if (chip != null) selected.add(chip.getText().toString());
        }
        return selected;
    }

    private int countValues(List<String> values) {
        if (values == null) return 0;
        int count = 0;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) count++;
        }
        return count;
    }

    private String valueOrFallback(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
