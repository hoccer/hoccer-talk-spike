package com.hoccer.talk.model;

import java.util.Date;

public class TalkGroupMember {

    public static final String ROLE_NONE  = "none";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MEMBER = "member";

    private String groupId;

    private String clientId;

    private String role;

    private String status; // XXX should be named "state"

    private String invitationSecret;

    private String groupKeyCipherText; // XXX should be named "encryptedGroupKey"

    private String pubkeyId;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInvitationSecret() {
        return invitationSecret;
    }

    public void setInvitationSecret(String invitationSecret) {
        this.invitationSecret = invitationSecret;
    }

    public String getGroupKeyCipherText() {
        return groupKeyCipherText;
    }

    public void setGroupKeyCipherText(String groupKeyCipherText) {
        this.groupKeyCipherText = groupKeyCipherText;
    }

    public String getPubkeyId() {
        return pubkeyId;
    }

    public void setPubkeyId(String pubkeyId) {
        this.pubkeyId = pubkeyId;
    }

    public Date getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
    }

}
