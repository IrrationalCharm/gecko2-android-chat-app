package com.dominik.Gecko2Chat.activity.main_activity.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.model.ContactModel;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder>{

    private List<ContactModel> contactList;

    public ContactAdapter(List<ContactModel> contactList) {
        this.contactList = contactList;
    }

    @NonNull
    @Override
    public ContactAdapter.ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Inflate item_chat.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);

        return new ContactAdapter.ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactAdapter.ContactViewHolder holder, int position) {
        ContactModel contact = contactList.get(position);
        holder.tvName.setText(contact.displayName());
        holder.tvLastSeen.setText(contact.lastSeen());
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvLastSeen;
        private ImageView ivAvatar;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastSeen = itemView.findViewById(R.id.tvLastSeen);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }
    }
}
