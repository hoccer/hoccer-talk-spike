package com.hoccer.talk.model;

import java.util.Date;

public class TalkGroupMember {

    public static final String ROLE_NONE  = "none";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MEMBER = "member";

    private String groupId;

    private String clientId;

    private String role;

    private String state;

    private String invitationSecret;

    private String encryptedGroupKey;

    private Date lastChanged;

    public TalkGroupMember() {

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

    public String getInvitationSecret() {
        return invitationSecret;
    }

    public void setInvitationSecret(String invitationSecret) {
        this.invitationSecret = invitationSecret;
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
