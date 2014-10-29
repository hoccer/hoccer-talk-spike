package com.hoccer.xo.android.fragment;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ClientContactListAdapter;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.android.view.Placeholder;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class ClientContactListFragment extends ContactListFragment {

    private static final Logger LOG = Logger.getLogger(ContactListFragment.class);
    private static final Placeholder PLACEHOLDER = new Placeholder(
            R.drawable.placeholder_chats,
            R.drawable.placeholder_chats_head,
            R.string.placeholder_conversations_text);

    public ClientContactListFragment() {
        super(R.string.contacts_tab_friends, SingleProfileActivity.class, PLACEHOLDER);
    }

    @Override
    protected ContactListAdapter createAdapter() {
        return new ClientContactListAdapter(getActivity());
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    @Override
    protected int getInvitedMeCount() {
        try {
            return (int) XoApplication.getXoClient().getDatabase().getCountOfInvitedMeClients();
        } catch (SQLException e) {
            LOG.error("Error getting invitation count", e);
        }

        return 0;
    }
}
