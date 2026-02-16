package com.dominik.Gecko2Chat.activity.message_activity;

import android.animation.ValueAnimator;
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
            String expandedAvatarUrl = avatarUrl.isEmpty() ? "" : avatarUrl.replace("_thumb", "");

            Glide.with(activity.getApplicationContext()).load(avatarUrl.isEmpty() ? R.drawable.person_icon : avatarUrl)
                    .placeholder(R.drawable.person_icon).error(R.drawable.person_icon)
                    .transform(new CircleCrop()).into(ivChatAvatar);

            Glide.with(activity.getApplicationContext()).load(expandedAvatarUrl.isEmpty() ? R.drawable.person_icon : expandedAvatarUrl)
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
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) topBarContainer.getLayoutParams();

        int margin16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, activity.getResources().getDisplayMetrics());
        int margin40 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, activity.getResources().getDisplayMetrics());
        float statusBarOffset = margin40;

        int startHeight = topBarContainer.getHeight();
        int targetHeight;
        int targetMarginHorizontal;
        int targetMarginTop;

        // Safely get the parent dimensions
        View parentView = (View) topBarContainer.getParent();
        int parentWidth = parentView != null ? parentView.getWidth() : activity.getResources().getDisplayMetrics().widthPixels;
        int parentHeight = parentView != null ? parentView.getHeight() : activity.getResources().getDisplayMetrics().heightPixels;

        if (!isProfileExpanded) {
            // ==========================================
            // EXPANDING
            // ==========================================
            targetHeight = parentHeight;
            targetMarginHorizontal = 0;
            targetMarginTop = 0;

            // Animate inner views
            cvChatAvatar.animate().alpha(0f).setDuration(150).start();
            llChatTitles.animate().alpha(0f).setDuration(150).start();
            btnBack.animate().translationY(statusBarOffset).setDuration(300).start();

            cvChatAvatar.postDelayed(() -> cvChatAvatar.setVisibility(View.INVISIBLE), 150);
            llChatTitles.postDelayed(() -> llChatTitles.setVisibility(View.INVISIBLE), 150);

            profileDetailsScrollView.setVisibility(View.VISIBLE);
            profileDetailsScrollView.setAlpha(0f);
            profileDetailsScrollView.animate().alpha(1f).setDuration(250).setStartDelay(100).start();

            callback.onProfileToggled(true);

        } else {
            // ==========================================
            // COLLAPSING
            // ==========================================

            // 1. Lock ScrollView height to prevent content from squishing during collapse
            ViewGroup.LayoutParams scrollParams = profileDetailsScrollView.getLayoutParams();
            scrollParams.height = profileDetailsScrollView.getHeight();
            profileDetailsScrollView.setLayoutParams(scrollParams);

            // 2. Temporarily hide ScrollView to calculate the correct collapsed wrap_content height
            profileDetailsScrollView.setVisibility(View.GONE);
            int widthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth - (margin16 * 2), View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            topBarContainer.measure(widthSpec, heightSpec);
            targetHeight = topBarContainer.getMeasuredHeight();
            profileDetailsScrollView.setVisibility(View.VISIBLE); // Bring back for fade out animation

            targetMarginHorizontal = margin16;
            targetMarginTop = margin40;

            btnBack.animate().translationY(0f).setDuration(300).start();

            // 3. Fade out and restore ScrollView to 0dp (match constraint) at the end
            profileDetailsScrollView.animate().alpha(0f).setDuration(150).setStartDelay(0)
                    .withEndAction(() -> {
                        profileDetailsScrollView.setVisibility(View.GONE);
                        ViewGroup.LayoutParams p = profileDetailsScrollView.getLayoutParams();
                        p.height = 0; // Reset back to 0dp for ConstraintLayout match constraint
                        profileDetailsScrollView.setLayoutParams(p);
                    }).start();

            cvChatAvatar.setVisibility(View.VISIBLE);
            llChatTitles.setVisibility(View.VISIBLE);
            cvChatAvatar.animate().alpha(1f).setDuration(200).setStartDelay(100).start();
            llChatTitles.animate().alpha(1f).setDuration(200).setStartDelay(100).start();

            callback.onProfileToggled(false);
        }

        // Keep the corner radius during the animation
        topBarContainer.setRadius(originalCornerRadius);

        int startMarginHorizontal = params.leftMargin; // Assuming left and right margins are uniform
        int startMarginTop = params.topMargin;

        // Use ValueAnimator instead of TransitionManager to force BlurView to resize frame-by-frame
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();

            params.height = (int) (startHeight + (targetHeight - startHeight) * fraction);
            int currentMarginHorizontal = (int) (startMarginHorizontal + (targetMarginHorizontal - startMarginHorizontal) * fraction);
            int currentMarginTop = (int) (startMarginTop + (targetMarginTop - startMarginTop) * fraction);

            params.setMargins(currentMarginHorizontal, currentMarginTop, currentMarginHorizontal, 0);
            topBarContainer.setLayoutParams(params);
        });
        animator.start();

        isProfileExpanded = !isProfileExpanded;
    }

    public void updateStatus(boolean isOnline) {
        tvChatStatus.setText(isOnline ? R.string.online : R.string.waiting_for_network);
    }

    public boolean isExpanded() {
        return isProfileExpanded;
    }
}