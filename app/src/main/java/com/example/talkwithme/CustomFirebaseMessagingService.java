package com.example.talkwithme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomFirebaseMessagingService extends FirebaseMessagingService {
    private String classname = getClass().getName();
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            try {
                JSONObject json = new JSONObject(remoteMessage.getData().toString());
                sendPushNotification(json);
            } catch (Exception e) {
                Log.e(classname, e.toString());
            }
        }
    }

    private void sendPushNotification(JSONObject json) {
        try {
            JSONObject data = json.getJSONObject("data");
            String action = data.getString("Action");
            if(action.equals("INCOMING")){
                int id = data.getInt("ID");
                int opp_id = data.getInt("OppID");
                String opp_name = data.getString("OppName");
                String name = data.getString("Name");

                Intent notificationIntent = new Intent(getApplicationContext(), NotificationReceiver.class);
                notificationIntent.setAction(NotificationReceiver.INCOMING_CALL);
                notificationIntent.putExtra(NotificationReceiver.NOTIFICATION_ID, 1);
                notificationIntent.putExtra("ID", id);
                notificationIntent.putExtra("OppID", opp_id);
                notificationIntent.putExtra("OppName", opp_name);
                notificationIntent.putExtra("Name", name);
                sendBroadcast(notificationIntent);
            }else if(action.equals("CANCELED")){
                Intent notificationIntent = new Intent(getApplicationContext(), NotificationReceiver.class);
                notificationIntent.setAction(NotificationReceiver.CANCEL_CALL);
                notificationIntent.putExtra(NotificationReceiver.NOTIFICATION_ID, 1);
                sendBroadcast(notificationIntent);
            }
        } catch (JSONException e) {
            Log.e(classname, e.toString());
        }
    }

    @Override
    public void onNewToken(String token) {
        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("token", token);
        editor.apply();
    }
}
