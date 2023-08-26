package com.example.talkwithme;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.ortiz.touchview.TouchImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static android.content.Context.LOCATION_SERVICE;

public class Destination extends Fragment {
    private String classname = getClass().getName();
    private float scale;
    private ArrayAdapter<String> locationSpinnerAdapter;
    private HashMap<String, Integer> locationsDict = new HashMap<>();
    private List<String> locationsList = new LinkedList<>();
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.destination, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int id = -1, map_id = -1, start_id = -1;
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            map_id = bundle.getInt("map_id", -1);
            start_id = bundle.getInt("start_id", -1);
        }
        final int fid = id, fmap_id = map_id, fstart_id = start_id;

        final TouchImageView MapImageView = view.findViewById(R.id.MapImageView);
        final Spinner LocationSpinner = view.findViewById(R.id.LocationSpinner);
        final Button NextButton = view.findViewById(R.id.NextButton);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ImageUtils imageUtils = new ImageUtils();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.Dimension dimension = imageUtils.setImage(activity, context, MapImageView, Integer.toString(fmap_id), ImageUtils.defaultBackground);
                        int bitmapWidth = dimension.width;
                        int bitmapHeight = dimension.height;
                        try {
                            Field matchViewWidthField = MapImageView.getClass().getDeclaredField("matchViewWidth");
                            matchViewWidthField.setAccessible(true);
                            float matchViewWidth = (float) matchViewWidthField.get(MapImageView);
                            scale = matchViewWidth/(float)bitmapWidth;
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            Log.e(classname, e.toString());
                        }
                    }
                });
                if (updateLocationSpinnerAdapter(fmap_id, fstart_id)) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LocationSpinner.setAdapter(locationSpinnerAdapter);
                            NextButton.setEnabled(true);
                        }
                    });
                }
            }
        });
        thread.start();

        MapImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }
                try {
                    Method transformCoordTouchToBitmap = MapImageView.getClass().getDeclaredMethod("transformCoordTouchToBitmap", float.class, float.class, boolean.class);
                    transformCoordTouchToBitmap.setAccessible(true);
                    PointF bitmapPoint = (PointF) transformCoordTouchToBitmap.invoke(MapImageView, event.getX(), event.getY(), true);
                    Point point = findClosestPointToTouch(fmap_id, (int)bitmapPoint.x, (int)bitmapPoint.y, fstart_id);
                    if (point != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LocationSpinner.setSelection(locationSpinnerAdapter.getPosition(point.name));
                            }
                        });
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    Log.e(classname, e.toString());
                }
                return false;
            }
        });

        final Button AutoDetectButton = view.findViewById(R.id.AutoDetectButton);
        AutoDetectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermissions();
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Point point = findClosestPoint(fmap_id, fstart_id);
                    if (point != null) {
                        LocationSpinner.setSelection(locationSpinnerAdapter.getPosition(point.name));
                    }
                }
            }
        });

        NextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LocationSpinner.getSelectedItem() != null) {
                    String locationName = LocationSpinner.getSelectedItem().toString();
                    if (locationsDict.containsKey(locationName)) {
                        Fragment fragment = new Buggy();
                        Bundle bundle = new Bundle();
                        bundle.putInt("id", fid);
                        bundle.putInt("map_id", fmap_id);
                        bundle.putInt("start_id", fstart_id);
                        bundle.putInt("destination_id", Objects.requireNonNull(locationsDict.get(locationName)));
                        fragment.setArguments(bundle);
                        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment, fragment, "current");
                        ft.commit();
                    }
                }
            }
        });

        return view;
    }

    private boolean updateLocationSpinnerAdapter(int map_id, int start_id) {
        locationsList.clear();
        String map_info = new FileUtils().loadJson(context, "Maps", Integer.toString(map_id));
        try {
            JSONObject collection = new JSONObject(map_info);
            JSONObject points = collection.getJSONObject("Points");
            int length = points.getInt("Length");
            for (int i = 0; i < length; i++) {
                if (i + 1 == start_id) {
                    continue;
                }
                JSONObject point = points.getJSONObject(Integer.toString(i + 1));
                String name = point.getString("Name");
                locationsList.add(name);
                locationsDict.put(name, i + 1);
            }
        } catch(JSONException e) {
            Log.e(classname, e.toString());
            return false;
        }
        Collections.sort(locationsList, new Comparator<String>() {
            @Override
            public int compare(String c1, String c2) {
                return c1.compareTo(c2);
            }
        });
        locationSpinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, locationsList);
        locationSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return true;
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, StartActivity.REQUEST_CODE_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    static class Point{
        int index;
        String name;
        Point(int index, String name) {
            this.index = index;
            this.name = name;
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        //Haversine formula
        double latDistance = (lat2 - lat1) * (Math.PI/180);
        double lonDistance = (lon2 - lon1) * (Math.PI/180);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(lat1 * (Math.PI/180)) * Math.cos(lat2 * (Math.PI/180))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }

    private Point findClosestPoint(int map_id, int start_id) {
        Location location;
        double lat1 = 0, lon1 = 0;
        boolean coord_initialised = false;
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isNetworkEnabled || isGPSEnabled) {
                if (isNetworkEnabled) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            lat1 = location.getLatitude();
                            lon1 = location.getLongitude();
                            coord_initialised = true;
                        }
                    }
                }
                if (isGPSEnabled && !coord_initialised) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            lat1 = location.getLatitude();
                            lon1 = location.getLongitude();
                            coord_initialised = true;
                        }
                    }
                }
            }
        }

        if (!coord_initialised) {
            return null;
        }
        String map_info = new FileUtils().loadJson(context, "Maps", Integer.toString(map_id));
        try {
            JSONObject collection = new JSONObject(map_info);
            JSONObject points = collection.getJSONObject("Points");
            int length = points.getInt("Length");
            if (length == 0) {
                return null;
            }
            double minDist = 40075, dist;
            int pointIndex = 0;
            String pointName = "";
            for (int i = 0; i < length; i++) {
                if (i + 1 == start_id) {
                    continue;
                }
                JSONObject point = points.getJSONObject(Integer.toString(i + 1));
                String name = point.getString("Name");
                double lon2 = point.getDouble("Longitude");
                double lat2 = point.getDouble("Latitude");
                dist = calculateDistance(lat1, lon1, lat2, lon2);
                if (dist < minDist) {
                    minDist = dist;
                    pointIndex = i + 1;
                    pointName = name;
                }
            }
            return new Point(pointIndex, pointName);
        } catch(JSONException e) {
            Log.e(classname, e.toString());
            return null;
        }
    }

    private Point findClosestPointToTouch(int map_id, int x, int y, int start_id) {
        String map_info = new FileUtils().loadJson(context, "Maps", Integer.toString(map_id));
        try {
            JSONObject collection = new JSONObject(map_info);
            JSONObject points = collection.getJSONObject("Points");
            int length = points.getInt("Length");
            if (length == 0) {
                return null;
            }
            int minDist = 2500, dist;// set minDist to a suitable value
            int pointIndex = 0;
            String pointName = "";
            for (int i = 0; i < length; i++) {
                if (i + 1 == start_id) {
                    continue;
                }
                JSONObject point = points.getJSONObject(Integer.toString(i + 1));
                String name = point.getString("Name");
                int xx = Math.round(point.getInt("x") * scale);
                int yy = Math.round(point.getInt("y") * scale);
                dist = ((xx - x) * (xx - x)) + ((yy - y) * (yy - y));
                if (dist < minDist) {
                    minDist = dist;
                    pointIndex = i + 1;
                    pointName = name;
                }
            }
            if (pointIndex == 0) {
                return null;
            }
            return new Point(pointIndex, pointName);
        }catch(JSONException e) {
            Log.e(classname, e.toString());
            return null;
        }
    }
}