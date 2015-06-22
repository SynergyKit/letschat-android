package com.letsgood.letschat.synergykit;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.letsgood.letschat.ChatActivity;
import com.letsgood.letschat.CustomProgressDialog;
import com.letsgood.letschat.LoginActivity;
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
import com.letsgood.synergykitsdkandroid.listeners.SocketStateListener;
import com.letsgood.synergykitsdkandroid.listeners.UserResponseListener;
import com.letsgood.synergykitsdkandroid.log.SynergykitLog;
import com.letsgood.synergykitsdkandroid.request.SynergykitRequest;
import com.letsgood.synergykitsdkandroid.resources.SynergykitError;
import com.letsgood.synergykitsdkandroid.resources.SynergykitFacebookAuthData;
import com.letsgood.synergykitsdkandroid.resources.SynergykitObject;
import com.letsgood.synergykitsdkandroid.resources.SynergykitPlatform;
import com.letsgood.synergykitsdkandroid.resources.SynergykitUri;
import com.letsgood.synergykitsdkandroid.resources.SynergykitUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class SKChatActivity extends ChatActivity {

    private static final String EVENT_CREATED = "created";
    private static final String COLLECTION_MESSAGES = "messages";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String SENDER_ID = "125192900606";
    private static final String SHARED_PREFERENCES_USER_ID = "SKChatActivity.SHARED_PREFERENCES_USER_ID";
    private static final String SHARED_PREFERENCES_SESSION_TOKEN = "SKChatActivity.SHARED_PREFERENCES_SESSION_TOKEN";

    private GoogleCloudMessaging gcm;
    private String regid;

    private SKUser user;
    private SynergykitPlatform platform;

    private boolean messageSending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get user id and sessionToken
        String userId = sharedPreferences.getString(SHARED_PREFERENCES_USER_ID, null);
        String sessionToken = sharedPreferences.getString(SHARED_PREFERENCES_SESSION_TOKEN, null);

        if (userId == null || sessionToken == null)
            signInViaFacebook();
        else {
            Synergykit.setSessionToken(sessionToken);
            Synergykit.getUser(userId, SKUser.class, new UserResponseListener() {
                @Override
                public void doneCallback(int i, SynergykitUser synergykitUser) {
                    user = (SKUser) synergykitUser;
                    setOnline(true, true);
                    setupSynergyKit();
                }

                @Override
                public void errorCallback(int i, SynergykitError synergykitError) {
                    signInViaFacebook();
                }
            }, true);
        }
    }

    /* Sign in via Facebook to SynergyKit */
    protected void signInViaFacebook() {

        final AccessToken accessToken = AccessToken.getCurrentAccessToken();

        if (accessToken == null) {
            Toast.makeText(getApplicationContext(), R.string.chat_facebook_not_signed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        GraphRequest request = GraphRequest.newMeRequest(accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        if (object == null) {
                            Toast.makeText(getApplicationContext(), R.string.chat_login_failed, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        //show loading progress bar
                        final CustomProgressDialog progressDialog = new CustomProgressDialog(SKChatActivity.this, getString(R.string.synergykit_sign_message));

                        SKUser user = new SKUser();
                        SynergykitFacebookAuthData synergykitFacebookAuthData = new SynergykitFacebookAuthData(accessToken.getUserId(), accessToken.getToken());

                        try {
                            user.setName(object.getString("name"));
                            user.setOnline(true);
                        } catch (JSONException e) {
                            user.setName("JSONException :(");
                            e.printStackTrace();
                        }

                        // Sign up via SynergyKit
                        Synergykit.linkFacebook(user, synergykitFacebookAuthData, new UserResponseListener() {
                            @Override
                            public void doneCallback(int statusCode, SynergykitUser user) {
                                SKChatActivity.this.user = (SKUser) user;
                                sharedPreferences.edit()
                                        .putString(SHARED_PREFERENCES_USER_ID, user.getId())
                                        .putString(SHARED_PREFERENCES_SESSION_TOKEN, user.getSessionToken())
                                        .apply();
                                progressDialog.dismiss();
                                setupSynergyKit();
                            }

                            @Override
                            public void errorCallback(int statusCode, SynergykitError errorObject) {
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), R.string.chat_login_failed, Toast.LENGTH_SHORT).show();
                                connected = false;
                                finish();
                            }
                        });
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    // Setup everything that synergykit needs
    private void setupSynergyKit() {
        if (user == null) return;
        setupAdapter(user.getName(), true);

        setupStore();

        // Connect to socket
        Synergykit.connectSocket(new SocketStateListener() {
            @Override
            public void connected() {
                setStatus(STATUS_CONNECTED);
            }

            @Override
            public void disconnected() {
                setStatus(STATUS_DISCONNECTED);
            }

            @Override
            public void reconnected() {

            }

            @Override
            public void unauthorized() {
                Toast.makeText(getApplicationContext(), R.string.chat_login_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        });


        // Check device for Play Services APK.
        if (checkPlayServices()) {

            gcm = GoogleCloudMessaging.getInstance(getApplicationContext());

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
            finish();
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
                Toast.makeText(getApplicationContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });

        // Listen for changes in collection
        Synergykit.onSocket(EVENT_CREATED, COLLECTION_MESSAGES, new SocketEventListener() {
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

                messageSending = true;
                enableSend();
                SKMessage message = new SKMessage(user.getName(), messageEditText.getText().toString());

                Synergykit.createRecord(COLLECTION_MESSAGES, message, new ResponseListener() {
                    @Override
                    public void doneCallback(int statusCode, SynergykitObject synergykitObject) {
                        messageEditText.setText("");
                        messageEditText.setError(null);
                        messageSending = false;
                        hasText = false;
                        enableSend();
                    }

                    @Override
                    public void errorCallback(int statusCode, SynergykitError synergykitError) {
//                        Toast.makeText(getApplicationContext(), R.string.chat_message_send_failed, Toast.LENGTH_SHORT).show();
                        messageSending = false;
                        messageEditText.setError("Message has not been sent. Try again");
                    }
                }, true);
            }
        });
    }

    @Override
    protected void enableSend() {
        sendButton.setEnabled(hasText && connected && !messageSending);
    }

    // sets user online
    private void setOnline(final boolean online, boolean parallelMode) {
        if (user == null) return;

        user.setOnline(online);
        Synergykit.updateUser(user, new UserResponseListener() {
            @Override
            public void doneCallback(int i, SynergykitUser synergykitUser) {
                user = (SKUser) synergykitUser;
            }

            @Override
            public void errorCallback(int i, SynergykitError synergykitError) {
                if (user.isOnline()) {
                    Toast.makeText(getApplicationContext(), R.string.chat_online_error_push, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.chat_offline_error_push, Toast.LENGTH_LONG).show();
                }
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
                            Toast.makeText(getApplicationContext(), "Error - You won't receive push notification.", Toast.LENGTH_SHORT).show();
                        }
                    }, true);
                } else if (!platform.getRegistrationId().equals(regid)) {
                    platform.setRegistrationId(regid);
                    Synergykit.updatePlatform(platform, null, true);
                }
            }
        }, true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!fromLoginActivity) {
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                }
            default:
                return super.onOptionsItemSelected(item);
        }
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
}
