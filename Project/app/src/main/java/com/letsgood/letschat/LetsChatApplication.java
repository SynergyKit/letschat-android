package com.letsgood.letschat;

import com.facebook.FacebookSdk;
import com.firebase.client.Firebase;
import com.letsgood.synergykitsdkandroid.Synergykit;

public class LetsChatApplication extends android.app.Application {

    /* Constants */
    private static final String APPLICATION_TENANT = "letschat"; //Application tenant from SynergyKit
    private static final String APPLICATION_KEY = "e61dc2d3-ce4a-4dd5-a704-7fc619896689"; //Application key from SynergyKit

    @Override
    public void onCreate() {
        super.onCreate();

        // firebase
        Firebase.setAndroidContext(this);

        // synergykit
        if (!Synergykit.isInit()) {
            Synergykit.init(APPLICATION_TENANT, APPLICATION_KEY);
            Synergykit.setDebugModeEnabled(BuildConfig.DEBUG);
        }
    }
}
