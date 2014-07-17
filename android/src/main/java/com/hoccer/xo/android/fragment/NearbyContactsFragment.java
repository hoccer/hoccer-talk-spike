package com.hoccer.xo.android.fragment;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.adapter.NearbyContactsAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.bofrostmessenger.R;

import org.apache.log4j.Logger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class NearbyContactsFragment extends XoListFragment {
    private static final Logger LOG = Logger.getLogger(NearbyContactsFragment.class);
    private NearbyContactsAdapter mNearbyAdapter;
    private ListView mContactList;
    private ImageView mPlaceholderImageFrame;
    private ImageView mPlaceholderImage;
    private TextView mPlaceholderText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_requests, container, false);
        mContactList = (ListView) view.findViewById(android.R.id.list);
        mPlaceholderImageFrame = (ImageView) view.findViewById(R.id.iv_contacts_placeholder_frame);
        mPlaceholderImageFrame.setImageResource(R.drawable.placeholder_chats);
        mPlaceholderImage = (ImageView) view.findViewById(R.id.iv_contacts_placeholder);
        mPlaceholderImage.setBackgroundDrawable(ColorSchemeManager.getRepaintedDrawable(getXoActivity(), R.drawable.placeholder_chats_head, true));
        mPlaceholderText = (TextView) view.findViewById(R.id.tv_contacts_placeholder);
        hidePlaceholder();
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNearbyAdapter = new NearbyContactsAdapter(getXoDatabase(), getXoActivity());
        mNearbyAdapter.retrieveDataFromDb();
        mNearbyAdapter.registerListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        setListAdapter(mNearbyAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mNearbyAdapter.unregisterListeners();
        super.onDestroy();
    }

    private void hidePlaceholder() {
        mPlaceholderImageFrame.setVisibility(View.GONE);
        mPlaceholderImage.setVisibility(View.GONE);
        mPlaceholderText.setVisibility(View.GONE);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (l == mContactList) {
            LOG.debug("onListItemClick(contactList," + position + ")");
            Object item = mContactList.getItemAtPosition(position);
            if (item instanceof TalkClientContact) {
                TalkClientContact contact = (TalkClientContact) item;
                getXoActivity().showContactConversation(contact);
            }
        }
    }
}
