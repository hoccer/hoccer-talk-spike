package com.hoccer.xo.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.TransferListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for adapters dealing with contacts
 * <p/>
 * This allows base classes to override the UI for various needs.
 * <p/>
 * It also has a filter feature that allows restricting the displayed set of contacts.
 */
public abstract class ContactsAdapter extends BaseAdapter implements IXoContactListener, IXoMessageListener, TransferListener {

    private static final Logger LOG = Logger.getLogger(ContactsAdapter.class);

    protected final static long ITEM_ID_UNKNOWN = -1000;
    protected final static long ITEM_ID_CLIENT_HEADER = -1;
    protected final static long ITEM_ID_GROUP_HEADER = -2;

    protected final static int VIEW_TYPE_SEPARATOR = 0;
    protected final static int VIEW_TYPE_CLIENT = 1;
    protected final static int VIEW_TYPE_GROUP = 2;
    protected static final int VIEW_TYPE_NEARBY_HISTORY = 3;

    protected final static int VIEW_TYPE_COUNT = 4;

    private boolean mShowNearbyHistory;
    private long mNearbyMessagesCount;

    protected final XoClientDatabase mDatabase;
    protected final XoActivity mActivity;

    protected ContactsAdapter(XoActivity activity) {
        mActivity = activity;
        mDatabase = XoApplication.get().getXoClient().getDatabase();
    }

    protected ContactsAdapter(XoActivity activity, boolean showNearbyHistory) {
        this(activity);
        mShowNearbyHistory = showNearbyHistory;
    }

    Filter mFilter;

    List<TalkClientContact> mContacts = new ArrayList<TalkClientContact>();

    public Filter getFilter() {
        return mFilter;
    }

    public void setFilter(Filter filter) {
        this.mFilter = filter;
    }

    public void registerListeners() {
        XoApplication.get().getXoClient().registerContactListener(this);
        XoApplication.get().getXoClient().registerMessageListener(this);
        XoApplication.get().getXoClient().getDownloadAgent().registerListener(this);
        XoApplication.get().getXoClient().getUploadAgent().registerListener(this);
    }

    public void unRegisterListeners() {
        XoApplication.get().getXoClient().unregisterContactListener(this);
        XoApplication.get().getXoClient().unregisterMessageListener(this);
        XoApplication.get().getXoClient().getDownloadAgent().unregisterListener(this);
        XoApplication.get().getXoClient().getUploadAgent().unregisterListener(this);
    }

    public void loadContacts() {
        synchronized (this) {
            try {
                List<TalkClientContact> newContacts = mDatabase.findAllContactsExceptSelfOrderedByRecentMessage();
                LOG.debug("found " + newContacts.size() + " contacts");

                if (mFilter != null) {
                    newContacts = filter(newContacts, mFilter);
                }
                LOG.debug("filtered " + newContacts.size() + " contacts");

                for (TalkClientContact contact : newContacts) {
                    TalkClientDownload avatarDownload = contact.getAvatarDownload();
                    if (avatarDownload != null) {
                        mDatabase.refreshClientDownload(avatarDownload);
                    }
                }

                mContacts = newContacts;
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetInvalidated();
            }
        });
    }

    private static List<TalkClientContact> filter(List<TalkClientContact> contacts, Filter filter) {
        ArrayList<TalkClientContact> result = new ArrayList<TalkClientContact>();
        for (TalkClientContact contact : contacts) {
            if (filter.shouldShow(contact)) {
                result.add(contact);
            }
        }
        return result;
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        loadContacts();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        loadContacts();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        loadContacts();
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        loadContacts();
    }

    @Override
    public void onMessageCreated(TalkClientMessage message) {
        loadContacts();
    }

    @Override
    public void onMessageDeleted(TalkClientMessage message) {
    }

    @Override
    public void onMessageUpdated(TalkClientMessage message) {
    }

    @Override
    public void onDownloadRegistered(TalkClientDownload download) {
    }

    @Override
    public void onDownloadStarted(TalkClientDownload download) {
    }

    @Override
    public void onDownloadProgress(TalkClientDownload download) {
    }

    @Override
    public void onDownloadFinished(TalkClientDownload download) {
    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {
    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {
        if (download.isAvatar() && download.getState() == TalkClientDownload.State.COMPLETE) {
            loadContacts();
        }
    }

    @Override
    public void onUploadStarted(TalkClientUpload upload) {
    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {
    }

    @Override
    public void onUploadFinished(TalkClientUpload upload) {
    }

    public void onUploadFailed(TalkClientUpload upload) {
    }

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {
        if (upload.isAvatar()) {
            loadContacts();
        }
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        int count = 0;
        count += mContacts.size();

        // add saved nearby messages
        if (mShowNearbyHistory) {

            // TODO: only if nearby history was found in db
            try {
                mNearbyMessagesCount = mDatabase.getNearbyGroupMessageCount();
                if (mNearbyMessagesCount > 0) {
                    count++;
                }
            } catch (SQLException e) {
                LOG.error("SQL Error while retrieving archived nearby messages", e);
            }
        }
        return count;
    }

    @Override
    public Object getItem(int position) {
        if (position >= 0 && position < mContacts.size()) {
            return mContacts.get(position);
        }

        // TODO: only if nearby history was found in db
        if (position == getCount() - 1 && mNearbyMessagesCount > 0) {
            return "nearbyArchived";
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= 0 && position < mContacts.size()) {
            return VIEW_TYPE_CLIENT;
        }

        // TODO: only if nearby history was found in db
        if (position == getCount() - 1 && mNearbyMessagesCount > 0) {
            return VIEW_TYPE_NEARBY_HISTORY;
        }
        return VIEW_TYPE_SEPARATOR;
    }

    @Override
    public long getItemId(int position) {
        Object item = getItem(position);
        if (item instanceof TalkClientContact) {
            return ((TalkClientContact) item).getClientContactId();
        }
        return ITEM_ID_UNKNOWN;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);

        View view = convertView;
        switch (type) {
            case VIEW_TYPE_CLIENT:
                if (view == null) {
                    view = mActivity.getLayoutInflater().inflate(getClientLayout(), null);
                }
                updateContact(view, (TalkClientContact) getItem(position));
                break;
            case VIEW_TYPE_GROUP:
                if (view == null) {
                    view = mActivity.getLayoutInflater().inflate(getGroupLayout(), null);
                }
                updateContact(view, (TalkClientContact) getItem(position));
                break;
            case VIEW_TYPE_SEPARATOR:
                if (view == null) {
                    view = mActivity.getLayoutInflater().inflate(getSeparatorLayout(), null);
                }
                updateSeparator(view, position);
                break;
            case VIEW_TYPE_NEARBY_HISTORY:
                if (view == null) {
                    view = mActivity.getLayoutInflater().inflate(getNearbyHistoryLayout(), null);
                }
                updateNearbyHistoryLayout(view);
                break;
            default:
                view = mActivity.getLayoutInflater().inflate(getSeparatorLayout(), null);
                break;
        }

        return view;
    }

    protected abstract int getClientLayout();

    protected abstract int getGroupLayout();

    protected abstract int getSeparatorLayout();

    protected abstract int getNearbyHistoryLayout();

    protected abstract void updateNearbyHistoryLayout(View v);

    protected abstract void updateContact(View view, final TalkClientContact contact);

    protected void updateSeparator(View view, int position) {
        LOG.debug("updateSeparator()");
        TextView separator = (TextView) view;
        separator.setText((String) getItem(position));
    }

    public List<TalkClientContact> getContacts() {
        return ListUtils.unmodifiableList(mContacts);
    }

    public interface Filter {
        public boolean shouldShow(TalkClientContact contact);
    }
}
