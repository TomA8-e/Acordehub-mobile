package com.example.acordehub.modelos;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class ProjectJoinRequest {

    private String id;
    private String projectId;
    private String projectTitle;
    private String ownerUid;
    private String requesterUid;
    private String requesterName;
    private String requesterEmail;
    private String status;

    @ServerTimestamp
    private Date createdAt;

    public ProjectJoinRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public String getRequesterUid() { return requesterUid; }
    public void setRequesterUid(String requesterUid) { this.requesterUid = requesterUid; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isPending() {
        return status == null || status.trim().isEmpty() || "pending".equals(status);
    }
}
