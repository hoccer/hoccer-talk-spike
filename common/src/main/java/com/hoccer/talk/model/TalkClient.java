package com.hoccer.talk.model;

public class TalkClient {
	
	/** Server-assigned client ID */
	String clientId;
	
	/** GCM registration token */
	String gcmRegistration;
	
	/** GCM android application package */
	String gcmPackage;
	
	public TalkClient(String clientId) {
		this.clientId = clientId;
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
	
}
