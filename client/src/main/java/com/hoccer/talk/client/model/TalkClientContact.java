package com.hoccer.talk.client.model;

import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.model.*;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * These represent a target of communication
 * This may currently be either a groupPresence or another user.
 */
@DatabaseTable(tableName = "clientContact")
public class TalkClientContact implements Serializable {

    public @interface ClientMethodOnly {}

    public @interface ClientOrGroupMethodOnly {}

    public @interface ClientOrSelfMethodOnly {}

    public @interface GroupMethodOnly {}

    public @interface SelfMethodOnly {}

    public static final String TYPE_SELF = "self";

    public static final String TYPE_CLIENT = "client";
    public static final String TYPE_GROUP = "group";
    @DatabaseField(generatedId = true)
    private int clientContactId;

    @DatabaseField
    private String contactType;

    @Deprecated @DatabaseField
    private boolean deleted;

    @Deprecated @DatabaseField
    private boolean everRelated;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkKey publicKey;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkPrivateKey privateKey;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkClientSelf self;

    @DatabaseField(canBeNull = true)
    private String clientId;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkPresence clientPresence;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkRelationship clientRelationship;

    @DatabaseField(canBeNull = true)
    private String groupId;

    @DatabaseField(canBeNull = true)
    private String groupTag;

    @DatabaseField(canBeNull = true)
    private String groupKey;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkGroupPresence groupPresence;

    // when contact is a group, groupMembership is the own member description
    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true, columnName = "groupMember_id")
    private TalkGroupMembership groupMembership;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkClientDownload avatarDownload;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkClientUpload avatarUpload;

    @DatabaseField
    private boolean isNearby;

    @DatabaseField
    private boolean worldwide;

    @DatabaseField
    private String nickname;

    @DatabaseField(canBeNull = true)
    private Date createdTimeStamp;

    public TalkClientContact() {
    }

    public TalkClientContact(String contactType) {
        this.contactType = contactType;
    }

    public TalkClientContact(String contactType, String id) {
        this(contactType);
        if (contactType.equals(TYPE_CLIENT) || contactType.equals(TYPE_SELF)) {
            this.clientId = id;
        }
        if (contactType.equals(TYPE_GROUP)) {
            this.groupId = id;
        }
    }

    @SelfMethodOnly
    public boolean isEditable() {
        return isSelf() || isGroupAdmin();
    }

    public int getClientContactId() {
        return clientContactId;
    }

    public XoTransfer getAvatar() {
        if (avatarDownload != null && avatarDownload.isContentAvailable()) {
            return avatarDownload;
        } else if (avatarUpload != null && avatarUpload.isContentAvailable()) {
            return avatarUpload;
        }
        return null;
    }

    public String getAvatarFilePath() {
        XoTransfer avatar = getAvatar();
        if (avatar != null) {
            return avatar.getFilePath();
        }
        return null;
    }

    public boolean isSelf() {
        return this.contactType.equals(TYPE_SELF);
    }

    public boolean isSelfRegistered() {
        return isSelf() && this.clientId != null;
    }

    public boolean isClient() {
        return this.contactType.equals(TYPE_CLIENT);
    }

    public boolean isClientRelated() {
        return isClient() && this.clientRelationship != null && this.clientRelationship.isRelated();
    }

    public boolean isClientFriend() {
        return isClient() && this.clientRelationship != null && this.clientRelationship.isFriend();
    }

    public boolean isFriendOrBlocked() {
        return isClient() && this.clientRelationship != null && (this.clientRelationship.isFriend() || this.clientRelationship.isBlocked());
    }

    public boolean isNearbyAcquaintance() {
        return isClient() && this.clientPresence.isNearbyAcquaintance();
    }

    public boolean isWorldwideAcquaintance() {
        return isClient() && this.clientPresence.isWorldwideAcquaintance();
    }

    public boolean isKept() {
        return isClient() && this.clientPresence != null && this.clientPresence.isKept();
    }

    public boolean isKeptGroup() {
        return isGroup() && this.groupPresence != null && this.groupPresence.isKept();
    }

    public boolean isGroup() {
        return this.contactType.equals(TYPE_GROUP);
    }

    public boolean isGroupExisting() {
        return isGroup() && this.groupPresence != null && this.groupPresence.getState().equals(TalkGroupPresence.STATE_EXISTS);
    }

    public boolean isGroupAdmin() {
        return isGroup() && this.groupMembership != null && this.groupMembership.isAdmin();
    }

    public boolean isGroupInvolved() {
        return isGroup() && this.groupMembership != null && this.groupMembership.isInvolved();
    }

    public boolean isGroupJoined() {
        return isGroup() && this.groupMembership != null && this.groupMembership.isJoined();
    }

    public boolean isNearbyGroup() {
        return isGroup() && this.groupPresence != null && this.groupPresence.isTypeNearby();
    }

    public boolean isWorldwideGroup() {
        return isGroup() && this.groupPresence != null && this.groupPresence.isTypeWorldwide();
    }

    public boolean isEnvironmentGroup() {
        return isWorldwideGroup() || isNearbyGroup();
    }

    public boolean isInEnvironment() {
        return isNearby() || isWorldwide();
    }

    public String getName() {
        if (isClient() || isSelf()) {
            if (clientPresence != null) {
                return clientPresence.getClientName();
            }
            if (isSelf()) {
                return "<self>";
            }
        }
        if (isGroup()) {
            if (groupPresence != null) {
                return groupPresence.getGroupName();
            }
        }
        return "<unknown>";
    }

    public String getStatus() {
        if (isClient()) {
            if (clientPresence != null) {
                return clientPresence.getClientStatus();
            }
        }
        return "";
    }

    public TalkClientDownload getAvatarDownload() {
        return avatarDownload;
    }

    @ClientOrGroupMethodOnly
    public void setAvatarDownload(TalkClientDownload avatarDownload) {
        ensureClientOrGroup();
        this.avatarDownload = avatarDownload;
    }

    public TalkClientUpload getAvatarUpload() {
        return avatarUpload;
    }

    @ClientOrSelfMethodOnly
    public void setAvatarUpload(TalkClientUpload avatarUpload) {
        ensureGroupOrSelf();
        this.avatarUpload = avatarUpload;
    }

    private void ensureSelf() {
        if (!isSelf()) {
            throw new RuntimeException("Client is not of type self");
        }
    }

    private void ensureClient() {
        if (!isClient()) {
            throw new RuntimeException("Client is not of type client");
        }
    }

    private void ensureClientOrSelf() {
        if (!(isClient() || isSelf())) {
            throw new RuntimeException("Client is not of type client or self");
        }
    }

    private void ensureClientOrGroup() {
        if (!(isClient() || isGroup())) {
            throw new RuntimeException("Client is not of type client or group");
        }
    }

    private void ensureGroup() {
        if (!isGroup()) {
            throw new RuntimeException("Client is not of type group");
        }
    }

    private void ensureGroupOrSelf() {
        if (!(isGroup() || isSelf())) {
            throw new RuntimeException("Client is not of type group or self");
        }
    }


    public String getContactType() {
        return contactType;
    }

    public TalkKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(TalkKey publicKey) {
        this.publicKey = publicKey;
    }

    public TalkPrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(TalkPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String getNickname() {
        if (nickname == null || nickname.isEmpty()) {
            return getName();
        }
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Date getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    public void setCreatedTimeStamp(Date createdTimeStamp) {
        this.createdTimeStamp = createdTimeStamp;
    }

    @SelfMethodOnly
    public TalkClientSelf getSelf() {
        ensureSelf();
        return self;
    }

    @ClientOrSelfMethodOnly
    public String getClientId() {
        ensureClientOrSelf();
        return clientId;
    }

    @ClientOrSelfMethodOnly
    public TalkPresence getClientPresence() {
        ensureClientOrSelf();
        return clientPresence;
    }

    @ClientMethodOnly
    public TalkRelationship getClientRelationship() {
        ensureClient();
        return clientRelationship;
    }

    @GroupMethodOnly
    public String getGroupId() {
        ensureGroup();
        return groupId;
    }

    @GroupMethodOnly
    public String getGroupTag() {
        ensureGroup();
        return groupTag;
    }

    @GroupMethodOnly
    @Nullable
    public TalkGroupPresence getGroupPresence() {
        ensureGroup();
        return groupPresence;
    }

    @GroupMethodOnly
    public TalkGroupMembership getGroupMembership() {
        ensureGroup();
        return groupMembership;
    }

    // the actual group key, Base64-encoded
    @GroupMethodOnly
    public String getGroupKey() {
        ensureGroup();
        return groupKey;
    }

    // the actual group key, Base64-encoded
    @GroupMethodOnly
    public void setGroupKey(String groupKey) {
        ensureGroup();
        this.groupKey = groupKey;
    }

    public boolean isNearby() {
        return isNearby;
    }

    public void setNearby(boolean isNearby) {
        this.isNearby = isNearby;
    }

    public boolean isWorldwide() {
        return worldwide;
    }

    public void setWorldwide(boolean worldwide) {
        this.worldwide = worldwide;
    }

    @SelfMethodOnly
    public boolean initializeSelf() {
        boolean changed = false;
        ensureSelf();
        if (this.self == null) {
            this.self = new TalkClientSelf();
            changed = true;
        }
        if (this.clientPresence == null) {
            this.clientPresence = new TalkPresence();
            changed = true;
        }
        return changed;
    }

    @SelfMethodOnly
    public void updateSelfRegistered(String clientId) {
        ensureSelf();
        this.clientId = clientId;
    }

    @GroupMethodOnly
    public void updateGroupId(String groupId) {
        ensureGroup();
        this.groupId = groupId;
    }

    @GroupMethodOnly
    public void updateGroupTag(String groupTag) {
        ensureGroup();
        this.groupTag = groupTag;
    }

    @ClientOrSelfMethodOnly
    public void updatePresence(TalkPresence presence) {
        ensureClientOrSelf();
        if (this.clientPresence == null) {
            this.clientPresence = presence;
        } else {
            this.clientPresence.updateWith(presence);
        }
    }

    @ClientOrSelfMethodOnly
    public void modifyPresence(TalkPresence presence, Set<String> fields) {
        ensureClientOrSelf();
        if (this.clientPresence == null) {
            throw new RuntimeException("try to modify empty presence");
        } else {
            this.clientPresence.updateWith(presence, fields);
        }
    }

    @ClientMethodOnly
    public void updateRelationship(TalkRelationship relationship) {
        ensureClient();
        if (this.clientRelationship == null) {
            this.clientRelationship = relationship;
        } else {
            this.clientRelationship.updateWith(relationship);
        }
    }

    @GroupMethodOnly
    public void updateGroupPresence(TalkGroupPresence groupPresence) {
        ensureGroup();
        if (this.groupPresence == null) {
            if (groupPresence.getGroupId() != null) {
                groupId = groupPresence.getGroupId();
            }
            if (groupPresence.getGroupTag() != null) {
                groupTag = groupPresence.getGroupTag();
            }
            this.groupPresence = groupPresence;
        } else {
            this.groupPresence.updateWith(groupPresence);
        }
    }

    @GroupMethodOnly
    public void updateGroupMembership(TalkGroupMembership membership) {
        ensureGroup();
        if (this.groupMembership == null) {
            this.groupMembership = membership;
        } else {
            this.groupMembership.updateWith(membership);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof TalkClientContact && clientContactId == ((TalkClientContact) obj).clientContactId;
    }

    @Override
    public int hashCode() {
        return clientContactId;
    }
}
