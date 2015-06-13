package com.letsgood.letschat.synergykit;

import com.google.gson.annotations.Expose;
import com.letsgood.letschat.Message;
import com.letsgood.synergykitsdkandroid.resources.SynergykitObject;

public class SKMessage extends SynergykitObject implements Message {

    @Expose
    private String name;

    @Expose
    private String text;

    public SKMessage(String name, String text) {
        this.name = name;
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
