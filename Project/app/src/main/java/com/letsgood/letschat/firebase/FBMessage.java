package com.letsgood.letschat.firebase;

import com.letsgood.letschat.Message;

public class FBMessage implements Message {

    String name;
    String text;
    long timestamp;

    public FBMessage() {
    }

    public FBMessage(String name, String text) {
        this.name = name;
        this.text = text;
        timestamp = System.currentTimeMillis();
    }

    public FBMessage(String name, String text, long timestamp) {
        this.name = name;
        this.text = text;
        this.timestamp = timestamp;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
