package com.dominik.Gecko2Chat.activity.onBoarding.fragments;

public interface OnboardingStep {

    /**
     * Called when the user clicks next
     * @return true if data is valid and we can move to the next slide.
     * false if data is invalid (Fragment handles showing the error).
     */
    boolean isDataValid();

    /**
     * Used to extract data from the fragment when the wizard is finished.
     */
    String getData();
}
