package com.example.talkwithme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.graphics.BitmapFactory.decodeFile;

public class Register2 extends Fragment {
    private String classname = getClass().getName();
    private Spinner GenderSpinner;
    private ImageView ProfileImageView;
    private boolean uploaded = false;
    private String encodedImage = "";
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.register2, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int state = -1;
        String mobile = "", password = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            mobile = bundle.getString("mobile", "");
            password = bundle.getString("password", "");
            state = bundle.getInt("state", -1);
        }
        final int fstate = state;
        final String fmobile = mobile, fpassword = password;

        ProfileImageView = view.findViewById(R.id.ProfileImageView);
        final Button UploadButton = view.findViewById(R.id.UploadButton);
        UploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkImagePermissions();
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, StartActivity.RESULT_REGISTER_LOAD_IMG);
            }
        });

        GenderSpinner = view.findViewById(R.id.GenderSpinner);
        //Assigning gender list (male, female) to the spinner
        //R.array.Gender can be found in res/values/strings.xml
        String[] GenderArray = getResources().getStringArray(R.array.Gender);
        List<String> list = Arrays.asList(GenderArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.gender_spinner_item, list);
        GenderSpinner.setAdapter(adapter);
        //Set default selection to male
        GenderSpinner.setSelection(0);
        GenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                //Change default gender pictures only when no image is already uploaded
                if(!uploaded){
                    String gender = GenderSpinner.getSelectedItem().toString();
                    Bitmap bitmap = new ImageUtils().getProfileBitmap(context, gender);
                    ProfileImageView.setImageBitmap(bitmap);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        final EditText NameEntry = view.findViewById(R.id.NameEntry);
        final Button SignupButton = view.findViewById(R.id.SignupButton);
        SignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = NameEntry.getText().toString();
                //Quick validation checks for name
                if(name.length() == 0){
                    NameEntry.requestFocus();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Please set a name for yourself", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                String gender = GenderSpinner.getSelectedItem().toString();
                //Gender, Status, State are represented by integers in the database, other fields are represented as strings
                String gender_enum = "0";
                if(gender.equals("Female")){
                    gender_enum = "1";
                }
                //May want to remove state & status
                RunnableTask task = new RegisterRunnableTask(context, StartActivity.host_dir + "register.php"
                        , new String[]{"mobile", "password", "name", "gender", "state", "image"}
                        , new String[]{fmobile, fpassword, name, gender_enum, Integer.toString(fstate), encodedImage});
                Thread thread = new Thread(task);
                thread.start();
            }
        });
        return view;
    }

    class RegisterRunnableTask extends RunnableTask{
        RegisterRunnableTask(Context context, String address, String[] categories, String[] args) {
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
            try {
                //Check for result/response from server/database
                JSONObject collection = new JSONObject(result);
                String response = collection.getString("Response");
                if (response.equals("Registration successful")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Registration is successful", Toast.LENGTH_SHORT).show();
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
                    StartActivity.current_id = id;
                    //Save image locally in cache only if it is uploaded (customisation)
                    //Default gender images are not uploaded and so will not be duplicated in the cache
                    //If default images are needed, retrieve straight from application contents
                    if (!encodedImage.equals("")) {
                        boolean success = new ImageUtils().saveBitmap(context, encodedImage, "Images", Integer.toString(id));
                        if (!success) {
                            Log.e(classname, "Image is not saved successfully");
                        }
                    }

                    String sstate = find("state");
                    if(sstate == null){
                        return;
                    }
                    String name = find("name");
                    if(name == null){
                        return;
                    }
                    String sgender = find("gender");
                    if(sgender == null){
                        return;
                    }
                    int state = Integer.parseInt(sstate);
                    Fragment fragment = new ChatMove();
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", id);
                    bundle.putString("name", name);
                    bundle.putInt("gender", Integer.parseInt(sgender));
                    bundle.putString("organisation", "");
                    bundle.putInt("status", 0);
                    bundle.putInt("state", state);
                    fragment.setArguments(bundle);
                    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment, fragment, "current");
                    ft.commit();
                } else {
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

    private void checkImagePermissions(){
        //REQUEST_CODE_REQUEST_READ_IMAGE is unique identifier for request (See under StartActivity activity class)
        //REQUEST_CODE_REQUEST_READ_IMAGE allows customized response to when permission is granted or not (see onRequestPermissionsResult)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, StartActivity.REQUEST_CODE_REQUEST_READ_IMAGE);
        }
    }

    /*
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == StartActivity.REQUEST_CODE_REQUEST_READ_IMAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.v(classname, "Read image permission granted");
            } else {
                Log.v(classname, "Read image permission rejected");
            }
        }
    }
    */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == StartActivity.RESULT_REGISTER_LOAD_IMG && resultCode == RESULT_OK && data != null) {
                Uri selectedImage = data.getData();
                //Check whether uri(universal resource identifier) is empty
                if (selectedImage != null) {
                    //Find path to local image
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = activity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor != null){
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String imagePath = cursor.getString(columnIndex);
                        cursor.close();
                        //Get image from path
                        Bitmap bitmap = decodeFile(imagePath);
                        if (bitmap == null) {
                            //No valid image selected or something went wrong, using default male and female images
                            bitmap = new ImageUtils().getProfileBitmap(context, GenderSpinner.getSelectedItem().toString());
                        }else{
                            //Scale down bitmap to 150 x 150 pixels so to reduce load on server
                            bitmap = new ImageUtils().getScaledBitmap(bitmap, 150, 150);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
                            byte[] byte_arr = stream.toByteArray();
                            encodedImage = Base64.encodeToString(byte_arr, 0);
                            //"uploaded" boolean to disable changing of default gender pictures when gender is switched
                            uploaded = true;
                        }
                        //Display the image
                        ProfileImageView.setImageBitmap(bitmap);
                    }else{
                        Log.e(classname, "Cursor is null");
                    }
                } else {
                    Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }
}

