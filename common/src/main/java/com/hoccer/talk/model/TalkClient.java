package com.hoccer.talk.model;

public class TalkClient {
	
	/** Server-assigned client ID */
	String clientId;
	
	/** GCM registration token */
	String gcmRegistration;
	
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
	
}
