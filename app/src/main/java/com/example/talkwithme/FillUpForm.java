package com.example.talkwithme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import java.io.ByteArrayOutputStream;

public class FillUpForm extends Fragment {
    private FragmentActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fillupform, container, false);
        Context context = getContext();
        if (context == null) {
            return view;
        }
        activity = getActivity();

        int id = -1, gender = -1, state = -1, form = -1;
        String name = "", selected_organisation = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getInt("id", -1);
            name = bundle.getString("name", "");
            gender = bundle.getInt("gender", -1);
            state = bundle.getInt("state", -1);
            selected_organisation = bundle.getString("selected organisation", "");
            form = bundle.getInt("form", -1);
        }
        final int fid = id, fgender = gender, fstate = state;
        final String fname = name, fselected_organisation = selected_organisation;

        Fragment fragment = new FormContent();
        bundle = new Bundle();
        bundle.putInt("form", form);
        fragment.setArguments(bundle);
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.formfragment, fragment);
        ft.commit();

        final ScrollView formfragment = view.findViewById(R.id.formfragment);
        final Button DoneButton = view.findViewById(R.id.DoneButton);
        DoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Take a screenshot of application
                Bitmap bitmap = Bitmap.createBitmap(formfragment.getChildAt(0).getWidth(), formfragment.getChildAt(0).getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                Drawable bgDrawable = formfragment.getBackground();
                if (bgDrawable != null)
                    bgDrawable.draw(canvas);
                else
                    canvas.drawColor(Color.WHITE);
                formfragment.draw(canvas);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/3, bitmap.getHeight()/3, true);
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
                byte[] byte_arr = stream.toByteArray();
                String encodedImage = Base64.encodeToString(byte_arr, 0);

                Fragment fragment = new InformationIsTrue();
                Bundle bundle = new Bundle();
                bundle.putInt("id", fid);
                bundle.putString("name", fname);
                bundle.putInt("gender", fgender);
                bundle.putInt("state", fstate);
                bundle.putString("selected organisation", fselected_organisation);
                bundle.putString("encodedImage", encodedImage);
                fragment.setArguments(bundle);
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, fragment, "current");
                ft.commit();
            }
        });
        return view;
    }
}