package com.hoccer.talk.model;

import java.util.Date;

public class TalkServerInfo {

    Date serverTime;

    boolean supportMode;

    public TalkServerInfo() {
    }

    public Date getServerTime() {
        return serverTime;
    }

    public void setServerTime(Date serverTime) {
        this.serverTime = serverTime;
    }

    public boolean isSupportMode() {
        return supportMode;
    }

    public void setSupportMode(boolean supportMode) {
        this.supportMode = supportMode;
    }

}
