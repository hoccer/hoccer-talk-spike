/**
 * Created by pavel on 21.03.14.
 */

package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
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

    public final static int LONGITUDE_INDEX = 0;
    public final static int LATITUDE_INDEX = 1;

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

    // a group tag for worldwide grouping
    @DatabaseField
    String tag;

    // preferences for group notifications
    @DatabaseField
    String notificationPreference;

    // time to live in milliseconds
    @DatabaseField
    long timeToLive;

    // server provided timestamp when releaseEnvironment has been called
    @DatabaseField
    Date timeReleased;


    public TalkEnvironment() {
    }

    @JsonIgnore
    public boolean isNearby() {
        return TYPE_NEARBY.equals(this.type);
    }

    @JsonIgnore
    public boolean isWorldwide() {
        return TYPE_WORLDWIDE.equals(this.type);
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

    @Nullable
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Nullable
    public String getNotificationPreference() {
        return notificationPreference;
    }

    public void setNotificationPreference(String notificationPreference) {
        this.notificationPreference = notificationPreference;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public Date getTimeReleased() {
        return timeReleased;
    }

    public void setTimeReleased(Date timeReleased) {
        this.timeReleased = timeReleased;
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
        this.tag = environment.tag;
        this.notificationPreference = environment.notificationPreference;
        this.timeToLive = environment.timeToLive;
        this.timeReleased = environment.timeReleased;
    }

    @JsonIgnore
    public boolean isValid() {
        if (isNearby()) {
            if (bssids != null && bssids.length > 0) {
                return true;
            }

            if (geoLocation != null && geoLocation.length == 2) {
                return true;
            }

            if (identifiers != null && identifiers.length > 0) {
                return true;
            }
        } else if (isWorldwide()) {
            return true;
        }

        return false;
    }


    // expire any environment 25 hours after it has been received
    public final static long MAX_LIFE_TIME = 25 * 60 * 60 * 1000;

    @JsonIgnore
    public boolean hasExpired() {
        long expiredMillis;
        if (timeReleased != null) {
            expiredMillis = this.getTimeReleased().getTime() + this.getTimeToLive();
        } else {
            expiredMillis = this.getTimeReceived().getTime() + MAX_LIFE_TIME;
        }
        return (expiredMillis <= new Date().getTime());
    }

    @JsonIgnore
    public boolean willLiveAfterRelease() {
        if (!this.isNearby() && this.getTimeToLive() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "TalkEnvironment{" +
                "_id='" + _id + '\'' +
                ", clientId='" + clientId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", timestamp=" + timestamp +
                ", timeReceived=" + timeReceived +
                ", locationType='" + locationType + '\'' +
                ", geoLocation=" + Arrays.toString(geoLocation) +
                ", accuracy=" + accuracy +
                ", bssids=" + Arrays.toString(bssids) +
                ", identifiers=" + Arrays.toString(identifiers) +
                ", tag='" + tag + '\'' +
                ", notificationPreference='" + notificationPreference + '\'' +
                ", timeToLive=" + timeToLive +
                ", timeReleased=" + timeReleased +
                '}';
    }
}
