package com.dominik.Gecko2Chat.activity.main_activity.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.model.ChatModel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatModel> chatList;
    private final OnChatClickListener listener;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    public interface OnChatClickListener {
        void onChatClick(ChatModel chat);
    }

    public ChatAdapter(List<ChatModel> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatAdapter.ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Inflate item_chat.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);

        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatAdapter.ChatViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);
        holder.tvName.setText(chat.name());
        holder.tvMessage.setText(chat.lastMessage());
        holder.tvTimestamp.setText(formatTime(chat.timestamp()));

        if (chat.unreadCount() == 0) holder.cvUnreadBadge.setVisibility(View.GONE);

        if (chat.unreadCount() > 0) {
            holder.cvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(chat.unreadCount()));
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));

        Glide.with(holder.itemView.getContext())
                .load(chat.avatar().contains("null") ? R.drawable.person_icon : chat.avatar())
                .placeholder(R.drawable.person_icon)
                .error(R.drawable.person_icon)
                .circleCrop()
                .into(holder.imgAvatar);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private static String formatTime(Instant instant) {
        return instant.atZone(ZoneId.systemDefault())
                .format(TIME_FORMATTER);
    }

    //Inner class the hold teh views
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        CardView cvUnreadBadge;
        ImageView imgAvatar;
        TextView tvName, tvMessage, tvUnreadCount, tvTimestamp;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            cvUnreadBadge = itemView.findViewById(R.id.cvUnreadBadge);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
