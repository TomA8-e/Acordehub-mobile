package com.example.acordehub.modelos;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Project {

    private String id;
    private String ownerUid;
    private String ownerName;
    private String title;
    private String description;
    private String genre;
    private String imageUri;
    private String demoUri;
    private String status;

    @ServerTimestamp
    private Date createdAt;

    public Project() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getDemoUri() { return demoUri; }
    public void setDemoUri(String demoUri) { this.demoUri = demoUri; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isActive() {
        return status == null || status.isEmpty() || "active".equals(status);
    }

    public boolean hasDemo() {
        return demoUri != null && !demoUri.trim().isEmpty();
    }
}
