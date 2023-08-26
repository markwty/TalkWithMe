package com.example.talkwithme;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class UpdateContactsRunnableTask extends RunnableTask {
    private String classname = getClass().getName();
    private String TABLE_NAME;
    private String[] COL_NAMES;
    private int id;
    UpdateContactsRunnableTask(Context context, String address, String[] categories, String[] args
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
    private void updateDatabase(){
        if(result.equals("")){
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(context, Integer.toString(id), TABLE_NAME, COL_NAMES);
        ImageUtils imageUtils = new ImageUtils();
        try {
            JSONObject collection = new JSONObject(result);
            String response = collection.getString("Response");
            if (response.equals("Success")){
                String removed = collection.getString("Deleted");
                String[] removed_arr = removed.split(",");
                for (String sopp_id : removed_arr) {
                    dbHelper.deleteData(sopp_id);
                }
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
                    }else {
                        boolean success = dbHelper.insertData(ID, values);
                        if (!success) {
                            Log.e(classname, "Failed to insert data " + printValues.toString());
                        }
                    }
                    String encodedImage = item.getString("Image");
                    if(!encodedImage.equals("")){
                        boolean success = imageUtils.saveBitmap(context, encodedImage, "Images", ID);
                        if(!success){
                            Log.v(classname, "Image failed to save");
                        }
                    }
                }

                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = new Date();
                String datetime = dateFormat.format(date);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("modified" + id, datetime);
                editor.apply();
            }
        }catch(JSONException e) {
            Log.e(classname, e.toString());
        }
    }
}