package com.letsgood.letschat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ChatActivity extends AppCompatActivity {

    protected Button sendButton;
    protected EditText messageEditText;
    protected ListView messageListView;
    protected MessageAdapter adapter;

    public static final String EXTRA_FROM_LOGIN_ACTIVITY = "extra_from_login_activity";
    private static final String SHARED_PREFERENCES = "i7h2g9uefh0a909ujc48ej8cmq";

    protected int prevMessageCount = 100;
    protected boolean fromLoginActivity;
    protected SharedPreferences sharedPreferences;

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
                if (s == null) return;
                sendButton.setEnabled(s.length() > 0);
            }
        });
    }

    /* This method should be called after getUserName returns correct value */
    protected void setupAdapter(String userName, boolean isSynergykit) {
        adapter = new MessageAdapter(getApplicationContext(), userName, isSynergykit);
        messageListView.setAdapter(adapter);
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