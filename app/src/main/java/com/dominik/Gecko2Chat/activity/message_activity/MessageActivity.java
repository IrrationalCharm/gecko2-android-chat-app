package com.dominik.Gecko2Chat.activity.message_activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.BaseActivity;
import com.dominik.Gecko2Chat.activity.message_activity.adapter.MessageAdapter;
import com.dominik.Gecko2Chat.enums.TextType;
import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.model.response.ChatMessageDto;
import com.dominik.Gecko2Chat.model.response.MessageDto;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.dominik.Gecko2Chat.viewmodel.MessageViewModel;
import com.google.gson.Gson;

import java.time.LocalDateTime;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MessageActivity extends BaseActivity {

    private RecyclerView rvChatMessages;
    private MessageAdapter adapter;
    private EditText etMessageInput;
    private ImageView btnBack;
    private CardView btnSend;
    private String myId, friendId, friendName;
    private Gson gson = new Gson();

    private Disposable messageSubscription;
    private MessageViewModel messageViewModel;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        myId = userManager.getUser().internalId();
        friendId = getIntent().getStringExtra("FRIEND_ID");
        friendName = getIntent().getStringExtra("FRIEND_NAME");

        initViews();
        initListeners();

        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.getMessageList().observe(this, messages -> {
            int oldSize = adapter.getItemCount();
            adapter.setMessages(messages);

            if (oldSize == 0) {
                // First load -> Scroll to bottom
                rvChatMessages.scrollToPosition(adapter.getItemCount() - 1);
            } else if (messages.size() > oldSize && !isUserAtBottom()) {

            } else {
                // New message received -> Scroll to bottom
                rvChatMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });

        setupPaginationListener();

        if (adapter.getItemCount() == 0)
            messageViewModel.loadNextPage(friendId);

    }


    @Override
    protected void onResume() {
        super.onResume();
        // Listen for incoming messages
        messageSubscription = WebSocketManager.getInstance().getMessageStream()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleIncomingMessage, Throwable::printStackTrace);
    }


    @Override
    protected void onPause() {
        super.onPause();
        // Unsubscribe to avoid memory leaks
        if (messageSubscription != null && !messageSubscription.isDisposed())
            messageSubscription.dispose();
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




    private void handleIncomingMessage(String message) {
        try {
            MessageDto dto = gson.fromJson(message, MessageDto.class);

            // FILTER: Only show messages from the friend we are currently chatting with
            if (dto.senderId().equals(friendId)) {
                var msg = new MessageModel(
                        dto.id(),
                        dto.senderId(),
                        myId,
                        dto.content(),
                        LocalDateTime.parse(dto.timestamp()),
                        dto.textType()
                );

                adapter.addMessage(msg);
                rvChatMessages.smoothScrollToPosition(adapter.getItemCount() - 1);

                runOnUiThread(() -> messageViewModel.addNewMessage(msg));
            }
        } catch (Exception e) {
            Log.e("Chat", "Error parsing message", e);
        }
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

        //
        String json = gson.toJson(new ChatMessageDto(myId, friendId, content, LocalDateTime.now().toString()));
        System.out.println(json);
        WebSocketManager.getInstance().sendMessage(json);
        etMessageInput.setText("");

        // Optimistically add to adapter
        adapter.addMessage(new MessageModel(null, myId, friendId, content, LocalDateTime.now(), TextType.TEXT));
    }

}
