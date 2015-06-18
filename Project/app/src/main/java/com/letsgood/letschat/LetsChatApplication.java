package com.letsgood.letschat;

import com.firebase.client.Firebase;
import com.firebase.client.Logger;
import com.letsgood.synergykitsdkandroid.Synergykit;

public class LetsChatApplication extends android.app.Application {

    /* Constants */
    public static final String APPLICATION_TENANT = "letschat"; //Application tenant from SynergyKit
    public static final String APPLICATION_KEY = "e61dc2d3-ce4a-4dd5-a704-7fc619896689"; //Application key from SynergyKit

    @Override
    public void onCreate() {
        super.onCreate();

        // firebase
        Firebase.setAndroidContext(this);
        if (BuildConfig.DEBUG)
            Firebase.getDefaultConfig().setLogLevel(Logger.Level.DEBUG);

        // synergykit
        if (!Synergykit.isInit()) {
            Synergykit.init(APPLICATION_TENANT, APPLICATION_KEY);
            if (BuildConfig.DEBUG)
                Synergykit.setDebugModeEnabled(BuildConfig.DEBUG);
        }
    }
}
