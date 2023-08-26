package com.example.talkwithme;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.ortiz.touchview.TouchImageView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.content.Context.LOCATION_SERVICE;

public class StartLocation extends Fragment {
    private String classname = getClass().getName();
    private int y_offset;
    private float scale;
    private HashMap<String, Integer> mapsDict = new HashMap<>();
    private List<String> mapsList = new LinkedList<>();
    private ArrayAdapter<String> locationSpinnerAdapter;
    private HashMap<String, Integer> locationsDict = new HashMap<>();
    private List<String> locationsList = new LinkedList<>();
    private Spinner MapSpinner;
    private Field matchViewWidthField = null;
    private Method transformCoordTouchToBitmap = null, transformCoordBitmapToTouch = null;
    private Animation pulse = null;
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.start_location, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int id = -1;
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
        }
        final int fid = id;

        MapSpinner = view.findViewById(R.id.MapSpinner);
        final TouchImageView MapImageView = view.findViewById(R.id.MapImageView);
        final Spinner LocationSpinner = view.findViewById(R.id.LocationSpinner);
        final Button NextButton = view.findViewById(R.id.NextButton);
        MapSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String text = parent.getItemAtPosition(position).toString();
                if (mapsDict.containsKey(text)) {
                    int map_id = Objects.requireNonNull(mapsDict.get(text));
                    ImageUtils imageUtils = new ImageUtils();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.Dimension dimension = imageUtils.setImage(activity, context, MapImageView, Integer.toString(map_id), ImageUtils.defaultBackground);
                            int bitmapWidth = dimension.width;
                            int bitmapHeight = dimension.height;
                            try {
                                if (matchViewWidthField == null) {
                                    matchViewWidthField = MapImageView.getClass().getDeclaredField("matchViewWidth");
                                    matchViewWidthField.setAccessible(true);
                                }
                                float matchViewWidth = (float) matchViewWidthField.get(MapImageView);
                                scale = matchViewWidth/(float)bitmapWidth;
                                y_offset = (int) ((MapImageView.getHeight() - bitmapHeight * scale)/2.0);
                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                Log.e(classname, e.toString());
                            }
                        }
                    });
                    if (updateLocationSpinnerAdapter(map_id)) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LocationSpinner.setAdapter(locationSpinnerAdapter);
                                NextButton.setEnabled(true);
                            }
                        });
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final ImageView OverlayImageView = view.findViewById(R.id.OverlayImageView);
        MapImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }
                try {
                    if (transformCoordTouchToBitmap == null) {
                        transformCoordTouchToBitmap = MapImageView.getClass().getDeclaredMethod("transformCoordTouchToBitmap", float.class, float.class, boolean.class);
                        transformCoordTouchToBitmap.setAccessible(true);
                    }
                    PointF bitmapPoint = (PointF) transformCoordTouchToBitmap.invoke(MapImageView, event.getX(), event.getY(), true);

                    String text = MapSpinner.getSelectedItem().toString();
                    if (mapsDict.containsKey(text)) {
                        int map_id = mapsDict.get(text);
                        Point point = findClosestPointToTouch(map_id, (int)bitmapPoint.x, (int)bitmapPoint.y);
                        if (point != null) {
                            if (transformCoordBitmapToTouch == null) {
                                transformCoordBitmapToTouch = MapImageView.getClass().getDeclaredMethod("transformCoordBitmapToTouch", float.class, float.class);
                                transformCoordBitmapToTouch.setAccessible(true);
                            }
                            int[] viewCoords = new int[2];
                            MapImageView.getLocationOnScreen(viewCoords);
                            PointF touchPoint = (PointF) transformCoordBitmapToTouch.invoke(MapImageView, point.x, point.y);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    LocationSpinner.setSelection(locationSpinnerAdapter.getPosition(point.name));
                                    ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) OverlayImageView.getLayoutParams();
                                    marginParams.leftMargin = Math.round(touchPoint.x - dpToPixels(context, 10));
                                    marginParams.topMargin = Math.round(touchPoint.y - dpToPixels(context, 10) + y_offset);
                                    OverlayImageView.requestLayout();
                                    if (pulse == null) {
                                        pulse = AnimationUtils.loadAnimation(context, R.anim.location_pulse);
                                        pulse.setAnimationListener(new Animation.AnimationListener() {
                                            @Override
                                            public void onAnimationStart(Animation animation) {
                                                OverlayImageView.setVisibility(View.VISIBLE);
                                            }

                                            @Override
                                            public void onAnimationEnd(Animation animation) {
                                                OverlayImageView.setVisibility(View.INVISIBLE);
                                            }

                                            @Override
                                            public void onAnimationRepeat(Animation animation) {}
                                        });
                                    }
                                    OverlayImageView.startAnimation(pulse);
                                }
                            });
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    Log.e(classname, e.toString());
                }
                return false;
            }
        });
        /*
        final CustomImageView OverlayImageView = view.findViewById(R.id.OverlayImageView);
        OverlayImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int[] viewCoords = new int[2];
                OverlayImageView.getLocationOnScreen(viewCoords);
                int touchX = (int) event.getX();
                int touchY = (int) event.getY();
                int imageX = touchX - viewCoords[0];
                int imageY = touchY - viewCoords[1];
                return true;
            }
        });
        */
        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        String modified = prefs.getString("modified(map)", "0000-00-00 00:00:00");
        RunnableTask task = new GetMapsRunnableTask(context, StartActivity.host_dir + "get_maps.php"
                , new String[]{"modified"}, new String[]{modified}, "map_table", fid);
        Thread thread = new Thread(task);
        thread.start();

        final Button AutoDetectButton = view.findViewById(R.id.AutoDetectButton);
        AutoDetectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermissions();
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (MapSpinner.getSelectedItem() != null) {
                        String text = MapSpinner.getSelectedItem().toString();
                        if (mapsDict.containsKey(text)) {
                            int map_id = Objects.requireNonNull(mapsDict.get(text));
                            Point point = findClosestPoint(map_id);
                            if (point != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        LocationSpinner.setSelection(locationSpinnerAdapter.getPosition(point.name));
                                    }
                                });
                            }
                        }
                    }
                }
            }
        });

        NextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MapSpinner.getSelectedItem() != null && LocationSpinner.getSelectedItem() != null) {
                    String mapName = MapSpinner.getSelectedItem().toString();
                    String locationName = LocationSpinner.getSelectedItem().toString();
                    if (mapsDict.containsKey(mapName) && locationsDict.containsKey(locationName)) {
                        Fragment fragment = new Destination();
                        Bundle bundle = new Bundle();
                        bundle.putInt("id", fid);
                        bundle.putInt("map_id", Objects.requireNonNull(mapsDict.get(mapName)));
                        bundle.putInt("start_id", Objects.requireNonNull(locationsDict.get(locationName)));
                        fragment.setArguments(bundle);
                        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment, fragment, "current");
                        ft.addToBackStack("Main");
                        ft.commit();
                    }
                }
            }
        });

        return view;
    }

    private int dpToPixels(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private class GetMapsRunnableTask extends RunnableTask {
        private String classname = getClass().getName();
        private String TABLE_NAME;
        private String[] COL_NAMES;
        private int id;

        GetMapsRunnableTask(Context context, String address, String[] categories, String[] args
                , String TABLE_NAME, int id) {
            super(context, address, categories, args);
            this.TABLE_NAME = TABLE_NAME;
            this.COL_NAMES = DatabaseHelper.tableColumnsMap.get(TABLE_NAME);
            this.id = id;
        }

        public void run() {
            super.run();
            updateDatabase();
        }

        private void updateDatabase() {
            if (result.equals("")) {
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

            DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(this.id), TABLE_NAME, COL_NAMES);
            ImageUtils imageUtils = new ImageUtils();
            FileUtils fileUtils = new FileUtils();
            try {
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                if (response.equals("Success")) {
                    collection = collection.getJSONObject("List");
                    for (int i = 0; i < collection.getInt("length"); i++) {
                        JSONObject item = collection.getJSONObject(Integer.toString(i));
                        String[] values = new String[COL_NAMES.length];
                        StringBuilder printValues = new StringBuilder();
                        for (int ii = 0; ii < COL_NAMES.length; ii++) {
                            values[ii] = item.getString(COL_NAMES[ii]);
                            printValues.append(values[ii]);
                        }
                        String ID = item.getString("ID");
                        if (dbHelper.IDExist(ID)) {
                            boolean success = dbHelper.updateData(ID, values);
                            if (!success) {
                                Log.e(classname, "Failed to update data " + printValues.toString());
                            }
                        } else {
                            boolean success = dbHelper.insertData(ID, values);
                            if (!success) {
                                Log.e(classname, "Failed to insert data " + printValues.toString());
                            }
                        }
                        String pointsJson = item.getString("Info");
                        if (!pointsJson.equals("")) {
                            boolean success = fileUtils.saveJson(context, pointsJson, "Maps", ID);
                            if (!success) {
                                Log.v(classname, "Map info failed to save");
                            }
                        }
                        String encodedImage = item.getString("Map");
                        if (!encodedImage.equals("")) {
                            boolean success = imageUtils.saveBitmap(context, encodedImage, "Maps", ID);
                            if (!success) {
                                Log.v(classname, "Map image failed to save");
                            }
                        }
                    }
                }
                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                //dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date date = new Date();
                String datetime = dateFormat.format(date);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("modified(map)", datetime);
                editor.apply();
                updateAdapter(this.id);
            } catch (JSONException e) {
                Log.e(classname, e.toString());
            }
        }
    }

    private void updateAdapter(int id) {
        //Getting all data from table (local database) and update list of maps
        DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(id), "map_table");
        Cursor res = dbHelper.getAllData();
        mapsList.clear();
        if (res.getCount() == 0) {
            Log.v(classname, "Nothing to display");
        } else {
            while (res.moveToNext()) {
                int map_id = res.getInt(res.getColumnIndex("ID"));
                String name = res.getString(res.getColumnIndex("Name"));
                mapsList.add(name);
                mapsDict.put(name, map_id);
            }
            Collections.sort(mapsList, new Comparator<String>() {
                @Override
                public int compare(String c1, String c2) {
                    return c1.compareTo(c2);
                }
            });
        }
        res.close();
        dbHelper.db.close();

        ArrayAdapter<String> mapSpinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, mapsList);
        mapSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MapSpinner.setAdapter(mapSpinnerAdapter);
            }
        });
    }

    private boolean updateLocationSpinnerAdapter(int map_id) {
        locationsList.clear();
        String map_info = new FileUtils().loadJson(context, "Maps", Integer.toString(map_id));
        try {
            JSONObject collection = new JSONObject(map_info);
            JSONObject points = collection.getJSONObject("Points");
            int length = points.getInt("Length");
            for (int i = 0; i < length; i++) {
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
        int index, x, y;
        String name;
        Point(int index, String name) {
            this.index = index;
            this.name = name;
        }
        Point(int index, String name, int x, int y) {
            this.index = index;
            this.name = name;
            this.x = x;
            this.y = y;
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

    private Point findClosestPoint(int map_id) {
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
        }catch(JSONException e) {
            Log.e(classname, e.toString());
            return null;
        }
    }

    private Point findClosestPointToTouch(int map_id, int x, int y) {
        String map_info = new FileUtils().loadJson(context, "Maps", Integer.toString(map_id));
        try {
            JSONObject collection = new JSONObject(map_info);
            JSONObject points = collection.getJSONObject("Points");
            int length = points.getInt("Length");
            if (length == 0) {
                return null;
            }
            int minDist = 2500, dist;// set minDist to a suitable value
            int pointIndex = 0, min_x = 0, min_y = 0;
            String pointName = "";
            for (int i = 0; i < length; i++) {
                JSONObject point = points.getJSONObject(Integer.toString(i + 1));
                String name = point.getString("Name");
                int xx = Math.round(point.getInt("x") * scale);
                int yy = Math.round(point.getInt("y") * scale);
                dist = ((xx - x) * (xx - x)) + ((yy - y) * (yy - y));
                if (dist < minDist) {
                    minDist = dist;
                    pointIndex = i + 1;
                    pointName = name;
                    min_x = xx;
                    min_y = yy;
                }
            }
            if (pointIndex == 0) {
                return null;
            }
            return new Point(pointIndex, pointName, min_x, min_y);
        }catch(JSONException e) {
            Log.e(classname, e.toString());
            return null;
        }
    }
}