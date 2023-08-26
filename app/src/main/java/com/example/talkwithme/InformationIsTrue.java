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

public class InformationIsTrue extends Fragment {
    private String classname = getClass().getName();
    private int id = -1, gender = -1, state = -1;
    private String name = "";
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.information_is_true, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();
        if (activity == null) {
            return view;
        }

        String selected_organisation = "", encodedImage = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            name = bundle.getString("name", "");
            gender = bundle.getInt("gender", -1);
            state = bundle.getInt("state", -1);
            selected_organisation = bundle.getString("selected organisation", "");
            encodedImage = bundle.getString("encodedImage", "");
        }
        final String fselected_organisation = selected_organisation, fencodedImage = encodedImage;


        final Button YesButton = view.findViewById(R.id.YesButton);
        YesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fencodedImage.equals("")){
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Something went wrong... Please fill in the form again", Toast.LENGTH_SHORT).show();
                        }
                    });
                    activity.onBackPressed();
                    return;
                }

                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                String mobile = prefs.getString("mobile", "");
                String password = prefs.getString("password", "");
                RunnableTask task = new AddRequestRunnableTask(context, StartActivity.host_dir + "add_application_request.php"
                        , new String[]{"mobile", "password", "organisation", "image"}
                        , new String[]{mobile, password, fselected_organisation, fencodedImage});
                Thread thread = new Thread(task);
                thread.start();
            }
        });

        final Button NoButton = view.findViewById(R.id.NoButton);
        NoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onBackPressed();
            }
        });
        return view;
    }

    private class AddRequestRunnableTask extends RunnableTask{
        AddRequestRunnableTask(Context context, String address, String[] categories, String[] args){
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
                    Fragment fragment = new PendingRequest();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", id);
                    bundle.putString("name", name);
                    bundle.putInt("gender", gender);
                    bundle.putInt("state", state);
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
                }
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }
}