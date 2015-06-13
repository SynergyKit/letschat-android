package com.letsgood.letschat.synergykit;

import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.letsgood.letschat.ChatActivity;
import com.letsgood.letschat.CustomProgressDialog;
import com.letsgood.letschat.R;
import com.letsgood.synergykitsdkandroid.Synergykit;
import com.letsgood.synergykitsdkandroid.addons.GsonWrapper;
import com.letsgood.synergykitsdkandroid.listeners.ResponseListener;
import com.letsgood.synergykitsdkandroid.listeners.SocketEventListener;
import com.letsgood.synergykitsdkandroid.listeners.UserResponseListener;
import com.letsgood.synergykitsdkandroid.resources.SynergykitError;
import com.letsgood.synergykitsdkandroid.resources.SynergykitFacebookAuthData;
import com.letsgood.synergykitsdkandroid.resources.SynergykitObject;
import com.letsgood.synergykitsdkandroid.resources.SynergykitUser;

import org.json.JSONObject;

public class SKChatActivity extends ChatActivity {

    private static final String EVENT_MESSAGE = "created";
    private static final String COLLECTION_MESSAGES = "messages";
    private static final boolean STORE_MESSAGES = true;

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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Synergykit.isSocketConnected())
            Synergykit.disconnectSocket();
        if (Synergykit.getLoggedUser() != null)
            Synergykit.logoutUser();
    }
}
