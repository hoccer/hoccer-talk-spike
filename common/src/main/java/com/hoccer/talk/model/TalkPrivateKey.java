package com.hoccer.talk.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "privateKey")
public class TalkPrivateKey {

    public static final String FIELD_CLIENT_ID = "clientId";
    public static final String FIELD_KEY_ID = "keyId";
    public static final String FIELD_KEY = "key";
    public static final String FIELD_TIMESTAMP = "timestamp";

    @DatabaseField(columnName = FIELD_CLIENT_ID)
    private String clientId;

    @DatabaseField(columnName = FIELD_KEY_ID)
    private String keyId;

    @DatabaseField(columnName = FIELD_KEY, width = 1024)
    private String key;

    @DatabaseField(columnName = FIELD_TIMESTAMP)
    private Date timestamp;

    public TalkPrivateKey() {
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
