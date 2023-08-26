package com.example.talkwithme;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class ImageUtils {
    static int defaultBackground = 0, defaultIcon = 1;
    private String classname = getClass().getName();
    private String [] extensions = new String[]{".jfif", ".jpg", ".jpeg", ".png", ".webp", ".9.png"};
    boolean uploaded = false;

    ImageUtils(){}
    private Bitmap getBitmap(String directory, String filename) {
        File f;
        if (!filename.contains(".")){
            for(String extension:extensions){
                f = new File(directory, filename + extension);
                if(f.exists()) {
                    filename += extension;
                    break;
                }
            }
        }
        f = new File(directory, filename);
        if(!f.exists()) {
            return null;
        }
        return decodeWithoutSampling(f);
    }

    private Bitmap decodeWithoutSampling(File f) {
        try {
            FileInputStream in = new FileInputStream(f);
            Bitmap b = BitmapFactory.decodeStream(in);
            in.close();
            return b;
        } catch (Exception e) {
            Log.e(classname, e.toString());
        }
        return null;
    }

    private Bitmap getBitmapFromLocalResource(Context context, String identifier, String directory){
        Bitmap bitmap;
        bitmap = this.getBitmap(context.getCacheDir() + "/" + directory + "/", identifier);
        uploaded = bitmap != null;
        return bitmap;
    }

    private Bitmap getBitmapFromLocalResource(Context context, String identifier, String directory, String gender){
        Bitmap bitmap;
        bitmap = this.getBitmap(context.getCacheDir() + "/" + directory + "/", identifier);
        if (bitmap == null) {
            bitmap = getProfileBitmap(context, gender);
            uploaded = false;
        } else {
            uploaded = true;
        }
        return bitmap;
    }

    void setImage(Context context, ImageView imageView, String identifier, String gender, int default_mode) {
        Bitmap bitmap = getBitmapFromLocalResource(context, identifier, "Images", gender);
        if (bitmap == null) {
            if(default_mode == defaultBackground) {
                imageView.setImageResource(R.color.white);
            }else{
                imageView.setImageResource(android.R.color.transparent);
            }
        }else {
            imageView.setImageBitmap(bitmap);
        }
    }

    void setImage(Activity activity, ImageView imageView, String encodedImage, int default_mode) {
        if(!encodedImage.equals("")){
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            int width = imageView.getWidth();
            if(width == 0){
                DisplayMetrics displayMetrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                width = displayMetrics.widthPixels;
            }
            Bitmap scaledBitmap = getScaledBitmap(bitmap, width);
            imageView.getLayoutParams().height = scaledBitmap.getHeight();
            imageView.requestLayout();
            imageView.setImageBitmap(scaledBitmap);
        }else{
            if (default_mode == defaultBackground) {
                imageView.setImageResource(R.color.white);
            } else {
                imageView.setImageResource(android.R.color.transparent);
            }
        }
    }

    class Dimension{
        int width, height;
        Dimension(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
    Dimension setImage(Activity activity, Context context, ImageView imageView, String identifier, int default_mode) {
        Bitmap bitmap = getBitmapFromLocalResource(context, identifier, "Maps");
        if (bitmap != null) {
            Bitmap.Config bitmapConfig = bitmap.getConfig();
            if(bitmapConfig == null) {
                bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
            }
            bitmap = bitmap.copy(bitmapConfig, true);

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(20);
            paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
            String[] KELLY_COLOURS = {"#FFB300", "#803E75", "#FF6800", "#A6BDD7", "#C10020", "#CEA262", "#817066",
                    "#007D34", "#F6768E", "#00538A", "#FF7A5C", "#53377A", "#FF8E00", "#B32851", "#F4C800", "#7F180D",
                    "#93AA00", "#593315", "#F13A13", "#232C16"};
            String map_info = new FileUtils().loadJson(context, "Maps", identifier);
            try {
                JSONObject collection = new JSONObject(map_info);
                JSONObject points = collection.getJSONObject("Points");
                int length = points.getInt("Length");
                for (int i = 0; i < length; i++) {
                    JSONObject point = points.getJSONObject(Integer.toString(i + 1));
                    String name = point.getString("Name");
                    int x = point.getInt("x");
                    int y = point.getInt("y");
                    int radius = 8; //Radius defaults to 8
                    if (point.has("Radius")) {
                        radius = point.getInt("Radius");
                    }

                    paint.setColor(Color.parseColor(KELLY_COLOURS[i%KELLY_COLOURS.length]));
                    Rect bounds = new Rect();
                    paint.getTextBounds(name, 0, name.length(), bounds);
                    int xx = x - bounds.width()/2;
                    int yy = y - bounds.height()/2;
                    canvas.drawText(name, xx, yy, paint);
                    canvas.drawCircle(x, y, radius, paint);
                }
            }catch(JSONException e) {
                Log.e(classname, e.toString());
                if (default_mode == defaultBackground) {
                    imageView.setImageResource(R.color.white);
                } else {
                    imageView.setImageResource(android.R.color.transparent);
                }
                return new Dimension(imageView.getDrawable().getIntrinsicWidth(), imageView.getDrawable().getIntrinsicHeight());
            }

            int width = imageView.getWidth();
            if(width == 0){
                DisplayMetrics displayMetrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                width = displayMetrics.widthPixels;
            }
            Bitmap scaledBitmap = getScaledBitmap(bitmap, width);
            imageView.getLayoutParams().height = scaledBitmap.getHeight();
            imageView.requestLayout();
            imageView.setImageBitmap(scaledBitmap);
            return new Dimension(bitmap.getWidth(), bitmap.getHeight());
        } else {
            if (default_mode == defaultBackground) {
                imageView.setImageResource(R.color.white);
            } else {
                imageView.setImageResource(android.R.color.transparent);
            }
            return new Dimension(imageView.getDrawable().getIntrinsicWidth(), imageView.getDrawable().getIntrinsicHeight());
        }
    }

    @SuppressWarnings("unused")
    Drawable resizeIcon(Context context, int resource, int width, int height){
        BitmapDrawable drawable = (BitmapDrawable)ContextCompat.getDrawable(context, resource);
        if(drawable == null){
            return null;
        }
        Bitmap bitmap = drawable.getBitmap();
        return new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap, width, height, true));
    }

    Bitmap getProfileBitmap(Context context, String gender) {
        Bitmap bitmap = null;
        switch (gender) {
            case "Male":
                bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.male);
                break;
            case "Female":
                bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.female);
                break;
            case "Organisation":
                bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.organisation);
                break;
        }
        if (bitmap == null) {
            return BitmapFactory.decodeResource(context.getResources(), R.color.white);
        }
        return bitmap;
    }

    boolean saveBitmap(Context context, String encodedImage, String directory, String id){
        dirChecker(context.getCacheDir() + "/Images");
        File file = new File(context.getCacheDir() + "/" + directory + "/", id + ".png");
        try{
            boolean success = true;
            if(!file.exists()){
                success = file.createNewFile();
            }
            if(!success){
               return false;
            }
            FileOutputStream fout = new FileOutputStream(file);
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, fout);
            fout.flush();
            fout.close();
        }catch(IOException e){
            Log.e(classname, e.toString());
            return false;
        }
        return true;
    }

    /*
    boolean deleteBitmap(Context context, String id){
        File file = new File(context.getCacheDir() + "/Images/", id + ".jpg");
        if(!file.exists()){
            return true;
        }
        return file.delete();
    }
    */
    Bitmap getScaledBitmap(Bitmap bitmap, @SuppressWarnings("SameParameterValue") int width, @SuppressWarnings("SameParameterValue") int height) {
        int bWidth = bitmap.getWidth();
        int bHeight = bitmap.getHeight();
        float parentRatio = height/(float)width;
        Bitmap resizedBitmap;
        if(bWidth*parentRatio > bHeight){
            float newWidth = height*bWidth/(float)bHeight;
            resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int)newWidth, height, true);
            resizedBitmap = Bitmap.createBitmap(resizedBitmap, (int)(newWidth - width)/2, 0, width, height);
        }else{
            float newHeight = width*bHeight/(float)bWidth;
            resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, (int)newHeight, true);
            resizedBitmap = Bitmap.createBitmap(resizedBitmap, 0, (int)(newHeight - height)/2, width, height);
        }
        //bitmap.recycle();
        return resizedBitmap;
    }

    private Bitmap getScaledBitmap(Bitmap bitmap, int width) {
        int bWidth = bitmap.getWidth();
        int bHeight = bitmap.getHeight();
        float scaleRatio = width/(float)bWidth;
        Bitmap resizedBitmap;
        resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, (int)(bHeight*scaleRatio), true);
        return resizedBitmap;
    }

    private void dirChecker(String dir) {
        File f = new File(dir);
        if(!f.isDirectory()) {
            boolean success = f.mkdirs();
            if(!success) {
                Log.v(classname, "Failed to create " + dir);
            }
        }
    }
}