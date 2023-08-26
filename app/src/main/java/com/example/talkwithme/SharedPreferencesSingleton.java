package com.example.talkwithme;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

class SharedPreferencesSingleton {
    private static SharedPreferences prefs = null;
    private SharedPreferencesSingleton(){}

    static synchronized SharedPreferences getPrefsInstance(Context context) {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                    prefs = EncryptedSharedPreferences.create(
                            "secret_shared_prefs",
                            masterKeyAlias,
                            context,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );
                } catch (IOException | GeneralSecurityException e) {
                    Log.e("SharedPrefSingleton:", e.toString());
                }
            }
        }
        return prefs;
    }
}
