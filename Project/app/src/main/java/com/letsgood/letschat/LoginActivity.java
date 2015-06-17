package com.letsgood.letschat;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
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
import com.letsgood.letschat.firebase.FBChatActivity;
import com.letsgood.letschat.synergykit.SKChatActivity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private ImageView synergykitIV;
    private ImageView firebaseIV;
    private LoginButton facebookButton; // facebook
    private CallbackManager callbackManager; // facebook

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext()); // facebook
        setContentView(R.layout.activity_login);
        initViews();
        setupListeners();
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
                Intent i = new Intent(getApplicationContext(), SKChatActivity.class);
                i.putExtra(SKChatActivity.EXTRA_FROM_LOGIN_ACTIVITY, true);
                startActivity(i);
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
                startActivity(new Intent(getApplicationContext(), FBChatActivity.class));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LoginManager.getInstance().logOut();
    }
}
