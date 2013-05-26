package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * These objects represent clients in the server database
 *
 * They contain authentication tokens and push registration data.
 *
 * The client never encounters these objects.
 */
@DatabaseTable(tableName="client")
public class TalkClient {

    private String _id;

	/** Server-assigned client ID */
    @DatabaseField(id = true)
	String clientId;

    /** SRP salt */
    @DatabaseField
    String srpSalt;

    /** SRP verifier */
    @DatabaseField(width = 512)
    String srpVerifier;

    /** SRP secret (CLIENT ONLY) */
    @DatabaseField
    String srpSecret;
	
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

    @DatabaseField
    Date timeRegistered;

    @DatabaseField
    Date timeLastLogin;

    @DatabaseField
    Date timeLastPush;

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

    public String getSrpSecret() {
        return srpSecret;
    }

    public void setSrpSecret(String srpSecret) {
        this.srpSecret = srpSecret;
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

    public Date getTimeRegistered() {
        return timeRegistered;
    }

    public void setTimeRegistered(Date timeRegistered) {
        this.timeRegistered = timeRegistered;
    }

    public Date getTimeLastLogin() {
        return timeLastLogin;
    }

    public void setTimeLastLogin(Date timeLastLogin) {
        this.timeLastLogin = timeLastLogin;
    }

    public Date getTimeLastPush() {
        return timeLastPush;
    }

    public void setTimeLastPush(Date timeLastPush) {
        this.timeLastPush = timeLastPush;
    }
}
