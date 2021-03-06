package com.letsgood.letschat.firebase;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.letsgood.letschat.ChatActivity;
import com.letsgood.letschat.CustomProgressDialog;
import com.letsgood.letschat.R;

import java.util.HashMap;
import java.util.Map;

public class FBChatActivity extends ChatActivity {

    private static final String COLLECTION_MESSAGES = "messages";
    private static final String COLLECTION_USERS = "users";

    private Firebase firebaseRoot; // firebase
    private Firebase firebaseMessages; // firebase
    private Firebase firebaseUsers; // firebase
    private Firebase firebaseConnected; // firebase
    private String userName;
    private String uId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        signInViaFacebook();
    }

    protected void signInViaFacebook() {
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        firebaseRoot = new Firebase("https://letsgood-letschat.firebaseio.com"); // firebase
        firebaseMessages = new Firebase("https://letsgood-letschat.firebaseio.com/"+COLLECTION_MESSAGES); // firebase
        firebaseUsers = new Firebase("https://letsgood-letschat.firebaseio.com/"+COLLECTION_USERS); // firebase
        firebaseConnected = new Firebase("https://letsgood-letschat.firebaseio.com/.info/connected"); // firebase

        /* Firebase sign in facebook */
        final CustomProgressDialog progressDialog = new CustomProgressDialog(FBChatActivity.this, getString(R.string.firebase_sign_message));

        firebaseRoot.authWithOAuthToken("facebook", accessToken.getToken(), new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                progressDialog.dismiss();
                setupFirebase(authData);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), R.string.chat_login_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupFirebase(AuthData authData) {
        userName = (String) authData.getProviderData().get("displayName");
        uId = authData.getUid();
        setOnline(true);
        setupAdapter(userName, false);

        // Reading & listening messages
        Query query = firebaseMessages.orderByChild("timestamp").limitToLast(prevMessageCount);
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot == null) return;
                FBMessage message = dataSnapshot.getValue(FBMessage.class);
                adapter.add(message);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        // Sending messages
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageEditText.getText() == null) return;
                sendButton.setEnabled(false);
                final FBMessage message = new FBMessage(userName, messageEditText.getText().toString());
                firebaseMessages.push().setValue(message, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if (firebaseError != null) {
//                            Toast.makeText(getApplicationContext(), R.string.chat_message_send_failed, Toast.LENGTH_SHORT).show();
                            messageEditText.setError("Message has not been sent. Try again");
                            return;
                        }
                        messageEditText.setText("");
                        messageEditText.setError(null);
                        hasText = false;
                        enableSend();
                    }
                });
            }
        });

        // Connected Status
        firebaseConnected.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    setStatus(STATUS_CONNECTED);
                } else {
                    setStatus(STATUS_DISCONNECTED);
                }
            }
            @Override
            public void onCancelled(FirebaseError error) {
                System.err.println("Listener was cancelled");
            }
        });
    }

    // Set status of user in firebase
    private void setOnline(boolean online) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", userName);
        map.put("online", online);
        firebaseUsers.child("" + uId).setValue(map);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uId != null) setOnline(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (uId != null) setOnline(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        firebaseRoot.unauth();
    }
}
