package com.example.acordehub.chat;

import androidx.annotation.Nullable;

import com.example.acordehub.modelos.ChatConversation;
import com.example.acordehub.modelos.ChatMessage;
import com.example.acordehub.modelos.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ListenerRegistration listenToConversations(ConversationsCallback callback) {
        String uid = getCurrentUid();
        if (uid == null) {
            callback.onError("Usuario no autenticado");
            return null;
        }

        return db.collection("chats")
                .whereArrayContains("participantIds", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    List<ChatConversation> conversations = new ArrayList<>();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : snapshots.getDocuments()) {
                            ChatConversation conversation = document.toObject(ChatConversation.class);
                            if (conversation != null) {
                                conversation.setId(document.getId());
                                conversations.add(conversation);
                            }
                        }
                    }

                    Collections.sort(conversations, (a, b) -> {
                        if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
                        if (a.getUpdatedAt() == null) return 1;
                        if (b.getUpdatedAt() == null) return -1;
                        return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                    });
                    callback.onSuccess(conversations);
                });
    }

    public ListenerRegistration listenToMessages(String chatId, MessagesCallback callback) {
        return db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    List<ChatMessage> messages = new ArrayList<>();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : snapshots.getDocuments()) {
                            ChatMessage message = document.toObject(ChatMessage.class);
                            if (message != null) {
                                message.setId(document.getId());
                                messages.add(message);
                            }
                        }
                    }
                    callback.onSuccess(messages);
                });
    }

    public void startChatWithEmail(String rawEmail, StartChatCallback callback) {
        String email = rawEmail.trim().toLowerCase();
        String currentUid = getCurrentUid();
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (currentUid == null || firebaseUser == null) {
            callback.onError("Usuario no autenticado");
            return;
        }
        if (email.isEmpty()) {
            callback.onError("Ingresá un email");
            return;
        }
        if (firebaseUser.getEmail() != null && firebaseUser.getEmail().equalsIgnoreCase(email)) {
            callback.onError("No podés iniciar un chat con vos mismo");
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        callback.onError("No encontramos un usuario con ese email");
                        return;
                    }

                    UserModel otherUser = query.getDocuments().get(0).toObject(UserModel.class);
                    if (otherUser == null || otherUser.getUid() == null) {
                        callback.onError("No se pudo cargar el usuario");
                        return;
                    }
                    createOrOpenChat(currentUid, firebaseUser, otherUser, callback);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void startChatWithUid(String otherUid, StartChatCallback callback) {
        String currentUid = getCurrentUid();
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (currentUid == null || firebaseUser == null) {
            callback.onError("Usuario no autenticado");
            return;
        }
        if (otherUid == null || otherUid.trim().isEmpty()) {
            callback.onError("No se pudo identificar al usuario");
            return;
        }
        if (currentUid.equals(otherUid)) {
            callback.onError("No podes iniciar un chat con vos mismo");
            return;
        }

        db.collection("users").document(otherUid).get()
                .addOnSuccessListener(snapshot -> {
                    UserModel otherUser = snapshot.toObject(UserModel.class);
                    if (otherUser == null) {
                        callback.onError("No se pudo cargar el usuario");
                        return;
                    }
                    if (otherUser.getUid() == null || otherUser.getUid().trim().isEmpty()) {
                        otherUser.setUid(snapshot.getId());
                    }
                    createOrOpenChat(currentUid, firebaseUser, otherUser, callback);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendMessage(String chatId, String rawText, SimpleCallback callback) {
        String uid = getCurrentUid();
        if (uid == null) {
            callback.onError("Usuario no autenticado");
            return;
        }

        String text = rawText.trim();
        if (text.isEmpty()) {
            callback.onError("Escribí un mensaje");
            return;
        }

        DocumentReference chatRef = db.collection("chats").document(chatId);
        ChatMessage message = new ChatMessage(uid, text);

        chatRef.collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", text);
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    chatRef.set(updates, SetOptions.merge())
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public String getCurrentUidOrEmpty() {
        String uid = getCurrentUid();
        return uid != null ? uid : "";
    }

    private void createOrOpenChat(String currentUid, FirebaseUser firebaseUser, UserModel otherUser,
                                  StartChatCallback callback) {
        String otherUid = otherUser.getUid();
        String chatId = buildChatId(currentUid, otherUid);
        DocumentReference chatRef = db.collection("chats").document(chatId);

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(currentSnapshot -> {
                    UserModel currentUser = currentSnapshot.toObject(UserModel.class);
                    Map<String, Object> data = new HashMap<>();
                    data.put("participantIds", Arrays.asList(currentUid, otherUid));
                    data.put("participantNames", buildNamesMap(currentUid, firebaseUser, currentUser, otherUser));
                    data.put("participantEmails", buildEmailsMap(currentUid, firebaseUser, currentUser, otherUser));
                    data.put("participantPhotos", buildPhotosMap(currentUid, currentUser, otherUser));
                    chatRef.get()
                            .addOnSuccessListener(chatSnapshot -> {
                                if (!chatSnapshot.exists()) {
                                    data.put("createdAt", FieldValue.serverTimestamp());
                                }
                                data.put("updatedAt", FieldValue.serverTimestamp());
                                chatRef.set(data, SetOptions.merge())
                                        .addOnSuccessListener(unused -> callback.onSuccess(chatId, otherUser.getName()))
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private Map<String, String> buildNamesMap(String currentUid, FirebaseUser firebaseUser,
                                              @Nullable UserModel currentUser, UserModel otherUser) {
        Map<String, String> names = new HashMap<>();
        String currentName = currentUser != null ? currentUser.getName() : null;
        if (currentName == null || currentName.trim().isEmpty()) {
            currentName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Usuario";
        }
        names.put(currentUid, currentName);
        names.put(otherUser.getUid(), otherUser.getName() != null ? otherUser.getName() : "Usuario");
        return names;
    }

    private Map<String, String> buildEmailsMap(String currentUid, FirebaseUser firebaseUser,
                                               @Nullable UserModel currentUser, UserModel otherUser) {
        Map<String, String> emails = new HashMap<>();
        String currentEmail = currentUser != null ? currentUser.getEmail() : null;
        if (currentEmail == null || currentEmail.trim().isEmpty()) currentEmail = firebaseUser.getEmail();
        emails.put(currentUid, currentEmail != null ? currentEmail : "");
        emails.put(otherUser.getUid(), otherUser.getEmail() != null ? otherUser.getEmail() : "");
        return emails;
    }

    private Map<String, String> buildPhotosMap(String currentUid, @Nullable UserModel currentUser,
                                               UserModel otherUser) {
        Map<String, String> photos = new HashMap<>();
        photos.put(currentUid, currentUser != null && currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl() : "");
        photos.put(otherUser.getUid(), otherUser.getPhotoUrl() != null ? otherUser.getPhotoUrl() : "");
        return photos;
    }

    private String buildChatId(String firstUid, String secondUid) {
        List<String> ids = new ArrayList<>(Arrays.asList(firstUid, secondUid));
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    private String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public interface ConversationsCallback {
        void onSuccess(List<ChatConversation> conversations);
        void onError(String error);
    }

    public interface MessagesCallback {
        void onSuccess(List<ChatMessage> messages);
        void onError(String error);
    }

    public interface StartChatCallback {
        void onSuccess(String chatId, String title);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }
}
