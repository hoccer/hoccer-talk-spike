package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;
import static com.hoccer.talk.util.Comparer.isEqual;


@DatabaseTable(tableName = "group")
public class TalkGroup {

    // TODO: define all field strings
    public static final String FIELD_GROUP_ID         = "groupId";
    public static final String FIELD_GROUP_NAME       = "groupName";
    public static final String FIELD_GROUP_TAG        = "groupTag";
    public static final String FIELD_GROUP_TYPE       = "groupType";
    public static final String FIELD_GROUP_AVATAR_URL = "groupAvatarUrl";
    public static final String FIELD_STATE            = "state";
    public static final String FIELD_LAST_CHANGED     = "lastChanged";

    public static final String STATE_NONE   = "none";
    public static final String STATE_EXISTS = "exists";
    public static final String STATE_KEPT = "kept";

    public static final String GROUP_TYPE_USER   = "user";
    public static final String GROUP_TYPE_NEARBY = "nearby";


    public static boolean isValidState(String state) {
        if(state != null) {
            if(state.equals(STATE_NONE) || state.equals(STATE_EXISTS)) {
                return true;
            }
        }
        return false;
    }

    // needed for ormlight database
    private String _id;

    @DatabaseField(columnName = FIELD_GROUP_ID, id = true)
    String groupId;

    @DatabaseField(columnName = FIELD_GROUP_NAME)
    String groupName;

    @DatabaseField(columnName = FIELD_GROUP_TAG)
    String groupTag;

    @DatabaseField(columnName = FIELD_GROUP_AVATAR_URL)
    String groupAvatarUrl;

    @DatabaseField(columnName = FIELD_STATE)
    String state;

    @DatabaseField(columnName = FIELD_LAST_CHANGED)
    Date lastChanged;

    @DatabaseField(columnName = FIELD_GROUP_TYPE)
    String groupType;

    @DatabaseField
    String sharedKeyId;

    @DatabaseField
    String sharedKeyIdSalt;

    @DatabaseField
    String keySupplier;  // ClientId of the Admin having set the current groupkey

    @DatabaseField
    Date keyDate;

    @DatabaseField
    Date groupKeyUpdateInProgress;

    @JsonIgnore
    public boolean isTypeNearby() {
        return (this.groupType != null) && this.groupType.equals(GROUP_TYPE_NEARBY);
    }

    @JsonIgnore
    public boolean isTypeUser() {
        return (this.groupType != null) && this.groupType.equals(GROUP_TYPE_USER);
    }

    @JsonIgnore
    public boolean exists() {
        return (this.state != null) && this.state.equals(STATE_EXISTS);
    }

    @JsonIgnore
    public boolean isKept() {
        return (this.state != null) && this.state.equals(STATE_KEPT);
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupTag() {
        return groupTag;
    }

    public void setGroupTag(String groupTag) {
        this.groupTag = groupTag;
    }

    public String getGroupAvatarUrl() {
        return groupAvatarUrl;
    }

    public void setGroupAvatarUrl(String groupAvatarUrl) {
        this.groupAvatarUrl = groupAvatarUrl;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        // TODO: validate state (e.g. with isValidState)
        this.state = state;
    }

    public Date getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public String getSharedKeyId() {
        return sharedKeyId;
    }

    public void setSharedKeyId(String sharedKeyId) {
        this.sharedKeyId = sharedKeyId;
    }

    public String getSharedKeyIdSalt() {
        return sharedKeyIdSalt;
    }

    public void setSharedKeyIdSalt(String sharedKeyIdSalt) {
        this.sharedKeyIdSalt = sharedKeyIdSalt;
    }

    public String getKeySupplier() {
        return keySupplier;
    }

    public void setKeySupplier(String keySupplier) {
        this.keySupplier = keySupplier;
    }

    public Date getKeyDate() {
        return keyDate;
    }

    public void setKeyDate(Date keyDate) {
        this.keyDate = keyDate;
    }

    public Date getGroupKeyUpdateInProgress() {
        return groupKeyUpdateInProgress;
    }

    public Date setGroupKeyUpdateInProgress(Date groupKeyUpdateInProgress) {
        this.groupKeyUpdateInProgress = groupKeyUpdateInProgress;
        return groupKeyUpdateInProgress;
    }

    @JsonIgnore
    public boolean updateWith(final TalkGroup other) {
        boolean updated = false;

        if(!isEqual(groupId, other.groupId)) {
            groupId = other.groupId;
            updated = true;
        }

        if(!isEqual(groupName, other.groupName)) {
            groupName = other.groupName;
            updated = true;
        }

        if(!isEqual(groupTag, other.groupTag)) {
            groupTag = other.groupTag;
            updated = true;
        }

        if(!isEqual(groupAvatarUrl, other.groupAvatarUrl)) {
            groupAvatarUrl = other.groupAvatarUrl;
            updated = true;
        }

        if(!isEqual(state, other.state)) {
            state = other.state;
            updated = true;
        }

        if(!isEqual(lastChanged, other.lastChanged)) {
            lastChanged = other.lastChanged;
            updated = true;
        }

        if(!isEqual(groupType, other.groupType)) {
            groupType = other.groupType;
            updated = true;
        }

        if(!isEqual(sharedKeyId, other.sharedKeyId)) {
            sharedKeyId = other.sharedKeyId;
            updated = true;
        }

        if(!isEqual(sharedKeyIdSalt, other.sharedKeyIdSalt)) {
            sharedKeyIdSalt = other.sharedKeyIdSalt;
            updated = true;
        }

        if(!isEqual(keySupplier, other.keySupplier)) {
            keySupplier = other.keySupplier;
            updated = true;
        }

        if(!isEqual(keyDate, other.keyDate)) {
            keyDate = other.keyDate;
            updated = true;
        }

        if(!isEqual(groupKeyUpdateInProgress, other.groupKeyUpdateInProgress)) {
            groupKeyUpdateInProgress = other.groupKeyUpdateInProgress;
            updated = true;
        }

        return updated;
    }
}
