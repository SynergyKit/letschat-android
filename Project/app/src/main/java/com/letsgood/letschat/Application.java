package com.letsgood.letschat;

import com.facebook.FacebookSdk;
import com.firebase.client.Firebase;
import com.letsgood.synergykitsdkandroid.Synergykit;

public class Application extends android.app.Application {

    /* Constants */
    private static final String APPLICATION_TENANT = "CHANGE"; //Application tenant from SynergyKit
    private static final String APPLICATION_KEY = "CHANGE"; //Application key from SynergyKit

    @Override
    public void onCreate() {
        super.onCreate();

        // firebase
        Firebase.setAndroidContext(this);

        // synergykit
        if (!Synergykit.isInit())
            Synergykit.init(APPLICATION_TENANT, APPLICATION_KEY);
    }
}
