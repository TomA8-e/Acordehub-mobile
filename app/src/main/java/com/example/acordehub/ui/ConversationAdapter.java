package com.example.acordehub.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.acordehub.R;
import com.example.acordehub.databinding.ItemConversationBinding;
import com.example.acordehub.modelos.ChatConversation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private final String currentUid;
    private final OnConversationClickListener listener;
    private final List<ChatConversation> conversations = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ConversationAdapter(String currentUid, OnConversationClickListener listener) {
        this.currentUid = currentUid;
        this.listener = listener;
    }

    public void setConversations(List<ChatConversation> newConversations) {
        conversations.clear();
        if (newConversations != null) conversations.addAll(newConversations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConversationBinding binding = ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ConversationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(conversations.get(position));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {

        private final ItemConversationBinding binding;

        ConversationViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatConversation conversation) {
            binding.tvConversationName.setText(conversation.getDisplayName(currentUid));
            String lastMessage = conversation.getLastMessage();
            binding.tvLastMessage.setText(lastMessage != null && !lastMessage.isEmpty()
                    ? lastMessage
                    : "Todavía no hay mensajes");

            if (conversation.getUpdatedAt() != null) {
                binding.tvConversationTime.setText(timeFormat.format(conversation.getUpdatedAt()));
            } else {
                binding.tvConversationTime.setText("");
            }

            String photo = conversation.getDisplayPhoto(currentUid);
            Glide.with(binding.ivConversationPhoto)
                    .load(photo)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .into(binding.ivConversationPhoto);

            binding.getRoot().setOnClickListener(v -> listener.onConversationClick(conversation));
        }
    }

    public interface OnConversationClickListener {
        void onConversationClick(ChatConversation conversation);
    }
}
