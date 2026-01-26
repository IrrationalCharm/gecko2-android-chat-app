package com.dominik.Gecko2Chat.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.model.FriendRequestModel;
import com.dominik.Gecko2Chat.viewmodel.FriendRequestViewModel;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestActivity extends BaseActivity {

    private FriendRequestViewModel viewModel;
    private FriendRequestAdapter adapter;
    private List<FriendRequestModel> friendRequestList;
    private LinearLayout layoutEmptyState;
    private RecyclerView rvFriendRequests;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_friend_request);

        initViews();
        setupViewModel();
        setupRecyclerView();
        observeViewModel();
    }

    private void initViews() {
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        rvFriendRequests = findViewById(R.id.rvFriendRequests);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(FriendRequestViewModel.class);
    }

    private void setupRecyclerView() {
        rvFriendRequests.setLayoutManager(new LinearLayoutManager(this));

        friendRequestList = new ArrayList<>();
        adapter = new FriendRequestAdapter(friendRequestList, (requestModel, isAccepted) -> {
            if (isAccepted) {
                viewModel.acceptRequest(requestModel.id());
            } else {
                viewModel.declineRequest(requestModel.id());
            }
        });

        rvFriendRequests.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getFriendRequests().observe(this, requests -> {
            updateEmptyState(requests.isEmpty());

            friendRequestList.clear();
            friendRequestList.addAll(requests);
            adapter.notifyDataSetChanged();
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvFriendRequests.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvFriendRequests.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}