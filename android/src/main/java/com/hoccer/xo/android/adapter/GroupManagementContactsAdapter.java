package com.hoccer.xo.android.adapter;

import android.view.View;
import android.widget.CheckedTextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Contacts adapter for group management lists
 * <p/>
 * This displays the avatar and the name of a contact and a checkbox.
 * <p/>
 * It is used mostly for managing users in a group context.
 */
public class GroupManagementContactsAdapter extends ContactsAdapter {

    private static final Logger LOG = Logger.getLogger(GroupManagementContactsAdapter.class);

    private final TalkClientContact mGroup;
    private final ArrayList<TalkClientContact> mContactsToInvite;
    private final ArrayList<TalkClientContact> mContactsToKick;

    public GroupManagementContactsAdapter(XoActivity activity, TalkClientContact group, ArrayList<TalkClientContact> contactsToInvite, ArrayList<TalkClientContact> contactsToKick) {
        super(activity);

        mGroup = group;
        mContactsToInvite = contactsToInvite;
        mContactsToKick = contactsToKick;
    }

    @Override
    protected int getClientLayout() {
        return R.layout.item_contact_checked;
    }

    @Override
    protected int getGroupLayout() {
        return R.layout.item_contact_checked;
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
        CheckedTextView checkedTextView = (CheckedTextView) view.findViewById(R.id.contact_name_checked);
        checkedTextView.setText(contact.getNickname());

        AvatarView avatarView = (AvatarView) view.findViewById(R.id.contact_icon);
        avatarView.setContact(contact);

        try {
            TalkGroupMembership membership = mDatabase.findMembershipInGroupByClientId(mGroup.getGroupId(), contact.getClientId());
            boolean checked = membership != null && (membership.isInvited() || membership.isJoined());
            checkedTextView.setChecked(checked);

            if (mContactsToInvite.contains(contact)) {
                checkedTextView.setChecked(true);
            } else if (mContactsToKick.contains(contact)) {
                checkedTextView.setChecked(false);
            }
        } catch (SQLException e) {
            LOG.error(e);
        }
    }

}
