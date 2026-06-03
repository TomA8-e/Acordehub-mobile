package com.example.acordehub.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acordehub.databinding.ItemMessageReceivedBinding;
import com.example.acordehub.databinding.ItemMessageSentBinding;
import com.example.acordehub.modelos.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final String currentUid;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageAdapter(String currentUid) {
        this.currentUid = currentUid;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return currentUid.equals(message.getSenderId()) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            return new SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false));
        }
        return new ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class SentViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageSentBinding binding;

        SentViewHolder(ItemMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message) {
            binding.tvMessageText.setText(message.getText());
            binding.tvMessageTime.setText(message.getCreatedAt() != null ? timeFormat.format(message.getCreatedAt()) : "");
        }
    }

    class ReceivedViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageReceivedBinding binding;

        ReceivedViewHolder(ItemMessageReceivedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message) {
            binding.tvMessageText.setText(message.getText());
            binding.tvMessageTime.setText(message.getCreatedAt() != null ? timeFormat.format(message.getCreatedAt()) : "");
        }
    }
}
