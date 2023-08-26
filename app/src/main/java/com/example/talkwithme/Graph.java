package com.example.talkwithme;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Graph extends Fragment {
    private String classname = getClass().getName();
    private int id;
    private LineGraphSeries<DataPoint> bpm_series, temperature_series, moisture_series;
    private EditText BPMEntry, TemperatureEntry, MoistureEntry;
    private GraphView BPMGraph, TemperatureGraph, MoistureGraph;
    private DateFormat displayDateFormat = new SimpleDateFormat("MMM-d \n HH:mm", Locale.getDefault());
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.graph, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
        }

        BPMEntry = view.findViewById(R.id.BPMEntry);
        TemperatureEntry = view.findViewById(R.id.TemperatureEntry);
        MoistureEntry = view.findViewById(R.id.MoistureEntry);

        BPMGraph = view.findViewById(R.id.BPMGraph);
        BPMGraph.setTitle("BPM");
        BPMGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        BPMGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX){
                if(isValueX){
                    return displayDateFormat.format(new Date((long)value));
                }else{
                    return super.formatLabel(value, false);
                }
            }
        });

        TemperatureGraph = view.findViewById(R.id.TemperatureGraph);
        TemperatureGraph.setTitle("Temperature");
        TemperatureGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        TemperatureGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX){
                if(isValueX){
                    return displayDateFormat.format(new Date((long)value));
                }else{
                    return super.formatLabel(value, false);
                }
            }
        });

        MoistureGraph = view.findViewById(R.id.MoistureGraph);
        MoistureGraph.setTitle("Moisture");
        MoistureGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        MoistureGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX){
                if(isValueX){
                    return displayDateFormat.format(new Date((long)value));
                }else{
                    return super.formatLabel(value, false);
                }
            }
        });

        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        String deviceName = prefs.getString("device" + id, "");
        RunnableTask task = new GetSensorsDataRunnableTask(context, StartActivity.host_dir + "get_sensors_data.php"
                , new String[]{"device"}, new String[]{deviceName}, "sensors_table");
        Thread thread = new Thread(task);
        thread.start();

        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        String deviceName = prefs.getString("device" + id, "");
        RunnableTask task = new GetSensorsDataRunnableTask(context, StartActivity.host_dir + "get_sensors_data.php"
                , new String[]{"device"}, new String[]{deviceName}, "sensors_table");
        Thread thread = new Thread(task);
        thread.start();
    }

    private class GetSensorsDataRunnableTask extends RunnableTask{
        String TABLE_NAME;
        String[] COL_NAMES;
        GetSensorsDataRunnableTask(Context context, String address, String[] categories, String[] args
                , String TABLE_NAME){
            super(context, address, categories, args);
            this.TABLE_NAME = TABLE_NAME;
            this.COL_NAMES = DatabaseHelper.tableColumnsMap.get(TABLE_NAME);
        }
        public void run() {
            super.run();
            updateDatabase();
            updateAdapter();
        }
        private void updateDatabase(){
            if(result.equals("")){
                return;
            }

            DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(id), TABLE_NAME, COL_NAMES);
            try {
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                if (response.equals("Success")) {
                    String[] values = new String[COL_NAMES.length];
                    StringBuilder printValues = new StringBuilder();
                    for (int ii = 0; ii < COL_NAMES.length; ii++) {
                        values[ii] = collection.getString(COL_NAMES[ii]);
                        printValues.append(values[ii]);
                    }
                    String time = collection.getString("Time");
                    int loop = 3;
                    if (!dbHelper.fieldExist("Time", time)) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Updating data", Toast.LENGTH_SHORT).show();
                            }
                        });
                        if (dbHelper.getRowCount() < loop) {
                            SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                            int current = prefs.getInt("current" + id, 1);
                            boolean success = dbHelper.insertData(Integer.toString(current), values);
                            if (success) {
                                SharedPreferences.Editor editor = prefs.edit();
                                current++;
                                if (current == loop + 1) {
                                    current = 1;
                                }
                                editor.putInt("current" + id, current);
                                editor.apply();
                            }else{
                                Log.e(classname, "Failed to insert data " + printValues.toString());
                            }
                        }else{
                            SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                            int current = prefs.getInt("current" + id, 1);
                            boolean success = dbHelper.updateData(Integer.toString(current), values);
                            if (success) {
                                SharedPreferences.Editor editor = prefs.edit();
                                current++;
                                if (current == loop + 1) {
                                    current = 1;
                                }
                                editor.putInt("current" + id, current);
                                editor.apply();
                            }else{
                                Log.e(classname, "Failed to update data " + printValues.toString());
                            }
                        }
                    }
                }
            } catch(JSONException e) {
                Log.e(classname, e.toString());
            }
        }
    }

    private void updateAdapter(){
        DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(id), "sensors_table");
        Cursor res = dbHelper.getAllData();
        bpm_series = new LineGraphSeries<>();
        temperature_series = new LineGraphSeries<>();
        moisture_series = new LineGraphSeries<>();

        if(res.getCount() == 0) {
            Log.v(classname, "Nothing to display");
        }else{
            List<SensorsEntry> SensorsTable = new ArrayList<>();
            float bpm, temperature, moisture;
            String time;
            while (res.moveToNext()) {
                time = res.getString(res.getColumnIndex("Time"));
                bpm = Float.parseFloat(res.getString(res.getColumnIndex("BPM")));
                temperature = Float.parseFloat(res.getString(res.getColumnIndex("Temperature")));
                moisture = Float.parseFloat(res.getString(res.getColumnIndex("Moisture")));
                SensorsTable.add(new SensorsEntry(time, bpm, temperature, moisture));
            }

            Collections.sort(SensorsTable, new Comparator<SensorsEntry>() {
                @Override
                public int compare(SensorsEntry o1, SensorsEntry o2) {
                    return o1.time.compareTo(o2.time);
                }
            });

            long firstTime = 0;
            for (int i = 0; i < SensorsTable.size(); i++) {
                SensorsEntry sensorsEntry = SensorsTable.get(i);
                Date datetime = null;
                try {
                    datetime = dateFormat.parse(sensorsEntry.time);
                } catch(ParseException e) {
                    Log.e(classname, e.toString());
                }
                if (datetime == null) {
                    datetime = new Date();
                }
                long ltime = datetime.getTime() + 3600 * 8 * 1000;
                bpm_series.appendData(new DataPoint(ltime, sensorsEntry.bpm), true, 3);
                temperature_series.appendData(new DataPoint(ltime, sensorsEntry.temperature), true, 3);
                moisture_series.appendData(new DataPoint(ltime, sensorsEntry.moisture), true, 3);

                if (i == 0) {
                    firstTime = ltime;
                }
                if (i == SensorsTable.size() - 1) {
                    final float fcurrent_bpm = sensorsEntry.bpm;
                    final float fcurrent_temperature = sensorsEntry.temperature;
                    final float fcurrent_moisture = sensorsEntry.moisture;
                    final float ffirstTime = firstTime, fltime = ltime;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BPMEntry.setText(String.valueOf(fcurrent_bpm));
                            TemperatureEntry.setText(String.valueOf(fcurrent_temperature));
                            MoistureEntry.setText(String.valueOf(fcurrent_moisture));
                            BPMGraph.removeAllSeries();
                            TemperatureGraph.removeAllSeries();
                            MoistureGraph.removeAllSeries();
                            BPMGraph.addSeries(bpm_series);
                            TemperatureGraph.addSeries(temperature_series);
                            MoistureGraph.addSeries(moisture_series);

                            Viewport viewport = BPMGraph.getViewport();
                            viewport.setXAxisBoundsManual(true);
                            viewport.setMinX(ffirstTime);
                            viewport.setMaxX(fltime);

                            viewport = TemperatureGraph.getViewport();
                            viewport.setXAxisBoundsManual(true);
                            viewport.setMinX(ffirstTime);
                            viewport.setMaxX(fltime);

                            viewport = MoistureGraph.getViewport();
                            viewport.setXAxisBoundsManual(true);
                            viewport.setMinX(ffirstTime);
                            viewport.setMaxX(fltime);
                        }
                    });
                }
            }
        }
        res.close();
        dbHelper.db.close();
    }

    static class SensorsEntry{
        String time;
        float bpm, temperature, moisture;
        SensorsEntry(String time, float bpm, float temperature, float moisture) {
            this.time = time;
            this.bpm = bpm;
            this.temperature = temperature;
            this.moisture = moisture;
        }
    }
}
