package com.hoccer.talk.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TalkServerInfo {

    Date serverTime;

    boolean supportMode;
    String version;
    String commitId;
    List<String> protocolVersions = new ArrayList<String>();

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

    public String getCommitId() {
        return commitId;
    }
    public void setCommitId(String pCommitId) {
        commitId = pCommitId;
    }

    public List<String> getProtocolVersions() {
        return protocolVersions;
    }
    public void addProtocolVersion(String pProtocolVersion) {
        protocolVersions.add(pProtocolVersion);
    }
}
