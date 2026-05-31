package com.example.acordehub.modelos.spotify;

import java.util.List;

public class SpotifySearchResponse {
    private Artists artists;

    public Artists getArtists() { return artists; }

    public static class Artists {
        private List<Artist> items;
        public List<Artist> getItems() { return items; }
    }
}