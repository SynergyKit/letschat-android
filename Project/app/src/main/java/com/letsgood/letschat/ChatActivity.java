package com.letsgood.letschat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.letsgood.synergykitsdkandroid.Synergykit;
import com.letsgood.synergykitsdkandroid.addons.GsonWrapper;
import com.letsgood.synergykitsdkandroid.listeners.ResponseListener;
import com.letsgood.synergykitsdkandroid.listeners.SocketEventListener;
import com.letsgood.synergykitsdkandroid.resources.SynergykitError;
import com.letsgood.synergykitsdkandroid.resources.SynergykitObject;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_IS_SYNERGYKIT = "ChatActivity.EXTRA_IS_SYNERGYKIT";

    private static final String EVENT_MESSAGE = "created";
    private static final String COLLECTION_MESSAGE = "messages";

    private boolean isSynergyKit = true;
    private String userName;

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
        if (isSynergyKit) synergykitSetup();
        else firebaseSetup();
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
        String userName = "";
        if (isSynergyKit && Synergykit.getLoggedUser() != null) {
            userName = ((SynUser) Synergykit.getLoggedUser()).getName();
        }
        adapter = new MessageAdapter(getApplicationContext(), userName, isSynergyKit);
        messageListView.setAdapter(adapter);
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

    private void synergykitSetup() {
        if (Synergykit.getLoggedUser() == null) return;
        userName = ((SynUser) Synergykit.getLoggedUser()).getName();

        Synergykit.onSocket(EVENT_MESSAGE/*, COLLECTION_MESSAGE*/, new SocketEventListener() {
            @Override
            public void call(Object... objects) {

                String data = objects[0].toString();
                final Message message = GsonWrapper.getGson().fromJson(data, Message.class);

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
                // Start listening for changes
            }

            @Override
            public void unsubscribed() {
                // End listening for changes
            }
        });

        Synergykit.connectSocket();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageEditText.getText() != null) {
                    Message message = new Message(userName, messageEditText.getText().toString());
//                    Synergykit.createRecord(COLLECTION_MESSAGE, message, new ResponseListener() {
//                        @Override
//                        public void doneCallback(int i, SynergykitObject synergykitObject) {
//                            Toast.makeText(getApplicationContext(), "Message sent!", Toast.LENGTH_SHORT).show();
//                            messageEditText.setText("");
//                        }
//
//                        @Override
//                        public void errorCallback(int i, SynergykitError synergykitError) {
//                            Toast.makeText(getApplicationContext(), "Error sending message!", Toast.LENGTH_SHORT).show();
//                        }
//                    }, false);
                    Synergykit.emitViaSocket(EVENT_MESSAGE, message);
                }
            }
        });
    }

    private void firebaseSetup() {

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
