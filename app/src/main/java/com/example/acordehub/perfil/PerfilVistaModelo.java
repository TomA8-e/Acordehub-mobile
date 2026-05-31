package com.example.acordehub.perfil;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.acordehub.modelos.FavoriteArtist;
import com.example.acordehub.modelos.UserModel;

import java.util.List;

public class PerfilVistaModelo extends ViewModel {

    private final PerfilRepository repository;

    private final MutableLiveData<UserModel> perfilLiveData   = new MutableLiveData<>();
    private final MutableLiveData<String>    errorLiveData    = new MutableLiveData<>();
    private final MutableLiveData<Boolean>   loadingLiveData  = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>   guardadoLiveData = new MutableLiveData<>();
    private final MutableLiveData<String>    fotoUrlLiveData  = new MutableLiveData<>();

    public LiveData<UserModel> getPerfilLiveData()   { return perfilLiveData; }
    public LiveData<String>    getErrorLiveData()    { return errorLiveData; }
    public LiveData<Boolean>   getLoadingLiveData()  { return loadingLiveData; }
    public LiveData<Boolean>   getGuardadoLiveData() { return guardadoLiveData; }
    public LiveData<String>    getFotoUrlLiveData()  { return fotoUrlLiveData; }

    public PerfilVistaModelo() {
        this.repository = new PerfilRepository();
    }

    // ── Cargar perfil desde Firestore ─────────────────────────────────────────

    public void cargarPerfil() {
        loadingLiveData.setValue(true);
        repository.getPerfil(new PerfilRepository.PerfilCallback() {
            @Override
            public void onSuccess(UserModel user) {
                loadingLiveData.setValue(false);
                perfilLiveData.setValue(user);
            }
            @Override
            public void onError(String error) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(error);
            }
        });
    }

    // ── Guardar perfil ────────────────────────────────────────────────────────

    public void guardarPerfil(String name, String role, List<String> genres,
                              List<String> instruments, String level,
                              String description, String location, String photoUrl,
                              List<FavoriteArtist> favoriteArtists) {
        loadingLiveData.setValue(true);

        UserModel user = new UserModel();
        user.setName(name);
        user.setRole(role);
        user.setGenres(genres);
        user.setInstruments(instruments);
        user.setLevel(level);
        user.setDescription(description);
        user.setLocation(location);
        user.setPhotoUrl(photoUrl != null ? photoUrl : "");
        user.setFavoriteArtists(favoriteArtists);

        repository.guardarPerfil(user, new PerfilRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                loadingLiveData.setValue(false);
                guardadoLiveData.setValue(true);
            }
            @Override
            public void onError(String error) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(error);
            }
        });
    }

    // ── Subir foto ────────────────────────────────────────────────────────────

    public void subirFoto(Uri imageUri) {
        loadingLiveData.setValue(true);
        repository.subirFoto(imageUri, new PerfilRepository.UrlCallback() {
            @Override
            public void onSuccess(String url) {
                loadingLiveData.setValue(false);
                fotoUrlLiveData.setValue(url);
            }
            @Override
            public void onError(String error) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(error);
            }
        });
    }
}