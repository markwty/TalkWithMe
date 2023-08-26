package com.example.talkwithme;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class ForgetPassword extends Fragment {
    private String classname = getClass().getName();
    private boolean through = false;
    private EditText EnterVerificationCodeEntry;
    private LinearLayout DetailsLinearLayout;
    private LinearLayout VerifyLinearLayout;
    private String mobile, password;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.forget_password, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        final EditText MobileEntry = view.findViewById(R.id.MobileEntry);
        final EditText PasswordEntry = view.findViewById(R.id.PasswordEntry);
        final EditText EnterPasswordAgainEntry = view.findViewById(R.id.EnterPasswordAgainEntry);
        DetailsLinearLayout = view.findViewById(R.id.DetailsLinearLayout);
        EnterVerificationCodeEntry = view.findViewById(R.id.EnterVerificationCodeEntry);
        VerifyLinearLayout = view.findViewById(R.id.VerifyLinearLayout);

        final Button NextButton = view.findViewById(R.id.NextButton);
        NextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mobile = MobileEntry.getText().toString();
                password = PasswordEntry.getText().toString();
                //Quick validation checks for mobile number and password
                if(mobile.length() != 8){
                    //Mobile number should contain only 8 digits (no country code etc)
                    MobileEntry.requestFocus();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Mobile number is invalid", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }else if(!password.equals(EnterPasswordAgainEntry.getText().toString())){
                    EnterPasswordAgainEntry.requestFocus();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }else if(password.equals("")){
                    PasswordEntry.requestFocus();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Password should not be empty", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        through = false;
                        RunnableTask task = new CheckMobileRunnableTask(context, StartActivity.host_dir + "check_mobile_in_use.php"
                                , new String[]{"mobile"}, new String[]{mobile});
                        Thread thread = new Thread(task);
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            Log.e(classname, e.toString());
                        }
                        if (through) {
                            //Phone authentication
                            //5 SMS per phone number per 4 hours
                            mAuth = FirebaseAuth.getInstance();
                            sendVerificationCode(mobile);
                        }
                    }
                });
                thread.start();
            }
        });

        final Button VerifyButton = view.findViewById(R.id.VerifyButton);
        VerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Manual verification by typing the verification code in "EnterVerificationCodeEntry" and clicking the "VerifyButton"
                signInWithCredential(EnterVerificationCodeEntry.getText().toString());
            }
        });

        final Button BackButton = view.findViewById(R.id.BackButton);
        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Allows user to retype mobile and/or password (or send authentication code again)
                DetailsLinearLayout.setVisibility(View.VISIBLE);
                VerifyLinearLayout.setVisibility(View.GONE);
            }
        });
        return view;
    }

    class CheckMobileRunnableTask extends RunnableTask{
        CheckMobileRunnableTask(Context context, String address, String[] categories, String[] args) {
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
                if (response.equals("Mobile is in use")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DetailsLinearLayout.setVisibility(View.GONE);
                            VerifyLinearLayout.setVisibility(View.VISIBLE);
                        }
                    });
                    through = true;
                }else if (response.equals("Mobile is not in use")){
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Mobile number is not registered", Toast.LENGTH_SHORT).show();
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

    class ChangePasswordRunnableTask extends RunnableTask{
        ChangePasswordRunnableTask(Context context, String address, String[] categories, String[] args) {
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
                            Toast.makeText(context, "Password changed!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("mobile", mobile);
                    editor.putString("password", password);
                    editor.apply();

                    int id = collection.getInt("ID");
                    String name = collection.getString("Name");
                    int gender = collection.getInt("Gender");
                    String organisation = collection.getString("Organisation");
                    int status = collection.getInt("Status");
                    int state = collection.getInt("State");

                    activity.getSupportFragmentManager().popBackStack();
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

    private void sendVerificationCode(String mobile) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+65" + mobile,
                60,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                mCallbacks);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            String code = phoneAuthCredential.getSmsCode();
            DetailsLinearLayout.setVisibility(View.GONE);
            VerifyLinearLayout.setVisibility(View.VISIBLE);
            if (code != null) {
                EnterVerificationCodeEntry.setText(code);
                signInWithCredential(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            mVerificationId = s;
        }
    };

    private void signInWithCredential(String code){
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        mAuth.signInWithCredential(credential).addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Verification success", Toast.LENGTH_SHORT).show();
                        }
                    });

                    RunnableTask ftask = new ChangePasswordRunnableTask(context, StartActivity.host_dir + "change_password.php"
                            , new String[]{"code", "mobile", "password"}, new String[]{"some_random_code", mobile, password});
                    Thread thread = new Thread(ftask);
                    thread.start();
                } else {
                    String message = "Something is wrong";
                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        message = "Invalid code entered...";
                    }
                    final String fmessage = message;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, fmessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
}