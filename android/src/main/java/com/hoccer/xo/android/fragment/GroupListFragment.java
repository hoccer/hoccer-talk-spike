package com.hoccer.xo.android.fragment;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.activity.GroupProfileActivity;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.android.adapter.GroupListAdapter;
import com.hoccer.xo.release.R;

public class GroupListFragment extends ContactListFragment {

    public GroupListFragment() {
        mPlaceholderId = R.drawable.placeholder_group;
        mPlaceholderHeadId = R.drawable.placeholder_group_head;
        mPlaceholderTextId = R.string.placeholder_groups_text;
        mTabLayoutId = R.layout.view_contacts_tab_groups;
        mTabNameId = R.string.contacts_tab_groups;

        mItemActivityClass = GroupProfileActivity.class;
    }

    @Override
    protected ContactListAdapter createAdapter() {
        return new GroupListAdapter(getActivity());
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }
}
