package com.hoccer.xo.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.hoccer.talk.client.*;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.model.TalkGroupPresence;
import com.hoccer.xo.android.XoAndroidClient;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.BaseActivity;
import com.hoccer.xo.android.view.model.ChatItem;
import com.hoccer.xo.android.view.model.ContactChatItem;
import com.hoccer.xo.android.view.model.NearbyGroupHistoryChatItem;
import com.hoccer.xo.android.view.model.WorldwideGroupHistoryChatItem;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;

import static com.hoccer.talk.model.TalkGroupPresence.GROUP_TYPE_NEARBY;
import static com.hoccer.talk.model.TalkGroupPresence.GROUP_TYPE_WORLDWIDE;


public class ChatListAdapter extends BaseAdapter implements IXoContactListener, IXoMessageListener, TransferListener {

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
    private final XoClientDatabase mDatabase;
    private final XoAndroidClient mXoClient;

    private BaseActivity mActivity;

    @Nullable
    private Filter mFilter;
    private boolean mDoUpdateUI = true;

    public ChatListAdapter(BaseActivity activity, @Nullable Filter filter) {
        mActivity = activity;
        mFilter = filter;
        mXoClient = XoApplication.get().getClient();
        mXoClient.registerStateListener(new IXoStateListener() {
            @Override
            public void onClientStateChange(XoClient client) {
                if (client.getState() == XoClient.State.READY){
                    LOG.info("XOClient is ready. UI updates enabled.");
                    mDoUpdateUI = true;
                    loadChatItems();
                }
                if (client.getState() == XoClient.State.SYNCING){
                    LOG.info("XOClient is syncing. UI updates disabled.");
                    mDoUpdateUI = false;
                }
            }
        });
        mDatabase = mXoClient.getDatabase();
    }

    public void loadChatItems() {
        long start = System.currentTimeMillis();

        try {

            final List<TalkClientContact> filteredContacts = filter(mDatabase.findAllContacts());
            final long nearbyMessageCount = mDatabase.getHistoryGroupMessageCount(GROUP_TYPE_NEARBY);
            final long worldwideMessageCount = mDatabase.getHistoryGroupMessageCount(GROUP_TYPE_WORLDWIDE);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    mChatItems.clear();

                    for (final TalkClientContact contact : filteredContacts) {
                        mChatItems.add(new ContactChatItem(contact, mActivity));
                    }

                    if (nearbyMessageCount > 0) {
                        mChatItems.add(new NearbyGroupHistoryChatItem(mActivity));
                    }

                    if (worldwideMessageCount > 0) {
                        mChatItems.add(new WorldwideGroupHistoryChatItem(mActivity));
                    }

                    notifyDataSetChanged();
                }
            });
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }

        LOG.info("loadChatItems() " + (System.currentTimeMillis() - start));
    }

    public void registerListeners() {
        mXoClient.registerContactListener(this);
        mXoClient.registerMessageListener(this);
        mXoClient.getDownloadAgent().registerListener(this);
        mXoClient.getUploadAgent().registerListener(this);
    }

    public void unregisterListeners() {
        mXoClient.unregisterContactListener(this);
        mXoClient.unregisterMessageListener(this);
        mXoClient.getDownloadAgent().unregisterListener(this);
        mXoClient.getUploadAgent().unregisterListener(this);
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

    private ChatItem findChatItemForContact(TalkClientContact contact) {
        for (ChatItem item : mChatItems) {
            if (item instanceof ContactChatItem) {
                if (contact.equals(((ContactChatItem) item).getContact())) {
                    return item;
                }
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
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatItem item = findChatItemForContact(contact);
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
        if (mDoUpdateUI) {
            loadChatItems();
        }
    }

    @Override
    public void onGroupPresenceChanged(final TalkClientContact contact) {
        if (contact.getGroupPresence() == null || (contact.getGroupPresence().isTypeNearby() || contact.getGroupPresence().isKept())) {
            return;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatItem item = findChatItemForContact(contact);
                if (item != null) {
                    item.update();
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onGroupMembershipChanged(final TalkClientContact contact) {
        if (mDoUpdateUI) {
            loadChatItems();
        }
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
            ContactChatItem item = (ContactChatItem) findChatItemForContact(contact);
            if (item != null) {
                item.update();

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            } else {
                // message received from worldwide contact which is not in worldwide anymore, so update contacts to list the acquaintance
                loadChatItems();
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
        mActivity.runOnUiThread(new Runnable() {
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
        mActivity.runOnUiThread(new Runnable() {
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
