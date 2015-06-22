package com.letsgood.letschat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity {

    protected Button sendButton;
    protected EditText messageEditText;
    protected TextView connectedStatusTextView;
    protected ListView messageListView;
    protected MessageAdapter adapter;

    public static final String EXTRA_FROM_LOGIN_ACTIVITY = "extra_from_login_activity";
    private static final String SHARED_PREFERENCES = "i7h2g9uefh0a909ujc48ej8cmq";
    protected static final int STATUS_CONNECTED = 1;
    protected static final int STATUS_DISCONNECTED = 2;

    protected int prevMessageCount = 100;
    protected boolean fromLoginActivity;
    protected SharedPreferences sharedPreferences;

    private int currentStatus;
    protected boolean connected;
    protected boolean hasText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        if (getIntent() != null && getIntent().getExtras() != null)
            fromLoginActivity = getIntent().getExtras().getBoolean(EXTRA_FROM_LOGIN_ACTIVITY, false);
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, 0);
        setupActionBar();
        initViews();
        setupListeners();
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void initViews() {
        messageListView = (ListView) findViewById(R.id.messageListView);
        messageEditText = (EditText) findViewById(R.id.messageEditText);
        sendButton = (Button) findViewById(R.id.buttonSend);
        connectedStatusTextView = (TextView) findViewById(R.id.connectedStatus);
    }

    private void setupListeners() {
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                hasText = s != null && s.length() > 0;
                enableSend();
            }
        });
    }

    protected void enableSend() {
        sendButton.setEnabled(connected && hasText);
    }

    /* This method should be called after getUserName returns correct value */
    protected void setupAdapter(String userName, boolean isSynergykit) {
        adapter = new MessageAdapter(getApplicationContext(), userName, isSynergykit);
        messageListView.setAdapter(adapter);
    }

    protected void setStatus(final int status) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                switch (status) {
                    case STATUS_CONNECTED:
                        setConnected();
                        break;
                    case STATUS_DISCONNECTED:
                        setDisconnected();
                        break;
                }
            }
        });
    }

    private void setConnected() {
        connected = true;
        enableSend();
        connectedStatusTextView.setText(R.string.chat_connected);
        connectedStatusTextView.setVisibility(View.VISIBLE);
        connectedStatusTextView.setBackgroundColor(getResources().getColor(R.color.chatConnectedBackground));
        connectedStatusTextView.setTextColor(getResources().getColor(R.color.chatConnectedText));
        currentStatus = STATUS_CONNECTED;
        hideView(STATUS_CONNECTED);
    }

    private void setDisconnected() {
        connected = false;
        enableSend();
        connectedStatusTextView.setText(R.string.chat_disconnected);
        connectedStatusTextView.setVisibility(View.VISIBLE);
        connectedStatusTextView.setBackgroundColor(getResources().getColor(R.color.chatDisconnectedBackground));
        connectedStatusTextView.setTextColor(getResources().getColor(R.color.chatDisconnectedText));
        currentStatus = STATUS_DISCONNECTED;
    }

    private void hideView(final int status) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentStatus == status)
                    connectedStatusTextView.setVisibility(View.GONE);
            }
        }, 3000);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}