package com.example.talkwithme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import static com.example.talkwithme.ConnectTogether.CHANNEL_1_ID;

public class NotificationReceiver extends BroadcastReceiver {
    private String classname = getClass().getName();
    public static String NOTIFICATION_ID = "notification-id";
    public static String INCOMING_CALL = "ACTION_NOTIFY_INCOMING_CALL";
    public static String CALL_DISMISSED = "ACTION_NOTIFY_CALL_DISMISSED";
    public static String CANCEL_CALL = "ACTION_NOTIFY_CANCEL_CALL";
    private int channel_id;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action == null){
            return;
        }
        Bundle bundle = intent.getExtras();
        if(action.equals(INCOMING_CALL)){
            if (bundle != null) {
                channel_id = intent.getExtras().getInt(NOTIFICATION_ID, 0);
                int id = intent.getExtras().getInt("ID");
                int opp_id = intent.getExtras().getInt("OppID");
                String opp_name = intent.getExtras().getString("OppName");
                String name = intent.getExtras().getString("Name");
                showNotificationIncoming(context, "Incoming call from " + opp_name + " for " + name, id, opp_id);
            }
        }else if(action.equals(CALL_DISMISSED)){
            if (bundle != null) {
                int id = intent.getExtras().getInt("ID");
                int opp_id = intent.getExtras().getInt("OppID");
                RunnableTask task = new DismissCallRunnableTask(context, StartActivity.host_dir + "dismiss_call.php"
                        , new String[]{"id", "opp_id"}
                        , new String[]{Integer.toString(id), Integer.toString(opp_id)});
                Thread thread = new Thread(task);
                thread.start();
            }
        }else if(action.equals(CANCEL_CALL)){
            if (bundle != null) {
                channel_id = intent.getExtras().getInt(NOTIFICATION_ID, 0);
                showNotificationCancel(context);
            }
        }
    }

    void showNotificationIncoming(Context context, String message, int id, int opp_id) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent activityIntent = new Intent(context, StartActivity.class);
        activityIntent.setAction(INCOMING_CALL);
        Bundle bundle = new Bundle();
        bundle.putInt("id", id);
        activityIntent.putExtras(bundle);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent deleteNotificationIntent = new Intent(context, NotificationReceiver.class);
        deleteNotificationIntent.setAction(CALL_DISMISSED);
        bundle = new Bundle();
        bundle.putInt("ID", id);
        bundle.putInt("OppID", opp_id);
        deleteNotificationIntent.putExtras(bundle);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 1, deleteNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.call_end)
                .setContentTitle("Connect Together")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(ContextCompat.getColor(context, R.color.blue))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        if(notificationManager != null){
            notificationManager.notify(channel_id, notification);
        }
    }

    void showNotificationCancel(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent activityIntent = new Intent(context, StartActivity.class);
        activityIntent.setAction(CANCEL_CALL);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 2, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.call_end)
                .setContentTitle("Connect Together")
                .setContentText("Your call is dismissed")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(ContextCompat.getColor(context, R.color.blue))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        if(notificationManager != null){
            notificationManager.notify(channel_id, notification);
        }
    }

    class DismissCallRunnableTask extends RunnableTask{
        DismissCallRunnableTask(Context context, String address, String[] categories, String[] args) {
            super(context, address, categories, args);
        }
        public void run() {
            super.run();
            if(result.equals("")){
                return;
            }
            try{
                //Check for result/response from server/database
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                Log.v(classname, response);
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }
}
