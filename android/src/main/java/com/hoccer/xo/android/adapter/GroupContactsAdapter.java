package com.hoccer.xo.android.adapter;

import android.view.View;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMembership;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.talk.model.TalkGroupMember;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

/**
 * Contacts adapter for simple lists
 * <p/>
 * This displays just the avatar and the name of a contact.
 * <p/>
 * It is used mostly for selecting users in a group context.
 */
public class GroupContactsAdapter extends ContactsAdapter {

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
    protected int getTokenLayout() {
        return -1;
    }

    @Override
    protected int getNearbyHistoryLayout() {
        return -1;
    }

    @Override
    protected void updateNearbyHistoryLayout(View v) {

    }

    @Override
    protected void updateToken(View view, TalkClientSmsToken token) {
        LOG.debug("updateToken(" + token.getSmsTokenId() + ")");
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

        TextView roleView = (TextView) view.findViewById(R.id.contact_role);
        if (isContactAdminInGroup(contact, mGroup)) {
            roleView.setVisibility(View.VISIBLE);
        } else {
            roleView.setVisibility(View.GONE);
        }
    }

    private boolean isContactAdminInGroup(TalkClientContact contact, TalkClientContact group) {
        if (contact.isClient()) {
            if (group.getGroupMemberships() == null) {
                return false;
            }
            for (TalkClientMembership groupMembership : group.getGroupMemberships()) {
                TalkGroupMember groupMember = groupMembership.getMember();
                if (groupMember.getClientId().equals(contact.getClientId())) {
                    if (groupMember.getRole().equals(TalkGroupMember.ROLE_ADMIN)) {
                        return true;
                    }
                }
            }
        } else if (contact.isSelf()) {
            TalkGroupMember member = mGroup.getGroupMember();
            if (member != null && member.getRole().equals(TalkGroupMember.ROLE_ADMIN)) {
                return true;
            }
        }
        return false;
    }
}
