package com.dominik.Gecko2Chat.activity.main_activity.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dominik.Gecko2Chat.R;
import com.dominik.Gecko2Chat.activity.main_activity.adapter.ContactAdapter;
import com.dominik.Gecko2Chat.model.ContactModel;
import com.dominik.Gecko2Chat.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends Fragment {

    private final static String LAST_SEEN = "Last seen recently";
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private List<ContactModel> contactList;

    private MainViewModel viewModel;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        recyclerView = view.findViewById(R.id.rvContactList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        contactList = new ArrayList<>();
        adapter = new ContactAdapter(contactList);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        viewModel.getContactList().observe(getViewLifecycleOwner(), contacts -> {
            contactList.clear();
            contactList.addAll(contacts);
            adapter.notifyDataSetChanged();
        });

        // 3. Observe Loading (Optional)
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Toggle your ProgressBar visibility here
        });

        // 4. Observe Errors (Optional)
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}