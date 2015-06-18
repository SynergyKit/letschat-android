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
import com.letsgood.letschat.ChatActivity;
import com.letsgood.letschat.CustomProgressDialog;
import com.letsgood.letschat.R;

import java.util.HashMap;
import java.util.Map;

public class FBChatActivity extends ChatActivity {

    private static final String COLLECTION_MESSAGES = "messages";

    private Firebase firebase; // firebase
    private String userName;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        signInViaFacebook();
    }

    protected void signInViaFacebook() {
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        firebase = new Firebase("https://letsgood-letschat.firebaseio.com"); // firebase

        /* Firebase sign in facebook */
        final CustomProgressDialog progressDialog = new CustomProgressDialog(FBChatActivity.this, getString(R.string.firebase_sign_message));

        firebase.authWithOAuthToken("facebook", accessToken.getToken(), new Firebase.AuthResultHandler() {
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
        userId = Long.parseLong(authData.getProviderData().get("id").toString());
        setOnline(true);
        setupAdapter(userName, false);
        Query query = firebase.getRoot().child(COLLECTION_MESSAGES).orderByChild("timestamp").limitToLast(prevMessageCount);
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

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendButton.setEnabled(false);
                FBMessage message = new FBMessage(userName, messageEditText.getText().toString());
                firebase.getRoot().child(COLLECTION_MESSAGES).push().setValue(message, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if (firebaseError != null) {
                            Toast.makeText(getApplicationContext(), R.string.chat_message_send_failed, Toast.LENGTH_SHORT).show();
                            sendButton.setEnabled(true);
                            return;
                        }
                        messageEditText.setText("");
                    }
                });
            }
        });
    }

    // Set status of user in firebase
    private void setOnline(boolean online) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", userName);
        map.put("online", online);
        firebase.child("users").child("" + userId).setValue(map);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != 0) setOnline(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userId != 0) setOnline(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        firebase.unauth();
    }
}
