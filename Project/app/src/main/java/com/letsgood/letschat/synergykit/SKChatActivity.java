package com.letsgood.letschat.synergykit;

import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.letsgood.letschat.ChatActivity;
import com.letsgood.letschat.CustomProgressDialog;
import com.letsgood.letschat.R;
import com.letsgood.synergykitsdkandroid.Synergykit;
import com.letsgood.synergykitsdkandroid.addons.GsonWrapper;
import com.letsgood.synergykitsdkandroid.listeners.DeleteResponseListener;
import com.letsgood.synergykitsdkandroid.listeners.NotificationResponseListener;
import com.letsgood.synergykitsdkandroid.listeners.PlatformResponseListener;
import com.letsgood.synergykitsdkandroid.listeners.ResponseListener;
import com.letsgood.synergykitsdkandroid.listeners.SocketEventListener;
import com.letsgood.synergykitsdkandroid.listeners.UserResponseListener;
import com.letsgood.synergykitsdkandroid.log.SynergykitLog;
import com.letsgood.synergykitsdkandroid.request.SynergykitRequest;
import com.letsgood.synergykitsdkandroid.resources.SynergykitError;
import com.letsgood.synergykitsdkandroid.resources.SynergykitFacebookAuthData;
import com.letsgood.synergykitsdkandroid.resources.SynergykitNotification;
import com.letsgood.synergykitsdkandroid.resources.SynergykitObject;
import com.letsgood.synergykitsdkandroid.resources.SynergykitPlatform;
import com.letsgood.synergykitsdkandroid.resources.SynergykitUser;

import org.json.JSONObject;

import java.io.IOException;

public class SKChatActivity extends ChatActivity {

    private static final String EVENT_MESSAGE = "created";
    private static final String COLLECTION_MESSAGES = "messages";
    private static final String COLLECTION_ONLINE = "online";
    private static final boolean STORE_MESSAGES = true;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String SENDER_ID = "442287231370";
    private GoogleCloudMessaging gcm;
    private String regid;
    private SKOnlineStatus onlineStatus;

    private String userName;

    /* Sign in via Facebook to SynergyKit */
    protected void signInViaFacebook() {
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null) return;

        // App code
        GraphRequest request = GraphRequest.newMeRequest(accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        SKUser user;

                        //show loading progress bar
                        final CustomProgressDialog progressDialog = new CustomProgressDialog(SKChatActivity.this);

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

    // Setup everything that synergykit needs
    private void setupSK() {
        if (Synergykit.getLoggedUser() == null) return;
        userName = ((SKUser) Synergykit.getLoggedUser()).getName();
        setupAdapter(userName, true);

        if (STORE_MESSAGES) setupStore();
        else setupNonStore();

        // Connect to socket
        Synergykit.connectSocket();

        // Check device for Play Services APK.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = null;

            if (Synergykit.getLoggedUser().getPlatforms() != null) {
                for (SynergykitPlatform p : Synergykit.getLoggedUser().getPlatforms()) {

                    if (p.getPlatformName().equals("android"))
                        regid = p.getRegistrationId();

                }
            }

            if (regid == null || regid.isEmpty())
                registerInBackground();
            else
                sendButton.setEnabled(true);

        } else {
            SynergykitLog.print("No valid Google Play Services APK found.");
        }

        setOnline(true);
    }

    // Setup storing messages in collection and listening for creation changes on collection
    private void setupStore() {
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

                SKMessage message = new SKMessage(userName, messageEditText.getText().toString());

                Synergykit.createRecord(COLLECTION_MESSAGES, message, new ResponseListener() {
                    @Override
                    public void doneCallback(int statusCode, SynergykitObject synergykitObject) {
                        //create notification
//                        SynergykitNotification notification = new SynergykitNotification();
//                        notification.setAlert(messageEditText.getText().toString());
//                        notification.addUserId(Synergykit.getLoggedUser().getId());

                        //send notification
//                        Synergykit.sendNotification(notification, new NotificationResponseListener() {
//                            @Override
//                            public void doneCallback(int statusCode) {
//                                Toast.makeText(getApplicationContext(), "Notification sent!", Toast.LENGTH_SHORT).show();
//                            }
//
//                            @Override
//                            public void errorCallback(int statusCode, SynergykitError errorObject) {
//                                Toast.makeText(getApplicationContext(), "Notification error!", Toast.LENGTH_SHORT).show();
//                            }
//                        }, true);

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

    // Setup sending event and listening for that event
    private void setupNonStore() {
        // Listen for event
        Synergykit.onSocket(EVENT_MESSAGE, new SocketEventListener() {
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

        // Send event to all user listening for that event
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageEditText.getText() != null) {
                    SKMessage message = new SKMessage(userName, messageEditText.getText().toString());

                    Synergykit.emitViaSocket(EVENT_MESSAGE, message);
                }
            }
        });
    }

    private void setOnline(boolean online) {
        if (Synergykit.getLoggedUser() != null) {

            if (online && onlineStatus == null) {
                // create online Record
                SKOnlineStatus status = new SKOnlineStatus(userName);
                Synergykit.createRecord(COLLECTION_ONLINE, status, new ResponseListener() {
                    @Override
                    public void doneCallback(int i, SynergykitObject synergykitObject) {
                        onlineStatus = (SKOnlineStatus) synergykitObject;
                        Toast.makeText(getApplicationContext(), "You are online", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void errorCallback(int i, SynergykitError synergykitError) {
                        Toast.makeText(getApplicationContext(), synergykitError.toString(), Toast.LENGTH_SHORT).show();
                    }
                }, true);
            } else if (!online && onlineStatus != null){
                // delete online Record
                Synergykit.deleteRecord(COLLECTION_ONLINE, onlineStatus.getId(), new DeleteResponseListener() {
                    @Override
                    public void doneCallback(int i) {
                        Toast.makeText(getApplicationContext(), "You are offline", Toast.LENGTH_SHORT).show();
                        onlineStatus = null;
                    }

                    @Override
                    public void errorCallback(int i, SynergykitError synergykitError) {
                        Toast.makeText(getApplicationContext(), synergykitError.toString(), Toast.LENGTH_SHORT).show();
                    }
                }, true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setOnline(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        setOnline(false);
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
                SynergykitPlatform platform = new SynergykitPlatform((String) object);

                SynergykitLog.print(Synergykit.getSessionToken());

                Synergykit.addPlatform(platform, new PlatformResponseListener() {
                    @Override
                    public void doneCallback(int statusCode, SynergykitPlatform platform) {
                        sendButton.setEnabled(true);
                    }

                    @Override
                    public void errorCallback(int statusCode, SynergykitError errorObject) {

                    }
                }, true);
            }
        }, false);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Synergykit.isSocketConnected())
            Synergykit.disconnectSocket();
        if (Synergykit.getLoggedUser() != null)
            Synergykit.logoutUser();
    }
}
