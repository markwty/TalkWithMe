package com.example.talkwithme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class Sending extends CallSearchFragment {
    private String classname = getClass().getName();
    private int opp_id = -1, time = 0;
    private String opp_name = "", token = "";
    private boolean stopSearch = false, through = false, finalize = false;
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.sending, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            opp_id = bundle.getInt("opp_id", -1);
        }

        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        String mobile = prefs.getString("mobile", "");
        String password = prefs.getString("password", "");
        final Button CancelButton = view.findViewById(R.id.CancelButton);
        CancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSearch = true;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        RunnableTask task = new RunnableTask(context, StartActivity.host_dir + "offline.php"
                                , new String[]{"mobile", "password"}
                                , new String[]{mobile, password});
                        Thread thread = new Thread(task);
                        thread.start();
                    }
                });
                thread.start();
                activity.onBackPressed();
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                checkPermissions();
                through = false;
                while (true) {
                    RunnableTask task = new OnlineRunnableTask(context, StartActivity.host_dir + "online_targeted.php"
                            , new String[]{"mobile", "password", "target"}
                            , new String[]{mobile, password, Integer.toString(opp_id)});
                    Thread thread = new Thread(task);
                    thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        Log.e(classname, e.toString());
                    }
                    if (through) {
                        break;
                    }
                    if (stopSearch) {
                        return;
                    }
                    try {
                        TimeUnit.SECONDS.sleep(15);
                    } catch (InterruptedException e) {
                        Log.e(classname, e.toString());
                    }
                }

                while (true) {
                    RunnableTask task = new CheckPeopleRunnableTask(context, StartActivity.host_dir + "check_people_online.php"
                            , new String[]{"mobile", "password"}
                            , new String[]{mobile, password});
                    Thread thread = new Thread(task);
                    thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        Log.e(classname, e.toString());
                    }
                    if (!opp_name.equals("")) {
                        break;
                    }
                    if (stopSearch) {
                        return;
                    }
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        Log.e(classname, e.toString());
                    }
                    time += 5;
                    if (time >= 30) {
                        //Restart
                        Fragment fragment = new Sending();
                        fragment.setArguments(getArguments());
                        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment, fragment, "current");
                        ft.commit();
                    }
                }
                finalize = true;

                Intent intent = new Intent(context, VidyoCallActivity.class);
                intent.putExtra("id", opp_id);
                intent.putExtra("opp_name", opp_name);
                intent.putExtra("token", token);
                startActivity(intent);
            }
        });
        thread.start();

        final VideoView SearchingVideoView = view.findViewById(R.id.SearchingVideoView);
        Uri uri = Uri.parse("android.resource://" + activity.getPackageName() + "/" + R.raw.searching);
        SearchingVideoView.setVideoURI(uri);
        SearchingVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
                float screenRatio = SearchingVideoView.getWidth() / (float) SearchingVideoView.getHeight();
                float scaleX = videoRatio / screenRatio;
                if (scaleX >= 1f) {
                    SearchingVideoView.setScaleX(scaleX);
                } else {
                    SearchingVideoView.setScaleY(1f / scaleX);
                }
                //Start the video when loaded/prepared
                SearchingVideoView.start();
            }
        });
        SearchingVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //Play video in loop
                SearchingVideoView.start();
            }
        });
        return view;
    }

    @Override
    public void onStop(){
        super.onStop();
        //Ensures loop don't run infinitely and crash the app
        stopSearch = true;
        if(finalize){
            return;
        }
        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        String mobile = prefs.getString("mobile", "");
        String password = prefs.getString("password", "");
        RunnableTask task = new RunnableTask(context, StartActivity.host_dir + "offline.php"
                , new String[]{"mobile", "password"}
                , new String[]{mobile, password});
        Thread thread = new Thread(task);
        thread.start();
    }

    class OnlineRunnableTask extends RunnableTask {
        OnlineRunnableTask(Context context, String address, String[] categories, String[] args) {
            super(context, address, categories, args);
        }

        public void run() {
            super.run();
            if(stopSearch){
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
                Log.v(classname, response);
                if (response.equals("Success")) {
                    through = true;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "You are now online! Please wait...", Toast.LENGTH_SHORT).show();
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

    class CheckPeopleRunnableTask extends RunnableTask {
        CheckPeopleRunnableTask(Context context, String address, String[] categories, String[] args) {
            super(context, address, categories, args);
        }

        public void run() {
            super.run();
            if(stopSearch){
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
                if (response.equals("Success")) {
                    opp_name = collection.getString("Opp_name");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "We found " + opp_name + "!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    opp_id = collection.getInt("Opp_id");
                    token = collection.getString("Token");
                    bundleOnResume = new Bundle();
                    bundleOnResume.putInt("opp_id", opp_id);
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

    private void checkPermissions(){
        //REQUEST_CODE_REQUEST_CAMERA is unique identifier for request (See under StartActivity activity class)
        //REQUEST_CODE_REQUEST_CAMERA allows customized response to when permission is granted or not (see onRequestPermissionsResult)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, StartActivity.REQUEST_CODE_REQUEST_CAMERA);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, StartActivity.REQUEST_CODE_REQUEST_AUDIO);
        }
    }
}
