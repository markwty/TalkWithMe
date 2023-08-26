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

public class Receiving extends CallSearchFragment {
    private String classname = getClass().getName();
    private int id = -1, time = 0;
    private String opp_name, token = "";
    private boolean stopSearch = false, through = false;
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.receiving, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        boolean temp = false;
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            temp = bundle.getBoolean("temp", false);
        }
        final boolean ftemp = temp;

        final Button CancelButton = view.findViewById(R.id.CancelButton);
        CancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSearch = true;
                activity.onBackPressed();
            }
        });

        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        String mobile = prefs.getString("mobile", "");
        String password = prefs.getString("password", "");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                checkPermissions();
                opp_name = "";
                while(true){
                    RunnableTask task = new FindPeopleRunnableTask(context, StartActivity.host_dir + "find_people_targeted.php"
                            , new String[]{"mobile", "password"}
                            , new String[]{mobile, password}, ftemp);
                    Thread thread = new Thread(task);
                    thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        Log.e(classname, e.toString());
                    }
                    if(!opp_name.equals("")){
                        break;
                    }
                    if(stopSearch){
                        return;
                    }
                    try{
                        TimeUnit.SECONDS.sleep(15);
                    }catch(InterruptedException e){
                        Log.e(classname, e.toString());
                    }
                }

                through = false;
                while(true){
                    RunnableTask task = new CheckStartRunnableTask(context, StartActivity.host_dir + "can_start_video_call.php"
                            , new String[]{"mobile", "password"}
                            , new String[]{mobile, password});
                    Thread thread = new Thread(task);
                    thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        Log.e(classname, e.toString());
                    }
                    if(through){
                        break;
                    }
                    if(stopSearch){
                        return;
                    }
                    try{
                        TimeUnit.SECONDS.sleep(5);
                    }catch(InterruptedException e){
                        Log.e(classname, e.toString());
                    }
                    time += 5;
                    if(time >= 30){
                        //Restart
                        Fragment fragment = new Receiving();
                        fragment.setArguments(getArguments());
                        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment, fragment, "current");
                        ft.commit();
                    }
                }

                Intent intent = new Intent(context, VidyoCallActivity.class);
                intent.putExtra("id", id);
                intent.putExtra("opp_name", opp_name);
                intent.putExtra("token", token);
                startActivity(intent);
            }
        });
        thread.start();

        final VideoView SearchingVideoView = view.findViewById(R.id.SearchingVideoView);
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.searching);
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
    public void onStop() {
        super.onStop();
        //Ensures loop don't run infinitely and crash the app
        stopSearch = true;
    }

    class FindPeopleRunnableTask extends RunnableTask {
        private boolean temp;
        FindPeopleRunnableTask(Context context, String address, String[] categories, String[] args, boolean temp) {
            super(context, address, categories, args);
            this.temp = temp;
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
                    int opp_id = collection.getInt("Opp_id");
                    opp_name = collection.getString("Opp_name");
                    token = collection.getString("Token");

                    bundleOnResume = new Bundle();
                    bundleOnResume.putInt("opp_id", opp_id);
                    bundleOnResume.putBoolean("temp", temp);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "We found " + opp_name + "!", Toast.LENGTH_SHORT).show();
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

    class CheckStartRunnableTask extends RunnableTask {
        CheckStartRunnableTask(Context context, String address, String[] categories, String[] args) {
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
                    through = true;
                }else if (response.equals("Restart")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Person went offline...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    activity.onBackPressed();
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

    /*
    private void checkDefaultDialer() {
        TelecomManager telecomManager = (TelecomManager) activity.getSystemService(TELECOM_SERVICE);
        String defaultPackage = null;
        if (telecomManager != null){
            defaultPackage = telecomManager.getDefaultDialerPackage();
        }

        if (defaultPackage != null){
            Log.v(classname, defaultPackage);
            if(!context.getPackageName().equals(defaultPackage)){
                Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.getPackageName());
                startActivityForResult(intent, StartActivity.REQUEST_CODE_SET_DEFAULT_DIALER);
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE}, StartActivity.REQUEST_CODE_REQUEST_CALL_PHONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == StartActivity.REQUEST_CODE_SET_DEFAULT_DIALER){
            if (resultCode == Activity.RESULT_OK) {
                Log.v(classname, "User accepted request to become default dialer");
            }else if (resultCode == Activity.RESULT_CANCELED) {
                Log.v(classname, "User declined request to become default dialer");
            }else{
                Log.v(classname, "Unexpected result code: " + resultCode);
            }
        }
    }
    */
}
