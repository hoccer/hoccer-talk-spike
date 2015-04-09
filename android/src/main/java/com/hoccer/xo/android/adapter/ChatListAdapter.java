package com.hoccer.xo.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.IXoTransferListenerOld;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.view.model.*;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class ChatListAdapter extends XoAdapter implements IXoContactListener, IXoMessageListener, IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(ChatListAdapter.class);

    private static final Comparator<ChatItem> LATEST_ITEM_COMPARATOR = new Comparator<ChatItem>() {
        @Override
        public int compare(ChatItem chatItem1, ChatItem chatItem2) {

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

    final protected List<ChatItem> mChatItems = new ArrayList<ChatItem>();

    @Nullable
    private Filter mFilter;

    public ChatListAdapter(XoActivity activity, @Nullable Filter filter) {
        super(activity);
        mFilter = filter;
        loadChatItems();
    }

    public void loadChatItems() {
        try {
            final List<TalkClientContact> filteredContacts = filter(mDatabase.findAllContacts());
            final long nearbyMessageCount = mDatabase.getNearbyGroupMessageCount();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mChatItems.clear();

                    for (final TalkClientContact contact : filteredContacts) {
                        if (contact.isClient() && contact.getClientRelationship() != null && (contact.getClientRelationship().isFriend() || contact.getClientRelationship().isBlocked())) {
                            mChatItems.add(new ClientChatItem(contact, mActivity));
                        } else if (contact.isClient() && contact.isKept()) {
                            if (contact.isNearbyAcquaintance()) {
                                mChatItems.add(new NearbyHistoryClientChatItem(contact, mActivity));
                            } else {
                                mChatItems.add(new HistoryClientChatItem(contact, mActivity));
                            }
                        }
                    }

                    if (nearbyMessageCount > 0) {
                        mChatItems.add(new NearbyHistoryGroupChatItem());
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
        getXoClient().registerTransferListener(this);
        getXoClient().registerMessageListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getXoClient().unregisterContactListener(this);
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

    private ChatItem findChatItemForContent(Object content) {
        for (ChatItem item : mChatItems) {
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
    public void onClientPresenceChanged(final TalkClientContact contact) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatItem item = findChatItemForContent(contact);
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
        loadChatItems();
    }

    @Override
    public void onGroupPresenceChanged(final TalkClientContact contact) {
        if (contact.getGroupPresence() == null || (contact.getGroupPresence().isTypeNearby() || contact.getGroupPresence().isKept())) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatItem item = findChatItemForContent(contact);
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
                contact = mDatabase.findGroupContactByGroupId(conversationContact.getGroupId(), false);
            } else {
                contact = mDatabase.findContactByClientId(conversationContact.getClientId(), false);
            }
            if (contact == null) {
                return;
            }
            ClientChatItem item = (ClientChatItem) findChatItemForContent(contact);
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
