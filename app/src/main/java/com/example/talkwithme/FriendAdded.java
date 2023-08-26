package com.example.talkwithme;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

public class FriendAdded extends TransitionFragment {
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.friend_added, container, false);
        Context context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        boolean added = false;
        Bundle bundle = getArguments();
        if (bundle != null) {
            added = bundle.getBoolean("added", false);
        }
        boolean fadded = added;

        final TextView FriendAddedLabel = view.findViewById(R.id.FriendAddedLabel);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(fadded){
                    FriendAddedLabel.setText(R.string.FriendRequestSent);
                }else{
                    FriendAddedLabel.setText(R.string.FriendNotAdded);
                }
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(stop){
                    return;
                }
                activity.onBackPressed();
            }
        }, 5000);
        return view;
    }
}