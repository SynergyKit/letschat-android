package com.letsgood.letschat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import com.facebook.login.widget.LoginButton;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.letsgood.synergykitsdkandroid.Synergykit;
import com.letsgood.synergykitsdkandroid.listeners.UserResponseListener;
import com.letsgood.synergykitsdkandroid.resources.SynergykitError;
import com.letsgood.synergykitsdkandroid.resources.SynergykitFacebookAuthData;
import com.letsgood.synergykitsdkandroid.resources.SynergykitUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private ImageView synergykitIV;
    private ImageView firebaseIV;
    private LoginButton facebookButton; // facebook

    private CallbackManager callbackManager; // facebook

    private Firebase firebase; // firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext()); // facebook
        firebase = new Firebase("https://letsgood-letschat.firebaseio.com"); // firebase
        setContentView(R.layout.activity_login);
        setupActionBar();
        initViews();
        setupListeners();
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) getSupportActionBar().hide();
    }

    private void initViews() {
        synergykitIV = (ImageView) findViewById(R.id.chat_synergykit);
        firebaseIV = (ImageView) findViewById(R.id.chat_firebase);
        facebookButton = (LoginButton) findViewById(R.id.login_button);
    }

    private void setupListeners() {
        // synergykit
        synergykitIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AccessToken.getCurrentAccessToken() == null) {
                    Toast.makeText(getApplicationContext(), R.string.login_not_logged_in, Toast.LENGTH_SHORT).show();
                    return;
                }
                synergykitSignInFacebook(AccessToken.getCurrentAccessToken());
            }
        });

        // firebase
        firebaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AccessToken.getCurrentAccessToken() == null) {
                    Toast.makeText(getApplicationContext(), R.string.login_not_logged_in, Toast.LENGTH_SHORT).show();
                    return;
                }
                firebaseSignInFacebook(AccessToken.getCurrentAccessToken());
            }
        });

        // facebook
        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("public_profile"));
            }
        });

        callbackManager = CallbackManager.Factory.create();
        facebookButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
//                Toast.makeText(getApplicationContext(), R.string.facebook_login_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(), R.string.facebook_login_cancel, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(getApplicationContext(), R.string.facebook_login_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /* Synergykit Sign in via Facebook */
    private void synergykitSignInFacebook(final AccessToken accessToken) {
        // App code
        GraphRequest request = GraphRequest.newMeRequest(accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        SynUser user = null;

                        //show loading progress bar
                        final CustomProgressDialog progressDialog = new CustomProgressDialog(LoginActivity.this, "Signing in ...");

                        SynergykitFacebookAuthData synergykitFacebookAuthData = new SynergykitFacebookAuthData(accessToken.getUserId(), accessToken.getToken());

                        if (Synergykit.getLoggedUser() == null) {
                            user = new SynUser();
                        } else {
                            user = (SynUser) Synergykit.getLoggedUser();
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


//                        LoginManager.getInstance().logOut();

                        // Sign up via SynergyKit
                        Synergykit.linkFacebook(user, synergykitFacebookAuthData, new UserResponseListener() {
                            @Override
                            public void doneCallback(int statusCode, SynergykitUser user) {
                                progressDialog.dismiss();

                                Toast.makeText(getApplicationContext(), "You are signed in via Facebook!", Toast.LENGTH_SHORT).show();
                                startChat(true);
                            }

                            @Override
                            public void errorCallback(int statusCode, SynergykitError errorObject) {
                                LoginManager.getInstance().logOut();
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), errorObject.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    /* Firebase sign in facebook */
    private void firebaseSignInFacebook(final AccessToken accessToken) {
        final CustomProgressDialog progressDialog = new CustomProgressDialog(LoginActivity.this, "Signing in ...");

        firebase.authWithOAuthToken("facebook", accessToken.getToken(), new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                progressDialog.dismiss();
                String name = (String) authData.getProviderData().get("displayName");
                Toast.makeText(getApplicationContext(), "onAuthenticated " + name, Toast.LENGTH_SHORT).show();
                startChat(false);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "onAuthenticationError", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void startChat(boolean isSynergyKit) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra(ChatActivity.EXTRA_IS_SYNERGYKIT, isSynergyKit);
        startActivity(i);
    }

}
