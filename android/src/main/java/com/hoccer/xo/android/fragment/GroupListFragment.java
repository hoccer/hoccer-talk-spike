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
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.GroupProfileActivity;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.GroupsAdapter;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;

public class GroupListFragment extends ListFragment implements IPagerFragment {

    GroupsAdapter mGroupsAdapter;
    private ImageView mPlaceholderImageFrame;
    private ImageView mPlaceholderImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPlaceholderImageFrame = (ImageView) view.findViewById(R.id.iv_contacts_placeholder_frame);
        mPlaceholderImage= (ImageView) view.findViewById(R.id.iv_contacts_placeholder);

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
        mGroupsAdapter = new GroupsAdapter(getActivity());
        setListAdapter(mGroupsAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        XoApplication.getXoClient().registerContactListener(mGroupsAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        XoApplication.getXoClient().unregisterContactListener(mGroupsAdapter);
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
        return null;
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(R.string.contacts_tab_groups);
    }
}
