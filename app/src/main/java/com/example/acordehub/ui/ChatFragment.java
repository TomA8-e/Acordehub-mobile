package com.example.acordehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.acordehub.chat.ChatVistaModelo;
import com.example.acordehub.databinding.FragmentChatBinding;
import com.example.acordehub.modelos.ChatConversation;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatVistaModelo chatVistaModelo;
    private ConversationAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatVistaModelo = new ViewModelProvider(this).get(ChatVistaModelo.class);
        adapter = new ConversationAdapter(chatVistaModelo.getCurrentUid(), this::openConversation);
        binding.rvConversations.setAdapter(adapter);

        binding.fabNewChat.setOnClickListener(v -> showNewChatDialog());
        observeViewModel();
        chatVistaModelo.escucharConversaciones();
    }

    private void observeViewModel() {
        chatVistaModelo.getConversationsLiveData().observe(getViewLifecycleOwner(), conversations -> {
            adapter.setConversations(conversations);
            boolean isEmpty = conversations == null || conversations.isEmpty();
            binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.rvConversations.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        chatVistaModelo.getOpenedChatLiveData().observe(getViewLifecycleOwner(), chatId -> {
            if (chatId != null && !chatId.isEmpty()) {
                String title = chatVistaModelo.getOpenedChatTitleLiveData().getValue();
                openChatDetail(chatId, title != null ? title : "Chat");
                chatVistaModelo.limpiarChatAbierto();
            }
        });

        chatVistaModelo.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), formatError(error), Toast.LENGTH_LONG).show();
            }
        });

        chatVistaModelo.getLoadingLiveData().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    private void showNewChatDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("email@ejemplo.com");
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        new AlertDialog.Builder(requireContext())
                .setTitle("Nuevo chat")
                .setMessage("Ingresá el email de otro usuario registrado.")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Iniciar", (dialog, which) ->
                        chatVistaModelo.iniciarChatConEmail(input.getText().toString()))
                .show();
    }

    private void openConversation(ChatConversation conversation) {
        openChatDetail(conversation.getId(), conversation.getDisplayName(chatVistaModelo.getCurrentUid()));
    }

    private void openChatDetail(String chatId, String title) {
        Intent intent = new Intent(requireContext(), ChatDetailActivity.class);
        intent.putExtra(ChatDetailActivity.EXTRA_CHAT_ID, chatId);
        intent.putExtra(ChatDetailActivity.EXTRA_CHAT_TITLE, title);
        startActivity(intent);
    }

    private String formatError(String error) {
        if (error.contains("PERMISSION_DENIED") || error.contains("insufficient permissions")) {
            return "Faltan permisos de Firestore para usar el chat.";
        }
        return error;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
