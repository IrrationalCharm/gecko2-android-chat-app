package com.dominik.Gecko2Chat.activity.main_activity.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.model.ChatModel;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatModel> chatList;
    private final OnChatClickListener listener;

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

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));

        //TODO set avatar image here
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    //Inner class the hold teh views
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMessage;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}
