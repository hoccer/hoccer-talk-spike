package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName="client")
public class TalkClient {

    private String _id;

	/** Server-assigned client ID */
    @DatabaseField
	String clientId;

    /** SRP salt */
    String srpSalt;

    /** SRP verifier */
    String srpVerifier;
	
	/** GCM registration token */
    @DatabaseField
	String gcmRegistration;
	
	/** GCM android application package */
    @DatabaseField
	String gcmPackage;

    /** APNS registration token */
    @DatabaseField
    String apnsToken;

    /** APNS unread message count */
    @DatabaseField
    int apnsUnreadMessages;

    public TalkClient() {

    }

	public TalkClient(String clientId) {
		this.clientId = clientId;
	}


    @JsonIgnore
    public boolean isPushCapable() {
        return isGcmCapable() || isApnsCapable();
    }

    @JsonIgnore
    public boolean isGcmCapable() {
        return gcmPackage != null && gcmRegistration != null;
    }

    @JsonIgnore
    public boolean isApnsCapable() {
        return apnsToken != null;
    }


	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

    public String getSrpSalt() {
        return srpSalt;
    }

    public void setSrpSalt(String srpSalt) {
        this.srpSalt = srpSalt;
    }

    public String getSrpVerifier() {
        return srpVerifier;
    }

    public void setSrpVerifier(String srpVerifier) {
        this.srpVerifier = srpVerifier;
    }

    public String getGcmRegistration() {
		return gcmRegistration;
	}

	public void setGcmRegistration(String gcmRegistration) {
		this.gcmRegistration = gcmRegistration;
	}

	public String getGcmPackage() {
		return gcmPackage;
	}

	public void setGcmPackage(String gcmPackage) {
		this.gcmPackage = gcmPackage;
	}

    public String getApnsToken() {
        return apnsToken;
    }

    public void setApnsToken(String apnsToken) {
        this.apnsToken = apnsToken;
    }

    public int getApnsUnreadMessages() {
        return apnsUnreadMessages;
    }

    public void setApnsUnreadMessages(int apnsUnreadMessages) {
        this.apnsUnreadMessages = apnsUnreadMessages;
    }
}
