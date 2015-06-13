package com.letsgood.letschat.firebase;

import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.letsgood.letschat.ChatActivity;
import com.letsgood.letschat.CustomProgressDialog;
import com.letsgood.letschat.R;

public class FBChatActivity extends ChatActivity {

    private static final String COLLECTION_MESSAGES = "messages";

    private Firebase firebase; // firebase
    private String userName;
    private long startingTimestamp;

    protected void signInViaFacebook() {
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        firebase = new Firebase("https://letsgood-letschat.firebaseio.com"); // firebase

        /* Firebase sign in facebook */
        final CustomProgressDialog progressDialog = new CustomProgressDialog(FBChatActivity.this);

        firebase.authWithOAuthToken("facebook", accessToken.getToken(), new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                progressDialog.dismiss();
                setupFB(authData);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), R.string.chat_login_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupFB(AuthData authData) {
        userName = (String) authData.getProviderData().get("displayName");
        startingTimestamp = System.currentTimeMillis();
        setupAdapter(userName, false);
        firebase.getRoot().child(COLLECTION_MESSAGES).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot == null) return;
                FBMessage message = dataSnapshot.getValue(FBMessage.class);
                if (message.timestamp < startingTimestamp)
                    return; // workaround to not show old messages
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
                FBMessage message = new FBMessage(userName, messageEditText.getText().toString());
                firebase.getRoot().child(COLLECTION_MESSAGES).push().setValue(message, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if (firebaseError != null) {
                            Toast.makeText(getApplicationContext(), R.string.chat_message_send_failed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        messageEditText.setText("");
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        firebase.unauth();
    }
}