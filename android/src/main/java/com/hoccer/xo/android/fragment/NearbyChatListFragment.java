package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.adapter.NearbyChatListAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.view.Placeholder;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;


public class NearbyChatListFragment extends XoListFragment {
    private static final Logger LOG = Logger.getLogger(NearbyChatListFragment.class);
    private static final Placeholder PLACEHOLDER = new Placeholder(
            R.drawable.placeholder_nearby,
            R.drawable.placeholder_nearby_point,
            R.string.placeholder_nearby_text);

    private NearbyChatListAdapter mNearbyAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        PLACEHOLDER.applyToView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        createAdapter();
    }

    @Override
    public void onDestroy() {
        if (mNearbyAdapter != null) {
            mNearbyAdapter.unregisterListeners();
        }

        destroyAdapter();
        super.onDestroy();
    }

    private void createAdapter() {
        if (mNearbyAdapter == null) {
            mNearbyAdapter = new NearbyChatListAdapter(getXoDatabase(), getXoActivity());
            mNearbyAdapter.registerListeners();
            setListAdapter(mNearbyAdapter);
        }
    }

    private void destroyAdapter() {
        if (mNearbyAdapter != null) {
            setListAdapter(null);
            mNearbyAdapter.unregisterListeners();
            mNearbyAdapter = null;
        }
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        super.onListItemClick(listView, v, position, id);
        Object item = listView.getItemAtPosition(position);
        if (item instanceof TalkClientContact) {
            TalkClientContact contact = (TalkClientContact) item;
            getXoActivity().showContactConversation(contact);
        }
    }
}
