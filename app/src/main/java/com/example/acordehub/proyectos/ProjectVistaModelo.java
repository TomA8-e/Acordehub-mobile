package com.example.acordehub.proyectos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.acordehub.modelos.Project;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ProjectVistaModelo extends ViewModel {

    private final ProjectRepository repository = new ProjectRepository();
    private final MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> activeProjectsCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private ListenerRegistration projectsListener;
    private ListenerRegistration countListener;

    public LiveData<List<Project>> getProjectsLiveData() { return projectsLiveData; }
    public LiveData<Integer> getActiveProjectsCountLiveData() { return activeProjectsCountLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }

    public void escucharProyectosDestacados() {
        if (projectsListener != null) projectsListener.remove();
        projectsListener = repository.listenToFeaturedProjects(new ProjectRepository.ProjectsCallback() {
            @Override
            public void onSuccess(List<Project> projects) {
                projectsLiveData.setValue(projects);
            }

            @Override
            public void onError(String error) {
                errorLiveData.setValue(error);
            }
        });
    }

    public void escucharMisProyectosActivos() {
        if (countListener != null) countListener.remove();
        countListener = repository.listenToMyActiveProjectsCount(new ProjectRepository.CountCallback() {
            @Override
            public void onSuccess(int count) {
                activeProjectsCountLiveData.setValue(count);
            }

            @Override
            public void onError(String error) {
                errorLiveData.setValue(error);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (projectsListener != null) projectsListener.remove();
        if (countListener != null) countListener.remove();
    }
}
