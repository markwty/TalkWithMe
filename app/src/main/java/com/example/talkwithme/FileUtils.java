package com.example.talkwithme;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

class FileUtils {
    private String classname = getClass().getName();

    FileUtils(){}

    boolean checkJsonExist(Context context, String form){
        File file = new File(context.getCacheDir() + "/Json/", form + ".json");
        return file.exists();
    }

    boolean saveJson(Context context, String json, String directory, String id){
        File file = new File(context.getCacheDir() + "/" + directory + "/", id + ".json");
        try{
            if(!file.exists()){
                dirChecker(context.getCacheDir() + "/" + directory + "/");
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.getBytes());
            fos.close();
        }catch(IOException e){
            Log.e(classname, e.toString());
            return false;
        }
        return true;
    }

    private void dirChecker(String dir) {
        File f = new File(dir);
        if(!f.isDirectory()) {
            boolean success = f.mkdirs();
            if(!success) {
                Log.e(classname, "Failed to create " + dir);
            }
        }
    }

    String loadJson(Context context, String directory, String id){
        File file = new File(context.getCacheDir() + "/" + directory + "/", id + ".json");
        if(!file.exists()){
            return null;
        }
        try {
            FileInputStream fin = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            fin.close();
            return sb.toString();
        }catch(IOException e){
            Log.e(classname, e.toString());
            return "";
        }
    }
}
