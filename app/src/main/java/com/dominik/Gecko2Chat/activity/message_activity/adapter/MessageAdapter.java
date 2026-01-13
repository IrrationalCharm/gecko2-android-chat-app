package com.dominik.Gecko2Chat.activity.message_activity.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.model.MessageModel;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private static final int TYPE_SENT_TEXT = 1;
    private static final int TYPE_RECEIVED_TEXT = 2;
    private final List<MessageModel> messageList;
    private final String currentUserId;

    public MessageAdapter( String currentUserId) {
        this.messageList = new ArrayList<>();
        this.currentUserId = currentUserId;
    }

    // Logic to update list properly
    public void addMessage(MessageModel message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    public void addMessages(List<MessageModel> messages) {
        messageList.addAll(messages);
        notifyItemInserted(messageList.size() - messages.size());
    }

    public void setMessages(List<MessageModel> messages) {
        this.messageList.clear();
        this.messageList.addAll(messages);
        notifyDataSetChanged();
    }


    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageList.get(position);

        if (message.senderId().equals(currentUserId))
            return TYPE_SENT_TEXT;
        else
            return TYPE_RECEIVED_TEXT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT_TEXT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageList.get(position);

        if (holder instanceof SentMessageViewHolder sentMessageViewHolder) {
            sentMessageViewHolder.bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder messageViewHolder) {
            messageViewHolder.bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageTime, tvMessageContent;
        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
        }

        void bind(MessageModel message) {
            tvMessageTime.setText(message.timestamp().toString());
            tvMessageContent.setText(message.content());
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageTime, tvMessageContent;
        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
        }

        void bind(MessageModel message) {
            tvMessageTime.setText(message.timestamp().toString());
            tvMessageContent.setText(message.content());
        }
    }

}
