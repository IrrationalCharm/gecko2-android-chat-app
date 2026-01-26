package com.dominik.Gecko2Chat.activity.main_activity.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.model.ContactModel;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder>{

    private List<ContactModel> contactList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ContactModel contact);
    }


    public ContactAdapter(List<ContactModel> contactList, OnItemClickListener listener) {
        this.contactList = contactList;
        this.listener = listener;
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
        holder.clItemContact.setOnClickListener(v -> listener.onItemClick(contact));
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvLastSeen;
        private ImageView ivAvatar;
        private ConstraintLayout clItemContact;


        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastSeen = itemView.findViewById(R.id.tvLastSeen);
            clItemContact = itemView.findViewById(R.id.clItemContact);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }
    }
}
