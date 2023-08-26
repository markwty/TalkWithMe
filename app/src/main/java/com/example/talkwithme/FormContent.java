package com.example.talkwithme;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.flipkart.android.proteus.Proteus;
import com.flipkart.android.proteus.ProteusBuilder;
import com.flipkart.android.proteus.ProteusContext;
import com.flipkart.android.proteus.ProteusLayoutInflater;
import com.flipkart.android.proteus.ProteusView;
import com.flipkart.android.proteus.gson.ProteusTypeAdapterFactory;
import com.flipkart.android.proteus.value.Layout;
import com.flipkart.android.proteus.value.ObjectValue;
import com.google.gson.stream.JsonReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class FormContent extends Fragment {
    private String classname = getClass().getName();
    private boolean success = false;
    private Context context;
    private FragmentActivity activity;
    //Default form templates
    private Map<Integer, Integer> formMap = new HashMap<Integer, Integer>(){
        {
            put(1,R.layout.form1);
            put(2,R.layout.form2);
            put(3,R.layout.form3);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.form1, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int form = 0;
        Bundle bundle = getArguments();
        if (bundle != null) {
            form = bundle.getInt("form", -1);
        }

        Integer mappedForm = formMap.get(form);
        if(mappedForm != null){
            view = inflater.inflate(mappedForm, container, false);
        }else{
            /*
            //Check whether exist
            FileUtils fileUtils = new FileUtils();
            boolean exist = fileUtils.checkJsonExist(context, Integer.toString(form));
            */
            RunnableTask task = new DownloadLayoutRunnableTask(context, StartActivity.host_dir + "download_form_layout.php"
                    , new String[]{"form"}, new String[]{Integer.toString(form)});
            Thread thread = new Thread(task);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Log.e(classname, e.toString());
            }
            if(success){
                Proteus proteus = new ProteusBuilder().build();
                ProteusTypeAdapterFactory adapter = new ProteusTypeAdapterFactory(context);
                ProteusTypeAdapterFactory.PROTEUS_INSTANCE_HOLDER.setProteus(proteus);
                Layout layout = null;
                ObjectValue data = null;
                try {
                    layout = adapter.LAYOUT_TYPE_ADAPTER.read(new JsonReader(new StringReader(new FileUtils().loadJson(context, "Json", Integer.toString(form)))));
                    data = adapter.OBJECT_TYPE_ADAPTER.read(new JsonReader(new StringReader("{}")));
                } catch (IOException e) {
                    Log.e(classname, e.toString());
                }
                ProteusContext pcontext = proteus.createContextBuilder(context).build();
                ProteusLayoutInflater pinflater = pcontext.getInflater();
                ProteusView pview = null;
                if(layout != null && data != null){
                    pview = pinflater.inflate(layout, data, container, 0);
                }
                if(pview != null){
                    return pview.getAsView();
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Something went wrong...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    activity.onBackPressed();
                }
            }else{
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Something went wrong...", Toast.LENGTH_SHORT).show();
                    }
                });
                activity.onBackPressed();
            }
        }
        return view;
    }

    class DownloadLayoutRunnableTask extends RunnableTask{
        DownloadLayoutRunnableTask(Context context, String address, String[] categories, String[] args) {
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
                if (response.equals("Success")) {
                    String form = find("form");
                    if(form == null){
                        return;
                    }
                    success = new FileUtils().saveJson(context, collection.getString("Json"), "Forms", form);
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Usually "Failed to process request"
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }
}

