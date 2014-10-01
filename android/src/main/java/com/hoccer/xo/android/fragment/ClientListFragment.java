package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ClientContactsAdapter;
import com.hoccer.xo.release.R;

public class ClientListFragment extends ListFragment implements IPagerFragment {

    ClientContactsAdapter mClientContactsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClientContactsAdapter = new ClientContactsAdapter();
        setListAdapter(mClientContactsAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        XoApplication.getXoClient().registerContactListener(mClientContactsAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        XoApplication.getXoClient().unregisterContactListener(mClientContactsAdapter);
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
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        TalkClientContact contact = (TalkClientContact) getListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), SingleProfileActivity.class);
        intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
        startActivity(intent);
    }

    @Override
    public View getCustomTabView(Context context) {
        return null;
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(R.string.contacts_tab_friends);
    }
}
