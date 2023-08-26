package com.example.talkwithme;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class Sensors extends Fragment implements BluetoothLeUart.Callback {
    private String classname = getClass().getName();
    private static int id = -1;
    private String[] tabNames = new String[]{"Setup", "Graph", "Recommend"};
    private TextView ConnectedLabel;
    private ViewPager2 SensorsViewPager;
    private BluetoothLeUart uart;
    private Context context;
    private FragmentActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        final View view = inflater.inflate(R.layout.sensors, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        if (bundle != null) {
            id = bundle.getInt("id", -1);
        }

        uart = new BluetoothLeUart(context);
        checkBluetoothPermissions();
        /*
        if(!uart.adapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
         */

        ConnectedLabel = view.findViewById(R.id.ConnectedLabel);
        EditText NameOfDeviceEntry = view.findViewById(R.id.NameOfDeviceEntry);
        final SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        NameOfDeviceEntry.setText(prefs.getString("device" + id, ""));
        final Button ConnectButton = view.findViewById(R.id.ConnectButton);
        ConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String deviceName = NameOfDeviceEntry.getText().toString();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("device" + id, deviceName);
                editor.apply();
                checkBluetoothPermissions();
                uart.startScan(deviceName);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Scanning", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TabLayout SensorsTabLayout = view.findViewById(R.id.SensorsTabLayout);
        //Setting names for the tabs
        for(String tabName:tabNames){
            SensorsTabLayout.addTab(SensorsTabLayout.newTab().setText(tabName));
        }
        SensorsTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        CollectionAdapter collectionAdapter = new CollectionAdapter(this, SensorsTabLayout.getTabCount());
        SensorsViewPager = view.findViewById(R.id.SensorsViewPager);
        SensorsViewPager.setAdapter(collectionAdapter);

        //TabLayoutMediator synchronises the viewpager and tab layout
        new TabLayoutMediator(SensorsTabLayout, SensorsViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(tabNames[position]);
                SensorsViewPager.setCurrentItem(position, true);
            }
        }).attach();
        SensorsTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //Change tab on click of the names of the tabs
                SensorsViewPager.setCurrentItem(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        //Set default tab
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Set default tab
                SensorsViewPager.setCurrentItem(0);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                uart.registerCallback(Sensors.this);
                checkBluetoothPermissions();
                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                uart.startScan(prefs.getString("device" + id, ""));
            }
        });
        thread.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        uart.unregisterCallback(this);
        uart.disconnect();
    }

    //UART Callback event handlers.
    @Override
    public void onConnected(BluetoothLeUart uart) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ConnectedLabel.setText(R.string.Connected);
            }
        });
    }

    @Override
    public void onConnectFailed(BluetoothLeUart uart) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ConnectedLabel.setText(R.string.ConnectionFailed);
            }
        });
    }

    @Override
    public void onDisconnected(BluetoothLeUart uart) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ConnectedLabel.setText(R.string.Disconnected);
            }
        });
    }

    @Override
    public void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
        Log.v(classname, "Received: " + rx.getStringValue(0));
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {}

    BluetoothLeUart getUart(){
        return uart;
    }

    private static class CollectionAdapter extends FragmentStateAdapter {
        private int totalTabs;

        CollectionAdapter(Fragment fragment, int totalTabs) {
            super(fragment);
            this.totalTabs = totalTabs;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment;
            //Zero-based index
            if(position == 0){
                fragment = new Setup();
            }else if(position == 1){
                fragment = new Graph();
                Bundle bundle = new Bundle();
                bundle.putInt("id", id);
                fragment.setArguments(bundle);
            }else{
                //Should only be 2 but just in case there are more to prevent application from crashing
                fragment = new Recommend();
            }
            return fragment;
        }

        @Override
        public int getItemCount() {
            return totalTabs;
        }
    }

    private void checkBluetoothPermissions(){
        //To get scan results
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED | ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, StartActivity.REQUEST_CODE_REQUEST_ACCESS_LOCATION);
        }
    }
}
