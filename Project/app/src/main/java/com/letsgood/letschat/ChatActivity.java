package com.letsgood.letschat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_IS_SYNERGYKIT = "ChatActivity.EXTRA_IS_SYNERGYKIT";

    private boolean isSynergyKit = true;

    private Button sendButton;
    private EditText messageEditText;
    private ListView messageListView;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getExtras();
        setTheme(isSynergyKit ? R.style.AppTheme_Synergykit : R.style.AppTheme_Firebase);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setupActionBar();
        initViews();
        setupViews();
        setupListeners();
    }

    private void getExtras() {
        isSynergyKit = true;
        if (getIntent() == null) return;
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) return;
        isSynergyKit = bundle.getBoolean(EXTRA_IS_SYNERGYKIT, true);
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(isSynergyKit ? R.string.chat_title_synergykit : R.string.chat_title_firebase);
        }
    }

    private void initViews() {
        messageListView = (ListView) findViewById(R.id.messageListView);
        messageEditText = (EditText) findViewById(R.id.messageEditText);
        sendButton = (Button) findViewById(R.id.buttonSend);
    }

    private void setupViews() {
        sendButton.setBackgroundResource(isSynergyKit ? R.drawable.button_synergykit_selector : R.drawable.button_firebase_selector);
        sendButton.setTextColor(getResources().getColor(isSynergyKit ? R.color.synergykit_white : R.color.firebase_black));
        adapter = new MessageAdapter(getApplicationContext(), 1, isSynergyKit);
        messageListView.setAdapter(adapter);
    }

    private void setupListeners() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageEditText.getText() != null) {
                    sendMessage(messageEditText.getText().toString());
//                    Toast.makeText(getApplicationContext(), messageEditText.getText() , Toast.LENGTH_SHORT).show();
                }
            }
        });

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

    private void sendMessage(String message) {
        adapter.add(new Message(1, message));
        adapter.notifyDataSetChanged();
    }


}
