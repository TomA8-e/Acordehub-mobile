package com.example.acordehub.ui;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.acordehub.chat.ChatVistaModelo;
import com.example.acordehub.databinding.ActivityChatDetailBinding;

public class ChatDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "extra_chat_id";
    public static final String EXTRA_CHAT_TITLE = "extra_chat_title";

    private ActivityChatDetailBinding binding;
    private ChatVistaModelo chatVistaModelo;
    private MessageAdapter adapter;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        String title = getIntent().getStringExtra(EXTRA_CHAT_TITLE);
        if (chatId == null || chatId.isEmpty()) {
            finish();
            return;
        }

        chatVistaModelo = new ViewModelProvider(this).get(ChatVistaModelo.class);
        adapter = new MessageAdapter(chatVistaModelo.getCurrentUid());
        binding.rvMessages.setAdapter(adapter);
        binding.tvChatTitle.setText(title != null ? title : "Chat");

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        observeViewModel();
        chatVistaModelo.escucharMensajes(chatId);
    }

    private void observeViewModel() {
        chatVistaModelo.getMessagesLiveData().observe(this, messages -> {
            adapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size() - 1);
            }
        });

        chatVistaModelo.getErrorLiveData().observe(this, error -> {
            if (error != null) Toast.makeText(this, formatError(error), Toast.LENGTH_LONG).show();
        });
    }

    private void sendMessage() {
        String text = binding.etMessage.getText().toString();
        if (text.trim().isEmpty()) return;
        chatVistaModelo.enviarMensaje(chatId, text);
        binding.etMessage.setText("");
    }

    private String formatError(String error) {
        if (error.contains("PERMISSION_DENIED") || error.contains("insufficient permissions")) {
            return "Faltan permisos de Firestore para usar el chat.";
        }
        return error;
    }
}
