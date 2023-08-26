package com.example.talkwithme;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

public class AddFriend extends Fragment {
    private String classname = getClass().getName();
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.add_friend, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int opp_id = -1;
        Bundle bundle = getArguments();
        if (bundle != null) {
            opp_id = bundle.getInt("opp_id", -1);
        }
        final int fopp_id = opp_id;

        final Button YesButton = view.findViewById(R.id.YesButton);
        YesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                RunnableTask task = new AddFriendRunnableTask(context, StartActivity.host_dir + "add_friend.php"
                        , new String[]{"mobile", "password", "opp_id"},
                        new String[]{prefs.getString("mobile", ""), prefs.getString("password", ""), Integer.toString(fopp_id)});
                Thread thread = new Thread(task);
                thread.start();

                Fragment fragment = new FriendAdded();
                Bundle bundle = new Bundle();
                bundle.putBoolean("added", true);
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
                Fragment fragment = new FriendAdded();
                Bundle bundle = new Bundle();
                bundle.putBoolean("added", false);
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.commit();
            }
        });
        return view;
    }

    class AddFriendRunnableTask extends RunnableTask {
        AddFriendRunnableTask(Context context, String address, String[] categories, String[] args) {
            super(context, address, categories, args);
        }

        public void run() {
            super.run();
            if(result.equals("")){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //If there is an error, RunnableTask instance will set the result to ""(empty string)
                        //This is assumed to be a connection error
                        Toast.makeText(context, "Check your connection", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            try {
                //Check for result/response from server/database
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                if (response.equals("Success")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Sent friend request", Toast.LENGTH_SHORT).show();
                        }
                    });
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Usually "Failed to process request"
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }
}
