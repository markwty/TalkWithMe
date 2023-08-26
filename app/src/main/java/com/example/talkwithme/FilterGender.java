package com.example.talkwithme;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

public class FilterGender extends Fragment {
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.filter_gender, container, false);
        Context context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int id = -1;
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
        }
        final int fid = id;

        final Button MaleButton = view.findViewById(R.id.MaleButton);
        MaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new SearchingForVolunteer();
                Bundle bundle = new Bundle();
                bundle.putInt("id", fid);
                bundle.putInt("opp_gender", 0);
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.commit();
            }
        });

        final Button FemaleButton = view.findViewById(R.id.FemaleButton);
        FemaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new SearchingForVolunteer();
                Bundle bundle = new Bundle();
                bundle.putInt("id", fid);
                bundle.putInt("opp_gender", 1);
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.commit();
            }
        });
        return view;
    }
}