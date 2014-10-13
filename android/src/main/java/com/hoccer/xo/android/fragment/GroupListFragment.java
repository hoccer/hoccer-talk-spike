package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.GroupProfileActivity;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.GroupListAdapter;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;

import java.sql.SQLException;

public class GroupListFragment extends SearchableListFragment implements IPagerFragment, IXoContactListener {

    GroupListAdapter mGroupListAdapter;
    private ImageView mPlaceholderImageFrame;
    private ImageView mPlaceholderImage;
    private TextView mPlaceholderText;

    private View mTabView;
    private TextView mNotificationBadgeTextView;

    private int mInvitedMeCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPlaceholderImageFrame = (ImageView) view.findViewById(R.id.iv_contacts_placeholder_frame);
        mPlaceholderImage = (ImageView) view.findViewById(R.id.iv_contacts_placeholder);
        mPlaceholderText = (TextView) view.findViewById(R.id.tv_contacts_placeholder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mPlaceholderImageFrame.setBackground(getResources().getDrawable(R.drawable.placeholder_group));
            mPlaceholderImage.setBackground(ColorSchemeManager.getRepaintedDrawable(getActivity(), R.drawable.placeholder_group_head, true));
        } else {
            mPlaceholderImageFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.placeholder_group));
            mPlaceholderImage.setBackgroundDrawable(ColorSchemeManager.getRepaintedDrawable(getActivity(), R.drawable.placeholder_group_head, true));
        }

        mPlaceholderText.setText(R.string.placeholder_groups_text);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGroupListAdapter = new GroupListAdapter(getActivity());

        mNotificationBadgeTextView = (TextView) getCustomTabView(getActivity()).findViewById(R.id.tv_contact_invite_notification_badge);
        setListAdapter(mGroupListAdapter);

        XoApplication.getXoClient().registerContactListener(mGroupListAdapter);
        XoApplication.getXoClient().registerContactListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNotificationBadge();
    }

    @Override
      public void onDestroy() {
        super.onDestroy();
        XoApplication.getXoClient().unregisterContactListener(mGroupListAdapter);
        XoApplication.getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        TalkClientContact contact = (TalkClientContact) getListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), GroupProfileActivity.class);
        intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
        startActivity(intent);
    }

    @Override
    public void onPageResume() {}

    @Override
    public void onPageSelected() {}

    @Override
    public void onPageUnselected() {}

    @Override
    public void onPagePause() {}

    @Override
    public void onPageScrollStateChanged(int state) {}

    @Override
    public View getCustomTabView(Context context) {
        if (mTabView == null) {
            mTabView = LayoutInflater.from(context).inflate(R.layout.view_contacts_tab_groups, null);
        }
        return mTabView;
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(R.string.contacts_tab_groups);
    }

    @Override
    protected ListAdapter searchInAdapter(String query) {
        mGroupListAdapter.setQuery(query);
        return mGroupListAdapter;
    }

    @Override
    protected void onSearchModeEnabled() {}

    @Override
    protected void onSearchModeDisabled() {
        mGroupListAdapter.setQuery(null);
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {

    }

    @Override
    public void onContactAdded(TalkClientContact contact) {

    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {

    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {

    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
    }

    private void updateNotificationBadge() {
        try {
            mInvitedMeCount = XoApplication.getXoClient().getDatabase().getCountOfInvitedMeGroups();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mInvitedMeCount > 0) {
                    mNotificationBadgeTextView.setVisibility(View.VISIBLE);
                    if (mInvitedMeCount < 10) {
                        mNotificationBadgeTextView.setTextSize(13);
                    } else if (mInvitedMeCount < 100) {
                        mNotificationBadgeTextView.setText(11);
                    } else if (mInvitedMeCount < 1000) {
                        mNotificationBadgeTextView.setText(9);
                    }
                    mNotificationBadgeTextView.setText(Integer.toString(mInvitedMeCount));
                } else {
                    mNotificationBadgeTextView.setText("");
                    mNotificationBadgeTextView.setVisibility(View.GONE);
                }
            }
        });
    }
}
