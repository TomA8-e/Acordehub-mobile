package com.example.acordehub.modelos;

import com.google.firebase.Timestamp;

public class ConnectionRequest {
    private String id;
    private String requesterUid;
    private String requesterName;
    private String requesterRole;
    private String targetUid;
    private String targetName;
    private String targetRole;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ConnectionRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterUid() { return requesterUid; }
    public void setRequesterUid(String requesterUid) { this.requesterUid = requesterUid; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String requesterRole) { this.requesterRole = requesterRole; }

    public String getTargetUid() { return targetUid; }
    public void setTargetUid(String targetUid) { this.targetUid = targetUid; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
