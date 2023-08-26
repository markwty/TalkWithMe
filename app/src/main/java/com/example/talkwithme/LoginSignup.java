package com.example.talkwithme;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginSignup extends Fragment {
    private String classname = getClass().getName();
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.login_signup, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        final EditText MobileEntry = view.findViewById(R.id.MobileEntry);
        final EditText PasswordEntry = view.findViewById(R.id.PasswordEntry);
        final CheckBox KeepMeSignedInCheckBox = view.findViewById(R.id.KeepMeSignedInCheckBox);
        final Button LoginButton = view.findViewById(R.id.LoginButton);
        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RunnableTask task = new LoginRunnableTask(context, StartActivity.host_dir + "verify.php"
                        , new String[]{"mobile", "password"}
                        , new String[]{MobileEntry.getText().toString(), PasswordEntry.getText().toString()}, KeepMeSignedInCheckBox.isChecked());
                Thread thread = new Thread(task);
                thread.start();
            }
        });

        final Button SignupButton = view.findViewById(R.id.SignupButton);
        SignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new StayInCondo();
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.addToBackStack(null);
                ft.commit();
            }
        });

        final Button ForgetButton = view.findViewById(R.id.ForgetButton);
        ForgetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new ForgetPassword();
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.addToBackStack(null);
                ft.commit();
            }
        });
        return view;
    }

    class LoginRunnableTask extends RunnableTask{
        boolean isChecked;
        LoginRunnableTask(Context context, String address, String[] categories, String[] args, boolean isChecked) {
            super(context, address, categories, args);
            this.isChecked = isChecked;
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
                if (response.equals("Login successful")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Informs the user that verification succeeds
                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show();
                        }
                    });

                    SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                    SharedPreferences.Editor editor = prefs.edit();
                    String mobile = find("mobile");
                    if(mobile == null){
                        return;
                    }
                    String password = find("password");
                    if(password == null){
                        return;
                    }
                    editor.putString("mobile", mobile);
                    editor.putString("password", password);
                    editor.putBoolean("Keep me signed in", isChecked);
                    editor.apply();

                    int id = collection.getInt("ID");
                    StartActivity.current_id = id;
                    String name = collection.getString("Name");
                    int gender = collection.getInt("Gender");
                    String organisation = collection.getString("Organisation");
                    int status = collection.getInt("Status");
                    int state = collection.getInt("State");

                    Fragment fragment = new ChatMove();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", id);
                    bundle.putString("name", name);
                    bundle.putInt("gender", gender);
                    bundle.putString("organisation", organisation);
                    bundle.putInt("status", status);
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
