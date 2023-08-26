package com.example.talkwithme;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

public class GetStarted extends TransitionFragment {
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.get_started, container, false);
        Context context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int id = -1, gender = -1, state = -1;
        String name = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            name = bundle.getString("name", "");
            gender = bundle.getInt("gender", -1);
            state = bundle.getInt("state", -1);
        }
        final int fid = id, fgender = gender, fstate = state;
        final String fname = name;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(stop){
                    return;
                }
                Fragment fragment = new SelectOrganisation();
                Bundle bundle = new Bundle();
                bundle.putInt("id", fid);
                bundle.putString("name", fname);
                bundle.putInt("gender", fgender);
                bundle.putInt("state", fstate);
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.commit();
            }
        }, 2000);
        return view;
    }
}
