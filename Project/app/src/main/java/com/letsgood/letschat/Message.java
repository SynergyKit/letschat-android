package com.letsgood.letschat;

public class Message {

    protected String text;
    protected long userId;

    public Message(long userId, String text) {
        this.userId = userId;
        this.text = text;
    }

}
