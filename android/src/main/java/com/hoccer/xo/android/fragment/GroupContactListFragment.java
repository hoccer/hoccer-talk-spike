package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.FlavorBaseActivity;
import com.hoccer.xo.android.profile.group.GroupProfileActivity;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.android.adapter.GroupContactListAdapter;
import com.hoccer.xo.android.view.Placeholder;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class GroupContactListFragment extends ContactListFragment {

    private static final Logger LOG = Logger.getLogger(GroupContactListFragment.class);
    private static final Placeholder PLACEHOLDER = new Placeholder(R.drawable.placeholder_group, R.string.placeholder_groups_text);

    public GroupContactListFragment() {
        super(R.string.contacts_tab_groups);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PLACEHOLDER.applyToView(view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FlavorBaseActivity) getActivity()).showNewGroup();
            }
        });
    }

    @Override
    protected ContactListAdapter createAdapter() {
        return new GroupContactListAdapter(getActivity());
    }

    @Override
    protected Intent getProfileActivityIntent(TalkClientContact contact) {
        return new Intent(getActivity(), GroupProfileActivity.class)
                .setAction(GroupProfileActivity.ACTION_SHOW)
                .putExtra(GroupProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    @Override
    protected int getInvitedMeCount() {
        try {
            return XoApplication.get().getClient().getDatabase().getCountOfInvitedMeGroupContacts();
        } catch (SQLException e) {
            LOG.error("Error getting group invitation count", e);
        }

        return 0;
    }
}
