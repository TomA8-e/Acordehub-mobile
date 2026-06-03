package com.example.acordehub.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.acordehub.modelos.ChatConversation;
import com.example.acordehub.modelos.ChatMessage;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ChatVistaModelo extends ViewModel {

    private final ChatRepository repository = new ChatRepository();
    private final MutableLiveData<List<ChatConversation>> conversationsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> openedChatLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> openedChatTitleLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private ListenerRegistration conversationsListener;
    private ListenerRegistration messagesListener;

    public LiveData<List<ChatConversation>> getConversationsLiveData() { return conversationsLiveData; }
    public LiveData<List<ChatMessage>> getMessagesLiveData() { return messagesLiveData; }
    public LiveData<String> getOpenedChatLiveData() { return openedChatLiveData; }
    public LiveData<String> getOpenedChatTitleLiveData() { return openedChatTitleLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }
    public LiveData<Boolean> getLoadingLiveData() { return loadingLiveData; }

    public void escucharConversaciones() {
        if (conversationsListener != null) conversationsListener.remove();
        conversationsListener = repository.listenToConversations(new ChatRepository.ConversationsCallback() {
            @Override
            public void onSuccess(List<ChatConversation> conversations) {
                conversationsLiveData.setValue(conversations);
            }

            @Override
            public void onError(String error) {
                errorLiveData.setValue(error);
            }
        });
    }

    public void escucharMensajes(String chatId) {
        if (messagesListener != null) messagesListener.remove();
        messagesListener = repository.listenToMessages(chatId, new ChatRepository.MessagesCallback() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                messagesLiveData.setValue(messages);
            }

            @Override
            public void onError(String error) {
                errorLiveData.setValue(error);
            }
        });
    }

    public void iniciarChatConEmail(String email) {
        loadingLiveData.setValue(true);
        repository.startChatWithEmail(email, new ChatRepository.StartChatCallback() {
            @Override
            public void onSuccess(String chatId, String title) {
                loadingLiveData.setValue(false);
                openedChatTitleLiveData.setValue(title != null ? title : "Chat");
                openedChatLiveData.setValue(chatId);
            }

            @Override
            public void onError(String error) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(error);
            }
        });
    }

    public void limpiarChatAbierto() {
        openedChatLiveData.setValue(null);
        openedChatTitleLiveData.setValue(null);
    }

    public void enviarMensaje(String chatId, String text) {
        repository.sendMessage(chatId, text, new ChatRepository.SimpleCallback() {
            @Override
            public void onSuccess() {}

            @Override
            public void onError(String error) {
                errorLiveData.setValue(error);
            }
        });
    }

    public String getCurrentUid() {
        return repository.getCurrentUidOrEmpty();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (conversationsListener != null) conversationsListener.remove();
        if (messagesListener != null) messagesListener.remove();
    }
}
