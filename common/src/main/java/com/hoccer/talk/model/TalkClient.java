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

    public static final String FIELD_CLIENT_ID            = "clientId";
    public static final String FIELD_SRP_SALT             = "srpSalt";
    public static final String FIELD_SRP_VERIFIER         = "srpVerifier";
    public static final String FIELD_SRP_SAVED_VERIFIER   = "srpSavedVerifier";
    public static final String FIELD_SRP_SECRET           = "srpSecret";
    public static final String FIELD_GCM_REGISTRATION     = "gcmRegistration";
    public static final String FIELD_GCM_PACKAGE          = "gcmPackage";
    public static final String FIELD_APNS_TOKEN           = "apnsToken";
    public static final String FIELD_APNS_UNREAD_MESSAGES = "apnsUnreadMessages";
    public static final String FIELD_TIME_REGISTERED      = "timeRegistered";
    public static final String FIELD_TIME_LAST_LOGIN      = "timeLastLogin";
    public static final String FIELD_TIME_LAST_DISCONNECT = "timeLastDisconnect";
    public static final String FIELD_TIME_LAST_PUSH       = "timeLastPush";
    public static final String FIELD_TIME_READY           = "timeReady";
    public static final String FIELD_TIME_DELETED         = "timeDeleted";
    public static final String FIELD_REASON_DELETED       = "reasonDeleted";
    public static final String FIELD_LAST_PUSH_MESSAGE    = "lastPushMessage";
    public static final String FIELD_LATEST_PUSH_MESSAGE_TIME  = "latestPushMessageTime";
    public static final String FIELD_PUSH_ALERT_MESSAGE   = "pushAlertMessage";
    public static final String FIELD_APNS_MODE            = "apnsMode";
    public static final String FIELD_PUSH_RETRY_COUNT     = "pushRetryCount";
    public static final String FIELD_RESCUE_CODE          = "rescueCode";
    public static final String FIELD_RESCUE_CODE_FAILS    = "rescueCodeFails";
    public static final String FIELD_TIME_SUSPENDED       = "timeSuspended";
    public static final String FIELD_DURATION_SUSPENDED   = "durationSuspended";

    public static final String APNS_MODE_DEFAULT          = "default";
    public static final String APNS_MODE_BACKGROUND       = "background";
    public static final String APNS_MODE_DIRECT           = "direct";

    /** Object ID for jongo */
    private String _id;

	/** Server-assigned client ID */
    @DatabaseField(columnName = FIELD_CLIENT_ID, id = true)
	String clientId;

    /** SRP salt */
    @DatabaseField(columnName = FIELD_SRP_SALT)
    String srpSalt;

    /** SRP verifier */
    @DatabaseField(columnName = FIELD_SRP_VERIFIER, width = 512)
    String srpVerifier;

    /** saved SRP verifier after deletion*/
    @DatabaseField(columnName = FIELD_SRP_SAVED_VERIFIER, width = 512)
    String srpSavedVerifier;

    /** SRP secret (CLIENT ONLY) */ // XXX needed?
    @DatabaseField(columnName = FIELD_SRP_SECRET, canBeNull = true)
    String srpSecret;
	
	/** GCM registration token */
    @DatabaseField(columnName = FIELD_GCM_REGISTRATION, canBeNull = true)
	String gcmRegistration;
	
	/** GCM android application package */
    @DatabaseField(columnName = FIELD_GCM_PACKAGE, canBeNull = true)
	String gcmPackage;

    /** APNS registration token */
    @DatabaseField(columnName = FIELD_APNS_TOKEN, canBeNull = true)
    String apnsToken;

    /** APNS unread message count */
    @DatabaseField(columnName = FIELD_APNS_UNREAD_MESSAGES)
    int apnsUnreadMessages;

    /** push retry count */
    @DatabaseField(columnName = FIELD_PUSH_RETRY_COUNT)
    int pushRetryCount;

    /** Time of registration */
    @DatabaseField(columnName = FIELD_TIME_REGISTERED, canBeNull = false)
    Date timeRegistered;

    /** Time of last login */
    @DatabaseField(columnName = FIELD_TIME_LAST_LOGIN, canBeNull = true)
    Date timeLastLogin;

    /** Time of last disconnect */
    @DatabaseField(columnName = FIELD_TIME_LAST_DISCONNECT, canBeNull = true)
    Date timeLastDisconnect;

    /** Time of last push notification */
    @DatabaseField(columnName = FIELD_TIME_LAST_PUSH, canBeNull = true)
    Date timeLastPush;

    /** Time of last ready call received */
    @DatabaseField(columnName = FIELD_TIME_READY, canBeNull = true)
    Date timeReady;

    /** Time of account deletion */
    @DatabaseField(columnName = FIELD_TIME_DELETED, canBeNull = true)
    Date timeDeleted;

    /** Time of account deletion */
    @DatabaseField(columnName = FIELD_REASON_DELETED, canBeNull = true)
    String reasonDeleted;

    /** Some identifier that describes the content of the last message, typically number of out outstanding messages */
    @DatabaseField(columnName = FIELD_LAST_PUSH_MESSAGE, canBeNull = true)
    String lastPushMessage;

    /** Next push message to be displayed to the user when connected */
    @DatabaseField(columnName = FIELD_PUSH_ALERT_MESSAGE, canBeNull = true)
    String pushAlertMessage;

    /** Next push message to be displayed to the user when connected */
    @DatabaseField(columnName = FIELD_APNS_MODE, canBeNull = true)
    String apnsMode;

    /** Next push message to be displayed to the user when connected */
    @DatabaseField(columnName = FIELD_RESCUE_CODE, canBeNull = true)
    String rescueCode;

    /** Failed rescue counts */
    @DatabaseField(columnName = FIELD_RESCUE_CODE_FAILS)
    int rescueCodeFails;

    /** Time of account suspension */
    @DatabaseField(columnName = FIELD_TIME_SUSPENDED, canBeNull = true)
    Date timeSuspended;

    /** Duration of account suspension */
    @DatabaseField(columnName = FIELD_DURATION_SUSPENDED, canBeNull = true)
    long durationSuspended;

    /** Time of last push notification */
    @DatabaseField(columnName = FIELD_LATEST_PUSH_MESSAGE_TIME, canBeNull = true)
    Date latestPushMessageTime;

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

    @JsonIgnore
    public boolean isReady() {
        return timeReady != null && timeLastLogin != null && timeReady.getTime() > timeLastLogin.getTime();
    }

    @JsonIgnore
    public boolean isConnected() {
        return timeLastLogin != null && (timeLastDisconnect == null || timeLastLogin.getTime() > timeLastDisconnect.getTime());
    }

    @JsonIgnore
    public boolean isSuspended(Date when) {
        return timeSuspended != null && (timeSuspended.getTime() + durationSuspended > when.getTime());
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

    public Date getTimeLastDisconnect() {
        return timeLastDisconnect;
    }

    public void setTimeLastDisconnect(Date timeLastDisconnect) {
        this.timeLastDisconnect = timeLastDisconnect;
    }

    public Date getTimeLastPush() {
        return timeLastPush;
    }

    public void setTimeLastPush(Date timeLastPush) {
        this.timeLastPush = timeLastPush;
    }

    public Date getTimeReady() {
        return timeReady;
    }

    public void setTimeReady(Date timeReady) {
        this.timeReady = timeReady;
    }

    public Date getTimeDeleted() {
        return timeDeleted;
    }

    public String getReasonDeleted() {
        return reasonDeleted;
    }

    public void setReasonDeleted(String reasonDeleted) {
        this.reasonDeleted = reasonDeleted;
    }

    public void setTimeDeleted(Date timeDeleted) {
        this.timeDeleted = timeDeleted;
    }

    public String getLastPushMessage() {
        return lastPushMessage;
    }

    public void setLastPushMessage(String lastPushMessage) {
        this.lastPushMessage = lastPushMessage;
    }

    public String getPushAlertMessage() {
        return pushAlertMessage;
    }

    public void setPushAlertMessage(String pushAlertMessage) {
        this.pushAlertMessage = pushAlertMessage;
    }

    public String getApnsMode() {
        return apnsMode;
    }

    public void setApnsMode(String apnsMode) {
        this.apnsMode = apnsMode;
    }

    public int getPushRetryCount() {
        return pushRetryCount;
    }

    public void setPushRetryCount(int pushRetryCount) {
        this.pushRetryCount = pushRetryCount;
    }

    public String getRescueCode() {
        return rescueCode;
    }

    public void setRescueCode(String rescueCode) {
        this.rescueCode = rescueCode;
    }

    public int getRescueCodeFails() {
        return rescueCodeFails;
    }

    public void setRescueCodeFails(int rescueCodeFails) {
        this.rescueCodeFails = rescueCodeFails;
    }

    public Date getTimeSuspended() {
        return timeSuspended;
    }

    public void setTimeSuspended(Date timeSuspended) {
        this.timeSuspended = timeSuspended;
    }

    public long getDurationSuspended() {
        return durationSuspended;
    }

    public void setDurationSuspended(long durationSuspended) {
        this.durationSuspended = durationSuspended;
    }

    public String getSrpSavedVerifier() {
        return srpSavedVerifier;
    }

    public void setSrpSavedVerifier(String srpSavedVerifier) {
        this.srpSavedVerifier = srpSavedVerifier;
    }

    public Date getLatestPushMessageTime() {
        return latestPushMessageTime;
    }

    public void setLatestPushMessageTime(Date latestPushMessageTime) {
        this.latestPushMessageTime = latestPushMessageTime;
    }
}
