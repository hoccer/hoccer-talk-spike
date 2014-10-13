package com.hoccer.xo.android.adapter;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.BaseAdapter;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;

import java.util.ArrayList;
import java.util.List;

public abstract class ContactListAdapter extends BaseAdapter implements IXoContactListener {

    protected Activity mActivity;
    protected List<TalkClientContact> mContacts = new ArrayList<TalkClientContact>();
    private String mQuery;

    public ContactListAdapter(Activity activity) {
        mContacts = getContacts();
        mActivity = activity;
    }

    public void setQuery(String query) {
        mQuery = query;
        refreshView();
    }

    protected List<TalkClientContact> getContacts() {
        List<TalkClientContact> all = getAllContacts();

        if (mQuery != null && !mQuery.isEmpty()) {
            return filterContacts(all, mQuery);
        } else {
            return all;
        }
    }

    private List<TalkClientContact> filterContacts(List<TalkClientContact> contacts, String query) {
        List<TalkClientContact> filtered = new ArrayList<TalkClientContact>();

        for (TalkClientContact contact : contacts) {
            if (contact.getNickname().toLowerCase().contains(query)) {
                filtered.add(contact);
            }
        }

        return filtered;
    }

    protected abstract List<TalkClientContact> getAllContacts();

    @Override
    public int getCount() {
        return mContacts.size();
    }

    @Override
    public Object getItem(int position) {
        return mContacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mContacts.get(position).getClientContactId();
    }

    protected void refreshView() {
        final List<TalkClientContact> newContacts = getContacts();

        Handler guiHandler = new Handler(Looper.getMainLooper());
        guiHandler.post(new Runnable() {
            @Override
            public void run() {
                mContacts = newContacts;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        refreshView();
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        refreshView();
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        refreshView();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        refreshView();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {}
}
