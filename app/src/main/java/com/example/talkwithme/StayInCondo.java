package com.example.talkwithme;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

public class StayInCondo extends Fragment {
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.stay_in_condo, container, false);
        activity = getActivity();

        final Button YesButton = view.findViewById(R.id.YesButton);
        YesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new Register();
                Bundle bundle = new Bundle();
                bundle.putInt("state", 0);
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.commit();
            }
        });

        final Button NoButton = view.findViewById(R.id.NoButton);
        NoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new Register();
                Bundle bundle = new Bundle();
                bundle.putInt("state", 1);
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.commit();
            }
        });

        return view;
    }
}