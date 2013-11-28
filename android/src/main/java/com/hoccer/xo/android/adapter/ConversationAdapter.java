package com.hoccer.xo.android.adapter;

import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.IXoTransferListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.content.ContentView;
import com.hoccer.xo.release.R;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapter for messages in a conversation
 */
public class ConversationAdapter extends XoAdapter
        implements IXoMessageListener, IXoTransferListener {

    private static final int VIEW_TYPE_INCOMING = 0;
    private static final int VIEW_TYPE_OUTGOING = 1;

    private static final int VIEW_TYPE_COUNT = 2;

    private static final Logger LOG = Logger.getLogger(ConversationAdapter.class);

    TalkClientContact mContact;

    List<TalkClientMessage> mMessages = new Vector<TalkClientMessage>();

    ScheduledFuture<?> mReloadFuture;

    AtomicInteger mVersion = new AtomicInteger();

    public ConversationAdapter(XoActivity activity) {
        super(activity);
    }

    public void converseWithContact(TalkClientContact contact) {
        mContact = contact;
        reload();
    }

    @Override
    public void register() {
        getXoClient().registerMessageListener(this);
        getXoClient().registerTransferListener(this);
    }

    @Override
    public void unregister() {
        getXoClient().unregisterMessageListener(this);
        getXoClient().unregisterTransferListener(this);
    }

    /** Triggers change notification on the ui thread */
    private void update() {
        LOG.trace("update()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    /** Used internally to fault in related objects we need */
    private void reloadRelated(TalkClientMessage message) throws SQLException {
        TalkClientContact sender = message.getSenderContact();
        if(sender != null) {
            mDatabase.refreshClientContact(sender);
        }
        TalkClientContact conversation = message.getConversationContact();
        if(conversation != null) {
            mDatabase.refreshClientContact(conversation);
        }
    }

    /** Performs a full reload */
    @Override
    public void reload() {
        LOG.debug("reload()");
        ScheduledExecutorService executor = XoApplication.getExecutor();
        final int startVersion = mVersion.get();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    mDatabase.refreshClientContact(mContact);
                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    final List<TalkClientMessage> messages = mDatabase.findMessagesByContactId(mContact.getClientContactId());
                    for(TalkClientMessage message: messages) {
                        if(Thread.interrupted()) {
                            throw new InterruptedException();
                        }
                        reloadRelated(message);
                    }
                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    LOG.info(messages.size() + " messages");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LOG.info("new data");
                            if(mVersion.compareAndSet(startVersion, startVersion + 1)) {
                                mMessages = messages;
                            }
                            notifyDataSetChanged();
                        }
                    });
                } catch (SQLException e) {
                    LOG.error("sql error", e);
                } catch (InterruptedException e) {
                    LOG.info("RELOAD INTERRUPTED");
                } catch (Throwable e) {
                    LOG.error("error reloading", e);
                }
                synchronized (ConversationAdapter.this) {
                    mReloadFuture = null;
                }
            }
        };
        synchronized (this) {
            if(mReloadFuture != null) {
                mReloadFuture.cancel(true);
            }
            mReloadFuture = executor.schedule(runnable, 0, TimeUnit.MILLISECONDS);
        }
    }

    public boolean cancelReload() {
        boolean cancelled = false;
        synchronized (ConversationAdapter.this) {
            if(mReloadFuture != null) {
                cancelled = mReloadFuture.cancel(true);
                mReloadFuture = null;
            }
        }
        return cancelled;
    }

    @Override
    public void onMessageAdded(final TalkClientMessage message) {
        if(mContact != null && message.getConversationContact() == mContact) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean reloadAgain = cancelReload();
                    mVersion.incrementAndGet();
                    mMessages.add(message);
                    notifyDataSetChanged();
                    if(reloadAgain) {
                        reload();
                    }
                }
            });
        }
    }
    @Override
    public void onMessageRemoved(TalkClientMessage message) {
        reload();
    }

    @Override
    public void onMessageStateChanged(TalkClientMessage message) {
        update();
    }

    @Override
    public void onDownloadRegistered(TalkClientDownload download) {
    }
    @Override
    public void onDownloadStarted(TalkClientDownload download) {
        update();
    }
    @Override
    public void onDownloadProgress(TalkClientDownload download) {
        update();
    }
    @Override
    public void onDownloadFinished(TalkClientDownload download) {
        update();
    }
    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {
        update();
    }

    @Override
    public void onUploadStarted(TalkClientUpload upload) {
        update();
    }
    @Override
    public void onUploadProgress(TalkClientUpload upload) {
        update();
    }
    @Override
    public void onUploadFinished(TalkClientUpload upload) {
        update();
    }
    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {
        update();
    }




    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getCount() {
        return mMessages.size();
    }

    @Override
    public TalkClientMessage getItem(int position) {
        return mMessages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getClientMessageId();
    }

    @Override
    public int getItemViewType(int position) {
        TalkClientMessage msg = getItem(position);
        if(msg.isOutgoing()) {
            return VIEW_TYPE_OUTGOING;
        } else {
            return VIEW_TYPE_INCOMING;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        TalkClientMessage message = getItem(position);

        View v = convertView;

        switch (viewType) {
        case VIEW_TYPE_OUTGOING:
            if(v == null) {
                v = mInflater.inflate(R.layout.item_conversation_outgoing, null);
            }
            updateViewOutgoing(v, message);
            break;
        case VIEW_TYPE_INCOMING:
            if(v == null) {
                v = mInflater.inflate(R.layout.item_conversation_incoming, null);
            }
            updateViewIncoming(v, message);
            break;
        }

        return v;
    }

    private void updateViewOutgoing(View view, TalkClientMessage message) {
        updateViewCommon(view, message);
    }

    private void updateViewIncoming(View view, TalkClientMessage message) {
        updateViewCommon(view, message);
    }

    private void updateViewCommon(View view, TalkClientMessage message) {
        final TalkClientContact sendingContact = message.getSenderContact();

        if(!message.isSeen()) {
            markMessageAsSeen(message);
        }

        TextView text = (TextView)view.findViewById(R.id.message_text);
        String textString = message.getText();
        if(textString == null) {
            text.setText("<Unreadable>"); // XXX
        } else {
            text.setText(textString);
            if(textString.length() > 0) {
                text.setVisibility(View.VISIBLE);
            } else {
                text.setVisibility(View.GONE);
            }
        }

        TextView timestamp = (TextView)view.findViewById(R.id.message_time);
        Date time = message.getTimestamp();
        if(time != null) {
            timestamp.setVisibility(View.VISIBLE);
            timestamp.setText(DateUtils.getRelativeDateTimeString(
                    mActivity,
                    message.getTimestamp().getTime(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    0
            ));
        } else {
            timestamp.setVisibility(View.GONE);
        }

        final ImageView avatar = (ImageView)view.findViewById(R.id.message_avatar);
        String avatarUri = null;
        if(sendingContact != null) {
            avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.showContactProfile(sendingContact);
                }
            });
            avatarUri = sendingContact.getAvatarContentUrl();
            if(avatarUri == null) {
                if(sendingContact.isGroup()) {
                    avatarUri = "content://" + R.drawable.avatar_default_group;
                }
            }
        }
        if(avatarUri == null) {
            avatarUri = "content://" + R.drawable.avatar_default_contact;
        }
        loadAvatar(avatar, avatarUri);

        ContentView contentView = (ContentView)view.findViewById(R.id.message_content);

        int displayHeight = mResources.getDisplayMetrics().heightPixels;
        // XXX better place for this? also we might want to use the measured height of our list view
        contentView.setMaxContentHeight(Math.round(displayHeight * 0.8f));

        IContentObject contentObject = null;
        TalkClientUpload attachmentUpload = message.getAttachmentUpload();
        if(attachmentUpload != null) {
            contentObject = attachmentUpload;
        } else {
            TalkClientDownload attachmentDownload = message.getAttachmentDownload();
            if(attachmentDownload != null) {
                contentObject = attachmentDownload;
            }
        }
        if(contentObject == null) {
            contentView.setVisibility(View.GONE);
            contentView.clear();
        } else {
            contentView.setVisibility(View.VISIBLE);
            contentView.displayContent(mActivity, contentObject);
        }
    }

    private void loadAvatar(ImageView view, String url) {
        ImageLoader.getInstance().displayImage(url, view);
    }

    private void markMessageAsSeen(final TalkClientMessage message) {
        mActivity.getBackgroundExecutor().execute(new Runnable() {
            @Override
            public void run() {
                getXoClient().markAsSeen(message);
            }
        });
    }
}
