package com.example.talkwithme;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class Setup extends Fragment {
    private String classname = getClass().getName();
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.setup, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        final EditText SSIDEntry = view.findViewById(R.id.SSIDEntry);
        final Button RetrieveButton = view.findViewById(R.id.RetrieveButton);
        RetrieveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        checkPermissions();
                        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if (wifiManager == null) {
                            return;
                        }
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        String ssid = "";
                        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                            ssid = wifiInfo.getSSID();
                        }
                        if(ssid.startsWith("\"")){
                            ssid = ssid.substring(1, ssid.length() - 1);
                        }
                        SSIDEntry.setText(ssid);
                    }
                });
                thread.start();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Retrieving", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        //RetrieveButton.callOnClick();

        final EditText PasswordEntry = view.findViewById(R.id.PasswordEntry);
        final Button SendButton = view.findViewById(R.id.SendButton);
        SendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = activity.getSupportFragmentManager();
                Fragment fragment = fm.findFragmentByTag("current");
                if(fragment == null){
                    return;
                }
                if (fragment instanceof Sensors) {
                    BluetoothLeUart uart = ((Sensors) fragment).getUart();
                    String ssid = SSIDEntry.getText().toString();
                    String password = PasswordEntry.getText().toString();
                    if (ssid.contains("ä") || ssid.contains("ü") || ssid.contains("::")) {
                        Toast.makeText(context, "ssid should not contain 'ä', 'ü' or '::'", Toast.LENGTH_LONG).show();
                    }else if (password.contains("ä") || password.contains("ü") || password.contains("::")) {
                        Toast.makeText(context, "password should not contain 'ä', 'ü' or '::'", Toast.LENGTH_LONG).show();
                    }else{
                        if (uart.isConnected()) {
                            uart.send("ssid::" + ssid);
                            uart.send("password::" + password);
                        }
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Sent", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        return view;
    }

    private void checkPermissions(){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, StartActivity.REQUEST_CODE_REQUEST_ACCESS_WIFI_STATE);
        }
    }
}
