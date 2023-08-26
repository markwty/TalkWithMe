package com.example.talkwithme;

import android.content.Context;
import android.util.Log;

import java.io.File;

@SuppressWarnings("unused")
class LoggingUtils {
    private String classname = getClass().getName();
    private Context context;

    LoggingUtils(Context context){
        this.context = context;
    }

    private void retrieveData(File dir){
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    Log.v(dir.getName(), child);
                    retrieveData(new File(dir, child));
                }
            }
        }
    }

    void getAllFiles(){
        Log.v(classname, context.getCacheDir().toString());
        retrieveData(context.getCacheDir());
    }
}
