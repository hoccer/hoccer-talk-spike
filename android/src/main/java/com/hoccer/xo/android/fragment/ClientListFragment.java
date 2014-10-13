package com.hoccer.xo.android.fragment;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ClientListAdapter;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.release.R;

public class ClientListFragment extends ContactListFragment {

    public ClientListFragment() {
        mPlaceholderId = R.drawable.placeholder_chats;
        mPlaceholderHeadId = R.drawable.placeholder_chats_head;
        mPlaceholderTextId = R.string.placeholder_conversations_text;
        mTabLayoutId = R.layout.view_contacts_tab_friends;
        mTabNameId = R.string.contacts_tab_friends;

        mItemActivityClass = SingleProfileActivity.class;
    }

    @Override
    protected ContactListAdapter createAdapter() {
        return new ClientListAdapter(getActivity());
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }
}
