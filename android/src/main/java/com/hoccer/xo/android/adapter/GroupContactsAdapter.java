package com.hoccer.xo.android.adapter;

import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupMember;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Contacts adapter for simple lists
 * <p/>
 * This displays just the avatar and the name of a contact.
 * <p/>
 * It is used mostly for selecting users in a group context.
 */
public class GroupContactsAdapter extends ContactsAdapter {

    private static final Logger LOG = Logger.getLogger(GroupContactsAdapter.class);

    private TalkClientContact mGroup;

    public GroupContactsAdapter(XoActivity activity, TalkClientContact group) {
        super(activity);
        mGroup = group;
    }

    @Override
    protected int getClientLayout() {
        return R.layout.item_contact_group_member;
    }

    @Override
    protected int getGroupLayout() {
        return R.layout.item_contact_group_member;
    }

    @Override
    protected int getSeparatorLayout() {
        return R.layout.item_contact_separator;
    }

    @Override
    protected int getNearbyHistoryLayout() {
        return -1;
    }

    @Override
    protected void updateNearbyHistoryLayout(View v) {
    }

    @Override
    protected void updateContact(final View view, final TalkClientContact contact) {
        LOG.debug("updateContact(" + contact.getClientContactId() + ")");
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        nameView.setText(contact.getNickname());

        AvatarView avatarView = (AvatarView) view.findViewById(R.id.contact_icon);
        avatarView.setContact(contact);
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.showContactProfile(contact);
            }
        });

        TextView statusView = (TextView) view.findViewById(R.id.status);
        statusView.setText(getMemberStatus(contact, view.getResources()));
    }

    private String getMemberStatus(TalkClientContact contact, Resources resources) {
        ArrayList<String> status = new ArrayList<String>();

        // check group membership status if the group is registered already
        if(mGroup.getGroupId() != null) {
            if (isContactAdminInGroup(contact, mGroup)) {
                status.add(resources.getString(R.string.contact_role_owner));
            }

            if (isContactInvitedToGroup(contact, mGroup)) {
                status.add(resources.getString(R.string.common_group_invite));
            }
        }

        if (contact.isClientFriend()) {
            status.add(resources.getString(R.string.common_friend));
        }

        return TextUtils.join("\n", status);
    }

    private boolean isContactAdminInGroup(TalkClientContact contact, TalkClientContact group) {
        try {
            TalkClientContact admin = mDatabase.findAdminInGroup(group.getGroupId());
            if (admin != null && contact.getClientId().equals(admin.getClientId())) {
                return true;
            }
        } catch (SQLException e) {
            LOG.error("isContactAdminInGroup", e);
        }
        return false;
    }

    private boolean isContactInvitedToGroup(TalkClientContact contact, TalkClientContact group) {
        try {
            TalkGroupMember member = mDatabase.findMemberInGroupWithClientId(group.getGroupId(), contact.getClientId());
            if (member != null && TalkGroupMember.STATE_INVITED.equals(member.getState())) {
                return true;
            }
        } catch (SQLException e) {
            LOG.error("isContactInvitedToGroup", e);
        }
        return false;
    }
}
