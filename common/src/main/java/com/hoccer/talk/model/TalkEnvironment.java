/**
 * Created by pavel on 21.03.14.
 */

package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.jetbrains.annotations.Nullable;

import java.util.Date;




@DatabaseTable(tableName="environment")
public class TalkEnvironment {

    public final static String LOCATION_TYPE_GPS = "gps";         // location from gps
    public final static String LOCATION_TYPE_WIFI = "wifi";       // location from wifi triangulation
    public final static String LOCATION_TYPE_NETWORK = "network"; // location provided by cellular network (cell tower)
    public final static String LOCATION_TYPE_MANUAL = "manual";   // location was set by user
    public final static String LOCATION_TYPE_OTHER = "other";
    public final static String LOCATION_TYPE_NONE = "none";       // indicates that location is invalid

    public final static String TYPE_NEARBY = "nearby";            // a nearby grouping environment
    public final static String TYPE_WORLDWIDE = "worldwide";      // a worldwide grouping environment

    private String _id;

    // id of the sending client
    @DatabaseField(id = true)
    String clientId;

    // optional group the location is associated with
    @DatabaseField
    String groupId;

    // type of the environment; only one environment of each type can exist on the server per client
    @DatabaseField
    String type;

    // some name that can be set by the client and may or maybe not used by the server
    @DatabaseField
    String name;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Nullable
    public String[] getBssids() {
        return bssids;
    }

    public void setBssids(String[] bssids) {
        this.bssids = bssids;
    }

    @Nullable
    public String[] getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(String[] identifiers) {
        this.identifiers = identifiers;
    }

    @Nullable
    public Double[] getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(Double[] geoLocation) {
        this.geoLocation = geoLocation;
    }

    public void updateWith(TalkEnvironment environment) {
        this.type = environment.type;
        this.name = environment.name;
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

    @JsonIgnore
    public boolean isValid() {
        if (bssids != null && bssids.length > 0) {
            return true;
        }

        if (geoLocation != null && geoLocation.length == 2) {
            return true;
        }

        if (identifiers != null && identifiers.length > 0) {
            return true;
        }

        return false;
    }
}
