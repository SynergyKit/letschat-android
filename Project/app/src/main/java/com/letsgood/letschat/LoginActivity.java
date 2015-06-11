package com.letsgood.letschat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupActionBar();

        // Init
        ImageView sk = (ImageView) findViewById(R.id.chat_synergykit);
        ImageView fb = (ImageView) findViewById(R.id.chat_firebase);

        // Listeners
        sk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChat(true);
            }
        });

        fb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChat(false);
            }
        });
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) getSupportActionBar().hide();
    }

    private void startChat(boolean isSynergyKit) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra(ChatActivity.EXTRA_IS_SYNERGYKIT, isSynergyKit);
        startActivity(i);
    }

}
