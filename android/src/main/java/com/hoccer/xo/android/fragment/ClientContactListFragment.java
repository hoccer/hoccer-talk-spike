package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ClientContactListAdapter;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.Placeholder;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class ClientContactListFragment extends ContactListFragment {

    private static final Logger LOG = Logger.getLogger(ClientContactListFragment.class);
    private static final Placeholder PLACEHOLDER = new Placeholder(R.drawable.placeholder_chats, R.string.placeholder_conversations_text);

    public ClientContactListFragment() {
        super(R.string.contacts_tab_friends);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PLACEHOLDER.applyToView(view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((XoActivity) getActivity()).showPairing();
            }
        });
    }

    @Override
    protected ContactListAdapter createAdapter() {
        return new ClientContactListAdapter(getActivity());
    }

    @Override
    protected Intent getProfileActivityIntent(TalkClientContact contact) {
        return new Intent(getActivity(), SingleProfileActivity.class)
                .setAction(SingleProfileActivity.ACTION_SHOW)
                .putExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    @Override
    protected int getInvitedMeCount() {
        try {
            return (int) XoApplication.get().getXoClient().getDatabase().getCountOfInvitedMeClients();
        } catch (SQLException e) {
            LOG.error("Error getting invitation count", e);
        }

        return 0;
    }
}
