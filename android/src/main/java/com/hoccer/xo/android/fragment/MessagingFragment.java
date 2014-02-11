package com.hoccer.xo.android.fragment;

import com.actionbarsherlock.widget.SearchView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.adapter.ConversationAdapter;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.release.R;

import org.apache.log4j.Logger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Fragment for conversations
 */
public class MessagingFragment extends XoListFragment
        implements SearchView.OnQueryTextListener,
        XoAdapter.AdapterReloadListener {

    private static final Logger LOG = Logger.getLogger(MessagingFragment.class);

    private ListView mMessageList;

    private TextView mEmptyText;

    private TalkClientContact mContact;

    private ConversationAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LOG.debug("onCreateView()");
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_messaging, container, false);

        mMessageList = (ListView) view.findViewById(android.R.id.list);
        mEmptyText = (TextView) view.findViewById(R.id.messaging_empty);

        return view;
    }

    @Override
    public void onResume() {
        LOG.debug("onResume()");
        super.onResume();

        if (mAdapter == null) {
            mAdapter = new ConversationAdapter(getXoActivity());
            mAdapter.setAdapterReloadListener(this);
            mAdapter.onCreate();
            mAdapter.requestReload();
            mMessageList.setAdapter(mAdapter);
        }

        mAdapter.onResume();

        if (mContact != null) {
            mAdapter.converseWithContact(mContact);
        }
    }

    @Override
    public void onPause() {
        LOG.debug("onPause()");
        super.onPause();
        mAdapter.onPause();
    }

    @Override
    public void onDestroy() {
        LOG.debug("onDestroy()");
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.onDestroy();
            mAdapter = null;
        }
    }

    @Override
    public void onAdapterReloadStarted(XoAdapter adapter) {
        LOG.debug("onAdapterReloadStarted()");
        mEmptyText.setText(R.string.messaging_loading);
    }

    @Override
    public void onAdapterReloadFinished(XoAdapter adapter) {
        LOG.debug("onAdapterReloadFinished()");
        mEmptyText.setText(R.string.messaging_no_messages);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        LOG.debug("onQueryTextChange(\"" + newText + "\")");
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        LOG.debug("onQueryTextSubmit(\"" + query + "\")");
        return true;
    }

    public void converseWithContact(TalkClientContact contact) {
        LOG.debug("converseWithContact(" + contact.getClientContactId() + ")");
        mContact = contact;
        if (mAdapter != null) {
            mAdapter.converseWithContact(contact);
        }
    }

}
