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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;
import static android.graphics.BitmapFactory.decodeFile;

public class Particulars extends Fragment {
    private String classname = getClass().getName();
    Bundle bundleToHome = null;
    private String password, mobile;
    private int id = -1, status = -1;
    private String name = "", organisation = "";
    private boolean through = false, uploaded = false;
    private String encodedImage = "";
    private ConstraintLayout DetailsConstraintLayout;
    private LinearLayout VerifyLinearLayout;
    private ImageView ProfileImageView;
    private Button UploadButton, MakeEditsButton;
    private Spinner GenderSpinner;
    private EditText NameEntry, MobileEntry, PasswordEntry, EnterVerificationCodeEntry;
    private CheckBox StayInCondoCheckBox;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private Context context;
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.particulars, container, false);
        context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int gender = -1, state = -1;
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);//
            name = bundle.getString("name", "");
            gender = bundle.getInt("gender", -1);//
            organisation = bundle.getString("organisation", "");
            status = bundle.getInt("status", -1);
            state = bundle.getInt("state", -1);
        }
        final int fstate = state;

        SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
        ProfileImageView = view.findViewById(R.id.ProfileImageView);
        String genderString = "Male";
        if(gender == 1){
            genderString = "Female";
        }
        final String fgenderString = genderString;
        final String fmobile = prefs.getString("mobile", "");
        final String fpassword = prefs.getString("password", "");

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageUtils imageUtils = new ImageUtils();
                if (status == 2) {
                    imageUtils.setImage(context, ProfileImageView, Integer.toString(id), "Organisation", ImageUtils.defaultBackground);
                } else {
                    imageUtils.setImage(context, ProfileImageView, Integer.toString(id), fgenderString, ImageUtils.defaultBackground);
                    //uploaded is false if default image is used and vice versa
                    uploaded = imageUtils.uploaded;
                }
            }
        });

        RunnableTask task = new GetProfilePictureRunnableTask(context, StartActivity.host_dir + "get_profile_picture.php"
                , new String[]{"mobile", "password"}, new String[]{fmobile, fpassword}, fgenderString);
        Thread thread = new Thread(task);
        thread.start();

        UploadButton = view.findViewById(R.id.UploadButton);
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
        GenderSpinner.setEnabled(false);
        //Starts off disabled
        String[] GenderArray = getResources().getStringArray(R.array.Gender);
        List<String> list = Arrays.asList(GenderArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.gender_spinner_item, list);
        GenderSpinner.setAdapter(adapter);
        //Set default selection to male
        if(!GenderSpinner.getSelectedItem().toString().equals(genderString)){
            //setSelection takes an index, can only use this logic because there are only 2 options
            GenderSpinner.setSelection(1 - GenderSpinner.getSelectedItemPosition());
        }
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

        NameEntry = view.findViewById(R.id.NameEntry);
        MobileEntry = view.findViewById(R.id.MobileEntry);
        PasswordEntry = view.findViewById(R.id.PasswordEntry);
        final TextView OrganisationLabel = view.findViewById(R.id.OrganisationLabel);
        final EditText OrganisationEntry = view.findViewById(R.id.OrganisationEntry);
        StayInCondoCheckBox = view.findViewById(R.id.StayInCondoCheckBox);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                NameEntry.setText(name);
                SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                MobileEntry.setText(prefs.getString("mobile", ""));
                //If there is no organisation (elderly), remove the organisation field from display
                if (status == 1) {
                    OrganisationEntry.setText(organisation);
                } else {
                    OrganisationLabel.setVisibility(View.GONE);
                    OrganisationEntry.setVisibility(View.GONE);
                }
                if (fstate == 0) {
                    StayInCondoCheckBox.setChecked(true);
                } else {
                    StayInCondoCheckBox.setChecked(false);
                }
                if(status == 2){
                    TextView GenderLabel = view.findViewById(R.id.GenderLabel);
                    TextView StayInCondoLabel = view.findViewById(R.id.StayInCondoLabel);
                    GenderLabel.setVisibility(View.GONE);
                    GenderSpinner.setVisibility(View.GONE);
                    StayInCondoLabel.setVisibility(View.GONE);
                    StayInCondoCheckBox.setVisibility(View.GONE);
                }
            }
        });

        DetailsConstraintLayout = view.findViewById(R.id.DetailsConstraintLayout);
        VerifyLinearLayout = view.findViewById(R.id.VerifyLinearLayout);
        EnterVerificationCodeEntry = view.findViewById(R.id.EnterVerificationCodeEntry);
        MakeEditsButton = view.findViewById(R.id.MakeEditsButton);

        //All widgets in DetailsConstraintLayout starts off disabled
        //This prevents user from making accidental changes
        //Edits can only be made if MakeEditsButton is pressed
        MakeEditsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Changes text in button from "Make edits" to "Save" and vice versa
                if(MakeEditsButton.getText().toString().toLowerCase().equals("make edits")){
                    setClickable(true);
                    MakeEditsButton.setText(R.string.Save);
                }else if(MakeEditsButton.getText().toString().toLowerCase().equals("save")){
                    name = NameEntry.getText().toString();

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

                    mobile = MobileEntry.getText().toString();
                    password = PasswordEntry.getText().toString();
                    //Quick validation checks for mobile number and password
                    if(mobile.length() != 8){
                        MobileEntry.requestFocus();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Mobile number is invalid", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }else if(password.equals("")){
                        PasswordEntry.requestFocus();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Password should not be empty", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            through = false;
                            RunnableTask task = new CheckMobileRunnableTask(context, StartActivity.host_dir + "check_mobile_in_use_exclude_id.php"
                                    , new String[]{"mobile", "id"}, new String[]{mobile, Integer.toString(id)});
                            Thread thread = new Thread(task);
                            thread.start();
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                Log.e(classname, e.toString());
                            }
                            if (through) {
                                //Phone authentication
                                //5 SMS per phone number per 4 hours
                                mAuth = FirebaseAuth.getInstance();
                                sendVerificationCode(mobile);

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DetailsConstraintLayout.setVisibility(View.GONE);
                                        VerifyLinearLayout.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                    });
                    thread.start();
                }
            }
        });

        final Button VerifyButton = view.findViewById(R.id.VerifyButton);
        VerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Manual verification by typing the verification code in "EnterVerificationCodeEntry" and clicking the "VerifyButton"
                signInWithCredential(EnterVerificationCodeEntry.getText().toString());
            }
        });

        final Button BackButton = view.findViewById(R.id.BackButton);
        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       DetailsConstraintLayout.setVisibility(View.VISIBLE);
                       VerifyLinearLayout.setVisibility(View.GONE);
                       MakeEditsButton.setText(R.string.MakeEdits);
                   }
                });
                setClickable(false);
            }
        });
        return view;
    }

    private void setClickable(boolean clickable){
        //List of widgets enabled/disabled
        DetailsConstraintLayout.setClickable(clickable);
        UploadButton.setEnabled(clickable);
        if(status != 2){
            NameEntry.setEnabled(clickable);
            MobileEntry.setEnabled(clickable);
            GenderSpinner.setEnabled(clickable);
            StayInCondoCheckBox.setEnabled(clickable);
        }
        PasswordEntry.setEnabled(clickable);
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

    class GetProfilePictureRunnableTask extends RunnableTask{
        private String genderString;
        GetProfilePictureRunnableTask(Context context, String address, String[] categories, String[] args, String genderString) {
            super(context, address, categories, args);
            this.genderString = genderString;
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
                ImageUtils imageUtils = new ImageUtils();
                if (response.equals("Success")) {
                    String encodedImage = collection.getString("Image");
                    if(!encodedImage.equals("")){
                        boolean success = imageUtils.saveBitmap(context, encodedImage, "Images", Integer.toString(id));
                        if(!success){
                            Log.v(classname, "Image failed to save");
                        }
                    }
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (status == 2) {
                            imageUtils.setImage(context, ProfileImageView, Integer.toString(id), "Organisation", ImageUtils.defaultBackground);
                        } else {
                            imageUtils.setImage(context, ProfileImageView, Integer.toString(id), genderString, ImageUtils.defaultBackground);
                            //uploaded is false if default image is used and vice versa
                            uploaded = imageUtils.uploaded;
                        }
                    }
                });
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }

    class CheckMobileRunnableTask extends RunnableTask{
        CheckMobileRunnableTask(Context context, String address, String[] categories, String[] args) {
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
                if (response.equals("Mobile is not in use")) {
                    through = true;
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }catch(JSONException e){
                Log.e(classname, e.toString());
            }
        }
    }

    class ChangeDetailsRunnableTask extends RunnableTask{
        ChangeDetailsRunnableTask(Context context, String address, String[] categories, String[] args) {
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
                    through = true;
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

                    String sid = find("id");
                    if(sid == null){
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
                    String sstate = find("state");
                    if(sstate == null){
                        return;
                    }
                    if(status != 2) {
                        bundleToHome = new Bundle();
                        bundleToHome.putInt("id", Integer.parseInt(sid));
                        bundleToHome.putString("name", name);
                        bundleToHome.putInt("gender", Integer.parseInt(sgender));
                        bundleToHome.putString("organisation", organisation);
                        bundleToHome.putInt("status", status);
                        bundleToHome.putInt("state", Integer.parseInt(sstate));
                    }
                    if (!encodedImage.equals("")) {
                        boolean success = new ImageUtils().saveBitmap(context, encodedImage, "Images", sid);
                        if (!success) {
                            Log.e(classname, "Image is not saved successfully");
                        }
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DetailsConstraintLayout.setVisibility(View.VISIBLE);
                            VerifyLinearLayout.setVisibility(View.GONE);
                            MakeEditsButton.setText(R.string.MakeEdits);
                            setClickable(false);
                        }
                    });
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

    private void sendVerificationCode(String mobile) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+65" + mobile,
                60,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                mCallbacks);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            String code = phoneAuthCredential.getSmsCode();
            DetailsConstraintLayout.setVisibility(View.GONE);
            VerifyLinearLayout.setVisibility(View.VISIBLE);
            if (code != null) {
                EnterVerificationCodeEntry.setText(code);
                signInWithCredential(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            mVerificationId = s;
        }
    };

    private void signInWithCredential(String code){
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        mAuth.signInWithCredential(credential).addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Verification success", Toast.LENGTH_SHORT).show();
                        }
                    });

                    String gender = GenderSpinner.getSelectedItem().toString();
                    String gender_enum = "0";
                    if(gender.equals("Female")){
                        gender_enum = "1";
                    }

                    String state_enum = "1";
                    if(StayInCondoCheckBox.isChecked()){
                        state_enum = "0";
                    }

                    SharedPreferences prefs = SharedPreferencesSingleton.getPrefsInstance(context);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date date = new Date();
                    String datetime = dateFormat.format(date);
                    RunnableTask rtask = new ChangeDetailsRunnableTask(context, StartActivity.host_dir + "change_particulars.php"
                            , new String[]{"id", "mobile", "password", "name", "gender", "modified", "state", "old_password", "image"}
                            , new String[]{Integer.toString(id), mobile, password, name, gender_enum, datetime, state_enum,
                            prefs.getString("password", ""), encodedImage});
                    Thread thread = new Thread(rtask);
                    thread.start();
                } else {
                    String message = "Something is wrong";
                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        message = "Invalid code entered...";
                    }
                    final String fmessage = message;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, fmessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == StartActivity.RESULT_REGISTER_LOAD_IMG && resultCode == RESULT_OK && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = activity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor != null){
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String imagePath = cursor.getString(columnIndex);
                        cursor.close();
                        Bitmap bitmap = decodeFile(imagePath);
                        if (bitmap == null) {
                            //No valid image selected or something went wrong, using default male and female images
                            bitmap = new ImageUtils().getProfileBitmap(context, GenderSpinner.getSelectedItem().toString());
                        }else{
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


