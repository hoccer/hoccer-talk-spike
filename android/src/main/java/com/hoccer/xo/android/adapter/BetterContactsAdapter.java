package com.hoccer.xo.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.IXoTokenListener;
import com.hoccer.talk.client.IXoTransferListenerOld;
import com.hoccer.talk.client.model.*;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.view.model.BaseContactItem;
import com.hoccer.xo.android.view.model.NearbyGroupContactItem;
import com.hoccer.xo.android.view.model.SmsContactItem;
import com.hoccer.xo.android.view.model.TalkClientContactItem;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class BetterContactsAdapter extends XoAdapter implements IXoContactListener, IXoMessageListener, IXoTokenListener, IXoTransferListenerOld {

    private static final Comparator<BaseContactItem> LATEST_MESSAGE_COMPARATOR = new Comparator<BaseContactItem>() {
        @Override
        public int compare(BaseContactItem o1, BaseContactItem o2) {
            if (o1.getTimeStamp() == o2.getTimeStamp()) {
                return 0;
            } else if (o1.getTimeStamp() > o2.getTimeStamp()) {
                return -1;
            }
            return 1;
        }
    };

    final private List<BaseContactItem> mContactItems = new ArrayList<BaseContactItem>();

    @Nullable
    private Filter mFilter = null;

    @Nullable
    private OnItemCountChangedListener mOnItemCountChangedListener;

    public BetterContactsAdapter(XoActivity activity) {
        super(activity);
        initialize();
    }

    public BetterContactsAdapter(XoActivity activity, @Nullable Filter filter) {
        super(activity);
        mFilter = filter;
        initialize();
    }

    private void initialize() {
        LOG.debug("initialize()");

        synchronized (this) {

            int oldItemCount = mContactItems.size();
            mContactItems.clear();
            try {
                List<TalkClientContact> allClientContacts = mActivity.getXoDatabase().findAllClientContacts();
                List<TalkClientContact> filteredContacts = filter(allClientContacts);
                for (TalkClientContact contact : filteredContacts) {
                    mContactItems.add(new TalkClientContactItem(mActivity, contact));
                }

                List<TalkClientSmsToken> allSmsTokens = mActivity.getXoDatabase().findAllSmsTokens();
                for (TalkClientSmsToken smsToken : allSmsTokens) {
                    mContactItems.add(new SmsContactItem(mActivity, smsToken));
                }

                long nearbyMessageCount = mDatabase.getNearbyMessageCount();
                if (nearbyMessageCount > 0) {
                    mContactItems.add(new NearbyGroupContactItem(mActivity));
                }

                if (mOnItemCountChangedListener != null && oldItemCount != getCount()) {
                    mOnItemCountChangedListener.onItemCountChanged(getCount());
                }
                notifyDataSetChanged();
                reloadFinished();
            } catch (SQLException e) {
                LOG.error("sql error", e);
            }
        }
    }

    public void setOnItemCountChangedListener(OnItemCountChangedListener onItemCountChangedListener) {
        mOnItemCountChangedListener = onItemCountChangedListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getXoClient().registerContactListener(this);
        getXoClient().registerTokenListener(this);
        getXoClient().registerTransferListener(this);
        getXoClient().registerMessageListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getXoClient().unregisterContactListener(this);
        getXoClient().unregisterTokenListener(this);
        getXoClient().unregisterTransferListener(this);
        getXoClient().unregisterMessageListener(this);
    }

    @Override
    public void onReloadRequest() {
        initialize();
        super.onReloadRequest();
    }

    public Filter getFilter() {
        return mFilter;
    }

    public void setFilter(Filter filter) {
        this.mFilter = filter;
        initialize();
    }

    private List<TalkClientContact> filter(List<TalkClientContact> in) {
        if (mFilter == null) {
            return in;
        }
        ArrayList<TalkClientContact> res = new ArrayList<TalkClientContact>();
        for (TalkClientContact contact : in) {
            if (mFilter.shouldShow(contact)) {
                res.add(contact);
            }
        }
        return res;
    }

    private BaseContactItem findContactItemForContent(Object content) {
        for (int i = 0; i < mContactItems.size(); i++) {
            BaseContactItem item = mContactItems.get(i);
            if (content.equals(item.getContent())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public void notifyDataSetChanged() {
        LOG.debug("notifyDataSetChanged()");
        Collections.sort(mContactItems, LATEST_MESSAGE_COMPARATOR);
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mContactItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mContactItems.get(i).getContent();
    }

    @Override
    public long getItemId(int i) {
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if (position >= mContactItems.size()) {
            return convertView;
        }

        return mContactItems.get(position).getView(convertView);
    }


    /**
     * ******************************* LISTENER IMPLEMENTATIONS ******************************
     */

    @Override
    public void onContactAdded(final TalkClientContact contact) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TalkClientContactItem item = new TalkClientContactItem(mActivity, contact);
                mContactItems.add(item);
                refreshTokens(null, false);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onContactRemoved(final TalkClientContact contact) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BaseContactItem item = findContactItemForContent(contact);
                if (item == null) {
                    return;
                }
                mContactItems.remove(item);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onClientPresenceChanged(final TalkClientContact contact) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BaseContactItem item = findContactItemForContent(contact);
                if (item == null) {
                    return;
                }
                item.update();
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onClientRelationshipChanged(final TalkClientContact contact) {
        final TalkRelationship relationship = contact.getClientRelationship();
        if (relationship == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (relationship.isNone()) {
                    BaseContactItem item = findContactItemForContent(contact);
                    if (item != null) {
                        mContactItems.remove(item);

                    }
                } else {
                    BaseContactItem item = findContactItemForContent(contact);
                    if (item == null) {
                        item = new TalkClientContactItem(mActivity, contact);
                        mContactItems.add(item);
                    }
                    item.update();
                }
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onGroupPresenceChanged(final TalkClientContact contact) {
        if (contact.getGroupPresence() != null && (contact.getGroupPresence().isTypeNearby() || contact.getGroupPresence().isKept())) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BaseContactItem item = findContactItemForContent(contact);
                if (item != null) {
                    item.update();
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onGroupMembershipChanged(final TalkClientContact contact) {
        if (contact.getGroupPresence() != null && (contact.getGroupPresence().isTypeNearby() || contact.getGroupPresence().isKept())) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BaseContactItem item = findContactItemForContent(contact);
                if (item == null) {
                    item = new TalkClientContactItem(mActivity, contact);
                    mContactItems.add(item);
                    notifyDataSetChanged();
                } else {
                    if (!contact.isGroupInvolved()) {
                        mContactItems.remove(item);
                        notifyDataSetChanged();
                    }
                }
            }
        });
    }

    @Override
    public void onMessageAdded(TalkClientMessage message) {
        try {
            if (message.isIncoming()) {
                TalkClientContact conversationContact = message.getConversationContact();
                TalkClientContact contact;
                if (conversationContact.isGroup()) {
                    contact = mDatabase.findContactByGroupId(conversationContact.getGroupId(), false);
                } else {
                    contact = mDatabase.findContactByClientId(conversationContact.getClientId(), false);
                }
                if (contact == null) {
                    return;
                }
                TalkClientContactItem item = (TalkClientContactItem) findContactItemForContent(contact);
                if (item == null) { // the contact is not in our list so we won't update anything
                    return;
                }
                item.update();
                notifyDataSetChanged();
            }
        } catch (SQLException e) {
            LOG.error("Error while retrieving contacts for message " + message.getMessageId(), e);
        }
    }

    @Override
    public void onMessageRemoved(TalkClientMessage message) {
    }

    @Override
    public void onMessageStateChanged(TalkClientMessage message) {
    }

    @Override
    public void onTokensChanged(List<TalkClientSmsToken> tokens, boolean newTokens) {
        refreshTokens(tokens, newTokens);
        requestReload();
    }

    private void refreshTokens(List<TalkClientSmsToken> tokens, boolean newTokens) {
        if (tokens == null) {
            try {
                tokens = mDatabase.findAllSmsTokens();
            } catch (SQLException e) {
                LOG.error("SQLError while retrieving SMS tokens ", e);
            }
        }
        if (tokens == null) {
            return;
        }

        for (TalkClientSmsToken token : tokens) {
            SmsContactItem item = (SmsContactItem) findContactItemForContent(token);
            if (item != null) {
                mContactItems.remove(item);
            }
        }
        for (TalkClientSmsToken token : tokens) {
            SmsContactItem item = (SmsContactItem) findContactItemForContent(token);
            if (item == null) {
                item = new SmsContactItem(mActivity, token);
            }
            int index = mContactItems.indexOf(item);
            if (index > -1) {
                mContactItems.set(index, item);
            } else {
                mContactItems.add(item);
            }
        }
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
    public void onDownloadFinished(final TalkClientDownload download) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (download.isAvatar()) {
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {
    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {
    }

    @Override
    public void onUploadStarted(TalkClientUpload upload) {
    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {
    }

    @Override
    public void onUploadFinished(final TalkClientUpload upload) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (upload.isAvatar()) {
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onUploadFailed(TalkClientUpload upload) {
    }

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {
    }

    public interface Filter {
        public boolean shouldShow(TalkClientContact contact);
    }

}
