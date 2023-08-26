package com.example.talkwithme;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class Recommend extends Fragment {
    private String classname = getClass().getName();
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.recommend, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        return view;
    }
}
