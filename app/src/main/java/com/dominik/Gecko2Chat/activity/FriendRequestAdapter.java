package com.dominik.Gecko2Chat.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.model.FriendRequestModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {

    private List<FriendRequestModel> friendRequestList;
    private OnClickListener listener;


    public interface OnClickListener {
        void onClick(FriendRequestModel requestModel, boolean isAccepted);
    }

    public FriendRequestAdapter(@NonNull List<FriendRequestModel> friendRequestList, OnClickListener listener) {
        this.friendRequestList = friendRequestList;
        this.listener = listener;
    }


    @NonNull
    @Override
    public FriendRequestAdapter.FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);

        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestAdapter.FriendRequestViewHolder holder, int position) {
        if (friendRequestList.isEmpty()) {
            holder.infoContainer.setVisibility(View.GONE);
            return;
        } else {
            holder.infoContainer.setVisibility(View.VISIBLE);
        }

        FriendRequestModel friendRequest = friendRequestList.get(position);

        holder.tvDisplayName.setText(friendRequest.displayName());
        String username = "@" + friendRequest.username();
        holder.tvUsername.setText(username);

        holder.btnAccept.setOnClickListener(v -> listener.onClick(friendRequest, true));
        holder.btnDecline.setOnClickListener(v -> listener.onClick(friendRequest, false));

        String avatarUrl = friendRequest.profileImageUrl();


        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl.contains("null") ? R.drawable.person_icon : avatarUrl)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .transform(new CircleCrop())
                    .into(holder.ivAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return friendRequestList.size();
    }

    public static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        MaterialButton btnAccept,btnDecline;
        TextView tvDisplayName,tvUsername;
        LinearLayout infoContainer;
        ImageView ivAvatar;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDisplayName = itemView.findViewById(R.id.tvDisplayName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            infoContainer = itemView.findViewById(R.id.infoContainer);

        }
    }
}
