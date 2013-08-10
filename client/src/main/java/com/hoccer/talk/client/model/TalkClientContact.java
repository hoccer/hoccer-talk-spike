package com.hoccer.talk.client.model;

import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.model.TalkGroupMember;
import com.hoccer.talk.model.TalkKey;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkPrivateKey;
import com.hoccer.talk.model.TalkRelationship;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * These represent a target of communication
 *
 * This may currently be either a groupPresence or another user.
 */
@DatabaseTable(tableName = "clientContact")
public class TalkClientContact {

    public static final String TYPE_SELF   = "self";
    public static final String TYPE_CLIENT = "client";
    public static final String TYPE_GROUP  = "group";

    @DatabaseField(generatedId = true)
    private int clientContactId;

    @DatabaseField
    private String contactType;


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

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkGroup groupPresence;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkGroupMember groupMember;

    @ForeignCollectionField(eager = false, foreignFieldName = "groupContact")
    private ForeignCollection<TalkClientMembership> groupMemberships;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = false)
    private TalkClientDownload avatarDownload;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = false)
    private TalkClientUpload avatarUpload;
    

    public TalkClientContact() {

    }

    public TalkClientContact(String contactType) {
        this.contactType = contactType;
    }

    public TalkClientContact(String contactType, String id) {
        this(contactType);
        if(contactType.equals(TYPE_CLIENT) || contactType.equals(TYPE_SELF)) {
            this.clientId = id;
        }
        if(contactType.equals(TYPE_GROUP)) {
            this.groupId = id;
        }
    }

    public int getClientContactId() {
        return clientContactId;
    }

    public boolean isSelf() {
        return this.contactType.equals(TYPE_SELF);
    }

    public boolean isSelfRegistered() {
        return isSelf() && this.clientId != null && this.self != null;
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

    public boolean isClientBlocked() {
        return isClient() && this.clientRelationship != null && this.clientRelationship.isBlocked();
    }

    public boolean isGroup() {
        return this.contactType.equals(TYPE_GROUP);
    }

    public boolean isGroupRegistered() {
        return isGroup() && this.groupId != null;
    }

    public boolean isGroupAdmin() {
        return isGroup() && this.groupMember != null && this.groupMember.isAdmin();
    }

    public boolean isGroupInvited() {
        return isGroup() && this.groupMember != null && this.groupMember.isInvited();
    }

    public boolean isGroupJoined() {
        return isGroup() && this.groupMember != null && this.groupMember.isJoined();
    }

    public String getName() {
        if(isClient() || isSelf()) {
            if(clientPresence != null) {
                return clientPresence.getClientName();
            }
            if(isSelf()) {
                return "<self>";
            }
        }
        if(isGroup()) {
            if(groupPresence != null) {
                return groupPresence.getGroupName();
            }
        }
        return "<unknown>";
    }

    public String getStatus() {
        if(isClient()) {
            if(clientPresence != null) {
                return clientPresence.getClientStatus();
            }
        }
        return "";
    }

    public TalkClientDownload getAvatarDownload() {
        ensureClientOrGroup();
        return avatarDownload;
    }

    public void setAvatarDownload(TalkClientDownload avatarDownload) {
        ensureClientOrGroup();
        this.avatarDownload = avatarDownload;
    }

    public TalkClientUpload getAvatarUpload() {
        ensureSelf();
        return avatarUpload;
    }

    public void setAvatarUpload(TalkClientUpload avatarUpload) {
        ensureSelf();
        this.avatarUpload = avatarUpload;
    }

    private void ensureSelf() {
        if(!isSelf()) {
            throw new RuntimeException("Client is not of type self");
        }
    }

    private void ensureClient() {
        if(!isClient()) {
            throw new RuntimeException("Client is not of type client");
        }
    }

    private void ensureClientOrSelf() {
        if(!(isClient() || isSelf())) {
            throw new RuntimeException("Client is not of type client or self");
        }
    }

    private void ensureClientOrGroup() {
        if(!(isClient() || isGroup())) {
            throw new RuntimeException("Client is not of type client or group");
        }
    }

    private void ensureGroup() {
        if(!isGroup()) {
            throw new RuntimeException("Client is not of type group");
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
    

    public TalkClientSelf getSelf() {
        ensureSelf();
        return self;
    }


    public String getClientId() {
        ensureClientOrSelf();
        return clientId;
    }


    public TalkPresence getClientPresence() {
        ensureClientOrSelf();
        return clientPresence;
    }

    public TalkRelationship getClientRelationship() {
        ensureClient();
        return clientRelationship;
    }


    public String getGroupId() {
        ensureGroup();
        return groupId;
    }

    public String getGroupTag() {
        ensureGroup();
        return groupTag;
    }

    public TalkGroup getGroupPresence() {
        ensureGroup();
        return groupPresence;
    }

    public TalkGroupMember getGroupMember() {
        ensureGroup();
        return groupMember;
    }

    public ForeignCollection<TalkClientMembership> getGroupMemberships() {
        ensureGroup();
        return groupMemberships;
    }

    public void updateSelfRegistered(String clientId, TalkClientSelf self) {
        ensureSelf();
        this.clientId = clientId;
        if(this.self == null) {
            this.self = self;
        } else {
            this.self.update(self);
        }
    }

    public void updateGroupId(String groupId) {
        ensureGroup();
        this.groupId = groupId;
    }

    public void updateGroupTag(String groupTag) {
        ensureGroup();
        this.groupTag = groupTag;
    }

    public void updatePresence(TalkPresence presence) {
        ensureClientOrSelf();
        if(this.clientPresence == null) {
            this.clientPresence = presence;
        } else {
            TalkPresence my = this.clientPresence;
            my.setClientName(presence.getClientName());
            my.setClientStatus(presence.getClientStatus());
            my.setConnectionStatus(presence.getConnectionStatus());
            my.setAvatarUrl(presence.getAvatarUrl());
            my.setKeyId(presence.getKeyId());
            my.setTimestamp(presence.getTimestamp());
        }
    }

    public void updateRelationship(TalkRelationship relationship) {
        ensureClient();
        if(this.clientRelationship == null) {
            this.clientRelationship = relationship;
        } else {
            TalkRelationship my = this.clientRelationship;
            my.setClientId(relationship.getClientId());
            my.setOtherClientId(relationship.getOtherClientId());
            my.setLastChanged(relationship.getLastChanged());
            my.setState(relationship.getState());
        }
    }

    public void updateGroupPresence(TalkGroup group) {
        ensureGroup();
        if(this.groupPresence == null) {
            if(group.getGroupId() != null) {
                groupId = group.getGroupId();
            }
            if(group.getGroupTag() != null) {
                groupTag = group.getGroupTag();
            }
            this.groupPresence = group;
        } else {
            TalkGroup my = this.groupPresence;
            my.setState(group.getState());
            my.setGroupName(group.getGroupName());
            my.setGroupAvatarUrl(group.getGroupAvatarUrl());
            my.setLastChanged(group.getLastChanged());
        }
    }

    public void updateGroupMember(TalkGroupMember member) {
        ensureGroup();
        if(this.groupMember == null) {
            this.groupMember = member;
        } else {
            TalkGroupMember my = this.groupMember;
            my.setState(member.getState());
            my.setLastChanged(member.getLastChanged());
            my.setMemberKeyId(member.getMemberKeyId());
            my.setEncryptedGroupKey(member.getEncryptedGroupKey());
            my.setRole(member.getRole());
        }
    }

}
