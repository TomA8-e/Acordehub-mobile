package com.example.acordehub.proyectos;

import com.example.acordehub.modelos.Project;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

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

    public interface ProjectsCallback {
        void onSuccess(List<Project> projects);
        void onError(String error);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onError(String error);
    }
}
