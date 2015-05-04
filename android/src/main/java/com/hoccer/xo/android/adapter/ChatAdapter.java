package com.hoccer.xo.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.IXoTransferListenerOld;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.view.chat.MessageItem;
import com.hoccer.xo.android.view.chat.attachments.*;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents an adapter which loads data from a given conversation and configures the
 * chat view.
 * <p/>
 * When loading the all messages from the data base this adaptor performs batching.
 * The size of a batch is defined by the constant BATCH_SIZE.
 * <p/>
 * To configure list items it uses instances of ChatMessageItem and its subtypes.
 */
public class ChatAdapter extends XoAdapter implements IXoMessageListener, IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(ChatAdapter.class);

    /**
     * Number of TalkClientMessage objects in a batch
     */
    protected static final long BATCH_SIZE = 10L;

    /**
     * Defines the distance from the bottom-most item in the chat view - in number of items.
     * If you scroll up beyond this limit the chat view will not automatically scroll to the bottom
     * when a new message is displayed.
     */
    private static final int AUTO_SCROLL_LIMIT = 5;

    protected TalkClientContact mContact;

    protected List<MessageItem> mMessageItems;

    private final ListView mListView;

    public ChatAdapter(ListView listView, XoActivity activity, TalkClientContact contact) {
        super(activity);
        mListView = listView;
        mContact = contact;
        initialize();
    }

    protected void initialize() {
        int totalMessageCount = 0;
        try {
            final List<TalkClientMessage> messages = mDatabase.findMessagesByContactId(mContact.getClientContactId(), -1, -1);
            mMessageItems = Collections.synchronizedList(new ArrayList<MessageItem>(totalMessageCount));
            for (TalkClientMessage message : messages) {
                mMessageItems.add(getItemForMessage(message));
            }
        } catch (SQLException e) {
            LOG.error("SQLException while loading message count: " + mContact.getClientId(), e);
        }
    }

    /**
     * Loads a range of TalkClientMessage objects from database starting at a given offset.
     * Range is defined by constant BATCH_SIZE.
     * <p/>
     * Creates the appropriate ChatMessageItem for each TalkClientMessage and adds it to mChatMessageItems.
     *
     * @param offset Index of the first TalkClientMessage object
     */
    public synchronized void loadNextMessages(int offset) {
        // we disabled the batching option for message loading to avoid some common errors like double messages
        // TODO: enable batch loading of messages and see if double messages still occur
        if (true) {
            return;
        }
        try {
            long batchSize = BATCH_SIZE;
            if (offset < 0) {
                batchSize = batchSize + offset;
                offset = 0;
            }
            LOG.debug("loading Messages " + offset + "-" + Math.max((offset - batchSize - 1), 0));
            final List<TalkClientMessage> messagesBatch = mDatabase.findMessagesByContactId(mContact.getClientContactId(), batchSize, offset);
            for (int i = 0; i < messagesBatch.size(); i++) {
                MessageItem messageItem = getItemForMessage(messagesBatch.get(i));

                mMessageItems.set(offset + i, messageItem);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        } catch (SQLException e) {
            LOG.error("SQLException while batch retrieving messages for contact: " + mContact.getClientId(), e);
        }
    }

    public void setContact(TalkClientContact contact) {
        mContact = contact;
        initialize();
        requestReload();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getXoClient().registerMessageListener(this);
        getXoClient().registerTransferListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getXoClient().unregisterMessageListener(this);
        getXoClient().unregisterTransferListener(this);
    }

    @Override
    public int getCount() {
        return mMessageItems.size();
    }

    @Override
    public MessageItem getItem(int position) {
        if (mMessageItems.get(position) == null) {
            int offset = position - (int) BATCH_SIZE + 1;
            loadNextMessages(offset);
        }
        return mMessageItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MessageItem chatItem = getItem(position);

        if (!chatItem.getMessage().isSeen()) {
            markMessageAsSeen(chatItem.getMessage());
        }

        if (convertView == null) {
            convertView = chatItem.createViewForMessage();
        } else {
            convertView = chatItem.recycleViewForMessage(convertView);
        }

        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return ChatItemType.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        MessageItem item = getItem(position);
        return item.getType().ordinal();
    }

    /**
     * Return the ListItemType which is appropriate for the given message / attachment.
     *
     * @param message The message to display
     * @return The corresponding ListItemType
     */
    private static ChatItemType getListItemTypeForMessage(TalkClientMessage message) {
        ChatItemType chatItemType = ChatItemType.ChatItemWithText;
        String mediaType = null;

        if (message.getAttachmentDownload() != null) {
            mediaType = message.getAttachmentDownload().getMediaType();
        } else if (message.getAttachmentUpload() != null) {
            mediaType = message.getAttachmentUpload().getMediaType();
        }

        if (mediaType != null) {
            if (mediaType.equalsIgnoreCase(ContentMediaType.IMAGE)) {
                chatItemType = ChatItemType.ChatItemWithImage;
            } else if (mediaType.equalsIgnoreCase(ContentMediaType.VIDEO)) {
                chatItemType = ChatItemType.ChatItemWithVideo;
            } else if (mediaType.equalsIgnoreCase(ContentMediaType.AUDIO)) {
                chatItemType = ChatItemType.ChatItemWithAudio;
            } else if (mediaType.equalsIgnoreCase(ContentMediaType.DATA)) {
                chatItemType = ChatItemType.ChatItemWithData;
            } else if (mediaType.equalsIgnoreCase(ContentMediaType.VCARD)) {
                chatItemType = ChatItemType.ChatItemWithContact;
            } else if (mediaType.equalsIgnoreCase(ContentMediaType.LOCATION)) {
                chatItemType = ChatItemType.ChatItemWithLocation;
            }
        }
        return chatItemType;
    }

    protected MessageItem getItemForMessage(TalkClientMessage message) {
        ChatItemType itemType = getListItemTypeForMessage(message);

        if (itemType == ChatItemType.ChatItemWithImage) {
            return new ImageMessageItem(mActivity, message);
        } else if (itemType == ChatItemType.ChatItemWithVideo) {
            return new ChatVideoItem(mActivity, message);
        } else if (itemType == ChatItemType.ChatItemWithAudio) {
            return new AudioMessageItem(mActivity, message);
        } else if (itemType == ChatItemType.ChatItemWithData) {
            return new DataMessageItem(mActivity, message);
        } else if (itemType == ChatItemType.ChatItemWithContact) {
            return new ContactMessageItem(mActivity, message);
        } else if (itemType == ChatItemType.ChatItemWithLocation) {
            return new LocationMessageItem(mActivity, message);
        } else {
            return new MessageItem(mActivity, message);
        }
    }

    protected void markMessageAsSeen(final TalkClientMessage message) {
        XoApplication.get().getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                getXoClient().markAsSeen(message);
            }
        });
    }

    @Override
    public void onReloadRequest() {
        super.onReloadRequest();
        initialize();
        notifyDataSetChanged();
    }

    // Returns whether the given message is relevant for this adapter or not
    protected boolean isMessageRelevant(TalkClientMessage message) {
        return (message.getConversationContact() == mContact);
    }

    @Override
    public void onMessageCreated(final TalkClientMessage message) {
        LOG.debug("onMessageCreated()");
        if (isMessageRelevant(message)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MessageItem messageItem = getItemForMessage(message);
                    if (!mMessageItems.contains(messageItem)) {
                        mMessageItems.add(messageItem);
                        notifyDataSetChanged();

                        // autoscroll to new item
                        if (mListView.getLastVisiblePosition() >= getCount() - AUTO_SCROLL_LIMIT) {
                            mListView.smoothScrollToPosition(getCount() - 1);
                        }
                    } else {
                        LOG.warn("tried to add a new message which was already added!");
                    }
                }
            });
        }
    }

    @Override
    public void onMessageDeleted(final TalkClientMessage message) {
        LOG.debug("onMessageDeleted()");
        if (isMessageRelevant(message)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MessageItem item = new MessageItem(mActivity, message);
                    mMessageItems.remove(item);
                    notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void onMessageUpdated(final TalkClientMessage message) {
        LOG.debug("onMessageUpdated()");
        if (isMessageRelevant(message)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
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
    public void onDownloadFinished(TalkClientDownload download) {

    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {

    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {
        if (download.isAvatar() && download.getState() == TalkClientDownload.State.COMPLETE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
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

    @Override
    public void onUploadFailed(TalkClientUpload upload) {

    }

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {
        if (upload.isAvatar()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
}
