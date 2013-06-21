package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName="relationship")
public class TalkRelationship {

    public static final String STATE_NONE    = "none";
    public static final String STATE_FRIEND  = "friend";
    public static final String STATE_BLOCKED = "blocked";

    public static boolean isValidState(String state) {
        if(state.equals(STATE_NONE)) {
            return true;
        }
        if(state.equals(STATE_FRIEND)) {
            return true;
        }
        if(state.equals(STATE_BLOCKED)) {
            return true;
        }
        return false;
    }

    private String _id;

    @DatabaseField(generatedId = true)
    private long relationshipId;

    @DatabaseField
    String clientId;

    @DatabaseField
    String otherClientId;

    @DatabaseField
    String state;

    @DatabaseField
    Date lastChanged;

    public TalkRelationship() {
    }

    @JsonIgnore
    public boolean isRelated() {
        return !state.equals(STATE_NONE);
    }

    @JsonIgnore
    public boolean isFriend() {
        return state.equals(STATE_FRIEND);
    }

    @JsonIgnore
    public boolean isBlocked() {
        return state.equals(STATE_BLOCKED);
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

    public Date getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
    }

}
