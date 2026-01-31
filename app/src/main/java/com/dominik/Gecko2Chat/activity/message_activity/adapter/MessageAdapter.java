package com.dominik.Gecko2Chat.activity.message_activity.adapter;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
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


    public void setMessages(List<MessageModel> newMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return messageList.size();
            }

            @Override
            public int getNewListSize() {
                return newMessages.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                //Compare unique ids to check if it's the same message
                return messageList.get(oldItemPosition).id().equals(newMessages.get(newItemPosition).id());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return messageList.get(oldItemPosition).equals(newMessages.get(newItemPosition));
            }
        });

        this.messageList.clear();
        this.messageList.addAll(newMessages);
        //Dispatch specific updates (inserts/removes) instead of refreshing everything
        diffResult.dispatchUpdatesTo(this);
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
        ImageView ivMessageStatus;
        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
            ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);
        }

        void bind(MessageModel message) {
            tvMessageContent.setText(message.content());

            switch (message.status()) {
                case SENDING -> {
                    tvMessageTime.setVisibility(View.GONE);
                    ivMessageStatus.clearColorFilter();
                    ivMessageStatus.setImageResource(R.drawable.ic_clock);
                }
                case SENT -> {
                    tvMessageTime.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_tick);
                    ivMessageStatus.clearColorFilter();
                    tvMessageTime.setText(message.timestamp().toString());
                }
                case DELIVERED -> {
                    tvMessageTime.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_double_tick);
                    ivMessageStatus.clearColorFilter();
                    tvMessageTime.setText(message.timestamp().toString());
                }
                case READ -> {
                    tvMessageTime.setVisibility(View.VISIBLE);
                    ivMessageStatus.setImageResource(R.drawable.ic_double_tick);
                    ivMessageStatus.setColorFilter(
                            ContextCompat.getColor(ivMessageStatus.getContext(), R.color.blue_read_receipt),
                            PorterDuff.Mode.SRC_IN
                    );
                    tvMessageTime.setText(message.timestamp().toString());
                }
                case FAILED -> {
                }
            };

             //TODO: Format timestamp

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
