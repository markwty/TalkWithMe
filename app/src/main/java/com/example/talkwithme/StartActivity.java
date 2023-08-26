package com.example.talkwithme;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

public class StartActivity extends AppCompatActivity {
    //Suppressed warnings
    //Anonymous type can be replaced by lambda
    //Anonymous type can be replaced by method reference
    private String classname = getClass().getName();
    private Context context;
    final static String host_dir = "https://markelili.000webhostapp.com/Connect Together/";
    static int current_id = 0;

    static final int RESULT_REGISTER_LOAD_IMG = 101;
    static final int REQUEST_CODE_REQUEST_READ_IMAGE = 102;
    static final int REQUEST_CODE_REQUEST_CAMERA = 103;
    static final int REQUEST_CODE_REQUEST_AUDIO = 104;
    static final int REQUEST_CODE_REQUEST_ACCESS_LOCATION = 105;
    static final int REQUEST_CODE_REQUEST_ACCESS_WIFI_STATE = 106;
    static final int REQUEST_CODE_REQUEST_ACCESS_COARSE_LOCATION = 107;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        //Sets the theme under res/values/styles, transition from SplashScreenTheme(the splash screen itself) to AppTheme
        setTheme(R.style.AppTheme);
        setContentView(R.layout.start_activity);
        //ClientEncrypt clientEncrypt = ClientEncrypt.getInstance();
        //clientEncrypt.generateRSAKeyPair();
        //new LoggingUtils(getApplicationContext()).getAllFiles();

        String action = getIntent().getAction();
        Bundle bundle = getIntent().getExtras();
        if(action != null){
            if(action.equals(NotificationReceiver.INCOMING_CALL)){
                int id = -1;
                if (bundle != null) {
                    id = bundle.getInt("id", -1);
                }
                if(id != -1){
                    Fragment fragment = new Receiving();
                    bundle.putBoolean("temp", true);
                    fragment.setArguments(bundle);
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    ft.commit();
                    return;
                }
            }
        }

        //The bar on top of the android screen
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            //Main logo on action bar
            /*
            Drawable drawable = new ImageUtils().resizeIcon(getApplicationContext(), R.drawable.call, 30, 30);
            if(drawable != null){
                actionBar.setIcon(drawable);
            }
             */
            //Whether to show the main logo on action bar
            actionBar.setDisplayShowHomeEnabled(true);
            //Back logo on the left of action bar
            //actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //A project-wide map that allows communication between fragments and activities
        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        if (prefs.getBoolean("Keep me signed in", false)) {
            //Run on another thread in the background to prevent overloading on main thread (ensures smoothness of app)
            RunnableTask task = new LoginRunnableTask(context, StartActivity.host_dir + "verify.php"
                    , new String[]{"mobile", "password"}
                    , new String[]{prefs.getString("mobile", ""), prefs.getString("password", "")});
            Thread thread = new Thread(task);
            thread.start();
        }
        Fragment fragment = new LoginSignup();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment, fragment, "current");
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Showing the menu
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
       //What happens when menu options are being clicked
       if (item.getItemId() == R.id.LogOut) {
           Fragment fragment = getSupportFragmentManager().findFragmentByTag("current");
           if(fragment instanceof LoginSignup){
               Toast.makeText(context, "You are already logged out", Toast.LENGTH_SHORT).show();
               return true;
           }
           SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
           SharedPreferences.Editor editor = prefs.edit();
           editor.putBoolean("Keep me signed in", false);
           editor.apply();

           Toast.makeText(context, "Logging out...", Toast.LENGTH_SHORT).show();
           FragmentManager fm = getSupportFragmentManager();
           int backStackCount = fm.getBackStackEntryCount();
           for(int i = 0; i < backStackCount; i++){
               fm.popBackStack();
           }
           fragment = new LoginSignup();
           FragmentTransaction ft = fm.beginTransaction();
           ft.replace(R.id.fragment, fragment, "current");
           ft.commit();
           return true;
       }else{
           return super.onOptionsItemSelected(item);
       }
    }

    @Override
    public void onBackPressed(){
        FragmentManager fm = getSupportFragmentManager();
        if(fm.getBackStackEntryCount() > 0) {
            //Go back to previous fragments (checkpoints) that are "added to back stack"
            Fragment fragment = fm.findFragmentByTag("current");
            if (fragment != null){
                //Special case to recreate the "Home" fragment after changing particulars
                if(fragment instanceof Contacts){
                    Fragment subFragment = ((Contacts) fragment).collectionAdapter.fragments[0];
                    if(subFragment != null){
                        Bundle bundle = ((Particulars) subFragment).bundleToHome;
                        if(bundle != null){
                            //Most current fragment is labelled with "current", removing destroys the fragment
                            //This prevents overlapping of fragments (many fragments in view) and wastage of resources
                            fm.beginTransaction().remove(fragment).commit();
                            fm.popBackStack();
                            int state = bundle.getInt("state", -1);
                            if(state == 1){
                                Fragment newFragment = new ContactsVolunteer();
                                newFragment.setArguments(bundle);
                                FragmentTransaction ft = fm.beginTransaction();
                                ft.replace(R.id.fragment, newFragment, "current");
                                ft.commit();
                            }else{
                                Fragment newFragment = new ContactsConnectMake();
                                newFragment.setArguments(bundle);
                                FragmentTransaction ft = fm.beginTransaction();
                                ft.replace(R.id.fragment, newFragment, "current");
                                ft.commit();
                            }
                            return;
                        }
                    }
                }else if(fragment instanceof PendingRequest){
                    Bundle bundle = ((PendingRequest) fragment).bundleToHome;
                    if(bundle != null){
                        //Most current fragment is labelled with "current", removing destroys the fragment
                        //This prevents overlapping of fragments (many fragments in view) and wastage of resources
                        fm.beginTransaction().remove(fragment).commit();
                        fm.popBackStack();

                        int state = bundle.getInt("state", -1);
                        if(state == 1){
                            Fragment newFragment = new ContactsVolunteer();
                            newFragment.setArguments(bundle);
                            FragmentTransaction ft = fm.beginTransaction();
                            ft.replace(R.id.fragment, newFragment, "current");
                            ft.commit();
                        }else{
                            Fragment newFragment = new ContactsConnectMake();
                            newFragment.setArguments(bundle);
                            FragmentTransaction ft = fm.beginTransaction();
                            ft.replace(R.id.fragment, newFragment, "current");
                            ft.commit();
                        }
                        return;
                    }
                }else if(fragment instanceof RequestVerified){
                    ((TransitionFragment) fragment).stop = true;
                    Bundle bundle = ((RequestVerified) fragment).bundleToHome;
                    if(bundle != null){
                        //Most current fragment is labelled with "current", removing destroys the fragment
                        //This prevents overlapping of fragments (many fragments in view) and wastage of resources
                        fm.beginTransaction().remove(fragment).commit();
                        fm.popBackStack();

                        int state = bundle.getInt("state", -1);
                        if(state == 1){
                            Fragment newFragment = new ContactsVolunteer();
                            newFragment.setArguments(bundle);
                            FragmentTransaction ft = fm.beginTransaction();
                            ft.replace(R.id.fragment, newFragment, "current");
                            ft.commit();
                        }else{
                            Fragment newFragment = new ContactsConnectMake();
                            newFragment.setArguments(bundle);
                            FragmentTransaction ft = fm.beginTransaction();
                            ft.replace(R.id.fragment, newFragment, "current");
                            ft.commit();
                        }
                        return;
                    }
                }else if(fragment instanceof TransitionFragment){
                    ((TransitionFragment) fragment).stop = true;
                }

                //Most current fragment is labelled with "current", removing destroys the fragment
                //This prevents overlapping of fragments (many fragments in view) and wastage of resources
                fm.beginTransaction().remove(fragment).commit();
            }
            fm.popBackStack();
        }else{
            //Nothing on back stack, pressing back goes out of app
            super.onBackPressed();
        }
    }

    @Override
    public void onResume(){
        //Called whenever activity becomes visible
        //https://stackoverflow.com/questions/12203651/why-is-onresume-called-when-an-activity-starts
        super.onResume();
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag("current");
        if (fragment != null) {
            //After call activity is finished
            if (fragment instanceof CallSearchFragment) {
                Bundle bundle = ((CallSearchFragment) fragment).bundleOnResume;
                if(bundle != null){
                    fragment = new CallHasEnded();
                    fragment.setArguments(bundle);
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    ft.commit();
                }
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent){
        if(intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if(action.equals(NotificationReceiver.INCOMING_CALL)) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        int id = bundle.getInt("id", -1);
                        boolean temp = id != current_id;
                        if(id != -1){
                            Fragment fragment = new Receiving();
                            bundle.putBoolean("temp", temp);
                            fragment.setArguments(bundle);
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.replace(R.id.fragment, fragment, "current");
                            if(!temp){
                                ft.addToBackStack("Call");
                            }
                            ft.commit();
                        }
                    }
                }else if (action.equals(NotificationReceiver.CANCEL_CALL)) {
                    FragmentManager fm = getSupportFragmentManager();
                    Fragment fragment = fm.findFragmentByTag("current");
                    if (fragment instanceof Sending) {
                        onBackPressed();
                    }
                }
            }
        }
    }

    class LoginRunnableTask extends RunnableTask {
        LoginRunnableTask(Context context, String address, String[] categories, String[] args) {
            super(context, address, categories, args);
        }

        public void run() {
            super.run();
            if(result.equals("")){
                runOnUiThread(new Runnable() {
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
                if (response.equals("Login successful")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Informs the user that verification succeeds
                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show();
                        }
                    });

                    SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                    SharedPreferences.Editor editor = prefs.edit();
                    String mobile = find("mobile");
                    if(mobile == null){
                        return;
                    }
                    String password = find("password");
                    if(password == null){
                        return;
                    }
                    editor.putString("mobile", mobile);
                    editor.putString("password", password);
                    editor.apply();

                    int id = collection.getInt("ID");
                    current_id = id;
                    String name = collection.getString("Name");
                    int gender = collection.getInt("Gender");
                    String organisation = collection.getString("Organisation");
                    int status = collection.getInt("Status");
                    int state = collection.getInt("State");

                    Fragment fragment = new ChatMove();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", id);
                    bundle.putString("name", name);
                    bundle.putInt("gender", gender);
                    bundle.putString("organisation", organisation);
                    bundle.putInt("status", status);
                    bundle.putInt("state", state);
                    fragment.setArguments(bundle);
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    ft.commit();
                } else {
                    runOnUiThread(new Runnable() {
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