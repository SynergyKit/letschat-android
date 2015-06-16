package com.letsgood.letschat.synergykit;

import com.google.gson.annotations.Expose;
import com.letsgood.synergykitsdkandroid.resources.SynergykitObject;

public class SKOnlineStatus extends SynergykitObject {

    @Expose
    private String name;

    public SKOnlineStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
