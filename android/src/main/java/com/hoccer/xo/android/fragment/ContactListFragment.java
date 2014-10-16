package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;

import java.sql.SQLException;

public abstract class ContactListFragment extends SearchableListFragment implements IPagerFragment, IXoContactListener {

    private ContactListAdapter mContactListAdapter;
    protected  Class<?> mItemActivityClass;

    protected int mPlaceholderId;
    protected int mPlaceholderHeadId;
    protected int mPlaceholderTextId;

    private RelativeLayout mPlaceholderLayout;
    private DataSetObserver mPlaceholderUpdateObserver;

    protected int mTabLayoutId;
    protected int mTabNameId;

    private MenuItem mMenuItemPairing;
    private MenuItem mMenuItemNewGroup;

    private View mTabView;
    private TextView mNotificationBadgeTextView;

    protected int mInvitedMeCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPlaceholderLayout = (RelativeLayout) view.findViewById(R.id.rl_placeholder);
        mPlaceholderUpdateObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                updatePlaceholder();
            }
        };

        ImageView placeholderImageFrame = (ImageView) view.findViewById(R.id.iv_contacts_placeholder_frame);
        ImageView placeholderImage = (ImageView) view.findViewById(R.id.iv_contacts_placeholder);
        TextView placeholderText = (TextView) view.findViewById(R.id.tv_contacts_placeholder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            placeholderImageFrame.setBackground(getResources().getDrawable(mPlaceholderId));
            placeholderImage.setBackground(ColorSchemeManager.getRepaintedDrawable(getActivity(), mPlaceholderHeadId, true));
        } else {
            placeholderImageFrame.setBackgroundDrawable(getResources().getDrawable(mPlaceholderId));
            placeholderImage.setBackgroundDrawable(ColorSchemeManager.getRepaintedDrawable(getActivity(), mPlaceholderHeadId, true));
        }

        placeholderText.setText(mPlaceholderTextId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenuItemPairing = menu.findItem(R.id.menu_pair);
        mMenuItemNewGroup = menu.findItem(R.id.menu_new_group);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotificationBadgeTextView = (TextView) getCustomTabView(getActivity()).findViewById(R.id.tv_contact_invite_notification_badge);

        mContactListAdapter = createAdapter();
        setListAdapter(mContactListAdapter);

        XoApplication.getXoClient().registerContactListener(mContactListAdapter);
        XoApplication.getXoClient().registerContactListener(this);
    }

    protected abstract ContactListAdapter createAdapter();

    @Override
    public void onResume() {
        super.onResume();
        updateAdapter();
        updateNotificationBadge();
        updatePlaceholder();
        mContactListAdapter.registerDataSetObserver(mPlaceholderUpdateObserver);
    }

    private void updateAdapter() {
        mContactListAdapter.updateContactsAndView();
    }

    protected void updateNotificationBadge() {
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

    private void updatePlaceholder() {
        int visibility = View.GONE;

        if (mContactListAdapter.getCount() == 0 && mContactListAdapter.getQuery() == null) {
            visibility = View.VISIBLE;
        }

        mPlaceholderLayout.setVisibility(visibility);
    }

    @Override
    protected ListAdapter searchInAdapter(String query) {
        mContactListAdapter.setQuery(query);
        return mContactListAdapter;
    }

    @Override
    protected void onSearchModeEnabled() {
        mMenuItemPairing.setVisible(false);
        mMenuItemNewGroup.setVisible(false);
    }

    @Override
    protected void onSearchModeDisabled() {
        mContactListAdapter.setQuery(null);

        if (mMenuItemPairing != null) {
            mMenuItemPairing.setVisible(true);
        }

        if (mMenuItemNewGroup != null) {
            mMenuItemNewGroup.setVisible(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mContactListAdapter.unregisterDataSetObserver(mPlaceholderUpdateObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XoApplication.getXoClient().unregisterContactListener(mContactListAdapter);
        XoApplication.getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        TalkClientContact contact = (TalkClientContact) getListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), mItemActivityClass);
        intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
        startActivity(intent);
    }

    public void onPageResume() {}

    public void onPageSelected() {}

    public void onPageUnselected() {}

    public void onPagePause() {}

    public void onPageScrollStateChanged(int state) {}

    public View getCustomTabView(Context context) {
        if (mTabView == null) {
            mTabView = LayoutInflater.from(context).inflate(mTabLayoutId, null);
        }

        return mTabView;
    }

    public String getTabName(Resources resources) {
        return resources.getString(mTabNameId);
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {}

    @Override
    public void onContactAdded(TalkClientContact contact) {}

    @Override
    public void onContactRemoved(TalkClientContact contact) {}

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {}
}
