package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.ClientContactsAdapter;

public class ClientListFragment extends ListFragment {

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
}
