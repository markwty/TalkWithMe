package com.example.talkwithme;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class RunnableTask implements Runnable {
    private String classname = getClass().getName();
    protected Context context;
    private String address;
    String result = "";
    private String[] categories;
    private String[] args;

    RunnableTask(Context context, String address, String[] categories, String[] args) {
        this.context = context;
        this.address = address;
        //categories and args should match correspondingly
        this.categories = categories;
        this.args = args;
    }

    public void run() {
        int length = categories.length;
        if(length != args.length){
            Log.e(classname, "Length of categories is not equal to length of args");
            result = "";
            return;
        }
        try {
            URL url = new URL(address);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setConnectTimeout(2000);
            httpURLConnection.setReadTimeout(4000);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            //"UTF-8"
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            StringBuilder data_string = new StringBuilder();

            JSONObject json = new JSONObject();
            for(int i = 0; i < categories.length; i++){
                json.put(categories[i], args[i]);
            }

            String encryptedPackage;
            encryptedPackage = ClientEncrypt.getInstance().encrypt(json.toString());
            data_string.append(URLEncoder.encode("data", "UTF-8")).append("=").append(URLEncoder.encode(encryptedPackage, "UTF-8"));

            bufferedWriter.write(data_string.toString());
            bufferedWriter.flush();
            outputStream.close();
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = reader.readLine())!= null){
                sb.append(line);
            }
            result = sb.toString();
            result = ClientEncrypt.getInstance().decrypt(result);
            inputStream.close();
            httpURLConnection.disconnect();
        } catch(IOException | JSONException e){
            Log.e(classname, e.toString());
            result = "";
        }
    }

    String find(String field){
        for(int i = 0; i < categories.length; i++){
            if(categories[i].equals(field)){
                if(args.length > i){
                    return args[i];
                }else{
                    return null;
                }
            }
        }
        return null;
    }
}