package com.example.acordehub.match;

import com.example.acordehub.modelos.FavoriteArtist;
import com.example.acordehub.modelos.UserModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MatchCandidate {

    private final UserModel user;
    private final int score;
    private final String reason;

    public MatchCandidate(UserModel currentUser, UserModel user) {
        this.user = user;
        this.score = calculateScore(currentUser, user);
        this.reason = buildReason(currentUser, user);
    }

    public UserModel getUser() { return user; }
    public int getScore() { return score; }
    public String getReason() { return reason; }

    private int calculateScore(UserModel currentUser, UserModel otherUser) {
        int total = 35;
        if (sameText(currentUser.getRole(), otherUser.getRole())) total += 15;
        total += Math.min(20, overlapCount(currentUser.getGenres(), otherUser.getGenres()) * 10);
        total += Math.min(15, overlapCount(currentUser.getInstruments(), otherUser.getInstruments()) * 8);
        total += Math.min(15, artistOverlapCount(currentUser.getFavoriteArtists(), otherUser.getFavoriteArtists()) * 8);
        if (sameText(currentUser.getLocation(), otherUser.getLocation())) total += 10;
        if (sameText(currentUser.getLevel(), otherUser.getLevel())) total += 5;
        return Math.min(total, 99);
    }

    private String buildReason(UserModel currentUser, UserModel otherUser) {
        if (overlapCount(currentUser.getGenres(), otherUser.getGenres()) > 0) return "Coinciden en generos";
        if (overlapCount(currentUser.getInstruments(), otherUser.getInstruments()) > 0) return "Comparten instrumentos";
        if (artistOverlapCount(currentUser.getFavoriteArtists(), otherUser.getFavoriteArtists()) > 0) return "Escuchan artistas similares";
        if (sameText(currentUser.getRole(), otherUser.getRole())) return "Tienen un rol parecido";
        if (sameText(currentUser.getLocation(), otherUser.getLocation())) return "Estan cerca musicalmente";
        return "Puede ser una buena conexion";
    }

    private boolean sameText(String first, String second) {
        return first != null && second != null
                && !first.trim().isEmpty()
                && first.trim().equalsIgnoreCase(second.trim());
    }

    private int overlapCount(List<String> first, List<String> second) {
        if (first == null || second == null || first.isEmpty() || second.isEmpty()) return 0;
        Set<String> normalized = new HashSet<>();
        for (String value : first) {
            if (value != null) normalized.add(value.trim().toLowerCase());
        }

        int count = 0;
        for (String value : second) {
            if (value != null && normalized.contains(value.trim().toLowerCase())) count++;
        }
        return count;
    }

    private int artistOverlapCount(List<FavoriteArtist> first, List<FavoriteArtist> second) {
        if (first == null || second == null || first.isEmpty() || second.isEmpty()) return 0;
        Set<String> normalized = new HashSet<>();
        for (FavoriteArtist artist : first) {
            if (artist.getName() != null) normalized.add(artist.getName().trim().toLowerCase());
        }

        int count = 0;
        for (FavoriteArtist artist : second) {
            if (artist.getName() != null && normalized.contains(artist.getName().trim().toLowerCase())) count++;
        }
        return count;
    }
}
