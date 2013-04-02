package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName="client")
public class TalkClient {
	
	/** Server-assigned client ID */
    @DatabaseField
	String clientId;
	
	/** GCM registration token */
    @DatabaseField
	String gcmRegistration;
	
	/** GCM android application package */
    @DatabaseField
	String gcmPackage;

    /** APNS registration token */
    @DatabaseField
    String apnsToken;

    public TalkClient() {

    }

	public TalkClient(String clientId) {
		this.clientId = clientId;
	}

    public boolean isGcmCapable() {
        return gcmPackage != null && gcmRegistration != null;
    }

    public boolean isApnsCapable() {
        return apnsToken != null;
    }

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
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

}
