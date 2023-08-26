package com.example.talkwithme;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

public class CallHasEnded extends TransitionFragment {
    private String classname = getClass().getName();
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.call_has_ended, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int opp_id = -1;
        boolean temp = false;
        Bundle bundle = getArguments();
        if (bundle != null) {
            opp_id = bundle.getInt("opp_id", -1);
            temp = bundle.getBoolean("temp", false);
        }
        final int fopp_id = opp_id;
        final boolean ftemp = temp;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(stop){
                    return;
                }
                if(ftemp){
                    activity.finish();
                    return;
                }
                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                String mobile = prefs.getString("mobile", "");
                String password = prefs.getString("password", "");
                RunnableTask task = new CheckFriendRunnableTask(context, StartActivity.host_dir + "check_friend.php"
                        , new String[]{"mobile", "password", "opp_id"}
                        , new String[]{mobile, password, Integer.toString(fopp_id)}, fopp_id);
                Thread thread = new Thread(task);
                thread.start();
            }
        }, 5000);
        return view;
    }

    private class CheckFriendRunnableTask extends RunnableTask{
        private int fopp_id;
        CheckFriendRunnableTask(Context context, String address, String[] categories, String[] args, int fopp_id){
            super(context, address, categories, args);
            this.fopp_id = fopp_id;
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
            try{
                //Check for result/response from server/database
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                if (response.equals("Already friends")) {
                    activity.onBackPressed();
                }else if(response.equals("Not friends")) {
                    Fragment fragment = new AddFriend();
                    Bundle bundle = new Bundle();
                    bundle.putInt("opp_id", fopp_id);
                    fragment.setArguments(bundle);
                    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    ft.commit();
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Usually "Failed to process request"
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                    activity.onBackPressed();
                }
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }
}