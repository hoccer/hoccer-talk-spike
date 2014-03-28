package com.hoccer.talk.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "group")
public class TalkGroup {

    public static final String FIELD_GROUP_ID         = "groupId";
    public static final String FIELD_GROUP_NAME       = "groupName";
    public static final String FIELD_GROUP_TAG        = "groupTag";
    public static final String FIELD_GROUP_AVATAR_URL = "groupAvatarUrl";
    public static final String FIELD_STATE            = "state";
    public static final String FIELD_LAST_CHANGED     = "lastChanged";

    public static final String STATE_NONE   = "none";
    public static final String STATE_EXISTS = "exists";

    public static boolean isValidState(String state) {
        if(state != null) {
            if(state.equals(STATE_NONE) || state.equals(STATE_EXISTS)) {
                return true;
            }
        }
        return false;
    }

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

    public TalkGroup() {
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

}
