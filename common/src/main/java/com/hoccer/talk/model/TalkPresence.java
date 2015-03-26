package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@DatabaseTable(tableName = "presence")
public class TalkPresence {

    public final static String STATUS_OFFLINE = "offline";
    public final static String STATUS_BACKGROUND = "background";
    public final static String STATUS_ONLINE = "online";
    public final static String STATUS_TYPING = "typing";

    public final static String FIELD_CLIENT_ID = "clientId";
    public final static String FIELD_CLIENT_NAME = "clientName";
    public final static String FIELD_CLIENT_STATUS = "clientStatus";
    public final static String FIELD_TIMESTAMP = "timestamp";
    public final static String FIELD_AVATAR_URL = "avatarUrl";
    public final static String FIELD_KEY_ID = "keyId";
    public final static String FIELD_CONNECTION_STATUS = "connectionStatus";

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

    @JsonIgnore
    public boolean isOffline() {
        return (connectionStatus == null || (connectionStatus != null && connectionStatus.equals(STATUS_OFFLINE)));
    }

    @JsonIgnore
    public boolean isBackground() {
        return (connectionStatus != null && connectionStatus.equals(STATUS_BACKGROUND));
    }

    @JsonIgnore
    public boolean isOnline() {
        return (connectionStatus != null && connectionStatus.equals(STATUS_ONLINE));
    }

    @JsonIgnore
    public boolean isTyping() {
        return (connectionStatus != null && connectionStatus.equals(STATUS_TYPING));
    }

    @JsonIgnore
    public boolean isPresent() {
        return (isOnline() || isTyping());
    }

    @JsonIgnore
    public boolean isConnected() {
        return isPresent() || isBackground();
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

    @JsonIgnore
    public void updateWith(TalkPresence p, Set<String> fields) {
        if (fields == null || fields.contains(TalkPresence.FIELD_CLIENT_ID)) {
            this.setClientId(p.getClientId());
        }
        if (fields == null || fields.contains(TalkPresence.FIELD_CLIENT_NAME)) {
            this.setClientName(p.getClientName());
        }
        if (fields == null || fields.contains(TalkPresence.FIELD_CLIENT_STATUS)) {
            this.setClientStatus(p.getClientStatus());
        }
        if (fields == null || fields.contains(TalkPresence.FIELD_TIMESTAMP)) {
            this.setTimestamp(p.getTimestamp());
        }
        if (fields == null || fields.contains(TalkPresence.FIELD_AVATAR_URL)) {
            this.setAvatarUrl(p.getAvatarUrl());
        }
        if (fields == null || fields.contains(TalkPresence.FIELD_KEY_ID)) {
            this.setKeyId(p.getKeyId());
        }
        if (fields == null || fields.contains(TalkPresence.FIELD_CONNECTION_STATUS)) {
            this.setConnectionStatus(p.getConnectionStatus());
        }
    }

    @JsonIgnore
    public Set<String> nonNullFields() {
        Set<String> result = new HashSet<String>();
        if (clientId != null) {
            result.add(TalkPresence.FIELD_CLIENT_ID);
        }
        if (clientName != null) {
            result.add(TalkPresence.FIELD_CLIENT_NAME);
        }
        if (clientStatus != null) {
            result.add(TalkPresence.FIELD_CLIENT_STATUS);
        }
        if (timestamp != null) {
            result.add(TalkPresence.FIELD_TIMESTAMP);
        }
        if (avatarUrl != null) {
            result.add(TalkPresence.FIELD_AVATAR_URL);
        }
        if (keyId != null) {
            result.add(TalkPresence.FIELD_KEY_ID);
        }
        if (connectionStatus != null) {
            result.add(TalkPresence.FIELD_CONNECTION_STATUS);
        }
        return result;
    }

}
