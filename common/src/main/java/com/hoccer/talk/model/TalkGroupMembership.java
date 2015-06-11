package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "groupMembership")
public class TalkGroupMembership {

    public static final String STATE_NONE = "none";
    public static final String STATE_INVITED = "invited";
    public static final String STATE_JOINED = "joined";
    public static final String STATE_GROUP_REMOVED = "groupRemoved";
    public static final String STATE_SUSPENDED = "suspended";

    public static final String ROLE_NONE = "none";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MEMBER = "member";
    public static final String ROLE_NEARBY_MEMBER = "nearbyMember";
    public static final String ROLE_WORLDWIDE_MEMBER = "worldwideMember";

    public static final String NOTIFICATIONS_DISABLED = "disabled";
    public static final String NOTIFICATIONS_ENABLED = "enabled";

    public static final String LOCK_PREFIX = "mbr-";

    public static boolean isValidRoleForGroupType(String role, String type) {
        if (TalkGroupPresence.GROUP_TYPE_USER.equals(type)) {
            return ROLE_ADMIN.equals(role) || ROLE_MEMBER.equals(role);
        } else if (TalkGroupPresence.GROUP_TYPE_NEARBY.equals(type)) {
            return ROLE_NEARBY_MEMBER.equals(role);
        } else if (TalkGroupPresence.GROUP_TYPE_WORLDWIDE.equals(type)) {
            return ROLE_WORLDWIDE_MEMBER.equals(role);
        } else {
            return false;
        }
    }
    public static boolean isValidNotificationPreference(String preference) {
        return NOTIFICATIONS_DISABLED.equals(preference) || NOTIFICATIONS_ENABLED.equals(preference);
    }

    public static final String[] ACTIVE_STATES = {
            TalkGroupMembership.STATE_INVITED,
            TalkGroupMembership.STATE_JOINED
    };

    // needed for ormlight database
    private String _id;

    @DatabaseField(generatedId = true)
    private long memberId;

    @DatabaseField
    String groupId;

    @DatabaseField
    String clientId;

    @DatabaseField
    String role;

    @DatabaseField
    String state;

    @DatabaseField
    String memberKeyId;

    @DatabaseField
    String encryptedGroupKey;

    @DatabaseField
    String sharedKeyId;

    @DatabaseField
    String sharedKeyIdSalt;

    @DatabaseField
    Date sharedKeyDate;

    @DatabaseField
    String keySupplier;

    @DatabaseField
    Date lastChanged;

    @DatabaseField
    String notificationPreference;

    public TalkGroupMembership() {
        this.role = ROLE_MEMBER;
        this.state = STATE_NONE;
    }

    @JsonIgnore
    public boolean isAdmin() {
        return isJoined() && ROLE_ADMIN.equals(this.role);
    }

    @JsonIgnore
    public boolean isNearby() {
        return isJoined() && ROLE_NEARBY_MEMBER.equals(this.role);
    }
    @JsonIgnore
    public boolean isWorldwide() {
        return (isJoined() || isSuspended()) && ROLE_WORLDWIDE_MEMBER.equals(this.role);
    }

    @JsonIgnore
    public boolean isMember() {
        return isJoined() &&
                (ROLE_MEMBER.equals(this.role) ||
                ROLE_NEARBY_MEMBER.equals(this.role) ||
                ROLE_WORLDWIDE_MEMBER.equals(this.role) ||
                ROLE_ADMIN.equals(this.role));
    }

    @JsonIgnore
    public boolean isJoined() {
        return STATE_JOINED.equals(this.state);
    }

    @JsonIgnore
    public boolean isInvited() {
        return STATE_INVITED.equals(this.state);
    }

    @JsonIgnore
    public boolean isSuspended() {
        return STATE_SUSPENDED.equals(this.state);
    }

    @JsonIgnore
    public boolean isGroupRemoved() {
        return STATE_GROUP_REMOVED.equals(this.state);
    }
    @JsonIgnore
    public boolean isNone() {
        return STATE_NONE.equals(this.state);
    }


    @JsonIgnore
    public boolean isInvolved() {
        return !isNone() && !isGroupRemoved();
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        // TODO: validate state
        this.state = state;
    }

    public String getMemberKeyId() {
        return memberKeyId;
    }

    public void setMemberKeyId(String memberKeyId) {
        this.memberKeyId = memberKeyId;
    }

    public String getEncryptedGroupKey() {
        return encryptedGroupKey;
    }

    public void setEncryptedGroupKey(String encryptedGroupKey) {
        this.encryptedGroupKey = encryptedGroupKey;
    }

    public Date getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
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

    public Date getSharedKeyDate() {
        return sharedKeyDate;
    }

    public void setSharedKeyDate(Date sharedKeyDate) {
        this.sharedKeyDate = sharedKeyDate;
    }

    public String getNotificationPreference() {
        return notificationPreference;
    }

    public void setNotificationPreference(String notificationPreference) {
        this.notificationPreference = notificationPreference;
    }

    @JsonIgnore
    public void updateWith(TalkGroupMembership membership) {
        this.setClientId(membership.getClientId());
        this.setGroupId(membership.getGroupId());
        this.setRole(membership.getRole());
        this.setState(membership.getState());
        this.setMemberKeyId(membership.getMemberKeyId());
        this.setEncryptedGroupKey(membership.getEncryptedGroupKey());
        this.setLastChanged(membership.getLastChanged());
        this.setSharedKeyId(membership.getSharedKeyId());
        this.setSharedKeyIdSalt(membership.getSharedKeyIdSalt());
        this.setKeySupplier(membership.getKeySupplier());
        this.setSharedKeyDate(membership.getSharedKeyDate());
        this.setNotificationPreference(membership.getNotificationPreference());
    }

    // only copies the field where a foreign member is interested in
    @JsonIgnore
    public void foreignUpdateWith(TalkGroupMembership membership) {
        this.setClientId(membership.getClientId());
        this.setGroupId(membership.getGroupId());
        this.setRole(membership.getRole());
        this.setState(membership.getState());
        this.setLastChanged(membership.getLastChanged());
    }

    @JsonIgnore
    public void trashPrivate() {
        this.setSharedKeyDate(null);
        this.setSharedKeyIdSalt(null);
        this.setMemberKeyId(null);
        this.setSharedKeyId(null);
        this.setEncryptedGroupKey(null);
        this.setKeySupplier(null);
        this.setNotificationPreference(null);
    }

    @JsonIgnore
    public boolean isNotificationsDisabled() {
        return NOTIFICATIONS_DISABLED.equals(notificationPreference);
    }
}
