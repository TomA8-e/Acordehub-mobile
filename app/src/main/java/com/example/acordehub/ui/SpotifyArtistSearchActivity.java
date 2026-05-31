package com.example.acordehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.acordehub.R;
import com.example.acordehub.api.SpotifyService;
import com.example.acordehub.databinding.ActivitySpotifySearchBinding;
import com.example.acordehub.modelos.spotify.Artist;
import com.example.acordehub.modelos.spotify.SpotifySearchResponse;
import com.example.acordehub.modelos.spotify.TokenResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SpotifyArtistSearchActivity extends AppCompatActivity {

    private static final String TAG = "SpotifySearch";

    private ActivitySpotifySearchBinding binding;
    private String accessToken;
    private SpotifyService spotifyService;
    private ArtistAdapter adapter;
    private boolean tokenLoading = false;
    private String lastQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpotifySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRetrofit();
        setupRecyclerView();
        setupSearch();

        showStatus("Conectando con Spotify...");
        fetchAccessToken();
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/v1/")
                .client(createHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        spotifyService = retrofit.create(SpotifyService.class);
    }

    private OkHttpClient createHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }

    private void fetchAccessToken() {
        if (tokenLoading) return;

        tokenLoading = true;
        binding.pbLoading.setVisibility(View.VISIBLE);
        showStatus("Conectando con Spotify...");

        String clientId = getString(R.string.spotify_client_id).trim();
        String clientSecret = getString(R.string.spotify_client_secret).trim();
        String credentials = clientId + ":" + clientSecret;
        String authHeader = "Basic " + Base64.encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP
        );

        Retrofit authRetrofit = new Retrofit.Builder()
                .baseUrl("https://accounts.spotify.com/")
                .client(createHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SpotifyService authService = authRetrofit.create(SpotifyService.class);
        authService.getToken(authHeader, "client_credentials")
                .enqueue(new Callback<TokenResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TokenResponse> call,
                                           @NonNull Response<TokenResponse> response) {
                        tokenLoading = false;
                        binding.pbLoading.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            accessToken = response.body().getAccessToken();
                            Log.d(TAG, "Token obtenido con exito");
                            showStatus("Escribi al menos 3 letras para buscar artistas.");
                            searchIfReady();
                            return;
                        }

                        String message = "Spotify no autorizo la app (" + response.code()
                                + "). Revisa client_id y client_secret.";
                        Log.e(TAG, message);
                        showError(message);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                        tokenLoading = false;
                        binding.pbLoading.setVisibility(View.GONE);
                        Log.e(TAG, "Fallo de red al obtener token", t);
                        showError("No se pudo conectar con Spotify. Revisa internet del dispositivo/emulador.");
                    }
                });
    }

    private void setupRecyclerView() {
        adapter = new ArtistAdapter(artist -> {
            Intent data = new Intent();
            data.putExtra("ARTIST_NAME", artist.getName());

            String imageUrl = "";
            if (artist.getImages() != null && !artist.getImages().isEmpty()) {
                imageUrl = artist.getImages().get(0).getUrl();
            }

            data.putExtra("ARTIST_IMAGE", imageUrl);
            setResult(RESULT_OK, data);
            finish();
        });

        binding.rvArtists.setLayoutManager(new LinearLayoutManager(this));
        binding.rvArtists.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lastQuery = s.toString().trim();
                searchIfReady();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchIfReady() {
        if (lastQuery.isEmpty()) {
            adapter.setArtists(new ArrayList<>());
            showStatus(accessToken == null
                    ? "Conectando con Spotify..."
                    : "Escribi al menos 3 letras para buscar artistas.");
            return;
        }

        if (lastQuery.length() < 3) {
            adapter.setArtists(new ArrayList<>());
            showStatus("Escribi al menos 3 letras para buscar artistas.");
            return;
        }

        if (accessToken == null) {
            showStatus("Esperando conexion con Spotify...");
            fetchAccessToken();
            return;
        }

        searchArtists(lastQuery);
    }

    private void searchArtists(String query) {
        binding.pbLoading.setVisibility(View.VISIBLE);
        showStatus("Buscando artistas...");

        spotifyService.searchArtists("Bearer " + accessToken, query, "artist", "AR")
                .enqueue(new Callback<SpotifySearchResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SpotifySearchResponse> call,
                                           @NonNull Response<SpotifySearchResponse> response) {
                        binding.pbLoading.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            SpotifySearchResponse.Artists artists = response.body().getArtists();
                            List<Artist> items = artists != null && artists.getItems() != null
                                    ? artists.getItems()
                                    : new ArrayList<>();

                            adapter.setArtists(items);
                            showStatus(items.isEmpty()
                                    ? "No encontramos artistas para esa busqueda."
                                    : "");
                            return;
                        }

                        String message = "Error de busqueda en Spotify (" + response.code() + "): "
                                + readErrorBody(response.errorBody());
                        Log.e(TAG, message);

                        if (response.code() == 401) {
                            accessToken = null;
                            fetchAccessToken();
                        } else {
                            showError(message);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SpotifySearchResponse> call,
                                          @NonNull Throwable t) {
                        binding.pbLoading.setVisibility(View.GONE);
                        Log.e(TAG, "Fallo de red al buscar artistas", t);
                        showError("No se pudo buscar en Spotify. Revisa la conexion del dispositivo/emulador.");
                    }
                });
    }

    private void showStatus(String message) {
        if (message == null || message.isEmpty()) {
            binding.tvSpotifyStatus.setVisibility(View.GONE);
            binding.tvSpotifyStatus.setText("");
            return;
        }

        binding.tvSpotifyStatus.setText(message);
        binding.tvSpotifyStatus.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        showStatus(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String readErrorBody(ResponseBody errorBody) {
        if (errorBody == null) return "sin detalle";

        try {
            return errorBody.string();
        } catch (Exception e) {
            Log.e(TAG, "No se pudo leer el detalle del error de Spotify", e);
            return "sin detalle";
        }
    }
}
