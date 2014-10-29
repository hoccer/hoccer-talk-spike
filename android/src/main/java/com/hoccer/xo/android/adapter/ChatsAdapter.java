package com.hoccer.xo.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import com.hoccer.talk.client.*;
import com.hoccer.talk.client.model.*;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.view.model.*;
import com.hoccer.xo.android.view.model.SmsChatItem;
import com.hoccer.xo.android.view.model.TalkClientChatItem;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class ChatsAdapter extends XoAdapter implements IXoContactListener, IXoMessageListener, IXoTokenListener, IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(ChatsAdapter.class);

    private static final Comparator<BaseChatItem> LATEST_ITEM_COMPARATOR = new Comparator<BaseChatItem>() {
        @Override
        public int compare(BaseChatItem chatItem1, BaseChatItem chatItem2) {

            long value1 = Math.max(chatItem1.getMessageTimeStamp(), chatItem1.getContactCreationTimeStamp());
            long value2 = Math.max(chatItem2.getMessageTimeStamp(), chatItem2.getContactCreationTimeStamp());

            if (value1 == value2) {
                return 0;
            } else if (value1 > value2) {
                return -1;
            }
            return 1;
        }
    };

    final protected List<BaseChatItem> mChatItems = new ArrayList<BaseChatItem>();

    @Nullable
    private Filter mFilter = null;

    @Nullable
    private XoClientDatabase mDatabase;

    public ChatsAdapter(XoActivity activity) {
        this(activity, null);
    }

    public ChatsAdapter(XoActivity activity, @Nullable Filter filter) {
        super(activity);
        mDatabase = XoApplication.getXoClient().getDatabase();
        mFilter = filter;
        loadChatItems();
    }

    public void loadChatItems() {
        try {
            final List<TalkClientContact> filteredContacts = filter(mDatabase.findAllContacts());
            final List<TalkClientSmsToken> allSmsTokens = mDatabase.findAllSmsTokens();
            final long nearbyMessageCount = mDatabase.getNearbyMessageCount();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int oldItemCount = mChatItems.size();
                    mChatItems.clear();

                    for (final TalkClientContact contact : filteredContacts) {
                        mChatItems.add(new TalkClientChatItem(contact, mActivity));
                    }

                    for (final TalkClientSmsToken smsToken : allSmsTokens) {
                        mChatItems.add(new SmsChatItem(smsToken, mActivity));
                    }

                    if (nearbyMessageCount > 0) {
                        mChatItems.add(new NearbyGroupChatItem());
                    }

                    notifyDataSetChanged();
                    reloadFinished();
                }
            });
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getXoClient().registerContactListener(this);
        getXoClient().registerTokenListener(this);
        getXoClient().registerTransferListener(this);
        getXoClient().registerMessageListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getXoClient().unregisterContactListener(this);
        getXoClient().unregisterTokenListener(this);
        getXoClient().unregisterTransferListener(this);
        getXoClient().unregisterMessageListener(this);
    }

    @Override
    public void onReloadRequest() {
        loadChatItems();
        super.onReloadRequest();
    }

    public Filter getFilter() {
        return mFilter;
    }

    public void setFilter(Filter filter) {
        this.mFilter = filter;
        loadChatItems();
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

    private BaseChatItem findChatItemForContent(Object content) {
        for (int i = 0; i < mChatItems.size(); i++) {
            BaseChatItem item = mChatItems.get(i);
            if (content.equals(item.getContent())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public void notifyDataSetChanged() {
        Collections.sort(mChatItems, LATEST_ITEM_COMPARATOR);
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mChatItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mChatItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= mChatItems.size()) {
            return convertView;
        }

        return mChatItems.get(position).getView(convertView, parent);
    }

    /**
     * ******************************* LISTENER IMPLEMENTATIONS ******************************
     */

    @Override
    public void onContactAdded(final TalkClientContact contact) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mFilter.shouldShow(contact)) {
                    int oldItemCount = mChatItems.size();
                    TalkClientChatItem item = new TalkClientChatItem(contact, mActivity);
                    mChatItems.add(item);
                    refreshTokens(null, false);
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onContactRemoved(final TalkClientContact contact) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int oldItemCount = mChatItems.size();

                BaseChatItem item = findChatItemForContent(contact);
                if (item == null) {
                    return;
                }
                mChatItems.remove(item);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onClientPresenceChanged(final TalkClientContact contact) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BaseChatItem item = findChatItemForContent(contact);
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

                int oldItemCount = mChatItems.size();

                if (relationship.isNone()) {
                    BaseChatItem item = findChatItemForContent(contact);
                    if (item != null) {
                        mChatItems.remove(item);

                    }
                } else {
                    BaseChatItem item = findChatItemForContent(contact);
                    if (item != null) {
                        item.update();
                    } else {
                        if (mFilter.shouldShow(contact)) {
                            item = new TalkClientChatItem(contact, mActivity);
                            mChatItems.add(item);
                        }
                    }
                }
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onGroupPresenceChanged(final TalkClientContact contact) {
        if (contact.getGroupPresence() == null || (contact.getGroupPresence().isTypeNearby() || contact.getGroupPresence().isKept())) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BaseChatItem item = findChatItemForContent(contact);
                if (item != null) {
                    item.update();
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onGroupMembershipChanged(final TalkClientContact contact) {
        loadChatItems();
    }

    @Override
    public void onMessageCreated(TalkClientMessage message) {
        updateItemForMessage(message);
    }

    @Override
    public void onMessageDeleted(TalkClientMessage message) {
        updateItemForMessage(message);
    }

    private void updateItemForMessage(TalkClientMessage message) {
        try {
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
            TalkClientChatItem item = (TalkClientChatItem) findChatItemForContent(contact);
            if (item != null) { // the contact is not in our list so we won't update anything
                item.update();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        } catch (SQLException e) {
            LOG.error("Error while retrieving contacts for message " + message.getMessageId(), e);
        }
    }

    @Override
    public void onMessageUpdated(TalkClientMessage message) {
        updateItemForMessage(message);
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
            SmsChatItem item = (SmsChatItem) findChatItemForContent(token);
            if (item != null) {
                mChatItems.remove(item);
            }
        }
        for (TalkClientSmsToken token : tokens) {
            SmsChatItem item = (SmsChatItem) findChatItemForContent(token);
            if (item == null) {
                item = new SmsChatItem(token, mActivity);
            }
            int index = mChatItems.indexOf(item);
            if (index > -1) {
                mChatItems.set(index, item);
            } else {
                mChatItems.add(item);
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
