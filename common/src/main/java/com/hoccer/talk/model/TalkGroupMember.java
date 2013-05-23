package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

public class TalkGroupMember {

    public static final String STATE_NONE    = "none";
    public static final String STATE_INVITED = "invited";
    public static final String STATE_JOINED  = "joined";

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MEMBER = "member";

    private String _id;

    String groupId;

    String clientId;

    String role;

    String state;

    String encryptedGroupKey;

    Date lastChanged;

    public TalkGroupMember() {
        this.role = ROLE_MEMBER;
        this.state = STATE_NONE;
    }

    @JsonIgnore
    public boolean isAdmin() {
        return this.state.equals(STATE_JOINED) && this.role.equals(ROLE_ADMIN);
    }

    @JsonIgnore
    public boolean isMember() {
        return this.state.equals(STATE_JOINED) && (this.role.equals(ROLE_ADMIN) || this.role.equals(ROLE_MEMBER));
    }

    @JsonIgnore
    public boolean isJoined() {
        return this.state.equals(STATE_JOINED);
    }

    @JsonIgnore
    public boolean isInvited() {
        return this.state.equals(STATE_INVITED);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getEncryptedGroupKey() {
        return encryptedGroupKey;
    }

    public void setEncryptedGroupKey(String encryptedGroupKey) {
        this.encryptedGroupKey = encryptedGroupKey;
    }

    public Date getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
    }

}
