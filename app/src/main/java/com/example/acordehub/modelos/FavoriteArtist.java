package com.example.acordehub.modelos;

public class FavoriteArtist {
    private String name;
    private String imageUrl;

    public FavoriteArtist() {} // Requerido por Firestore

    public FavoriteArtist(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}