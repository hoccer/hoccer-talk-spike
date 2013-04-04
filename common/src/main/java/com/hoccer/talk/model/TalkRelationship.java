package com.hoccer.talk.model;

public class TalkRelationship {

    public static final String STATE_NONE    = "none";
    public static final String STATE_FRIEND  = "friend";
    public static final String STATE_BLOCKED = "blocked";

    private String _id;

    String clientId;

    String otherClientId;

    String state;

    public TalkRelationship() {
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getOtherClientId() {
        return otherClientId;
    }

    public void setOtherClientId(String otherClientId) {
        this.otherClientId = otherClientId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

}
