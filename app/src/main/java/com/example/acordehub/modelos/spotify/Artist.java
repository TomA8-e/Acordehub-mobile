package com.example.acordehub.modelos.spotify;

import java.util.List;

public class Artist {
    private String id;
    private String name;
    private List<SpotifyImage> images;

    public String getId() { return id; }
    public String getName() { return name; }
    public List<SpotifyImage> getImages() { return images; }

    public static class SpotifyImage {
        private String url;
        public String getUrl() { return url; }
    }
}