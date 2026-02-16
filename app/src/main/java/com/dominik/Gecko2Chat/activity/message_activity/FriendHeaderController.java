package com.dominik.Gecko2Chat.activity.message_activity;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.dominik.Gecko2Chat.R;
import com.google.android.material.card.MaterialCardView;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;

public class FriendHeaderController {

    private final Activity activity;
    private final ProfileActionCallback callback;

    private final MaterialCardView topBarContainer;
    private final ScrollView profileDetailsScrollView;
    private final LinearLayout llChatTitles;
    private final ImageView ivChatAvatar, ivExpandedProfile, btnBack;
    private final TextView tvChatName, tvChatStatus;
    private final View cvChatAvatar; // Add this line!
    private float originalCornerRadius; // Add this line to store the radius

    private boolean isProfileExpanded = false;

    public interface ProfileActionCallback {
        void onProfileToggled(boolean isExpanded);
        void onExitChat();
    }

    public FriendHeaderController(Activity activity, View rootLayout, Drawable windowBackground,
                                  String friendName, String friendAvatarUrl, ProfileActionCallback callback) {
        this.activity = activity;
        this.callback = callback;

        topBarContainer = activity.findViewById(R.id.topBarContainer);
        profileDetailsScrollView = activity.findViewById(R.id.profileDetailsScrollView);
        llChatTitles = activity.findViewById(R.id.llChatTitles);
        ivChatAvatar = activity.findViewById(R.id.ivChatAvatar);
        ivExpandedProfile = activity.findViewById(R.id.ivExpandedProfile);
        btnBack = activity.findViewById(R.id.btnBack);
        tvChatName = activity.findViewById(R.id.tvChatName);
        tvChatStatus = activity.findViewById(R.id.tvChatStatus);
        cvChatAvatar = activity.findViewById(R.id.cvChatAvatar);
        originalCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, activity.getResources().getDisplayMetrics());

        setupData(friendName, friendAvatarUrl);
        setupBlurView((ViewGroup) rootLayout, windowBackground);
        initListeners();
    }

    private void setupData(String friendName, String friendAvatarUrl) {
        tvChatName.setText(friendName);

        if (friendAvatarUrl != null && !friendAvatarUrl.isEmpty()) {
            String avatarUrl = friendAvatarUrl.contains("null") ? "" : friendAvatarUrl;

            Glide.with(activity.getApplicationContext()).load(avatarUrl.isEmpty() ? R.drawable.person_icon : avatarUrl)
                    .placeholder(R.drawable.person_icon).error(R.drawable.person_icon)
                    .transform(new CircleCrop()).into(ivChatAvatar);

            Glide.with(activity.getApplicationContext()).load(avatarUrl.isEmpty() ? R.drawable.person_icon : avatarUrl)
                    .placeholder(R.drawable.person_icon).error(R.drawable.person_icon)
                    .transform(new CircleCrop()).into(ivExpandedProfile);
        }
    }

    private void setupBlurView(ViewGroup root, Drawable bg) {
        BlurView topBlur = activity.findViewById(R.id.blurViewTop);
        if (topBlur != null) {
            topBlur.setupWith(root, new RenderEffectBlur())
                    .setFrameClearDrawable(bg)
                    .setBlurRadius(10f)
                    .setOverlayColor(0x501E1E1E);
        }
    }

    private void initListeners() {
        ivChatAvatar.setOnClickListener(v -> toggleProfileExpanded());
        llChatTitles.setOnClickListener(v -> toggleProfileExpanded());

        btnBack.setOnClickListener(v -> {
            if (isProfileExpanded) {
                toggleProfileExpanded();
            } else {
                callback.onExitChat();
            }
        });
    }

    public void toggleProfileExpanded() {
        // 1. Setup the Transition
        AutoTransition transition = new AutoTransition();
        transition.setDuration(300);
        // Add these targets to make the transition smoother and prevent the square glitch
        transition.addTarget(topBarContainer);
        transition.addTarget(profileDetailsScrollView);

        TransitionManager.beginDelayedTransition((ViewGroup) topBarContainer.getParent(), transition);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) topBarContainer.getLayoutParams();

        // Calculate 40dp offset for the translation
        float statusBarOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, activity.getResources().getDisplayMetrics());

        if (!isProfileExpanded) {
            // ==========================================
            // EXPANDING
            // ==========================================
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;

            // Remove margins to fill screen
            params.setMargins(0, 0, 0, 0);

            // Keep the corner radius during expansion to prevent the square glitch
            topBarContainer.setRadius(originalCornerRadius);

            cvChatAvatar.animate().alpha(0f).setDuration(150).start();
            llChatTitles.animate().alpha(0f).setDuration(150).start();

            // FIX: Smoothly slide the back button down 40dp!
            btnBack.animate().translationY(statusBarOffset).setDuration(300).start();

            // Small delay to ensure they are unclickable while invisible
            cvChatAvatar.postDelayed(() -> cvChatAvatar.setVisibility(View.INVISIBLE), 150);
            llChatTitles.postDelayed(() -> llChatTitles.setVisibility(View.INVISIBLE), 150);

            // Fade IN the big profile details
            profileDetailsScrollView.setVisibility(View.VISIBLE);
            profileDetailsScrollView.setAlpha(0f);
            profileDetailsScrollView.animate().alpha(1f).setDuration(250).setStartDelay(100).start();

            callback.onProfileToggled(true);

        } else {
            // ==========================================
            // COLLAPSING
            // ==========================================
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            // Restore original margins
            int margin16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, activity.getResources().getDisplayMetrics());
            int margin40 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, activity.getResources().getDisplayMetrics());
            params.setMargins(margin16, margin40, margin16, 0);

            // Restore rounded corners
            topBarContainer.setRadius(originalCornerRadius);

            // FIX: Smoothly slide the back button back to its original top position (0 offset)
            btnBack.animate().translationY(0f).setDuration(300).start();

            // Fade OUT the big profile details
            profileDetailsScrollView.animate().alpha(0f).setDuration(150).setStartDelay(0)
                    .withEndAction(() -> profileDetailsScrollView.setVisibility(View.GONE)).start();

            // Fade IN the small header elements
            cvChatAvatar.setVisibility(View.VISIBLE);
            llChatTitles.setVisibility(View.VISIBLE);
            cvChatAvatar.animate().alpha(1f).setDuration(200).setStartDelay(100).start();
            llChatTitles.animate().alpha(1f).setDuration(200).setStartDelay(100).start();

            callback.onProfileToggled(false);
        }

        topBarContainer.setLayoutParams(params);
        isProfileExpanded = !isProfileExpanded;
    }

    public void updateStatus(boolean isOnline) {
        tvChatStatus.setText(isOnline ? R.string.online : R.string.waiting_for_network);
    }

    public boolean isExpanded() {
        return isProfileExpanded;
    }
}