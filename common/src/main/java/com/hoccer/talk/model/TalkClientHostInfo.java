package com.hoccer.talk.model;

import java.util.Date;

/**
 * A server-side container for all the information transfered via the hello() rpc call
 */
public class TalkClientHostInfo {

    /**
     * unique object ID for the database, never transfered
     */
    private String _id;

    /** the time on the server whenever this information was received */
    Date serverTime;

    /** the time on the client when the hello call was made */
    Date clientTime;

    /** The name of the client program; on iOS it is the name of the app 'Hoccer XO'*/
    String clientName;

    /** A version string for the client program */
    String clientVersion;

    /** A build number of the client program; on iOS it is the build number */
    int clientBuildNumber;

    /** The build variant. Currently valid values: [debug|release] */
    String clientBuildVariant;

    /** A ISO 639-1 language identifier for the current client UI language, e.g. 'de' for german */
    String clientLanguage;

    /** Name of the operating system, e.g. 'iPhone OS' */
    String systemName;

    /** Version of the operating system, e.g. '7.0.4' */
    String systemVersion;

    /** A ISO 639-1 language identifier for the current system language , e.g. 'de' for german */
    String systemLanguage;

    /** A hardware device name identifier, e.g. 'iPhone6,1' */
    String deviceModel;

    /** An optional string determining the environment, typically 'production' or 'development' */
    String environment;

    /** clientCrashed is set to 1 if the client has crashed since the last connection */
    // boolean clientCrashed;

    /** hasUpdated is set to 1 if the client has been updated the last connection */
    // boolean hasUpdated;

    /** uncleanShutdown is set to 1 if last connection has been terminated with an error on the client side */
    // boolean uncleanShutdown;

    String clientId;

    public TalkClientHostInfo() {
    }

    public TalkClientHostInfo(TalkClientInfo clientInfo) {
        this.clientBuildNumber = clientInfo.getClientBuildNumber();
        this.clientBuildVariant = clientInfo.getClientBuildVariant();
        this.clientLanguage    = clientInfo.getClientLanguage();
        this.clientName        = clientInfo.getClientName();
        this.clientTime        = clientInfo.getClientTime();
        this.clientVersion     = clientInfo.getClientVersion();
        this.systemVersion     = clientInfo.getSystemVersion();
        this.systemLanguage    = clientInfo.getSystemLanguage();
        this.systemName        = clientInfo.getSystemName();
        this.environment = clientInfo.getEnvironment();
    }

    public String info() {
        return clientName+"-"+clientVersion+"/"+systemName+"-"+systemVersion+"-"+systemLanguage+"/"+deviceModel;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Date getClientTime() {
        return clientTime;
    }

    public void setClientTime(Date clientTime) {
        this.clientTime = clientTime;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public int getClientBuildNumber() {
        return clientBuildNumber;
    }

    public void setClientBuildNumber(int clientBuildNumber) {
        this.clientBuildNumber = clientBuildNumber;
    }

    public String getClientBuildVariant() {
        return clientBuildVariant;
    }

    public void setClientBuildVariant(String clientBuildVariant) {
        this.clientBuildVariant = clientBuildVariant;
    }

    public String getClientLanguage() {
        return clientLanguage;
    }

    public void setClientLanguage(String clientLanguage) {
        this.clientLanguage = clientLanguage;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemVersion() {
        return systemVersion;
    }

    public void setSystemVersion(String systemVersion) {
        this.systemVersion = systemVersion;
    }

    public String getSystemLanguage() {
        return systemLanguage;
    }

    public void setSystemLanguage(String systemLanguage) {
        this.systemLanguage = systemLanguage;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public Date getServerTime() {
        return serverTime;
    }

    public void setServerTime(Date serverTime) {
        this.serverTime = serverTime;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void updateWith(TalkClientInfo clientInfo) {
        this.clientBuildNumber = clientInfo.getClientBuildNumber();
        this.clientBuildVariant = clientInfo.getClientBuildVariant();
        this.clientLanguage = clientInfo.getClientLanguage();
        this.clientName = clientInfo.getClientName();
        this.clientTime = clientInfo.getClientTime();
        this.clientVersion = clientInfo.getClientVersion();
        this.deviceModel = clientInfo.getDeviceModel();
        this.systemLanguage = clientInfo.getSystemLanguage();
        this.systemName = clientInfo.getSystemName();
        this.systemVersion = clientInfo.getSystemVersion();
        this.environment = clientInfo.getEnvironment();
    }
}
