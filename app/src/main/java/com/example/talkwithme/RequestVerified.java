package com.example.talkwithme;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;

public class RequestVerified extends TransitionFragment {
    Bundle bundleToHome = null;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.request_verified, container, false);
        Context context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        bundleToHome = getArguments();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(stop){
                    return;
                }
                activity.onBackPressed();
            }
        }, 4000);
        return view;
    }
}