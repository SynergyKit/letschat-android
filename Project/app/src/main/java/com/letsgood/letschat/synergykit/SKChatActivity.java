package com.letsgood.letschat.synergykit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.letsgood.letschat.ChatActivity;
import com.letsgood.letschat.CustomProgressDialog;
import com.letsgood.letschat.LetsChatApplication;
import com.letsgood.letschat.R;
import com.letsgood.synergykitsdkandroid.Synergykit;
import com.letsgood.synergykitsdkandroid.addons.GsonWrapper;
import com.letsgood.synergykitsdkandroid.builders.UriBuilder;
import com.letsgood.synergykitsdkandroid.builders.uri.Resource;
import com.letsgood.synergykitsdkandroid.config.SynergykitConfig;
import com.letsgood.synergykitsdkandroid.listeners.PlatformResponseListener;
import com.letsgood.synergykitsdkandroid.listeners.RecordsResponseListener;
import com.letsgood.synergykitsdkandroid.listeners.ResponseListener;
import com.letsgood.synergykitsdkandroid.listeners.SocketEventListener;
import com.letsgood.synergykitsdkandroid.listeners.UserResponseListener;
import com.letsgood.synergykitsdkandroid.log.SynergykitLog;
import com.letsgood.synergykitsdkandroid.request.SynergykitRequest;
import com.letsgood.synergykitsdkandroid.resources.SynergykitError;
import com.letsgood.synergykitsdkandroid.resources.SynergykitFacebookAuthData;
import com.letsgood.synergykitsdkandroid.resources.SynergykitObject;
import com.letsgood.synergykitsdkandroid.resources.SynergykitPlatform;
import com.letsgood.synergykitsdkandroid.resources.SynergykitUri;
import com.letsgood.synergykitsdkandroid.resources.SynergykitUser;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

public class SKChatActivity extends ChatActivity {

    private static final String EVENT_MESSAGE = "created";
    private static final String COLLECTION_MESSAGES = "messages";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String SENDER_ID = "125192900606";
    private static final String SHARED_PREFERENCES_USER_ID = "SKChatActivity.SHARED_PREFERENCES_USER_ID";
    private static final String SHARED_PREFERENCES_SESSION_TOKEN = "SKChatActivity.SHARED_PREFERENCES_SESSION_TOKEN";

    private String userId;
    private String sessionToken;

    private GoogleCloudMessaging gcm;
    private String regid;

    private SKUser user;
    private SynergykitPlatform platform;

    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = sharedPreferences.getString(SHARED_PREFERENCES_USER_ID, null);
        sessionToken = sharedPreferences.getString(SHARED_PREFERENCES_SESSION_TOKEN, null);

        if (userId == null || sessionToken == null)
            signInViaFacebook();
        else {
            Synergykit.setSessionToken(sessionToken);
            Synergykit.getUser(userId, SKUser.class, new UserResponseListener() {
                @Override
                public void doneCallback(int i, SynergykitUser synergykitUser) {
                    user = (SKUser) synergykitUser;
                    setOnline(true, true);
                    setupSK();
                }

                @Override
                public void errorCallback(int i, SynergykitError synergykitError) {
                    Toast.makeText(getApplicationContext(), "getUser - error", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }, true);
        }
    }

    /* Sign in via Facebook to SynergyKit */
    protected void signInViaFacebook() {

        if (!fromLoginActivity) init();
        if (AccessToken.getCurrentAccessToken() == null) {
            facebookLogin(new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    if (AccessToken.getCurrentAccessToken() == null) {
                        Toast.makeText(getApplicationContext(), getString(R.string.chat_facebook_not_signed), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    facebookGetDataAndRegister(AccessToken.getCurrentAccessToken());
                }

                @Override
                public void onCancel() {
                    Toast.makeText(getApplicationContext(), getString(R.string.chat_facebook_not_signed), Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(FacebookException e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.chat_facebook_not_signed), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            facebookGetDataAndRegister(AccessToken.getCurrentAccessToken());
        }

    }

    private void facebookLogin(FacebookCallback<LoginResult> callback) {
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, callback);
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
    }

    private void facebookGetDataAndRegister(final AccessToken accessToken) {
        // App code
        GraphRequest request = GraphRequest.newMeRequest(accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        SKUser user;

                        //show loading progress bar
                        final CustomProgressDialog progressDialog = new CustomProgressDialog(SKChatActivity.this, getString(R.string.synergykit_sign_message));

                        SynergykitFacebookAuthData synergykitFacebookAuthData = new SynergykitFacebookAuthData(accessToken.getUserId(), accessToken.getToken());

                        if (Synergykit.getLoggedUser() == null) {
                            user = new SKUser();
                        } else {
                            user = (SKUser) Synergykit.getLoggedUser();
                        }

                        //set user name if not exists
                        if (user.getName() == null || user.getName().isEmpty()) {
                            if (Profile.getCurrentProfile() != null && Profile.getCurrentProfile().getFirstName() != null)
                                user.setName(Profile.getCurrentProfile().getFirstName());

                            if (Profile.getCurrentProfile() != null && Profile.getCurrentProfile().getLastName() != null) {
                                if (user.getName() != null && !user.getName().isEmpty()) {
                                    user.setName(user.getName() + " " + Profile.getCurrentProfile().getLastName());
                                } else {
                                    user.setName(Profile.getCurrentProfile().getLastName());
                                }
                            }
                        }

                        // Sign up via SynergyKit
                        Synergykit.linkFacebook(user, synergykitFacebookAuthData, new UserResponseListener() {
                            @Override
                            public void doneCallback(int statusCode, SynergykitUser user) {
                                SKChatActivity.this.user = (SKUser) user;
                                userId = user.getId();
                                sessionToken = user.getSessionToken();
                                sharedPreferences.edit()
                                        .putString(SHARED_PREFERENCES_USER_ID, userId)
                                        .putString(SHARED_PREFERENCES_SESSION_TOKEN, sessionToken)
                                        .apply();
                                setOnline(true, false);
                                progressDialog.dismiss();
                                setupSK();
                            }

                            @Override
                            public void errorCallback(int statusCode, SynergykitError errorObject) {
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), R.string.chat_login_failed, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                });
        request.executeAsync();
    }

    private void init() {
        // init SDKs
        if (!FacebookSdk.isInitialized()) // facebook
            FacebookSdk.sdkInitialize(getApplicationContext());
        if (!Synergykit.isInit()) // synergykit
            Synergykit.init(LetsChatApplication.APPLICATION_TENANT, LetsChatApplication.APPLICATION_KEY);
    }

    // Setup everything that synergykit needs
    private void setupSK() {
        if (user == null) return;
        setupAdapter(user.getName(), true);

        setupStore();

        // Connect to socket
        Synergykit.connectSocket();

        // Check device for Play Services APK.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);

            if (user.getPlatforms() != null) {
                for (SynergykitPlatform p : user.getPlatforms()) {

                    if (p.getPlatformName().equals("android")) {
                        platform = p;
                    }

                }
            }

            registerInBackground();

        } else {
            SynergykitLog.print("No valid Google Play Services APK found.");
        }
    }

    // Setup storing messages in collection and listening for creation changes on collection
    private void setupStore() {

        // Get old messages - uri
        SynergykitUri synergyKitUri = UriBuilder
                .newInstance()
                .setResource(Resource.RESOURCE_DATA)
                .setCollection(COLLECTION_MESSAGES)
                .setOrderByDesc("createdAt")
                .setTop(prevMessageCount)
                .build();
        // Get old messages - config
        SynergykitConfig config = SynergykitConfig.newInstance()
                .setParallelMode(true)
                .setType(SKMessage[].class)
                .setUri(synergyKitUri);
        // Get old messages
        Synergykit.getRecords(config, new RecordsResponseListener() {
            @Override
            public void doneCallback(int statusCode, SynergykitObject[] synergykitObjects) {
                SKMessage[] messages = (SKMessage[]) synergykitObjects;
                for (int i = messages.length - 1; i >= 0; i--)
                    adapter.add(messages[i]);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void errorCallback(int statusCode, SynergykitError synergykitError) {

            }
        });

        // Listen for changes in collection
        Synergykit.onSocket(EVENT_MESSAGE, COLLECTION_MESSAGES, new SocketEventListener() {
            @Override
            public void call(Object... objects) {

                String data = objects[0].toString();
                final SKMessage message = GsonWrapper.getGson().fromJson(data, SKMessage.class);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.add(message);
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void subscribed() {
                // On start listening for changes
            }

            @Override
            public void unsubscribed() {
                // On end listening for changes
            }
        });

        // Store messages in collection
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageEditText.getText() == null) return;

                if (user == null) return;
                SKMessage message = new SKMessage(user.getName(), messageEditText.getText().toString());

                Synergykit.createRecord(COLLECTION_MESSAGES, message, new ResponseListener() {
                    @Override
                    public void doneCallback(int statusCode, SynergykitObject synergykitObject) {
                        messageEditText.setText("");
                    }

                    @Override
                    public void errorCallback(int statusCode, SynergykitError synergykitError) {
                        Toast.makeText(getApplicationContext(), R.string.chat_message_send_failed, Toast.LENGTH_SHORT).show();

                    }
                }, false);
            }
        });
    }

    // sets user online
    private void setOnline(boolean online, boolean parallelMode) {
        if (user == null) return;

        user.setOnline(online);
        Synergykit.updateUser(user, new UserResponseListener() {
            @Override
            public void doneCallback(int i, SynergykitUser synergykitUser) {
                user = (SKUser) synergykitUser;
            }

            @Override
            public void errorCallback(int i, SynergykitError synergykitError) {

            }
        }, parallelMode);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setOnline(true, true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        setOnline(false, true);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                SynergykitLog.print("This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p/>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        Synergykit.synergylize(new SynergykitRequest() {
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }

                    regid = gcm.register(SENDER_ID);

                } catch (IOException ex) {
                }

                return regid;
            }

            @Override
            protected void onPostExecute(Object object) {
                // You should send the registration ID to your server over HTTP,
                // so it can use GCM/HTTP or CCS to send messages to your app.
                // The request to your server should be authenticated if your app
                // is using accounts.

                // Not registered before
                if (platform == null) {
                    SynergykitPlatform platform = new SynergykitPlatform((String) object);

                    Synergykit.addPlatform(platform, new PlatformResponseListener() {
                        @Override
                        public void doneCallback(int statusCode, SynergykitPlatform platform) {
                            Synergykit.getUser(user.getId(), SKUser.class, new UserResponseListener() {
                                @Override
                                public void doneCallback(int i, SynergykitUser synergykitUser) {
                                    user = (SKUser) synergykitUser;
                                }

                                @Override
                                public void errorCallback(int i, SynergykitError synergykitError) {

                                }
                            }, true);
                        }

                        @Override
                        public void errorCallback(int statusCode, SynergykitError errorObject) {

                        }
                    }, true);
                } else if (!platform.getRegistrationId().equals(regid)) {
                    platform.setRegistrationId(regid);
                    Synergykit.updatePlatform(platform, new PlatformResponseListener() {
                        @Override
                        public void doneCallback(int i, SynergykitPlatform synergykitPlatform) {

                        }

                        @Override
                        public void errorCallback(int i, SynergykitError synergykitError) {

                        }
                    }, true);
                }
            }
        }, true);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Synergykit.isSocketConnected())
            Synergykit.disconnectSocket();
        if (user != null) {
            Synergykit.logoutUser();
            user = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (callbackManager != null)
            callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
