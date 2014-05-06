package com.hoccer.talk.model;

import java.util.Date;

public class TalkServerInfo {

    Date serverTime;

    boolean supportMode;
    private String version;

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

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
