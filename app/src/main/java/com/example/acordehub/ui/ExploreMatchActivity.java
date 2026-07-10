package com.example.acordehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.acordehub.chat.ChatRepository;
import com.example.acordehub.databinding.ActivityExploreMatchBinding;
import com.example.acordehub.match.MatchCandidate;
import com.example.acordehub.modelos.UserModel;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ExploreMatchActivity extends AppCompatActivity {

    private static final String ALL_FILTER = "Todos";

    private ActivityExploreMatchBinding binding;
    private MatchCandidateAdapter adapter;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final ChatRepository chatRepository = new ChatRepository();
    private final List<MatchCandidate> candidates = new ArrayList<>();
    private UserModel currentUser;
    private String actionLoadingUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExploreMatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        setupRecyclerView();
        setupSearch();
        setupInitialFilters();
        loadCandidates();
    }

    private void setupRecyclerView() {
        adapter = new MatchCandidateAdapter(this::openPublicProfile);
        binding.rvCandidates.setAdapter(adapter);
    }

    private void openPublicProfile(MatchCandidate candidate) {
        UserModel user = candidate.getUser();
        if (user == null || isBlank(user.getUid())) return;
        Intent intent = new Intent(this, PublicProfileActivity.class);
        intent.putExtra(PublicProfileActivity.EXTRA_USER_UID, user.getUid());
        intent.putExtra(PublicProfileActivity.EXTRA_MATCH_SCORE, candidate.getScore());
        startActivity(intent);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                applyFilters();
            }
        });
    }

    private void setupInitialFilters() {
        List<String> defaults = Collections.singletonList(ALL_FILTER);
        setDropdownOptions(binding.dropdownGenre, defaults);
        setDropdownOptions(binding.dropdownInstrument, defaults);
        setDropdownOptions(binding.dropdownLevel, defaults);

        binding.dropdownGenre.setOnItemClickListener((parent, view, position, id) -> applyFilters());
        binding.dropdownInstrument.setOnItemClickListener((parent, view, position, id) -> applyFilters());
        binding.dropdownLevel.setOnItemClickListener((parent, view, position, id) -> applyFilters());
    }

    private void loadCandidates() {
        String uid = getCurrentUid();
        if (uid == null) {
            Toast.makeText(this, "Inicia sesion para explorar", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setLoading(true);
        db.collection("users").document(uid).get()
                .addOnSuccessListener(currentSnapshot -> {
                    currentUser = currentSnapshot.toObject(UserModel.class);
                    if (currentUser == null) {
                        setLoading(false);
                        showEmptyState("Completa tu perfil para encontrar recomendaciones");
                        return;
                    }
                    if (isBlank(currentUser.getUid())) {
                        currentUser.setUid(uid);
                    }
                    loadUsers(uid, new HashSet<>());
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showEmptyState("No pudimos cargar tu perfil");
                });
    }

    private void loadLikedUsersAndCandidates(String uid) {
        db.collection("userLikes").document(uid)
                .collection("likedUsers")
                .get()
                .addOnSuccessListener(likesSnapshot -> {
                    Set<String> likedIds = new HashSet<>();
                    for (com.google.firebase.firestore.DocumentSnapshot document : likesSnapshot.getDocuments()) {
                        likedIds.add(document.getId());
                    }
                    loadUsers(uid, likedIds);
                })
                .addOnFailureListener(e -> loadUsers(uid, new HashSet<>()));
    }

    private void loadUsers(String uid, Set<String> likedIds) {
        db.collection("users").get()
                .addOnSuccessListener(usersSnapshot -> {
                    candidates.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot document : usersSnapshot.getDocuments()) {
                        UserModel user = document.toObject(UserModel.class);
                        if (user != null && isBlank(user.getUid())) {
                            user.setUid(document.getId());
                        }
                        if (user != null && shouldShowUser(uid, user, likedIds)) {
                            candidates.add(new MatchCandidate(currentUser, user));
                        }
                    }
                    Collections.sort(candidates, (a, b) -> b.getScore() - a.getScore());
                    populateFilterOptions();
                    setLoading(false);
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showEmptyState("No pudimos cargar usuarios");
                });
    }

    private boolean shouldShowUser(String currentUid, UserModel user, Set<String> likedIds) {
        return !isBlank(user.getUid())
                && !user.getUid().equals(currentUid)
                && !likedIds.contains(user.getUid());
    }

    private void populateFilterOptions() {
        String selectedGenre = selectedValue(binding.dropdownGenre);
        String selectedInstrument = selectedValue(binding.dropdownInstrument);
        String selectedLevel = selectedValue(binding.dropdownLevel);
        Set<String> genres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> instruments = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> levels = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (MatchCandidate candidate : candidates) {
            UserModel user = candidate.getUser();
            addCleanValues(genres, user.getGenres());
            addCleanValues(instruments, user.getInstruments());
            addCleanValue(levels, user.getLevel());
        }

        setDropdownOptions(binding.dropdownGenre, buildOptions(genres), selectedGenre);
        setDropdownOptions(binding.dropdownInstrument, buildOptions(instruments), selectedInstrument);
        setDropdownOptions(binding.dropdownLevel, buildOptions(levels), selectedLevel);
    }

    private void setDropdownOptions(MaterialAutoCompleteTextView dropdown, List<String> options) {
        setDropdownOptions(dropdown, options, ALL_FILTER);
    }

    private void setDropdownOptions(MaterialAutoCompleteTextView dropdown, List<String> options, String selectedValue) {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);
        dropdown.setAdapter(arrayAdapter);
        dropdown.setText(options.contains(selectedValue) ? selectedValue : ALL_FILTER, false);
    }

    private List<String> buildOptions(Set<String> values) {
        List<String> options = new ArrayList<>();
        options.add(ALL_FILTER);
        options.addAll(values);
        return options;
    }

    private void applyFilters() {
        if (adapter == null) return;

        List<MatchCandidate> filteredCandidates = new ArrayList<>();
        String query = getText(binding.etSearch.getText()).toLowerCase(Locale.ROOT);
        String selectedGenre = selectedValue(binding.dropdownGenre);
        String selectedInstrument = selectedValue(binding.dropdownInstrument);
        String selectedLevel = selectedValue(binding.dropdownLevel);

        for (MatchCandidate candidate : candidates) {
            UserModel user = candidate.getUser();
            if (matchesSearch(user, query)
                    && matchesListFilter(user.getGenres(), selectedGenre)
                    && matchesListFilter(user.getInstruments(), selectedInstrument)
                    && matchesTextFilter(user.getLevel(), selectedLevel)) {
                filteredCandidates.add(candidate);
            }
        }

        adapter.setCandidates(filteredCandidates);
        boolean hasResults = !filteredCandidates.isEmpty();
        binding.rvCandidates.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        binding.emptyStateContainer.setVisibility(hasResults ? View.GONE : View.VISIBLE);

        if (candidates.isEmpty()) {
            binding.tvEmptyState.setText("No hay perfiles recomendados por ahora");
            binding.tvResultCount.setText("Aun no encontramos perfiles compatibles");
        } else if (hasResults) {
            binding.tvResultCount.setText(filteredCandidates.size() + " perfiles recomendados");
        } else {
            binding.tvEmptyState.setText("No hay perfiles que coincidan con la busqueda");
            binding.tvResultCount.setText("Ajusta los filtros para ver mas perfiles");
        }
    }

    private boolean matchesSearch(UserModel user, String query) {
        if (query.isEmpty()) return true;
        return contains(user.getName(), query)
                || contains(user.getRole(), query)
                || contains(user.getLocation(), query)
                || contains(user.getDescription(), query)
                || contains(user.getLevel(), query)
                || containsAny(user.getGenres(), query)
                || containsAny(user.getInstruments(), query);
    }

    private boolean matchesListFilter(List<String> values, String selected) {
        if (ALL_FILTER.equals(selected)) return true;
        if (values == null) return false;
        for (String value : values) {
            if (value != null && value.trim().equalsIgnoreCase(selected)) return true;
        }
        return false;
    }

    private boolean matchesTextFilter(String value, String selected) {
        return ALL_FILTER.equals(selected)
                || (value != null && value.trim().equalsIgnoreCase(selected));
    }

    private boolean containsAny(List<String> values, String query) {
        if (values == null) return false;
        for (String value : values) {
            if (contains(value, query)) return true;
        }
        return false;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    void likeCandidate(MatchCandidate candidate) {
        UserModel otherUser = candidate.getUser();
        String currentUid = getCurrentUid();
        if (currentUid == null || otherUser.getUid() == null || actionLoadingUid != null) return;

        setActionLoading(otherUser.getUid());
        Map<String, Object> like = new HashMap<>();
        like.put("targetUid", otherUser.getUid());
        like.put("targetName", otherUser.getName());
        like.put("score", candidate.getScore());
        like.put("createdAt", FieldValue.serverTimestamp());

        db.collection("userLikes").document(currentUid)
                .collection("likedUsers").document(otherUser.getUid())
                .set(like, SetOptions.merge())
                .addOnSuccessListener(unused -> checkForMutualMatch(candidate))
                .addOnFailureListener(e -> {
                    setActionLoading(null);
                    Toast.makeText(this, "No pudimos guardar el interes", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkForMutualMatch(MatchCandidate candidate) {
        UserModel otherUser = candidate.getUser();
        String currentUid = getCurrentUid();
        if (currentUid == null || otherUser.getUid() == null) {
            setActionLoading(null);
            return;
        }

        db.collection("userLikes").document(otherUser.getUid())
                .collection("likedUsers").document(currentUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        createMatchAndOpenChat(candidate);
                    } else {
                        setActionLoading(null);
                        Toast.makeText(this, "Interes enviado", Toast.LENGTH_SHORT).show();
                        removeCandidate(candidate);
                    }
                })
                .addOnFailureListener(e -> {
                    setActionLoading(null);
                    Toast.makeText(this, "Interes enviado", Toast.LENGTH_SHORT).show();
                    removeCandidate(candidate);
                });
    }

    private void createMatchAndOpenChat(MatchCandidate candidate) {
        UserModel otherUser = candidate.getUser();
        String currentUid = getCurrentUid();
        if (currentUid == null || otherUser.getUid() == null) {
            setActionLoading(null);
            return;
        }

        String matchId = buildMatchId(currentUid, otherUser.getUid());
        Map<String, Object> match = new HashMap<>();
        match.put("participantIds", Arrays.asList(currentUid, otherUser.getUid()));
        match.put("participantNames", buildNamesMap(currentUid, otherUser));
        match.put("createdAt", FieldValue.serverTimestamp());
        match.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("matches").document(matchId)
                .set(match, SetOptions.merge())
                .addOnSuccessListener(unused -> openChatForMatch(candidate))
                .addOnFailureListener(e -> {
                    setActionLoading(null);
                    Toast.makeText(this, "Hay match, pero no pudimos guardarlo", Toast.LENGTH_LONG).show();
                });
    }

    private void openChatForMatch(MatchCandidate candidate) {
        UserModel otherUser = candidate.getUser();
        if (otherUser.getEmail() == null || otherUser.getEmail().trim().isEmpty()) {
            setActionLoading(null);
            Toast.makeText(this, "Match! Ya pueden conectarse", Toast.LENGTH_LONG).show();
            removeCandidate(candidate);
            return;
        }

        chatRepository.startChatWithEmail(otherUser.getEmail(), new ChatRepository.StartChatCallback() {
            @Override
            public void onSuccess(String chatId, String title) {
                setActionLoading(null);
                Toast.makeText(ExploreMatchActivity.this, "Match!", Toast.LENGTH_LONG).show();
                removeCandidate(candidate);
                Intent intent = new Intent(ExploreMatchActivity.this, ChatDetailActivity.class);
                intent.putExtra(ChatDetailActivity.EXTRA_CHAT_ID, chatId);
                intent.putExtra(ChatDetailActivity.EXTRA_CHAT_TITLE, title);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                setActionLoading(null);
                Toast.makeText(ExploreMatchActivity.this, "Match! " + error, Toast.LENGTH_LONG).show();
                removeCandidate(candidate);
            }
        });
    }

    private void removeCandidate(MatchCandidate candidate) {
        String uid = candidate.getUser().getUid();
        for (int i = candidates.size() - 1; i >= 0; i--) {
            String candidateUid = candidates.get(i).getUser().getUid();
            if (candidateUid != null && candidateUid.equals(uid)) {
                candidates.remove(i);
            }
        }
        populateFilterOptions();
        applyFilters();
    }

    private Map<String, String> buildNamesMap(String currentUid, UserModel otherUser) {
        Map<String, String> names = new HashMap<>();
        names.put(currentUid, currentUser != null && currentUser.getName() != null ? currentUser.getName() : "Usuario");
        names.put(otherUser.getUid(), otherUser.getName() != null ? otherUser.getName() : "Usuario");
        return names;
    }

    private String buildMatchId(String firstUid, String secondUid) {
        List<String> ids = new ArrayList<>(Arrays.asList(firstUid, secondUid));
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    private String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.rvCandidates.setVisibility(View.GONE);
            binding.emptyStateContainer.setVisibility(View.GONE);
            binding.tvResultCount.setText("Buscando perfiles recomendados...");
        }
    }

    private void showEmptyState(String message) {
        binding.rvCandidates.setVisibility(View.GONE);
        binding.emptyStateContainer.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setText(message);
    }

    private void setActionLoading(String uid) {
        actionLoadingUid = uid;
        adapter.setActionLoadingUid(uid);
    }

    private void addCleanValues(Set<String> target, List<String> values) {
        if (values == null) return;
        for (String value : values) {
            addCleanValue(target, value);
        }
    }

    private void addCleanValue(Set<String> target, String value) {
        if (value != null && !value.trim().isEmpty()) {
            target.add(value.trim());
        }
    }

    private String selectedValue(MaterialAutoCompleteTextView dropdown) {
        String value = getText(dropdown.getText());
        return value.isEmpty() ? ALL_FILTER : value;
    }

    private String getText(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
