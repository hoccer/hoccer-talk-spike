package com.hoccer.xo.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.TransferListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.client.predicates.TalkClientContactPredicates;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.avatar.AvatarView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.hoccer.talk.model.TalkEnvironment.TYPE_NEARBY;
import static com.hoccer.talk.model.TalkEnvironment.TYPE_WORLDWIDE;

public class EnvironmentChatListAdapter extends BaseAdapter implements IXoContactListener, IXoMessageListener, TransferListener {

    private static final Logger LOG = Logger.getLogger(EnvironmentChatListAdapter.class);
    private static final long RATE_LIMIT_MSECS = 1000;

    private final String mEnvironmentType;

    private final XoClientDatabase mDatabase;
    private final ScheduledExecutorService mExecutor;
    private final XoActivity mXoActivity;
    private ScheduledFuture<?> mNotifyFuture;
    private long mNotifyTimestamp;

    private List<TalkClientContact> mContacts = new ArrayList<TalkClientContact>();
    private TalkClientContact mCurrentEnvironmentGroup;

    public EnvironmentChatListAdapter(String environmentType, XoActivity activity) {
        super();
        mEnvironmentType = environmentType;
        mXoActivity = activity;
        mDatabase = XoApplication.get().getXoClient().getDatabase();
        mExecutor = XoApplication.get().getExecutor();
    }

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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_client, null);
        }
        updateContact(convertView, (TalkClientContact) getItem(position));
        return convertView;
    }

    public void registerListeners() {
        XoApplication.get().getXoClient().registerContactListener(this);
        XoApplication.get().getXoClient().registerMessageListener(this);
        XoApplication.get().getXoClient().getDownloadAgent().registerListener(this);
        XoApplication.get().getXoClient().getUploadAgent().registerListener(this);
    }

    public void unregisterListeners() {
        XoApplication.get().getXoClient().unregisterContactListener(this);
        XoApplication.get().getXoClient().unregisterMessageListener(this);
        XoApplication.get().getXoClient().getDownloadAgent().unregisterListener(this);
        XoApplication.get().getXoClient().getUploadAgent().unregisterListener(this);
    }

    private void updateContact(final View view, final TalkClientContact contact) {
        TextView nameView = ViewHolderForAdapters.get(view, R.id.contact_name);
        AvatarView avatarView = ViewHolderForAdapters.get(view, R.id.contact_icon);
        TextView lastMessageTimeView = (TextView) view.findViewById(R.id.contact_time);
        TextView lastMessageText = (TextView) view.findViewById(R.id.contact_last_message);
        TextView unseenView = (TextView) view.findViewById(R.id.contact_unseen_messages);

        if (contact.isGroup()) {
            if (TYPE_WORLDWIDE.equals(mEnvironmentType)) {
                nameView.setText(mXoActivity.getResources().getString(R.string.all_worldwide) + " (" + (mContacts.size() - 1) + ")");
            } else if (TYPE_NEARBY.equals(mEnvironmentType)) {
                nameView.setText(mXoActivity.getResources().getString(R.string.all_nearby) + " (" + (mContacts.size() - 1) + ")");
            }
        } else {
            nameView.setText(contact.getNickname());
        }
        avatarView.setContact(contact);

        lastMessageText.setText("");
        lastMessageTimeView.setText("");
        unseenView.setText("");

        TalkClientMessage message = null;
        long unseenMessages = 0;
        try {
            message = mDatabase.findLatestMessageByContactId(contact.getClientContactId());
            unseenMessages = mDatabase.findUnseenMessageCountByContactId(contact.getClientContactId());
        } catch (SQLException e) {
            LOG.error("SQL error while retrieving " + mEnvironmentType + " message data ", e);
        }
        if (message != null) {
            Date messageTime = message.getTimestamp();
            SimpleDateFormat sdf = new SimpleDateFormat("EEE HH:mm");
            String lastMessageTime = sdf.format(messageTime);

            lastMessageTimeView.setText(lastMessageTime);
            if (message.getAttachmentDownload() != null) {
                TalkClientDownload attachment = message.getAttachmentDownload();
                lastMessageText.setText(getAttachmentReceivedText(view.getContext(),
                        attachment.getMediaType()));
            } else {
                lastMessageText.setText(message.getText());
            }
        }
        if (unseenMessages > 0) {
            unseenView.setText(Long.toString(unseenMessages));
            unseenView.setVisibility(View.VISIBLE);
        } else {
            unseenView.setVisibility(View.INVISIBLE);
        }
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mXoActivity.showContactProfile(contact);
            }
        });
    }

    private static String getAttachmentReceivedText(Context context, String attachmentType) {
        String text = context.getResources().getString(R.string.contact_item_received_attachment);
        return String.format(text, attachmentType);
    }

    public void scheduleUpdate(final TalkClientContact group) {
        long now = System.currentTimeMillis();
        long delta = now - mNotifyTimestamp;
        if (mNotifyFuture != null) {
            mNotifyFuture.cancel(false);
            mNotifyFuture = null;
        }

        if (delta < RATE_LIMIT_MSECS) {
            long delay = RATE_LIMIT_MSECS - delta;

            LOG.debug("Scheduling update of adapter with delay " + delay);
            mNotifyFuture = mExecutor.schedule(
                    new Runnable() {
                        @Override
                        public void run() {
                            mNotifyTimestamp = System.currentTimeMillis();
                            LOG.debug("Executing scheduled update of adapter.");
                            updateFromDatabase(group);
                        }
                    },
                    delay,
                    TimeUnit.MILLISECONDS);
        } else {
            mNotifyTimestamp = System.currentTimeMillis();
            LOG.debug("Updating adapter right away.");
            updateFromDatabase(group);
        }
    }

    private void updateFromDatabase(final TalkClientContact group) {
        if (group == null || mDatabase == null) {
            return;
        }

        try {
            final List<TalkClientContact> contacts = mDatabase.findContactsInGroupByState(group.getGroupId(), TalkGroupMembership.STATE_JOINED);
            CollectionUtils.filterInverse(contacts, TalkClientContactPredicates.IS_SELF_PREDICATE);

            if (!contacts.isEmpty()) {
                contacts.add(0, group);
            }

            mXoActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mContacts = contacts;
                    notifyDataSetChanged();
                }
            });
        } catch (SQLException e) {
            LOG.error("SQL Error while retrieving " + mEnvironmentType + " group contacts.", e);
        }
    }

    private void refreshList() {
        mXoActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        refreshList();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        if (mCurrentEnvironmentGroup != null && contact.getGroupId().equals(mCurrentEnvironmentGroup.getGroupId())) {
            LOG.debug("onGroupPresenceChanged()");
            scheduleUpdate(contact);
        }
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        TalkClientContact environmentGroup = getCurrentEnvironmentGroup();

        if (environmentGroup != null && groupMatchesEnvironmentType(environmentGroup)) {
            mCurrentEnvironmentGroup = environmentGroup;
        } else {
            mCurrentEnvironmentGroup = null;
        }
        scheduleUpdate(mCurrentEnvironmentGroup);
    }

    private boolean groupMatchesEnvironmentType(TalkClientContact environmentGroup) {
        return TalkEnvironment.TYPE_NEARBY.equals(mEnvironmentType) && environmentGroup.isNearbyGroup() ||
                TalkEnvironment.TYPE_WORLDWIDE.equals(mEnvironmentType) && environmentGroup.isWorldwideGroup();
    }

    @Nullable
    private TalkClientContact getCurrentEnvironmentGroup() {
        return XoApplication.get().getXoClient().getCurrentEnvironmentGroup();
    }

    @Override
    public void onMessageCreated(final TalkClientMessage message) {
        mXoActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TalkClientContact conversationContact = message.getConversationContact();
                if (mContacts.contains(conversationContact)) {
                    mContacts.remove(conversationContact);
                    int position = 0;
                    if (conversationContact.isClient()) {
                        position = 1;
                    }
                    mContacts.add(position, conversationContact);
                }
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onMessageDeleted(TalkClientMessage message) {
        refreshList();
    }

    @Override
    public void onMessageUpdated(TalkClientMessage message) {
        refreshList();
    }

    @Override
    public void onDownloadRegistered(TalkClientDownload download) {}

    @Override
    public void onDownloadStarted(TalkClientDownload download) {}

    @Override
    public void onDownloadProgress(TalkClientDownload download) {}

    @Override
    public void onDownloadFinished(TalkClientDownload download) {
        if (download.isAvatar()) {
            refreshList();
        }
    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {}

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {}

    @Override
    public void onUploadStarted(TalkClientUpload upload) {
        if (upload.isAvatar()) {
            refreshList();
        }
    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {}

    @Override
    public void onUploadFinished(TalkClientUpload upload) {}

    @Override
    public void onUploadFailed(TalkClientUpload upload) {}

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {}
}
