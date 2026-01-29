package com.dominik.Gecko2Chat.activity.main_activity.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.add_friend_activity.AddFriendActivity;
import com.dominik.Gecko2Chat.activity.FriendRequestActivity;
import com.dominik.Gecko2Chat.activity.main_activity.adapter.ContactAdapter;
import com.dominik.Gecko2Chat.activity.message_activity.MessageActivity;
import com.dominik.Gecko2Chat.model.ContactModel;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.dominik.Gecko2Chat.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends BaseConnectionFragment {

    private final static String LAST_SEEN = "Last seen recently";
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private List<ContactModel> contactList;
    private CardView btnAddContact;
    private CardView btnFriendRequests;
    private TextView tvRequestCount;

    public ContactsFragment(WebSocketManager.ConnectionStatus lastStatus) {
        super(lastStatus);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        btnFriendRequests = view.findViewById(R.id.cvFriendRequests);
        btnAddContact = view.findViewById(R.id.btnAddContact);
        recyclerView = view.findViewById(R.id.rvContactList);
        tvRequestCount = view.findViewById(R.id.tvRequestCount);

        btnAddContact.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddFriendActivity.class);
            startActivity(intent);
        });

        btnFriendRequests.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FriendRequestActivity.class);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        contactList = new ArrayList<>();
        adapter = new ContactAdapter(contactList, contact -> {
            Intent intent = new Intent(getContext(), MessageActivity.class);
            intent.putExtra("FRIEND_ID", contact.internalId());
            intent.putExtra("FRIEND_NAME", contact.displayName());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        viewModel.getFriendRequestsCount().observe(getViewLifecycleOwner(), count -> {
            if (count > 0) {
                btnFriendRequests.setVisibility(View.VISIBLE);
                tvRequestCount.setText(String.valueOf(count));
            } else {
                btnFriendRequests.setVisibility(View.GONE);
            }
        });


        viewModel.getContactList().observe(getViewLifecycleOwner(), contacts -> {
            contactList.clear();
            contactList.addAll(contacts);
            adapter.notifyDataSetChanged();
        });


    }

}