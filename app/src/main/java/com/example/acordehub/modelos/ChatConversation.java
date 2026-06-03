package com.example.acordehub.modelos;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatConversation {

    private String id;
    private List<String> participantIds;
    private Map<String, String> participantNames = new HashMap<>();
    private Map<String, String> participantEmails = new HashMap<>();
    private Map<String, String> participantPhotos = new HashMap<>();
    private String lastMessage;

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date updatedAt;

    public ChatConversation() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }

    public Map<String, String> getParticipantNames() { return participantNames; }
    public void setParticipantNames(Map<String, String> participantNames) { this.participantNames = participantNames; }

    public Map<String, String> getParticipantEmails() { return participantEmails; }
    public void setParticipantEmails(Map<String, String> participantEmails) { this.participantEmails = participantEmails; }

    public Map<String, String> getParticipantPhotos() { return participantPhotos; }
    public void setParticipantPhotos(Map<String, String> participantPhotos) { this.participantPhotos = participantPhotos; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getOtherParticipantId(String currentUid) {
        if (participantIds == null) return "";
        for (String participantId : participantIds) {
            if (!participantId.equals(currentUid)) return participantId;
        }
        return "";
    }

    public String getDisplayName(String currentUid) {
        String otherUid = getOtherParticipantId(currentUid);
        String name = participantNames != null ? participantNames.get(otherUid) : null;
        String email = participantEmails != null ? participantEmails.get(otherUid) : null;
        if (name != null && !name.trim().isEmpty()) return name;
        if (email != null && !email.trim().isEmpty()) return email;
        return "Usuario";
    }

    public String getDisplayPhoto(String currentUid) {
        String otherUid = getOtherParticipantId(currentUid);
        String photo = participantPhotos != null ? participantPhotos.get(otherUid) : null;
        return photo != null ? photo : "";
    }
}
