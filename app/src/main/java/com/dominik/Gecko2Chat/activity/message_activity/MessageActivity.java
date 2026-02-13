package com.dominik.Gecko2Chat.activity.message_activity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.BaseActivity;
import com.dominik.Gecko2Chat.activity.message_activity.adapter.MessageAdapter;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.dominik.Gecko2Chat.viewmodel.MessageViewModel;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;


public class MessageActivity extends BaseActivity {

    private String myId;

    private RecyclerView rvChatMessages;
    private MessageAdapter adapter;
    private EditText etMessageInput;
    private ImageView btnBack, ivChatAvatar, btnAttach;
    private View btnSend;
    private String friendId, friendName, friendAvatarUrl;
    private TextView tvChatStatus;
    private MessageViewModel messageViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        myId = UserManager.getInstance(this).getUser().internalId();
        friendId = getIntent().getStringExtra("FRIEND_ID");
        friendName = getIntent().getStringExtra("FRIEND_NAME");
        friendAvatarUrl = getIntent().getStringExtra("FRIEND_AVATAR");


        initViews();
        initListeners();

        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.initChat(friendId);

        setupObservers();
        setupPaginationListener();

        if (adapter.getItemCount() == 0)
            messageViewModel.loadNextPage(friendId);

    }


    private void setupObservers() {
        //Populates recycle view of messages
        messageViewModel.getMessageList().observe(this, messages -> {
            int oldSize = adapter.getItemCount();
            adapter.setMessages(messages);

            boolean wasAtBottom = isUserAtBottom();

            if (adapter.getItemCount() > 0) {
                if (oldSize == 0) {
                    // Initial load -> Scroll to bottom
                    rvChatMessages.scrollToPosition(adapter.getItemCount() - 1);
                } else if (messages.size() > oldSize) {
                    // ONLY scroll to bottom if the user was already there (i.e. new incoming message)
                    if (wasAtBottom) {
                        rvChatMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                }
            }
        });


        //Monitor connection status
        messageViewModel.getConnectionStatus().observe(this, status -> {
            if (status == WebSocketManager.ConnectionStatus.CONNECTED) {
                tvChatStatus.setText(R.string.online); //TODO make this dependent on user activity
            } else {
                tvChatStatus.setText(R.string.waiting_for_network);
            }
        });
    }


    //If user scrolls up, it loads next page
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
        return (pos >= numItems ); //-1
    }


    private void initListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void initViews() {
        ((TextView) findViewById(R.id.tvChatName)).setText(friendName);
        etMessageInput = findViewById(R.id.etMessageInput);

        btnSend = findViewById(R.id.btnSendContainer);

        btnBack = findViewById(R.id.btnBack);
        btnAttach = findViewById(R.id.btnAttach);

        tvChatStatus = findViewById(R.id.tvChatStatus);
        ivChatAvatar = findViewById(R.id.ivChatAvatar);



        if (friendAvatarUrl != null && !friendAvatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(friendAvatarUrl.contains("null") ? R.drawable.person_icon : friendAvatarUrl)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .transform(new CircleCrop())
                    .into(ivChatAvatar);
        }

        rvChatMessages = findViewById(R.id.rvChatMessages);
        var layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(myId);
        rvChatMessages.setAdapter(adapter);

        setupBlurViews();
    }

    private void setupBlurViews() {
        float radius = 10f; // Increased blur for better liquid effect
        int glassColor = 0x501E1E1E; // ~30% opacity dark grey

        View decorView = getWindow().getDecorView();
        ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
        Drawable windowBackground = decorView.getBackground();

        // 1. Top Bar
        BlurView topBlur = findViewById(R.id.blurViewTop);
        // 2. Attach Bubble (Left Circle)
        BlurView attachBlur = findViewById(R.id.blurViewAttach);
        // 3. Input Bubble (Right Pill)
        BlurView inputBlur = findViewById(R.id.blurViewInput);

        // Helper function (or just repeat the setup 3 times)
        setupSingleBlur(topBlur, rootView, windowBackground, radius, glassColor);
        setupSingleBlur(attachBlur, rootView, windowBackground, radius, glassColor);
        setupSingleBlur(inputBlur, rootView, windowBackground, radius, glassColor);
    }

    private void setupSingleBlur(BlurView view, ViewGroup root, Drawable bg, float radius, int color) {
        if (view == null) return;
        view.setupWith(root, new RenderEffectBlur()) // Ensure API compatibility if needed
                .setFrameClearDrawable(bg)
                .setBlurRadius(radius)
                .setOverlayColor(color);
    }

    private void sendMessage() {
        String content = etMessageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        //TODO sanitize input

        messageViewModel.addNewMessage(content);

        etMessageInput.setText("");

    }

}
