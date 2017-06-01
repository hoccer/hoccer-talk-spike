package com.hoccer.talk.model;

import java.util.Date;

/**
 * A container for all the information transfered via the hello() rpc call
 */
public class TalkClientInfo {

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

    /** A string to control the support mode for this device; 'log' will activate additional logging on the server fpr this connection */
    String supportTag;

    /** clientCrashed is set to 1 if the client has crashed since the last connection */
    // boolean clientCrashed;

    /** hasUpdated is set to 1 if the client has been updated the last connection */
    // boolean hasUpdated;

    /** uncleanShutdown is set to 1 if last connection has been terminated with an error on the client side */
    // boolean uncleanShutdown;

    /** An optional string determining the environment, typically 'production' or 'development' */
    String environment;

    public TalkClientInfo() {
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

    public String getSupportTag() {
        return supportTag;
    }

    public void setSupportTag(String supportTag) {
        this.supportTag = supportTag;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
