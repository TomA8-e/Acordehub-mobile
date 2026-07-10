package com.example.acordehub.proyectos;

import com.example.acordehub.modelos.Project;
import com.example.acordehub.modelos.ProjectJoinRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ListenerRegistration listenToFeaturedProjects(ProjectsCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Usuario no autenticado");
            return null;
        }

        return db.collection("projects")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    List<Project> projects = new ArrayList<>();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : snapshots.getDocuments()) {
                            Project project = document.toObject(Project.class);
                            if (project != null && project.isActive()) {
                                project.setId(document.getId());
                                projects.add(project);
                            }
                        }
                    }
                    callback.onSuccess(projects);
                });
    }

    public ListenerRegistration listenToMyActiveProjectsCount(CountCallback callback) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            callback.onError("Usuario no autenticado");
            return null;
        }

        return db.collection("projects")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    int count = 0;
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : snapshots.getDocuments()) {
                            Project project = document.toObject(Project.class);
                            if (project != null && project.isActive()) count++;
                        }
                    }
                    callback.onSuccess(count);
                });
    }

    public ListenerRegistration listenToReceivedJoinRequests(JoinRequestsCallback callback) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            callback.onError("Usuario no autenticado");
            return null;
        }

        return db.collectionGroup("joinRequests")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    List<ProjectJoinRequest> requests = new ArrayList<>();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : snapshots.getDocuments()) {
                            ProjectJoinRequest request = document.toObject(ProjectJoinRequest.class);
                            if (request != null && request.isPending()) {
                                request.setId(document.getId());
                                requests.add(request);
                            }
                        }
                    }

                    Collections.sort(requests, (a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    callback.onSuccess(requests);
                });
    }

    public void respondToJoinRequest(ProjectJoinRequest request, String status, SimpleCallback callback) {
        if (request == null || request.getProjectId() == null || request.getRequesterUid() == null) {
            callback.onError("Solicitud invalida");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("respondedAt", FieldValue.serverTimestamp());

        db.collection("projects").document(request.getProjectId())
                .collection("joinRequests").document(request.getRequesterUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface ProjectsCallback {
        void onSuccess(List<Project> projects);
        void onError(String error);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onError(String error);
    }

    public interface JoinRequestsCallback {
        void onSuccess(List<ProjectJoinRequest> requests);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }
}
