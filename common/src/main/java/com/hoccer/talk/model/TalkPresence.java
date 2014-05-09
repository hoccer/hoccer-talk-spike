package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName="presence")
public class TalkPresence {

    public final static String CONN_STATUS_ONLINE = "online";
    public final static String CONN_STATUS_OFFLINE = "offline";

    // required for OrmLite!
    private String _id;

    @DatabaseField(id = true)
    String clientId;

    @DatabaseField
    String clientName;

    @DatabaseField
    String clientStatus;

    @DatabaseField
    Date timestamp;

    @DatabaseField
    String avatarUrl;

    @DatabaseField
    String keyId;

    @DatabaseField
    String connectionStatus;

    public TalkPresence() {
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(String clientStatus) {
        this.clientStatus = clientStatus;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        // TODO: validate connection status
        this.connectionStatus = connectionStatus;
    }

    @JsonIgnore
    public void updateWith(TalkPresence p) {
        this.setClientId(p.getClientId());
        this.setClientName(p.getClientName());
        this.setClientStatus(p.getClientStatus());
        this.setTimestamp(p.getTimestamp());
        this.setAvatarUrl(p.getAvatarUrl());
        this.setKeyId(p.getKeyId());
        this.setConnectionStatus(p.getConnectionStatus());
    }

}
