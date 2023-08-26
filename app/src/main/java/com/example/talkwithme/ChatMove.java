package com.example.talkwithme;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

public class ChatMove extends Fragment {
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.chat_move, container, false);
        activity = getActivity();

        int id = -1, gender = -1, status = -1, state = -1;
        String name = "", organisation = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            name = bundle.getString("name", "");
            gender = bundle.getInt("gender", -1);
            organisation = bundle.getString("organisation", "");
            status = bundle.getInt("status", -1);
            state = bundle.getInt("state", -1);
        }
        final int fid = id, fgender = gender, fstatus = status, fstate = state;
        final String fname = name, forganisation = organisation;

        final Button ChatButton = view.findViewById(R.id.ChatButton);
        ChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fstatus == 2){
                    Fragment fragment = new Contacts();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", fid);
                    bundle.putString("name", fname);
                    bundle.putInt("status", 2);
                    fragment.setArguments(bundle);
                    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    //Add checkpoint
                    ft.addToBackStack("Main");
                    ft.commit();
                    return;
                }
                //state refers to the interface users previously chosen
                if(fstate == 1){
                    Fragment fragment = new ContactsVolunteer();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", fid);
                    bundle.putString("name", fname);
                    bundle.putInt("gender", fgender);
                    bundle.putString("organisation", forganisation);
                    bundle.putInt("status", fstatus);
                    fragment.setArguments(bundle);
                    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    //Add checkpoint
                    ft.addToBackStack("Main");
                    ft.commit();
                }else{
                    Fragment fragment = new ContactsConnectMake();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", fid);
                    bundle.putString("name", fname);
                    bundle.putInt("gender", fgender);
                    bundle.putString("organisation", forganisation);
                    bundle.putInt("status", fstatus);
                    fragment.setArguments(bundle);
                    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    //Add checkpoint
                    ft.addToBackStack("Main");
                    ft.commit();
                }
            }
        });

        final Button MoveButton = view.findViewById(R.id.MoveButton);
        MoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new StartLocation();
                Bundle bundle = new Bundle();
                bundle.putInt("id", fid);
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.addToBackStack("Main");
                ft.commit();
            }
        });
        return view;
    }
}