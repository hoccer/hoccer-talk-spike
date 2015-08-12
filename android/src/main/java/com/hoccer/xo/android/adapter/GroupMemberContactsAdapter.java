package com.hoccer.xo.android.adapter;

import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.base.BaseActivity;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Contacts adapter for simple lists
 * <p/>
 * This displays just the avatar and the name of a contact.
 * <p/>
 * It is used mostly for selecting users in a group context.
 */
public class GroupMemberContactsAdapter extends ContactsAdapter {

    private static final Logger LOG = Logger.getLogger(GroupMemberContactsAdapter.class);

    @Nullable
    private final String mGroupId;

    public GroupMemberContactsAdapter(BaseActivity activity) {
        this(activity, null);
    }

    public GroupMemberContactsAdapter(BaseActivity activity, @Nullable String groupId) {
        super(activity);
        mGroupId = groupId;
    }

    @Override
    protected int getClientLayout() {
        return R.layout.item_contact_group_member;
    }

    @Override
    protected int getSeparatorLayout() {
        return R.layout.item_contact_separator;
    }

    @Override
    protected void updateContact(final View view, final TalkClientContact contact) {
        LOG.debug("updateContact(" + contact.getClientContactId() + ")");
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        nameView.setText(contact.getNickname());

        TextView statusView = (TextView) view.findViewById(R.id.status);
        statusView.setText(getMemberStatus(contact, view.getResources()));
    }

    private String getMemberStatus(TalkClientContact contact, Resources resources) {
        ArrayList<String> status = new ArrayList<String>();

        if (isContactAdminInGroup(contact)) {
            status.add(resources.getString(R.string.contact_role_owner));
        }

        if (isContactInvitedToGroup(contact)) {
            status.add(resources.getString(R.string.state_invited));
        }

        if (contact.isClientFriend()) {
            status.add(resources.getString(R.string.state_friend));
        }

        return TextUtils.join("\n", status);
    }

    private boolean isContactAdminInGroup(TalkClientContact contact) {
        try {
            if (mGroupId != null) {
                TalkClientContact admin = mDatabase.findAdminInGroup(mGroupId);
                return admin != null && contact.getClientId().equals(admin.getClientId());
            }
        } catch (SQLException e) {
            LOG.error("isContactAdminInGroup", e);
        }
        return false;
    }

    private boolean isContactInvitedToGroup(TalkClientContact contact) {
        try {
            if (mGroupId != null) {
                TalkGroupMembership membership = mDatabase.findMembershipInGroupByClientId(mGroupId, contact.getClientId());
                return membership != null && membership.isInvited();
            }
        } catch (SQLException e) {
            LOG.error("isContactInvitedToGroup", e);
        }
        return false;
    }
}
