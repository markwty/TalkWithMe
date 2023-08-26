package com.example.talkwithme;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.ortiz.touchview.TouchImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Buggy extends Fragment implements BluetoothLeUart.Callback {
    private String classname = getClass().getName();
    private int id, destination_id, status = 0, prev_location;
    private String buggyName = "", accumrxValue = "";
    private HashMap<Integer, String> locationsDict = new HashMap<>();
    private TextView StatusLabel;
    private BluetoothLeUart uart;
    private Context context;
    private FragmentActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        final View view = inflater.inflate(R.layout.buggy, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int map_id = -1, start_id = -1;
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            map_id = bundle.getInt("map_id", -1);
            start_id = bundle.getInt("start_id", -1);
            destination_id = bundle.getInt("destination_id", -1);
        }
        final int fmap_id = map_id;

        final TouchImageView MapImageView = view.findViewById(R.id.MapImageView);
        ImageUtils imageUtils = new ImageUtils();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageUtils.setImage(activity, context, MapImageView, Integer.toString(fmap_id), ImageUtils.defaultBackground);
            }
        });

        StatusLabel = view.findViewById(R.id.StatusLabel);
        RunnableTask task = new CallBuggyRunnableTask(context, StartActivity.host_dir + "simple_buggy_call.php"
                , new String[]{"device", "location"}, new String[]{Integer.toString(id), Integer.toString(start_id)}, map_id);
        Thread thread = new Thread(task);
        thread.start();

        uart = new BluetoothLeUart(context);
        checkBluetoothPermissions();
        /*
        if(!uart.adapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
         */
        return view;
    }

    private void initialiseLocationsDict(int map_id) {
        String map_info = new FileUtils().loadJson(context, "Maps", Integer.toString(map_id));
        try {
            JSONObject collection = new JSONObject(map_info);
            JSONObject points = collection.getJSONObject("Points");
            int length = points.getInt("Length");
            for (int i = 0; i < length; i++) {
                JSONObject point = points.getJSONObject(Integer.toString(i + 1));
                String name = point.getString("Name");
                locationsDict.put(i + 1, name);
            }
        } catch(JSONException e) {
            Log.e(classname, e.toString());
        }
    }

    private class CallBuggyRunnableTask extends RunnableTask {
        private String classname = getClass().getName();
        private int map_id;

        CallBuggyRunnableTask(Context context, String address, String[] categories, String[] args, int map_id) {
            super(context, address, categories, args);
            this.map_id = map_id;
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
                if (response.equals("Success")) {
                    initialiseLocationsDict(this.map_id);
                    buggyName = "Buggy" + collection.getInt("ID");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Informs the user that the buggy call is successful
                            StatusLabel.append(context.getResources().getString(R.string.BuggyCalled));
                            Toast.makeText(context, "Called buggy", Toast.LENGTH_SHORT).show();
                        }
                    });
                    String start_id = find("location");
                    while (status == 0) {
                        RunnableTask task = new GetBuggyLocationRunnableTask(context, StartActivity.host_dir + "get_buggy_location.php"
                                , new String[]{"device"}, new String[]{Integer.toString(id)}, start_id);
                        task.run();
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            Log.e(classname, e.toString());
                        }
                    }
                    while (status == 1) {
                        RunnableTask task = new CheckWhetherBuggyLeftRunnableTask(context, StartActivity.host_dir + "check_buggy_left.php"
                                , new String[]{"device"}, new String[]{Integer.toString(id)});
                        task.run();
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            Log.e(classname, e.toString());
                        }
                    }
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Usually "Failed to process request"
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch(JSONException e) {
                Log.e(classname, e.toString());
            }
        }
    }

    private class GetBuggyLocationRunnableTask extends RunnableTask {
        private String classname = getClass().getName();
        private int start_id;
        GetBuggyLocationRunnableTask(Context context, String address, String[] categories, String[] args, String start_id) {
            super(context, address, categories, args);
            this.start_id = Integer.parseInt(start_id);
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
                if (response.equals("Success")) {
                    int location = collection.getInt("Location");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (location == start_id) {
                                StatusLabel.append(context.getResources().getString(R.string.BuggyReached, locationsDict.get(location)));
                                status = 1;
                            } else {
                                if (location != prev_location) {
                                    if (locationsDict.containsKey(location)) {
                                        StatusLabel.append(context.getResources().getString(R.string.BuggyLongLocation, locationsDict.get(location), location));
                                    } else {
                                        StatusLabel.append(context.getResources().getString(R.string.BuggyShortLocation, location));
                                    }
                                    prev_location = location;
                                }
                            }
                        }
                    });
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Usually "Failed to process request"
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch(JSONException e) {
                Log.e(classname, e.toString());
            }
        }
    }

    private class CheckWhetherBuggyLeftRunnableTask extends RunnableTask {
        private String classname = getClass().getName();
        CheckWhetherBuggyLeftRunnableTask(Context context, String address, String[] categories, String[] args) {
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
                if (response.equals("Buggy left")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            StatusLabel.append(context.getResources().getString(R.string.BuggyLeft));
                            status = 2;
                        }
                    });
                } else if (!response.equals("Buggy is here")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Usually "Failed to process request"
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch(JSONException e) {
                Log.e(classname, e.toString());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                uart.registerCallback(Buggy.this);
                checkBluetoothPermissions();
                //uart.startScan(buggyName);
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
    public void onConnected(BluetoothLeUart uart) {}

    @Override
    public void onConnectFailed(BluetoothLeUart uart) {}

    @Override
    public void onDisconnected(BluetoothLeUart uart) {}

    private void process(String processedValue){
        accumrxValue = "";
        int pos = processedValue.indexOf("::");
        if (pos != -1) {
            String field = processedValue.substring(0, pos);
            String value = processedValue.substring(pos + 2);
            if (field.equals(buggyName) && value.equals(Integer.toString(id))) {
                uart.send(id + "::" + destination_id);
            }
        }
    }

    @Override
    public void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
        String rxValue = rx.getStringValue(0);
        Log.v(classname, "Received: " + rxValue);
        if (rxValue.length() > 0) {
            if (rxValue.charAt(0) == 195 && rxValue.charAt(1) == 164) {//4294967295
                if (rxValue.charAt(rxValue.length() - 1) == 188 && rxValue.charAt(rxValue.length() - 2) == 195) {
                    accumrxValue = rxValue.substring(2, rxValue.length() - 4);
                    process(accumrxValue);
                } else {
                    accumrxValue = rxValue.substring(2);
                }
            } else if (rxValue.charAt(rxValue.length() - 1) == 188 && rxValue.charAt(rxValue.length() - 2) == 195) {
                accumrxValue += rxValue.substring(0, rxValue.length() - 2);
                process(accumrxValue);
            } else {
                accumrxValue += rxValue;
            }
        }
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {}

    BluetoothLeUart getUart(){
        return uart;
    }

    private void checkBluetoothPermissions(){
        //To get scan results
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED | ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, StartActivity.REQUEST_CODE_REQUEST_ACCESS_LOCATION);
        }
    }
}
