package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

//import org.jetbrains.annotations.Nullable;

@DatabaseTable(tableName = "relationship")
public class TalkRelationship {

    public static final String STATE_NONE = "none";
    public static final String STATE_INVITED = "invited";
    public static final String STATE_INVITED_ME = "invitedMe";
    public static final String STATE_FRIEND = "friend";
    public static final String STATE_BLOCKED = "blocked";

    public static final String LOCK_PREFIX = "rel-";

    public static final String[] STATES_VALID = {STATE_NONE, STATE_FRIEND, STATE_BLOCKED, STATE_INVITED, STATE_INVITED_ME};
    public static final Set<String> STATES_VALID_SET = new HashSet<String>(Arrays.asList(STATES_VALID));

    public static final String[] STATES_RELATED = {STATE_FRIEND, STATE_BLOCKED, STATE_INVITED, STATE_INVITED_ME};
    public static final Set<String> STATES_RELATED_SET = new HashSet<String>(Arrays.asList(STATES_RELATED));

    public static boolean isValidState(String state) {
        return STATES_VALID_SET.contains(state);
    }

    // needed for ormlight database
    private String _id;

    @DatabaseField(generatedId = true)
    private long relationshipId;

    @DatabaseField
    String clientId;

    @DatabaseField
    String otherClientId;

    @DatabaseField
    String state;

    @DatabaseField
    String unblockState;

    @DatabaseField
    Date lastChanged;

    @JsonIgnore
    public boolean isRelated() {
        return STATES_RELATED_SET.contains(state);
    }

    @JsonIgnore
    public boolean isNone() {
        return STATE_NONE.equals(state);
    }

    @JsonIgnore
    public boolean isFriend() {
        return STATE_FRIEND.equals(state);
    }

    @JsonIgnore
    public boolean isBlocked() {
        return STATE_BLOCKED.equals(state);
    }

    @JsonIgnore
    public boolean isInvited() {
        return STATE_INVITED.equals(state);
    }

    @JsonIgnore
    public boolean invitedMe() {
        return STATE_INVITED_ME.equals(state);
    }

    @JsonIgnore
    public boolean isDirectlyRelated() {
        return isFriend() || isInvited() || isBlocked() || invitedMe();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getOtherClientId() {
        return otherClientId;
    }

    public void setOtherClientId(String otherClientId) {
        this.otherClientId = otherClientId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        if (!isValidState(state)) {
            throw new RuntimeException("Invalid relationship state: " + state);
        }
        this.state = state;
    }

    public String getUnblockState() {
        return unblockState;
    }

    public void setUnblockState(String unblockState) {
        this.unblockState = unblockState;
    }

    public Date getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
    }

    @JsonIgnore
    public void updateWith(TalkRelationship r) {
        this.setClientId(r.getClientId());
        this.setOtherClientId(r.getOtherClientId());
        this.setState(r.getState());
        this.setLastChanged(r.getLastChanged());
    }
}
