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
import com.dominik.Gecko2Chat.model.MessageModel;
import com.dominik.Gecko2Chat.utils.UserManager;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.dominik.Gecko2Chat.viewmodel.MessageViewModel;
import com.google.android.material.card.MaterialCardView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;


public class MessageActivity extends BaseActivity {

    private String myId;

    private String currentDateBadgeText = "";
    private boolean isDateBadgeVisible = false;
    private boolean isScrollButtonVisible = false;
    private final float SWIPE_DISTANCE = 70f;
    private MaterialCardView scrollToBottomContainer, dateBadgeContainer;

    private RecyclerView rvChatMessages;
    private MessageAdapter adapter;
    private EditText etMessageInput;
    private ImageView btnBack, ivChatAvatar, btnAttach;
    private View btnSend;
    private String friendId, friendName, friendAvatarUrl;
    private TextView tvChatStatus, tvDateBadge;
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
            boolean wasAtBottom = isUserAtBottom();

            int oldSize = adapter.getItemCount();
            adapter.setMessages(messages);

            if (adapter.getItemCount() > 0) {
                if (oldSize == 0) {
                    // Initial load -> Scroll to bottom
                    rvChatMessages.scrollToPosition(adapter.getItemCount() - 1);
                } else if (messages.size() > oldSize) {
                    // ONLY scroll to bottom if the user was already there (i.e. new incoming message)
                    if (wasAtBottom) {
                        rvChatMessages.post(() ->
                                rvChatMessages.smoothScrollToPosition(adapter.getItemCount() - 1)
                        );
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

            // Handler to auto-hide the date badge after scrolling stops
            private final android.os.Handler hideBadgeHandler = new android.os.Handler();
            private final Runnable hideBadgeRunnable = () -> hideDateBadge();

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    // User is actively scrolling: cancel the hide timer and show badge
                    hideBadgeHandler.removeCallbacks(hideBadgeRunnable);
                    showDateBadge();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // User stopped scrolling: hide the badge after 1.5 seconds
                    hideBadgeHandler.postDelayed(hideBadgeRunnable, 1500);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                if (layoutManager != null && adapter.getItemCount() > 0) {

                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                    // 1. Pagination Check
                    if (firstVisibleItem <= 1 && dy < 0) {
                        messageViewModel.loadNextPage(friendId);
                    }

                    // 2. Update the text of the Date Badge based on the top item
                    updateDateBadge(firstVisibleItem);

                    // 3. Scroll To Bottom Button Logic (Independent of Date Badge)
                    int lastVisible = layoutManager.findLastVisibleItemPosition();
                    int lastItem = adapter.getItemCount() - 1;
                    int hiddenItemsBelow = lastItem - lastVisible;

                    if (hiddenItemsBelow >= 4) {
                        showScrollToBottomButton();
                    } else {
                        hideScrollToBottomButton();
                    }
                }
            }
        });
    }

    //Determine if it should autoscroll
    private boolean isUserAtBottom() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvChatMessages.getLayoutManager();

        int lastVisible = layoutManager.findLastVisibleItemPosition();
        int totalItems = adapter.getItemCount();

        return lastVisible >= totalItems - 2;
    }


    private void initListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        scrollToBottomContainer.setOnClickListener(v -> rvChatMessages.smoothScrollToPosition(adapter.getItemCount() - 1));
    }

    private void initViews() {
        ((TextView) findViewById(R.id.tvChatName)).setText(friendName);
        etMessageInput = findViewById(R.id.etMessageInput);

        btnSend = findViewById(R.id.btnSendContainer);

        tvDateBadge = findViewById(R.id.tvDateBadge);
        dateBadgeContainer = findViewById(R.id.dateBadgeContainer);
        dateBadgeContainer.setVisibility(View.GONE);
        dateBadgeContainer.setAlpha(0f); // Start completely transparent
        dateBadgeContainer.setTranslationY(-SWIPE_DISTANCE);

        scrollToBottomContainer = findViewById(R.id.scrollToBottomContainer);
        scrollToBottomContainer.setVisibility(View.GONE);
        scrollToBottomContainer.setAlpha(0f); // Start completely transparent
        scrollToBottomContainer.setTranslationY(-SWIPE_DISTANCE); // Start shifted slightly up

        btnBack = findViewById(R.id.btnBack);
        btnAttach = findViewById(R.id.btnAttach);

        tvChatStatus = findViewById(R.id.tvChatStatus);
        ivChatAvatar = findViewById(R.id.ivChatAvatar);

        //Load avatar
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

        BlurView blurViewDate = findViewById(R.id.blurViewDate);
        BlurView topBlur = findViewById(R.id.blurViewTop);
        BlurView blurViewScroll = findViewById(R.id.blurViewScroll);
        BlurView attachBlur = findViewById(R.id.blurViewAttach);
        BlurView inputBlur = findViewById(R.id.blurViewInput);

        // Helper function (or just repeat the setup 3 times)
        setupSingleBlur(topBlur, rootView, windowBackground, radius, glassColor);
        setupSingleBlur(attachBlur, rootView, windowBackground, radius, glassColor);
        setupSingleBlur(inputBlur, rootView, windowBackground, radius, glassColor);
        setupSingleBlur(blurViewScroll, rootView, windowBackground, radius, glassColor);
        setupSingleBlur(blurViewDate, rootView, windowBackground, radius, glassColor);
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

        rvChatMessages.post(() ->
                rvChatMessages.smoothScrollToPosition(adapter.getItemCount() - 1)
        );
    }


    private void updateDateBadge(int firstVisiblePosition) {
        if (adapter.getItemCount() > 0 && firstVisiblePosition >= 0) {
            MessageModel message = adapter.getMessage(firstVisiblePosition);
            String formattedDate = formatDateBadge(message.timestamp());

            // Only update the TextView if the date string has actually changed
            if (!formattedDate.equals(currentDateBadgeText)) {
                tvDateBadge.setText(formattedDate);
                currentDateBadgeText = formattedDate;
            }
        }
    }

    private void showDateBadge() {
        if (!isDateBadgeVisible) {
            isDateBadgeVisible = true;

            // Instantly snap to the top before making it visible
            // This resets it in case the previous hide animation left it at the bottom
            dateBadgeContainer.setTranslationY(-SWIPE_DISTANCE);
            dateBadgeContainer.setVisibility(View.VISIBLE);

            dateBadgeContainer.animate()
                    .alpha(1f)                     // Fade in
                    .translationY(0f)              // Swipe down to its original resting position
                    .setDuration(400)
                    .withEndAction(null)
                    .start();
        }
    }

    private void hideDateBadge() {
        if (isDateBadgeVisible) {
            isDateBadgeVisible = false;

            dateBadgeContainer.animate()
                    .alpha(0f)                     // Fade out
                    .translationY(SWIPE_DISTANCE)  // Swipe down further (positive = down)
                    .setDuration(200)
                    .withEndAction(() -> {
                        // Hide it completely once the animation finishes
                        dateBadgeContainer.setVisibility(View.GONE);
                    })
                    .start();
        }
    }

    private String formatDateBadge(Instant instant) {
        LocalDate messageDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (messageDate.equals(today)) {
            return "Today";
        } else if (messageDate.equals(yesterday)) {
            return "Yesterday";
        } else {
            // Formats older dates as "Oct 24, 2023" (adjust pattern as needed)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault());
            return messageDate.format(formatter);
        }
    }

    private void showScrollToBottomButton() {
        if (!isScrollButtonVisible) {
            isScrollButtonVisible = true;

            // Instantly snap to the top before making it visible
            // This resets it in case the previous hide animation left it at the bottom
            scrollToBottomContainer.setTranslationY(-SWIPE_DISTANCE);
            scrollToBottomContainer.setVisibility(View.VISIBLE);

            scrollToBottomContainer.animate()
                    .alpha(1f)                     // Fade in
                    .translationY(0f)              // Swipe down to its original resting position
                    .setDuration(400)
                    .withEndAction(null)
                    .start();
        }
    }

    private void hideScrollToBottomButton() {
        if (isScrollButtonVisible) {
            isScrollButtonVisible = false;

            scrollToBottomContainer.animate()
                    .alpha(0f)                     // Fade out
                    .translationY(SWIPE_DISTANCE)  // Swipe down further (positive = down)
                    .setDuration(200)
                    .withEndAction(() -> {
                        // Hide it completely once the animation finishes
                        scrollToBottomContainer.setVisibility(View.GONE);
                    })
                    .start();
        }
    }

}
