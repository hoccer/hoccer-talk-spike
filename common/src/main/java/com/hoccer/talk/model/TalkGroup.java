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
    public static final String FIELD_LAST_CHANGED     = "lastChanged";

    private String _id;

    @DatabaseField(id = true)
    String groupId;

    @DatabaseField
    String groupName;

    @DatabaseField
    String groupTag;

    @DatabaseField
    String groupAvatarUrl;

    @DatabaseField
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

    public Date getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
    }

}
