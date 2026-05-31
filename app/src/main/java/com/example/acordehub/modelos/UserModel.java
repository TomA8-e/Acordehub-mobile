// Archivo: app/src/main/java/com/example/acordehub/modelos/UserModel.java

package com.example.acordehub.modelos;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class UserModel {

    private String uid;
    private String name;
    private String email;
    private String photoUrl;
    private boolean isPremium;

    // ── Campos del perfil musical (Fase 2) ───────────────────────────────────
    private String role;           // "Músico", "Productor", "Cantante", etc.
    private List<String> genres;   // ["Rock", "Pop", "Metal", ...]
    private List<String> instruments; // ["Guitarra", "Piano", ...]
    private String level;          // "Principiante", "Intermedio", "Avanzado"
    private String description;    // descripción personal
    private String location;       // ciudad/provincia
    private List<FavoriteArtist> favoriteArtists; // Lista de objetos con nombre e imagen

    @ServerTimestamp
    private Date createdAt;

    // Constructor vacío requerido por Firestore
    public UserModel() {}

    // Constructor registro inicial
    public UserModel(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.photoUrl = "";
        this.isPremium = false;
        this.role = "";
        this.genres = new ArrayList<>();
        this.instruments = new ArrayList<>();
        this.level = "";
        this.description = "";
        this.location = "";
        this.favoriteArtists = new ArrayList<>();
    }

    // ── Getters y Setters ─────────────────────────────────────────────────────

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public List<String> getInstruments() { return instruments; }
    public void setInstruments(List<String> instruments) { this.instruments = instruments; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<FavoriteArtist> getFavoriteArtists() { return favoriteArtists; }
    public void setFavoriteArtists(List<FavoriteArtist> favoriteArtists) { this.favoriteArtists = favoriteArtists; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}