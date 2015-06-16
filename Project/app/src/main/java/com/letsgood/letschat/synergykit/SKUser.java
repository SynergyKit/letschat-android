package com.letsgood.letschat.synergykit;

import com.google.gson.annotations.Expose;
import com.letsgood.synergykitsdkandroid.resources.SynergykitUser;

public class SKUser extends SynergykitUser {

    @Expose
    private String name;

    @Expose
    private boolean online;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
