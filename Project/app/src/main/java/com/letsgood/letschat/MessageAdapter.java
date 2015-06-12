package com.letsgood.letschat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MessageAdapter extends ArrayAdapter<Message> {

    private long myUserId;
    private boolean isSynergykit;

    public MessageAdapter(Context context, long myUserId, boolean isSynergykit) {
        super(context, R.layout.item_chat_message_left);
        this.myUserId = myUserId;
        this.isSynergykit = isSynergykit;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Message message = getItem(position);

        if (message == null) return convertView;
        boolean myMessage = myUserId == message.userId;

        // my message
        if (myMessage) {
            if (isSynergykit)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat_message_right_synergykit, parent, false);
            else
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat_message_right_firebase, parent, false);
        } else {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat_message_left, parent, false);
        }

        TextView messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
        messageTextView.setText(message.text);

        TextView userLetterTextView = (TextView) convertView.findViewById(R.id.userLetterTextView);
        userLetterTextView.setText(""+myUserId);

        return convertView;
    }
}
