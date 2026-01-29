package com.dominik.Gecko2Chat.activity.main_activity.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.main_activity.adapter.ChatAdapter;
import com.dominik.Gecko2Chat.activity.message_activity.MessageActivity;
import com.dominik.Gecko2Chat.model.ChatModel;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.dominik.Gecko2Chat.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.List;


public class ChatsFragment extends BaseConnectionFragment {

    private ChatAdapter adapter;
    private List<ChatModel> chatList;


    public ChatsFragment(WebSocketManager.ConnectionStatus lastStatus) {
        super(lastStatus);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.rvList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        chatList = new ArrayList<>();
        adapter = new ChatAdapter(chatList, chat -> {
            // This runs when a user clicks a chat item
            Intent intent = new Intent(getContext(), MessageActivity.class);
            intent.putExtra("FRIEND_ID", chat.friendId());
            intent.putExtra("FRIEND_NAME", chat.name());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        viewModel.getChatList().observe(getViewLifecycleOwner(), chats -> {
            chatList.clear();
            chatList.addAll(chats);
            adapter.notifyDataSetChanged();
        });

    }
}