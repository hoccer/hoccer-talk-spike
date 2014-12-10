package com.hoccer.xo.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.IXoTransferListenerOld;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.base.XoAdapter;
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
public abstract class ContactsAdapter extends XoAdapter
        implements IXoContactListener, IXoMessageListener, IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(ContactsAdapter.class);

    protected final static long ITEM_ID_UNKNOWN = -1000;
    protected final static long ITEM_ID_CLIENT_HEADER = -1;
    protected final static long ITEM_ID_GROUP_HEADER = -2;

    protected final static int VIEW_TYPE_SEPARATOR = 0;
    protected final static int VIEW_TYPE_CLIENT = 1;
    protected final static int VIEW_TYPE_GROUP = 2;
    protected static final int VIEW_TYPE_NEARBY_HISTORY = 3;

    protected final static int VIEW_TYPE_COUNT = 4;

    private boolean showNearbyHistory;
    private long mNearbyMessagesCount;

    protected ContactsAdapter(XoActivity activity) {
        super(activity);
    }

    protected ContactsAdapter(XoActivity activity, boolean showNearbyHistory) {
        super(activity);
        this.showNearbyHistory = showNearbyHistory;
    }

    Filter mFilter;

    List<TalkClientContact> mContacts = new ArrayList<TalkClientContact>();

    public Filter getFilter() {
        return mFilter;
    }

    public void setFilter(Filter filter) {
        this.mFilter = filter;
    }

    @Override
    public void onCreate() {
        LOG.debug("onCreate()");
        super.onCreate();
        getXoClient().registerContactListener(this);
        getXoClient().registerTransferListener(this);
        getXoClient().registerMessageListener(this);
    }

    @Override
    public void onDestroy() {
        LOG.debug("onDestroy()");
        super.onDestroy();
        getXoClient().unregisterContactListener(this);
        getXoClient().unregisterTransferListener(this);
        getXoClient().unregisterMessageListener(this);
    }

    @Override
    public void onReloadRequest() {
        LOG.debug("onReloadRequest()");
        super.onReloadRequest();
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                reloadFinished();
                notifyDataSetInvalidated();
            }
        });
    }

    private static List<TalkClientContact> filter(List<TalkClientContact> in, Filter filter) {
        ArrayList<TalkClientContact> res = new ArrayList<TalkClientContact>();
        for (TalkClientContact contact : in) {
            if (filter.shouldShow(contact)) {
                res.add(contact);
            }
        }
        return res;
    }

    @Override
    public void onContactAdded(TalkClientContact c) {
        requestReload();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        requestReload();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        requestReload();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        requestReload();
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        requestReload();
    }

    @Override
    public void onMessageCreated(TalkClientMessage message) {
        requestReload();
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
            requestReload();
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
            requestReload();
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
        if (showNearbyHistory) {

            // TODO: only if nearby history was found in db
            try {
                mNearbyMessagesCount = mDatabase.getNearbyMessageCount();
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

        View v = convertView;
        switch (type) {
            case VIEW_TYPE_CLIENT:
                if (v == null) {
                    v = mInflater.inflate(getClientLayout(), null);
                }
                updateContact(v, (TalkClientContact) getItem(position));
                break;
            case VIEW_TYPE_GROUP:
                if (v == null) {
                    v = mInflater.inflate(getGroupLayout(), null);
                }
                updateContact(v, (TalkClientContact) getItem(position));
                break;
            case VIEW_TYPE_SEPARATOR:
                if (v == null) {
                    v = mInflater.inflate(getSeparatorLayout(), null);
                }
                updateSeparator(v, position);
                break;
            case VIEW_TYPE_NEARBY_HISTORY:
                if (v == null) {
                    v = mInflater.inflate(getNearbyHistoryLayout(), null);
                }
                updateNearbyHistoryLayout(v);
                break;
            default:
                v = mInflater.inflate(getSeparatorLayout(), null);
                break;
        }

        return v;
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
