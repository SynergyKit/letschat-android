package com.letsgood.letschat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private ImageView synergykit;
    private ImageView firebase;
    private LoginButton facebookButton; // facebook

    private CallbackManager callbackManager; // facebook

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext()); // facebook
        setContentView(R.layout.activity_login);
        setupActionBar();
        initViews();
        setListeners();
        setCallbacks();
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) getSupportActionBar().hide();
    }

    private void initViews() {
        synergykit = (ImageView) findViewById(R.id.chat_synergykit);
        firebase = (ImageView) findViewById(R.id.chat_firebase);
        facebookButton = (LoginButton) findViewById(R.id.login_button);
    }

    private void setListeners() {
        // synergykit
        synergykit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AccessToken.getCurrentAccessToken() == null) {
                    Toast.makeText(getApplicationContext(), R.string.login_not_logged_in, Toast.LENGTH_SHORT).show();
                    return;
                }
                startChat(true);
            }
        });

        // firebase
        firebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AccessToken.getCurrentAccessToken() == null) {
                    Toast.makeText(getApplicationContext(), R.string.login_not_logged_in, Toast.LENGTH_SHORT).show();
                    return;
                }
                startChat(false);
            }
        });

        // facebook
        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AccessToken.getCurrentAccessToken() == null)
                    LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("public_profile"));
            }
        });
    }

    private void setCallbacks() {
        callbackManager = CallbackManager.Factory.create();
        facebookButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast.makeText(getApplicationContext(), R.string.facebook_login_success, Toast.LENGTH_SHORT).show();
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

    private void startChat(boolean isSynergyKit) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra(ChatActivity.EXTRA_IS_SYNERGYKIT, isSynergyKit);
        startActivity(i);
    }

}
