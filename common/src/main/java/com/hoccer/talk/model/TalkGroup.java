package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

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
    public boolean updateWith(TalkGroup g) {
        boolean updated = false;

        if(!groupId.equals(g.groupId)) {
            groupId = g.groupId;
            updated = true;
        }

        if(!groupName.equals(g.groupName)) {
            groupName = g.groupName;
            updated = true;
        }

        if(!groupAvatarUrl.equals(g.groupAvatarUrl)) {
            groupAvatarUrl = g.groupAvatarUrl;
            updated = true;
        }

        if(!state.equals(g.state)) {
            state = g.state;
            updated = true;
        }

        if(!lastChanged.equals(g.lastChanged)) {
            lastChanged = g.lastChanged;
            updated = true;
        }

        if(!groupType.equals(g.groupType)) {
            groupType = g.groupType;
            updated = true;
        }

        if(!sharedKeyId.equals(g.sharedKeyId)) {
            sharedKeyId = g.sharedKeyId;
            updated = true;
        }

        if(!sharedKeyIdSalt.equals(g.sharedKeyIdSalt)) {
            sharedKeyIdSalt = g.sharedKeyIdSalt;
            updated = true;
        }

        if(!keySupplier.equals(g.keySupplier)) {
            keySupplier = g.keySupplier;
            updated = true;
        }

        if(!keyDate.equals(g.keyDate)) {
            keyDate = g.keyDate;
            updated = true;
        }

        if(!groupKeyUpdateInProgress.equals(g.groupKeyUpdateInProgress)) {
            groupKeyUpdateInProgress = g.groupKeyUpdateInProgress;
            updated = true;
        }

        return updated;
    }
}
