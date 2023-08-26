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

import java.util.concurrent.TimeUnit;

public class PendingRequest extends Fragment {
    private String classname = getClass().getName();
    private int id = -1, gender = -1, state = -1;
    private String name = "";
    Bundle bundleToHome = null;
    private boolean stop = false;
    private int requestStatus = 0;
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pending_request, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            name = bundle.getString("name", "");
            gender = bundle.getInt("gender", -1);
            state = bundle.getInt("state", -1);
        }

        final Button CancelButton = view.findViewById(R.id.CancelButton);
        CancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop = true;
                activity.onBackPressed();
            }
        });

        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        String mobile = prefs.getString("mobile", "");
        String password = prefs.getString("password", "");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    RunnableTask task = new CheckRequestRunnableTask(context, StartActivity.host_dir + "check_application_status.php"
                            , new String[]{"mobile", "password"}, new String[]{mobile, password});
                    Thread thread = new Thread(task);
                    thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        Log.e(classname, e.toString());
                    }
                    if(requestStatus != 0){
                        break;
                    }
                    if(stop){
                        break;
                    }
                    try {
                        TimeUnit.SECONDS.sleep(15);
                    } catch (InterruptedException e) {
                        Log.e(classname, e.toString());
                    }
                }
                if(requestStatus == 1){
                    Fragment fragment = new RequestVerified();
                    fragment.setArguments(bundleToHome);
                    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    ft.commit();
                }else if(requestStatus == 2){
                    Fragment fragment = new RequestRejected();
                    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    ft.commit();
                }
            }
        });
        thread.start();
        return view;
    }

    @Override
    public void onStop(){
        super.onStop();
        stop = true;
    }

    private class CheckRequestRunnableTask extends RunnableTask{
        CheckRequestRunnableTask(Context context, String address, String[] categories, String[] args){
            super(context, address, categories, args);
        }
        public void run() {
            super.run();
            if(stop){
                return;
            }
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
                if (response.equals("Request is accepted")) {
                    //Change organisation and status, recreate "Home" fragment
                    bundleToHome = new Bundle();
                    bundleToHome.putInt("id", id);
                    bundleToHome.putString("name", name);
                    bundleToHome.putInt("gender", gender);
                    bundleToHome.putString("organisation", collection.getString("Organisation"));
                    bundleToHome.putInt("status", 1);
                    bundleToHome.putInt("state", state);
                    requestStatus = 1;
                }else if(response.equals("Request is rejected")) {
                    requestStatus = 2;
                }else{
                    requestStatus = 0;
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