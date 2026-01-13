package com.dominik.Gecko2Chat.activity.onBoarding.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

//holds the fragments so the ViewPager can show them
public class OnBoardingAdapter extends FragmentStateAdapter {

    // Keep references to fragments so we can call methods on them
    private final List<Fragment> fragmentList = new ArrayList<>();


    public OnBoardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        // Add your slides here in order
        fragmentList.add(new UsernameFragment());
        fragmentList.add(new DisplayNameFragment());
        fragmentList.add(new ProfilePictureFragment());
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }

    // Helper to cast Fragment to our Interface
    public OnboardingStep getFragment(int position) {
        if (position >= 0 && position < fragmentList.size()) {
            return (OnboardingStep) fragmentList.get(position);
        }
        return null;
    }
}
