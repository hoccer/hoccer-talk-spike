package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ClientsAdapter;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;

import java.sql.SQLException;

public class ClientListFragment extends ListFragment implements IPagerFragment, IXoContactListener {


    ClientsAdapter mClientsAdapter;
    private ImageView mPlaceholderImageFrame;
    private ImageView mPlaceholderImage;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mPlaceholderImageFrame.setBackground(getResources().getDrawable(R.drawable.placeholder_chats));
            mPlaceholderImage.setBackground(ColorSchemeManager.getRepaintedDrawable(getActivity(), R.drawable.placeholder_chats_head, true));
        } else {
            mPlaceholderImageFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.placeholder_chats));
            mPlaceholderImage.setBackgroundDrawable(ColorSchemeManager.getRepaintedDrawable(getActivity(), R.drawable.placeholder_chats_head, true));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClientsAdapter = new ClientsAdapter(getActivity());

        mTabView = LayoutInflater.from(getActivity()).inflate(R.layout.view_contacts_tab_friends, null);
        mNotificationBadgeTextView = (TextView) mTabView.findViewById(R.id.tv_contact_invite_notification_badge);
        setListAdapter(mClientsAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mInvitedMeCount = (int) XoApplication.getXoClient().getDatabase().getInvitedMeRequestsCount();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (mInvitedMeCount > 0) {
            mNotificationBadgeTextView.setVisibility(View.VISIBLE);
        }
        mNotificationBadgeTextView.setText(Integer.toString(mInvitedMeCount));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XoApplication.getXoClient().unregisterContactListener(mClientsAdapter);
        XoApplication.getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        TalkClientContact contact = (TalkClientContact) getListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), SingleProfileActivity.class);
        intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
        startActivity(intent);
    }

    @Override
    public void onPageSelected() {

    }

    @Override
    public void onPageUnselected() {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public View getCustomTabView(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.view_contacts_tab_friends, null);
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(R.string.contacts_tab_friends);
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        if (contact.getClientRelationship().invitedMe()) {
            try {
                mInvitedMeCount = (int) XoApplication.getXoClient().getDatabase().getInvitedMeRequestsCount();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (mInvitedMeCount > 0) {
                mNotificationBadgeTextView.setVisibility(View.VISIBLE);
                mNotificationBadgeTextView.setText(Integer.toString(mInvitedMeCount));
            } else {
                mNotificationBadgeTextView.setVisibility(View.GONE);
                mNotificationBadgeTextView.setText("");
            }
        }
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

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {

    }
}
