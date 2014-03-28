/**
 * Created by pavel on 21.03.14.
 */

package com.hoccer.talk.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;




@DatabaseTable(tableName="environment")
public class TalkEnvironment {

    public final static String LOCATION_TYPE_GPS = "gps";         // location from gps
    public final static String LOCATION_TYPE_WIFI = "wifi";       // location from wifi triangulation
    public final static String LOCATION_TYPE_NETWORK = "network"; // location provided by cellular network (cell tower)
    public final static String LOCATION_TYPE_MANUAL = "manual";   // location was set by user
    public final static String LOCATION_TYPE_OTHER = "other";
    public final static String LOCATION_TYPE_NONE = "none";       // indicates that location is invalid

    private String _id;

    // id of the sending client
    @DatabaseField(id = true)
    String clientId;

    // optional group the location is associated with
    @DatabaseField
    String groupId;

    // client provided timestamp
    @DatabaseField
    Date timestamp;

    // server provided timestamp
    @DatabaseField
    Date timeReceived;

    // indicates what was used on the client to determine the location
    @DatabaseField
    String locationType;

    // longitude and latitude (in this order!)
    @DatabaseField
    Double[] geoLocation;

    // accuracy of the location in meters; set to 0 if accuracy not available
    @DatabaseField
    Float accuracy;

    // bssids in the vicinity of the client
    @DatabaseField
    String[] bssids;

    // possible other location identifiers
    @DatabaseField
    String[] identifiers;

    public TalkEnvironment() {
    }


    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimeReceived() {
        return timeReceived;
    }

    public void setTimeReceived(Date timeReceived) {
        this.timeReceived = timeReceived;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        // TODO: validate location Type
        this.locationType = locationType;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public String[] getBssids() {
        return bssids;
    }

    public void setBssids(String[] bssids) {
        this.bssids = bssids;
    }

    public String[] getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(String[] identifiers) {
        this.identifiers = identifiers;
    }

    public Double[] getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(Double[] geoLocation) {
        this.geoLocation = geoLocation;
    }

    public void updateWith(TalkEnvironment environment) {
        this.clientId = environment.clientId;
        this.groupId = environment.groupId;
        this.timestamp = environment.timestamp;
        this.timeReceived = environment.timeReceived;
        this.locationType = environment.locationType;
        this.geoLocation = environment.getGeoLocation();
        this.accuracy = environment.accuracy;
        this.bssids = environment.bssids;
        this.identifiers = environment.identifiers;
    }
}
