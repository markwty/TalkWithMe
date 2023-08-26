package com.example.talkwithme;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

public class ContactsVolunteer extends Fragment {
    private String classname = getClass().getName();
    private boolean pending = false;
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.contacts_volunteer, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int id = -1, gender = -1, status = -1;
        String name = "", organisation = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            name = bundle.getString("name", "");
            gender = bundle.getInt("gender", -1);
            organisation = bundle.getString("organisation", "");
            status = bundle.getInt("status", -1);
        }
        final int fid = id, fgender = gender, fstatus = status;
        final String fname = name, forganisation = organisation;

        final Button ContactsButton = view.findViewById(R.id.ContactsButton);
        ContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new Contacts();
                Bundle bundle = new Bundle();
                bundle.putInt("id", fid);
                bundle.putString("name", fname);
                bundle.putInt("gender", fgender);
                bundle.putString("organisation", forganisation);
                bundle.putInt("status", fstatus);
                bundle.putInt("state", 1);
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.addToBackStack("Home");
                ft.commit();
            }
        });

        final Button VolunteerButton = view.findViewById(R.id.VolunteerButton);
        VolunteerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fstatus == 1){
                    Fragment fragment = new SearchingForElderly();
                    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    ft.addToBackStack("Home");
                    ft.commit();
                }else{
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            pending = false;
                            RunnableTask task = new CheckPendingRunnableTask(context, StartActivity.host_dir + "check_for_pending_application.php"
                                    , new String[]{"id"}, new String[]{Integer.toString(fid)});
                            Thread thread = new Thread(task);
                            thread.start();
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                Log.e(classname, e.toString());
                            }
                            if (pending) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setMessage("Do you want to check pending request?");
                                builder.setCancelable(true);
                                builder.setPositiveButton(
                                        "Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                Fragment fragment = new PendingRequest();
                                                Bundle bundle = new Bundle();
                                                bundle.putInt("id", fid);
                                                bundle.putString("name", fname);
                                                bundle.putInt("gender", fgender);
                                                bundle.putInt("state", 1);
                                                fragment.setArguments(bundle);
                                                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                                                ft.replace(R.id.fragment, fragment, "current");
                                                ft.addToBackStack("Home");
                                                ft.commit();
                                            }
                                        });
                                builder.setNegativeButton(
                                        "No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                Fragment fragment = new GetStarted();
                                                Bundle bundle = new Bundle();
                                                bundle.putInt("id", fid);
                                                bundle.putString("name", fname);
                                                bundle.putInt("gender", fgender);
                                                bundle.putInt("state", 1);
                                                fragment.setArguments(bundle);
                                                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                                                ft.replace(R.id.fragment, fragment, "current");
                                                ft.addToBackStack("Home");
                                                ft.commit();
                                            }
                                        });
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AlertDialog alert = builder.create();
                                        alert.show();
                                    }
                                });
                            } else {
                                Fragment fragment = new GetStarted();
                                Bundle bundle = new Bundle();
                                bundle.putInt("id", fid);
                                bundle.putString("name", fname);
                                bundle.putInt("gender", fgender);
                                bundle.putInt("state", 1);
                                fragment.setArguments(bundle);
                                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                                ft.replace(R.id.fragment, fragment, "current");
                                ft.addToBackStack("Home");
                                ft.commit();
                            }
                        }
                    });
                    thread.start();
                }
            }
        });

        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        RunnableTask task = new RunnableTask(context, StartActivity.host_dir + "update_token.php"
                , new String[]{"mobile", "password", "token"}, new String[]{prefs.getString("mobile", "")
                , prefs.getString("password", ""), prefs.getString("token", "")});
        Thread thread = new Thread(task);
        thread.start();
        return view;
    }

    class CheckPendingRunnableTask extends RunnableTask{
        CheckPendingRunnableTask(Context context, String address, String[] categories, String[] args) {
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
            try{
                //Check for result/response from server/database
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                if (response.equals("Pending request")) {
                    pending = true;
                }else{
                    pending = false;
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