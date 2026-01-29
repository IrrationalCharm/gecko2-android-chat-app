package com.dominik.Gecko2Chat.activity.main_activity.fragments;

import android.animation.ValueAnimator;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.utils.WebSocketManager;
import com.dominik.Gecko2Chat.viewmodel.MainViewModel;

public abstract class BaseConnectionFragment extends Fragment {

    protected MainViewModel viewModel;
    private LinearLayout layoutConnecting;
    private ProgressBar progressBar;
    private TextView tvConnecting;
    private TextView tvHeader;

    private ValueAnimator shimmerAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable transitionRunnable;
    private WebSocketManager.ConnectionStatus lastStatus;


    public BaseConnectionFragment(WebSocketManager.ConnectionStatus lastStatus) {
        this.lastStatus = lastStatus;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        layoutConnecting = view.findViewById(R.id.layoutConnecting);
        progressBar = view.findViewById(R.id.progressBar);
        tvConnecting = view.findViewById(R.id.tvConnecting);
        tvHeader = view.findViewById(R.id.tvHeader);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.getConnectionStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == WebSocketManager.ConnectionStatus.CONNECTED && lastStatus == WebSocketManager.ConnectionStatus.CONNECTED) {
                showChatsImmediately();
                return;
            }

            if (status == lastStatus) return;

            // Cancel pending transitions
            if (transitionRunnable != null) handler.removeCallbacks(transitionRunnable);

            if(lastStatus != WebSocketManager.ConnectionStatus.CONNECTED) {
                if (status == WebSocketManager.ConnectionStatus.CONNECTED) {
                    handleConnectedState();
                    lastStatus = status;
                } else {
                    handleConnectingState();
                    lastStatus = status;
                }
            } else {
                lastStatus = status;
            }
        });
    }


    private void handleConnectedState() {
        //Stop the shimmer effect
        stopShimmer();
        //Hide Spinner, Show "Connected" in Green
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (tvConnecting != null) {
            tvConnecting.setText("Connected");
            tvConnecting.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_accent));
        }

        //Wait 1.5 seconds, then show Chats
        transitionRunnable = () -> {
            if (!isAdded()) return; // Safety check

            // Fade out the connecting layout
            layoutConnecting.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        layoutConnecting.setVisibility(View.GONE);
                        layoutConnecting.setAlpha(1f); // Reset for next time

                        // Show Chats Header
                        tvHeader.setAlpha(0f);
                        tvHeader.setVisibility(View.VISIBLE);
                        tvHeader.animate().alpha(1f).setDuration(300).start();
                    })
                    .start();
        };
        handler.postDelayed(transitionRunnable, 1500);
    }


    private void handleConnectingState() {
        // Reset UI to "Connecting..."
        tvHeader.setVisibility(View.GONE);

        layoutConnecting.setVisibility(View.VISIBLE);
        layoutConnecting.setAlpha(1f);

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        if (tvConnecting != null) {
            tvConnecting.setText("Connecting...");
            tvConnecting.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }

        startShimmer();
    }


    private void showChatsImmediately() {
        stopShimmer();
        // Hide connecting layout
        if (layoutConnecting != null) {
            layoutConnecting.setVisibility(View.GONE);
            layoutConnecting.setAlpha(1f); // Reset alpha just in case
        }

        // Show Headers
        if (tvHeader != null) {
            tvHeader.setVisibility(View.VISIBLE);
            tvHeader.setAlpha(1f);
        }
    }

    private void startShimmer() {
        if (tvConnecting == null) return;

        tvConnecting.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                tvConnecting.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (tvConnecting.getWidth() == 0) return;

                int gray = ContextCompat.getColor(requireContext(), R.color.text_secondary);
                int green = ContextCompat.getColor(requireContext(), R.color.green_accent);

                float textWidth = tvConnecting.getWidth();
                Shader textShader = new LinearGradient(0, 0, textWidth, 0,
                        new int[]{gray, green, gray},
                        new float[]{0, 0.5f, 1},
                        Shader.TileMode.CLAMP);

                tvConnecting.getPaint().setShader(textShader);

                shimmerAnimator = ValueAnimator.ofFloat(0, textWidth * 2);
                shimmerAnimator.setDuration(2000);
                shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
                shimmerAnimator.addUpdateListener(animation -> {
                    float translate = (float) animation.getAnimatedValue();
                    Matrix matrix = new Matrix();
                    matrix.setTranslate(translate - textWidth, 0);
                    textShader.setLocalMatrix(matrix);
                    tvConnecting.invalidate();
                });
                shimmerAnimator.start();
            }
        });
    }


    private void stopShimmer() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
        if (tvConnecting != null) {
            tvConnecting.getPaint().setShader(null);
            tvConnecting.invalidate();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transitionRunnable != null) handler.removeCallbacks(transitionRunnable);
        stopShimmer();
    }
}
