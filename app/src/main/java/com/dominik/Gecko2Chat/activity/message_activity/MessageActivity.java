package com.dominik.Gecko2Chat.activity.message_activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.BaseActivity;
import com.dominik.Gecko2Chat.activity.message_activity.adapter.MessageAdapter;
import com.dominik.Gecko2Chat.enums.PrivateMessageType;
import com.dominik.Gecko2Chat.enums.TextType;
import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.dominik.Gecko2Chat.viewmodel.MessageViewModel;
import com.dominik.Gecko2Chat.model.response.websocket.*;
import com.dominik.Gecko2Chat.model.response.websocket.adapter.PrivateMessageDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MessageActivity extends BaseActivity {

    private String myId;

    private RecyclerView rvChatMessages;
    private MessageAdapter adapter;
    private EditText etMessageInput;
    private ImageView btnBack;
    private CardView btnSend;
    private String friendId, friendName;

    private MessageViewModel messageViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        myId = new UserManager(this).getUser().internalId();
        friendId = getIntent().getStringExtra("FRIEND_ID");
        friendName = getIntent().getStringExtra("FRIEND_NAME");

        initViews();
        initListeners();

        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.initChat(friendId);

        messageViewModel.getMessageList().observe(this, messages -> {
            int oldSize = adapter.getItemCount();
            adapter.setMessages(messages);

            if (oldSize == 0) {
                rvChatMessages.scrollToPosition(adapter.getItemCount() - 1);
            } else if (messages.size() > oldSize && !isUserAtBottom()) {
                rvChatMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });

        setupPaginationListener();

        if (adapter.getItemCount() == 0)
            messageViewModel.loadNextPage(friendId);

    }



    private void setupPaginationListener() {
        rvChatMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                    // If user is near top (position 0) and scrolling up
                    if (firstVisibleItem <= 1 && dy < 0) { // dy < 0 means scrolling up
                        messageViewModel.loadNextPage(friendId);
                    }
                }
            }
        });
    }

    //Determine if it should autoscroll
    private boolean isUserAtBottom() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvChatMessages.getLayoutManager();
        int pos = layoutManager.findLastCompletelyVisibleItemPosition();
        int numItems = rvChatMessages.getAdapter().getItemCount();
        return (pos >= numItems - 2);
    }




    private void initListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void initViews() {

        ((TextView) findViewById(R.id.tvChatName)).setText(friendName);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        rvChatMessages = findViewById(R.id.rvChatMessages);
        var layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(myId);
        rvChatMessages.setAdapter(adapter);
    }



    private void sendMessage() {
        String content = etMessageInput.getText().toString();
        if (content.isEmpty()) return;

        //TODO sanitize input

        messageViewModel.sendMessage(content);

        etMessageInput.setText("");
    }

}
